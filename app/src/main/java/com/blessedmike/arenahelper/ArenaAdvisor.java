package com.blessedmike.arenahelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class ArenaAdvisor {

    private static class CardData {

        String name;
        double baseScore;
        int mana;
        int attack;
        int health;

        boolean minion;
        boolean spell;
        boolean weapon;

        boolean removal;
        boolean aoe;
        boolean draw;
        boolean discover;
        boolean taunt;
        boolean divineShield;
        boolean battlecry;
        boolean deathrattle;

        CardData(
                String name,
                double baseScore,
                int mana,
                int attack,
                int health
        ) {
            this.name = name;
            this.baseScore = baseScore;
            this.mana = mana;
            this.attack = attack;
            this.health = health;

            this.minion = true;
            this.spell = false;
            this.weapon = false;
        }
    }

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    static {

        // =====================================================
        // TEST / CLASSIC CARDS
        // =====================================================

        addCard("Frostbolt", 7.5, 2, 0, 0);
        makeSpell("Frostbolt");
        CARDS.get(normalize("Frostbolt")).removal = true;

        addCard("Fireball", 8.0, 4, 0, 0);
        makeSpell("Fireball");
        CARDS.get(normalize("Fireball")).removal = true;

        addCard("Water Elemental", 8.0, 4, 3, 6);

        addCard("Chillwind Yeti", 6.0, 4, 4, 5);

        addCard("Boulderfist Ogre", 5.0, 6, 6, 7);

        addCard("River Crocolisk", 4.0, 2, 2, 3);

        addCard("Bloodfen Raptor", 4.0, 2, 3, 2);

        addCard("Arcane Intellect", 6.5, 3, 0, 0);
        makeSpell("Arcane Intellect");
        CARDS.get(normalize("Arcane Intellect")).draw = true;

        addCard("Polymorph", 7.0, 4, 0, 0);
        makeSpell("Polymorph");
        CARDS.get(normalize("Polymorph")).removal = true;

        addCard("Flamestrike", 8.0, 7, 0, 0);
        makeSpell("Flamestrike");
        CARDS.get(normalize("Flamestrike")).removal = true;
        CARDS.get(normalize("Flamestrike")).aoe = true;

        addCard("Consecration", 7.5, 4, 0, 0);
        makeSpell("Consecration");
        CARDS.get(normalize("Consecration")).removal = true;
        CARDS.get(normalize("Consecration")).aoe = true;

        addCard("Truesilver Champion", 8.5, 4, 4, 2);
        makeWeapon("Truesilver Champion");
        CARDS.get(normalize("Truesilver Champion")).removal = true;

        addCard("Fiery War Axe", 8.0, 2, 3, 2);
        makeWeapon("Fiery War Axe");
        CARDS.get(normalize("Fiery War Axe")).removal = true;

        addCard("Shadow Word: Pain", 6.5, 2, 0, 0);
        makeSpell("Shadow Word: Pain");
        CARDS.get(normalize("Shadow Word: Pain")).removal = true;

        addCard("Holy Nova", 7.0, 5, 0, 0);
        makeSpell("Holy Nova");
        CARDS.get(normalize("Holy Nova")).removal = true;
        CARDS.get(normalize("Holy Nova")).aoe = true;

        addCard("Backstab", 7.0, 0, 0, 0);
        makeSpell("Backstab");
        CARDS.get(normalize("Backstab")).removal = true;

        addCard("Eviscerate", 8.0, 2, 0, 0);
        makeSpell("Eviscerate");
        CARDS.get(normalize("Eviscerate")).removal = true;

        addCard("Swipe", 8.0, 4, 0, 0);
        makeSpell("Swipe");
        CARDS.get(normalize("Swipe")).removal = true;
        CARDS.get(normalize("Swipe")).aoe = true;

        addCard("Kill Command", 7.0, 3, 0, 0);
        makeSpell("Kill Command");
        CARDS.get(normalize("Kill Command")).removal = true;

        addCard("Animal Companion", 8.0, 3, 0, 0);
        makeSpell("Animal Companion");

        addCard("Hex", 7.5, 4, 0, 0);
        makeSpell("Hex");
        CARDS.get(normalize("Hex")).removal = true;

        addCard("Lightning Bolt", 6.5, 1, 0, 0);
        makeSpell("Lightning Bolt");
        CARDS.get(normalize("Lightning Bolt")).removal = true;

        addCard("Hellfire", 7.0, 4, 0, 0);
        makeSpell("Hellfire");
        CARDS.get(normalize("Hellfire")).removal = true;
        CARDS.get(normalize("Hellfire")).aoe = true;


        // =====================================================
        // CURRENT ARENA CARDS
        // 0-10 SCALE
        // =====================================================

        addCard("Soldier of the Infinite", 4.1, 3, 3, 4);
        CARDS.get(
                normalize("Soldier of the Infinite")
        ).taunt = true;

        addCard("Bursting Leyline", 4.1, 4, 0, 0);
        makeSpell("Bursting Leyline");
        CARDS.get(
                normalize("Bursting Leyline")
        ).removal = true;

        addCard("Contraband Wands", 4.6, 2, 0, 0);
        makeSpell("Contraband Wands");

        addCard("Crystallized Leyline", 5.4, 0, 0, 0);
        makeSpell("Crystallized Leyline");

        addCard("Surge Needle", 5.6, 0, 0, 0);
        makeSpell("Surge Needle");

        addCard("Leyline Nexus", 4.5, 0, 0, 0);
        makeSpell("Leyline Nexus");

        addCard("Mystic Runesaber", 5.5, 4, 3, 4);

        addCard("Ley Walker", 6.4, 0, 0, 0);

        addCard("Cold Snap", 5.4, 0, 0, 0);
        makeSpell("Cold Snap");

        addCard("Code Violet", 5.1, 0, 0, 0);
        makeSpell("Code Violet");

        addCard("Tunneling Geomancer", 5.4, 3, 3, 3);

        addCard("Watfin", 6.1, 0, 0, 0);

        addCard("Zilliax Deluxe 3000", 5.7, 0, 0, 0);

        addCard("Shadowed Informant", 6.7, 0, 0, 0);

        addCard("Hopeful Dryad", 5.8, 3, 3, 3);

        addCard("Raptor Herald", 6.4, 3, 3, 3);

        addCard("Carrier Whelp", 6.4, 0, 0, 0);

        addCard("Experimental Animation", 8.2, 0, 0, 0);

        addCard("Obsessive Technician", 8.2, 0, 0, 0);

        addCard("Violet Punisher", 7.4, 0, 0, 0);

        addCard("Whelp of the Infinite", 7.4, 0, 0, 0);

        addCard("Infested Breath", 7.2, 0, 0, 0);

        addCard("Emergency Surgery", 5.7, 0, 0, 0);

        addCard("Drink Blood", 5.2, 0, 0, 0);

        addCard("Disguised Doctor", 4.1, 0, 0, 0);

        addCard("Corpse Cannon", 6.9, 0, 0, 0);

        addCard("Void Soul", 5.1, 0, 0, 0);

        addCard("Void Blast", 5.7, 0, 0, 0);

        addCard("Vicious Voidscale", 6.1, 0, 0, 0);

        addCard("Widow's Bite", 5.4, 0, 0, 0);

        addCard("Infest the Scullery", 4.2, 0, 0, 0);

        addCard("Underbelly Network", 6.8, 0, 0, 0);

        addCard("Guard Dog", 6.3, 0, 0, 0);

        addCard("Dig for Freedom", 6.4, 0, 0, 0);

        addCard("Vigilant Sentry", 1.7, 0, 0, 0);

        addCard("Truth Seeker", 5.5, 0, 0, 0);

        addCard("Judgment", 6.2, 0, 0, 0);

        addCard("Holy Bola!", 4.0, 0, 0, 0);

        addCard("Dalaran Champion", 3.9, 0, 0, 0);

        addCard("Commander Beatrix", 3.9, 0, 0, 0);

        addCard("Undeath Sentence", 3.6, 0, 0, 0);

        addCard("Karov the Broken", 7.4, 0, 0, 0);

        addCard("Jade Guardians", 5.8, 0, 0, 0);

        addCard("Inspector Murloc Holmes", 5.0, 0, 0, 0);

        addCard("Jailhouse Manastorm", 9.3, 0, 0, 0);

        addCard("Warptooth", 6.4, 0, 0, 0);


        // =====================================================
        // OCR ALIASES
        // =====================================================

        addAlias("soldierolf theinfinite", "Soldier of the Infinite");
        addAlias("soldierolf the infinite", "Soldier of the Infinite");
        addAlias("soldierolf the infinit", "Soldier of the Infinite");
        addAlias("sotdier of infinite", "Soldier of the Infinite");
        addAlias("so1dier of the infinite", "Soldier of the Infinite");
        addAlias("soldieroftheinfinit", "Soldier of the Infinite");
        addAlias("soldier of the infinite", "Soldier of the Infinite");

        addAlias("burstingleyline", "Bursting Leyline");
        addAlias("bursting leyline", "Bursting Leyline");

        addAlias("contrabandwands", "Contraband Wands");
        addAlias("contraband wand", "Contraband Wands");

        addAlias("crystallizedleyline", "Crystallized Leyline");
        addAlias("crystalizedleyline", "Crystallized Leyline");

        addAlias("surgeneedle", "Surge Needle");
        addAlias("leylinenexus", "Leyline Nexus");
        addAlias("mysticrunesaber", "Mystic Runesaber");
        addAlias("leywalker", "Ley Walker");
        addAlias("coldsnap", "Cold Snap");
        addAlias("codeviolet", "Code Violet");
        addAlias("tunnelinggeomancer", "Tunneling Geomancer");
        addAlias("shadowedinformant", "Shadowed Informant");
        addAlias("carrierwhelp", "Carrier Whelp");
        addAlias("hopefuldryad", "Hopeful Dryad");
        addAlias("raptorherald", "Raptor Herald");
        addAlias("experimentalanimation", "Experimental Animation");
        addAlias("obsessivetechnician", "Obsessive Technician");
        addAlias("violetpunisher", "Violet Punisher");
        addAlias("whelpinfinite", "Whelp of the Infinite");
        addAlias("infestedbreath", "Infested Breath");
        addAlias("emergencysurgery", "Emergency Surgery");
        addAlias("drinkblood", "Drink Blood");
        addAlias("disguiseddoctor", "Disguised Doctor");
        addAlias("corpsecannon", "Corpse Cannon");
        addAlias("voidsoul", "Void Soul");
        addAlias("voidblast", "Void Blast");
        addAlias("viciousvoidscale", "Vicious Voidscale");
        addAlias("widowsbite", "Widow's Bite");
        addAlias("infestthescullery", "Infest the Scullery");
        addAlias("underbellynetwork", "Underbelly Network");
        addAlias("guarddog", "Guard Dog");
        addAlias("digforfreedom", "Dig for Freedom");
        addAlias("vigilantsentry", "Vigilant Sentry");
        addAlias("truthseeker", "Truth Seeker");
        addAlias("holybola", "Holy Bola!");
        addAlias("dalaranchampion", "Dalaran Champion");
        addAlias("commanderbeatrix", "Commander Beatrix");
        addAlias("undeathsentence", "Undeath Sentence");
        addAlias("karovthebroken", "Karov the Broken");
        addAlias("jadeguardians", "Jade Guardians");
        addAlias("inspectormurloc", "Inspector Murloc Holmes");
        addAlias("jailhousemanastorm", "Jailhouse Manastorm");
        addAlias("warptooth", "Warptooth");
    }


    // =========================================================
    // CARD HELPERS
    // =========================================================

    private static void addCard(
            String name,
            double score,
            int mana,
            int attack,
            int health
    ) {

        CardData card =
                new CardData(
                        name,
                        score,
                        mana,
                        attack,
                        health
                );

        CARDS.put(
                normalize(name),
                card
        );
    }


    private static void makeSpell(String name) {

        CardData card =
                CARDS.get(normalize(name));

        if (card == null) {
            return;
        }

        card.spell = true;
        card.minion = false;
    }


    private static void makeWeapon(String name) {

        CardData card =
                CARDS.get(normalize(name));

        if (card == null) {
            return;
        }

        card.weapon = true;
        card.minion = false;
    }


    private static void addAlias(
            String alias,
            String realName
    ) {

        CardData card =
                CARDS.get(normalize(realName));

        if (card != null) {
            CARDS.put(
                    normalize(alias),
                    card
            );
        }
    }


    // =========================================================
    // OCR CORRECTION
    // =========================================================

    private static String correctOcr(String cardName) {

        if (cardName == null) {
            return "";
        }

        String original = cardName.trim();

        if (original.isEmpty()) {
            return "";
        }

        String normalized = normalize(original);


        if (
                normalized.contains("soldierolf")
                        && normalized.contains("infinite")
        ) {
            return "Soldier of the Infinite";
        }

        if (
                normalized.contains("sotdier")
                        && normalized.contains("infinite")
        ) {
            return "Soldier of the Infinite";
        }

        if (
                normalized.contains("so1dier")
                        && normalized.contains("infinite")
        ) {
            return "Soldier of the Infinite";
        }

        if (
                normalized.contains("soldier")
                        && normalized.contains("infinite")
        ) {
            return "Soldier of the Infinite";
        }


        if (
                normalized.contains("whelp")
                        && normalized.contains("infinite")
        ) {
            return "Whelp of the Infinite";
        }


        if (
                normalized.contains("bursting")
                        && normalized.contains("leyline")
        ) {
            return "Bursting Leyline";
        }


        if (
                normalized.contains("contraband")
                        && normalized.contains("wand")
        ) {
            return "Contraband Wands";
        }


        if (
                normalized.contains("crystallized")
                        && normalized.contains("leyline")
        ) {
            return "Crystallized Leyline";
        }

        if (
                normalized.contains("crystalized")
                        && normalized.contains("leyline")
        ) {
            return "Crystallized Leyline";
        }


        if (
                normalized.contains("surge")
                        && normalized.contains("needle")
        ) {
            return "Surge Needle";
        }


        if (
                normalized.contains("leyline")
                        && normalized.contains("nexus")
        ) {
            return "Leyline Nexus";
        }


        if (
                normalized.contains("mystic")
                        && normalized.contains("runesaber")
        ) {
            return "Mystic Runesaber";
        }


        if (
                normalized.contains("ley")
                        && normalized.contains("walker")
        ) {
            return "Ley Walker";
        }


        if (
                normalized.contains("tunneling")
                        && normalized.contains("geomancer")
        ) {
            return "Tunneling Geomancer";
        }


        if (
                normalized.contains("shadowed")
                        && normalized.contains("informant")
        ) {
            return "Shadowed Informant";
        }


        if (
                normalized.contains("carrier")
                        && normalized.contains("whelp")
        ) {
            return "Carrier Whelp";
        }


        if (
                normalized.contains("hopeful")
                        && normalized.contains("dryad")
        ) {
            return "Hopeful Dryad";
        }


        if (
                normalized.contains("raptor")
                        && normalized.contains("herald")
        ) {
            return "Raptor Herald";
        }


        return original;
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private static boolean isValidCard(String cardName) {

        if (cardName == null) {
            return false;
        }

        String value = cardName.trim();

        if (value.isEmpty()) {
            return false;
        }

        if (
                value.equalsIgnoreCase(
                        "Ei tunnistettu"
                )
        ) {
            return false;
        }

        return true;
    }


    // =========================================================
    // NORMALIZE
    // =========================================================

    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.US)
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }


    // =========================================================
    // INTERNAL NUMERIC SCORE
    // =========================================================

    private static double score(String cardName) {

        if (!isValidCard(cardName)) {
            return 0.0;
        }

        String corrected =
                correctOcr(cardName);

        CardData card =
                CARDS.get(
                        normalize(corrected)
                );

        if (card == null) {
            return 0.0;
        }

        return Math.min(
                10.0,
                Math.max(
                        0.0,
                        card.baseScore
                )
        );
    }


    // =========================================================
    // GET CARD SCORE - STRING CARD NAME
    //
    // CaptureService expects String here.
    // =========================================================

    public static String getCardScore(String cardName) {

        return formatScore(
                score(cardName)
        );
    }


    // =========================================================
    // GET CARD SCORE - NUMERIC VALUE
    //
    // IMPORTANT:
    // CaptureService passes a double and expects a String.
    // =========================================================

    public static String getCardScore(double value) {

        double safeValue =
                Math.min(
                        10.0,
                        Math.max(
                                0.0,
                                value
                        )
                );

        return formatScore(safeValue);
    }


    // =========================================================
    // RECOMMEND - CARD NAMES
    // =========================================================

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

        int best =
                getBestIndex(
                        score1,
                        score2,
                        score3
                );

        String selected;

        double selectedScore;

        if (best == 1) {

            selected =
                    correctOcr(card1);

            selectedScore =
                    score1;

        } else if (best == 2) {

            selected =
                    correctOcr(card2);

            selectedScore =
                    score2;

        } else {

            selected =
                    correctOcr(card3);

            selectedScore =
                    score3;
        }

        return "KORTTI "
                + best
                + ": "
                + selected
                + "\nPisteet: "
                + formatScore(selectedScore)
                + "/10";
    }


    // =========================================================
    // RECOMMEND - INT SCORES
    //
    // CaptureService expects String here.
    // =========================================================

    public static String recommend(
            int score1,
            int score2,
            int score3
    ) {

        int best =
                getBestIndex(
                        score1,
                        score2,
                        score3
                );

        return String.valueOf(best);
    }


    // =========================================================
    // RECOMMEND - DOUBLE SCORES
    //
    // Kept for compatibility.
    // =========================================================

    public static String recommend(
            double score1,
            double score2,
            double score3
    ) {

        int best =
                getBestIndex(
                        score1,
                        score2,
                        score3
                );

        return String.valueOf(best);
    }


    // =========================================================
    // BEST CARD
    // =========================================================

    private static int getBestIndex(
            double score1,
            double score2,
            double score3
    ) {

        if (
                score1 >= score2
                        && score1 >= score3
        ) {
            return 1;
        }

        if (
                score2 >= score1
                        && score2 >= score3
        ) {
            return 2;
        }

        return 3;
    }


    // =========================================================
    // REASON
    // =========================================================

    public static String getReason(
            String cardName
    ) {

        if (!isValidCard(cardName)) {
            return "Korttia ei tunnistettu";
        }

        String corrected =
                correctOcr(cardName);

        CardData card =
                CARDS.get(
                        normalize(corrected)
                );

        if (card == null) {
            return "Korttia ei tunnistettu";
        }

        return "Arena-arvo "
                + formatScore(card.baseScore)
                + "/10";
    }


    // =========================================================
    // FORMAT SCORE
    // =========================================================

    private static String formatScore(
            double score
    ) {

        return String.format(
                Locale.US,
                "%.1f",
                score
        );
    }
}
