package com.blessedmike.arenahelper;

import java.util.HashMap;
import java.util.Map;

public class ArenaAdvisor {

    /*
     * Arena Advisor v2
     *
     * Tavoite:
     * - tunnistaa tunnetut kortit
     * - antaa niille peruspisteen
     * - käyttää kortin ominaisuuksia arvioinnissa
     * - mahdollistaa myöhemmin koko korttidatan lisäämisen
     */

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
        }
    }

    private static final Map<String, CardData> CARDS =
            new HashMap<>();


    // =========================================================
    // KORTTIDATA
    // =========================================================

    static {

        /*
         * Soldier of the Infinite
         */
        CardData soldier =
                new CardData(
                        "Soldier of the Infinite",
                        8.2,
                        3,
                        3,
                        4
                );

        soldier.taunt = true;

        add(soldier);


        /*
         * Raban Wands
         */
        CardData raban =
                new CardData(
                        "Raban Wands",
                        6.5,
                        3,
                        3,
                        3
                );

        add(raban);


        /*
         * Frostbolt
         */
        CardData frostbolt =
                new CardData(
                        "Frostbolt",
                        7.5,
                        2,
                        0,
                        0
                );

        frostbolt.minion = false;
        frostbolt.spell = true;
        frostbolt.removal = true;

        add(frostbolt);


        /*
         * Fireball
         */
        CardData fireball =
                new CardData(
                        "Fireball",
                        8.0,
                        4,
                        0,
                        0
                );

        fireball.minion = false;
        fireball.spell = true;
        fireball.removal = true;

        add(fireball);


        /*
         * Water Elemental
         */
        CardData waterElemental =
                new CardData(
                        "Water Elemental",
                        8.0,
                        4,
                        3,
                        6
                );

        add(waterElemental);


        /*
         * Chillwind Yeti
         */
        CardData yeti =
                new CardData(
                        "Chillwind Yeti",
                        6.0,
                        4,
                        4,
                        5
                );

        add(yeti);


        /*
         * Boulderfist Ogre
         */
        CardData ogre =
                new CardData(
                        "Boulderfist Ogre",
                        5.0,
                        6,
                        6,
                        7
                );

        add(ogre);


        /*
         * River Crocolisk
         */
        CardData crocolisk =
                new CardData(
                        "River Crocolisk",
                        4.0,
                        2,
                        2,
                        3
                );

        add(crocolisk);


        /*
         * Bloodfen Raptor
         */
        CardData raptor =
                new CardData(
                        "Bloodfen Raptor",
                        4.0,
                        2,
                        3,
                        2
                );

        add(raptor);


        /*
         * Arcane Intellect
         */
        CardData arcaneIntellect =
                new CardData(
                        "Arcane Intellect",
                        6.5,
                        3,
                        0,
                        0
                );

        arcaneIntellect.minion = false;
        arcaneIntellect.spell = true;
        arcaneIntellect.draw = true;

        add(arcaneIntellect);


        /*
         * Polymorph
         */
        CardData polymorph =
                new CardData(
                        "Polymorph",
                        7.0,
                        4,
                        0,
                        0
                );

        polymorph.minion = false;
        polymorph.spell = true;
        polymorph.removal = true;

        add(polymorph);


        /*
         * Flamestrike
         */
        CardData flamestrike =
                new CardData(
                        "Flamestrike",
                        8.0,
                        7,
                        0,
                        0
                );

        flamestrike.minion = false;
        flamestrike.spell = true;
        flamestrike.removal = true;
        flamestrike.aoe = true;

        add(flamestrike);


        /*
         * Consecration
         */
        CardData consecration =
                new CardData(
                        "Consecration",
                        7.5,
                        4,
                        0,
                        0
                );

        consecration.minion = false;
        consecration.spell = true;
        consecration.removal = true;
        consecration.aoe = true;

        add(consecration);


        /*
         * Truesilver Champion
         */
        CardData truesilver =
                new CardData(
                        "Truesilver Champion",
                        8.5,
                        4,
                        4,
                        0
                );

        truesilver.minion = false;
        truesilver.spell = false;
        truesilver.weapon = true;
        truesilver.removal = true;

        add(truesilver);


        /*
         * Fiery War Axe
         */
        CardData fieryWarAxe =
                new CardData(
                        "Fiery War Axe",
                        8.0,
                        2,
                        3,
                        0
                );

        fieryWarAxe.minion = false;
        fieryWarAxe.weapon = true;
        fieryWarAxe.removal = true;

        add(fieryWarAxe);


        /*
         * Shadow Word: Pain
         */
        CardData shadowWordPain =
                new CardData(
                        "Shadow Word: Pain",
                        6.5,
                        2,
                        0,
                        0
                );

        shadowWordPain.minion = false;
        shadowWordPain.spell = true;
        shadowWordPain.removal = true;

        add(shadowWordPain);


        /*
         * Holy Nova
         */
        CardData holyNova =
                new CardData(
                        "Holy Nova",
                        7.0,
                        5,
                        0,
                        0
                );

        holyNova.minion = false;
        holyNova.spell = true;
        holyNova.removal = true;
        holyNova.aoe = true;

        add(holyNova);


        /*
         * Backstab
         */
        CardData backstab =
                new CardData(
                        "Backstab",
                        7.0,
                        0,
                        0,
                        0
                );

        backstab.minion = false;
        backstab.spell = true;
        backstab.removal = true;

        add(backstab);


        /*
         * Eviscerate
         */
        CardData eviscerate =
                new CardData(
                        "Eviscerate",
                        8.0,
                        2,
                        0,
                        0
                );

        eviscerate.minion = false;
        eviscerate.spell = true;
        eviscerate.removal = true;

        add(eviscerate);


        /*
         * Swipe
         */
        CardData swipe =
                new CardData(
                        "Swipe",
                        8.0,
                        4,
                        0,
                        0
                );

        swipe.minion = false;
        swipe.spell = true;
        swipe.removal = true;
        swipe.aoe = true;

        add(swipe);


        /*
         * Kill Command
         */
        CardData killCommand =
                new CardData(
                        "Kill Command",
                        7.0,
                        3,
                        0,
                        0
                );

        killCommand.minion = false;
        killCommand.spell = true;
        killCommand.removal = true;

        add(killCommand);


        /*
         * Animal Companion
         */
        CardData animalCompanion =
                new CardData(
                        "Animal Companion",
                        8.0,
                        3,
                        0,
                        0
                );

        animalCompanion.minion = false;
        animalCompanion.spell = true;

        add(animalCompanion);


        /*
         * Hex
         */
        CardData hex =
                new CardData(
                        "Hex",
                        7.5,
                        4,
                        0,
                        0
                );

        hex.minion = false;
        hex.spell = true;
        hex.removal = true;

        add(hex);


        /*
         * Lightning Bolt
         */
        CardData lightningBolt =
                new CardData(
                        "Lightning Bolt",
                        6.5,
                        1,
                        0,
                        0
                );

        lightningBolt.minion = false;
        lightningBolt.spell = true;
        lightningBolt.removal = true;

        add(lightningBolt);


        /*
         * Hellfire
         */
        CardData hellfire =
                new CardData(
                        "Hellfire",
                        7.0,
                        4,
                        0,
                        0
                );

        hellfire.minion = false;
        hellfire.spell = true;
        hellfire.removal = true;
        hellfire.aoe = true;

        add(hellfire);
    }


    // =========================================================
    // LISÄÄ KORTTI
    // =========================================================

    private static void add(CardData card) {

        CARDS.put(
                normalize(card.name),
                card
        );
    }


    // =========================================================
    // SUOSITUS
    // =========================================================

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

            return
                    "★ SUOSITUS\n" +
                    "Ei tunnistettavaa korttia";
        }


        return String.format(
                "★ SUOSITUS: KORTTI %d\n%s\n\n" +
                "Pisteet: %.1f / 10",
                bestIndex + 1,
                cards[bestIndex],
                bestScore
        );
    }


    // =========================================================
    // KORTIN PISTEYTYS
    // =========================================================

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


        /*
         * Tuntematon kortti.
         */
        if (card == null) {

            return 5.0;
        }


        double score =
                card.baseScore;


        /*
         * Removal on Arenassa arvokasta.
         */
        if (card.removal) {

            score += 0.2;
        }


        /*
         * AoE saa pienen lisän.
         */
        if (card.aoe) {

            score += 0.3;
        }


        /*
         * Kortin nosto on hyödyllistä.
         */
        if (card.draw) {

            score += 0.15;
        }


        /*
         * Discover antaa joustavuutta.
         */
        if (card.discover) {

            score += 0.2;
        }


        /*
         * Taunt auttaa board controlissa.
         */
        if (card.taunt) {

            score += 0.1;
        }


        /*
         * Divine Shield.
         */
        if (card.divineShield) {

            score += 0.15;
        }


        /*
         * Battlecry.
         */
        if (card.battlecry) {

            score += 0.1;
        }


        /*
         * Deathrattle.
         */
        if (card.deathrattle) {

            score += 0.1;
        }


        /*
         * Pidetään pisteet välillä 0–10.
         */
        if (score > 10.0) {

            score = 10.0;
        }

        if (score < 0.0) {

            score = 0.0;
        }


        return score;
    }


    // =========================================================
    // OCR:N KORTTITARKISTUS
    // =========================================================

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


    // =========================================================
    // NORMALISOINTI
    // =========================================================

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
