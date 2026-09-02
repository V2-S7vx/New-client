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
        widthProperty().addListener((obs,o,n)->draw());
        heightProperty().addListener((obs,o,n)->draw());
        draw();
    }

    void load(String url, boolean slim) {
        this.slim=slim;
        if(url==null||url.isBlank()){draw();return;}
        Thread.startVirtualThread(()->{
            try{
                HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(12)).GET().build();
                HttpResponse<byte[]> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofByteArray());
                if(response.statusCode()==200){
                    Image loaded=new Image(new ByteArrayInputStream(response.body()));
                    javafx.application.Platform.runLater(()->{skin=loaded;draw();});
                }
            }catch(Exception ignored){}
        });
    }

    private void draw(){
        double W=getWidth(),H=getHeight(); if(W<=0||H<=0)return;
        GraphicsContext g=getGraphicsContext2D(); g.clearRect(0,0,W,H); g.setImageSmoothing(false);
        if(skin==null){drawPlaceholder(g,W,H);return;}

        double u=Math.min(W/52.0,H/62.0);
        double cx=W/2.0;
        double top=H*.055;
        double depth=3.0*u;
        double slant=1.15*u;
        double head=8*u;
        double bodyW=8*u, bodyH=12*u;
        double armW=(slim?3:4)*u, armH=12*u;
        double legW=4*u, legH=12*u;
        double bodyX=cx-bodyW/2.0;
        double bodyY=top+head+2*u;
        double legY=bodyY+bodyH;

        // Legs.
        cuboid(g,20,52,4,12,20,48,4,4, bodyX+bodyW/2+0.3*u,legY,legW,legH,depth*.72,slant);
        cuboid(g,4,20,4,12,4,16,4,4, bodyX+0.2*u,legY,legW,legH,depth*.72,slant);
        // Arms.
        if(slim){
            cuboid(g,36,52,3,12,36,48,3,4, bodyX-armW-1.2*u,bodyY+.2*u,armW,armH,depth*.70,slant);
            cuboid(g,44,20,3,12,44,16,3,4, bodyX+bodyW+1.2*u,bodyY,armW,armH,depth*.70,slant);
        }else{
            cuboid(g,36,52,4,12,36,48,4,4, bodyX-armW-1.2*u,bodyY+.2*u,armW,armH,depth*.70,slant);
            cuboid(g,44,20,4,12,44,16,4,4, bodyX+bodyW+1.2*u,bodyY,armW,armH,depth*.70,slant);
        }
        // Body.
        cuboid(g,20,20,8,12,20,16,8,4,bodyX,bodyY,bodyW,bodyH,depth,slant);
        // Head on top, visibly turned toward the right.
        cuboid(g,16,8,8,8,8,0,8,8,cx-head/2,top,head,head,depth*1.2,slant*1.15);

        g.setFill(Color.rgb(70,130,255,.07));
        g.fillOval(cx-16*u,H*.895,32*u,2.2*u);
    }

    private void cuboid(GraphicsContext g,int sideX,int sideY,int sideW,int sideH,
                        int topX,int topY,int topW,int topH,double x,double y,
                        double w,double h,double d,double slant){
        // Right side recedes behind the front face, creating the three-quarter turn.
        drawFace(g,sideX,sideY,sideW,sideH,x+w,y,d,-slant,d,h);
        // Top plane recedes up/right.
        drawFace(g,topX,topY,topW,topH,x,y,w*.0,-slant,w,0);
        // Front face is kept perfectly pixel-sharp.
        int fx,fy,fw,fh;
        if(topW==8&&topH==8){fx=8;fy=8;fw=8;fh=8;}
        else if(w>5){fx=20;fy=20;fw=8;fh=12;}
        else if(sideX==36){fx=36;fy=52;fw=sideW;fh=sideH;}
        else if(sideX==44){fx=44;fy=20;fw=sideW;fh=sideH;}
        else if(sideX==4){fx=4;fy=20;fw=4;fh=12;}
        else {fx=20;fy=52;fw=4;fh=12;}
        drawFace(g,fx,fy,fw,fh,x,y,w,0,0,h);
    }

    private void drawFace(GraphicsContext g,int sx,int sy,int sw,int sh,double x,double y,
                           double ux,double uy,double vx,double vy){
        if(sw<=0||sh<=0)return;
        g.save();
        Affine a=new Affine(ux/sw,uy/sw,vx/sh,vy/sh,x,y);
        g.setTransform(a.getMxx(),a.getMyx(),a.getMxy(),a.getMyy(),a.getTx(),a.getTy());
        g.drawImage(skin,sx,sy,sw,sh,0,0,sw,sh);
        g.restore();
    }

    private void drawPlaceholder(GraphicsContext g,double W,double H){
        double u=Math.min(W/52.0,H/62.0),x=W/2-4*u,t=H*.055;
        g.setFill(Color.web("#151a23"));g.fillRect(x,t,8*u,8*u);
        g.setFill(Color.web("#242b38"));g.fillRect(x,t+10*u,8*u,12*u);
        g.fillRect(x-4*u,t+10*u,4*u,12*u);g.fillRect(x+8*u,t+10*u,4*u,12*u);
        g.fillRect(x,t+22*u,4*u,12*u);g.fillRect(x+4*u,t+22*u,4*u,12*u);
    }
}
