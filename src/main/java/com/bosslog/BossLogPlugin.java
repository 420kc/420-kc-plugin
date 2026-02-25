package com.bosslog;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
    name = "420 kc",
    description = "Boss KC tracker with parallelized account type detection and 420 celebrations",
    tags = {"boss", "kc", "hiscore", "ironman", "pvm", "420"}
)
public class BossLogPlugin extends Plugin
{
    private static final Pattern KC_PATTERN = Pattern.compile(
        "Your (.+) kill count is: (\\d+)"
    );

    @Inject
    private Client client;

    @Inject
    private BossLogConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private BossLogPanel panel;

    private NavigationButton navButton;

    @Provides
    BossLogConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BossLogConfig.class);
    }

    @Override
    protected void startUp()
    {
        navButton = NavigationButton.builder()
            .tooltip("420 kc")
            .icon(getIcon())
            .priority(6)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);

        String defaultPlayer = config.defaultPlayer();
        if (!defaultPlayer.isEmpty())
        {
            panel.setPlayerName(defaultPlayer);
            panel.doLookup();
        }

        log.info("420 kc plugin started");
    }

    @Override
    protected void shutDown()
    {
        clientToolbar.removeNavigation(navButton);
        log.info("420 kc plugin stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            if (config.defaultPlayer().isEmpty())
            {
                Player local = client.getLocalPlayer();
                if (local != null && local.getName() != null)
                {
                    panel.setPlayerName(local.getName());
                }
            }
        }
    }

    /**
     * Right-click player menu: "420 kc Lookup"
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        MenuEntry[] entries = event.getMenuEntries();
        for (MenuEntry entry : entries)
        {
            // Look for player menu entries (Walk here, Trade, Follow, etc.)
            if (entry.getType() == MenuAction.WALK
                || entry.getType() == MenuAction.PLAYER_FIRST_OPTION
                || entry.getType() == MenuAction.PLAYER_SECOND_OPTION
                || entry.getType() == MenuAction.PLAYER_THIRD_OPTION
                || entry.getType() == MenuAction.PLAYER_FOURTH_OPTION
                || entry.getType() == MenuAction.PLAYER_FIFTH_OPTION
                || entry.getType() == MenuAction.PLAYER_SIXTH_OPTION
                || entry.getType() == MenuAction.PLAYER_SEVENTH_OPTION
                || entry.getType() == MenuAction.PLAYER_EIGHTH_OPTION)
            {
                String target = entry.getTarget();
                if (target != null && !target.isEmpty())
                {
                    String playerName = Text.removeTags(target).trim();
                    if (!playerName.isEmpty())
                    {
                        addLookupMenuEntry(entries, playerName);
                        return;
                    }
                }
            }
        }
    }

    private void addLookupMenuEntry(MenuEntry[] existing, String playerName)
    {
        client.createMenuEntry(1)
            .setOption("<col=00ff00>420 kc</col> Lookup")
            .setTarget("<col=ffffff>" + playerName + "</col>")
            .setType(MenuAction.RUNELITE)
            .onClick(e ->
            {
                panel.setPlayerName(playerName);
                panel.doLookup();
            });
    }

    /**
     * Detect boss KC messages containing "420" and celebrate.
     * OSRS sends: "Your [boss] kill count is: [number]."
     */
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        String msg = Text.removeTags(event.getMessage());
        Matcher matcher = KC_PATTERN.matcher(msg);
        if (matcher.find())
        {
            String bossName = matcher.group(1);
            String kcStr = matcher.group(2);

            if (kcStr.contains("420"))
            {
                String blazeMsg = "420 kc: " + bossName + " " + kcStr + " kc — Blaze it!";
                client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    "<col=4caf6e>" + blazeMsg + "</col>",
                    ""
                );
            }
        }
    }

    private BufferedImage getIcon()
    {
        return ImageUtil.loadImageResource(getClass(), "icon.png");
    }
}
