package dev.mapz.launcher;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
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

    private final MicrosoftAuth auth = new MicrosoftAuth();
    private Label loginStatus;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        StackPane root = new StackPane();
        Starfield starfield = new Starfield();
        starfield.widthProperty().bind(root.widthProperty());
        starfield.heightProperty().bind(root.heightProperty());
        root.getChildren().add(starfield);
        root.getChildren().add(buildLoginView(root));

        Scene scene = new Scene(root, DESIGN_WIDTH, DESIGN_HEIGHT);
        scene.setFill(Color.web("#030508"));
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        FadeTransition intro = new FadeTransition(Duration.millis(500), root);
        intro.setFromValue(0);
        intro.setToValue(1);
        intro.play();
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
        signIn.setOnAction(e -> beginMicrosoftSignIn(signIn));

        Label hint = new Label("Your Microsoft account is used only to authenticate your Minecraft profile.");
        hint.getStyleClass().add("note");
        hint.setWrapText(true);
        hint.setMaxWidth(520);
        hint.setAlignment(Pos.CENTER);

        VBox content = new VBox(20, logo, title, loginStatus, signIn, hint);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("login-content");
        content.setMaxWidth(650);

        StackPane center = new StackPane(content);
        center.setAlignment(Pos.CENTER);
        BorderPane layout = new BorderPane(center);
        layout.getStyleClass().add("root-content");

        Label version = new Label("SPACE LAUNCHER  •  0.2");
        version.getStyleClass().add("version");
        layout.setBottom(version);
        BorderPane.setAlignment(version, Pos.CENTER);
        BorderPane.setMargin(version, new Insets(0, 0, 24, 0));
        layout.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());
        return layout;
    }

    private void beginMicrosoftSignIn(Button signIn) {
        signIn.setDisable(true);
        loginStatus.setText("Opening Microsoft sign-in…");

        auth.signIn(new MicrosoftAuth.Listener() {
            @Override
            public void onDeviceCode(String message, String url) {
                Platform.runLater(() -> {
                    loginStatus.setText("Microsoft opened in your browser. Finish signing in there…");
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
                    loginStatus.setText(message);
                });
            }
        });
    }

    private StackPane rootOf(Button node) {
        return (StackPane) node.getScene().getRoot();
    }

    private void showLauncher(StackPane root, MicrosoftAuth.MinecraftProfile profile) {
        if (root.getChildren().size() > 1) root.getChildren().remove(1);

        Label brand = new Label("SPACE");
        brand.getStyleClass().add("brand-small");

        SkinRenderer skin = new SkinRenderer();
        skin.setWidth(300);
        skin.setHeight(390);
        skin.load(profile.skinUrl(), "SLIM".equals(profile.model()));

        Label name = new Label(profile.name());
        name.getStyleClass().add("profile-name");
        Label account = new Label("Microsoft account  •  Minecraft: Java Edition");
        account.getStyleClass().add("profile-subtitle");

        VBox profileBox = new VBox(4, skin, name, account);
        profileBox.setAlignment(Pos.CENTER);

        Button play = new Button("PLAY");
        play.getStyleClass().add("play-button");
        play.setOnAction(e -> { });

        VBox centerBox = new VBox(8, brand, profileBox);
        centerBox.setAlignment(Pos.CENTER);
        StackPane center = new StackPane(centerBox);
        center.setAlignment(Pos.CENTER);
        StackPane.setMargin(centerBox, new Insets(-5, 0, 55, 0));

        BorderPane layout = new BorderPane(center);
        layout.getStyleClass().add("root-content");
        layout.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());

        StackPane playHolder = new StackPane(play);
        playHolder.setAlignment(Pos.CENTER);
        BorderPane.setMargin(playHolder, new Insets(0, 0, 34, 0));
        layout.setBottom(playHolder);

        Label version = new Label("1.21.x  •  VANILLA");
        version.getStyleClass().add("version-pill");
        layout.setTop(version);
        BorderPane.setAlignment(version, Pos.TOP_RIGHT);
        BorderPane.setMargin(version, new Insets(26, 28, 0, 0));

        root.getChildren().add(layout);
        layout.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(400), layout);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public static void main(String[] args) { launch(args); }

    private static final class Starfield extends javafx.scene.canvas.Canvas {
        private final Star[] stars = new Star[300];
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
