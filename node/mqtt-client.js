const mqtt = require('mqtt');
const { HttpsProxyAgent } = require('https-proxy-agent');
// Importar variáveis de ambiente
const env = require('dotenv').config();

console.log(env);

const MQTT_BROKER = process.env.MQTT_BROKER_URL;

// Criar opções de conexão
const options = {
    keepalive: 30,
    protocolId: 'MQTT',
    protocolVersion: 5,
    username: process.env.MQTT_USERNAME || undefined,
    password: process.env.MQTT_PASSWORD || undefined,
    clean: true,
    reconnectPeriod: 1000,
    connectTimeout: 30000,
    rejectUnauthorized: false,
    debug: true, // Habilita logs de debug do MQTT
    enableTrace: true  // Habilita rastreamento detalhado
};

// Adicionar configuração do proxy se necessário
if (process.env.HTTP_PROXY) {
    console.log('Usando proxy:', process.env.HTTP_PROXY);
    const proxyAgent = new HttpsProxyAgent(process.env.HTTP_PROXY);

    // Adicionar handlers de eventos do proxy
    proxyAgent.on('error', (err) => console.error('Erro no proxy:', err));
    proxyAgent.on('debug', (debug) => console.log('Debug proxy:', debug));

    options.wsOptions = { agent: proxyAgent };
}

// Criar cliente
console.log('Usando MQTT_BROKER:', process.env.MQTT_BROKER_URL);
const client = mqtt.connect(MQTT_BROKER, options);

// Adicionar handler de debug
client.on('debug', (debug) => {
    console.log('Debug MQTT:', debug);
});

// Handlers de eventos
client.on('connect', () => {
    console.log('Conectado ao broker MQTT');
    
    // Inscrever em um tópico
    client.subscribe('test/topic', (err) => {
        if (!err) {
            console.log('Inscrito no tópico test/topic');
            // Publicar uma mensagem de teste
            client.publish('test/topic', 'Hello MQTT over WebSocket!');
        }
    });
});

client.on('message', (topic, message) => {
    console.log(`Mensagem recebida no tópico ${topic}: ${message.toString()}`);
});

client.on('error', (error) => {
    console.error('Erro de conexão:', error);
});

client.on('close', () => {
    console.log('Conexão fechada');
});
