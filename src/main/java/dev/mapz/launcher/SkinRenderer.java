package dev.mapz.launcher;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class SkinRenderer extends Canvas {
    private Image skin;
    private boolean slim;

    SkinRenderer() {
        setWidth(260);
        setHeight(350);
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
                HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
                HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    Image loaded = new Image(new ByteArrayInputStream(response.body()));
                    javafx.application.Platform.runLater(() -> {
                        skin = loaded;
                        draw();
                    });
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
        g.setFill(Color.rgb(0, 0, 0, 0.28));
        g.fillOval(w * .18, h * .86, w * .64, h * .07);

        double scale = Math.min(w / 20.0, h / 27.0);
        double unit = scale;
        double head = 8 * unit;
        double bodyW = 8 * unit;
        double bodyH = 12 * unit;
        double armW = (slim ? 3 : 4) * unit;
        double legW = 4 * unit;
        double legH = 12 * unit;
        double totalW = armW + bodyW + armW;
        double x = (w - totalW) / 2.0;
        double headX = (w - head) / 2.0;
        double headY = h * .05;
        double bodyY = headY + head + unit;
        double legY = bodyY + bodyH;

        if (skin == null) {
            g.setFill(Color.web("#11151d"));
            g.fillRoundRect(headX, headY, head, head, unit * .5, unit * .5);
            g.setFill(Color.web("#242b38"));
            g.fillRect(x + armW, bodyY, bodyW, bodyH);
            g.fillRect(x, bodyY, armW, bodyH);
            g.fillRect(x + armW + bodyW, bodyY, armW, bodyH);
            g.fillRect(x + armW, legY, legW, legH);
            g.fillRect(x + armW + legW, legY, legW, legH);
            return;
        }

        // Front-facing Minecraft avatar, using the real 64x64 skin texture.
        drawPart(g, 8, 8, 8, 8, headX, headY, head, head);
        drawPart(g, 20, 20, 8, 12, x + armW, bodyY, bodyW, bodyH);

        if (slim) {
            drawPart(g, 44, 20, 3, 12, x + totalW - armW, bodyY, armW, bodyH);
            drawPart(g, 36, 52, 3, 12, x, bodyY, armW, bodyH);
        } else {
            drawPart(g, 44, 20, 4, 12, x + totalW - armW, bodyY, armW, bodyH);
            drawPart(g, 36, 52, 4, 12, x, bodyY, armW, bodyH);
        }

        drawPart(g, 4, 20, 4, 12, x + armW, legY, legW, legH);
        drawPart(g, 20, 52, 4, 12, x + armW + legW, legY, legW, legH);

        // Subtle highlights make the model read cleanly without a busy background.
        g.setStroke(Color.rgb(255, 255, 255, .08));
        g.strokeRoundRect(headX, headY, head, head, unit * .35, unit * .35);
    }

    private void drawPart(GraphicsContext g, int sx, int sy, int sw, int sh, double dx, double dy, double dw, double dh) {
        g.drawImage(skin, sx, sy, sw, sh, dx, dy, dw, dh);
    }
}
