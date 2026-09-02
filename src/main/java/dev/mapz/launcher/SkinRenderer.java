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
        setWidth(300); setHeight(330);
        widthProperty().addListener((obs,o,n)->draw());
        heightProperty().addListener((obs,o,n)->draw());
        draw();
    }

    void load(String url, boolean slim) {
        this.slim=slim;
        if(url==null||url.isBlank()){draw();return;}
        Thread.startVirtualThread(()->{
            try {
                HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(12)).GET().build();
                HttpResponse<byte[]> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofByteArray());
                if(response.statusCode()==200){
                    Image loaded=new Image(new ByteArrayInputStream(response.body()));
                    javafx.application.Platform.runLater(()->{skin=loaded;draw();});
                }
            } catch(Exception ignored) { }
        });
    }

    private void draw(){
        double W=getWidth(),H=getHeight(); if(W<=0||H<=0)return;
        GraphicsContext g=getGraphicsContext2D(); g.clearRect(0,0,W,H); g.setImageSmoothing(false);
        if(skin==null){drawPlaceholder(g,W,H);return;}
        double u=Math.min(W/52.0,H/62.0), cx=W/2.0, top=H*.055;
        double head=8*u, bodyW=8*u, bodyH=12*u, armW=(slim?3:4)*u, limbH=12*u;
        double depth=3.0*u, slant=1.15*u, bodyX=cx-bodyW/2, bodyY=top+head+2*u, legY=bodyY+bodyH;

        // Draw the far limbs first, then body, then near limbs and head.
        part(g,4,20,4,12,4,16,4,4,bodyX+.2*u,legY,4*u,limbH,depth*.72,slant);
        part(g,20,52,4,12,20,48,4,4,bodyX+bodyW/2+.3*u,legY,4*u,limbH,depth*.72,slant);
        part(g,36,52,slim?3:4,12,36,48,slim?3:4,4,bodyX-armW-1.2*u,bodyY+.2*u,armW,limbH,depth*.70,slant);
        part(g,44,20,slim?3:4,12,44,16,slim?3:4,4,bodyX+bodyW+1.2*u,bodyY,armW,limbH,depth*.70,slant);
        part(g,20,20,8,12,20,16,8,4,bodyX,bodyY,bodyW,bodyH,depth,slant);
        part(g,16,8,8,8,8,0,8,8,cx-head/2,top,head,head,depth*1.2,slant*1.15);

        g.setFill(Color.rgb(70,130,255,.07)); g.fillOval(cx-16*u,H*.895,32*u,2.2*u);
    }

    private void part(GraphicsContext g,int sideX,int sideY,int sideW,int sideH,
                      int topX,int topY,int topW,int topH,double x,double y,
                      double w,double h,double d,double slant){
        int fx,fy,fw,fh;
        if(topW==8&&topH==8){fx=8;fy=8;fw=8;fh=8;}
        else if(w>5*uPlaceholder()){fx=20;fy=20;fw=8;fh=12;}
        else if(sideX==36){fx=36;fy=52;fw=sideW;fh=sideH;}
        else if(sideX==44){fx=44;fy=20;fw=sideW;fh=sideH;}
        else if(sideX==4){fx=4;fy=20;fw=4;fh=12;}
        else {fx=20;fy=52;fw=4;fh=12;}
        drawFace(g,sideX,sideY,sideW,sideH,x+w,y,d,-slant,0,h);
        drawFace(g,topX,topY,topW,topH,x,y,w,0,0,-slant);
        drawFace(g,fx,fy,fw,fh,x,y,w,0,0,h);
    }

    private double uPlaceholder(){ return 5; }
    private void drawFace(GraphicsContext g,int sx,int sy,int sw,int sh,double x,double y,double ux,double uy,double vx,double vy){
        if(sw<=0||sh<=0)return; g.save();
        Affine a=new Affine(ux/sw,uy/sw,vx/sh,vy/sh,x,y);
        g.setTransform(a.getMxx(),a.getMyx(),a.getMxy(),a.getMyy(),a.getTx(),a.getTy());
        g.drawImage(skin,sx,sy,sw,sh,0,0,sw,sh); g.restore();
    }

    private void drawPlaceholder(GraphicsContext g,double W,double H){
        double u=Math.min(W/52.0,H/62.0),x=W/2-4*u,t=H*.055;
        g.setFill(Color.web("#151a23"));g.fillRect(x,t,8*u,8*u);
        g.setFill(Color.web("#242b38"));g.fillRect(x,t+10*u,8*u,12*u);
        g.fillRect(x-4*u,t+10*u,4*u,12*u);g.fillRect(x+8*u,t+10*u,4*u,12*u);
        g.fillRect(x,t+22*u,4*u,12*u);g.fillRect(x+4*u,t+22*u,4*u,12*u);
    }
}
