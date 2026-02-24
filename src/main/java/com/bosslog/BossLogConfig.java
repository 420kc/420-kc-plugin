package com.bosslog;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("420kc")
public interface BossLogConfig extends Config
{
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
}
