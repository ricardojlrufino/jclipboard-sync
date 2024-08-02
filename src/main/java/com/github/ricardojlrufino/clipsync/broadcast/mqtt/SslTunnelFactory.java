package com.github.ricardojlrufino.clipsync.broadcast.mqtt;

import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.logging.Logger;

public class SslTunnelFactory extends SSLSocketFactory {

    public static final Logger log = Logger.getLogger(SslTunnelFactory.class.getName());

    String tunnelHost;
    int tunnelPort;
    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

    public SslTunnelFactory() {
        tunnelHost = System.getProperty("http.proxyHost");
        tunnelPort = Integer.parseInt(System.getProperty("http.proxyPort"));
    }

    @Override
    public Socket createSocket(Socket socket, InputStream inputStream, boolean b) throws IOException {
        log.info("createSocket(Socket socket, InputStream inputStream, boolean b)");
        return super.createSocket(socket, inputStream, b);
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {

        log.info(MessageFormat.format("creating tunneled socket to {}:{} via {}:{}", host, port, tunnelHost, tunnelPort));
        System.out.printf("SslTunnelFactory creating tunneled socket to %s:%s via %s:%s%n", host, port, tunnelHost, tunnelPort);

        Socket tunnel = new Socket(tunnelHost, tunnelPort);
        tunnel.setKeepAlive(true);
        tunnel.setSoTimeout(10000);

        doTunnelHandshake(tunnel, host, port);

        SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(tunnel, host, port, autoClose);
        socket.setSoTimeout(10000);
        tunnel.setKeepAlive(true);

        socket.addHandshakeCompletedListener(event -> {
            System.out.println("Handshake finished!");
            System.out.println("\t CipherSuite:" + event.getCipherSuite());
            System.out.println("\t SessionId " + event.getSession());
            System.out.println("\t PeerHost " + event.getSession().getPeerHost());
        });
        return socket;
    }

    private void doTunnelHandshake(Socket tunnel, String host, int port) throws IOException {
        OutputStream out = tunnel.getOutputStream();
        String msg = "CONNECT " + host + ":" + port + " HTTP/1.1\n" + "User-Agent: java"
                + "\r\n\r\n";
        byte[] b;
        try {
            /*
             * We really do want ASCII7 -- the http protocol doesn't change with
             * locale.
             */
            b = msg.getBytes("ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            /*
             * If ASCII7 isn't there, something serious is wrong, but Paranoia
             * Is Good (tm)
             */
            b = msg.getBytes();
        }
        out.write(b);
        out.flush();

        /*
         * We need to store the reply so we can create a detailed error message
         * to the user.
         */
        byte[] reply = new byte[200];
        int replyLen = 0;
        int newlinesSeen = 0;
        boolean headerDone = false; /* Done on first newline */

        InputStream in = tunnel.getInputStream();
        boolean error = false;

        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("Unexpected EOF from proxy");
            }
            if (i == '\n') {
                headerDone = true;
                ++newlinesSeen;
            } else if (i != '\r') {
                newlinesSeen = 0;
                if (!headerDone && replyLen < reply.length) {
                    reply[replyLen++] = (byte) i;
                }
            }
        }

        /*
         * Converting the byte array to a string is slightly wasteful in the
         * case where the connection was successful, but it's insignificant
         * compared to the network overhead.
         */
        String replyStr;
        try {
            replyStr = new String(reply, 0, replyLen, "ASCII7");
        } catch (UnsupportedEncodingException ignored) {
            replyStr = new String(reply, 0, replyLen);
        }

        /*
         * We check for Connection Established because our proxy returns
         * HTTP/1.1 instead of 1.0
         */
        //if (!replyStr.startsWith("HTTP/1.0 200")) {
        System.out.println("reply str : " + replyStr);
        if (!replyStr.toLowerCase().contains("200")) {
            throw new IOException("Unable to tunnel through " + tunnelHost + ":" + tunnelPort + ".  Proxy returns \""
                    + replyStr + "\"");
        }

        log.info("tunnel ssl handshake successful");
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return sslSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return sslSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(String arg0, int arg1) throws IOException {
        return createSocket(null, arg0, arg1, false);
    }

    @Override
    public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
        return createSocket(null, arg0.getHostName(), arg1, false);
    }

    @Override
    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
        return createSocket(null, arg0, arg1, false);
    }

    @Override
    public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
        return createSocket(null, arg0.getHostName(), arg1, false);
    }

}