/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2013 enStratus Networks Inc - ALL RIGHTS RESERVED
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
package com.imaginary.hue;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Hue {
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
        this.endpoint = "http://" + ipAddress + "/api/" + accessKey + "/";
    }

    public @Nonnull String getAccessKey() {
        return accessKey;
    }

    public @Nonnull Properties getCustomProperties() {
        return customProperties;
    }

    public @Nonnull String getEndpoint() {
        return endpoint;
    }

    public @Nonnull String getIpAddress() {
        return ipAddress;
    }

    public Iterable<Bulb> listBulbs() throws HueException {
        HueMethod method = new HueMethod(this);

        JSONObject list = method.get("lights");

        if( list == null ) {
            return Collections.emptyList();
        }
        ArrayList<Bulb> matches = new ArrayList<Bulb>();

        for( String id : JSONObject.getNames(list) ) {
            try {
                JSONObject item = list.getJSONObject(id);
                String name = (item.has("name") ? item.getString("name") : id);


                matches.add(new Bulb(this, id, name));
            }
            catch( JSONException e ) {
                throw new HueException(e);
            }
        }
        return matches;
    }
}
