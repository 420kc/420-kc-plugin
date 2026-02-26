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
        keyName = "showCollectionLog",
        name = "Show Collection Log",
        description = "<html>Fetch collection log from TempleOSRS and show in tooltips.<br><br>"
            + "<b>To sync your collection log:</b><br>"
            + "1. Install the 'TempleOSRS' plugin from the Plugin Hub<br>"
            + "2. Open your collection log in-game<br>"
            + "3. Click the sync button in the top-right corner<br><br>"
            + "Players without a TempleOSRS profile will show KC only.</html>"
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

    @ConfigItem(
        keyName = "playerMenuLookup",
        name = "Player Menu Lookup",
        description = "Add '420 kc Lookup' to right-click menu on players"
    )
    default boolean playerMenuLookup()
    {
        return true;
    }

    @ConfigItem(
        keyName = "chatMessages",
        name = "420 Chat Messages",
        description = "Show a green chat message when your boss KC contains 420"
    )
    default boolean chatMessages()
    {
        return true;
    }
}
