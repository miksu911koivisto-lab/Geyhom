package com.blessedmike.arenahelper;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
     * HearthArena käyttää pisteitä pääasiassa noin 0–150
     * välillä. Sovelluksessa muutetaan ne 0–10 asteikolle.
     */
    private static final double HEARTHARENA_SCALE = 13.0;

    private static final double UNKNOWN_CARD_SCORE = 0.0;

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    private static final Map<String, String> OCR_ALIASES =
            new HashMap<>();

    private static final Set<String> ONLINE_NAMES =
            new HashSet<>();

    private static final Set<String> PICKED_CARDS =
            new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static boolean onlineLoadStarted = false;

    private static boolean onlineDataLoaded = false;

    /*
     * Estetään sitä, että sama kortti päätyy useita kertoja
     * listalle saman sivun eri luokkien takia.
     *
     * Jos kortti esiintyy usealla luokalla, pidämme suurimman
     * HearthArena-arvon. Tällä hetkellä sovellus ei vielä tiedä
     * pelaajan Arena-luokkaa, joten tämä antaa kortille käyttökelpoisen
     * yleisarvon. Luokkakohtainen valinta voidaan lisätä myöhemmin.
     */
    private static final Map<String, Integer> ONLINE_RAW_SCORES =
            new HashMap<>();

    private static class CardData {

        final String name;
        final double score;

        CardData(
                String name,
                double score
        ) {
            this.name = name;
            this.score = score;
        }
    }

    static {

        /*
         * ---------------------------------------------------------
         * VARAKORTIT
         * ---------------------------------------------------------
         *
         * Nämä ovat vain varmistuksia siltä varalta, että verkkolataus
         * ei onnistu. Varsinainen korttidata haetaan HearthArenasta.
         */

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
                62.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Scorching Winds",
                48.0 / HEARTHARENA_SCALE
        );


        /*
         * ---------------------------------------------------------
         * OCR-ALIAKSET
         * ---------------------------------------------------------
         */

        addAlias(
                "soldier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldierofinfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "so1dier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "so1dier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of ihfinite",
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
                "raban wand",
                "Raban Wands"
        );

        addAlias(
                "raban wands",
                "Raban Wands"
        );

        addAlias(
                "raban wandz",
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
                "crystalized leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "crystalised leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "surge needle",
                "Surge Needle"
        );

        addAlias(
                "surge need1e",
                "Surge Needle"
        );

        addAlias(
                "spellweavers brilliance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "spellweaver's brilliance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "spellweavers brillance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "windswept pageturner",
                "Windswept Pageturner"
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
                "spirit gather",
                "Spirit Gatherer"
        );

        addAlias(
                "spirit gathere",
                "Spirit Gatherer"
        );

        addAlias(
                "vault breaker",
                "Vault Breaker"
        );

        addAlias(
                "vault breake",
                "Vault Breaker"
        );

        addAlias(
                "vault breker",
                "Vault Breaker"
        );

        addAlias(
                "scorching winds",
                "Scorching Winds"
        );

        addAlias(
                "scorching wind",
                "Scorching Winds"
        );

        addAlias(
                "scorching wnds",
                "Scorching Winds"
        );


        /*
         * Aloitetaan HearthArena-datan lataus.
         *
         * Tämä tapahtuu taustasäikeessä, joten sovellus ei jääty.
         */
        loadOnlineData();
    }

    private ArenaAdvisor() {
    }

    private static void addCard(
            String name,
            double score
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return;
        }

        String key =
                normalizeKey(name);

        CARDS.put(
                key,
                new CardData(
                        name,
                        score
                )
        );
    }

    private static void addAlias(
            String alias,
            String cardName
    ) {

        if (alias == null ||
                cardName == null) {

            return;
        }

        OCR_ALIASES.put(
                normalizeKey(alias),
                cardName
        );
    }

    /*
     * -------------------------------------------------------------
     * ONLINE-DATA
     * -------------------------------------------------------------
     */

    private static void loadOnlineData() {

        if (onlineLoadStarted) {
            return;
        }

        onlineLoadStarted = true;

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
                        12000
                );

                connection.setReadTimeout(
                        20000
                );

                connection.setUseCaches(
                        false
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 " +
                        "(Android) ArenaHelper"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml"
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode !=
                        HttpURLConnection.HTTP_OK) {

                    return;
                }

                InputStream input =
                        connection.getInputStream();

                String html =
                        readStream(
                                input
                        );

                if (html == null ||
                        html.trim().isEmpty()) {

                    return;
                }

                parseHearthArenaPage(
                        html
                );

                if (!ONLINE_RAW_SCORES.isEmpty()) {

                    synchronized (ArenaAdvisor.class) {

                        for (
                                Map.Entry<String, Integer>
                                        entry :
                                        ONLINE_RAW_SCORES.entrySet()
                        ) {

                            String normalizedName =
                                    entry.getKey();

                            int rawScore =
                                    entry.getValue();

                            String displayName =
                                    findOriginalName(
                                            normalizedName
                                    );

                            if (displayName == null ||
                                    displayName.isEmpty()) {

                                displayName =
                                        normalizedName;
                            }

                            double score =
                                    rawScore /
                                            HEARTHARENA_SCALE;

                            /*
                             * Online data korvaa vanhan fallback-arvon.
                             */
                            CARDS.put(
                                    normalizedName,
                                    new CardData(
                                            displayName,
                                            score
                                    )
                            );

                            ONLINE_NAMES.add(
                                    normalizedName
                            );
                        }

                        onlineDataLoaded = true;
                    }
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

    private static String readStream(
            InputStream input
    ) {

        if (input == null) {
            return "";
        }

        StringBuilder builder =
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

            while (
                    (line = reader.readLine())
                            != null
            ) {

                builder.append(
                        line
                );

                builder.append(
                        '\n'
                );
            }

            reader.close();

        } catch (Exception ignored) {}

        return builder.toString();
    }

    /*
     * HearthArenan sivu sisältää kortin nimen ja sen jälkeen
     * numeerisen pisteen.
     *
     * Sivulla on paljon tyhjiä sijoituspaikkoja, joten parseri
     * ei oleta että piste on välittömästi seuraavassa raakatekstin
     * merkissä.
     */
    private static void parseHearthArenaPage(
            String html
    ) {

        if (html == null ||
                html.isEmpty()) {

            return;
        }

        String text =
                stripHtml(
                        html
                );

        if (text == null ||
                text.isEmpty()) {

            return;
        }

        String[] lines =
                text.split(
                        "\\r?\\n"
                );

        String previousCard =
                "";

        for (String rawLine : lines) {

            if (rawLine == null) {
                continue;
            }

            String line =
                    decodeHtml(
                            rawLine
                    ).trim();

            if (line.isEmpty()) {
                continue;
            }

            line =
                    normalizeWhitespace(
                            line
                    );

            /*
             * Piste:
             * 115
             * 100
             * 79↓
             * jne.
             */
            String scoreLine =
                    line.replace(
                            "↓",
                            ""
                    ).trim();

            if (scoreLine.matches(
                    "\\d+(?:\\.\\d+)?"
            )) {

                if (!previousCard.isEmpty()) {

                    try {

                        double parsed =
                                Double.parseDouble(
                                        scoreLine
                                );

                        if (parsed >= 0 &&
                                parsed <= 200) {

                            int integerScore =
                                    (int)
                                            Math.round(
                                                    parsed
                                            );

                            String key =
                                    normalizeKey(
                                            previousCard
                                    );

                            if (!key.isEmpty() &&
                                    isLikelyCardName(
                                            previousCard
                                    )) {

                                Integer old =
                                        ONLINE_RAW_SCORES.get(
                                                key
                                        );

                                /*
                                 * Sama kortti voi esiintyä useassa
                                 * luokkataulukossa. Otetaan korkein
                                 * nykyinen HearthArena-arvo.
                                 */
                                if (old == null ||
                                        integerScore > old) {

                                    ONLINE_RAW_SCORES.put(
                                            key,
                                            integerScore
                                    );
                                }
                            }
                        }

                    } catch (Exception ignored) {}
                }

                previousCard =
                        "";

                continue;
            }

            /*
             * Numerorivit, otsikot ja muut sivun tekstit eivät
             * saa muuttua korttinimiksi.
             */
            if (isLikelyCardName(line)) {

                previousCard =
                        line;
            }
        }
    }

    private static boolean isLikelyCardName(
            String text
    ) {

        if (text == null) {
            return false;
        }

        String value =
                text.trim();

        if (value.isEmpty()) {
            return false;
        }

        /*
         * Otsikoita ei käsitellä kortteina.
         */
        String lower =
                value.toLowerCase(
                        Locale.US
                );

        if (lower.equals(
                "death knight"
        )
                ||
                lower.equals(
                        "demon hunter"
                )
                ||
                lower.equals(
                        "druid"
                )
                ||
                lower.equals(
                        "hunter"
                )
                ||
                lower.equals(
                        "mage"
                )
                ||
                lower.equals(
                        "paladin"
                )
                ||
                lower.equals(
                        "priest"
                )
                ||
                lower.equals(
                        "rogue"
                )
                ||
                lower.equals(
                        "shaman"
                )
                ||
                lower.equals(
                        "warlock"
                )
                ||
                lower.equals(
                        "warrior"
                )
                ||
                lower.equals(
                        "neutral"
                )
                ||
                lower.contains(
                        "cards"
                )
                ||
                lower.equals(
                        "great"
                )
                ||
                lower.equals(
                        "good"
                )
                ||
                lower.equals(
                        "above average"
                )
                ||
                lower.equals(
                        "average"
                )
                ||
                lower.equals(
                        "below average"
                )
                ||
                lower.equals(
                        "bad"
                )
                ||
                lower.equals(
                        "terrible"
                )
                ||
                lower.startsWith(
                        "cards in "
                )
                ||
                lower.startsWith(
                        "english"
                )
        ) {

            return false;
        }

        /*
         * Tyhjät sijoituspaikat.
         */
        if (value.matches(
                "\\d+\\."
        )) {

            return false;
        }

        /*
         * Pelkkä numero.
         */
        if (value.matches(
                "\\d+(?:\\.\\d+)?"
        )) {

            return false;
        }

        /*
         * Varmistetaan, että tekstissä on kirjaimia.
         */
        boolean hasLetter =
                false;

        for (int i = 0;
                i < value.length();
                i++) {

            if (Character.isLetter(
                    value.charAt(i)
            )) {

                hasLetter = true;
                break;
            }
        }

        if (!hasLetter) {
            return false;
        }

        /*
         * Liian pitkä rivi on lähes varmasti sivutekstiä.
         */
        if (value.length() > 100) {
            return false;
        }

        return true;
    }

    private static String stripHtml(
            String html
    ) {

        if (html == null) {
            return "";
        }

        String result =
                html;

        /*
         * Scriptit ja tyylit pois.
         */
        result =
                result.replaceAll(
                        "(?is)<script[^>]*>.*?</script>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?is)<style[^>]*>.*?</style>",
                        "\n"
                );

        /*
         * Rivin vaihdot säilytetään tärkeinä.
         */
        result =
                result.replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</div>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</li>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</p>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</h[1-6]>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</tr>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "<[^>]+>",
                        ""
                );

        result =
                decodeHtml(
                        result
                );

        return result;
    }

    private static String decodeHtml(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                text;

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
                        "&#039;",
                        "'"
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
                        "&lt;",
                        "<"
                );

        result =
                result.replace(
                        "&gt;",
                        ">"
                );

        /*
         * Yleinen numeerinen HTML-entiteetti.
         */
        result =
                result.replaceAll(
                        "&#x27;",
                        "'"
                );

        result =
                result.replaceAll(
                        "&#x2F;",
                        "/"
                );

        return result;
    }

    private static String normalizeWhitespace(
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

    private static String findOriginalName(
            String normalized
    ) {

        if (normalized == null ||
                normalized.isEmpty()) {

            return "";
        }

        for (CardData card :
                CARDS.values()) {

            if (normalizeKey(
                    card.name
            ).equals(
                    normalized
            )) {

                return card.name;
            }
        }

        /*
         * Muutamat uudet kortit eivät ole fallback-listassa,
         * joten muodostetaan nimi normalisoidusta arvosta.
         */
        return restoreBasicName(
                normalized
        );
    }

    private static String restoreBasicName(
            String normalized
    ) {

        if (normalized == null ||
                normalized.isEmpty()) {

            return "";
        }

        /*
         * Tätä käytetään vain siinä tapauksessa,
         * ettei korttia löydy valmiista nimilistasta.
         *
         * Useimmat HearthArena-nimet säilyvät jo alkuperäisessä
         * muodossaan parserissa, joten tämä on viimeinen varmistus.
         */
        return normalized
                .replace(
                        "_",
                        " "
                );
    }

    /*
     * -------------------------------------------------------------
     * OCR
     * -------------------------------------------------------------
     */

    public static String correctOcr(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String cleaned =
                cleanInput(
                        input
                );

        if (cleaned.isEmpty()) {
            return "";
        }

        String normalized =
                normalizeKey(
                        cleaned
                );

        /*
         * Soldier of the Infinite - erikoiskorjaus.
         */
        if (normalized.contains(
                "soldierofinfinite"
        )
                ||
                normalized.contains(
                        "so1dierofinfinite"
                )
                ||
                normalized.contains(
                        "sotdierofinfinite"
                )
                ||
                normalized.contains(
                        "soldierofihfinite"
                )
                ||
                normalized.contains(
                        "soldierofihfini"
                )) {

            return "Soldier of the Infinite";
        }

        /*
         * Suora alias.
         */
        String alias =
                OCR_ALIASES.get(
                        normalized
                );

        if (alias != null) {
            return alias;
        }

        /*
         * Täsmällinen tunnettu kortti.
         */
        CardData exact =
                CARDS.get(
                        normalized
                );

        if (exact != null) {
            return exact.name;
        }

        /*
         * Fuzzy matching.
         */
        String bestName =
                "";

        int bestDistance =
                Integer.MAX_VALUE;

        int maxAllowed =
                Math.max(
                        2,
                        normalized.length() / 4
                );

        for (CardData card :
                CARDS.values()) {

            String candidate =
                    normalizeKey(
                            card.name
                    );

            if (candidate.isEmpty()) {
                continue;
            }

            if (candidate.equals(
                    normalized
            )) {

                return card.name;
            }

            if (candidate.contains(
                    normalized
            )
                    ||
                    normalized.contains(
                            candidate
                    )) {

                int distance =
                        Math.abs(
                                candidate.length()
                                        -
                                        normalized.length()
                        );

                if (distance <
                        bestDistance) {

                    bestDistance =
                            distance;

                    bestName =
                            card.name;
                }

                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            if (distance <
                    bestDistance) {

                bestDistance =
                        distance;

                bestName =
                        card.name;
            }
        }

        if (!bestName.isEmpty() &&
                bestDistance <=
                        maxAllowed) {

            return bestName;
        }

        return cleaned;
    }

    private static String cleanInput(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                text.trim();

        result =
                result.replace(
                        "&#039;",
                        "'"
                );

        result =
                result.replace(
                        "&#39;",
                        "'"
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        result =
                result.replaceAll(
                        "^[^A-Za-zÀ-ÿ0-9]+",
                        ""
                );

        result =
                result.replaceAll(
                        "[^A-Za-zÀ-ÿ0-9'&\\-\\.\\s]+$",
                        ""
                );

        result =
                result.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        return result.trim();
    }

    private static String normalizeKey(
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
                        "’",
                        "'"
                )
                .replace(
                        "´",
                        "'"
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    /*
     * -------------------------------------------------------------
     * SCORE
     * -------------------------------------------------------------
     */

    public static String getCardScore(
            String cardName
    ) {

        double value =
                score(
                        cardName
                );

        if (value <= 0.0) {
            return "0.0";
        }

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

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

        if (corrected == null ||
                corrected.trim().isEmpty()) {

            return UNKNOWN_CARD_SCORE;
        }

        String key =
                normalizeKey(
                        corrected
                );

        CardData card =
                CARDS.get(
                        key
                );

        /*
         * Jos online-data on ladattu, yritetään myös
         * suoraan online-nimilistaa.
         */
        if (card == null &&
                onlineDataLoaded) {

            for (String onlineName :
                    ONLINE_NAMES) {

                if (similarKeys(
                        onlineName,
                        key
                )) {

                    card =
                            CARDS.get(
                                    onlineName
                            );

                    if (card != null) {
                        break;
                    }
                }
            }
        }

        if (card == null) {
            return UNKNOWN_CARD_SCORE;
        }

        double result =
                card.score;

        /*
         * Pieni synergy-bonus jo valittujen korttien perusteella.
         */
        result +=
                synergyBonus(
                        card.name
                );

        return result;
    }

    private static double synergyBonus(
            String cardName
    ) {

        if (cardName == null ||
                cardName.isEmpty()) {

            return 0.0;
        }

        String key =
                normalizeKey(
                        cardName
                );

        if (PICKED_CARDS.contains(
                key
        )) {

            return 0.10;
        }

        return 0.0;
    }

    /*
     * -------------------------------------------------------------
     * RECOMMENDATION
     * -------------------------------------------------------------
     */

    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        double score1 =
                score(
                        card1
                );

        double score2 =
                score(
                        card2
                );

        double score3 =
                score(
                        card3
                );

        if (score1 <= 0.0 &&
                score2 <= 0.0 &&
                score3 <= 0.0) {

            return "Kortteja ei tunnistettu";
        }

        if (score1 >= score2 &&
                score1 >= score3) {

            return formatRecommendation(
                    1,
                    card1,
                    score1
            );
        }

        if (score2 >= score1 &&
                score2 >= score3) {

            return formatRecommendation(
                    2,
                    card2,
                    score2
            );
        }

        return formatRecommendation(
                3,
                card3,
                score3
        );
    }

    private static String formatRecommendation(
            int number,
            String cardName,
            double score
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return "Korttia ei tunnistettu";
        }

        return "Kortti " +
                number +
                " – " +
                cardName +
                " (" +
                String.format(
                        Locale.US,
                        "%.2f",
                        score
                ) +
                ")";
    }

    public static String getReason(
            String cardName
    ) {

        double value =
                score(
                        cardName
                );

        if (value <= 0.0) {

            return "Kortille ei löytynyt arvoa.";
        }

        if (value >= 8.0) {

            return "Erittäin vahva Arena-valinta.";
        }

        if (value >= 7.0) {

            return "Vahva Arena-valinta.";
        }

        if (value >= 6.0) {

            return "Hyvä Arena-valinta.";
        }

        if (value >= 5.0) {

            return "Keskitasoinen Arena-valinta.";
        }

        if (value >= 4.0) {

            return "Alle keskitason Arena-valinta.";
        }

        return "Heikko Arena-valinta.";
    }

    public static boolean isValidCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return false;
        }

        return score(
                cardName
        ) > 0.0;
    }

    /*
     * -------------------------------------------------------------
     * PICKED CARDS
     * -------------------------------------------------------------
     */

    public static void recordPickedCard(
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

        if (corrected == null ||
                corrected.trim().isEmpty()) {

            return;
        }

        PICKED_CARDS.add(
                normalizeKey(
                        corrected
                )
        );
    }

    public static int getPickedCount(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return 0;
        }

        String key =
                normalizeKey(
                        correctOcr(
                                cardName
                        )
                );

        if (key.isEmpty()) {
            return 0;
        }

        int count = 0;

        /*
         * Nykyinen rakenne pitää yksinkertaista settiä.
         * Tämä palauttaa 1 jos kortti on jo valittu.
         */
        if (PICKED_CARDS.contains(
                key
        )) {

            count = 1;
        }

        return count;
    }

    public static void clearPickedCards() {

        PICKED_CARDS.clear();
    }

    /*
     * -------------------------------------------------------------
     * FUZZY MATCHING
     * -------------------------------------------------------------
     */

    private static boolean similarKeys(
            String a,
            String b
    ) {

        if (a == null ||
                b == null) {

            return false;
        }

        String aa =
                normalizeKey(
                        a
                );

        String bb =
                normalizeKey(
                        b
                );

        if (aa.equals(
                bb
        )) {

            return true;
        }

        if (aa.contains(
                bb
        )
                ||
                bb.contains(
                        aa
                )) {

            return true;
        }

        int distance =
                levenshtein(
                        aa,
                        bb
                );

        int maxLength =
                Math.max(
                        aa.length(),
                        bb.length()
                );

        return distance <=
                Math.max(
                        2,
                        maxLength / 5
                );
    }

    private static int levenshtein(
            String a,
            String b
    ) {

        if (a == null) {
            return b == null ? 0 : b.length();
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

    /*
     * -------------------------------------------------------------
     * DEBUG / STATUS
     * -------------------------------------------------------------
     */

    public static boolean isOnlineDataLoaded() {

        return onlineDataLoaded;
    }

    public static int getKnownCardCount() {

        return CARDS.size();
    }

    public static int getOnlineCardCount() {

        return ONLINE_NAMES.size();
    }

    public static String getDataStatus() {

        if (onlineDataLoaded) {

            return "HearthArena: " +
                    ONLINE_NAMES.size() +
                    " korttia ladattu";
        }

        return "HearthArena-dataa ladataan...";
    }
}
