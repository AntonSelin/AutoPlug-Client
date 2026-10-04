package com.osiris.autoplug.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Neutral dashboard surfaces with a consistent blue accent in light and dark themes. */
final class DashboardTheme {
    static final int ARC = 16;
    private static final Color BLUE = new Color(0x0066FF);

    private DashboardTheme() {}

    static void installDefaults() {
        UIManager.put("Button.arc", ARC);
        UIManager.put("Component.arc", ARC);
        UIManager.put("TextComponent.arc", ARC);
        UIManager.put("ProgressBar.arc", ARC);
        UIManager.put("Component.focusColor", BLUE);
        UIManager.put("Component.focusedBorderColor", BLUE);
        UIManager.put("Button.focusedBorderColor", BLUE);
        UIManager.put("ToggleButton.selectedBackground", BLUE);
        UIManager.put("ToggleButton.selectedForeground", Color.WHITE);
        UIManager.put("ToggleButton.focusedBorderColor", BLUE);
        UIManager.put("TabbedPane.underlineColor", BLUE);
        UIManager.put("ProgressBar.foreground", BLUE);
        UIManager.put("CheckBox.icon.selectedBackground", BLUE);
        UIManager.put("CheckBox.icon.selectedBorderColor", BLUE);
        UIManager.put("CheckBox.icon.checkmarkColor", Color.WHITE);
        UIManager.put("Table.selectionBackground", BLUE);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", new ColorUIResource(dark() ? 0x2B3443 : 0xF3F6FB));
        UIManager.put("TableHeader.foreground", new ColorUIResource(dark() ? 0xE4EAF5 : 0x34435A));
        UIManager.put("Table.gridColor", new ColorUIResource(dark() ? 0x3A4555 : 0xE4EAF3));
    }

    static boolean dark() {
        Color background = UIManager.getColor("Panel.background");
        return background != null && background.getRed() + background.getGreen() + background.getBlue() < 420;
    }

    static Color muted() { return dark() ? new Color(0xBCC8DA) : new Color(0x526176); }
    static Color accent() { return dark() ? new Color(0x8AB8FF) : BLUE; }

    static JButton primary(JButton button) {
        button.setBackground(BLUE); button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }

    static void navigation(JToggleButton button) {
        button.putClientProperty("AutoPlug.navigation", Boolean.TRUE);
        button.addItemListener(event -> refreshNavigation(button));
        refreshNavigation(button);
    }

    private static void refreshNavigation(JToggleButton button) {
        button.setBackground(button.isSelected() ? BLUE : (dark() ? new Color(0x273142) : new Color(0xF6F8FC)));
        button.setForeground(button.isSelected() ? Color.WHITE : (dark() ? new Color(0xE4EAF5) : new Color(0x34435A)));
    }

    static JLabel badge(String text) {
        JLabel label = new JLabel(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(dark() ? new Color(36, 75, 128, 150) : new Color(0xEAF2FF));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                } finally { g.dispose(); }
                super.paintComponent(graphics);
            }
        };
        label.setBorder(new EmptyBorder(2, 8, 2, 8)); tint(label, true);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f)); return label;
    }

    static void tint(JComponent component, boolean accent) {
        component.putClientProperty("AutoPlug.accent", accent);
        component.setForeground(accent ? accent() : muted());
    }

    static void refreshColors(Component component) {
        if (component instanceof JComponent) {
            Object accent = ((JComponent) component).getClientProperty("AutoPlug.accent");
            if (accent instanceof Boolean) component.setForeground((Boolean) accent ? accent() : muted());
            if (component instanceof JToggleButton && Boolean.TRUE.equals(((JComponent) component).getClientProperty("AutoPlug.navigation")))
                refreshNavigation((JToggleButton) component);
        }
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) refreshColors(child);
    }

    static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }

    static JPanel surface(LayoutManager layout, int padding) {
        JPanel panel = new Surface(layout);
        panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    static void canvas(Graphics graphics, int width, int height) {
        if (width <= 0 || height <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(dark() ? new Color(0x181E29) : new Color(0xF5F7FB));
            g.fillRect(0, 0, width, height);
        } finally { g.dispose(); }
    }

    static JLabel worldThumbnail() {
        JLabel label = new JLabel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                    g.setColor(dark() ? new Color(0x263750) : new Color(0xEEF4FF));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (getIcon() == null) {
                        // A neutral world symbol while no saved thumbnail is available.
                        g.setColor(accent()); g.setStroke(new BasicStroke(1.7f));
                        int x = getWidth() / 2 - 21, y = getHeight() / 2 - 21;
                        g.drawOval(x, y, 42, 42); g.drawOval(x + 11, y, 20, 42);
                        g.drawLine(x, y + 21, x + 42, y + 21);
                        g.drawArc(x + 3, y + 6, 36, 12, 180, 180);
                        g.drawArc(x + 3, y + 25, 36, 12, 0, 180);
                    }
                    super.paintComponent(g);
                } finally { g.dispose(); }
            }
        };
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(96, 96)); return label;
    }

    private static final class Surface extends JPanel {
        Surface(LayoutManager layout) { super(layout); setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = dark() ? new Color(39, 49, 65, 242) : new Color(255, 255, 255, 245);
                Color bottom = dark() ? new Color(32, 41, 56, 230) : new Color(250, 252, 255, 225);
                g.setPaint(new GradientPaint(0, 0, top, 0, Math.max(1, getHeight()), bottom));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                g.setColor(dark() ? new Color(149, 171, 204, 55) : new Color(209, 219, 234, 190));
                g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC));
            } finally { g.dispose(); }
            super.paintComponent(graphics);
        }
    }
}
