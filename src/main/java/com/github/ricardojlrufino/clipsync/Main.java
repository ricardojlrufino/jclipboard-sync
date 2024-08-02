package com.github.ricardojlrufino.clipsync;


import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttBroadcaster;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;
import com.github.ricardojlrufino.clipsync.clipboard.ClipboardHandler;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws Exception {

        AppConfig config = loadConfig();
        configureLogs(config);
        configureProxy(config);

        try {
            System.out.println("AppTry start");
            //AppSystemTry.main(args);
            //AppTry2.main(args);
            System.out.println("AppTryEnd");
        }catch (Exception ex){
            ex.printStackTrace();
        }

        SecretKeySpec key = new SecretKeySpec((config.getSecretKey()).getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipheriIn = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipheriIn.init(Cipher.DECRYPT_MODE, key);

        Cipher cipherOut = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipherOut.init(Cipher.ENCRYPT_MODE, key);

        MqttConfig mqtt = config.getMqtt();
        MqttBroadcaster broadcaster = new MqttBroadcaster(mqtt);
        broadcaster.setCipherIn(cipheriIn);
        broadcaster.setCipherOut(cipherOut);

        ClipboardHandler clipboardHandler = new ClipboardHandler(config);
        clipboardHandler.start();
        broadcaster.setClipboardHandler(clipboardHandler);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(200);
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shutting down ...");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        });


    }

    public static void configureProxy(AppConfig config) {

        String http_proxy = System.getenv("http_proxy");
        if(http_proxy == null) return;

        URI uri = URI.create(http_proxy);
        String proxyHost = uri.getHost();
        int proxyPort = uri.getPort();
        String proxyUser = uri.getUserInfo();
        String proxyPassword = "";
        if(proxyUser != null){
            String[] split = proxyUser.split(":");
            proxyUser = split[0];
            proxyPassword = split[1];
        }

        System.out.println(proxyHost);
        System.out.println(proxyPort);
        System.out.println(proxyUser);
        System.out.println(proxyPassword);

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", ""+proxyPort);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", ""+proxyPort);

        if(proxyPassword != null) System.setProperty("http.proxyPassword", proxyPassword);
        if(proxyUser != null) System.setProperty("http.proxyUser", proxyUser);

        if(proxyPassword != null) System.setProperty("https.proxyPassword", proxyPassword);
        if(proxyUser != null) System.setProperty("https.proxyUser", proxyUser);
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");



    }

    public static AppConfig loadConfig() throws IOException, ClassNotFoundException {
        Path configFile = Path.of(System.getProperty("user.home"), "jclipboard.properties");
        if(!configFile.toFile().exists()){
            AppConfig.createExample(configFile);
            throw new IllegalStateException("ERROR: Please create mqtt file: "+configFile);
        }
        return AppConfig.read(configFile.toString());
    }


    public static void configureLogs(AppConfig config){
        try {
            InputStream fis = Main.class.getResourceAsStream("/logging.properties");
            LogManager.getLogManager().readConfiguration(fis);

            Logger.getLogger(Main.class.getPackageName()).setLevel(Level.parse(config.getLogLevel()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
