package com.blessedmike.arenahelper;

import java.util.HashMap;
import java.util.Map;

public class ArenaAdvisor {

    private static class CardData {
        String name;
        double score;

        CardData(String name, double score) {
            this.name = name;
            this.score = score;
        }
    }

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    static {

        // =========================
        // TESTIKORTIT
        // =========================

        addCard("Soldier of the Infinite", 8.2);
        addCard("Raban Wands", 6.5);

        // =========================
        // YLEISIÄ TESTIKORTTEJA
        // =========================

        addCard("Frostbolt", 7.5);
        addCard("Fireball", 8.0);
        addCard("Water Elemental", 8.0);
        addCard("Chillwind Yeti", 6.0);
        addCard("Boulderfist Ogre", 5.0);
        addCard("River Crocolisk", 4.0);
        addCard("Bloodfen Raptor", 4.0);
        addCard("Wisp", 2.0);
        addCard("Murloc Raider", 3.0);
        addCard("Arcane Intellect", 6.5);
        addCard("Polymorph", 7.0);
        addCard("Flamestrike", 8.0);
        addCard("Consecration", 7.5);
        addCard("Truesilver Champion", 8.5);
        addCard("Fiery War Axe", 8.0);
        addCard("Shadow Word: Pain", 6.5);
        addCard("Holy Nova", 7.0);
        addCard("Backstab", 7.0);
        addCard("Eviscerate", 8.0);
        addCard("Swipe", 8.0);
        addCard("Starfall", 7.0);
        addCard("Kill Command", 7.0);
        addCard("Animal Companion", 8.0);
        addCard("Hex", 7.5);
        addCard("Lightning Bolt", 6.5);
        addCard("Earth Elemental", 6.5);
        addCard("Hellfire", 7.0);
        addCard("Shadow Bolt", 6.0);
        addCard("Darkbomb", 6.5);
        addCard("Fiery Win Axe", 8.0);
    }

    private static void addCard(
            String name,
            double score
    ) {
        CARDS.put(normalize(name),
                new CardData(name, score));
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

        double[] scores = {
                score(card1),
                score(card2),
                score(card3)
        };

        int bestIndex = -1;

        double bestScore = -1;

        for (int i = 0; i < 3; i++) {

            if (!isValidCard(cards[i])) {
                continue;
            }

            if (scores[i] > bestScore) {
                bestScore = scores[i];
                bestIndex = i;
            }
        }

        if (bestIndex == -1) {
            return "★ SUOSITUS: Ei tunnistettavaa korttia";
        }

        return String.format(
                "★ SUOSITUS: KORTTI %d\n%s\n\nPisteet: %.1f / 10",
                bestIndex + 1,
                cards[bestIndex],
                bestScore
        );
    }

    public static double score(
            String cardName
    ) {

        if (!isValidCard(cardName)) {
            return 0.0;
        }

        String normalized =
                normalize(cardName);

        CardData card =
                CARDS.get(normalized);

        if (card != null) {
            return card.score;
        }

        /*
         * Tuntematon kortti.
         *
         * Tätä ei vielä arvioida oikeasti.
         * Myöhemmin tähän tulee varsinainen
         * Arena-arvio.
         */
        return 5.0;
    }

    private static boolean isValidCard(
            String cardName
    ) {

        if (cardName == null) {
            return false;
        }

        if (cardName.trim().isEmpty()) {
            return false;
        }

        if (cardName.equalsIgnoreCase(
                "Ei tunnistettu")) {
            return false;
        }

        return true;
    }

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }
}
