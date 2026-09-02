package dev.mapz.launcher;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Random;

public final class LauncherApp extends Application {
    private static final double DESIGN_WIDTH = 1440;
    private static final double DESIGN_HEIGHT = 900;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMaximized(true);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        StackPane root = new StackPane();
        Starfield starfield = new Starfield();
        starfield.widthProperty().bind(root.widthProperty());
        starfield.heightProperty().bind(root.heightProperty());
        root.getChildren().add(starfield);

        BorderPane content = buildLoginView(root);
        root.getChildren().add(content);

        Scene scene = new Scene(root, DESIGN_WIDTH, DESIGN_HEIGHT);
        scene.setFill(Color.web("#05070d"));
        stage.setScene(scene);
        stage.show();

        FadeTransition intro = new FadeTransition(Duration.millis(550), root);
        intro.setFromValue(0);
        intro.setToValue(1);
        intro.play();
    }

    private BorderPane buildLoginView(StackPane root) {
        Label logo = new Label("_mapz");
        logo.getStyleClass().add("logo");
        Label title = new Label("Welcome to _mapz");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Sign in with your Microsoft account to continue");
        subtitle.getStyleClass().add("subtitle");
        Button signIn = new Button("CONTINUE WITH MICROSOFT");
        signIn.getStyleClass().add("primary-button");
        signIn.setOnAction(e -> showLauncherPreview(root));
        Label note = new Label("Microsoft authentication will be connected in a later build");
        note.getStyleClass().add("note");

        VBox card = new VBox(18, logo, title, subtitle, signIn, note);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(520);

        StackPane center = new StackPane(card);
        center.setAlignment(Pos.CENTER);
        BorderPane layout = new BorderPane(center);
        layout.getStyleClass().add("root-content");

        Label version = new Label("_mapz launcher  •  prototype");
        version.getStyleClass().add("version");
        layout.setBottom(version);
        BorderPane.setAlignment(version, Pos.CENTER);
        BorderPane.setMargin(version, new Insets(0, 0, 24, 0));
        layout.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());
        return layout;
    }

    private void showLauncherPreview(StackPane root) {
        BorderPane current = (BorderPane) root.getChildren().get(1);
        root.getChildren().remove(current);

        Label name = new Label("_mapz");
        name.getStyleClass().add("profile-name");
        Label account = new Label("Microsoft account");
        account.getStyleClass().add("profile-subtitle");

        StackPane skin = createDemoSkin();
        VBox profile = new VBox(6, skin, name, account);
        profile.setAlignment(Pos.CENTER);

        Label heading = new Label("_mapz CLIENT");
        heading.getStyleClass().add("client-title");
        Label welcome = new Label("Ready when you are.");
        welcome.getStyleClass().add("client-subtitle");

        Button play = new Button("PLAY");
        play.getStyleClass().add("play-button");
        play.setOnAction(e -> { });

        VBox centerBox = new VBox(22, profile, heading, welcome);
        centerBox.setAlignment(Pos.CENTER);
        StackPane center = new StackPane(centerBox);
        center.setAlignment(Pos.CENTER);
        StackPane.setMargin(centerBox, new Insets(0, 0, 110, 0));

        BorderPane layout = new BorderPane(center);
        layout.getStyleClass().add("root-content");
        layout.getStylesheets().add(getClass().getResource("/launcher.css").toExternalForm());

        StackPane playHolder = new StackPane(play);
        playHolder.setAlignment(Pos.CENTER);
        BorderPane.setMargin(playHolder, new Insets(0, 0, 35, 0));
        layout.setBottom(playHolder);

        Label status = new Label("1.21.x  •  Vanilla");
        status.getStyleClass().add("version-pill");
        layout.setTop(status);
        BorderPane.setAlignment(status, Pos.TOP_RIGHT);
        BorderPane.setMargin(status, new Insets(28, 32, 0, 0));

        root.getChildren().add(layout);
        layout.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(450), layout);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private StackPane createDemoSkin() {
        StackPane avatar = new StackPane();
        avatar.setPrefSize(112, 145);
        avatar.getStyleClass().add("avatar");

        Rectangle head = new Rectangle(58, 58, Color.web("#8b5a3c"));
        head.setTranslateY(-32);
        Rectangle hair = new Rectangle(62, 18, Color.web("#20170f"));
        hair.setTranslateY(-54);
        Rectangle body = new Rectangle(64, 55, Color.web("#2777d8"));
        body.setTranslateY(27);
        Rectangle leftArm = new Rectangle(17, 55, Color.web("#245fa7"));
        leftArm.setTranslateX(-42);
        leftArm.setTranslateY(27);
        Rectangle rightArm = new Rectangle(17, 55, Color.web("#245fa7"));
        rightArm.setTranslateX(42);
        rightArm.setTranslateY(27);
        Rectangle leftLeg = new Rectangle(25, 48, Color.web("#27334b"));
        leftLeg.setTranslateX(-16);
        leftLeg.setTranslateY(76);
        Rectangle rightLeg = new Rectangle(25, 48, Color.web("#27334b"));
        rightLeg.setTranslateX(16);
        rightLeg.setTranslateY(76);
        avatar.getChildren().addAll(leftLeg, rightLeg, leftArm, rightArm, body, head, hair);
        return avatar;
    }

    public static void main(String[] args) { launch(args); }

    private static final class Starfield extends Canvas {
        private final Star[] stars = new Star[260];
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
            GraphicsContext g = getGraphicsContext2D();
            g.setFill(Color.web("#04060b"));
            g.fillRect(0, 0, w, h);
            g.setFill(Color.web("#111a3b", 0.42));
            g.fillOval(w * 0.25, h * 0.05, w * 0.55, h * 0.75);
            g.setFill(Color.web("#17103b", 0.28));
            g.fillOval(w * 0.50, h * 0.10, w * 0.45, h * 0.72);
            for (Star s : stars) {
                s.y += dt * (0.004 + s.depth * 0.012);
                if (s.y > 1.03) s.y = -0.03;
                double size = 0.6 + s.depth * 1.7;
                double opacity = 0.18 + s.depth * 0.65;
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
