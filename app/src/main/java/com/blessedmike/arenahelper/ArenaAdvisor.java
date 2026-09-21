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


    /*
     * =========================================================
     * KORTIT
     * =========================================================
     */

    static {

        /*
         * VANHAT / TESTIKORTIT
         */

        addCard(
                "Soldier of the Infinite",
                4.36, 3, 3, 4
        );
        CARDS.get(normalize(
                "Soldier of the Infinite"
        )).taunt = true;


        addCard(
                "Raban Wands",
                6.5, 3, 3, 3
        );


        addCard(
                "Frostbolt",
                7.5, 2, 0, 0
        );
        CARDS.get(normalize(
                "Frostbolt"
        )).spell = true;
        CARDS.get(normalize(
                "Frostbolt"
        )).minion = false;
        CARDS.get(normalize(
                "Frostbolt"
        )).removal = true;


        addCard(
                "Fireball",
                8.0, 4, 0, 0
        );
        CARDS.get(normalize(
                "Fireball"
        )).spell = true;
        CARDS.get(normalize(
                "Fireball"
        )).minion = false;
        CARDS.get(normalize(
                "Fireball"
        )).removal = true;


        addCard(
                "Water Elemental",
                8.0, 4, 3, 6
        );


        addCard(
                "Chillwind Yeti",
                6.0, 4, 4, 5
        );


        addCard(
                "Boulderfist Ogre",
                5.0, 6, 6, 7
        );


        addCard(
                "River Crocolisk",
                4.0, 2, 2, 3
        );


        addCard(
                "Bloodfen Raptor",
                4.0, 2, 3, 2
        );


        addCard(
                "Arcane Intellect",
                6.5, 3, 0, 0
        );
        CARDS.get(normalize(
                "Arcane Intellect"
        )).spell = true;
        CARDS.get(normalize(
                "Arcane Intellect"
        )).minion = false;
        CARDS.get(normalize(
                "Arcane Intellect"
        )).draw = true;


        addCard(
                "Polymorph",
                7.0, 4, 0, 0
        );
        CARDS.get(normalize(
                "Polymorph"
        )).spell = true;
        CARDS.get(normalize(
                "Polymorph"
        )).minion = false;
        CARDS.get(normalize(
                "Polymorph"
        )).removal = true;


        addCard(
                "Flamestrike",
                8.0, 7, 0, 0
        );
        CARDS.get(normalize(
                "Flamestrike"
        )).spell = true;
        CARDS.get(normalize(
                "Flamestrike"
        )).minion = false;
        CARDS.get(normalize(
                "Flamestrike"
        )).removal = true;
        CARDS.get(normalize(
                "Flamestrike"
        )).aoe = true;


        addCard(
                "Consecration",
                7.5, 4, 0, 0
        );
        CARDS.get(normalize(
                "Consecration"
        )).spell = true;
        CARDS.get(normalize(
                "Consecration"
        )).minion = false;
        CARDS.get(normalize(
                "Consecration"
        )).removal = true;
        CARDS.get(normalize(
                "Consecration"
        )).aoe = true;


        addCard(
                "Truesilver Champion",
                8.5, 4, 4, 2
        );
        CARDS.get(normalize(
                "Truesilver Champion"
        )).weapon = true;
        CARDS.get(normalize(
                "Truesilver Champion"
        )).minion = false;
        CARDS.get(normalize(
                "Truesilver Champion"
        )).removal = true;


        addCard(
                "Fiery War Axe",
                8.0, 2, 3, 2
        );
        CARDS.get(normalize(
                "Fiery War Axe"
        )).weapon = true;
        CARDS.get(normalize(
                "Fiery War Axe"
        )).minion = false;
        CARDS.get(normalize(
                "Fiery War Axe"
        )).removal = true;


        addCard(
                "Shadow Word: Pain",
                6.5, 2, 0, 0
        );
        CARDS.get(normalize(
                "Shadow Word: Pain"
        )).spell = true;
        CARDS.get(normalize(
                "Shadow Word: Pain"
        )).minion = false;
        CARDS.get(normalize(
                "Shadow Word: Pain"
        )).removal = true;


        addCard(
                "Holy Nova",
                7.0, 5, 0, 0
        );
        CARDS.get(normalize(
                "Holy Nova"
        )).spell = true;
        CARDS.get(normalize(
                "Holy Nova"
        )).minion = false;
        CARDS.get(normalize(
                "Holy Nova"
        )).removal = true;
        CARDS.get(normalize(
                "Holy Nova"
        )).aoe = true;


        addCard(
                "Backstab",
                7.0, 0, 0, 0
        );
        CARDS.get(normalize(
                "Backstab"
        )).spell = true;
        CARDS.get(normalize(
                "Backstab"
        )).minion = false;
        CARDS.get(normalize(
                "Backstab"
        )).removal = true;


        addCard(
                "Eviscerate",
                8.0, 2, 0, 0
        );
        CARDS.get(normalize(
                "Eviscerate"
        )).spell = true;
        CARDS.get(normalize(
                "Eviscerate"
        )).minion = false;
        CARDS.get(normalize(
                "Eviscerate"
        )).removal = true;


        addCard(
                "Swipe",
                8.0, 4, 0, 0
        );
        CARDS.get(normalize(
                "Swipe"
        )).spell = true;
        CARDS.get(normalize(
                "Swipe"
        )).minion = false;
        CARDS.get(normalize(
                "Swipe"
        )).removal = true;
        CARDS.get(normalize(
                "Swipe"
        )).aoe = true;


        addCard(
                "Kill Command",
                7.0, 3, 0, 0
        );
        CARDS.get(normalize(
                "Kill Command"
        )).spell = true;
        CARDS.get(normalize(
                "Kill Command"
        )).minion = false;
        CARDS.get(normalize(
                "Kill Command"
        )).removal = true;


        addCard(
                "Animal Companion",
                8.0, 3, 0, 0
        );
        CARDS.get(normalize(
                "Animal Companion"
        )).spell = true;
        CARDS.get(normalize(
                "Animal Companion"
        )).minion = false;


        addCard(
                "Hex",
                7.5, 4, 0, 0
        );
        CARDS.get(normalize(
                "Hex"
        )).spell = true;
        CARDS.get(normalize(
                "Hex"
        )).minion = false;
        CARDS.get(normalize(
                "Hex"
        )).removal = true;


        addCard(
                "Lightning Bolt",
                6.5, 1, 0, 0
        );
        CARDS.get(normalize(
                "Lightning Bolt"
        )).spell = true;
        CARDS.get(normalize(
                "Lightning Bolt"
        )).minion = false;
        CARDS.get(normalize(
                "Lightning Bolt"
        )).removal = true;


        addCard(
                "Hellfire",
                7.0, 4, 0, 0
        );
        CARDS.get(normalize(
                "Hellfire"
        )).spell = true;
        CARDS.get(normalize(
                "Hellfire"
        )).minion = false;
        CARDS.get(normalize(
                "Hellfire"
        )).removal = true;
        CARDS.get(normalize(
                "Hellfire"
        )).aoe = true;


        /*
         * =====================================================
         * NYKYISEN ARENA-POOLIN UUDET KORTIT
         * =====================================================
         */

        /*
         * Mage
         */

        addCard(
                "Bursting Leyline",
                4.46, 4, 0, 0
        );
        CARDS.get(normalize(
                "Bursting Leyline"
        )).spell = true;
        CARDS.get(normalize(
                "Bursting Leyline"
        )).minion = false;
        CARDS.get(normalize(
                "Bursting Leyline"
        )).removal = true;


        addCard(
                "Contraband Wands",
                5.0, 2, 0, 0
        );
        CARDS.get(normalize(
                "Contraband Wands"
        )).spell = true;
        CARDS.get(normalize(
                "Contraband Wands"
        )).minion = false;


        addCard(
                "Crystallized Leyline",
                5.85, 0, 0, 0
        );
        CARDS.get(normalize(
                "Crystallized Leyline"
        )).spell = true;
        CARDS.get(normalize(
                "Crystallized Leyline"
        )).minion = false;


        addCard(
                "Surge Needle",
                6.08, 0, 0, 0
        );
        CARDS.get(normalize(
                "Surge Needle"
        )).spell = true;
        CARDS.get(normalize(
                "Surge Needle"
        )).minion = false;


        addCard(
                "Leyline Nexus",
                4.85, 0, 0, 0
        );
        CARDS.get(normalize(
                "Leyline Nexus"
        )).spell = true;
        CARDS.get(normalize(
                "Leyline Nexus"
        )).minion = false;


        addCard(
                "Mystic Runesaber",
                5.46, 4, 3, 4
        );


        addCard(
                "Ley Walker",
                0.0, 0, 0, 0
        );


        addCard(
                "Cold Snap",
                5.77, 0, 0, 0
        );
        CARDS.get(normalize(
                "Cold Snap"
        )).spell = true;
        CARDS.get(normalize(
                "Cold Snap"
        )).minion = false;


        addCard(
                "Code Violet",
                5.54, 0, 0, 0
        );
        CARDS.get(normalize(
                "Code Violet"
        )).spell = true;
        CARDS.get(normalize(
                "Code Violet"
        )).minion = false;


        /*
         * Neutral
         */

        addCard(
                "Tunneling Geomancer",
                5.85, 3, 3, 3
        );


        addCard(
                "Watfin",
                6.54, 0, 0, 0
        );


        addCard(
                "Zilliax Deluxe 3000",
                6.15, 0, 0, 0
        );


        addCard(
                "Shadowed Informant",
                7.69, 0, 0, 0
        );


        addCard(
                "Hopeful Dryad",
                6.31, 3, 3, 3
        );


        addCard(
                "Raptor Herald",
                6.77, 3, 3, 3
        );


        addCard(
                "Carrier Whelp",
                6.92, 0, 0, 0
        );


        /*
         * Death Knight
         */

        addCard(
                "Experimental Animation",
                8.85, 0, 0, 0
        );


        addCard(
                "Obsessive Technician",
                8.85, 0, 0, 0
        );


        addCard(
                "Violet Punisher",
                7.92, 0, 0, 0
        );


        addCard(
                "Whelp of the Infinite",
                7.92, 0, 0, 0
        );


        addCard(
                "Infested Breath",
                7.77, 0, 0, 0
        );


        addCard(
                "Emergency Surgery",
                6.15, 0, 0, 0
        );


        addCard(
                "Drink Blood",
                5.62, 0, 0, 0
        );


        addCard(
                "Disguised Doctor",
                4.38, 0, 0, 0
        );


        addCard(
                "Corpse Cannon",
                7.38, 0, 0, 0
        );


        /*
         * Demon Hunter
         */

        addCard(
                "Void Soul",
                5.54, 0, 0, 0
        );


        addCard(
                "Void Blast",
                6.15, 0, 0, 0
        );


        addCard(
                "Vicious Voidscale",
                6.62, 0, 0, 0
        );


        /*
         * Druid
         */

        addCard(
                "Widow's Bite",
                5.77, 0, 0, 0
        );


        addCard(
                "Infest the Scullery",
                4.54, 0, 0, 0
        );


        /*
         * Hunter
         */

        addCard(
                "Underbelly Network",
                7.31, 0, 0, 0
        );


        addCard(
                "Guard Dog",
                6.77, 0, 0, 0
        );


        addCard(
                "Dig for Freedom",
                6.92, 0, 0, 0
        );


        /*
         * Paladin
         */

        addCard(
                "Vigilant Sentry",
                1.85, 0, 0, 0
        );


        addCard(
                "Truth Seeker",
                5.92, 0, 0, 0
        );


        addCard(
                "Judgment",
                6.69, 0, 0, 0
        );


        addCard(
                "Holy Bola!",
                4.31, 0, 0, 0
        );


        addCard(
                "Dalaran Champion",
                4.15, 0, 0, 0
        );


        addCard(
                "Commander Beatrix",
                4.23, 0, 0, 0
        );


        /*
         * Priest
         */

        addCard(
                "Undeath Sentence",
                3.85, 0, 0, 0
        );


        addCard(
                "Karov the Broken",
                7.92, 0, 0, 0
        );


        /*
         * Rogue
         */

        addCard(
                "Jade Guardians",
                6.23, 0, 0, 0
        );


        addCard(
                "Inspector Murloc Holmes",
                5.38, 0, 0, 0
        );


        /*
         * Mage
         */

        addCard(
                "Jailhouse Manastorm",
                10.0, 0, 0, 0
        );


        /*
         * Warrior
         */

        addCard(
                "Warptooth",
                6.92, 0, 0, 0
        );
    }


    /*
     * =========================================================
     * ADD CARD
     * =========================================================
     */

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

        add(card);
    }


    private static void add(
            CardData card
    ) {

        CARDS.put(
                normalize(card.name),
                card
        );
    }


    /*
     * =========================================================
     * OCR-KORJAUKSET
     * =========================================================
     */

    private static String correctOcr(
            String cardName
    ) {

        if (cardName == null) {
            return "";
        }


        String original =
                cardName.trim();


        if (original.isEmpty()) {
            return "";
        }


        String normalized =
                normalize(original);


        /*
         * Soldier of the Infinite
         */

        if (normalized.contains("soldierolf")
                || normalized.contains("sotdier")
                || normalized.contains("so1dier")
                || normalized.contains("soldiero")
                || normalized.contains("soldier2of")
                || normalized.contains("soldieroftheinfinite")
                || normalized.contains("soldierofinfinite")
                || normalized.contains("soldieroftheinfinite")) {

            return "Soldier of the Infinite";
        }


        /*
         * Raban Wands
         */

        if (normalized.equals("rabanwands")
                || normalized.contains("rabanwand")) {

            return "Raban Wands";
        }


        /*
         * Bursting Leyline
         */

        if (normalized.equals("burstingleyline")
                || normalized.contains("burstingleyline")
                || normalized.contains("burstingley1ine")
                || normalized.contains("burstingleyIine")) {

            return "Bursting Leyline";
        }


        /*
         * Contraband Wands
         */

        if (normalized.equals("contrabandwands")
                || normalized.contains("contrabandwand")
                || normalized.contains("contrabandwands")) {

            return "Contraband Wands";
        }


        /*
         * Crystallized Leyline
         */

        if (normalized.contains("crystallizedleyline")
                || normalized.contains("crystallizedley1ine")
                || normalized.contains("crystalizedleyline")) {

            return "Crystallized Leyline";
        }


        /*
         * Surge Needle
         */

        if (normalized.contains("surgeneedle")) {

            return "Surge Needle";
        }


        /*
         * Leyline Nexus
         */

        if (normalized.contains("leylinenexus")) {

            return "Leyline Nexus";
        }


        /*
         * Mystic Runesaber
         */

        if (normalized.contains("mysticrunesaber")
                || normalized.contains("mysticrunesabre")) {

            return "Mystic Runesaber";
        }


        /*
         * Tunneling Geomancer
         */

        if (normalized.contains("tunnelinggeomancer")
                || normalized.contains("tunnelinggeomancer")) {

            return "Tunneling Geomancer";
        }


        /*
         * Cold Snap
         */

        if (normalized.contains("coldsnap")) {

            return "Cold Snap";
        }


        /*
         * Code Violet
         */

        if (normalized.contains("codeviolet")) {

            return "Code Violet";
        }


        /*
         * Frostbolt
         */

        if (normalized.equals("frostbo1t")
                || normalized.equals("frostboit")) {

            return "Frostbolt";
        }


        /*
         * Fireball
         */

        if (normalized.equals("fireba11")
                || normalized.equals("firebali")) {

            return "Fireball";
        }


        /*
         * Water Elemental
         */

        if (normalized.contains("waterelementa1")
                || normalized.contains("waterelementai")) {

            return "Water Elemental";
        }


        return original;
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

        String[] cards = {
                card1,
                card2,
                card3
        };


        double bestScore = -1;

        int bestIndex = -1;

        String bestCard = null;


        for (int i = 0;
             i < cards.length;
             i++) {

            if (!isValidCard(cards[i])) {
                continue;
            }


            String correctedCard =
                    correctOcr(
                            cards[i]
                    );


            double currentScore =
                    score(
                            correctedCard
                    );


            if (currentScore > bestScore) {

                bestScore =
                        currentScore;

                bestIndex =
                        i;

                bestCard =
                        correctedCard;
            }
        }


        if (bestIndex == -1) {

            return "Ei suositusta";
        }


        return "KORTTI "
                + (bestIndex + 1)
                + ": "
                + bestCard
                + "\nPisteet: "
                + formatScore(bestScore)
                + "/10";
    }


    /*
     * =========================================================
     * PISTEET
     * =========================================================
     */

    public static String getCardScore(
            String cardName
    ) {

        if (!isValidCard(cardName)) {

            return "0.0";
        }


        String corrected =
                correctOcr(
                        cardName
                );


        return formatScore(
                score(corrected)
        );
    }


    /*
     * =========================================================
     * PERUSTELU
     * =========================================================
     */

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

            return "Kortille ei ole vielä tarkempia tietoja";
        }


        StringBuilder reason =
                new StringBuilder();


        if (card.removal) {

            reason.append("poisto");
        }


        if (card.aoe) {

            addReason(
                    reason,
                    "aluepoisto"
            );
        }


        if (card.draw) {

            addReason(
                    reason,
                    "korttien nosto"
            );
        }


        if (card.discover) {

            addReason(
                    reason,
                    "Discover"
            );
        }


        if (card.taunt) {

            addReason(
                    reason,
                    "Taunt"
            );
        }


        if (card.divineShield) {

            addReason(
                    reason,
                    "Divine Shield"
            );
        }


        if (card.battlecry) {

            addReason(
                    reason,
                    "Battlecry"
            );
        }


        if (card.deathrattle) {

            addReason(
                    reason,
                    "Deathrattle"
            );
        }


        if (reason.length() == 0) {

            return "Hyvä kokonaisarvo Arena-pakassa";
        }


        return reason.toString();
    }


    private static void addReason(
            StringBuilder reason,
            String text
    ) {

        if (reason.length() > 0) {

            reason.append(", ");
        }


        reason.append(text);
    }


    /*
     * =========================================================
     * SCORE
     * =========================================================
     */

    private static double score(
            String cardName
    ) {

        if (!isValidCard(cardName)) {

            return 0.0;
        }


        String corrected =
                correctOcr(cardName);


        CardData card =
                CARDS.get(
                        normalize(corrected)
                );


        /*
         * Tuntematon kortti.
         *
         * 5.0 säilyy edelleen vararatkaisuna,
         * mutta tunnetut uudet kortit eivät enää
         * päädy tähän.
         */

        if (card == null) {

            return 5.0;
        }


        double score =
                card.baseScore;


        /*
         * Ominaisuusbonukset.
         */

        if (card.removal) {

            score += 0.2;
        }


        if (card.aoe) {

            score += 0.3;
        }


        if (card.draw) {

            score += 0.15;
        }


        if (card.discover) {

            score += 0.2;
        }


        if (card.taunt) {

            score += 0.1;
        }


        if (card.divineShield) {

            score += 0.15;
        }


        if (card.battlecry) {

            score += 0.1;
        }


        if (card.deathrattle) {

            score += 0.1;
        }


        return Math.min(
                10.0,
                Math.max(
                        0.0,
                        score
                )
        );
    }


    /*
     * =========================================================
     * VALID CARD
     * =========================================================
     */

    private static boolean isValidCard(
            String cardName
    ) {

        if (cardName == null) {

            return false;
        }


        String value =
                cardName.trim();


        if (value.isEmpty()) {

            return false;
        }


        if (value.equalsIgnoreCase(
                "Ei tunnistettu"
        )) {

            return false;
        }


        return true;
    }


    /*
     * =========================================================
     * NORMALIZE
     * =========================================================
     */

    private static String normalize(
            String value
    ) {

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


    /*
     * =========================================================
     * SCORE DISPLAY
     * =========================================================
     */

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
