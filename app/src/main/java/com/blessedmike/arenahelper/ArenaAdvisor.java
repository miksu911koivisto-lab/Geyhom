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

    private static final String TAG =
            "ArenaAdvisor";

    private static final String CARDS_URL =
            "https://api.hearthstonejson.com/v1/latest/enUS/cards.collectible.json";

    /*
     * Yleiset korttiarvot.
     *
     * Näitä käytetään silloin kun:
     *  - classia ei ole vielä tunnistettu
     *  - kortille ei ole class-kohtaista arvoa
     */
    private static final Map<String, Double> CARDS =
            new HashMap<>();

    /*
     * Kortin oikea HearthstoneJSON-nimi.
     */
    private static final Map<String, String> CANONICAL_NAMES =
            new HashMap<>();

    /*
     * OCR-korjaukset.
     */
    private static final Map<String, String> ALIASES =
            new HashMap<>();

    /*
     * Täydet korttitiedot HearthstoneJSON:sta.
     */
    private static final Map<String, CardInfo> CARD_INFO =
            new HashMap<>();

    /*
     * Class-kohtaiset Arena-arvot.
     *
     * Rakenne:
     *
     * kortin nimi
     *      ->
     * class
     *      ->
     * piste
     *
     * Esimerkiksi:
     *
     * CLASS_SCORES.get("CARD NAME").get("MAGE")
     *
     * Tähän ei laiteta keksittyjä arvoja.
     */
    private static final Map<String, Map<String, Double>>
            CLASS_SCORES =
            new HashMap<>();

    /*
     * Jo draftatut kortit.
     */
    private static final Set<String> PICKED_CARDS =
            new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static volatile boolean onlineLoaded =
            false;

    private static volatile String status =
            "Ladataan korttitietoja...";

    private static volatile String reason =
            "";

    /*
     * Automaattinen class-tunnistus.
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
     * Tunnetut pisteet, joita nykyinen toimiva versio
     * käyttää.
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
         * OCR-aliases.
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

            this.name = name == null
                    ? ""
                    : name;

            this.cardClass = cardClass == null
                    ? ""
                    : cardClass;

            this.type = type == null
                    ? ""
                    : type;

            this.rarity = rarity == null
                    ? ""
                    : rarity;

            this.cost = cost;
            this.attack = attack;
            this.health = health;

            this.text = text == null
                    ? ""
                    : text;
        }
    }

    /*
     * Lisää yleinen piste.
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
     * Julkinen OCR-korjaus.
     *
     * CaptureService käyttää tätä suoraan.
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

        /*
         * Suora canonical-haku.
         */
        String canonical =
                CANONICAL_NAMES.get(
                        normalized
                );

        if (canonical != null) {
            return canonical;
        }

        /*
         * Alias-haku.
         */
        String alias =
                ALIASES.get(
                        normalizeCompact(
                                cleaned
                        )
                );

        if (alias != null) {
            return alias;
        }

        /*
         * Kompakti haku.
         */
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

        /*
         * Alias voi löytyä hieman erilaisena
         * versiona.
         */
        for (Map.Entry<String, String> entry :
                ALIASES.entrySet()) {

            if (entry.getKey().equals(compact)) {

                return entry.getValue();
            }
        }

        /*
         * Fuzzy matching.
         */
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
     * Class-kohtainen arvo tarkistetaan ensin.
     * Jos sitä ei ole, käytetään yleistä arvoa.
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
         * 1. Class-kohtainen Arena-arvo.
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
         * 2. Yleinen tarkka arvo.
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
         * 3. Kompakti tarkka haku.
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
         * 4. HearthstoneJSON-tietojen perusteella
         *    muodostettu fallback.
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
         * 5. Fuzzy matching tunnetuista arvoista.
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
     * Class-kohtainen piste.
     */
    private static Double getClassSpecificScore(
            String cardName,
            String className
    ) {

        if (cardName == null ||
                className == null) {

            return null;
        }

        String normalizedCard =
                normalize(
                        cardName
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

        if (values == null) {

            /*
             * Tarkistetaan vielä compact-nimellä.
             */
            String compact =
                    normalizeCompact(
                            cardName
                    );

            for (Map.Entry<String,
                    Map<String, Double>> entry :
                    CLASS_SCORES.entrySet()) {

                if (normalizeCompact(
                        entry.getKey()
                ).equals(compact)) {

                    values =
                            entry.getValue();

                    break;
                }
            }
        }

        if (values == null) {
            return null;
        }

        return values.get(
                normalizedClass
        );
    }

    /*
     * Julkinen metodi, jolla class-kohtainen
     * arvo voidaan lisätä myöhemmin.
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
     * Poista kaikki class-kohtaiset arvot.
     */
    public static void clearClassScores() {

        synchronized (CLASS_SCORES) {

            CLASS_SCORES.clear();
        }
    }

    /*
     * Palauttaa kortin pisteen tekstinä.
     *
     * CaptureService käyttää tätä.
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
     * Palauttaa numeerisen pisteen.
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
     * Tallennetaan draftattu kortti.
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

    /*
     * Alias vanhalle API:lle.
     */
    public static void addPickedCard(
            String cardName
    ) {

        recordPickedCard(
                cardName
        );
    }

    /*
     * Tarkistaa onko kortti jo valittu.
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

    /*
     * Tyhjennä draftatut kortit.
     */
    public static void clearPickedCards() {

        synchronized (PICKED_CARDS) {

            PICKED_CARDS.clear();
        }
    }

    /*
     * Automaattinen class-tunnistus.
     *
     * Tämän CaptureService kutsuu jokaisen
     * varmennetun kolmen kortin tarjouksen jälkeen.
     */
    public static synchronized void detectClassFromCards(
            String card1,
            String card2,
            String card3
    ) {

        /*
         * Jos class on jo lukittu, ei vaihdeta
         * sitä kesken draftin.
         */
        if (isClassDetected()) {
            return;
        }

        String detected =
                "";

        String class1 =
                getClassForCard(
                        card1
                );

        String class2 =
                getClassForCard(
                        card2
                );

        String class3 =
                getClassForCard(
                        card3
                );

        /*
         * Otetaan ensimmäinen class-kohtainen kortti.
         */
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

        /*
         * Jos kaikki kolme ovat neutraaleja,
         * tästä tarjouksesta ei voida päätellä classia.
         */
        if (detected.isEmpty()) {
            return;
        }

        /*
         * Jos samassa tarjouksessa löytyy toinen
         * class-kohtainen kortti eri classille,
         * tarjousta ei käytetä tunnistukseen.
         */
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

        /*
         * Sama havainto uudestaan.
         */
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

        /*
         * Kaksi peräkkäistä varmistusta.
         */
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

    /*
     * Raaka class-arvo.
     */
    public static String getCurrentClassRaw() {

        return currentClass == null
                ? ""
                : currentClass;
    }

    /*
     * Onko class tunnistettu?
     */
    public static boolean isClassDetected() {

        return currentClass != null &&
                !currentClass.trim().isEmpty();
    }

    /*
     * Class-tunnistuksen resetointi.
     */
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
     *
     * HearthstoneJSON:n collectible-tiedosto sisältää
     * korttien cardClass-, type-, rarity-, cost-,
     * attack-, health- ja text-kentät.
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

                    onlineLoaded =
                            false;

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

                onlineLoaded =
                        false;

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
     * JSON-parseri.
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

                /*
                 * Jos kortille ei ole ennestään
                 * tunnettua arvoa, tehdään fallback.
                 *
                 * Tämä ei korvaa oikeaa Arena-ratingia.
                 */
                if (!CARDS.containsKey(key)) {

                    double fallback =
                            generateFallbackScore(
                                    info
                            );

                    CARDS.put(
                            key,
                            fallback
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
     * Hakee kortin tiedot.
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
     * Fallback-arvo kortin ominaisuuksista.
     *
     * HUOM:
     * Tämä ei ole HearthArena-rating.
     */
    private static double generateFallbackScore(
            CardInfo info
    ) {

        if (info == null) {
            return 0.0;
        }

        double score =
                3.0;

        /*
         * Minionin perusrunko.
         */
        if ("MINION".equalsIgnoreCase(
                info.type
        )) {

            score +=
                    info.attack * 0.18;

            score +=
                    info.health * 0.14;
        }

        /*
         * Mana-costin karkea vaikutus.
         */
        if (info.cost > 0) {

            double expectedStats =
                    info.cost * 1.45;

            double actualStats =
                    info.attack
                            +
                    info.health;

            double statDifference =
                    actualStats
                            -
                    expectedStats;

            score +=
                    statDifference * 0.15;
        }

        /*
         * Spellien ja korttitekstin karkea
         * lisäarvio.
         */
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

        /*
         * Harvinaisuus vaikuttaa vain vähän,
         * koska rarity ei yksin kerro Arena-arvoa.
         */
        if ("LEGENDARY".equalsIgnoreCase(
                info.rarity
        )) {

            score += 0.10;

        } else if ("EPIC".equalsIgnoreCase(
                info.rarity
        )) {

            score += 0.05;
        }

        /*
         * Pidetään fallback järkevällä välillä.
         */
        if (score < 0.0) {
            score = 0.0;
        }

        if (score > 8.0) {
            score = 8.0;
        }

        return score;
    }

    /*
     * Erityiset OCR-korjaukset.
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
     * Fuzzy canonical-haku.
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

                int lengthDifference =
                        Math.abs(
                                candidate.length()
                                        -
                                target.length()
                        );

                if (lengthDifference <= 5 &&
                        lengthDifference <
                                bestDistance) {

                    bestDistance =
                            lengthDifference;

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
     * Normalisoi kortin nimen.
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
     * Poistaa välilyönnit ja OCR:n kannalta
     * epäolennaiset merkit.
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
     * Puhdistaa OCR-tekstin.
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

    /*
     * Tunnettujen korttien määrä.
     */
    public static int getKnownCardCount() {

        return CARD_INFO.size();
    }

    /*
     * Status-teksti.
     */
    public static String getStatus() {

        return status;
    }

    /*
     * Virheen syy.
     */
    public static String getReason() {

        return reason;
    }

    /*
     * Class-kohtaisen datan määrä.
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

    /*
     * Palauttaa kortin classin.
     */
    public static String getCardClass(
            String cardName
    ) {

        return getClassForCard(
                cardName
        );
    }

    /*
     * Palauttaa korttitiedot.
     */
    public static CardInfo getCardData(
            String cardName
    ) {

        return getCardInfo(
                cardName
        );
    }

    /*
     * Käynnistetään korttidatan lataus
     * automaattisesti, kun classia käytetään.
     */
    static {

        loadCards();
    }
}
