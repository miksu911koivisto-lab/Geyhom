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
    private static final String CARDS_URL = "https://api.hearthstonejson.com/v1/latest/enUS/cards.collectible.json";
    private static final String HEARTHARENA_URL = "https://www.heartharena.com/tierlist";
    private static final double UNKNOWN_CARD_SCORE = 0.0;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private static final Map<String, CardData> CARDS = new HashMap<>();
    private static final Map<String, String> CANONICAL_NAMES = new HashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, CardInfo> CARD_INFO = new HashMap<>();
    private static final Map<String, Map<String, Double>> CLASS_SCORES = new HashMap<>();
    private static final Map<String, Double> NEUTRAL_SCORES = new HashMap<>();
    private static final Map<String, Integer> PICKED_CARDS = new HashMap<>();
    private static final Map<String, Double> FALLBACK_SCORES = new HashMap<>();
    private static final Map<String, DraftAnalysis> LAST_ANALYSES = new HashMap<>();

    private static volatile boolean onlineLoaded = false;
    private static volatile boolean hearthArenaLoaded = false;
    private static volatile boolean hearthArenaLoading = false;
    private static volatile String status = "Käynnistetään...";
    private static volatile String reason = "";
    private static volatile String currentClass = "";
    private static volatile String lastRecommendation = "";
    private static volatile double lastRecommendationScore = 0.0;
    private static volatile double lastRecommendationGap = 0.0;
    private static volatile String lastRecommendationReason = "";

    private static String candidateClass = "";
    private static int candidateClassCount = 0;
    private static final int CLASS_CONFIRMATIONS = 2;

    private static final String[] VALID_CLASSES = {
            "DEATH KNIGHT", "DEMON HUNTER", "DRUID", "HUNTER", "MAGE",
            "PALADIN", "PRIEST", "ROGUE", "SHAMAN", "WARLOCK", "WARRIOR"
    };

    private static final double CURVE_MAX_BONUS = 0.45;
    private static final double TYPE_MAX_BONUS = 0.25;
    private static final double SYNERGY_MAX_BONUS = 0.60;
    private static final double DECK_FIT_MAX_BONUS = 0.45;
    private static final double REMOVAL_TEMPO_MAX_BONUS = 0.55;
    private static final double DUPLICATE_PENALTY = 0.15;
    private static final int MAX_MANA_COST = 10;

    static {
        initializeClassMaps();
        initializeFallbackScores();
        initializeAliases();
        loadCards();
    }

    private static void initializeClassMaps() {
        for (String cls : VALID_CLASSES) {
            CLASS_SCORES.put(cls, new HashMap<>());
        }
    }

    private static void initializeFallbackScores() {
        FALLBACK_SCORES.put(normalize("Temporal Construct"), 5.54);
        FALLBACK_SCORES.put(normalize("Bitter End"), 6.62);
        FALLBACK_SCORES.put(normalize("Sealed Lancer"), 5.56);
        FALLBACK_SCORES.put(normalize("Scaled Lancer"), 5.56);
        FALLBACK_SCORES.put(normalize("Soldier of the Infinite"), 5.80);
        FALLBACK_SCORES.put(normalize("Soldier of the Bronze"), 4.60);

        FALLBACK_SCORES.put(normalize("Toreth the Unbreaking"), 7.60);
        FALLBACK_SCORES.put(normalize("Chromatus"), 10.10);
        FALLBACK_SCORES.put(normalize("Naralex, Herald of the Flights"), 6.60);
    }

    private static void initializeAliases() {
        addAlias("soldier of ihfini", "Soldier of the Infinite");
        addAlias("sotdier of infinite", "Soldier of the Infinite");
        addAlias("so1dier of infinite", "Soldier of the Infinite");
        addAlias("soldier of the infinite", "Soldier of the Infinite");
        addAlias("soldieroftheinfinite", "Soldier of the Infinite");
        addAlias("soldier of ihfinite", "Soldier of the Infinite");

        addAlias("soldier of bronze", "Soldier of the Bronze");
        addAlias("soldierofthebronze", "Soldier of the Bronze");

        addAlias("temporalconstruct", "Temporal Construct");
        addAlias("temporal construct", "Temporal Construct");

        addAlias("bitterend", "Bitter End");
        addAlias("bitter end", "Bitter End");

        addAlias("sealedlancer", "Sealed Lancer");
        addAlias("sealed lancer", "Sealed Lancer");

        addAlias("scaledlancer", "Scaled Lancer");
        addAlias("scaled lancer", "Scaled Lancer");

        // OCR variants from the Arena legendary-group screen.
        addAlias("unbreakin", "Toreth the Unbreaking");
        addAlias("toreth the unbreakin", "Toreth the Unbreaking");

        addAlias("matus", "Chromatus");
        addAlias("chromatu", "Chromatus");

        addAlias("ald of the flighte", "Naralex, Herald of the Flights");
        addAlias("herald of the flighte", "Naralex, Herald of the Flights");
    }

    private static void addAlias(String bad, String correct) {
        ALIASES.put(normalize(bad), correct);
    }

    private static void loadCards() {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;

            try {
                status = "Ladataan korttitietoja...";

                connection = (HttpURLConnection)
                        new URL(CARDS_URL).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty(
                        "User-Agent",
                        "ArenaHelper/1.0"
                );

                int code = connection.getResponseCode();

                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP " + code);
                }

                JSONArray array =
                        new JSONArray(readStream(connection.getInputStream()));

                CARDS.clear();
                CANONICAL_NAMES.clear();
                CARD_INFO.clear();

                int count = 0;

                for (int i = 0; i < array.length(); i++) {

                    JSONObject o = array.optJSONObject(i);

                    if (o == null) {
                        continue;
                    }

                    String name =
                            o.optString("name", "").trim();

                    if (name.isEmpty()) {
                        continue;
                    }

                    String cardClass =
                            o.optString("cardClass", "").trim();

                    String type =
                            o.optString("type", "").trim();

                    String rarity =
                            o.optString("rarity", "").trim();

                    String id =
                            o.optString("id", "").trim();

                    String text =
                            o.optString("text", "").trim();

                    String race =
                            o.optString("race", "").trim();

                    String school =
                            o.optString("spellSchool", "").trim();

                    String mechanics =
                            jsonArrayToText(
                                    o.optJSONArray("mechanics")
                            );

                    int cost =
                            o.optInt("cost", 0);

                    int attack =
                            o.optInt("attack", 0);

                    int health =
                            o.optInt("health", 0);

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
                                    school,
                                    mechanics
                            );

                    String key =
                            normalize(name);

                    CARDS.put(key, data);
                    CANONICAL_NAMES.put(key, name);

                    CARD_INFO.put(
                            key,
                            new CardInfo(
                                    name,
                                    normalizeClass(cardClass)
                            )
                    );

                    count++;
                }

                onlineLoaded = count > 0;

                if (!onlineLoaded) {
                    status = "Korttitietokanta tyhjä";
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

    private static String jsonArrayToText(JSONArray array) {

        if (array == null || array.length() == 0) {
            return "";
        }

        StringBuilder b =
                new StringBuilder();

        for (int i = 0; i < array.length(); i++) {

            String v =
                    array.optString(i, "");

            if (v.isEmpty()) {
                continue;
            }

            if (b.length() > 0) {
                b.append(' ');
            }

            b.append(v);
        }

        return b.toString();
    }

    private static synchronized void loadHearthArenaScores() {

        if (hearthArenaLoading ||
                CANONICAL_NAMES.isEmpty()) {
            return;
        }

        hearthArenaLoading = true;

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

                status =
                        "Ladataan HearthArena-arvoja...";

                connection =
                        (HttpURLConnection)
                                new URL(
                                        HEARTHARENA_URL
                                ).openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140.0 Mobile Safari/537.36"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                );

                connection.setRequestProperty(
                        "Accept-Language",
                        "en-US,en;q=0.9"
                );

                int code =
                        connection.getResponseCode();

                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception(
                            "HearthArena HTTP " + code
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
                        "HearthArena scores loaded: "
                                + parsed
                );

            } catch (Exception e) {

                hearthArenaLoaded = false;

                Log.e(
                        TAG,
                        "HearthArena loading failed; local fallback remains active",
                        e
                );

                status =
                        "HearthArena ei latautunut – fallback käytössä";

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

    private static int parseHearthArenaHtml(String html) {
        return parseHearthArenaText(
                htmlToText(html)
        );
    }

    private static int parseHearthArenaText(String input) {

        if (input == null || input.isEmpty()) {
            return 0;
        }

        String[] lines =
                input.split("\\r?\\n");

        String activeClass = "";
        int parsed = 0;

        Set<String> seen =
                new HashSet<>();

        for (int i = 0; i < lines.length; i++) {

            String line =
                    cleanLine(lines[i]);

            if (line.isEmpty()) {
                continue;
            }

            String detectedClass =
                    detectClassHeader(line);

            if (!detectedClass.isEmpty()) {
                activeClass = detectedClass;
                continue;
            }

            if (isNeutralHeader(line)) {
                activeClass = "NEUTRAL";
                continue;
            }

            if (activeClass.isEmpty()) {
                continue;
            }

            String possibleName =
                    removeTierNoise(
                            removeRankingPrefix(line)
                    );

            if (possibleName.isEmpty() ||
                    parseScore(possibleName) != null ||
                    isTierOnlyLine(possibleName)) {
                continue;
            }

            String canonical =
                    findCanonicalCardName(
                            possibleName
                    );

            if (canonical == null) {
                canonical =
                        findCardNameInsideLine(
                                possibleName
                        );
            }

            if (canonical == null) {
                continue;
            }

            Double raw =
                    findScoreOnSameLine(
                            possibleName,
                            canonical
                    );

            if (raw == null) {
                raw =
                        findFollowingScore(
                                lines,
                                i
                        );
            }

            if (raw == null ||
                    raw < 0 ||
                    raw > 200) {
                continue;
            }

            double score =
                    normalizeHearthArenaScore(raw);

            String key =
                    normalize(canonical);

            String seenKey =
                    activeClass + "|" + key;

            if (seen.contains(seenKey)) {
                continue;
            }

            seen.add(seenKey);

            if ("NEUTRAL".equals(activeClass)) {

                NEUTRAL_SCORES.put(
                        key,
                        score
                );

            } else {

                Map<String, Double> map =
                        CLASS_SCORES.get(activeClass);

                if (map == null) {
                    map = new HashMap<>();
                    CLASS_SCORES.put(
                            activeClass,
                            map
                    );
                }

                map.put(key, score);
            }

            parsed++;
        }

        return parsed;
    }

    private static double normalizeHearthArenaScore(
            double raw) {

        if (raw > 10.0) {
            return roundScore(raw / 10.0);
        }

        return roundScore(raw);
    }

    private static String findCardNameInsideLine(
            String line) {

        if (line == null || line.isEmpty()) {
            return null;
        }

        String n =
                normalize(line);

        String best = null;
        int bestLength = -1;

        for (Map.Entry<String, String> e :
                CANONICAL_NAMES.entrySet()) {

            String card =
                    e.getKey();

            if (card.isEmpty()) {
                continue;
            }

            boolean found =
                    n.equals(card)
                            || n.startsWith(card + " ")
                            || n.endsWith(" " + card)
                            || n.contains(" " + card + " ");

            if (found &&
                    card.length() > bestLength) {

                best =
                        e.getValue();

                bestLength =
                        card.length();
            }
        }

        if (best != null) {
            return best;
        }

        for (Map.Entry<String, String> e :
                ALIASES.entrySet()) {

            String alias =
                    e.getKey();

            if (n.equals(alias)
                    || n.contains(" " + alias + " ")
                    || n.startsWith(alias + " ")
                    || n.endsWith(" " + alias)) {

                return e.getValue();
            }
        }

        return null;
    }

    private static Double findScoreOnSameLine(
            String line,
            String canonical) {

        if (line == null ||
                canonical == null) {
            return null;
        }

        String n =
                normalize(line);

        String card =
                normalize(canonical);

        int pos =
                n.indexOf(card);

        if (pos < 0) {
            return null;
        }

        String after =
                n.substring(
                        pos + card.length()
                ).trim();

        if (after.isEmpty()) {
            return null;
        }

        for (String part :
                after.split("\\s+")) {

            Double s =
                    parseScore(part);

            if (s != null) {
                return s;
            }
        }

        return null;
    }

    private static Double findFollowingScore(
            String[] lines,
            int start) {

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

            if (!detectClassHeader(value).isEmpty()
                    || isNeutralHeader(value)) {
                return null;
            }

            String next =
                    removeRankingPrefix(value);

            if (looksLikeCardLine(next)) {
                return null;
            }

            if (isTierOnlyLine(next)) {
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
            String line) {

        if (line == null ||
                line.isEmpty()) {
            return null;
        }

        String cleaned =
                line.replace("↓", " ")
                        .replace("↑", " ")
                        .replace(":", " ");

        for (String part :
                cleaned.split("\\s+")) {

            Double score =
                    parseScore(part);

            if (score != null) {
                return score;
            }
        }

        return null;
    }

    private static Double parseScore(String text) {

        if (text == null) {
            return null;
        }

        String value =
                text.trim()
                        .replace("↓", "")
                        .replace("↑", "")
                        .replace(",", ".");

        if (value.matches("\\d+\\.")) {
            return null;
        }

        if (!value.matches(
                "\\d+(?:\\.\\d+)?")) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String findCanonicalCardName(
            String input) {

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

        String best = null;

        for (Map.Entry<String, String> e :
                CANONICAL_NAMES.entrySet()) {

            String candidate =
                    e.getKey();

            if (candidate.length() < 3) {
                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            int allowed =
                    normalized.length() <= 8
                            ? 1
                            : (normalized.length() <= 15
                            ? 2
                            : 3);

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        e.getValue();
            }
        }

        return best;
    }

    private static String htmlToText(String html) {

        if (html == null) {
            return "";
        }

        String text = html;

        text = text.replaceAll(
                "(?is)<script[^>]*>.*?</script>",
                "\\n"
        );

        text = text.replaceAll(
                "(?is)<style[^>]*>.*?</style>",
                "\\n"
        );

        text = text.replaceAll(
                "(?i)</(div|p|li|ul|ol|h1|h2|h3|h4|h5|h6|tr|td|th|section|article)>",
                "\\n"
        );

        text = text.replaceAll(
                "(?i)<br\\s*/?>",
                "\\n"
        );

        text = text.replaceAll(
                "(?s)<[^>]+>",
                " "
        );

        text =
                decodeHtmlEntities(text)
                        .replace("\r", "");

        text =
                text.replaceAll(
                        "[\\t ]+",
                        " "
                );

        text =
                text.replaceAll(
                        "\\n[ \\t]+",
                        "\\n"
                );

        return text.replaceAll(
                "\\n{3,}",
                "\\n\\n"
        );
    }

    private static String decodeHtmlEntities(
            String text) {

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

    private static String detectClassHeader(
            String line) {

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

        String n =
                normalize(cleaned);

        for (String valid :
                VALID_CLASSES) {

            String cls =
                    normalize(valid);

            if (n.equals(cls)
                    || n.equals(cls + " cards")
                    || n.equals(
                    "common " + cls + " cards")
                    || n.equals(
                    "rare " + cls + " cards")
                    || n.equals(
                    "epic " + cls + " cards")
                    || n.equals(
                    "legendary " + cls + " cards")
                    || n.equals(
                    "basic " + cls + " cards")
                    || n.equals(
                    "common " + cls)
                    || n.equals(
                    "rare " + cls)
                    || n.equals(
                    "epic " + cls)
                    || n.equals(
                    "legendary " + cls)
                    || n.equals(
                    "basic " + cls)) {

                return valid;
            }
        }

        return "";
    }

    private static boolean isNeutralHeader(
            String line) {

        if (line == null) {
            return false;
        }

        String n =
                normalize(
                        line
                                .replaceAll(
                                        "^\\s*[\\*•-]\\s*",
                                        ""
                                )
                                .replaceAll(
                                        "^\\s*#{1,6}\\s*",
                                        ""
                                )
                                .trim()
                );

        return n.equals("neutral")
                || n.equals("neutral cards")
                || n.equals(
                "common neutral cards")
                || n.equals(
                "rare neutral cards")
                || n.equals(
                "epic neutral cards")
                || n.equals(
                "legendary neutral cards")
                || n.equals(
                "basic neutral cards");
    }

    private static String cleanLine(
            String line) {

        if (line == null) {
            return "";
        }

        return line
                .replace("\u00A0", " ")
                .replace("\u200B", "")
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String removeRankingPrefix(
            String line) {

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
            String name) {

        if (name == null) {
            return "";
        }

        return name
                .replace("↓", "")
                .replace("↑", "")
                .trim();
    }

    private static boolean isTierOnlyLine(
            String line) {

        if (line == null) {
            return false;
        }

        String n =
                normalize(line);

        return n.equals("great")
                || n.equals("good")
                || n.equals("average")
                || n.equals("poor")
                || n.equals("bad")
                || n.equals("premium")
                || n.equals("solid")
                || n.equals("weak")
                || n.equals("terrible")
                || n.equals("tier")
                || n.equals("above average")
                || n.equals("below average");
    }

    private static boolean looksLikeCardLine(
            String line) {

        if (line == null ||
                line.isEmpty()) {
            return false;
        }

        String n =
                normalize(line);

        return CANONICAL_NAMES.containsKey(n)
                || ALIASES.containsKey(n)
                || findCardNameInsideLine(line) != null;
    }

    public static String correctOcr(
            String detected) {

        if (detected == null) {
            return "";
        }

        String input =
                detected.trim();

        if (input.isEmpty()) {
            return "";
        }

        String n =
                normalize(input);

        String alias =
                ALIASES.get(n);

        if (alias != null) {
            return alias;
        }

        String canonical =
                CANONICAL_NAMES.get(n);

        if (canonical != null) {
            return canonical;
        }

        CardInfo info =
                CARD_INFO.get(n);

        if (info != null) {
            return info.name;
        }

        String best =
                findBestOcrMatch(n);

        return best == null
                ? input
                : best;
    }

    private static String findBestOcrMatch(
            String normalized) {

        if (normalized == null ||
                normalized.isEmpty() ||
                CANONICAL_NAMES.isEmpty()) {
            return null;
        }

        String best = null;
        int bestDistance =
                Integer.MAX_VALUE;

        for (Map.Entry<String, String> e :
                CANONICAL_NAMES.entrySet()) {

            String candidate =
                    e.getKey();

            if (candidate.isEmpty()) {
                continue;
            }

            if (normalized.length() >= 6 &&
                    candidate.length() >= 6 &&
                    normalized.charAt(0) !=
                            candidate.charAt(0)) {
                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            int allowed =
                    normalized.length() <= 7
                            ? 1
                            : (normalized.length() <= 14
                            ? 2
                            : 3);

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        e.getValue();
            }
        }

        return best;
    }

    private static double getBaseScore(
            String cardName) {

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

        Double fuzzyFallback =
                findBestFallbackScore(key);

        if (fuzzyFallback != null) {
            return fuzzyFallback;
        }

        CardData card =
                CARDS.get(key);

        if (card != null) {
            return heuristicBaseScore(card);
        }

        return UNKNOWN_CARD_SCORE;
    }

    private static Double findBestFallbackScore(
            String normalized) {

        if (normalized == null ||
                normalized.isEmpty() ||
                FALLBACK_SCORES.isEmpty()) {
            return null;
        }

        Double exact =
                FALLBACK_SCORES.get(normalized);

        if (exact != null) {
            return exact;
        }

        String bestKey = null;
        int bestDistance =
                Integer.MAX_VALUE;

        for (String candidate :
                FALLBACK_SCORES.keySet()) {

            if (candidate == null ||
                    candidate.isEmpty()) {
                continue;
            }

            if (normalized.length() >= 5 &&
                    candidate.contains(normalized)) {

                int distance =
                        candidate.length()
                                - normalized.length();

                if (distance < bestDistance) {
                    bestDistance =
                            distance;
                    bestKey =
                            candidate;
                }

                continue;
            }

            if (candidate.length() >= 5 &&
                    normalized.contains(candidate)) {

                int distance =
                        normalized.length()
                                - candidate.length();

                if (distance < bestDistance) {
                    bestDistance =
                            distance;
                    bestKey =
                            candidate;
                }

                continue;
            }

            int allowed =
                    normalized.length() <= 8
                            ? 2
                            : (normalized.length() <= 16
                            ? 4
                            : 5);

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                bestKey =
                        candidate;
            }
        }

        return bestKey == null
                ? null
                : FALLBACK_SCORES.get(bestKey);
    }

    private static double heuristicBaseScore(
            CardData card) {

        if (card == null) {
            return 0.0;
        }

        double score = 4.0;

        int cost =
                Math.max(
                        0,
                        card.cost
                );

        double statValue =
                card.attack + card.health;

        if (card.type.equalsIgnoreCase(
                "MINION")) {

            score += clamp(
                    (statValue -
                            Math.max(
                                    1,
                                    cost * 2.0
                            )) * 0.18,
                    -1.5,
                    2.0
            );

        } else if (
                card.type.equalsIgnoreCase(
                        "WEAPON")) {

            score += clamp(
                    (card.attack +
                            card.health -
                            cost) * 0.15,
                    -1.0,
                    1.5
            );

        } else {
            score += 0.20;
        }

        String text =
                normalizeForSynergy(
                        card.text
                );

        if (containsAny(
                text,
                "discover",
                "draw",
                "destroy",
                "deal damage",
                "transform",
                "silence")) {
            score += 0.55;
        }

        if (containsAny(
                text,
                "taunt",
                "rush",
                "lifesteal",
                "divine shield",
                "freeze")) {
            score += 0.35;
        }

        if (containsAny(
                text,
                "summon",
                "buff",
                "gain",
                "give")) {
            score += 0.25;
        }

        if (containsAny(
                text,
                "random",
                "discard",
                "enemy hero",
                "your opponent")) {
            score -= 0.15;
        }

        if (cost >= 8) {
            score -= 0.35;
        }

        if (cost <= 2) {
            score += 0.25;
        }

        if (card.rarity.equalsIgnoreCase(
                "LEGENDARY")) {
            score += 0.20;
        }

        return roundScore(
                clamp(
                        score,
                        1.0,
                        9.5
                )
        );
    }

    public static double score(
            String cardName) {

        DraftAnalysis a =
                analyzeCard(cardName);

        if (a == null ||
                a.finalScore <= 0) {

            reason =
                    "Kortille ei löytynyt arvoa";

            return 0.0;
        }

        reason =
                a.reason;

        return a.finalScore;
    }

    public static double getClassSpecificScore(
            String cardName) {

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
                normalizeClass(
                        currentClass
                );

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

        return value == null
                ? -1
                : value;
    }

    public static String getCardScore(
            String cardName) {

        DraftAnalysis a =
                analyzeCard(cardName);

        return a == null ||
                a.finalScore <= 0
                ? "?"
                : String.format(
                Locale.US,
                "%.2f",
                a.finalScore
        );
    }

    public static double getCardScoreValue(
            String cardName) {
        return getAnalyzedScore(cardName);
    }

    public static synchronized DraftAnalysis analyzeCard(
            String cardName) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return emptyAnalysis(
                    "",
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
                    emptyAnalysis(
                            corrected,
                            "Kortin perustietoja ei löytynyt"
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
                calculateDuplicateAdjustment(
                        corrected
                );

        double finalScore =
                roundScore(
                        clamp(
                                base
                                        + curve
                                        + type
                                        + synergy
                                        + deckFit
                                        + removalTempo
                                        + duplicate,
                                0.0,
                                12.0
                        )
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

    private static DraftAnalysis emptyAnalysis(
            String name,
            String why) {

        return new DraftAnalysis(
                name,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                -1,
                why
        );
    }

    public static synchronized DraftAnalysis[] analyzeDraft(
            String card1,
            String card2,
            String card3) {

        detectClassFromCards(
                card1,
                card2,
                card3
        );

        DraftAnalysis[] result = {
                analyzeCard(card1),
                analyzeCard(card2),
                analyzeCard(card3)
        };

        double best = -1;
        double second = -1;
        String recommendation = "";

        for (DraftAnalysis a : result) {

            if (a == null ||
                    a.finalScore <= 0) {
                continue;
            }

            if (a.finalScore > best) {

                second = best;
                best = a.finalScore;
                recommendation =
                        a.cardName;

            } else if (a.finalScore > second) {

                second =
                        a.finalScore;
            }
        }

        if (recommendation.isEmpty()) {

            lastRecommendation = "";
            lastRecommendationScore = 0;
            lastRecommendationGap = 0;

            lastRecommendationReason =
                    "Kortteja ei tunnistettu";

            return result;
        }

        if (second < 0) {
            second = 0;
        }

        lastRecommendation =
                recommendation;

        lastRecommendationScore =
                roundScore(best);

        lastRecommendationGap =
                roundScore(
                        Math.max(
                                0,
                                best - second
                        )
                );

        lastRecommendationReason =
                buildRecommendationReason(
                        recommendation,
                        best,
                        second
                );

        reason =
                lastRecommendationReason;

        return result;
    }

    public static String recommend(
            String card1,
            String card2,
            String card3) {

        DraftAnalysis[] analyses =
                analyzeDraft(
                        card1,
                        card2,
                        card3
                );

        if (analyses == null) {
            return "EI TUNNISTETTAVA";
        }

        String bestName = "";
        double best = -1;

        for (DraftAnalysis a :
                analyses) {

            if (a != null &&
                    a.finalScore > best &&
                    a.finalScore > 0) {

                best =
                        a.finalScore;

                bestName =
                        a.cardName;
            }
        }

        if (bestName.isEmpty()) {

            reason =
                    "Yhdellekään kortille ei löytynyt arvoa";

            return "EI TUNNISTETTAVAA";
        }

        return bestName;
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
            String cardName) {

        DraftAnalysis a =
                analyzeCard(cardName);

        return a == null
                ? ""
                : a.reason;
    }

    public static double getAnalyzedScore(
            String cardName) {

        DraftAnalysis a =
                analyzeCard(cardName);

        return a == null
                ? 0.0
                : a.finalScore;
    }

    private static double calculateCurveAdjustment(
            CardData candidate) {

        if (candidate == null) {
            return 0;
        }

        int cost =
                getManaCost(candidate);

        int total =
                getPickedCardCountTotal();

        if (cost < 0 ||
                total < 3) {
            return 0;
        }

        int[] curve =
                getCurrentManaCurve();

        int same =
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

        if (same < desired) {

            return roundScore(
                    clamp(
                            (desired - same) * 0.12,
                            0,
                            CURVE_MAX_BONUS
                    )
            );
        }

        if (same > desired + 2) {

            return roundScore(
                    -clamp(
                            (same - desired - 2) * 0.10,
                            0,
                            CURVE_MAX_BONUS
                    )
            );
        }

        return 0;
    }

    private static int desiredCardsAtCost(
            int cost,
            int total) {

        if (cost <= 1) {
            return total >= 15 ? 2 : 1;
        }

        if (cost == 2 ||
                cost == 3) {
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

        for (Map.Entry<String, Integer> e :
                PICKED_CARDS.entrySet()) {

            Integer count =
                    e.getValue();

            if (count == null ||
                    count <= 0) {
                continue;
            }

            CardData card =
                    CARDS.get(e.getKey());

            if (card == null) {
                continue;
            }

            int cost =
                    getManaCost(card);

            if (cost < 0) {
                continue;
            }

            curve[
                    Math.min(
                            cost,
                            MAX_MANA_COST
                    )
            ] += count;
        }

        return curve;
    }

    private static int getPickedCardCountTotal() {

        int total = 0;

        for (Integer v :
                PICKED_CARDS.values()) {

            if (v != null &&
                    v > 0) {
                total += v;
            }
        }

        return total;
    }

    private static double calculateTypeAdjustment(
            CardData card) {

        if (card == null) {
            return 0;
        }

        int total =
                getPickedCardCountTotal();

        if (total < 4) {
            return 0;
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

        if (type.equals("SPELL") &&
                spells > 10 &&
                total >= 20) {
            return -TYPE_MAX_BONUS;
        }

        if (type.equals("WEAPON") &&
                weapons >= 3) {
            return -TYPE_MAX_BONUS;
        }

        return 0;
    }

    private static int countPickedType(
            String wantedType) {

        int total = 0;

        for (Map.Entry<String, Integer> e :
                PICKED_CARDS.entrySet()) {

            CardData c =
                    CARDS.get(e.getKey());

            if (c == null) {
                continue;
            }

            if (normalizeType(c.type)
                    .equals(wantedType)
                    && e.getValue() != null) {

                total += e.getValue();
            }
        }

        return total;
    }

    private static String normalizeType(
            String type) {

        if (type == null) {
            return "";
        }

        return type
                .trim()
                .toUpperCase(Locale.US)
                .replace("-", "_")
                .replace(" ", "_");
    }

    private static double calculateDeckFitAdjustment(
            CardData candidate) {

        if (candidate == null) {
            return 0;
        }

        int total =
                getPickedCardCountTotal();

        if (total < 4) {
            return 0;
        }

        double value = 0;

        String type =
                normalizeType(candidate.type);

        int minions =
                countPickedType("MINION");

        int spells =
                countPickedType("SPELL");

        int weapons =
                countPickedType("WEAPON");

        if (type.equals("MINION")) {

            if (total >= 8 &&
                    minions < 7) {
                value += .20;
            }

            if (total >= 15 &&
                    minions < 10) {
                value += .12;
            }
        }

        if (type.equals("SPELL")) {

            if (total >= 12 &&
                    spells >= 7) {
                value -= .12;
            }

            if (total >= 20 &&
                    spells >= 10) {
                value -= .15;
            }
        }

        if (type.equals("WEAPON")) {

            if (weapons >= 2) {
                value -= .10;
            }

            if (weapons >= 3) {
                value -= .15;
            }
        }

        int cost =
                getManaCost(candidate);

        if (cost <= 2 &&
                total >= 8 &&
                getCurveCount(1)
                        + getCurveCount(2) < 4) {
            value += .15;
        }

        int mid =
                getCurveCount(3)
                        + getCurveCount(4)
                        + getCurveCount(5);

        if (cost >= 3 &&
                cost <= 5 &&
                total >= 15 &&
                mid < 7) {
            value += .12;
        }

        if (cost >= 7 &&
                total >= 15) {

            int expensive =
                    getCurveCount(7)
                            + getCurveCount(8)
                            + getCurveCount(9)
                            + getCurveCount(10);

            if (expensive >= 3) {
                value -= .18;
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
            int mana) {

        int[] c =
                getCurrentManaCurve();

        return mana >= 0 &&
                mana < c.length
                ? c[mana]
                : 0;
    }

    private static double calculateSynergyAdjustment(
            CardData candidate) {

        if (candidate == null ||
                getPickedCardCountTotal() < 2) {
            return 0;
        }

        double value = 0;

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

            value +=
                    calculatePairSynergy(
                            candidate,
                            candidateText,
                            picked,
                            getFullSynergyText(picked)
                    )
                    * Math.min(count, 2);
        }

        value +=
                calculateTextSynergy(
                        candidateText
                );

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
            String pickedText) {

        if (candidate == null ||
                picked == null) {
            return 0;
        }

        double value = 0;

        String ct =
                normalizeType(
                        candidate.type
                );

        String pt =
                normalizeType(
                        picked.type
                );

        if ((ct.equals("MINION") &&
                pt.equals("MINION"))
                ||
                (ct.equals("SPELL") &&
                        pt.equals("SPELL"))) {

            if (sharesImportantKeyword(
                    candidateText,
                    pickedText)) {
                value += .12;
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
                "totem")
                &&
                sharesImportantKeyword(
                        candidateText,
                        pickedText)) {

            value += .10;
        }

        if (candidateText.contains("spell") &&
                pt.equals("SPELL")) {
            value += .08;
        }

        if (pickedText.contains("spell") &&
                ct.equals("SPELL")) {
            value += .08;
        }

        return value;
    }

    private static boolean sharesImportantKeyword(
            String a,
            String b) {

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

        for (String k : keywords) {

            if (a.contains(k) &&
                    b.contains(k)) {
                return true;
            }
        }

        return false;
    }

    private static double calculateTextSynergy(
            String text) {

        if (text == null ||
                text.isEmpty()) {
            return 0;
        }

        double value = 0;

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

            if (text.contains(concept) &&
                    hasPickedText(concept)) {
                value += .10;
            }
        }

        return value;
    }

    private static double calculateRaceSynergy(
            CardData candidate) {

        if (candidate == null) {
            return 0;
        }

        String race =
                normalizeRace(candidate.race);

        if (race.isEmpty()) {
            return 0;
        }

        int matching = 0;

        for (String pickedName :
                PICKED_CARDS.keySet()) {

            CardData picked =
                    CARDS.get(pickedName);

            if (picked == null) {
                continue;
            }

            if (!normalizeRace(
                    picked.race
            ).isEmpty()
                    &&
                    normalizeRace(
                            picked.race
                    ).equals(race)) {

                matching +=
                        Math.min(
                                2,
                                getPickedCount(
                                        picked.name
                                )
                        );
            }
        }

        return matching <= 0
                ? 0
                : clamp(
                matching * .06,
                0,
                .18
        );
    }

    private static boolean hasPickedText(
            String keyword) {

        for (String pickedName :
                PICKED_CARDS.keySet()) {

            CardData c =
                    CARDS.get(pickedName);

            if (c != null &&
                    getFullSynergyText(c)
                            .contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private static String getFullSynergyText(
            CardData card) {

        if (card == null) {
            return "";
        }

        StringBuilder b =
                new StringBuilder();

        if (!card.text.isEmpty()) {
            b.append(card.text);
        }

        if (!card.race.isEmpty()) {
            b.append(' ')
                    .append(card.race);
        }

        if (!card.mechanics.isEmpty()) {
            b.append(' ')
                    .append(card.mechanics);
        }

        if (!card.spellSchool.isEmpty()) {
            b.append(' ')
                    .append(card.spellSchool);
        }

        return normalizeForSynergy(
                b.toString()
        );
    }

    private static String normalizeForSynergy(
            String text) {

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
            String race) {

        return race == null
                ? ""
                : race
                .toLowerCase(Locale.US)
                .replace("_", " ")
                .trim();
    }

    private static double calculateRemovalTempoAdjustment(
            CardData card) {

        if (card == null ||
                getPickedCardCountTotal() < 3) {
            return 0;
        }

        String text =
                normalizeForSynergy(
                        card.text
                );

        double value = 0;

        int removal = 0;
        int tempo = 0;

        if (containsAny(
                text,
                "destroy",
                "deal damage",
                "destroy a",
                "destroy an",
                "kill",
                "silence",
                "transform",
                "return it to its owner")) {

            removal++;
        }

        if (containsAny(
                text,
                "all enemy",
                "enemy minion",
                "enemy minions",
                "random enemy",
                "freeze",
                "aoe")) {

            removal++;
        }

        if (containsAny(
                text,
                "polymorph",
                "hex",
                "silence",
                "transform")) {

            removal++;
        }

        if (containsAny(
                text,
                "rush",
                "charge",
                "taunt",
                "freeze")) {

            tempo++;
        }

        if (containsAny(
                text,
                "summon",
                "gain",
                "give",
                "buff",
                "draw")) {

            tempo++;
        }

        if (normalizeType(card.type)
                .equals("WEAPON")) {
            tempo++;
        }

        if (removal > 0) {
            value +=
                    .18 * Math.min(
                            removal,
                            2
                    );
        }

        if (tempo > 0) {
            value +=
                    .10 * Math.min(
                            tempo,
                            2
                    );
        }

        if (normalizeType(card.type)
                .equals("MINION")) {

            int cost =
                    getManaCost(card);

            int stats =
                    card.attack + card.health;

            if (cost >= 1 &&
                    cost <= 4 &&
                    stats >= cost * 3) {

                value += .08;
            }

            if (cost <= 3 &&
                    card.attack >= 3) {

                value += .05;
            }
        }

        int existingRemoval =
                countPickedRemovalCards();

        if (removal > 0 &&
                existingRemoval >= 5) {
            value -= .10;
        }

        if (removal > 0 &&
                existingRemoval <= 1 &&
                getPickedCardCountTotal() >= 8) {
            value += .10;
        }

        return roundScore(
                clamp(
                        value,
                        0,
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

            if (containsAny(
                    normalizeForSynergy(
                            card.text
                    ),
                    "destroy",
                    "deal damage",
                    "kill",
                    "silence",
                    "transform",
                    "enemy minion",
                    "enemy minions",
                    "freeze")) {

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

    private static double calculateDuplicateAdjustment(
            String cardName) {

        int count =
                getPickedCount(cardName);

        if (count <= 0) {
            return 0;
        }

        return count == 1
                ? -DUPLICATE_PENALTY
                : -DUPLICATE_PENALTY * 2;
    }

    private static String buildAnalysisReason(
            CardData card,
            double base,
            double curve,
            double type,
            double synergy,
            double deckFit,
            double removalTempo,
            double duplicate) {

        List<String> reasons =
                new ArrayList<>();

        String source =
                getClassSpecificScore(
                        card == null
                                ? ""
                                : card.name
                ) >= 0
                        ? "HearthArena " + currentClass
                        : (
                        NEUTRAL_SCORES.containsKey(
                                normalize(
                                        card == null
                                                ? ""
                                                : card.name
                                )
                        )
                                ? "HearthArena Neutral"
                                : (
                                FALLBACK_SCORES.containsKey(
                                        normalize(
                                                card == null
                                                        ? ""
                                                        : card.name
                                        )
                                )
                                        ? "Paikallinen korttiarvo"
                                        : "Automaattinen fallback"
                        )
                );

        reasons.add(
                source + " " + formatScore(base)
        );

        if (curve > .04) {
            reasons.add(
                    "Mana curve +" +
                            formatScore(curve)
            );
        } else if (curve < -.04) {
            reasons.add(
                    "Mana curve " +
                            formatScore(curve)
            );
        }

        if (type > .04) {
            reasons.add(
                    "Type +" +
                            formatScore(type)
            );
        } else if (type < -.04) {
            reasons.add(
                    "Type " +
                            formatScore(type)
            );
        }

        if (synergy > .04) {
            reasons.add(
                    "Synergies +" +
                            formatScore(synergy)
            );
        } else if (synergy < -.04) {
            reasons.add(
                    "Synergies " +
                            formatScore(synergy)
            );
        }

        if (deckFit > .04) {
            reasons.add(
                    "Deck fit +" +
                            formatScore(deckFit)
            );
        } else if (deckFit < -.04) {
            reasons.add(
                    "Deck fit " +
                            formatScore(deckFit)
            );
        }

        if (removalTempo > .04) {
            reasons.add(
                    "Removal/tempo +" +
                            formatScore(removalTempo)
            );
        }

        if (duplicate < -.04) {
            reasons.add(
                    "Duplicate " +
                            formatScore(duplicate)
            );
        }

        if (card != null) {
            reasons.add(
                    getReadableType(card.type)
                            + " "
                            + getManaCost(card)
                            + " mana"
            );
        }

        return joinReasons(reasons);
    }

    private static String buildRecommendationReason(
            String recommendation,
            double best,
            double secondBest) {

        DraftAnalysis a =
                LAST_ANALYSES.get(
                        normalize(recommendation)
                );

        if (a == null) {
            return "Paras analysoitu kortti: "
                    + recommendation;
        }

        StringBuilder b =
                new StringBuilder(
                        "Arvo "
                )
                        .append(
                                formatScore(best)
                        )
                        .append(" — ")
                        .append(recommendation);

        double gap =
                Math.max(
                        0,
                        best - secondBest
                );

        if (gap > 0) {
            b.append(
                    " • Ero seuraavaan: +"
            )
                    .append(
                            formatScore(gap)
                    );
        }

        b.append(" • ")
                .append(a.reason);

        return b.toString();
    }

    private static String joinReasons(
            List<String> reasons) {

        StringBuilder b =
                new StringBuilder();

        for (String r : reasons) {

            if (b.length() > 0) {
                b.append(" • ");
            }

            b.append(r);
        }

        return b.toString();
    }

    private static String formatScore(
            double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    public static synchronized void recordPickedCard(
            String cardName) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {
            return;
        }

        String key =
                normalize(
                        correctOcr(cardName)
                );

        if (key.isEmpty()) {
            return;
        }

        PICKED_CARDS.put(
                key,
                getPickedCount(cardName) + 1
        );

        LAST_ANALYSES.clear();
    }

    public static synchronized void clearPickedCards() {

        PICKED_CARDS.clear();
        LAST_ANALYSES.clear();

        lastRecommendation = "";
        lastRecommendationScore = 0;
        lastRecommendationGap = 0;
        lastRecommendationReason = "";
    }

    public static synchronized int getPickedCount(
            String cardName) {

        if (cardName == null) {
            return 0;
        }

        Integer count =
                PICKED_CARDS.get(
                        normalize(
                                correctOcr(cardName)
                        )
                );

        return count == null
                ? 0
                : count;
    }

    public static synchronized void detectClassFromCards(
            String card1,
            String card2,
            String card3) {

        if (!currentClass.isEmpty()) {
            return;
        }

        String[] cards = {
                card1,
                card2,
                card3
        };

        Map<String, Integer> counts =
                new HashMap<>();

        for (String card : cards) {

            String cls =
                    getClassForCard(card);

            if (!cls.isEmpty()) {

                counts.put(
                        cls,
                        counts.containsKey(cls)
                                ? counts.get(cls) + 1
                                : 1
                );
            }
        }

        String majority = "";
        int majorityCount = 0;

        for (Map.Entry<String, Integer> e :
                counts.entrySet()) {

            if (e.getValue() >
                    majorityCount) {

                majority =
                        e.getKey();

                majorityCount =
                        e.getValue();
            }
        }

        if (!majority.isEmpty() &&
                majorityCount >= 2) {

            currentClass =
                    majority;

            candidateClass = "";
            candidateClassCount = 0;

            status =
                    "Luokka tunnistettu: "
                            + currentClass;

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

        if (detected.equals(candidateClass)) {

            candidateClassCount++;

        } else {

            candidateClass =
                    detected;

            candidateClassCount = 1;
        }

        if (candidateClassCount >=
                CLASS_CONFIRMATIONS) {

            currentClass =
                    candidateClass;

            status =
                    "Luokka tunnistettu: "
                            + currentClass;
        }
    }

    private static String getFirstClassFromCards(
            String card1,
            String card2,
            String card3) {

        for (String card :
                new String[]{
                        card1,
                        card2,
                        card3
                }) {

            String cls =
                    getClassForCard(card);

            if (!cls.isEmpty()) {
                return cls;
            }
        }

        return "";
    }

    public static String getClassForCard(
            String cardName) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {
            return "";
        }

        CardInfo info =
                CARD_INFO.get(
                        normalize(
                                correctOcr(cardName)
                        )
                );

        if (info == null) {
            return "";
        }

        String cls =
                normalizeClass(
                        info.className
                );

        return cls.equals("NEUTRAL")
                || cls.equals("INVALID")
                || cls.isEmpty()
                ? ""
                : cls;
    }

    public static String getCurrentClass() {

        return currentClass == null ||
                currentClass.isEmpty()
                ? "EI TUNNISTETTU"
                : currentClass;
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
        clearPickedCards();

        status =
                "Luokka ja varatut kortit nollattu";
    }

    public static synchronized void setClassScore(
            String cardName,
            String className,
            double score) {

        if (cardName == null ||
                className == null) {
            return;
        }

        String key =
                normalize(
                        correctOcr(cardName)
                );

        String cls =
                normalizeClass(className);

        if (key.isEmpty() ||
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
                key,
                normalizeHearthArenaScore(score)
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

        int total =
                NEUTRAL_SCORES.size();

        for (Map<String, Double> map :
                CLASS_SCORES.values()) {

            total += map.size();
        }

        return total;
    }

    public static CardData getCardData(
            String cardName) {

        return cardName == null
                ? null
                : CARDS.get(
                normalize(
                        correctOcr(cardName)
                )
        );
    }

    public static int getCardManaCost(
            String cardName) {

        CardData c =
                getCardData(cardName);

        return c == null
                ? -1
                : getManaCost(c);
    }

    public static String getCardType(
            String cardName) {

        CardData c =
                getCardData(cardName);

        return c == null
                ? ""
                : getReadableType(c.type);
    }

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

    private static int getManaCost(
            CardData card) {

        return card == null
                ? -1
                : Math.max(
                0,
                card.cost
        );
    }

    private static String getReadableType(
            String type) {

        String n =
                normalizeType(type);

        if (n.equals("MINION")) {
            return "Minion";
        }

        if (n.equals("SPELL")) {
            return "Spell";
        }

        if (n.equals("WEAPON")) {
            return "Weapon";
        }

        if (n.equals("LOCATION")) {
            return "Location";
        }

        if (n.equals("HERO")) {
            return "Hero";
        }

        return type == null
                ? ""
                : type;
    }

    private static String normalize(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.US)
                .trim()
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String normalizeClass(
            String value) {

        if (value == null) {
            return "";
        }

        String result =
                value
                        .trim()
                        .toUpperCase(Locale.US)
                        .replace("_", " ")
                        .replace("-", " ")
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (result.equals(
                "DEATHKNIGHT")) {
            return "DEATH KNIGHT";
        }

        if (result.equals(
                "DEMONHUNTER")) {
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

    private static double clamp(
            double value,
            double min,
            double max) {

        return value < min
                ? min
                : (value > max
                ? max
                : value);
    }

    private static double roundScore(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    private static boolean containsAny(
            String text,
            String... values) {

        if (text == null ||
                text.isEmpty() ||
                values == null) {
            return false;
        }

        for (String value : values) {

            if (value != null &&
                    text.contains(value)) {
                return true;
            }
        }

        return false;
    }

    private static String readStream(
            InputStream input) throws Exception {

        StringBuilder b =
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

            b.append(line)
                    .append('\n');
        }

        reader.close();

        return b.toString();
    }

    private static int levenshtein(
            String a,
            String b) {

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
                                == b.charAt(j - 1)
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

    public static class CardData {

        public final String
                name,
                cardClass,
                type,
                rarity,
                id,
                text;

        public final int
                cost,
                attack,
                health;

        public final String
                race,
                spellSchool,
                mechanics;

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
                String mechanics) {

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

        public CardData(
                String name,
                String cardClass,
                String type,
                String rarity,
                String id,
                String text,
                int cost,
                int attack,
                int health) {

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

        public CardData(
                String name,
                String cardClass,
                String type,
                String rarity,
                String id) {

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

        public final String
                name,
                className;

        public CardInfo(
                String name,
                String className) {

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

    public static class DraftAnalysis {

        public final String cardName;

        public final double
                hearthArenaScore,
                curveAdjustment,
                typeAdjustment,
                synergyAdjustment,
                deckFitAdjustment,
                removalTempoAdjustment,
                duplicateAdjustment,
                finalScore;

        public final int manaCost;

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
                String reason) {

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

        public DraftAnalysis(
                String cardName,
                double hearthArenaScore,
                double curveAdjustment,
                double typeAdjustment,
                double synergyAdjustment,
                double duplicateAdjustment,
                double finalScore,
                int manaCost,
                String reason) {

            this(
                    cardName,
                    hearthArenaScore,
                    curveAdjustment,
                    typeAdjustment,
                    synergyAdjustment,
                    0,
                    0,
                    duplicateAdjustment,
                    finalScore,
                    manaCost,
                    reason
            );
        }
    }
}
