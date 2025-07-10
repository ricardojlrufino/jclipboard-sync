package com.github.ricardojlrufino.clipsync.utils;

import java.net.URI;

public class ProxyConfig {

    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;

    public static ProxyConfig configureProxyEnv() {
        String http_proxy = System.getenv("http_proxy");

        URI uri = URI.create(http_proxy);
        String proxyHost = uri.getHost();
        int proxyPort = uri.getPort();
        String proxyUser = uri.getUserInfo();
        String proxyPassword = "";
        if (proxyUser != null) {
            String[] split = proxyUser.split(":");
            proxyUser = split[0];
            proxyPassword = split[1];
        }

        System.out.println("Proxy config ENV:");
        System.out.println("Host: " + proxyHost);
        System.out.println("Port: " + proxyPort);
        System.out.println("User: " + proxyUser);
        System.out.println("Password: " + (proxyPassword != null && !proxyPassword.isEmpty() ? "****" : "null"));

        ProxyConfig config = new ProxyConfig();
        config.setProxyHost(proxyHost);
        config.setProxyPort(proxyPort);
        config.setProxyUser(proxyUser);
        config.setProxyPassword(proxyPassword);

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", "" + proxyPort);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", "" + proxyPort);

        if (proxyPassword != null)
            System.setProperty("http.proxyPassword", proxyPassword);
        if (proxyUser != null)
            System.setProperty("http.proxyUser", proxyUser);

        if (proxyPassword != null)
            System.setProperty("https.proxyPassword", proxyPassword);
        if (proxyUser != null)
            System.setProperty("https.proxyUser", proxyUser);

        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        return config;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

}
