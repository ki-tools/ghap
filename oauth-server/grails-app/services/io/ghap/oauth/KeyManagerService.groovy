package io.ghap.oauth

import java.security.KeyStore

class KeyManagerService {

    def grailsApplication

    private KeyStore keyStore;

    def getKeystore() {
        if (keyStore != null) {
            return keyStore;
        }
        loadKeystore();
        return keyStore;
    }

    private synchronized void loadKeystore() {
        if (keyStore == null) {
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(getClass().getClassLoader().getResourceAsStream(grailsApplication.config.ghap.keystore.file), grailsApplication.config.ghap.keystore.password.toCharArray());
        }
    }
}
