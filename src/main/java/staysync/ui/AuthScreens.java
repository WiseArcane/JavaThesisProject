package staysync.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import staysync.core.StaySyncService;

interface AuthActions {
    void showWelcomeScreen();

    void showTenantLoginScreen();

    void showTenantRegistrationScreen();

    void showLandlordLoginScreen();

    void loginTenant(String username, String password);

    void loginLandlord(String username, String password);

    void toggleTheme();

    boolean isDarkMode();
}

final class WelcomePanel extends JPanel {
    WelcomePanel(AuthActions actions) {
        setLayout(new BorderLayout());
        AppTheme.applyPageBackground(this);

        JButton tenantLoginButton = AppTheme.createPrimaryButton("Tenant Login");
        JButton tenantRegisterButton = AppTheme.createSecondaryButton("Tenant Registration");
        JButton landlordLoginButton = AppTheme.createSecondaryButton("Landlord Login");

        tenantLoginButton.addActionListener(event -> actions.showTenantLoginScreen());
        tenantRegisterButton.addActionListener(event -> actions.showTenantRegistrationScreen());
        landlordLoginButton.addActionListener(event -> actions.showLandlordLoginScreen());

        JPanel actionPanel = AppTheme.createMutedPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.add(AppTheme.createEyebrowLabel("Choose a workspace"));
        AppTheme.addVerticalSpacing(actionPanel, 12);
        AppTheme.makeButtonFillWidth(tenantLoginButton);
        AppTheme.makeButtonFillWidth(tenantRegisterButton);
        AppTheme.makeButtonFillWidth(landlordLoginButton);
        actionPanel.add(tenantLoginButton);
        AppTheme.addVerticalSpacing(actionPanel, 10);
        actionPanel.add(tenantRegisterButton);
        AppTheme.addVerticalSpacing(actionPanel, 10);
        actionPanel.add(landlordLoginButton);

        JTextArea credentialsArea = AppTheme.createDescriptionArea(
                "Landlord demo access\n"
                        + StaySyncService.getLandlordUsername()
                        + " / "
                        + StaySyncService.getLandlordPassword());
        credentialsArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JPanel credentialsPanel = AppTheme.createMutedPanel();
        credentialsPanel.setLayout(new BoxLayout(credentialsPanel, BoxLayout.Y_AXIS));
        credentialsPanel.add(AppTheme.createEyebrowLabel("Demo credentials"));
        AppTheme.addVerticalSpacing(credentialsPanel, 8);
        credentialsPanel.add(credentialsArea);

        JPanel contentPanel = AuthScreenSupport.createAuthPage(
                actions,
                "Dorm and apartment management",
                "StaySync",
                "A calm, modern workspace for onboarding residents, checking room assignments, and tracking payments without visual clutter.",
                "Made for quick walkthroughs",
                "Balanced panels, focused actions, and seeded records give the app a cleaner presentation flow.",
                actionPanel,
                credentialsPanel);
        add(contentPanel, BorderLayout.CENTER);
    }
}

final class TenantLoginPanel extends JPanel {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPanel feedbackPanel;
    private final JLabel feedbackLabel;

    TenantLoginPanel(AuthActions actions) {
        setLayout(new BorderLayout());
        AppTheme.applyPageBackground(this);

        feedbackPanel = AppTheme.createFeedbackBanner();
        feedbackLabel = AppTheme.createFormLabel("");
        feedbackPanel.add(feedbackLabel, BorderLayout.CENTER);

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        AppTheme.styleTextField(usernameField);
        AppTheme.styleTextField(passwordField);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(feedbackPanel);
        AppTheme.addVerticalSpacing(formPanel, 12);
        formPanel.add(AppTheme.createFieldGroup("Username", usernameField));
        AppTheme.addVerticalSpacing(formPanel, 14);
        formPanel.add(AppTheme.createFieldGroup("Password", passwordField));

        JButton backButton = AppTheme.createSecondaryButton("Back");
        JButton loginButton = AppTheme.createPrimaryButton("Sign In");

        backButton.addActionListener(event -> actions.showWelcomeScreen());
        loginButton.addActionListener(event -> actions.loginTenant(
                usernameField.getText(),
                new String(passwordField.getPassword())));

        JPanel buttonPanel = AuthScreenSupport.createFormActions(loginButton, backButton);

        add(AuthScreenSupport.createAuthPage(
                actions,
                "Tenant portal",
                "Stay on top of your room",
                "A cleaner tenant experience with better typography, better spacing, and a more focused sign-in flow.",
                "What you can do",
                "Review your room, due reminders, and payment history from one focused dashboard.",
                AuthScreenSupport.createAuthCard(
                        "Tenant access",
                        "Sign in",
                        "Use your tenant account to review room details, payment status, and recent activity.",
                        formPanel,
                        buttonPanel),
                AuthScreenSupport.createInfoStrip(
                        "Tip",
                        "Use one of the seeded tenant accounts to demo the dashboard immediately.")),
                BorderLayout.CENTER);
    }

    void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        clearFeedback();
    }

    void showError(String message) {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.ERROR_COLOR, AppTheme.ERROR_BACKGROUND, message);
    }

    void showSuccess(String message) {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.SUCCESS_COLOR, AppTheme.SUCCESS_BACKGROUND, message);
    }

    void setUsername(String username) {
        usernameField.setText(username == null ? "" : username);
        passwordField.setText("");
    }

    void clearFeedback() {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.MUTED_TEXT_COLOR, AppTheme.MUTED_SURFACE, "");
    }
}

final class LandlordLoginPanel extends JPanel {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPanel feedbackPanel;
    private final JLabel feedbackLabel;

    LandlordLoginPanel(AuthActions actions) {
        setLayout(new BorderLayout());
        AppTheme.applyPageBackground(this);

        feedbackPanel = AppTheme.createFeedbackBanner();
        feedbackLabel = AppTheme.createFormLabel("");
        feedbackPanel.add(feedbackLabel, BorderLayout.CENTER);

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        AppTheme.styleTextField(usernameField);
        AppTheme.styleTextField(passwordField);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.add(feedbackPanel);
        AppTheme.addVerticalSpacing(formPanel, 12);
        formPanel.add(AppTheme.createFieldGroup("Username", usernameField));
        AppTheme.addVerticalSpacing(formPanel, 14);
        formPanel.add(AppTheme.createFieldGroup("Password", passwordField));
        AppTheme.addVerticalSpacing(formPanel, 16);

        JPanel notePanel = AppTheme.createMutedPanel();
        notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.Y_AXIS));
        notePanel.add(AppTheme.createEyebrowLabel("Default credentials"));
        AppTheme.addVerticalSpacing(notePanel, 8);
        JTextArea noteArea = AppTheme.createDescriptionArea(
                StaySyncService.getLandlordUsername() + " / " + StaySyncService.getLandlordPassword());
        noteArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        notePanel.add(noteArea);
        formPanel.add(notePanel);

        JButton backButton = AppTheme.createSecondaryButton("Back");
        JButton loginButton = AppTheme.createPrimaryButton("Sign In");

        backButton.addActionListener(event -> actions.showWelcomeScreen());
        loginButton.addActionListener(event -> actions.loginLandlord(
                usernameField.getText(),
                new String(passwordField.getPassword())));

        JPanel buttonPanel = AuthScreenSupport.createFormActions(loginButton, backButton);

        add(AuthScreenSupport.createAuthPage(
                actions,
                "Operations panel",
                "Control the whole property view",
                "A sharper command center for tracking statuses, reviewing tenants, and presenting the system with a more professional feel.",
                "Designed for demos",
                "Quick search, summary stats, and live status updates are now visually connected.",
                AuthScreenSupport.createAuthCard(
                        "Landlord access",
                        "Overview and controls",
                        "Review tenant status, search rooms quickly, and update payment records from one surface.",
                        formPanel,
                        buttonPanel),
                AuthScreenSupport.createInfoStrip(
                        "Default access",
                        StaySyncService.getLandlordUsername() + " / " + StaySyncService.getLandlordPassword())),
                BorderLayout.CENTER);
    }

    void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        clearFeedback();
    }

    void showError(String message) {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.ERROR_COLOR, AppTheme.ERROR_BACKGROUND, message);
    }

    void clearFeedback() {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.MUTED_TEXT_COLOR, AppTheme.MUTED_SURFACE, "");
    }
}

final class AuthScreenSupport {
    private AuthScreenSupport() {
    }

    static JPanel createAuthPage(
            AuthActions actions,
            String heroEyebrow,
            String heroTitle,
            String heroDescription,
            String metricTitle,
            String metricDescription,
            JPanel primaryCard,
            JPanel secondaryCard) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(false);
        shell.setBorder(new InsetsPanelBorder(8));

        JPanel heroPanel = AppTheme.createHeroPanel();
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));
        heroPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        heroPanel.add(AppTheme.createEyebrowLabel(heroEyebrow));
        AppTheme.addVerticalSpacing(heroPanel, 12);
        heroPanel.add(AppTheme.createTitleLabel(heroTitle));
        AppTheme.addVerticalSpacing(heroPanel, 14);
        JTextArea heroArea = AppTheme.createDescriptionArea(heroDescription);
        heroArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        heroPanel.add(heroArea);
        AppTheme.addVerticalSpacing(heroPanel, 18);
        heroPanel.add(createHeroHighlights());
        AppTheme.addVerticalSpacing(heroPanel, 24);
        heroPanel.add(createMetricCard(metricTitle, metricDescription));
        AppTheme.addVerticalSpacing(heroPanel, 14);
        heroPanel.add(createThemePanel(actions));
        heroPanel.add(Box.createVerticalGlue());

        JPanel rightColumn = new JPanel();
        rightColumn.setOpaque(false);
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setPreferredSize(new Dimension(360, 0));
        rightColumn.setMinimumSize(new Dimension(340, 0));
        rightColumn.add(primaryCard);
        AppTheme.addVerticalSpacing(rightColumn, 16);
        rightColumn.add(secondaryCard);
        rightColumn.add(Box.createVerticalGlue());

        primaryCard.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        primaryCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        secondaryCard.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        secondaryCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        shell.add(createPageScrollPane(new ResponsiveAuthStage(heroPanel, rightColumn)), BorderLayout.CENTER);
        return shell;
    }

    static JPanel createAuthCard(String eyebrowText, String title, String description, JPanel body, JPanel footer) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 24));
        card.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        card.add(createHeaderBlock(eyebrowText, title, description), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    static JPanel createInfoStrip(String eyebrow, String message) {
        JPanel panel = AppTheme.createMutedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        panel.add(AppTheme.createEyebrowLabel(eyebrow));
        AppTheme.addVerticalSpacing(panel, 8);
        JTextArea area = AppTheme.createDescriptionArea(message);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        panel.add(area);
        return panel;
    }

    static JPanel createHeaderBlock(String eyebrowText, String title, String description) {
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        headerPanel.add(AppTheme.createEyebrowLabel(eyebrowText));
        AppTheme.addVerticalSpacing(headerPanel, 8);
        headerPanel.add(AppTheme.createTitleLabel(title));
        AppTheme.addVerticalSpacing(headerPanel, 10);

        JTextArea descriptionArea = AppTheme.createDescriptionArea(description);
        descriptionArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        headerPanel.add(descriptionArea);
        return headerPanel;
    }

    static JPanel createFormActions(JButton primaryButton, JButton secondaryButton) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        AppTheme.makeButtonFillWidth(primaryButton);
        AppTheme.makeButtonFillWidth(secondaryButton);
        primaryButton.setPreferredSize(new Dimension(320, 46));
        secondaryButton.setPreferredSize(new Dimension(320, 46));
        panel.add(primaryButton);
        AppTheme.addVerticalSpacing(panel, 10);
        panel.add(secondaryButton);
        return panel;
    }

    private static JPanel createMetricCard(String title, String description) {
        JPanel metricPanel = AppTheme.createMutedPanel();
        metricPanel.setLayout(new BoxLayout(metricPanel, BoxLayout.Y_AXIS));
        metricPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        metricPanel.add(AppTheme.createEyebrowLabel("Why it feels better"));
        AppTheme.addVerticalSpacing(metricPanel, 10);

        metricPanel.add(AppTheme.createHeaderLabel(title));
        AppTheme.addVerticalSpacing(metricPanel, 8);

        JTextArea area = AppTheme.createDescriptionArea(description);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        metricPanel.add(area);
        AppTheme.addVerticalSpacing(metricPanel, 14);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chipRow.setOpaque(false);
        chipRow.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        chipRow.add(AppTheme.createTag("Soft depth"));
        chipRow.add(AppTheme.createTag("Clear hierarchy"));
        metricPanel.add(chipRow);
        return metricPanel;
    }

    private static JPanel createThemePanel(AuthActions actions) {
        JPanel panel = AppTheme.createMutedPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        panel.add(AppTheme.createEyebrowLabel("Appearance"), BorderLayout.NORTH);

        JTextArea area = AppTheme.createDescriptionArea(
                actions.isDarkMode()
                        ? "Dark mode is active for lower-glare presentations."
                        : "Light mode is active for bright, clean classroom demos.");
        panel.add(area, BorderLayout.CENTER);

        JButton toggleButton = AppTheme.createSecondaryButton(AppTheme.getModeActionLabel());
        toggleButton.addActionListener(event -> actions.toggleTheme());
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(toggleButton);
        panel.add(actionRow, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel createHeroHighlights() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        row.add(AppTheme.createTag("Resident onboarding"));
        row.add(AppTheme.createTag("Payment tracking"));
        row.add(AppTheme.createTag("Minimal dashboard"));
        return row;
    }

    private static JScrollPane createPageScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(AppTheme.FRAME_BACKGROUND);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private static final class ResponsiveAuthStage extends JPanel implements Scrollable {
        private static final int STACK_BREAKPOINT = 980;
        private static final int COLUMN_GAP = 28;
        private static final int STACK_GAP = 18;

        private final JComponent heroPanel;
        private final JComponent rightColumn;

        private ResponsiveAuthStage(JComponent heroPanel, JComponent rightColumn) {
            this.heroPanel = heroPanel;
            this.rightColumn = rightColumn;
            setOpaque(false);
            setLayout(null);
            setBorder(new InsetsPanelBorder(20));
            add(heroPanel);
            add(rightColumn);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int availableWidth = Math.max(0, getWidth() - insets.left - insets.right);
            int x = insets.left;
            int y = insets.top;

            if (availableWidth < STACK_BREAKPOINT) {
                int rightHeight = preferredHeight(rightColumn, availableWidth);
                int heroHeight = preferredHeight(heroPanel, availableWidth);
                rightColumn.setBounds(x, y, availableWidth, rightHeight);
                heroPanel.setBounds(x, y + rightHeight + STACK_GAP, availableWidth, heroHeight);
                return;
            }

            int sidebarWidth = Math.max(340, Math.min(390, (int) Math.round(availableWidth * 0.35)));
            int heroWidth = Math.max(0, availableWidth - sidebarWidth - COLUMN_GAP);
            int heroHeight = preferredHeight(heroPanel, heroWidth);
            int rightHeight = preferredHeight(rightColumn, sidebarWidth);
            int sharedHeight = Math.max(heroHeight, rightHeight);

            heroPanel.setBounds(x, y, heroWidth, sharedHeight);
            rightColumn.setBounds(x + heroWidth + COLUMN_GAP, y, sidebarWidth, sharedHeight);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            int parentWidth = getParent() == null ? 0 : getParent().getWidth() - insets.left - insets.right;
            int width = parentWidth > 0 ? parentWidth : 1080;
            int contentHeight;

            if (width < STACK_BREAKPOINT) {
                int heroHeight = preferredHeight(heroPanel, width);
                int rightHeight = preferredHeight(rightColumn, width);
                contentHeight = heroHeight + STACK_GAP + rightHeight;
            } else {
                int sidebarWidth = Math.max(340, Math.min(390, (int) Math.round(width * 0.35)));
                int heroWidth = Math.max(0, width - sidebarWidth - COLUMN_GAP);
                contentHeight = Math.max(
                        preferredHeight(heroPanel, heroWidth),
                        preferredHeight(rightColumn, sidebarWidth));
            }

            return new Dimension(width + insets.left + insets.right, contentHeight + insets.top + insets.bottom);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(80, visibleRect.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        private int preferredHeight(JComponent component, int width) {
            component.setSize(Math.max(width, 0), Short.MAX_VALUE);
            return component.getPreferredSize().height;
        }
    }

    private static final class InsetsPanelBorder extends javax.swing.border.EmptyBorder {
        private InsetsPanelBorder(int inset) {
            super(inset, inset, inset, inset);
        }
    }
}
