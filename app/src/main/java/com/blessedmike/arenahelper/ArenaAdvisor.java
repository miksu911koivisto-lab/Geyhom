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

    private static final Map<String, String> CANONICAL_NAMES =
            new HashMap<>();

    private static final Map<String, String> OCR_ALIASES =
            new HashMap<>();

    private static final Map<String, String> ONLINE_NAMES =
            new HashMap<>();

    private static final Map<String, Double> ONLINE_RAW_SCORES =
            new HashMap<>();

    private static final Set<String> ONLINE_CARD_NAMES =
            new HashSet<>();

    private static final Set<String> PICKED_CARDS =
            new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static final Object DATA_LOCK =
            new Object();

    private static volatile boolean onlineDataLoaded =
            false;

    /*
     * =========================================================
     * KORTTIDATA
     * =========================================================
     *
     * Nämä kolme ovat nyt pakotetusti paikallisessa datassa.
     * Verkkodata ei voi tehdä niistä nollaa.
     */

    static {

        addCard(
                "Temporal Construct",
                5.54
        );

        addCard(
                "Bitter End",
                6.62
        );

        addCard(
                "Sealed Lancer",
                5.56
        );

        addCard(
                "Soldier of the Infinite",
                8.20
        );

        addCard(
                "Crystallized Leyline",
                5.65
        );

        addCard(
                "Surge Needle",
                5.85
        );

        addCard(
                "Bursting Leyline",
                6.50
        );

        addCard(
                "Contraband Wands",
                6.30
        );

        addCard(
                "Cold Snap",
                7.50
        );

        addCard(
                "Code Violet",
                7.20
        );

        addCard(
                "Ley Walker",
                5.50
        );

        addCard(
                "Leyline Nexus",
                6.00
        );

        addCard(
                "Raban Wands",
                6.50
        );

        addCard(
                "Spellweaver's Brilliance",
                3.92
        );

        addCard(
                "Windswept Pageturner",
                5.23
        );

        addCard(
                "Spirit Gatherer",
                7.69
        );

        addCard(
                "Vault Breaker",
                62.0 / 13.0
        );

        addCard(
                "Scorching Winds",
                48.0 / 13.0
        );

        addCard(
                "Platysaur",
                67.0 / 13.0
        );

        addCard(
                "Sizzling Cinder",
                69.0 / 13.0
        );

        addCard(
                "Relic Miner",
                66.0 / 13.0
        );

        addCard(
                "Temporal Traveler",
                66.0 / 13.0
        );

        addCard(
                "Hourglass Attendant",
                65.0 / 13.0
        );

        addCard(
                "Sewer Imp",
                65.0 / 13.0
        );

        addCard(
                "Critter Caretaker",
                64.0 / 13.0
        );

        addCard(
                "Drakeadon Mongrel",
                64.0 / 13.0
        );

        addCard(
                "Scalehide Kodo",
                67.0 / 13.0
        );

        addCard(
                "Living Paradox",
                69.0 / 13.0
        );

        addCard(
                "Battlefield Blaster",
                69.0 / 13.0
        );

        addCard(
                "Gemstone Hoarder",
                71.0 / 13.0
        );

        addCard(
                "Ancient Stegodon",
                70.0 / 13.0
        );

        addCard(
                "Briarspawn Drake",
                70.0 / 13.0
        );

        addCard(
                "Dreambound Raptor",
                70.0 / 13.0
        );

        addCard(
                "Whirling Stormdrake",
                70.0 / 13.0
        );

        addCard(
                "Willful Watcher",
                70.0 / 13.0
        );

        addCard(
                "Gnawing Greenfin",
                73.0 / 13.0
        );

        addCard(
                "Aeon Wizard",
                73.0 / 13.0
        );

        addCard(
                "Rockskipper",
                74.0 / 13.0
        );

        addCard(
                "Undercover Cultist",
                74.0 / 13.0
        );

        addCard(
                "Fae Trickster",
                74.0 / 13.0
        );

        addCard(
                "Daydreaming Pixie",
                76.0 / 13.0
        );

        addCard(
                "Flutterwing Guardian",
                76.0 / 13.0
        );

        addCard(
                "Quantum Destabilizer",
                76.0 / 13.0
        );

        addCard(
                "Mother Duck",
                77.0 / 13.0
        );

        addCard(
                "Defias Smuggler",
                79.0 / 13.0
        );

        /*
         * =====================================================
         * OCR-ALIAKSET
         * =====================================================
         */

        addAlias(
                "temporal construct",
                "Temporal Construct"
        );

        addAlias(
                "temporalconstruct",
                "Temporal Construct"
        );

        addAlias(
                "bitter end",
                "Bitter End"
        );

        addAlias(
                "bitterend",
                "Bitter End"
        );

        addAlias(
                "sealed lancer",
                "Sealed Lancer"
        );

        addAlias(
                "sealedlancer",
                "Sealed Lancer"
        );

        addAlias(
                "scaled lancer",
                "Sealed Lancer"
        );

        addAlias(
                "scaledlancer",
                "Sealed Lancer"
        );

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
                "soldier of ihfinite",
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
    }

    /*
     * =========================================================
     * KORTIN LISÄYS
     * =========================================================
     */

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

        double safeScore =
                clamp(score);

        CARDS.put(
                key,
                safeScore
        );

        CANONICAL_NAMES.put(
                key,
                name
        );
    }

    private static void addAlias(
            String alias,
            String canonicalName
    ) {

        if (alias == null ||
                canonicalName == null) {

            return;
        }

        String key =
                normalize(alias);

        String compact =
                compactNormalize(alias);

        if (!key.isEmpty()) {

            OCR_ALIASES.put(
                    key,
                    canonicalName
            );
        }

        if (!compact.isEmpty()) {

            OCR_ALIASES.put(
                    compact,
                    canonicalName
            );
        }
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
         * Erikoiskorjaus ensin.
         */
        String soldier =
                correctSoldier(
                        cleaned
                );

        if (soldier != null) {
            return soldier;
        }

        /*
         * 1. Normaali exact-match.
         */
        String key =
                normalize(
                        cleaned
                );

        String canonical =
                CANONICAL_NAMES.get(
                        key
                );

        if (canonical != null) {
            return canonical;
        }

        /*
         * 2. Alias.
         */
        String alias =
                OCR_ALIASES.get(
                        key
                );

        if (alias != null) {
            return alias;
        }

        /*
         * 3. Sama teksti ilman välilyöntejä.
         *
         * TemporalConstruct
         * Temporal Construct
         *
         * -> sama kortti.
         */
        String compact =
                compactNormalize(
                        cleaned
                );

        canonical =
                findCanonicalByCompact(
                        compact
                );

        if (canonical != null) {
            return canonical;
        }

        alias =
                OCR_ALIASES.get(
                        compact
                );

        if (alias != null) {
            return alias;
        }

        /*
         * 4. Online-nimi.
         */
        synchronized (DATA_LOCK) {

            canonical =
                    ONLINE_NAMES.get(
                            key
                    );

            if (canonical != null) {
                return canonical;
            }

            for (String onlineKey :
                    ONLINE_CARD_NAMES) {

                if (compactNormalize(
                        onlineKey
                ).equals(compact)) {

                    String name =
                            ONLINE_NAMES.get(
                                    onlineKey
                            );

                    if (name != null) {
                        return name;
                    }
                }
            }
        }

        /*
         * 5. Fuzzy.
         */
        String fuzzy =
                fuzzyFind(
                        cleaned
                );

        if (fuzzy != null) {
            return fuzzy;
        }

        /*
         * Tuntematon OCR:
         * älä poista välilyöntejä.
         */
        return cleaned;
    }

    private static String correctSoldier(
            String text
    ) {

        String compact =
                compactNormalize(
                        text
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

    /*
     * =========================================================
     * ARVON HAKU
     * =========================================================
     *
     * TÄSSÄ ON NYT KORJAUKSEN PÄÄKOHTA.
     *
     * Arvo haetaan ensin suoraan CARDS-mapista.
     * Verkkodata/OCR-fuzzy ei voi tehdä tunnetusta
     * kortista 0.0-arvoista.
     */

    private static double score(
            String cardName
    ) {

        if (cardName == null) {
            return UNKNOWN_CARD_SCORE;
        }

        String cleaned =
                cleanCardName(
                        cardName
                );

        if (cleaned.isEmpty()) {
            return UNKNOWN_CARD_SCORE;
        }

        /*
         * ENSIMMÄINEN HAKU:
         * suora nimi.
         */
        String key =
                normalize(
                        cleaned
                );

        Double direct =
                CARDS.get(
                        key
                );

        if (direct != null) {

            return clamp(
                    direct
            );
        }

        /*
         * TOINEN HAKU:
         * ilman välilyöntejä.
         */
        String compact =
                compactNormalize(
                        cleaned
                );

        String canonical =
                findCanonicalByCompact(
                        compact
                );

        if (canonical != null) {

            Double value =
                    CARDS.get(
                            normalize(
                                    canonical
                            )
                    );

            if (value != null) {

                return clamp(
                        value
                );
            }
        }

        /*
         * KOLMAS HAKU:
         * OCR-korjaus.
         */
        String corrected =
                correctOcr(
                        cleaned
                );

        if (corrected != null &&
                !corrected.isEmpty()) {

            String correctedKey =
                    normalize(
                            corrected
                    );

            Double correctedValue =
                    CARDS.get(
                            correctedKey
                    );

            if (correctedValue != null) {

                return clamp(
                        correctedValue
                );
            }
        }

        /*
         * NELJÄS HAKU:
         * online-data.
         */
        synchronized (DATA_LOCK) {

            Double online =
                    CARDS.get(
                            key
                    );

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
         * Ei tunnistettu.
         */
        return UNKNOWN_CARD_SCORE;
    }

    /*
     * CaptureService.java käyttää juuri tätä.
     */
    public static String getCardScore(
            String cardName
    ) {

        /*
         * Pakotettu suora testi ennen mitään
         * muuta käsittelyä.
         */
        if (cardName != null) {

            String key =
                    normalize(
                            cardName
                    );

            Double direct =
                    CARDS.get(
                            key
                    );

            if (direct != null) {

                return String.format(
                        Locale.US,
                        "%.2f",
                        direct
                );
            }

            String compact =
                    compactNormalize(
                            cardName
                    );

            String canonical =
                    findCanonicalByCompact(
                            compact
                    );

            if (canonical != null) {

                Double value =
                        CARDS.get(
                                normalize(
                                        canonical
                                )
                        );

                if (value != null) {

                    return String.format(
                            Locale.US,
                            "%.2f",
                            value
                    );
                }
            }
        }

        return String.format(
                Locale.US,
                "%.2f",
                score(cardName)
        );
    }

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
     * KANONINEN NIMI ILMAN VÄLILYÖNTEJÄ
     * =========================================================
     */

    private static String findCanonicalByCompact(
            String compact
    ) {

        if (compact == null ||
                compact.isEmpty()) {

            return null;
        }

        /*
         * Paikalliset kortit ensin.
         */
        for (Map.Entry<String, String> entry :
                CANONICAL_NAMES.entrySet()) {

            String knownCompact =
                    compactNormalize(
                            entry.getValue()
                    );

            if (knownCompact.equals(
                    compact
            )) {

                return entry.getValue();
            }
        }

        return null;
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

        double value =
                score(
                        cardName
                );

        if (value <= 0.0) {
            return 0.0;
        }

        if (isPicked(
                cardName
        )) {

            return clamp(
                    value - 0.10
            );
        }

        return value;
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
     * PICKED CARDS
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
     * ONLINE DATA
     * =========================================================
     */

    public static void loadOnlineData() {

        EXECUTOR.execute(() -> {

            HttpURLConnection connection =
                    null;

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

                int response =
                        connection.getResponseCode();

                if (response < 200 ||
                        response >= 300) {

                    return;
                }

                InputStream input =
                        connection.getInputStream();

                StringBuilder html =
                        new StringBuilder();

                try {

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

                        html.append(
                                line
                        );

                        html.append(
                                '\n'
                        );
                    }

                } finally {

                    try {
                        input.close();
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
                stripHtml(
                        html
                );

        String[] lines =
                clean.split(
                        "\\r?\\n"
                );

        for (String line :
                lines) {

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
                    splitCardAndScore(
                            line
                    );

            if (pair == null) {
                continue;
            }

            String name =
                    cleanCardName(
                            pair[0]
                    );

            double raw =
                    parseScore(
                            pair[1]
                    );

            if (name.isEmpty() ||
                    raw <= 0.0) {

                continue;
            }

            if (!isLikelyCardName(
                    name
            )) {

                continue;
            }

            String key =
                    normalize(
                            name
                    );

            synchronized (DATA_LOCK) {

                Double previous =
                        ONLINE_RAW_SCORES.get(
                                key
                        );

                if (previous == null ||
                        raw > previous) {

                    ONLINE_RAW_SCORES.put(
                            key,
                            raw
                    );

                    ONLINE_NAMES.put(
                            key,
                            name
                    );

                    ONLINE_CARD_NAMES.add(
                            key
                    );

                    /*
                     * TÄRKEÄ:
                     *
                     * Älä koskaan korvaa kolmea
                     * paikallisesti pakotettua testiarvoa
                     * huonommalla verkkotiedolla.
                     */
                    if (!key.equals(
                            normalize(
                                    "Temporal Construct"
                            ))
                            &&
                            !key.equals(
                                    normalize(
                                            "Bitter End"
                                    ))
                            &&
                            !key.equals(
                                    normalize(
                                            "Sealed Lancer"
                                    ))) {

                        CARDS.put(
                                key,
                                clamp(
                                        raw /
                                                HEARTHARENA_SCALE
                                )
                        );

                        CANONICAL_NAMES.put(
                                key,
                                name
                        );
                    }
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

        return text;
    }

    private static String[] splitCardAndScore(
            String line
    ) {

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

        return new String[]{
                matcher.group(1),
                matcher.group(2)
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
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    /*
     * =========================================================
     * FUZZY SEARCH
     * =========================================================
     */

    private static String fuzzyFind(
            String text
    ) {

        String compact =
                compactNormalize(
                        text
                );

        if (compact.isEmpty()) {
            return null;
        }

        String best =
                null;

        int bestDistance =
                Integer.MAX_VALUE;

        /*
         * Paikalliset kortit.
         */
        for (Map.Entry<String, String> entry :
                CANONICAL_NAMES.entrySet()) {

            String knownCompact =
                    compactNormalize(
                            entry.getValue()
                    );

            if (knownCompact.equals(
                    compact
            )) {

                return entry.getValue();
            }

            int distance =
                    levenshtein(
                            knownCompact,
                            compact
                    );

            int allowed =
                    Math.max(
                            2,
                            Math.min(
                                    5,
                                    compact.length() / 5
                            )
                    );

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
     * =========================================================
     * NORMALISOINTI
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
                        );

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

    private static String compactNormalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(
                        Locale.US
                )
                .replace(
                        '\u2019',
                        '\''
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
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

    /*
     * =========================================================
     * STATUS
     * =========================================================
     */

    public static boolean isOnlineDataLoaded() {

        return onlineDataLoaded;
    }

    public static int getKnownCardCount() {

        return CARDS.size();
    }

    public static int getOnlineCardCount() {

        synchronized (DATA_LOCK) {

            return ONLINE_CARD_NAMES.size();
        }
    }

    /*
     * =========================================================
     * REASON
     * =========================================================
     */

    public static String getReason(
            double value
    ) {

        if (value >= 8.0) {

            return "Erittäin vahva Arena-kortti";

        } else if (value >= 7.0) {

            return "Vahva Arena-kortti";

        } else if (value >= 6.0) {

            return "Hyvä Arena-kortti";

        } else if (value >= 5.0) {

            return "Keskitasoinen Arena-kortti";

        } else if (value > 0.0) {

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
     * CLAMP
     * =========================================================
     */

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

    /*
     * =========================================================
     * LEVENSHTEIN
     * =========================================================
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
