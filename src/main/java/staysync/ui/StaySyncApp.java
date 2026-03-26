package staysync.ui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.URL;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import staysync.core.StaySyncService;
import staysync.core.StaySyncService.DashboardSnapshot;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.PaymentRecord;
import staysync.core.TenantAccount.PaymentStatus;

public class StaySyncApp extends Application {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private static final String TENANT_DEMO_USERNAME = "maria.s";
    private static final String TENANT_DEMO_PASSWORD = "maria123";

    private final StaySyncService staySyncService = new StaySyncService();
    private final StackPane root = new StackPane();
    private final StackPane contentHost = new StackPane();
    private final Button themeButton = new Button();

    private Stage stage;
    private Scene scene;
    private View view = View.AUTH;
    private boolean darkMode;

    private AuthTab authTab = AuthTab.LOGIN;
    private String authMessage = "";
    private boolean authMessageSuccess;
    private String loginUsername = "";
    private String loginPassword = "";
    private String registerFullName = "";
    private String registerUsername = "";
    private String registerPassword = "";
    private String registerConfirmPassword = "";
    private String registerContactNumber = "";
    private String registerRoomNumber = "";
    private String registerRoomType = StaySyncService.getRoomTypes()[0];

    private TenantAccount currentTenant;
    private TenantSection tenantSection = TenantSection.OVERVIEW;
    private String tenantMessage = "";
    private boolean tenantMessageSuccess;

    private LandlordSection landlordSection = LandlordSection.OVERVIEW;
    private String landlordQuery = "";
    private PaymentStatus landlordFilter;
    private String landlordSelectedUsername;

    private enum View {
        AUTH,
        TENANT,
        LANDLORD
    }

    private enum AuthTab {
        LOGIN,
        REGISTER
    }

    private enum TenantSection {
        OVERVIEW,
        PAYMENTS,
        ACCOUNT
    }

    private enum LandlordSection {
        OVERVIEW,
        RESIDENTS,
        CONTROLS
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        root.getStyleClass().add("app-root");

        Region backgroundLayer = new Region();
        backgroundLayer.getStyleClass().add("background-layer");

        themeButton.getStyleClass().addAll("ui-button", "theme-button");
        themeButton.setOnAction(event -> {
            darkMode = !darkMode;
            applyThemeMode();
        });

        contentHost.setMaxWidth(1280);
        StackPane.setAlignment(contentHost, Pos.TOP_CENTER);
        StackPane.setMargin(contentHost, new Insets(118, 20, 24, 20));
        StackPane.setAlignment(themeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(themeButton, new Insets(18, 28, 0, 0));

        root.getChildren().addAll(backgroundLayer, contentHost, themeButton);

        scene = new Scene(root, 1420, 900);
        scene.getStylesheets().add(resolveStylesheet());

        applyThemeMode();
        renderCurrentView();

        primaryStage.setTitle("StaySync Dorm and Apartment Management System");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(780);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void applyThemeMode() {
        root.getStyleClass().remove("dark-mode");
        if (darkMode) {
            root.getStyleClass().add("dark-mode");
        }
        themeButton.setText(darkMode ? "LIGHT MODE" : "DARK MODE");
    }

    private void renderCurrentView() {
        Node content;
        if (view == View.TENANT && currentTenant != null) {
            content = createTenantShell();
        } else if (view == View.LANDLORD) {
            content = createLandlordShell();
        } else {
            content = createAuthShell();
        }
        contentHost.getChildren().setAll(content);
    }

    private Node createAuthShell() {
        HBox shell = new HBox(18);
        shell.getStyleClass().add("auth-shell");
        shell.setFillHeight(false);

        VBox heroPanel = new VBox(18);
        heroPanel.getStyleClass().addAll("retro-panel", "hero-panel");
        heroPanel.setPrefWidth(600);
        heroPanel.setMinHeight(660);
        HBox.setHgrow(heroPanel, Priority.ALWAYS);

        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("logo-box");
        Label logoGlyph = new Label("\u2302");
        logoGlyph.getStyleClass().add("logo-glyph");
        logoBox.getChildren().add(logoGlyph);

        VBox brandCopy = new VBox(3);
        Label brandName = new Label("STAYSYNC");
        brandName.getStyleClass().add("brand-title");
        Label brandTagline = new Label("DORM AND APARTMENT OS");
        brandTagline.getStyleClass().add("eyebrow-copy");
        brandCopy.getChildren().addAll(brandName, brandTagline);

        brandRow.getChildren().addAll(logoBox, brandCopy);

        Label commandChip = new Label("8-BIT OPERATIONS CONSOLE");
        commandChip.getStyleClass().addAll("chip-label", "accent-chip");

        Label headline = new Label("MANAGE ROOMS, RENT, AND REQUESTS FROM ONE PIXEL-PERFECT COMMAND DECK.");
        headline.getStyleClass().add("hero-headline");
        headline.setWrapText(true);
        headline.setMaxWidth(Double.MAX_VALUE);

        Label intro = new Label(
                "StaySync now feels like a retro management sim without losing real workflow clarity. "
                        + "Sign in to track rooms, residents, billing, maintenance requests, and announcements from one command deck.");
        intro.setWrapText(true);
        intro.getStyleClass().add("body-copy");

        HBox rail = new HBox(10,
                createMiniCard("Player portal",
                        "Create an account, view assigned rooms, track billing, submit maintenance requests, and message management."),
                createMiniCard("Admin HUD",
                        "Control occupancy, tenant assignments, announcements, billing updates, and service queues from one dashboard."),
                createDemoCard());
        rail.setAlignment(Pos.TOP_LEFT);

        Region heroSpacer = new Region();
        VBox.setVgrow(heroSpacer, Priority.ALWAYS);

        heroPanel.getChildren().addAll(brandRow, commandChip, headline, intro, rail, heroSpacer, createHeroConsoleCard());

        VBox authPanel = new VBox(16);
        authPanel.getStyleClass().addAll("retro-panel", "auth-panel");
        authPanel.setPrefWidth(560);
        authPanel.setMinWidth(470);
        authPanel.setMinHeight(660);
        authPanel.setPadding(new Insets(36, 16, 16, 16));

        HBox tabs = new HBox(10);
        tabs.getStyleClass().add("tab-strip");
        ToggleGroup authTabGroup = new ToggleGroup();
        ToggleButton loginTabButton = createTabButton("LOGIN", authTabGroup, authTab == AuthTab.LOGIN);
        ToggleButton registerTabButton = createTabButton("CREATE ACCOUNT", authTabGroup, authTab == AuthTab.REGISTER);
        loginTabButton.setOnAction(event -> switchAuthTab(AuthTab.LOGIN));
        registerTabButton.setOnAction(event -> switchAuthTab(AuthTab.REGISTER));
        tabs.getChildren().addAll(loginTabButton, registerTabButton);

        VBox formArea = authTab == AuthTab.LOGIN ? createLoginForm() : createRegistrationForm();
        VBox protocolCard = createAuthProtocolCard();
        VBox.setMargin(protocolCard, new Insets(14, 0, 0, 0));
        Region authSpacer = new Region();
        VBox.setVgrow(authSpacer, Priority.ALWAYS);
        authPanel.getChildren().addAll(tabs, formArea, authSpacer, protocolCard);

        shell.getChildren().setAll(heroPanel, authPanel);
        return shell;
    }

    private VBox createLoginForm() {
        VBox form = new VBox(12);

        Label heading = new Label("SIGN IN");
        heading.getStyleClass().addAll("section-title", "auth-heading");
        heading.setWrapText(true);
        Label intro = new Label("Use your username and password to access your dashboard.");
        intro.setWrapText(true);
        intro.getStyleClass().add("body-copy");

        Label feedback = createFeedbackLabel(authMessage, authMessageSuccess);

        TextField usernameField = createTextField("wise");
        usernameField.setText(loginUsername);
        PasswordField passwordField = createPasswordField("1234");
        passwordField.setText(loginPassword);

        Button loginButton = createPrimaryButton("LOGIN");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> {
            loginUsername = usernameField.getText();
            loginPassword = passwordField.getText();
            handleLogin();
        });

        HBox credentialRow = new HBox(8);
        credentialRow.setAlignment(Pos.CENTER_LEFT);
        Label adminLabel = new Label("Admin account test:");
        adminLabel.getStyleClass().add("meta-copy");
        credentialRow.getChildren().addAll(
                adminLabel,
                createInlineCode(StaySyncService.getLandlordUsername()),
                new Label("/"),
                createInlineCode(StaySyncService.getLandlordPassword()));

        HBox tenantRow = new HBox(8);
        tenantRow.setAlignment(Pos.CENTER_LEFT);
        Label tenantLabel = new Label("Tenant demo:");
        tenantLabel.getStyleClass().add("meta-copy");
        tenantRow.getChildren().addAll(
                tenantLabel,
                createInlineCode(TENANT_DEMO_USERNAME),
                new Label("/"),
                createInlineCode(TENANT_DEMO_PASSWORD));

        form.getChildren().addAll(
                heading,
                intro,
                feedback,
                createFieldGroup("Username", usernameField),
                createFieldGroup("Password", passwordField),
                loginButton,
                credentialRow,
                tenantRow);
        return form;
    }

    private VBox createRegistrationForm() {
        VBox form = new VBox(12);

        Label heading = new Label("CREATE TENANT ACCOUNT");
        heading.getStyleClass().addAll("section-title", "auth-heading");
        heading.setWrapText(true);
        Label intro = new Label("Register a resident profile and immediately reuse it in the tenant dashboard.");
        intro.setWrapText(true);
        intro.getStyleClass().add("body-copy");

        Label feedback = createFeedbackLabel(authMessage, authMessageSuccess);

        TextField fullNameField = createTextField("Juan Dela Cruz");
        fullNameField.setText(registerFullName);
        TextField usernameField = createTextField("resident.username");
        usernameField.setText(registerUsername);
        PasswordField passwordField = createPasswordField("Password");
        passwordField.setText(registerPassword);
        PasswordField confirmPasswordField = createPasswordField("Confirm Password");
        confirmPasswordField.setText(registerConfirmPassword);
        TextField contactField = createTextField("09171234567");
        contactField.setText(registerContactNumber);
        TextField roomField = createTextField("A-101");
        roomField.setText(registerRoomNumber);
        ComboBox<String> roomTypeBox = new ComboBox<>(FXCollections.observableArrayList(StaySyncService.getRoomTypes()));
        roomTypeBox.getStyleClass().add("ui-combo");
        roomTypeBox.setMaxWidth(Double.MAX_VALUE);
        roomTypeBox.setValue(registerRoomType);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        grid.getColumnConstraints().addAll(column, column);
        grid.add(createFieldGroup("Full Name", fullNameField), 0, 0);
        grid.add(createFieldGroup("Username", usernameField), 1, 0);
        grid.add(createFieldGroup("Password", passwordField), 0, 1);
        grid.add(createFieldGroup("Confirm Password", confirmPasswordField), 1, 1);
        grid.add(createFieldGroup("Contact Number", contactField), 0, 2);
        grid.add(createFieldGroup("Room Number", roomField), 1, 2);
        grid.add(createFieldGroup("Room Type", roomTypeBox), 0, 3);

        Button createButton = createPrimaryButton("CREATE ACCOUNT");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> {
            registerFullName = fullNameField.getText();
            registerUsername = usernameField.getText();
            registerPassword = passwordField.getText();
            registerConfirmPassword = confirmPasswordField.getText();
            registerContactNumber = contactField.getText();
            registerRoomNumber = roomField.getText();
            registerRoomType = roomTypeBox.getValue();
            handleRegistration();
        });

        Label tip = new Label("Use a unique room number and a valid contact number so the landlord board stays clean.");
        tip.setWrapText(true);
        tip.getStyleClass().add("meta-copy");

        form.getChildren().addAll(heading, intro, feedback, grid, createButton, tip);
        return form;
    }

    private Node createTenantShell() {
        BorderPane shell = new BorderPane();
        shell.setPrefSize(1140, 760);
        shell.setLeft(createTenantSidebar());
        shell.setCenter(createTenantContent());
        return shell;
    }

    private Node createTenantSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().addAll("retro-panel", "sidebar-panel");
        sidebar.setPrefWidth(260);

        Label eyebrow = new Label("STAYSYNC");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("TENANT DECK");
        title.getStyleClass().add("section-title");
        Label tenantName = new Label(currentTenant.getFullName());
        tenantName.getStyleClass().add("sidebar-name");
        Label room = new Label(currentTenant.getRoomInfo().getRoomNumber() + " / " + currentTenant.getRoomInfo().getRoomType());
        room.getStyleClass().add("meta-copy");

        VBox userCard = createSideInfoCard("Current resident", tenantName, room,
                createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()));

        VBox nav = new VBox(8);
        nav.getChildren().addAll(
                createNavButton("OVERVIEW", tenantSection == TenantSection.OVERVIEW, () -> switchTenantSection(TenantSection.OVERVIEW)),
                createNavButton("PAYMENTS", tenantSection == TenantSection.PAYMENTS, () -> switchTenantSection(TenantSection.PAYMENTS)),
                createNavButton("ACCOUNT", tenantSection == TenantSection.ACCOUNT, () -> switchTenantSection(TenantSection.ACCOUNT)));

        Button markPaidButton = createPrimaryButton("MARK AS PAID");
        markPaidButton.setDisable(currentTenant.getPaymentStatus() == PaymentStatus.PAID);
        markPaidButton.setMaxWidth(Double.MAX_VALUE);
        markPaidButton.setOnAction(event -> {
            staySyncService.markTenantAsPaid(currentTenant);
            tenantMessage = "Payment status updated to Paid.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });

        Button signOutButton = createSecondaryButton("SIGN OUT");
        signOutButton.setMaxWidth(Double.MAX_VALUE);
        signOutButton.setOnAction(event -> signOut());

        Label note = new Label("Use Account for profile edits and password changes without leaving the retro shell.");
        note.getStyleClass().add("meta-copy");
        note.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(eyebrow, title, userCard, nav, spacer, markPaidButton, signOutButton, note);
        return sidebar;
    }

    private Node createTenantContent() {
        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(0, 0, 0, 14));

        VBox header = new VBox(10);
        header.getStyleClass().addAll("retro-panel", "content-header");
        Label title = new Label(getTenantSectionTitle());
        title.getStyleClass().add("section-title");
        Label subtitle = new Label(getTenantSectionSubtitle());
        subtitle.getStyleClass().add("body-copy");
        subtitle.setWrapText(true);
        header.getChildren().addAll(title, subtitle, createFeedbackLabel(tenantMessage, tenantMessageSuccess));

        ScrollPane scrollPane = new ScrollPane(createTenantPageBody());
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        panel.setTop(header);
        panel.setCenter(scrollPane);
        BorderPane.setMargin(header, new Insets(0, 0, 14, 0));
        return panel;
    }

    private Node createTenantPageBody() {
        VBox page = new VBox(14);
        if (tenantSection == TenantSection.PAYMENTS) {
            page.getChildren().addAll(createTenantPaymentSummary(), createTenantHistoryCard());
        } else if (tenantSection == TenantSection.ACCOUNT) {
            HBox topRow = new HBox(14, createTenantIdentityCard(), createTenantRoomCard());
            HBox.setHgrow(topRow.getChildren().get(0), Priority.ALWAYS);
            HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);
            page.getChildren().addAll(topRow, createTenantAccountActionsCard());
        } else {
            page.getChildren().addAll(createTenantHeroCard(), createTenantStatsRow(), createTenantOverviewDetailRow());
        }
        return page;
    }

    private Node createTenantHeroCard() {
        VBox card = createPanelCard("hero-panel");
        Label eyebrow = new Label("TENANT WORKSPACE");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("WELCOME BACK, " + currentTenant.getFullName().toUpperCase(Locale.ROOT));
        title.getStyleClass().add("section-title");
        Label description = new Label(currentTenant.getDueNotificationMessage());
        description.getStyleClass().add("body-copy");
        description.setWrapText(true);
        card.getChildren().addAll(eyebrow, title, description, createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()));
        return card;
    }

    private Node createTenantStatsRow() {
        HBox row = new HBox(14,
                createMetricCard("PAYMENT STATUS", currentTenant.getPaymentStatusLabel(), "Live billing state"),
                createMetricCard("MONTHLY RENT", formatCurrency(currentTenant.getRoomInfo().getMonthlyRent()), "Current room price"),
                createMetricCard("DUE DAY", "DAY " + currentTenant.getRoomInfo().getDueDay(), "Monthly cycle"),
                createMetricCard("ASSIGNED ROOM", currentTenant.getRoomInfo().getRoomNumber(), "Room reference"));
        return row;
    }

    private Node createTenantOverviewDetailRow() {
        HBox row = new HBox(14, createTenantRecentActivityCard(), createTenantProfileCard());
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private Node createTenantRecentActivityCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("RECENT ACTIVITY");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("LATEST PAYMENT UPDATE");
        title.getStyleClass().add("card-title");
        PaymentRecord latestRecord = currentTenant.getPaymentHistory().isEmpty() ? null : currentTenant.getPaymentHistory().get(0);
        Label copy = new Label(latestRecord == null
                ? "No recent payment updates yet."
                : latestRecord.getFormattedTimestamp() + "\n"
                        + latestRecord.getStatus().getLabel() + " by " + latestRecord.getUpdatedBy() + "\n"
                        + latestRecord.getNote());
        copy.getStyleClass().add("body-copy");
        copy.setWrapText(true);
        card.getChildren().addAll(eyebrow, title, copy);
        return card;
    }

    private Node createTenantProfileCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("PROFILE SNAPSHOT");
        eyebrow.getStyleClass().add("eyebrow-copy");
        card.getChildren().addAll(
                eyebrow,
                createDetailRow("FULL NAME", currentTenant.getFullName()),
                createDetailRow("USERNAME", currentTenant.getUsername()),
                createDetailRow("CONTACT", currentTenant.getContactNumber()));
        return card;
    }

    private Node createTenantPaymentSummary() {
        HBox row = new HBox(14);

        VBox reminderCard = createPanelCard("hero-panel");
        Label eyebrow = new Label("BILLING STATUS");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("CURRENT REMINDER");
        title.getStyleClass().add("card-title");
        Label description = new Label(currentTenant.getDueNotificationMessage());
        description.getStyleClass().add("body-copy");
        description.setWrapText(true);
        reminderCard.getChildren().addAll(eyebrow, title, description,
                createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()));

        VBox actionCard = createPanelCard();
        Label actionEyebrow = new Label("PAYMENT ACTION");
        actionEyebrow.getStyleClass().add("eyebrow-copy");
        Label latest = new Label("LAST STATUS UPDATE");
        latest.getStyleClass().add("card-title");
        PaymentRecord latestRecord = currentTenant.getPaymentHistory().isEmpty() ? null : currentTenant.getPaymentHistory().get(0);
        Label latestCopy = new Label(latestRecord == null
                ? "No recent payment updates yet."
                : latestRecord.getFormattedTimestamp() + "\n"
                        + latestRecord.getStatus().getLabel() + " update from " + latestRecord.getUpdatedBy() + "\n"
                        + latestRecord.getNote());
        latestCopy.getStyleClass().add("body-copy");
        latestCopy.setWrapText(true);
        Button button = createPrimaryButton("CONFIRM PAID STATUS");
        button.setDisable(currentTenant.getPaymentStatus() == PaymentStatus.PAID);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            staySyncService.markTenantAsPaid(currentTenant);
            tenantMessage = "Payment status updated to Paid.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });
        actionCard.getChildren().addAll(actionEyebrow, latest, latestCopy, button);

        HBox.setHgrow(reminderCard, Priority.ALWAYS);
        HBox.setHgrow(actionCard, Priority.ALWAYS);
        row.getChildren().addAll(reminderCard, actionCard);
        return row;
    }

    private Node createTenantHistoryCard() {
        VBox card = createPanelCard();
        Label title = new Label("PAYMENT HISTORY");
        title.getStyleClass().add("card-title");

        TableView<PaymentRecord> historyTable = new TableView<>();
        historyTable.getStyleClass().add("retro-table");
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPlaceholder(new Label("No payment activity yet."));
        historyTable.getColumns().add(createRecordColumn("Updated On", "formattedTimestamp", 150));
        historyTable.getColumns().add(createRecordColumn("Status", "status", 110));
        historyTable.getColumns().add(createRecordColumn("Updated By", "updatedBy", 110));
        historyTable.getColumns().add(createRecordColumn("Note", "note", 320));
        historyTable.getItems().setAll(currentTenant.getPaymentHistory());
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        card.getChildren().addAll(title, historyTable);
        return card;
    }

    private Node createTenantIdentityCard() {
        VBox card = createPanelCard();
        Label title = new Label("IDENTITY");
        title.getStyleClass().add("card-title");
        card.getChildren().addAll(
                title,
                createDetailRow("FULL NAME", currentTenant.getFullName()),
                createDetailRow("USERNAME", currentTenant.getUsername()),
                createDetailRow("CONTACT", currentTenant.getContactNumber()));
        return card;
    }

    private Node createTenantRoomCard() {
        VBox card = createPanelCard();
        Label title = new Label("ROOM AND BILLING");
        title.getStyleClass().add("card-title");
        card.getChildren().addAll(
                title,
                createDetailRow("ROOM NUMBER", currentTenant.getRoomInfo().getRoomNumber()),
                createDetailRow("ROOM TYPE", currentTenant.getRoomInfo().getRoomType()),
                createDetailRow("MONTHLY RENT", formatCurrency(currentTenant.getRoomInfo().getMonthlyRent())),
                createDetailRow("DUE DAY", "Every month on day " + currentTenant.getRoomInfo().getDueDay()),
                createDetailRow("PAYMENT STATUS", currentTenant.getPaymentStatusLabel()));
        return card;
    }

    private Node createTenantAccountActionsCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("ACCESS");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("ACTIONS AND SECURITY");
        title.getStyleClass().add("card-title");

        Button editButton = createSecondaryButton("EDIT PROFILE");
        editButton.setMaxWidth(Double.MAX_VALUE);
        editButton.setOnAction(event -> showProfileDialog());

        Button passwordButton = createSecondaryButton("CHANGE PASSWORD");
        passwordButton.setMaxWidth(Double.MAX_VALUE);
        passwordButton.setOnAction(event -> showPasswordDialog());

        card.getChildren().addAll(
                eyebrow,
                title,
                createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()),
                editButton,
                passwordButton,
                createMutedCopy("Use this page for profile maintenance and password changes without leaving the tenant deck."));
        return card;
    }

    private Node createLandlordShell() {
        BorderPane shell = new BorderPane();
        shell.setPrefSize(1100, 640);
        shell.setLeft(createLandlordSidebar());
        shell.setCenter(createLandlordContent());
        return shell;
    }

    private Node createLandlordSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().addAll("retro-panel", "sidebar-panel");
        sidebar.setPrefWidth(244);

        Label eyebrow = new Label("STAYSYNC");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Admin hub");
        title.getStyleClass().add("section-title");

        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        Label residentCount = new Label(snapshot.getTotalTenantCount() + " residents");
        residentCount.getStyleClass().add("sidebar-name");
        VBox summaryCard = createSideInfoCard(
                "Portfolio",
                residentCount,
                createMutedCopy(landlordQuery.isBlank() ? "Showing all records." : "Query: \"" + landlordQuery + "\""),
                null);

        VBox nav = new VBox(8);
        nav.getChildren().addAll(
                createNavButton("Overview", landlordSection == LandlordSection.OVERVIEW, () -> switchLandlordSection(LandlordSection.OVERVIEW)),
                createNavButton("Residents", landlordSection == LandlordSection.RESIDENTS, () -> switchLandlordSection(LandlordSection.RESIDENTS)),
                createNavButton("Controls", landlordSection == LandlordSection.CONTROLS, () -> switchLandlordSection(LandlordSection.CONTROLS)));

        Button refreshButton = createPrimaryButton("Refresh data");
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        refreshButton.setOnAction(event -> renderCurrentView());

        Button signOutButton = createSecondaryButton("Sign out");
        signOutButton.setMaxWidth(Double.MAX_VALUE);
        signOutButton.setOnAction(event -> signOut());

        Label note = new Label("Use Residents for discovery and Controls for payment updates so the table stays uncluttered.");
        note.getStyleClass().add("meta-copy");
        note.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(eyebrow, title, summaryCard, nav, spacer, refreshButton, signOutButton, note);
        return sidebar;
    }

    private Node createLandlordContent() {
        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(0, 0, 0, 14));

        VBox header = new VBox(10);
        header.getStyleClass().addAll("retro-panel", "content-header");
        Label title = new Label(getLandlordSectionTitle());
        title.getStyleClass().add("section-title");
        Label subtitle = new Label(getLandlordSectionSubtitle());
        subtitle.getStyleClass().add("body-copy");
        subtitle.setWrapText(true);
        header.getChildren().addAll(title, subtitle);

        ScrollPane scrollPane = new ScrollPane(createLandlordPageBody());
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);

        panel.setTop(header);
        panel.setCenter(scrollPane);
        BorderPane.setMargin(header, new Insets(0, 0, 14, 0));
        return panel;
    }

    private Node createLandlordPageBody() {
        VBox page = new VBox(14);
        if (landlordSection == LandlordSection.RESIDENTS) {
            page.getChildren().addAll(createLandlordSearchCard(), createLandlordResidentsCard());
        } else if (landlordSection == LandlordSection.CONTROLS) {
            page.getChildren().add(createLandlordControlsRow());
        } else {
            page.getChildren().addAll(createLandlordStatsRow(), createLandlordOverviewRow());
        }
        return page;
    }

    private Node createLandlordStatsRow() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        HBox row = new HBox(14,
                createMetricCard("Residents", String.valueOf(snapshot.getTotalTenantCount()), "Seeded accounts"),
                createMetricCard("Paid", String.valueOf(snapshot.getPaidCount()), "Settled this cycle"),
                createMetricCard("Pending", String.valueOf(snapshot.getPendingCount()), "Awaiting payment"),
                createMetricCard("Late", String.valueOf(snapshot.getLateCount()), "Needs action"));
        return row;
    }

    private Node createLandlordOverviewRow() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        Node summaryCard = createLandlordSummaryCard(snapshot);
        Node guideCard = createLandlordGuideCard(snapshot);
        HBox row = new HBox(14, summaryCard, guideCard);
        if (summaryCard instanceof Region summaryRegion) {
            summaryRegion.setPrefWidth(340);
            summaryRegion.setMinWidth(320);
        }
        if (guideCard instanceof Region guideRegion) {
            HBox.setHgrow(guideRegion, Priority.ALWAYS);
            guideRegion.setPrefWidth(580);
        }
        return row;
    }

    private Node createLandlordSummaryCard(DashboardSnapshot snapshot) {
        VBox card = createPanelCard("hero-panel");
        Label eyebrow = new Label("Operations summary");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Resident payment board");
        title.getStyleClass().addAll("card-title", "compact-card-title");
        title.setWrapText(true);
        Label description = new Label(buildOverviewSummary(snapshot));
        description.getStyleClass().add("body-copy");
        description.setWrapText(true);
        card.getChildren().addAll(eyebrow, title, description);
        return card;
    }

    private Node createLandlordGuideCard(DashboardSnapshot snapshot) {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Attention needed");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Collection watchlist");
        title.getStyleClass().addAll("card-title", "compact-card-title");
        title.setWrapText(true);
        Label description = new Label(buildAttentionSummary(snapshot));
        description.getStyleClass().add("body-copy");
        description.setWrapText(true);
        card.getChildren().addAll(
                eyebrow,
                title,
                description,
                createMutedCopy(buildWorkflowSummary(snapshot)));
        return card;
    }

    private Node createLandlordSearchCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Residents search");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Search and filter");
        title.getStyleClass().add("card-title");

        TextField searchField = createTextField("Search tenant or room");
        searchField.setText(landlordQuery);
        searchField.setOnAction(event -> {
            landlordQuery = searchField.getText();
            landlordSelectedUsername = null;
            renderCurrentView();
        });

        Button searchButton = createPrimaryButton("Search");
        searchButton.setOnAction(event -> {
            landlordQuery = searchField.getText();
            landlordSelectedUsername = null;
            renderCurrentView();
        });

        Button clearButton = createSecondaryButton("Clear");
        clearButton.setOnAction(event -> {
            landlordQuery = "";
            landlordFilter = null;
            landlordSelectedUsername = null;
            renderCurrentView();
        });

        HBox searchRow = new HBox(10, searchField, searchButton, clearButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox filters = new HBox(8);
        filters.getChildren().addAll(
                createFilterButton("All", landlordFilter == null, () -> setLandlordFilter(null)),
                createFilterButton("Paid", landlordFilter == PaymentStatus.PAID, () -> setLandlordFilter(PaymentStatus.PAID)),
                createFilterButton("Pending", landlordFilter == PaymentStatus.PENDING, () -> setLandlordFilter(PaymentStatus.PENDING)),
                createFilterButton("Late", landlordFilter == PaymentStatus.LATE, () -> setLandlordFilter(PaymentStatus.LATE)));

        card.getChildren().addAll(eyebrow, title, searchRow, filters);
        return card;
    }

    private Node createLandlordResidentsCard() {
        VBox card = createPanelCard();
        Label title = new Label("Resident records");
        title.getStyleClass().add("card-title");

        List<TenantAccount> tenants = getVisibleLandlordTenants();
        TableView<TenantAccount> table = new TableView<>();
        table.getStyleClass().add("retro-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No matching tenants found."));
        table.getColumns().add(createTenantColumn("Tenant Name", tenant -> tenant.getFullName(), 180));
        table.getColumns().add(createTenantColumn("Username", tenant -> tenant.getUsername(), 120));
        table.getColumns().add(createTenantColumn("Room Number", tenant -> tenant.getRoomInfo().getRoomNumber(), 110));
        table.getColumns().add(createTenantColumn("Room Type", tenant -> tenant.getRoomInfo().getRoomType(), 110));
        table.getColumns().add(createTenantColumn("Contact", tenant -> tenant.getContactNumber(), 140));
        table.getColumns().add(createTenantColumn("Monthly Rent", tenant -> formatCurrency(tenant.getRoomInfo().getMonthlyRent()), 120));
        table.getColumns().add(createTenantColumn("Payment Status", tenant -> tenant.getPaymentStatusLabel(), 120));

        ObservableList<TenantAccount> items = FXCollections.observableArrayList(tenants);
        table.setItems(items);
        if (landlordSelectedUsername != null) {
            for (TenantAccount tenant : items) {
                if (tenant.getUsername().equalsIgnoreCase(landlordSelectedUsername)) {
                    table.getSelectionModel().select(tenant);
                    break;
                }
            }
        }
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            landlordSelectedUsername = newValue == null ? null : newValue.getUsername();
        });
        table.setPrefHeight(calculateResidentTableHeight(tenants.size()));
        table.setMinHeight(Region.USE_PREF_SIZE);
        table.setMaxHeight(Region.USE_PREF_SIZE);

        Label summary = createMutedCopy("Current view: "
                + tenants.size()
                + " visible resident(s). Paid "
                + countStatus(tenants, PaymentStatus.PAID)
                + ", pending "
                + countStatus(tenants, PaymentStatus.PENDING)
                + ", late "
                + countStatus(tenants, PaymentStatus.LATE)
                + ".");

        Button controlsButton = createSecondaryButton("Jump to controls");
        controlsButton.setOnAction(event -> {
            landlordSection = LandlordSection.CONTROLS;
            renderCurrentView();
        });

        HBox lowerRow = new HBox(14, createLandlordSelectedCard(), createLandlordResidentsInsightCard(tenants));
        HBox.setHgrow(lowerRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lowerRow.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(title, table, summary, controlsButton, lowerRow);
        return card;
    }

    private Node createLandlordControlsRow() {
        HBox row = new HBox(14, createLandlordSelectedCard(), createLandlordStatusControlCard());
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        return row;
    }

    private Node createLandlordSelectedCard() {
        VBox card = createPanelCard();
        Label title = new Label("Selected tenant");
        title.getStyleClass().add("card-title");
        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            card.getChildren().addAll(
                    title,
                    createDetailRow("NAME", "No tenant selected"),
                    createDetailRow("ROOM", "Select a row from Residents."),
                    createDetailRow("CONTACT", "-"),
                    createDetailRow("MONTHLY RENT", "-"));
            return card;
        }

        card.getChildren().addAll(
                title,
                createDetailRow("NAME", tenant.getFullName()),
                createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()),
                createDetailRow("ROOM", tenant.getRoomInfo().getRoomNumber() + " / " + tenant.getRoomInfo().getRoomType()),
                createDetailRow("CONTACT", tenant.getContactNumber()),
                createDetailRow("MONTHLY RENT", formatCurrency(tenant.getRoomInfo().getMonthlyRent())));
        return card;
    }

    private Node createLandlordStatusControlCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Controls");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Update payment status");
        title.getStyleClass().add("card-title");

        TenantAccount tenant = getSelectedTenant();
        ComboBox<PaymentStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
        statusBox.getStyleClass().add("ui-combo");
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setValue(tenant == null ? PaymentStatus.PENDING : tenant.getPaymentStatus());

        Button applyButton = createPrimaryButton("Apply status");
        applyButton.setDisable(tenant == null);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(event -> {
            if (tenant != null && statusBox.getValue() != null) {
                staySyncService.updateTenantStatusFromLandlord(tenant, statusBox.getValue());
                renderCurrentView();
            }
        });

        Button residentsButton = createSecondaryButton("Back to residents");
        residentsButton.setMaxWidth(Double.MAX_VALUE);
        residentsButton.setOnAction(event -> {
            landlordSection = LandlordSection.RESIDENTS;
            renderCurrentView();
        });

        Label hint = createMutedCopy(tenant == null
                ? "Select a tenant from the residents table to enable payment-status controls."
                : "Selected tenant is ready for a status update. Choose a new value and apply it from this page.");

        card.getChildren().addAll(eyebrow, title, createFieldGroup("NEW STATUS", statusBox), applyButton, residentsButton, hint);
        return card;
    }

    private Node createLandlordResidentsInsightCard(List<TenantAccount> tenants) {
        VBox card = createPanelCard("hero-panel");
        Label eyebrow = new Label("Resident pulse");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Collection snapshot");
        title.getStyleClass().add("card-title");

        int paid = countStatus(tenants, PaymentStatus.PAID);
        int pending = countStatus(tenants, PaymentStatus.PENDING);
        int late = countStatus(tenants, PaymentStatus.LATE);

        String summaryText;
        if (late > 0) {
            summaryText = late + " resident account(s) need immediate follow-up before the next billing sweep.";
        } else if (pending > 0) {
            summaryText = pending + " resident account(s) are still pending and should be reviewed soon.";
        } else {
            summaryText = "This filtered board is clear. All visible residents are marked paid.";
        }

        HBox chips = new HBox(10,
                createConsoleStat("Paid", String.valueOf(paid)),
                createConsoleStat("Pending", String.valueOf(pending)),
                createConsoleStat("Late", String.valueOf(late)));
        chips.getStyleClass().add("console-stat-row");

        card.getChildren().addAll(
                eyebrow,
                title,
                createMutedCopy(summaryText),
                chips,
                createMutedCopy("Use Residents to scan the board quickly, then switch to Controls only when a status update is needed."));
        return card;
    }

    private void handleLogin() {
        String validationMessage = staySyncService.validateLoginCredentials(loginUsername, loginPassword);
        if (validationMessage != null) {
            showAuthMessage(validationMessage, false);
            return;
        }

        if (staySyncService.authenticateLandlord(loginUsername.trim(), loginPassword)) {
            authMessage = "";
            landlordSection = LandlordSection.OVERVIEW;
            view = View.LANDLORD;
            renderCurrentView();
            return;
        }

        TenantAccount tenant = staySyncService.authenticateTenant(loginUsername, loginPassword);
        if (tenant == null) {
            showAuthMessage("Invalid username or password.", false);
            return;
        }

        currentTenant = tenant;
        tenantSection = TenantSection.OVERVIEW;
        tenantMessage = "Signed in successfully. Your dashboard is ready.";
        tenantMessageSuccess = true;
        authMessage = "";
        view = View.TENANT;
        renderCurrentView();
    }

    private void handleRegistration() {
        String result = staySyncService.registerTenant(
                registerFullName,
                registerUsername,
                registerPassword,
                registerConfirmPassword,
                registerContactNumber,
                registerRoomNumber,
                registerRoomType);

        if (result != null) {
            showAuthMessage(result, false);
            return;
        }

        loginUsername = registerUsername;
        loginPassword = "";
        registerFullName = "";
        registerUsername = "";
        registerPassword = "";
        registerConfirmPassword = "";
        registerContactNumber = "";
        registerRoomNumber = "";
        registerRoomType = StaySyncService.getRoomTypes()[0];
        authTab = AuthTab.LOGIN;
        showAuthMessage("Registration successful. Sign in with your new tenant credentials.", true);
    }

    private void showAuthMessage(String message, boolean success) {
        authMessage = message == null ? "" : message;
        authMessageSuccess = success;
        view = View.AUTH;
        renderCurrentView();
    }

    private void switchAuthTab(AuthTab target) {
        authTab = target;
        authMessage = "";
        renderCurrentView();
    }

    private void switchTenantSection(TenantSection section) {
        tenantSection = section;
        renderCurrentView();
    }

    private void switchLandlordSection(LandlordSection section) {
        landlordSection = section;
        renderCurrentView();
    }

    private void setLandlordFilter(PaymentStatus status) {
        landlordFilter = status;
        landlordSelectedUsername = null;
        renderCurrentView();
    }

    private void signOut() {
        currentTenant = null;
        view = View.AUTH;
        authTab = AuthTab.LOGIN;
        authMessage = "";
        tenantMessage = "";
        landlordSelectedUsername = null;
        landlordQuery = "";
        landlordFilter = null;
        renderCurrentView();
    }

    private List<TenantAccount> getVisibleLandlordTenants() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        List<TenantAccount> visible = new ArrayList<>();
        for (TenantAccount tenant : snapshot.getTenants()) {
            if (landlordFilter == null || tenant.getPaymentStatus() == landlordFilter) {
                visible.add(tenant);
            }
        }
        return visible;
    }

    private TenantAccount getSelectedTenant() {
        if (landlordSelectedUsername == null) {
            return null;
        }
        for (TenantAccount tenant : staySyncService.getLandlordDashboardData(landlordQuery).getTenants()) {
            if (tenant.getUsername().equalsIgnoreCase(landlordSelectedUsername)) {
                return tenant;
            }
        }
        return null;
    }

    private void showProfileDialog() {
        if (currentTenant == null) {
            return;
        }

        Dialog<ButtonType> dialog = createDialog("Edit Tenant Profile");
        ButtonType saveType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        TextField fullNameField = createTextField("Full Name");
        fullNameField.setText(currentTenant.getFullName());
        TextField contactField = createTextField("Contact Number");
        contactField.setText(currentTenant.getContactNumber());
        TextField roomField = createTextField("Room Number");
        roomField.setText(currentTenant.getRoomInfo().getRoomNumber());
        ComboBox<String> roomTypeBox = new ComboBox<>(FXCollections.observableArrayList(StaySyncService.getRoomTypes()));
        roomTypeBox.getStyleClass().add("ui-combo");
        roomTypeBox.setMaxWidth(Double.MAX_VALUE);
        roomTypeBox.setValue(currentTenant.getRoomInfo().getRoomType());

        VBox content = new VBox(12,
                feedback,
                createFieldGroup("FULL NAME", fullNameField),
                createFieldGroup("CONTACT NUMBER", contactField),
                createFieldGroup("ROOM NUMBER", roomField),
                createFieldGroup("ROOM TYPE", roomTypeBox));
        dialog.getDialogPane().setContent(content);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String result = staySyncService.updateTenantProfile(
                    currentTenant,
                    fullNameField.getText(),
                    contactField.getText(),
                    roomField.getText(),
                    roomTypeBox.getValue());
            if (result != null) {
                feedback.getStyleClass().setAll("feedback-box", "error-box");
                feedback.setText(result);
                feedback.setVisible(true);
                feedback.setManaged(true);
                event.consume();
                return;
            }

            tenantMessage = "Tenant profile updated successfully.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });

        dialog.showAndWait();
    }

    private void showPasswordDialog() {
        if (currentTenant == null) {
            return;
        }

        Dialog<ButtonType> dialog = createDialog("Change Password");
        ButtonType saveType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        PasswordField currentField = createPasswordField("Current Password");
        PasswordField newField = createPasswordField("New Password");
        PasswordField confirmField = createPasswordField("Confirm Password");

        VBox content = new VBox(12,
                feedback,
                createFieldGroup("CURRENT PASSWORD", currentField),
                createFieldGroup("NEW PASSWORD", newField),
                createFieldGroup("CONFIRM PASSWORD", confirmField));
        dialog.getDialogPane().setContent(content);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String result = staySyncService.changeTenantPassword(
                    currentTenant,
                    currentField.getText(),
                    newField.getText(),
                    confirmField.getText());
            if (result != null) {
                feedback.getStyleClass().setAll("feedback-box", "error-box");
                feedback.setText(result);
                feedback.setVisible(true);
                feedback.setManaged(true);
                event.consume();
                return;
            }

            tenantMessage = "Password changed successfully.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });

        dialog.showAndWait();
    }

    private Dialog<ButtonType> createDialog(String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().addAll(scene.getStylesheets());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        return dialog;
    }

    private Label createFeedbackLabel(String message, boolean success) {
        Label label = new Label(message == null ? "" : message);
        label.setWrapText(true);
        label.getStyleClass().addAll("feedback-box", success ? "success-box" : "error-box");
        boolean hasMessage = message != null && !message.isBlank();
        label.setVisible(hasMessage);
        label.setManaged(hasMessage);
        return label;
    }

    private VBox createMiniCard(String title, String copy) {
        VBox card = createPanelCard("mini-panel");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("mini-title");
        Label copyLabel = createMutedCopy(copy);
        copyLabel.setWrapText(true);
        card.getChildren().addAll(titleLabel, copyLabel);
        return card;
    }

    private VBox createDemoCard() {
        VBox card = createPanelCard("mini-panel");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label title = new Label("Demo login");
        title.getStyleClass().add("mini-title");
        HBox userRow = new HBox(8, new Label("Username:"), createInlineCode(TENANT_DEMO_USERNAME));
        HBox passwordRow = new HBox(8, new Label("Password:"), createInlineCode(TENANT_DEMO_PASSWORD));
        userRow.getStyleClass().add("mini-copy-row");
        passwordRow.getStyleClass().add("mini-copy-row");
        card.getChildren().addAll(title, userRow, passwordRow);
        return card;
    }

    private VBox createHeroConsoleCard() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData("");

        VBox card = createPanelCard("console-panel");
        Label eyebrow = new Label("LIVE SYSTEM FEED");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("COMMAND DECK STATUS");
        title.getStyleClass().add("card-title");
        Label copy = createMutedCopy(
                "Portfolio sync is online with "
                        + snapshot.getTotalTenantCount()
                        + " resident profiles, "
                        + snapshot.getPendingCount()
                        + " pending payment checks, and "
                        + snapshot.getLateCount()
                        + " late alerts on the board.");

        HBox statRow = new HBox(10,
                createConsoleStat("RESIDENTS", String.valueOf(snapshot.getTotalTenantCount())),
                createConsoleStat("PAID", String.valueOf(snapshot.getPaidCount())),
                createConsoleStat("ALERTS", String.valueOf(snapshot.getPendingCount() + snapshot.getLateCount())));
        statRow.getStyleClass().add("console-stat-row");

        card.getChildren().addAll(eyebrow, title, copy, statRow);
        return card;
    }

    private VBox createAuthProtocolCard() {
        VBox card = createPanelCard("protocol-panel");
        Label eyebrow = new Label("ONBOARDING PROTOCOL");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("TENANT STARTER CHECK");
        title.getStyleClass().add("card-title");

        VBox checklist = new VBox(8,
                createProtocolRow("01", "Claim a unique room number before account creation."),
                createProtocolRow("02", "Use an active contact number so notices reach the resident deck."),
                createProtocolRow("03", "Jump into the tenant dashboard right after registration."));

        Label note = createMutedCopy("Built to feel like a small retro operations terminal, not an empty signup page.");

        card.getChildren().addAll(eyebrow, title, checklist, note);
        return card;
    }

    private VBox createConsoleStat(String labelText, String valueText) {
        VBox stat = new VBox(4);
        stat.getStyleClass().add("console-stat");
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("console-value");
        stat.getChildren().addAll(label, value);
        HBox.setHgrow(stat, Priority.ALWAYS);
        return stat;
    }

    private HBox createProtocolRow(String id, String text) {
        HBox row = new HBox(10);
        row.getStyleClass().add("protocol-row");
        Label badge = new Label(id);
        badge.getStyleClass().add("protocol-badge");
        Label copy = new Label(text);
        copy.getStyleClass().add("meta-copy");
        copy.setWrapText(true);
        row.getChildren().addAll(badge, copy);
        HBox.setHgrow(copy, Priority.ALWAYS);
        return row;
    }

    private VBox createSideInfoCard(String title, Node main, Node sub, Node extra) {
        VBox card = createPanelCard("mini-panel");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("eyebrow-copy");
        card.getChildren().add(titleLabel);
        if (main != null) {
            card.getChildren().add(main);
        }
        if (sub != null) {
            card.getChildren().add(sub);
        }
        if (extra != null) {
            card.getChildren().add(extra);
        }
        return card;
    }

    private Button createNavButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "nav-button");
        if (active) {
            button.getStyleClass().add("active-nav");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> action.run());
        return button;
    }

    private ToggleButton createTabButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.getStyleClass().add("tab-button");
        return button;
    }

    private Button createFilterButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "chip-button");
        if (active) {
            button.getStyleClass().add("active-chip");
        }
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "primary-button");
        return button;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "secondary-button");
        return button;
    }

    private VBox createFieldGroup(String labelText, Node control) {
        VBox box = new VBox(6);
        Label label = new Label(labelText.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("field-label");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        box.getChildren().addAll(label, control);
        return box;
    }

    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("ui-input");
        return field;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().add("ui-input");
        return field;
    }

    private VBox createPanelCard(String... extraClasses) {
        VBox card = new VBox(10);
        card.getStyleClass().add("retro-panel");
        for (String extraClass : extraClasses) {
            card.getStyleClass().add(extraClass);
        }
        return card;
    }

    private Label createInlineCode(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("inline-code");
        return label;
    }

    private Label createMutedCopy(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("meta-copy");
        label.setWrapText(true);
        return label;
    }

    private HBox createStatusPill(String text, PaymentStatus status) {
        HBox pill = new HBox();
        pill.setAlignment(Pos.CENTER_LEFT);
        pill.getStyleClass().addAll("status-pill", status.name().toLowerCase(Locale.ROOT));
        Label label = new Label(text.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("status-label");
        pill.getChildren().add(label);
        return pill;
    }

    private VBox createMetricCard(String title, String value, String helperText) {
        VBox card = createPanelCard();
        HBox.setHgrow(card, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("eyebrow-copy");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value");
        Label helper = createMutedCopy(helperText);
        card.getChildren().addAll(titleLabel, valueLabel, helper);
        return card;
    }

    private VBox createDetailRow(String title, String value) {
        VBox box = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("detail-value");
        valueLabel.setWrapText(true);
        box.getChildren().addAll(titleLabel, valueLabel, new Separator());
        return box;
    }

    private TableColumn<PaymentRecord, String> createRecordColumn(String title, String propertyName, double width) {
        TableColumn<PaymentRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<TenantAccount, String> createTenantColumn(String title, java.util.function.Function<TenantAccount, String> mapper, double width) {
        TableColumn<TenantAccount, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private String getTenantSectionTitle() {
        return switch (tenantSection) {
            case PAYMENTS -> "PAYMENTS";
            case ACCOUNT -> "ACCOUNT";
            default -> "OVERVIEW";
        };
    }

    private String getTenantSectionSubtitle() {
        return switch (tenantSection) {
            case PAYMENTS -> "Review payment history, due reminders, and the latest status updates.";
            case ACCOUNT -> "Manage identity details, room assignment, and access settings.";
            default -> "See your room, billing health, and profile details in one command deck.";
        };
    }

    private String getLandlordSectionTitle() {
        return switch (landlordSection) {
            case RESIDENTS -> "Residents";
            case CONTROLS -> "Controls";
            default -> "Overview";
        };
    }

    private String getLandlordSectionSubtitle() {
        return switch (landlordSection) {
            case RESIDENTS -> "Search, filter, and review tenant records without losing payment context.";
            case CONTROLS -> "Apply payment-status changes with clear selection feedback.";
            default -> "Track resident counts, search results, and payment health from one shell.";
        };
    }

    private String buildOverviewSummary(DashboardSnapshot snapshot) {
        if (snapshot.getQuery().isBlank()) {
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
        if (!snapshot.getQuery().isBlank()) {
            return "A filtered search is active. Review the residents list, select a record, then move to Controls for payment updates.";
        }
        return "Start in Residents to search or filter the portfolio, then use Controls only when a selected tenant needs a status change.";
    }

    private double calculateResidentTableHeight(int visibleTenants) {
        int rowCount = Math.max(4, Math.min(visibleTenants, 6));
        return 44 + (rowCount * 42);
    }

    private int countStatus(List<TenantAccount> tenants, PaymentStatus status) {
        int count = 0;
        for (TenantAccount tenant : tenants) {
            if (tenant.getPaymentStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    private String resolveStylesheet() {
        URL bundledStylesheet = StaySyncApp.class.getResource("staysync.css");
        if (bundledStylesheet != null) {
            return bundledStylesheet.toExternalForm();
        }
        return Path.of("src", "main", "java", "staysync", "ui", "staysync.css").toUri().toString();
    }

    @Override
    public void stop() {
        if (stage != null) {
            stage.close();
        }
    }
}
