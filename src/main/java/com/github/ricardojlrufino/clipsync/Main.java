package com.github.ricardojlrufino.clipsync;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.github.ricardojlrufino.clipsync.broadcast.NodePipeMqttBroadcaster;
import com.github.ricardojlrufino.clipsync.broadcast.hivemq.HiveMqttBroadcaster;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttBroadcaster;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;
import com.github.ricardojlrufino.clipsync.clipboard.ClipboardHandler;
import com.github.ricardojlrufino.clipsync.utils.ProxyConfig;

public class Main {

    private static final String IMPL_MQTT = "mqtt";
    private static final String IMPL_MQTT_PROXY = "mqtt-with-proxy";
    private static final String IMPL_NODE_MQTT = "node-mqtt";

    public static void main(String[] args) throws Exception {

        AppConfig config = loadConfig();
        configureLogs(config);

        String http_proxy = System.getenv("http_proxy");
        if(http_proxy != null){
            System.out.println("Using proxy system variable !");
            ProxyConfig.configureProxyEnv();
        } 


        // try {
        //     System.out.println("AppTry start");
        //     AppSystemTry.main(args);
        //     AppTry2.main(args);
        //     System.out.println("AppTryEnd");
        // }catch (Exception ex){
        //     ex.printStackTrace();
        // }

        SecretKeySpec key = new SecretKeySpec((config.getSecretKey()).getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipheriIn = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipheriIn.init(Cipher.DECRYPT_MODE, key);

        Cipher cipherOut = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipherOut.init(Cipher.ENCRYPT_MODE, key);

        MqttConfig mqtt = config.getMqtt();
    
        ClipboardHandler clipboardHandler = new ClipboardHandler(config);
        clipboardHandler.start();

        if(config.getImplementation().equals(IMPL_MQTT)) {
            MqttBroadcaster broadcaster = new MqttBroadcaster(mqtt);
            broadcaster.setCipherIn(cipheriIn);
            broadcaster.setCipherOut(cipherOut);
            broadcaster.setClipboardHandler(clipboardHandler);
        } else if(config.getImplementation().equals(IMPL_NODE_MQTT)) {
            NodePipeMqttBroadcaster broadcaster = new NodePipeMqttBroadcaster(mqtt);
            broadcaster.setCipherIn(cipheriIn);
            broadcaster.setCipherOut(cipherOut);
            broadcaster.setClipboardHandler(clipboardHandler);
        } else if(config.getImplementation().equals(IMPL_MQTT_PROXY)) {
            HiveMqttBroadcaster broadcaster = new HiveMqttBroadcaster(mqtt);
            broadcaster.setCipherIn(cipheriIn);
            broadcaster.setCipherOut(cipherOut);
            broadcaster.setClipboardHandler(clipboardHandler);
        } else {
            throw new IllegalArgumentException("Unknown implementation: " + config.getImplementation());
        }

        // Main loop;
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
