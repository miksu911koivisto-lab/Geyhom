package com.blessedmike.arenahelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ArenaAdvisor {

    // ============================================================
    // CARD DATA
    // ============================================================

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

    private static final Map<String, CardData> CARDS = new HashMap<>();
    private static final Map<String, String> OCR_ALIASES = new HashMap<>();

    // ============================================================
    // ARENA DECK MEMORY
    // ============================================================

    private static final List<String> pickedCards = new ArrayList<>();

    /*
     * Synergy counters.
     *
     * These are calculated from cards that have actually been
     * selected into the Arena deck.
     */
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
    // CURRENT CARD DATABASE
    // ============================================================

    static {

        // --------------------------------------------------------
        // CURRENT / ARENA CARDS
        // --------------------------------------------------------

        addCard(new CardData(
                "Soldier of the Infinite",
                4.1, 3, 3, 3,
                true, false, false,
                false, false, false, false,
                true, false, false, false
        ));

        addCard(new CardData(
                "Bursting Leyline",
                4.1, 4, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Contraband Wands",
                4.6, 4, 0, 0,
                false, true, false,
                false, false, false, true,
                false, false, false, false
        ));

        addCard(new CardData(
                "Crystallized Leyline",
                5.4, 3, 0, 0,
                false, true, false,
                false, false, true, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Surge Needle",
                5.6, 3, 0, 0,
                false, false, true,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Leyline Nexus",
                4.5, 5, 0, 0,
                false, true, false,
                false, true, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Mystic Runesaber",
                5.5, 4, 3, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Ley Walker",
                0.0, 2, 2, 2,
                true, false, false,
                false, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Cold Snap",
                5.8, 2, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Code Violet",
                5.5, 3, 0, 0,
                false, true, false,
                false, true, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Tunneling Geomancer",
                5.4, 4, 4, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Watfin",
                6.5, 3, 3, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Zilliax Deluxe 3000",
                6.2, 5, 3, 5,
                true, false, false,
                false, false, false, false,
                true, true, false, false
        ));

        addCard(new CardData(
                "Shadowed Informant",
                7.0, 4, 4, 4,
                true, false, false,
                false, false, false, true,
                false, false, true, false
        ));

        addCard(new CardData(
                "Hopeful Dryad",
                6.3, 3, 3, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Raptor Herald",
                6.8, 4, 4, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Carrier Whelp",
                6.9, 4, 3, 4,
                true, false, false,
                false, false, false, false,
                false, false, false, true
        ));

        addCard(new CardData(
                "Experimental Animation",
                8.9, 4, 4, 5,
                true, false, false,
                false, false, false, false,
                false, false, false, true
        ));

        addCard(new CardData(
                "Obsessive Technician",
                8.9, 4, 4, 4,
                true, false, false,
                false, false, true, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Violet Punisher",
                7.9, 5, 5, 5,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Whelp of the Infinite",
                7.9, 4, 4, 4,
                true, false, false,
                false, false, false, false,
                false, false, false, true
        ));

        addCard(new CardData(
                "Infested Breath",
                7.8, 4, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Emergency Surgery",
                6.2, 3, 0, 0,
                false, true, false,
                false, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Drink Blood",
                5.6, 2, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Disguised Doctor",
                4.4, 4, 3, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Corpse Cannon",
                7.4, 6, 6, 6,
                true, false, false,
                false, true, false, false,
                false, false, false, true
        ));

        addCard(new CardData(
                "Void Soul",
                5.5, 3, 3, 3,
                true, false, false,
                false, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Void Blast",
                6.2, 4, 0, 0,
                false, true, false,
                true, true, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Vicious Voidscale",
                6.6, 3, 3, 3,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Widow's Bite",
                5.8, 3, 3, 2,
                false, false, true,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Infest the Scullery",
                4.5, 4, 0, 0,
                false, true, false,
                false, true, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Underbelly Network",
                7.3, 4, 4, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Guard Dog",
                6.8, 3, 3, 4,
                true, false, false,
                false, false, false, false,
                true, false, true, false
        ));

        addCard(new CardData(
                "Dig for Freedom",
                6.9, 4, 0, 0,
                false, true, false,
                false, false, false, true,
                false, false, false, false
        ));

        addCard(new CardData(
                "Vigilant Sentry",
                1.9, 2, 2, 2,
                true, false, false,
                false, false, false, false,
                true, false, false, false
        ));

        addCard(new CardData(
                "Truth Seeker",
                5.9, 4, 4, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Judgment",
                6.7, 4, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Holy Bola!",
                4.3, 3, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Dalaran Champion",
                4.2, 4, 4, 4,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Commander Beatrix",
                4.2, 5, 4, 5,
                true, false, false,
                false, false, false, false,
                true, false, true, false
        ));

        addCard(new CardData(
                "Undeath Sentence",
                3.9, 5, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Karov the Broken",
                7.4, 6, 6, 6,
                true, false, false,
                false, false, false, false,
                true, false, true, false
        ));

        addCard(new CardData(
                "Jade Guardians",
                5.8, 6, 5, 5,
                true, false, false,
                false, false, false, false,
                true, false, false, false
        ));

        addCard(new CardData(
                "Inspector Murloc Holmes",
                5.0, 5, 4, 5,
                true, false, false,
                false, false, false, true,
                false, false, true, false
        ));

        addCard(new CardData(
                "Jailhouse Manastorm",
                9.3, 7, 5, 5,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        addCard(new CardData(
                "Warptooth",
                6.9, 5, 5, 5,
                true, false, false,
                false, false, false, false,
                false, false, true, false
        ));

        // --------------------------------------------------------
        // OLD / KNOWN CARDS
        // --------------------------------------------------------

        addCard(new CardData(
                "Raban Wands",
                6.5, 4, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Frostbolt",
                7.5, 2, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        addCard(new CardData(
                "Fireball",
                8.0, 4, 0, 0,
                false, true, false,
                true, false, false, false,
                false, false, false, false
        ));

        // --------------------------------------------------------
        // OCR ALIASES
        // --------------------------------------------------------

        addAlias("soldiero", "Soldier of the Infinite");
        addAlias("soldier0", "Soldier of the Infinite");
        addAlias("soldier", "Soldier of the Infinite");
        addAlias("sotdier", "Soldier of the Infinite");
        addAlias("so1dier", "Soldier of the Infinite");
        addAlias("soldieroftheinfinite", "Soldier of the Infinite");
        addAlias("soldierolftheinfinite", "Soldier of the Infinite");
        addAlias("soldierolf theinfinite", "Soldier of the Infinite");
        addAlias("soldierolf the infinite", "Soldier of the Infinite");
        addAlias("soldieroftheinfinite", "Soldier of the Infinite");

        addAlias("burstingleyline", "Bursting Leyline");
        addAlias("crystallizedleyline", "Crystallized Leyline");
        addAlias("contrabandwands", "Contraband Wands");
        addAlias("surge needle", "Surge Needle");
        addAlias("surgen eedle", "Surge Needle");
        addAlias("ley linenexus", "Leyline Nexus");
        addAlias("leyline nexus", "Leyline Nexus");
        addAlias("mysticrunesaber", "Mystic Runesaber");
        addAlias("leywalker", "Ley Walker");
        addAlias("coldsnap", "Cold Snap");
        addAlias("codeviolet", "Code Violet");
        addAlias("tunnelinggeomancer", "Tunneling Geomancer");
        addAlias("watfin", "Watfin");
        addAlias("shadowedinformant", "Shadowed Informant");
        addAlias("hopefuldryad", "Hopeful Dryad");
        addAlias("raptorherald", "Raptor Herald");
        addAlias("carrierwhelp", "Carrier Whelp");
        addAlias("experimentalanimation", "Experimental Animation");
        addAlias("obsessivetechnician", "Obsessive Technician");
        addAlias("violetpunisher", "Violet Punisher");
        addAlias("whelp of the infinite", "Whelp of the Infinite");
        addAlias("whelp ofthe infinite", "Whelp of the Infinite");
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
        addAlias("judgment", "Judgment");
        addAlias("holybola", "Holy Bola!");
        addAlias("dalaranchampion", "Dalaran Champion");
        addAlias("commanderbeatrix", "Commander Beatrix");
        addAlias("undeathsentence", "Undeath Sentence");
        addAlias("karovthebroken", "Karov the Broken");
        addAlias("jadeguardians", "Jade Guardians");
        addAlias("inspectormurlocholmes", "Inspector Murloc Holmes");
        addAlias("jailhousemanastorm", "Jailhouse Manastorm");
        addAlias("warptooth", "Warptooth");
    }

    // ============================================================
    // DATABASE HELPERS
    // ============================================================

    private static void addCard(CardData card) {
        CARDS.put(normalize(card.name), card);
    }

    private static void addAlias(String alias, String cardName) {
        OCR_ALIASES.put(normalize(alias), cardName);
    }

    // ============================================================
    // OCR CORRECTION
    // ============================================================

    private static String correctOcr(String input) {

        if (input == null) {
            return "";
        }

        String original = input.trim();

        if (original.length() == 0) {
            return "";
        }

        String normalized = normalize(original);

        if (OCR_ALIASES.containsKey(normalized)) {
            return OCR_ALIASES.get(normalized);
        }

        // Soldier of the Infinite is particularly prone to OCR errors.
        if (normalized.equals("soldiero")
                || normalized.equals("soldier0")
                || normalized.equals("soldier")
                || normalized.startsWith("soldiero")
                || normalized.startsWith("soldier0")
                || normalized.startsWith("sotdier")
                || normalized.startsWith("so1dier")) {

            return "Soldier of the Infinite";
        }

        // Common OCR omissions.
        if (normalized.contains("soldier")
                && (normalized.contains("infinite")
                || normalized.contains("infinit")
                || normalized.contains("infinte"))) {

            return "Soldier of the Infinite";
        }

        return original;
    }

    // ============================================================
    // PUBLIC SCORE API
    // ============================================================

    public static String getCardScore(String cardName) {

        double value = score(cardName);

        return formatScore(value);
    }

    public static String getCardScore(double value) {

        return formatScore(clamp(value, 0.0, 10.0));
    }

    // ============================================================
    // NUMERIC SCORE
    // ============================================================

    private static double score(String cardName) {

        String corrected = correctOcr(cardName);

        if (corrected.length() == 0) {
            return 0.0;
        }

        CardData card = CARDS.get(normalize(corrected));

        if (card == null) {
            return 0.0;
        }

        double value = card.baseScore;

        /*
         * Small intrinsic quality bonuses.
         *
         * These are deliberately small because the database's base
         * score remains the most important component.
         */
        if (card.removal) {
            value += 0.10;
        }

        if (card.aoe) {
            value += 0.15;
        }

        if (card.draw) {
            value += 0.08;
        }

        if (card.discover) {
            value += 0.10;
        }

        if (card.taunt) {
            value += 0.05;
        }

        if (card.divineShield) {
            value += 0.08;
        }

        if (card.battlecry) {
            value += 0.05;
        }

        if (card.deathrattle) {
            value += 0.05;
        }

        /*
         * New deck-dependent synergy system.
         */
        value += synergyScore(card);

        /*
         * Keep synergy from completely overpowering the actual
         * Arena value.
         */
        value = clamp(value, 0.0, 10.0);

        return value;
    }

    // ============================================================
    // SYNERGY ENGINE
    // ============================================================

    private static double synergyScore(CardData card) {

        if (pickedCards.isEmpty()) {
            return 0.0;
        }

        double bonus = 0.0;

        // --------------------------------------------------------
        // GENERIC MINION / SPELL BALANCE
        // --------------------------------------------------------

        if (card.minion && spellCount >= 5) {
            bonus += 0.10;
        }

        if (card.spell && minionCount >= 5) {
            bonus += 0.10;
        }

        // --------------------------------------------------------
        // REMOVAL SYNERGY
        // --------------------------------------------------------

        if (card.removal && removalCount >= 2) {
            bonus += 0.10;
        }

        if (card.aoe && aoeCount == 0) {
            bonus += 0.25;
        }

        if (card.removal && aoeCount == 0 && minionCount >= 6) {
            bonus += 0.10;
        }

        // --------------------------------------------------------
        // DRAW SYNERGY
        // --------------------------------------------------------

        if (card.draw && drawCount >= 2) {
            bonus += 0.12;
        }

        /*
         * A deck with very little draw gets extra value from the
         * first draw cards.
         */
        if (card.draw && drawCount == 0 && pickedCards.size() >= 5) {
            bonus += 0.18;
        }

        // --------------------------------------------------------
        // DISCOVER
        // --------------------------------------------------------

        if (card.discover && discoverCount >= 1) {
            bonus += 0.12;
        }

        if (card.discover && spellCount >= 5) {
            bonus += 0.08;
        }

        // --------------------------------------------------------
        // TAUNT
        // --------------------------------------------------------

        if (card.taunt && tauntCount >= 2) {
            bonus += 0.10;
        }

        if (card.taunt && tauntCount == 0 && pickedCards.size() >= 7) {
            bonus += 0.15;
        }

        // --------------------------------------------------------
        // DIVINE SHIELD
        // --------------------------------------------------------

        if (card.divineShield && divineShieldCount >= 1) {
            bonus += 0.12;
        }

        // --------------------------------------------------------
        // BATTLECRY
        // --------------------------------------------------------

        if (card.battlecry && battlecryCount >= 2) {
            bonus += 0.10;
        }

        // --------------------------------------------------------
        // DEATHRATTLE
        // --------------------------------------------------------

        if (card.deathrattle && deathrattleCount >= 1) {
            bonus += 0.20;
        }

        if (card.deathrattle && deathrattleCount >= 3) {
            bonus += 0.10;
        }

        if (card.name.equals("Corpse Cannon") && deathrattleCount >= 2) {
            bonus += 0.25;
        }

        if (card.name.equals("Carrier Whelp") && deathrattleCount >= 2) {
            bonus += 0.15;
        }

        if (card.name.equals("Whelp of the Infinite") && deathrattleCount >= 2) {
            bonus += 0.15;
        }

        if (card.name.equals("Experimental Animation") && deathrattleCount >= 2) {
            bonus += 0.15;
        }

        // --------------------------------------------------------
        // CLASS / TRIBE-LIKE SYNERGIES
        // --------------------------------------------------------

        if (card.mech && mechCount >= 2) {
            bonus += 0.20;
        }

        if (card.beast && beastCount >= 2) {
            bonus += 0.18;
        }

        if (card.dragon && dragonCount >= 2) {
            bonus += 0.18;
        }

        if (card.undead && undeadCount >= 2) {
            bonus += 0.18;
        }

        if (card.elemental && elementalCount >= 2) {
            bonus += 0.18;
        }

        if (card.demon && demonCount >= 2) {
            bonus += 0.18;
        }

        if (card.murloc && murlocCount >= 2) {
            bonus += 0.18;
        }

        if (card.pirate && pirateCount >= 2) {
            bonus += 0.18;
        }

        if (card.naga && nagaCount >= 2) {
            bonus += 0.18;
        }

        if (card.quilboar && quilboarCount >= 2) {
            bonus += 0.18;
        }

        // --------------------------------------------------------
        // SPELL SCHOOL STYLE SYNERGIES
        // --------------------------------------------------------

        if (card.nature && natureCount >= 2) {
            bonus += 0.15;
        }

        if (card.frost && frostCount >= 2) {
            bonus += 0.15;
        }

        if (card.fire && fireCount >= 2) {
            bonus += 0.15;
        }

        if (card.shadow && shadowCount >= 2) {
            bonus += 0.15;
        }

        if (card.holy && holyCount >= 2) {
            bonus += 0.15;
        }

        if (card.arcane && arcaneCount >= 2) {
            bonus += 0.15;
        }

        // --------------------------------------------------------
        // SPECIFIC KNOWN SYNERGIES
        // --------------------------------------------------------

        String name = normalize(card.name);

        /*
         * Soldier of the Infinite:
         * Taunt cards make a defensive curve more coherent.
         */
        if (name.equals(normalize("Soldier of the Infinite"))) {

            if (tauntCount >= 2) {
                bonus += 0.20;
            }

            if (minionCount >= 5) {
                bonus += 0.08;
            }
        }

        /*
         * Contraband Wands:
         * More spells and Discover make spell-heavy picks more
         * attractive.
         */
        if (name.equals(normalize("Contraband Wands"))) {

            if (spellCount >= 4) {
                bonus += 0.20;
            }

            if (discoverCount >= 1) {
                bonus += 0.12;
            }
        }

        /*
         * Bursting Leyline / Crystallized Leyline / Leyline Nexus:
         * spell-heavy decks get additional value.
         */
        if (name.equals(normalize("Bursting Leyline"))
                || name.equals(normalize("Crystallized Leyline"))
                || name.equals(normalize("Leyline Nexus"))) {

            if (spellCount >= 4) {
                bonus += 0.20;
            }

            if (spellCount >= 8) {
                bonus += 0.10;
            }
        }

        /*
         * Surge Needle:
         * Weapon/removal package.
         */
        if (name.equals(normalize("Surge Needle"))) {

            if (removalCount >= 2) {
                bonus += 0.12;
            }

            if (weaponCount >= 1) {
                bonus += 0.10;
            }
        }

        /*
         * Cold Snap:
         * Removal-heavy and spell-heavy decks.
         */
        if (name.equals(normalize("Cold Snap"))) {

            if (spellCount >= 4) {
                bonus += 0.15;
            }

            if (removalCount >= 2) {
                bonus += 0.15;
            }
        }

        /*
         * Code Violet / Void Blast:
         * AoE/removal packages.
         */
        if (name.equals(normalize("Code Violet"))
                || name.equals(normalize("Void Blast"))) {

            if (removalCount >= 2) {
                bonus += 0.15;
            }

            if (aoeCount >= 1) {
                bonus += 0.12;
            }
        }

        /*
         * Shadowed Informant / Dig for Freedom:
         * Discover-heavy decks.
         */
        if (name.equals(normalize("Shadowed Informant"))
                || name.equals(normalize("Dig for Freedom"))) {

            if (discoverCount >= 1) {
                bonus += 0.18;
            }

            if (drawCount >= 2) {
                bonus += 0.08;
            }
        }

        /*
         * Guard Dog / Commander Beatrix / Jade Guardians:
         * Taunt package.
         */
        if (name.equals(normalize("Guard Dog"))
                || name.equals(normalize("Commander Beatrix"))
                || name.equals(normalize("Jade Guardians"))) {

            if (tauntCount >= 2) {
                bonus += 0.18;
            }
        }

        /*
         * Emergency Surgery:
         * Minion-heavy decks get more value from support effects.
         */
        if (name.equals(normalize("Emergency Surgery"))) {

            if (minionCount >= 7) {
                bonus += 0.15;
            }
        }

        /*
         * Corpse Cannon:
         * Deathrattle-heavy decks.
         */
        if (name.equals(normalize("Corpse Cannon"))) {

            if (deathrattleCount >= 2) {
                bonus += 0.30;
            }
        }

        /*
         * Underbelly Network:
         * Larger minion packages make it more useful.
         */
        if (name.equals(normalize("Underbelly Network"))) {

            if (minionCount >= 8) {
                bonus += 0.15;
            }
        }

        /*
         * Jailhouse Manastorm:
         * Very strong standalone card, but especially useful in a
         * spell-heavy deck.
         */
        if (name.equals(normalize("Jailhouse Manastorm"))) {

            if (spellCount >= 5) {
                bonus += 0.20;
            }
        }

        return clamp(bonus, -0.50, 1.00);
    }

    // ============================================================
    // RECOMMENDATION API
    // ============================================================

    public static String recommend(
            String card1,
            String card2,
            String card3
    ) {

        double score1 = score(card1);
        double score2 = score(card2);
        double score3 = score(card3);

        int best = getBestIndex(score1, score2, score3);

        String bestCard;

        if (best == 1) {
            bestCard = correctOcr(card1);
        } else if (best == 2) {
            bestCard = correctOcr(card2);
        } else {
            bestCard = correctOcr(card3);
        }

        return "SUOSITUS: Kortti " + best;
    }

    public static String recommend(
            int score1,
            int score2,
            int score3
    ) {

        int best = getBestIndex(score1, score2, score3);

        return "SUOSITUS: Kortti " + best;
    }

    public static String recommend(
            double score1,
            double score2,
            double score3
    ) {

        int best = getBestIndex(score1, score2, score3);

        return "SUOSITUS: Kortti " + best;
    }

    private static int getBestIndex(
            double score1,
            double score2,
            double score3
    ) {

        if (score1 >= score2 && score1 >= score3) {
            return 1;
        }

        if (score2 >= score1 && score2 >= score3) {
            return 2;
        }

        return 3;
    }

    // ============================================================
    // PICKED CARD MEMORY
    // ============================================================

    /**
     * Call this when the player has actually selected a card.
     *
     * Example:
     *
     * ArenaAdvisor.recordPickedCard("Soldier of the Infinite");
     */
    public static void recordPickedCard(String cardName) {

        String corrected = correctOcr(cardName);

        if (corrected.length() == 0) {
            return;
        }

        CardData card = CARDS.get(normalize(corrected));

        if (card == null) {
            return;
        }

        /*
         * Prevent accidental duplicate registration.
         *
         * The same card can technically be drafted multiple times,
         * so we do NOT use this as a duplicate-card prohibition.
         * We simply add every actual pick.
         */
        pickedCards.add(card.name);

        updateCounters(card);
    }

    private static void updateCounters(CardData card) {

        if (card.minion) {
            minionCount++;
        }

        if (card.spell) {
            spellCount++;
        }

        if (card.weapon) {
            weaponCount++;
        }

        if (card.removal) {
            removalCount++;
        }

        if (card.aoe) {
            aoeCount++;
        }

        if (card.draw) {
            drawCount++;
        }

        if (card.discover) {
            discoverCount++;
        }

        if (card.taunt) {
            tauntCount++;
        }

        if (card.divineShield) {
            divineShieldCount++;
        }

        if (card.battlecry) {
            battlecryCount++;
        }

        if (card.deathrattle) {
            deathrattleCount++;
        }

        if (card.mech) {
            mechCount++;
        }

        if (card.beast) {
            beastCount++;
        }

        if (card.dragon) {
            dragonCount++;
        }

        if (card.undead) {
            undeadCount++;
        }

        if (card.elemental) {
            elementalCount++;
        }

        if (card.demon) {
            demonCount++;
        }

        if (card.murloc) {
            murlocCount++;
        }

        if (card.pirate) {
            pirateCount++;
        }

        if (card.naga) {
            nagaCount++;
        }

        if (card.quilboar) {
            quilboarCount++;
        }

        if (card.nature) {
            natureCount++;
        }

        if (card.frost) {
            frostCount++;
        }

        if (card.fire) {
            fireCount++;
        }

        if (card.shadow) {
            shadowCount++;
        }

        if (card.holy) {
            holyCount++;
        }

        if (card.arcane) {
            arcaneCount++;
        }
    }

    // ============================================================
    // RESET
    // ============================================================

    /**
     * Clears the current Arena deck memory.
     *
     * Call this when starting a completely new Arena run.
     */
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
    // DEBUG / STATUS
    // ============================================================

    public static int getPickedCardCount() {
        return pickedCards.size();
    }

    public static List<String> getPickedCards() {
        return new ArrayList<>(pickedCards);
    }

    /**
     * Useful for debugging the current Arena deck.
     */
    public static String getDeckSummary() {

        return "Arena: "
                + pickedCards.size()
                + " korttia | "
                + "Minionit " + minionCount
                + " | Spells " + spellCount
                + " | Removal " + removalCount
                + " | AoE " + aoeCount
                + " | Draw " + drawCount
                + " | Discover " + discoverCount
                + " | Taunt " + tauntCount
                + " | Deathrattle " + deathrattleCount;
    }

    // ============================================================
    // REASON
    // ============================================================

    public static String getReason(String cardName) {

        String corrected = correctOcr(cardName);
        CardData card = CARDS.get(normalize(corrected));

        if (card == null) {
            return "Korttia ei löytynyt tietokannasta.";
        }

        double base = card.baseScore;
        double total = score(corrected);
        double synergy = total - base;

        if (synergy > 0.05) {

            return "Perusarvo "
                    + formatScore(base)
                    + " + synergiat "
                    + formatScore(synergy)
                    + " = "
                    + formatScore(total);
        }

        return "Perusarvo " + formatScore(base);
    }

    // ============================================================
    // NORMALIZATION
    // ============================================================

    private static String normalize(String value) {

        if (value == null) {
            return "";
        }

        String result = value
                .toLowerCase(Locale.US)
                .trim();

        result = result
                .replace("’", "'")
                .replace("`", "'")
                .replace("\"", "")
                .replace(".", "")
                .replace(",", "")
                .replace("!", "")
                .replace("?", "")
                .replace(":", "")
                .replace(";", "")
                .replace("-", "")
                .replace("_", "")
                .replace("/", "")
                .replace("\\", "");

        result = result.replaceAll("\\s+", "");

        return result;
    }

    // ============================================================
    // FORMATTING
    // ============================================================

    private static String formatScore(double value) {

        value = clamp(value, 0.0, 10.0);

        return String.format(Locale.US, "%.1f", value);
    }

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
