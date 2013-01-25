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
import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;
import org.dasein.persist.Memento;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.SearchTerm;
import org.dasein.persist.Transaction;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/24/13 6:30 PM</p>
 *
 * @author George Reese
 */
public class PoweredDevice extends Device {
    static private PersistentCache<PoweredDevice> cache;

    static private @Nonnull PersistentCache<PoweredDevice> getCache() throws PersistenceException {
        if( cache == null ) {
            //noinspection unchecked
            cache = (PersistentCache<PoweredDevice>)PersistentCache.getCache(PoweredDevice.class);
        }
        return cache;
    }

    static @Nonnull PoweredDevice createPoweredDevice(@Nonnull ControllerRelay relay, @Nonnull JSONObject json) throws JSONException, PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();

        mapPoweredDevice(relay, json, state);

        Transaction xaction = Transaction.getInstance();

        try {
            PoweredDevice d = getCache().create(xaction, state);

            xaction.commit();
            return d;
        }
        finally {
            xaction.rollback();
        }
    }

    static public Collection<PoweredDevice> findPoweredDevicesForRelay(@Nonnull ControllerRelay relay) throws PersistenceException {
        return getCache().find(new SearchTerm("relayId", relay.getControllerRelayId()));
    }

    static public void findPoweredDevicesForRelayWithChildren(@Nonnull ControllerRelay relay, @Nonnull Collection<Device> devices) throws PersistenceException {
        devices.addAll(findPoweredDevicesForRelay(relay));
    }

    static public @Nullable PoweredDevice getPoweredDevice(@Nonnull String deviceId) throws PersistenceException {
        return getCache().get(deviceId);
    }

    static public void mapPoweredDevice(@Nonnull ControllerRelay relay, @Nonnull JSONObject json, @Nonnull Map<String,Object> state) throws JSONException {
        mapDevice(relay, json, state);
        state.put("deviceType", "powered");
        state.put("on", json.has("on") && !json.isNull("on") && json.getBoolean("on"));
    }

    private boolean on;

    public PoweredDevice() {}

    public void flipOn() {
        // TODO: implement me
    }

    public void flipOff() {
        // TODO: implement me
    }

    public boolean isOn() {
        return on;
    }

    @Override
    public void remove() throws PersistenceException {
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().remove(xaction, this);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
    }

    @Override
    public void update(@Nonnull JSONObject json) throws PersistenceException {
        try {
            Memento<PoweredDevice> memento = new Memento<PoweredDevice>(this);
            Map<String,Object> state = new HashMap<String, Object>();

            mapPoweredDevice(getRelay(), json, state);
            state.remove("vendorDeviceId");
            state.remove("deviceId");
            memento.save(state);
            state = memento.getState();

            Transaction xaction = Transaction.getInstance();

            try {
                getCache().update(xaction, this, state);
                xaction.commit();
            }
            finally {
                xaction.rollback();
            }
            memento.load(state);
        }
        catch( JSONException e ) {
            throw new PersistenceException(e);
        }
    }
}
