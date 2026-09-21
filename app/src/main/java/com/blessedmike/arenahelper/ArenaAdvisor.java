package com.blessedmike.arenahelper;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaAdvisor {

    // ============================================================
    // ONLINE ARENA DATA
    // ============================================================

    private static final String HEARTHARENA_URL =
            "https://www.heartharena.com/tierlist";

    private static final ExecutorService NETWORK_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static volatile boolean onlineDataLoaded = false;
    private static volatile boolean onlineDataLoading = false;

    private static final double HEARTHARENA_SCALE = 13.0;

    private static final double UNKNOWN_CARD_SCORE = 5.0;

    private static final Map<String, Double> ONLINE_SCORES =
            new HashMap<>();

    private static final Map<String, String> ONLINE_NAMES =
            new HashMap<>();

    // ============================================================
    // CARD DATA
    // ============================================================

    public static class CardData {

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

        boolean mech;
        boolean beast;
        boolean dragon;
        boolean undead;
        boolean elemental;
        boolean demon;
        boolean murloc;
        boolean pirate;
        boolean naga;
        boolean quilboar;

        boolean nature;
        boolean frost;
        boolean fire;
        boolean shadow;
        boolean holy;
        boolean arcane;

        CardData(
                String name,
                double baseScore,
                int mana,
                int attack,
                int health,
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
            this.name = name;
            this.baseScore = baseScore;
            this.mana = mana;
            this.attack = attack;
            this.health = health;

            this.minion = minion;
            this.spell = spell;
            this.weapon = weapon;

            this.removal = removal;
            this.aoe = aoe;
            this.draw = draw;
            this.discover = discover;
            this.taunt = taunt;
            this.divineShield = divineShield;
            this.battlecry = battlecry;
            this.deathrattle = deathrattle;
        }
    }

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    // ============================================================
    // PICK HISTORY
    // ============================================================

    private static final ArrayList<String> pickedCards =
            new ArrayList<>();

    private static int minionCount = 0;
    private static int spellCount = 0;
    private static int weaponCount = 0;

    private static int removalCount = 0;
    private static int aoeCount = 0;
    private static int drawCount = 0;
    private static int discoverCount = 0;
    private static int tauntCount = 0;
    private static int divineShieldCount = 0;
    private static int battlecryCount = 0;
    private static int deathrattleCount = 0;

    private static int mechCount = 0;
    private static int beastCount = 0;
    private static int dragonCount = 0;
    private static int undeadCount = 0;
    private static int elementalCount = 0;
    private static int demonCount = 0;
    private static int murlocCount = 0;
    private static int pirateCount = 0;
    private static int nagaCount = 0;
    private static int quilboarCount = 0;

    private static int natureCount = 0;
    private static int frostCount = 0;
    private static int fireCount = 0;
    private static int shadowCount = 0;
    private static int holyCount = 0;
    private static int arcaneCount = 0;

    // ============================================================
    // START ONLINE LOADING
    // ============================================================

    static {
        loadFallbackCards();
        refreshOnlineTierlist();
    }

    public static void refreshOnlineTierlist() {

        if (onlineDataLoading) {
            return;
        }

        onlineDataLoading = true;

        NETWORK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection connection = null;

                try {

                    URL url = new URL(HEARTHARENA_URL);

                    connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(10000);

                    connection.setRequestProperty(
                            "User-Agent",
                            "ArenaHelper/1.0 Android"
                    );

                    int responseCode =
                            connection.getResponseCode();

                    if (responseCode >= 200 &&
                            responseCode < 300) {

                        InputStream input =
                                connection.getInputStream();

                        String html =
                                readStream(input);

                        parseHearthArena(html);

                        onlineDataLoaded =
                                !ONLINE_SCORES.isEmpty();
                    }

                } catch (Exception ignored) {

                } finally {

                    onlineDataLoading = false;

                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    // ============================================================
    // READ STREAM
    // ============================================================

    private static String readStream(InputStream input)
            throws Exception {

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(input, "UTF-8")
                );

        StringBuilder result =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {

            result.append(line);
            result.append("\n");
        }

        reader.close();

        return result.toString();
    }

    // ============================================================
    // PARSE HEARTHARENA
    // ============================================================

    private static void parseHearthArena(String html) {

        if (html == null || html.length() == 0) {
            return;
        }

        try {

            String text = html;

            text = text.replaceAll(
                    "(?i)<br\\s*/?>",
                    "\n"
            );

            text = text.replaceAll(
                    "(?i)</p>",
                    "\n"
            );

            text = text.replaceAll(
                    "(?i)</div>",
                    "\n"
            );

            text = text.replaceAll(
                    "(?i)</li>",
                    "\n"
            );

            text = text.replaceAll(
                    "(?i)</h[1-6]>",
                    "\n"
            );

            text = text.replaceAll(
                    "<[^>]+>",
                    " "
            );

            text = decodeHtml(text);

            text = text.replace("\r", "\n");

            String[] lines =
                    text.split("\n");

            String previousName = null;

            for (int i = 0; i < lines.length; i++) {

                String line =
                        cleanWebLine(lines[i]);

                if (line.length() == 0) {
                    continue;
                }

                Integer score =
                        extractScore(line);

                if (score != null) {

                    if (previousName != null) {

                        addOnlineScore(
                                previousName,
                                score
                        );

                        previousName = null;
                    }

                    continue;
                }

                String possibleName =
                        cleanCardNameForLookup(line);

                if (isPossibleCardName(possibleName)) {

                    previousName =
                            possibleName;
                }
            }

            parseInlinePatterns(html);

        } catch (Exception ignored) {
        }
    }

    private static void parseInlinePatterns(String html) {

        try {

            Pattern pattern =
                    Pattern.compile(
                            ">\\s*([^<>\\r\\n]{3,80})\\s*<"
                                    + "[^>]*>"
                                    + "(?:[^<>]*<[^>]+>){0,8}"
                                    + "\\s*(\\d{1,3})\\s*<",
                            Pattern.CASE_INSENSITIVE
                    );

            Matcher matcher =
                    pattern.matcher(html);

            while (matcher.find()) {

                String name =
                        cleanCardNameForLookup(
                                decodeHtml(
                                        matcher.group(1)
                                )
                        );

                int score;

                try {
                    score =
                            Integer.parseInt(
                                    matcher.group(2)
                            );
                } catch (Exception e) {
                    continue;
                }

                if (isPossibleCardName(name)
                        && score >= 0
                        && score <= 200) {

                    addOnlineScore(name, score);
                }
            }

        } catch (Exception ignored) {
        }
    }

    private static Integer extractScore(String line) {

        String cleaned =
                line.replace("↓", "")
                        .trim();

        if (cleaned.matches("\\d{1,3}")) {

            try {

                int value =
                        Integer.parseInt(cleaned);

                if (value >= 0 && value <= 200) {
                    return value;
                }

            } catch (Exception ignored) {
            }
        }

        Matcher matcher =
                Pattern.compile(
                        "^\\s*(\\d{1,3})\\s*$"
                ).matcher(cleaned);

        if (matcher.find()) {

            try {

                int value =
                        Integer.parseInt(
                                matcher.group(1)
                        );

                if (value >= 0 && value <= 200) {
                    return value;
                }

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static void addOnlineScore(
            String name,
            int hearthArenaScore
    ) {

        if (name == null) {
            return;
        }

        name =
                cleanCardNameForLookup(name);

        if (!isPossibleCardName(name)) {
            return;
        }

        if (hearthArenaScore < 0 ||
                hearthArenaScore > 200) {
            return;
        }

        double converted =
                hearthArenaScore /
                        HEARTHARENA_SCALE;

        converted =
                clamp(
                        converted,
                        0.0,
                        10.0
                );

        String key =
                normalize(name);

        ONLINE_SCORES.put(
                key,
                converted
        );

        ONLINE_NAMES.put(
                key,
                name
        );
    }

    // ============================================================
    // HTML DECODING
    // ============================================================

    private static String decodeHtml(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ");
    }

    // ============================================================
    // CARD LOOKUP
    // ============================================================

    private static CardData findCard(String cardName) {

        if (cardName == null) {
            return null;
        }

        String corrected =
                correctOcr(cardName);

        String key =
                normalize(corrected);

        CardData data =
                CARDS.get(key);

        if (data != null) {
            return data;
        }

        Double onlineScore =
                ONLINE_SCORES.get(key);

        if (onlineScore != null) {

            CardData generated =
                    new CardData(
                            ONLINE_NAMES.containsKey(key)
                                    ? ONLINE_NAMES.get(key)
                                    : corrected,
                            onlineScore,
                            0,
                            0,
                            0,
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

            CARDS.put(
                    key,
                    generated
            );

            return generated;
        }

        String fuzzy =
                findClosestName(corrected);

        if (fuzzy != null) {

            CardData fuzzyData =
                    CARDS.get(
                            normalize(fuzzy)
                    );

            if (fuzzyData != null) {
                return fuzzyData;
            }

            Double fuzzyScore =
                    ONLINE_SCORES.get(
                            normalize(fuzzy)
                    );

            if (fuzzyScore != null) {

                CardData generated =
                        new CardData(
                                fuzzy,
                                fuzzyScore,
                                0,
                                0,
                                0,
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

                CARDS.put(
                        normalize(fuzzy),
                        generated
                );

                return generated;
            }
        }

        return null;
    }

    private static String findClosestName(
            String input
    ) {

        if (input == null ||
                input.length() < 4) {
            return null;
        }

        String normalizedInput =
                normalize(input);

        String bestName = null;

        int bestDistance =
                Integer.MAX_VALUE;

        for (String name : CARDS.keySet()) {

            CardData data =
                    CARDS.get(name);

            if (data == null) {
                continue;
            }

            int distance =
                    levenshtein(
                            normalizedInput,
                            name
                    );

            int allowed =
                    Math.max(
                            2,
                            normalizedInput.length() / 5
                    );

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance = distance;
                bestName = data.name;
            }
        }

        for (Map.Entry<String, String> entry :
                ONLINE_NAMES.entrySet()) {

            String key = entry.getKey();

            int distance =
                    levenshtein(
                            normalizedInput,
                            key
                    );

            int allowed =
                    Math.max(
                            2,
                            normalizedInput.length() / 5
                    );

            if (distance <= allowed &&
                    distance < bestDistance) {

                bestDistance = distance;
                bestName = entry.getValue();
            }
        }

        return bestName;
    }

    // ============================================================
    // OCR CORRECTION
    // ============================================================

    public static String correctOcr(
            String cardName
    ) {

        if (cardName == null) {
            return "";
        }

        String cleaned =
                cleanCardNameForLookup(cardName);

        String normalized =
                normalize(cleaned);

        if (normalized.contains(
                "soldieroftheinfinite"
        ) ||
                normalized.contains(
                        "so1dieroftheinfinite"
                ) ||
                normalized.contains(
                        "sodieroftheinfinite"
                ) ||
                normalized.contains(
                        "soldierofihfinite"
                ) ||
                normalized.contains(
                        "soldierofinfinite"
                ) ||
                normalized.contains(
                        "soldieroftheinfinit"
                )) {

            return "Soldier of the Infinite";
        }

        if (normalized.equals(
                "scrappyscavenger"
        ) ||
                normalized.equals(
                        "scrappyscavenger"
                )) {

            return "Scrappy Scavenger";
        }

        if (normalized.equals(
                "tricksyimproviser"
        ) ||
                normalized.equals(
                        "tricksyimproviser"
                )) {

            return "Tricksy Improviser";
        }

        if (normalized.equals(
                "arrivalofthetitans"
        ) ||
                normalized.equals(
                        "arrivalofthetitan"
                )) {

            return "Arrival of the Titans";
        }

        for (CardData data : CARDS.values()) {

            if (normalize(data.name)
                    .equals(normalized)) {

                return data.name;
            }
        }

        String online =
                ONLINE_NAMES.get(normalized);

        if (online != null) {
            return online;
        }

        String closest =
                findClosestName(cleaned);

        if (closest != null) {
            return closest;
        }

        return cleaned;
    }

    // ============================================================
    // SCORE
    // ============================================================

    public static String getCardScore(
            String cardName
    ) {

        double score =
                score(cardName);

        return String.format(
                Locale.US,
                "%.1f",
                score
        );
    }

    public static String getCardScore(
            double value
    ) {

        return String.format(
                Locale.US,
                "%.1f",
                clamp(
                        value,
                        0.0,
                        10.0
                )
        );
    }

    public static double score(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().length() == 0) {

            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(cardName);

        CardData data =
                findCard(corrected);

        double base;

        if (data != null) {

            base =
                    data.baseScore;

        } else {

            Double online =
                    ONLINE_SCORES.get(
                            normalize(corrected)
                    );

            if (online != null) {
                base = online;
            } else {
                base = UNKNOWN_CARD_SCORE;
            }
        }

        double result =
                base;

        if (data != null) {

            if (data.removal) {
                result += 0.10;
            }

            if (data.aoe) {
                result += 0.15;
            }

            if (data.draw) {
                result += 0.08;
            }

            if (data.discover) {
                result += 0.10;
            }

            if (data.taunt) {
                result += 0.05;
            }

            if (data.divineShield) {
                result += 0.08;
            }

            if (data.battlecry) {
                result += 0.05;
            }

            if (data.deathrattle) {
                result += 0.05;
            }

            result += synergyScore(data);
        }

        return clamp(
                result,
                0.0,
                10.0
        );
    }

    // ============================================================
    // SYNERGY
    // ============================================================

    private static double synergyScore(
            CardData card
    ) {

        if (card == null) {
            return 0.0;
        }

        double bonus = 0.0;

        if (card.minion) {

            if (minionCount < 10) {
                bonus += 0.15;
            } else if (minionCount > 20) {
                bonus -= 0.15;
            }
        }

        if (card.spell) {

            if (spellCount < 5) {
                bonus += 0.12;
            } else if (spellCount > 12) {
                bonus -= 0.15;
            }
        }

        if (card.removal) {

            if (removalCount < 4) {
                bonus += 0.15;
            } else if (removalCount >= 7) {
                bonus -= 0.08;
            }
        }

        if (card.aoe) {

            if (aoeCount < 2) {
                bonus += 0.18;
            } else if (aoeCount >= 4) {
                bonus -= 0.10;
            }
        }

        if (card.draw) {

            if (drawCount < 4) {
                bonus += 0.12;
            }
        }

        if (card.discover) {

            if (discoverCount < 4) {
                bonus += 0.12;
            }
        }

        if (card.taunt) {

            if (tauntCount < 4) {
                bonus += 0.06;
            }
        }

        if (card.divineShield) {

            if (divineShieldCount < 3) {
                bonus += 0.06;
            }
        }

        if (card.battlecry) {

            if (battlecryCount < 8) {
                bonus += 0.05;
            }
        }

        if (card.deathrattle) {

            if (deathrattleCount < 5) {
                bonus += 0.05;
            }
        }

        if (card.mech && mechCount > 0) {
            bonus += 0.20;
        }

        if (card.beast && beastCount > 0) {
            bonus += 0.20;
        }

        if (card.dragon && dragonCount > 0) {
            bonus += 0.20;
        }

        if (card.undead && undeadCount > 0) {
            bonus += 0.20;
        }

        if (card.elemental && elementalCount > 0) {
            bonus += 0.20;
        }

        if (card.demon && demonCount > 0) {
            bonus += 0.20;
        }

        if (card.murloc && murlocCount > 0) {
            bonus += 0.20;
        }

        if (card.pirate && pirateCount > 0) {
            bonus += 0.20;
        }

        if (card.naga && nagaCount > 0) {
            bonus += 0.20;
        }

        if (card.quilboar && quilboarCount > 0) {
            bonus += 0.20;
        }

        if (card.nature && natureCount > 0) {
            bonus += 0.12;
        }

        if (card.frost && frostCount > 0) {
            bonus += 0.15;
        }

        if (card.fire && fireCount > 0) {
            bonus += 0.15;
        }

        if (card.shadow && shadowCount > 0) {
            bonus += 0.12;
        }

        if (card.holy && holyCount > 0) {
            bonus += 0.12;
        }

        if (card.arcane && arcaneCount > 0) {
            bonus += 0.15;
        }

        String name =
                normalize(card.name);

        if (name.equals(
                normalize("Soldier of the Infinite")
        )) {

            if (arcaneCount > 0) {
                bonus += 0.20;
            }
        }

        if (name.equals(
                normalize("Contraband Wands")
        )) {

            if (spellCount >= 5) {
                bonus += 0.20;
            }
        }

        if (name.equals(
                normalize("Bursting Leyline")
        ) ||
                name.equals(
                        normalize("Crystallized Leyline")
                ) ||
                name.equals(
                        normalize("Leyline Nexus")
                )) {

            if (spellCount >= 5) {
                bonus += 0.20;
            }
        }

        if (name.equals(
                normalize("Surge Needle")
        )) {

            if (spellCount >= 4) {
                bonus += 0.15;
            }
        }

        if (name.equals(
                normalize("Cold Snap")
        )) {

            if (frostCount >= 2) {
                bonus += 0.20;
            }
        }

        if (name.equals(
                normalize("Code Violet")
        ) ||
                name.equals(
                        normalize("Void Blast")
                )) {

            if (spellCount >= 6) {
                bonus += 0.15;
            }
        }

        if (name.equals(
                normalize("Shadowed Informant")
        ) ||
                name.equals(
                        normalize("Dig for Freedom")
                )) {

            if (drawCount >= 2) {
                bonus += 0.15;
            }
        }

        if (name.equals(
                normalize("Guard Dog")
        ) ||
                name.equals(
                        normalize("Commander Beatrix")
                ) ||
                name.equals(
                        normalize("Jade Guardians")
                )) {

            if (tauntCount >= 2) {
                bonus += 0.15;
            }
        }

        if (name.equals(
                normalize("Emergency Surgery")
        )) {

            if (deathrattleCount >= 2) {
                bonus += 0.20;
            }
        }

        if (name.equals(
                normalize("Corpse Cannon")
        )) {

            if (deathrattleCount >= 3) {
                bonus += 0.25;
            }
        }

        if (name.equals(
                normalize("Underbelly Network")
        )) {

            if (pirateCount >= 2) {
                bonus += 0.20;
            }
        }

        if (name.equals(
                normalize("Jailhouse Manastorm")
        )) {

            if (spellCount >= 8) {
                bonus += 0.20;
            }
        }

        return clamp(
                bonus,
                -0.50,
                1.00
        );
    }

    // ============================================================
    // RECOMMENDATION
    // ============================================================

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

        int best = 1;

        double bestScore =
                score1;

        if (score2 > bestScore) {
            best = 2;
            bestScore = score2;
        }

        if (score3 > bestScore) {
            best = 3;
            bestScore = score3;
        }

        return "SUOSITUS: Kortti " + best;
    }

    public static String recommend(
            int score1,
            int score2,
            int score3
    ) {

        int best = 1;

        if (score2 > score1 &&
                score2 >= score3) {

            best = 2;

        } else if (score3 > score1 &&
                score3 > score2) {

            best = 3;
        }

        return "SUOSITUS: Kortti " + best;
    }

    public static String recommend(
            double score1,
            double score2,
            double score3
    ) {

        int best = 1;

        double bestScore =
                score1;

        if (score2 > bestScore) {
            best = 2;
            bestScore = score2;
        }

        if (score3 > bestScore) {
            best = 3;
        }

        return "SUOSITUS: Kortti " + best;
    }

    // ============================================================
    // PICK TRACKING
    // ============================================================

    public static void recordPickedCard(
            String cardName
    ) {

        if (cardName == null ||
                cardName.trim().length() == 0) {
            return;
        }

        String corrected =
                correctOcr(cardName);

        CardData data =
                findCard(corrected);

        if (data == null) {

            pickedCards.add(corrected);
            return;
        }

        pickedCards.add(
                data.name
        );

        if (data.minion) {
            minionCount++;
        }

        if (data.spell) {
            spellCount++;
        }

        if (data.weapon) {
            weaponCount++;
        }

        if (data.removal) {
            removalCount++;
        }

        if (data.aoe) {
            aoeCount++;
        }

        if (data.draw) {
            drawCount++;
        }

        if (data.discover) {
            discoverCount++;
        }

        if (data.taunt) {
            tauntCount++;
        }

        if (data.divineShield) {
            divineShieldCount++;
        }

        if (data.battlecry) {
            battlecryCount++;
        }

        if (data.deathrattle) {
            deathrattleCount++;
        }

        if (data.mech) {
            mechCount++;
        }

        if (data.beast) {
            beastCount++;
        }

        if (data.dragon) {
            dragonCount++;
        }

        if (data.undead) {
            undeadCount++;
        }

        if (data.elemental) {
            elementalCount++;
        }

        if (data.demon) {
            demonCount++;
        }

        if (data.murloc) {
            murlocCount++;
        }

        if (data.pirate) {
            pirateCount++;
        }

        if (data.naga) {
            nagaCount++;
        }

        if (data.quilboar) {
            quilboarCount++;
        }

        if (data.nature) {
            natureCount++;
        }

        if (data.frost) {
            frostCount++;
        }

        if (data.fire) {
            fireCount++;
        }

        if (data.shadow) {
            shadowCount++;
        }

        if (data.holy) {
            holyCount++;
        }

        if (data.arcane) {
            arcaneCount++;
        }
    }

    // ============================================================
    // RESET
    // ============================================================

    public static void resetArena() {

        pickedCards.clear();

        minionCount = 0;
        spellCount = 0;
        weaponCount = 0;

        removalCount = 0;
        aoeCount = 0;
        drawCount = 0;
        discoverCount = 0;
        tauntCount = 0;
        divineShieldCount = 0;
        battlecryCount = 0;
        deathrattleCount = 0;

        mechCount = 0;
        beastCount = 0;
        dragonCount = 0;
        undeadCount = 0;
        elementalCount = 0;
        demonCount = 0;
        murlocCount = 0;
        pirateCount = 0;
        nagaCount = 0;
        quilboarCount = 0;

        natureCount = 0;
        frostCount = 0;
        fireCount = 0;
        shadowCount = 0;
        holyCount = 0;
        arcaneCount = 0;
    }

    // ============================================================
    // INFORMATION
    // ============================================================

    public static int getPickedCardCount() {
        return pickedCards.size();
    }

    public static ArrayList<String> getPickedCards() {
        return new ArrayList<>(
                pickedCards
        );
    }

    public static String getDeckSummary() {

        return
                "Kortteja: " + pickedCards.size()
                        + "\nMinioneja: " + minionCount
                        + "\nLoitsuja: " + spellCount
                        + "\nAseita: " + weaponCount
                        + "\nPoistoja: " + removalCount
                        + "\nAoE: " + aoeCount
                        + "\nNostoa: " + drawCount
                        + "\nDiscover: " + discoverCount
                        + "\nTaunt: " + tauntCount
                        + "\nBattlecry: " + battlecryCount
                        + "\nDeathrattle: " + deathrattleCount;
    }

    public static String getReason(
            String cardName
    ) {

        String corrected =
                correctOcr(cardName);

        CardData data =
                findCard(corrected);

        double value =
                score(corrected);

        if (data == null) {

            return String.format(
                    Locale.US,
                    "Tuntematon kortti – varapiste %.1f/10",
                    value
            );
        }

        StringBuilder reason =
                new StringBuilder();

        reason.append(
                String.format(
                        Locale.US,
                        "Arvo %.1f/10",
                        value
                )
        );

        if (data.removal) {
            reason.append(" • poisto");
        }

        if (data.aoe) {
            reason.append(" • AoE");
        }

        if (data.draw) {
            reason.append(" • kortinnostoa");
        }

        if (data.discover) {
            reason.append(" • Discover");
        }

        if (data.taunt) {
            reason.append(" • Taunt");
        }

        if (data.battlecry) {
            reason.append(" • Battlecry");
        }

        if (data.deathrattle) {
            reason.append(" • Deathrattle");
        }

        if (onlineDataLoaded) {
            reason.append(" • Arena-data päivitetty");
        }

        return reason.toString();
    }

    // ============================================================
    // FALLBACK DATABASE
    // ============================================================

    private static void loadFallbackCards() {

        CARDS.clear();

        add(
                "Soldier of the Infinite",
                4.46,
                0,
                0,
                0,
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
                "Bursting Leyline",
                4.46,
                0,
                0,
                0,
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
                "Contraband Wands",
                5.00,
                0,
                0,
                0,
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
                "Crystallized Leyline",
                5.92,
                0,
                0,
                0,
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
                6.08,
                0,
                0,
                0,
                false,
                true,
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
                "Leyline Nexus",
                4.85,
                0,
                0,
                0,
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
                "Mystic Runesaber",
                5.92,
                0,
                0,
                0,
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
                "Ley Walker",
                6.85,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Cold Snap",
                5.77,
                0,
                0,
                0,
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
                5.54,
                0,
                0,
                0,
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
                "Tunneling Geomancer",
                4.85,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                true,
                false
        );

        add(
                "Watfin",
                6.15,
                0,
                0,
                0,
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
                "Shadowed Informant",
                7.23,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Hopeful Dryad",
                6.23,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Raptor Herald",
                6.92,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Carrier Whelp",
                6.92,
                0,
                0,
                0,
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
                true
        );

        add(
                "Violet Punisher",
                7.92,
                0,
                0,
                0,
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
                "Whelp of the Infinite",
                7.92,
                0,
                0,
                0,
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
                "Infested Breath",
                7.77,
                0,
                0,
                0,
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
                "Emergency Surgery",
                7.38,
                0,
                0,
                0,
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
                "Corpse Cannon",
                7.54,
                0,
                0,
                0,
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                true
        );

        add(
                "Underbelly Network",
                7.30,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Guard Dog",
                6.77,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                true,
                false
        );

        add(
                "Dig for Freedom",
                6.92,
                0,
                0,
                0,
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
                "Jailhouse Manastorm",
                10.0,
                0,
                0,
                0,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Frostbolt",
                7.50,
                2,
                0,
                0,
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
                "Fireball",
                8.00,
                4,
                0,
                0,
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
                "Scrappy Scavenger",
                7.77,
                0,
                0,
                0,
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
                "Tricksy Improviser",
                8.38,
                5,
                3,
                5,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        );

        add(
                "Arrival of the Titans",
                7.50,
                6,
                0,
                0,
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
    }

    // ============================================================
    // ADD FALLBACK CARD
    // ============================================================

    private static void add(
            String name,
            double score,
            int mana,
            int attack,
            int health,
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
                        score,
                        mana,
                        attack,
                        health,
                        minion,
                        spell,
                        weapon,
                        removal,
                        aoe,
                        draw,
                        discover,
                        taunt,
                        divineShield,
                        battlecry,
                        deathrattle
                );

        CARDS.put(
                normalize(name),
                data
        );
    }

    // ============================================================
    // TEXT CLEANING
    // ============================================================

    private static String cleanCardNameForLookup(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String value =
                decodeHtml(text)
                        .replace("\u00A0", " ")
                        .replace("↓", "")
                        .trim();

        value =
                value.replaceFirst(
                        "^\\s*\\d+\\.\\s*",
                        ""
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    private static String cleanWebLine(
            String line
    ) {

        if (line == null) {
            return "";
        }

        String result =
                decodeHtml(line);

        result =
                result.replace(
                        "\u00A0",
                        " "
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        return result.trim();
    }

    private static boolean isPossibleCardName(
            String name
    ) {

        if (name == null) {
            return false;
        }

        if (name.length() < 3 ||
                name.length() > 100) {
            return false;
        }

        String normalized =
                normalize(name);

        if (normalized.equals("great") ||
                normalized.equals("good") ||
                normalized.equals("aboveaverage") ||
                normalized.equals("average") ||
                normalized.equals("belowaverage") ||
                normalized.equals("bad") ||
                normalized.equals("terrible")) {

            return false;
        }

        if (name.equalsIgnoreCase(
                "Death Knight Cards"
        ) ||
                name.equalsIgnoreCase(
                        "Mage Cards"
                ) ||
                name.equalsIgnoreCase(
                        "Neutral Cards"
                )) {

            return false;
        }

        boolean hasLetter = false;

        for (int i = 0;
             i < name.length();
             i++) {

            if (Character.isLetter(
                    name.charAt(i)
            )) {

                hasLetter = true;
                break;
            }
        }

        return hasLetter;
    }

    // ============================================================
    // NORMALIZATION
    // ============================================================

    private static String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String lower =
                text.toLowerCase(
                        Locale.US
                );

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < lower.length();
             i++) {

            char c =
                    lower.charAt(i);

            if (Character.isLetterOrDigit(c)) {

                result.append(c);
            }
        }

        return result.toString();
    }

    // ============================================================
    // LEVENSHTEIN
    // ============================================================

    private static int levenshtein(
            String a,
            String b
    ) {

        if (a == null) {
            return b == null ? 0 : b.length();
        }

        if (b == null) {
            return a.length();
        }

        int[] previous =
                new int[b.length() + 1];

        int[] current =
                new int[b.length() + 1];

        for (int j = 0;
             j <= b.length();
             j++) {

            previous[j] = j;
        }

        for (int i = 1;
             i <= a.length();
             i++) {

            current[0] = i;

            for (int j = 1;
                 j <= b.length();
                 j++) {

                int cost =
                        a.charAt(i - 1)
                                == b.charAt(j - 1)
                                ? 0
                                : 1;

                current[j] =
                        Math.min(
                                Math.min(
                                        current[j - 1] + 1,
                                        previous[j] + 1
                                ),
                                previous[j - 1] + cost
                        );
            }

            int[] temp =
                    previous;

            previous =
                    current;

            current =
                    temp;
        }

        return previous[b.length()];
    }

    // ============================================================
    // CLAMP
    // ============================================================

    private static double clamp(
            double value,
            double min,
            double max
    ) {

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}
