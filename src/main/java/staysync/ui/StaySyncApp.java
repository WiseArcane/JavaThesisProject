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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
import staysync.core.StaySyncService;
import staysync.core.StaySyncService.DashboardSnapshot;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.NotificationRecord;
import staysync.core.TenantAccount.NotificationType;
import staysync.core.TenantAccount.PaymentRecord;
import staysync.core.TenantAccount.PaymentStatus;

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
        applyButtonHoverAnimation(themeButton);
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
        loginTabButton.setOnAction(event -> switchAuthTab(AuthTab.LOGIN));
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

        CheckBox rememberMeBox = new CheckBox("Remember me");
        rememberMeBox.getStyleClass().add("remember-check");

        Label forgotPasswordLabel = new Label("Forgot password?");
        forgotPasswordLabel.getStyleClass().add("inline-link");

        Region utilitySpacer = new Region();
        HBox.setHgrow(utilitySpacer, Priority.ALWAYS);
        HBox utilityRow = new HBox(10, rememberMeBox, utilitySpacer, forgotPasswordLabel);
        utilityRow.setAlignment(Pos.CENTER_LEFT);

        Button loginButton = createPrimaryButton("Sign in");
        loginButton.setMaxWidth(Double.MAX_VALUE);
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
                utilityRow,
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

        Label note = new Label("Use Account for profile updates and password changes without leaving the workspace.");
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
                        + latestRecord.getNote()
                        + (latestRecord.hasReceiptImage() ? "\nReceipt photo attached: " + latestRecord.getReceiptFileName() : ""));
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
        Label actionEyebrow = new Label("PAYMENT RECEIPT");
        actionEyebrow.getStyleClass().add("eyebrow-copy");
        Label latest = new Label("SEND RECEIPT PHOTO");
        latest.getStyleClass().add("card-title");
        PaymentRecord latestReceipt = findLatestReceiptRecord(currentTenant);
        Label latestCopy = new Label(latestReceipt == null
                ? "Attach a clear photo of your receipt so the landlord can review your payment proof."
                : "Last receipt sent on " + latestReceipt.getFormattedTimestamp() + "\n"
                        + latestReceipt.getReceiptFileName() + "\n"
                        + latestReceipt.getNote());
        latestCopy.getStyleClass().add("body-copy");
        latestCopy.setWrapText(true);

        Node receiptPreview = createReceiptPreview(
                latestReceipt,
                "No receipt photo sent yet. Choose an image to send proof of payment.",
                340,
                190);

        Button sendReceiptButton = createPrimaryButton("SEND RECEIPT PHOTO");
        sendReceiptButton.setMaxWidth(Double.MAX_VALUE);
        sendReceiptButton.setOnAction(event -> handleTenantReceiptSubmission());

        Button openReceiptButton = createSecondaryButton("OPEN LAST RECEIPT");
        openReceiptButton.setDisable(latestReceipt == null || !latestReceipt.hasReceiptImage());
        openReceiptButton.setMaxWidth(Double.MAX_VALUE);
        openReceiptButton.setOnAction(event -> openReceiptImage(latestReceipt, false));

        Button confirmButton = createSecondaryButton("CONFIRM PAID STATUS");
        confirmButton.setDisable(currentTenant.getPaymentStatus() == PaymentStatus.PAID);
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(event -> {
            staySyncService.markTenantAsPaid(currentTenant);
            tenantMessage = "Payment status updated to Paid.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });
        actionCard.getChildren().addAll(actionEyebrow, latest, latestCopy, receiptPreview, sendReceiptButton, openReceiptButton, confirmButton);

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
        historyTable.getStyleClass().add("data-table");
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPlaceholder(new Label("No payment activity yet."));
        historyTable.getColumns().add(createRecordColumn("Updated On", "formattedTimestamp", 150));
        historyTable.getColumns().add(createRecordColumn("Status", "status", 110));
        historyTable.getColumns().add(createRecordColumn("Updated By", "updatedBy", 110));
        historyTable.getColumns().add(createRecordColumn("Receipt", "receiptStatusLabel", 120));
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
                createMutedCopy("Use this page for profile maintenance and password changes in one place."));
        return card;
    }

    private Node createLandlordShell() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("workspace-shell");
        shell.setPadding(new Insets(24));
        shell.setPrefSize(1100, 640);
        shell.setLeft(createLandlordSidebar());
        shell.setCenter(createLandlordContent());
        return shell;
    }

    private Node createLandlordSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().addAll("surface-card", "sidebar-panel");
        sidebar.setPrefWidth(272);

        Label eyebrow = new Label("STAYSYNC");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Landlord hub");
        title.getStyleClass().addAll("section-title", "sidebar-heading");
        title.setWrapText(true);
        title.setTextOverrun(OverrunStyle.CLIP);

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
        header.getStyleClass().addAll("surface-card", "content-header");
        Label title = new Label(getLandlordSectionTitle());
        title.getStyleClass().add("section-title");
        Label subtitle = new Label(getLandlordSectionSubtitle());
        subtitle.getStyleClass().add("body-copy");
        subtitle.setWrapText(true);
        header.getChildren().addAll(title, subtitle, createFeedbackLabel(landlordMessage, landlordMessageSuccess));

        ScrollPane scrollPane = createPageScrollPane(createLandlordPageBody(), getLandlordScrollVvalue(), this::setLandlordScrollVvalue);

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
                createMetricCard("RESIDENT ACCOUNTS", String.valueOf(snapshot.getTotalTenantCount()), "Registered residents"),
                createMetricCard("PAID THIS CYCLE", formatPercentage(snapshot.getPaidCount(), snapshot.getTotalTenantCount()), "Collection rate"),
                createMetricCard("NEEDS FOLLOW-UP", String.format("%02d", snapshot.getPendingCount() + snapshot.getLateCount()), "Pending and late"));
        return row;
    }

    private Node createLandlordOverviewRow() {
        DashboardSnapshot snapshot = staySyncService.getLandlordDashboardData(landlordQuery);
        Node summaryCard = createLandlordSummaryCard(snapshot);
        Node guideCard = createLandlordGuideCard(snapshot);
        HBox row = new HBox(14, summaryCard, guideCard);
        row.getStyleClass().add("landlord-overview-row");
        if (summaryCard instanceof Region summaryRegion) {
            summaryRegion.setPrefWidth(430);
            summaryRegion.setMinWidth(380);
        }
        if (guideCard instanceof Region guideRegion) {
            HBox.setHgrow(guideRegion, Priority.ALWAYS);
            guideRegion.setPrefWidth(560);
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
                createPortfolioStatusChip("Pending", snapshot.getPendingCount(), PaymentStatus.PENDING),
                createPortfolioStatusChip("Late", snapshot.getLateCount(), PaymentStatus.LATE));

        card.getChildren().addAll(header, title, metricsRow, statusRow);
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
                snapshot.getLateCount() > 0 ? "Urgent" : snapshot.getPendingCount() > 0 ? "Pending" : "Clear",
                snapshot.getLateCount() > 0 ? "danger" : snapshot.getPendingCount() > 0 ? "warning" : "calm"));

        Label title = new Label("Collection watchlist");
        title.getStyleClass().addAll("card-title", "compact-card-title");
        title.setWrapText(true);

        HBox watchStatsRow = new HBox(10,
                createOverviewMiniCard(
                        "Late accounts",
                        String.format("%02d", snapshot.getLateCount()),
                        snapshot.getLateCount() > 0 ? "Review now" : "No urgent items",
                        "watch-stat-card",
                        snapshot.getLateCount() > 0 ? "danger" : "calm"),
                createOverviewMiniCard(
                        "Pending",
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
        TenantAccount selectedTenant = getSelectedTenant();

        VBox pickerSection = new VBox(10);
        pickerSection.getStyleClass().add("resident-picker-section");
        Label pickerEyebrow = new Label("Quick select");
        pickerEyebrow.getStyleClass().add("eyebrow-copy");
        Label pickerSummary = createMutedCopy(selectedTenant == null
                ? "Pick a resident card below to load their details and controls."
                : "Selected: " + selectedTenant.getFullName() + " | Room "
                        + selectedTenant.getRoomInfo().getRoomNumber() + " | "
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

        HBox lowerRow = new HBox(14, createLandlordSelectedCard(), createLandlordResidentsInsightCard(tenants));
        HBox.setHgrow(lowerRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lowerRow.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(title, pickerSection, lowerRow);
        return card;
    }

    private Node createLandlordControlsRow() {
        VBox actionsColumn = new VBox(14, createLandlordReceiptReviewCard(), createLandlordStatusControlCard(), createLandlordNotificationCard());
        HBox row = new HBox(14, createLandlordSelectedCard(), actionsColumn);
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(actionsColumn, Priority.ALWAYS);
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

    private Node createLandlordReceiptReviewCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Receipt review");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Latest tenant receipt");
        title.getStyleClass().add("card-title");

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

        Button openButton = createSecondaryButton("Open full photo");
        openButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setOnAction(event -> openReceiptImage(latestReceipt, true));

        card.getChildren().addAll(
                eyebrow,
                title,
                submittedMeta,
                createReceiptPreview(latestReceipt, "Receipt image is unavailable.", 360, 220),
                openButton,
                createMutedCopy(latestReceipt.getNote()));
        return card;
    }

    private Node createLandlordStatusControlCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Controls");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Update status and rent");
        title.getStyleClass().add("card-title");

        TenantAccount tenant = getSelectedTenant();
        ComboBox<PaymentStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
        statusBox.getStyleClass().add("ui-combo");
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setValue(tenant == null ? PaymentStatus.PENDING : tenant.getPaymentStatus());

        TextField rentField = createTextField("Enter monthly rent");
        rentField.setText(tenant == null ? "" : formatEditableAmount(tenant.getRoomInfo().getMonthlyRent()));

        Button applyButton = createPrimaryButton("Apply status");
        applyButton.setDisable(tenant == null);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(event -> {
            if (tenant != null && statusBox.getValue() != null) {
                staySyncService.updateTenantStatusFromLandlord(tenant, statusBox.getValue());
                showLandlordMessage("Payment status updated for " + tenant.getFullName() + ".", true);
                renderCurrentView();
            }
        });

        Button saveRentButton = createSecondaryButton("Save rent");
        saveRentButton.setDisable(tenant == null);
        saveRentButton.setMaxWidth(Double.MAX_VALUE);
        saveRentButton.setOnAction(event -> {
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

            String result = staySyncService.updateTenantMonthlyRent(tenant, monthlyRent);
            if (result != null) {
                showLandlordMessage(result, false);
                renderCurrentView();
                return;
            }

            showLandlordMessage("Monthly rent updated for " + tenant.getFullName() + ".", true);
            renderCurrentView();
        });

        Button deleteButton = createDangerButton("Delete tenant");
        deleteButton.setDisable(tenant == null);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> handleTenantDeletion(tenant));

        Button residentsButton = createSecondaryButton("Back to residents");
        residentsButton.setMaxWidth(Double.MAX_VALUE);
        residentsButton.setOnAction(event -> {
            landlordSection = LandlordSection.RESIDENTS;
            renderCurrentView();
        });

        Label hint = createMutedCopy(tenant == null
                ? "Select a tenant to enable status and rent controls."
                : "Adjust payment status or update the tenant's monthly rent here.");

        Label deleteHint = createMutedCopy(tenant == null
                ? "Delete stays disabled until a tenant is selected."
                : "Deleting a tenant permanently removes the resident account and cannot be undone.");

        card.getChildren().addAll(
                eyebrow,
                title,
                createFieldGroup("NEW STATUS", statusBox),
                applyButton,
                createFieldGroup("MONTHLY RENT", rentField),
                saveRentButton,
                deleteButton,
                residentsButton,
                hint,
                deleteHint);
        return card;
    }

    private Node createLandlordNotificationCard() {
        VBox card = createPanelCard();
        Label eyebrow = new Label("Notifications");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Send tenant notice");
        title.getStyleClass().add("card-title");

        TenantAccount tenant = getSelectedTenant();

        ComboBox<NotificationType> typeBox = new ComboBox<>(FXCollections.observableArrayList(NotificationType.values()));
        typeBox.getStyleClass().add("ui-combo");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setValue(landlordNotificationType);

        TextField titleField = createTextField("Enter notice title");
        titleField.setText(landlordNotificationTitle);

        TextArea messageArea = createTextArea("Enter the message the tenant should see");
        messageArea.setText(landlordNotificationMessageBody);

        Button sendButton = createPrimaryButton("Send notification");
        sendButton.setDisable(tenant == null);
        sendButton.setMaxWidth(Double.MAX_VALUE);
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

        Label hint = createMutedCopy(tenant == null
                ? "Select a tenant from Residents to send a payment reminder, maintenance notice, or general update."
                : "Use this for payment reminders, maintenance announcements, or other important resident updates.");

        card.getChildren().addAll(
                eyebrow,
                title,
                createFieldGroup("NOTICE TYPE", typeBox),
                createFieldGroup("TITLE", titleField),
                createFieldGroup("MESSAGE", messageArea),
                sendButton,
                hint);
        return card;
    }

    private Node createLandlordResidentsInsightCard(List<TenantAccount> tenants) {
        VBox card = createPanelCard("hero-panel", "residents-insight-card");
        int totalVisible = tenants.size();
        int paid = countStatus(tenants, PaymentStatus.PAID);
        int pending = countStatus(tenants, PaymentStatus.PENDING);
        int late = countStatus(tenants, PaymentStatus.LATE);
        int followUp = pending + late;

        HBox header = new HBox(10);
        header.getStyleClass().add("overview-header-row");
        header.setAlignment(Pos.CENTER_LEFT);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label title = new Label("Payment overview");
        title.getStyleClass().add("card-title");
        header.getChildren().addAll(
                title,
                headerSpacer,
                createOverviewBadge(
                        followUp == 0 ? "Clear" : followUp + " need review",
                        late > 0 ? "danger" : pending > 0 ? "warning" : "calm"));

        HBox spotlight = new HBox(16);
        spotlight.getStyleClass().add("residents-insight-spotlight");
        spotlight.setAlignment(Pos.CENTER_LEFT);
        Label spotlightValue = new Label(String.format("%02d", followUp));
        spotlightValue.getStyleClass().add("residents-insight-spotlight-value");
        VBox spotlightCopy = new VBox(4);
        Label spotlightLabel = new Label("Needs attention");
        spotlightLabel.getStyleClass().add("sidebar-name");
        spotlightCopy.getChildren().add(spotlightLabel);
        spotlight.getChildren().addAll(spotlightValue, spotlightCopy);

        HBox statsRow = new HBox(10,
                createResidentsInsightStat("Paid", paid, "paid"),
                createResidentsInsightStat("Pending", pending, "pending"),
                createResidentsInsightStat("Late", late, "late"));
        statsRow.getStyleClass().add("residents-insight-stats");

        card.getChildren().addAll(
                header,
                spotlight,
                statsRow);
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
        tenantMessage = TENANT_LOGIN_SUCCESS_MESSAGE;
        tenantMessageSuccess = true;
        tenantNotificationMenu = null;
        authMessage = "";
        view = View.TENANT;
        renderCurrentView();
    }

    private void handleRegistration() {
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

    private double getLandlordScrollVvalue() {
        return switch (landlordSection) {
            case RESIDENTS -> landlordResidentsScrollVvalue;
            case CONTROLS -> landlordControlsScrollVvalue;
            default -> landlordOverviewScrollVvalue;
        };
    }

    private void setLandlordScrollVvalue(double value) {
        switch (landlordSection) {
            case RESIDENTS -> landlordResidentsScrollVvalue = value;
            case CONTROLS -> landlordControlsScrollVvalue = value;
            default -> landlordOverviewScrollVvalue = value;
        }
    }

    private void signOut() {
        currentTenant = null;
        view = View.AUTH;
        authTab = AuthTab.LOGIN;
        authMessage = "";
        tenantMessage = "";
        if (tenantNotificationMenu != null) {
            tenantNotificationMenu.hide();
            tenantNotificationMenu = null;
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

    private void handleTenantReceiptSubmission() {
        if (currentTenant == null) {
            tenantMessage = "Tenant account was not found.";
            tenantMessageSuccess = false;
            renderCurrentView();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose receipt photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        File initialDirectory = new File(System.getProperty("user.home", "."));
        if (initialDirectory.exists() && initialDirectory.isDirectory()) {
            chooser.setInitialDirectory(initialDirectory);
        }

        File selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        String result = staySyncService.submitTenantPaymentReceipt(currentTenant, selectedFile.toPath());
        if (result != null) {
            tenantMessage = result;
            tenantMessageSuccess = false;
            renderCurrentView();
            return;
        }

        tenantMessage = "Receipt photo sent to admin for review.";
        tenantMessageSuccess = true;
        renderCurrentView();
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

        Label title = new Label("Delete " + tenant.getFullName() + "?");
        title.getStyleClass().add("card-title");

        Label warning = new Label(
                "This will permanently remove the tenant account, room assignment details, and payment history. This action is irreversible.");
        warning.setWrapText(true);
        warning.getStyleClass().add("body-copy");

        VBox details = new VBox(12,
                title,
                warning,
                createDetailRow("USERNAME", tenant.getUsername()),
                createDetailRow("ROOM", tenant.getRoomInfo().getRoomNumber() + " / " + tenant.getRoomInfo().getRoomType()));
        dialog.getDialogPane().setContent(details);

        Node deleteButton = dialog.getDialogPane().lookupButton(deleteType);
        if (deleteButton != null) {
            deleteButton.getStyleClass().addAll("ui-button", "danger-button");
            applyButtonHoverAnimation((ButtonBase) deleteButton);
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

    private Button createNavButton(String text, boolean active, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("ui-button", "nav-button");
        if (active) {
            button.getStyleClass().add("active-nav");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        applyButtonHoverAnimation(button);
        button.setOnAction(event -> action.run());
        return button;
    }

    private ToggleButton createTabButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setSelected(selected);
        button.getStyleClass().add("tab-button");
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

    private ScrollPane createPageScrollPane(Node content, double initialVvalue, DoubleConsumer onVvalueChanged) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("page-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
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
            scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> onVvalueChanged.accept(newValue.doubleValue()));
        }
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
        button.setGraphic(createNotificationBellGraphic(currentTenant == null ? 0 : currentTenant.getNotifications().size()));
        applyButtonHoverAnimation(button);
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
        pill.getChildren().add(label);
        return pill;
    }

    private Label createOverviewBadge(String text, String toneClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("overview-badge", toneClass);
        return badge;
    }

    private HBox createPortfolioStatusChip(String labelText, int count, PaymentStatus status) {
        HBox chip = createStatusPill(labelText + " " + String.format("%02d", count), status);
        chip.getStyleClass().add("portfolio-status-chip");
        return chip;
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

    private VBox createResidentsInsightStat(String title, int count, String toneClass) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("residents-insight-stat", toneClass);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("field-label");
        Label valueLabel = new Label(String.format("%02d", count));
        valueLabel.getStyleClass().add("residents-insight-stat-value");

        card.getChildren().addAll(titleLabel, valueLabel);
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
        card.setPrefWidth(288);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(tenant.getFullName());
        name.getStyleClass().add("sidebar-name");
        name.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getChildren().add(createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()));

        VBox meta = new VBox(2);
        meta.getStyleClass().add("resident-picker-meta");
        Label room = new Label("Room " + tenant.getRoomInfo().getRoomNumber() + " | " + tenant.getRoomInfo().getRoomType());
        room.getStyleClass().add("field-label");
        Label username = new Label("@" + tenant.getUsername());
        username.getStyleClass().add("meta-copy");
        username.setWrapText(true);
        meta.getChildren().addAll(room, username);

        GridPane details = new GridPane();
        details.getStyleClass().add("resident-picker-details");
        details.setHgap(12);
        details.setVgap(10);
        details.add(createResidentPickerDetail("Contact", tenant.getContactNumber()), 0, 0);
        details.add(createResidentPickerDetail("Monthly rent", formatCurrency(tenant.getRoomInfo().getMonthlyRent())), 1, 0);
        details.add(createResidentPickerDetail("Username", tenant.getUsername()), 0, 1);
        details.add(createResidentPickerDetail("Room type", tenant.getRoomInfo().getRoomType()), 1, 1);

        Button selectButton = createSecondaryButton(selected ? "Selected" : "Select");
        selectButton.setMaxWidth(Double.MAX_VALUE);
        if (selected) {
            selectButton.getStyleClass().add("resident-picker-action-active");
        }
        selectButton.setOnAction(event -> {
            landlordSelectedUsername = tenant.getUsername();
            renderCurrentView();
        });

        Button controlsButton = createPrimaryButton("Open controls");
        controlsButton.setMaxWidth(Double.MAX_VALUE);
        controlsButton.setOnAction(event -> {
            landlordSelectedUsername = tenant.getUsername();
            landlordSection = LandlordSection.CONTROLS;
            renderCurrentView();
        });

        HBox actions = new HBox(10, selectButton, controlsButton);
        actions.getStyleClass().add("resident-picker-actions");
        HBox.setHgrow(selectButton, Priority.ALWAYS);
        HBox.setHgrow(controlsButton, Priority.ALWAYS);

        card.getChildren().addAll(
                header,
                meta,
                statusRow,
                details,
                actions);
        header.getChildren().addAll(name, spacer);
        return card;
    }

    private VBox createResidentPickerDetail(String labelText, String valueText) {
        VBox box = new VBox(2);
        box.getStyleClass().add("resident-picker-detail");

        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("detail-value");
        value.setWrapText(true);

        box.getChildren().addAll(label, value);
        return box;
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

    private VBox createNotificationItem(NotificationRecord notification) {
        VBox item = createPanelCard("mini-panel");
        item.getStyleClass().add("notification-item");
        Label type = new Label(notification.getType().getLabel());
        type.getStyleClass().add("eyebrow-copy");

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

        item.getChildren().addAll(type, title, message, meta);
        return item;
    }

    private Node createTenantNotificationsPanel() {
        VBox panel = createPanelCard("notification-panel");
        Label eyebrow = new Label("NOTIFICATIONS");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Latest notices");
        title.getStyleClass().add("card-title");

        List<NotificationRecord> notifications = currentTenant.getNotifications();
        if (notifications.isEmpty()) {
            panel.getChildren().addAll(
                    eyebrow,
                    title,
                    createMutedCopy("No notifications yet. Payment reminders and maintenance updates will appear here."));
            return panel;
        }

        VBox list = new VBox(10);
        list.getStyleClass().add("notification-list");
        int visibleCount = Math.min(4, notifications.size());
        for (int i = 0; i < visibleCount; i++) {
            list.getChildren().add(createNotificationItem(notifications.get(i)));
        }

        panel.getChildren().addAll(eyebrow, title, list);
        return panel;
    }

    private TableColumn<PaymentRecord, String> createRecordColumn(String title, String propertyName, double width) {
        TableColumn<PaymentRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
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
            default -> "See your room, billing health, profile details, and notifications in one place.";
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
            case RESIDENTS -> "Search and review residents.";
            case CONTROLS -> "Update payment status and notices.";
            default -> "Monitor residents and payment status.";
        };
    }

    private String buildOverviewSummary(DashboardSnapshot snapshot) {
        if (snapshot.getQuery().isBlank()) {
            return snapshot.getTotalTenantCount() + " residents in view: "
                    + snapshot.getPaidCount() + " paid, " + snapshot.getPendingCount() + " pending, "
                    + snapshot.getLateCount() + " late.";
        }
        return snapshot.getTenants().size() + " match" + (snapshot.getTenants().size() == 1 ? "" : "es")
                + " for \"" + snapshot.getQuery() + "\". Portfolio: "
                + snapshot.getPaidCount() + " paid, " + snapshot.getPendingCount() + " pending, "
                + snapshot.getLateCount() + " late.";
    }

    private String buildAttentionSummary(DashboardSnapshot snapshot) {
        if (snapshot.getLateCount() > 0) {
            return snapshot.getLateCount() + " late account(s) need review first.";
        }
        if (snapshot.getPendingCount() > 0) {
            return snapshot.getPendingCount() + " pending account(s) still need review.";
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
