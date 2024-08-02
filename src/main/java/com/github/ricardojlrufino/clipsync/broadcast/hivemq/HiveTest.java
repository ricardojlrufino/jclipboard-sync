package com.github.ricardojlrufino.clipsync.broadcast.hivemq;

import com.github.ricardojlrufino.clipsync.AppConfig;
import com.github.ricardojlrufino.clipsync.Main;
import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.MqttProxyConfig;
import com.hivemq.client.mqtt.MqttProxyProtocol;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;

import java.io.IOException;
import java.util.UUID;

public class HiveTest {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        AppConfig appConfig = Main.loadConfig();
        Main.configureProxy(appConfig);
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
                //.serverAddress(InetSocketAddress.createUnresolved("185.213.2.121", 443))
                .serverHost("mqtt.flespi.io")
                .serverPort(80)
                .useMqttVersion5()
                .transportConfig().proxyConfig(mqttProxyConfig).applyTransportConfig()
                .buildBlocking();

        Mqtt5ConnAck connAck = client
                .connectWith()
                .cleanStart(true)

                .simpleAuth().username(mqtt.getUsername()).applySimpleAuth()
                .send();

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
