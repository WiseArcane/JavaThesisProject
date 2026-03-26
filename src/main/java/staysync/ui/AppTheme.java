package staysync.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

import staysync.core.TenantAccount.PaymentStatus;

public final class AppTheme {
    public enum Mode {
        LIGHT,
        DARK
    }

    public static Color FRAME_BACKGROUND;
    public static Color SURFACE_BACKGROUND;
    public static Color MUTED_SURFACE;
    public static Color SIDEBAR_BACKGROUND;
    public static Color PRIMARY_COLOR;
    public static Color PRIMARY_SOFT;
    public static Color ACCENT_COLOR;
    public static Color HERO_TINT;
    public static Color BORDER_COLOR;
    public static Color FOCUS_BORDER_COLOR;
    public static Color TEXT_COLOR;
    public static Color MUTED_TEXT_COLOR;
    public static Color CAPTION_COLOR;
    public static Color SUCCESS_COLOR;
    public static Color WARNING_COLOR;
    public static Color ERROR_COLOR;
    public static Color SUCCESS_BACKGROUND;
    public static Color WARNING_BACKGROUND;
    public static Color ERROR_BACKGROUND;
    public static Color SHADOW_COLOR;

    public static final Font TITLE_FONT = new Font("Consolas", Font.BOLD, 28);
    public static final Font HEADER_FONT = new Font("Consolas", Font.BOLD, 20);
    public static final Font SUBHEADER_FONT = new Font("Consolas", Font.BOLD, 15);
    public static final Font LABEL_FONT = new Font("Consolas", Font.PLAIN, 13);
    public static final Font EMPHASIS_FONT = new Font("Consolas", Font.BOLD, 13);
    public static final Font CAPTION_FONT = new Font("Consolas", Font.PLAIN, 12);
    public static final Font BUTTON_FONT = new Font("Consolas", Font.BOLD, 13);

    private static Mode currentMode = Mode.LIGHT;

    static {
        applyMode(currentMode);
    }

    private AppTheme() {
    }

    public static void install() {
        applyMode(currentMode);
        UIManager.put("OptionPane.background", SURFACE_BACKGROUND);
        UIManager.put("Panel.background", FRAME_BACKGROUND);
        UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
        UIManager.put("OptionPane.messageFont", LABEL_FONT);
        UIManager.put("OptionPane.buttonFont", BUTTON_FONT);
        UIManager.put("Label.font", LABEL_FONT);
        UIManager.put("Button.font", BUTTON_FONT);
        UIManager.put("TextField.font", LABEL_FONT);
        UIManager.put("ComboBox.font", LABEL_FONT);
        UIManager.put("Table.font", LABEL_FONT);
        UIManager.put("TableHeader.font", EMPHASIS_FONT);
    }

    public static void toggleMode() {
        setMode(isDarkMode() ? Mode.LIGHT : Mode.DARK);
    }

    public static void setMode(Mode mode) {
        currentMode = mode;
        install();
    }

    public static boolean isDarkMode() {
        return currentMode == Mode.DARK;
    }

    public static String getModeActionLabel() {
        return isDarkMode() ? "Light Theme" : "Dark Theme";
    }

    public static JPanel createSurfacePanel() {
        JPanel panel = new RoundedPanel(SURFACE_BACKGROUND, BORDER_COLOR, 10);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JPanel createMutedPanel() {
        JPanel panel = new RoundedPanel(MUTED_SURFACE, blend(BORDER_COLOR, MUTED_SURFACE, 0.12), 10);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    public static JPanel createSidebarPanel() {
        JPanel panel = new RoundedPanel(SIDEBAR_BACKGROUND, blend(BORDER_COLOR, SIDEBAR_BACKGROUND, 0.08), 10);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    public static JPanel createTopBarPanel() {
        JPanel panel = new RoundedPanel(MUTED_SURFACE, blend(BORDER_COLOR, MUTED_SURFACE, 0.1), 10);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));
        return panel;
    }

    public static JPanel createHeroPanel() {
        JPanel panel = new HeroPanel(
                HERO_TINT,
                blend(HERO_TINT, PRIMARY_SOFT, 0.2),
                blend(BORDER_COLOR, PRIMARY_COLOR, 0.14),
                10);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JLabel createEyebrowLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(CAPTION_FONT);
        label.setForeground(CAPTION_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HEADER_FONT);
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createSubheaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SUBHEADER_FONT);
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(CAPTION_FONT);
        label.setForeground(CAPTION_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createValueLabel() {
        JLabel label = new JLabel();
        label.setFont(EMPHASIS_FONT);
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createStatValueLabel() {
        JLabel label = new JLabel();
        label.setFont(TITLE_FONT.deriveFont(28f));
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JTextArea createDescriptionArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(LABEL_FONT);
        area.setForeground(MUTED_TEXT_COLOR);
        area.setBackground(new Color(0, 0, 0, 0));
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setOpaque(false);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    public static JPanel createTag(String text) {
        JPanel panel = new RoundedPanel(
                blend(PRIMARY_SOFT, SURFACE_BACKGROUND, 0.34),
                blend(BORDER_COLOR, PRIMARY_COLOR, 0.16),
                8);
        panel.setLayout(new BorderLayout());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(5, 8, 5, 8));

        JLabel label = new JLabel(text);
        label.setFont(EMPHASIS_FONT.deriveFont(12f));
        label.setForeground(blend(TEXT_COLOR, PRIMARY_COLOR, 0.12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel createStatusPill(String text, Color foreground, Color background) {
        JPanel panel = new RoundedPanel(background, blend(background, foreground, 0.14), 8);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 8, 5, 8));

        JLabel label = new JLabel(text);
        label.setFont(EMPHASIS_FONT.deriveFont(11f));
        label.setForeground(foreground);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = createFormLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        group.add(label);
        group.add(Box.createRigidArea(new Dimension(0, 6)));
        group.add(field);
        return group;
    }

    public static JPanel createFeedbackBanner() {
        JPanel panel = new RoundedPanel(MUTED_SURFACE, BORDER_COLOR, 8);
        panel.setLayout(new BorderLayout());
        panel.setVisible(false);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        return panel;
    }

    public static JButton createThemeToggleButton() {
        return createSecondaryButton(getModeActionLabel());
    }

    public static void applyFeedback(JPanel panel, JLabel label, Color foreground, Color background, String message) {
        label.setText(message);
        label.setForeground(foreground);
        panel.setBackground(background);
        panel.setVisible(message != null && !message.isBlank());
    }

    public static void styleTextField(JTextField field) {
        field.setFont(LABEL_FONT);
        field.setForeground(TEXT_COLOR);
        field.setBackground(blend(MUTED_SURFACE, SURFACE_BACKGROUND, 0.42));
        field.setCaretColor(TEXT_COLOR);
        field.setSelectionColor(blend(PRIMARY_SOFT, PRIMARY_COLOR, 0.42));
        field.setSelectedTextColor(TEXT_COLOR);
        field.setPreferredSize(new Dimension(320, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                new FocusLineBorder(FOCUS_BORDER_COLOR, BORDER_COLOR, 8),
                new EmptyBorder(9, 12, 9, 12)));
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(LABEL_FONT);
        comboBox.setForeground(TEXT_COLOR);
        comboBox.setBackground(blend(MUTED_SURFACE, SURFACE_BACKGROUND, 0.42));
        comboBox.setPreferredSize(new Dimension(320, 40));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                new FocusLineBorder(FOCUS_BORDER_COLOR, BORDER_COLOR, 8),
                new EmptyBorder(0, 8, 0, 8)));
    }

    public static JButton createPrimaryButton(String text) {
        JButton button = new RoundedButton(text);
        styleButton(button, blend(PRIMARY_SOFT, SURFACE_BACKGROUND, 0.46), TEXT_COLOR, blend(BORDER_COLOR, PRIMARY_COLOR, 0.42));
        return button;
    }

    public static JButton createSecondaryButton(String text) {
        JButton button = new RoundedButton(text);
        styleButton(button, SURFACE_BACKGROUND, TEXT_COLOR, blend(BORDER_COLOR, PRIMARY_SOFT, 0.14));
        return button;
    }

    public static JButton createGhostButton(String text) {
        JButton button = new RoundedButton(text);
        styleButton(
                button,
                new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 0),
                TEXT_COLOR,
                blend(BORDER_COLOR, PRIMARY_COLOR, 0.12));
        return button;
    }

    public static void makeButtonFillWidth(JButton button) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
    }

    public static void styleTable(JTable table) {
        table.setFont(LABEL_FONT);
        table.setForeground(TEXT_COLOR);
        table.setBackground(SURFACE_BACKGROUND);
        table.setSelectionBackground(blend(PRIMARY_SOFT, SURFACE_BACKGROUND, 0.3));
        table.setSelectionForeground(TEXT_COLOR);
        table.setRowHeight(36);
        table.setGridColor(blend(BORDER_COLOR, SURFACE_BACKGROUND, 0.55));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setFont(EMPHASIS_FONT);
        table.getTableHeader().setBackground(blend(SIDEBAR_BACKGROUND, MUTED_SURFACE, 0.36));
        table.getTableHeader().setForeground(TEXT_COLOR);
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public static JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new RoundedLineBorder(BORDER_COLOR, 18));
        scrollPane.getViewport().setBackground(SURFACE_BACKGROUND);
        scrollPane.setBackground(SURFACE_BACKGROUND);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        return scrollPane;
    }

    public static JScrollPane createPageScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(FRAME_BACKGROUND);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(scrollPane.getVerticalScrollBar());
        styleScrollBar(scrollPane.getHorizontalScrollBar());
        return scrollPane;
    }

    public static void addVerticalSpacing(JPanel panel, int height) {
        panel.add(Box.createRigidArea(new Dimension(0, height)));
    }

    public static void applyPageBackground(JComponent component) {
        component.setBackground(FRAME_BACKGROUND);
    }

    public static Color getPaymentStatusColor(PaymentStatus status) {
        if (status == PaymentStatus.PAID) {
            return SUCCESS_COLOR;
        }
        if (status == PaymentStatus.LATE) {
            return ERROR_COLOR;
        }
        return WARNING_COLOR;
    }

    public static Color getPaymentStatusBackground(PaymentStatus status) {
        if (status == PaymentStatus.PAID) {
            return SUCCESS_BACKGROUND;
        }
        if (status == PaymentStatus.LATE) {
            return ERROR_BACKGROUND;
        }
        return WARNING_BACKGROUND;
    }

    public static String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance().format(amount);
    }

    private static void styleButton(JButton button, Color background, Color foreground, Color borderColor) {
        button.setFont(BUTTON_FONT);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setRolloverEnabled(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(164, 40));
        button.setBorder(new EmptyBorder(10, 14, 10, 14));

        if (button instanceof RoundedButton roundedButton) {
            roundedButton.configure(
                    background,
                    shift(background, isDarkMode() ? 0.04 : -0.02),
                    shift(background, isDarkMode() ? 0.08 : -0.05),
                    foreground,
                    borderColor == null ? blend(background, PRIMARY_COLOR, 0.14) : borderColor);
        }
    }

    private static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setOpaque(false);
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = blend(PRIMARY_SOFT, BORDER_COLOR, 0.32);
                trackColor = new Color(0, 0, 0, 0);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected void paintTrack(Graphics graphics, JComponent component, java.awt.Rectangle trackBounds) {
            }

            @Override
            protected void paintThumb(Graphics graphics, JComponent component, java.awt.Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }

                Graphics2D graphics2d = (Graphics2D) graphics.create();
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setColor(thumbColor);
                graphics2d.fillRoundRect(
                        thumbBounds.x + 3,
                        thumbBounds.y + 3,
                        Math.max(6, thumbBounds.width - 6),
                        Math.max(18, thumbBounds.height - 6),
                        6,
                        6);
                graphics2d.dispose();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
    }

    private static void applyMode(Mode mode) {
        if (mode == Mode.DARK) {
            FRAME_BACKGROUND = new Color(28, 33, 27);
            SURFACE_BACKGROUND = new Color(38, 44, 35);
            MUTED_SURFACE = new Color(46, 53, 42);
            SIDEBAR_BACKGROUND = new Color(33, 39, 31);
            PRIMARY_COLOR = new Color(180, 189, 162);
            PRIMARY_SOFT = new Color(78, 89, 70);
            ACCENT_COLOR = new Color(154, 164, 138);
            HERO_TINT = new Color(42, 49, 38);
            BORDER_COLOR = new Color(91, 101, 81);
            FOCUS_BORDER_COLOR = new Color(196, 205, 176);
            TEXT_COLOR = new Color(220, 226, 210);
            MUTED_TEXT_COLOR = new Color(170, 178, 160);
            CAPTION_COLOR = new Color(144, 152, 136);
            SUCCESS_COLOR = new Color(172, 187, 152);
            WARNING_COLOR = new Color(189, 179, 145);
            ERROR_COLOR = new Color(182, 151, 143);
            SUCCESS_BACKGROUND = new Color(58, 70, 52);
            WARNING_BACKGROUND = new Color(72, 68, 52);
            ERROR_BACKGROUND = new Color(76, 58, 54);
            SHADOW_COLOR = new Color(11, 14, 10, 26);
            return;
        }

        FRAME_BACKGROUND = new Color(232, 236, 220);
        SURFACE_BACKGROUND = new Color(241, 243, 234);
        MUTED_SURFACE = new Color(226, 231, 211);
        SIDEBAR_BACKGROUND = new Color(218, 223, 198);
        PRIMARY_COLOR = new Color(121, 133, 110);
        PRIMARY_SOFT = new Color(210, 217, 186);
        ACCENT_COLOR = new Color(133, 144, 120);
        HERO_TINT = new Color(222, 227, 203);
        BORDER_COLOR = new Color(177, 186, 160);
        FOCUS_BORDER_COLOR = new Color(123, 136, 112);
        TEXT_COLOR = new Color(63, 73, 57);
        MUTED_TEXT_COLOR = new Color(96, 106, 89);
        CAPTION_COLOR = new Color(112, 121, 102);
        SUCCESS_COLOR = new Color(105, 123, 92);
        WARNING_COLOR = new Color(134, 127, 97);
        ERROR_COLOR = new Color(143, 111, 103);
        SUCCESS_BACKGROUND = new Color(220, 228, 205);
        WARNING_BACKGROUND = new Color(228, 224, 202);
        ERROR_BACKGROUND = new Color(231, 218, 213);
        SHADOW_COLOR = new Color(129, 137, 116, 18);
    }

    private static Color blend(Color a, Color b, double ratio) {
        double safeRatio = Math.max(0.0, Math.min(1.0, ratio));
        int red = (int) Math.round(a.getRed() * (1.0 - safeRatio) + b.getRed() * safeRatio);
        int green = (int) Math.round(a.getGreen() * (1.0 - safeRatio) + b.getGreen() * safeRatio);
        int blue = (int) Math.round(a.getBlue() * (1.0 - safeRatio) + b.getBlue() * safeRatio);
        int alpha = (int) Math.round(a.getAlpha() * (1.0 - safeRatio) + b.getAlpha() * safeRatio);
        return new Color(red, green, blue, alpha);
    }

    private static Color shift(Color color, double amount) {
        double safeAmount = Math.max(-0.5, Math.min(0.5, amount));
        int red = adjust(color.getRed(), safeAmount);
        int green = adjust(color.getGreen(), safeAmount);
        int blue = adjust(color.getBlue(), safeAmount);
        return new Color(red, green, blue, color.getAlpha());
    }

    private static int adjust(int value, double amount) {
        if (amount >= 0) {
            return (int) Math.round(value + (255 - value) * amount);
        }
        return (int) Math.round(value * (1.0 + amount));
    }

    private static final class RoundedLineBorder extends AbstractBorder {
        private final Color strokeColor;
        private final int radius;

        private RoundedLineBorder(Color strokeColor, int radius) {
            this.strokeColor = strokeColor;
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.top = 1;
            insets.left = 1;
            insets.bottom = 1;
            insets.right = 1;
            return insets;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(strokeColor);
            graphics2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            graphics2d.dispose();
        }
    }

    private static final class FocusLineBorder extends AbstractBorder {
        private final Color focusColor;
        private final Color defaultColor;
        private final int radius;

        private FocusLineBorder(Color focusColor, Color defaultColor, int radius) {
            this.focusColor = focusColor;
            this.defaultColor = defaultColor;
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.top = 1;
            insets.left = 1;
            insets.bottom = 1;
            insets.right = 1;
            return insets;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(component.hasFocus() ? focusColor : defaultColor);
            graphics2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            if (component.hasFocus()) {
                graphics2d.setColor(new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), 54));
                graphics2d.setStroke(new BasicStroke(1.5f));
                graphics2d.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
            }
            graphics2d.dispose();
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final Color strokeColor;
        private final int radius;

        private RoundedPanel(Color fillColor, Color strokeColor, int radius) {
            this.strokeColor = strokeColor;
            this.radius = radius;
            setOpaque(false);
            setBackground(fillColor);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(SHADOW_COLOR);
            graphics2d.fillRoundRect(1, 2, getWidth() - 3, getHeight() - 3, radius, radius);
            graphics2d.setColor(getBackground());
            graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            graphics2d.setColor(strokeColor);
            graphics2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            graphics2d.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class HeroPanel extends JPanel {
        private final Color startColor;
        private final Color endColor;
        private final Color strokeColor;
        private final int radius;

        private HeroPanel(Color startColor, Color endColor, Color strokeColor, int radius) {
            this.startColor = startColor;
            this.endColor = endColor;
            this.strokeColor = strokeColor;
            this.radius = radius;
            setOpaque(false);
            setBackground(startColor);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2d.setColor(startColor);
            graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            graphics2d.setColor(endColor);
            for (int x = 18; x < getWidth(); x += 28) {
                graphics2d.drawLine(x, 0, x, getHeight());
            }
            for (int y = 18; y < getHeight(); y += 28) {
                graphics2d.drawLine(0, y, getWidth(), y);
            }
            graphics2d.setColor(strokeColor);
            graphics2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            graphics2d.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private Color pressedColor;
        private Color strokeColor;
        private final int radius;

        private RoundedButton(String text) {
            super(text);
            this.radius = 8;
            setBorder(new EmptyBorder(10, 14, 10, 14));
        }

        private void configure(
                Color background,
                Color hoverBackground,
                Color pressedBackground,
                Color foreground,
                Color borderColor) {
            this.baseColor = background;
            this.hoverColor = hoverBackground;
            this.pressedColor = pressedBackground;
            this.strokeColor = borderColor;
            setForeground(foreground);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color fill = baseColor;
            if (baseColor.getAlpha() == 0) {
                if (getModel().isPressed()) {
                    fill = new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 40);
                } else if (getModel().isRollover()) {
                    fill = new Color(PRIMARY_COLOR.getRed(), PRIMARY_COLOR.getGreen(), PRIMARY_COLOR.getBlue(), 24);
                }
            } else if (!isEnabled()) {
                fill = blend(baseColor, FRAME_BACKGROUND, 0.45);
            } else if (getModel().isPressed()) {
                fill = pressedColor;
            } else if (getModel().isRollover()) {
                fill = hoverColor;
            }

            if (fill.getAlpha() > 0) {
                graphics2d.setColor(fill);
                graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            if (strokeColor != null) {
                graphics2d.setColor(strokeColor);
                graphics2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            graphics2d.dispose();

            super.paintComponent(graphics);
        }
    }
}
