package com.blessedmike.arenahelper;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArenaAdvisor {

    private static final String TAG = "ArenaAdvisor";

    private static final String CARDS_URL =
            "https://api.hearthstonejson.com/v1/latest/enUS/cards.collectible.json";

    private static final String HEARTHARENA_URL =
            "https://www.heartharena.com/tierlist";

    private static final double UNKNOWN_CARD_SCORE = 0.0;

    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(2);

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    private static final Map<String, String> CANONICAL_NAMES =
            new HashMap<>();

    private static final Map<String, String> ALIASES =
            new HashMap<>();

    private static final Map<String, CardInfo> CARD_INFO =
            new HashMap<>();

    private static final Map<String, Map<String, Double>> CLASS_SCORES =
            new HashMap<>();

    private static final Map<String, Double> NEUTRAL_SCORES =
            new HashMap<>();

    private static final Map<String, Integer> PICKED_CARDS =
            new HashMap<>();

    private static volatile boolean onlineLoaded = false;
    private static volatile boolean hearthArenaLoaded = false;
    private static volatile boolean hearthArenaLoading = false;

    private static volatile String status = "Käynnistetään...";
    private static volatile String reason = "";

    private static volatile String currentClass = "";

    private static String candidateClass = "";
    private static int candidateClassCount = 0;

    private static final int CLASS_CONFIRMATIONS = 2;

    private static final String[] VALID_CLASSES = {
            "DEATH KNIGHT",
            "DEMON HUNTER",
            "DRUID",
            "HUNTER",
            "MAGE",
            "PALADIN",
            "PRIEST",
            "ROGUE",
            "SHAMAN",
            "WARLOCK",
            "WARRIOR"
    };

    private static final Map<String, Double> FALLBACK_SCORES =
            new HashMap<>();

    /*
     * ============================================================
     * ANALYSIS CONSTANTS
     * ============================================================
     *
     * HearthArena remains the primary card-quality signal.
     *
     * Secondary systems modify the value only within bounded
     * ranges so that deck context cannot completely override
     * the underlying card quality.
     */

    private static final double CURVE_MAX_BONUS = 0.45;

    private static final double TYPE_MAX_BONUS = 0.25;

    private static final double SYNERGY_MAX_BONUS = 0.60;

    private static final double DECK_FIT_MAX_BONUS = 0.45;

    private static final double REMOVAL_TEMPO_MAX_BONUS = 0.55;

    private static final double DUPLICATE_PENALTY = 0.15;

    private static final int MAX_MANA_COST = 10;

    /*
     * ============================================================
     * RECOMMENDATION CACHE
     * ============================================================
     */

    private static volatile String lastRecommendation = "";

    private static volatile double lastRecommendationScore = 0.0;

    private static volatile double lastRecommendationGap = 0.0;

    private static volatile String lastRecommendationReason = "";

    private static final Map<String, DraftAnalysis> LAST_ANALYSES =
            new HashMap<>();

    static {

        initializeClassMaps();
        initializeFallbackScores();
        initializeAliases();

        loadCards();
    }

    /*
     * ============================================================
     * INITIALIZATION
     * ============================================================
     */

    private static void initializeClassMaps() {

        for (String className : VALID_CLASSES) {

            CLASS_SCORES.put(
                    className,
                    new HashMap<>()
            );
        }
    }

    private static void initializeFallbackScores() {

        FALLBACK_SCORES.put(
                normalize("Temporal Construct"),
                5.54
        );

        FALLBACK_SCORES.put(
                normalize("Bitter End"),
                6.62
        );

        FALLBACK_SCORES.put(
                normalize("Sealed Lancer"),
                5.56
        );

        FALLBACK_SCORES.put(
                normalize("Scaled Lancer"),
                5.56
        );

        FALLBACK_SCORES.put(
                normalize("Soldier of the Infinite"),
                5.80
        );

        FALLBACK_SCORES.put(
                normalize("Soldier of the Bronze"),
                4.60
        );
    }

    private static void initializeAliases() {

        addAlias(
                "soldier of ihfini",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "so1dier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldieroftheinfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of ihfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of bronze",
                "Soldier of the Bronze"
        );

        addAlias(
                "soldierofthebronze",
                "Soldier of the Bronze"
        );

        addAlias(
                "temporalconstruct",
                "Temporal Construct"
        );

        addAlias(
                "temporal construct",
                "Temporal Construct"
        );

        addAlias(
                "bitterend",
                "Bitter End"
        );

        addAlias(
                "bitter end",
                "Bitter End"
        );

        addAlias(
                "sealedlancer",
                "Sealed Lancer"
        );

        addAlias(
                "sealed lancer",
                "Sealed Lancer"
        );

        addAlias(
                "scaledlancer",
                "Scaled Lancer"
        );

        addAlias(
                "scaled lancer",
                "Scaled Lancer"
        );
    }

    private static void addAlias(
            String bad,
            String correct
    ) {

        ALIASES.put(
                normalize(bad),
                correct
        );
    }

    /*
     * ============================================================
     * HEARTHSTONEJSON
     * ============================================================
     */

    private static void loadCards() {

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

                status =
                        "Ladataan korttitietoja...";

                URL url =
                        new URL(CARDS_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);

                connection.setRequestProperty(
                        "User-Agent",
                        "ArenaHelper/1.0"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode !=
                        HttpURLConnection.HTTP_OK) {

                    throw new Exception(
                            "HTTP " + responseCode
                    );
                }

                String json =
                        readStream(
                                connection.getInputStream()
                        );

                JSONArray array =
                        new JSONArray(json);

                int count = 0;

                CARDS.clear();
                CANONICAL_NAMES.clear();
                CARD_INFO.clear();

                for (int i = 0;
                     i < array.length();
                     i++) {

                    JSONObject object =
                            array.optJSONObject(i);

                    if (object == null) {
                        continue;
                    }

                    String name =
                            object.optString(
                                    "name",
                                    ""
                            ).trim();

                    if (name.isEmpty()) {
                        continue;
                    }

                    String cardClass =
                            object.optString(
                                    "cardClass",
                                    ""
                            ).trim();

                    String type =
                            object.optString(
                                    "type",
                                    ""
                            ).trim();

                    String rarity =
                            object.optString(
                                    "rarity",
                                    ""
                            ).trim();

                    String id =
                            object.optString(
                                    "id",
                                    ""
                            ).trim();

                    String text =
                            object.optString(
                                    "text",
                                    ""
                            ).trim();

                    String race =
                            object.optString(
                                    "race",
                                    ""
                            ).trim();

                    String spellSchool =
                            object.optString(
                                    "spellSchool",
                                    ""
                            ).trim();

                    JSONArray mechanicsArray =
                            object.optJSONArray(
                                    "mechanics"
                            );

                    String mechanics =
                            jsonArrayToText(
                                    mechanicsArray
                            );

                    int cost =
                            object.optInt(
                                    "cost",
                                    0
                            );

                    int attack =
                            object.optInt(
                                    "attack",
                                    0
                            );

                    int health =
                            object.optInt(
                                    "health",
                                    0
                            );

                    CardData data =
                            new CardData(
                                    name,
                                    cardClass,
                                    type,
                                    rarity,
                                    id,
                                    text,
                                    cost,
                                    attack,
                                    health,
                                    race,
                                    spellSchool,
                                    mechanics
                            );

                    String key =
                            normalize(name);

                    CARDS.put(
                            key,
                            data
                    );

                    CANONICAL_NAMES.put(
                            key,
                            name
                    );

                    CARD_INFO.put(
                            key,
                            new CardInfo(
                                    name,
                                    normalizeClass(cardClass)
                            )
                    );

                    count++;
                }

                onlineLoaded =
                        count > 0;

                Log.d(
                        TAG,
                        "HearthstoneJSON cards: " + count
                );

                if (!onlineLoaded) {

                    status =
                            "Korttitietokanta tyhjä";

                    return;
                }

                status =
                        "Korttitiedot ladattu: " + count;

                loadHearthArenaScores();

            } catch (Exception e) {

                onlineLoaded = false;

                Log.e(
                        TAG,
                        "Card database failed",
                        e
                );

                status =
                        "Korttitiedot eivät latautuneet";

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static String jsonArrayToText(
            JSONArray array
    ) {

        if (array == null ||
                array.length() == 0) {

            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < array.length();
             i++) {

            String value =
                    array.optString(
                            i,
                            ""
                    );

            if (value.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(" ");
            }

            builder.append(value);
        }

        return builder.toString();
    }

    /*
     * ============================================================
     * HEARTHARENA
     * ============================================================
     */

    private static synchronized void loadHearthArenaScores() {

        if (hearthArenaLoading) {
            return;
        }

        if (CANONICAL_NAMES.isEmpty()) {
            return;
        }

        hearthArenaLoading = true;

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

                status =
                        "Ladataan HearthArena-arvoja...";

                URL url =
                        new URL(HEARTHARENA_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);

                connection.setInstanceFollowRedirects(true);

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 16) " +
                                "AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) " +
                                "Chrome/140.0 Mobile Safari/537.36"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml," +
                                "application/xml;q=0.9,*/*;q=0.8"
                );

                connection.setRequestProperty(
                        "Accept-Language",
                        "en-US,en;q=0.9"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode !=
                        HttpURLConnection.HTTP_OK) {

                    throw new Exception(
                            "HearthArena HTTP " + responseCode
                    );
                }

                String html =
                        readStream(
                                connection.getInputStream()
                        );

                if (html == null ||
                        html.length() < 1000) {

                    throw new Exception(
                            "HearthArena response empty"
                    );
                }

                clearHearthArenaMapsOnly();

                int parsed =
                        parseHearthArenaHtml(html);

                if (parsed < 5) {

                    clearHearthArenaMapsOnly();

                    parsed =
                            parseHearthArenaText(
                                    htmlToText(html)
                            );
                }

                if (parsed <= 0) {

                    throw new Exception(
                            "HearthArena parser found 0 scores"
                    );
                }

                hearthArenaLoaded = true;

                status =
                        "HearthArena ladattu: "
                                + parsed
                                + " arvoa";

                Log.d(
                        TAG,
                        "HearthArena scores loaded: " + parsed
                );

                logTestScore("Alter Time");
                logTestScore("Merry Moonkin");
                logTestScore("Soldier of the Infinite");
                logTestScore("Soldier of the Bronze");
                logTestScore("Temporal Construct");
                logTestScore("Bitter End");

            } catch (Exception e) {

                hearthArenaLoaded = false;

                Log.e(
                        TAG,
                        "HearthArena loading failed",
                        e
                );

                status =
                        "HearthArena ei latautunut";

            } finally {

                hearthArenaLoading = false;

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static synchronized void clearHearthArenaMapsOnly() {

        for (Map<String, Double> map :
                CLASS_SCORES.values()) {

            map.clear();
        }

        NEUTRAL_SCORES.clear();

        hearthArenaLoaded = false;
    }

    private static void logTestScore(
            String cardName
    ) {

        String key =
                normalize(cardName);

        Double neutral =
                NEUTRAL_SCORES.get(key);

        if (neutral != null) {

            Log.d(
                    TAG,
                    "TEST SCORE "
                            + cardName
                            + " = "
                            + neutral
                            + " [NEUTRAL]"
            );

            return;
        }

        for (String className :
                VALID_CLASSES) {

            Map<String, Double> map =
                    CLASS_SCORES.get(className);

            if (map == null) {
                continue;
            }

            Double value =
                    map.get(key);

            if (value != null) {

                Log.d(
                        TAG,
                        "TEST SCORE "
                                + cardName
                                + " = "
                                + value
                                + " ["
                                + className
                                + "]"
                );

                return;
            }
        }

        Log.d(
                TAG,
                "TEST SCORE "
                        + cardName
                        + " = NOT FOUND"
        );
    }

    /*
     * ============================================================
     * HTML PARSER
     * ============================================================
     */

    private static int parseHearthArenaHtml(
            String html
    ) {

        if (html == null ||
                html.isEmpty()) {

            return 0;
        }

        String text =
                htmlToText(html);

        return parseHearthArenaText(text);
    }

    /*
     * ============================================================
     * TEXT PARSER
     * ============================================================
     */

    private static int parseHearthArenaText(
            String input
    ) {

        if (input == null ||
                input.isEmpty()) {

            return 0;
        }

        String text =
                input;

        String[] lines =
                text.split("\\r?\\n");

        String activeClass = "";

        int parsed = 0;

        Set<String> seen =
                new HashSet<>();

        for (int i = 0;
             i < lines.length;
             i++) {

            String line =
                    cleanLine(lines[i]);

            if (line.isEmpty()) {
                continue;
            }

            String detectedClass =
                    detectClassHeader(line);

            if (!detectedClass.isEmpty()) {

                activeClass =
                        detectedClass;

                Log.d(
                        TAG,
                        "HearthArena class section: "
                                + activeClass
                                + " <- "
                                + line
                );

                continue;
            }

            if (isNeutralHeader(line)) {

                activeClass =
                        "NEUTRAL";

                continue;
            }

            if (activeClass.isEmpty()) {
                continue;
            }

            String possibleName =
                    removeRankingPrefix(line);

            possibleName =
                    removeTierNoise(possibleName);

            if (possibleName.isEmpty()) {
                continue;
            }

            if (parseScore(possibleName) != null) {
                continue;
            }

            if (isTierOnlyLine(possibleName)) {
                continue;
            }

            String canonical =
                    findCanonicalCardName(possibleName);

            if (canonical == null) {

                canonical =
                        findCardNameInsideLine(
                                possibleName
                        );
            }

            if (canonical == null) {
                continue;
            }

            Double score =
                    findScoreOnSameLine(
                            possibleName,
                            canonical
                    );

            if (score == null) {

                score =
                        findFollowingScore(
                                lines,
                                i
                        );
            }

            if (score == null) {
                continue;
            }

            if (score < 0 ||
                    score > 200) {

                continue;
            }

            String key =
                    normalize(canonical);

            if (key.isEmpty()) {
                continue;
            }

            String seenKey =
                    activeClass + "|" + key;

            if (seen.contains(seenKey)) {
                continue;
            }

            seen.add(seenKey);

            if (activeClass.equals("NEUTRAL")) {

                NEUTRAL_SCORES.put(
                        key,
                        score
                );

            } else {

                Map<String, Double> classMap =
                        CLASS_SCORES.get(activeClass);

                if (classMap == null) {

                    classMap =
                            new HashMap<>();

                    CLASS_SCORES.put(
                            activeClass,
                            classMap
                    );
                }

                classMap.put(
                        key,
                        score
                );
            }

            parsed++;

            Log.d(
                    TAG,
                    "PARSED CARD: "
                            + canonical
                            + " = "
                            + score
                            + " ["
                            + activeClass
                            + "]"
            );
        }

        return parsed;
    }

    /*
     * ============================================================
     * CARD NAME SEARCH
     * ============================================================
     */

    private static String findCardNameInsideLine(
            String line
    ) {

        if (line == null ||
                line.isEmpty()) {

            return null;
        }

        String normalizedLine =
                normalize(line);

        String best =
                null;

        int bestLength = -1;

        for (Map.Entry<String, String> entry :
                CANONICAL_NAMES.entrySet()) {

            String normalizedCard =
                    entry.getKey();

            if (normalizedCard.isEmpty()) {
                continue;
            }

            boolean found =
                    normalizedLine.equals(normalizedCard)
                            ||
                    normalizedLine.startsWith(
                            normalizedCard + " "
                    )
                            ||
                    normalizedLine.endsWith(
                            " " + normalizedCard
                    )
                            ||
                    normalizedLine.contains(
                            " " + normalizedCard + " "
                    );

            if (found &&
                    normalizedCard.length() > bestLength) {

                best =
                        entry.getValue();

                bestLength =
                        normalizedCard.length();
            }
        }

        if (best != null) {
            return best;
        }

        for (Map.Entry<String, String> entry :
                ALIASES.entrySet()) {

            String alias =
                    entry.getKey();

            if (normalizedLine.equals(alias)
                    ||
                    normalizedLine.contains(
                            " " + alias + " "
                    )
                    ||
                    normalizedLine.startsWith(
                            alias + " "
                    )
                    ||
                    normalizedLine.endsWith(
                            " " + alias
                    )) {

                return entry.getValue();
            }
        }

        return null;
    }

    /*
     * ============================================================
     * SAME-LINE SCORE
     * ============================================================
     */

    private static Double findScoreOnSameLine(
            String line,
            String canonical
    ) {

        if (line == null ||
                canonical == null) {

            return null;
        }

        String normalizedLine =
                normalize(line);

        String normalizedCard =
                normalize(canonical);

        int position =
                normalizedLine.indexOf(
                        normalizedCard
                );

        if (position < 0) {
            return null;
        }

        String afterCard =
                normalizedLine.substring(
                        position
                                + normalizedCard.length()
                ).trim();

        if (afterCard.isEmpty()) {
            return null;
        }

        String[] parts =
                afterCard.split("\\s+");

        for (String part : parts) {

            Double score =
                    parseScore(part);

            if (score != null) {
                return score;
            }
        }

        return null;
    }

    /*
     * ============================================================
     * NEXT-LINE SCORE
     * ============================================================
     */

    private static Double findFollowingScore(
            String[] lines,
            int start
    ) {

        int max =
                Math.min(
                        lines.length,
                        start + 8
                );

        for (int i = start + 1;
             i < max;
             i++) {

            String value =
                    cleanLine(lines[i]);

            if (value.isEmpty()) {
                continue;
            }

            if (!detectClassHeader(value).isEmpty()) {
                return null;
            }

            if (isNeutralHeader(value)) {
                return null;
            }

            String possibleNext =
                    removeRankingPrefix(value);

            if (looksLikeCardLine(possibleNext)) {
                return null;
            }

            if (isTierOnlyLine(possibleNext)) {
                continue;
            }

            Double parsed =
                    parseScore(value);

            if (parsed != null) {
                return parsed;
            }

            Double inline =
                    findAnyScoreInLine(value);

            if (inline != null) {
                return inline;
            }
        }

        return null;
    }

    private static Double findAnyScoreInLine(
            String line
    ) {

        if (line == null ||
                line.isEmpty()) {

            return null;
        }

        String cleaned =
                line
                        .replace("↓", " ")
                        .replace("↑", " ")
                        .replace(":", " ");

        String[] parts =
                cleaned.split("\\s+");

        for (String part : parts) {

            Double score =
                    parseScore(part);

            if (score != null) {
                return score;
            }
        }

        return null;
    }

    private static Double parseScore(
            String text
    ) {

        if (text == null) {
            return null;
        }

        String value =
                text
                        .trim()
                        .replace("↓", "")
                        .replace("↑", "")
                        .replace(",", ".")
                        .trim();

        if (value.matches("\\d+\\.")) {
            return null;
        }

        if (!value.matches(
                "\\d+(?:\\.\\d+)?"
        )) {
            return null;
        }

        try {

            return Double.parseDouble(value);

        } catch (Exception e) {

            return null;
        }
    }

    /*
     * ============================================================
     * CANONICAL MATCHING
     * ============================================================
     */

    private static String findCanonicalCardName(
            String input
    ) {

        if (input == null ||
                input.isEmpty()) {

            return null;
        }

        String normalized =
                normalize(input);

        String exact =
                CANONICAL_NAMES.get(normalized);

        if (exact != null) {
            return exact;
        }

        String alias =
                ALIASES.get(normalized);

        if (alias != null) {
            return alias;
        }

        String withoutNew =
                normalized
                        .replace(" new", "")
                        .trim();

        exact =
                CANONICAL_NAMES.get(
                        withoutNew
                );

        if (exact != null) {
            return exact;
        }

        int bestDistance =
                Integer.MAX_VALUE;

        String best =
                null;

        for (Map.Entry<String, String> entry :
                CANONICAL_NAMES.entrySet()) {

            String candidate =
                    entry.getKey();

            if (candidate.length() < 3) {
                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            int allowed;

            if (normalized.length() <= 8) {
                allowed = 1;
            } else if (normalized.length() <= 15) {
                allowed = 2;
            } else {
                allowed = 3;
            }

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        entry.getValue();
            }
        }

        return best;
    }

    /*
     * ============================================================
     * HTML -> TEXT
     * ============================================================
     */

    private static String htmlToText(
            String html
    ) {

        if (html == null) {
            return "";
        }

        String text =
                html;

        text =
                text.replaceAll(
                        "(?is)<script[^>]*>.*?</script>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?is)<style[^>]*>.*?</style>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?i)</(div|p|li|ul|ol|h1|h2|h3|h4|h5|h6|tr|td|th|section|article)>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?s)<[^>]+>",
                        " "
                );

        text =
                decodeHtmlEntities(text);

        text =
                text.replace("\r", "");

        text =
                text.replaceAll(
                        "[\\t ]+",
                        " "
                );

        text =
                text.replaceAll(
                        "\\n[ \\t]+",
                        "\n"
                );

        text =
                text.replaceAll(
                        "\\n{3,}",
                        "\n\n"
                );

        return text;
    }

    private static String decodeHtmlEntities(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace("&nbsp;", " ")
                .replace("&#39;", "'")
                .replace("&#039;", "'")
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    /*
     * ============================================================
     * CLASS HEADER
     * ============================================================
     */

    private static String detectClassHeader(
            String line
    ) {

        if (line == null ||
                line.trim().isEmpty()) {

            return "";
        }

        String cleaned =
                line
                        .replaceAll(
                                "^\\s*[\\*•-]\\s*",
                                ""
                        )
                        .replaceAll(
                                "^\\s*#{1,6}\\s*",
                                ""
                        )
                        .trim();

        String normalized =
                normalize(cleaned);

        for (String valid :
                VALID_CLASSES) {

            String cls =
                    normalize(valid);

            if (normalized.equals(cls)) {
                return valid;
            }

            if (normalized.equals(
                    cls + " cards"
            )) {
                return valid;
            }

            String[] rarityPrefixes = {
                    "common ",
                    "rare ",
                    "epic ",
                    "legendary ",
                    "basic "
            };

            for (String prefix :
                    rarityPrefixes) {

                if (normalized.equals(
                        prefix + cls + " cards"
                )) {

                    return valid;
                }

                if (normalized.equals(
                        prefix + cls
                )) {

                    return valid;
                }
            }

            if (normalized.equals(
                    cls + " - cards"
            ) ||
                    normalized.equals(
                            cls + " : cards"
                    )) {

                return valid;
            }
        }

        return "";
    }

    private static boolean isNeutralHeader(
            String line
    ) {

        if (line == null) {
            return false;
        }

        String cleaned =
                line
                        .replaceAll(
                                "^\\s*[\\*•-]\\s*",
                                ""
                        )
                        .replaceAll(
                                "^\\s*#{1,6}\\s*",
                                ""
                        )
                        .trim();

        String normalized =
                normalize(cleaned);

        return normalized.equals("neutral")
                ||
                normalized.equals("neutral cards")
                ||
                normalized.equals("common neutral cards")
                ||
                normalized.equals("rare neutral cards")
                ||
                normalized.equals("epic neutral cards")
                ||
                normalized.equals("legendary neutral cards")
                ||
                normalized.equals("basic neutral cards");
    }

    /*
     * ============================================================
     * TEXT CLEANUP
     * ============================================================
     */

    private static String cleanLine(
            String line
    ) {

        if (line == null) {
            return "";
        }

        return line
                .replace(
                        "\u00A0",
                        " "
                )
                .replace(
                        "\u200B",
                        ""
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String removeRankingPrefix(
            String line
    ) {

        if (line == null) {
            return "";
        }

        String result =
                line.trim();

        result =
                result.replaceFirst(
                        "^\\s*\\d+\\s*[\\.\\)]\\s*",
                        ""
                );

        result =
                result.replaceFirst(
                        "^\\s*\\d+\\s+",
                        ""
                );

        result =
                result.replaceFirst(
                        "^\\s*[\\*•]\\s*",
                        ""
                );

        result =
                result.replaceFirst(
                        "^\\s*#{1,6}\\s*",
                        ""
                );

        return result.trim();
    }

    private static String removeTierNoise(
            String name
    ) {

        if (name == null) {
            return "";
        }

        return name
                .replace("↓", "")
                .replace("↑", "")
                .trim();
    }

    private static boolean isTierOnlyLine(
            String line
    ) {

        if (line == null) {
            return false;
        }

        String normalized =
                normalize(line);

        return normalized.equals("great")
                ||
                normalized.equals("good")
                ||
                normalized.equals("average")
                ||
                normalized.equals("poor")
                ||
                normalized.equals("bad")
                ||
                normalized.equals("premium")
                ||
                normalized.equals("solid")
                ||
                normalized.equals("weak")
                ||
                normalized.equals("terrible")
                ||
                normalized.equals("tier");
    }

    private static boolean looksLikeCardLine(
            String line
    ) {

        if (line == null ||
                line.isEmpty()) {

            return false;
        }

        String normalized =
                normalize(line);

        if (CANONICAL_NAMES.containsKey(
                normalized
        )) {

            return true;
        }

        if (ALIASES.containsKey(
                normalized
        )) {

            return true;
        }

        return findCardNameInsideLine(line) != null;
    }

    /*
     * ============================================================
     * OCR CORRECTION
     * ============================================================
     */

    public static String correctOcr(
            String detected
    ) {

        if (detected == null) {
            return "";
        }

        String input =
                detected.trim();

        if (input.isEmpty()) {
            return "";
        }

        String normalized =
                normalize(input);

        String alias =
                ALIASES.get(normalized);

        if (alias != null) {
            return alias;
        }

        String canonical =
                CANONICAL_NAMES.get(normalized);

        if (canonical != null) {
            return canonical;
        }

        CardInfo info =
                CARD_INFO.get(normalized);

        if (info != null) {
            return info.name;
        }

        String best =
                findBestOcrMatch(normalized);

        if (best != null) {
            return best;
        }

        return input;
    }

    private static String findBestOcrMatch(
            String normalized
    ) {

        if (normalized == null ||
                normalized.isEmpty() ||
                CANONICAL_NAMES.isEmpty()) {

            return null;
        }

        String best =
                null;

        int bestDistance =
                Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry :
                CANONICAL_NAMES.entrySet()) {

            String candidate =
                    entry.getKey();

            if (candidate.isEmpty()) {
                continue;
            }

            if (normalized.length() >= 6 &&
                    candidate.length() >= 6 &&
                    normalized.charAt(0)
                            != candidate.charAt(0)) {

                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            int allowed;

            if (normalized.length() <= 7) {
                allowed = 1;
            } else if (normalized.length() <= 14) {
                allowed = 2;
            } else {
                allowed = 3;
            }

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        entry.getValue();
            }
        }

        return best;
    }

    /*
     * ============================================================
     * BASE SCORE
     * ============================================================
     */

    private static double getBaseScore(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(cardName);

        String key =
                normalize(corrected);

        if (key.isEmpty()) {
            return UNKNOWN_CARD_SCORE;
        }

        double classScore =
                getClassSpecificScore(key);

        if (classScore >= 0) {

            return classScore;
        }

        Double neutral =
                NEUTRAL_SCORES.get(key);

        if (neutral != null) {

            return neutral;
        }

        Double fallback =
                FALLBACK_SCORES.get(key);

        if (fallback != null) {

            return fallback;
        }

        return UNKNOWN_CARD_SCORE;
    }

    /*
     * Public base-score API is retained.
     *
     * The complete dynamic score is available through
     * getCardScore(), getCardScoreValue() and analyzeCard().
     */

    public static double score(
            String cardName
    ) {

        double value =
                getBaseScore(cardName);

        if (value <= 0) {

            reason =
                    "Kortille ei löytynyt HearthArena-arvoa";

            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(cardName);

        if (getClassSpecificScore(corrected) >= 0) {

            reason =
                    "HearthArena " + currentClass;

        } else if (NEUTRAL_SCORES.containsKey(
                normalize(corrected)
        )) {

            reason =
                    "HearthArena Neutral";

        } else {

            reason =
                    "Paikallinen fallback";
        }

        return roundScore(value);
    }

    public static double getClassSpecificScore(
            String cardName
    ) {

        if (cardName == null) {
            return -1;
        }

        String key =
                normalize(
                        correctOcr(cardName)
                );

        if (key.isEmpty()) {
            return -1;
        }

        String cls =
                normalizeClass(currentClass);

        if (cls.isEmpty()) {
            return -1;
        }

        Map<String, Double> scores =
                CLASS_SCORES.get(cls);

        if (scores == null) {
            return -1;
        }

        Double value =
                scores.get(key);

        if (value == null) {
            return -1;
        }

        return value;
    }

    /*
     * This now returns the SAME dynamic final value that
     * recommendation uses.
     */

    public static String getCardScore(
            String cardName
    ) {

        DraftAnalysis analysis =
                analyzeCard(cardName);

        if (analysis == null ||
                analysis.finalScore <= 0) {

            return "?";
        }

        return String.format(
                Locale.US,
                "%.2f",
                analysis.finalScore
        );
    }

    public static double getCardScoreValue(
            String cardName
    ) {

        DraftAnalysis analysis =
                analyzeCard(cardName);

        if (analysis == null) {
            return 0.0;
        }

        return analysis.finalScore;
    }

    /*
     * ============================================================
     * NEW DRAFT ANALYSIS
     * ============================================================
     */

    public static synchronized DraftAnalysis analyzeCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return new DraftAnalysis(
                    "",
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0,
                    "Korttia ei tunnistettu"
            );
        }

        String corrected =
                correctOcr(cardName);

        CardData card =
                getCardData(corrected);

        double base =
                getBaseScore(corrected);

        if (base <= 0) {

            DraftAnalysis unknown =
                    new DraftAnalysis(
                            corrected,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            getManaCost(card),
                            "HearthArena-arvo puuttuu"
                    );

            LAST_ANALYSES.put(
                    normalize(corrected),
                    unknown
            );

            return unknown;
        }

        double curve =
                calculateCurveAdjustment(card);

        double type =
                calculateTypeAdjustment(card);

        double synergy =
                calculateSynergyAdjustment(card);

        double deckFit =
                calculateDeckFitAdjustment(card);

        double removalTempo =
                calculateRemovalTempoAdjustment(card);

        double duplicate =
                calculateDuplicateAdjustment(corrected);

        double finalScore =
                base
                        + curve
                        + type
                        + synergy
                        + deckFit
                        + removalTempo
                        + duplicate;

        finalScore =
                roundScore(
                        finalScore
                );

        String analysisReason =
                buildAnalysisReason(
                        card,
                        base,
                        curve,
                        type,
                        synergy,
                        deckFit,
                        removalTempo,
                        duplicate
                );

        DraftAnalysis result =
                new DraftAnalysis(
                        corrected,
                        base,
                        curve,
                        type,
                        synergy,
                        deckFit,
                        removalTempo,
                        duplicate,
                        finalScore,
                        getManaCost(card),
                        analysisReason
                );

        LAST_ANALYSES.put(
                normalize(corrected),
                result
        );

        return result;
    }

    /*
     * ============================================================
     * FULL THREE-CARD ANALYSIS
     * ============================================================
     */

    public static synchronized DraftAnalysis[] analyzeDraft(
            String card1,
            String card2,
            String card3
    ) {

        DraftAnalysis a1 =
                analyzeCard(card1);

        DraftAnalysis a2 =
                analyzeCard(card2);

        DraftAnalysis a3 =
                analyzeCard(card3);

        DraftAnalysis[] result = {
                a1,
                a2,
                a3
        };

        double best =
                -Double.MAX_VALUE;

        String recommendation =
                "";

        for (DraftAnalysis analysis :
                result) {

            if (analysis == null) {
                continue;
            }

            if (analysis.finalScore > best &&
                    analysis.finalScore > 0) {

                best =
                        analysis.finalScore;

                recommendation =
                        analysis.cardName;
            }
        }

        if (recommendation.isEmpty()) {

            lastRecommendation =
                    "";

            lastRecommendationScore =
                    0.0;

            lastRecommendationGap =
                    0.0;

            lastRecommendationReason =
                    "Kortteja ei tunnistettu";

            return result;
        }

        double secondBest =
                -Double.MAX_VALUE;

        for (DraftAnalysis analysis :
                result) {

            if (analysis == null) {
                continue;
            }

            if (analysis.cardName.equals(
                    recommendation
            )) {
                continue;
            }

            if (analysis.finalScore > secondBest) {

                secondBest =
                        analysis.finalScore;
            }
        }

        if (secondBest ==
                -Double.MAX_VALUE) {

            secondBest = 0.0;
        }

        lastRecommendation =
                recommendation;

        lastRecommendationScore =
                roundScore(best);

        lastRecommendationGap =
                roundScore(
                        Math.max(
                                0.0,
                                best - secondBest
                        )
                );

        lastRecommendationReason =
                buildRecommendationReason(
                        recommendation,
                        best,
                        secondBest
                );

        reason =
                lastRecommendationReason;

        return result;
    }

    /*
     * ============================================================
     * RECOMMENDATION
     * ============================================================
     */

    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        DraftAnalysis[] analyses =
                analyzeDraft(
                        card1,
                        card2,
                        card3
                );

        if (analyses == null ||
                analyses.length == 0) {

            return "EI TUNNISTETTAVA";
        }

        String recommendation =
                "";

        double best =
                -Double.MAX_VALUE;

        for (DraftAnalysis analysis :
                analyses) {

            if (analysis == null) {
                continue;
            }

            if (analysis.finalScore > best &&
                    analysis.finalScore > 0) {

                best =
                        analysis.finalScore;

                recommendation =
                        analysis.cardName;
            }
        }

        if (recommendation.isEmpty()) {

            reason =
                    "Yhdellekään kortille ei löytynyt arvoa";

            return "EI TUNNISTETTAVAA";
        }

        return recommendation;
    }

    public static String getRecommendationReason() {

        return lastRecommendationReason;
    }

    public static double getRecommendationScore() {

        return lastRecommendationScore;
    }

    public static double getRecommendationGap() {

        return lastRecommendationGap;
    }

    public static String getCardReason(
            String cardName
    ) {

        DraftAnalysis analysis =
                analyzeCard(cardName);

        if (analysis == null) {
            return "";
        }

        return analysis.reason;
    }

    public static double getAnalyzedScore(
            String cardName
    ) {

        DraftAnalysis analysis =
                analyzeCard(cardName);

        if (analysis == null) {
            return 0.0;
        }

        return analysis.finalScore;
    }

    /*
     * ============================================================
     * MANA CURVE
     * ============================================================
     */

    private static double calculateCurveAdjustment(
            CardData candidate
    ) {

        if (candidate == null) {
            return 0.0;
        }

        int cost =
                getManaCost(candidate);

        if (cost < 0) {
            return 0.0;
        }

        int[] curve =
                getCurrentManaCurve();

        int total =
                getPickedCardCountTotal();

        /*
         * First picks should primarily be raw card quality.
         */
        if (total < 3) {
            return 0.0;
        }

        int sameCost =
                curve[
                        Math.min(
                                cost,
                                MAX_MANA_COST
                        )
                ];

        int desired =
                desiredCardsAtCost(
                        cost,
                        total
                );

        if (sameCost < desired) {

            double shortage =
                    desired - sameCost;

            return roundScore(
                    clamp(
                            shortage * 0.12,
                            0.0,
                            CURVE_MAX_BONUS
                    )
            );
        }

        if (sameCost > desired + 2) {

            double overload =
                    sameCost - desired - 2;

            return roundScore(
                    -clamp(
                            overload * 0.10,
                            0.0,
                            CURVE_MAX_BONUS
                    )
            );
        }

        return 0.0;
    }

    private static int desiredCardsAtCost(
            int cost,
            int total
    ) {

        if (cost <= 1) {
            return total >= 15 ? 2 : 1;
        }

        if (cost == 2) {
            return total >= 12 ? 4 : 2;
        }

        if (cost == 3) {
            return total >= 12 ? 4 : 2;
        }

        if (cost == 4) {
            return total >= 15 ? 3 : 2;
        }

        if (cost == 5) {
            return total >= 15 ? 2 : 1;
        }

        if (cost == 6) {
            return total >= 20 ? 2 : 1;
        }

        if (cost >= 7) {
            return total >= 20 ? 1 : 0;
        }

        return 1;
    }

    private static int[] getCurrentManaCurve() {

        int[] curve =
                new int[MAX_MANA_COST + 1];

        for (Map.Entry<String, Integer> entry :
                PICKED_CARDS.entrySet()) {

            Integer count =
                    entry.getValue();

            if (count == null ||
                    count <= 0) {

                continue;
            }

            CardData card =
                    CARDS.get(entry.getKey());

            if (card == null) {
                continue;
            }

            int cost =
                    getManaCost(card);

            if (cost < 0) {
                continue;
            }

            int bucket =
                    Math.min(
                            cost,
                            MAX_MANA_COST
                    );

            curve[bucket] += count;
        }

        return curve;
    }

    private static int getPickedCardCountTotal() {

        int total = 0;

        for (Integer value :
                PICKED_CARDS.values()) {

            if (value != null &&
                    value > 0) {

                total += value;
            }
        }

        return total;
    }

    /*
     * ============================================================
     * TYPE BALANCE
     * ============================================================
     */

    private static double calculateTypeAdjustment(
            CardData card
    ) {

        if (card == null) {
            return 0.0;
        }

        int total =
                getPickedCardCountTotal();

        if (total < 4) {
            return 0.0;
        }

        String type =
                normalizeType(card.type);

        int minions =
                countPickedType("MINION");

        int spells =
                countPickedType("SPELL");

        int weapons =
                countPickedType("WEAPON");

        if (type.equals("MINION")) {

            if (minions < 8 &&
                    total >= 10) {

                return TYPE_MAX_BONUS;
            }

            if (minions > 18 &&
                    total >= 22) {

                return -TYPE_MAX_BONUS;
            }
        }

        if (type.equals("SPELL")) {

            if (spells > 10 &&
                    total >= 20) {

                return -TYPE_MAX_BONUS;
            }
        }

        if (type.equals("WEAPON")) {

            if (weapons >= 3) {

                return -TYPE_MAX_BONUS;
            }
        }

        return 0.0;
    }

    private static int countPickedType(
            String wantedType
    ) {

        int total = 0;

        for (Map.Entry<String, Integer> entry :
                PICKED_CARDS.entrySet()) {

            CardData card =
                    CARDS.get(entry.getKey());

            if (card == null) {
                continue;
            }

            if (normalizeType(card.type)
                    .equals(wantedType)) {

                Integer count =
                        entry.getValue();

                if (count != null) {
                    total += count;
                }
            }
        }

        return total;
    }

    private static String normalizeType(
            String type
    ) {

        if (type == null) {
            return "";
        }

        return type
                .trim()
                .toUpperCase(Locale.US)
                .replace("-", "_")
                .replace(" ", "_");
    }

    /*
     * ============================================================
     * DECK FIT
     * ============================================================
     *
     * This measures whether the candidate fills a structural
     * need in the current Arena deck.
     *
     * It deliberately does NOT replace card quality.
     */

    private static double calculateDeckFitAdjustment(
            CardData candidate
    ) {

        if (candidate == null) {
            return 0.0;
        }

        int total =
                getPickedCardCountTotal();

        if (total < 4) {
            return 0.0;
        }

        double value =
                0.0;

        String type =
                normalizeType(candidate.type);

        int minions =
                countPickedType("MINION");

        int spells =
                countPickedType("SPELL");

        int weapons =
                countPickedType("WEAPON");

        /*
         * Arena decks generally need a healthy number of
         * playable minions.
         */
        if (type.equals("MINION")) {

            if (total >= 8 &&
                    minions < 7) {

                value += 0.20;
            }

            if (total >= 15 &&
                    minions < 10) {

                value += 0.12;
            }
        }

        /*
         * If the deck is already spell-heavy, another spell
         * gets less structural value unless it provides
         * removal / tempo.
         */
        if (type.equals("SPELL")) {

            if (total >= 12 &&
                    spells >= 7) {

                value -= 0.12;
            }

            if (total >= 20 &&
                    spells >= 10) {

                value -= 0.15;
            }
        }

        /*
         * Weapons have diminishing structural value.
         */
        if (type.equals("WEAPON")) {

            if (weapons >= 2) {
                value -= 0.10;
            }

            if (weapons >= 3) {
                value -= 0.15;
            }
        }

        /*
         * Very low-cost cards are useful when the deck lacks
         * early plays.
         */
        int cost =
                getManaCost(candidate);

        int early =
                getCurveCount(1)
                        + getCurveCount(2);

        if (cost <= 2 &&
                total >= 8 &&
                early < 4) {

            value += 0.15;
        }

        /*
         * Mid-game cards are useful when the deck has too many
         * cheap cards.
         */
        int mid =
                getCurveCount(3)
                        + getCurveCount(4)
                        + getCurveCount(5);

        if (cost >= 3 &&
                cost <= 5 &&
                total >= 15 &&
                mid < 7) {

            value += 0.12;
        }

        /*
         * High-cost cards should not be accumulated blindly.
         */
        if (cost >= 7 &&
                total >= 15) {

            int expensive =
                    getCurveCount(7)
                            + getCurveCount(8)
                            + getCurveCount(9)
                            + getCurveCount(10);

            if (expensive >= 3) {
                value -= 0.18;
            }
        }

        return roundScore(
                clamp(
                        value,
                        -DECK_FIT_MAX_BONUS,
                        DECK_FIT_MAX_BONUS
                )
        );
    }

    private static int getCurveCount(
            int mana
    ) {

        int[] curve =
                getCurrentManaCurve();

        if (mana < 0 ||
                mana >= curve.length) {

            return 0;
        }

        return curve[mana];
    }

    /*
     * ============================================================
     * SYNERGY SYSTEM
     * ============================================================
     */

    private static double calculateSynergyAdjustment(
            CardData candidate
    ) {

        if (candidate == null) {
            return 0.0;
        }

        int total =
                getPickedCardCountTotal();

        if (total < 2) {
            return 0.0;
        }

        double value =
                0.0;

        String candidateText =
                getFullSynergyText(candidate);

        for (String pickedName :
                PICKED_CARDS.keySet()) {

            CardData picked =
                    CARDS.get(pickedName);

            if (picked == null) {
                continue;
            }

            int count =
                    getPickedCount(
                            picked.name
                    );

            if (count <= 0) {
                continue;
            }

            String pickedText =
                    getFullSynergyText(picked);

            double pair =
                    calculatePairSynergy(
                            candidate,
                            candidateText,
                            picked,
                            pickedText
                    );

            value +=
                    pair * Math.min(
                            count,
                            2
                    );
        }

        value +=
                calculateTextSynergy(
                        candidateText
                );

        /*
         * Shared tribe is a stronger signal than simply
         * mentioning a generic keyword in text.
         */
        value +=
                calculateRaceSynergy(
                        candidate
                );

        return roundScore(
                clamp(
                        value,
                        -SYNERGY_MAX_BONUS,
                        SYNERGY_MAX_BONUS
                )
        );
    }

    private static double calculatePairSynergy(
            CardData candidate,
            String candidateText,
            CardData picked,
            String pickedText
    ) {

        if (candidate == null ||
                picked == null) {

            return 0.0;
        }

        double value =
                0.0;

        String candidateType =
                normalizeType(candidate.type);

        String pickedType =
                normalizeType(picked.type);

        if (candidateType.equals("MINION") &&
                pickedType.equals("MINION")) {

            if (sharesImportantKeyword(
                    candidateText,
                    pickedText
            )) {

                value += 0.12;
            }
        }

        if (candidateType.equals("SPELL") &&
                pickedType.equals("SPELL")) {

            if (sharesImportantKeyword(
                    candidateText,
                    pickedText
            )) {

                value += 0.10;
            }
        }

        if (containsAny(
                candidateText,
                "battlecry",
                "deathrattle",
                "discover",
                "secret",
                "spell",
                "weapon",
                "minion",
                "dragon",
                "beast",
                "mech",
                "murloc",
                "demon",
                "elemental",
                "undead",
                "naga",
                "pirate",
                "totem"
        )) {

            if (sharesImportantKeyword(
                    candidateText,
                    pickedText
            )) {

                value += 0.10;
            }
        }

        /*
         * Direct spell-related synergy.
         */
        if (candidateText.contains("spell") &&
                pickedType.equals("SPELL")) {

            value += 0.08;
        }

        if (pickedText.contains("spell") &&
                candidateType.equals("SPELL")) {

            value += 0.08;
        }

        return value;
    }

    private static boolean sharesImportantKeyword(
            String a,
            String b
    ) {

        if (a == null ||
                b == null ||
                a.isEmpty() ||
                b.isEmpty()) {

            return false;
        }

        String[] keywords = {

                "battlecry",
                "deathrattle",
                "discover",
                "secret",

                "spell",
                "weapon",
                "minion",

                "dragon",
                "beast",
                "mech",
                "murloc",
                "demon",
                "elemental",
                "undead",
                "naga",
                "pirate",
                "totem",

                "freeze",
                "damage",
                "healing",
                "heal",
                "buff",
                "draw",

                "divine shield",
                "taunt",
                "rush",
                "charge",
                "lifesteal",
                "silence"
        };

        for (String keyword :
                keywords) {

            if (a.contains(keyword) &&
                    b.contains(keyword)) {

                return true;
            }
        }

        return false;
    }

    private static double calculateTextSynergy(
            String text
    ) {

        if (text == null ||
                text.isEmpty()) {

            return 0.0;
        }

        double value =
                0.0;

        String[] concepts = {

                "dragon",
                "beast",
                "mech",
                "murloc",
                "demon",
                "elemental",
                "undead",
                "naga",
                "pirate",
                "totem",

                "spell",
                "weapon",

                "deathrattle",
                "battlecry",
                "discover"
        };

        for (String concept :
                concepts) {

            if (text.contains(concept)) {

                if (hasPickedText(
                        concept
                )) {

                    value += 0.10;
                }
            }
        }

        return value;
    }

    private static double calculateRaceSynergy(
            CardData candidate
    ) {

        if (candidate == null) {
            return 0.0;
        }

        String race =
                normalizeRace(
                        candidate.race
                );

        if (race.isEmpty()) {
            return 0.0;
        }

        int matching =
                0;

        for (String pickedName :
                PICKED_CARDS.keySet()) {

            CardData picked =
                    CARDS.get(pickedName);

            if (picked == null) {
                continue;
            }

            String pickedRace =
                    normalizeRace(
                            picked.race
                    );

            if (!pickedRace.isEmpty() &&
                    pickedRace.equals(race)) {

                matching +=
                        Math.min(
                                2,
                                getPickedCount(
                                        picked.name
                                )
                        );
            }
        }

        if (matching <= 0) {
            return 0.0;
        }

        return clamp(
                matching * 0.06,
                0.0,
                0.18
        );
    }

    private static boolean hasPickedText(
            String keyword
    ) {

        for (String pickedName :
                PICKED_CARDS.keySet()) {

            CardData card =
                    CARDS.get(pickedName);

            if (card == null) {
                continue;
            }

            String text =
                    getFullSynergyText(card);

            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private static String getFullSynergyText(
            CardData card
    ) {

        if (card == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        if (!card.text.isEmpty()) {
            builder.append(card.text);
        }

        if (!card.race.isEmpty()) {

            builder.append(" ");
            builder.append(card.race);
        }

        if (!card.mechanics.isEmpty()) {

            builder.append(" ");
            builder.append(card.mechanics);
        }

        if (!card.spellSchool.isEmpty()) {

            builder.append(" ");
            builder.append(card.spellSchool);
        }

        return normalizeForSynergy(
                builder.toString()
        );
    }

    private static String normalizeForSynergy(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(Locale.US)
                .replaceAll(
                        "<[^>]+>",
                        " "
                )
                .replaceAll(
                        "[^a-z0-9 ]",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String normalizeRace(
            String race
    ) {

        if (race == null) {
            return "";
        }

        return race
                .toLowerCase(Locale.US)
                .replace(
                        "_",
                        " "
                )
                .trim();
    }

    /*
     * ============================================================
     * REMOVAL / TEMPO
     * ============================================================
     *
     * Arena decks benefit from interaction.
     *
     * This is intentionally conservative and based on actual
     * card text plus simple stat/curve information.
     */

    private static double calculateRemovalTempoAdjustment(
            CardData card
    ) {

        if (card == null) {
            return 0.0;
        }

        int total =
                getPickedCardCountTotal();

        if (total < 3) {
            return 0.0;
        }

        String text =
                normalizeForSynergy(
                        card.text
                );

        double value =
                0.0;

        int removal =
                0;

        int tempo =
                0;

        /*
         * Direct removal / damage.
         */
        if (containsAny(
                text,
                "destroy",
                "deal damage",
                "destroy a",
                "destroy an",
                "kill",
                "silence",
                "transform",
                "return it to its owner"
        )) {

            removal++;
        }

        /*
         * Board control.
         */
        if (containsAny(
                text,
                "all enemy",
                "enemy minion",
                "enemy minions",
                "random enemy",
                "freeze",
                "aoe"
        )) {

            removal++;
        }

        /*
         * Hard control.
         */
        if (containsAny(
                text,
                "polymorph",
                "hex",
                "silence",
                "transform"
        )) {

            removal++;
        }

        /*
         * Tempo keywords.
         */
        if (containsAny(
                text,
                "rush",
                "charge",
                "taunt",
                "freeze"
        )) {

            tempo++;
        }

        /*
         * Immediate board impact.
         */
        if (containsAny(
                text,
                "summon",
                "gain",
                "give",
                "buff",
                "draw"
        )) {

            tempo++;
        }

        /*
         * Weapons provide board-control tempo.
         */
        if (normalizeType(card.type)
                .equals("WEAPON")) {

            tempo++;
        }

        if (removal > 0) {

            value +=
                    0.18
                            * Math.min(
                            removal,
                            2
                    );
        }

        if (tempo > 0) {

            value +=
                    0.10
                            * Math.min(
                            tempo,
                            2
                    );
        }

        /*
         * Actual stats can provide modest tempo value for
         * early/mid-game minions.
         */
        if (normalizeType(card.type)
                .equals("MINION")) {

            int cost =
                    getManaCost(card);

            int stats =
                    card.attack +
                            card.health;

            if (cost >= 1 &&
                    cost <= 4 &&
                    stats >= cost * 3) {

                value += 0.08;
            }

            if (cost <= 3 &&
                    card.attack >= 3) {

                value += 0.05;
            }
        }

        /*
         * If the existing deck already has a lot of removal,
         * the next removal card gets slightly less structural
         * value.
         */
        int existingRemoval =
                countPickedRemovalCards();

        if (removal > 0 &&
                existingRemoval >= 5) {

            value -= 0.10;
        }

        /*
         * A deck with very little interaction benefits more
         * from a removal card.
         */
        if (removal > 0 &&
                existingRemoval <= 1 &&
                total >= 8) {

            value += 0.10;
        }

        return roundScore(
                clamp(
                        value,
                        0.0,
                        REMOVAL_TEMPO_MAX_BONUS
                )
        );
    }

    private static int countPickedRemovalCards() {

        int total = 0;

        for (String pickedName :
                PICKED_CARDS.keySet()) {

            CardData card =
                    CARDS.get(pickedName);

            if (card == null) {
                continue;
            }

            String text =
                    normalizeForSynergy(
                            card.text
                    );

            boolean isRemoval =
                    containsAny(
                            text,
                            "destroy",
                            "deal damage",
                            "kill",
                            "silence",
                            "transform",
                            "enemy minion",
                            "enemy minions",
                            "freeze"
                    );

            if (isRemoval) {

                total +=
                        Math.min(
                                2,
                                getPickedCount(
                                        pickedName
                                )
                        );
            }
        }

        return total;
    }

    /*
     * ============================================================
     * DUPLICATE ADJUSTMENT
     * ============================================================
     */

    private static double calculateDuplicateAdjustment(
            String cardName
    ) {

        int count =
                getPickedCount(cardName);

        if (count <= 0) {
            return 0.0;
        }

        if (count == 1) {
            return -DUPLICATE_PENALTY;
        }

        return -DUPLICATE_PENALTY * 2.0;
    }

    /*
     * ============================================================
     * REASON BUILDER
     * ============================================================
     */

    private static String buildAnalysisReason(
            CardData card,
            double base,
            double curve,
            double type,
            double synergy,
            double deckFit,
            double removalTempo,
            double duplicate
    ) {

        List<String> reasons =
                new ArrayList<>();

        reasons.add(
                "HearthArena "
                        + formatScore(base)
        );

        if (curve > 0.04) {

            reasons.add(
                    "Mana curve +"
                            + formatScore(curve)
            );

        } else if (curve < -0.04) {

            reasons.add(
                    "Mana curve "
                            + formatScore(curve)
            );
        }

        if (type > 0.04) {

            reasons.add(
                    "Type +"
                            + formatScore(type)
            );

        } else if (type < -0.04) {

            reasons.add(
                    "Type "
                            + formatScore(type)
            );
        }

        if (synergy > 0.04) {

            reasons.add(
                    "Synergies +"
                            + formatScore(synergy)
            );

        } else if (synergy < -0.04) {

            reasons.add(
                    "Synergies "
                            + formatScore(synergy)
            );
        }

        if (deckFit > 0.04) {

            reasons.add(
                    "Deck fit +"
                            + formatScore(deckFit)
            );

        } else if (deckFit < -0.04) {

            reasons.add(
                    "Deck fit "
                            + formatScore(deckFit)
            );
        }

        if (removalTempo > 0.04) {

            reasons.add(
                    "Removal/tempo +"
                            + formatScore(removalTempo)
            );
        }

        if (duplicate < -0.04) {

            reasons.add(
                    "Duplicate "
                            + formatScore(duplicate)
            );
        }

        if (card != null) {

            String typeText =
                    getReadableType(card.type);

            if (!typeText.isEmpty()) {

                reasons.add(
                        typeText
                                + " "
                                + getManaCost(card)
                                + " mana"
                );
            }
        }

        return joinReasons(reasons);
    }

    private static String buildRecommendationReason(
            String recommendation,
            double best,
            double secondBest
    ) {

        DraftAnalysis analysis =
                LAST_ANALYSES.get(
                        normalize(recommendation)
                );

        if (analysis == null) {

            return "Paras analysoitu kortti: "
                    + recommendation;
        }

        double gap =
                Math.max(
                        0.0,
                        best - secondBest
                );

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "Arvo "
        );

        builder.append(
                formatScore(best)
        );

        builder.append(
                " — "
        );

        builder.append(
                recommendation
        );

        if (gap > 0.0) {

            builder.append(
                    " • Ero seuraavaan: +"
            );

            builder.append(
                    formatScore(gap)
            );
        }

        builder.append(
                " • "
        );

        builder.append(
                analysis.reason
        );

        return builder.toString();
    }

    private static String joinReasons(
            List<String> reasons
    ) {

        if (reasons == null ||
                reasons.isEmpty()) {

            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < reasons.size();
             i++) {

            if (i > 0) {
                builder.append(" • ");
            }

            builder.append(
                    reasons.get(i)
            );
        }

        return builder.toString();
    }

    private static String formatScore(
            double value
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    /*
     * ============================================================
     * PICKED CARDS
     * ============================================================
     */

    public static synchronized void recordPickedCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return;
        }

        String corrected =
                correctOcr(cardName);

        String key =
                normalize(corrected);

        if (key.isEmpty()) {
            return;
        }

        Integer count =
                PICKED_CARDS.get(key);

        if (count == null) {
            count = 0;
        }

        PICKED_CARDS.put(
                key,
                count + 1
        );

        LAST_ANALYSES.clear();
    }

    public static synchronized void clearPickedCards() {

        PICKED_CARDS.clear();
        LAST_ANALYSES.clear();

        lastRecommendation =
                "";

        lastRecommendationScore =
                0.0;

        lastRecommendationGap =
                0.0;

        lastRecommendationReason =
                "";
    }

    public static synchronized int getPickedCount(
            String cardName
    ) {

        if (cardName == null) {
            return 0;
        }

        String key =
                normalize(
                        correctOcr(cardName)
                );

        Integer count =
                PICKED_CARDS.get(key);

        return count == null
                ? 0
                : count;
    }

    /*
     * ============================================================
     * CLASS DETECTION
     * ============================================================
     */

    public static synchronized void detectClassFromCards(
            String card1,
            String card2,
            String card3
    ) {

        if (!currentClass.isEmpty()) {
            return;
        }

        String detected =
                getFirstClassFromCards(
                        card1,
                        card2,
                        card3
                );

        if (detected.isEmpty()) {
            return;
        }

        String second =
                getSecondDifferentClass(
                        detected,
                        card1,
                        card2,
                        card3
                );

        if (!second.isEmpty()) {

            candidateClass = "";
            candidateClassCount = 0;

            return;
        }

        if (detected.equals(candidateClass)) {

            candidateClassCount++;

        } else {

            candidateClass =
                    detected;

            candidateClassCount =
                    1;
        }

        if (candidateClassCount >=
                CLASS_CONFIRMATIONS) {

            currentClass =
                    candidateClass;

            status =
                    "Luokka tunnistettu: "
                            + currentClass;

            Log.d(
                    TAG,
                    "Class locked: "
                            + currentClass
            );
        }
    }

    private static String getFirstClassFromCards(
            String card1,
            String card2,
            String card3
    ) {

        String[] cards = {
                card1,
                card2,
                card3
        };

        for (String card : cards) {

            String cls =
                    getClassForCard(card);

            if (!cls.isEmpty()) {
                return cls;
            }
        }

        return "";
    }

    private static String getSecondDifferentClass(
            String first,
            String card1,
            String card2,
            String card3
    ) {

        String[] cards = {
                card1,
                card2,
                card3
        };

        for (String card : cards) {

            String cls =
                    getClassForCard(card);

            if (!cls.isEmpty() &&
                    !cls.equals(first)) {

                return cls;
            }
        }

        return "";
    }

    public static String getClassForCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return "";
        }

        String corrected =
                correctOcr(cardName);

        String key =
                normalize(corrected);

        CardInfo info =
                CARD_INFO.get(key);

        if (info == null) {
            return "";
        }

        String cls =
                normalizeClass(info.className);

        if (cls.equals("NEUTRAL") ||
                cls.equals("INVALID") ||
                cls.isEmpty()) {

            return "";
        }

        return cls;
    }

    /*
     * ============================================================
     * CLASS API
     * ============================================================
     */

    public static String getCurrentClass() {

        if (currentClass == null ||
                currentClass.isEmpty()) {

            return "EI TUNNISTETTU";
        }

        return currentClass;
    }

    public static String getCurrentClassRaw() {
        return currentClass;
    }

    public static boolean isClassDetected() {

        return currentClass != null &&
                !currentClass.isEmpty();
    }

    public static synchronized void resetClassDetection() {

        currentClass = "";
        candidateClass = "";
        candidateClassCount = 0;

        status =
                "Luokka nollattu";
    }

    /*
     * ============================================================
     * MANUAL SCORE API
     * ============================================================
     */

    public static synchronized void setClassScore(
            String cardName,
            String className,
            double score
    ) {

        if (cardName == null ||
                className == null) {

            return;
        }

        String cardKey =
                normalize(
                        correctOcr(cardName)
                );

        String cls =
                normalizeClass(className);

        if (cardKey.isEmpty() ||
                cls.isEmpty()) {

            return;
        }

        Map<String, Double> scores =
                CLASS_SCORES.get(cls);

        if (scores == null) {

            scores =
                    new HashMap<>();

            CLASS_SCORES.put(
                    cls,
                    scores
            );
        }

        scores.put(
                cardKey,
                score
        );

        LAST_ANALYSES.clear();
    }

    public static synchronized void clearClassScores() {

        for (Map<String, Double> map :
                CLASS_SCORES.values()) {

            map.clear();
        }

        NEUTRAL_SCORES.clear();

        hearthArenaLoaded = false;

        LAST_ANALYSES.clear();
    }

    public static int getClassScoreCount() {

        int total = 0;

        for (Map<String, Double> map :
                CLASS_SCORES.values()) {

            total += map.size();
        }

        total +=
                NEUTRAL_SCORES.size();

        return total;
    }

    /*
     * ============================================================
     * CARD DATA API
     * ============================================================
     */

    public static CardData getCardData(
            String cardName
    ) {

        if (cardName == null) {
            return null;
        }

        String key =
                normalize(
                        correctOcr(cardName)
                );

        return CARDS.get(key);
    }

    public static int getCardManaCost(
            String cardName
    ) {

        CardData card =
                getCardData(cardName);

        if (card == null) {
            return -1;
        }

        return getManaCost(card);
    }

    public static String getCardType(
            String cardName
    ) {

        CardData card =
                getCardData(cardName);

        if (card == null) {
            return "";
        }

        return getReadableType(card.type);
    }

    /*
     * ============================================================
     * STATUS
     * ============================================================
     */

    public static String getStatus() {
        return status;
    }

    public static String getReason() {
        return reason;
    }

    public static boolean isOnlineLoaded() {
        return onlineLoaded;
    }

    public static boolean isHearthArenaLoaded() {
        return hearthArenaLoaded;
    }

    public static boolean isHearthArenaLoading() {
        return hearthArenaLoading;
    }

    /*
     * ============================================================
     * CARD HELPERS
     * ============================================================
     */

    private static int getManaCost(
            CardData card
    ) {

        if (card == null) {
            return -1;
        }

        return Math.max(
                0,
                card.cost
        );
    }

    private static String getReadableType(
            String type
    ) {

        String normalized =
                normalizeType(type);

        if (normalized.equals("MINION")) {
            return "Minion";
        }

        if (normalized.equals("SPELL")) {
            return "Spell";
        }

        if (normalized.equals("WEAPON")) {
            return "Weapon";
        }

        if (normalized.equals("LOCATION")) {
            return "Location";
        }

        if (normalized.equals("HERO")) {
            return "Hero";
        }

        return type == null
                ? ""
                : type;
    }

    /*
     * ============================================================
     * NORMALIZATION
     * ============================================================
     */

    private static String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String result =
                value
                        .toLowerCase(Locale.US)
                        .trim();

        result =
                result.replaceAll(
                        "[^a-z0-9]+",
                        " "
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                )
                .trim();

        return result;
    }

    private static String normalizeClass(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String result =
                value
                        .trim()
                        .toUpperCase(Locale.US);

        result =
                result.replace("_", " ");

        result =
                result.replace("-", " ");

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        if (result.equals("DEATHKNIGHT")) {
            return "DEATH KNIGHT";
        }

        if (result.equals("DEMONHUNTER")) {
            return "DEMON HUNTER";
        }

        if (result.equals("DEATH KNIGHT")) {
            return "DEATH KNIGHT";
        }

        if (result.equals("DEMON HUNTER")) {
            return "DEMON HUNTER";
        }

        for (String valid :
                VALID_CLASSES) {

            if (result.equals(valid)) {
                return valid;
            }
        }

        if (result.equals("NEUTRAL")) {
            return "NEUTRAL";
        }

        return result;
    }

    /*
     * ============================================================
     * UTILITY
     * ============================================================
     */

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private static double roundScore(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    private static boolean containsAny(
            String text,
            String... values
    ) {

        if (text == null ||
                text.isEmpty() ||
                values == null) {

            return false;
        }

        for (String value :
                values) {

            if (value != null &&
                    text.contains(
                            value
                    )) {

                return true;
            }
        }

        return false;
    }

    private static String readStream(
            InputStream input
    ) throws Exception {

        StringBuilder builder =
                new StringBuilder();

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                input,
                                StandardCharsets.UTF_8
                        )
                );

        String line;

        while ((line =
                reader.readLine()) != null) {

            builder
                    .append(line)
                    .append('\n');
        }

        reader.close();

        return builder.toString();
    }

    /*
     * ============================================================
     * LEVENSHTEIN
     * ============================================================
     */

    private static int levenshtein(
            String a,
            String b
    ) {

        if (a == null) {

            return b == null
                    ? 0
                    : b.length();
        }

        if (b == null) {
            return a.length();
        }

        int[] previous =
                new int[b.length() + 1];

        int[] current =
                new int[b.length() + 1];

        for (int j = 0;
             j <= b.length();
             j++) {

            previous[j] = j;
        }

        for (int i = 1;
             i <= a.length();
             i++) {

            current[0] = i;

            for (int j = 1;
                 j <= b.length();
                 j++) {

                int cost =
                        a.charAt(i - 1)
                                ==
                                b.charAt(j - 1)
                                ? 0
                                : 1;

                current[j] =
                        Math.min(
                                Math.min(
                                        current[j - 1] + 1,
                                        previous[j] + 1
                                ),
                                previous[j - 1] + cost
                        );
            }

            int[] tmp =
                    previous;

            previous =
                    current;

            current =
                    tmp;
        }

        return previous[b.length()];
    }

    /*
     * ============================================================
     * CARD DATA
     * ============================================================
     */

    public static class CardData {

        public final String name;
        public final String cardClass;
        public final String type;
        public final String rarity;
        public final String id;

        public final String text;

        public final int cost;
        public final int attack;
        public final int health;

        /*
         * Additional Hearthstone metadata used by the
         * synergy/deck-analysis system.
         */
        public final String race;
        public final String spellSchool;
        public final String mechanics;

        public CardData(
                String name,
                String cardClass,
                String type,
                String rarity,
                String id,
                String text,
                int cost,
                int attack,
                int health,
                String race,
                String spellSchool,
                String mechanics
        ) {

            this.name =
                    name == null
                            ? ""
                            : name;

            this.cardClass =
                    cardClass == null
                            ? ""
                            : cardClass;

            this.type =
                    type == null
                            ? ""
                            : type;

            this.rarity =
                    rarity == null
                            ? ""
                            : rarity;

            this.id =
                    id == null
                            ? ""
                            : id;

            this.text =
                    text == null
                            ? ""
                            : text;

            this.cost =
                    Math.max(
                            0,
                            cost
                    );

            this.attack =
                    Math.max(
                            0,
                            attack
                    );

            this.health =
                    Math.max(
                            0,
                            health
                    );

            this.race =
                    race == null
                            ? ""
                            : race;

            this.spellSchool =
                    spellSchool == null
                            ? ""
                            : spellSchool;

            this.mechanics =
                    mechanics == null
                            ? ""
                            : mechanics;
        }

        /*
         * Backward-compatible constructor.
         */
        public CardData(
                String name,
                String cardClass,
                String type,
                String rarity,
                String id,
                String text,
                int cost,
                int attack,
                int health
        ) {

            this(
                    name,
                    cardClass,
                    type,
                    rarity,
                    id,
                    text,
                    cost,
                    attack,
                    health,
                    "",
                    "",
                    ""
            );
        }

        /*
         * Original compatibility constructor.
         */
        public CardData(
                String name,
                String cardClass,
                String type,
                String rarity,
                String id
        ) {

            this(
                    name,
                    cardClass,
                    type,
                    rarity,
                    id,
                    "",
                    0,
                    0,
                    0,
                    "",
                    "",
                    ""
            );
        }
    }

    public static class CardInfo {

        public final String name;
        public final String className;

        public CardInfo(
                String name,
                String className
        ) {

            this.name =
                    name == null
                            ? ""
                            : name;

            this.className =
                    className == null
                            ? ""
                            : className;
        }
    }

    /*
     * ============================================================
     * DRAFT ANALYSIS RESULT
     * ============================================================
     */

    public static class DraftAnalysis {

        public final String cardName;

        /*
         * Original HearthArena value.
         */
        public final double hearthArenaScore;

        /*
         * Mana curve adjustment.
         */
        public final double curveAdjustment;

        /*
         * Minion / spell / weapon balance.
         */
        public final double typeAdjustment;

        /*
         * Existing deck synergy.
         */
        public final double synergyAdjustment;

        /*
         * How well the card fills a structural deck need.
         */
        public final double deckFitAdjustment;

        /*
         * Removal / tempo contribution.
         */
        public final double removalTempoAdjustment;

        /*
         * Duplicate penalty.
         */
        public final double duplicateAdjustment;

        /*
         * Final recommendation value.
         */
        public final double finalScore;

        /*
         * Mana cost.
         */
        public final int manaCost;

        /*
         * Human-readable explanation.
         */
        public final String reason;

        public DraftAnalysis(
                String cardName,
                double hearthArenaScore,
                double curveAdjustment,
                double typeAdjustment,
                double synergyAdjustment,
                double deckFitAdjustment,
                double removalTempoAdjustment,
                double duplicateAdjustment,
                double finalScore,
                int manaCost,
                String reason
        ) {

            this.cardName =
                    cardName == null
                            ? ""
                            : cardName;

            this.hearthArenaScore =
                    roundScore(
                            hearthArenaScore
                    );

            this.curveAdjustment =
                    roundScore(
                            curveAdjustment
                    );

            this.typeAdjustment =
                    roundScore(
                            typeAdjustment
                    );

            this.synergyAdjustment =
                    roundScore(
                            synergyAdjustment
                    );

            this.deckFitAdjustment =
                    roundScore(
                            deckFitAdjustment
                    );

            this.removalTempoAdjustment =
                    roundScore(
                            removalTempoAdjustment
                    );

            this.duplicateAdjustment =
                    roundScore(
                            duplicateAdjustment
                    );

            this.finalScore =
                    roundScore(
                            finalScore
                    );

            this.manaCost =
                    manaCost;

            this.reason =
                    reason == null
                            ? ""
                            : reason;
        }

        /*
         * Backward-compatible constructor matching the
         * previous DraftAnalysis structure.
         */
        public DraftAnalysis(
                String cardName,
                double hearthArenaScore,
                double curveAdjustment,
                double typeAdjustment,
                double synergyAdjustment,
                double duplicateAdjustment,
                double finalScore,
                int manaCost,
                String reason
        ) {

            this(
                    cardName,
                    hearthArenaScore,
                    curveAdjustment,
                    typeAdjustment,
                    synergyAdjustment,
                    0.0,
                    0.0,
                    duplicateAdjustment,
                    finalScore,
                    manaCost,
                    reason
            );
        }
    }
}
