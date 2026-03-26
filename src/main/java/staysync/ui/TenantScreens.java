package staysync.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import staysync.core.StaySyncService;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.PaymentRecord;
import staysync.core.TenantAccount.PaymentStatus;

interface TenantActions {
    void showWelcomeScreen();

    void registerTenant(
            String fullName,
            String username,
            String password,
            String confirmPassword,
            String contactNumber,
            String roomNumber,
            String roomType);

    void editTenantProfile(TenantAccount tenant);

    void changeTenantPassword(TenantAccount tenant);

    void markTenantAsPaid(TenantAccount tenant);

    void toggleTheme();

    boolean isDarkMode();
}

final class TenantRegistrationPanel extends JPanel {
    private final JTextField fullNameField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JTextField contactNumberField;
    private final JTextField roomNumberField;
    private final JComboBox<String> roomTypeComboBox;
    private final JPanel feedbackPanel;
    private final JLabel feedbackLabel;

    TenantRegistrationPanel(TenantActions actions) {
        setLayout(new BorderLayout());
        AppTheme.applyPageBackground(this);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        feedbackPanel = AppTheme.createFeedbackBanner();
        feedbackLabel = AppTheme.createFormLabel("");
        feedbackPanel.add(feedbackLabel, BorderLayout.CENTER);

        fullNameField = new JTextField();
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        contactNumberField = new JTextField();
        roomNumberField = new JTextField();
        roomTypeComboBox = new JComboBox<>(StaySyncService.getRoomTypes());

        AppTheme.styleTextField(fullNameField);
        AppTheme.styleTextField(usernameField);
        AppTheme.styleTextField(passwordField);
        AppTheme.styleTextField(confirmPasswordField);
        AppTheme.styleTextField(contactNumberField);
        AppTheme.styleTextField(roomNumberField);
        AppTheme.styleComboBox(roomTypeComboBox);

        JPanel fieldGrid = new JPanel(new GridLayout(0, 2, 14, 12));
        fieldGrid.setOpaque(false);
        fieldGrid.add(AppTheme.createFieldGroup("Full Name", fullNameField));
        fieldGrid.add(AppTheme.createFieldGroup("Username", usernameField));
        fieldGrid.add(AppTheme.createFieldGroup("Password", passwordField));
        fieldGrid.add(AppTheme.createFieldGroup("Confirm Password", confirmPasswordField));
        fieldGrid.add(AppTheme.createFieldGroup("Contact Number", contactNumberField));
        fieldGrid.add(AppTheme.createFieldGroup("Room Number", roomNumberField));
        fieldGrid.add(AppTheme.createFieldGroup("Room Type", roomTypeComboBox));
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        fieldGrid.add(filler);

        formPanel.add(feedbackPanel);
        AppTheme.addVerticalSpacing(formPanel, 10);
        formPanel.add(fieldGrid);
        AppTheme.addVerticalSpacing(formPanel, 10);
        formPanel.add(AppTheme.createDescriptionArea(
                "Use a unique room number and a valid contact number so the landlord table stays clean."));

        JButton backButton = AppTheme.createSecondaryButton("Back");
        JButton registerButton = AppTheme.createPrimaryButton("Create Account");

        backButton.addActionListener(event -> actions.showWelcomeScreen());
        registerButton.addActionListener(event -> actions.registerTenant(
                fullNameField.getText(),
                usernameField.getText(),
                new String(passwordField.getPassword()),
                new String(confirmPasswordField.getPassword()),
                contactNumberField.getText(),
                roomNumberField.getText(),
                roomTypeComboBox.getSelectedItem().toString()));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);
        registerButton.setPreferredSize(new Dimension(0, 46));
        backButton.setPreferredSize(new Dimension(0, 46));
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        JPanel page = new JPanel();
        page.setOpaque(false);
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel heroCard = AppTheme.createMutedPanel();
        heroCard.setLayout(new BoxLayout(heroCard, BoxLayout.Y_AXIS));
        heroCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        heroCard.setMaximumSize(new Dimension(960, 150));
        heroCard.add(AppTheme.createEyebrowLabel("Tenant onboarding"));
        AppTheme.addVerticalSpacing(heroCard, 8);
        heroCard.add(AppTheme.createHeaderLabel("Create account"));
        AppTheme.addVerticalSpacing(heroCard, 8);
        JTextArea heroText = AppTheme.createDescriptionArea(
                "Create a tenant record in one compact form. Everything stays inside a single visible card so the action buttons remain accessible.");
        heroText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        heroCard.add(heroText);

        JPanel formCard = AppTheme.createSurfacePanel();
        formCard.setLayout(new BorderLayout(0, 18));
        formCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        formCard.setMaximumSize(new Dimension(960, Integer.MAX_VALUE));
        formCard.add(AuthScreenSupport.createHeaderBlock(
                "Registration details",
                "Tenant account information",
                "Fill in the required fields below. The layout is intentionally compact for smaller desktop windows."),
                BorderLayout.NORTH);
        formCard.add(formPanel, BorderLayout.CENTER);
        formCard.add(buttonPanel, BorderLayout.SOUTH);

        page.add(heroCard);
        page.add(Box.createRigidArea(new Dimension(0, 16)));
        page.add(formCard);
        page.add(Box.createVerticalGlue());

        add(AppTheme.createPageScrollPane(page), BorderLayout.CENTER);
    }

    void clearFields() {
        fullNameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        contactNumberField.setText("");
        roomNumberField.setText("");
        roomTypeComboBox.setSelectedIndex(0);
        clearFeedback();
    }

    void showError(String message) {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.ERROR_COLOR, AppTheme.ERROR_BACKGROUND, message);
    }

    void showSuccess(String message) {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.SUCCESS_COLOR, AppTheme.SUCCESS_BACKGROUND, message);
    }

    void clearFeedback() {
        AppTheme.applyFeedback(feedbackPanel, feedbackLabel, AppTheme.MUTED_TEXT_COLOR, AppTheme.MUTED_SURFACE, "");
    }
}

final class TenantDashboardPanel extends JPanel {
    private enum Section {
        OVERVIEW("Overview", "Resident summary", "See your room, billing health, and profile details in one calm workspace."),
        PAYMENTS("Payments", "Billing timeline", "Review payment history, due reminders, and your latest status updates."),
        ACCOUNT("Account", "Profile and security", "Manage identity details, room assignment, and access settings.");

        private final String title;
        private final String subtitle;
        private final String description;

        Section(String title, String subtitle, String description) {
            this.title = title;
            this.subtitle = subtitle;
            this.description = description;
        }
    }

    private TenantAccount currentTenant;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private final JLabel topTitleLabel;
    private final JLabel topSubtitleLabel;
    private final JPanel feedbackPanel;
    private final JLabel feedbackLabel;
    private final JButton topMarkPaidButton;
    private final JButton paymentMarkPaidButton;
    private JLabel sidebarTenantNameLabel;
    private JLabel sidebarRoomLabel;
    private JPanel sidebarStatusBadgeHost;
    private final JLabel heroWelcomeLabel;
    private final JTextArea heroMessageArea;
    private final JLabel overviewStatusValue;
    private final JLabel overviewRentValue;
    private final JLabel overviewDueValue;
    private final JLabel overviewRoomValue;
    private final JPanel overviewStatusBadgeHost;
    private final JTextArea paymentAlertArea;
    private final JPanel paymentStatusBadgeHost;
    private final JTextArea overviewRecentArea;
    private final JTextArea latestActivityArea;
    private final DefaultTableModel historyTableModel;
    private final JLabel historyEmptyLabel;
    private final JLabel profileFullNameValue;
    private final JLabel profileUsernameValue;
    private final JLabel profileContactValue;
    private final JLabel roomNumberValue;
    private final JLabel roomTypeValue;
    private final JLabel monthlyRentValue;
    private final JLabel dueDayValue;
    private final JLabel paymentStatusValue;
    private final JLabel accountNameValue;
    private final JLabel accountUsernameValue;
    private final JLabel accountContactValue;
    private final JLabel accountRoomValue;
    private final JPanel accountStatusBadgeHost;

    TenantDashboardPanel(TenantActions actions) {
        setLayout(new BorderLayout(20, 0));
        AppTheme.applyPageBackground(this);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(createSidebar(actions), BorderLayout.WEST);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 18));
        mainPanel.setOpaque(false);

        topTitleLabel = AppTheme.createHeaderLabel(Section.OVERVIEW.title);
        topSubtitleLabel = AppTheme.createFormLabel(Section.OVERVIEW.subtitle);
        feedbackPanel = AppTheme.createFeedbackBanner();
        feedbackLabel = AppTheme.createFormLabel("");
        feedbackPanel.add(feedbackLabel, BorderLayout.CENTER);

        topMarkPaidButton = AppTheme.createPrimaryButton("Mark as Paid");
        topMarkPaidButton.addActionListener(event -> actions.markTenantAsPaid(currentTenant));

        JPanel headerStack = new JPanel();
        headerStack.setOpaque(false);
        headerStack.setLayout(new BoxLayout(headerStack, BoxLayout.Y_AXIS));
        headerStack.add(createTopBar(actions));
        AppTheme.addVerticalSpacing(headerStack, 12);
        headerStack.add(feedbackPanel);
        mainPanel.add(headerStack, BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setOpaque(false);

        heroWelcomeLabel = AppTheme.createTitleLabel("Welcome back");
        heroMessageArea = AppTheme.createDescriptionArea("");
        overviewStatusValue = AppTheme.createStatValueLabel();
        overviewRentValue = AppTheme.createStatValueLabel();
        overviewDueValue = AppTheme.createStatValueLabel();
        overviewRoomValue = AppTheme.createStatValueLabel();
        overviewStatusBadgeHost = createBadgeHost();

        paymentAlertArea = AppTheme.createDescriptionArea("");
        paymentAlertArea.setOpaque(true);
        paymentAlertArea.setBorder(new EmptyBorder(14, 14, 14, 14));
        paymentStatusBadgeHost = createBadgeHost();
        overviewRecentArea = AppTheme.createDescriptionArea("");
        overviewRecentArea.setOpaque(true);
        overviewRecentArea.setBackground(AppTheme.MUTED_SURFACE);
        overviewRecentArea.setBorder(new EmptyBorder(14, 14, 14, 14));
        latestActivityArea = AppTheme.createDescriptionArea("");
        latestActivityArea.setOpaque(true);
        latestActivityArea.setBackground(AppTheme.MUTED_SURFACE);
        latestActivityArea.setBorder(new EmptyBorder(14, 14, 14, 14));
        paymentMarkPaidButton = AppTheme.createPrimaryButton("Confirm Paid Status");
        paymentMarkPaidButton.addActionListener(event -> actions.markTenantAsPaid(currentTenant));

        profileFullNameValue = AppTheme.createValueLabel();
        profileUsernameValue = AppTheme.createValueLabel();
        profileContactValue = AppTheme.createValueLabel();
        roomNumberValue = AppTheme.createValueLabel();
        roomTypeValue = AppTheme.createValueLabel();
        monthlyRentValue = AppTheme.createValueLabel();
        dueDayValue = AppTheme.createValueLabel();
        paymentStatusValue = AppTheme.createValueLabel();
        paymentStatusValue.setFont(AppTheme.SUBHEADER_FONT);

        accountNameValue = AppTheme.createValueLabel();
        accountUsernameValue = AppTheme.createValueLabel();
        accountContactValue = AppTheme.createValueLabel();
        accountRoomValue = AppTheme.createValueLabel();
        accountStatusBadgeHost = createBadgeHost();

        historyTableModel = new DefaultTableModel(new Object[] { "Updated On", "Status", "Updated By", "Note" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyEmptyLabel = AppTheme.createFormLabel("No payment activity yet. New updates will appear here.");

        contentPanel.add(AppTheme.createPageScrollPane(createOverviewPage(actions)), Section.OVERVIEW.name());
        contentPanel.add(AppTheme.createPageScrollPane(createPaymentsPage()), Section.PAYMENTS.name());
        contentPanel.add(AppTheme.createPageScrollPane(createAccountPage(actions)), Section.ACCOUNT.name());
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        selectSection(Section.OVERVIEW);
    }

    TenantAccount getCurrentTenant() {
        return currentTenant;
    }

    void setTenant(TenantAccount tenant) {
        currentTenant = tenant;
        refreshTenantData();
    }

    void refreshTenantData() {
        if (currentTenant == null) {
            return;
        }

        PaymentStatus status = currentTenant.getPaymentStatus();
        PaymentRecord latestRecord = currentTenant.getPaymentHistory().isEmpty() ? null : currentTenant.getPaymentHistory().get(0);

        sidebarTenantNameLabel.setText(currentTenant.getFullName());
        sidebarRoomLabel.setText(currentTenant.getRoomInfo().getRoomNumber() + " | " + currentTenant.getRoomInfo().getRoomType());
        heroWelcomeLabel.setText("Welcome back, " + currentTenant.getFullName());
        heroMessageArea.setText(currentTenant.getDueNotificationMessage());

        overviewStatusValue.setText(status.getLabel());
        overviewStatusValue.setForeground(AppTheme.getPaymentStatusColor(status));
        overviewRentValue.setText(AppTheme.formatCurrency(currentTenant.getRoomInfo().getMonthlyRent()));
        overviewDueValue.setText("Day " + currentTenant.getRoomInfo().getDueDay());
        overviewRoomValue.setText(currentTenant.getRoomInfo().getRoomNumber());

        profileFullNameValue.setText(currentTenant.getFullName());
        profileUsernameValue.setText(currentTenant.getUsername());
        profileContactValue.setText(currentTenant.getContactNumber());
        roomNumberValue.setText(currentTenant.getRoomInfo().getRoomNumber());
        roomTypeValue.setText(currentTenant.getRoomInfo().getRoomType());
        monthlyRentValue.setText(AppTheme.formatCurrency(currentTenant.getRoomInfo().getMonthlyRent()));
        dueDayValue.setText("Every month on day " + currentTenant.getRoomInfo().getDueDay());
        paymentStatusValue.setText(currentTenant.getPaymentStatusLabel());
        paymentStatusValue.setForeground(AppTheme.getPaymentStatusColor(status));

        accountNameValue.setText(currentTenant.getFullName());
        accountUsernameValue.setText(currentTenant.getUsername());
        accountContactValue.setText(currentTenant.getContactNumber());
        accountRoomValue.setText(currentTenant.getRoomInfo().getRoomNumber() + " | " + currentTenant.getRoomInfo().getRoomType());

        paymentAlertArea.setText(currentTenant.getDueNotificationMessage());
        paymentAlertArea.setForeground(AppTheme.getPaymentStatusColor(status));
        paymentAlertArea.setBackground(AppTheme.getPaymentStatusBackground(status));

        if (latestRecord == null) {
            overviewRecentArea.setText("No recent payment updates yet.");
            latestActivityArea.setText("No recent payment updates yet.");
        } else {
            overviewRecentArea.setText(
                    latestRecord.getFormattedTimestamp()
                            + "\n"
                            + latestRecord.getStatus().getLabel()
                            + " by "
                            + latestRecord.getUpdatedBy()
                            + "\n"
                            + latestRecord.getNote());
            latestActivityArea.setText(
                    latestRecord.getFormattedTimestamp()
                            + "\n"
                            + latestRecord.getStatus().getLabel()
                            + " update from "
                            + latestRecord.getUpdatedBy()
                            + "\n"
                            + latestRecord.getNote());
        }

        historyTableModel.setRowCount(0);
        for (PaymentRecord record : currentTenant.getPaymentHistory()) {
            historyTableModel.addRow(new Object[] {
                    record.getFormattedTimestamp(),
                    record.getStatus().getLabel(),
                    record.getUpdatedBy(),
                    record.getNote()
            });
        }

        boolean canMarkPaid = status != PaymentStatus.PAID;
        topMarkPaidButton.setEnabled(canMarkPaid);
        paymentMarkPaidButton.setEnabled(canMarkPaid);
        historyEmptyLabel.setVisible(historyTableModel.getRowCount() == 0);

        refreshStatusBadge(sidebarStatusBadgeHost, status);
        refreshStatusBadge(overviewStatusBadgeHost, status);
        refreshStatusBadge(paymentStatusBadgeHost, status);
        refreshStatusBadge(accountStatusBadgeHost, status);
    }

    void showFeedback(String message, boolean success) {
        AppTheme.applyFeedback(
                feedbackPanel,
                feedbackLabel,
                success ? AppTheme.SUCCESS_COLOR : AppTheme.ERROR_COLOR,
                success ? AppTheme.SUCCESS_BACKGROUND : AppTheme.ERROR_BACKGROUND,
                message);
    }

    private JPanel createSidebar(TenantActions actions) {
        JPanel sidebar = AppTheme.createSidebarPanel();
        sidebar.setLayout(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(248, 0));

        JPanel brandBlock = new JPanel();
        brandBlock.setOpaque(false);
        brandBlock.setLayout(new BoxLayout(brandBlock, BoxLayout.Y_AXIS));
        brandBlock.add(AppTheme.createEyebrowLabel("StaySync"));
        AppTheme.addVerticalSpacing(brandBlock, 8);
        brandBlock.add(AppTheme.createSubheaderLabel("Tenant Console"));
        AppTheme.addVerticalSpacing(brandBlock, 6);
        brandBlock.add(AppTheme.createDescriptionArea("Minimal controls for account, billing, and room details."));

        ButtonGroup navGroup = new ButtonGroup();
        JToggleButton overviewButton = createSidebarNavButton("[ ] Overview");
        JToggleButton paymentButton = createSidebarNavButton("[$] Payments");
        JToggleButton accountButton = createSidebarNavButton("[@] Account");
        navGroup.add(overviewButton);
        navGroup.add(paymentButton);
        navGroup.add(accountButton);
        overviewButton.setSelected(true);

        overviewButton.addActionListener(event -> selectSection(Section.OVERVIEW));
        paymentButton.addActionListener(event -> selectSection(Section.PAYMENTS));
        accountButton.addActionListener(event -> selectSection(Section.ACCOUNT));

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.add(overviewButton);
        AppTheme.addVerticalSpacing(navPanel, 8);
        navPanel.add(paymentButton);
        AppTheme.addVerticalSpacing(navPanel, 8);
        navPanel.add(accountButton);

        JPanel profileCard = AppTheme.createMutedPanel();
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.add(AppTheme.createEyebrowLabel("Current resident"));
        AppTheme.addVerticalSpacing(profileCard, 10);
        sidebarTenantNameLabel = AppTheme.createSubheaderLabel("No tenant selected");
        sidebarRoomLabel = AppTheme.createFormLabel("Room details will appear here.");
        sidebarStatusBadgeHost = createBadgeHost();
        profileCard.add(sidebarTenantNameLabel);
        AppTheme.addVerticalSpacing(profileCard, 6);
        profileCard.add(sidebarRoomLabel);
        AppTheme.addVerticalSpacing(profileCard, 12);
        profileCard.add(sidebarStatusBadgeHost);

        JPanel bottomTip = AppTheme.createMutedPanel();
        bottomTip.setLayout(new BoxLayout(bottomTip, BoxLayout.Y_AXIS));
        bottomTip.add(AppTheme.createEyebrowLabel("Usage note"));
        AppTheme.addVerticalSpacing(bottomTip, 8);
        bottomTip.add(AppTheme.createDescriptionArea("Keep billing current before the monthly due date to avoid late status changes."));

        JPanel centerStack = new JPanel();
        centerStack.setOpaque(false);
        centerStack.setLayout(new BoxLayout(centerStack, BoxLayout.Y_AXIS));
        centerStack.add(navPanel);
        AppTheme.addVerticalSpacing(centerStack, 18);
        centerStack.add(profileCard);
        centerStack.add(Box.createVerticalGlue());

        sidebar.add(brandBlock, BorderLayout.NORTH);
        sidebar.add(centerStack, BorderLayout.CENTER);
        sidebar.add(bottomTip, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createTopBar(TenantActions actions) {
        JPanel topBar = AppTheme.createTopBarPanel();
        topBar.setLayout(new BorderLayout(16, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(topTitleLabel);
        AppTheme.addVerticalSpacing(titleBlock, 4);
        titleBlock.add(topSubtitleLabel);
        topBar.add(titleBlock, BorderLayout.CENTER);

        JButton editProfileButton = AppTheme.createSecondaryButton("Edit Profile");
        JButton themeButton = AppTheme.createGhostButton(AppTheme.getModeActionLabel());
        JButton signOutButton = AppTheme.createSecondaryButton("Sign Out");

        editProfileButton.addActionListener(event -> actions.editTenantProfile(currentTenant));
        themeButton.addActionListener(event -> actions.toggleTheme());
        signOutButton.addActionListener(event -> actions.showWelcomeScreen());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.add(themeButton);
        actionRow.add(editProfileButton);
        actionRow.add(topMarkPaidButton);
        actionRow.add(signOutButton);
        topBar.add(actionRow, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createOverviewPage(TenantActions actions) {
        JPanel page = createPageContainer();

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.add(createMetricCard("Payment status", overviewStatusValue, "Live billing state"));
        statsRow.add(createMetricCard("Monthly rent", overviewRentValue, "Current room pricing"));
        statsRow.add(createMetricCard("Due day", overviewDueValue, "Monthly collection cycle"));
        statsRow.add(createMetricCard("Assigned room", overviewRoomValue, "Room reference"));
        page.add(statsRow);
        page.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel summaryRow = new JPanel(new GridLayout(1, 2, 16, 0));
        summaryRow.setOpaque(false);
        summaryRow.add(createOverviewSummaryCard());
        summaryRow.add(createOverviewActivityCard());
        page.add(summaryRow);

        page.add(Box.createRigidArea(new Dimension(0, 16)));
        JPanel detailRow = new JPanel(new GridLayout(1, 2, 16, 0));
        detailRow.setOpaque(false);
        detailRow.add(createProfileSnapshotCard(actions));
        detailRow.add(createRoomSnapshotCard());
        page.add(detailRow);
        return page;
    }

    private JPanel createPaymentsPage() {
        JPanel page = createPageContainer();

        JPanel summaryCard = AppTheme.createSurfacePanel();
        summaryCard.setLayout(new BorderLayout(16, 0));

        JPanel summaryText = new JPanel();
        summaryText.setOpaque(false);
        summaryText.setLayout(new BoxLayout(summaryText, BoxLayout.Y_AXIS));
        summaryText.add(AppTheme.createEyebrowLabel("Billing status"));
        AppTheme.addVerticalSpacing(summaryText, 8);
        summaryText.add(AppTheme.createSubheaderLabel("Current reminder"));
        AppTheme.addVerticalSpacing(summaryText, 10);
        summaryText.add(paymentAlertArea);
        summaryCard.add(summaryText, BorderLayout.CENTER);

        JPanel summarySide = AppTheme.createMutedPanel();
        summarySide.setLayout(new BoxLayout(summarySide, BoxLayout.Y_AXIS));
        summarySide.add(AppTheme.createEyebrowLabel("Current state"));
        AppTheme.addVerticalSpacing(summarySide, 10);
        summarySide.add(paymentStatusBadgeHost);
        AppTheme.addVerticalSpacing(summarySide, 12);
        summarySide.add(AppTheme.createDescriptionArea("Use the payment action only when the current billing month has been settled."));
        summaryCard.add(summarySide, BorderLayout.EAST);
        JPanel topRow = new JPanel(new GridLayout(1, 2, 16, 0));
        topRow.setOpaque(false);
        topRow.add(summaryCard);
        topRow.add(createBillingActionsCard());
        page.add(topRow);
        page.add(Box.createRigidArea(new Dimension(0, 16)));
        page.add(createHistoryCard());
        return page;
    }

    private JPanel createAccountPage(TenantActions actions) {
        JPanel page = createPageContainer();
        JPanel topRow = new JPanel(new GridLayout(1, 2, 16, 0));
        topRow.setOpaque(false);
        topRow.add(createIdentityCard());
        topRow.add(createResidenceCard());
        page.add(topRow);
        page.add(Box.createRigidArea(new Dimension(0, 16)));
        page.add(createAccountActionsCard(actions));
        return page;
    }

    private JPanel createOverviewSummaryCard() {
        JPanel card = AppTheme.createHeroPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(AppTheme.createEyebrowLabel("Tenant workspace"));
        AppTheme.addVerticalSpacing(body, 8);
        body.add(heroWelcomeLabel);
        AppTheme.addVerticalSpacing(body, 8);
        heroMessageArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        body.add(heroMessageArea);
        AppTheme.addVerticalSpacing(body, 12);
        body.add(overviewStatusBadgeHost);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createOverviewActivityCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Recent activity"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Latest payment update"));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(overviewRecentArea);
        AppTheme.addVerticalSpacing(card, 12);
        card.add(AppTheme.createDescriptionArea("Use Payments for the full billing history and update controls."));
        return card;
    }

    private JPanel createProfileSnapshotCard(TenantActions actions) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(AppTheme.createSubheaderLabel("Profile snapshot"), BorderLayout.NORTH);

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setOpaque(false);
        TenantScreenSupport.addDetailRow(detailPanel, 0, "Full name", profileFullNameValue);
        TenantScreenSupport.addDetailRow(detailPanel, 1, "Username", profileUsernameValue);
        TenantScreenSupport.addDetailRow(detailPanel, 2, "Contact", profileContactValue);
        card.add(detailPanel, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        JButton editButton = AppTheme.createSecondaryButton("Edit Profile");
        JButton passwordButton = AppTheme.createGhostButton("Change Password");
        editButton.addActionListener(event -> actions.editTenantProfile(currentTenant));
        passwordButton.addActionListener(event -> actions.changeTenantPassword(currentTenant));
        actionRow.add(editButton);
        actionRow.add(passwordButton);
        card.add(actionRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createRoomSnapshotCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(AppTheme.createSubheaderLabel("Room and billing"), BorderLayout.NORTH);

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setOpaque(false);
        TenantScreenSupport.addDetailRow(detailPanel, 0, "Room number", roomNumberValue);
        TenantScreenSupport.addDetailRow(detailPanel, 1, "Room type", roomTypeValue);
        TenantScreenSupport.addDetailRow(detailPanel, 2, "Monthly rent", monthlyRentValue);
        TenantScreenSupport.addDetailRow(detailPanel, 3, "Due day", dueDayValue);
        TenantScreenSupport.addDetailRow(detailPanel, 4, "Payment status", paymentStatusValue);
        card.add(detailPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createHistoryCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(AppTheme.createSubheaderLabel("Payment history"), BorderLayout.NORTH);
        JTable historyTable = new JTable(historyTableModel);
        AppTheme.styleTable(historyTable);
        card.add(AppTheme.createTableScrollPane(historyTable), BorderLayout.CENTER);
        card.add(historyEmptyLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createBillingActionsCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Recent activity"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Last status update"));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(latestActivityArea);
        AppTheme.addVerticalSpacing(card, 16);
        paymentMarkPaidButton.setAlignmentX(LEFT_ALIGNMENT);
        card.add(paymentMarkPaidButton);
        AppTheme.addVerticalSpacing(card, 12);
        card.add(AppTheme.createDescriptionArea("If the rent is already settled, update the record here so the landlord dashboard stays in sync."));
        return card;
    }

    private JPanel createIdentityCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(AppTheme.createSubheaderLabel("Identity"), BorderLayout.NORTH);
        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setOpaque(false);
        TenantScreenSupport.addDetailRow(detailPanel, 0, "Full name", accountNameValue);
        TenantScreenSupport.addDetailRow(detailPanel, 1, "Username", accountUsernameValue);
        TenantScreenSupport.addDetailRow(detailPanel, 2, "Contact", accountContactValue);
        card.add(detailPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createResidenceCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Residence"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Assignment"));
        AppTheme.addVerticalSpacing(card, 12);
        card.add(accountRoomValue);
        AppTheme.addVerticalSpacing(card, 16);
        card.add(AppTheme.createDescriptionArea("Your room type and number are managed together so payment values stay aligned with the assigned space."));
        return card;
    }

    private JPanel createAccountActionsCard(TenantActions actions) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Access"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Actions and security"));
        AppTheme.addVerticalSpacing(card, 12);
        card.add(accountStatusBadgeHost);
        AppTheme.addVerticalSpacing(card, 14);

        JButton editButton = AppTheme.createSecondaryButton("Edit Profile");
        JButton passwordButton = AppTheme.createSecondaryButton("Change Password");
        JButton themeButton = AppTheme.createGhostButton(AppTheme.getModeActionLabel());
        AppTheme.makeButtonFillWidth(editButton);
        AppTheme.makeButtonFillWidth(passwordButton);
        AppTheme.makeButtonFillWidth(themeButton);

        editButton.addActionListener(event -> actions.editTenantProfile(currentTenant));
        passwordButton.addActionListener(event -> actions.changeTenantPassword(currentTenant));
        themeButton.addActionListener(event -> actions.toggleTheme());

        card.add(editButton);
        AppTheme.addVerticalSpacing(card, 10);
        card.add(passwordButton);
        AppTheme.addVerticalSpacing(card, 10);
        card.add(themeButton);
        AppTheme.addVerticalSpacing(card, 14);
        card.add(AppTheme.createDescriptionArea("Use this page for profile maintenance and password updates without leaving the tenant workspace."));
        return card;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, String helperText) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel(title));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(valueLabel);
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createDescriptionArea(helperText));
        return card;
    }

    private JPanel createPageContainer() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(4, 4, 12, 4));
        return panel;
    }

    private JPanel createBadgeHost() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        return panel;
    }

    private void refreshStatusBadge(JPanel host, PaymentStatus status) {
        host.removeAll();
        host.add(AppTheme.createStatusPill(
                status.getLabel(),
                AppTheme.getPaymentStatusColor(status),
                AppTheme.getPaymentStatusBackground(status)));
        host.revalidate();
        host.repaint();
    }

    private void selectSection(Section section) {
        topTitleLabel.setText(section.title);
        topSubtitleLabel.setText(section.subtitle + "  •  " + section.description);
        contentLayout.show(contentPanel, section.name());
    }

    private JToggleButton createSidebarNavButton(String text) {
        return new SidebarNavButton(text);
    }

    private static final class SidebarNavButton extends JToggleButton {
        private SidebarNavButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setHorizontalAlignment(LEFT);
            setForeground(AppTheme.MUTED_TEXT_COLOR);
            setFont(AppTheme.EMPHASIS_FONT);
            setBorder(new EmptyBorder(10, 12, 10, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected()) {
                graphics2d.setColor(new java.awt.Color(
                        AppTheme.PRIMARY_COLOR.getRed(),
                        AppTheme.PRIMARY_COLOR.getGreen(),
                        AppTheme.PRIMARY_COLOR.getBlue(),
                        34));
                graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                graphics2d.setColor(AppTheme.BORDER_COLOR);
                graphics2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                setForeground(AppTheme.TEXT_COLOR);
            } else if (getModel().isRollover()) {
                graphics2d.setColor(new java.awt.Color(AppTheme.PRIMARY_COLOR.getRed(), AppTheme.PRIMARY_COLOR.getGreen(), AppTheme.PRIMARY_COLOR.getBlue(), 18));
                graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                setForeground(AppTheme.TEXT_COLOR);
            } else {
                setForeground(AppTheme.MUTED_TEXT_COLOR);
            }

            graphics2d.dispose();
            super.paintComponent(graphics);
        }
    }
}

final class TenantProfileFormPanel extends JPanel {
    private final JTextField fullNameField;
    private final JTextField contactNumberField;
    private final JTextField roomNumberField;
    private final JComboBox<String> roomTypeComboBox;

    TenantProfileFormPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppTheme.SURFACE_BACKGROUND);

        fullNameField = new JTextField();
        contactNumberField = new JTextField();
        roomNumberField = new JTextField();
        roomTypeComboBox = new JComboBox<>(StaySyncService.getRoomTypes());

        AppTheme.styleTextField(fullNameField);
        AppTheme.styleTextField(contactNumberField);
        AppTheme.styleTextField(roomNumberField);
        AppTheme.styleComboBox(roomTypeComboBox);

        add(AppTheme.createFieldGroup("Full Name", fullNameField));
        AppTheme.addVerticalSpacing(this, 12);
        add(AppTheme.createFieldGroup("Contact Number", contactNumberField));
        AppTheme.addVerticalSpacing(this, 12);
        add(AppTheme.createFieldGroup("Room Number", roomNumberField));
        AppTheme.addVerticalSpacing(this, 12);
        add(AppTheme.createFieldGroup("Room Type", roomTypeComboBox));
    }

    void setTenant(TenantAccount tenant) {
        fullNameField.setText(tenant.getFullName());
        contactNumberField.setText(tenant.getContactNumber());
        roomNumberField.setText(tenant.getRoomInfo().getRoomNumber());
        roomTypeComboBox.setSelectedItem(tenant.getRoomInfo().getRoomType());
    }

    String getFullNameValue() {
        return fullNameField.getText();
    }

    String getContactNumberValue() {
        return contactNumberField.getText();
    }

    String getRoomNumberValue() {
        return roomNumberField.getText();
    }

    String getRoomTypeValue() {
        return roomTypeComboBox.getSelectedItem().toString();
    }
}

final class ChangePasswordPanel extends JPanel {
    private final JPasswordField currentPasswordField;
    private final JPasswordField newPasswordField;
    private final JPasswordField confirmPasswordField;

    ChangePasswordPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AppTheme.SURFACE_BACKGROUND);

        currentPasswordField = new JPasswordField();
        newPasswordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();

        AppTheme.styleTextField(currentPasswordField);
        AppTheme.styleTextField(newPasswordField);
        AppTheme.styleTextField(confirmPasswordField);

        add(AppTheme.createFieldGroup("Current Password", currentPasswordField));
        AppTheme.addVerticalSpacing(this, 12);
        add(AppTheme.createFieldGroup("New Password", newPasswordField));
        AppTheme.addVerticalSpacing(this, 12);
        add(AppTheme.createFieldGroup("Confirm Password", confirmPasswordField));
    }

    String getCurrentPasswordValue() {
        return new String(currentPasswordField.getPassword());
    }

    String getNewPasswordValue() {
        return new String(newPasswordField.getPassword());
    }

    String getConfirmPasswordValue() {
        return new String(confirmPasswordField.getPassword());
    }
}

final class TenantScreenSupport {
    private TenantScreenSupport() {
    }

    static void addDetailRow(JPanel panel, int row, String labelText, JComponent field) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(0, 0, 16, 18);
        panel.add(AppTheme.createFormLabel(labelText), constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(0, 0, 16, 0);
        panel.add(field, constraints);
    }

    static final class TenantAuthAdapter implements AuthActions {
        private final TenantActions tenantActions;

        TenantAuthAdapter(TenantActions tenantActions) {
            this.tenantActions = tenantActions;
        }

        @Override
        public void showWelcomeScreen() {
            tenantActions.showWelcomeScreen();
        }

        @Override
        public void showTenantLoginScreen() {
        }

        @Override
        public void showTenantRegistrationScreen() {
        }

        @Override
        public void showLandlordLoginScreen() {
        }

        @Override
        public void loginTenant(String username, String password) {
        }

        @Override
        public void loginLandlord(String username, String password) {
        }

        @Override
        public void toggleTheme() {
            tenantActions.toggleTheme();
        }

        @Override
        public boolean isDarkMode() {
            return tenantActions.isDarkMode();
        }
    }
}

