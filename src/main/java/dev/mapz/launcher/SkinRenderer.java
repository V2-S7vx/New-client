package dev.mapz.launcher;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class SkinRenderer extends Canvas {
    private Image skin;
    private boolean slim;

    SkinRenderer() {
        setWidth(230);
        setHeight(305);
        widthProperty().addListener((obs, oldV, newV) -> draw());
        heightProperty().addListener((obs, oldV, newV) -> draw());
        drawPlaceholder();
    }

    void load(String url, boolean slim) {
        this.slim = slim;
        if (url == null || url.isBlank()) {
            drawPlaceholder();
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(12))
                        .GET().build();
                HttpResponse<byte[]> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    Image loaded = new Image(new ByteArrayInputStream(response.body()));
                    javafx.application.Platform.runLater(() -> {
                        skin = loaded;
                        draw();
                    });
                } else {
                    javafx.application.Platform.runLater(this::drawPlaceholder);
                }
            } catch (Exception ignored) {
                javafx.application.Platform.runLater(this::drawPlaceholder);
            }
        });
    }

    private void drawPlaceholder() {
        skin = null;
        draw();
    }

    private void draw() {
        double w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setImageSmoothing(false);

        // Ground shadow.
        g.setFill(Color.rgb(0, 0, 0, 0.38));
        g.fillOval(w * .17, h * .87, w * .66, h * .055);

        if (skin == null) {
            drawPlaceholderModel(g, w, h);
            return;
        }

        // Small, centered three-quarter Minecraft avatar.
        double unit = Math.min(w / 25.0, h / 36.0);
        double cube = 8 * unit;
        double bodyW = 8 * unit;
        double bodyH = 12 * unit;
        double limbW = (slim ? 3 : 4) * unit;
        double limbH = 12 * unit;
        double depth = 2.1 * unit;
        double x = w / 2.0;
        double top = h * .075;

        // Legs first, then body, arms, and head for natural overlap.
        double legY = top + cube + 1.2 * unit + bodyH;
        drawCuboid(g, 4, 20, 4, 12, 8, 12, x - 4.05 * unit, legY, 4 * unit, limbH, depth);
        drawCuboid(g, 20, 52, 4, 12, 8, 12, x + .05 * unit, legY, 4 * unit, limbH, depth);

        double bodyY = top + cube + 1.2 * unit;
        drawCuboid(g, 20, 20, 8, 12, 8, 12, x - 4 * unit, bodyY, bodyW, bodyH, depth);

        double armY = bodyY + .1 * unit;
        drawCuboid(g, 36, 52, slim ? 3 : 4, 12, slim ? 3 : 4, 12,
                x - 7.95 * unit, armY + .1 * unit, limbW, limbH, depth);
        drawCuboid(g, 44, 20, slim ? 3 : 4, 12, slim ? 3 : 4, 12,
                x + 4.05 * unit, armY, limbW, limbH, depth);

        drawCuboid(g, 8, 8, 8, 8, 8, 8, x - 4 * unit, top, cube, cube, depth * 1.25);
    }

    private void drawPlaceholderModel(GraphicsContext g, double w, double h) {
        double unit = Math.min(w / 25.0, h / 36.0);
        double x = w / 2.0;
        double top = h * .075;
        double cube = 8 * unit;
        double bodyY = top + cube + unit;
        double bodyH = 12 * unit;
        double legY = bodyY + bodyH;

        g.setFill(Color.web("#151a23"));
        g.fillRoundRect(x - 4 * unit, top, cube, cube, unit, unit);
        g.setFill(Color.web("#242b38"));
        g.fillRect(x - 4 * unit, bodyY, 8 * unit, bodyH);
        g.fillRect(x - 8 * unit, bodyY, 4 * unit, bodyH);
        g.fillRect(x + 4 * unit, bodyY, 4 * unit, bodyH);
        g.fillRect(x - 4 * unit, legY, 4 * unit, 12 * unit);
        g.fillRect(x, legY, 4 * unit, 12 * unit);
    }

    /** Draw a textured cuboid using three affine-mapped skin faces. */
    private void drawCuboid(GraphicsContext g, int frontX, int frontY, int frontW, int frontH,
                            int texW, int texH, double x, double y, double w, double h, double depth) {
        double side = depth;
        double up = depth * .52;

        // Right side: gives the model its 3D turn.
        drawFace(g, frontX + frontW, frontY, sideTextureWidth(frontW), frontH,
                x + w, y, side, -up, side, h * 0.0 + 0.0);

        // Top face.
        drawFace(g, frontX, frontY - Math.max(1, frontH == 8 ? 8 : 4), frontW,
                Math.max(1, frontH == 8 ? 8 : 4),
                x, y, w, 0, -side, -up);

        // Front face last so the silhouette stays crisp.
        drawFace(g, frontX, frontY, frontW, frontH,
                x, y, w, 0, 0, h);
    }

    private int sideTextureWidth(int frontW) {
        return frontW == 8 ? 8 : frontW;
    }

    private void drawFace(GraphicsContext g, int sx, int sy, int sw, int sh,
                           double x, double y, double ux, double uy, double vx, double vy) {
        if (sw <= 0 || sh <= 0) return;
        g.save();
        Affine transform = new Affine(
                ux / sw, uy / sw,
                vx / sh, vy / sh,
                x, y
        );
        g.setTransform(transform.getMxx(), transform.getMyx(), transform.getMxy(),
                transform.getMyy(), transform.getTx(), transform.getTy());
        g.drawImage(skin, sx, sy, sw, sh, 0, 0, sw, sh);
        g.restore();
    }
}
