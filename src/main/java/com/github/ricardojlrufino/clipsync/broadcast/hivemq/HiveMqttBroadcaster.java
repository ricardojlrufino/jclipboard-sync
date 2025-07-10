package com.github.ricardojlrufino.clipsync.broadcast.hivemq;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

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
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;

public class HiveMqttBroadcaster extends AbstractBroadcaster {

    public static final Logger logger = Logger.getLogger(MqttBroadcaster.class.getName());

    private MqttConfig config;
    private Mqtt5AsyncClient client;
    private String clientId;
    private String topic;
    private String targetTopic;

    private boolean useBase64 = true; // required

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
                .buildAsync();


         // 2. Connect to the broker
        client.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Connection failed: " + throwable.getMessage());
                    } else {
                        System.out.println("Connected to broker");

                        // 3. Subscribe to the topic
                        final CompletableFuture<Mqtt5SubAck> subAck = client.subscribeWith()
                                .topicFilter(topic)
                                .qos(MqttQos.AT_LEAST_ONCE) 
                                .send();

                        subAck.whenComplete((ack, t) -> {
                            if (t != null) {
                                System.err.println("Subscription failed: " + t.getMessage());
                            } else {
                                System.out.println("Subscribed to topic: " + topic);

                                client.publishes(MqttGlobalPublishFilter.ALL, (publish) -> {

                                    String topicSrc = publish.getTopic().toString();
                                    byte[] payload = publish.getPayloadAsBytes();
                                    if(useBase64){
                                        payload = Base64.getDecoder().decode(payload);
                                    }
                        
                                    payload = super.decrypt(payload);
                        
                                    logger.finest("messageArrived from " + topicSrc + ", size: " + payload.length + ", type: " + payload[0]);
                        
                                    updateClipboard(payload);
                        
                                });
                            }
                        });
                    }
                });


    }

    @Override
    protected void broadcast(int type, byte[] data) {

        if (client == null || !client.getState().isConnected()) {
            throw new IllegalStateException("Not connected to MQTT broker");
        }

        try {
            logger.fine("Sending clipboard data to: " + targetTopic + ", length: " + data.length + ", type: "
                    + ClipboardType.describe(type));
   
            if (useBase64) {
                String base64Data = Base64.getEncoder().encodeToString(data);
                data = base64Data.getBytes(StandardCharsets.UTF_8);
            }
            
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
