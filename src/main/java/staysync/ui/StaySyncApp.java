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
import staysync.core.StaySyncService;
import staysync.core.StaySyncService.DashboardSnapshot;
import staysync.core.TenantAccount;
import staysync.core.TenantAccount.CoOccupantRequest;
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
        markPaidButton.setOnAction(event -> {
            staySyncService.submitTenantPaymentForVerification(currentTenant);
            tenantMessage = "Payment sent to the landlord for verification.";
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
        boolean billingAssigned = hasBillingAssignment(currentTenant);
        Label latestCopy = new Label(!billingAssigned
                ? "Receipt uploads unlock after the landlord assigns your room and monthly rent."
                : currentTenant.isPaymentAwaitingVerification()
                        ? "Payment was already sent for verification. You can still upload or replace the receipt photo while the landlord reviews it."
                : latestReceipt == null
                        ? "Attach a clear photo of your receipt, then send your payment for landlord verification."
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
        sendReceiptButton.setDisable(!billingAssigned || currentTenant.getPaymentStatus() == PaymentStatus.PAID);
        sendReceiptButton.setMaxWidth(Double.MAX_VALUE);
        sendReceiptButton.setOnAction(event -> handleTenantReceiptSubmission());

        Button openReceiptButton = createSecondaryButton("OPEN LAST RECEIPT");
        openReceiptButton.setDisable(latestReceipt == null || !latestReceipt.hasReceiptImage());
        openReceiptButton.setMaxWidth(Double.MAX_VALUE);
        openReceiptButton.setOnAction(event -> openReceiptImage(latestReceipt, false));

        Button confirmButton = createSecondaryButton(getTenantPaymentActionLabel(currentTenant));
        confirmButton.setDisable(!canTenantSubmitPayment(currentTenant));
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(event -> {
            staySyncService.submitTenantPaymentForVerification(currentTenant);
            tenantMessage = "Payment sent to the landlord for verification.";
            tenantMessageSuccess = true;
            renderCurrentView();
        });
        actionCard.getChildren().addAll(
                actionEyebrow,
                latest,
                latestCopy,
                receiptPreview,
                sendReceiptButton,
                openReceiptButton,
                confirmButton,
                createMutedCopy(getTenantPaymentActionHelper(currentTenant)));

        HBox.setHgrow(reminderCard, Priority.ALWAYS);
        HBox.setHgrow(actionCard, Priority.ALWAYS);
        row.getChildren().addAll(reminderCard, actionCard);
        return row;
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
        historyTable.getColumns().add(createHistoryStatusColumn("Status", 128));
        historyTable.getColumns().add(createHistoryTextColumn("Updated By", "updatedBy", 126, "payment-history-author-cell"));
        historyTable.getColumns().add(createHistoryReceiptColumn("Receipt", 138));
        historyTable.getColumns().add(createHistoryTextColumn("Note", "note", 340, "payment-history-note-cell"));
        historyTable.getItems().setAll(currentTenant.getPaymentHistory());
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        card.getChildren().addAll(eyebrow, title, summary, historyTable);
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
                createDetailRow("PAYMENT STATUS", currentTenant.getPaymentStatusLabel()));
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

        HBox lowerRow = new HBox(14, createLandlordSelectedCard(), createLandlordResidentsInsightCard(tenants));
        HBox.setHgrow(lowerRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(lowerRow.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(title, pickerSection, lowerRow);
        return card;
    }

    private Node createLandlordControlsRow() {
        VBox summaryColumn = new VBox(14, createLandlordSelectedCard(), createLandlordReceiptReviewCard());
        summaryColumn.getStyleClass().add("controls-primary-column");
        summaryColumn.setPrefWidth(390);
        summaryColumn.setMinWidth(340);

        GridPane actionsGrid = new GridPane();
        actionsGrid.getStyleClass().add("controls-card-grid");
        actionsGrid.setHgap(14);
        actionsGrid.setVgap(14);
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        column.setHgrow(Priority.ALWAYS);
        actionsGrid.getColumnConstraints().addAll(column, column);

        Node statusCard = createLandlordStatusControlCard();
        Node coOccupantCard = createLandlordCoOccupantCard();
        Node notificationCard = createLandlordNotificationCard();
        if (statusCard instanceof Region statusRegion) {
            statusRegion.setMaxWidth(Double.MAX_VALUE);
        }
        if (coOccupantCard instanceof Region coOccupantRegion) {
            coOccupantRegion.setMaxWidth(Double.MAX_VALUE);
        }
        if (notificationCard instanceof Region notificationRegion) {
            notificationRegion.setMaxWidth(Double.MAX_VALUE);
        }

        actionsGrid.add(statusCard, 0, 0);
        actionsGrid.add(coOccupantCard, 1, 0);
        actionsGrid.add(notificationCard, 0, 1, 2, 1);

        HBox row = new HBox(14, summaryColumn, actionsGrid);
        row.getStyleClass().add("landlord-controls-row");
        HBox.setHgrow(actionsGrid, Priority.ALWAYS);
        return row;
    }

    private Node createLandlordSelectedCard() {
        VBox card = createPanelCard("controls-summary-card");
        Label title = new Label("Selected tenant");
        title.getStyleClass().add("card-title");
        TenantAccount tenant = getSelectedTenant();
        if (tenant == null) {
            card.getChildren().addAll(
                    title,
                    createMutedCopy("Select a resident from the Residents view to load account controls."),
                    createLandlordFactGrid(
                            createLandlordFactCell("Name", "No tenant selected"),
                            createLandlordFactCell("Room", "Select a resident first"),
                            createLandlordFactCell("Contact", "-"),
                            createLandlordFactCell("Monthly rent", "-")));
            return card;
        }

        HBox badgeRow = new HBox(
                8,
                createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()),
                tenant.isPaymentAwaitingVerification()
                        ? createOverviewBadge("Awaiting verification", "warning")
                        : createOverviewBadge("Verification clear", "calm"));
        badgeRow.getStyleClass().add("controls-badge-row");

        GridPane factsGrid = createLandlordFactGrid(
                createLandlordFactCell("Name", getOccupantDisplayName(tenant)),
                createLandlordFactCell("Room", getRoomSummary(tenant)),
                createLandlordFactCell("Contact", tenant.getContactNumber()),
                createLandlordFactCell("Monthly rent", getRentDisplay(tenant)),
                createLandlordFactCell("Username", "@" + tenant.getUsername()),
                createLandlordFactCell("Co-occupant", getApprovedCoOccupantLabel(tenant)));

        card.getChildren().addAll(title, badgeRow, factsGrid);
        CoOccupantRequest request = tenant.getCoOccupantRequest();
        if (request != null) {
            card.getChildren().add(createOverviewBadge(
                    "Co-occupant request: " + request.getStatusLabel(),
                    getCoOccupantRequestTone(request)));
        }
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
                tenant.isPaymentAwaitingVerification()
                        ? createOverviewBadge("Awaiting verification", "warning")
                        : createOverviewBadge("Reference only", "calm"),
                submittedMeta,
                createReceiptPreview(latestReceipt, "Receipt image is unavailable.", 360, 220),
                openButton,
                createMutedCopy(latestReceipt.getNote()));
        return card;
    }

    private Node createLandlordStatusControlCard() {
        VBox card = createPanelCard("controls-form-card");
        Label eyebrow = new Label("Controls");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Verify payment, update status and rent");
        title.getStyleClass().add("card-title");

        TenantAccount tenant = getSelectedTenant();
        ComboBox<PaymentStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(PaymentStatus.values()));
        statusBox.getStyleClass().add("ui-combo");
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setValue(tenant == null ? PaymentStatus.PENDING : tenant.getPaymentStatus());

        TextField roomField = createTextField("Enter room number");
        roomField.setText(tenant == null || !tenant.getRoomInfo().hasAssignedRoom() ? "" : tenant.getRoomInfo().getRoomNumber());

        ComboBox<String> roomTypeBox = new ComboBox<>(FXCollections.observableArrayList(StaySyncService.getRoomTypes()));
        roomTypeBox.getStyleClass().add("ui-combo");
        roomTypeBox.setMaxWidth(Double.MAX_VALUE);
        roomTypeBox.setValue(tenant == null || !tenant.getRoomInfo().hasAssignedRoomType() ? null : tenant.getRoomInfo().getRoomType());

        TextField rentField = createTextField("Enter monthly rent");
        rentField.setText(tenant == null || !tenant.getRoomInfo().hasAssignedRent() ? "" : formatEditableAmount(tenant.getRoomInfo().getMonthlyRent()));

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

        Button assignRoomButton = createSecondaryButton("Save room and rent");
        assignRoomButton.setDisable(tenant == null);
        assignRoomButton.setMaxWidth(Double.MAX_VALUE);
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
        deleteButton.setOnAction(event -> handleTenantDeletion(tenant));

        Button residentsButton = createSecondaryButton("Back to residents");
        residentsButton.setMaxWidth(Double.MAX_VALUE);
        residentsButton.setOnAction(event -> {
            landlordSection = LandlordSection.RESIDENTS;
            renderCurrentView();
        });

        Label hint = createMutedCopy(tenant == null
                ? "Select a tenant to enable status, room, and rent controls."
                : tenant.isPaymentAwaitingVerification()
                        ? "This tenant already sent payment for verification. Review the receipt, then set the status to Paid once confirmed."
                        : "Adjust payment status or assign the tenant's room, room type, and monthly rent here.");

        Label deleteHint = createMutedCopy(tenant == null
                ? "Delete stays disabled until a tenant is selected."
                : "Deleting a tenant permanently removes the resident account and cannot be undone.");

        GridPane assignmentGrid = new GridPane();
        assignmentGrid.getStyleClass().add("controls-form-grid");
        assignmentGrid.setHgap(12);
        assignmentGrid.setVgap(12);
        ColumnConstraints assignmentColumn = new ColumnConstraints();
        assignmentColumn.setPercentWidth(33.333);
        assignmentColumn.setHgrow(Priority.ALWAYS);
        assignmentGrid.getColumnConstraints().addAll(assignmentColumn, assignmentColumn, assignmentColumn);
        assignmentGrid.add(createFieldGroup("ROOM NUMBER", roomField), 0, 0);
        assignmentGrid.add(createFieldGroup("ROOM TYPE", roomTypeBox), 1, 0);
        assignmentGrid.add(createFieldGroup("MONTHLY RENT", rentField), 2, 0);

        GridPane actionGrid = new GridPane();
        actionGrid.getStyleClass().add("controls-action-grid");
        actionGrid.setHgap(10);
        actionGrid.setVgap(10);
        ColumnConstraints actionColumn = new ColumnConstraints();
        actionColumn.setPercentWidth(50);
        actionColumn.setHgrow(Priority.ALWAYS);
        actionGrid.getColumnConstraints().addAll(actionColumn, actionColumn);
        GridPane.setHgrow(applyButton, Priority.ALWAYS);
        GridPane.setHgrow(assignRoomButton, Priority.ALWAYS);
        GridPane.setHgrow(residentsButton, Priority.ALWAYS);
        GridPane.setHgrow(deleteButton, Priority.ALWAYS);
        actionGrid.add(applyButton, 0, 0);
        actionGrid.add(assignRoomButton, 1, 0);
        actionGrid.add(residentsButton, 0, 1);
        actionGrid.add(deleteButton, 1, 1);

        card.getChildren().addAll(
                eyebrow,
                title,
                createFieldGroup("NEW STATUS", statusBox),
                assignmentGrid,
                actionGrid,
                hint,
                deleteHint);
        return card;
    }

    private Node createLandlordCoOccupantCard() {
        VBox card = createPanelCard("controls-form-card");
        Label eyebrow = new Label("Residence approval");
        eyebrow.getStyleClass().add("eyebrow-copy");
        Label title = new Label("Co-occupant request");
        title.getStyleClass().add("card-title");

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

        ComboBox<NotificationType> typeBox = new ComboBox<>(FXCollections.observableArrayList(NotificationType.values()));
        typeBox.getStyleClass().add("ui-combo");
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setValue(landlordNotificationType);

        TextField titleField = createTextField("Enter notice title");
        titleField.setText(landlordNotificationTitle);

        TextArea messageArea = createTextArea("Enter the message the tenant should see");
        messageArea.setText(landlordNotificationMessageBody);
        messageArea.setPrefRowCount(5);

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
                topFields,
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
        Label spotlightLabel = new Label("Accounts to review");
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
        tenantCoOccupantRequestName = "";
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
        tenantCoOccupantRequestName = "";
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

        tenantMessage = currentTenant.isPaymentAwaitingVerification()
                ? "Receipt photo updated while your payment is waiting for verification."
                : "Receipt photo uploaded. Send payment when you're ready for landlord verification.";
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
                createDetailRow("ROOM", getRoomSummary(tenant)));
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

    private boolean canTenantSubmitPayment(TenantAccount tenant) {
        return tenant != null
                && hasBillingAssignment(tenant)
                && tenant.getPaymentStatus() != PaymentStatus.PAID
                && !tenant.isPaymentAwaitingVerification();
    }

    private String getTenantPaymentActionLabel(TenantAccount tenant) {
        if (tenant == null) {
            return "SEND PAYMENT";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "MARKED AS PAID";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "WAITING FOR VERIFICATION";
        }
        return "SEND PAYMENT";
    }

    private String getTenantPaymentActionHelper(TenantAccount tenant) {
        if (tenant == null || !hasBillingAssignment(tenant)) {
            return "Payment actions unlock after the landlord assigns your room and monthly rent.";
        }
        if (tenant.getPaymentStatus() == PaymentStatus.PAID) {
            return "The landlord already verified this payment cycle.";
        }
        if (tenant.isPaymentAwaitingVerification()) {
            return "Your payment request is already with the landlord. The button will change to Marked as paid after verification.";
        }
        return "Send payment here, then wait for the landlord to review the receipt and verify it.";
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
        Label name = new Label(getOccupantDisplayName(tenant));
        name.getStyleClass().add("sidebar-name");
        name.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.getChildren().add(createStatusPill(tenant.getPaymentStatusLabel(), tenant.getPaymentStatus()));

        VBox meta = new VBox(2);
        meta.getStyleClass().add("resident-picker-meta");
        Label room = new Label(getResidentPickerRoomLabel(tenant));
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
        details.add(createResidentPickerDetail("Monthly rent", getRentDisplay(tenant)), 1, 0);
        details.add(createResidentPickerDetail("Username", tenant.getUsername()), 0, 1);
        details.add(createResidentPickerDetail("Room type", getRoomTypeDisplay(tenant)), 1, 1);

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

    private GridPane createLandlordFactGrid(VBox... cells) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("controls-detail-grid");
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(50);
        column.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(column, column);

        for (int index = 0; index < cells.length; index++) {
            VBox cell = cells[index];
            GridPane.setHgrow(cell, Priority.ALWAYS);
            grid.add(cell, index % 2, index / 2);
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

    private boolean hasBillingAssignment(TenantAccount tenant) {
        return tenant != null && tenant.getRoomInfo().isAssignmentComplete();
    }

    private String getOccupantDisplayName(TenantAccount tenant) {
        if (tenant == null) {
            return "No occupants assigned";
        }
        return tenant.getOccupantDisplayName();
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
            return "Every receipt upload and payment review will be tracked here in order.";
        }

        PaymentRecord latest = paymentHistory.get(0);
        String updateCountLabel = paymentHistory.size() == 1 ? "update" : "updates";
        return paymentHistory.size() + " " + updateCountLabel + " recorded. Latest: "
                + latest.getStatus().getLabel() + " by " + latest.getUpdatedBy()
                + (latest.hasReceiptImage() ? " with a receipt attached." : ".");
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
