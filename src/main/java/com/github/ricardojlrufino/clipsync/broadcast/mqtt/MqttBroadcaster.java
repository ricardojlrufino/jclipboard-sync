package com.github.ricardojlrufino.clipsync.broadcast.mqtt;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import com.github.ricardojlrufino.clipsync.broadcast.AbstractBroadcaster;
import com.github.ricardojlrufino.clipsync.clipboard.ClipboardType;

public class MqttBroadcaster extends AbstractBroadcaster implements MqttCallback {

    public static final Logger logger = Logger.getLogger(MqttBroadcaster.class.getName());

    private final MqttClient client;
    private final String topic;
    private final String targetTopic;
    private static final int MQTT_QOS = 1;

    public MqttBroadcaster(MqttConfig config) throws MqttException {
        this.client = new MqttClient(
                config.getServerURI(), // URI
                "clipboard-" + randonString(12), // ClientId
                new MemoryPersistence());

        this.topic = config.getTopic();
        this.targetTopic = config.getTargetTopic();
        this.connect(config);
    }

    private void connect(MqttConfig config) throws MqttException {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true); // disable persistence
        options.setKeepAliveInterval(30);
        options.setUserName(config.getUsername());
        if (config.getPassword() != null) {
            options.setPassword(config.getPassword().getBytes());
        }

        logger.info("Connecting to " + config.getServerURI());
        client.connect(options);

        if (client.isConnected()) {
            logger.info("Connected !");
            client.setCallback(this);
            client.subscribe(topic + "/+", MQTT_QOS);
        } else {
            logger.severe("Connection fail");
        }
    }

    @Override
    protected void broadcast(int type, byte[] data) {
        try {
            logger.fine("Sending clipboard data to: " + targetTopic + ", length: " + data.length + ", type: "
                    + ClipboardType.describe(type));
            client.publish(targetTopic, data, MQTT_QOS, false);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {
        logger.info("disconnected");
    }

    @Override
    public void mqttErrorOccurred(MqttException e) {
        logger.severe("mqttErrorOccurred: " + e);
    }

    @Override
    public void messageArrived(String topicSrc, MqttMessage mqttMessage) throws Exception {

        String clientID = topicSrc.substring(topic.length() + 1);
        byte[] payload = mqttMessage.getPayload();
        payload = super.decrypt(payload);

        logger.finest("messageArrived from " + topicSrc + ", size: " + payload.length + ", type: " + payload[0]);

        if (!clientID.equals(this.client.getClientId())) {

            updateClipboard(payload);

        } else {
            logger.fine("ignoring self messages, payload size: " + payload.length);

            // Allow debug using single client... self messages
            if (logger.isLoggable(Level.FINEST)) {
                updateClipboard(payload);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttToken iMqttToken) {
        logger.finest("deliveryComplete: " + iMqttToken.getMessageId());
    }

    @Override
    public void connectComplete(boolean b, String s) {
        logger.info("connectComplete: ");
    }

    @Override
    public void authPacketArrived(int i, MqttProperties mqttProperties) {
    }
}
