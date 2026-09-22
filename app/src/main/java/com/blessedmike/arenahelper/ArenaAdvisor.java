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

    /*
     * HearthArena käyttää suurempaa raakaarvoa.
     * Muutetaan se meidän 0-10 asteikolle.
     */
    private static final double HEARTHARENA_SCALE = 13.0;

    private static final double UNKNOWN_CARD_SCORE = 0.0;

    private static final Map<String, Double> CARDS =
            new HashMap<>();

    private static final Map<String, String> OCR_ALIASES =
            new HashMap<>();

    private static final Map<String, String> ONLINE_NAMES =
            new HashMap<>();

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

    private static volatile boolean onlineDataLoaded = false;

    private static final Object DATA_LOCK =
            new Object();

    static {

        /*
         * -----------------------------
         * VARAARVOT
         * -----------------------------
         *
         * Näitä käytetään heti ilman
         * verkkoyhteyttäkin.
         */

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

        addCard("Vault Breaker", 62.0 / 13.0);
        addCard("Scorching Winds", 48.0 / 13.0);
        addCard("Platysaur", 67.0 / 13.0);
        addCard("Sizzling Cinder", 69.0 / 13.0);
        addCard("Relic Miner", 66.0 / 13.0);
        addCard("Temporal Traveler", 66.0 / 13.0);
        addCard("Hourglass Attendant", 65.0 / 13.0);
        addCard("Sewer Imp", 65.0 / 13.0);
        addCard("Critter Caretaker", 64.0 / 13.0);
        addCard("Drakeadon Mongrel", 64.0 / 13.0);
        addCard("Scalehide Kodo", 67.0 / 13.0);
        addCard("Living Paradox", 69.0 / 13.0);
        addCard("Battlefield Blaster", 69.0 / 13.0);
        addCard("Gemstone Hoarder", 71.0 / 13.0);
        addCard("Ancient Stegodon", 70.0 / 13.0);
        addCard("Briarspawn Drake", 70.0 / 13.0);
        addCard("Dreambound Raptor", 70.0 / 13.0);
        addCard("Whirling Stormdrake", 70.0 / 13.0);
        addCard("Willful Watcher", 70.0 / 13.0);
        addCard("Gnawing Greenfin", 73.0 / 13.0);
        addCard("Aeon Wizard", 73.0 / 13.0);
        addCard("Rockskipper", 74.0 / 13.0);
        addCard("Undercover Cultist", 74.0 / 13.0);
        addCard("Fae Trickster", 74.0 / 13.0);
        addCard("Daydreaming Pixie", 76.0 / 13.0);
        addCard("Flutterwing Guardian", 76.0 / 13.0);
        addCard("Quantum Destabilizer", 76.0 / 13.0);
        addCard("Mother Duck", 77.0 / 13.0);
        addCard("Defias Smuggler", 79.0 / 13.0);

        /*
         * -----------------------------
         * OCR-ALIAKSET
         * -----------------------------
         */

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
                "soldier of the infinite",
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
    }

    private static void addCard(
            String name,
            double score
    ) {

        if (name == null) {
            return;
        }

        String key =
                normalize(name);

        if (key.isEmpty()) {
            return;
        }

        CARDS.put(
                key,
                clamp(score)
        );
    }

    private static void addAlias(
            String alias,
            String realName
    ) {

        String key =
                normalize(alias);

        if (!key.isEmpty() &&
                realName != null) {

            OCR_ALIASES.put(
                    key,
                    realName
            );
        }
    }

    /*
     * =========================================================
     * VERKKODATA
     * =========================================================
     */

    public static void loadOnlineData() {

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

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
                        10000
                );

                connection.setReadTimeout(
                        15000
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml"
                );

                int response =
                        connection.getResponseCode();

                if (response < 200 ||
                        response >= 300) {

                    return;
                }

                InputStream stream =
                        connection.getInputStream();

                StringBuilder html =
                        new StringBuilder();

                try {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            stream,
                                            StandardCharsets.UTF_8
                                    )
                            );

                    String line;

                    while ((line =
                            reader.readLine()) != null) {

                        html.append(line);
                        html.append('\n');
                    }

                } finally {

                    try {
                        stream.close();
                    } catch (Exception ignored) {}
                }

                parseHearthArenaPage(
                        html.toString()
                );

                synchronized (DATA_LOCK) {

                    onlineDataLoaded =
                            !ONLINE_CARD_NAMES.isEmpty();
                }

            } catch (Exception ignored) {

                /*
                 * Jos internet ei toimi,
                 * käytetään CARDS-varaarvoja.
                 */

            } finally {

                if (connection != null) {

                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {}
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

        String clean =
                stripHtml(html);

        String[] lines =
                clean.split("\\r?\\n");

        for (String line : lines) {

            if (line == null) {
                continue;
            }

            line =
                    decodeHtmlEntities(
                            line
                    ).trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] pair =
                    splitCardAndScore(line);

            if (pair == null) {
                continue;
            }

            String cardName =
                    cleanCardName(
                            pair[0]
                    );

            double rawScore =
                    parseScore(
                            pair[1]
                    );

            if (cardName.isEmpty() ||
                    rawScore <= 0.0) {

                continue;
            }

            if (!isLikelyCardName(
                    cardName
            )) {

                continue;
            }

            String key =
                    normalize(cardName);

            if (key.isEmpty()) {
                continue;
            }

            synchronized (DATA_LOCK) {

                Double previous =
                        ONLINE_RAW_SCORES.get(key);

                /*
                 * Jos sama kortti esiintyy monta kertaa,
                 * pidetään suurin arvo.
                 */
                if (previous == null ||
                        rawScore > previous) {

                    ONLINE_RAW_SCORES.put(
                            key,
                            rawScore
                    );

                    ONLINE_NAMES.put(
                            key,
                            cardName
                    );

                    ONLINE_CARD_NAMES.add(
                            key
                    );

                    CARDS.put(
                            key,
                            clamp(
                                    rawScore /
                                            HEARTHARENA_SCALE
                            )
                    );
                }
            }
        }
    }

    private static String stripHtml(
            String html
    ) {

        String text =
                html.replaceAll(
                        "(?is)<script.*?</script>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?is)<style.*?</style>",
                        "\n"
                );

        text =
                text.replaceAll(
                        "(?is)<[^>]+>",
                        "\n"
                );

        text =
                decodeHtmlEntities(
                        text
                );

        text =
                text.replace(
                        "\u00A0",
                        " "
                );

        text =
                text.replaceAll(
                        "[ \\t]+",
                        " "
                );

        text =
                text.replaceAll(
                        "\\n+",
                        "\n"
                );

        return text;
    }

    private static String[] splitCardAndScore(
            String line
    ) {

        /*
         * Muoto esimerkiksi:
         *
         * Card Name 7.25
         * Card Name 7,25
         *
         * Otetaan viimeinen numero.
         */

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile(
                                "^(.*?)[\\s:|\\-]+([0-9]+(?:[\\.,][0-9]+)?)$"
                        )
                        .matcher(
                                line.trim()
                        );

        if (!matcher.matches()) {
            return null;
        }

        String name =
                matcher.group(1);

        String score =
                matcher.group(2);

        if (name == null ||
                score == null) {

            return null;
        }

        name =
                cleanCardName(
                        name
                );

        if (name.isEmpty()) {
            return null;
        }

        return new String[]{
                name,
                score
        };
    }

    private static double parseScore(
            String text
    ) {

        if (text == null) {
            return -1.0;
        }

        try {

            return Double.parseDouble(
                    text
                            .trim()
                            .replace(
                                    ",",
                                    "."
                            )
            );

        } catch (Exception ignored) {

            return -1.0;
        }
    }

    private static boolean isLikelyCardName(
            String name
    ) {

        if (name == null ||
                name.length() < 2 ||
                name.length() > 80) {

            return false;
        }

        String normalized =
                normalize(name);

        if (normalized.isEmpty()) {
            return false;
        }

        /*
         * Estetään tavallisia HTML-sivun tekstejä
         * päätymästä korttilistaan.
         */
        String lower =
                normalized.toLowerCase(
                        Locale.US
                );

        if (lower.equals("tier list") ||
                lower.equals("heartharena") ||
                lower.equals("score") ||
                lower.equals("rating") ||
                lower.equals("class")) {

            return false;
        }

        int letters = 0;

        for (int i = 0;
                i < name.length();
                i++) {

            if (Character.isLetter(
                    name.charAt(i)
            )) {

                letters++;
            }
        }

        return letters >= 2;
    }

    private static String decodeHtmlEntities(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        "&amp;",
                        "&"
                )
                .replace(
                        "&quot;",
                        "\""
                )
                .replace(
                        "&#039;",
                        "'"
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
                        "&nbsp;",
                        " "
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
     * =========================================================
     * OCR
     * =========================================================
     */

    public static String correctOcr(
            String ocrText
    ) {

        if (ocrText == null) {
            return "";
        }

        String cleaned =
                cleanCardName(
                        ocrText
                );

        if (cleaned.isEmpty()) {
            return "";
        }

        /*
         * Ensimmäisenä Soldier of the Infinite,
         * koska sen OCR on ollut erityisen epävakaa.
         */
        String soldier =
                correctSoldier(
                        cleaned
                );

        if (soldier != null) {
            return soldier;
        }

        String key =
                normalize(cleaned);

        /*
         * Täsmällinen alias.
         */
        String alias =
                OCR_ALIASES.get(key);

        if (alias != null) {
            return alias;
        }

        /*
         * Täsmällinen fallback-kortti.
         */
        if (CARDS.containsKey(key)) {

            return keyToDisplayName(
                    key
            );
        }

        /*
         * Täsmällinen online-kortti.
         */
        synchronized (DATA_LOCK) {

            String online =
                    ONLINE_NAMES.get(key);

            if (online != null) {
                return online;
            }
        }

        /*
         * Jos välilyönti on joskus kadonnut OCR:ssa,
         * verrataan normalisoitua tekstiä tunnettuun
         * korttinimeen.
         */
        String fuzzy =
                fuzzyFind(
                        cleaned
                );

        if (fuzzy != null) {
            return fuzzy;
        }

        /*
         * Jos mitään varmaa osumaa ei löytynyt,
         * palautetaan alkuperäinen nimi siistittynä.
         *
         * Tärkeää:
         * emme poista tässä välilyöntejä.
         */
        return cleaned;
    }

    private static String correctSoldier(
            String text
    ) {

        String compact =
                text.toLowerCase(
                                Locale.US
                        )
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        if (compact.equals(
                "soldierofinfinite"
        )
                ||
                compact.equals(
                        "soldieroftheinfinite"
                )
                ||
                compact.equals(
                        "so1dierofinfinite"
                )
                ||
                compact.equals(
                        "sotdierofinfinite"
                )
                ||
                compact.equals(
                        "soldierofihfini"
                )
                ||
                compact.equals(
                        "soldierofihfinite"
                )
                ||
                compact.equals(
                        "sotdierofihfinite"
                )) {

            return "Soldier of the Infinite";
        }

        /*
         * Sallitaan myös yleiset lyhentyneet OCR-versiot.
         */
        if (compact.startsWith(
                "soldierof"
        )
                &&
                compact.contains(
                        "infin"
                )) {

            return "Soldier of the Infinite";
        }

        return null;
    }

    private static String fuzzyFind(
            String text
    ) {

        String key =
                normalize(text);

        if (key.isEmpty()) {
            return null;
        }

        String bestName = null;
        int bestDistance = Integer.MAX_VALUE;

        /*
         * Ensin paikalliset kortit.
         */
        for (String cardKey :
                CARDS.keySet()) {

            if (cardKey == null ||
                    cardKey.isEmpty()) {

                continue;
            }

            if (cardKey.equals(key)) {

                return keyToDisplayName(
                        cardKey
                );
            }

            if (cardKey.contains(key) ||
                    key.contains(cardKey)) {

                int difference =
                        Math.abs(
                                cardKey.length()
                                        -
                                key.length()
                        );

                if (difference <
                        bestDistance) {

                    bestDistance =
                            difference;

                    bestName =
                            keyToDisplayName(
                                    cardKey
                            );
                }

                continue;
            }

            int distance =
                    levenshtein(
                            cardKey,
                            key
                    );

            int allowed =
                    Math.max(
                            2,
                            Math.min(
                                    5,
                                    key.length() / 5
                            )
                    );

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                bestName =
                        keyToDisplayName(
                                cardKey
                        );
            }
        }

        /*
         * Online-kortit.
         */
        synchronized (DATA_LOCK) {

            for (String cardKey :
                    ONLINE_CARD_NAMES) {

                if (cardKey == null ||
                        cardKey.isEmpty()) {

                    continue;
                }

                String display =
                        ONLINE_NAMES.get(
                                cardKey
                        );

                if (display == null) {
                    display =
                            cardKey;
                }

                if (cardKey.equals(key)) {
                    return display;
                }

                if (cardKey.contains(key) ||
                        key.contains(cardKey)) {

                    int difference =
                            Math.abs(
                                    cardKey.length()
                                            -
                                    key.length()
                            );

                    if (difference <
                            bestDistance) {

                        bestDistance =
                                difference;

                        bestName =
                                display;
                    }

                    continue;
                }

                int distance =
                        levenshtein(
                                cardKey,
                                key
                        );

                int allowed =
                        Math.max(
                                2,
                                Math.min(
                                        5,
                                        key.length() / 5
                                )
                        );

                if (distance <= allowed &&
                        distance < bestDistance) {

                    bestDistance =
                            distance;

                    bestName =
                            display;
                }
            }
        }

        return bestName;
    }

    /*
     * =========================================================
     * ARVO
     * =========================================================
     */

    private static double score(
            String cardName
    ) {

        if (cardName == null) {
            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (corrected == null ||
                corrected.trim().isEmpty()) {

            return UNKNOWN_CARD_SCORE;
        }

        String key =
                normalize(corrected);

        if (key.isEmpty()) {
            return UNKNOWN_CARD_SCORE;
        }

        /*
         * Online-arvo ensin.
         */
        synchronized (DATA_LOCK) {

            Double online =
                    CARDS.get(key);

            if (online != null) {

                return clamp(
                        online
                );
            }

            Double raw =
                    ONLINE_RAW_SCORES.get(
                            key
                    );

            if (raw != null &&
                    raw > 0.0) {

                return clamp(
                        raw /
                                HEARTHARENA_SCALE
                );
            }
        }

        /*
         * Fallback vielä kerran.
         */
        Double local =
                CARDS.get(key);

        if (local != null) {

            return clamp(
                    local
            );
        }

        /*
         * Jos OCR-nimi ei ollut täydellinen,
         * yritetään fuzzy-osumaa.
         */
        String fuzzy =
                fuzzyFind(
                        corrected
                );

        if (fuzzy != null &&
                !fuzzy.equals(
                        corrected
                )) {

            String fuzzyKey =
                    normalize(
                            fuzzy
                    );

            Double fuzzyScore =
                    CARDS.get(
                            fuzzyKey
                    );

            if (fuzzyScore != null) {

                return clamp(
                        fuzzyScore
                );
            }
        }

        return UNKNOWN_CARD_SCORE;
    }

    /*
     * Tätä CaptureService käyttää.
     */
    public static String getCardScore(
            String cardName
    ) {

        double value =
                score(
                        cardName
                );

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    /*
     * Säilytetään myös double-versio,
     * jotta mahdolliset vanhat CaptureService-versiot
     * eivät riko buildia.
     */
    public static String getCardScore(
            double value
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                clamp(value)
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
     * =========================================================
     * SUOSITUS
     * =========================================================
     */

    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        double score1 =
                getDraftAdjustedScore(
                        card1
                );

        double score2 =
                getDraftAdjustedScore(
                        card2
                );

        double score3 =
                getDraftAdjustedScore(
                        card3
                );

        double best =
                Math.max(
                        score1,
                        Math.max(
                                score2,
                                score3
                        )
                );

        if (best <= 0.0) {

            return "Ei tunnistettu";
        }

        if (score1 >= score2 &&
                score1 >= score3) {

            return displayName(
                    card1
            );
        }

        if (score2 >= score1 &&
                score2 >= score3) {

            return displayName(
                    card2
            );
        }

        return displayName(
                card3
        );
    }

    private static double getDraftAdjustedScore(
            String cardName
    ) {

        double base =
                score(
                        cardName
                );

        if (base <= 0.0) {
            return 0.0;
        }

        if (isPicked(
                cardName
        )) {

            /*
             * Pieni vähennys jo valitulle kortille.
             */
            return clamp(
                    base - 0.10
            );
        }

        return base;
    }

    private static String displayName(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return "Ei tunnistettu";
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (corrected == null ||
                corrected.trim().isEmpty()) {

            return cardName.trim();
        }

        return corrected.trim();
    }

    /*
     * =========================================================
     * VALITTUJEN KORTTIEN SEURANTA
     * =========================================================
     */

    private static void addPickedCard(
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

        if (!key.isEmpty()) {

            PICKED_CARDS.add(
                    key
            );
        }
    }

    public static void recordPickedCard(
            String cardName
    ) {

        addPickedCard(
                cardName
        );
    }

    public static void markPickedCard(
            String cardName
    ) {

        addPickedCard(
                cardName
        );
    }

    public static boolean isPicked(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return false;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        String key =
                normalize(
                        corrected
                );

        return PICKED_CARDS.contains(
                key
        );
    }

    public static int getPickedCardCount() {

        return PICKED_CARDS.size();
    }

    public static void clearPickedCards() {

        PICKED_CARDS.clear();
    }

    public static void resetDraft() {

        PICKED_CARDS.clear();
    }

    /*
     * =========================================================
     * STATUS
     * =========================================================
     */

    public static boolean isOnlineDataLoaded() {

        return onlineDataLoaded;
    }

    public static int getKnownCardCount() {

        synchronized (DATA_LOCK) {

            return CARDS.size();
        }
    }

    public static int getOnlineCardCount() {

        synchronized (DATA_LOCK) {

            return ONLINE_CARD_NAMES.size();
        }
    }

    /*
     * =========================================================
     * SELITYKSET
     * =========================================================
     */

    public static String getReason(
            double score
    ) {

        if (score >= 8.0) {

            return "Erittäin vahva Arena-kortti";

        } else if (score >= 7.0) {

            return "Vahva Arena-kortti";

        } else if (score >= 6.0) {

            return "Hyvä Arena-kortti";

        } else if (score >= 5.0) {

            return "Keskitasoinen Arena-kortti";

        } else if (score > 0.0) {

            return "Heikompi Arena-kortti";
        }

        return "Kortille ei löytynyt arvoa";
    }

    public static String getReason(
            String cardName
    ) {

        return getReason(
                score(
                        cardName
                )
        );
    }

    /*
     * =========================================================
     * APUFUNKTIOT
     * =========================================================
     */

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                text
                        .toLowerCase(
                                Locale.US
                        )
                        .replace(
                                '\u2019',
                                '\''
                        )
                        .replace(
                                '\u2018',
                                '\''
                        )
                        .replace(
                                '\u201c',
                                '"'
                        )
                        .replace(
                                '\u201d',
                                '"'
                        );

        /*
         * Tärkeä:
         * välilyönnit säilytetään.
         */
        result =
                result.replaceAll(
                        "[^a-z0-9' ]",
                        " "
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        return result.trim();
    }

    private static String cleanCardName(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                decodeHtmlEntities(
                        text
                );

        text =
                text.replace(
                        '\u00A0',
                        ' '
                );

        text =
                text.replace(
                        '\u2019',
                        '\''
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                );

        text =
                text.replaceAll(
                        "^[\\s\\.,:;|]+",
                        ""
                );

        text =
                text.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        return text.trim();
    }

    private static String keyToDisplayName(
            String key
    ) {

        if (key == null ||
                key.isEmpty()) {

            return "";
        }

        /*
         * Paikallisten korttien oikeat nimet.
         */
        String[] names = {

                "Soldier of the Infinite",
                "Crystallized Leyline",
                "Surge Needle",
                "Bursting Leyline",
                "Contraband Wands",
                "Cold Snap",
                "Code Violet",
                "Ley Walker",
                "Leyline Nexus",
                "Raban Wands",
                "Spellweaver's Brilliance",
                "Windswept Pageturner",
                "Spirit Gatherer",
                "Vault Breaker",
                "Scorching Winds",
                "Platysaur",
                "Sizzling Cinder",
                "Relic Miner",
                "Temporal Traveler",
                "Hourglass Attendant",
                "Sewer Imp",
                "Critter Caretaker",
                "Drakeadon Mongrel",
                "Scalehide Kodo",
                "Living Paradox",
                "Battlefield Blaster",
                "Gemstone Hoarder",
                "Ancient Stegodon",
                "Briarspawn Drake",
                "Dreambound Raptor",
                "Whirling Stormdrake",
                "Willful Watcher",
                "Gnawing Greenfin",
                "Aeon Wizard",
                "Rockskipper",
                "Undercover Cultist",
                "Fae Trickster",
                "Daydreaming Pixie",
                "Flutterwing Guardian",
                "Quantum Destabilizer",
                "Mother Duck",
                "Defias Smuggler"
        };

        for (String name :
                names) {

            if (normalize(name)
                    .equals(key)) {

                return name;
            }
        }

        synchronized (DATA_LOCK) {

            String online =
                    ONLINE_NAMES.get(
                            key
                    );

            if (online != null) {
                return online;
            }
        }

        /*
         * Jos nimeä ei tunneta,
         * palautetaan siistitty versio.
         */
        return restoreDisplayName(
                key
        );
    }

    private static String restoreDisplayName(
            String key
    ) {

        if (key == null ||
                key.isEmpty()) {

            return "";
        }

        String[] words =
                key.split(
                        " "
                );

        StringBuilder result =
                new StringBuilder();

        for (String word :
                words) {

            if (word == null ||
                    word.isEmpty()) {

                continue;
            }

            if (result.length() > 0) {
                result.append(" ");
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

    private static double clamp(
            double value
    ) {

        if (Double.isNaN(value) ||
                Double.isInfinite(value)) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        10.0,
                        value
                )
        );
    }

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

        int[][] dp =
                new int[
                        a.length() + 1
                ][
                        b.length() + 1
                ];

        for (int i = 0;
                i <= a.length();
                i++) {

            dp[i][0] = i;
        }

        for (int j = 0;
                j <= b.length();
                j++) {

            dp[0][j] = j;
        }

        for (int i = 1;
                i <= a.length();
                i++) {

            for (int j = 1;
                    j <= b.length();
                    j++) {

                int cost =
                        a.charAt(
                                i - 1
                        )
                                ==
                        b.charAt(
                                j - 1
                        )
                                ? 0
                                : 1;

                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j] + 1,
                                        dp[i][j - 1] + 1
                                ),
                                dp[i - 1][j - 1]
                                        + cost
                        );
            }
        }

        return dp[
                a.length()
        ][
                b.length()
        ];
    }
}
