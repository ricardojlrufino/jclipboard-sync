package com.github.ricardojlrufino.clipsync.desktop;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.application.Application;
import javafx.stage.Stage;

public class AppTry2 extends Application {
    public static void main(String[] args) {

        if(! FXTrayIcon.isSupported()) throw new RuntimeException("FXTrayIcon not supported");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                launch(args);
            }
        });
        thread.start();
    }

    @Override
    public void start(Stage primaryStage) {
        FXTrayIcon icon = new FXTrayIcon(primaryStage, AppTry2.class.getResource("/app.png"));
        icon.show();
    }
}
