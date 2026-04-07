package staysync.ui;

import java.net.URL;
import java.nio.file.Path;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StaySyncAuthLandingView extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene = createScene();
        stage.setTitle("StaySync");
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    public static Scene createScene() {
        Parent content = createContent();
        Scene scene = new Scene(content, 1360, 860);
        scene.getStylesheets().add(resolveStylesheet());
        return scene;
    }

    public static Parent createContent() {
        StackPane root = new StackPane();
        root.getStyleClass().add("auth-root");

        BorderPane mainPanel = new BorderPane();
        mainPanel.getStyleClass().add("main-panel");
        mainPanel.setTop(createTopBar());
        mainPanel.setCenter(createMainContent());

        StackPane.setMargin(mainPanel, new Insets(36));
        root.getChildren().add(mainPanel);
        return root;
    }

    private static HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox brandArea = new HBox(14);
        brandArea.getStyleClass().add("brand-area");
        brandArea.setAlignment(Pos.CENTER_LEFT);

        StackPane logoBox = new StackPane();
        logoBox.getStyleClass().add("logo-box");

        Label logoLetter = new Label("S");
        logoLetter.getStyleClass().add("logo-letter");
        logoBox.getChildren().add(logoLetter);

        VBox brandCopy = new VBox(2);
        brandCopy.getStyleClass().add("brand-copy");

        Label appName = new Label("StaySync");
        appName.getStyleClass().add("app-name");

        Label appSubtitle = new Label("Dorm and apartment management");
        appSubtitle.getStyleClass().add("app-subtitle");
        brandCopy.getChildren().addAll(appName, appSubtitle);

        brandArea.getChildren().addAll(logoBox, brandCopy);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button signInButton = new Button("Sign in");
        signInButton.getStyleClass().addAll("button-base", "button-ghost");

        topBar.getChildren().addAll(brandArea, spacer, signInButton);
        return topBar;
    }

    private static HBox createMainContent() {
        HBox contentRow = new HBox(56);
        contentRow.getStyleClass().add("content-row");
        contentRow.setAlignment(Pos.CENTER_LEFT);

        VBox heroColumn = createHeroColumn();
        VBox formColumn = createSignupColumn();

        HBox.setHgrow(heroColumn, Priority.ALWAYS);
        HBox.setHgrow(formColumn, Priority.ALWAYS);

        contentRow.getChildren().addAll(heroColumn, formColumn);
        return contentRow;
    }

    private static VBox createHeroColumn() {
        VBox heroColumn = new VBox(24);
        heroColumn.getStyleClass().addAll("hero-section", "left-content");
        heroColumn.setAlignment(Pos.CENTER_LEFT);
        heroColumn.setMaxWidth(580);

        Label pillLabel = new Label("Housing operations made simple");
        pillLabel.getStyleClass().add("pill-label");

        Label headline = new Label("Minimal housing management with a premium feel.");
        headline.getStyleClass().add("hero-headline");
        headline.setWrapText(true);

        Label description = new Label(
                "Manage residents, rooms, billing, and maintenance from one calm, focused workspace built for daily operations.");
        description.getStyleClass().add("hero-description");
        description.setWrapText(true);

        HBox actionRow = new HBox(14);
        actionRow.getStyleClass().add("hero-actions");
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button createAccountButton = new Button("Create account");
        createAccountButton.getStyleClass().addAll("button-base", "button-primary");

        Button viewDemoButton = new Button("View demo");
        viewDemoButton.getStyleClass().addAll("button-base", "button-secondary");

        actionRow.getChildren().addAll(createAccountButton, viewDemoButton);
        heroColumn.getChildren().addAll(pillLabel, headline, description, actionRow);
        return heroColumn;
    }

    private static VBox createSignupColumn() {
        VBox signupCard = new VBox(18);
        signupCard.getStyleClass().addAll("signup-card", "right-content");
        signupCard.setAlignment(Pos.TOP_LEFT);
        signupCard.setMaxWidth(440);
        signupCard.setPrefWidth(440);

        Label cardEyebrow = new Label("Create account");
        cardEyebrow.getStyleClass().add("card-eyebrow");

        Label cardTitle = new Label("Get started");
        cardTitle.getStyleClass().add("card-title");

        Label cardSubtitle = new Label(
                "Set up your StaySync account and start managing rooms, residents, and billing from one clear desktop workspace.");
        cardSubtitle.getStyleClass().add("card-subtitle");
        cardSubtitle.setWrapText(true);

        VBox formFields = new VBox(12);
        formFields.getStyleClass().add("form-fields");

        formFields.getChildren().addAll(
                createFieldGroup("Full name", createTextField("Enter your full name")),
                createFieldGroup("Email or username", createTextField("Enter your email or username")),
                createFieldGroup("Password", createPasswordField("Create a password")));

        Button submitButton = new Button("Create account");
        submitButton.getStyleClass().addAll("button-base", "button-primary", "button-full");
        submitButton.setMaxWidth(Double.MAX_VALUE);

        HBox footerRow = new HBox(6);
        footerRow.getStyleClass().add("footer-row");
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Label footerCopy = new Label("Already have an account?");
        footerCopy.getStyleClass().add("footer-copy");

        Button footerSignIn = new Button("Sign in");
        footerSignIn.getStyleClass().addAll("button-link");

        footerRow.getChildren().addAll(footerCopy, footerSignIn);

        signupCard.getChildren().addAll(cardEyebrow, cardTitle, cardSubtitle, formFields, submitButton, footerRow);
        return signupCard;
    }

    private static VBox createFieldGroup(String labelText, TextField field) {
        VBox fieldGroup = new VBox(7);
        fieldGroup.getStyleClass().add("field-group");

        Label fieldLabel = new Label(labelText);
        fieldLabel.getStyleClass().add("field-label");

        fieldGroup.getChildren().addAll(fieldLabel, field);
        return fieldGroup;
    }

    private static TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("text-input");
        return field;
    }

    private static PasswordField createPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.getStyleClass().add("text-input");
        return field;
    }

    private static String resolveStylesheet() {
        URL bundledStylesheet = StaySyncAuthLandingView.class.getResource("staysync-auth.css");
        if (bundledStylesheet != null) {
            return bundledStylesheet.toExternalForm();
        }
        return Path.of("src", "main", "java", "staysync", "ui", "staysync-auth.css").toUri().toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
