package com.bosslog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Parallelized hiscore lookup with account type detection.
 * Fires all 4 hiscore endpoints simultaneously, then determines
 * account type from the combination of results — same logic as
 * the 420kc.live proxy.js implementation.
 */
@Slf4j
@Singleton
public class HiscoreService
{
    private static final String BASE_URL = "https://secure.runescape.com/m=";
    private static final String SUFFIX = "/index_lite.ws?player=";

    // Boss names in the order they appear in hiscores (after skills + activities)
    // Jagex hiscores: 24 skills, then activities, then bosses starting at index 48
    private static final int BOSS_START_INDEX = 48;
    private static final String[] BOSS_NAMES = {
        "Abyssal Sire", "Alchemical Hydra", "Amoxliatl", "Araxxor",
        "Artio", "Barrows Chests", "Bryophyta", "Callisto",
        "Cal'varion", "Cerberus", "Chambers of Xeric",
        "Chambers of Xeric: Challenge Mode", "Chaos Elemental", "Chaos Fanatic",
        "Commander Zilyana", "Corporeal Beast", "Crazy Archaeologist",
        "Dagannoth Prime", "Dagannoth Rex", "Dagannoth Supreme",
        "Deranged Archaeologist", "Duke Sucellus", "General Graardor",
        "Giant Mole", "Grotesque Guardians", "Hespori", "Hueycoatl",
        "K'ril Tsutsaroth", "Kalphite Queen", "King Black Dragon",
        "Kraken", "Kree'Arra", "Lunar Chests", "Nex",
        "Nightmare", "Phosani's Nightmare", "Obor",
        "Phantom Muspah", "Royal Titans", "Sarachnis", "Scorpia", "Scurrius",
        "Skotizo", "Sol Heredit", "Spindel", "Tempoross",
        "The Gauntlet", "The Corrupted Gauntlet", "The Leviathan",
        "The Whisperer", "Theatre of Blood",
        "Theatre of Blood: Hard Mode", "Thermonuclear Smoke Devil",
        "Tombs of Amascut", "Tombs of Amascut: Expert Mode",
        "TzKal-Zuk", "TzTok-Jad", "Vardorvis", "Venenatis",
        "Vet'ion", "Vorkath", "Wintertodt", "Zalcano",
        "Zulrah"
    };

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Inject
    public HiscoreService()
    {
    }

    /**
     * Look up a player across all 4 hiscore endpoints in parallel.
     * Returns a CompletableFuture that resolves with the parsed result.
     */
    public CompletableFuture<HiscoreResult> lookup(String playerName)
    {
        String encoded;
        try
        {
            encoded = URLEncoder.encode(playerName, "UTF-8");
        }
        catch (Exception e)
        {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to encode player name"));
        }

        CompletableFuture<String> uimFuture = fetchAsync("hiscore_oldschool_ultimate", encoded);
        CompletableFuture<String> hcimFuture = fetchAsync("hiscore_oldschool_hardcore_ironman", encoded);
        CompletableFuture<String> ironFuture = fetchAsync("hiscore_oldschool_ironman", encoded);
        CompletableFuture<String> regFuture = fetchAsync("hiscore_oldschool", encoded);

        return CompletableFuture.allOf(uimFuture, hcimFuture, ironFuture, regFuture)
            .thenApply(v ->
            {
                String uimBody = uimFuture.join();
                String hcimBody = hcimFuture.join();
                String ironBody = ironFuture.join();
                String regBody = regFuture.join();

                // Detect account type using same logic as proxy.js
                AccountType type = detectAccountType(uimBody, hcimBody, ironBody, regBody);

                // Parse boss KC from the best available data
                String bestBody = pickBestBody(type, uimBody, hcimBody, ironBody, regBody);
                if (bestBody == null)
                {
                    return null;
                }

                return parseHiscoreBody(bestBody, type);
            });
    }

    /**
     * Detect account type from parallel hiscore responses.
     * Same algorithm as the 420kc.live proxy:
     * - If UIM endpoint returns data with matching XP -> UIM
     * - If HCIM endpoint returns data with matching XP -> HCIM
     * - If Iron endpoint returns data but not UIM/HCIM -> Ironman
     * - Otherwise -> Regular
     */
    private AccountType detectAccountType(String uimBody, String hcimBody, String ironBody, String regBody)
    {
        long regXp = extractTotalXp(regBody);
        long uimXp = extractTotalXp(uimBody);
        long hcimXp = extractTotalXp(hcimBody);
        long ironXp = extractTotalXp(ironBody);

        if (uimBody != null && uimXp == regXp && regXp > 0)
        {
            return AccountType.ULTIMATE_IRONMAN;
        }
        if (hcimBody != null && hcimXp == regXp && regXp > 0)
        {
            return AccountType.HARDCORE_IRONMAN;
        }
        if (ironBody != null && ironXp == regXp && regXp > 0)
        {
            return AccountType.IRONMAN;
        }
        // De-ironed: appeared on iron hiscores but XP no longer matches
        if (ironBody != null && regXp > 0 && ironXp > 0 && ironXp != regXp)
        {
            return AccountType.DE_IRONED;
        }
        return AccountType.REGULAR;
    }

    private long extractTotalXp(String body)
    {
        if (body == null || body.isEmpty())
        {
            return -1;
        }
        try
        {
            String firstLine = body.trim().split("\n")[0];
            String[] parts = firstLine.split(",");
            return parts.length >= 3 ? Long.parseLong(parts[2]) : -1;
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    private String pickBestBody(AccountType type, String uim, String hcim, String iron, String reg)
    {
        switch (type)
        {
            case ULTIMATE_IRONMAN:
                return uim;
            case HARDCORE_IRONMAN:
                return hcim;
            case IRONMAN:
                return iron;
            default:
                return reg;
        }
    }

    private HiscoreResult parseHiscoreBody(String body, AccountType type)
    {
        String[] lines = body.trim().split("\n");
        Map<String, Integer> bossKills = new LinkedHashMap<>();

        // Parse total level and XP from first line (Overall)
        int totalLevel = 0;
        long totalXp = 0;
        try
        {
            String[] overall = lines[0].split(",");
            totalLevel = Integer.parseInt(overall[1]);
            totalXp = Long.parseLong(overall[2]);
        }
        catch (Exception ignored) {}

        // Parse boss KCs starting at BOSS_START_INDEX
        for (int i = 0; i < BOSS_NAMES.length; i++)
        {
            int lineIdx = BOSS_START_INDEX + i;
            if (lineIdx >= lines.length)
            {
                break;
            }
            try
            {
                String[] parts = lines[lineIdx].split(",");
                int kc = Integer.parseInt(parts[1]);
                bossKills.put(BOSS_NAMES[i], kc);
            }
            catch (Exception e)
            {
                bossKills.put(BOSS_NAMES[i], -1);
            }
        }

        return new HiscoreResult(type, bossKills, totalLevel, totalXp);
    }

    private CompletableFuture<String> fetchAsync(String hiscoreKey, String encodedPlayer)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                URL url = new URL(BASE_URL + hiscoreKey + SUFFIX + encodedPlayer);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "RuneLite Boss Log Plugin");

                int status = conn.getResponseCode();
                if (status != 200)
                {
                    return null;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            }
            catch (Exception e)
            {
                log.debug("Hiscore fetch failed for {}: {}", hiscoreKey, e.getMessage());
                return null;
            }
        }, executor);
    }
}
