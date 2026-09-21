package com.blessedmike.arenahelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ArenaAdvisor {

    private static final Map<String, CardData> CARDS = new HashMap<>();

    static {

        // ============================================================
        // CURRENT HEARTHARENA VALUES
        // HearthArena scale -> our 0-10 scale
        // Conversion: HearthArena value / 14.0
        // ============================================================

        // Across the Timeways / current Arena pool
        addCard("Soldier of the Infinite", 57.8);
        addCard("Whelp of the Infinite", 103.0);
        addCard("Violet Punisher", 103.0);
        addCard("Infested Breath", 101.0);

        // Restoration of Azeroth / Mage
        addCard("Bursting Leyline", 58.0);
        addCard("Mystic Runesaber", 77.0);
        addCard("Ley Walker", 89.0);
        addCard("Crystallized Leyline", 76.0);
        addCard("Surge Needle", 79.0);
        addCard("Leyline Nexus", 63.0);

        // Escape from the Violet Hold
        addCard("Cold Snap", 75.0);
        addCard("Code Violet", 72.0);
        addCard("Contraband Wands", 65.0);
        addCard("Jailhouse Manastorm", 130.0);

        // Neutral / other current cards
        addCard("Tunneling Geomancer", 76.0);
        addCard("Shadowed Informant", 100.0);
        addCard("Carrier Whelp", 90.0);
        addCard("Hopeful Dryad", 81.0);
        addCard("Raptor Herald", 86.0);

        // Death Knight
        addCard("Emergency Surgery", 80.0);
        addCard("Drink Blood", 73.0);
        addCard("Disguised Doctor", 57.0);
        addCard("Corpse Cannon", 96.0);

        // Demon Hunter
        addCard("Void Soul", 72.0);
        addCard("Void Blast", 80.0);
        addCard("Vicious Voidscale", 86.0);

        // Druid
        addCard("Infest the Scullery", 59.0);
        addCard("Widow's Bite", 75.0);

        // Hunter
        addCard("Guard Dog", 88.0);
        addCard("Dig for Freedom", 90.0);
        addCard("Underbelly Network", 95.0);

        // Paladin
        addCard("Vigilant Sentry", 24.0);
        addCard("Truth Seeker", 77.0);
        addCard("Judgment", 87.0);
        addCard("Holy Bola!", 56.0);
        addCard("Dalaran Champion", 54.0);
        addCard("Commander Beatrix", 55.0);

        // Priest
        addCard("Karov the Broken", 103.0);
        addCard("Undeath Sentence", 50.0);

        // Rogue
        addCard("Jade Guardians", 81.0);
        addCard("Inspector Murloc Holmes", 70.0);

        // Warrior
        addCard("Warptooth", 90.0);

        // ============================================================
        // OTHER CURRENTLY VERIFIED HIGH-VALUE CARDS
        // ============================================================

        addCard("Experimental Animation", 115.0);
        addCard("Obsessive Technician", 115.0);

        addCard("Zilliax Deluxe 3000", 80.0);
        addCard("Watfin", 85.0);

        // ============================================================
        // OCR ALIASES
        // ============================================================

        addAlias("soldieroftheinfinite", "Soldier of the Infinite");
        addAlias("soldieroftheinfinite", "Soldier of the Infinite");
        addAlias("soldierolf theinfinite", "Soldier of the Infinite");
        addAlias("soldierolf the infinite", "Soldier of the Infinite");
        addAlias("soldierolf the infinit", "Soldier of the Infinite");
        addAlias("sotdierofinfinite", "Soldier of the Infinite");
        addAlias("sotdierof theinfinite", "Soldier of the Infinite");
        addAlias("so1dieroftheinfinite", "Soldier of the Infinite");
        addAlias("so1dieroftheinfinite", "Soldier of the Infinite");
        addAlias("soldieroftheinfinit", "Soldier of the Infinite");
        addAlias("soldier of the infinite", "Soldier of the Infinite");

        addAlias("burstingleyline", "Bursting Leyline");
        addAlias("bursting leyline", "Bursting Leyline");
        addAlias("burstingleylinee", "Bursting Leyline");

        addAlias("contrabandwands", "Contraband Wands");
        addAlias("contraband wand", "Contraband Wands");
        addAlias("contrabandwands", "Contraband Wands");

        addAlias("crystallizedleyline", "Crystallized Leyline");
        addAlias("crystallisedleyline", "Crystallized Leyline");

        addAlias("surgen eedle", "Surge Needle");
        addAlias("surgeneedle", "Surge Needle");

        addAlias("leyline nexus", "Leyline Nexus");
        addAlias("leylinenexus", "Leyline Nexus");

        addAlias("mysticrunesaber", "Mystic Runesaber");
        addAlias("mystic runesaber", "Mystic Runesaber");

        addAlias("leywalker", "Ley Walker");
        addAlias("ley walker", "Ley Walker");

        addAlias("tunnelinggeomancer", "Tunneling Geomancer");
        addAlias("tunneling geomancer", "Tunneling Geomancer");

        addAlias("shadowedinformant", "Shadowed Informant");
        addAlias("shadowed informant", "Shadowed Informant");

        addAlias("carrierwhelp", "Carrier Whelp");
        addAlias("carrier whelp", "Carrier Whelp");

        addAlias("hopefuldryad", "Hopeful Dryad");
        addAlias("hopeful dryad", "Hopeful Dryad");

        addAlias("raptorherald", "Raptor Herald");
        addAlias("raptor herald", "Raptor Herald");

        addAlias("experimentalanimation", "Experimental Animation");
        addAlias("experimental animation", "Experimental Animation");

        addAlias("obsessivetechnician", "Obsessive Technician");
        addAlias("obsessive technician", "Obsessive Technician");

        addAlias("violetpunisher", "Violet Punisher");
        addAlias("violet punisher", "Violet Punisher");

        addAlias("whelp of the infinite", "Whelp of the Infinite");
        addAlias("whelpinfinite", "Whelp of the Infinite");
        addAlias("whelp of infinite", "Whelp of the Infinite");

        addAlias("infestedbreath", "Infested Breath");
        addAlias("infested breath", "Infested Breath");

        addAlias("emergencysurgery", "Emergency Surgery");
        addAlias("emergency surgery", "Emergency Surgery");

        addAlias("drinkblood", "Drink Blood");
        addAlias("drink blood", "Drink Blood");

        addAlias("disguiseddoctor", "Disguised Doctor");
        addAlias("disguised doctor", "Disguised Doctor");

        addAlias("corpsecannon", "Corpse Cannon");
        addAlias("corpse cannon", "Corpse Cannon");

        addAlias("voidsoul", "Void Soul");
        addAlias("void soul", "Void Soul");

        addAlias("voidblast", "Void Blast");
        addAlias("void blast", "Void Blast");

        addAlias("viciousvoidscale", "Vicious Voidscale");
        addAlias("vicious voidscale", "Vicious Voidscale");

        addAlias("widowsbite", "Widow's Bite");
        addAlias("widows bite", "Widow's Bite");
        addAlias("widow's bite", "Widow's Bite");

        addAlias("infestthescullery", "Infest the Scullery");
        addAlias("infest the scullery", "Infest the Scullery");

        addAlias("underbellynetwork", "Underbelly Network");
        addAlias("underbelly network", "Underbelly Network");

        addAlias("guarddog", "Guard Dog");
        addAlias("guard dog", "Guard Dog");

        addAlias("digforfreedom", "Dig for Freedom");
        addAlias("dig for freedom", "Dig for Freedom");

        addAlias("vigilantsentry", "Vigilant Sentry");
        addAlias("vigilant sentry", "Vigilant Sentry");

        addAlias("truthseeker", "Truth Seeker");
        addAlias("truth seeker", "Truth Seeker");

        addAlias("holybola", "Holy Bola!");
        addAlias("holy bola", "Holy Bola!");

        addAlias("dalaranchampion", "Dalaran Champion");
        addAlias("dalaran champion", "Dalaran Champion");

        addAlias("commanderbeatrix", "Commander Beatrix");
        addAlias("commander beatrix", "Commander Beatrix");

        addAlias("undeathsentence", "Undeath Sentence");
        addAlias("undeath sentence", "Undeath Sentence");

        addAlias("karovthebroken", "Karov the Broken");
        addAlias("karov the broken", "Karov the Broken");

        addAlias("jadeguardians", "Jade Guardians");
        addAlias("jade guardians", "Jade Guardians");

        addAlias("inspectormurloc holmes", "Inspector Murloc Holmes");
        addAlias("inspectormurloc holmes", "Inspector Murloc Holmes");
        addAlias("inspectormurloc", "Inspector Murloc Holmes");

        addAlias("warptooth", "Warptooth");
        addAlias("warptooth", "Warptooth");

        addAlias("jailhousemanastorm", "Jailhouse Manastorm");
        addAlias("jailhouse manastorm", "Jailhouse Manastorm");

        addAlias("zilliaxdeluxe3000", "Zilliax Deluxe 3000");
        addAlias("zilliax deluxe 3000", "Zilliax Deluxe 3000");

        addAlias("watfin", "Watfin");
    }

    // ================================================================
    // OCR CORRECTION
    // ================================================================

    public static String correctOcr(String input) {

        if (input == null) {
            return "";
        }

        String normalized = normalize(input);

        if (normalized.isEmpty()) {
            return "";
        }

        // Exact alias
        CardData aliasCard = CARDS.get(normalized);

        if (aliasCard != null) {
            return aliasCard.name;
        }

        // Soldier of the Infinite - OCR is especially unreliable here
        if (normalized.contains("soldier")
                && normalized.contains("infinite")) {
            return "Soldier of the Infinite";
        }

        if (normalized.contains("sotdier")
                || normalized.contains("so1dier")) {
            return "Soldier of the Infinite";
        }

        if (normalized.contains("soldierolf")
                && normalized.contains("infinit")) {
            return "Soldier of the Infinite";
        }

        // Whelp of the Infinite
        if (normalized.contains("whelp")
                && normalized.contains("infinite")) {
            return "Whelp of the Infinite";
        }

        // Burstingleyline
        if (normalized.contains("bursting")
                && normalized.contains("leyline")) {
            return "Bursting Leyline";
        }

        // Contraband Wands
        if (normalized.contains("contraband")
                && normalized.contains("wand")) {
            return "Contraband Wands";
        }

        // Crystallized Leyline
        if (normalized.contains("crystallized")
                && normalized.contains("leyline")) {
            return "Crystallized Leyline";
        }

        // Surge Needle
        if (normalized.contains("surge")
                && normalized.contains("needle")) {
            return "Surge Needle";
        }

        // Ley Walker
        if (normalized.contains("ley")
                && normalized.contains("walker")) {
            return "Ley Walker";
        }

        // Mystic Runesaber
        if (normalized.contains("mystic")
                && normalized.contains("runesaber")) {
            return "Mystic Runesaber";
        }

        return cleanCardName(input);
    }

    // ================================================================
    // SCORE
    // ================================================================

    public static double getCardScore(String cardName) {

        if (cardName == null || cardName.trim().isEmpty()) {
            return 0.0;
        }

        String corrected = correctOcr(cardName);
        String normalized = normalize(corrected);

        CardData card = CARDS.get(normalized);

        if (card == null) {
            return 0.0;
        }

        return card.baseScore;
    }

    // ================================================================
    // RECOMMENDATION
    // ================================================================

    public static int recommend(
            String card1,
            String card2,
            String card3) {

        double score1 = getCardScore(card1);
        double score2 = getCardScore(card2);
        double score3 = getCardScore(card3);

        if (score1 >= score2 && score1 >= score3) {
            return 1;
        }

        if (score2 >= score1 && score2 >= score3) {
            return 2;
        }

        return 3;
    }

    // ================================================================
    // REASON
    // ================================================================

    public static String getReason(String cardName) {

        if (cardName == null || cardName.trim().isEmpty()) {
            return "Korttia ei tunnistettu";
        }

        String corrected = correctOcr(cardName);
        double score = getCardScore(corrected);

        if (score <= 0.0) {
            return "Korttia ei tunnistettu";
        }

        return "Arena-arvo " + formatScore(score) + "/10";
    }

    // ================================================================
    // VALID CARD
    // ================================================================

    public static boolean isValidCard(String cardName) {

        if (cardName == null || cardName.trim().isEmpty()) {
            return false;
        }

        String corrected = correctOcr(cardName);
        return CARDS.containsKey(normalize(corrected));
    }

    // ================================================================
    // CLEAN OCR
    // ================================================================

    private static String cleanCardName(String text) {

        if (text == null) {
            return "";
        }

        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {

            String cleaned = line
                    .replaceAll("[^A-Za-z0-9'! ]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            int letters = 0;

            for (int i = 0; i < cleaned.length(); i++) {
                if (Character.isLetter(cleaned.charAt(i))) {
                    letters++;
                }
            }

            if (letters >= 2) {
                return cleaned;
            }
        }

        return text
                .replaceAll("[^A-Za-z0-9'! ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ================================================================
    // NORMALIZE
    // ================================================================

    private static String normalize(String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]", "");
    }

    // ================================================================
    // FORMAT
    // ================================================================

    public static String formatScore(double score) {

        return String.format(Locale.US, "%.1f", score);
    }

    // ================================================================
    // CARD DATA
    // ================================================================

    private static class CardData {

        String name;
        double baseScore;

        CardData(String name, double hearthArenaScore) {

            this.name = name;

            // HearthArena uses roughly a 0-140 scale.
            // Our application uses 0-10.
            this.baseScore = roundToOneDecimal(
                    hearthArenaScore / 14.0
            );

            if (this.baseScore < 0.0) {
                this.baseScore = 0.0;
            }

            if (this.baseScore > 10.0) {
                this.baseScore = 10.0;
            }
        }
    }

    // ================================================================
    // ADD CARD
    // ================================================================

    private static void addCard(
            String name,
            double hearthArenaScore) {

        CARDS.put(
                normalize(name),
                new CardData(
                        name,
                        hearthArenaScore
                )
        );
    }

    // ================================================================
    // ADD OCR ALIAS
    // ================================================================

    private static void addAlias(
            String alias,
            String realName) {

        CardData card = CARDS.get(normalize(realName));

        if (card != null) {
            CARDS.put(
                    normalize(alias),
                    card
            );
        }
    }

    // ================================================================
    // ROUND
    // ================================================================

    private static double roundToOneDecimal(double value) {

        return Math.round(value * 10.0) / 10.0;
    }
}
