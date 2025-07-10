const readline = require('readline');
const mqtt = require('mqtt');
const { HttpsProxyAgent } = require('https-proxy-agent');

// Get MQTT config from command line argument
const mqttConfig = JSON.parse(process.argv[2]);

const clientID = mqttConfig.clientId || 'jclipboard-' + Math.random().toString(16).substr(2, 8);

// MQTT client setup
const options = {
    keepalive: 30,
    protocolId: 'MQTT',
    protocolVersion: 5,
    username: mqttConfig.username || undefined,
    password: mqttConfig.password || undefined,
    clean: true,
    clientId: clientID,
    reconnectPeriod: 1000,
    connectTimeout: 30000,
    rejectUnauthorized: false
};

if (process.env.HTTP_PROXY) {
    options.wsOptions = {
        agent: new HttpsProxyAgent(process.env.HTTP_PROXY)
    };
}

const client = mqtt.connect(mqttConfig.broker, options);

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

// MQTT connection handler
client.on('connect', () => {
    console.error('Connected to MQTT broker');
    client.subscribe(mqttConfig.topic + "/+", (err) => {
        if (err) console.error('Subscription error:', err);
    });
});

// Handle incoming MQTT messages
client.on('message', (topic, message) => {
    if (!topic.includes(clientID)) {
        console.error('Received message on topic:', topic);
        console.log('DATA:' + message.toString()); // send to JAVA
    }
});

client.on('error', (error) => {

    console.error('❌ Erro MQTT:', error.message);

});

client.on('close', () => {

    console.error('🔌 Conexão MQTT fechada');

});

client.on('reconnect', () => {

    console.error('🔄 Reconectando ao broker MQTT...');

});

client.on('offline', () => {

    console.error('📴 Cliente MQTT offline');

});

rl.on('line', (line) => {
    if (line.startsWith('DATA:')) {
        const data = line.substring(5);
        console.error('Publishing data:', data.length);
        client.publish(mqttConfig.topic + "/" + clientID, data);
    }
});

process.on('SIGTERM', () => {
    client.end();
    rl.close();
    process.exit(0);
});
