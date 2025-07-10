package com.github.ricardojlrufino.clipsync.broadcast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Base64;
import java.util.logging.Logger;

import com.github.ricardojlrufino.clipsync.broadcast.mqtt.MqttConfig;

public class NodePipeMqttBroadcaster extends AbstractBroadcaster {
    
    public static final Logger logger = Logger.getLogger(NodePipeMqttBroadcaster.class.getName());

    private Process nodeProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private MqttConfig mqtt;

    public NodePipeMqttBroadcaster(MqttConfig mqtt) {
        this.mqtt = mqtt;
        startNodeProcess();
    }

    private void startNodeProcess() {
        try {
            String mqttJson = String.format("{\"broker\":\"%s\",\"topic\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}",
                mqtt.getServerURI(), mqtt.getTopic(), mqtt.getUsername(), mqtt.getPassword());
            
            ProcessBuilder builder = new ProcessBuilder("node", "sync.js", mqttJson);
            builder.directory(new File("node"));
            builder.redirectErrorStream(true); // Redirecionar stderr para stdout
            nodeProcess = builder.start();
            
            processInput = new BufferedWriter(new OutputStreamWriter(nodeProcess.getOutputStream()));
            processOutput = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()));
            
            logger.fine("Node.js process started with PID: " + nodeProcess.pid());

            // Start reading output in background
            new Thread(this::readProcessOutput).start();

            // Verificar se o processo iniciou corretamente
            if (!nodeProcess.isAlive()) {
                throw new RuntimeException("Node.js process failed to start");
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to start Node.js process", e);
        }
    }

    private void readProcessOutput() {
        try {
            String line;
            while (nodeProcess.isAlive() && (line = processOutput.readLine()) != null) {
                
                try {
                    if (line.startsWith("DATA:")) {
                        String base64Data = line.substring(5);
                        byte[] data = Base64.getDecoder().decode(base64Data);
                        byte[] decrypted = decrypt(data);
                        logger.finest("messageArrived size: " + data.length + ", type: " + data[0]);
                        updateClipboard(decrypted);
                    } else {
                        System.out.println("Node.js output: " + line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            }
            
            // Se chegou aqui, o processo morreu
            if (!nodeProcess.isAlive()) {
                logger.warning("Node.js process died unexpectedly. Exit code: " + nodeProcess.exitValue());
                startNodeProcess();
            }
        } catch (IOException e) {
            logger.warning("Error reading from Node.js process: " + e.getMessage());
            startNodeProcess();
        }
    }

    @Override
    protected void broadcast(int type, byte[] data) {
        // Verificar se o processo está vivo
        if (nodeProcess == null || !nodeProcess.isAlive()) {
            logger.warning("Node.js process is not running. Attempting to restart...");
            startNodeProcess();
        }

        logger.finest("Broadcasting data to Node.js process: " + nodeProcess.pid());

        try {
            String base64Data = Base64.getEncoder().encodeToString(data);
            processInput.write("DATA:" + base64Data + "\n");
            processInput.flush();
        } catch (IOException e) {
            System.err.println("Failed to send data to Node.js process. Restarting...");
            startNodeProcess();
            // Tentar enviar novamente após reiniciar
            broadcast(type, data);
        }
    }

    public void stop() {
        try {
            processInput.close();
            processOutput.close();
            nodeProcess.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
