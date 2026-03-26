package staysync.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import staysync.core.StaySyncService.DashboardSnapshot;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.PaymentStatus;

interface LandlordActions {
    void showWelcomeScreen();

    void searchLandlordTenants(String query);

    void refreshLandlordDashboard();

    void updateTenantStatusFromLandlord(TenantAccount tenant, PaymentStatus status);

    void toggleTheme();

    boolean isDarkMode();
}

final class LandlordDashboardPanel extends JPanel {
    private enum Section {
        OVERVIEW("Overview", "Portfolio status", "Track resident counts, search results, and payment health from one shell."),
        RESIDENTS("Residents", "Tenant list", "Search, filter, and review tenant records without losing the payment context."),
        CONTROLS("Controls", "Selected tenant", "Apply payment-status changes with clear selection feedback and fewer clicks.");

        private final String title;
        private final String subtitle;
        private final String description;

        Section(String title, String subtitle, String description) {
            this.title = title;
            this.subtitle = subtitle;
            this.description = description;
        }
    }

    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> tableSorter;
    private final JLabel topTitleLabel;
    private final JLabel topSubtitleLabel;
    private final JLabel paidStatLabel;
    private final JLabel pendingStatLabel;
    private final JLabel lateStatLabel;
    private final JLabel totalStatLabel;
    private final JLabel sidebarTotalLabel;
    private final JLabel sidebarQueryLabel;
    private final JTextArea overviewSummaryArea;
    private final JTextArea overviewAttentionArea;
    private final JTextArea overviewWorkflowArea;
    private final JTextArea residentsSummaryArea;
    private final JTextField searchField;
    private final JComboBox<PaymentStatus> statusComboBox;
    private final JTable tenantTable;
    private final JLabel tableEmptyLabel;
    private JToggleButton allFilterButton;
    private final JLabel selectedNameValue;
    private final JLabel selectedRoomValue;
    private final JLabel selectedContactValue;
    private final JLabel selectedRentValue;
    private final JPanel selectedStatusBadgeHost;
    private final JTextArea selectionHintArea;
    private final CardLayout contentLayout;
    private final JPanel contentPanel;
    private List<TenantAccount> currentTenants;
    private DashboardSnapshot currentSnapshot;
    private PaymentStatus activeStatusFilter;

    LandlordDashboardPanel(LandlordActions actions) {
        currentTenants = Collections.emptyList();
        activeStatusFilter = null;

        setLayout(new BorderLayout(20, 0));
        AppTheme.applyPageBackground(this);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 18));
        mainPanel.setOpaque(false);

        topTitleLabel = AppTheme.createHeaderLabel(Section.OVERVIEW.title);
        topSubtitleLabel = AppTheme.createFormLabel(Section.OVERVIEW.subtitle);
        paidStatLabel = AppTheme.createStatValueLabel();
        pendingStatLabel = AppTheme.createStatValueLabel();
        lateStatLabel = AppTheme.createStatValueLabel();
        totalStatLabel = AppTheme.createStatValueLabel();
        sidebarTotalLabel = AppTheme.createSubheaderLabel("0 residents");
        sidebarQueryLabel = AppTheme.createFormLabel("Showing all records.");
        overviewSummaryArea = AppTheme.createDescriptionArea("Resident data will appear after the dashboard loads.");
        overviewAttentionArea = AppTheme.createDescriptionArea("Late and pending account guidance will appear here.");
        overviewWorkflowArea = AppTheme.createDescriptionArea("Use Residents to search and Controls to apply payment changes.");
        residentsSummaryArea = AppTheme.createDescriptionArea("Search results and filter guidance will appear here.");

        searchField = new JTextField();
        AppTheme.styleTextField(searchField);
        searchField.addActionListener(event -> actions.searchLandlordTenants(searchField.getText()));

        statusComboBox = new JComboBox<>(PaymentStatus.values());
        AppTheme.styleComboBox(statusComboBox);

        tableModel = new DefaultTableModel(
                new Object[] { "Tenant Name", "Username", "Room Number", "Room Type", "Contact Number", "Monthly Rent", "Payment Status" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tenantTable = new JTable(tableModel);
        AppTheme.styleTable(tenantTable);
        tenantTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tenantTable.getColumnModel().getColumn(6).setCellRenderer(new PaymentStatusCellRenderer());
        tableSorter = new TableRowSorter<>(tableModel);
        tenantTable.setRowSorter(tableSorter);
        tenantTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionDetails();
            }
        });

        tableEmptyLabel = AppTheme.createFormLabel("No matching tenants found. Try a different search or filter.");
        selectedNameValue = AppTheme.createValueLabel();
        selectedRoomValue = AppTheme.createValueLabel();
        selectedContactValue = AppTheme.createValueLabel();
        selectedRentValue = AppTheme.createValueLabel();
        selectedStatusBadgeHost = createBadgeHost();
        selectionHintArea = AppTheme.createDescriptionArea("Select a tenant from the residents table to view controls.");
        selectionHintArea.setOpaque(true);
        selectionHintArea.setBackground(AppTheme.MUTED_SURFACE);
        selectionHintArea.setBorder(new EmptyBorder(14, 14, 14, 14));

        mainPanel.add(createTopBar(actions), BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setOpaque(false);
        contentPanel.add(AppTheme.createPageScrollPane(createOverviewPage()), Section.OVERVIEW.name());
        contentPanel.add(AppTheme.createPageScrollPane(createResidentsPage(actions)), Section.RESIDENTS.name());
        contentPanel.add(AppTheme.createPageScrollPane(createControlsPage(actions)), Section.CONTROLS.name());
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(createSidebar(), BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
        selectSection(Section.OVERVIEW);
    }

    void refreshTable(DashboardSnapshot snapshot) {
        currentSnapshot = snapshot;
        currentTenants = new ArrayList<>(snapshot.getTenants());
        tableModel.setRowCount(0);

        for (TenantAccount tenant : currentTenants) {
            tableModel.addRow(new Object[] {
                    tenant.getFullName(),
                    tenant.getUsername(),
                    tenant.getRoomInfo().getRoomNumber(),
                    tenant.getRoomInfo().getRoomType(),
                    tenant.getContactNumber(),
                    AppTheme.formatCurrency(tenant.getRoomInfo().getMonthlyRent()),
                    tenant.getPaymentStatus()
            });
        }

        paidStatLabel.setText(String.valueOf(snapshot.getPaidCount()));
        pendingStatLabel.setText(String.valueOf(snapshot.getPendingCount()));
        lateStatLabel.setText(String.valueOf(snapshot.getLateCount()));
        totalStatLabel.setText(String.valueOf(snapshot.getTotalTenantCount()));
        sidebarTotalLabel.setText(snapshot.getTotalTenantCount() + " residents");
        sidebarQueryLabel.setText(snapshot.getQuery().isEmpty() ? "Showing all records." : "Query: \"" + snapshot.getQuery() + "\"");
        overviewSummaryArea.setText(buildOverviewSummary(snapshot));
        overviewAttentionArea.setText(buildAttentionSummary(snapshot));
        overviewWorkflowArea.setText(buildWorkflowSummary(snapshot));
        residentsSummaryArea.setText(buildResidentsRailSummary(snapshot));

        applyStatusFilter();
        tenantTable.clearSelection();
        updateSelectionDetails();
        tableEmptyLabel.setVisible(tenantTable.getRowCount() == 0);
    }

    void clearSearch() {
        searchField.setText("");
        tenantTable.clearSelection();
        activeStatusFilter = null;
        allFilterButton.setSelected(true);
        applyStatusFilter();
    }

    String getSearchQuery() {
        return searchField.getText();
    }

    void setSearchQuery(String query) {
        searchField.setText(query == null ? "" : query);
    }

    private JPanel createSidebar() {
        JPanel sidebar = AppTheme.createSidebarPanel();
        sidebar.setLayout(new BorderLayout(0, 18));
        sidebar.setPreferredSize(new Dimension(250, 0));

        JPanel brandBlock = new JPanel();
        brandBlock.setOpaque(false);
        brandBlock.setLayout(new BoxLayout(brandBlock, BoxLayout.Y_AXIS));
        brandBlock.add(AppTheme.createEyebrowLabel("StaySync"));
        AppTheme.addVerticalSpacing(brandBlock, 8);
        brandBlock.add(AppTheme.createSubheaderLabel("Landlord Console"));
        AppTheme.addVerticalSpacing(brandBlock, 6);
        brandBlock.add(AppTheme.createDescriptionArea("A focused operations shell for resident search, payment review, and status control."));

        ButtonGroup navGroup = new ButtonGroup();
        JToggleButton overviewButton = createSidebarNavButton("[ ] Overview");
        JToggleButton residentsButton = createSidebarNavButton("[#] Residents");
        JToggleButton controlsButton = createSidebarNavButton("[*] Controls");
        navGroup.add(overviewButton);
        navGroup.add(residentsButton);
        navGroup.add(controlsButton);
        overviewButton.setSelected(true);
        overviewButton.addActionListener(event -> selectSection(Section.OVERVIEW));
        residentsButton.addActionListener(event -> selectSection(Section.RESIDENTS));
        controlsButton.addActionListener(event -> selectSection(Section.CONTROLS));

        JPanel navPanel = new JPanel();
        navPanel.setOpaque(false);
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.add(overviewButton);
        AppTheme.addVerticalSpacing(navPanel, 8);
        navPanel.add(residentsButton);
        AppTheme.addVerticalSpacing(navPanel, 8);
        navPanel.add(controlsButton);

        JPanel summaryCard = AppTheme.createMutedPanel();
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.add(AppTheme.createEyebrowLabel("Portfolio"));
        AppTheme.addVerticalSpacing(summaryCard, 8);
        summaryCard.add(sidebarTotalLabel);
        AppTheme.addVerticalSpacing(summaryCard, 6);
        summaryCard.add(sidebarQueryLabel);

        JPanel tipCard = AppTheme.createMutedPanel();
        tipCard.setLayout(new BoxLayout(tipCard, BoxLayout.Y_AXIS));
        tipCard.add(AppTheme.createEyebrowLabel("Workflow tip"));
        AppTheme.addVerticalSpacing(tipCard, 8);
        tipCard.add(AppTheme.createDescriptionArea("Use Residents for discovery and Controls for status changes so the table stays uncluttered."));

        JPanel centerStack = new JPanel();
        centerStack.setOpaque(false);
        centerStack.setLayout(new BoxLayout(centerStack, BoxLayout.Y_AXIS));
        centerStack.add(navPanel);
        AppTheme.addVerticalSpacing(centerStack, 18);
        centerStack.add(summaryCard);
        centerStack.add(Box.createVerticalGlue());

        sidebar.add(brandBlock, BorderLayout.NORTH);
        sidebar.add(centerStack, BorderLayout.CENTER);
        sidebar.add(tipCard, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createTopBar(LandlordActions actions) {
        JPanel topBar = AppTheme.createTopBarPanel();
        topBar.setLayout(new BorderLayout(16, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.add(topTitleLabel);
        AppTheme.addVerticalSpacing(titleBlock, 4);
        titleBlock.add(topSubtitleLabel);
        topBar.add(titleBlock, BorderLayout.CENTER);

        JButton themeButton = AppTheme.createGhostButton(AppTheme.getModeActionLabel());
        JButton refreshButton = AppTheme.createSecondaryButton("Refresh");
        JButton signOutButton = AppTheme.createSecondaryButton("Sign Out");
        themeButton.addActionListener(event -> actions.toggleTheme());
        refreshButton.addActionListener(event -> actions.refreshLandlordDashboard());
        signOutButton.addActionListener(event -> actions.showWelcomeScreen());

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.add(themeButton);
        actionRow.add(refreshButton);
        actionRow.add(signOutButton);
        topBar.add(actionRow, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createOverviewPage() {
        JPanel page = createPageContainer();

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.add(createStatCard("Residents", totalStatLabel, "Seeded accounts"));
        statsRow.add(createStatCard("Paid", paidStatLabel, "Settled this cycle"));
        statsRow.add(createStatCard("Pending", pendingStatLabel, "Awaiting payment"));
        statsRow.add(createStatCard("Late", lateStatLabel, "Needs action"));
        page.add(statsRow);
        page.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel summaryRow = new JPanel(new GridLayout(1, 2, 16, 0));
        summaryRow.setOpaque(false);
        summaryRow.add(createOverviewSummaryCard());
        summaryRow.add(createOverviewWorkflowCard());
        page.add(summaryRow);

        page.add(Box.createRigidArea(new Dimension(0, 16)));
        JPanel detailRow = new JPanel(new GridLayout(1, 2, 16, 0));
        detailRow.setOpaque(false);
        detailRow.add(createOverviewAttentionCard());
        detailRow.add(createStatusRailCard());
        page.add(detailRow);
        return page;
    }

    private JPanel createResidentsPage(LandlordActions actions) {
        JPanel page = createPageContainer();
        page.add(createSearchControls(actions));
        page.add(Box.createRigidArea(new Dimension(0, 16)));
        JPanel contentRow = new JPanel(new GridLayout(1, 2, 16, 0));
        contentRow.setOpaque(false);
        contentRow.add(createResidentTableCard());
        contentRow.add(createResidentsSummaryCard(actions));
        page.add(contentRow);
        return page;
    }

    private JPanel createControlsPage(LandlordActions actions) {
        JPanel page = createPageContainer();
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(createSelectedTenantCard());
        row.add(createStatusControlCard(actions));
        page.add(row);
        return page;
    }

    private JPanel createOverviewSummaryCard() {
        JPanel card = AppTheme.createHeroPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(AppTheme.createEyebrowLabel("Operations summary"));
        AppTheme.addVerticalSpacing(body, 8);
        body.add(AppTheme.createTitleLabel("Resident payment board"));
        AppTheme.addVerticalSpacing(body, 8);
        overviewSummaryArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        body.add(overviewSummaryArea);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createOverviewWorkflowCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Workflow"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Next best actions"));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(overviewWorkflowArea);
        AppTheme.addVerticalSpacing(card, 12);

        JButton residentsButton = AppTheme.createPrimaryButton("Open Residents");
        JButton controlsButton = AppTheme.createSecondaryButton("Open Controls");
        AppTheme.makeButtonFillWidth(residentsButton);
        AppTheme.makeButtonFillWidth(controlsButton);
        residentsButton.addActionListener(event -> selectSection(Section.RESIDENTS));
        controlsButton.addActionListener(event -> selectSection(Section.CONTROLS));
        card.add(residentsButton);
        AppTheme.addVerticalSpacing(card, 8);
        card.add(controlsButton);
        return card;
    }

    private JPanel createOverviewAttentionCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Attention needed"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Collection watchlist"));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(overviewAttentionArea);
        return card;
    }

    private JPanel createStatusRailCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Status guide"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Reading the board"));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(AppTheme.createDescriptionArea("Paid accounts need no action. Pending accounts are approaching collection. Late accounts should move to Controls first."));
        AppTheme.addVerticalSpacing(card, 12);
        card.add(AppTheme.createStatusPill("PAID", AppTheme.SUCCESS_COLOR, AppTheme.SUCCESS_BACKGROUND));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createStatusPill("PENDING", AppTheme.WARNING_COLOR, AppTheme.WARNING_BACKGROUND));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createStatusPill("LATE", AppTheme.ERROR_COLOR, AppTheme.ERROR_BACKGROUND));
        return card;
    }

    private JPanel createSearchControls(LandlordActions actions) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Residents search"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Search and filter"));
        AppTheme.addVerticalSpacing(card, 12);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchRow.setOpaque(false);
        searchRow.add(AppTheme.createFieldGroup("Tenant or room", searchField));
        JButton searchButton = AppTheme.createPrimaryButton("Search");
        JButton clearButton = AppTheme.createSecondaryButton("Clear");
        searchButton.addActionListener(event -> actions.searchLandlordTenants(searchField.getText()));
        clearButton.addActionListener(event -> {
        clearSearch();
        actions.searchLandlordTenants("");
        });
        searchRow.add(searchButton);
        searchRow.add(clearButton);
        card.add(searchRow);
        AppTheme.addVerticalSpacing(card, 14);

        ButtonGroup filterGroup = new ButtonGroup();
        allFilterButton = createFilterButton("All");
        JToggleButton paidButton = createFilterButton("Paid");
        JToggleButton pendingButton = createFilterButton("Pending");
        JToggleButton lateButton = createFilterButton("Late");
        filterGroup.add(allFilterButton);
        filterGroup.add(paidButton);
        filterGroup.add(pendingButton);
        filterGroup.add(lateButton);
        allFilterButton.setSelected(true);

        allFilterButton.addActionListener(event -> updateStatusFilter(null));
        paidButton.addActionListener(event -> updateStatusFilter(PaymentStatus.PAID));
        pendingButton.addActionListener(event -> updateStatusFilter(PaymentStatus.PENDING));
        lateButton.addActionListener(event -> updateStatusFilter(PaymentStatus.LATE));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.add(AppTheme.createFormLabel("Quick filter"));
        filterRow.add(allFilterButton);
        filterRow.add(paidButton);
        filterRow.add(pendingButton);
        filterRow.add(lateButton);
        card.add(filterRow);
        return card;
    }

    private JPanel createResidentTableCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(AppTheme.createSubheaderLabel("Resident records"), BorderLayout.NORTH);
        card.add(AppTheme.createTableScrollPane(tenantTable), BorderLayout.CENTER);
        card.add(tableEmptyLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createResidentsSummaryCard(LandlordActions actions) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Residents rail"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Search summary"));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(residentsSummaryArea);
        AppTheme.addVerticalSpacing(card, 14);

        JButton refreshButton = AppTheme.createPrimaryButton("Refresh Data");
        JButton controlsButton = AppTheme.createSecondaryButton("Jump to Controls");
        AppTheme.makeButtonFillWidth(refreshButton);
        AppTheme.makeButtonFillWidth(controlsButton);
        refreshButton.addActionListener(event -> actions.refreshLandlordDashboard());
        controlsButton.addActionListener(event -> selectSection(Section.CONTROLS));

        card.add(refreshButton);
        AppTheme.addVerticalSpacing(card, 8);
        card.add(controlsButton);
        AppTheme.addVerticalSpacing(card, 12);
        card.add(AppTheme.createDescriptionArea("Use this rail for quick status context while the resident table stays the primary workspace."));
        return card;
    }

    private JPanel createSelectedTenantCard() {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(AppTheme.createSubheaderLabel("Selected tenant"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(AppTheme.createFormLabel("Name"));
        AppTheme.addVerticalSpacing(body, 6);
        body.add(selectedNameValue);
        AppTheme.addVerticalSpacing(body, 12);
        body.add(AppTheme.createFormLabel("Status"));
        AppTheme.addVerticalSpacing(body, 6);
        body.add(selectedStatusBadgeHost);
        AppTheme.addVerticalSpacing(body, 12);
        body.add(AppTheme.createFormLabel("Room"));
        AppTheme.addVerticalSpacing(body, 6);
        body.add(selectedRoomValue);
        AppTheme.addVerticalSpacing(body, 12);
        body.add(AppTheme.createFormLabel("Contact"));
        AppTheme.addVerticalSpacing(body, 6);
        body.add(selectedContactValue);
        AppTheme.addVerticalSpacing(body, 12);
        body.add(AppTheme.createFormLabel("Monthly rent"));
        AppTheme.addVerticalSpacing(body, 6);
        body.add(selectedRentValue);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusControlCard(LandlordActions actions) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel("Controls"));
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createSubheaderLabel("Update payment status"));
        AppTheme.addVerticalSpacing(card, 12);
        card.add(AppTheme.createFieldGroup("New status", statusComboBox));
        AppTheme.addVerticalSpacing(card, 14);

        JButton updateButton = AppTheme.createPrimaryButton("Apply Status");
        JButton residentsButton = AppTheme.createGhostButton("Back to Residents");
        AppTheme.makeButtonFillWidth(updateButton);
        AppTheme.makeButtonFillWidth(residentsButton);
        updateButton.addActionListener(event -> actions.updateTenantStatusFromLandlord(
                getSelectedTenant(),
                (PaymentStatus) statusComboBox.getSelectedItem()));
        residentsButton.addActionListener(event -> selectSection(Section.RESIDENTS));

        card.add(updateButton);
        AppTheme.addVerticalSpacing(card, 10);
        card.add(residentsButton);
        AppTheme.addVerticalSpacing(card, 14);
        card.add(selectionHintArea);
        return card;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String helper) {
        JPanel card = AppTheme.createSurfacePanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(AppTheme.createEyebrowLabel(title));
        AppTheme.addVerticalSpacing(card, 10);
        card.add(valueLabel);
        AppTheme.addVerticalSpacing(card, 8);
        card.add(AppTheme.createDescriptionArea(helper));
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

    private void updateStatusFilter(PaymentStatus status) {
        activeStatusFilter = status;
        applyStatusFilter();
    }

    private void applyStatusFilter() {
        if (activeStatusFilter == null) {
            tableSorter.setRowFilter(null);
        } else {
            tableSorter.setRowFilter(RowFilter.regexFilter("^" + activeStatusFilter.getLabel() + "$", 6));
        }
        tableEmptyLabel.setVisible(tenantTable.getRowCount() == 0);
    }

    private void updateSelectionDetails() {
        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            selectedNameValue.setText("No tenant selected");
            selectedRoomValue.setText("Select a row from Residents.");
            selectedContactValue.setText("-");
            selectedRentValue.setText("-");
            selectionHintArea.setText("Select a tenant from the residents table to enable payment-status controls.");
            selectedStatusBadgeHost.removeAll();
            selectedStatusBadgeHost.add(AppTheme.createStatusPill("No selection", AppTheme.MUTED_TEXT_COLOR, AppTheme.MUTED_SURFACE));
            selectedStatusBadgeHost.revalidate();
            selectedStatusBadgeHost.repaint();
            return;
        }

        selectedNameValue.setText(tenant.getFullName());
        selectedRoomValue.setText(tenant.getRoomInfo().getRoomNumber() + " | " + tenant.getRoomInfo().getRoomType());
        selectedContactValue.setText(tenant.getContactNumber());
        selectedRentValue.setText(AppTheme.formatCurrency(tenant.getRoomInfo().getMonthlyRent()));
        selectionHintArea.setText("Selected tenant is ready for a status update. Choose a new value and apply it from this page.");
        statusComboBox.setSelectedItem(tenant.getPaymentStatus());
        selectedStatusBadgeHost.removeAll();
        selectedStatusBadgeHost.add(AppTheme.createStatusPill(
                tenant.getPaymentStatusLabel(),
                AppTheme.getPaymentStatusColor(tenant.getPaymentStatus()),
                AppTheme.getPaymentStatusBackground(tenant.getPaymentStatus())));
        selectedStatusBadgeHost.revalidate();
        selectedStatusBadgeHost.repaint();
    }

    private TenantAccount getSelectedTenant() {
        int selectedRow = tenantTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = tenantTable.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= currentTenants.size()) {
            return null;
        }
        return currentTenants.get(modelRow);
    }

    private String buildOverviewSummary(DashboardSnapshot snapshot) {
        if (snapshot.getQuery().isEmpty()) {
            return "Showing " + snapshot.getTotalTenantCount() + " resident accounts. Paid: " + snapshot.getPaidCount()
                    + ", pending: " + snapshot.getPendingCount() + ", late: " + snapshot.getLateCount() + ".";
        }
        return "Showing " + snapshot.getTenants().size() + " of " + snapshot.getTotalTenantCount() + " residents for \""
                + snapshot.getQuery() + "\". Paid: " + snapshot.getPaidCount() + ", pending: " + snapshot.getPendingCount()
                + ", late: " + snapshot.getLateCount() + ".";
    }

    private String buildAttentionSummary(DashboardSnapshot snapshot) {
        if (snapshot.getLateCount() > 0) {
            return snapshot.getLateCount()
                    + " late account(s) need immediate review. Prioritize those entries before moving on to pending accounts.";
        }
        if (snapshot.getPendingCount() > 0) {
            return snapshot.getPendingCount()
                    + " pending account(s) are still open. Review them before the due date passes.";
        }
        return "No urgent payment issues right now. The board is clear for routine monitoring.";
    }

    private String buildWorkflowSummary(DashboardSnapshot snapshot) {
        if (!snapshot.getQuery().isEmpty()) {
            return "A filtered search is active. Review the residents list, select a record, then move to Controls for payment updates.";
        }
        return "Start in Residents to search or filter the portfolio, then use Controls only when a selected tenant needs a status change.";
    }

    private String buildResidentsRailSummary(DashboardSnapshot snapshot) {
        return "Current view: "
                + snapshot.getTenants().size()
                + " visible resident(s). Paid "
                + snapshot.getPaidCount()
                + ", pending "
                + snapshot.getPendingCount()
                + ", late "
                + snapshot.getLateCount()
                + ".";
    }

    private void selectSection(Section section) {
        topTitleLabel.setText(section.title);
        topSubtitleLabel.setText(section.subtitle + "  •  " + section.description);
        contentLayout.show(contentPanel, section.name());
    }

    private JToggleButton createSidebarNavButton(String text) {
        return new SidebarNavButton(text);
    }

    private JToggleButton createFilterButton(String text) {
        return new FilterChipButton(text);
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
                graphics2d.setColor(new java.awt.Color(AppTheme.PRIMARY_COLOR.getRed(), AppTheme.PRIMARY_COLOR.getGreen(), AppTheme.PRIMARY_COLOR.getBlue(), 34));
                graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
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

    private static final class FilterChipButton extends JToggleButton {
        private FilterChipButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(AppTheme.MUTED_TEXT_COLOR);
            setFont(AppTheme.EMPHASIS_FONT.deriveFont(13f));
            setBorder(new EmptyBorder(8, 12, 8, 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                graphics2d.setColor(new java.awt.Color(AppTheme.PRIMARY_COLOR.getRed(), AppTheme.PRIMARY_COLOR.getGreen(), AppTheme.PRIMARY_COLOR.getBlue(), 34));
                graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                setForeground(AppTheme.TEXT_COLOR);
            } else if (getModel().isRollover()) {
                graphics2d.setColor(new java.awt.Color(AppTheme.PRIMARY_COLOR.getRed(), AppTheme.PRIMARY_COLOR.getGreen(), AppTheme.PRIMARY_COLOR.getBlue(), 18));
                graphics2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                setForeground(AppTheme.TEXT_COLOR);
            } else {
                graphics2d.setColor(AppTheme.BORDER_COLOR);
                graphics2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                setForeground(AppTheme.MUTED_TEXT_COLOR);
            }
            graphics2d.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class PaymentStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(CENTER);
            if (value instanceof PaymentStatus) {
                PaymentStatus status = (PaymentStatus) value;
                setText(status.getLabel());
                if (!isSelected) {
                    setForeground(AppTheme.getPaymentStatusColor(status));
                }
            }
            return this;
        }
    }
}

