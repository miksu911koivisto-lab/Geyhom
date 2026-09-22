package com.blessedmike.arenahelper;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArenaAdvisor {

    private static final String HEARTHARENA_URL =
            "https://www.heartharena.com/tierlist";

    private static final double HEARTHARENA_SCALE = 13.0;

    private static final double UNKNOWN_CARD_SCORE = 0.0;

    private static final Map<String, Double> CARDS =
            new HashMap<>();

    private static final Map<String, String> OCR_ALIASES =
            new HashMap<>();

    private static final Set<String> ONLINE_NAMES =
            new HashSet<>();

    private static final Set<String> PICKED_CARDS =
            new HashSet<>();

    private static final Map<String, Double> ONLINE_RAW_SCORES =
            new HashMap<>();

    private static final Set<String> ONLINE_CARD_NAMES =
            new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static volatile boolean onlineLoaded = false;

    // ================================================================
    // FALLBACK-KORTIT
    // ================================================================

    static {

        addCard("Soldier of the Infinite", 8.20);
        addCard("Crystallized Leyline", 5.65);
        addCard("Surge Needle", 5.85);
        addCard("Bursting Leyline", 6.50);
        addCard("Contraband Wands", 6.30);
        addCard("Cold Snap", 7.50);
        addCard("Code Violet", 7.20);
        addCard("Ley Walker", 5.50);
        addCard("Leyline Nexus", 6.00);
        addCard("Raban Wands", 6.50);
        addCard("Spellweaver's Brilliance", 3.92);
        addCard("Windswept Pageturner", 5.23);
        addCard("Spirit Gatherer", 7.69);

        addRawCard("Vault Breaker", 62);
        addRawCard("Scorching Winds", 48);
        addRawCard("Platysaur", 67);
        addRawCard("Sizzling Cinder", 69);
        addRawCard("Relic Miner", 66);
        addRawCard("Temporal Traveler", 66);
        addRawCard("Hourglass Attendant", 65);
        addRawCard("Sewer Imp", 65);
        addRawCard("Critter Caretaker", 64);
        addRawCard("Drakeadon Mongrel", 64);
        addRawCard("Scalehide Kodo", 67);
        addRawCard("Living Paradox", 69);
        addRawCard("Battlefield Blaster", 69);
        addRawCard("Gemstone Hoarder", 71);
        addRawCard("Ancient Stegodon", 70);
        addRawCard("Briarspawn Drake", 70);
        addRawCard("Dreambound Raptor", 70);
        addRawCard("Whirling Stormdrake", 70);
        addRawCard("Willful Watcher", 70);
        addRawCard("Gnawing Greenfin", 73);
        addRawCard("Aeon Wizard", 73);
        addRawCard("Rockskipper", 74);
        addRawCard("Undercover Cultist", 74);
        addRawCard("Fae Trickster", 74);
        addRawCard("Daydreaming Pixie", 76);
        addRawCard("Flutterwing Guardian", 76);
        addRawCard("Quantum Destabilizer", 76);
        addRawCard("Mother Duck", 77);
        addRawCard("Defias Smuggler", 79);

        // ============================================================
        // OCR-ALIAKSET
        // ============================================================

        addAlias(
                "soldier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of infinit",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of the infinit",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of ihfini",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of infinit",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "raban wands",
                "Raban Wands"
        );

        addAlias(
                "crystallized leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "crystallised leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "surge needle",
                "Surge Needle"
        );

        addAlias(
                "spellweaver's brilliance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "spellweavers brilliance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "windswept pageturner",
                "Windswept Pageturner"
        );

        addAlias(
                "spirit gatherer",
                "Spirit Gatherer"
        );

        addAlias(
                "vault breaker",
                "Vault Breaker"
        );

        addAlias(
                "scorching winds",
                "Scorching Winds"
        );

        addAlias(
                "platysaur",
                "Platysaur"
        );

        loadOnlineData();
    }

    private static void addCard(
            String name,
            double score
    ) {
        CARDS.put(
                normalize(name),
                score
        );
    }

    private static void addRawCard(
            String name,
            int rawScore
    ) {
        addCard(
                name,
                rawScore / HEARTHARENA_SCALE
        );
    }

    private static void addAlias(
            String from,
            String to
    ) {
        OCR_ALIASES.put(
                normalize(from),
                to
        );
    }

    // ================================================================
    // ONLINE-DATA
    // ================================================================

    private static void loadOnlineData() {

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url =
                        new URL(HEARTHARENA_URL);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("GET");

                connection.setConnectTimeout(10000);

                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Android) ArenaHelper"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode < 200 ||
                        responseCode >= 300) {

                    return;
                }

                InputStream inputStream =
                        connection.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder html =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {

                    html.append(line)
                            .append('\n');
                }

                reader.close();

                parseHearthArenaPage(
                        html.toString()
                );

                synchronized (CARDS) {

                    for (Map.Entry<String, Double> entry
                            : ONLINE_RAW_SCORES.entrySet()) {

                        double value =
                                entry.getValue()
                                        / HEARTHARENA_SCALE;

                        if (value > 0.0) {

                            CARDS.put(
                                    entry.getKey(),
                                    value
                            );
                        }
                    }
                }

                onlineLoaded = true;

            } catch (Exception ignored) {

                // Fallback-arvot jäävät käyttöön.

            } finally {

                if (connection != null) {

                    connection.disconnect();
                }
            }
        });
    }

    private static void parseHearthArenaPage(
            String html
    ) {

        if (html == null ||
                html.isEmpty()) {

            return;
        }

        String text =
                stripHtml(html);

        if (text.isEmpty()) {
            return;
        }

        text =
                text.replace(
                        "|",
                        "\n"
                );

        String[] lines =
                text.split("\\r?\\n");

        String previousCard = null;

        for (String rawLine : lines) {

            if (rawLine == null) {
                continue;
            }

            String line =
                    cleanLine(rawLine);

            if (line.isEmpty()) {
                continue;
            }

            CardScore sameLine =
                    splitCardAndScore(line);

            if (sameLine != null) {

                if (isLikelyCardName(
                        sameLine.name
                )) {

                    storeOnlineCard(
                            sameLine.name,
                            sameLine.score
                    );

                    previousCard =
                            sameLine.name;
                }

                continue;
            }

            if (isLikelyCardName(line)) {

                previousCard = line;

                continue;
            }

            if (previousCard != null) {

                Double score =
                        parseScore(line);

                if (score != null &&
                        score > 0.0 &&
                        score <= 200.0) {

                    storeOnlineCard(
                            previousCard,
                            score
                    );

                    previousCard = null;
                }
            }
        }
    }

    private static void storeOnlineCard(
            String name,
            double score
    ) {

        if (name == null ||
                name.isEmpty()) {

            return;
        }

        if (score <= 0.0 ||
                score > 200.0) {

            return;
        }

        String normalized =
                normalize(name);

        if (normalized.isEmpty()) {
            return;
        }

        synchronized (ONLINE_RAW_SCORES) {

            Double old =
                    ONLINE_RAW_SCORES.get(
                            normalized
                    );

            if (old == null ||
                    score > old) {

                ONLINE_RAW_SCORES.put(
                        normalized,
                        score
                );
            }
        }

        ONLINE_NAMES.add(
                normalized
        );

        ONLINE_CARD_NAMES.add(
                normalized
        );
    }

    private static CardScore splitCardAndScore(
            String line
    ) {

        if (line == null) {
            return null;
        }

        String clean =
                cleanLine(line);

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        "^(.+?)\\s+(\\d+(?:\\.\\d+)?)$"
                ).matcher(clean);

        if (!matcher.find()) {
            return null;
        }

        String name =
                cleanLine(
                        matcher.group(1)
                );

        String scoreText =
                matcher.group(2);

        if (!isLikelyCardName(name)) {
            return null;
        }

        try {

            double score =
                    Double.parseDouble(
                            scoreText
                    );

            return new CardScore(
                    name,
                    score
            );

        } catch (Exception ignored) {

            return null;
        }
    }

    private static Double parseScore(
            String line
    ) {

        if (line == null) {
            return null;
        }

        String clean =
                cleanLine(line);

        clean =
                clean.replace(
                        "↓",
                        ""
                );

        clean =
                clean.replace(
                        "↑",
                        ""
                );

        if (!clean.matches(
                "\\d+(?:\\.\\d+)?"
        )) {

            return null;
        }

        try {

            return Double.parseDouble(
                    clean
            );

        } catch (Exception ignored) {

            return null;
        }
    }

    private static boolean isLikelyCardName(
            String name
    ) {

        if (name == null) {
            return false;
        }

        String value =
                cleanLine(name);

        if (value.length() < 2 ||
                value.length() > 100) {

            return false;
        }

        String lower =
                value.toLowerCase(
                        Locale.US
                );

        String[] ignored = {

                "great",
                "good",
                "above average",
                "average",
                "below average",
                "bad",
                "terrible",

                "paladin",
                "mage",
                "warrior",
                "warlock",
                "priest",
                "rogue",
                "hunter",
                "druid",
                "shaman",
                "death knight",
                "demon hunter",

                "card",
                "cards",
                "class",
                "score",
                "before",
                "after",
                "change",

                "heartharena tier list",
                "tier list",
                "tierlist"
        };

        for (String ignoredWord : ignored) {

            if (lower.equals(
                    ignoredWord
            )) {

                return false;
            }
        }

        if (lower.matches(
                "\\d+(?:\\.\\d+)?"
        )) {

            return false;
        }

        if (lower.matches(
                "[+-]?\\d+(?:\\.\\d+)?"
        )) {

            return false;
        }

        return true;
    }

    private static String stripHtml(
            String html
    ) {

        String result = html;

        result =
                result.replaceAll(
                        "(?is)<script.*?>.*?</script>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?is)<style.*?>.*?</style>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</tr>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</td>",
                        " | "
                );

        result =
                result.replaceAll(
                        "(?i)</th>",
                        " | "
                );

        result =
                result.replaceAll(
                        "<[^>]+>",
                        " "
                );

        return decodeHtmlEntities(
                result
        );
    }

    private static String decodeHtmlEntities(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result = text;

        result =
                result.replace(
                        "&amp;",
                        "&"
                );

        result =
                result.replace(
                        "&quot;",
                        "\""
                );

        result =
                result.replace(
                        "&#39;",
                        "'"
                );

        result =
                result.replace(
                        "&apos;",
                        "'"
                );

        result =
                result.replace(
                        "&nbsp;",
                        " "
                );

        result =
                result.replace(
                        "&ndash;",
                        "-"
                );

        result =
                result.replace(
                        "&mdash;",
                        "-"
                );

        result =
                result.replace(
                        "&rsquo;",
                        "'"
                );

        result =
                result.replace(
                        "&lsquo;",
                        "'"
                );

        result =
                result.replace(
                        "&rdquo;",
                        "\""
                );

        result =
                result.replace(
                        "&ldquo;",
                        "\""
                );

        java.util.regex.Matcher decimal =
                java.util.regex.Pattern.compile(
                        "&#(\\d+);"
                ).matcher(result);

        StringBuffer decimalBuffer =
                new StringBuffer();

        while (decimal.find()) {

            try {

                int code =
                        Integer.parseInt(
                                decimal.group(1)
                        );

                decimal.appendReplacement(
                        decimalBuffer,
                        java.util.regex.Matcher
                                .quoteReplacement(
                                        String.valueOf(
                                                (char) code
                                        )
                                )
                );

            } catch (Exception ignored) {

                decimal.appendReplacement(
                        decimalBuffer,
                        java.util.regex.Matcher
                                .quoteReplacement(
                                        decimal.group()
                                )
                );
            }
        }

        decimal.appendTail(
                decimalBuffer
        );

        result =
                decimalBuffer.toString();

        java.util.regex.Matcher hex =
                java.util.regex.Pattern.compile(
                        "&#x([0-9a-fA-F]+);"
                ).matcher(result);

        StringBuffer hexBuffer =
                new StringBuffer();

        while (hex.find()) {

            try {

                int code =
                        Integer.parseInt(
                                hex.group(1),
                                16
                        );

                hex.appendReplacement(
                        hexBuffer,
                        java.util.regex.Matcher
                                .quoteReplacement(
                                        String.valueOf(
                                                (char) code
                                        )
                                )
                );

            } catch (Exception ignored) {

                hex.appendReplacement(
                        hexBuffer,
                        java.util.regex.Matcher
                                .quoteReplacement(
                                        hex.group()
                                )
                );
            }
        }

        hex.appendTail(
                hexBuffer
        );

        return hexBuffer.toString();
    }

    // ================================================================
    // OCR-KORJAUS
    // ================================================================

    public static String correctOcr(
            String ocrText
    ) {

        if (ocrText == null) {
            return "";
        }

        String cleaned =
                cleanCardName(ocrText);

        if (cleaned.isEmpty()) {
            return "";
        }

        String normalized =
                normalize(cleaned);

        // Soldier of the Infinite
        if (normalized.contains(
                "soldier of the infinite"
        ) ||
                normalized.contains(
                        "soldier of infinit"
                ) ||
                normalized.contains(
                        "soldier of ihfini"
                ) ||
                normalized.contains(
                        "sotdier of infinit"
                )) {

            return "Soldier of the Infinite";
        }

        // OCR-alias
        String alias =
                OCR_ALIASES.get(
                        normalized
                );

        if (alias != null) {
            return alias;
        }

        // Tarkka paikallinen osuma
        synchronized (CARDS) {

            if (CARDS.containsKey(
                    normalized
            )) {

                return keyToDisplayName(
                        normalized
                );
            }
        }

        // Tarkka online-osuma
        if (ONLINE_NAMES.contains(
                normalized
        )) {

            return keyToDisplayName(
                    normalized
            );
        }

        // Prefix
        String prefix =
                findPrefixMatch(
                        normalized
                );

        if (prefix != null) {

            return keyToDisplayName(
                    prefix
            );
        }

        // Fuzzy paikalliset
        String fuzzy =
                findBestLocalMatch(
                        normalized
                );

        if (fuzzy != null) {

            return keyToDisplayName(
                    fuzzy
            );
        }

        // Fuzzy online
        fuzzy =
                findBestOnlineMatch(
                        normalized
                );

        if (fuzzy != null) {

            return keyToDisplayName(
                    fuzzy
            );
        }

        return cleaned;
    }

    private static String findPrefixMatch(
            String normalized
    ) {

        if (normalized.length() < 5) {
            return null;
        }

        synchronized (CARDS) {

            for (String key :
                    CARDS.keySet()) {

                if (key.startsWith(
                        normalized
                ) ||
                        normalized.startsWith(
                                key
                        )) {

                    return key;
                }
            }
        }

        synchronized (ONLINE_NAMES) {

            for (String key :
                    ONLINE_NAMES) {

                if (key.startsWith(
                        normalized
                ) ||
                        normalized.startsWith(
                                key
                        )) {

                    return key;
                }
            }
        }

        return null;
    }

    private static String findBestLocalMatch(
            String input
    ) {

        if (input == null ||
                input.length() < 4) {

            return null;
        }

        String best = null;

        int bestDistance =
                Integer.MAX_VALUE;

        synchronized (CARDS) {

            for (String candidate :
                    CARDS.keySet()) {

                int distance =
                        levenshtein(
                                input,
                                candidate
                        );

                int allowed =
                        Math.max(
                                2,
                                candidate.length() / 4
                        );

                if (distance <= allowed &&
                        distance < bestDistance) {

                    bestDistance =
                            distance;

                    best = candidate;
                }
            }
        }

        return best;
    }

    private static String findBestOnlineMatch(
            String input
    ) {

        if (input == null ||
                input.length() < 4) {

            return null;
        }

        String best = null;

        int bestDistance =
                Integer.MAX_VALUE;

        synchronized (ONLINE_NAMES) {

            for (String candidate :
                    ONLINE_NAMES) {

                int distance =
                        levenshtein(
                                input,
                                candidate
                        );

                int allowed =
                        Math.max(
                                2,
                                candidate.length() / 4
                        );

                if (distance <= allowed &&
                        distance < bestDistance) {

                    bestDistance =
                            distance;

                    best = candidate;
                }
            }
        }

        return best;
    }

    private static int levenshtein(
            String a,
            String b
    ) {

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
                        a.charAt(i - 1) ==
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

            int[] temp =
                    previous;

            previous =
                    current;

            current =
                    temp;
        }

        return previous[b.length()];
    }

    // ================================================================
    // SCORE
    // ================================================================

    public static double score(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(cardName);

        String normalized =
                normalize(corrected);

        synchronized (CARDS) {

            Double value =
                    CARDS.get(
                            normalized
                    );

            if (value != null) {
                return value;
            }
        }

        String online =
                findBestOnlineMatch(
                        normalized
                );

        if (online != null) {

            synchronized (CARDS) {

                Double value =
                        CARDS.get(
                                online
                        );

                if (value != null) {
                    return value;
                }
            }

            synchronized (ONLINE_RAW_SCORES) {

                Double raw =
                        ONLINE_RAW_SCORES.get(
                                online
                        );

                if (raw != null) {

                    return raw /
                            HEARTHARENA_SCALE;
                }
            }
        }

        return UNKNOWN_CARD_SCORE;
    }

    // ================================================================
    // CAPTURE SERVICE - YHTEENSOPIVUUS
    // ================================================================

    /*
     * CaptureService.java käyttää tätä nimeä.
     *
     * ÄLÄ POISTA tätä metodia.
     */
    public static double getCardScore(
            String cardName
    ) {

        return score(cardName);
    }

    // ================================================================
    // SUOSITUS
    // ================================================================

    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        String[] cards = {
                card1,
                card2,
                card3
        };

        String bestCard = "";

        double bestScore =
                -Double.MAX_VALUE;

        for (String card : cards) {

            if (card == null ||
                    card.trim().isEmpty()) {

                continue;
            }

            String corrected =
                    correctOcr(card);

            double baseScore =
                    getCardScore(corrected);

            if (baseScore <= 0.0) {
                continue;
            }

            double adjustedScore =
                    getDraftAdjustedScore(
                            corrected,
                            baseScore
                    );

            if (adjustedScore > bestScore) {

                bestScore =
                        adjustedScore;

                bestCard =
                        corrected;
            }
        }

        return bestCard;
    }

    private static double getDraftAdjustedScore(
            String cardName,
            double baseScore
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return baseScore;
        }

        String normalized =
                normalize(cardName);

        /*
         * Jos sama kortti on jo valittu,
         * pieni 0.10 pisteen vähennys.
         *
         * Tämä on tarkoituksella erittäin pieni,
         * jotta HearthArena-arvo pysyy päätekijänä.
         */
        synchronized (PICKED_CARDS) {

            if (PICKED_CARDS.contains(
                    normalized
            )) {

                return baseScore - 0.10;
            }
        }

        return baseScore;
    }

    // ================================================================
    // VALITUT KORTIT
    // ================================================================

    public static void addPickedCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return;
        }

        String corrected =
                correctOcr(cardName);

        String normalized =
                normalize(corrected);

        if (normalized.isEmpty()) {
            return;
        }

        synchronized (PICKED_CARDS) {

            PICKED_CARDS.add(
                    normalized
            );
        }
    }

    /*
     * CaptureService.java käyttää tätä nimeä.
     *
     * Tämä on tarkoituksella sama toiminto kuin
     * addPickedCard().
     */
    public static void recordPickedCard(
            String cardName
    ) {

        addPickedCard(cardName);
    }

    public static void markPickedCard(
            String cardName
    ) {

        addPickedCard(cardName);
    }

    public static boolean isPicked(
            String cardName
    ) {

        if (cardName == null) {
            return false;
        }

        String normalized =
                normalize(
                        correctOcr(cardName)
                );

        synchronized (PICKED_CARDS) {

            return PICKED_CARDS.contains(
                    normalized
            );
        }
    }

    public static int getPickedCardCount() {

        synchronized (PICKED_CARDS) {

            return PICKED_CARDS.size();
        }
    }

    public static void clearPickedCards() {

        synchronized (PICKED_CARDS) {

            PICKED_CARDS.clear();
        }
    }

    public static void resetDraft() {

        clearPickedCards();
    }

    // ================================================================
    // SYY
    // ================================================================

    public static String getReason(
            double value
    ) {

        if (value >= 8.0) {

            return "Erittäin vahva valinta";

        } else if (value >= 7.0) {

            return "Vahva valinta";

        } else if (value >= 6.0) {

            return "Hyvä valinta";

        } else if (value >= 5.0) {

            return "Kohtuullinen valinta";

        } else if (value >= 4.0) {

            return "Heikompi valinta";

        } else if (value > 0.0) {

            return "Heikko valinta";
        }

        return "Kortille ei löytynyt arvoa";
    }

    public static String getReason(
            String cardName
    ) {

        return getReason(
                getCardScore(cardName)
        );
    }

    // ================================================================
    // STATUS
    // ================================================================

    public static boolean isOnlineDataLoaded() {

        return onlineLoaded;
    }

    public static int getKnownCardCount() {

        synchronized (CARDS) {

            return CARDS.size();
        }
    }

    public static int getOnlineCardCount() {

        synchronized (ONLINE_CARD_NAMES) {

            return ONLINE_CARD_NAMES.size();
        }
    }

    // ================================================================
    // NIMIEN NORMALISOINTI
    // ================================================================

    private static String cleanCardName(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                text.trim();

        result =
                result.replace(
                        "\n",
                        " "
                );

        result =
                result.replace(
                        "\r",
                        " "
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        result =
                result.replaceAll(
                        "^[\\s|:;,.]+",
                        ""
                );

        result =
                result.replaceAll(
                        "[\\s|:;,.]+$",
                        ""
                );

        return result.trim();
    }

    private static String cleanLine(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        '\u00A0',
                        ' '
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String value =
                cleanCardName(text)
                        .toLowerCase(
                                Locale.US
                        );

        value =
                value.replace(
                        "’",
                        "'"
                );

        value =
                value.replace(
                        "‘",
                        "'"
                );

        value =
                value.replace(
                        "“",
                        "\""
                );

        value =
                value.replace(
                        "”",
                        "\""
                );

        value =
                value.replaceAll(
                        "[^a-z0-9' ]",
                        " "
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    private static String keyToDisplayName(
            String normalizedKey
    ) {

        if (normalizedKey == null) {
            return "";
        }

        synchronized (CARDS) {

            if (CARDS.containsKey(
                    normalizedKey
            )) {

                return restoreDisplayName(
                        normalizedKey
                );
            }
        }

        synchronized (ONLINE_CARD_NAMES) {

            if (ONLINE_CARD_NAMES.contains(
                    normalizedKey
            )) {

                return restoreDisplayName(
                        normalizedKey
                );
            }
        }

        return normalizedKey;
    }

    private static String restoreDisplayName(
            String normalized
    ) {

        if (normalized == null ||
                normalized.isEmpty()) {

            return "";
        }

        String[][] known = {

                {
                        "soldier of the infinite",
                        "Soldier of the Infinite"
                },

                {
                        "crystallized leyline",
                        "Crystallized Leyline"
                },

                {
                        "surge needle",
                        "Surge Needle"
                },

                {
                        "bursting leyline",
                        "Bursting Leyline"
                },

                {
                        "contraband wands",
                        "Contraband Wands"
                },

                {
                        "cold snap",
                        "Cold Snap"
                },

                {
                        "code violet",
                        "Code Violet"
                },

                {
                        "ley walker",
                        "Ley Walker"
                },

                {
                        "leyline nexus",
                        "Leyline Nexus"
                },

                {
                        "raban wands",
                        "Raban Wands"
                },

                {
                        "spellweaver's brilliance",
                        "Spellweaver's Brilliance"
                },

                {
                        "windswept pageturner",
                        "Windswept Pageturner"
                },

                {
                        "spirit gatherer",
                        "Spirit Gatherer"
                },

                {
                        "vault breaker",
                        "Vault Breaker"
                },

                {
                        "scorching winds",
                        "Scorching Winds"
                },

                {
                        "platysaur",
                        "Platysaur"
                },

                {
                        "sizzling cinder",
                        "Sizzling Cinder"
                },

                {
                        "relic miner",
                        "Relic Miner"
                },

                {
                        "temporal traveler",
                        "Temporal Traveler"
                },

                {
                        "hourglass attendant",
                        "Hourglass Attendant"
                },

                {
                        "sewer imp",
                        "Sewer Imp"
                },

                {
                        "critter caretaker",
                        "Critter Caretaker"
                },

                {
                        "drakeadon mongrel",
                        "Drakeadon Mongrel"
                },

                {
                        "scalehide kodo",
                        "Scalehide Kodo"
                },

                {
                        "living paradox",
                        "Living Paradox"
                },

                {
                        "battlefield blaster",
                        "Battlefield Blaster"
                },

                {
                        "gemstone hoarder",
                        "Gemstone Hoarder"
                },

                {
                        "ancient stegodon",
                        "Ancient Stegodon"
                },

                {
                        "briarspawn drake",
                        "Briarspawn Drake"
                },

                {
                        "dreambound raptor",
                        "Dreambound Raptor"
                },

                {
                        "whirling stormdrake",
                        "Whirling Stormdrake"
                },

                {
                        "willful watcher",
                        "Willful Watcher"
                },

                {
                        "gnawing greenfin",
                        "Gnawing Greenfin"
                },

                {
                        "aeon wizard",
                        "Aeon Wizard"
                },

                {
                        "rockskipper",
                        "Rockskipper"
                },

                {
                        "undercover cultist",
                        "Undercover Cultist"
                },

                {
                        "fae trickster",
                        "Fae Trickster"
                },

                {
                        "daydreaming pixie",
                        "Daydreaming Pixie"
                },

                {
                        "flutterwing guardian",
                        "Flutterwing Guardian"
                },

                {
                        "quantum destabilizer",
                        "Quantum Destabilizer"
                },

                {
                        "mother duck",
                        "Mother Duck"
                },

                {
                        "defias smuggler",
                        "Defias Smuggler"
                }
        };

        for (String[] pair : known) {

            if (pair[0].equals(
                    normalized
            )) {

                return pair[1];
            }
        }

        String[] words =
                normalized.split(" ");

        StringBuilder result =
                new StringBuilder();

        for (String word : words) {

            if (word.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(' ');
            }

            if (word.length() == 1) {

                result.append(
                        word.toUpperCase(
                                Locale.US
                        )
                );

            } else {

                result.append(
                        Character.toUpperCase(
                                word.charAt(0)
                        )
                );

                result.append(
                        word.substring(1)
                );
            }
        }

        return result.toString();
    }

    // ================================================================
    // DATA-LUOKKA
    // ================================================================

    private static class CardScore {

        final String name;
        final double score;

        CardScore(
                String name,
                double score
        ) {

            this.name = name;
            this.score = score;
        }
    }
}
