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

public final class ArenaAdvisor {

    private static final String TAG = "ArenaAdvisor";

    private static final String CARD_DATABASE_URL =
            "https://api.hearthstonejson.com/v1/latest/enUS/cards.collectible.json";

    private static final Map<String, Double> CARDS = new HashMap<>();
    private static final Map<String, String> CANONICAL_NAMES = new HashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, CardInfo> CARD_INFO = new HashMap<>();

    private static final Set<String> PICKED_CARDS = new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static volatile boolean onlineLoaded = false;
    private static volatile boolean onlineLoading = false;

    /*
     * -------------------------------------------------------------------------
     * CLASS-TUNNISTUS
     * -------------------------------------------------------------------------
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
     * Käyttäjän testaamat toimivat arvot.
     */
    private static final double TEMPORAL_CONSTRUCT_SCORE = 5.54;
    private static final double BITTER_END_SCORE = 6.62;
    private static final double SEALED_LANCER_SCORE = 5.56;

    static {
        initializeKnownScores();
        initializeAliases();
        loadOnlineData();
    }

    private ArenaAdvisor() {
    }

    // -------------------------------------------------------------------------
    // CARD INFO
    // -------------------------------------------------------------------------

    private static final class CardInfo {

        final String name;
        final String cardClass;
        final String type;
        final String rarity;
        final int cost;
        final int attack;
        final int health;
        final String text;

        CardInfo(
                String name,
                String cardClass,
                String type,
                String rarity,
                int cost,
                int attack,
                int health,
                String text
        ) {
            this.name = name;
            this.cardClass = cardClass;
            this.type = type;
            this.rarity = rarity;
            this.cost = cost;
            this.attack = attack;
            this.health = health;
            this.text = text == null ? "" : text;
        }
    }

    // -------------------------------------------------------------------------
    // KNOWN SCORES
    // -------------------------------------------------------------------------

    private static void initializeKnownScores() {

        putScore(
                "Temporal Construct",
                TEMPORAL_CONSTRUCT_SCORE
        );

        putScore(
                "Bitter End",
                BITTER_END_SCORE
        );

        putScore(
                "Sealed Lancer",
                SEALED_LANCER_SCORE
        );

        putFallback("Soldier of the Infinite", 5.80);
        putFallback("Soldier of the Bronze", 4.60);
        putFallback("Scaled Lancer", 5.56);

        putFallback("Unknown Voyager", 5.90);
        putFallback("Cloud Serpent", 5.60);
        putFallback("Sporegnasher", 5.30);
        putFallback("Scorching Winds", 4.80);
        putFallback("Disguised Executioner", 4.50);
        putFallback("Stranglevine", 4.50);
        putFallback("Ancient Pterrordax", 4.40);
        putFallback("Dissolving Ooze", 4.30);
        putFallback("Mystic Misdirection", 4.20);
        putFallback("Jailbird", 3.90);
        putFallback("Omen of the End", 3.60);
        putFallback("Earthen Drake", 2.80);
        putFallback("Scavenging Flytrap", 2.70);
        putFallback("Envoy of the Glade", 2.50);
        putFallback("Primalfin Challenger", 2.50);
        putFallback("Holy Eggbearer", 1.40);
        putFallback("Alarm-o-Matic", 1.20);

        putFallback("Storage Scuffle", 7.90);
        putFallback("Surge Needle", 7.90);
        putFallback("Crystallized Leyline", 7.60);
        putFallback("Jaina's Gift", 7.60);
        putFallback("Sindragosa's Triumph", 7.40);
        putFallback("Escape Artist", 7.30);
        putFallback("Portal Vanguard", 7.30);
        putFallback("Frantic Forger", 7.10);
        putFallback("Semi-Stable Portal", 7.00);
        putFallback("Sheep Mask", 7.00);
        putFallback("Twisted Monstrosity", 7.00);

        putFallback("Merry Moonkin", 6.90);
        putFallback("Divination", 6.80);
        putFallback("Selfless Protector", 6.70);
        putFallback("Paltry Flutterwing", 6.30);
        putFallback("Petal Picker", 6.30);
        putFallback("Fragment of Nothing", 6.10);
        putFallback("Raincaller", 6.10);
        putFallback("Darkscale Broodmother", 6.00);

        putFallback("Defias Smuggler", 7.90);
        putFallback("Activated Golem", 7.80);
        putFallback("Bronze Keeper", 7.70);
        putFallback("Daydreaming Pixie", 7.70);
        putFallback("Flutterwing Guardian", 7.60);
        putFallback("Quantum Destabilizer", 7.60);
        putFallback("Rockskipper", 7.50);
        putFallback("Timestop", 7.40);
        putFallback("Undercover Cultist", 7.40);
        putFallback("Fae Trickster", 7.30);
        putFallback("Gnawing Greenfin", 7.30);
        putFallback("Aeon Wizard", 7.20);
        putFallback("Living Paradox", 7.20);
        putFallback("Battlefield Blaster", 7.10);
        putFallback("Gemstone Hoarder", 7.10);
        putFallback("Ancient Stegodon", 7.00);
        putFallback("Briarspawn Drake", 7.00);
        putFallback("Dreambound Raptor", 7.00);
        putFallback("Whirling Stormdrake", 7.00);
        putFallback("Willful Watcher", 7.00);

        putFallback("Sizzling Cinder", 6.90);
        putFallback("Platysaur", 6.70);
        putFallback("Relic Miner", 6.60);
        putFallback("Temporal Traveler", 6.60);
        putFallback("Hourglass Attendant", 6.50);
        putFallback("Sewer Imp", 6.50);
        putFallback("Critter Caretaker", 6.40);
        putFallback("Drakeadon Mongrel", 6.40);
        putFallback("Scalehide Kodo", 6.60);
        putFallback("Fleeing Treant", 6.20);
        putFallback("Scorching Observer", 6.10);
        putFallback("P1CK-P0K3T", 6.00);
        putFallback("Time Machine", 6.00);

        putFallback("Sheltered Survivor", 5.80);
        putFallback("Yesterloc", 5.80);
        putFallback("Primal Sabretooth", 5.70);
        putFallback("Marshland Thresher", 5.60);
        putFallback("Bitterbloom Knight", 5.50);
        putFallback("Cyborg Patriarch", 5.50);
        putFallback("Devious Coyote", 5.50);
        putFallback("Gullible Guard", 5.50);
        putFallback("Curious Cumulus", 5.30);
        putFallback("Captive Nathrezim", 5.20);
        putFallback("Curious Explorer", 5.20);
        putFallback("Silithid Queen", 5.20);
        putFallback("Clockwork Rager", 5.10);
        putFallback("Living Flame", 5.10);
        putFallback("Tormented Dreadwing", 5.10);
        putFallback("Chronicle Keeper", 5.00);
        putFallback("Misplaced Pyromancer", 5.00);
        putFallback("Questing Assistant", 5.00);
        putFallback("Solitude", 5.00);
        putFallback("Fading Memory", 4.90);
        putFallback("Sentient Hourglass", 4.70);
        putFallback("Tar Tyrant", 4.70);
        putFallback("Wizened Truthseeker", 4.50);
        putFallback("Crater Gator", 4.40);
        putFallback("Frostbitten Imp", 4.40);
        putFallback("Petrified Ogre", 4.10);
        putFallback("Animated Moonwell", 4.00);
        putFallback("Dark Bribe", 4.00);
        putFallback("Petal Peddler", 4.00);

        putFallback("Herbivore Assistant", 3.90);
        putFallback("Meadowstrider", 3.90);
        putFallback("Prescient Slitherdrake", 3.80);
        putFallback("Twisted Treant", 3.80);
        putFallback("Solitary Prisoner", 3.50);
        putFallback("Tranquil Treant", 3.40);
        putFallback("Dangerous Variant", 3.30);
    }

    private static void putFallback(
            String name,
            double score
    ) {
        if (!CARDS.containsKey(normalize(name))) {
            putScore(name, score);
        }
    }

    private static void putScore(
            String name,
            double score
    ) {
        String key = normalize(name);

        CARDS.put(
                key,
                clamp(score)
        );

        CANONICAL_NAMES.put(
                key,
                name
        );
    }

    // -------------------------------------------------------------------------
    // OCR ALIASES
    // -------------------------------------------------------------------------

    private static void initializeAliases() {

        addAlias(
                "TemporalConstruct",
                "Temporal Construct"
        );

        addAlias(
                "Temporal Construct",
                "Temporal Construct"
        );

        addAlias(
                "Temporal  Construct",
                "Temporal Construct"
        );

        addAlias(
                "BitterEnd",
                "Bitter End"
        );

        addAlias(
                "Bitter End",
                "Bitter End"
        );

        addAlias(
                "Bitter  End",
                "Bitter End"
        );

        addAlias(
                "SealedLancer",
                "Sealed Lancer"
        );

        addAlias(
                "Sealed Lancer",
                "Sealed Lancer"
        );

        addAlias(
                "ScaledLancer",
                "Sealed Lancer"
        );

        addAlias(
                "Scaled Lancer",
                "Sealed Lancer"
        );

        addAlias(
                "SoldieroftheInfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "Soldier of Infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "SoldierofInfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "Soldier of the Infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "Soldier of Ihfini",
                "Soldier of the Infinite"
        );

        addAlias(
                "So1dier of the Infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "Sotdier of Infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "Sotdier of the Infinite",
                "Soldier of the Infinite"
        );

        for (String canonical : CANONICAL_NAMES.values()) {

            String compact =
                    compactNormalize(canonical);

            if (!compact.isEmpty()) {
                ALIASES.put(
                        compact,
                        canonical
                );
            }
        }
    }

    private static void addAlias(
            String alias,
            String canonical
    ) {

        String key = normalize(alias);

        if (!key.isEmpty()) {
            ALIASES.put(
                    key,
                    canonical
            );
        }

        String compact =
                compactNormalize(alias);

        if (!compact.isEmpty()) {
            ALIASES.put(
                    compact,
                    canonical
            );
        }
    }

    // -------------------------------------------------------------------------
    // OCR CORRECTION
    // -------------------------------------------------------------------------

    public static String correctOcr(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String text =
                cleanName(input);

        if (text.isEmpty()) {
            return "";
        }

        String soldier =
                matchSoldierOfInfinite(text);

        if (soldier != null) {
            return soldier;
        }

        String canonical =
                CANONICAL_NAMES.get(
                        normalize(text)
                );

        if (canonical != null) {
            return canonical;
        }

        canonical =
                ALIASES.get(
                        normalize(text)
                );

        if (canonical != null) {
            return canonical;
        }

        canonical =
                ALIASES.get(
                        compactNormalize(text)
                );

        if (canonical != null) {
            return canonical;
        }

        String compact =
                compactNormalize(text);

        if (!compact.isEmpty()) {

            for (
                    Map.Entry<String, String> entry
                            : CANONICAL_NAMES.entrySet()
            ) {

                if (compact.equals(
                        compactNormalize(
                                entry.getValue()
                        )
                )) {

                    return entry.getValue();
                }
            }
        }

        String fuzzy =
                findClosestName(text);

        if (fuzzy != null) {
            return fuzzy;
        }

        return text;
    }

    // -------------------------------------------------------------------------
    // CLASS DETECTION
    // -------------------------------------------------------------------------

    /**
     * Tunnistaa Hearthstone classin korttitarjouksista.
     *
     * Vain class-kohtaiset kortit toimivat todisteena.
     * Neutraaleista korteista ei tehdä arvausta.
     */
    public static synchronized void detectClassFromCards(
            String card1,
            String card2,
            String card3
    ) {

        String detected =
                "";

        detected =
                getClassForCard(card1);

        if (detected.isEmpty()) {
            detected =
                    getClassForCard(card2);
        }

        if (detected.isEmpty()) {
            detected =
                    getClassForCard(card3);
        }

        /*
         * Jos ensimmäinen class-kohtainen kortti ei vielä
         * riitä, tarkistetaan että mahdolliset class-kortit
         * ovat samaa classia.
         */
        String class2 =
                getClassForCard(card2);

        String class3 =
                getClassForCard(card3);

        if (!detected.isEmpty()
                && !class2.isEmpty()
                && !detected.equals(class2)) {

            detected = "";
        }

        if (!detected.isEmpty()
                && !class3.isEmpty()
                && !detected.equals(class3)) {

            detected = "";
        }

        if (detected.isEmpty()) {
            return;
        }

        /*
         * Jos class on jo lukittu, pidetään siitä kiinni.
         */
        if (!currentClass.isEmpty()) {

            if (currentClass.equals(detected)) {
                candidateClass = "";
                candidateClassCount = 0;
            }

            return;
        }

        /*
         * Ensimmäinen havainto.
         */
        if (candidateClass.isEmpty()
                || !candidateClass.equals(detected)) {

            candidateClass =
                    detected;

            candidateClassCount = 1;

            return;
        }

        /*
         * Sama class havaittu uudelleen.
         */
        candidateClassCount++;

        if (candidateClassCount >=
                CLASS_CONFIRMATIONS) {

            currentClass =
                    candidateClass;

            candidateClass = "";
            candidateClassCount = 0;

            Log.i(
                    TAG,
                    "Arena class detected: "
                            + currentClass
            );
        }
    }

    private static String getClassForCard(
            String cardName
    ) {

        if (cardName == null
                || cardName.trim().isEmpty()) {

            return "";
        }

        String corrected =
                correctOcr(cardName);

        CardInfo info =
                CARD_INFO.get(
                        normalize(corrected)
                );

        if (info == null) {
            return "";
        }

        return normalizeClass(
                info.cardClass
        );
    }

    private static String normalizeClass(
            String className
    ) {

        if (className == null) {
            return "";
        }

        String value =
                className
                        .trim()
                        .toUpperCase(Locale.US)
                        .replace(
                                "_",
                                " "
                        )
                        .replace(
                                "-",
                                " "
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if ("DEATHKNIGHT".equals(value)) {
            value = "DEATH KNIGHT";
        }

        if ("DEMONHUNTER".equals(value)) {
            value = "DEMON HUNTER";
        }

        for (String valid : VALID_CLASSES) {

            if (valid.equals(value)) {
                return valid;
            }
        }

        return "";
    }

    /**
     * Palauttaa nykyisen automaattisesti tunnistetun classin.
     */
    public static String getCurrentClass() {

        if (currentClass.isEmpty()) {
            return "EI TUNNISTETTU";
        }

        return currentClass;
    }

    /**
     * Palauttaa tyhjän merkkijonon, jos classia ei ole vielä
     * tunnistettu.
     */
    public static String getCurrentClassRaw() {
        return currentClass;
    }

    public static boolean isClassDetected() {
        return !currentClass.isEmpty();
    }

    /**
     * Aloittaa uuden Arena-draftin class-tunnistuksen.
     */
    public static synchronized void resetClassDetection() {

        currentClass = "";
        candidateClass = "";
        candidateClassCount = 0;
    }

    // -------------------------------------------------------------------------
    // SCORE
    // -------------------------------------------------------------------------

    private static double score(
            String cardName
    ) {

        if (cardName == null
                || cardName.trim().isEmpty()) {

            return 0.0;
        }

        String corrected =
                correctOcr(cardName);

        if (corrected.isEmpty()) {
            return 0.0;
        }

        Double direct =
                CARDS.get(
                        normalize(corrected)
                );

        if (direct != null) {
            return applyPickBonus(
                    corrected,
                    direct
            );
        }

        String compact =
                compactNormalize(corrected);

        if (!compact.isEmpty()) {

            for (
                    Map.Entry<String, String> entry
                            : CANONICAL_NAMES.entrySet()
            ) {

                if (compact.equals(
                        compactNormalize(
                                entry.getValue()
                        )
                )) {

                    Double value =
                            CARDS.get(
                                    entry.getKey()
                            );

                    if (value != null) {

                        return applyPickBonus(
                                entry.getValue(),
                                value
                        );
                    }
                }
            }
        }

        CardInfo info =
                CARD_INFO.get(
                        normalize(corrected)
                );

        if (info != null) {

            double generated =
                    generateFallbackScore(info);

            return applyPickBonus(
                    corrected,
                    generated
            );
        }

        String fuzzy =
                findClosestName(corrected);

        if (fuzzy != null) {

            Double value =
                    CARDS.get(
                            normalize(fuzzy)
                    );

            if (value != null) {

                return applyPickBonus(
                        fuzzy,
                        value
                );
            }

            CardInfo fuzzyInfo =
                    CARD_INFO.get(
                            normalize(fuzzy)
                    );

            if (fuzzyInfo != null) {

                double generated =
                        generateFallbackScore(
                                fuzzyInfo
                        );

                return applyPickBonus(
                        fuzzy,
                        generated
                );
            }
        }

        return 0.0;
    }

    public static String getCardScore(
            String cardName
    ) {

        double value =
                score(cardName);

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    public static double getCardScoreValue(
            String cardName
    ) {
        return score(cardName);
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

    private static double applyPickBonus(
            String cardName,
            double value
    ) {

        if (cardName == null) {
            return clamp(value);
        }

        if (PICKED_CARDS.contains(
                normalize(cardName)
        )) {

            return clamp(
                    value - 0.15
            );
        }

        return clamp(value);
    }

    // -------------------------------------------------------------------------
    // RECOMMENDATION
    // -------------------------------------------------------------------------

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

        String bestCard =
                card1;

        double bestScore =
                score1;

        if (score2 > bestScore) {

            bestCard =
                    card2;

            bestScore =
                    score2;
        }

        if (score3 > bestScore) {

            bestCard =
                    card3;

            bestScore =
                    score3;
        }

        if (bestCard == null
                || bestCard.trim().isEmpty()) {

            return "";
        }

        return correctOcr(bestCard);
    }

    // -------------------------------------------------------------------------
    // PICKED CARDS
    // -------------------------------------------------------------------------

    public static void recordPickedCard(
            String cardName
    ) {
        addPickedCard(cardName);
    }

    public static void addPickedCard(
            String cardName
    ) {

        if (cardName == null) {
            return;
        }

        String corrected =
                correctOcr(cardName);

        if (!corrected.isEmpty()) {

            PICKED_CARDS.add(
                    normalize(corrected)
            );
        }
    }

    public static void clearPickedCards() {
        PICKED_CARDS.clear();
    }

    public static boolean wasPicked(
            String cardName
    ) {

        if (cardName == null) {
            return false;
        }

        String corrected =
                correctOcr(cardName);

        return PICKED_CARDS.contains(
                normalize(corrected)
        );
    }

    // -------------------------------------------------------------------------
    // ONLINE CARD DATABASE
    // -------------------------------------------------------------------------

    public static void loadOnlineData() {

        if (onlineLoaded || onlineLoading) {
            return;
        }

        onlineLoading = true;

        EXECUTOR.execute(() -> {

            HttpURLConnection connection =
                    null;

            try {

                URL url =
                        new URL(
                                CARD_DATABASE_URL
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

                    Log.w(
                            TAG,
                            "HearthstoneJSON HTTP "
                                    + responseCode
                    );

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

                StringBuilder builder =
                        new StringBuilder();

                String line;

                while (
                        (line =
                                reader.readLine())
                                != null
                ) {

                    builder.append(line);
                }

                reader.close();
                inputStream.close();

                parseCardDatabase(
                        builder.toString()
                );

                onlineLoaded = true;

                rebuildCompactAliases();

                Log.i(
                        TAG,
                        "Loaded Hearthstone card database: "
                                + CARD_INFO.size()
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Failed to load HearthstoneJSON",
                        e
                );

            } finally {

                onlineLoading = false;

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static synchronized void parseCardDatabase(
            String json
    ) throws Exception {

        JSONArray cards =
                new JSONArray(json);

        for (
                int i = 0;
                i < cards.length();
                i++
        ) {

            JSONObject card =
                    cards.optJSONObject(i);

            if (card == null) {
                continue;
            }

            String name =
                    card.optString(
                            "name",
                            ""
                    ).trim();

            if (name.isEmpty()) {
                continue;
            }

            String normalized =
                    normalize(name);

            if (normalized.isEmpty()) {
                continue;
            }

            String cardClass =
                    card.optString(
                            "cardClass",
                            card.optString(
                                    "playerClass",
                                    ""
                            )
                    );

            String type =
                    card.optString(
                            "type",
                            ""
                    );

            String rarity =
                    card.optString(
                            "rarity",
                            ""
                    );

            int cost =
                    card.optInt(
                            "cost",
                            0
                    );

            int attack =
                    card.optInt(
                            "attack",
                            0
                    );

            int health =
                    card.optInt(
                            "health",
                            0
                    );

            String text =
                    card.optString(
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

            CARD_INFO.put(
                    normalized,
                    info
            );

            if (!CARDS.containsKey(
                    normalized
            )) {

                double generated =
                        generateFallbackScore(
                                info
                        );

                CARDS.put(
                        normalized,
                        generated
                );
            }

            CANONICAL_NAMES.put(
                    normalized,
                    name
            );
        }

        /*
         * Palautetaan käyttäjän varmistamat arvot.
         */
        putScore(
                "Temporal Construct",
                TEMPORAL_CONSTRUCT_SCORE
        );

        putScore(
                "Bitter End",
                BITTER_END_SCORE
        );

        putScore(
                "Sealed Lancer",
                SEALED_LANCER_SCORE
        );

        addAlias(
                "Scaled Lancer",
                "Sealed Lancer"
        );

        addAlias(
                "ScaledLancer",
                "Sealed Lancer"
        );
    }

    private static synchronized void rebuildCompactAliases() {

        for (
                String canonical
                        : CANONICAL_NAMES.values()
        ) {

            String compact =
                    compactNormalize(
                            canonical
                    );

            if (!compact.isEmpty()) {

                ALIASES.put(
                        compact,
                        canonical
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // FALLBACK SCORE
    // -------------------------------------------------------------------------

    private static double generateFallbackScore(
            CardInfo card
    ) {

        if (card == null) {
            return 0.0;
        }

        double value = 4.50;

        if ("MINION".equalsIgnoreCase(
                card.type
        )) {

            double statTotal =
                    card.attack + card.health;

            value += Math.min(
                    1.50,
                    statTotal * 0.08
            );

            if (card.cost >= 2
                    && card.cost <= 5) {

                value += 0.35;
            }

            if (card.cost >= 6) {
                value -= 0.15;
            }
        }

        if ("SPELL".equalsIgnoreCase(
                card.type
        )) {
            value += 0.10;
        }

        if ("WEAPON".equalsIgnoreCase(
                card.type
        )) {
            value += 0.15;
        }

        String text =
                card.text.toLowerCase(
                        Locale.US
                );

        if (text.contains("battlecry")) {
            value += 0.35;
        }

        if (text.contains("deathrattle")) {
            value += 0.25;
        }

        if (text.contains("discover")) {
            value += 0.40;
        }

        if (text.contains("draw")) {
            value += 0.30;
        }

        if (text.contains("rush")) {
            value += 0.20;
        }

        if (text.contains("taunt")) {
            value += 0.15;
        }

        if (text.contains("lifesteal")) {
            value += 0.15;
        }

        if (text.contains("random")) {
            value -= 0.10;
        }

        if (text.contains("enemy")) {
            value += 0.05;
        }

        if ("LEGENDARY".equalsIgnoreCase(
                card.rarity
        )) {
            value += 0.20;
        }

        return clamp(value);
    }

    // -------------------------------------------------------------------------
    // NORMALIZATION
    // -------------------------------------------------------------------------

    private static String cleanName(
            String input
    ) {

        String text = input;

        text =
                text.replace(
                        "&#039;",
                        "'"
                );

        text =
                text.replace(
                        "&apos;",
                        "'"
                );

        text =
                text.replace(
                        "\u2019",
                        "'"
                );

        text =
                text.replace(
                        "\u2018",
                        "'"
                );

        text =
                text.replace(
                        "\u00A0",
                        " "
                );

        text =
                text.replaceAll(
                        "[\\r\\n]+",
                        " "
                );

        text =
                text.replaceAll(
                        "\\s+",
                        " "
                );

        text =
                text.trim();

        text =
                text.replaceAll(
                        "[.,:;!?]+$",
                        ""
                );

        return text.trim();
    }

    private static String normalize(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String text =
                cleanName(input);

        return text
                .toLowerCase(Locale.US)
                .replaceAll(
                        "[^a-z0-9']",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String compactNormalize(
            String input
    ) {

        if (input == null) {
            return "";
        }

        return normalize(input)
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    // -------------------------------------------------------------------------
    // SOLDIER OCR
    // -------------------------------------------------------------------------

    private static String matchSoldierOfInfinite(
            String text
    ) {

        String compact =
                compactNormalize(text);

        if (compact.isEmpty()) {
            return null;
        }

        String target =
                "soldieroftheinfinite";

        if (compact.equals(target)) {
            return "Soldier of the Infinite";
        }

        if (compact.contains("soldier")
                && compact.contains("infinite")) {

            return "Soldier of the Infinite";
        }

        if (compact.contains("so1dier")
                && compact.contains("infinite")) {

            return "Soldier of the Infinite";
        }

        if (compact.contains("sotdier")
                && compact.contains("infinite")) {

            return "Soldier of the Infinite";
        }

        if (compact.contains("soldier")
                && compact.contains("ihfini")) {

            return "Soldier of the Infinite";
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // FUZZY MATCH
    // -------------------------------------------------------------------------

    private static String findClosestName(
            String input
    ) {

        String source =
                compactNormalize(input);

        if (source.length() < 4) {
            return null;
        }

        String bestName = null;

        int bestDistance =
                Integer.MAX_VALUE;

        for (
                String canonical
                        : CANONICAL_NAMES.values()
        ) {

            String candidate =
                    compactNormalize(
                            canonical
                    );

            if (candidate.length() < 4) {
                continue;
            }

            int distance =
                    levenshteinDistance(
                            source,
                            candidate
                    );

            int maxLength =
                    Math.max(
                            source.length(),
                            candidate.length()
                    );

            int allowed;

            if (maxLength <= 8) {
                allowed = 2;
            } else if (maxLength <= 14) {
                allowed = 3;
            } else {
                allowed = 4;
            }

            if (distance <= allowed
                    && distance < bestDistance) {

                bestDistance = distance;
                bestName = canonical;
            }
        }

        return bestName;
    }

    private static int levenshteinDistance(
            String a,
            String b
    ) {

        int[] previous =
                new int[b.length() + 1];

        int[] current =
                new int[b.length() + 1];

        for (
                int j = 0;
                j <= b.length();
                j++
        ) {

            previous[j] = j;
        }

        for (
                int i = 1;
                i <= a.length();
                i++
        ) {

            current[0] = i;

            for (
                    int j = 1;
                    j <= b.length();
                    j++
            ) {

                int insertion =
                        current[j - 1] + 1;

                int deletion =
                        previous[j] + 1;

                int substitution =
                        previous[j - 1]
                                + (
                                a.charAt(i - 1)
                                        ==
                                        b.charAt(j - 1)
                                        ? 0
                                        : 1
                        );

                current[j] =
                        Math.min(
                                Math.min(
                                        insertion,
                                        deletion
                                ),
                                substitution
                        );
            }

            int[] temp =
                    previous;

            previous =
                    current;

            current =
                    temp;
        }

        return previous[
                b.length()
        ];
    }

    // -------------------------------------------------------------------------
    // STATUS
    // -------------------------------------------------------------------------

    public static boolean isOnlineLoaded() {
        return onlineLoaded;
    }

    public static int getKnownCardCount() {
        return CARD_INFO.size();
    }

    public static String getStatus() {

        if (onlineLoaded) {

            return "Korttitietokanta ladattu: "
                    + CARD_INFO.size()
                    + " korttia";
        }

        if (onlineLoading) {
            return "Korttitietokantaa ladataan...";
        }

        return "Paikallinen korttitietokanta käytössä";
    }

    public static String getReason(
            String cardName
    ) {

        if (cardName == null
                || cardName.trim().isEmpty()) {

            return "Ei korttia";
        }

        String corrected =
                correctOcr(cardName);

        double value =
                score(corrected);

        return corrected
                + " → "
                + String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    // -------------------------------------------------------------------------
    // CLAMP
    // -------------------------------------------------------------------------

    private static double clamp(
            double value
    ) {

        if (Double.isNaN(value)
                || Double.isInfinite(value)) {

            return 0.0;
        }

        if (value < 0.0) {
            return 0.0;
        }

        if (value > 10.0) {
            return 10.0;
        }

        return value;
    }
}
