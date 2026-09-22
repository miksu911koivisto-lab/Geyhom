package com.blessedmike.arenahelper;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArenaAdvisor {

    private static final String HEARTHARENA_URL =
            "https://www.heartharena.com/tierlist";

    /*
     * HearthArena käyttää noin 0-130 asteikkoa.
     * Sovellus näyttää arvon 0-10.
     */
    private static final double HEARTHARENA_SCALE = 13.0;

    /*
     * Tunnistamaton kortti = 0.0.
     */
    private static final double UNKNOWN_CARD_SCORE = 0.0;

    private static final Map<String, CardData> CARDS =
            Collections.synchronizedMap(
                    new HashMap<>()
            );

    private static final Map<String, String> OCR_ALIASES =
            Collections.synchronizedMap(
                    new HashMap<>()
            );

    private static final Map<String, Integer> PICKED_CARDS =
            Collections.synchronizedMap(
                    new HashMap<>()
            );

    private static final Set<String> ONLINE_NAMES =
            Collections.synchronizedSet(
                    new HashSet<>()
            );

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static volatile boolean onlineLoaded = false;
    private static volatile boolean loadingOnline = false;

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
                double baseScore
        ) {
            this.name = name;
            this.baseScore = baseScore;
        }
    }

    static {

        /*
         * ============================
         * VARMISTETUT FALLBACK-KORTIT
         * ============================
         */

        add(
                "Soldier of the Infinite",
                8.2,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false
        );

        add(
                "Crystallized Leyline",
                5.65,
                false,
                true,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Surge Needle",
                5.85,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Bursting Leyline",
                6.5,
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Contraband Wands",
                6.3,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Cold Snap",
                7.5,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Code Violet",
                7.2,
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Ley Walker",
                5.5,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Leyline Nexus",
                6.0,
                false,
                true,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Raban Wands",
                6.5,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Spellweaver's Brilliance",
                3.92,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        add(
                "Windswept Pageturner",
                5.23,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        /*
         * ============================
         * SPIRIT GATHERER
         * ============================
         *
         * HearthArena: 100
         *
         * 100 / 13 = 7.69
         *
         * Näytetään sovelluksessa 7.7.
         *
         * Tämä fallback varmistaa, että kortti
         * saa arvon myös silloin, kun
         * HearthArena-sivun online-dataa ei
         * saada ladattua tai parseri ei löydä
         * korttia.
         */

        add(
                "Spirit Gatherer",
                7.69,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        /*
         * ============================
         * OCR-ALIAKSET
         * ============================
         */

        alias(
                "soldier of ihfinite",
                "Soldier of the Infinite"
        );

        alias(
                "soldier of ihfini",
                "Soldier of the Infinite"
        );

        alias(
                "soldier of infinite",
                "Soldier of the Infinite"
        );

        alias(
                "soldier of the infinite",
                "Soldier of the Infinite"
        );

        alias(
                "so1dier of infinite",
                "Soldier of the Infinite"
        );

        alias(
                "sotdier of infinite",
                "Soldier of the Infinite"
        );

        alias(
                "soldier of the infini",
                "Soldier of the Infinite"
        );

        alias(
                "raban wands",
                "Raban Wands"
        );

        alias(
                "crystallized leyline",
                "Crystallized Leyline"
        );

        alias(
                "crystalized leyline",
                "Crystallized Leyline"
        );

        alias(
                "surge needle",
                "Surge Needle"
        );

        /*
         * Spirit Gatherer:
         * varmistetaan OCR:n erilaiset muodot.
         */
        alias(
                "spirit gatherer",
                "Spirit Gatherer"
        );

        alias(
                "spirit gather",
                "Spirit Gatherer"
        );

        alias(
                "spirit gathere",
                "Spirit Gatherer"
        );

        alias(
                "spirit gatherer",
                "Spirit Gatherer"
        );

        /*
         * Spellweaver's Brilliance:
         * apostrofi voi kadota OCR:ssä.
         */
        alias(
                "spellweaver's brilliance",
                "Spellweaver's Brilliance"
        );

        alias(
                "spellweavers brilliance",
                "Spellweaver's Brilliance"
        );

        alias(
                "spellweaver brilliance",
                "Spellweaver's Brilliance"
        );

        alias(
                "spellweaver's brillianc",
                "Spellweaver's Brilliance"
        );

        alias(
                "spellweavers brillianc",
                "Spellweaver's Brilliance"
        );

        /*
         * Windswept Pageturner:
         * yleisiä yhden merkin OCR-virheitä.
         */
        alias(
                "windswept pageturner",
                "Windswept Pageturner"
        );

        alias(
                "windswept pageturn",
                "Windswept Pageturner"
        );

        alias(
                "windswept pagetumer",
                "Windswept Pageturner"
        );

        /*
         * Yritetään ladata koko nykyinen
         * HearthArena-lista.
         */
        loadOnlineTierList();
    }

    private static void add(
            String name,
            double score,
            boolean minion,
            boolean spell,
            boolean weapon,
            boolean removal,
            boolean aoe,
            boolean draw,
            boolean discover,
            boolean taunt,
            boolean divineShield,
            boolean battlecry,
            boolean deathrattle
    ) {

        CardData data =
                new CardData(
                        name,
                        score
                );

        data.minion = minion;
        data.spell = spell;
        data.weapon = weapon;

        data.removal = removal;
        data.aoe = aoe;
        data.draw = draw;
        data.discover = discover;
        data.taunt = taunt;
        data.divineShield = divineShield;
        data.battlecry = battlecry;
        data.deathrattle = deathrattle;

        CARDS.put(
                normalize(name),
                data
        );
    }

    private static void alias(
            String wrong,
            String correct
    ) {

        OCR_ALIASES.put(
                normalize(wrong),
                correct
        );
    }

    public static void loadOnlineTierList() {

        if (loadingOnline ||
                onlineLoaded) {
            return;
        }

        loadingOnline = true;

        EXECUTOR.execute(() -> {

            HttpURLConnection connection = null;

            try {

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

                connection.setInstanceFollowRedirects(
                        true
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 " +
                        "(Linux; Android 16) " +
                        "AppleWebKit/537.36 " +
                        "Chrome/140.0 Mobile Safari/537.36"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml"
                );

                connection.setRequestProperty(
                        "Accept-Language",
                        "en-US,en;q=0.9"
                );

                int response =
                        connection.getResponseCode();

                if (response >= 200 &&
                        response < 300) {

                    InputStream stream =
                            connection.getInputStream();

                    String html =
                            readStream(stream);

                    if (html != null &&
                            !html.isEmpty()) {

                        decodeHtml(html);

                        /*
                         * Merkitään ladatuksi vain,
                         * jos jotain oikeasti löytyi.
                         */
                        if (!ONLINE_NAMES.isEmpty()) {
                            onlineLoaded = true;
                        }
                    }
                }

            } catch (Exception ignored) {

                /*
                 * Fallback-kortit jäävät käyttöön.
                 */

            } finally {

                if (connection != null) {

                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {}
                }

                loadingOnline = false;
            }
        });
    }

    private static String readStream(
            InputStream stream
    ) throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                stream,
                                "UTF-8"
                        )
                );

        StringBuilder result =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {

            result.append(line);
            result.append('\n');
        }

        reader.close();

        return result.toString();
    }

    private static void decodeHtml(
            String html
    ) {

        if (html == null ||
                html.isEmpty()) {
            return;
        }

        String cleaned =
                html
                        .replace(
                                "&amp;",
                                "&"
                        )
                        .replace(
                                "&quot;",
                                "\""
                        )
                        .replace(
                                "&#039;",
                                "'"
                        )
                        .replace(
                                "&#39;",
                                "'"
                        )
                        .replace(
                                "&apos;",
                                "'"
                        )
                        .replace(
                                "&lt;",
                                "<"
                        )
                        .replace(
                                "&gt;",
                                ">"
                        )
                        .replace(
                                "&nbsp;",
                                " "
                        );

        parseTierRows(cleaned);
        parseLooseCardScores(cleaned);
    }

    private static void parseTierRows(
            String html
    ) {

        String[] lines =
                html.split(
                        "\\r?\\n"
                );

        String previousName = "";

        for (String raw :
                lines) {

            if (raw == null) {
                continue;
            }

            String line =
                    stripHtml(raw)
                            .trim();

            if (line.isEmpty()) {
                continue;
            }

            line =
                    decodeEntities(line)
                            .trim();

            if (looksLikeCardName(line)) {

                previousName =
                        cleanCardName(line);

                continue;
            }

            if (!previousName.isEmpty() &&
                    isScoreLine(line)) {

                try {

                    double score =
                            Double.parseDouble(
                                    line
                            );

                    if (score >= 0 &&
                            score <= 130) {

                        addOnlineCard(
                                previousName,
                                score
                        );

                        previousName = "";
                    }

                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void parseLooseCardScores(
            String html
    ) {

        String text =
                stripHtml(html);

        text =
                decodeEntities(text);

        String[] lines =
                text.split(
                        "\\r?\\n"
                );

        for (int i = 0;
                i < lines.length;
                i++) {

            String line =
                    lines[i].trim();

            if (!looksLikeCardName(line)) {
                continue;
            }

            /*
             * Normaali:
             *
             * Card Name
             * 85
             */
            if (i + 1 <
                    lines.length) {

                String next =
                        lines[i + 1]
                                .trim();

                if (isScoreLine(next)) {

                    try {

                        double score =
                                Double.parseDouble(
                                        next
                                );

                        addOnlineCard(
                                cleanCardName(line),
                                score
                        );

                    } catch (Exception ignored) {
                    }
                }
            }

            /*
             * Jos HTML on pakannut useamman
             * elementin samalle riville,
             * yritetään löytää numeroita
             * samasta tekstistä.
             */
            String[] pieces =
                    line.split(
                            "\\s{2,}"
                    );

            if (pieces.length > 1) {

                for (String piece :
                        pieces) {

                    piece =
                            piece.trim();

                    if (isScoreLine(piece)) {
                        continue;
                    }
                }
            }
        }
    }

    private static boolean isScoreLine(
            String text
    ) {

        if (text == null) {
            return false;
        }

        return text.trim().matches(
                "^\\d{1,3}(?:\\.\\d+)?$"
        );
    }

    private static void addOnlineCard(
            String name,
            double hearthArenaScore
    ) {

        if (name == null ||
                name.trim().isEmpty()) {
            return;
        }

        name =
                cleanCardName(name);

        if (name.isEmpty()) {
            return;
        }

        double converted =
                hearthArenaScore /
                        HEARTHARENA_SCALE;

        converted =
                Math.max(
                        0.0,
                        Math.min(
                                10.0,
                                converted
                        )
                );

        String key =
                normalize(name);

        if (key.isEmpty()) {
            return;
        }

        CardData existing =
                CARDS.get(key);

        if (existing == null) {

            CardData data =
                    new CardData(
                            name,
                            converted
                    );

            CARDS.put(
                    key,
                    data
            );

        } else {

            /*
             * Online-data saa päivittää
             * fallback-arvon.
             */
            existing.baseScore =
                    converted;
        }

        ONLINE_NAMES.add(key);
    }

    private static String stripHtml(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replaceAll(
                        "<script[\\s\\S]*?</script>",
                        " "
                )
                .replaceAll(
                        "<style[\\s\\S]*?</style>",
                        " "
                )
                .replaceAll(
                        "<[^>]+>",
                        "\n"
                )
                .replaceAll(
                        "[ \\t]+",
                        " "
                );
    }

    private static String decodeEntities(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        "&amp;",
                        "&"
                )
                .replace(
                        "&quot;",
                        "\""
                )
                .replace(
                        "&#039;",
                        "'"
                )
                .replace(
                        "&#39;",
                        "'"
                )
                .replace(
                        "&apos;",
                        "'"
                )
                .replace(
                        "&lt;",
                        "<"
                )
                .replace(
                        "&gt;",
                        ">"
                )
                .replace(
                        "&nbsp;",
                        " "
                );
    }

    private static boolean looksLikeCardName(
            String text
    ) {

        if (text == null) {
            return false;
        }

        text =
                text.trim();

        if (text.length() < 3 ||
                text.length() > 70) {
            return false;
        }

        if (text.matches(
                "^[0-9 .]+$"
        )) {
            return false;
        }

        if (text.contains(
                "HearthArena"
        )) {
            return false;
        }

        if (text.contains(
                "Cards in green"
        )) {
            return false;
        }

        if (text.contains(
                "Cards in red"
        )) {
            return false;
        }

        if (text.contains(
                "Tierlist"
        )) {
            return false;
        }

        if (text.contains(
                "Search for a card"
        )) {
            return false;
        }

        if (text.contains(
                "Common"
        )
                &&
                text.contains(
                        "Cards"
                )) {
            return false;
        }

        int letters = 0;

        for (int i = 0;
                i < text.length();
                i++) {

            if (Character.isLetter(
                    text.charAt(i)
            )) {
                letters++;
            }
        }

        return letters >= 2;
    }

    private static String cleanCardName(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                decodeEntities(text)
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        text =
                text.replaceAll(
                        "^[0-9]+\\.?\\s*",
                        ""
                );

        text =
                text.replaceAll(
                        "\\s*\\(.*?\\)\\s*$",
                        ""
                );

        return text.trim();
    }

    public static String correctOcr(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String cleaned =
                input.trim();

        cleaned =
                decodeEntities(
                        cleaned
                );

        cleaned =
                cleaned.replaceAll(
                        "\\s+",
                        " "
                ).trim();

        if (cleaned.isEmpty()) {
            return "";
        }

        /*
         * Tunnetut OCR-erikoiskorjaukset.
         */
        String soldier =
                fixSoldierOfInfinite(
                        cleaned
                );

        if (!soldier.equals(cleaned)) {
            return soldier;
        }

        /*
         * Normaali alias.
         */
        String normalized =
                normalize(cleaned);

        String alias =
                OCR_ALIASES.get(
                        normalized
                );

        if (alias != null) {
            return alias;
        }

        /*
         * Täsmälleen tunnettu nimi.
         */
        CardData exact =
                CARDS.get(
                        normalized
                );

        if (exact != null) {
            return exact.name;
        }

        /*
         * Fuzzy matching.
         */
        String bestName = "";
        int bestDistance =
                Integer.MAX_VALUE;

        synchronized (CARDS) {

            for (CardData card :
                    CARDS.values()) {

                if (card == null ||
                        card.name == null) {
                    continue;
                }

                String cardNormalized =
                        normalize(
                                card.name
                        );

                if (cardNormalized.isEmpty()) {
                    continue;
                }

                /*
                 * Jos toinen on toisen sisällä,
                 * hyväksytään se.
                 */
                if (cardNormalized.contains(
                        normalized
                )
                        ||
                        normalized.contains(
                                cardNormalized
                        )) {

                    int difference =
                            Math.abs(
                                    cardNormalized.length()
                                            -
                                    normalized.length()
                            );

                    if (difference <= 3) {

                        return card.name;
                    }
                }

                int distance =
                        levenshtein(
                                normalized,
                                cardNormalized
                        );

                if (distance <
                        bestDistance) {

                    bestDistance =
                            distance;

                    bestName =
                            card.name;
                }
            }
        }

        if (!bestName.isEmpty()) {

            int maxLength =
                    Math.max(
                            normalized.length(),
                            normalize(
                                    bestName
                            ).length()
                    );

            /*
             * Lyhyille nimille tiukempi raja.
             * Pitkille nimille hieman sallivampi.
             */
            int allowed;

            if (maxLength <= 8) {

                allowed = 1;

            } else if (maxLength <= 15) {

                allowed = 2;

            } else {

                allowed =
                        Math.max(
                                2,
                                maxLength / 5
                        );
            }

            if (bestDistance <= allowed) {
                return bestName;
            }
        }

        /*
         * Ei tunnistettu.
         *
         * Palautetaan alkuperäinen teksti,
         * mutta score() antaa tälle 0.0.
         */
        return cleaned;
    }

    private static String fixSoldierOfInfinite(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String normalized =
                normalize(text);

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
                )
                ||
                normalized.contains(
                        "soldieroftheinfinite"
                )) {

            return "Soldier of the Infinite";
        }

        return text;
    }

    public static String getCardScore(
            String cardName
    ) {

        return formatScore(
                score(cardName)
        );
    }

    public static double score(
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

        String key =
                normalize(corrected);

        CardData card =
                CARDS.get(key);

        /*
         * Jos korttia ei tunnistettu,
         * palautetaan 0.0.
         */
        if (card == null) {

            return UNKNOWN_CARD_SCORE;
        }

        double value =
                card.baseScore;

        if (card.removal) {
            value += 0.20;
        }

        if (card.aoe) {
            value += 0.30;
        }

        if (card.draw) {
            value += 0.15;
        }

        if (card.discover) {
            value += 0.20;
        }

        if (card.taunt) {
            value += 0.10;
        }

        if (card.divineShield) {
            value += 0.15;
        }

        if (card.battlecry) {
            value += 0.10;
        }

        if (card.deathrattle) {
            value += 0.10;
        }

        value += synergyBonus(
                card
        );

        return clamp(
                value,
                0.0,
                10.0
        );
    }

    private static double synergyBonus(
            CardData card
    ) {

        if (card == null) {
            return 0.0;
        }

        int copies =
                PICKED_CARDS.getOrDefault(
                        normalize(card.name),
                        0
                );

        if (copies <= 0) {
            return 0.0;
        }

        if (copies == 1) {
            return 0.05;
        }

        return 0.10;
    }

    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        String bestCard = "";
        double bestScore = -1.0;

        String[] cards = {
                card1,
                card2,
                card3
        };

        for (String card :
                cards) {

            if (card == null ||
                    card.trim().isEmpty()) {
                continue;
            }

            double value =
                    score(card);

            if (value > bestScore) {

                bestScore =
                        value;

                bestCard =
                        correctOcr(card);
            }
        }

        if (bestCard.isEmpty()) {
            return "Odotetaan kortteja...";
        }

        boolean anyKnown =
                score(card1) > 0.0
                        ||
                score(card2) > 0.0
                        ||
                score(card3) > 0.0;

        if (!anyKnown) {
            return "Kortteja ei tunnistettu";
        }

        return bestCard +
                " (" +
                formatScore(bestScore) +
                ")";
    }

    public static String getReason(
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

        CardData card =
                CARDS.get(
                        normalize(corrected)
                );

        if (card == null) {

            return "Korttia ei tunnistettu.";
        }

        ArrayList<String> reasons =
                new ArrayList<>();

        if (card.removal) {
            reasons.add("poisto");
        }

        if (card.aoe) {
            reasons.add("AOE");
        }

        if (card.draw) {
            reasons.add("korttien nosto");
        }

        if (card.discover) {
            reasons.add("Discover");
        }

        if (card.taunt) {
            reasons.add("Taunt");
        }

        if (card.divineShield) {
            reasons.add("Divine Shield");
        }

        if (card.battlecry) {
            reasons.add("Battlecry");
        }

        if (card.deathrattle) {
            reasons.add("Deathrattle");
        }

        if (reasons.isEmpty()) {

            return "HearthArena-arvo: " +
                    formatScore(
                            card.baseScore
                    );
        }

        return String.join(
                ", ",
                reasons
        );
    }

    public static boolean isValidCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().isEmpty()) {

            return false;
        }

        String normalized =
                normalize(
                        cardName
                );

        if (normalized.length() < 3) {
            return false;
        }

        if (CARDS.containsKey(
                normalized
        )) {
            return true;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (corrected == null ||
                corrected.trim().isEmpty()) {
            return false;
        }

        return CARDS.containsKey(
                normalize(corrected)
        );
    }

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

        String key =
                normalize(corrected);

        if (key.isEmpty()) {
            return;
        }

        int count =
                PICKED_CARDS.getOrDefault(
                        key,
                        0
                );

        PICKED_CARDS.put(
                key,
                count + 1
        );
    }

    public static void clearPickedCards() {

        PICKED_CARDS.clear();
    }

    public static int getPickedCount(
            String cardName
    ) {

        if (cardName == null) {
            return 0;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        return PICKED_CARDS.getOrDefault(
                normalize(corrected),
                0
        );
    }

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(
                        Locale.US
                )
                .replace(
                        "&#039;",
                        "'"
                )
                .replace(
                        "&#39;",
                        "'"
                )
                .replace(
                        "&apos;",
                        "'"
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    private static int levenshtein(
            String a,
            String b
    ) {

        if (a == null) {
            a = "";
        }

        if (b == null) {
            b = "";
        }

        int[][] dp =
                new int[
                        a.length() + 1
                ][
                        b.length() + 1
                ];

        for (int i = 0;
                i <= a.length();
                i++) {

            dp[i][0] = i;
        }

        for (int j = 0;
                j <= b.length();
                j++) {

            dp[0][j] = j;
        }

        for (int i = 1;
                i <= a.length();
                i++) {

            for (int j = 1;
                    j <= b.length();
                    j++) {

                int cost =
                        a.charAt(i - 1)
                                ==
                        b.charAt(j - 1)
                                ? 0
                                : 1;

                dp[i][j] =
                        Math.min(
                                Math.min(
                                        dp[i - 1][j] + 1,
                                        dp[i][j - 1] + 1
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

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    public static String formatScore(
            double score
    ) {

        return String.format(
                Locale.US,
                "%.1f",
                score
        );
    }

    public static String getCardScoreText(
            String cardName
    ) {

        return getCardScore(
                cardName
        );
    }

    public static boolean isOnlineLoaded() {

        return onlineLoaded;
    }
}
