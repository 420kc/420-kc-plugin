package com.bosslog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("420kc")
public interface BossLogConfig extends Config
{
    enum FourTwentyMode
    {
        OFF,
        ON,
        CAP
    }

    @ConfigItem(
        keyName = "defaultPlayer",
        name = "Default Player",
        description = "Player name to look up on startup (leave blank to use logged-in character)"
    )
    default String defaultPlayer()
    {
        return "";
    }

    @ConfigItem(
        keyName = "showAccountType",
        name = "Show Account Type",
        description = "Detect and display account type (Iron, HCIM, UIM)"
    )
    default boolean showAccountType()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showCollectionLog",
        name = "Show Collection Log",
        description = "Fetch collection log from TempleOSRS and show in tooltips"
    )
    default boolean showCollectionLog()
    {
        return true;
    }

    @ConfigItem(
        keyName = "fourTwentyMode",
        name = "420 Mode",
        description = "OFF: normal KC. ON: exact 420 KC turns green. CAP: KC >= 420 shows '420' in green."
    )
    default FourTwentyMode fourTwentyMode()
    {
        return FourTwentyMode.ON;
    }
}
