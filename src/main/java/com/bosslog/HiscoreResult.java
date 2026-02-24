package com.bosslog;

import java.util.Map;

/**
 * Parsed hiscore data for a player.
 */
public class HiscoreResult
{
    private final AccountType accountType;
    private final Map<String, Integer> bossKills;
    private final int totalLevel;
    private final long totalXp;

    public HiscoreResult(AccountType accountType, Map<String, Integer> bossKills, int totalLevel, long totalXp)
    {
        this.accountType = accountType;
        this.bossKills = bossKills;
        this.totalLevel = totalLevel;
        this.totalXp = totalXp;
    }

    public AccountType getAccountType()
    {
        return accountType;
    }

    public Map<String, Integer> getBossKills()
    {
        return bossKills;
    }

    public int getTotalLevel()
    {
        return totalLevel;
    }

    public long getTotalXp()
    {
        return totalXp;
    }

    public int getKc(String bossName)
    {
        return bossKills.getOrDefault(bossName, -1);
    }
}
