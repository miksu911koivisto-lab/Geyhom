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

    /*
     * ============================================================
     * CARD DATA
     * ============================================================
     */

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    private static final Map<String, String> CANONICAL_NAMES =
            new HashMap<>();

    private static final Map<String, String> ALIASES =
            new HashMap<>();

    private static final Map<String, CardInfo> CARD_INFO =
            new HashMap<>();

    /*
     * CLASS_SCORES:
     *
     * CLASS_SCORES.get("MAGE").get("ALTER TIME") -> 97.0
     *
     * HearthArena antaa eri luokille eri arvot.
     */
    private static final Map<String, Map<String, Double>> CLASS_SCORES =
            new HashMap<>();

    /*
     * Neutral / yleinen tierlist-arvo.
     */
    private static final Map<String, Double> NEUTRAL_SCORES =
            new HashMap<>();

    /*
     * Draftissa jo otetut kortit.
     */
    private static final Map<String, Integer> PICKED_CARDS =
            new HashMap<>();

    /*
     * ============================================================
     * STATUS
     * ============================================================
     */

    private static volatile boolean onlineLoaded = false;
    private static volatile boolean hearthArenaLoaded = false;
    private static volatile boolean hearthArenaLoading = false;

    private static volatile String status = "Käynnistetään...";
    private static volatile String reason = "";

    /*
     * ============================================================
     * CLASS DETECTION
     * ============================================================
     */

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

    /*
     * ============================================================
     * KNOWN FALLBACK SCORES
     * ============================================================
     *
     * Näitä käytetään vain jos HearthArena-dataa ei saada.
     * Ne eivät saa koskaan syrjäyttää ladattua HearthArena-arvoa.
     */

    private static final Map<String, Double> FALLBACK_SCORES =
            new HashMap<>();

    /*
     * ============================================================
     * STATIC INITIALIZATION
     * ============================================================
     */

    static {
        initializeClassMaps();
        initializeFallbackScores();
        initializeAliases();

        loadCards();
        loadHearthArenaScores();
    }

    /*
     * ============================================================
     * CLASS MAP INITIALIZATION
     * ============================================================
     */

    private static void initializeClassMaps() {
        for (String className : VALID_CLASSES) {
            CLASS_SCORES.put(className, new HashMap<>());
        }
    }

    /*
     * ============================================================
     * FALLBACK SCORES
     * ============================================================
     */

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

    /*
     * ============================================================
     * OCR ALIASES
     * ============================================================
     */

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

    private static void addAlias(String bad, String correct) {
        ALIASES.put(
                normalize(bad),
                correct
        );
    }

    /*
     * ============================================================
     * HEARTHSTONEJSON CARD DATABASE
     * ============================================================
     */

    private static void loadCards() {

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

                status = "Ladataan korttitietoja...";

                URL url = new URL(CARDS_URL);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);

                connection.setRequestProperty(
                        "User-Agent",
                        "ArenaHelper/1.0"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
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

                for (int i = 0; i < array.length(); i++) {

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

                    CardData data =
                            new CardData(
                                    name,
                                    cardClass,
                                    type,
                                    rarity,
                                    id
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

                onlineLoaded = count > 0;

                if (onlineLoaded) {
                    status =
                            "Korttitiedot ladattu: "
                                    + count;
                }

                Log.d(
                        TAG,
                        "HearthstoneJSON cards: "
                                + count
                );

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

    /*
     * ============================================================
     * HEARTHARENA LOADING
     * ============================================================
     */

    private static synchronized void loadHearthArenaScores() {

        if (hearthArenaLoading) {
            return;
        }

        hearthArenaLoading = true;

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

                status =
                        "Ladataan HearthArena-arvoja...";

                URL url =
                        new URL(
                                HEARTHARENA_URL
                        );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod(
                        "GET"
                );

                connection.setConnectTimeout(
                        15000
                );

                connection.setReadTimeout(
                        20000
                );

                /*
                 * HearthArena voi käyttäytyä eri tavalla
                 * ilman tavallista selaimen User-Agentia.
                 */
                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 16) " +
                                "AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) " +
                                "Chrome/140.0 Mobile Safari/537.36"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml"
                );

                connection.setRequestProperty(
                        "Accept-Language",
                        "en-US,en;q=0.9"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {

                    throw new Exception(
                            "HearthArena HTTP "
                                    + responseCode
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

                /*
                 * Ensin yritetään DOM/HTML-tyyppistä
                 * rakennetta.
                 */
                int parsed =
                        parseHearthArenaHtml(
                                html
                        );

                /*
                 * Jos ensimmäinen parseri ei löytänyt
                 * riittävästi rivejä, käytetään vielä
                 * raakatekstiparseria.
                 */
                if (parsed < 5) {

                    parsed =
                            parseHearthArenaText(
                                    html
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

        /*
         * Muutetaan HTML luettavaksi tekstiksi.
         *
         * Tärkeää:
         * emme poista kaikkea suoraan yhdeksi
         * merkkijonoksi, koska korttinimi ja score
         * ovat HearthArenassa usein eri DOM-riveillä.
         */
        String text =
                htmlToText(
                        html
                );

        return parseHearthArenaText(
                text
        );
    }

    /*
     * ============================================================
     * TEKSTIPARSER
     * ============================================================
     *
     * HearthArenan nykyinen sivu näkyy käytännössä
     * esimerkiksi:
     *
     * Mage
     *
     * 1. Alter Time
     * 97
     *
     * 2. Merry Moonkin
     * 69
     *
     * Parseri ei enää oleta tiettyä HTML-tagia.
     */

    private static int parseHearthArenaText(
            String input
    ) {

        if (input == null ||
                input.isEmpty()) {

            return 0;
        }

        String text =
                htmlToText(
                        input
                );

        String[] lines =
                text.split("\\n");

        String activeClass = "";

        int parsed = 0;

        Set<String> seen =
                new HashSet<>();

        for (int i = 0;
             i < lines.length;
             i++) {

            String line =
                    cleanLine(
                            lines[i]
                    );

            if (line.isEmpty()) {
                continue;
            }

            /*
             * Tunnista class-otsikko.
             */
            String detectedClass =
                    detectClassHeader(
                            line
                    );

            if (!detectedClass.isEmpty()) {

                activeClass =
                        detectedClass;

                continue;
            }

            /*
             * Neutral-osio.
             */
            if (normalize(line).equals(
                    normalize("Neutral")
            )) {

                activeClass =
                        "NEUTRAL";

                continue;
            }

            if (activeClass.isEmpty()) {
                continue;
            }

            /*
             * Poimitaan korttinimi tästä rivistä.
             *
             * HearthArena käyttää usein:
             *
             * 1. Card Name
             *
             * joten ranking-numero poistetaan.
             */
            String possibleName =
                    removeRankingPrefix(
                            line
                    );

            possibleName =
                    removeTierNoise(
                            possibleName
                    );

            if (possibleName.isEmpty()) {
                continue;
            }

            /*
             * Seuraavilta riveiltä etsitään score.
             *
             * Ensisijaisesti seuraava rivi.
             */
            Double score =
                    findFollowingScore(
                            lines,
                            i
                    );

            if (score == null) {
                continue;
            }

            /*
             * Varmistetaan, että kyseessä on
             * oikea Hearthstone-kortti.
             */
            String canonical =
                    findCanonicalCardName(
                            possibleName
                    );

            if (canonical == null) {
                continue;
            }

            String key =
                    normalize(
                            canonical
                    );

            /*
             * Älä tallenna samaa korttia
             * uudelleen saman classin sisällä.
             */
            String seenKey =
                    activeClass
                            + "|"
                            + key;

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
                        CLASS_SCORES.get(
                                activeClass
                        );

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
        }

        return parsed;
    }

    /*
     * ============================================================
     * FIND SCORE AFTER CARD NAME
     * ============================================================
     */

    private static Double findFollowingScore(
            String[] lines,
            int start
    ) {

        /*
         * HearthArena:ssa nimi ja arvo ovat yleensä
         * välittömästi peräkkäin.
         *
         * Katsotaan kuitenkin muutama rivi eteenpäin,
         * koska HTML:n tekstimuunnos voi lisätä tyhjiä
         * rivejä.
         */
        int max =
                Math.min(
                        lines.length,
                        start + 5
                );

        for (int i = start + 1;
             i < max;
             i++) {

            String value =
                    cleanLine(
                            lines[i]
                    );

            if (value.isEmpty()) {
                continue;
            }

            /*
             * Jos seuraava rivi näyttää uudelta kortilta,
             * lopetetaan tämän kortin etsintä.
             */
            String possibleNext =
                    removeRankingPrefix(
                            value
                    );

            if (looksLikeCardLine(
                    possibleNext
            )) {

                return null;
            }

            Double parsed =
                    parseScore(
                            value
                    );

            if (parsed != null) {

                /*
                 * HearthArena-arvot ovat normaalisti
                 * välillä 0-150.
                 */
                if (parsed >= 0 &&
                        parsed <= 150) {

                    return parsed;
                }
            }
        }

        return null;
    }

    /*
     * ============================================================
     * SCORE PARSING
     * ============================================================
     */

    private static Double parseScore(
            String text
    ) {

        if (text == null) {
            return null;
        }

        String value =
                text.trim()
                        .replace(
                                "↓",
                                ""
                        )
                        .replace(
                                "↑",
                                ""
                        )
                        .trim();

        /*
         * Vain puhdas numero.
         */
        if (!value.matches(
                "\\d+(?:[\\.,]\\d+)?"
        )) {

            return null;
        }

        try {

            return Double.parseDouble(
                    value.replace(
                            ',',
                            '.'
                    )
            );

        } catch (Exception e) {

            return null;
        }
    }

    /*
     * ============================================================
     * CANONICAL CARD MATCHING
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
                normalize(
                        input
                );

        /*
         * Suora osuma.
         */
        String exact =
                CANONICAL_NAMES.get(
                        normalized
                );

        if (exact != null) {
            return exact;
        }

        /*
         * Alias.
         */
        String alias =
                ALIASES.get(
                        normalized
                );

        if (alias != null) {
            return alias;
        }

        /*
         * New / suffix -ongelmat:
         *
         * "Mathias Shaw New"
         * "Captain Crowley New"
         *
         * HearthArena käyttää näitä merkintöjä
         * omassa tierlist-tekstissään.
         */
        String withoutNew =
                normalized
                        .replace(
                                " new",
                                ""
                        )
                        .trim();

        exact =
                CANONICAL_NAMES.get(
                        withoutNew
                );

        if (exact != null) {
            return exact;
        }

        /*
         * Fuzzy matching.
         *
         * Tätä käytetään vain lyhyelle etäisyydelle,
         * jotta emme yhdistä kahta eri korttia väärin.
         */
        int bestDistance =
                Integer.MAX_VALUE;

        String best =
                null;

        for (Map.Entry<String, String> entry
                : CANONICAL_NAMES.entrySet()) {

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

        /*
         * Script/style pois.
         */
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

        /*
         * Rivinvaihtoa aiheuttavat tagit.
         */
        text =
                text.replaceAll(
                        "(?i)</(div|p|li|ul|ol|h1|h2|h3|h4|h5|h6|tr|td|th|section|article|br)>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                );

        /*
         * Kaikki muut tagit pois.
         */
        text =
                text.replaceAll(
                        "(?s)<[^>]+>",
                        " "
                );

        /*
         * HTML-entiteetit.
         */
        text =
                decodeHtmlEntities(
                        text
                );

        /*
         * CRLF -> LF.
         */
        text =
                text.replace(
                        "\r",
                        ""
                );

        /*
         * Useita välilyöntejä yhdeksi.
         */
        text =
                text.replaceAll(
                        "[\\t ]+",
                        " "
                );

        /*
         * Tyhjät rivit siedettäviksi.
         */
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
                .replace(
                        "&nbsp;",
                        " "
                )
                .replace(
                        "&#39;",
                        "'"
                )
                .replace(
                        "&apos;",
                        "'"
                )
                .replace(
                        "&quot;",
                        "\""
                )
                .replace(
                        "&amp;",
                        "&"
                )
                .replace(
                        "&lt;",
                        "<"
                )
                .replace(
                        "&gt;",
                        ">"
                );
    }

    /*
     * ============================================================
     * CLASS HEADER
     * ============================================================
     */

    private static String detectClassHeader(
            String line
    ) {

        String normalized =
                normalizeClass(
                        line
                );

        for (String valid
                : VALID_CLASSES) {

            if (normalized.equals(
                    valid
            )) {

                return valid;
            }
        }

        return "";
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

        /*
         * "1. Alter Time"
         * "1) Alter Time"
         * "01. Alter Time"
         */
        result =
                result.replaceFirst(
                        "^\\d+\\s*[\\.\\)]\\s*",
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

        String result =
                name.trim();

        /*
         * Jos tekstimuunnos sisältää nuolen.
         */
        result =
                result.replace(
                        "↓",
                        ""
                )
                .replace(
                        "↑",
                        ""
                )
                .trim();

        return result;
    }

    private static boolean looksLikeCardLine(
            String line
    ) {

        if (line == null ||
                line.isEmpty()) {

            return false;
        }

        String normalized =
                normalize(
                        line
                );

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

        return false;
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
                normalize(
                        input
                );

        /*
         * Alias ensin.
         */
        String alias =
                ALIASES.get(
                        normalized
                );

        if (alias != null) {
            return alias;
        }

        /*
         * Täsmälleen tunnettu kortti.
         */
        String canonical =
                CANONICAL_NAMES.get(
                        normalized
                );

        if (canonical != null) {
            return canonical;
        }

        /*
         * CARD_INFO voi sisältää nimen vaikka
         * CANONICAL_NAMES ei olisi vielä täyttynyt.
         */
        CardInfo info =
                CARD_INFO.get(
                        normalized
                );

        if (info != null) {
            return info.name;
        }

        /*
         * Fuzzy.
         */
        String best =
                findBestOcrMatch(
                        normalized
                );

        if (best != null) {
            return best;
        }

        /*
         * Jos verkkodata ei ole vielä ehtinyt,
         * tarkista fallback-nimet.
         */
        for (String fallback
                : FALLBACK_SCORES.keySet()) {

            if (levenshtein(
                    normalized,
                    fallback
            ) <= 3) {

                String fallbackName =
                        CANONICAL_NAMES.get(
                                fallback
                        );

                if (fallbackName != null) {
                    return fallbackName;
                }
            }
        }

        /*
         * Palautetaan alkuperäinen nimi.
         * Tämä on tärkeää OCR:n kannalta:
         * emme halua muuttaa toimivaa tunnistusta
         * väkisin.
         */
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

        for (Map.Entry<String, String> entry
                : CANONICAL_NAMES.entrySet()) {

            String candidate =
                    entry.getKey();

            if (candidate.isEmpty()) {
                continue;
            }

            /*
             * Ensimmäinen merkki eri -> usein väärä
             * kortti, jos nimet ovat pitkiä.
             */
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
     * SCORE
     * ============================================================
     */

    public static double score(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        String key =
                normalize(
                        corrected
                );

        /*
         * 1. Class-specific HearthArena score.
         *
         * TÄMÄ ON ENSISIJAINEN.
         */
        double classScore =
                getClassSpecificScore(
                        key
                );

        if (classScore >= 0) {

            reason =
                    "HearthArena "
                            + currentClass;

            return adjustForDraft(
                    key,
                    classScore
            );
        }

        /*
         * 2. Neutral HearthArena score.
         */
        Double neutral =
                NEUTRAL_SCORES.get(
                        key
                );

        if (neutral != null) {

            reason =
                    "HearthArena Neutral";

            return adjustForDraft(
                    key,
                    neutral
            );
        }

        /*
         * 3. Tunnettu fallback.
         */
        Double fallback =
                FALLBACK_SCORES.get(
                        key
                );

        if (fallback != null) {

            reason =
                    "Paikallinen fallback";

            return adjustForDraft(
                    key,
                    fallback
            );
        }

        /*
         * 4. Card metadata -pohjainen kevyt fallback.
         */
        CardInfo info =
                CARD_INFO.get(
                        key
                );

        if (info != null) {

            double generated =
                    generateFallbackScore(
                            key,
                            info
                    );

            reason =
                    "Generoitua fallback-arvoa";

            return adjustForDraft(
                    key,
                    generated
            );
        }

        /*
         * Tuntematon kortti.
         */
        reason =
                "Korttia ei tunnistettu";

        return UNKNOWN_CARD_SCORE;
    }

    /*
     * ============================================================
     * GET CLASS SPECIFIC SCORE
     * ============================================================
     */

    public static double getClassSpecificScore(
            String cardName
    ) {

        if (cardName == null) {
            return -1;
        }

        String key =
                normalize(
                        correctOcr(
                                cardName
                        )
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
                CLASS_SCORES.get(
                        cls
                );

        if (scores == null) {
            return -1;
        }

        Double value =
                scores.get(
                        key
                );

        if (value == null) {
            return -1;
        }

        return value;
    }

    /*
     * ============================================================
     * PUBLIC SCORE DISPLAY
     * ============================================================
     */

    public static String getCardScore(
            String cardName
    ) {

        double value =
                score(
                        cardName
                );

        if (value <= 0) {
            return "?";
        }

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    public static double getCardScoreValue(
            String cardName
    ) {

        return score(
                cardName
        );
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

        double s1 =
                score(card1);

        double s2 =
                score(card2);

        double s3 =
                score(card3);

        double best =
                s1;

        String recommendation =
                correctOcr(
                        card1
                );

        if (s2 > best) {

            best = s2;

            recommendation =
                    correctOcr(
                            card2
                    );
        }

        if (s3 > best) {

            best = s3;

            recommendation =
                    correctOcr(
                            card3
                    );
        }

        if (best <= 0) {

            reason =
                    "Ei tunnistettavaa korttia";

            return "EI TUNNISTETTAVAA";
        }

        reason =
                "Paras arvo: "
                        + String.format(
                        Locale.US,
                        "%.2f",
                        best
                );

        return recommendation;
    }

    /*
     * ============================================================
     * PICKED CARD TRACKING
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
                correctOcr(
                        cardName
                );

        String key =
                normalize(
                        corrected
                );

        if (key.isEmpty()) {
            return;
        }

        Integer count =
                PICKED_CARDS.get(
                        key
                );

        if (count == null) {
            count = 0;
        }

        PICKED_CARDS.put(
                key,
                count + 1
        );
    }

    public static synchronized void clearPickedCards() {
        PICKED_CARDS.clear();
    }

    public static synchronized int getPickedCount(
            String cardName
    ) {

        if (cardName == null) {
            return 0;
        }

        String key =
                normalize(
                        correctOcr(
                                cardName
                        )
                );

        Integer count =
                PICKED_CARDS.get(
                        key
                );

        return count == null
                ? 0
                : count;
    }

    /*
     * ============================================================
     * DRAFT BONUS / PENALTY
     * ============================================================
     *
     * HearthArena "↓" tarkoittaa, että score voi pudota
     * ensimmäisen kopion jälkeen.
     *
     * Me pidetään vaikutus pienenä, jotta varsinainen
     * HearthArena-score pysyy tärkeimpänä.
     */

    private static double adjustForDraft(
            String key,
            double base
    ) {

        Integer picked =
                PICKED_CARDS.get(
                        key
                );

        if (picked == null ||
                picked <= 0) {

            return roundScore(
                    base
            );
        }

        /*
         * Kolmannen+ kopion jälkeen pieni vähennys.
         */
        if (picked >= 2) {

            base -= 0.75;
        }

        return roundScore(
                base
        );
    }

    /*
     * ============================================================
     * FALLBACK SCORE GENERATOR
     * ============================================================
     */

    private static double generateFallbackScore(
            String key,
            CardInfo info
    ) {

        /*
         * Erityiset tunnetut kortit.
         */
        Double known =
                FALLBACK_SCORES.get(
                        key
                );

        if (known != null) {
            return known;
        }

        /*
         * Tuntemattomalle mutta oikealle kortille
         * neutraali keskitaso.
         *
         * Tämä ei saa voittaa oikeaa HearthArena-arvoa.
         */
        double value =
                5.00;

        String name =
                info.name.toLowerCase(
                        Locale.US
                );

        /*
         * Kevyet tekstipohjaiset heuristiikat.
         */
        if (name.contains("legend")) {
            value += 0.15;
        }

        if (name.contains("dragon")) {
            value += 0.05;
        }

        if (name.contains("knight")) {
            value += 0.05;
        }

        if (name.contains("champion")) {
            value += 0.05;
        }

        return clamp(
                value,
                0.1,
                9.0
        );
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

        /*
         * Jos saman tarjouksen korteista löytyy
         * kaksi eri classia, ensimmäistä ei lukita.
         */
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

        if (detected.equals(
                candidateClass
        )) {

            candidateClassCount++;

        } else {

            candidateClass =
                    detected;

            candidateClassCount = 1;
        }

        if (candidateClassCount
                >= CLASS_CONFIRMATIONS) {

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
                    getClassForCard(
                            card
                    );

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
                    getClassForCard(
                            card
                    );

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
                correctOcr(
                        cardName
                );

        String key =
                normalize(
                        corrected
                );

        CardInfo info =
                CARD_INFO.get(
                        key
                );

        if (info == null) {
            return "";
        }

        String cls =
                normalizeClass(
                        info.className
                );

        /*
         * Neutral ei kerro pelaajan luokkaa.
         */
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
     * MANUAL CLASS SCORE API
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
                        correctOcr(
                                cardName
                        )
                );

        String cls =
                normalizeClass(
                        className
                );

        if (cardKey.isEmpty() ||
                cls.isEmpty()) {

            return;
        }

        Map<String, Double> scores =
                CLASS_SCORES.get(
                        cls
                );

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
    }

    public static synchronized void clearClassScores() {

        for (Map<String, Double> map
                : CLASS_SCORES.values()) {

            map.clear();
        }

        NEUTRAL_SCORES.clear();

        hearthArenaLoaded = false;
    }

    public static int getClassScoreCount() {

        int total = 0;

        for (Map<String, Double> map
                : CLASS_SCORES.values()) {

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
                        correctOcr(
                                cardName
                        )
                );

        return CARDS.get(
                key
        );
    }

    /*
     * ============================================================
     * STATUS API
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
                        .toLowerCase(
                                Locale.US
                        )
                        .trim();

        /*
         * Yhdistetään OCR:n erottamat
         * välilyönnit.
         */
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
                        .toUpperCase(
                                Locale.US
                        );

        result =
                result.replace(
                        "_",
                        " "
                );

        result =
                result.replace(
                        "-",
                        " "
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        /*
         * HearthstoneJSON voi käyttää:
         * DEATHKNIGHT
         * DEMONHUNTER
         */
        if (result.equals(
                "DEATHKNIGHT"
        )) {

            return "DEATH KNIGHT";
        }

        if (result.equals(
                "DEMONHUNTER"
        )) {

            return "DEMON HUNTER";
        }

        if (result.equals(
                "DEATH KNIGHT"
        )) {

            return "DEATH KNIGHT";
        }

        if (result.equals(
                "DEMON HUNTER"
        )) {

            return "DEMON HUNTER";
        }

        for (String valid
                : VALID_CLASSES) {

            if (result.equals(
                    valid
            )) {

                return valid;
            }
        }

        if (result.equals(
                "NEUTRAL"
        )) {

            return "NEUTRAL";
        }

        return result;
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

    /*
     * ============================================================
     * UTILITIES
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
     * CARD DATA CLASSES
     * ============================================================
     */

    public static class CardData {

        public final String name;
        public final String cardClass;
        public final String type;
        public final String rarity;
        public final String id;

        public CardData(
                String name,
                String cardClass,
                String type,
                String rarity,
                String id
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
}
