package com.bosslog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;

public class BossLogPanel extends PluginPanel
{
    private static final Color GOLD = new Color(76, 175, 110);
    private static final Color TEXT_DIM = new Color(160, 200, 160);

    // Boss display order matching vanilla RuneLite hiscores
    private static final HiscoreSkill[] BOSSES = {
        HiscoreSkill.ABYSSAL_SIRE,
        HiscoreSkill.ALCHEMICAL_HYDRA,
        HiscoreSkill.AMOXLIATL,
        HiscoreSkill.ARAXXOR,
        HiscoreSkill.ARTIO,
        HiscoreSkill.BARROWS_CHESTS,
        HiscoreSkill.BRYOPHYTA,
        HiscoreSkill.CALLISTO,
        HiscoreSkill.CALVARION,
        HiscoreSkill.CERBERUS,
        HiscoreSkill.CHAMBERS_OF_XERIC,
        HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE,
        HiscoreSkill.CHAOS_ELEMENTAL,
        HiscoreSkill.CHAOS_FANATIC,
        HiscoreSkill.COMMANDER_ZILYANA,
        HiscoreSkill.CORPOREAL_BEAST,
        HiscoreSkill.CRAZY_ARCHAEOLOGIST,
        HiscoreSkill.DAGANNOTH_PRIME,
        HiscoreSkill.DAGANNOTH_REX,
        HiscoreSkill.DAGANNOTH_SUPREME,
        HiscoreSkill.DERANGED_ARCHAEOLOGIST,
        HiscoreSkill.DOOM_OF_MOKHAIOTL,
        HiscoreSkill.DUKE_SUCELLUS,
        HiscoreSkill.GENERAL_GRAARDOR,
        HiscoreSkill.GIANT_MOLE,
        HiscoreSkill.GROTESQUE_GUARDIANS,
        HiscoreSkill.HESPORI,
        HiscoreSkill.THE_HUEYCOATL,
        HiscoreSkill.KALPHITE_QUEEN,
        HiscoreSkill.KING_BLACK_DRAGON,
        HiscoreSkill.KRAKEN,
        HiscoreSkill.KREEARRA,
        HiscoreSkill.KRIL_TSUTSAROTH,
        HiscoreSkill.LUNAR_CHESTS,
        HiscoreSkill.MIMIC,
        HiscoreSkill.NEX,
        HiscoreSkill.NIGHTMARE,
        HiscoreSkill.PHOSANIS_NIGHTMARE,
        HiscoreSkill.OBOR,
        HiscoreSkill.PHANTOM_MUSPAH,
        HiscoreSkill.THE_ROYAL_TITANS,
        HiscoreSkill.SARACHNIS,
        HiscoreSkill.SCORPIA,
        HiscoreSkill.SCURRIUS,
        HiscoreSkill.SHELLBANE_GRYPHON,
        HiscoreSkill.SKOTIZO,
        HiscoreSkill.SOL_HEREDIT,
        HiscoreSkill.SPINDEL,
        HiscoreSkill.TEMPOROSS,
        HiscoreSkill.THE_GAUNTLET,
        HiscoreSkill.THE_CORRUPTED_GAUNTLET,
        HiscoreSkill.THE_LEVIATHAN,
        HiscoreSkill.THE_WHISPERER,
        HiscoreSkill.THEATRE_OF_BLOOD,
        HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE,
        HiscoreSkill.THERMONUCLEAR_SMOKE_DEVIL,
        HiscoreSkill.TOMBS_OF_AMASCUT,
        HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT,
        HiscoreSkill.TZKAL_ZUK,
        HiscoreSkill.TZTOK_JAD,
        HiscoreSkill.VARDORVIS,
        HiscoreSkill.VENENATIS,
        HiscoreSkill.VETION,
        HiscoreSkill.VORKATH,
        HiscoreSkill.WINTERTODT,
        HiscoreSkill.YAMA,
        HiscoreSkill.ZALCANO,
        HiscoreSkill.ZULRAH,
    };

    // Map HiscoreSkill.getName() -> boss name as it appears in hiscore CSV data
    private static final Map<String, String> NAME_OVERRIDES = new LinkedHashMap<>();
    static
    {
        NAME_OVERRIDES.put("Calvar'ion", "Cal'varion");
    }

    private final HiscoreService hiscoreService;
    private final ClogService clogService;
    private final BossLogConfig config;
    private final SpriteManager spriteManager;

    private final JTextField playerInput = new JTextField();
    private final JButton lookupButton = new JButton("Lookup");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel resultsPanel = new JPanel();

    // Track labels for updating after lookup
    private final Map<HiscoreSkill, JLabel> bossLabels = new LinkedHashMap<>();

    // Current lookup state
    private HiscoreResult hiscoreResult;
    private ClogResult clogResult;

    @Inject
    public BossLogPanel(HiscoreService hiscoreService, ClogService clogService,
                        BossLogConfig config, SpriteManager spriteManager)
    {
        super(false);
        this.hiscoreService = hiscoreService;
        this.clogService = clogService;
        this.config = config;
        this.spriteManager = spriteManager;

        // Keep tooltips visible longer for reading item lists
        ToolTipManager.sharedInstance().setDismissDelay(15000);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildSearchPanel(), BorderLayout.NORTH);
        add(buildResultsScroll(), BorderLayout.CENTER);
    }

    private JPanel buildSearchPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 5, 10));

        // Title
        JLabel title = new JLabel("420 kc");
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
        title.setForeground(GOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        // Search row
        JPanel searchRow = new JPanel(new BorderLayout(5, 0));
        searchRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

        playerInput.setToolTipText("Player name");
        playerInput.addActionListener(e -> doLookup());
        searchRow.add(playerInput, BorderLayout.CENTER);

        lookupButton.addActionListener(e -> doLookup());
        searchRow.add(lookupButton, BorderLayout.EAST);

        panel.add(searchRow);
        panel.add(Box.createVerticalStrut(4));

        // Status
        statusLabel.setFont(FontManager.getRunescapeSmallFont());
        statusLabel.setForeground(TEXT_DIM);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(statusLabel);

        return panel;
    }

    private JScrollPane buildResultsScroll()
    {
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        resultsPanel.setBorder(new EmptyBorder(5, 5, 10, 5));

        // Build the static boss grid immediately (shows "--" until lookup)
        buildBossGrid();

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private void buildBossGrid()
    {
        JPanel grid = new JPanel(new GridLayout(0, 3));
        grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        for (HiscoreSkill boss : BOSSES)
        {
            JPanel cell = makeBossCell(boss);
            grid.add(cell);
        }

        resultsPanel.add(grid);
    }

    private JPanel makeBossCell(HiscoreSkill boss)
    {
        JLabel label = new JLabel();
        label.setToolTipText(boss.getName());
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setText(pad("--"));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setIconTextGap(4);

        // Load boss sprite asynchronously
        spriteManager.getSpriteAsync(boss.getSpriteId(), 0, sprite ->
            SwingUtilities.invokeLater(() ->
            {
                BufferedImage scaled = ImageUtil.resizeImage(
                    ImageUtil.resizeCanvas(sprite, 25, 25), 20, 20);
                label.setIcon(new ImageIcon(scaled));
            }));

        bossLabels.put(boss, label);

        JPanel cell = new JPanel();
        cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cell.setBorder(new EmptyBorder(2, 0, 2, 0));
        cell.add(label);

        return cell;
    }

    private static String pad(String text)
    {
        return StringUtils.leftPad(text, 4);
    }

    public void setPlayerName(String name)
    {
        playerInput.setText(name);
    }

    public void doLookup()
    {
        String player = playerInput.getText().trim();
        if (player.isEmpty())
        {
            statusLabel.setText("Enter a player name");
            return;
        }

        statusLabel.setText("Looking up " + player + "...");
        statusLabel.setForeground(TEXT_DIM);
        lookupButton.setEnabled(false);

        // Clear previous results
        hiscoreResult = null;
        clogResult = null;

        // Reset all labels to "--"
        for (JLabel label : bossLabels.values())
        {
            label.setText(pad("--"));
            label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            label.setToolTipText(null);
        }

        // Fire hiscore lookup
        hiscoreService.lookup(player).thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                lookupButton.setEnabled(true);

                if (result == null)
                {
                    statusLabel.setText("Player not found");
                    return;
                }

                hiscoreResult = result;

                statusLabel.setText(result.getAccountType().getLabel()
                    + " | Total: " + result.getTotalLevel());
                statusLabel.setForeground(result.getAccountType().getColor());

                updateBossLabels(result);
                updateTooltips();
            })
        ).exceptionally(ex ->
        {
            SwingUtilities.invokeLater(() ->
            {
                lookupButton.setEnabled(true);
                statusLabel.setText("Lookup failed");
                statusLabel.setForeground(TEXT_DIM);
            });
            return null;
        });

        // Fire clog lookup in parallel (if enabled)
        if (config.showCollectionLog())
        {
            clogService.lookup(player).thenAccept(result ->
                SwingUtilities.invokeLater(() ->
                {
                    clogResult = result;
                    updateTooltips();
                })
            ).exceptionally(ex ->
            {
                // Clog failure is non-fatal, tooltips just won't show items
                return null;
            });
        }
    }

    private void updateBossLabels(HiscoreResult result)
    {
        Map<String, Integer> bosses = result.getBossKills();

        for (Map.Entry<HiscoreSkill, JLabel> entry : bossLabels.entrySet())
        {
            HiscoreSkill skill = entry.getKey();
            JLabel label = entry.getValue();

            // Resolve the boss name used in hiscore data
            String hiscoreName = NAME_OVERRIDES.getOrDefault(skill.getName(), skill.getName());
            int kc = bosses.getOrDefault(hiscoreName, -1);

            boolean hasKc = kc > 0;
            boolean is420 = hasKc && String.valueOf(kc).contains("420");

            String kcText = kc <= 0 ? "--" : String.valueOf(kc);
            label.setText(pad(kcText));

            if (is420)
            {
                label.setForeground(GOLD);
            }
            else if (hasKc)
            {
                label.setForeground(Color.WHITE);
            }
            else
            {
                label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            }
        }
    }

    /**
     * Build HTML tooltips for each boss cell.
     * If clog data is available, shows obtained/missing items.
     * Otherwise falls back to simple "Boss Name - X kc" format.
     */
    private void updateTooltips()
    {
        for (Map.Entry<HiscoreSkill, JLabel> entry : bossLabels.entrySet())
        {
            HiscoreSkill skill = entry.getKey();
            JLabel label = entry.getValue();

            String bossName = skill.getName();
            String hiscoreName = NAME_OVERRIDES.getOrDefault(bossName, bossName);

            // Get KC from hiscore result
            int kc = -1;
            if (hiscoreResult != null)
            {
                kc = hiscoreResult.getKc(hiscoreName);
            }

            // If no clog data or config disabled, show simple tooltip
            if (clogResult == null || !config.showCollectionLog())
            {
                String tooltip = bossName;
                if (kc > 0)
                {
                    tooltip += " \u2014 " + kc + " kc";
                }
                label.setToolTipText(tooltip);
                continue;
            }

            // Build rich HTML tooltip with collection log items
            String category = ClogService.bossToCategory(hiscoreName);

            List<ClogResult.ClogItem> obtained = clogResult.getObtainedItems().get(category);
            List<Integer> allItems = clogResult.getCategoryItems().get(category);

            // No clog data for this boss
            if ((obtained == null || obtained.isEmpty()) && (allItems == null || allItems.isEmpty()))
            {
                String tooltip = bossName;
                if (kc > 0)
                {
                    tooltip += " \u2014 " + kc + " kc";
                }
                label.setToolTipText(tooltip);
                continue;
            }

            // Build obtained ID set for quick lookup
            Set<Integer> obtainedIds = new HashSet<>();
            Map<Integer, Integer> obtainedCounts = new LinkedHashMap<>();
            if (obtained != null)
            {
                for (ClogResult.ClogItem item : obtained)
                {
                    obtainedIds.add(item.getId());
                    obtainedCounts.put(item.getId(), item.getCount());
                }
            }

            int totalItems = allItems != null ? allItems.size() : obtainedIds.size();
            int obtainedCount = 0;

            // Count obtained from the full item list
            if (allItems != null)
            {
                for (int itemId : allItems)
                {
                    if (obtainedIds.contains(itemId))
                    {
                        obtainedCount++;
                    }
                }
            }
            else
            {
                obtainedCount = obtainedIds.size();
            }

            boolean isComplete = totalItems > 0 && obtainedCount == totalItems;

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='padding:4px;'>");

            // Header: Boss Name (obtained/total)
            String headerColor = isComplete ? "#4caf6e" : "#ffffff";
            html.append("<b style='color:").append(headerColor).append(";'>");
            html.append(escapeHtml(bossName));
            html.append(" (").append(obtainedCount).append("/").append(totalItems).append(")");
            html.append("</b>");

            if (kc > 0)
            {
                html.append("<span style='color:#a0c8a0;'> \u2014 ").append(kc).append(" kc</span>");
            }

            html.append("<br>");

            // Item list
            if (allItems != null)
            {
                for (int itemId : allItems)
                {
                    boolean hasItem = obtainedIds.contains(itemId);
                    String itemName = clogResult.getItemName(itemId);

                    if (hasItem)
                    {
                        int count = obtainedCounts.getOrDefault(itemId, 1);
                        html.append("<span style='color:#4caf6e;'>\u2713 ");
                        html.append(escapeHtml(itemName));
                        if (count > 1)
                        {
                            html.append(" (x").append(count).append(")");
                        }
                        html.append("</span><br>");
                    }
                    else
                    {
                        html.append("<span style='color:#ff6666;'>\u2717 ");
                        html.append(escapeHtml(itemName));
                        html.append("</span><br>");
                    }
                }
            }
            else if (obtained != null)
            {
                // No category data, just show obtained items
                for (ClogResult.ClogItem item : obtained)
                {
                    String itemName = clogResult.getItemName(item.getId());
                    html.append("<span style='color:#4caf6e;'>\u2713 ");
                    html.append(escapeHtml(itemName));
                    if (item.getCount() > 1)
                    {
                        html.append(" (x").append(item.getCount()).append(")");
                    }
                    html.append("</span><br>");
                }
            }

            html.append("</body></html>");
            label.setToolTipText(html.toString());
        }
    }

    private static String escapeHtml(String text)
    {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
