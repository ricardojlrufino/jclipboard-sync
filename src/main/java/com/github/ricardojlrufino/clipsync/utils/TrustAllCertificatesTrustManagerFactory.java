package com.github.ricardojlrufino.clipsync.utils;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

public class TrustAllCertificatesTrustManagerFactory extends TrustManagerFactory {

    public TrustAllCertificatesTrustManagerFactory() {
        super(new TrustManagerFactoryServiceImpl(), null, "NONE");
    }

    private static final TrustManager TRUST_MANAGER = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // Trust all clients
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // Trust all servers
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private static class TrustManagerFactoryServiceImpl extends TrustManagerFactorySpi {
        @Override
        protected void engineInit(KeyStore keyStore) {
            // No initialization needed
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
            // No initialization needed
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return new TrustManager[] { TRUST_MANAGER };
        }
    }
}
