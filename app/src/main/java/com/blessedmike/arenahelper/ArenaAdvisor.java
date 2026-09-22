package com.blessedmike.arenahelper;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaAdvisor {

    private static final String HEARTHARENA_URL =
            "https://www.heartharena.com/tierlist";

    private static final double HEARTHARENA_SCALE = 13.0;

    private static final double UNKNOWN_CARD_SCORE = 0.0;

    private static final Map<String, CardData> CARDS =
            new HashMap<>();

    private static final Map<String, String> OCR_ALIASES =
            new HashMap<>();

    private static final Set<String> ONLINE_NAMES =
            new HashSet<>();

    private static final Set<String> PICKED_CARDS =
            new HashSet<>();

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER =
            new Handler(Looper.getMainLooper());

    private static boolean onlineLoadStarted = false;
    private static boolean onlineDataLoaded = false;

    private static final Map<String, Integer> ONLINE_RAW_SCORES =
            new HashMap<>();

    private static final Map<String, String> ONLINE_CARD_NAMES =
            new HashMap<>();

    private static class CardData {

        final String name;
        final double score;

        CardData(
                String name,
                double score
        ) {
            this.name = name;
            this.score = score;
        }
    }

    static {

        /*
         * ----------------------------------------------------
         * FALLBACK-KORTIT
         * ----------------------------------------------------
         */

        addCard("Soldier of the Infinite", 8.20);
        addCard("Crystallized Leyline", 5.65);
        addCard("Surge Needle", 5.85);
        addCard("Bursting Leyline", 6.50);
        addCard("Contraband Wands", 6.30);
        addCard("Cold Snap", 7.50);
        addCard("Code Violet", 7.20);
        addCard("Ley Walker", 5.50);
        addCard("Leyline Nexus", 6.00);
        addCard("Raban Wands", 6.50);

        addCard(
                "Spellweaver's Brilliance",
                3.92
        );

        addCard(
                "Windswept Pageturner",
                5.23
        );

        addCard(
                "Spirit Gatherer",
                7.69
        );

        addCard(
                "Vault Breaker",
                62.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Scorching Winds",
                48.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Platysaur",
                67.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Sizzling Cinder",
                69.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Relic Miner",
                66.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Temporal Traveler",
                66.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Hourglass Attendant",
                65.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Sewer Imp",
                65.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Critter Caretaker",
                64.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Drakeadon Mongrel",
                64.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Scalehide Kodo",
                67.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Living Paradox",
                69.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Battlefield Blaster",
                69.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Gemstone Hoarder",
                71.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Ancient Stegodon",
                70.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Briarspawn Drake",
                70.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Dreambound Raptor",
                70.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Whirling Stormdrake",
                70.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Willful Watcher",
                70.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Gnawing Greenfin",
                73.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Aeon Wizard",
                73.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Rockskipper",
                74.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Undercover Cultist",
                74.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Fae Trickster",
                74.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Daydreaming Pixie",
                76.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Flutterwing Guardian",
                76.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Quantum Destabilizer",
                76.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Mother Duck",
                77.0 / HEARTHARENA_SCALE
        );

        addCard(
                "Defias Smuggler",
                79.0 / HEARTHARENA_SCALE
        );

        /*
         * ----------------------------------------------------
         * OCR-ALIASES
         * ----------------------------------------------------
         */

        addAlias(
                "soldier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldierofinfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "so1dier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "so1dier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "sotdier of the infinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of ihfinite",
                "Soldier of the Infinite"
        );

        addAlias(
                "soldier of ihfini",
                "Soldier of the Infinite"
        );

        addAlias(
                "raban wand",
                "Raban Wands"
        );

        addAlias(
                "raban wands",
                "Raban Wands"
        );

        addAlias(
                "raban wandz",
                "Raban Wands"
        );

        addAlias(
                "crystallized leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "crystallised leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "crystalized leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "crystalised leyline",
                "Crystallized Leyline"
        );

        addAlias(
                "surge needle",
                "Surge Needle"
        );

        addAlias(
                "surge need1e",
                "Surge Needle"
        );

        addAlias(
                "spellweavers brilliance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "spellweaver's brilliance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "spellweavers brillance",
                "Spellweaver's Brilliance"
        );

        addAlias(
                "windswept pageturner",
                "Windswept Pageturner"
        );

        addAlias(
                "spirit gatherer",
                "Spirit Gatherer"
        );

        addAlias(
                "spirit gather",
                "Spirit Gatherer"
        );

        addAlias(
                "spirit gathere",
                "Spirit Gatherer"
        );

        addAlias(
                "vault breaker",
                "Vault Breaker"
        );

        addAlias(
                "vault breake",
                "Vault Breaker"
        );

        addAlias(
                "vault breker",
                "Vault Breaker"
        );

        addAlias(
                "scorching winds",
                "Scorching Winds"
        );

        addAlias(
                "scorching wind",
                "Scorching Winds"
        );

        addAlias(
                "scorching wnds",
                "Scorching Winds"
        );

        addAlias(
                "platysaur",
                "Platysaur"
        );

        loadOnlineData();
    }

    private ArenaAdvisor() {
    }

    private static void addCard(
            String name,
            double score
    ) {

        if (
                name == null ||
                name.trim().isEmpty()
        ) {
            return;
        }

        String key =
                normalizeKey(name);

        if (key.isEmpty()) {
            return;
        }

        CARDS.put(
                key,
                new CardData(
                        name,
                        score
                )
        );
    }

    private static void addAlias(
            String alias,
            String cardName
    ) {

        if (
                alias == null ||
                cardName == null
        ) {
            return;
        }

        String key =
                normalizeKey(alias);

        if (key.isEmpty()) {
            return;
        }

        OCR_ALIASES.put(
                key,
                cardName
        );
    }

    /*
     * ----------------------------------------------------
     * ONLINE DATA
     * ----------------------------------------------------
     */

    private static void loadOnlineData() {

        synchronized (ArenaAdvisor.class) {

            if (onlineLoadStarted) {
                return;
            }

            onlineLoadStarted = true;
        }

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
                        12000
                );

                connection.setReadTimeout(
                        20000
                );

                connection.setUseCaches(
                        false
                );

                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android) ArenaHelper"
                );

                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml,text/plain"
                );

                connection.setRequestProperty(
                        "Accept-Language",
                        "en-US,en;q=0.9"
                );

                int responseCode =
                        connection.getResponseCode();

                if (
                        responseCode !=
                                HttpURLConnection.HTTP_OK
                ) {
                    return;
                }

                InputStream input =
                        connection.getInputStream();

                String html =
                        readStream(input);

                if (
                        html == null ||
                        html.trim().isEmpty()
                ) {
                    return;
                }

                parseHearthArenaPage(
                        html
                );

                if (
                        !ONLINE_RAW_SCORES.isEmpty()
                ) {

                    synchronized (
                            ArenaAdvisor.class
                    ) {

                        for (
                                Map.Entry<String, Integer>
                                        entry
                                : ONLINE_RAW_SCORES.entrySet()
                        ) {

                            String normalizedName =
                                    entry.getKey();

                            int rawScore =
                                    entry.getValue();

                            String displayName =
                                    ONLINE_CARD_NAMES.get(
                                            normalizedName
                                    );

                            if (
                                    displayName == null ||
                                    displayName.isEmpty()
                            ) {

                                displayName =
                                        findOriginalName(
                                                normalizedName
                                        );
                            }

                            if (
                                    displayName == null ||
                                    displayName.isEmpty()
                            ) {

                                displayName =
                                        normalizedName;
                            }

                            double score =
                                    rawScore /
                                            HEARTHARENA_SCALE;

                            CARDS.put(
                                    normalizedName,
                                    new CardData(
                                            displayName,
                                            score
                                    )
                            );

                            ONLINE_NAMES.add(
                                    normalizedName
                            );
                        }

                        onlineDataLoaded = true;
                    }
                }

            } catch (Exception ignored) {

            } finally {

                if (connection != null) {

                    try {
                        connection.disconnect();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private static String readStream(
            InputStream input
    ) {

        if (input == null) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            String line;

            while (
                    (line =
                            reader.readLine()) != null
            ) {

                builder.append(line);
                builder.append('\n');
            }

            reader.close();

        } catch (Exception ignored) {
        }

        return builder.toString();
    }

    /*
     * ----------------------------------------------------
     * PARSER
     * ----------------------------------------------------
     *
     * Tämä on tärkein muutos.
     *
     * HearthArena käyttää sivulla useita erilaisia
     * taulukkorakenteita. Parseri yrittää nyt:
     *
     * 1. tavallista "Card 67" -riviä
     * 2. "Card / 67" -riviä
     * 3. korttinimi + seuraava numero
     * 4. pipe-taulukon rivejä
     * 5. rank + kortti + score -rakennetta
     */

    private static void parseHearthArenaPage(
            String html
    ) {

        if (
                html == null ||
                html.isEmpty()
        ) {
            return;
        }

        String text =
                stripHtml(html);

        if (
                text == null ||
                text.isEmpty()
        ) {
            return;
        }

        /*
         * Muutetaan taulukkoerottimet omiksi riveikseen.
         */
        text =
                text.replace(
                        "|",
                        "\n"
                );

        text =
                text.replace(
                        "\t",
                        "\n"
                );

        String[] lines =
                text.split(
                        "\\r?\\n"
                );

        String previousCard =
                "";

        for (
                String rawLine
                : lines
        ) {

            if (rawLine == null) {
                continue;
            }

            String line =
                    decodeHtml(
                            rawLine
                    ).trim();

            if (line.isEmpty()) {
                continue;
            }

            line =
                    normalizeWhitespace(
                            line
                    );

            line =
                    removeRankingPrefix(
                            line
                    );

            /*
             * 1. "Card Name 67"
             */
            String[] sameLine =
                    splitCardAndScore(
                            line
                    );

            if (sameLine != null) {

                storeOnlineCard(
                        sameLine[0],
                        sameLine[1]
                );

                previousCard = "";

                continue;
            }

            /*
             * 2. Pelkkä numero.
             */
            String scoreLine =
                    cleanScoreLine(
                            line
                    );

            if (
                    isScore(
                            scoreLine
                    )
            ) {

                if (
                        !previousCard.isEmpty()
                ) {

                    storeOnlineCard(
                            previousCard,
                            scoreLine
                    );
                }

                previousCard = "";

                continue;
            }

            /*
             * 3. Kortin nimi.
             */
            if (
                    isLikelyCardName(
                            line
                    )
            ) {

                previousCard =
                        cleanOnlineCardName(
                                line
                        );
            }
        }

        /*
         * Toinen pass:
         * etsitään suoraan tekstistä yleisiä
         * "kortti + numero" -rakenteita.
         */
        parseCardScorePatterns(text);
    }

    private static void parseCardScorePatterns(
            String text
    ) {

        if (
                text == null ||
                text.isEmpty()
        ) {
            return;
        }

        String[] lines =
                text.split(
                        "\\r?\\n"
                );

        for (
                String line
                : lines
        ) {

            if (
                    line == null ||
                    line.trim().isEmpty()
            ) {
                continue;
            }

            String value =
                    normalizeWhitespace(
                            decodeHtml(
                                    line
                            )
                    );

            /*
             * "Card Name 67"
             */
            Matcher matcher =
                    Pattern.compile(
                            "^(.{2,100}?)\\s+(\\d{1,3})(?:↓)?$"
                    ).matcher(value);

            if (
                    matcher.matches()
            ) {

                String name =
                        cleanOnlineCardName(
                                matcher.group(1)
                        );

                String score =
                        matcher.group(2);

                if (
                        isLikelyCardName(name) &&
                        isScore(score)
                ) {

                    storeOnlineCard(
                            name,
                            score
                    );
                }
            }
        }
    }

    private static String cleanScoreLine(
            String line
    ) {

        if (line == null) {
            return "";
        }

        return line
                .replace(
                        "↓",
                        ""
                )
                .trim();
    }

    private static boolean isScore(
            String text
    ) {

        if (
                text == null ||
                text.trim().isEmpty()
        ) {
            return false;
        }

        String value =
                text.trim();

        if (
                !value.matches(
                        "\\d+(?:\\.\\d+)?"
                )
        ) {
            return false;
        }

        try {

            double score =
                    Double.parseDouble(
                            value
                    );

            return score >= 0 &&
                    score <= 200;

        } catch (Exception ignored) {

            return false;
        }
    }

    private static String removeRankingPrefix(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replaceFirst(
                        "^\\s*\\d+\\.\\s+",
                        ""
                )
                .trim();
    }

    private static String[] splitCardAndScore(
            String line
    ) {

        if (
                line == null ||
                line.trim().isEmpty()
        ) {
            return null;
        }

        String cleaned =
                line
                        .replace(
                                "↓",
                                ""
                        )
                        .trim();

        Matcher matcher =
                Pattern.compile(
                        "^(.+?)\\s+(\\d+(?:\\.\\d+)?)$"
                ).matcher(
                        cleaned
                );

        if (!matcher.matches()) {
            return null;
        }

        String possibleName =
                matcher.group(1);

        String score =
                matcher.group(2);

        if (
                !isLikelyCardName(
                        possibleName
                )
        ) {
            return null;
        }

        if (
                !isScore(score)
        ) {
            return null;
        }

        return new String[]{
                possibleName,
                score
        };
    }

    private static void storeOnlineCard(
            String cardName,
            String scoreText
    ) {

        if (
                cardName == null ||
                cardName.trim().isEmpty()
        ) {
            return;
        }

        if (
                scoreText == null ||
                scoreText.trim().isEmpty()
        ) {
            return;
        }

        try {

            double parsed =
                    Double.parseDouble(
                            scoreText
                    );

            if (
                    parsed < 0 ||
                    parsed > 200
            ) {
                return;
            }

            int integerScore =
                    (int)
                            Math.round(
                                    parsed
                            );

            String cleanedName =
                    cleanOnlineCardName(
                            cardName
                    );

            String key =
                    normalizeKey(
                            cleanedName
                    );

            if (
                    key.isEmpty() ||
                    !isLikelyCardName(
                            cleanedName
                    )
            ) {
                return;
            }

            if (
                    key.equals("cards") ||
                    key.equals("english") ||
                    key.equals("neutral") ||
                    key.equals("great") ||
                    key.equals("good") ||
                    key.equals("average") ||
                    key.equals("bad") ||
                    key.equals("terrible")
            ) {
                return;
            }

            Integer old =
                    ONLINE_RAW_SCORES.get(
                            key
                    );

            /*
             * Jos kortti esiintyy usealla luokalla,
             * käytetään suurinta löydettyä arvoa.
             *
             * Tämä vastaa nykyisen advisorin
             * luokkariippumatonta toimintaa.
             */
            if (
                    old == null ||
                    integerScore > old
            ) {

                ONLINE_RAW_SCORES.put(
                        key,
                        integerScore
                );

                ONLINE_CARD_NAMES.put(
                        key,
                        cleanedName
                );

            } else if (
                    !ONLINE_CARD_NAMES.containsKey(
                            key
                    )
            ) {

                ONLINE_CARD_NAMES.put(
                        key,
                        cleanedName
                );
            }

        } catch (Exception ignored) {
        }
    }

    private static String cleanOnlineCardName(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                decodeHtml(
                        text
                );

        result =
                removeRankingPrefix(
                        result
                );

        result =
                normalizeWhitespace(
                        result
                );

        result =
                result.replace(
                        "↓",
                        ""
                );

        return result.trim();
    }

    private static boolean isLikelyCardName(
            String text
    ) {

        if (text == null) {
            return false;
        }

        String value =
                text.trim();

        if (value.isEmpty()) {
            return false;
        }

        value =
                removeRankingPrefix(
                        value
                );

        String lower =
                value.toLowerCase(
                        Locale.US
                );

        if (
                lower.equals("death knight") ||
                lower.equals("demon hunter") ||
                lower.equals("druid") ||
                lower.equals("hunter") ||
                lower.equals("mage") ||
                lower.equals("paladin") ||
                lower.equals("priest") ||
                lower.equals("rogue") ||
                lower.equals("shaman") ||
                lower.equals("warlock") ||
                lower.equals("warrior") ||
                lower.equals("neutral") ||
                lower.equals("cards") ||
                lower.equals("great") ||
                lower.equals("good") ||
                lower.equals("above average") ||
                lower.equals("average") ||
                lower.equals("below average") ||
                lower.equals("bad") ||
                lower.equals("terrible") ||
                lower.startsWith("cards in ") ||
                lower.startsWith("english")
        ) {
            return false;
        }

        if (
                value.matches(
                        "\\d+\\."
                )
        ) {
            return false;
        }

        if (
                value.matches(
                        "\\d+(?:\\.\\d+)?"
                )
        ) {
            return false;
        }

        boolean hasLetter = false;

        for (
                int i = 0;
                i < value.length();
                i++
        ) {

            if (
                    Character.isLetter(
                            value.charAt(i)
                    )
            ) {

                hasLetter = true;
                break;
            }
        }

        if (!hasLetter) {
            return false;
        }

        if (value.length() > 100) {
            return false;
        }

        /*
         * Älä hyväksy selvästi sivun UI-tekstejä.
         */
        if (
                lower.contains("heartharena") ||
                lower.contains("tier list") ||
                lower.contains("tierlist") ||
                lower.contains("card list") ||
                lower.contains("last updated") ||
                lower.contains("copyright")
        ) {
            return false;
        }

        return true;
    }

    private static String stripHtml(
            String html
    ) {

        if (html == null) {
            return "";
        }

        String result =
                html;

        result =
                result.replaceAll(
                        "(?is)<script[^>]*>.*?</script>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?is)<style[^>]*>.*?</style>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)<br\\s*/?>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</div>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</li>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</p>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</h[1-6]>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</tr>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</td>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "(?i)</th>",
                        "\n"
                );

        result =
                result.replaceAll(
                        "<[^>]+>",
                        ""
                );

        result =
                decodeHtml(
                        result
                );

        return result;
    }

    private static String decodeHtml(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                text;

        result =
                result.replace(
                        "&amp;",
                        "&"
                );

        result =
                result.replace(
                        "&quot;",
                        "\""
                );

        result =
                result.replace(
                        "&#039;",
                        "'"
                );

        result =
                result.replace(
                        "&#39;",
                        "'"
                );

        result =
                result.replace(
                        "&apos;",
                        "'"
                );

        result =
                result.replace(
                        "&nbsp;",
                        " "
                );

        result =
                result.replace(
                        "&lt;",
                        "<"
                );

        result =
                result.replace(
                        "&gt;",
                        ">"
                );

        result =
                result.replaceAll(
                        "&#x27;",
                        "'"
                );

        result =
                result.replaceAll(
                        "&#x2F;",
                        "/"
                );

        /*
         * Tavallisimmat numeromuotoiset HTML-entiteetit.
         */
        result =
                result.replaceAll(
                        "&#x([0-9A-Fa-f]+);",
                        " "
                );

        return result;
    }

    private static String normalizeWhitespace(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        '\u00A0',
                        ' '
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    private static String findOriginalName(
            String normalized
    ) {

        if (
                normalized == null ||
                normalized.isEmpty()
        ) {
            return "";
        }

        String onlineName =
                ONLINE_CARD_NAMES.get(
                        normalized
                );

        if (
                onlineName != null &&
                !onlineName.isEmpty()
        ) {
            return onlineName;
        }

        for (
                CardData card
                : CARDS.values()
        ) {

            if (
                    normalizeKey(
                            card.name
                    ).equals(
                            normalized
                    )
            ) {
                return card.name;
            }
        }

        return restoreBasicName(
                normalized
        );
    }

    private static String restoreBasicName(
            String normalized
    ) {

        if (
                normalized == null ||
                normalized.isEmpty()
        ) {
            return "";
        }

        return normalized.replace(
                "_",
                " "
        );
    }

    /*
     * ----------------------------------------------------
     * OCR
     * ----------------------------------------------------
     */

    public static String correctOcr(
            String input
    ) {

        if (input == null) {
            return "";
        }

        String cleaned =
                cleanInput(
                        input
                );

        if (cleaned.isEmpty()) {
            return "";
        }

        String normalized =
                normalizeKey(
                        cleaned
                );

        if (normalized.isEmpty()) {
            return cleaned;
        }

        if (
                normalized.contains(
                        "soldierofinfinite"
                ) ||
                normalized.contains(
                        "so1dierofinfinite"
                ) ||
                normalized.contains(
                        "sotdierofinfinite"
                ) ||
                normalized.contains(
                        "soldierofihfinite"
                ) ||
                normalized.contains(
                        "soldierofihfini"
                )
        ) {

            return "Soldier of the Infinite";
        }

        String alias =
                OCR_ALIASES.get(
                        normalized
                );

        if (alias != null) {
            return alias;
        }

        CardData exact =
                CARDS.get(
                        normalized
                );

        if (exact != null) {
            return exact.name;
        }

        String onlineExact =
                ONLINE_CARD_NAMES.get(
                        normalized
                );

        if (
                onlineExact != null &&
                !onlineExact.isEmpty()
        ) {
            return onlineExact;
        }

        String prefixMatch =
                findBestKnownPrefixMatch(
                        normalized
                );

        if (
                prefixMatch != null &&
                !prefixMatch.isEmpty()
        ) {
            return prefixMatch;
        }

        String bestName =
                "";

        int bestDistance =
                Integer.MAX_VALUE;

        int maxAllowed =
                Math.max(
                        2,
                        normalized.length() / 4
                );

        for (
                CardData card
                : CARDS.values()
        ) {

            String candidate =
                    normalizeKey(
                            card.name
                    );

            if (candidate.isEmpty()) {
                continue;
            }

            if (
                    candidate.equals(
                            normalized
                    )
            ) {
                return card.name;
            }

            if (
                    candidate.contains(
                            normalized
                    )
            ) {

                int difference =
                        candidate.length()
                                -
                                normalized.length();

                if (
                        difference >= 0 &&
                        difference <= 12 &&
                        difference < bestDistance
                ) {

                    bestDistance =
                            difference;

                    bestName =
                            card.name;
                }

                continue;
            }

            if (
                    normalized.contains(
                            candidate
                    )
            ) {

                int difference =
                        normalized.length()
                                -
                                candidate.length();

                if (
                        difference <= 4 &&
                        difference < bestDistance
                ) {

                    bestDistance =
                            difference;

                    bestName =
                            card.name;
                }

                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            if (
                    distance <
                            bestDistance
            ) {

                bestDistance =
                        distance;

                bestName =
                        card.name;
            }
        }

        if (
                !bestName.isEmpty() &&
                bestDistance <= maxAllowed
        ) {

            return bestName;
        }

        String bestOnline =
                findBestOnlineMatch(
                        normalized
                );

        if (
                bestOnline != null &&
                !bestOnline.isEmpty()
        ) {

            return bestOnline;
        }

        return cleaned;
    }

    private static String findBestKnownPrefixMatch(
            String normalized
    ) {

        if (
                normalized == null ||
                normalized.isEmpty()
        ) {
            return "";
        }

        if (normalized.length() < 5) {
            return "";
        }

        String bestName =
                "";

        int bestLength =
                -1;

        for (
                CardData card
                : CARDS.values()
        ) {

            String candidate =
                    normalizeKey(
                            card.name
                    );

            if (candidate.isEmpty()) {
                continue;
            }

            if (
                    candidate.startsWith(
                            normalized
                    )
            ) {

                if (
                        candidate.length()
                                > bestLength
                ) {

                    bestLength =
                            candidate.length();

                    bestName =
                            card.name;
                }
            }
        }

        for (
                Map.Entry<String, String> entry
                : ONLINE_CARD_NAMES.entrySet()
        ) {

            String candidate =
                    entry.getKey();

            String displayName =
                    entry.getValue();

            if (
                    candidate == null ||
                    displayName == null
            ) {
                continue;
            }

            if (
                    candidate.startsWith(
                            normalized
                    )
            ) {

                if (
                        candidate.length()
                                > bestLength
                ) {

                    bestLength =
                            candidate.length();

                    bestName =
                            displayName;
                }
            }
        }

        return bestName;
    }

    private static String findBestOnlineMatch(
            String normalized
    ) {

        if (
                normalized == null ||
                normalized.isEmpty()
        ) {
            return "";
        }

        String bestName =
                "";

        int bestDistance =
                Integer.MAX_VALUE;

        for (
                Map.Entry<String, String> entry
                : ONLINE_CARD_NAMES.entrySet()
        ) {

            String candidate =
                    entry.getKey();

            String displayName =
                    entry.getValue();

            if (
                    candidate == null ||
                    displayName == null
            ) {
                continue;
            }

            if (
                    candidate.equals(
                            normalized
                    )
            ) {
                return displayName;
            }

            if (
                    candidate.startsWith(
                            normalized
                    ) &&
                    normalized.length() >= 5
            ) {

                int difference =
                        candidate.length()
                                -
                                normalized.length();

                if (
                        difference >= 0 &&
                        difference < bestDistance
                ) {

                    bestDistance =
                            difference;

                    bestName =
                            displayName;
                }

                continue;
            }

            int distance =
                    levenshtein(
                            normalized,
                            candidate
                    );

            int allowed =
                    Math.max(
                            2,
                            Math.max(
                                    normalized.length(),
                                    candidate.length()
                            ) / 5
                    );

            if (
                    distance <= allowed &&
                    distance < bestDistance
            ) {

                bestDistance =
                        distance;

                bestName =
                        displayName;
            }
        }

        return bestName;
    }

    private static String cleanInput(
            String text
    ) {

        if (text == null) {
            return "";
        }

        String result =
                text.trim();

        result =
                result.replace(
                        "&#039;",
                        "'"
                );

        result =
                result.replace(
                        "&#39;",
                        "'"
                );

        result =
                result.replaceAll(
                        "\\s+",
                        " "
                );

        result =
                result.replaceAll(
                        "^[^A-Za-zÀ-ÿ0-9]+",
                        ""
                );

        result =
                result.replaceAll(
                        "[^A-Za-zÀ-ÿ0-9'&\\-\\.\\s]+$",
                        ""
                );

        result =
                result.replaceAll(
                        "[\\s\\.,:;|]+$",
                        ""
                );

        return result.trim();
    }

    private static String normalizeKey(
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
                        "’",
                        "'"
                )
                .replace(
                        "´",
                        "'"
                )
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    /*
     * ----------------------------------------------------
     * SCORE
     * ----------------------------------------------------
     */

    public static String getCardScore(
            String cardName
    ) {

        double value =
                score(
                        cardName
                );

        if (value <= 0.0) {
            return "0.0";
        }

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }

    public static double score(
            String cardName
    ) {

        if (
                cardName == null ||
                cardName.trim().isEmpty()
        ) {
            return UNKNOWN_CARD_SCORE;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (
                corrected == null ||
                corrected.trim().isEmpty()
        ) {
            return UNKNOWN_CARD_SCORE;
        }

        String key =
                normalizeKey(
                        corrected
                );

        if (key.isEmpty()) {
            return UNKNOWN_CARD_SCORE;
        }

        CardData card =
                CARDS.get(
                        key
                );

        if (
                card == null &&
                onlineDataLoaded
        ) {

            for (
                    String onlineName
                    : ONLINE_NAMES
            ) {

                if (
                        similarKeys(
                                onlineName,
                                key
                        )
                ) {

                    card =
                            CARDS.get(
                                    onlineName
                            );

                    if (card != null) {
                        break;
                    }
                }
            }
        }

        if (card == null) {

            String onlineName =
                    findBestOnlineMatch(
                            key
                    );

            if (
                    onlineName != null &&
                    !onlineName.isEmpty()
            ) {

                CardData onlineCard =
                        CARDS.get(
                                normalizeKey(
                                        onlineName
                                )
                        );

                if (onlineCard != null) {
                    card = onlineCard;
                }
            }
        }

        if (card == null) {
            return UNKNOWN_CARD_SCORE;
        }

        double result =
                card.score;

        result +=
                synergyBonus(
                        card.name
                );

        return result;
    }

    private static double synergyBonus(
            String cardName
    ) {

        if (
                cardName == null ||
                cardName.isEmpty()
        ) {
            return 0.0;
        }

        String key =
                normalizeKey(
                        cardName
                );

        if (
                PICKED_CARDS.contains(
                        key
                )
        ) {
            return 0.10;
        }

        return 0.0;
    }

    /*
     * ----------------------------------------------------
     * RECOMMENDATION
     * ----------------------------------------------------
     */

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

        boolean valid1 =
                score1 > UNKNOWN_CARD_SCORE;

        boolean valid2 =
                score2 > UNKNOWN_CARD_SCORE;

        boolean valid3 =
                score3 > UNKNOWN_CARD_SCORE;

        if (
                !valid1 &&
                !valid2 &&
                !valid3
        ) {
            return "Kortteja ei tunnistettu";
        }

        if (
                valid1 &&
                !valid2 &&
                !valid3
        ) {
            return formatRecommendation(
                    1,
                    card1,
                    score1
            );
        }

        if (
                !valid1 &&
                valid2 &&
                !valid3
        ) {
            return formatRecommendation(
                    2,
                    card2,
                    score2
            );
        }

        if (
                !valid1 &&
                !valid2 &&
                valid3
        ) {
            return formatRecommendation(
                    3,
                    card3,
                    score3
            );
        }

        if (
                valid1 &&
                valid2 &&
                !valid3
        ) {

            if (score1 >= score2) {

                return formatRecommendation(
                        1,
                        card1,
                        score1
                );

            } else {

                return formatRecommendation(
                        2,
                        card2,
                        score2
                );
            }
        }

        if (
                valid1 &&
                !valid2 &&
                valid3
        ) {

            if (score1 >= score3) {

                return formatRecommendation(
                        1,
                        card1,
                        score1
                );

            } else {

                return formatRecommendation(
                        3,
                        card3,
                        score3
                );
            }
        }

        if (
                !valid1 &&
                valid2 &&
                valid3
        ) {

            if (score2 >= score3) {

                return formatRecommendation(
                        2,
                        card2,
                        score2
                );

            } else {

                return formatRecommendation(
                        3,
                        card3,
                        score3
                );
            }
        }

        if (
                score1 >= score2 &&
                score1 >= score3
        ) {

            return formatRecommendation(
                    1,
                    card1,
                    score1
            );
        }

        if (
                score2 >= score1 &&
                score2 >= score3
        ) {

            return formatRecommendation(
                    2,
                    card2,
                    score2
            );
        }

        return formatRecommendation(
                3,
                card3,
                score3
        );
    }

    private static String formatRecommendation(
            int number,
            String cardName,
            double score
    ) {

        if (
                cardName == null ||
                cardName.trim().isEmpty()
        ) {
            return "Korttia ei tunnistettu";
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (
                corrected != null &&
                !corrected.trim().isEmpty()
        ) {
            cardName =
                    corrected;
        }

        return "Kortti " +
                number +
                " – " +
                cardName +
                " (" +
                String.format(
                        Locale.US,
                        "%.2f",
                        score
                ) +
                ")";
    }

    /*
     * ----------------------------------------------------
     * REASON
     * ----------------------------------------------------
     */

    public static String getReason(
            String cardName
    ) {

        double value =
                score(
                        cardName
                );

        if (value <= 0.0) {
            return "Kortille ei löytynyt arvoa.";
        }

        if (value >= 8.0) {
            return "Erittäin vahva Arena-valinta.";
        }

        if (value >= 7.0) {
            return "Vahva Arena-valinta.";
        }

        if (value >= 6.0) {
            return "Hyvä Arena-valinta.";
        }

        if (value >= 5.0) {
            return "Keskitasoinen Arena-valinta.";
        }

        if (value >= 4.0) {
            return "Alle keskitason Arena-valinta.";
        }

        return "Heikko Arena-valinta.";
    }

    /*
     * ----------------------------------------------------
     * CARD HISTORY
     * ----------------------------------------------------
     */

    public static boolean isValidCard(
            String cardName
    ) {

        if (
                cardName == null ||
                cardName.trim().isEmpty()
        ) {
            return false;
        }

        return score(cardName) > 0.0;
    }

    public static void recordPickedCard(
            String cardName
    ) {

        if (
                cardName == null ||
                cardName.trim().isEmpty()
        ) {
            return;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        if (
                corrected == null ||
                corrected.trim().isEmpty()
        ) {
            return;
        }

        PICKED_CARDS.add(
                normalizeKey(
                        corrected
                )
        );
    }

    public static int getPickedCount(
            String cardName
    ) {

        if (
                cardName == null ||
                cardName.trim().isEmpty()
        ) {
            return 0;
        }

        String corrected =
                correctOcr(
                        cardName
                );

        String key =
                normalizeKey(
                        corrected
                );

        if (key.isEmpty()) {
            return 0;
        }

        return PICKED_CARDS.contains(
                key
        ) ? 1 : 0;
    }

    public static void clearPickedCards() {
        PICKED_CARDS.clear();
    }

    /*
     * ----------------------------------------------------
     * SIMILARITY
     * ----------------------------------------------------
     */

    private static boolean similarKeys(
            String a,
            String b
    ) {

        if (
                a == null ||
                b == null
        ) {
            return false;
        }

        String aa =
                normalizeKey(a);

        String bb =
                normalizeKey(b);

        if (
                aa.equals(bb)
        ) {
            return true;
        }

        if (
                aa.contains(bb) ||
                bb.contains(aa)
        ) {

            int shorter =
                    Math.min(
                            aa.length(),
                            bb.length()
                    );

            if (shorter >= 5) {
                return true;
            }
        }

        int distance =
                levenshtein(
                        aa,
                        bb
                );

        int maxLength =
                Math.max(
                        aa.length(),
                        bb.length()
                );

        return distance <=
                Math.max(
                        2,
                        maxLength / 5
                );
    }

    private static int levenshtein(
            String a,
            String b
    ) {

        if (a == null) {

            return b == null
                    ? 0
                    : b.length();
        }

        if (b == null) {
            return a.length();
        }

        int[][] dp =
                new int[
                        a.length() + 1
                ][
                        b.length() + 1
                ];

        for (
                int i = 0;
                i <= a.length();
                i++
        ) {

            dp[i][0] =
                    i;
        }

        for (
                int j = 0;
                j <= b.length();
                j++
        ) {

            dp[0][j] =
                    j;
        }

        for (
                int i = 1;
                i <= a.length();
                i++
        ) {

            for (
                    int j = 1;
                    j <= b.length();
                    j++
            ) {

                int cost =
                        a.charAt(
                                i - 1
                        )
                                ==
                        b.charAt(
                                j - 1
                        )
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

    /*
     * ----------------------------------------------------
     * STATUS
     * ----------------------------------------------------
     */

    public static boolean isOnlineDataLoaded() {
        return onlineDataLoaded;
    }

    public static int getKnownCardCount() {
        return CARDS.size();
    }

    public static int getOnlineCardCount() {
        return ONLINE_NAMES.size();
    }

    public static String getDataStatus() {

        if (onlineDataLoaded) {

            return "HearthArena: " +
                    ONLINE_NAMES.size() +
                    " korttia ladattu";
        }

        return "HearthArena-dataa ladataan...";
    }
}
