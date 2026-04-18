package staysync.ui;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import staysync.core.StaySyncService;
import staysync.core.StaySyncService.DashboardSnapshot;
import staysync.core.StaySyncService.LandlordNotification;
import staysync.core.StaySyncService.LandlordNotificationType;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.CoOccupantRequest;
import staysync.core.TenantAccount.NotificationRecord;
import staysync.core.TenantAccount.NotificationType;
import staysync.core.TenantAccount.PaymentRecord;
import staysync.core.TenantAccount.PaymentStatus;
import staysync.core.TenantAccount.VerificationStatus;

public class StaySyncApp extends Application {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private static final String TENANT_LOGIN_SUCCESS_MESSAGE = "Signed in successfully. Your dashboard is ready.";

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
    private String registerEmail = "";
    private String registerUsername = "";
    private String registerPassword = "";
    private String registerConfirmPassword = "";
    private String registerContactNumber = "";
    private String registerRoomNumber = "TBD";
    private String registerRoomType = StaySyncService.getRoomTypes()[0];

    private TenantAccount currentTenant;
    private TenantSection tenantSection = TenantSection.OVERVIEW;
    private String tenantMessage = "";
    private boolean tenantMessageSuccess;
    private ContextMenu tenantNotificationMenu;
    private ContextMenu landlordNotificationMenu;
    private String tenantCoOccupantRequestName = "";

    private LandlordSection landlordSection = LandlordSection.OVERVIEW;
    private String landlordMessage = "";
    private boolean landlordMessageSuccess;
    private String landlordQuery = "";
    private PaymentStatus landlordFilter;
    private String landlordSelectedUsername;
    private NotificationType landlordNotificationType = NotificationType.PAYMENT_REMINDER;
    private String landlordNotificationTitle = "";
    private String landlordNotificationMessageBody = "";
    private double tenantScrollVvalue;
    private double landlordOverviewScrollVvalue;
    private double landlordResidentsScrollVvalue;
    private double landlordControlsScrollVvalue;
    private double landlordNotificationsScrollVvalue;

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
        CONTROLS,
        NOTIFICATIONS
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        root.getStyleClass().add("app-root");

        Region backgroundLayer = new Region();
        backgroundLayer.getStyleClass().add("background-layer");

        themeButton.getStyleClass().addAll("ui-button", "theme-button");
        applyButtonHoverAnimation(themeButton);
        // theme button logic for app
        themeButton.setOnAction(event -> {
            darkMode = !darkMode;
            applyThemeMode();
        });

        contentHost.setMaxWidth(1560);
        StackPane.setAlignment(contentHost, Pos.TOP_CENTER);
        updateContentHostMargin();
        StackPane.setAlignment(themeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(themeButton, new Insets(26, 34, 0, 0));

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
        themeButton.setText(darkMode ? "Light mode" : "Dark mode");
    }

    private void renderCurrentView() {
        updateContentHostMargin();
        boolean showFloatingThemeButton = view != View.AUTH;
        themeButton.setVisible(showFloatingThemeButton);
        themeButton.setManaged(showFloatingThemeButton);
        contentHost.setMaxWidth(view == View.AUTH ? 1860 : 1560);
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

    private void updateContentHostMargin() {
        Insets margin = view == View.AUTH
                ? new Insets(16, 16, 16, 16)
                : new Insets(24, 16, 16, 16);
        StackPane.setMargin(contentHost, margin);
    }

    private Node createAuthShell() {
        VBox shell = new VBox(58);
        shell.getStyleClass().addAll("screen-shell", "auth-shell");
        shell.setPadding(new Insets(24, 142, 28, 142));
        shell.setAlignment(Pos.TOP_LEFT);

        HBox topBar = new HBox();
        topBar.getStyleClass().add("auth-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox heroPanel = new VBox(24);
        heroPanel.getStyleClass().add("auth-hero-panel");
        heroPanel.setPrefWidth(840);
        heroPanel.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(heroPanel, Priority.ALWAYS);

        HBox brandRow = new HBox(14);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        StackPane logoBox = createBrandLogo();

        VBox brandCopy = new VBox(3);
        Label brandName = new Label("StaySync");
        brandName.getStyleClass().add("brand-title");
        Label brandTagline = new Label("Dorm and apartment management");
        brandTagline.getStyleClass().add("eyebrow-copy");
        brandCopy.getChildren().addAll(brandName, brandTagline);

        Region topBarSpacer = new Region();
        HBox.setHgrow(topBarSpacer, Priority.ALWAYS);
        Button authThemeButton = createAuthThemeButton();
        brandRow.getChildren().addAll(logoBox, brandCopy);
        topBar.getChildren().addAll(brandRow, topBarSpacer, authThemeButton);

        Label commandChip = new Label("PROPERTY OPERATIONS PLATFORM");
        commandChip.getStyleClass().add("hero-kicker");

        Label headline = new Label("Minimal\nhousing\nmanagement\nwith a\npremium feel.");
        headline.getStyleClass().add("hero-headline");
        headline.setWrapText(true);
        headline.setMaxWidth(560);

        Label intro = new Label(
                "Manage residents, rooms, billing, and maintenance from a calm, focused workspace designed for daily operations.");
        intro.setWrapText(true);
        intro.getStyleClass().add("body-copy");
        intro.setMaxWidth(580);
        heroPanel.getChildren().addAll(commandChip, headline, intro);

        HBox mainRow = new HBox(74);
        mainRow.getStyleClass().add("auth-main-row");
        mainRow.setAlignment(Pos.CENTER_LEFT);

        VBox authPanel = new VBox(22);
        authPanel.getStyleClass().addAll("surface-card", "auth-panel");
        authPanel.setPrefWidth(510);
        authPanel.setMinWidth(490);
        authPanel.setPadding(new Insets(24));
        HBox.setMargin(authPanel, new Insets(14, 0, 0, 0));

        HBox tabs = new HBox(10);
        tabs.getStyleClass().add("tab-strip");
        ToggleGroup authTabGroup = new ToggleGroup();
        ToggleButton loginTabButton = createTabButton("Sign in", authTabGroup, authTab == AuthTab.LOGIN);
        ToggleButton registerTabButton = createTabButton("Create account", authTabGroup, authTab == AuthTab.REGISTER);
        // auth tab button logic for sign in
        loginTabButton.setOnAction(event -> switchAuthTab(AuthTab.LOGIN));
        // auth tab button logic for registration
        registerTabButton.setOnAction(event -> switchAuthTab(AuthTab.REGISTER));
        tabs.getChildren().addAll(loginTabButton, registerTabButton);

        VBox formArea = authTab == AuthTab.LOGIN ? createLoginForm() : createRegistrationForm();
        authPanel.getChildren().addAll(tabs, formArea);

        mainRow.getChildren().addAll(heroPanel, authPanel);
        shell.getChildren().setAll(topBar, mainRow);
        return shell;
    }

    private VBox createLoginForm() {
        VBox form = new VBox(14);

        Label eyebrow = new Label("WELCOME BACK");
        eyebrow.getStyleClass().add("eyebrow-copy");

        Label heading = new Label("Sign in");
        heading.getStyleClass().addAll("section-title", "auth-heading");
        heading.setWrapText(true);
        Label intro = new Label("Access your dashboard, track resident activity, and manage billing and maintenance in one place.");
        intro.setWrapText(true);
        intro.getStyleClass().add("body-copy");

        Label feedback = createFeedbackLabel(authMessage, authMessageSuccess);

        TextField usernameField = createTextField("Enter username or email");
        usernameField.setText(loginUsername);
        PasswordField passwordField = createPasswordField("Enter password");
        passwordField.setText(loginPassword);

        Button loginButton = createPrimaryButton("Sign in");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        // login button logic for auth
        loginButton.setOnAction(event -> {
            loginUsername = usernameField.getText();
            loginPassword = passwordField.getText();
            handleLogin();
        });

        form.getChildren().addAll(
                eyebrow,
                heading,
                intro,
                feedback,
                createFieldGroup("Username or email", usernameField),
                createFieldGroup("Password", passwordField),
                loginButton);
        return form;
    }

    private VBox createRegistrationForm() {
        VBox form = new VBox(14);

        Label eyebrow = new Label("CREATE ACCOUNT");
        eyebrow.getStyleClass().add("eyebrow-copy");

        Label heading = new Label("Create account");
        heading.getStyleClass().addAll("section-title", "auth-heading");
        heading.setWrapText(true);
        Label intro = new Label("Create a resident profile and start using the tenant workspace right away.");
        intro.setWrapText(true);
        intro.getStyleClass().add("body-copy");

        Label feedback = createFeedbackLabel(authMessage, authMessageSuccess);

        TextField fullNameField = createTextField("Enter full name");
        fullNameField.setText(registerFullName);
        TextField emailField = createTextField("Enter email address");
        emailField.setText(registerEmail);
        TextField usernameField = createTextField("Choose a username");
        usernameField.setText(registerUsername);
        PasswordField passwordField = createPasswordField("Create password");
        passwordField.setText(registerPassword);
        PasswordField confirmPasswordField = createPasswordField("Confirm password");
        confirmPasswordField.setText(registerConfirmPassword);
        TextField contactField = createTextField("Enter contact number");
        contactField.setText(registerContactNumber);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        grid.getColumnConstraints().addAll(column, column);
        grid.add(createFieldGroup("Full Name", fullNameField), 0, 0);
        grid.add(createFieldGroup("Email", emailField), 1, 0);
        grid.add(createFieldGroup("Username", usernameField), 0, 1);
        grid.add(createFieldGroup("Password", passwordField), 1, 1);
        grid.add(createFieldGroup("Confirm Password", confirmPasswordField), 0, 2);
        grid.add(createFieldGroup("Contact Number", contactField), 1, 2);

        Button createButton = createPrimaryButton("Create account");
        createButton.setMaxWidth(Double.MAX_VALUE);
        // create account button logic for auth
        createButton.setOnAction(event -> {
            registerFullName = fullNameField.getText();
            registerEmail = emailField.getText();
            registerUsername = usernameField.getText();
            registerPassword = passwordField.getText();
            registerConfirmPassword = confirmPasswordField.getText();
            registerContactNumber = contactField.getText();
            handleRegistration();
        });

        form.getChildren().addAll(eyebrow, heading, intro, feedback, grid, createButton);
        return form;
    }

    private Node createTenantShell() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("workspace-shell");
        shell.setPadding(new Insets(24));
        shell.setPrefSize(1140, 760);
        shell.setLeft(createTenantSidebar());
        shell.setCenter(createTenantContent());
        return shell;
    }

    private Node createTenantSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().addAll("surface-card", "sidebar-panel");
        sidebar.setPrefWidth(272);

        Label eyebrow = new Label("STAYSYNC");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("TENANT DECK");
        title.getStyleClass().addAll("section-title", "sidebar-heading");
        title.setWrapText(true);
        title.setTextOverrun(OverrunStyle.CLIP);
        Label tenantName = new Label(getOccupantDisplayName(currentTenant));
        tenantName.getStyleClass().add("sidebar-name");
        Label room = new Label(getRoomSummary(currentTenant));
        room.getStyleClass().add("meta-copy");

        VBox userCard = createSideInfoCard("Current resident", tenantName, room,
                createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()));

        VBox nav = new VBox(8);
        nav.getChildren().addAll(
                createNavButton("OVERVIEW", tenantSection == TenantSection.OVERVIEW, () -> switchTenantSection(TenantSection.OVERVIEW)),
                createNavButton("PAYMENTS", tenantSection == TenantSection.PAYMENTS, () -> switchTenantSection(TenantSection.PAYMENTS)),
                createNavButton("ACCOUNT", tenantSection == TenantSection.ACCOUNT, () -> switchTenantSection(TenantSection.ACCOUNT)));

        Button markPaidButton = createPrimaryButton(getTenantPaymentActionLabel(currentTenant));
        markPaidButton.setDisable(!canTenantSubmitPayment(currentTenant));
        markPaidButton.setMaxWidth(Double.MAX_VALUE);
        // payment button logic for tenant
        markPaidButton.setOnAction(event -> showSubmitPaymentDialog(currentTenant));

        Button signOutButton = createSecondaryButton("SIGN OUT");
        signOutButton.setMaxWidth(Double.MAX_VALUE);
        // sign out button logic for tenant
        signOutButton.setOnAction(event -> signOut());

        Label note = new Label("Use Payments to finish one billing proof at a time, then return to Account for profile and password changes.");
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
        header.getStyleClass().addAll("surface-card", "content-header");
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(getTenantSectionTitle());
        title.getStyleClass().add("section-title");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Button bellButton = createTenantNotificationBellButton();
        Label subtitle = new Label(getTenantSectionSubtitle());
        subtitle.getStyleClass().add("body-copy");
        subtitle.setWrapText(true);
        Label feedbackLabel = createFeedbackLabel(tenantMessage, tenantMessageSuccess);
        if (tenantMessageSuccess && TENANT_LOGIN_SUCCESS_MESSAGE.equals(tenantMessage)) {
            applyAutoDismissFeedback(feedbackLabel, tenantMessage, () -> {
                tenantMessage = "";
                tenantMessageSuccess = false;
            });
        }
        titleRow.getChildren().addAll(title, titleSpacer, bellButton);
        header.getChildren().addAll(titleRow, subtitle, feedbackLabel);

        ScrollPane scrollPane = createPageScrollPane(createTenantPageBody(), tenantScrollVvalue, value -> tenantScrollVvalue = value);

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
            page.getChildren().addAll(topRow, createTenantCoOccupantCard(), createTenantAccountActionsCard());
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
                createMetricCard("MONTHLY RENT", getRentDisplay(currentTenant), getRentHelperText(currentTenant)),
                createMetricCard("DUE DAY", "DAY " + currentTenant.getRoomInfo().getDueDay(), "Monthly cycle"),
                createMetricCard("ASSIGNED ROOM", getAssignedRoomLabel(currentTenant), getRoomHelperText(currentTenant)));
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
                : buildPaymentRecordSummary(latestRecord));
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
        PaymentRecord latestReceipt = findLatestReceiptRecord(currentTenant);
        PaymentRecord latestReferenceRecord = findLatestReferenceRecord(currentTenant);
        GridPane layout = new GridPane();
        layout.getStyleClass().add("tenant-payment-layout");
        layout.setHgap(14);
        layout.setVgap(14);

        ColumnConstraints overviewColumn = new ColumnConstraints();
        overviewColumn.setPercentWidth(38);
        overviewColumn.setMinWidth(320);
        overviewColumn.setHgrow(Priority.SOMETIMES);

        ColumnConstraints workflowColumn = new ColumnConstraints();
        workflowColumn.setPercentWidth(62);
        workflowColumn.setHgrow(Priority.ALWAYS);
        layout.getColumnConstraints().addAll(overviewColumn, workflowColumn);

        VBox leftColumn = new VBox(
                14,
                createTenantPaymentCycleCard(latestReceipt),
                createTenantPaymentFactsCard(latestReferenceRecord));
        leftColumn.getStyleClass().add("tenant-payment-column");
        leftColumn.setFillWidth(true);

        Node receiptCard = createTenantPaymentReceiptCard(latestReceipt, latestReferenceRecord);
        GridPane.setHgrow(leftColumn, Priority.ALWAYS);
        GridPane.setHgrow(receiptCard, Priority.ALWAYS);
        layout.add(leftColumn, 0, 0);
        layout.add(receiptCard, 1, 0);
        return layout;
    }

    private Node createTenantHistoryCard() {
        VBox card = createPanelCard("payment-history-card");
        Label eyebrow = new Label("Timeline");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("PAYMENT HISTORY");
        title.getStyleClass().add("card-title");
        Label summary = createMutedCopy(buildPaymentHistorySummary(currentTenant.getPaymentHistory()));
        summary.getStyleClass().add("payment-history-summary");

        TableView<PaymentRecord> historyTable = new TableView<>();
        historyTable.getStyleClass().addAll("data-table", "payment-history-table");
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPlaceholder(createPaymentHistoryPlaceholder());
        historyTable.getColumns().add(createHistoryTextColumn("Updated On", "formattedTimestamp", 168, "payment-history-timestamp-cell"));
        historyTable.getColumns().add(createHistoryTextColumn("Billing Month", "billingMonth", 146, "payment-history-billing-cell"));
        historyTable.getColumns().add(createHistoryTextColumn("Amount", "formattedAmount", 130, "payment-history-amount-cell"));
        historyTable.getColumns().add(createHistoryStatusColumn("Status", 128));
        historyTable.getColumns().add(createHistoryTextColumn("Reference", "referenceNumber", 154, "payment-history-reference-cell"));
        historyTable.getColumns().add(createHistoryTextColumn("Updated By", "updatedBy", 126, "payment-history-author-cell"));
        historyTable.getColumns().add(createHistoryReceiptColumn("Receipt", 138));
        historyTable.getColumns().add(createHistoryTextColumn("Note", "note", 320, "payment-history-note-cell"));
        historyTable.getItems().setAll(currentTenant.getPaymentHistory());
        historyTable.setPrefHeight(360);
        historyTable.setMinHeight(360);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        card.getChildren().addAll(eyebrow, title, summary, historyTable);
        return card;
    }

    private Node createTenantPaymentCycleCard(PaymentRecord latestReceipt) {
        VBox card = createPanelCard("hero-panel", "tenant-payment-cycle-card");
        Label eyebrow = new Label("THIS CYCLE");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Billing status");
        title.getStyleClass().add("card-title");
        Label description = new Label(currentTenant.getDueNotificationMessage());
        description.getStyleClass().addAll("body-copy", "tenant-payment-intro");
        description.setWrapText(true);

        FlowPane badges = new FlowPane();
        badges.getStyleClass().add("tenant-payment-badge-row");
        badges.setHgap(8);
        badges.setVgap(8);
        badges.getChildren().addAll(
                createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()),
                createOverviewBadge(currentTenant.getVerificationStatusLabel(), getVerificationTone(currentTenant.getVerificationStatus())),
                createOverviewBadge(currentTenant.getCurrentBillingMonth(), "filtered"));

        VBox callout = new VBox(4);
        callout.getStyleClass().add("tenant-payment-callout");
        Label calloutLabel = new Label("NEXT STEP");
        calloutLabel.getStyleClass().add("field-label");
        Label calloutCopy = createMutedCopy(getTenantPaymentCycleCallout(latestReceipt));
        calloutCopy.getStyleClass().add("tenant-payment-callout-copy");
        callout.getChildren().addAll(calloutLabel, calloutCopy);

        card.getChildren().addAll(eyebrow, title, description, badges, callout);
        return card;
    }

    private Node createTenantPaymentFactsCard(PaymentRecord latestReferenceRecord) {
        VBox card = createPanelCard("tenant-payment-facts-card");
        Label eyebrow = new Label("BILLING SNAPSHOT");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Cycle details");
        title.getStyleClass().addAll("card-title", "compact-card-title");

        GridPane factGrid = new GridPane();
        factGrid.getStyleClass().add("tenant-payment-fact-grid");
        factGrid.setHgap(10);
        factGrid.setVgap(10);

        ColumnConstraints firstColumn = new ColumnConstraints();
        firstColumn.setPercentWidth(50);
        firstColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints secondColumn = new ColumnConstraints();
        secondColumn.setPercentWidth(50);
        secondColumn.setHgrow(Priority.ALWAYS);
        factGrid.getColumnConstraints().addAll(firstColumn, secondColumn);

        ColumnConstraints thirdColumn = new ColumnConstraints();
        thirdColumn.setPercentWidth(33.333);
        thirdColumn.setHgrow(Priority.ALWAYS);
        factGrid.getColumnConstraints().setAll(firstColumn, secondColumn, thirdColumn);

        VBox monthCard = createTenantPaymentFactCard("Billing month", currentTenant.getCurrentBillingMonth(), "Current cycle");
        VBox amountCard = createTenantPaymentFactCard("Amount due", getRentDisplay(currentTenant), "Amount that should match the receipt");
        VBox dueCard = createTenantPaymentFactCard("Due day", "Day " + currentTenant.getRoomInfo().getDueDay(), "Monthly cycle cut-off");
        VBox roomCard = createTenantPaymentFactCard("Assigned room", getAssignedRoomLabel(currentTenant), getRoomTypeDisplay(currentTenant));
        VBox referenceCard = createTenantPaymentFactCard(
                "Latest reference",
                latestReferenceRecord == null || !latestReferenceRecord.hasReferenceNumber()
                        ? "Not added yet"
                        : latestReferenceRecord.getReferenceNumber(),
                latestReferenceRecord == null || !latestReferenceRecord.hasReferenceNumber()
                        ? "Add one inside the payment flow"
                        : "Stored for the latest proof");
        VBox nextStepCard = createTenantPaymentFactCard("Next step", getTenantNextStepText(currentTenant), "What to do now");

        GridPane.setHgrow(monthCard, Priority.ALWAYS);
        GridPane.setHgrow(amountCard, Priority.ALWAYS);
        GridPane.setHgrow(dueCard, Priority.ALWAYS);
        GridPane.setHgrow(roomCard, Priority.ALWAYS);
        GridPane.setHgrow(referenceCard, Priority.ALWAYS);
        GridPane.setHgrow(nextStepCard, Priority.ALWAYS);

        factGrid.add(monthCard, 0, 0);
        factGrid.add(amountCard, 1, 0);
        factGrid.add(dueCard, 2, 0);
        factGrid.add(roomCard, 0, 1);
        factGrid.add(referenceCard, 1, 1);
        factGrid.add(nextStepCard, 2, 1);

        card.getChildren().addAll(
                eyebrow,
                title,
                factGrid,
                createMutedCopy("Keep the billing month, amount, receipt image, and reference number aligned before you send the proof."));
        return card;
    }

    private Node createTenantPaymentReceiptCard(PaymentRecord latestReceipt, PaymentRecord latestReferenceRecord) {
        boolean billingAssigned = hasBillingAssignment(currentTenant);

        VBox card = createPanelCard("controls-form-card", "tenant-payment-receipt-card");
        Label eyebrow = new Label("RECEIPT WORKFLOW");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Finish one complete payment proof");
        title.getStyleClass().add("card-title");

        Label intro = createMutedCopy(getTenantReceiptWorkflowIntro(latestReceipt));
        intro.getStyleClass().add("tenant-payment-intro");

        FlowPane badges = new FlowPane();
        badges.getStyleClass().add("tenant-payment-badge-row");
        badges.setHgap(8);
        badges.setVgap(8);
        badges.getChildren().add(createOverviewBadge(getTenantReceiptStateLabel(latestReceipt), getTenantReceiptStateTone(latestReceipt)));
        if (latestReferenceRecord != null && latestReferenceRecord.hasReferenceNumber()) {
            badges.getChildren().add(createOverviewBadge("Reference saved", "filtered"));
        }
        if (latestReceipt != null && latestReceipt.hasReceiptImage()) {
            badges.getChildren().add(createOverviewBadge("Latest file ready", "calm"));
        }
        badges.getChildren().add(createOverviewBadge(currentTenant.getCurrentBillingMonth(), "filtered"));

        VBox latestUploadCard = new VBox(4);
        latestUploadCard.getStyleClass().add("tenant-payment-callout");
        Label latestUploadLabel = new Label("CURRENT RECEIPT STATE");
        latestUploadLabel.getStyleClass().add("field-label");
        Label latestUploadCopy = createMutedCopy(latestReceipt == null
                ? "No receipt image uploaded yet. Open the payment flow to choose a clear photo or screenshot, add the reference number, and submit everything together."
                : latestReceipt.getReceiptFileName() + "\n" + latestReceipt.getFormattedTimestamp() + "\nNext step: " + getTenantNextStepText(currentTenant));
        latestUploadCopy.getStyleClass().add("tenant-payment-callout-copy");
        latestUploadCard.getChildren().addAll(latestUploadLabel, latestUploadCopy);

        Node receiptPreview = createReceiptPreview(
                latestReceipt,
                "No receipt photo sent yet. Choose an image to send proof of payment.",
                420,
                220);

        Button workflowButton = createPrimaryButton(getTenantPaymentActionLabel(currentTenant));
        workflowButton.setDisable(!billingAssigned || currentTenant.getPaymentStatus() == PaymentStatus.PAID);
        workflowButton.setMaxWidth(Double.MAX_VALUE);
        // payment workflow button logic for tenant
        workflowButton.setOnAction(event -> showSubmitPaymentDialog(currentTenant));

        Button openReceiptButton = createSecondaryButton("OPEN LAST RECEIPT");
        openReceiptButton.setDisable(latestReceipt == null || !latestReceipt.hasReceiptImage());
        openReceiptButton.setMaxWidth(Double.MAX_VALUE);
        // open receipt button logic for tenant
        openReceiptButton.setOnAction(event -> openReceiptImage(latestReceipt, false));

        HBox receiptActions = new HBox(10, workflowButton, openReceiptButton);
        receiptActions.getStyleClass().add("tenant-payment-action-row");
        HBox.setHgrow(workflowButton, Priority.ALWAYS);
        HBox.setHgrow(openReceiptButton, Priority.ALWAYS);

        Label helper = createMutedCopy(getTenantPaymentActionHelper(currentTenant));
        helper.getStyleClass().add("tenant-payment-helper");

        card.getChildren().addAll(
                eyebrow,
                title,
                intro,
                badges,
                latestUploadCard,
                receiptPreview,
                receiptActions,
                helper);
        return card;
    }

    private VBox createTenantPaymentFactCard(String labelText, String valueText, String helperText) {
        VBox card = new VBox(4);
        card.getStyleClass().add("tenant-payment-fact-card");
        card.setMaxWidth(Double.MAX_VALUE);

        Label label = new Label(labelText.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("tenant-payment-fact-value");
        value.setWrapText(true);
        Label helper = createMutedCopy(helperText);
        helper.getStyleClass().add("tenant-payment-fact-helper");

        card.getChildren().addAll(label, value, helper);
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
                createDetailRow("ROOM NUMBER", getAssignedRoomLabel(currentTenant)),
                createDetailRow("ROOM TYPE", getRoomTypeDisplay(currentTenant)),
                createDetailRow("OCCUPANTS", getOccupantDisplayName(currentTenant)),
                createDetailRow("MONTHLY RENT", getRentDisplay(currentTenant)),
                createDetailRow("DUE DAY", "Every month on day " + currentTenant.getRoomInfo().getDueDay()),
                createDetailRow("PAYMENT STATUS", currentTenant.getPaymentStatusLabel()),
                createDetailRow("VERIFICATION", currentTenant.getVerificationStatusLabel()),
                createDetailRow("BILLING MONTH", currentTenant.getCurrentBillingMonth()));
        return card;
    }

    private Node createTenantCoOccupantCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("ROOM SHARING");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("CO-OCCUPANT REQUEST");
        title.getStyleClass().add("card-title");

        CoOccupantRequest request = currentTenant.getCoOccupantRequest();
        TextField coOccupantField = createTextField("Enter co-occupant full name");
        coOccupantField.setText(resolveTenantCoOccupantFieldValue());

        Button requestButton = createPrimaryButton(request != null && request.isPending() ? "Update request" : "Send request");
        requestButton.setMaxWidth(Double.MAX_VALUE);
        requestButton.setDisable(currentTenant == null || !currentTenant.getRoomInfo().hasAssignedRoom());
        // request button logic for tenant
        requestButton.setOnAction(event -> {
            tenantCoOccupantRequestName = coOccupantField.getText();
            String result = staySyncService.submitCoOccupantRequest(currentTenant, tenantCoOccupantRequestName);
            if (result != null) {
                tenantMessage = result;
                tenantMessageSuccess = false;
                renderCurrentView();
                return;
            }

            tenantCoOccupantRequestName = "";
            tenantMessage = "Co-occupant request sent to the landlord for approval.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });

        card.getChildren().addAll(
                eyebrow,
                title,
                createDetailRow("APPROVED CO-OCCUPANT", getApprovedCoOccupantLabel(currentTenant)),
                createDetailRow("ROOM OCCUPANTS", getOccupantDisplayName(currentTenant)));

        if (request != null) {
            card.getChildren().addAll(
                    createOverviewBadge("Request " + request.getStatusLabel(), getCoOccupantRequestTone(request)),
                    createDetailRow("REQUESTED NAME", request.getRequestedName()),
                    createDetailRow("LAST UPDATE", request.getFormattedUpdatedAt()),
                    createDetailRow("NOTE", request.getNote()));
        } else {
            card.getChildren().add(createMutedCopy("No co-occupant request yet."));
        }

        card.getChildren().addAll(
                createFieldGroup("CO-OCCUPANT NAME", coOccupantField),
                requestButton,
                createMutedCopy(!currentTenant.getRoomInfo().hasAssignedRoom()
                        ? "Co-occupant requests unlock after the landlord assigns your room."
                        : "Submit one co-occupant name for landlord approval. Approved requests will appear with your room assignment."));
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
        // edit profile button logic for tenant
        editButton.setOnAction(event -> showProfileDialog());

        Button passwordButton = createSecondaryButton("CHANGE PASSWORD");
        passwordButton.setMaxWidth(Double.MAX_VALUE);
        // password button logic for tenant
        passwordButton.setOnAction(event -> showPasswordDialog());

        Button concernButton = createSecondaryButton("SEND NOTICE / CONCERN");
        concernButton.setMaxWidth(Double.MAX_VALUE);
        // concern button logic for tenant
        concernButton.setOnAction(event -> showTenantConcernDialog());

        card.getChildren().addAll(
                eyebrow,
                title,
                createStatusPill(currentTenant.getPaymentStatusLabel(), currentTenant.getPaymentStatus()),
                editButton,
                passwordButton,
                concernButton,
                createMutedCopy("Use this page for profile maintenance and password changes in one place."));
        return card;
    }

    private Node createLandlordShell() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("workspace-shell");
        shell.setPadding(new Insets(24));
        shell.setPrefSize(1160, 760);
        shell.setLeft(createLandlordSidebar());
        shell.setCenter(createLandlordContent());
        return shell;
    }

    private Node createLandlordSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.getStyleClass().addAll("surface-card", "sidebar-panel");
        sidebar.setPrefWidth(272);

        Label eyebrow = new Label("STAYSYNC");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Landlord hub");
        title.getStyleClass().addAll("section-title", "sidebar-heading");
        title.setWrapText(true);
        title.setTextOverrun(OverrunStyle.CLIP);

        VBox nav = new VBox(8);
        nav.getStyleClass().add("sidebar-nav-stack");
        nav.getChildren().addAll(
                createNavButton("Overview", landlordSection == LandlordSection.OVERVIEW, () -> switchLandlordSection(LandlordSection.OVERVIEW)),
                createNavButton("Residents", landlordSection == LandlordSection.RESIDENTS, () -> switchLandlordSection(LandlordSection.RESIDENTS)),
                createNavButton("Controls", landlordSection == LandlordSection.CONTROLS, () -> switchLandlordSection(LandlordSection.CONTROLS)),
                createNavButton("Notifications", landlordSection == LandlordSection.NOTIFICATIONS, () -> switchLandlordSection(LandlordSection.NOTIFICATIONS)));

        Button refreshButton = createPrimaryButton("Refresh data");
        refreshButton.setMaxWidth(Double.MAX_VALUE);
        // refresh button logic for landlord
        refreshButton.setOnAction(event -> renderCurrentView());

        Button resetButton = createSecondaryButton("Clear search");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        // reset button logic for landlord
        resetButton.setOnAction(event -> resetLandlordDiscovery());

        Button signOutButton = createSecondaryButton("Sign out");
        signOutButton.setMaxWidth(Double.MAX_VALUE);
        // sign out button logic for landlord
        signOutButton.setOnAction(event -> signOut());

        Label note = new Label("Use Residents to choose tenants, Controls for billing and approvals, and Notifications for reminders and updates.");
        note.getStyleClass().add("meta-copy");
        note.setWrapText(true);

        VBox navGroup = new VBox(10, createSidebarSectionLabel("Navigate"), nav);
        VBox footerActions = new VBox(10, refreshButton, resetButton, signOutButton);
        footerActions.getStyleClass().add("sidebar-footer-actions");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(
                eyebrow,
                title,
                navGroup,
                spacer,
                footerActions,
                note);
        return sidebar;
    }

    private Node createLandlordContent() {
        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(0, 0, 0, 14));

        VBox header = new VBox(12);
        header.getStyleClass().addAll("surface-card", "content-header", "landlord-content-header");
        Label eyebrow = new Label("LANDLORD WORKSPACE");
        eyebrow.getStyleClass().addAll("eyebrow-copy", "landlord-header-kicker");
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(getLandlordSectionTitle());
        title.getStyleClass().add("section-title");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Button bellButton = createLandlordNotificationBellButton();
        Label subtitle = new Label(getLandlordSectionSubtitle());
        subtitle.getStyleClass().addAll("body-copy", "landlord-section-subtitle");
        subtitle.setWrapText(true);
        titleRow.getChildren().addAll(title, titleSpacer, bellButton);
        header.getChildren().addAll(eyebrow, titleRow, subtitle, createFeedbackLabel(landlordMessage, landlordMessageSuccess));

        ScrollPane scrollPane = createPageScrollPane(createLandlordPageBody(), getLandlordScrollVvalue(), this::setLandlordScrollVvalue);

        panel.setTop(header);
        panel.setCenter(scrollPane);
        BorderPane.setMargin(header, new Insets(0, 0, 14, 0));
        return panel;
    }

    private Node createLandlordPageBody() {
        VBox page = new VBox(14);
        if (landlordSection == LandlordSection.RESIDENTS) {
            HBox topRow = new HBox(14, createLandlordSearchCard(), createLandlordResidentsWorkspaceCard());
            topRow.getStyleClass().add("landlord-residents-top-row");
            HBox.setHgrow(topRow.getChildren().get(0), Priority.ALWAYS);
            HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);
            page.getChildren().addAll(topRow, createLandlordResidentsCard());
        } else if (landlordSection == LandlordSection.NOTIFICATIONS) {
            page.getChildren().add(createLandlordNotificationsRow());
        } else if (landlordSection == LandlordSection.CONTROLS) {
            page.getChildren().add(createLandlordControlsRow());
        } else {
            page.getChildren().addAll(createLandlordStatsRow(), createLandlordOverviewRow());
        }
        return page;
    }

    private Node createLandlordStatsRow() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        VBox portfolioCard = createLandlordStatCard(
                "Portfolio",
                String.valueOf(snapshot.getTotalTenantCount()),
                "Resident accounts",
                landlordQuery.isBlank() ? "All registered residents are visible." : "Search is refining the resident list.",
                "primary");
        VBox collectionCard = createLandlordStatCard(
                "This cycle",
                formatPercentage(snapshot.getPaidCount(), snapshot.getTotalTenantCount()),
                "Collected so far",
                snapshot.getPaidCount() == snapshot.getTotalTenantCount() && snapshot.getTotalTenantCount() > 0
                        ? "The current billing cycle is fully settled."
                        : "Share of residents already marked paid.",
                "performance");
        VBox followUpCard = createLandlordStatCard(
                "Watchlist",
                String.format("%02d", snapshot.getPendingCount() + snapshot.getLateCount()),
                "Needs follow-up",
                snapshot.getLateCount() > 0
                        ? snapshot.getLateCount() + " late account(s) need immediate review."
                        : snapshot.getPendingCount() > 0
                                ? snapshot.getPendingCount() + " not-yet-paid account(s) are still due soon."
                                : "No pending or late accounts right now.",
                snapshot.getLateCount() > 0 ? "alert" : snapshot.getPendingCount() > 0 ? "warning" : "calm");
        portfolioCard.setPrefWidth(360);
        collectionCard.setPrefWidth(250);
        followUpCard.setPrefWidth(250);

        HBox row = new HBox(14, portfolioCard, collectionCard, followUpCard);
        row.getStyleClass().add("landlord-stats-row");
        return row;
    }

    private Node createLandlordOverviewRow() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        Node summaryCard = createLandlordSummaryCard(snapshot);
        Node guideCard = createLandlordGuideCard(snapshot);
        HBox row = new HBox(14, summaryCard, guideCard);
        row.getStyleClass().add("landlord-overview-row");
        if (summaryCard instanceof Region summaryRegion) {
            summaryRegion.setPrefWidth(400);
            summaryRegion.setMinWidth(360);
        }
        if (guideCard instanceof Region guideRegion) {
            HBox.setHgrow(guideRegion, Priority.ALWAYS);
            guideRegion.setPrefWidth(590);
        }
        return row;
    }

    private Node createLandlordSummaryCard(DashboardSnapshot snapshot) {
        VBox card = createPanelCard("hero-panel", "landlord-summary-card");
        HBox header = new HBox(10);
        header.getStyleClass().add("overview-header-row");
        header.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = new Label("Operations summary");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(eyebrow, spacer, createOverviewBadge(
                snapshot.getQuery().isBlank() ? "Live" : "Filtered",
                snapshot.getQuery().isBlank() ? "live" : "filtered"));

        Label title = new Label("Payment board");
        title.getStyleClass().addAll("card-title", "compact-card-title");
        title.setWrapText(true);
        Label lead = createMutedCopy(buildOverviewSummary(snapshot));
        lead.getStyleClass().add("overview-lead");

        int visibleResidents = snapshot.getQuery().isBlank() ? snapshot.getTotalTenantCount() : snapshot.getTenants().size();
        int followUpCount = snapshot.getPendingCount() + snapshot.getLateCount();
        HBox metricsRow = new HBox(10,
                createOverviewMiniCard(
                        "Visible now",
                        String.valueOf(visibleResidents),
                        snapshot.getQuery().isBlank()
                                ? "All residents"
                                : "Matches current search"),
                createOverviewMiniCard(
                        "Collection rate",
                        formatPercentage(snapshot.getPaidCount(), snapshot.getTotalTenantCount()),
                        followUpCount == 0
                                ? "No follow-up"
                                : followUpCount + " to review"));
        metricsRow.getStyleClass().add("overview-metric-row");

        FlowPane statusRow = new FlowPane();
        statusRow.getStyleClass().add("portfolio-chip-row");
        statusRow.setHgap(8);
        statusRow.setVgap(8);
        statusRow.getChildren().addAll(
                createPortfolioStatusChip("Paid", snapshot.getPaidCount(), PaymentStatus.PAID),
                createPortfolioStatusChip("Not paid yet", snapshot.getPendingCount(), PaymentStatus.PENDING),
                createPortfolioStatusChip("Late", snapshot.getLateCount(), PaymentStatus.LATE));

        VBox summaryFooter = new VBox(6);
        summaryFooter.getStyleClass().add("summary-footer-band");
        Label footerEyebrow = new Label(snapshot.getQuery().isBlank() ? "Current mode" : "Search mode");
        footerEyebrow.getStyleClass().add("eyebrow-copy");
        Label footerCopy = createMutedCopy(snapshot.getQuery().isBlank()
                ? "This board shows the full portfolio so payment distribution is easy to read at a glance."
                : "The board keeps portfolio totals visible while you review search-specific matches.");
        footerCopy.getStyleClass().add("workflow-copy");
        summaryFooter.getChildren().addAll(footerEyebrow, footerCopy);

        card.getChildren().addAll(header, title, lead, metricsRow, statusRow, summaryFooter);
        return card;
    }

    private Node createLandlordGuideCard(DashboardSnapshot snapshot) {
        VBox card = createPanelCard("landlord-guide-card");
        HBox header = new HBox(10);
        header.getStyleClass().add("overview-header-row");
        header.setAlignment(Pos.CENTER_LEFT);

        Label eyebrow = new Label("Attention needed");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(eyebrow, spacer, createOverviewBadge(
                snapshot.getLateCount() > 0 ? "Urgent" : snapshot.getPendingCount() > 0 ? "Unpaid" : "Clear",
                snapshot.getLateCount() > 0 ? "danger" : snapshot.getPendingCount() > 0 ? "warning" : "calm"));

        Label title = new Label("Collection watchlist");
        title.getStyleClass().addAll("card-title", "compact-card-title");
        title.setWrapText(true);
        Label lead = createMutedCopy(buildAttentionSummary(snapshot));
        lead.getStyleClass().add("overview-lead");

        HBox watchStatsRow = new HBox(10,
                createOverviewMiniCard(
                        "Late accounts",
                        String.format("%02d", snapshot.getLateCount()),
                        snapshot.getLateCount() > 0 ? "Review now" : "No urgent items",
                        "watch-stat-card",
                        snapshot.getLateCount() > 0 ? "danger" : "calm"),
                createOverviewMiniCard(
                        "Not paid yet",
                        String.format("%02d", snapshot.getPendingCount()),
                        snapshot.getPendingCount() > 0 ? "Due soon" : "Nothing queued",
                        "watch-stat-card",
                        snapshot.getPendingCount() > 0 ? "warning" : "calm"));
        watchStatsRow.getStyleClass().add("watch-stats-row");

        VBox workflowBand = new VBox(6);
        workflowBand.getStyleClass().add("workflow-band");
        Label workflowEyebrow = new Label(snapshot.getQuery().isBlank() ? "Next step" : "Search context");
        workflowEyebrow.getStyleClass().add("eyebrow-copy");
        Label workflowCopy = createMutedCopy(buildWorkflowSummary(snapshot));
        workflowCopy.getStyleClass().add("workflow-copy");
        workflowBand.getChildren().addAll(workflowEyebrow, workflowCopy);

        card.getChildren().addAll(
                header,
                title,
                lead,
                watchStatsRow,
                workflowBand);
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
        // search field logic for landlord
        searchField.setOnAction(event -> {
            landlordQuery = searchField.getText();
            landlordSelectedUsername = null;
            renderCurrentView();
        });

        Button searchButton = createPrimaryButton("Search");
        // search button logic for landlord
        searchButton.setOnAction(event -> {
            landlordQuery = searchField.getText();
            landlordSelectedUsername = null;
            renderCurrentView();
        });

        Button clearButton = createSecondaryButton("Clear");
        // clear button logic for landlord
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
                createFilterButton("Not paid yet", landlordFilter == PaymentStatus.PENDING, () -> setLandlordFilter(PaymentStatus.PENDING)),
                createFilterButton("Late", landlordFilter == PaymentStatus.LATE, () -> setLandlordFilter(PaymentStatus.LATE)));

        card.getChildren().addAll(eyebrow, title, searchRow, filters);
        return card;
    }

    private Node createLandlordResidentsWorkspaceCard() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        List<TenantAccount> visibleTenants = getVisibleLandlordTenants();
        TenantAccount selectedTenant = getSelectedTenant();
        int proofQueueCount = countVerificationStatus(snapshot.getTenants(), VerificationStatus.FOR_REVIEW);

        VBox card = createPanelCard("hero-panel", "residents-workspace-card");
        Label eyebrow = new Label("Residents workspace");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Keep selection and next steps in view");
        title.getStyleClass().add("card-title");

        Label summary = createMutedCopy(selectedTenant == null
                ? "Search, filter, and pick a resident first. The selected card stays visible on the right so you can review without losing context."
                : "Selected: " + getOccupantDisplayName(selectedTenant) + " | "
                        + selectedTenant.getPaymentStatusLabel() + " | "
                        + getRoomSummary(selectedTenant));

        HBox statsRow = new HBox(10,
                createOverviewMiniCard("Visible", String.valueOf(visibleTenants.size()), "Current matches"),
                createOverviewMiniCard("Proof queue", String.format("%02d", proofQueueCount), proofQueueCount > 0 ? "Review ready" : "Nothing waiting"),
                createOverviewMiniCard("Late", String.format("%02d", snapshot.getLateCount()), snapshot.getLateCount() > 0 ? "Needs action" : "No urgent items"));
        statsRow.getStyleClass().add("overview-metric-row");

        Button watchlistButton = createSecondaryButton("Open watchlist");
        watchlistButton.setMaxWidth(Double.MAX_VALUE);
        // watchlist button logic for landlord
        watchlistButton.setOnAction(event -> openLandlordWatchlist());

        Button selectedButton = createPrimaryButton("Open selected in Controls");
        selectedButton.setDisable(selectedTenant == null);
        selectedButton.setMaxWidth(Double.MAX_VALUE);
        // selected tenant button logic for landlord
        selectedButton.setOnAction(event -> openLandlordSelectedTenant());

        HBox actions = new HBox(10, watchlistButton, selectedButton);
        actions.getStyleClass().add("controls-inline-actions");
        HBox.setHgrow(watchlistButton, Priority.ALWAYS);
        HBox.setHgrow(selectedButton, Priority.ALWAYS);

        card.getChildren().addAll(eyebrow, title, summary, statsRow, actions);
        return card;
    }

    private Node createLandlordResidentsCard() {
        VBox card = createPanelCard();
        List<TenantAccount> tenants = getVisibleLandlordTenants();
        TenantAccount selectedTenant = getSelectedTenant();
        HBox titleRow = new HBox(10);
        titleRow.getStyleClass().add("overview-header-row");
        Label title = new Label("Resident records");
        title.getStyleClass().add("card-title");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        titleRow.getChildren().addAll(
                title,
                titleSpacer,
                createOverviewBadge(
                        tenants.isEmpty()
                                ? "No matches"
                                : String.format("%02d visible", tenants.size()),
                        tenants.isEmpty()
                                ? "warning"
                                : landlordQuery.isBlank() && landlordFilter == null ? "live" : "filtered"));

        VBox pickerSection = new VBox(12);
        pickerSection.getStyleClass().add("resident-picker-section");
        Label pickerEyebrow = new Label("Quick select");
        pickerEyebrow.getStyleClass().add("eyebrow-copy");
        Label pickerSummary = createMutedCopy(selectedTenant == null
                ? "Pick a resident card below to load their details and controls."
                : "Selected: " + getOccupantDisplayName(selectedTenant) + " | "
                        + getResidentPickerRoomLabel(selectedTenant) + " | "
                        + selectedTenant.getPaymentStatusLabel());

        FlowPane pickerGrid = new FlowPane();
        pickerGrid.getStyleClass().add("resident-picker-grid");
        pickerGrid.setHgap(10);
        pickerGrid.setVgap(10);
        for (TenantAccount tenant : tenants) {
            pickerGrid.getChildren().add(createResidentPickerCard(
                    tenant,
                    selectedTenant != null && tenant.getUsername().equalsIgnoreCase(selectedTenant.getUsername())));
        }
        if (tenants.isEmpty()) {
            pickerGrid.getChildren().add(createMutedCopy("No residents match the current search or filter."));
        }
        pickerSection.getChildren().addAll(pickerEyebrow, pickerSummary, pickerGrid);

        VBox sideColumn = new VBox(14, createLandlordSelectedCard(), createLandlordResidentsInsightCard(tenants));
        sideColumn.getStyleClass().add("residents-side-column");
        sideColumn.setPrefWidth(376);
        sideColumn.setMinWidth(344);

        HBox contentRow = new HBox(14, pickerSection, sideColumn);
        contentRow.getStyleClass().add("landlord-residents-layout");
        HBox.setHgrow(pickerSection, Priority.ALWAYS);

        card.getChildren().addAll(titleRow, contentRow);
        return card;
    }

    private Node createLandlordControlsRow() {
        VBox mainColumn = new VBox(14, createLandlordReviewWorkspaceCard());
        mainColumn.getStyleClass().add("controls-main-column");

        VBox sideColumn = new VBox(14, createLandlordCoOccupantCard(), createLandlordNotificationCard());
        sideColumn.getStyleClass().add("controls-secondary-column");
        sideColumn.setPrefWidth(380);
        sideColumn.setMinWidth(344);

        HBox row = new HBox(14, mainColumn, sideColumn);
        row.getStyleClass().add("landlord-controls-row");
        HBox.setHgrow(mainColumn, Priority.ALWAYS);
        return row;
    }

    private Node createLandlordReviewWorkspaceCard() {
        VBox card = createPanelCard("controls-form-card");
        Label eyebrow = new Label("Resident review workspace");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Review proof, update billing, and keep the next step visible");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            Button residentsButton = createPrimaryButton("Open residents");
            residentsButton.setMaxWidth(Double.MAX_VALUE);
            // residents button logic for landlord
            residentsButton.setOnAction(event -> switchLandlordSection(LandlordSection.RESIDENTS));

            card.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("Select one resident first. This workspace will then keep tenant details, receipt review, billing controls, and the next recommended action together."),
                    residentsButton);
            return card;
        }

        PaymentRecord latestReceipt = findLatestReceiptRecord(tenant);
        PaymentRecord latestReferenceRecord = findLatestReferenceRecord(tenant);

        HBox hero = new HBox(14);
        hero.getStyleClass().add("selected-tenant-hero");
        StackPane avatar = createResidentAvatar(getTenantInitials(tenant), "selected-tenant-avatar");
        VBox heroCopy = new VBox(4);
        heroCopy.setMaxWidth(Double.MAX_VALUE);
        Label name = new Label(getOccupantDisplayName(tenant));
        name.getStyleClass().addAll("sidebar-name", "selected-tenant-name");
        name.setWrapText(true);
        Label room = new Label(getRoomSummary(tenant) + " | @" + tenant.getUsername());
        room.getStyleClass().addAll("field-label", "selected-tenant-room");
        room.setWrapText(true);
        Label summary = createMutedCopy("Next step: " + getLandlordNextStepText(tenant));
        summary.getStyleClass().add("selected-tenant-summary");
        heroCopy.getChildren().addAll(name, room, summary);
        hero.getChildren().addAll(avatar, heroCopy);
        HBox.setHgrow(heroCopy, Priority.ALWAYS);

        FlowPane badgeRow = new FlowPane();
        badgeRow.getStyleClass().addAll("controls-badge-row", "selected-tenant-badge-row");
        badgeRow.setHgap(8);
        badgeRow.setVgap(8);
        badgeRow.getChildren().addAll(
                createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()),
                createOverviewBadge(tenant.getVerificationStatusLabel(), getVerificationTone(tenant.getVerificationStatus())),
                createOverviewBadge(tenant.getCurrentBillingMonth(), "filtered"));

        GridPane factsGrid = createLandlordFactGrid(
                createLandlordFactCell("Contact", tenant.getContactNumber()),
                createLandlordFactCell("Room", getRoomSummary(tenant)),
                createLandlordFactCell("Amount", getRentDisplay(tenant)),
                createLandlordFactCell("Billing month", tenant.getCurrentBillingMonth()),
                createLandlordFactCell("Reference", latestReferenceRecord == null || !latestReferenceRecord.hasReferenceNumber()
                        ? "Not submitted yet"
                        : latestReferenceRecord.getReferenceNumber()),
                createLandlordFactCell("Next step", getLandlordNextStepText(tenant)));

        HBox proofRow = new HBox(14);
        proofRow.getStyleClass().add("landlord-overview-row");

        VBox receiptPanel = createPanelCard("controls-receipt-card");
        receiptPanel.getChildren().addAll(
                new Label("Latest receipt"),
                createReceiptPreview(latestReceipt, "No receipt photo submitted yet. Ask the tenant to complete the payment flow first.", 340, 188));
        ((Label) receiptPanel.getChildren().get(0)).getStyleClass().add("card-title");
        if (latestReceipt != null) {
            receiptPanel.getChildren().addAll(
                    createDetailRow("SUBMITTED", latestReceipt.getFormattedTimestamp()),
                    createDetailRow("FILE", latestReceipt.getReceiptFileName().isBlank() ? "Receipt available" : latestReceipt.getReceiptFileName()),
                    createMutedCopy(latestReceipt.getNote()));
        } else {
            receiptPanel.getChildren().add(createMutedCopy("Next step: ask the tenant to choose a receipt image, enter the payment reference number, and submit the proof."));
        }

        Button openReceiptButton = createSecondaryButton("Open full photo");
        openReceiptButton.setDisable(latestReceipt == null || !latestReceipt.hasReceiptImage());
        openReceiptButton.setMaxWidth(Double.MAX_VALUE);
        // open receipt button logic for landlord
        openReceiptButton.setOnAction(event -> openReceiptImage(latestReceipt, true));
        receiptPanel.getChildren().add(openReceiptButton);

        VBox workflowPanel = createPanelCard();
        Label workflowTitle = new Label("Review context");
        workflowTitle.getStyleClass().add("card-title");
        workflowPanel.getChildren().addAll(
                workflowTitle,
                createDetailRow("VERIFICATION", tenant.getVerificationStatusLabel()),
                createDetailRow("CURRENT STATUS", tenant.getPaymentStatusLabel()),
                createDetailRow("AMOUNT", getRentDisplay(tenant)),
                createDetailRow("BILLING MONTH", tenant.getCurrentBillingMonth()),
                createDetailRow("NEXT STEP", getLandlordNextStepText(tenant)));

        HBox.setHgrow(receiptPanel, Priority.ALWAYS);
        HBox.setHgrow(workflowPanel, Priority.ALWAYS);
        proofRow.getChildren().addAll(receiptPanel, workflowPanel);

        ComboBox<PaymentStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
        statusBox.getStyleClass().add("ui-combo");
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setValue(tenant.getPaymentStatus());

        TextArea reviewNoteArea = createTextArea("Add a short review note for the tenant");
        reviewNoteArea.setPrefRowCount(3);

        TextField roomField = createTextField("Enter room number");
        roomField.setText(tenant.getRoomInfo().hasAssignedRoom() ? tenant.getRoomInfo().getRoomNumber() : "");

        ComboBox<String> roomTypeBox = new ComboBox<>(FXCollections.observableArrayList(StaySyncService.getRoomTypes()));
        roomTypeBox.getStyleClass().add("ui-combo");
        roomTypeBox.setMaxWidth(Double.MAX_VALUE);
        roomTypeBox.setValue(tenant.getRoomInfo().hasAssignedRoomType() ? tenant.getRoomInfo().getRoomType() : null);

        TextField rentField = createTextField("Enter monthly rent");
        rentField.setText(tenant.getRoomInfo().hasAssignedRent() ? formatEditableAmount(tenant.getRoomInfo().getMonthlyRent()) : "");

        Button applyButton = createPrimaryButton("Save billing state");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        // billing state button logic for landlord
        applyButton.setOnAction(event -> {
            String result = staySyncService.updateTenantStatusFromLandlord(tenant, statusBox.getValue(), reviewNoteArea.getText());
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }
            showLandlordMessage("Billing state updated for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        Button verifyButton = createSecondaryButton("Verify payment");
        verifyButton.setDisable(!tenant.isPaymentAwaitingVerification());
        verifyButton.setMaxWidth(Double.MAX_VALUE);
        // verify payment button logic for landlord
        verifyButton.setOnAction(event -> showVerifyPaymentDialog(tenant, reviewNoteArea.getText()));

        Button rejectButton = createDangerButton("Reject proof");
        rejectButton.setDisable(!tenant.isPaymentAwaitingVerification());
        rejectButton.setMaxWidth(Double.MAX_VALUE);
        // reject proof button logic for landlord
        rejectButton.setOnAction(event -> showRejectPaymentDialog(tenant, reviewNoteArea.getText()));

        Button assignRoomButton = createSecondaryButton("Save room and rent");
        assignRoomButton.setMaxWidth(Double.MAX_VALUE);
        // save room button logic for landlord
        assignRoomButton.setOnAction(event -> {
            double monthlyRent;
            try {
                monthlyRent = Double.parseDouble(rentField.getText().trim());
            } catch (NumberFormatException exception) {
                showLandlordMessage("Enter a valid rent amount.", false);
                renderCurrentView();
                return;
            }

            String result = staySyncService.updateTenantRoomAssignment(
                    tenant,
                    roomField.getText(),
                    roomTypeBox.getValue(),
                    monthlyRent);
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }

            showLandlordMessage("Room and monthly rent updated for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        Button residentsButton = createSecondaryButton("Back to residents");
        residentsButton.setMaxWidth(Double.MAX_VALUE);
        // back to residents button logic for landlord
        residentsButton.setOnAction(event -> switchLandlordSection(LandlordSection.RESIDENTS));

        Button deleteButton = createDangerButton("Delete tenant");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        // delete tenant button logic for landlord
        deleteButton.setOnAction(event -> handleTenantDeletion(tenant));

        GridPane assignmentGrid = new GridPane();
        assignmentGrid.getStyleClass().add("controls-form-grid");
        assignmentGrid.setHgap(12);
        assignmentGrid.setVgap(12);
        ColumnConstraints assignmentColumn = new ColumnConstraints();
        assignmentColumn.setPercentWidth(50);
        assignmentColumn.setHgrow(Priority.ALWAYS);
        assignmentGrid.getColumnConstraints().addAll(assignmentColumn, assignmentColumn);
        assignmentGrid.add(createFieldGroup("ROOM NUMBER", roomField), 0, 0);
        assignmentGrid.add(createFieldGroup("ROOM TYPE", roomTypeBox), 1, 0);
        Node rentGroup = createFieldGroup("MONTHLY RENT", rentField);
        GridPane.setColumnSpan(rentGroup, 2);
        assignmentGrid.add(rentGroup, 0, 1);

        GridPane actionGrid = new GridPane();
        actionGrid.getStyleClass().add("controls-action-grid");
        actionGrid.setHgap(10);
        actionGrid.setVgap(10);
        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setPercentWidth(100.0 / 3.0);
        actionColumn.setHgrow(Priority.ALWAYS);
        actionGrid.getColumnConstraints().addAll(actionColumn, actionColumn, actionColumn);
        GridPane.setHgrow(applyButton, Priority.ALWAYS);
        GridPane.setHgrow(verifyButton, Priority.ALWAYS);
        GridPane.setHgrow(rejectButton, Priority.ALWAYS);
        GridPane.setHgrow(assignRoomButton, Priority.ALWAYS);
        GridPane.setHgrow(residentsButton, Priority.ALWAYS);
        GridPane.setHgrow(deleteButton, Priority.ALWAYS);
        actionGrid.add(applyButton, 0, 0);
        actionGrid.add(verifyButton, 1, 0);
        actionGrid.add(rejectButton, 2, 0);
        actionGrid.add(assignRoomButton, 0, 1);
        actionGrid.add(residentsButton, 1, 1);
        actionGrid.add(deleteButton, 2, 1);

        card.getChildren().addAll(
                eyebrow,
                title,
                hero,
                badgeRow,
                factsGrid,
                proofRow,
                createFieldGroup("BILLING STATUS", statusBox),
                createFieldGroup("REVIEW NOTE", reviewNoteArea),
                assignmentGrid,
                actionGrid,
                createMutedCopy("This workspace keeps the receipt, current billing context, review actions, and room assignment controls together so you can finish one tenant review without leaving the page."));
        return card;
    }

    private Node createLandlordNotificationsRow() {
        VBox sideColumn = new VBox(14, createLandlordSelectedCard(), createLandlordNotificationGuideCard());
        sideColumn.getStyleClass().add("controls-secondary-column");
        sideColumn.setPrefWidth(380);
        sideColumn.setMinWidth(340);

        VBox mainColumn = new VBox(14, createLandlordNotificationCard());
        mainColumn.getStyleClass().add("controls-main-column");
        mainColumn.setPrefWidth(620);
        mainColumn.setMinWidth(520);

        HBox row = new HBox(14, sideColumn, mainColumn);
        row.getStyleClass().add("landlord-controls-row");
        HBox.setHgrow(mainColumn, Priority.ALWAYS);
        return row;
    }

    private Node createLandlordSelectedCard() {
        VBox card = createPanelCard("controls-summary-card", "controls-selected-card");
        Label eyebrow = new Label("Resident focus");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Selected tenant");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            HBox emptyState = new HBox(14);
            emptyState.getStyleClass().add("selected-tenant-hero");
            StackPane avatar = createResidentAvatar("?", "selected-tenant-avatar", "resident-avatar-empty");
            VBox emptyCopy = new VBox(4);
            emptyCopy.setMaxWidth(Double.MAX_VALUE);
            Label emptyLead = new Label("No tenant selected");
            emptyLead.getStyleClass().addAll("sidebar-name", "selected-tenant-name");
            emptyLead.setWrapText(true);
            Label emptySummary = createMutedCopy(
                    "Choose a resident from the Notifications recipient picker or the Residents cards to load contact details, billing context, and proof review controls.");
            emptySummary.getStyleClass().add("selected-tenant-summary");
            emptyCopy.getChildren().addAll(emptyLead, emptySummary, createOverviewBadge("Awaiting selection", "filtered"));
            emptyState.getChildren().addAll(avatar, emptyCopy);
            HBox.setHgrow(emptyCopy, Priority.ALWAYS);

            card.getChildren().addAll(
                    eyebrow,
                    title,
                    emptyState,
                    createLandlordFactGrid(
                            createLandlordFactCell("Name", "No tenant selected"),
                            createLandlordFactCell("Room", "Select a resident first"),
                            createLandlordFactCell("Contact", "-"),
                            createLandlordFactCell("Monthly rent", "-")));
            return card;
        }

        HBox hero = new HBox(14);
        hero.getStyleClass().add("selected-tenant-hero");
        StackPane avatar = createResidentAvatar(getTenantInitials(tenant), "selected-tenant-avatar");
        VBox identity = new VBox(4);
        identity.setMaxWidth(Double.MAX_VALUE);
        Label name = new Label(getOccupantDisplayName(tenant));
        name.getStyleClass().addAll("sidebar-name", "selected-tenant-name");
        name.setWrapText(true);
        Label room = new Label(getRoomSummary(tenant));
        room.getStyleClass().addAll("field-label", "selected-tenant-room");
        room.setWrapText(true);
        Label summary = createMutedCopy("@" + tenant.getUsername() + " | " + tenant.getCurrentBillingMonth());
        summary.getStyleClass().add("selected-tenant-summary");
        identity.getChildren().addAll(name, room, summary);
        hero.getChildren().addAll(avatar, identity);
        HBox.setHgrow(identity, Priority.ALWAYS);

        FlowPane badgeRow = new FlowPane();
        badgeRow.getStyleClass().addAll("controls-badge-row", "selected-tenant-badge-row");
        badgeRow.setHgap(8);
        badgeRow.setVgap(8);
        badgeRow.getChildren().addAll(
                createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()),
                createOverviewBadge(tenant.getVerificationStatusLabel(), getVerificationTone(tenant.getVerificationStatus())));

        GridPane factsGrid = createLandlordFactGrid(
                createLandlordFactCell("Contact", tenant.getContactNumber()),
                createLandlordFactCell("Monthly rent", getRentDisplay(tenant)),
                createLandlordFactCell("Room type", getRoomTypeDisplay(tenant)),
                createLandlordFactCell("Co-occupant", getApprovedCoOccupantLabel(tenant)),
                createLandlordFactCell("Billing month", tenant.getCurrentBillingMonth()),
                createLandlordFactCell("Next step", getLandlordNextStepText(tenant)));

        Label guidance = createMutedCopy(getResidentPickerHelperText(tenant));
        guidance.getStyleClass().add("selected-tenant-guidance");

        card.getChildren().addAll(eyebrow, title, hero, badgeRow, factsGrid, guidance);
        CoOccupantRequest request = tenant.getCoOccupantRequest();
        if (request != null) {
            card.getChildren().add(createOverviewBadge(
                    "Co-occupant request: " + request.getStatusLabel(),
                    getCoOccupantRequestTone(request)));
        }
        return card;
    }

    private Node createLandlordReceiptReviewCard() {
        VBox card = createPanelCard("controls-receipt-card");
        Label eyebrow = new Label("Receipt review");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Latest tenant receipt");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            card.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("Select a tenant from Residents to review the latest payment receipt photo."));
            return card;
        }

        PaymentRecord latestReceipt = findLatestReceiptRecord(tenant);
        if (latestReceipt == null) {
            card.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("No receipt photo has been submitted for " + tenant.getFullName() + " yet."));
            return card;
        }

        Label submittedMeta = createMutedCopy(
                latestReceipt.getFormattedTimestamp() + " | " + latestReceipt.getReceiptFileName());
        submittedMeta.getStyleClass().add("receipt-meta");
        PaymentRecord latestReferenceRecord = findLatestReferenceRecord(tenant);

        Button openButton = createSecondaryButton("Open full photo");
        openButton.setMaxWidth(Double.MAX_VALUE);
        // open photo button logic for landlord
        openButton.setOnAction(event -> openReceiptImage(latestReceipt, true));

        card.getChildren().addAll(
                eyebrow,
                title,
                createOverviewBadge(tenant.getVerificationStatusLabel(), getVerificationTone(tenant.getVerificationStatus())),
                submittedMeta,
                createDetailRow("BILLING MONTH", latestReceipt.getBillingMonth().isBlank() ? tenant.getCurrentBillingMonth() : latestReceipt.getBillingMonth()),
                createDetailRow("AMOUNT", latestReceipt.getFormattedAmount()),
                createDetailRow("REFERENCE", latestReferenceRecord == null || !latestReferenceRecord.hasReferenceNumber()
                        ? "Not submitted yet"
                        : latestReferenceRecord.getReferenceNumber()),
                createReceiptPreview(latestReceipt, "Receipt image is unavailable.", 320, 176),
                openButton,
                createMutedCopy(latestReceipt.getNote()));
        return card;
    }

    private Node createLandlordStatusControlCard() {
        VBox card = createPanelCard("controls-form-card");
        Label eyebrow = new Label("Controls");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Review proof, update billing, and assign rent");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        TenantAccount tenant = getSelectedTenant();
        ComboBox<PaymentStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
        statusBox.getStyleClass().add("ui-combo");
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setValue(tenant == null ? PaymentStatus.PENDING : tenant.getPaymentStatus());
        TextArea reviewNoteArea = createTextArea("Add a short review note for the tenant");
        reviewNoteArea.setPrefRowCount(3);

        TextField roomField = createTextField("Enter room number");
        roomField.setText(tenant == null || !tenant.getRoomInfo().hasAssignedRoom() ? "" : tenant.getRoomInfo().getRoomNumber());

        ComboBox<String> roomTypeBox = new ComboBox<>(FXCollections.observableArrayList(StaySyncService.getRoomTypes()));
        roomTypeBox.getStyleClass().add("ui-combo");
        roomTypeBox.setMaxWidth(Double.MAX_VALUE);
        roomTypeBox.setValue(tenant == null || !tenant.getRoomInfo().hasAssignedRoomType() ? null : tenant.getRoomInfo().getRoomType());

        TextField rentField = createTextField("Enter monthly rent");
        rentField.setText(tenant == null || !tenant.getRoomInfo().hasAssignedRent() ? "" : formatEditableAmount(tenant.getRoomInfo().getMonthlyRent()));

        Button applyButton = createPrimaryButton("Save billing state");
        applyButton.setDisable(tenant == null);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        // billing state button logic for landlord
        applyButton.setOnAction(event -> {
            if (tenant != null && statusBox.getValue() != null) {
                String result = staySyncService.updateTenantStatusFromLandlord(tenant, statusBox.getValue(), reviewNoteArea.getText());
                if (result != null) {
                    showLandlordMessage(result, false);
                    renderCurrentView();
                    return;
                }
                showLandlordMessage("Billing state updated for " + tenant.getFullName() + ".", true);
                renderCurrentView();
            }
        });

        Button verifyButton = createSecondaryButton("Verify payment");
        verifyButton.setDisable(tenant == null || !tenant.isPaymentAwaitingVerification());
        verifyButton.setMaxWidth(Double.MAX_VALUE);
        // verify payment button logic for landlord
        verifyButton.setOnAction(event -> showVerifyPaymentDialog(tenant, reviewNoteArea.getText()));

        Button rejectButton = createDangerButton("Reject proof");
        rejectButton.setDisable(tenant == null || !tenant.isPaymentAwaitingVerification());
        rejectButton.setMaxWidth(Double.MAX_VALUE);
        // reject proof button logic for landlord
        rejectButton.setOnAction(event -> showRejectPaymentDialog(tenant, reviewNoteArea.getText()));

        Button assignRoomButton = createSecondaryButton("Save room and rent");
        assignRoomButton.setDisable(tenant == null);
        assignRoomButton.setMaxWidth(Double.MAX_VALUE);
        // save room button logic for landlord
        assignRoomButton.setOnAction(event -> {
            if (tenant == null) {
                return;
            }

            double monthlyRent;
            try {
                monthlyRent = Double.parseDouble(rentField.getText().trim());
            } catch (NumberFormatException exception) {
                showLandlordMessage("Enter a valid rent amount.", false);
                renderCurrentView();
                return;
            }

            String result = staySyncService.updateTenantRoomAssignment(
                    tenant,
                    roomField.getText(),
                    roomTypeBox.getValue(),
                    monthlyRent);
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }

            showLandlordMessage("Room and monthly rent updated for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        Button deleteButton = createDangerButton("Delete tenant");
        deleteButton.setDisable(tenant == null);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        // delete tenant button logic for landlord
        deleteButton.setOnAction(event -> handleTenantDeletion(tenant));

        Button residentsButton = createSecondaryButton("Back to residents");
        residentsButton.setMaxWidth(Double.MAX_VALUE);
        // back to residents button logic for landlord
        residentsButton.setOnAction(event -> {
            landlordSection = LandlordSection.RESIDENTS;
            renderCurrentView();
        });

        Label hint = createMutedCopy(tenant == null
                ? "Select a tenant to enable proof review, billing, room, and rent controls."
                : tenant.isPaymentAwaitingVerification()
                        ? "Payment proof is waiting for review. Add a short note, then verify the payment or reject it with guidance."
                        : tenant.hasRejectedPaymentSubmission()
                                ? "The last proof was rejected. The tenant can upload a new receipt and resubmit when ready."
                                : "Adjust billing status or assign the tenant's room, room type, and monthly rent here.");

        Label deleteHint = createMutedCopy(tenant == null
                ? "Delete stays disabled until a tenant is selected."
                : "Deleting a tenant permanently removes the resident account and cannot be undone.");

        GridPane assignmentGrid = new GridPane();
        assignmentGrid.getStyleClass().add("controls-form-grid");
        assignmentGrid.setHgap(12);
        assignmentGrid.setVgap(12);
        ColumnConstraints assignmentColumn = new ColumnConstraints();
        assignmentColumn.setPercentWidth(50);
        assignmentColumn.setHgrow(Priority.ALWAYS);
        assignmentGrid.getColumnConstraints().addAll(assignmentColumn, assignmentColumn);
        assignmentGrid.add(createFieldGroup("ROOM NUMBER", roomField), 0, 0);
        assignmentGrid.add(createFieldGroup("ROOM TYPE", roomTypeBox), 1, 0);
        Node rentGroup = createFieldGroup("MONTHLY RENT", rentField);
        GridPane.setColumnSpan(rentGroup, 2);
        assignmentGrid.add(rentGroup, 0, 1);

        GridPane actionGrid = new GridPane();
        actionGrid.getStyleClass().add("controls-action-grid");
        actionGrid.setHgap(10);
        actionGrid.setVgap(10);
        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setPercentWidth(100.0 / 3.0);
        actionColumn.setHgrow(Priority.ALWAYS);
        actionGrid.getColumnConstraints().addAll(actionColumn, actionColumn, actionColumn);
        GridPane.setHgrow(applyButton, Priority.ALWAYS);
        GridPane.setHgrow(verifyButton, Priority.ALWAYS);
        GridPane.setHgrow(rejectButton, Priority.ALWAYS);
        GridPane.setHgrow(assignRoomButton, Priority.ALWAYS);
        GridPane.setHgrow(residentsButton, Priority.ALWAYS);
        GridPane.setHgrow(deleteButton, Priority.ALWAYS);
        actionGrid.add(applyButton, 0, 0);
        actionGrid.add(verifyButton, 1, 0);
        actionGrid.add(rejectButton, 2, 0);
        actionGrid.add(assignRoomButton, 0, 1);
        actionGrid.add(residentsButton, 1, 1);
        actionGrid.add(deleteButton, 2, 1);

        card.getChildren().addAll(
                eyebrow,
                title,
                createFieldGroup("BILLING STATUS", statusBox),
                createFieldGroup("REVIEW NOTE", reviewNoteArea),
                assignmentGrid,
                actionGrid,
                hint,
                deleteHint);
        return card;
    }

    private Node createLandlordCoOccupantCard() {
        VBox card = createPanelCard("controls-form-card", "controls-secondary-card");
        Label eyebrow = new Label("Residence approval");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Co-occupant request");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            card.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("Select a tenant from Residents to approve or reject a co-occupant request."));
            return card;
        }

        CoOccupantRequest request = tenant.getCoOccupantRequest();

        Button approveButton = createPrimaryButton("Approve request");
        approveButton.setMaxWidth(Double.MAX_VALUE);
        approveButton.setDisable(request == null || !request.isPending());
        // approve button logic for landlord
        approveButton.setOnAction(event -> {
            String result = staySyncService.approveCoOccupantRequest(tenant);
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }

            showLandlordMessage("Co-occupant request approved for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        Button rejectButton = createSecondaryButton("Reject request");
        rejectButton.setMaxWidth(Double.MAX_VALUE);
        rejectButton.setDisable(request == null || !request.isPending());
        // reject button logic for landlord
        rejectButton.setOnAction(event -> {
            String result = staySyncService.rejectCoOccupantRequest(tenant);
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }

            showLandlordMessage("Co-occupant request rejected for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        card.getChildren().addAll(
                eyebrow,
                title,
                createDetailRow("TENANT", tenant.getFullName()),
                createDetailRow("CURRENT OCCUPANTS", getOccupantDisplayName(tenant)),
                createDetailRow("APPROVED CO-OCCUPANT", getApprovedCoOccupantLabel(tenant)));

        if (request == null) {
            HBox actionRow = new HBox(10, approveButton, rejectButton);
            actionRow.getStyleClass().add("controls-inline-actions");
            HBox.setHgrow(approveButton, Priority.ALWAYS);
            HBox.setHgrow(rejectButton, Priority.ALWAYS);
            card.getChildren().addAll(
                    createMutedCopy("This tenant has not submitted a co-occupant request yet."),
                    actionRow);
            return card;
        }

        HBox actionRow = new HBox(10, approveButton, rejectButton);
        actionRow.getStyleClass().add("controls-inline-actions");
        HBox.setHgrow(approveButton, Priority.ALWAYS);
        HBox.setHgrow(rejectButton, Priority.ALWAYS);

        card.getChildren().addAll(
                createOverviewBadge("Request " + request.getStatusLabel(), getCoOccupantRequestTone(request)),
                createDetailRow("REQUESTED NAME", request.getRequestedName()),
                createDetailRow("SUBMITTED", request.getFormattedSubmittedAt()),
                createDetailRow("LAST UPDATE", request.getFormattedUpdatedAt()),
                createDetailRow("NOTE", request.getNote()),
                actionRow,
                createMutedCopy(request.isPending()
                        ? "Approving adds the co-occupant to the room display for both tenant and landlord views."
                        : "Only pending requests can be approved or rejected."));
        return card;
    }

    private Node createLandlordNotificationCard() {
        VBox card = createPanelCard("controls-form-card", "controls-notification-card");
        Label eyebrow = new Label("Notifications");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Send tenant notice");
        title.getStyleClass().add("card-title");

        TenantAccount tenant = getSelectedTenant();
        List<TenantAccount> notificationTargets = getAllLandlordTenants();

        ComboBox<NotificationType> typeBox = new ComboBox<>(FXCollections.observableArrayList(NotificationType.values()));
        typeBox.getStyleClass().add("ui-combo");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setValue(landlordNotificationType);

        TextField titleField = createTextField("Enter notice title");
        titleField.setText(landlordNotificationTitle);

        TextArea messageArea = createTextArea("Enter the message the tenant should see");
        messageArea.setText(landlordNotificationMessageBody);
        messageArea.setPrefRowCount(5);

        ComboBox<TenantAccount> recipientBox = new ComboBox<>(FXCollections.observableArrayList(notificationTargets));
        recipientBox.getStyleClass().add("ui-combo");
        recipientBox.setMaxWidth(Double.MAX_VALUE);
        recipientBox.setPromptText(notificationTargets.isEmpty() ? "No residents available" : "Select a resident");
        recipientBox.setDisable(notificationTargets.isEmpty());
        recipientBox.setVisibleRowCount(Math.min(8, Math.max(notificationTargets.size(), 1)));
        recipientBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TenantAccount value) {
                return value == null ? "" : getLandlordNotificationRecipientLabel(value);
            }

            @Override
            public TenantAccount fromString(String string) {
                return null;
            }
        });
        recipientBox.setValue(tenant);
        recipientBox.valueProperty().addListener((observable, previousTenant, nextTenant) -> {
            String previousUsername = previousTenant == null ? "" : previousTenant.getUsername();
            String nextUsername = nextTenant == null ? "" : nextTenant.getUsername();
            if (previousUsername.equalsIgnoreCase(nextUsername)) {
                return;
            }

            landlordNotificationType = typeBox.getValue();
            landlordNotificationTitle = titleField.getText();
            landlordNotificationMessageBody = messageArea.getText();
            landlordSelectedUsername = nextTenant == null ? null : nextTenant.getUsername();
            renderCurrentView();
        });

        Button sendButton = createPrimaryButton("Send notification");
        sendButton.setDisable(tenant == null);
        sendButton.setMaxWidth(Double.MAX_VALUE);
        // send notification button logic for landlord
        sendButton.setOnAction(event -> {
            landlordNotificationType = typeBox.getValue();
            landlordNotificationTitle = titleField.getText();
            landlordNotificationMessageBody = messageArea.getText();

            String result = staySyncService.sendNotificationToTenant(
                    tenant,
                    landlordNotificationType,
                    landlordNotificationTitle,
                    landlordNotificationMessageBody);
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }

            landlordNotificationType = NotificationType.PAYMENT_REMINDER;
            landlordNotificationTitle = "";
            landlordNotificationMessageBody = "";
            showLandlordMessage("Notification sent to " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        Label hint = createMutedCopy(notificationTargets.isEmpty()
                ? "No resident records are available yet. Add a tenant first before sending notices."
                : tenant == null
                        ? "Pick a resident here to send a payment reminder, maintenance notice, or general update without returning to Residents."
                : "Use this for payment reminders, maintenance announcements, or other important resident updates.");

        GridPane topFields = new GridPane();
        topFields.getStyleClass().add("controls-form-grid");
        topFields.setHgap(12);
        topFields.setVgap(12);
        ColumnConstraints notificationColumn = new ColumnConstraints();
        notificationColumn.setPercentWidth(50);
        notificationColumn.setHgrow(Priority.ALWAYS);
        topFields.getColumnConstraints().addAll(notificationColumn, notificationColumn);
        topFields.add(createFieldGroup("NOTICE TYPE", typeBox), 0, 0);
        topFields.add(createFieldGroup("TITLE", titleField), 1, 0);

        card.getChildren().addAll(
                eyebrow,
                title,
                createFieldGroup("RECIPIENT", recipientBox),
                topFields,
                createFieldGroup("MESSAGE", messageArea),
                sendButton,
                hint);
        return card;
    }

    private Node createLandlordNotificationGuideCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Messaging guide");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Keep notices separate");
        title.getStyleClass().add("card-title");
        card.getChildren().addAll(
                eyebrow,
                title,
                createMutedCopy("Use Notifications for reminders, maintenance messages, and general updates so Controls stays focused on proof review, billing, room assignment, and tenant actions."),
                createOverviewBadge("Dedicated section", "filtered"));
        return card;
    }

    private Node createLandlordResidentsInsightCard(List<TenantAccount> tenants) {
        VBox card = createPanelCard("hero-panel", "residents-insight-card");
        int totalVisible = tenants.size();
        int paid = countStatus(tenants, PaymentStatus.PAID);
        int forReview = countVerificationStatus(tenants, VerificationStatus.FOR_REVIEW);
        int late = countStatus(tenants, PaymentStatus.LATE);
        int followUp = forReview + late;

        HBox header = new HBox(12);
        header.getStyleClass().addAll("overview-header-row", "residents-insight-head");
        header.setAlignment(Pos.CENTER_LEFT);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        VBox titleCopy = new VBox(4);
        titleCopy.getStyleClass().add("residents-insight-title-copy");
        Label eyebrow = new Label("Resident payments");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Payment overview");
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        titleCopy.getChildren().addAll(eyebrow, title);
        header.getChildren().addAll(
                titleCopy,
                headerSpacer,
                createOverviewBadge(
                        followUp == 0 ? "All clear" : followUp + " follow-up",
                        late > 0 ? "danger" : forReview > 0 ? "warning" : "calm"));

        Label summary = createMutedCopy(buildResidentsInsightSummary(totalVisible, paid, forReview, late));
        summary.getStyleClass().add("residents-insight-summary");

        FlowPane contextRow = new FlowPane();
        contextRow.getStyleClass().add("residents-insight-context");
        contextRow.setHgap(8);
        contextRow.setVgap(8);
        contextRow.setPrefWrapLength(280);
        contextRow.getChildren().addAll(
                createOverviewBadge(
                        totalVisible == 0 ? "No visible residents" : String.format("%02d visible residents", totalVisible),
                        totalVisible == 0 ? "warning" : "filtered"),
                createOverviewBadge(
                        forReview == 0 ? "Proof queue clear" : String.format("%02d proofs queued", forReview),
                        forReview == 0 ? "calm" : "warning"),
                createOverviewBadge(
                        late == 0 ? "No late accounts" : String.format("%02d late accounts", late),
                        late == 0 ? "calm" : "danger"));

        HBox spotlight = new HBox(16);
        spotlight.getStyleClass().add("residents-insight-spotlight");
        spotlight.setAlignment(Pos.CENTER_LEFT);
        Label spotlightValue = new Label(String.format("%02d", forReview));
        spotlightValue.getStyleClass().add("residents-insight-spotlight-value");
        VBox spotlightCopy = new VBox(4);
        spotlightCopy.getStyleClass().add("residents-insight-spotlight-copy");
        Label spotlightLabel = new Label("Proofs awaiting review");
        spotlightLabel.getStyleClass().add("sidebar-name");
        spotlightLabel.setWrapText(true);
        Label spotlightHelper = createMutedCopy(totalVisible == 0
                ? "No residents are visible in the current result set."
                : forReview > 0
                        ? "Receipt photos and references are queued for verification."
                        : "No payment proofs are waiting right now.");
        spotlightHelper.getStyleClass().add("residents-insight-helper");
        spotlightCopy.getChildren().addAll(spotlightLabel, spotlightHelper);
        spotlight.getChildren().addAll(spotlightValue, spotlightCopy);
        HBox.setHgrow(spotlightCopy, Priority.ALWAYS);

        FlowPane statsRow = new FlowPane();
        statsRow.getStyleClass().add("residents-insight-stats");
        statsRow.setHgap(10);
        statsRow.setVgap(10);
        statsRow.setPrefWrapLength(280);
        statsRow.getChildren().addAll(
                createResidentsInsightStat("Paid", paid, paid > 0 ? "Settled" : "None yet", "paid"),
                createResidentsInsightStat("For review", forReview, forReview > 0 ? "Queued now" : "Queue clear", "pending"),
                createResidentsInsightStat("Late", late, late > 0 ? "Needs action" : "No urgent items", "late"));

        Button proofQueueButton = createPrimaryButton(forReview > 0 ? "Open proof queue" : "Open next resident");
        proofQueueButton.setDisable(totalVisible == 0);
        proofQueueButton.setMaxWidth(Double.MAX_VALUE);
        proofQueueButton.setWrapText(true);
        proofQueueButton.getStyleClass().add("residents-insight-action-button");
        // proof queue button logic for landlord
        proofQueueButton.setOnAction(event -> openLandlordProofQueue());

        Button lateAccountsButton = createSecondaryButton(late > 0 ? "Review late accounts" : "Late accounts clear");
        lateAccountsButton.setDisable(late == 0);
        lateAccountsButton.setMaxWidth(Double.MAX_VALUE);
        lateAccountsButton.setWrapText(true);
        lateAccountsButton.getStyleClass().add("residents-insight-action-button");
        // late accounts button logic for landlord
        lateAccountsButton.setOnAction(event -> setLandlordFilter(PaymentStatus.LATE));

        VBox actions = new VBox(10, proofQueueButton, lateAccountsButton);
        actions.getStyleClass().add("residents-insight-actions");
        actions.setFillWidth(true);

        card.getChildren().addAll(
                header,
                summary,
                contextRow,
                spotlight,
                statsRow,
                actions);
        return card;
    }

    private void handleLogin() {
        // login logic for auth
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
        tenantMessage = TENANT_LOGIN_SUCCESS_MESSAGE;
        tenantMessageSuccess = true;
        tenantNotificationMenu = null;
        tenantCoOccupantRequestName = "";
        authMessage = "";
        view = View.TENANT;
        renderCurrentView();
    }

    private void handleRegistration() {
        // registration logic for auth
        if (registerEmail == null || registerEmail.trim().isEmpty()) {
            showAuthMessage("Email is required.", false);
            return;
        }

        String result = staySyncService.registerTenant(
                registerFullName,
                registerEmail,
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
        registerEmail = "";
        registerUsername = "";
        registerPassword = "";
        registerConfirmPassword = "";
        registerContactNumber = "";
        registerRoomNumber = "TBD";
        registerRoomType = StaySyncService.getRoomTypes()[0];
        authTab = AuthTab.LOGIN;
        showAuthMessage("Registration successful. Sign in with your new tenant credentials.", true);
    }

    private void showAuthMessage(String message, boolean success) {
        // message logic for auth
        authMessage = message == null ? "" : message;
        authMessageSuccess = success;
        view = View.AUTH;
        renderCurrentView();
    }

    private void switchAuthTab(AuthTab target) {
        // tab switch logic for auth
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

    private void openLandlordWatchlist() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        landlordSection = LandlordSection.RESIDENTS;
        landlordSelectedUsername = null;
        if (snapshot.getLateCount() > 0) {
            landlordFilter = PaymentStatus.LATE;
        } else if (snapshot.getPendingCount() > 0) {
            landlordFilter = PaymentStatus.PENDING;
        } else {
            landlordFilter = null;
        }
        renderCurrentView();
    }

    private void openLandlordProofQueue() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        for (TenantAccount tenant : snapshot.getTenants()) {
            if (tenant.getVerificationStatus() == VerificationStatus.FOR_REVIEW) {
                openLandlordControlsFor(tenant);
                return;
            }
        }

        List<TenantAccount> visibleTenants = getVisibleLandlordTenants();
        if (!visibleTenants.isEmpty()) {
            openLandlordControlsFor(visibleTenants.get(0));
            return;
        }

        landlordSection = LandlordSection.CONTROLS;
        renderCurrentView();
    }

    private void openLandlordSelectedTenant() {
        TenantAccount tenant = getSelectedTenant();
        if (tenant != null) {
            openLandlordControlsFor(tenant);
            return;
        }

        List<TenantAccount> visibleTenants = getVisibleLandlordTenants();
        if (!visibleTenants.isEmpty()) {
            openLandlordControlsFor(visibleTenants.get(0));
            return;
        }

        landlordSection = LandlordSection.CONTROLS;
        renderCurrentView();
    }

    private void openLandlordControlsFor(TenantAccount tenant) {
        if (tenant != null) {
            landlordSelectedUsername = tenant.getUsername();
        }
        landlordSection = LandlordSection.CONTROLS;
        renderCurrentView();
    }

    private void resetLandlordDiscovery() {
        landlordSection = LandlordSection.RESIDENTS;
        landlordQuery = "";
        landlordFilter = null;
        landlordSelectedUsername = null;
        clearLandlordMessage();
        renderCurrentView();
    }

    private void setLandlordFilter(PaymentStatus status) {
        landlordFilter = status;
        landlordSelectedUsername = null;
        renderCurrentView();
    }

    private double getLandlordScrollVvalue() {
        // scroll state logic for landlord
        return switch (landlordSection) {
            case RESIDENTS -> landlordResidentsScrollVvalue;
            case CONTROLS -> landlordControlsScrollVvalue;
            case NOTIFICATIONS -> landlordNotificationsScrollVvalue;
            default -> landlordOverviewScrollVvalue;
        };
    }

    private void setLandlordScrollVvalue(double value) {
        // scroll update logic for landlord
        switch (landlordSection) {
            case RESIDENTS -> landlordResidentsScrollVvalue = value;
            case CONTROLS -> landlordControlsScrollVvalue = value;
            case NOTIFICATIONS -> landlordNotificationsScrollVvalue = value;
            default -> landlordOverviewScrollVvalue = value;
        }
    }

    private void signOut() {
        currentTenant = null;
        view = View.AUTH;
        authTab = AuthTab.LOGIN;
        authMessage = "";
        tenantMessage = "";
        tenantCoOccupantRequestName = "";
        if (tenantNotificationMenu != null) {
            tenantNotificationMenu.hide();
            tenantNotificationMenu = null;
        }
        if (landlordNotificationMenu != null) {
            landlordNotificationMenu.hide();
            landlordNotificationMenu = null;
        }
        landlordMessage = "";
        landlordSelectedUsername = null;
        landlordQuery = "";
        landlordFilter = null;
        landlordNotificationType = NotificationType.PAYMENT_REMINDER;
        landlordNotificationTitle = "";
        landlordNotificationMessageBody = "";
        tenantScrollVvalue = 0;
        landlordOverviewScrollVvalue = 0;
        landlordResidentsScrollVvalue = 0;
        landlordControlsScrollVvalue = 0;
        landlordNotificationsScrollVvalue = 0;
        renderCurrentView();
    }

    private void showLandlordMessage(String message, boolean success) {
        landlordMessage = message == null ? "" : message;
        landlordMessageSuccess = success;
    }

    private void clearLandlordMessage() {
        landlordMessage = "";
        landlordMessageSuccess = false;
    }

    private Path chooseReceiptImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose receipt photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        File initialDirectory = new File(System.getProperty("user.home", "."));
        if (initialDirectory.exists() && initialDirectory.isDirectory()) {
            chooser.setInitialDirectory(initialDirectory);
        }

        File selectedFile = chooser.showOpenDialog(stage);
        return selectedFile == null ? null : selectedFile.toPath();
    }

    private void showTenantConcernDialog() {
        if (currentTenant == null) {
            return;
        }

        Dialog<ButtonType> dialog = createDialog("Send Notice or Concern");
        ButtonType sendType = new ButtonType("Send to landlord", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(540);

        TextField titleField = createTextField("Enter a short subject");
        TextArea messageArea = createTextArea("Explain the notice or concern for the landlord");
        messageArea.setPrefRowCount(6);

        VBox content = createDialogShell(
                "Tenant message",
                "Send a notice or concern",
                "Use this for updates, issues, or questions that the landlord should see in their notification bell.",
                createFieldGroup("SUBJECT", titleField),
                createFieldGroup("MESSAGE", messageArea));
        dialog.getDialogPane().setContent(content);

        Node sendButton = dialog.getDialogPane().lookupButton(sendType);
        if (sendButton instanceof ButtonBase buttonBase) {
            buttonBase.setDisable(false);
        }
        styleDialogButtons(dialog, sendType);

        dialog.setResultConverter(buttonType -> buttonType);
        dialog.showAndWait().ifPresent(result -> {
            if (result != sendType) {
                return;
            }

            String serviceResult = staySyncService.submitTenantConcern(currentTenant, titleField.getText(), messageArea.getText());
            if (serviceResult != null) {
                tenantMessage = serviceResult;
                tenantMessageSuccess = false;
                renderCurrentView();
                return;
            }

            tenantMessage = "Your notice or concern was sent to the landlord.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });
    }

    private void showSubmitPaymentDialog(TenantAccount tenant) {
        if (tenant == null) {
            return;
        }

        boolean awaitingReview = tenant.isPaymentAwaitingVerification();
        boolean rejected = tenant.hasRejectedPaymentSubmission();
        PaymentRecord latestReceipt = findLatestReceiptRecord(tenant);
        PaymentRecord latestReferenceRecord = findLatestReferenceRecord(tenant);

        Dialog<ButtonType> dialog = createDialog(awaitingReview ? "Update Receipt Photo" : "Complete Payment Proof");
        ButtonType submitType = new ButtonType(
                awaitingReview
                        ? "Update receipt"
                        : rejected
                                ? "Replace and resubmit"
                                : "Submit proof",
                ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(560);
        styleDialogButtons(dialog, submitType);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        TextField referenceField = createTextField("Enter payment reference number");
        referenceField.setText(latestReferenceRecord != null && latestReferenceRecord.hasReferenceNumber()
                ? latestReferenceRecord.getReferenceNumber()
                : "");
        referenceField.setDisable(awaitingReview);

        Label receiptValue = new Label(latestReceipt == null || latestReceipt.getReceiptFileName().isBlank()
                ? "No receipt image selected yet."
                : latestReceipt.getReceiptFileName() + " | " + latestReceipt.getFormattedTimestamp());
        receiptValue.getStyleClass().add("detail-value");
        receiptValue.setWrapText(true);

        Label nextStep = createMutedCopy(awaitingReview
                ? "Choose a replacement file only if the landlord needs a clearer receipt while the proof is still under review."
                : "Choose a receipt if needed, confirm the amount and billing month, add the reference number, then submit one complete proof.");
        nextStep.getStyleClass().add("tenant-payment-helper");

        Path[] selectedReceipt = new Path[1];

        Button chooseReceiptButton = createSecondaryButton(
                latestReceipt == null ? "Choose receipt photo" : awaitingReview ? "Replace receipt photo" : "Change receipt photo");
        chooseReceiptButton.setMaxWidth(Double.MAX_VALUE);
        // choose receipt button logic for tenant
        chooseReceiptButton.setOnAction(event -> {
            Path chosenReceipt = chooseReceiptImage();
            if (chosenReceipt == null) {
                return;
            }

            selectedReceipt[0] = chosenReceipt;
            receiptValue.setText(chosenReceipt.getFileName().toString() + " | Ready to upload");
            nextStep.setText(awaitingReview
                    ? "Press Update receipt to store this replacement file while the current proof stays under review."
                    : "Press " + submitType.getText() + " to upload this file and send the payment proof together.");
            feedback.setVisible(false);
            feedback.setManaged(false);
        });

        VBox receiptBlock = new VBox(4);
        Label receiptLabel = new Label("CURRENT RECEIPT");
        receiptLabel.getStyleClass().add("field-label");
        receiptBlock.getChildren().addAll(receiptLabel, receiptValue, new Separator());

        VBox content = createDialogShell(
                "Tenant payment flow",
                awaitingReview ? "Replace the receipt file for the current review" : "Keep receipt, reference, and submission in one flow",
                awaitingReview
                        ? "Your proof is already with the landlord. Use this only to replace the receipt image without creating a second submission."
                        : "This flow keeps the billing month, amount, receipt image, and reference number together before the proof goes to landlord review.",
                new VBox(12,
                        feedback,
                        createDetailRow("BILLING MONTH", tenant.getCurrentBillingMonth()),
                        createDetailRow("AMOUNT DUE", getRentDisplay(tenant)),
                        receiptBlock,
                        chooseReceiptButton,
                        awaitingReview
                                ? createDetailRow("CURRENT REFERENCE", latestReferenceRecord == null || !latestReferenceRecord.hasReferenceNumber()
                                        ? "Not submitted yet"
                                        : latestReferenceRecord.getReferenceNumber())
                                : createFieldGroup("REFERENCE NUMBER", referenceField),
                        nextStep));
        dialog.getDialogPane().setContent(content);

        Node submitButton = dialog.getDialogPane().lookupButton(submitType);
        submitButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (awaitingReview) {
                if (selectedReceipt[0] == null) {
                    feedback.getStyleClass().setAll("feedback-box", "error-box");
                    feedback.setText("Choose a new receipt image first.");
                    feedback.setVisible(true);
                    feedback.setManaged(true);
                    event.consume();
                    return;
                }

                String updateResult = staySyncService.submitTenantPaymentReceipt(tenant, selectedReceipt[0]);
                if (updateResult != null) {
                    feedback.getStyleClass().setAll("feedback-box", "error-box");
                    feedback.setText(updateResult);
                    feedback.setVisible(true);
                    feedback.setManaged(true);
                    event.consume();
                    return;
                }

                tenantMessage = "Receipt photo updated while your payment proof stays under landlord review.";
                tenantMessageSuccess = true;
                renderCurrentView();
                return;
            }

            if (selectedReceipt[0] != null) {
                String receiptResult = staySyncService.submitTenantPaymentReceipt(tenant, selectedReceipt[0]);
                if (receiptResult != null) {
                    feedback.getStyleClass().setAll("feedback-box", "error-box");
                    feedback.setText(receiptResult);
                    feedback.setVisible(true);
                    feedback.setManaged(true);
                    event.consume();
                    return;
                }
            } else if (latestReceipt == null) {
                feedback.getStyleClass().setAll("feedback-box", "error-box");
                feedback.setText("Choose a receipt image first.");
                feedback.setVisible(true);
                feedback.setManaged(true);
                event.consume();
                return;
            }

            String result = staySyncService.submitTenantPaymentForVerification(tenant, referenceField.getText());
            if (result != null) {
                feedback.getStyleClass().setAll("feedback-box", "error-box");
                feedback.setText(result);
                feedback.setVisible(true);
                feedback.setManaged(true);
                event.consume();
                return;
            }

            tenantMessage = rejected
                    ? "Updated payment proof submitted for another landlord review."
                    : "Payment proof submitted for landlord review.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });

        dialog.showAndWait();
    }

    private void showVerifyPaymentDialog(TenantAccount tenant, String defaultNote) {
        if (tenant == null) {
            return;
        }

        Dialog<ButtonType> dialog = createDialog("Verify Payment");
        ButtonType verifyType = new ButtonType("Verify payment", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(verifyType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);
        styleDialogButtons(dialog, verifyType);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        TextArea noteArea = createTextArea("Add an approval note");
        noteArea.setText(defaultNote == null ? "" : defaultNote);
        PaymentRecord latestReferenceRecord = findLatestReferenceRecord(tenant);

        VBox content = createDialogShell(
                "Landlord review",
                "Verify this payment proof",
                "Confirm the payment only after the receipt image, reference number, and amount match your records.",
                new VBox(12,
                        feedback,
                        createDetailRow("TENANT", tenant.getFullName()),
                        createDetailRow("BILLING MONTH", tenant.getCurrentBillingMonth()),
                        createDetailRow("AMOUNT", getRentDisplay(tenant)),
                        createDetailRow("REFERENCE", latestReferenceRecord == null || !latestReferenceRecord.hasReferenceNumber()
                                ? "Not submitted yet"
                                : latestReferenceRecord.getReferenceNumber()),
                        createFieldGroup("APPROVAL NOTE", noteArea)));
        dialog.getDialogPane().setContent(content);

        Node verifyButton = dialog.getDialogPane().lookupButton(verifyType);
        verifyButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String result = staySyncService.verifyTenantPaymentSubmission(tenant, noteArea.getText());
            if (result != null) {
                feedback.getStyleClass().setAll("feedback-box", "error-box");
                feedback.setText(result);
                feedback.setVisible(true);
                feedback.setManaged(true);
                event.consume();
                return;
            }

            showLandlordMessage("Payment verified for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        dialog.showAndWait();
    }

    private void showRejectPaymentDialog(TenantAccount tenant, String defaultNote) {
        if (tenant == null) {
            return;
        }

        Dialog<ButtonType> dialog = createDialog("Reject Payment Proof");
        ButtonType rejectType = new ButtonType("Reject proof", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rejectType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);
        styleDialogButtons(dialog, rejectType);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        TextArea noteArea = createTextArea("Tell the tenant what needs to be fixed");
        noteArea.setText(defaultNote == null ? "" : defaultNote);

        VBox content = createDialogShell(
                "Landlord review",
                "Reject this payment proof",
                "Use a clear note so the tenant knows whether to replace the receipt, correct the reference number, or resubmit another payment proof.",
                new VBox(12,
                        feedback,
                        createDetailRow("TENANT", tenant.getFullName()),
                        createDetailRow("BILLING MONTH", tenant.getCurrentBillingMonth()),
                        createDetailRow("CURRENT STATUS", tenant.getPaymentStatusLabel()),
                        createFieldGroup("REJECTION NOTE", noteArea)));
        dialog.getDialogPane().setContent(content);

        Node rejectButton = dialog.getDialogPane().lookupButton(rejectType);
        rejectButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String result = staySyncService.rejectTenantPaymentSubmission(tenant, noteArea.getText());
            if (result != null) {
                feedback.getStyleClass().setAll("feedback-box", "error-box");
                feedback.setText(result);
                feedback.setVisible(true);
                feedback.setManaged(true);
                event.consume();
                return;
            }

            showLandlordMessage("Payment proof rejected for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        dialog.showAndWait();
    }

    private void openReceiptImage(PaymentRecord record, boolean landlordContext) {
        if (record == null || !record.hasReceiptImage()) {
            if (landlordContext) {
                showLandlordMessage("No receipt photo is available for this record.", false);
            } else {
                tenantMessage = "No receipt photo is available for this record.";
                tenantMessageSuccess = false;
            }
            renderCurrentView();
            return;
        }

        try {
            Path receiptPath = Path.of(record.getReceiptImagePath());
            if (!Files.exists(receiptPath)) {
                throw new IllegalStateException("Missing receipt image");
            }
            getHostServices().showDocument(receiptPath.toUri().toString());
        } catch (RuntimeException exception) {
            if (landlordContext) {
                showLandlordMessage("The receipt photo could not be opened.", false);
            } else {
                tenantMessage = "The receipt photo could not be opened.";
                tenantMessageSuccess = false;
            }
            renderCurrentView();
        }
    }

    private void handleTenantDeletion(TenantAccount tenant) {
        if (tenant == null) {
            showLandlordMessage("Select a tenant first.", false);
            renderCurrentView();
            return;
        }

        if (!showDeleteTenantConfirmation(tenant)) {
            return;
        }

        String result = staySyncService.deleteTenant(tenant);
        if (result != null) {
            showLandlordMessage(result, false);
            renderCurrentView();
            return;
        }

        landlordSelectedUsername = null;
        landlordSection = LandlordSection.RESIDENTS;
        showLandlordMessage("Deleted tenant " + tenant.getFullName() + ". This action cannot be undone.", true);
        renderCurrentView();
    }

    private boolean showDeleteTenantConfirmation(TenantAccount tenant) {
        Dialog<ButtonType> dialog = createDialog("Delete Tenant");
        ButtonType deleteType = new ButtonType("Delete permanently", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        Label title = new Label("Delete " + tenant.getFullName() + "?");
        title.getStyleClass().add("card-title");

        Label warning = new Label(
                "This will permanently remove the tenant account, room assignment details, and payment history. This action is irreversible.");
        warning.setWrapText(true);
        warning.getStyleClass().add("body-copy");

        TextField confirmationField = createTextField("Type the username to confirm deletion");

        VBox details = new VBox(12,
                feedback,
                title,
                warning,
                createDetailRow("USERNAME", tenant.getUsername()),
                createDetailRow("ROOM", getRoomSummary(tenant)),
                createFieldGroup("CONFIRM USERNAME", confirmationField));
        dialog.getDialogPane().setContent(details);

        Node deleteButton = dialog.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().addAll("ui-button", "danger-button");
            applyButtonHoverAnimation((ButtonBase) deleteButton);
            deleteButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!tenant.getUsername().equalsIgnoreCase(confirmationField.getText().trim())) {
                    feedback.getStyleClass().setAll("feedback-box", "error-box");
                    feedback.setText("Type @" + tenant.getUsername() + " to confirm deletion.");
                    feedback.setVisible(true);
                    feedback.setManaged(true);
                    event.consume();
                }
            });
        }

        return dialog.showAndWait().filter(deleteType::equals).isPresent();
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

    private List<TenantAccount> getAllLandlordTenants() {
        return new ArrayList<>(staySyncService.getLandlordDashboardData("").getTenants());
    }

    private TenantAccount getSelectedTenant() {
        if (landlordSelectedUsername == null) {
            return null;
        }
        for (TenantAccount tenant : getAllLandlordTenants()) {
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
        VBox content = new VBox(12,
                feedback,
                createFieldGroup("FULL NAME", fullNameField),
                createFieldGroup("CONTACT NUMBER", contactField));
        dialog.getDialogPane().setContent(content);

        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String result = staySyncService.updateTenantProfile(
                    currentTenant,
                    fullNameField.getText(),
                    contactField.getText());
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
        dialog.getDialogPane().setPrefWidth(500);
        styleDialogButtons(dialog, saveType);

        Label feedback = createFeedbackLabel("", false);
        feedback.setVisible(false);
        feedback.setManaged(false);

        PasswordField currentField = createPasswordField("Current Password");
        PasswordField newField = createPasswordField("New Password");
        PasswordField confirmField = createPasswordField("Confirm Password");

        VBox form = new VBox(12,
                feedback,
                createFieldGroup("CURRENT PASSWORD", currentField),
                createFieldGroup("NEW PASSWORD", newField),
                createFieldGroup("CONFIRM PASSWORD", confirmField));
        VBox content = createDialogShell(
                "Tenant security",
                "Change your password",
                "Use a fresh password that is easy for you to remember and hard for others to guess.",
                form);
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

    private Dialog<ButtonType> createSystemDialog(String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.getDialogPane().setHeaderText(null);
        dialog.getDialogPane().setGraphic(null);
        return dialog;
    }

    private VBox createSystemFieldGroup(String labelText, Node control) {
        VBox box = new VBox(6);
        Label label = new Label(labelText);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        box.getChildren().addAll(label, control);
        return box;
    }

    private VBox createDialogShell(String eyebrowText, String titleText, String descriptionText, Node... content) {
        VBox shell = new VBox(18);
        shell.getStyleClass().add("dialog-content-shell");

        VBox header = new VBox(8);
        header.getStyleClass().add("dialog-copy-block");

        Label eyebrow = new Label(eyebrowText.toUpperCase(Locale.ROOT));
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label(titleText);
        title.getStyleClass().addAll("section-title", "dialog-title");
        title.setWrapText(true);
        Label description = new Label(descriptionText);
        description.getStyleClass().addAll("body-copy", "dialog-copy");
        description.setWrapText(true);

        Separator separator = new Separator();
        separator.getStyleClass().add("dialog-separator");

        header.getChildren().addAll(eyebrow, title, description);
        shell.getChildren().addAll(header, separator);
        shell.getChildren().addAll(content);
        return shell;
    }

    private Dialog<ButtonType> createDialog(String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().addAll(scene.getStylesheets());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        if (darkMode) {
            dialog.getDialogPane().getStyleClass().add("dark-dialog");
        }
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

    private void applyAutoDismissFeedback(Label label, String expectedMessage, Runnable clearAction) {
        if (label == null || !label.isVisible()) {
            return;
        }

        PauseTransition hold = new PauseTransition(Duration.seconds(2.6));
        FadeTransition fade = new FadeTransition(Duration.millis(420), label);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(hold, fade);
        sequence.setOnFinished(event -> {
            label.setManaged(false);
            label.setVisible(false);
            if (expectedMessage.equals(tenantMessage) && tenantMessageSuccess) {
                clearAction.run();
            }
        });
        sequence.play();
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

    private Label createSidebarSectionLabel(String text) {
        Label label = new Label(text.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("sidebar-section-label");
        return label;
    }

    private Button createNavButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "nav-button");
        if (active) {
            button.getStyleClass().add("active-nav");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        applyButtonHoverAnimation(button);
        // nav button logic for layout
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button createNavButton(String text, boolean active, Runnable action, String... extraStyleClasses) {
        Button button = createNavButton(text, active, action);
        button.getStyleClass().addAll(extraStyleClasses);
        return button;
    }

    private ToggleButton createTabButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.getStyleClass().add("tab-button");
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        applyButtonHoverAnimation(button);
        return button;
    }

    private Button createFilterButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "chip-button");
        if (active) {
            button.getStyleClass().add("active-chip");
        }
        applyButtonHoverAnimation(button);
        // filter button logic for layout
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "primary-button");
        applyButtonHoverAnimation(button);
        return button;
    }

    private Button createSecondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "secondary-button");
        applyButtonHoverAnimation(button);
        return button;
    }

    private Button createDangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "danger-button");
        applyButtonHoverAnimation(button);
        return button;
    }

    private void applyButtonHoverAnimation(ButtonBase button) {
        Duration duration = Duration.millis(140);

        ScaleTransition scaleIn = new ScaleTransition(duration, button);
        scaleIn.setToX(1.03);
        scaleIn.setToY(1.03);
        TranslateTransition liftIn = new TranslateTransition(duration, button);
        liftIn.setToY(-2);
        ParallelTransition hoverIn = new ParallelTransition(scaleIn, liftIn);

        ScaleTransition scaleOut = new ScaleTransition(duration, button);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);
        TranslateTransition liftOut = new TranslateTransition(duration, button);
        liftOut.setToY(0);
        ParallelTransition hoverOut = new ParallelTransition(scaleOut, liftOut);

        button.hoverProperty().addListener((observable, wasHovering, isHovering) -> {
            if (button.isDisabled()) {
                return;
            }

            if (isHovering) {
                hoverOut.stop();
                hoverIn.playFromStart();
            } else {
                hoverIn.stop();
                hoverOut.playFromStart();
            }
        });

        button.disabledProperty().addListener((observable, wasDisabled, isDisabled) -> {
            if (isDisabled) {
                hoverIn.stop();
                hoverOut.stop();
                button.setScaleX(1.0);
                button.setScaleY(1.0);
                button.setTranslateY(0);
            }
        });
    }

    private void styleDialogButtons(Dialog<ButtonType> dialog, ButtonType primaryType) {
        Node primaryButtonNode = dialog.getDialogPane().lookupButton(primaryType);
        if (primaryButtonNode instanceof ButtonBase primaryButton) {
            primaryButton.getStyleClass().add("primary-button");
            applyButtonHoverAnimation(primaryButton);
        }

        Node cancelButtonNode = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButtonNode instanceof ButtonBase cancelButton) {
            cancelButton.getStyleClass().add("secondary-button");
            applyButtonHoverAnimation(cancelButton);
        }
    }

    private ScrollPane createPageScrollPane(Node content, double initialVvalue, DoubleConsumer onVvalueChanged) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        // scroll wheel logic for page
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double contentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();
            double scrollRange = contentHeight - viewportHeight;
            if (scrollRange <= 0 || event.getDeltaY() == 0) {
                return;
            }

            double speedMultiplier = 2.2;
            double delta = (-event.getDeltaY() / scrollRange) * speedMultiplier;
            double nextValue = Math.max(0, Math.min(1, scrollPane.getVvalue() + delta));
            scrollPane.setVvalue(nextValue);
            event.consume();
        });
        if (onVvalueChanged != null) {
            // scroll position save logic for page
            scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> onVvalueChanged.accept(newValue.doubleValue()));
        }
        // scroll restore logic for page
        Platform.runLater(() -> scrollPane.setVvalue(initialVvalue));
        return scrollPane;
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

    private TextArea createTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(4);
        area.getStyleClass().add("ui-text-area");
        return area;
    }

    private Button createTenantNotificationBellButton() {
        Button button = new Button();
        button.getStyleClass().addAll("ui-button", "notification-bell-button");
        button.setGraphic(createNotificationBellGraphic(currentTenant == null ? 0 : currentTenant.getUnreadNotificationCount()));
        applyButtonHoverAnimation(button);
        // notification button logic for tenant
        button.setOnAction(event -> {
            if (tenantNotificationMenu != null && tenantNotificationMenu.isShowing()) {
                tenantNotificationMenu.hide();
                return;
            }

            tenantNotificationMenu = createTenantNotificationMenu();
            tenantNotificationMenu.setOnHidden(hiddenEvent -> tenantNotificationMenu = null);
            tenantNotificationMenu.show(button, Side.BOTTOM, -376, 10);
        });
        return button;
    }

    private void handleTenantNotificationReadState(NotificationRecord notification, boolean read) {
        if (currentTenant == null || notification == null) {
            return;
        }

        String result = staySyncService.updateTenantNotificationReadState(currentTenant, notification, read);
        if (tenantNotificationMenu != null) {
            tenantNotificationMenu.hide();
            tenantNotificationMenu = null;
        }
        if (result != null) {
            tenantMessage = result;
            tenantMessageSuccess = false;
            renderCurrentView();
            return;
        }

        tenantMessage = read ? "Notification marked as read." : "Notification marked as unread.";
        tenantMessageSuccess = true;
        renderCurrentView();
    }

    private void handleMarkAllTenantNotificationsRead() {
        if (currentTenant == null) {
            return;
        }

        String result = staySyncService.markAllTenantNotificationsRead(currentTenant);
        if (tenantNotificationMenu != null) {
            tenantNotificationMenu.hide();
            tenantNotificationMenu = null;
        }
        if (result != null) {
            tenantMessage = result;
            tenantMessageSuccess = false;
            renderCurrentView();
            return;
        }

        tenantMessage = "All notifications marked as read.";
        tenantMessageSuccess = true;
        renderCurrentView();
    }

    private ContextMenu createTenantNotificationMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("notification-menu");

        Node panel = createTenantNotificationsPanel();
        if (panel instanceof Region region) {
            region.setPrefWidth(430);
            region.setMinWidth(430);
            region.setMaxWidth(430);
        }

        CustomMenuItem item = new CustomMenuItem(panel, false);
        item.getStyleClass().add("notification-menu-item");
        menu.getItems().add(item);
        return menu;
    }

    private Button createLandlordNotificationBellButton() {
        Button button = new Button();
        button.getStyleClass().addAll("ui-button", "notification-bell-button");
        button.setGraphic(createNotificationBellGraphic(staySyncService.getUnreadLandlordNotificationCount()));
        applyButtonHoverAnimation(button);
        // notification button logic for landlord
        button.setOnAction(event -> {
            if (landlordNotificationMenu != null && landlordNotificationMenu.isShowing()) {
                landlordNotificationMenu.hide();
                return;
            }

            landlordNotificationMenu = createLandlordNotificationMenu();
            landlordNotificationMenu.setOnHidden(hiddenEvent -> landlordNotificationMenu = null);
            landlordNotificationMenu.show(button, Side.BOTTOM, -376, 10);
        });
        return button;
    }

    private void handleLandlordNotificationReadState(LandlordNotification notification, boolean read) {
        if (notification == null) {
            return;
        }

        String result = staySyncService.updateLandlordNotificationReadState(notification, read);
        if (landlordNotificationMenu != null) {
            landlordNotificationMenu.hide();
            landlordNotificationMenu = null;
        }
        if (result != null) {
            landlordMessage = result;
            landlordMessageSuccess = false;
            renderCurrentView();
            return;
        }

        landlordMessage = read ? "Notification marked as read." : "Notification marked as unread.";
        landlordMessageSuccess = true;
        renderCurrentView();
    }

    private void handleMarkAllLandlordNotificationsRead() {
        String result = staySyncService.markAllLandlordNotificationsRead();
        if (landlordNotificationMenu != null) {
            landlordNotificationMenu.hide();
            landlordNotificationMenu = null;
        }
        if (result != null) {
            landlordMessage = result;
            landlordMessageSuccess = false;
            renderCurrentView();
            return;
        }

        landlordMessage = "All landlord notifications marked as read.";
        landlordMessageSuccess = true;
        renderCurrentView();
    }

    private ContextMenu createLandlordNotificationMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("notification-menu");

        Node panel = createLandlordNotificationsPanel();
        if (panel instanceof Region region) {
            region.setPrefWidth(430);
            region.setMinWidth(430);
            region.setMaxWidth(430);
        }

        CustomMenuItem item = new CustomMenuItem(panel, false);
        item.getStyleClass().add("notification-menu-item");
        menu.getItems().add(item);
        return menu;
    }

    private StackPane createNotificationBellGraphic(int notificationCount) {
        StackPane graphic = new StackPane();
        graphic.getStyleClass().add("notification-bell-graphic");

        SVGPath bell = new SVGPath();
        bell.setContent("M12 3C8.7 3 6 5.7 6 9v3.4c0 .8-.3 1.6-.8 2.2L4 16h16l-1.2-1.4c-.5-.6-.8-1.4-.8-2.2V9c0-3.3-2.7-6-6-6zm0 18c1.5 0 2.7-1 3-2H9c.3 1 1.5 2 3 2z");
        bell.getStyleClass().add("notification-bell-icon");
        graphic.getChildren().add(bell);

        if (notificationCount > 0) {
            Label badge = new Label(String.valueOf(notificationCount));
            badge.getStyleClass().add("notification-badge");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            graphic.getChildren().add(badge);
        }

        return graphic;
    }

    private Button createAuthThemeButton() {
        Button button = new Button(darkMode ? "Light mode" : "Dark mode");
        button.getStyleClass().addAll("ui-button", "theme-button", "auth-theme-button");
        applyButtonHoverAnimation(button);
        // theme button logic for auth
        button.setOnAction(event -> {
            darkMode = !darkMode;
            applyThemeMode();
            renderCurrentView();
        });
        return button;
    }

    private StackPane createBrandLogo() {
        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("logo-box");

        try {
            Image logoImage = new Image(resolveLogoAsset(), 42, 42, true, true, false);
            if (!logoImage.isError()) {
                ImageView logoView = new ImageView(logoImage);
                logoView.getStyleClass().add("logo-image");
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
                logoBox.getChildren().add(logoView);
                return logoBox;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to an inline mark if the asset is unavailable.
        }

        Label fallback = new Label("SS");
        fallback.getStyleClass().add("logo-fallback");
        logoBox.getChildren().add(fallback);
        return logoBox;
    }

    private VBox createPanelCard(String... extraClasses) {
        VBox card = new VBox(10);
        card.getStyleClass().add("surface-card");
        for (String extraClass : extraClasses) {
            card.getStyleClass().add(extraClass);
        }
        return card;
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
        Label label = new Label(text);
        label.getStyleClass().add("status-label");
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.CLIP);
        pill.getChildren().add(label);
        return pill;
    }

    private Label createOverviewBadge(String text, String toneClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("overview-badge", toneClass);
        badge.setWrapText(true);
        badge.setTextOverrun(OverrunStyle.CLIP);
        return badge;
    }

    private HBox createPortfolioStatusChip(String labelText, int count, PaymentStatus status) {
        HBox chip = createStatusPill(labelText + " " + String.format("%02d", count), status);
        chip.getStyleClass().add("portfolio-status-chip");
        return chip;
    }

    private boolean canTenantSubmitPayment(TenantAccount tenant) {
        return tenant != null
                && hasBillingAssignment(tenant)
                && tenant.getPaymentStatus() != PaymentStatus.PAID;
    }

    private String getTenantPaymentActionLabel(TenantAccount tenant) {
        if (tenant == null) {
            return "OPEN PAYMENT FLOW";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "PAYMENT VERIFIED";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "UPDATE RECEIPT PHOTO";
        }
        if (tenant.hasRejectedPaymentSubmission()) {
            return "REPLACE RECEIPT AND RESUBMIT";
        }
        if (findLatestReceiptRecord(tenant) == null) {
            return "UPLOAD RECEIPT AND SUBMIT";
        }
        return "REVIEW AND SUBMIT";
    }

    private String getTenantPaymentActionHelper(TenantAccount tenant) {
        if (tenant == null || !hasBillingAssignment(tenant)) {
            return "The payment flow unlocks after the landlord assigns your room and monthly rent.";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "The landlord already verified this billing cycle.";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "Your proof is already under review. Use the same flow only if you need to replace the receipt image.";
        }
        if (findLatestReceiptRecord(tenant) == null) {
            return "Open the payment flow to choose a receipt, add the reference number, and submit the proof together.";
        }
        if (tenant.hasRejectedPaymentSubmission()) {
            return "The last proof was rejected. Replace the receipt if needed, review the landlord note, and resubmit the full proof.";
        }
        return "Open the payment flow to confirm the current receipt and reference number before sending the proof to the landlord.";
    }

    private String getTenantNextStepText(TenantAccount tenant) {
        if (tenant == null || !hasBillingAssignment(tenant)) {
            return "Wait for billing setup";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "No action needed";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "Wait for landlord review";
        }
        if (tenant.hasRejectedPaymentSubmission()) {
            return "Fix and resubmit proof";
        }
        if (findLatestReceiptRecord(tenant) == null) {
            return "Choose receipt and reference";
        }
        return "Submit current proof";
    }

    private String getTenantPaymentCycleCallout(PaymentRecord latestReceipt) {
        if (!hasBillingAssignment(currentTenant)) {
            return "Room assignment or monthly rent is still incomplete. Wait for the landlord to finish the billing setup before uploading proof.";
        }
        if (currentTenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "This billing cycle is already verified. Keep the receipt on hand in case you need to review the transaction later.";
        }
        if (currentTenant.isPaymentAwaitingVerification()) {
            return "Your payment proof is already with the landlord. You can still replace the receipt photo while the review is in progress.";
        }
        if (currentTenant.hasRejectedPaymentSubmission()) {
            return "The last proof was rejected. Update the receipt image or reference number, then resubmit the payment proof.";
        }
        if (latestReceipt == null) {
            return "Start by uploading a clear receipt photo. After that, submit the payment proof together with your reference number.";
        }
        return "Your receipt image is already uploaded. Submit the payment proof when your transfer reference is ready.";
    }

    private String getTenantReceiptWorkflowIntro(PaymentRecord latestReceipt) {
        if (!hasBillingAssignment(currentTenant)) {
            return "The guided payment flow unlocks after the landlord assigns your room and monthly rent.";
        }
        if (currentTenant.isPaymentAwaitingVerification()) {
            return "Payment proof was already submitted. Keep the current reference number and replace the receipt only if the landlord needs a clearer file.";
        }
        if (latestReceipt == null) {
            return "Start one guided payment flow: choose a clear receipt, add the reference number, and submit the proof together.";
        }
        return "Your latest receipt is already on file. Review it below, keep the amount and billing month aligned, then submit the proof.";
    }

    private String getTenantReceiptStateLabel(PaymentRecord latestReceipt) {
        if (!hasBillingAssignment(currentTenant)) {
            return "Billing setup pending";
        }
        if (currentTenant.hasRejectedPaymentSubmission()) {
            return "Proof needs update";
        }
        if (currentTenant.isPaymentAwaitingVerification()) {
            return "Proof under review";
        }
        if (currentTenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "Cycle already verified";
        }
        if (latestReceipt == null) {
            return "Receipt not chosen";
        }
        return "Ready to submit";
    }

    private String getTenantReceiptStateTone(PaymentRecord latestReceipt) {
        if (!hasBillingAssignment(currentTenant)) {
            return "warning";
        }
        if (currentTenant.hasRejectedPaymentSubmission()) {
            return "danger";
        }
        if (currentTenant.isPaymentAwaitingVerification()) {
            return "warning";
        }
        if (currentTenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "calm";
        }
        if (latestReceipt == null) {
            return "warning";
        }
        return "live";
    }

    private VBox createOverviewMiniCard(String title, String value, String helperText, String... extraClasses) {
        VBox card = new VBox(4);
        card.getStyleClass().add("overview-mini-card");
        Collections.addAll(card.getStyleClass(), extraClasses);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("metric-value", "overview-mini-value");
        Label helper = createMutedCopy(helperText);
        helper.getStyleClass().add("overview-mini-helper");

        card.getChildren().addAll(titleLabel, valueLabel, helper);
        return card;
    }

    private VBox createResidentsInsightStat(String title, int count, String helperText, String toneClass) {
        VBox card = new VBox(5);
        card.getStyleClass().addAll("residents-insight-stat", toneClass);
        card.setPrefWidth(148);
        card.setMinWidth(132);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label(String.format("%02d", count));
        valueLabel.getStyleClass().add("residents-insight-stat-value");
        Label helperLabel = createMutedCopy(helperText);
        helperLabel.getStyleClass().add("residents-insight-stat-meta");

        card.getChildren().addAll(titleLabel, valueLabel, helperLabel);
        return card;
    }

    private PaymentRecord findLatestReceiptRecord(TenantAccount tenant) {
        if (tenant == null) {
            return null;
        }
        for (PaymentRecord record : tenant.getPaymentHistory()) {
            if (record.hasReceiptImage()) {
                return record;
            }
        }
        return null;
    }

    private PaymentRecord findLatestReferenceRecord(TenantAccount tenant) {
        if (tenant == null) {
            return null;
        }
        for (PaymentRecord record : tenant.getPaymentHistory()) {
            if (record.hasReferenceNumber()) {
                return record;
            }
        }
        return null;
    }

    private String buildPaymentRecordSummary(PaymentRecord record) {
        if (record == null) {
            return "No recent payment updates yet.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(record.getFormattedTimestamp())
                .append("\n")
                .append(record.getStatus().getLabel())
                .append(" by ")
                .append(record.getUpdatedBy());
        if (!record.getBillingMonth().isBlank() || record.getAmount() > 0) {
            summary.append("\n")
                    .append(record.getBillingMonth().isBlank() ? currentTenant.getCurrentBillingMonth() : record.getBillingMonth())
                    .append(" | ")
                    .append(record.getFormattedAmount());
        }
        if (record.hasReferenceNumber()) {
            summary.append("\nReference: ").append(record.getReferenceNumber());
        }
        if (record.hasReceiptImage()) {
            summary.append("\nReceipt photo attached: ").append(record.getReceiptFileName());
        }
        summary.append("\n").append(record.getNote());
        return summary.toString();
    }

    private String getVerificationTone(VerificationStatus status) {
        if (status == null) {
            return "calm";
        }
        return switch (status) {
            case FOR_REVIEW -> "warning";
            case REJECTED -> "danger";
            default -> "calm";
        };
    }

    private Node createReceiptPreview(PaymentRecord record, String emptyMessage, double fitWidth, double fitHeight) {
        StackPane preview = new StackPane();
        preview.getStyleClass().add("receipt-preview");
        preview.setMinHeight(fitHeight);
        preview.setPrefHeight(fitHeight);
        preview.setMaxWidth(Double.MAX_VALUE);

        if (record == null || !record.hasReceiptImage()) {
            Label placeholder = createMutedCopy(emptyMessage);
            placeholder.getStyleClass().add("receipt-placeholder");
            preview.getChildren().add(placeholder);
            return preview;
        }

        try {
            Path receiptPath = Path.of(record.getReceiptImagePath());
            if (Files.exists(receiptPath)) {
                Image image = new Image(receiptPath.toUri().toString(), fitWidth, fitHeight, true, true, true);
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.getStyleClass().add("receipt-preview-image");
                    imageView.setFitWidth(fitWidth);
                    imageView.setFitHeight(fitHeight);
                    imageView.setPreserveRatio(true);
                    preview.getChildren().add(imageView);
                    return preview;
                }
            }
        } catch (RuntimeException ignored) {
            // Fall back to an unavailable-state placeholder below.
        }

        Label unavailable = createMutedCopy("Receipt image is unavailable.");
        unavailable.getStyleClass().add("receipt-placeholder");
        preview.getChildren().add(unavailable);
        return preview;
    }

    private VBox createResidentPickerCard(TenantAccount tenant, boolean selected) {
        VBox card = createPanelCard("mini-panel", "resident-picker-card");
        if (selected) {
            card.getStyleClass().add("selected");
        }
        card.setPrefWidth(304);
        card.setMinWidth(280);

        HBox header = new HBox(12);
        header.getStyleClass().add("resident-picker-header");
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = createResidentAvatar(getTenantInitials(tenant), "resident-avatar");
        VBox identity = new VBox(3);
        identity.getStyleClass().add("resident-picker-identity");
        identity.setMaxWidth(Double.MAX_VALUE);
        Label name = new Label(getOccupantDisplayName(tenant));
        name.getStyleClass().addAll("sidebar-name", "resident-picker-name");
        name.setWrapText(true);
        Label room = new Label(getResidentPickerRoomLabel(tenant));
        room.getStyleClass().addAll("field-label", "resident-picker-room");
        room.setWrapText(true);
        Label username = new Label("@" + tenant.getUsername());
        username.getStyleClass().addAll("meta-copy", "resident-picker-handle");
        username.setWrapText(true);
        identity.getChildren().addAll(name, room, username);

        FlowPane statusBand = new FlowPane();
        statusBand.getStyleClass().addAll("resident-picker-status-column", "resident-picker-status-band");
        statusBand.setHgap(8);
        statusBand.setVgap(8);
        statusBand.setMaxWidth(Double.MAX_VALUE);
        statusBand.getChildren().addAll(
                createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()),
                createOverviewBadge(tenant.getVerificationStatusLabel(), getVerificationTone(tenant.getVerificationStatus())));
        if (selected) {
            statusBand.getChildren().add(createOverviewBadge("Selected", "live"));
        }

        VBox headerContent = new VBox(10);
        headerContent.getStyleClass().add("resident-picker-header-content");
        headerContent.setMaxWidth(Double.MAX_VALUE);
        headerContent.getChildren().addAll(identity, statusBand);

        header.getChildren().addAll(avatar, headerContent);
        HBox.setHgrow(headerContent, Priority.ALWAYS);

        GridPane details = new GridPane();
        details.getStyleClass().add("resident-picker-details");
        details.setHgap(12);
        details.setVgap(10);
        ColumnConstraints detailColumn = new ColumnConstraints();
        detailColumn.setPercentWidth(50);
        detailColumn.setHgrow(Priority.ALWAYS);
        details.getColumnConstraints().addAll(detailColumn, detailColumn);
        VBox contactDetail = createResidentPickerDetail("Contact", tenant.getContactNumber());
        VBox rentDetail = createResidentPickerDetail("Monthly rent", getRentDisplay(tenant));
        GridPane.setHgrow(contactDetail, Priority.ALWAYS);
        GridPane.setHgrow(rentDetail, Priority.ALWAYS);
        details.add(contactDetail, 0, 0);
        details.add(rentDetail, 1, 0);

        FlowPane summaryBand = new FlowPane();
        summaryBand.getStyleClass().add("resident-picker-summary-band");
        summaryBand.setHgap(8);
        summaryBand.setVgap(8);
        summaryBand.setMaxWidth(Double.MAX_VALUE);
        summaryBand.getChildren().addAll(
                createOverviewBadge(tenant.getCurrentBillingMonth(), "filtered"),
                createOverviewBadge(getRoomTypeDisplay(tenant), "calm"),
                createOverviewBadge(
                        hasBillingAssignment(tenant) ? "Rent assigned" : "Rent pending",
                        hasBillingAssignment(tenant) ? "live" : "warning"));

        Label helper = createMutedCopy(getResidentPickerHelperText(tenant));
        helper.getStyleClass().add("resident-picker-helper");

        Button selectButton = createSecondaryButton(selected ? "Selected" : "Select");
        selectButton.setMaxWidth(Double.MAX_VALUE);
        selectButton.setWrapText(true);
        if (selected) {
            selectButton.getStyleClass().add("resident-picker-action-active");
        }
        // select button logic for landlord
        selectButton.setOnAction(event -> {
            landlordSelectedUsername = tenant.getUsername();
            renderCurrentView();
        });

        Button controlsButton = createPrimaryButton("Open controls");
        controlsButton.setMaxWidth(Double.MAX_VALUE);
        controlsButton.setWrapText(true);
        // open controls button logic for landlord
        controlsButton.setOnAction(event -> openLandlordControlsFor(tenant));

        HBox actions = new HBox(10, selectButton, controlsButton);
        actions.getStyleClass().add("resident-picker-actions");
        HBox.setHgrow(selectButton, Priority.ALWAYS);
        HBox.setHgrow(controlsButton, Priority.ALWAYS);

        card.getChildren().addAll(
                header,
                statusBand,
                summaryBand,
                details,
                helper,
                actions);
        return card;
    }

    private VBox createResidentPickerDetail(String labelText, String valueText) {
        VBox box = new VBox(2);
        box.getStyleClass().add("resident-picker-detail");
        box.setMaxWidth(Double.MAX_VALUE);

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("detail-value");
        value.setWrapText(true);

        box.getChildren().addAll(label, value);
        return box;
    }

    private StackPane createResidentAvatar(String text, String... extraClasses) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("resident-avatar");
        Collections.addAll(avatar.getStyleClass(), extraClasses);

        Label label = new Label(text);
        label.getStyleClass().add("resident-avatar-label");
        avatar.getChildren().add(label);
        return avatar;
    }

    private String getTenantInitials(TenantAccount tenant) {
        if (tenant == null) {
            return "?";
        }

        String[] parts = getOccupantDisplayName(tenant).trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.length() == 0 ? "?" : initials.toString();
    }

    private String getResidentPickerHelperText(TenantAccount tenant) {
        if (tenant == null) {
            return "Select a resident to review billing status, room assignment, and payment proof controls.";
        }
        if (!hasBillingAssignment(tenant)) {
            return "Room details or rent are still incomplete. Open controls to finish the billing setup.";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "Payment proof is waiting for review. Open controls to verify the receipt or reject it with notes.";
        }
        if (tenant.hasRejectedPaymentSubmission()) {
            return "The latest proof was rejected. Wait for the tenant to resubmit before marking the cycle paid.";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.LATE) {
            return "This account is marked late and should be followed up first.";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "This resident is settled for the current billing cycle.";
        }
        return "This billing cycle is still not paid yet. Open controls to review status or send a reminder.";
    }

    private String getLandlordNextStepText(TenantAccount tenant) {
        if (tenant == null) {
            return "Select a resident";
        }
        if (!hasBillingAssignment(tenant)) {
            return "Assign room and rent";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "Review receipt and verify or reject";
        }
        if (tenant.hasRejectedPaymentSubmission()) {
            return "Wait for tenant resubmission";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.LATE) {
            return "Follow up on late payment";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "No action needed";
        }
        return "Send reminder or update status";
    }

    private String buildResidentsInsightSummary(int totalVisible, int paid, int forReview, int late) {
        if (totalVisible == 0) {
            return "No residents match the current search or filter, so there is nothing in the payment queue right now.";
        }
        if (forReview > 0) {
            return forReview + " resident proof submission(s) are queued for review across " + totalVisible + " visible record(s).";
        }
        if (late > 0) {
            return late + " late account(s) still need follow-up in the current result set.";
        }
        if (paid == totalVisible) {
            return "All visible residents are already marked paid for the current billing cycle.";
        }
        return paid + " of " + totalVisible + " visible resident(s) are already marked paid this cycle.";
    }

    private GridPane createLandlordFactGrid(VBox... cells) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("controls-detail-grid");
        grid.setHgap(10);
        grid.setVgap(10);

        int columnCount = cells.length >= 6 ? 3 : 2;
        for (int index = 0; index < columnCount; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / columnCount);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }

        for (int index = 0; index < cells.length; index++) {
            VBox cell = cells[index];
            GridPane.setHgrow(cell, Priority.ALWAYS);
            grid.add(cell, index % columnCount, index / columnCount);
        }
        return grid;
    }

    private VBox createLandlordFactCell(String labelText, String valueText) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("controls-detail-cell");

        Label label = new Label(labelText.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().addAll("detail-value", "controls-detail-value");
        value.setWrapText(true);

        cell.getChildren().addAll(label, value);
        return cell;
    }

    private void expandCard(Node node) {
        if (node instanceof Region region) {
            region.setMinWidth(0);
            region.setMaxWidth(Double.MAX_VALUE);
        }
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

    private VBox createLandlordStatCard(String eyebrowText, String value, String title, String helperText, String... extraClasses) {
        VBox card = createPanelCard("landlord-stat-card");
        Collections.addAll(card.getStyleClass(), extraClasses);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label eyebrow = new Label(eyebrowText.toUpperCase(Locale.ROOT));
        eyebrow.getStyleClass().addAll("eyebrow-copy", "landlord-stat-eyebrow");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("metric-value", "landlord-stat-value");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("card-title", "compact-card-title", "landlord-stat-title");
        Label helper = createMutedCopy(helperText);
        helper.getStyleClass().add("landlord-stat-helper");

        card.getChildren().addAll(eyebrow, valueLabel, titleLabel, helper);
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

    private boolean hasBillingAssignment(TenantAccount tenant) {
        return tenant != null && tenant.getRoomInfo().isAssignmentComplete();
    }

    private String getOccupantDisplayName(TenantAccount tenant) {
        if (tenant == null) {
            return "No occupants assigned";
        }
        return tenant.getOccupantDisplayName();
    }

    private String getLandlordNotificationRecipientLabel(TenantAccount tenant) {
        if (tenant == null) {
            return "";
        }
        return getOccupantDisplayName(tenant)
                + " | "
                + getResidentPickerRoomLabel(tenant)
                + " | @"
                + tenant.getUsername();
    }

    private String getApprovedCoOccupantLabel(TenantAccount tenant) {
        if (tenant == null || !tenant.hasApprovedCoOccupant()) {
            return "None approved yet";
        }
        return tenant.getApprovedCoOccupantName();
    }

    private String getCoOccupantRequestTone(CoOccupantRequest request) {
        if (request == null) {
            return "calm";
        }
        return switch (request.getStatus()) {
            case APPROVED -> "live";
            case REJECTED -> "danger";
            default -> "warning";
        };
    }

    private String resolveTenantCoOccupantFieldValue() {
        if (tenantCoOccupantRequestName != null && !tenantCoOccupantRequestName.isBlank()) {
            return tenantCoOccupantRequestName;
        }
        if (currentTenant == null) {
            return "";
        }

        CoOccupantRequest request = currentTenant.getCoOccupantRequest();
        return request != null && request.isPending() ? request.getRequestedName() : "";
    }

    private String getAssignedRoomLabel(TenantAccount tenant) {
        if (tenant == null || !tenant.getRoomInfo().hasAssignedRoom()) {
            return "Not assigned yet";
        }
        return tenant.getRoomInfo().getRoomNumber();
    }

    private String getRoomTypeDisplay(TenantAccount tenant) {
        if (tenant == null || !tenant.getRoomInfo().hasAssignedRoomType()) {
            return "Not assigned yet";
        }
        return tenant.getRoomInfo().getRoomType();
    }

    private String getRoomSummary(TenantAccount tenant) {
        if (tenant == null) {
            return "Room assignment pending";
        }
        if (!tenant.getRoomInfo().hasAssignedRoom() && !tenant.getRoomInfo().hasAssignedRoomType()) {
            return "Room assignment pending";
        }
        if (!tenant.getRoomInfo().hasAssignedRoom()) {
            return getRoomTypeDisplay(tenant);
        }
        if (!tenant.getRoomInfo().hasAssignedRoomType()) {
            return getAssignedRoomLabel(tenant);
        }
        return getAssignedRoomLabel(tenant) + " / " + getRoomTypeDisplay(tenant);
    }

    private String getResidentPickerRoomLabel(TenantAccount tenant) {
        if (tenant == null) {
            return "Room assignment pending";
        }
        if (!tenant.getRoomInfo().hasAssignedRoom() && !tenant.getRoomInfo().hasAssignedRoomType()) {
            return "Room assignment pending";
        }
        return "Room " + getRoomSummary(tenant).replace(" / ", " | ");
    }

    private String getRentDisplay(TenantAccount tenant) {
        if (tenant == null || !tenant.getRoomInfo().hasAssignedRent()) {
            return "To be assigned";
        }
        return formatCurrency(tenant.getRoomInfo().getMonthlyRent());
    }

    private String getRentHelperText(TenantAccount tenant) {
        return hasBillingAssignment(tenant) ? "Current room price" : "Landlord will assign the price";
    }

    private String getRoomHelperText(TenantAccount tenant) {
        return tenant != null && tenant.getRoomInfo().hasAssignedRoom()
                ? "Room reference"
                : "Landlord will assign the room";
    }

    private VBox createNotificationItem(NotificationRecord notification) {
        VBox item = createPanelCard("mini-panel");
        item.getStyleClass().add("notification-item");
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label type = createOverviewBadge(notification.getType().getLabel(), "filtered");
        Label state = createOverviewBadge(notification.isUnread() ? "Unread" : "Read", notification.isUnread() ? "warning" : "calm");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button toggleReadButton = createSecondaryButton(notification.isUnread() ? "Mark read" : "Mark unread");
        toggleReadButton.getStyleClass().add("notification-toggle-button");
        // notification toggle button logic for tenant
        toggleReadButton.setOnAction(event -> handleTenantNotificationReadState(notification, notification.isUnread()));
        header.getChildren().addAll(type, state, headerSpacer, toggleReadButton);

        Label title = new Label(notification.getTitle());
        title.getStyleClass().addAll("card-title", "compact-card-title");
        title.setWrapText(true);

        Label message = new Label(notification.getMessage());
        message.getStyleClass().add("body-copy");
        message.setWrapText(true);

        Label meta = new Label(notification.getFormattedTimestamp() + " • " + notification.getSentBy());
        meta.getStyleClass().add("meta-copy");
        meta.setWrapText(true);
        meta.setText(notification.getFormattedTimestamp() + " | " + notification.getSentBy());

        item.getChildren().addAll(header, title, message, meta);
        return item;
    }

    private Node createTenantNotificationsPanel() {
        VBox panel = createPanelCard("notification-panel");
        Label eyebrow = new Label("NOTIFICATIONS");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Latest notices");
        title.getStyleClass().add("card-title");

        List<NotificationRecord> notifications = currentTenant.getNotifications();
        int unreadCount = currentTenant.getUnreadNotificationCount();
        if (notifications.isEmpty()) {
            panel.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("No notifications yet. Payment reminders and maintenance updates will appear here."));
            return panel;
        }

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label unreadSummary = createMutedCopy(unreadCount == 0
                ? "All caught up. Older notices stay here for reference."
                : unreadCount + " unread notification" + (unreadCount == 1 ? "" : "s") + " still need attention.");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAllReadButton = createSecondaryButton("Mark all as read");
        markAllReadButton.setDisable(unreadCount == 0);
        // mark all read button logic for tenant
        markAllReadButton.setOnAction(event -> handleMarkAllTenantNotificationsRead());
        toolbar.getChildren().addAll(unreadSummary, spacer, markAllReadButton);

        VBox list = new VBox(10);
        list.getStyleClass().add("notification-list");
        int visibleCount = Math.min(4, notifications.size());
        for (int i = 0; i < visibleCount; i++) {
            list.getChildren().add(createNotificationItem(notifications.get(i)));
        }

        panel.getChildren().addAll(eyebrow, title, toolbar, list);
        return panel;
    }

    private VBox createLandlordNotificationItem(LandlordNotification notification) {
        VBox item = createPanelCard("mini-panel");
        item.getStyleClass().add("notification-item");
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label type = createOverviewBadge(notification.getType().getLabel(), "filtered");
        Label state = createOverviewBadge(notification.isUnread() ? "Unread" : "Read", notification.isUnread() ? "warning" : "calm");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button toggleReadButton = createSecondaryButton(notification.isUnread() ? "Mark read" : "Mark unread");
        toggleReadButton.getStyleClass().add("notification-toggle-button");
        // notification toggle button logic for landlord
        toggleReadButton.setOnAction(event -> handleLandlordNotificationReadState(notification, notification.isUnread()));
        header.getChildren().addAll(type, state, headerSpacer, toggleReadButton);

        Label tenantName = new Label(notification.getTenantName().isBlank() ? "Resident" : notification.getTenantName());
        tenantName.getStyleClass().addAll("card-title", "compact-card-title");
        tenantName.setWrapText(true);

        Label title = new Label(notification.getTitle());
        title.getStyleClass().addAll("field-label", "notification-title");
        title.setWrapText(true);

        Label message = new Label(notification.getMessage());
        message.getStyleClass().add("body-copy");
        message.setWrapText(true);

        String tenantMeta = notification.getTenantUsername().isBlank() ? "" : "@" + notification.getTenantUsername();
        Label meta = new Label(notification.getFormattedTimestamp() + (tenantMeta.isBlank() ? "" : " | " + tenantMeta));
        meta.getStyleClass().add("meta-copy");
        meta.setWrapText(true);

        item.getChildren().addAll(header, tenantName, title, message, meta);
        return item;
    }

    private Node createLandlordNotificationsPanel() {
        VBox panel = createPanelCard("notification-panel");
        Label eyebrow = new Label("LANDLORD ALERTS");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Incoming tenant activity");
        title.getStyleClass().add("card-title");

        List<LandlordNotification> notifications = staySyncService.getLandlordNotifications();
        int unreadCount = staySyncService.getUnreadLandlordNotificationCount();
        if (notifications.isEmpty()) {
            panel.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("No tenant activity yet. Payment submissions and tenant notices or concerns will appear here."));
            return panel;
        }

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        Label unreadSummary = createMutedCopy(unreadCount == 0
                ? "All caught up. Older tenant activity stays here for reference."
                : unreadCount + " unread landlord notification" + (unreadCount == 1 ? "" : "s") + " still need attention.");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button markAllReadButton = createSecondaryButton("Mark all as read");
        markAllReadButton.setDisable(unreadCount == 0);
        // mark all read button logic for landlord
        markAllReadButton.setOnAction(event -> handleMarkAllLandlordNotificationsRead());
        toolbar.getChildren().addAll(unreadSummary, spacer, markAllReadButton);

        VBox list = new VBox(10);
        list.getStyleClass().add("notification-list");
        int visibleCount = Math.min(4, notifications.size());
        for (int i = 0; i < visibleCount; i++) {
            list.getChildren().add(createLandlordNotificationItem(notifications.get(i)));
        }

        panel.getChildren().addAll(eyebrow, title, toolbar, list);
        return panel;
    }

    private TableColumn<PaymentRecord, String> createHistoryTextColumn(
            String title,
            String propertyName,
            double width,
            String... extraStyleClasses) {
        TableColumn<PaymentRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellFactory(unused -> new TableCell<>() {
            {
                getStyleClass().add("payment-history-text-cell");
                getStyleClass().addAll(extraStyleClasses);
                setTextOverrun(OverrunStyle.ELLIPSIS);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }

                setText(item);
                setGraphic(null);
                setTooltip(new Tooltip(item));
            }
        });
        return column;
    }

    private TableColumn<PaymentRecord, PaymentRecord> createHistoryStatusColumn(String title, double width) {
        TableColumn<PaymentRecord, PaymentRecord> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellFactory(unused -> new TableCell<>() {
            {
                getStyleClass().add("payment-history-status-cell");
            }

            @Override
            protected void updateItem(PaymentRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox pill = createStatusPill(item.getStatus().getLabel(), item.getStatus());
                pill.getStyleClass().add("table-status-pill");
                setText(null);
                setGraphic(pill);
            }
        });
        return column;
    }

    private TableColumn<PaymentRecord, PaymentRecord> createHistoryReceiptColumn(String title, double width) {
        TableColumn<PaymentRecord, PaymentRecord> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setReorderable(false);
        column.setCellFactory(unused -> new TableCell<>() {
            {
                getStyleClass().add("payment-history-receipt-cell");
            }

            @Override
            protected void updateItem(PaymentRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }

                boolean hasReceipt = item.hasReceiptImage();
                Label badge = new Label(hasReceipt ? "Attached" : "No photo");
                badge.getStyleClass().addAll("receipt-badge", hasReceipt ? "attached" : "missing");
                setText(null);
                setGraphic(badge);
                setTooltip(hasReceipt && !item.getReceiptFileName().isBlank() ? new Tooltip(item.getReceiptFileName()) : null);
            }
        });
        return column;
    }

    private Node createPaymentHistoryPlaceholder() {
        VBox placeholder = new VBox(6);
        placeholder.getStyleClass().add("payment-history-empty-state");

        Label title = new Label("No payment updates yet");
        title.getStyleClass().add("payment-history-empty-title");

        Label copy = createMutedCopy("Receipt uploads, status changes, and landlord reviews will appear here.");
        copy.getStyleClass().add("payment-history-empty-copy");
        copy.setMaxWidth(320);

        placeholder.getChildren().addAll(title, copy);
        return placeholder;
    }

    private String buildPaymentHistorySummary(List<PaymentRecord> paymentHistory) {
        if (paymentHistory == null || paymentHistory.isEmpty()) {
            return "Every receipt upload, reference number, and landlord review will be tracked here in order.";
        }

        PaymentRecord latest = paymentHistory.get(0);
        String updateCountLabel = paymentHistory.size() == 1 ? "update" : "updates";
        return paymentHistory.size() + " " + updateCountLabel + " recorded. Latest: "
                + latest.getStatus().getLabel() + " by " + latest.getUpdatedBy()
                + (latest.hasReferenceNumber() ? " with reference " + latest.getReferenceNumber() : "")
                + (latest.hasReceiptImage() ? " and a receipt attached." : ".");
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
            case PAYMENTS -> "Upload receipts, submit payment proof, and review landlord verification updates.";
            case ACCOUNT -> "Manage identity details, room assignment, and access settings.";
            default -> "See your room, billing health, profile details, and notifications in one place.";
        };
    }

    private String getLandlordSectionTitle() {
        return switch (landlordSection) {
            case RESIDENTS -> "Residents";
            case CONTROLS -> "Controls";
            case NOTIFICATIONS -> "Notifications";
            default -> "Overview";
        };
    }

    private String getLandlordSectionSubtitle() {
        return switch (landlordSection) {
            case RESIDENTS -> "Search and review residents.";
            case CONTROLS -> "Review one resident at a time with receipt proof, billing status, room assignment, approvals, and notices kept together.";
            case NOTIFICATIONS -> "Send reminders and tenant updates without crowding the controls workspace.";
            default -> "Monitor residents, proof reviews, and payment status.";
        };
    }

    private String buildOverviewSummary(DashboardSnapshot snapshot) {
        if (snapshot.getQuery().isBlank()) {
            return snapshot.getTotalTenantCount() + " residents in view: "
                    + snapshot.getPaidCount() + " paid, " + snapshot.getPendingCount() + " not paid yet, "
                    + snapshot.getLateCount() + " late.";
        }
        return snapshot.getTenants().size() + " match" + (snapshot.getTenants().size() == 1 ? "" : "es")
                + " for \"" + snapshot.getQuery() + "\". Portfolio: "
                + snapshot.getPaidCount() + " paid, " + snapshot.getPendingCount() + " not paid yet, "
                + snapshot.getLateCount() + " late.";
    }

    private String buildAttentionSummary(DashboardSnapshot snapshot) {
        if (snapshot.getLateCount() > 0) {
            return snapshot.getLateCount() + " late account(s) need review first.";
        }
        if (snapshot.getPendingCount() > 0) {
            return snapshot.getPendingCount() + " not-yet-paid account(s) still need review.";
        }
        return "No urgent payment issues.";
    }

    private String buildWorkflowSummary(DashboardSnapshot snapshot) {
        if (!snapshot.getQuery().isBlank()) {
            return "Review matches here, then update them in Controls.";
        }
        return "Search in Residents, update in Controls.";
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

    private int countVerificationStatus(List<TenantAccount> tenants, VerificationStatus status) {
        int count = 0;
        for (TenantAccount tenant : tenants) {
            if (tenant.getVerificationStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    private String formatEditableAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private String formatPercentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0%";
        }
        int percentage = (int) Math.round((numerator * 100.0) / denominator);
        return percentage + "%";
    }

    private String resolveStylesheet() {
        URL bundledStylesheet = StaySyncApp.class.getResource("staysync.css");
        if (bundledStylesheet != null) {
            return bundledStylesheet.toExternalForm();
        }
        return Path.of("src", "main", "java", "staysync", "ui", "staysync.css").toUri().toString();
    }

    private String resolveLogoAsset() {
        URL bundledLogo = StaySyncApp.class.getResource("staysync-logo.png");
        if (bundledLogo != null) {
            return bundledLogo.toExternalForm();
        }
        return Path.of("src", "main", "java", "staysync", "ui", "staysync-logo.png").toUri().toString();
    }

    @Override
    public void stop() {
        if (stage != null) {
            stage.close();
        }
    }
}
