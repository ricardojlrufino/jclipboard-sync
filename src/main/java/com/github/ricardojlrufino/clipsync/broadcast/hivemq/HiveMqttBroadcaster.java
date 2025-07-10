package com.github.ricardojlrufino.clipsync.broadcast.hivemq;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.common.MqttException;

import com.github.ricardojlrufino.clipsync.broadcast.AbstractBroadcaster;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttBroadcaster;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;
import com.github.ricardojlrufino.clipsync.clipboard.ClipboardType;
import com.github.ricardojlrufino.clipsync.utils.TrustAllCertificatesTrustManagerFactory;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.MqttProxyConfig;
import com.hivemq.client.mqtt.MqttProxyProtocol;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;

public class HiveMqttBroadcaster extends AbstractBroadcaster {

    public static final Logger logger = Logger.getLogger(MqttBroadcaster.class.getName());

    private MqttConfig config;
    private Mqtt5BlockingClient client;
    private String clientId;
    private String topic;
    private String targetTopic;

    public HiveMqttBroadcaster(MqttConfig config) {
        this.config = config;
        this.clientId = UUID.randomUUID().toString();
        this.topic = config.getTopic();
        this.targetTopic = config.getTargetTopic();
        this.connect(config);
    }

    private void connect(MqttConfig config)  {

        int port = Integer.parseInt(System.getProperty("http.proxyPort"));
        String host = System.getProperty("http.proxyHost");
        String user = System.getProperty("http.proxyUser");
        String proxyPassword = System.getProperty("https.proxyPassword");

        MqttProxyConfig mqttProxyConfig = MqttProxyConfig.builder()
                .username(user)
                .password(proxyPassword)
                .host(host)
                .port(port)
                .protocol(MqttProxyProtocol.HTTP)  
                .build();

        client = MqttClient.builder()
                .identifier(clientId)
                .serverHost(config.getServerURI())
                .serverPort(443)
                .useMqttVersion5()
                .sslConfig()
                    .trustManagerFactory(new TrustAllCertificatesTrustManagerFactory())
                    .applySslConfig()
                .webSocketConfig()
                    .serverPath("/")
                    .applyWebSocketConfig()
                .simpleAuth()
                    .username(config.getUsername())
                    .password(config.getPassword().getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                .transportConfig()
                    .proxyConfig(mqttProxyConfig)
                    .applyTransportConfig()
                .buildBlocking();

         Mqtt5ConnAck connAck = client.connect();

        System.out.println("connAck: " + connAck);

        // Start message receiving
        client.toAsync().publishes(MqttGlobalPublishFilter.ALL, publish -> {
            if (!publish.getPayload().isPresent())
                return;

            byte[] payload = publish.getPayloadAsBytes();
            String topicSrc = publish.getTopic().toString();
            String messageClientID = topicSrc.substring(this.topic.length() + 1);

            payload = super.decrypt(payload);

            logger.finest("messageArrived from " + topicSrc + ", size: " + payload.length + ", type: " + payload[0]);

            if (!messageClientID.equals(this.clientId)) {

                updateClipboard(payload);

            } else {

                logger.fine("ignoring self messages, payload size: " + payload.length);

                // Allow debug using single client... self messages
                if (logger.isLoggable(Level.FINEST)) {
                    updateClipboard(payload);
                }
            }

        });

        client.subscribeWith()
                .topicFilter(config.getTopic()+ "/+")
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();
    }

    @Override
    protected void broadcast(int type, byte[] data) {

        if (client == null || !client.getState().isConnected()) {
            throw new IllegalStateException("Not connected to MQTT broker");
        }

        try {
            logger.fine("Sending clipboard data to: " + targetTopic + ", length: " + data.length + ", type: "
                    + ClipboardType.describe(type));
   
            client.publishWith()
                .topic(targetTopic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(data)
                .send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
