package com.github.ricardojlrufino.clipsync.broadcast.hivemq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import com.github.ricardojlrufino.clipsync.AppConfig;
import com.github.ricardojlrufino.clipsync.Main;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;
import com.github.ricardojlrufino.clipsync.utils.ProxyConfig;
import com.github.ricardojlrufino.clipsync.utils.TrustAllCertificatesTrustManagerFactory;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.MqttProxyConfig;
import com.hivemq.client.mqtt.MqttProxyProtocol;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;

public class HiveTest {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        AppConfig appConfig = Main.loadConfig();
        String http_proxy = System.getenv("http_proxy");
        if(http_proxy != null){
            System.out.println("Using proxy system variable !");
            ProxyConfig.configureProxyEnv();
        } 

        MqttConfig mqtt = appConfig.getMqtt();

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

        Mqtt5BlockingClient client = MqttClient.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost("mqtt.flespi.io")
                .serverPort(443)
                .useMqttVersion5()
                .sslConfig()
                    .trustManagerFactory(new TrustAllCertificatesTrustManagerFactory())  // Use a custom trust manager factory
                    .applySslConfig()
                .webSocketConfig()
                    .serverPath("/")
                    .applyWebSocketConfig()
                .simpleAuth()
                    .username(mqtt.getUsername())
                    .password(mqtt.getPassword().getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                .transportConfig()
                    .proxyConfig(mqttProxyConfig)
                    .applyTransportConfig()
                .buildBlocking();

        Mqtt5ConnAck connAck = client.connect();

        System.out.println("connAck: " + connAck);

        try (final Mqtt5BlockingClient.Mqtt5Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL)) {

            client.subscribeWith().topicFilter("test/topic").qos(MqttQos.AT_LEAST_ONCE).send();

            //publishes.receive(1, TimeUnit.SECONDS).ifPresent(System.out::println);
           // publishes.receive(100, TimeUnit.MILLISECONDS).ifPresent(System.out::println);

        } finally {
            client.disconnect();
        }

    }
}
