/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2013 George Reese - ALL RIGHTS RESERVED
 *
 * ====================================================================
 *  NOTICE: All information contained herein is, and remains the
 *  property of enStratus Networks Inc. The intellectual and technical
 *  concepts contained herein are proprietary to enStratus Networks Inc
 *  and may be covered by U.S. and Foreign Patents, patents in process,
 *  and are protected by trade secret or copyright law. Dissemination
 *  of this information or reproduction of this material is strictly
 *  forbidden unless prior written permission is obtained from
 *  enStratus Networks Inc.
 * ====================================================================
 */
package com.imaginary.home.sys.hue;

import com.imaginary.home.CommunicationException;
import com.imaginary.home.HomeAutomationSystem;
import com.imaginary.home.lighting.LightingService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Hue implements HomeAutomationSystem {
    static public final ExecutorService executorService = Executors.newCachedThreadPool();

    static private @Nonnull String getLastItem(@Nonnull String name) {
        int idx = name.lastIndexOf('.');

        if( idx < 0 ) {
            return name;
        }
        else if( idx == (name.length()-1) ) {
            return "";
        }
        return name.substring(idx+1);
    }

    static public @Nonnull Logger getLogger(@Nonnull Class<?> cls) {
        return getLogger(cls, "std");
    }

    static public @Nonnull Logger getLogger(@Nonnull Class<?> cls, @Nonnull String type) {
        String pkg = getLastItem(cls.getPackage().getName());

        if( pkg.equals("hue") ) {
            pkg = "";
        }
        else {
            pkg = pkg + ".";
        }
        return Logger.getLogger("imaginary.hue." + type + "." + pkg + getLastItem(cls.getName()));
    }

    static public @Nonnull Logger getWireLogger(@Nonnull Class<?> cls) {
        return getLogger(cls, "wire");
    }

    private String     accessKey;
    private Properties customProperties;
    private String     endpoint;
    private String     ipAddress;

    public Hue(@Nonnull String ipAddress, @Nonnull String accessKey) {
        this(ipAddress, accessKey, new Properties());
    }

    public Hue(@Nonnull String ipAddress, @Nonnull String accessKey, @Nonnull Properties customProperties) {
        this.ipAddress = ipAddress;
        this.accessKey = accessKey;
        this.customProperties = customProperties;
        if( accessKey.equals("") ) {
            this.endpoint = "http://" + ipAddress + "/api";
        }
        else {
            this.endpoint = "http://" + ipAddress + "/api/" + accessKey + "/";
        }
    }

    public @Nonnull String getAccessKey() {
        return accessKey;
    }

    @Override
    public @Nonnull String getAPIEndpoint() {
        return endpoint;
    }

    @Override
    public Properties getAuthenticationProperties() {
        Properties p = new Properties();

        p.put("accessKey", accessKey);
        return p;
    }

    public @Nonnull Properties getCustomProperties() {
        return customProperties;
    }


    public @Nonnull String getIpAddress() {
        return ipAddress;
    }

    @Override
    public @Nullable LightingService getLightingService() {
        return null;
    }

    private String generateKey() {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        int len = 20 + random.nextInt(10);

        while( str.length() < len ) {
            char c = (char)random.nextInt(255);

            if( c >= 'a' && c <= 'z' ) {
                if( c != 'l' && c != 'i' && c != 'o' ) {
                    str.append(c);
                }
            }
            else if( c >= 'A' && c <= 'Z' ) {
                if( c != 'I' && c != 'O' ) {
                    str.append(c);
                }
            }
            else if( c >= '2' && c <= '9' ) {
                str.append(c);
            }
        }
        return str.toString();
    }

    @Override
    public String getName() {
        return "Hue";
    }

    @Override
    public String getVendor() {
        return "Philips";
    }

    public Iterable<HueBulb> listBulbs() throws HueException {
        HueMethod method = new HueMethod(this);

        JSONObject list = method.get("lights");

        if( list == null ) {
            return Collections.emptyList();
        }
        ArrayList<HueBulb> matches = new ArrayList<HueBulb>();

        for( String id : JSONObject.getNames(list) ) {
            try {
                JSONObject item = list.getJSONObject(id);
                String name = (item.has("name") ? item.getString("name") : id);


                matches.add(new HueBulb(this, id, name));
            }
            catch( JSONException e ) {
                throw new HueException(e);
            }
        }
        return matches;
    }

    public Properties pair(String applicationName) throws CommunicationException {
        HueMethod method = new HueMethod(this);
        HashMap<String,Object> auth = new HashMap<String, Object>();

        auth.put("username", generateKey());
        auth.put("devicetype", applicationName);
        try {
            JSONObject result = method.post("", new JSONObject(auth));
            Properties properties = new Properties();

            properties.put("accessKey", accessKey);
            if( result != null && result.has("success") ) {
                result = result.getJSONObject("success");
                if( result.has("username") ) {
                    accessKey = result.getString("username");
                    endpoint = "http://" + ipAddress + "/api/" + accessKey + "/";
                    properties.put("accessKey", accessKey);
                    return properties;
                }
            }
            throw new HueException("Failed to receive authentication key");
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }
}
