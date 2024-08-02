package com.github.ricardojlrufino.clipsync.utils;

import java.util.Properties;

public class PropertiesConfig extends Properties {

    public String getAsRequired(String key){
        String value = this.getProperty(key);
        if( value == null ) throw new RuntimeException("config '" + key + "' is required !");
        return value;
    }

}
