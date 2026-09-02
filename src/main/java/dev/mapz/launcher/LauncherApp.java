package dev.mapz.launcher;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.util.Random;

public final class LauncherApp extends Application {
    private static final double DESIGN_WIDTH = 1440;
    private static final double DESIGN_HEIGHT = 900;
    private static final String DEV_USERNAME = "_mapz";
    private static final String DEV_SKIN_URL = "https://mc-heads.net/skin/_mapz";
    private static final String[] QUOTES = {
            "SPACE CLIENT #1", "Your Minecraft. Your space.", "Ready for launch?",
            "Made for a cleaner way to play.", "Welcome aboard, Minecraft player."
    };

    private final MicrosoftAuth auth = new MicrosoftAuth();
    private Stage launcherStage;
    private boolean fullscreen = true;
    private Starfield starfield;
    private StackPane content;
    private Label loginStatus;

    @Override
    public void start(Stage stage) {
        launcherStage = stage;
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Space Client");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

        StackPane root = new StackPane();
        starfield = new Starfield();
        starfield.widthProperty().bind(root.widthProperty());
        starfield.heightProperty().bind(root.heightProperty());
        root.getChildren().add(starfield);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("shell");
        shell.setTop(buildTopBar());
        shell.setLeft(buildSidebar());
        content = new StackPane();
        content.getStyleClass().add("content-area");
        shell.setCenter(content);
        root.getChildren().add(shell);

        QuoteTicker quotes = new QuoteTicker();
        root.getChildren().add(quotes);
        StackPane.setAlignment(quotes, Pos.BOTTOM_CENTER);
        StackPane.setMargin(quotes, new Insets(0, 0, 22, 66));

        Scene scene = new Scene(root, DESIGN_WIDTH, DESIGN_HEIGHT);
        scene.setFill(Color.web("#020305"));
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F11) {
                toggleFullscreen();
                event.consume();
            }
        });
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
        showLogin();

        FadeTransition intro = new FadeTransition(Duration.millis(750), shell);
        intro.setFromValue(0); intro.setToValue(1); intro.play();
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("S");
        logo.getStyleClass().add("top-logo");
        VBox labels = new VBox(0);
        labels.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Space Client");
        title.getStyleClass().add("top-title");
        Label version = new Label("Space Client - 1.21.11");
        version.getStyleClass().add("top-version");
        labels.getChildren().addAll(title, version);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label account = new Label("DEV ACCOUNT");
        account.getStyleClass().add("account-label");
        bar.getChildren().addAll(logo, labels, spacer, account);
        return bar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(9);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setAlignment(Pos.TOP_CENTER);
        String[] icons = {"▶", "▣", "⚒", "♙", "▤", "⚙", "↪"};
        for (int i = 0; i < icons.length; i++) {
            final int index = i;
            Button b = new Button(icons[i]);
            b.getStyleClass().add("nav-button");
            if (i == 0) b.getStyleClass().add("nav-active");
            setupSmoothHover(b, 1.045);
            b.setOnAction(e -> { if (index == 0) showHome(); else showSimpleScreen(navTitle(index)); });
            sidebar.getChildren().add(b);
        }
        return sidebar;
    }

    private String navTitle(int index) {
        return switch (index) {
            case 1 -> "LIBRARY"; case 2 -> "TOOLS"; case 3 -> "ACCOUNTS";
            case 4 -> "NEWS"; case 5 -> "SETTINGS"; case 6 -> "EXIT"; default -> "SPACE CLIENT";
        };
    }

    private void showLogin() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("login-content");
        Label title = new Label("Minecraft, simplified.");
        title.getStyleClass().add("title");
        loginStatus = new Label("Sign in with your Microsoft account to continue");
        loginStatus.getStyleClass().add("subtitle");
        Button signIn = new Button("CLICK HERE TO SIGN IN TO MICROSOFT");
        signIn.getStyleClass().add("primary-button");
        setupSmoothHover(signIn, 1.025);
        signIn.setOnAction(e -> beginMicrosoftSignIn(signIn));
        Button devSkip = new Button("DEV SKIP");
        devSkip.getStyleClass().add("dev-button");
        setupSmoothHover(devSkip, 1.045);
        devSkip.setOnAction(e -> showLauncher(new MicrosoftAuth.MinecraftProfile(
                DEV_USERNAME, "dev-profile", DEV_SKIN_URL, "CLASSIC", "")));
        box.getChildren().addAll(title, loginStatus, signIn, devSkip);
        content.getChildren().setAll(box);
        animateIn(box);
    }

    private void beginMicrosoftSignIn(Button signIn) {
        signIn.setDisable(true);
        loginStatus.setText("Opening Microsoft sign-in…");
        auth.signIn(new MicrosoftAuth.Listener() {
            @Override public void onDeviceCode(String message, String url) {
                Platform.runLater(() -> {
                    loginStatus.setText("Microsoft sign-in opened in your browser. Finish signing in there…");
                    try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url)); }
                    catch (Exception ignored) { }
                });
            }
            @Override public void onSuccess(MicrosoftAuth.MinecraftProfile profile) { Platform.runLater(() -> showLauncher(profile)); }
            @Override public void onError(String message) { Platform.runLater(() -> { signIn.setDisable(false); loginStatus.setText(message); }); }
        });
    }

    private void showHome() {
        showLauncher(new MicrosoftAuth.MinecraftProfile(DEV_USERNAME, "dev-profile", DEV_SKIN_URL, "CLASSIC", ""));
    }

    private void showLauncher(MicrosoftAuth.MinecraftProfile profile) {
        VBox home = new VBox(0);
        home.setAlignment(Pos.CENTER);
        home.getStyleClass().add("home-view");

        Label name = new Label(profile.name());
        name.getStyleClass().add("profile-name-top");

        SkinRenderer skin = new SkinRenderer();
        skin.setWidth(300); skin.setHeight(330);
        skin.load(profile.skinUrl(), "SLIM".equals(profile.model()));

        VBox player = new VBox(-2, name, skin);
        player.setAlignment(Pos.CENTER);
        player.setTranslateY(15);

        Button play = new Button("P L A Y");
        play.getStyleClass().add("play-button");
        setupSmoothHover(play, 1.025);
        play.setOnAction(e -> { });
        Button drop = new Button("⌄");
        drop.getStyleClass().add("play-drop");
        setupSmoothHover(drop, 1.035);

        HBox playRow = new HBox(8, play, drop);
        playRow.setAlignment(Pos.CENTER);
        VBox playBox = new VBox(2, playRow);
        playBox.setAlignment(Pos.CENTER);
        playBox.setTranslateY(48);

        home.getChildren().addAll(player, playBox);
        content.getChildren().setAll(home);
        animateIn(home);
    }

    private void showSimpleScreen(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("simple-screen-title");
        StackPane screen = new StackPane(title);
        screen.getStyleClass().add("simple-screen");
        content.getChildren().setAll(screen);
        animateIn(screen);
    }

    private void animateIn(javafx.scene.Node node) {
        node.setOpacity(0); node.setTranslateY(12);
        FadeTransition fade = new FadeTransition(Duration.millis(650), node);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), node);
        slide.setFromY(12); slide.setToY(0); slide.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private void setupSmoothHover(Button button, double scale) {
        button.setOnMouseEntered(e -> animateButton(button, scale));
        button.setOnMouseExited(e -> animateButton(button, 1.0));
    }

    private void animateButton(Button button, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(320), button);
        transition.setToX(scale); transition.setToY(scale);
        transition.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        transition.play();
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        launcherStage.setFullScreen(fullscreen);
        if (!fullscreen) launcherStage.setMaximized(true);
    }

    public static void main(String[] args) { launch(args); }

    private static final class QuoteTicker extends StackPane {
        private final Label label = new Label(QUOTES[0]); private int index;
        QuoteTicker() {
            label.getStyleClass().add("quote"); getChildren().add(label); setMouseTransparent(true);
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.seconds(30), e -> next()));
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE); timeline.play();
        }
        private void next() {
            index = (index + 1) % QUOTES.length;
            FadeTransition out = new FadeTransition(Duration.millis(600), label); out.setToValue(0);
            TranslateTransition move = new TranslateTransition(Duration.millis(600), label); move.setToY(12);
            ParallelTransition leave = new ParallelTransition(out, move);
            leave.setOnFinished(e -> {
                label.setText(QUOTES[index]); label.setTranslateY(-12);
                FadeTransition in = new FadeTransition(Duration.millis(900), label); in.setFromValue(0); in.setToValue(1);
                TranslateTransition rise = new TranslateTransition(Duration.millis(900), label); rise.setFromY(-12); rise.setToY(0);
                new ParallelTransition(in, rise).play();
            }); leave.play();
        }
    }

    private static final class Starfield extends javafx.scene.canvas.Canvas {
        private final Star[] stars = new Star[340]; private final Random random = new Random(1337); private long lastFrame;
        Starfield() {
            for (int i = 0; i < stars.length; i++) stars[i] = new Star(random.nextDouble(), random.nextDouble(), random.nextDouble());
            new AnimationTimer() { @Override public void handle(long now) {
                if (lastFrame == 0) lastFrame = now;
                double dt = Math.min((now-lastFrame)/1_000_000_000.0, 0.033); lastFrame = now; draw(dt);
            }}.start();
        }
        private void draw(double dt) {
            double w=getWidth(), h=getHeight(); if(w<=0||h<=0)return; var g=getGraphicsContext2D();
            g.setFill(Color.web("#020305")); g.fillRect(0,0,w,h);
            for(Star s:stars){ s.y+=dt*(0.004+s.depth*0.012); if(s.y>1.03)s.y=-0.03;
                double size=0.55+s.depth*1.65; g.setFill(Color.WHITE.deriveColor(0,1,1,0.16+s.depth*0.68)); g.fillOval(s.x*w,s.y*h,size,size); }
        }
        private static final class Star { double x,y,depth; Star(double x,double y,double depth){this.x=x;this.y=y;this.depth=depth;} }
    }
}
