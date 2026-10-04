package com.osiris.autoplug.client.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Original, resolution-independent block landscape and translucent dashboard surfaces. */
final class DashboardTheme {
    static final int ARC = 16;

    private DashboardTheme() {}

    static void installDefaults() {
        UIManager.put("Button.arc", ARC);
        UIManager.put("Component.arc", ARC);
        UIManager.put("TextComponent.arc", ARC);
        UIManager.put("ProgressBar.arc", ARC);
    }

    static boolean dark() {
        Color background = UIManager.getColor("Panel.background");
        return background != null && background.getRed() + background.getGreen() + background.getBlue() < 420;
    }

    static Color muted() { return dark() ? new Color(0xB6C6CD) : new Color(0x4A606A); }
    static Color accent() { return dark() ? new Color(0xA3D7BF) : new Color(0x286C52); }

    static void tint(JComponent component, boolean accent) {
        component.putClientProperty("AutoPlug.accent", accent);
        component.setForeground(accent ? accent() : muted());
    }

    static void refreshColors(Component component) {
        if (component instanceof JComponent) {
            Object accent = ((JComponent) component).getClientProperty("AutoPlug.accent");
            if (accent instanceof Boolean) component.setForeground((Boolean) accent ? accent() : muted());
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

    /** Paints only geometric artwork authored here; no Minecraft assets or remote images. */
    static void landscape(Graphics graphics, int width, int height) {
        if (width <= 0 || height <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.scale(width / 1200.0, height / 800.0);
            g.setPaint(new GradientPaint(0, 0, new Color(0x101E2C), 0, 800, new Color(0x496065)));
            g.fillRect(0, 0, 1200, 800);
            // Sparse square stars and a pale block moon keep the sky quiet behind the content.
            g.setColor(new Color(207, 224, 226, 62));
            int[][] stars = {{220, 86}, {392, 160}, {595, 94}, {726, 208}, {919, 100}, {1094, 184}};
            for (int[] star : stars) g.fillRect(star[0], star[1], 3, 3);
            g.setColor(new Color(182, 204, 208, 82)); g.fillRect(996, 83, 56, 56);
            g.setColor(new Color(31, 47, 61, 110)); g.fillRect(1014, 83, 38, 38);
            ridge(g, new Color(0x344B55), 800, new int[]{0, 180, 320, 470, 640, 830, 990, 1200},
                    new int[]{400, 360, 420, 310, 366, 290, 360, 336});
            ridge(g, new Color(0x293F46), 800, new int[]{0, 140, 290, 420, 590, 730, 910, 1080, 1200},
                    new int[]{480, 440, 504, 466, 534, 458, 492, 436, 480});
            ridge(g, new Color(0x213A36), 800, new int[]{0, 154, 310, 492, 680, 870, 1032, 1200},
                    new int[]{628, 560, 606, 646, 594, 554, 612, 584});
            // Top faces, rock layers and small block trees suggest a playable landscape.
            g.setColor(new Color(0x3B5647));
            g.fillRect(0, 628, 154, 10); g.fillRect(154, 560, 156, 10);
            g.fillRect(870, 554, 162, 10); g.fillRect(1032, 612, 168, 10);
            g.setColor(new Color(0x263B38));
            g.fillRect(46, 698, 170, 28); g.fillRect(970, 710, 230, 22);
            tree(g, 194, 560, 1.0); tree(g, 929, 554, 1.3); tree(g, 1112, 612, .85);
            g.setPaint(new GradientPaint(0, 0, new Color(4, 12, 19, 66), 0, 800, new Color(4, 12, 19, 22)));
            g.fillRect(0, 0, 1200, 800);
        } finally { g.dispose(); }
    }

    private static void ridge(Graphics2D g, Color color, int bottom, int[] x, int[] y) {
        Polygon polygon = new Polygon(); polygon.addPoint(x[0], bottom); polygon.addPoint(x[0], y[0]);
        for (int i = 1; i < x.length; i++) { polygon.addPoint(x[i], y[i - 1]); polygon.addPoint(x[i], y[i]); }
        polygon.addPoint(x[x.length - 1], bottom); g.setColor(color); g.fillPolygon(polygon);
    }

    private static void tree(Graphics2D graphics, int x, int ground, double scale) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, ground); g.scale(scale, scale);
            g.setColor(new Color(0x29352F)); g.fillRect(-5, -58, 10, 58);
            g.setColor(new Color(0x1D3430)); g.fillRect(-30, -86, 60, 35); g.fillRect(-21, -105, 42, 24);
            g.setColor(new Color(0x30483A)); g.fillRect(-21, -105, 42, 5); g.fillRect(-30, -86, 25, 5);
        } finally { g.dispose(); }
    }

    static JLabel worldThumbnail() {
        JLabel label = new JLabel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
                    landscape(g, getWidth(), getHeight()); super.paintComponent(g);
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
                Color top = dark() ? new Color(34, 47, 54, 232) : new Color(246, 250, 250, 237);
                Color bottom = dark() ? new Color(23, 35, 42, 221) : new Color(230, 239, 239, 224);
                g.setPaint(new GradientPaint(0, 0, top, 0, Math.max(1, getHeight()), bottom));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                g.setColor(dark() ? new Color(215, 235, 237, 36) : new Color(255, 255, 255, 155));
                g.draw(new RoundRectangle2D.Float(.5f, .5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC));
            } finally { g.dispose(); }
            super.paintComponent(graphics);
        }
    }
}
