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
        setWidth(300);
        setHeight(330);
        widthProperty().addListener((obs, oldV, newV) -> draw());
        heightProperty().addListener((obs, oldV, newV) -> draw());
        drawPlaceholder();
    }

    void load(String url, boolean slim) {
        this.slim = slim;
        if (url == null || url.isBlank()) { drawPlaceholder(); return; }
        Thread.startVirtualThread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(12)).GET().build();
                HttpResponse<byte[]> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200) {
                    Image loaded = new Image(new ByteArrayInputStream(response.body()));
                    PlatformBridge.run(() -> { skin = loaded; draw(); });
                } else PlatformBridge.run(this::drawPlaceholder);
            } catch (Exception ignored) { PlatformBridge.run(this::drawPlaceholder); }
        });
    }

    private void drawPlaceholder() { skin = null; draw(); }

    private void draw() {
        double w=getWidth(), h=getHeight(); if(w<=0||h<=0)return;
        GraphicsContext g=getGraphicsContext2D(); g.clearRect(0,0,w,h); g.setImageSmoothing(false);
        g.setFill(Color.rgb(0,0,0,0.34)); g.fillOval(w*.22,h*.875,w*.56,h*.045);
        if(skin==null){ drawPlaceholderModel(g,w,h); return; }

        // Pixel-perfect Minecraft skin faces arranged as a compact three-quarter model.
        double u=Math.min(w/52.0,h/62.0);
        double depth=2.8*u;
        double skew=1.15*u;
        double cx=w/2.0-2*u;
        double head=8*u;
        double bodyW=8*u, bodyH=12*u;
        double limbW=(slim?3:4)*u, limbH=12*u;
        double top=h*.07;
        double bodyY=top+head+1.8*u;
        double legY=bodyY+bodyH;

        // Back-to-front layering makes the right-facing pose read clearly.
        cuboid(g, 16,20,4,12, 20,16,8,4, cx+bodyW,bodyY, depth, bodyH, skew);
        cuboid(g, 0,20,4,12, 4,16,4,4, cx-limbW-1.0*u,legY, depth*.82,limbH, skew);
        cuboid(g, 16,52,4,12, 20,48,4,4, cx+0.3*u,legY, depth*.82,limbH, skew);
        cuboid(g, 40,20,4,12, 44,16,4,4, cx-limbW-1.1*u,bodyY+.3*u, depth*.72,limbH, skew);
        cuboid(g, 40,52,4,12, 44,48,4,4, cx+bodyW+.1*u,bodyY, depth*.72,limbH, skew);
        cuboid(g, 16,8,8,8, 8,0,8,8, cx,top,depth*1.18,head,skew*1.1);

        // A subtle ground glow anchors the avatar without adding a card/background.
        g.setFill(Color.rgb(60,120,255,0.08));
        g.fillOval(cx-16*u,h*.91,32*u,2.5*u);
    }

    private void cuboid(GraphicsContext g, int sideX,int sideY,int sideW,int sideH,
                         int topX,int topY,int topW,int topH,
                         double x,double y,double d,double height,double skew) {
        int frontX,frontY,frontW,frontH;
        if (sideW==4 && sideH==12 && topW==4) {
            // The caller's top/side coordinates identify arms or legs; infer front from the paired texture block.
            frontX = (sideX==40?44:(sideX==0?4:(sideX==16?20:(sideX==16?20:36))));
            if(sideX==40 && sideY==52) frontX=36;
            if(sideX==16 && sideY==20) frontX=4;
            frontY=sideY; frontW=4; frontH=12;
        } else { frontX=20; frontY=20; frontW=8; frontH=12; }
        if(topW==8 && topH==8){ frontX=8; frontY=8; frontW=8; frontH=8; }

        // right side
        drawFace(g,sideX,sideY,sideW,sideH,x+frontW*0.0/1.0*uScale(frontW),y,d,skew,d,height);
        // top
        drawFace(g,topX,topY,topW,topH,x,y,d,-skew,d*.72,0);
        // front
        drawFace(g,frontX,frontY,frontW,frontH,x,y,frontW*uScale(frontW),0,0,height);
    }

    private double uScale(int pixels){ return pixels==8 ? 1.0 : 1.0; }

    private void drawFace(GraphicsContext g,int sx,int sy,int sw,int sh,double x,double y,
                           double ux,double uy,double vx,double vy){
        if(sw<=0||sh<=0)return;
        g.save();
        Affine a=new Affine(ux/sw,uy/sw,vx/sh,vy/sh,x,y);
        g.setTransform(a.getMxx(),a.getMyx(),a.getMxy(),a.getMyy(),a.getTx(),a.getTy());
        g.drawImage(skin,sx,sy,sw,sh,0,0,sw,sh);
        g.restore();
    }

    private void drawPlaceholderModel(GraphicsContext g,double w,double h){
        double u=Math.min(w/52.0,h/62.0), x=w/2.0-4*u, top=h*.07;
        g.setFill(Color.web("#151a23")); g.fillRect(x,top,8*u,8*u);
        g.setFill(Color.web("#242b38")); g.fillRect(x,top+10*u,8*u,12*u);
        g.fillRect(x-4*u,top+10*u,4*u,12*u); g.fillRect(x+8*u,top+10*u,4*u,12*u);
        g.fillRect(x,top+22*u,4*u,12*u); g.fillRect(x+4*u,top+22*u,4*u,12*u);
    }

    private static final class PlatformBridge {
        static void run(Runnable action){ javafx.application.Platform.runLater(action); }
    }
}
