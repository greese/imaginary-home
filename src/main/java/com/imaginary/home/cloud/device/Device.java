/*
 * Copyright (C) 2013 George Reese
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.imaginary.home.cloud.device;

import com.imaginary.home.cloud.ControllerRelay;
import org.dasein.persist.DataIntegrityException;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for any kind of device.
 * <p>Created by George Reese: 1/14/13 9:17 AM</p>
 * @author George Reese
 */
public abstract class Device implements CachedItem {
    static public @Nonnull Device create(@Nonnull ControllerRelay relay, @Nonnull String deviceType, @Nonnull JSONObject json) throws JSONException, PersistenceException {
        if( deviceType.equals("powered") ) {
            return PoweredDevice.createPoweredDevice(relay, json);
        }
        else if( deviceType.equals("light") ) {
            return Light.createLight(relay, json);
        }
        throw new PersistenceException("No such device type: " + deviceType);
    }

    static public @Nonnull Collection<Device> findDevicesForRelay(@Nonnull ControllerRelay relay) throws PersistenceException {
        ArrayList<Device> devices = new ArrayList<Device>();

        PoweredDevice.findPoweredDevicesForRelayWithChildren(relay, devices);
        return devices;
    }

    static public @Nullable Device getDevice(@Nonnull String deviceType, @Nonnull String deviceId) throws PersistenceException {
        if( deviceType.equals("powered") ) {
            return PoweredDevice.getPoweredDevice(deviceId);
        }
        else if( deviceType.equals("light") ) {
            return Light.getLight(deviceId);
        }
        return null;
    }

    static void mapDevice(@Nonnull ControllerRelay relay, @Nonnull JSONObject json, @Nonnull Map<String,Object> state) throws JSONException {
        state.put("relayId", relay.getControllerRelayId());
        if( json.has("name") && !json.isNull("name") ) {
            state.put("name", json.getString("name"));
        }
        if( json.has("description") && !json.isNull("description") ) {
            state.put("description", json.getString("description"));
        }
        if( json.has("model") && !json.isNull("model") ) {
            state.put("model", json.getString("model"));
        }
        if( json.has("id") && !json.isNull("id") ) {
            state.put("vendorDeviceId", json.getString("id"));
            state.put("deviceId", relay.getControllerRelayId() + ":" + json.getString("id"));
        }
    }

    private String description;
    @Index(type= IndexType.PRIMARY)
    private String deviceId;
    private String deviceType;
    private String fixtureId;
    @Index(type=IndexType.SECONDARY)
    private String model;
    private String name;
    @Index(type=IndexType.FOREIGN, identifies=ControllerRelay.class)
    private String relayId;
    @Index(type=IndexType.SECONDARY, multi = { "relayId" }, cascade=true)
    private String vendorDeviceId;

    public Device() { }

    public @Nonnull String getDescription() {
        return description;
    }

    public @Nonnull String getDeviceId() {
        return deviceId;
    }

    public @Nonnull String getDeviceType() {
        return deviceType;
    }

    public @Nullable String getFixtureId() {
        return fixtureId;
    }

    public @Nullable String getModel() {
        return model;
    }

    public @Nonnull String getName() {
        return name;
    }

    public @Nonnull ControllerRelay getRelay() throws PersistenceException {
        ControllerRelay relay = ControllerRelay.getRelay(relayId);

        if( relay == null ) {
            throw new DataIntegrityException("Invalid relayId value for device " + deviceId + ": " + relayId);
        }
        return relay;
    }

    public @Nonnull String getRelayId() {
        return relayId;
    }

    public @Nonnull String getVendorDeviceId() {
        return vendorDeviceId;
    }

    public boolean isValidForCache() {
        return false;
    }

    public abstract void remove() throws PersistenceException;

    public abstract void update(@Nonnull JSONObject json) throws PersistenceException;
}
