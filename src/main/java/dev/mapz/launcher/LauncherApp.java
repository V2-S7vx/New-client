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
import javafx.scene.layout.BorderPane;
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
    private static final String DEV_USERNAME = "_mapz.2";
    private static final String DEV_SKIN_URL = "https://mc-heads.net/skin/_mapz.2";
    private static final String[] QUOTES = {
            "SPACE CLIENT #1",
            "Did you know? SPACE CLIENT is built for Minecraft.",
            "Your Minecraft. Your space.",
            "Ready for launch?",
            "Made for a cleaner way to play.",
            "Welcome aboard, Minecraft player."
    };

    private final MicrosoftAuth auth = new MicrosoftAuth();
    private Label loginStatus;
    private Stage launcherStage;
    private Starfield starfield;
    private boolean fullscreen = true;

    @Override
    public void start(Stage stage) {
        launcherStage = stage;
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Space Launcher");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

        StackPane root = new StackPane();
        starfield = new Starfield();
        starfield.widthProperty().bind(root.widthProperty());
        starfield.heightProperty().bind(root.heightProperty());
        root.getChildren().add(starfield);
        root.getChildren().add(buildLoginView(root));

        Scene scene = new Scene(root, DESIGN_WIDTH, DESIGN_HEIGHT);
        scene.setFill(Color.web("#020305"));
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.F11) {
                toggleFullscreen();
                event.consume();
            }
        });
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();

        FadeTransition intro = new FadeTransition(Duration.millis(700), root);
        intro.setFromValue(0);
        intro.setToValue(1);
        intro.play();
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        launcherStage.setFullScreen(fullscreen);
        if (!fullscreen) launcherStage.setMaximized(true);
    }

    private BorderPane buildLoginView(StackPane root) {
        Label logo = new Label("SPACE");
        logo.getStyleClass().add("logo");

        Label title = new Label("Minecraft, simplified.");
        title.getStyleClass().add("title");

        loginStatus = new Label("Sign in with your Microsoft account to continue");
        loginStatus.getStyleClass().add("subtitle");
        loginStatus.setWrapText(true);

        Button signIn = new Button("CLICK HERE TO SIGN IN TO MICROSOFT");
        signIn.getStyleClass().add("primary-button");
        setupSmoothHover(signIn, 1.025);
        signIn.setOnAction(e -> beginMicrosoftSignIn(signIn));

        Button devSkip = new Button("DEV SKIP");
        devSkip.getStyleClass().add("dev-button");
        setupSmoothHover(devSkip, 1.045);
        devSkip.setOnAction(e -> showLauncher(root, new MicrosoftAuth.MinecraftProfile(
                DEV_USERNAME,
                "dev-profile",
                DEV_SKIN_URL,
                "CLASSIC",
                ""
        )));

        Label hint = new Label("Microsoft authentication will be connected here later");
        hint.getStyleClass().add("note");

        VBox content = new VBox(16, logo, title, loginStatus, signIn, devSkip, hint);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("login-content");
        content.setMaxWidth(680);

        StackPane center = new StackPane(content);
        center.setAlignment(Pos.CENTER);
        center.setTranslateY(-18);

        BorderPane layout = new BorderPane(center);
        layout.getStyleClass().add("root-content");

        QuoteTicker quotes = new QuoteTicker();
        root.getChildren().add(quotes);
        StackPane.setAlignment(quotes, Pos.BOTTOM_CENTER);
        StackPane.setMargin(quotes, new Insets(0, 0, 55, 0));

        Label version = new Label("SPACE LAUNCHER  •  0.3");
        version.getStyleClass().add("version");
        layout.setBottom(version);
        BorderPane.setAlignment(version, Pos.CENTER);
        BorderPane.setMargin(version, new Insets(0, 0, 18, 0));
        layout.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());
        return layout;
    }

    private void setupSmoothHover(Button button, double scale) {
        button.setOnMouseEntered(e -> animateButton(button, scale, 1.0));
        button.setOnMouseExited(e -> animateButton(button, 1.0, 0.0));
    }

    private void animateButton(Button button, double scale, double ignored) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(260), button);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        transition.play();
    }

    private void beginMicrosoftSignIn(Button signIn) {
        signIn.setDisable(true);
        signIn.getStyleClass().add("loading");
        loginStatus.setText("Opening Microsoft sign-in…");

        auth.signIn(new MicrosoftAuth.Listener() {
            @Override
            public void onDeviceCode(String message, String url) {
                Platform.runLater(() -> {
                    loginStatus.setText("Microsoft sign-in opened in your browser. Finish signing in there…");
                    try {
                        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
                    } catch (Exception ignored) { }
                });
            }

            @Override
            public void onSuccess(MicrosoftAuth.MinecraftProfile profile) {
                Platform.runLater(() -> showLauncher(rootOf(signIn), profile));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    signIn.setDisable(false);
                    signIn.getStyleClass().remove("loading");
                    loginStatus.setText(message);
                });
            }
        });
    }

    private StackPane rootOf(Button node) {
        return (StackPane) node.getScene().getRoot();
    }

    private void showLauncher(StackPane root, MicrosoftAuth.MinecraftProfile profile) {
        // Keep only the animated starfield. The login screen must be completely replaced.
        root.getChildren().clear();
        root.getChildren().add(starfield);

        Label brand = new Label("SPACE");
        brand.getStyleClass().add("brand-small");

        SkinRenderer skin = new SkinRenderer();
        skin.setWidth(230);
        skin.setHeight(305);
        skin.load(profile.skinUrl(), "SLIM".equals(profile.model()));

        Label name = new Label(profile.name());
        name.getStyleClass().add("profile-name");
        Label account = new Label("MICROSOFT ACCOUNT  •  MINECRAFT: JAVA EDITION");
        account.getStyleClass().add("profile-subtitle");

        VBox profileBox = new VBox(0, skin, name, account);
        profileBox.setAlignment(Pos.CENTER);

        Button play = new Button("PLAY");
        play.getStyleClass().add("play-button");
        setupSmoothHover(play, 1.035);
        play.setOnAction(e -> { });

        VBox centerBox = new VBox(4, brand, profileBox);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setTranslateY(92);

        StackPane center = new StackPane(centerBox);
        center.setAlignment(Pos.CENTER);

        BorderPane layout = new BorderPane(center);
        layout.getStyleClass().add("root-content");
        layout.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());

        StackPane playHolder = new StackPane(play);
        playHolder.setAlignment(Pos.CENTER);
        BorderPane.setMargin(playHolder, new Insets(0, 0, 38, 0));
        layout.setBottom(playHolder);

        Label version = new Label("1.21.x  •  VANILLA");
        version.getStyleClass().add("version-pill");
        layout.setTop(version);
        BorderPane.setAlignment(version, Pos.TOP_RIGHT);
        BorderPane.setMargin(version, new Insets(22, 24, 0, 0));

        root.getChildren().add(layout);
        layout.setOpacity(0);
        layout.setTranslateY(18);

        FadeTransition fade = new FadeTransition(Duration.millis(650), layout);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(700), layout);
        slide.setFromY(18);
        slide.setToY(0);
        javafx.animation.Interpolator smooth = javafx.animation.Interpolator.EASE_OUT;
        fade.setInterpolator(smooth);
        slide.setInterpolator(smooth);
        new ParallelTransition(fade, slide).play();
    }

    public static void main(String[] args) { launch(args); }

    private static final class QuoteTicker extends StackPane {
        private final Label label = new Label();
        private int index;

        QuoteTicker() {
            label.getStyleClass().add("quote");
            label.setText(QUOTES[0]);
            getChildren().add(label);
            setMouseTransparent(true);
            setMinHeight(36);
            setMaxHeight(36);

            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.seconds(30), e -> nextQuote()));
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timeline.play();
        }

        private void nextQuote() {
            index = (index + 1) % QUOTES.length;
            FadeTransition out = new FadeTransition(Duration.millis(500), label);
            out.setToValue(0);
            TranslateTransition drop = new TranslateTransition(Duration.millis(500), label);
            drop.setToY(12);
            ParallelTransition leave = new ParallelTransition(out, drop);
            leave.setOnFinished(e -> {
                label.setText(QUOTES[index]);
                label.setTranslateY(-12);
                FadeTransition in = new FadeTransition(Duration.millis(800), label);
                in.setFromValue(0);
                in.setToValue(1);
                TranslateTransition rise = new TranslateTransition(Duration.millis(800), label);
                rise.setFromY(-12);
                rise.setToY(0);
                new ParallelTransition(in, rise).play();
            });
            leave.play();
        }
    }

    private static final class Starfield extends javafx.scene.canvas.Canvas {
        private final Star[] stars = new Star[340];
        private final Random random = new Random(1337);
        private long lastFrame;

        Starfield() {
            for (int i = 0; i < stars.length; i++) {
                stars[i] = new Star(random.nextDouble(), random.nextDouble(), random.nextDouble());
            }
            new AnimationTimer() {
                @Override public void handle(long now) {
                    if (lastFrame == 0) lastFrame = now;
                    double dt = Math.min((now - lastFrame) / 1_000_000_000.0, 0.033);
                    lastFrame = now;
                    draw(dt);
                }
            }.start();
        }

        private void draw(double dt) {
            double w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            var g = getGraphicsContext2D();
            g.setFill(Color.web("#020305"));
            g.fillRect(0, 0, w, h);
            for (Star s : stars) {
                s.y += dt * (0.004 + s.depth * 0.012);
                if (s.y > 1.03) s.y = -0.03;
                double size = 0.55 + s.depth * 1.65;
                double opacity = 0.16 + s.depth * 0.68;
                g.setFill(Color.WHITE.deriveColor(0, 1, 1, opacity));
                g.fillOval(s.x * w, s.y * h, size, size);
            }
        }

        private static final class Star {
            double x, y, depth;
            Star(double x, double y, double depth) { this.x = x; this.y = y; this.depth = depth; }
        }
    }
}
