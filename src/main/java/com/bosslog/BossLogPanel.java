package com.bosslog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class BossLogPanel extends PluginPanel
{
    private static final Color GOLD = new Color(76, 175, 110);
    private static final Color BG_DARK = new Color(6, 10, 7);
    private static final Color TEXT_DIM = new Color(160, 200, 160);

    private final HiscoreService hiscoreService;

    private final JTextField playerInput = new JTextField();
    private final JButton lookupButton = new JButton("Lookup");
    private final JLabel statusLabel = new JLabel(" ");
    private final JPanel resultsPanel = new JPanel();

    @Inject
    public BossLogPanel(HiscoreService hiscoreService)
    {
        super(false);
        this.hiscoreService = hiscoreService;

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
        resultsPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
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
        lookupButton.setEnabled(false);
        resultsPanel.removeAll();
        resultsPanel.revalidate();
        resultsPanel.repaint();

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

                renderBossGrid(result);
            })
        ).exceptionally(ex ->
        {
            SwingUtilities.invokeLater(() ->
            {
                lookupButton.setEnabled(true);
                statusLabel.setText("Lookup failed: " + ex.getMessage());
            });
            return null;
        });
    }

    private void renderBossGrid(HiscoreResult result)
    {
        resultsPanel.removeAll();

        Map<String, Integer> bosses = result.getBossKills();
        int killed = (int) bosses.values().stream().filter(kc -> kc > 0).count();
        int total = bosses.size();

        JLabel summary = new JLabel(killed + " / " + total + " bosses killed");
        summary.setFont(FontManager.getRunescapeSmallFont());
        summary.setForeground(GOLD);
        summary.setAlignmentX(Component.CENTER_ALIGNMENT);
        summary.setBorder(new EmptyBorder(0, 0, 8, 0));
        resultsPanel.add(summary);

        // Grid of boss cards — 3 columns
        JPanel grid = new JPanel(new GridLayout(0, 3, 3, 3));
        grid.setBackground(ColorScheme.DARK_GRAY_COLOR);

        for (Map.Entry<String, Integer> entry : bosses.entrySet())
        {
            grid.add(createBossCard(entry.getKey(), entry.getValue()));
        }

        resultsPanel.add(grid);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel createBossCard(String bossName, int kc)
    {
        boolean hasKc = kc > 0;
        boolean is420 = kc > 0 && String.valueOf(kc).contains("420");

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(hasKc ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_HOVER_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
            new EmptyBorder(4, 2, 4, 2)
        ));
        card.setToolTipText(bossName + (kc > 0 ? " — " + kc + " kc" : ""));

        // KC number only — centered, white text, green if ends in 420
        String kcText = kc < 0 ? "-" : String.valueOf(kc);
        JLabel kcLabel = new JLabel(kcText, SwingConstants.CENTER);
        kcLabel.setFont(FontManager.getRunescapeBoldFont());
        if (is420)
        {
            kcLabel.setForeground(GOLD);
        }
        else
        {
            kcLabel.setForeground(hasKc ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
        }
        card.add(kcLabel, BorderLayout.CENTER);

        return card;
    }
}
