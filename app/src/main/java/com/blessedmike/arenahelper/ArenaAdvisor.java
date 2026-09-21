package com.blessedmike.arenahelper;

import java.util.HashMap;
import java.util.Map;

public class ArenaAdvisor {

/*
 * Yksinkertainen ensimmäinen Arena-arvioija.
 *
 * Tärkeä idea:
 * Tätä tiedostoa voidaan myöhemmin päivittää paljon
 * paremmalla korttidatalla ilman että OCR:ää tarvitsee
 * muuttaa.
 */

private static final Map<String, Double> CARD_SCORES =
        new HashMap<>();

static {

    /*
     * Esimerkkikortteja.
     *
     * Näitä kasvatetaan myöhemmin kattamaan koko
     * Arena-korttipooli.
     */

    CARD_SCORES.put(
            normalize("Soldier of the Infinite"),
            8.2
    );

    CARD_SCORES.put(
            normalize("Raban Wands"),
            6.5
    );
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

    int bestIndex = 0;

    for (int i = 1; i < 3; i++) {

        if (scores[i] > scores[bestIndex]) {
            bestIndex = i;
        }
    }

    String bestCard =
            cards[bestIndex];

    double bestScore =
            scores[bestIndex];

    if (bestCard == null ||
            bestCard.isEmpty() ||
            bestCard.equals("Ei tunnistettu")) {

        return "SUOSITUS: Ei vielä tarpeeksi tietoa";
    }

    return String.format(
            "★ SUOSITUS: KORTTI %d\n%s\n\nPisteet: %.1f / 10",
            bestIndex + 1,
            bestCard,
            bestScore
    );
}

public static double score(
        String cardName
) {

    if (cardName == null ||
            cardName.isEmpty()) {

        return 0.0;
    }

    String normalized =
            normalize(cardName);

    Double knownScore =
            CARD_SCORES.get(normalized);

    if (knownScore != null) {
        return knownScore;
    }

    /*
     * Tuntemattomalle kortille annetaan
     * neutraali lähtöpiste.
     *
     * Tämä muutetaan myöhemmin oikeaksi
     * korttidatan haulla.
     */
    return 5.0;
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
