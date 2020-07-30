package io.ghap.auth;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class Settings {
    private static Settings instance;

    private Settings(){

    }

    public static Settings getInstance(){
        if(instance == null){
            synchronized (OauthClient.class){
                if(instance == null){
                    instance = new Settings();
                }

            }
        }
        return instance;
    }

    public String getOauthUrl(){
        return OauthClient.getInstance().getOauthPath();
    }

}
