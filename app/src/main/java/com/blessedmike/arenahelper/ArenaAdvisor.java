package com.blessedmike.arenahelper;

import java.util.HashMap;
import java.util.Map;

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

        addCard(
                "Soldier of the Infinite",
                8.2, 3, 3, 4
        );

        CARDS.get(normalize("Soldier of the Infinite")).taunt = true;

        addCard(
                "Raban Wands",
                6.5, 3, 3, 3
        );

        addCard(
                "Frostbolt",
                7.5, 2, 0, 0
        );

        addCard(
                "Fireball",
                8.0, 4, 0, 0
        );

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
        CARDS.get(normalize("Arcane Intellect")).draw = true;

        addCard(
                "Polymorph",
                7.0, 4, 0, 0
        );
        CARDS.get(normalize("Polymorph")).removal = true;

        addCard(
                "Flamestrike",
                8.0, 7, 0, 0
        );
        CARDS.get(normalize("Flamestrike")).removal = true;
        CARDS.get(normalize("Flamestrike")).aoe = true;

        addCard(
                "Consecration",
                7.5, 4, 0, 0
        );
        CARDS.get(normalize("Consecration")).removal = true;
        CARDS.get(normalize("Consecration")).aoe = true;

        addCard(
                "Truesilver Champion",
                8.5, 4, 4, 2
        );
        CARDS.get(normalize("Truesilver Champion")).weapon = true;
        CARDS.get(normalize("Truesilver Champion")).removal = true;

        addCard(
                "Fiery War Axe",
                8.0, 2, 3, 2
        );
        CARDS.get(normalize("Fiery War Axe")).weapon = true;
        CARDS.get(normalize("Fiery War Axe")).removal = true;

        addCard(
                "Shadow Word: Pain",
                6.5, 2, 0, 0
        );
        CARDS.get(normalize("Shadow Word: Pain")).removal = true;

        addCard(
                "Holy Nova",
                7.0, 5, 0, 0
        );
        CARDS.get(normalize("Holy Nova")).removal = true;
        CARDS.get(normalize("Holy Nova")).aoe = true;

        addCard(
                "Backstab",
                7.0, 0, 0, 0
        );
        CARDS.get(normalize("Backstab")).removal = true;

        addCard(
                "Eviscerate",
                8.0, 2, 0, 0
        );
        CARDS.get(normalize("Eviscerate")).removal = true;

        addCard(
                "Swipe",
                8.0, 4, 0, 0
        );
        CARDS.get(normalize("Swipe")).removal = true;
        CARDS.get(normalize("Swipe")).aoe = true;

        addCard(
                "Kill Command",
                7.0, 3, 0, 0
        );
        CARDS.get(normalize("Kill Command")).removal = true;

        addCard(
                "Animal Companion",
                8.0, 3, 0, 0
        );

        addCard(
                "Hex",
                7.5, 4, 0, 0
        );
        CARDS.get(normalize("Hex")).removal = true;

        addCard(
                "Lightning Bolt",
                6.5, 1, 0, 0
        );
        CARDS.get(normalize("Lightning Bolt")).removal = true;

        addCard(
                "Hellfire",
                7.0, 4, 0, 0
        );
        CARDS.get(normalize("Hellfire")).removal = true;
        CARDS.get(normalize("Hellfire")).aoe = true;
    }

    private static void addCard(
            String name,
            double score,
            int mana,
            int attack,
            int health
    ) {
        add(new CardData(
                name,
                score,
                mana,
                attack,
                health
        ));
    }

    private static void add(CardData card) {
        CARDS.put(
                normalize(card.name),
                card
        );
    }

    /*
     * ---------------------------------------------------------
     * OCR-KORJAUKSET
     * ---------------------------------------------------------
     */

    private static String correctOcr(String cardName) {

        if (cardName == null) {
            return "";
        }

        String original = cardName.trim();

        if (original.isEmpty()) {
            return "";
        }

        String normalized = normalize(original);

        /*
         * Soldier of the Infinite
         *
         * OCR voi lukea esimerkiksi:
         *
         * Soldierolf the Infinite
         * Soldierolf the infinit
         * Soldier of the Infinite
         * Soldier of the infinit
         * Sotdier of Infinite
         * So1dier of the Infinite
         */

        if (normalized.contains("soldierolf")
                || normalized.contains("sotdier")
                || normalized.contains("so1dier")
                || normalized.contains("soldiero")
                || normalized.contains("soldier2of")
                || normalized.contains("soldieroftheinfinite")
                || normalized.contains("soldierofinfinite")) {

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
         * Frostbolt
         */

        if (normalized.equals("frostbo1t")
                || normalized.equals("frostboit")
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
     * ---------------------------------------------------------
     * SUOSITUS
     * ---------------------------------------------------------
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

        for (int i = 0; i < cards.length; i++) {

            if (!isValidCard(cards[i])) {
                continue;
            }

            String correctedCard =
                    correctOcr(cards[i]);

            double currentScore =
                    score(correctedCard);

            if (currentScore > bestScore) {

                bestScore = currentScore;
                bestIndex = i;
                bestCard = correctedCard;
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
     * ---------------------------------------------------------
     * PISTEET
     * ---------------------------------------------------------
     */

    public static String getCardScore(
            String cardName
    ) {

        if (!isValidCard(cardName)) {
            return "0.0";
        }

        String corrected =
                correctOcr(cardName);

        return formatScore(
                score(corrected)
        );
    }

    /*
     * ---------------------------------------------------------
     * PERUSTELU
     * ---------------------------------------------------------
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
            addReason(reason, "aluepoisto");
        }

        if (card.draw) {
            addReason(reason, "korttien nosto");
        }

        if (card.discover) {
            addReason(reason, "Discover");
        }

        if (card.taunt) {
            addReason(reason, "Taunt");
        }

        if (card.divineShield) {
            addReason(reason, "Divine Shield");
        }

        if (card.battlecry) {
            addReason(reason, "Battlecry");
        }

        if (card.deathrattle) {
            addReason(reason, "Deathrattle");
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
     * ---------------------------------------------------------
     * SCORE
     * ---------------------------------------------------------
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
         * Tätä ei muuteta nollaksi, jotta yksi OCR-virhe
         * ei automaattisesti tuhoa koko suositusta.
         */

        if (card == null) {
            return 5.0;
        }

        double score =
                card.baseScore;

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
     * ---------------------------------------------------------
     * VALID CARD
     * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * NORMALIZE
     * ---------------------------------------------------------
     */

    private static String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    /*
     * ---------------------------------------------------------
     * SCORE DISPLAY
     * ---------------------------------------------------------
     */

    private static String formatScore(
            double score
    ) {

        return String.format(
                java.util.Locale.US,
                "%.1f",
                score
        );
    }
}
