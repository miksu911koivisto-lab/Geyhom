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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaAdvisor {

    private static final String TAG =
            "ArenaAdvisor";

    private static final String CARDS_URL =
            "https://api.hearthstonejson.com/v1/latest/enUS/cards.collectible.json";

    private static final String HEARTHARENA_URL =
            "https://www.heartharena.com/tierlist";

    /*
     * Yleiset korttiarvot.
     *
     * Näitä käytetään vain jos oikeaa
     * HearthArena-arvoa ei löydy.
     */
    private static final Map<String, Double> CARDS =
            new HashMap<>();

    /*
     * HearthstoneJSON:n oikeat korttinimet.
     */
    private static final Map<String, String> CANONICAL_NAMES =
            new HashMap<>();

    /*
     * OCR-aliaset.
     */
    private static final Map<String, String> ALIASES =
            new HashMap<>();

    /*
     * HearthstoneJSON-korttitiedot.
     */
    private static final Map<String, CardInfo> CARD_INFO =
            new HashMap<>();

    /*
     * Oikeat class-kohtaiset HearthArena-arvot.
     *
     * kortin nimi -> class -> arvo
     *
     * Esimerkiksi:
     *
     * Bitter End -> MAGE -> 78
     */
    private static final Map<String, Map<String, Double>>
            CLASS_SCORES =
            new HashMap<>();

    /*
     * Draftatut kortit.
     */
    private static final Set<String> PICKED_CARDS =
            new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newFixedThreadPool(2);

    private static volatile boolean onlineLoaded =
            false;

    private static volatile boolean hearthArenaLoaded =
            false;

    private static volatile boolean hearthArenaLoading =
            false;

    private static volatile String status =
            "Ladataan korttitietoja...";

    private static volatile String reason =
            "";

    /*
     * Nykyinen Arena-class.
     */
    private static volatile String currentClass =
            "";

    private static String candidateClass =
            "";

    private static int candidateClassCount =
            0;

    private static final int CLASS_CONFIRMATIONS =
            2;

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
     * Tunnetut testiarvot.
     *
     * Näitä käytetään vain fallbackina.
     */
    static {

        addKnownScore(
                "Temporal Construct",
                5.54
        );

        addKnownScore(
                "Bitter End",
                6.62
        );

        addKnownScore(
                "Sealed Lancer",
                5.56
        );

        addKnownScore(
                "Scaled Lancer",
                5.56
        );

        addKnownScore(
                "Soldier of the Infinite",
                5.80
        );

        addKnownScore(
                "Soldier of the Bronze",
                4.60
        );

        /*
         * OCR-aliaset.
         */
        addAlias(
                "temporalconstruct",
                "Temporal Construct"
        );

        addAlias(
                "bitterend",
                "Bitter End"
        );

        addAlias(
                "sealedlancer",
                "Sealed Lancer"
        );

        addAlias(
                "scaledlancer",
                "Sealed Lancer"
        );

        addAlias(
                "soldierofinfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldierofihfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldierofihfini",
                "Soldier of the Infinite"
        );

        addAlias(
                "so1dierofinfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdierofinfinite",
                "Soldier of the Infinite"
        );
    }

    /*
     * Kortin tietorakenne.
     */
    public static class CardInfo {

        public final String name;
        public final String cardClass;
        public final String type;
        public final String rarity;
        public final int cost;
        public final int attack;
        public final int health;
        public final String text;

        public CardInfo(
                String name,
                String cardClass,
                String type,
                String rarity,
                int cost,
                int attack,
                int health,
                String text
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

            this.cost =
                    cost;

            this.attack =
                    attack;

            this.health =
                    health;

            this.text =
                    text == null
                            ? ""
                            : text;
        }
    }

    /*
     * Lisää yleinen fallback-arvo.
     */
    private static void addKnownScore(
            String name,
            double score
    ) {

        if (name == null ||
                name.trim().isEmpty()) {

            return;
        }

        String key =
                normalize(
                        name
                );

        CARDS.put(
                key,
                score
        );

        CANONICAL_NAMES.put(
                key,
                name
        );
    }

    /*
     * Lisää OCR-alias.
     */
    private static void addAlias(
            String alias,
            String canonical
    ) {

        if (alias == null ||
                canonical == null) {

            return;
        }

        ALIASES.put(
                normalizeCompact(alias),
                canonical
        );
    }

    /*
     * OCR-korjaus.
     */
    public static String correctOcr(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String cleaned =
                cleanName(
                        input
                );

        if (cleaned.isEmpty()) {
            return "";
        }

        String special =
                correctSpecialNames(
                        cleaned
                );

        if (!special.equals(cleaned)) {
            return special;
        }

        String normalized =
                normalize(
                        cleaned
                );

        String canonical =
                CANONICAL_NAMES.get(
                        normalized
                );

        if (canonical != null) {
            return canonical;
        }

        String alias =
                ALIASES.get(
                        normalizeCompact(
                                cleaned
                        )
                );

        if (alias != null) {
            return alias;
        }

        String compact =
                normalizeCompact(
                        cleaned
                );

        for (Map.Entry<String, String> entry :
                CANONICAL_NAMES.entrySet()) {

            String key =
                    normalizeCompact(
                            entry.getKey()
                    );

            if (key.equals(compact)) {
                return entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry :
                ALIASES.entrySet()) {

            if (entry.getKey().equals(compact)) {
                return entry.getValue();
            }
        }

        String fuzzy =
                findFuzzyCanonical(
                        cleaned
                );

        if (fuzzy != null &&
                !fuzzy.isEmpty()) {

            return fuzzy;
        }

        return cleaned;
    }

    /*
     * Kortin piste.
     *
     * Järjestys:
     *
     * 1. HearthArena class-kohtainen arvo
     * 2. tunnettu fallback
     * 3. HearthstoneJSON fallback
     */
    private static double score(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return 0.0;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (corrected == null ||
                corrected.trim().isEmpty()) {

            return 0.0;
        }

        /*
         * 1. Oikea class-kohtainen Arena-arvo.
         */
        String detectedClass =
                currentClass;

        if (detectedClass != null &&
                !detectedClass.trim().isEmpty()) {

            Double classScore =
                    getClassSpecificScore(
                            corrected,
                            detectedClass
                    );

            if (classScore != null) {

                return classScore;
            }
        }

        /*
         * 2. Yleinen tunnettu fallback.
         */
        String normalized =
                normalize(
                        corrected
                );

        Double value =
                CARDS.get(
                        normalized
                );

        if (value != null) {
            return value;
        }

        /*
         * 3. Kompakti fallback-haku.
         */
        String compact =
                normalizeCompact(
                        corrected
                );

        for (Map.Entry<String, Double> entry :
                CARDS.entrySet()) {

            String key =
                    normalizeCompact(
                            entry.getKey()
                    );

            if (key.equals(compact)) {
                return entry.getValue();
            }
        }

        /*
         * 4. HearthstoneJSON fallback.
         */
        CardInfo info =
                getCardInfo(
                        corrected
                );

        if (info != null) {

            return generateFallbackScore(
                    info
            );
        }

        /*
         * 5. Fuzzy fallback.
         */
        String fuzzy =
                findFuzzyCanonical(
                        corrected
                );

        if (fuzzy != null &&
                !fuzzy.isEmpty()) {

            Double fuzzyScore =
                    CARDS.get(
                            normalize(
                                    fuzzy
                            )
                    );

            if (fuzzyScore != null) {
                return fuzzyScore;
            }

            CardInfo fuzzyInfo =
                    getCardInfo(
                            fuzzy
                    );

            if (fuzzyInfo != null) {

                return generateFallbackScore(
                        fuzzyInfo
                );
            }
        }

        return 0.0;
    }

    /*
     * Hakee class-kohtaisen HearthArena-arvon.
     */
    private static Double getClassSpecificScore(
            String cardName,
            String className
    ) {

        if (cardName == null ||
                className == null) {

            return null;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        String normalizedCard =
                normalize(
                        corrected
                );

        String normalizedClass =
                normalizeClass(
                        className
                );

        if (normalizedCard.isEmpty() ||
                normalizedClass.isEmpty()) {

            return null;
        }

        Map<String, Double> values =
                CLASS_SCORES.get(
                        normalizedCard
                );

        if (values != null) {

            Double score =
                    values.get(
                            normalizedClass
                    );

            if (score != null) {
                return score;
            }
        }

        /*
         * Compact-haku.
         */
        String compact =
                normalizeCompact(
                        corrected
                );

        for (Map.Entry<String,
                Map<String, Double>> entry :
                CLASS_SCORES.entrySet()) {

            if (normalizeCompact(
                    entry.getKey()
            ).equals(compact)) {

                Map<String, Double> classValues =
                        entry.getValue();

                if (classValues != null) {

                    return classValues.get(
                            normalizedClass
                    );
                }
            }
        }

        return null;
    }

    /*
     * Julkinen class-score API.
     */
    public static void setClassScore(
            String cardName,
            String className,
            double score
    ) {

        if (cardName == null ||
                className == null ||
                cardName.trim().isEmpty() ||
                className.trim().isEmpty()) {

            return;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        String normalizedClass =
                normalizeClass(
                        className
                );

        if (corrected.isEmpty() ||
                normalizedClass.isEmpty()) {

            return;
        }

        String key =
                normalize(
                        corrected
                );

        Map<String, Double> values =
                CLASS_SCORES.get(
                        key
                );

        if (values == null) {

            values =
                    new HashMap<>();

            CLASS_SCORES.put(
                    key,
                    values
            );
        }

        values.put(
                normalizedClass,
                score
        );
    }

    /*
     * Poista class-kohtaiset arvot.
     */
    public static void clearClassScores() {

        synchronized (CLASS_SCORES) {

            CLASS_SCORES.clear();
        }

        hearthArenaLoaded =
                false;
    }

    /*
     * Kortin piste tekstinä.
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
     * Numeerinen piste.
     */
    public static double getCardScoreValue(
            String cardName
    ) {

        return score(
                cardName
        );
    }

    /*
     * Overload.
     */
    public static String getCardScore(
            double value
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    /*
     * Suositus kolmesta kortista.
     */
    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        double score1 =
                score(card1);

        double score2 =
                score(card2);

        double score3 =
                score(card3);

        String best =
                "";

        double bestScore =
                -Double.MAX_VALUE;

        if (card1 != null &&
                !card1.trim().isEmpty() &&
                score1 > bestScore) {

            best =
                    correctOcr(
                            card1
                    );

            bestScore =
                    score1;
        }

        if (card2 != null &&
                !card2.trim().isEmpty() &&
                score2 > bestScore) {

            best =
                    correctOcr(
                            card2
                    );

            bestScore =
                    score2;
        }

        if (card3 != null &&
                !card3.trim().isEmpty() &&
                score3 > bestScore) {

            best =
                    correctOcr(
                            card3
                    );

            bestScore =
                    score3;
        }

        if (best.isEmpty()) {
            return "Ei suositusta";
        }

        return best;
    }

    /*
     * Draftatun kortin tallennus.
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

        if (!corrected.isEmpty()) {

            synchronized (PICKED_CARDS) {

                PICKED_CARDS.add(
                        normalize(
                                corrected
                        )
                );
            }
        }
    }

    public static void addPickedCard(
            String cardName
    ) {

        recordPickedCard(
                cardName
        );
    }

    /*
     * Onko kortti jo valittu?
     */
    public static boolean wasPicked(
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

        if (corrected.isEmpty()) {
            return false;
        }

        synchronized (PICKED_CARDS) {

            return PICKED_CARDS.contains(
                    normalize(
                            corrected
                    )
            );
        }
    }

    public static void clearPickedCards() {

        synchronized (PICKED_CARDS) {

            PICKED_CARDS.clear();
        }
    }

    /*
     * Class-tunnistus.
     */
    public static synchronized void detectClassFromCards(
            String card1,
            String card2,
            String card3
    ) {

        if (isClassDetected()) {
            return;
        }

        String detected =
                "";

        String class1 =
                getClassForCard(card1);

        String class2 =
                getClassForCard(card2);

        String class3 =
                getClassForCard(card3);

        if (!class1.isEmpty()) {

            detected =
                    class1;

        } else if (!class2.isEmpty()) {

            detected =
                    class2;

        } else if (!class3.isEmpty()) {

            detected =
                    class3;
        }

        if (detected.isEmpty()) {
            return;
        }

        if ((!class1.isEmpty() &&
                !class1.equals(detected))
                ||
                (!class2.isEmpty() &&
                        !class2.equals(detected))
                ||
                (!class3.isEmpty() &&
                        !class3.equals(detected))) {

            candidateClass =
                    "";

            candidateClassCount =
                    0;

            return;
        }

        if (detected.equals(
                candidateClass
        )) {

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

            candidateClass =
                    "";

            candidateClassCount =
                    0;

            Log.d(
                    TAG,
                    "Arena class tunnistettu: "
                            + currentClass
            );

            /*
             * Kun class on tunnistettu,
             * varmistetaan että HearthArena-data
             * on latautumassa.
             */
            loadHearthArenaScores();
        }
    }

    /*
     * Selvittää kortin classin.
     */
    private static String getClassForCard(
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

        if (corrected.isEmpty()) {
            return "";
        }

        CardInfo info =
                getCardInfo(
                        corrected
                );

        if (info == null) {
            return "";
        }

        return normalizeClass(
                info.cardClass
        );
    }

    /*
     * Nykyinen class.
     */
    public static String getCurrentClass() {

        String value =
                currentClass;

        if (value == null ||
                value.trim().isEmpty()) {

            return "EI TUNNISTETTU";
        }

        return value;
    }

    public static String getCurrentClassRaw() {

        return currentClass == null
                ? ""
                : currentClass;
    }

    public static boolean isClassDetected() {

        return currentClass != null &&
                !currentClass.trim().isEmpty();
    }

    public static synchronized void resetClassDetection() {

        currentClass =
                "";

        candidateClass =
                "";

        candidateClassCount =
                0;
    }

    /*
     * Lataa HearthstoneJSON.
     */
    public static void loadCards() {

        if (onlineLoaded) {
            return;
        }

        EXECUTOR.execute(() -> {

            HttpURLConnection connection =
                    null;

            try {

                status =
                        "Ladataan korttitietoja...";

                reason =
                        "";

                URL url =
                        new URL(
                                CARDS_URL
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

                connection.setUseCaches(
                        true
                );

                int responseCode =
                        connection.getResponseCode();

                if (responseCode !=
                        HttpURLConnection.HTTP_OK) {

                    status =
                            "Korttidatan lataus epäonnistui";

                    reason =
                            "HTTP "
                                    + responseCode;

                    return;
                }

                InputStream stream =
                        connection.getInputStream();

                StringBuilder builder =
                        new StringBuilder();

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

                    builder.append(
                            line
                    );
                }

                reader.close();

                parseCards(
                        builder.toString()
                );

                onlineLoaded =
                        !CARD_INFO.isEmpty();

                if (onlineLoaded) {

                    status =
                            "Korttidat ladattu";

                    reason =
                            "Kortteja: "
                                    + CARD_INFO.size();

                } else {

                    status =
                            "Korttidata tyhjä";

                    reason =
                            "JSON ei sisältänyt kortteja";
                }

            } catch (Exception e) {

                status =
                        "Korttidatan lataus epäonnistui";

                reason =
                        e.getClass()
                                .getSimpleName()
                                + ": "
                                + String.valueOf(
                                        e.getMessage()
                                );

                Log.e(
                        TAG,
                        "Korttidatan lataus epäonnistui",
                        e
                );

            } finally {

                if (connection != null) {

                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /*
     * Parsii HearthstoneJSON:n.
     */
    private static synchronized void parseCards(
            String json
    ) {

        if (json == null ||
                json.trim().isEmpty()) {

            return;
        }

        try {

            JSONArray array =
                    new JSONArray(
                            json
                    );

            for (
                    int i = 0;
                    i < array.length();
                    i++
            ) {

                JSONObject object =
                        array.optJSONObject(
                                i
                        );

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
                        );

                String type =
                        object.optString(
                                "type",
                                ""
                        );

                String rarity =
                        object.optString(
                                "rarity",
                                ""
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

                String text =
                        object.optString(
                                "text",
                                ""
                        );

                CardInfo info =
                        new CardInfo(
                                name,
                                cardClass,
                                type,
                                rarity,
                                cost,
                                attack,
                                health,
                                text
                        );

                String key =
                        normalize(
                                name
                        );

                CARD_INFO.put(
                        key,
                        info
                );

                CANONICAL_NAMES.put(
                        key,
                        name
                );

                if (!CARDS.containsKey(key)) {

                    CARDS.put(
                            key,
                            generateFallbackScore(
                                    info
                            )
                    );
                }
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Korttien JSON-parseri epäonnistui",
                    e
            );
        }
    }

    /*
     * ============================================================
     * HEARTHARENA
     * ============================================================
     *
     * HearthArena ei tarjoa virallista julkista API:a.
     *
     * Tämä hakee tierlist-sivun ja etsii sen tekstistä
     * class-kohtaisia:
     *
     * Card Name
     * score
     *
     * pareja.
     *
     * Jos sivu ei ole saatavilla tai anti-bot estää pyynnön,
     * sovellus jatkaa vanhalla fallbackilla.
     */

    public static void loadHearthArenaScores() {

        if (hearthArenaLoaded ||
                hearthArenaLoading) {

            return;
        }

        hearthArenaLoading =
                true;

        EXECUTOR.execute(() -> {

            HttpURLConnection connection =
                    null;

            try {

                Log.d(
                        TAG,
                        "Ladataan HearthArena tierlist..."
                );

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

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 16) "
                                + "AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) "
                                + "Chrome Mobile Safari/537.36"
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

                if (responseCode !=
                        HttpURLConnection.HTTP_OK) {

                    Log.w(
                            TAG,
                            "HearthArena HTTP "
                                    + responseCode
                    );

                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder html =
                        new StringBuilder();

                String line;

                while ((line =
                        reader.readLine()) != null) {

                    html.append(
                            line
                    ).append(
                            '\n'
                    );
                }

                reader.close();

                int before =
                        getClassScoreCount();

                parseHearthArenaPage(
                        html.toString()
                );

                int after =
                        getClassScoreCount();

                if (after > before) {

                    hearthArenaLoaded =
                            true;

                    status =
                            "HearthArena-arvot ladattu";

                    reason =
                            "Arena-arvoja: "
                                    + after;

                    Log.d(
                            TAG,
                            "HearthArena-arvoja ladattu: "
                                    + after
                    );

                } else {

                    Log.w(
                            TAG,
                            "HearthArena-sivulta ei löytynyt "
                                    + "class-kohtaisia arvoja"
                    );
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "HearthArena-datan lataus epäonnistui",
                        e
                );

            } finally {

                hearthArenaLoading =
                        false;

                if (connection != null) {

                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    /*
     * Parsii HearthArena HTML:n.
     *
     * Tärkeä periaate:
     * emme korvaa vanhaa dataa ennen kuin uusia arvoja
     * todella löytyi.
     */
    private static synchronized void parseHearthArenaPage(
            String html
    ) {

        if (html == null ||
                html.trim().isEmpty()) {

            return;
        }

        String page =
                html
                        .replace(
                                "\\u003c",
                                "<"
                        )
                        .replace(
                                "\\u003e",
                                ">"
                        )
                        .replace(
                                "&nbsp;",
                                " "
                        )
                        .replace(
                                "&amp;",
                                "&"
                        )
                        .replace(
                                "&#039;",
                                "'"
                        )
                        .replace(
                                "&quot;",
                                "\""
                        );

        /*
         * Yritetään ensin tunnistaa class-osioita.
         */
        for (String className :
                VALID_CLASSES) {

            parseHearthArenaClassSection(
                    page,
                    className
            );
        }

        /*
         * Tämän lisäksi luetaan suoraan HearthArena-sivun
         * tunnettu taulukkomuoto:
         *
         * Mage | Card | Mage | 86 | 78
         *
         * Käytetään viimeistä score-arvoa.
         */
        parseHearthArenaRows(
                page
        );
    }

    /*
     * Etsii class-kohtaisia osioita.
     */
    private static void parseHearthArenaClassSection(
            String html,
            String className
    ) {

        String escapedClass =
                Pattern.quote(
                        className
                );

        Pattern headingPattern =
                Pattern.compile(
                        "(?is)"
                                + escapedClass
                                + ".{0,1500}?"
                                + "(?="
                                + "DEATH\\s+KNIGHT"
                                + "|DEMON\\s+HUNTER"
                                + "|DRUID"
                                + "|HUNTER"
                                + "|MAGE"
                                + "|PALADIN"
                                + "|PRIEST"
                                + "|ROGUE"
                                + "|SHAMAN"
                                + "|WARLOCK"
                                + "|WARRIOR"
                                + "|$)",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher sectionMatcher =
                headingPattern.matcher(
                        html
                );

        while (sectionMatcher.find()) {

            String section =
                    sectionMatcher.group();

            if (section == null ||
                    section.isEmpty()) {

                continue;
            }

            parseScorePairs(
                    section,
                    className
            );
        }
    }

    /*
     * Etsii kortin ja numeron pareja.
     *
     * Tämä on tarkoituksella varovainen:
     * arvo hyväksytään vain kun se on 0-140.
     */
    private static void parseScorePairs(
            String section,
            String className
    ) {

        String clean =
                stripHtml(
                        section
                );

        clean =
                clean.replaceAll(
                        "\\s+",
                        " "
                );

        /*
         * Tunnettu HearthArena-rivin muoto:
         *
         * 1. Card Name 78
         *
         * Yritetään ensin rivipohjaisesti.
         */
        String[] parts =
                clean.split(
                        "(?=\\d+\\.\\s+)"
                );

        for (String part :
                parts) {

            if (part == null) {
                continue;
            }

            Matcher matcher =
                    Pattern.compile(
                            "(?s)"
                                    + "\\d+\\.\\s+"
                                    + "(.{2,100}?)"
                                    + "\\s+"
                                    + "(\\d{1,3})(?:\\s|$)"
                    ).matcher(
                            part
                    );

            if (!matcher.find()) {
                continue;
            }

            String name =
                    cleanupCardName(
                            matcher.group(1)
                    );

            int value;

            try {

                value =
                        Integer.parseInt(
                                matcher.group(2)
                        );

            } catch (Exception e) {

                continue;
            }

            addHearthArenaScore(
                    name,
                    className,
                    value
            );
        }
    }

    /*
     * Parsii taulukkomuotoisia rivejä.
     */
    private static void parseHearthArenaRows(
            String html
    ) {

        String text =
                stripHtml(
                        html
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                );

        /*
         * Esimerkiksi:
         *
         * Mage | Bitter End | Neutral | 86 | 78
         *
         * Viimeinen numero on nykyinen arvo.
         */
        Pattern pattern =
                Pattern.compile(
                        "(?i)"
                                + "(Death Knight|"
                                + "Demon Hunter|"
                                + "Druid|"
                                + "Hunter|"
                                + "Mage|"
                                + "Paladin|"
                                + "Priest|"
                                + "Rogue|"
                                + "Shaman|"
                                + "Warlock|"
                                + "Warrior)"
                                + "\\s*\\|\\s*"
                                + "([^|]{2,100}?)"
                                + "\\s*\\|\\s*"
                                + "[^|]{1,40}"
                                + "\\s*\\|\\s*"
                                + "\\d{1,3}"
                                + "\\s*\\|\\s*"
                                + "(\\d{1,3})",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                pattern.matcher(
                        text
                );

        while (matcher.find()) {

            String className =
                    normalizeClass(
                            matcher.group(1)
                    );

            String cardName =
                    cleanupCardName(
                            matcher.group(2)
                    );

            int score;

            try {

                score =
                        Integer.parseInt(
                                matcher.group(3)
                        );

            } catch (Exception e) {

                continue;
            }

            addHearthArenaScore(
                    cardName,
                    className,
                    score
            );
        }
    }

    /*
     * Lisää HearthArena-arvon.
     */
    private static void addHearthArenaScore(
            String cardName,
            String className,
            int score
    ) {

        if (cardName == null ||
                className == null) {

            return;
        }

        if (score < 0 ||
                score > 140) {

            return;
        }

        String cleaned =
                cleanupCardName(
                        cardName
                );

        if (cleaned.isEmpty()) {
            return;
        }

        String corrected =
                correctOcr(
                        cleaned
                );

        if (corrected.isEmpty()) {
            corrected =
                    cleaned;
        }

        String normalizedClass =
                normalizeClass(
                        className
                );

        if (normalizedClass.isEmpty()) {
            return;
        }

        String key =
                normalize(
                        corrected
                );

        Map<String, Double> values =
                CLASS_SCORES.get(
                        key
                );

        if (values == null) {

            values =
                    new HashMap<>();

            CLASS_SCORES.put(
                    key,
                    values
            );
        }

        values.put(
                normalizedClass,
                (double) score
        );

        /*
         * Varmistetaan että canonical-nimi tunnetaan.
         */
        CANONICAL_NAMES.put(
                key,
                corrected
        );
    }

    /*
     * Poistaa HTML-tagit.
     */
    private static String stripHtml(
            String html
    ) {

        if (html == null) {
            return "";
        }

        String value =
                html;

        value =
                value.replaceAll(
                        "(?is)<script.*?</script>",
                        " "
                );

        value =
                value.replaceAll(
                        "(?is)<style.*?</style>",
                        " "
                );

        value =
                value.replaceAll(
                        "(?s)<[^>]+>",
                        " "
                );

        value =
                value.replace(
                        "&nbsp;",
                        " "
                );

        value =
                value.replace(
                        "&amp;",
                        "&"
                );

        value =
                value.replace(
                        "&#039;",
                        "'"
                );

        value =
                value.replace(
                        "&quot;",
                        "\""
                );

        return value;
    }

    /*
     * Kortin nimen siivous.
     */
    private static String cleanupCardName(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String name =
                value.trim();

        name =
                name.replaceAll(
                        "^\\d+\\.\\s*",
                        ""
                );

        name =
                name.replaceAll(
                        "\\s+",
                        " "
                );

        name =
                name.replaceAll(
                        "\\s+\\d{1,3}$",
                        ""
                );

        name =
                name.replaceAll(
                        "^[|:\\-\\s]+",
                        ""
                );

        name =
                name.replaceAll(
                        "[|:\\-\\s]+$",
                        ""
                );

        return name.trim();
    }

    /*
     * Fallback score.
     */
    private static double generateFallbackScore(
            CardInfo info
    ) {

        if (info == null) {
            return 0.0;
        }

        double score =
                3.0;

        if ("MINION".equalsIgnoreCase(
                info.type
        )) {

            score +=
                    info.attack * 0.18;

            score +=
                    info.health * 0.14;
        }

        if (info.cost > 0) {

            double expectedStats =
                    info.cost * 1.45;

            double actualStats =
                    info.attack
                            +
                    info.health;

            double difference =
                    actualStats
                            -
                    expectedStats;

            score +=
                    difference * 0.15;
        }

        String text =
                info.text == null
                        ? ""
                        : info.text.toLowerCase(
                                Locale.US
                        );

        if (text.contains("draw")) {
            score += 0.35;
        }

        if (text.contains("discover")) {
            score += 0.45;
        }

        if (text.contains("battlecry")) {
            score += 0.20;
        }

        if (text.contains("deathrattle")) {
            score += 0.20;
        }

        if (text.contains("rush")) {
            score += 0.20;
        }

        if (text.contains("taunt")) {
            score += 0.20;
        }

        if (text.contains("lifesteal")) {
            score += 0.20;
        }

        if (text.contains("windfury")) {
            score += 0.25;
        }

        if (text.contains("divine shield")) {
            score += 0.30;
        }

        if (text.contains("spell damage")) {
            score += 0.20;
        }

        if (text.contains("deal ") &&
                text.contains(" damage")) {

            score += 0.20;
        }

        if (text.contains("destroy")) {
            score += 0.25;
        }

        if (text.contains("silence")) {
            score += 0.25;
        }

        if (text.contains("freeze")) {
            score += 0.15;
        }

        if (text.contains("secret")) {
            score += 0.10;
        }

        if ("LEGENDARY".equalsIgnoreCase(
                info.rarity
        )) {

            score += 0.10;

        } else if ("EPIC".equalsIgnoreCase(
                info.rarity
        )) {

            score += 0.05;
        }

        if (score < 0.0) {
            score = 0.0;
        }

        if (score > 8.0) {
            score = 8.0;
        }

        return score;
    }

    /*
     * OCR-erikoiskorjaukset.
     */
    private static String correctSpecialNames(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String normalized =
                normalizeCompact(
                        text
                );

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

        if (normalized.equals(
                "temporalconstruct"
        )) {

            return "Temporal Construct";
        }

        if (normalized.equals(
                "bitterend"
        )) {

            return "Bitter End";
        }

        if (normalized.equals(
                "sealedlancer"
        )
                ||
                normalized.equals(
                        "scaledlancer"
                )) {

            return "Sealed Lancer";
        }

        return text;
    }

    /*
     * Fuzzy-haku.
     */
    private static String findFuzzyCanonical(
            String input
    ) {

        if (input == null ||
                input.trim().isEmpty()) {

            return "";
        }

        String target =
                normalizeCompact(
                        input
                );

        if (target.isEmpty()) {
            return "";
        }

        String bestName =
                "";

        int bestDistance =
                Integer.MAX_VALUE;

        for (String canonical :
                CANONICAL_NAMES.values()) {

            String candidate =
                    normalizeCompact(
                            canonical
                    );

            if (candidate.isEmpty()) {
                continue;
            }

            if (candidate.equals(target)) {
                return canonical;
            }

            if (candidate.contains(target) ||
                    target.contains(candidate)) {

                int difference =
                        Math.abs(
                                candidate.length()
                                        -
                                target.length()
                        );

                if (difference <= 5 &&
                        difference < bestDistance) {

                    bestDistance =
                            difference;

                    bestName =
                            canonical;
                }

                continue;
            }

            int distance =
                    levenshtein(
                            target,
                            candidate
                    );

            int maxLength =
                    Math.max(
                            target.length(),
                            candidate.length()
                    );

            int allowed =
                    Math.max(
                            2,
                            maxLength / 5
                    );

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance =
                        distance;

                bestName =
                        canonical;
            }
        }

        return bestName;
    }

    /*
     * Kortin tiedot.
     */
    private static CardInfo getCardInfo(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return null;
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

        if (info != null) {
            return info;
        }

        String compact =
                normalizeCompact(
                        corrected
                );

        for (Map.Entry<String, CardInfo> entry :
                CARD_INFO.entrySet()) {

            if (normalizeCompact(
                    entry.getKey()
            ).equals(compact)) {

                return entry.getValue();
            }
        }

        return null;
    }

    /*
     * Normalisointi.
     */
    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String value =
                text.trim()
                        .toLowerCase(
                                Locale.US
                        );

        value =
                value.replace(
                        "&#039;",
                        "'"
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    /*
     * Kompakti normalisointi.
     */
    private static String normalizeCompact(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text.toLowerCase(
                        Locale.US
                )
                .replace(
                        "&#039;",
                        "'"
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    /*
     * OCR-tekstin puhdistus.
     */
    private static String cleanName(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String value =
                text.trim();

        value =
                value.replace(
                        "&#039;",
                        "'"
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        value =
                value.replaceAll(
                        "^[^A-Za-zÀ-ÿ0-9]+",
                        ""
                );

        value =
                value.replaceAll(
                        "[^A-Za-zÀ-ÿ0-9'&\\-\\.\\s]+$",
                        ""
                );

        value =
                value.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        return value.trim();
    }

    /*
     * Class-normalisointi.
     */
    private static String normalizeClass(
            String className
    ) {

        if (className == null) {
            return "";
        }

        String value =
                className.trim()
                        .toUpperCase(
                                Locale.US
                        );

        value =
                value.replace(
                        "_",
                        " "
                );

        value =
                value.replace(
                        "-",
                        " "
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                )
                .trim();

        if (value.equals(
                "DEATHKNIGHT"
        )) {

            return "DEATH KNIGHT";
        }

        if (value.equals(
                "DEMONHUNTER"
        )) {

            return "DEMON HUNTER";
        }

        for (String valid :
                VALID_CLASSES) {

            if (valid.equals(
                    value
            )) {

                return valid;
            }
        }

        return "";
    }

    /*
     * Levenshtein.
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

        for (
                int i = 0;
                i <= a.length();
                i++
        ) {

            dp[i][0] =
                    i;
        }

        for (
                int j = 0;
                j <= b.length();
                j++
        ) {

            dp[0][j] =
                    j;
        }

        for (
                int i = 1;
                i <= a.length();
                i++
        ) {

            for (
                    int j = 1;
                    j <= b.length();
                    j++
            ) {

                int cost =
                        a.charAt(i - 1)
                                ==
                        b.charAt(j - 1)
                                ? 0
                                : 1;

                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j]
                                                + 1,
                                        dp[i][j - 1]
                                                + 1
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
     * Status.
     */
    public static boolean isOnlineLoaded() {

        return onlineLoaded;
    }

    public static int getKnownCardCount() {

        return CARD_INFO.size();
    }

    public static String getStatus() {

        return status;
    }

    public static String getReason() {

        return reason;
    }

    /*
     * Kuinka monta oikeaa class-kohtaista
     * HearthArena-arvoa on ladattu.
     */
    public static int getClassScoreCount() {

        int count =
                0;

        synchronized (CLASS_SCORES) {

            for (Map<String, Double> values :
                    CLASS_SCORES.values()) {

                if (values != null) {

                    count +=
                            values.size();
                }
            }
        }

        return count;
    }

    public static String getCardClass(
            String cardName
    ) {

        return getClassForCard(
                cardName
        );
    }

    public static CardInfo getCardData(
            String cardName
    ) {

        return getCardInfo(
                cardName
        );
    }

    /*
     * Käynnistetään molemmat datalataukset.
     */
    static {

        loadCards();

        loadHearthArenaScores();
    }
}
