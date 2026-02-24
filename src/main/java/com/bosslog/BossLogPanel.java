package com.bosslog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
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

    // Map HiscoreSkill name -> boss name as it appears in hiscore CSV data
    // Most names match, these handle the mismatches
    private static final Map<String, String> NAME_OVERRIDES = new LinkedHashMap<>();
    static
    {
        NAME_OVERRIDES.put("Calvar'ion", "Cal'varion");
        NAME_OVERRIDES.put("The Hueycoatl", "Hueycoatl");
        NAME_OVERRIDES.put("The Royal Titans", "Royal Titans");
        NAME_OVERRIDES.put("Doom of Mokhaiotl", "Doom of Mokhaiotl");
        NAME_OVERRIDES.put("Shellbane Gryphon", "Shellbane Gryphon");
        NAME_OVERRIDES.put("Yama", "Yama");
    }

    private final HiscoreService hiscoreService;
    private final SpriteManager spriteManager;

    private final JTextField playerInput = new JTextField();
    private final JButton lookupButton = new JButton("Lookup");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel resultsPanel = new JPanel();

    // Track labels for updating after lookup
    private final Map<HiscoreSkill, JLabel> bossLabels = new LinkedHashMap<>();

    @Inject
    public BossLogPanel(HiscoreService hiscoreService, SpriteManager spriteManager)
    {
        super(false);
        this.hiscoreService = hiscoreService;
        this.spriteManager = spriteManager;

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

        // Reset all labels to "--"
        for (JLabel label : bossLabels.values())
        {
            label.setText(pad("--"));
            label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        }

        hiscoreService.lookup(player).thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                lookupButton.setEnabled(true);

                if (result == null)
                {
                    statusLabel.setText("Player not found");
                    return;
                }

                statusLabel.setText(result.getAccountType().getLabel()
                    + " | Total: " + result.getTotalLevel());
                statusLabel.setForeground(result.getAccountType().getColor());

                updateBossLabels(result);
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

            // Update tooltip with KC
            String tooltip = skill.getName();
            if (hasKc)
            {
                tooltip += " — " + kc + " kc";
            }
            label.setToolTipText(tooltip);
        }
    }
}
