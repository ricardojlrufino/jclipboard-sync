package com.github.ricardojlrufino.clipsync;

import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;
import com.github.ricardojlrufino.clipsync.utils.PropertiesConfig;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig implements Serializable {

    private MqttConfig mqtt;

    private String secretKey;

    private boolean enableFiles;

    private int fileSizeMB;

    private String logLevel = "INFO";

    public void setMqtt(MqttConfig mqtt) {
        this.mqtt = mqtt;
    }
    public MqttConfig getMqtt() {
        return mqtt;
    }

    public boolean isEnableFiles() {
        return enableFiles;
    }

    public void setEnableFiles(boolean enableFiles) {
        this.enableFiles = enableFiles;
    }

    public int getFileSizeMB() {
        return fileSizeMB;
    }

    public void setFileSizeMB(int fileSizeMB) {
        this.fileSizeMB = fileSizeMB;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public static void createExample(Path configFile) throws IOException, ClassNotFoundException {
        AppConfig config = new AppConfig();
        config.setSecretKey("CHANGE_MY_SECRETS");
        config.setEnableFiles(false);
        config.setFileSizeMB(2);

        MqttConfig mqttConfig = new MqttConfig();
        config.setMqtt(mqttConfig);
        mqttConfig.setServerURI("wss://mqtt.flespi.io");
        mqttConfig.setUsername("CHANGE");
        mqttConfig.setPassword("");
        mqttConfig.setTopic("messages/XXXXXX");

        config.save(configFile+".example");
    }


    public static AppConfig read(String file) throws IOException, ClassNotFoundException {
        PropertiesConfig properties = new PropertiesConfig();
        properties.load(new FileInputStream(file));
        AppConfig config = new AppConfig();

        config.setEnableFiles(Boolean.parseBoolean(properties.getAsRequired("EnableFiles")));
        config.setFileSizeMB(Integer.parseInt(properties.getAsRequired("FileSizeMB")));
        config.setSecretKey(properties.getAsRequired("SecretKey"));
        config.setLogLevel(properties.getAsRequired("LogLevel"));


        boolean mqtt = Boolean.parseBoolean(properties.getAsRequired("mqtt.Enabled"));

        if(mqtt){
            MqttConfig mqttConfig = new MqttConfig();
            mqttConfig.setServerURI(properties.getAsRequired("mqtt.ServerURI"));
            mqttConfig.setUsername(properties.getAsRequired("mqtt.Username"));
            mqttConfig.setPassword(properties.getAsRequired("mqtt.Password"));
            mqttConfig.setTopic(properties.getAsRequired("mqtt.Topic"));
            config.setMqtt(mqttConfig);
        }

        return config;
    }

    public void save(String file) throws IOException, ClassNotFoundException {
        Properties properties = new Properties();

        properties.setProperty("EnableFiles", ""+this.enableFiles);
        properties.setProperty("FileSizeMB", ""+this.fileSizeMB);
        properties.setProperty("SecretKey", this.secretKey);
        properties.setProperty("LogLevel", this.logLevel);

        properties.setProperty("mqtt.Enabled", ""+true);
        properties.setProperty("mqtt.ServerURI", getMqtt().getServerURI());
        properties.setProperty("mqtt.Username", getMqtt().getUsername());
        properties.setProperty("mqtt.Password", getMqtt().getPassword());
        properties.setProperty("mqtt.Topic", getMqtt().getTopic());

        OutputStream output = new FileOutputStream(file);
        properties.store(output, null);
        output.close();

    }
}
