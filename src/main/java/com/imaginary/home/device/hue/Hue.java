/**
 * Copyright (C) 2013 George Reese
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imaginary.home.device.hue;

import com.imaginary.home.controller.CommunicationException;
import com.imaginary.home.controller.HomeAutomationSystem;
import com.imaginary.home.lighting.ColorMode;
import com.imaginary.home.lighting.Light;
import com.imaginary.home.lighting.LightingService;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public class Hue implements HomeAutomationSystem, LightingService {
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
    private String     id;
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

    @Override
    public @Nonnull String getAPIEndpoint() {
        return endpoint;
    }

    @Override
    public @Nonnull Properties getAuthenticationProperties() {
        Properties p = new Properties();

        p.put("accessKey", accessKey);
        return p;
    }

    public @Nonnull Properties getCustomProperties() {
        return customProperties;
    }

    @Override
    public @Nonnull String getId() {
        return id;
    }

    @Override
    public @Nonnull String getName() {
        return "Hue";
    }

    @Override
    public @Nonnull String getVendor() {
        return "Philips";
    }

    @Override
    public void init(@Nonnull String id, @Nonnull Properties auth, @Nonnull Properties custom) {
        this.id = id;
        accessKey = auth.getProperty("accessKey", "");
        ipAddress = auth.getProperty("ipAddress");
        customProperties = custom;
    }

    @Override
    public @Nonnull Iterable<Light> listLights() throws CommunicationException {
        HueMethod method = new HueMethod(this);

        JSONObject list = method.get("lights");

        if( list == null ) {
            return Collections.emptyList();
        }
        ArrayList<Light> matches = new ArrayList<Light>();

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
    public @Nonnull Iterable<ColorMode> listNativeColorModes() {
        ArrayList<ColorMode> modes = new ArrayList<ColorMode>();

        modes.add(ColorMode.CIEXYZ);
        modes.add(ColorMode.CT);
        modes.add(ColorMode.HSV);
        return modes;
    }

    @Override
    public @Nonnull Properties pair(@Nonnull String applicationName) throws CommunicationException {
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
