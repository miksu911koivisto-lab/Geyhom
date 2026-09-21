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

        add(new CardData(
                "Soldier of the Infinite",
                8.2, 3, 3, 4
        ));

        CARDS.get(normalize("Soldier of the Infinite")).taunt = true;

        add(new CardData(
                "Raban Wands",
                6.5, 3, 3, 3
        ));

        add(new CardData(
                "Frostbolt",
                7.5, 2, 0, 0
        ));

        add(new CardData(
                "Fireball",
                8.0, 4, 0, 0
        ));

        add(new CardData(
                "Water Elemental",
                8.0, 4, 3, 6
        ));

        add(new CardData(
                "Chillwind Yeti",
                6.0, 4, 4, 5
        ));

        add(new CardData(
                "Boulderfist Ogre",
                5.0, 6, 6, 7
        ));

        add(new CardData(
                "River Crocolisk",
                4.0, 2, 2, 3
        ));

        add(new CardData(
                "Bloodfen Raptor",
                4.0, 2, 3, 2
        ));

        add(new CardData(
                "Arcane Intellect",
                6.5, 3, 0, 0
        ));
        CARDS.get(normalize("Arcane Intellect")).draw = true;

        add(new CardData(
                "Polymorph",
                7.0, 4, 0, 0
        ));
        CARDS.get(normalize("Polymorph")).removal = true;

        add(new CardData(
                "Flamestrike",
                8.0, 7, 0, 0
        ));
        CARDS.get(normalize("Flamestrike")).removal = true;
        CARDS.get(normalize("Flamestrike")).aoe = true;

        add(new CardData(
                "Consecration",
                7.5, 4, 0, 0
        ));
        CARDS.get(normalize("Consecration")).removal = true;
        CARDS.get(normalize("Consecration")).aoe = true;

        add(new CardData(
                "Truesilver Champion",
                8.5, 4, 4, 2
        ));
        CARDS.get(normalize("Truesilver Champion")).weapon = true;
        CARDS.get(normalize("Truesilver Champion")).removal = true;

        add(new CardData(
                "Fiery War Axe",
                8.0, 2, 3, 2
        ));
        CARDS.get(normalize("Fiery War Axe")).weapon = true;
        CARDS.get(normalize("Fiery War Axe")).removal = true;

        add(new CardData(
                "Shadow Word: Pain",
                6.5, 2, 0, 0
        ));
        CARDS.get(normalize("Shadow Word: Pain")).removal = true;

        add(new CardData(
                "Holy Nova",
                7.0, 5, 0, 0
        ));
        CARDS.get(normalize("Holy Nova")).removal = true;
        CARDS.get(normalize("Holy Nova")).aoe = true;

        add(new CardData(
                "Backstab",
                7.0, 0, 0, 0
        ));
        CARDS.get(normalize("Backstab")).removal = true;

        add(new CardData(
                "Eviscerate",
                8.0, 2, 0, 0
        ));
        CARDS.get(normalize("Eviscerate")).removal = true;

        add(new CardData(
                "Swipe",
                8.0, 4, 0, 0
        ));
        CARDS.get(normalize("Swipe")).removal = true;
        CARDS.get(normalize("Swipe")).aoe = true;

        add(new CardData(
                "Kill Command",
                7.0, 3, 0, 0
        ));
        CARDS.get(normalize("Kill Command")).removal = true;

        add(new CardData(
                "Animal Companion",
                8.0, 3, 0, 0
        ));

        add(new CardData(
                "Hex",
                7.5, 4, 0, 0
        ));
        CARDS.get(normalize("Hex")).removal = true;

        add(new CardData(
                "Lightning Bolt",
                6.5, 1, 0, 0
        ));
        CARDS.get(normalize("Lightning Bolt")).removal = true;

        add(new CardData(
                "Hellfire",
                7.0, 4, 0, 0
        ));
        CARDS.get(normalize("Hellfire")).removal = true;
        CARDS.get(normalize("Hellfire")).aoe = true;
    }

    private static void add(CardData card) {
        CARDS.put(normalize(card.name), card);
    }

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

        for (int i = 0; i < cards.length; i++) {

            if (!isValidCard(cards[i])) {
                continue;
            }

            double currentScore = score(cards[i]);

            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestIndex = i;
            }
        }

        if (bestIndex == -1) {
            return "Ei suositusta";
        }

        return "KORTTI " + (bestIndex + 1)
                + ": "
                + cards[bestIndex]
                + "\nPisteet: "
                + formatScore(bestScore)
                + "/10";
    }

    public static String getCardScore(String cardName) {

        if (!isValidCard(cardName)) {
            return "0.0";
        }

        return formatScore(score(cardName));
    }

    public static String getReason(String cardName) {

        if (!isValidCard(cardName)) {
            return "Korttia ei tunnistettu";
        }

        CardData card = CARDS.get(normalize(cardName));

        if (card == null) {
            return "Kortille ei ole vielä tarkempia tietoja";
        }

        StringBuilder reason = new StringBuilder();

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

    private static double score(String cardName) {

        if (!isValidCard(cardName)) {
            return 0.0;
        }

        CardData card =
                CARDS.get(normalize(cardName));

        if (card == null) {
            return 5.0;
        }

        double score = card.baseScore;

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

        return Math.min(10.0, Math.max(0.0, score));
    }

    private static boolean isValidCard(String cardName) {

        if (cardName == null) {
            return false;
        }

        String value = cardName.trim();

        if (value.isEmpty()) {
            return false;
        }

        if (value.equalsIgnoreCase("Ei tunnistettu")) {
            return false;
        }

        return true;
    }

    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private static String formatScore(double score) {
        return String.format(
                java.util.Locale.US,
                "%.1f",
                score
        );
    }
}
