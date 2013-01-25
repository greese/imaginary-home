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
import com.imaginary.home.lighting.Color;
import com.imaginary.home.lighting.ColorMode;
import org.dasein.persist.Memento;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.SearchTerm;
import org.dasein.persist.Transaction;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/24/13 6:30 PM</p>
 *
 * @author George Reese
 */
public class Light extends PoweredDevice {
    static private PersistentCache<Light> cache;

    static private @Nonnull PersistentCache<Light> getCache() throws PersistenceException {
        if( cache == null ) {
            //noinspection unchecked
            cache = (PersistentCache<Light>)PersistentCache.getCache(Light.class);
        }
        return cache;
    }

    static @Nonnull PoweredDevice createLight(@Nonnull ControllerRelay relay, @Nonnull JSONObject json) throws JSONException, PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();

        mapLight(relay, json, state);

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

    static public @Nonnull Collection<Light> findLightsForRelay(@Nonnull ControllerRelay relay) throws PersistenceException {
        return getCache().find(new SearchTerm("relayId", relay.getControllerRelayId()));
    }

    static public void findLightssForRelayWithChildren(@Nonnull ControllerRelay relay, @Nonnull Collection<Device> devices) throws PersistenceException {
        devices.addAll(findLightsForRelay(relay));
    }

    static public @Nullable Light getLight(@Nonnull String lightId) throws PersistenceException {
        return getCache().get(lightId);
    }

    static public @Nullable Light getLight(@Nonnull ControllerRelay relay, @Nonnull String vendorDeviceId) throws PersistenceException {
        Iterator<Light> it = getCache().find(new SearchTerm("vendorDeviceId", vendorDeviceId), new SearchTerm("relayId", relay.getControllerRelayId())).iterator();

        if( it.hasNext() ) {
            return it.next();
        }
        return null;
    }

    static void mapLight(@Nonnull ControllerRelay relay, @Nonnull JSONObject json, @Nonnull Map<String,Object> state) throws JSONException {
        mapPoweredDevice(relay, json, state);
        state.put("deviceType", "light");
        if( json.has("color") ) {
            JSONObject color = json.getJSONObject("color");
            ColorMode colorMode = null;
            float[] components = null;

            if( color.has("colorMode") && !color.isNull("colorMode") ) {
                try {
                    colorMode = ColorMode.valueOf(color.getString("colorMode"));
                }
                catch( IllegalArgumentException e ) {
                    throw new JSONException("Invalid color mode: " + color.getString("colorMode"));
                }
            }
            if( color.has("components") && !color.isNull("components") ) {
                JSONArray arr = color.getJSONArray("components");
                components = new float[arr.length()];

                for( int i=0; i<arr.length(); i++ ) {
                    components[i] = (float)arr.getDouble(i);
                }
            }
            if( colorMode != null || components != null ) {
                state.put("color", new Color(colorMode, components));
            }
        }
        if( json.has("brightness") && !json.isNull("brightness") ) {
            state.put("brightness", (float)json.getDouble("brightness"));
        }
        if( json.has("supportsColorChanges") ) {
            state.put("colorChangeSupported", !json.isNull("supportsColorChanges") && json.getBoolean("supportsColorChanges"));
        }
        if( json.has("supportsBrightnessChanges") ) {
            state.put("dimmable", !json.isNull("supportsBrightnessChanges") && json.getBoolean("supportsBrightnessChanges"));
        }
        if( json.has("colorModes") ) {
            JSONArray arr = json.getJSONArray("colorModes");
            ColorMode[] modes = new ColorMode[arr.length()];

            for( int i=0; i<arr.length(); i++ ) {
                try {
                    modes[i] = ColorMode.valueOf(arr.getString(i));
                }
                catch( IllegalArgumentException e ) {
                    throw new JSONException("Invalid color mode: " + arr.getString(i));
                }
            }
            state.put("colorModesSupported", modes);
        }
    }

    private float       brightness;
    private Color       color;
    private boolean     colorChangeSupported;
    private ColorMode[] colorModesSupported;
    private boolean     dimmable;

    public Light() { }

    public void changeColor(@Nonnull Color newColor, @Nullable TimePeriod<?> transitionTime) {
        // TODO: implement me
    }

    public void changeWhite(@Nonnegative int warmthInMireds, @Nonnegative float brightness, @Nullable TimePeriod<?> transitionTime) {
        // TODO: implement me
    }

    public void fadeOff(@Nonnull  TimePeriod<?> transitionTime) {
        // TODO: implement me
    }

    public void fadeOn(@Nonnull TimePeriod<?> transitionTime) {
        // TODO: implement me
    }

    public void fadeOn(@Nonnull TimePeriod<?> transitionTime, @Nonnegative float targetBrightness) {
        // TODO: implement me
    }

    public @Nonnegative float getBrightness() {
        return brightness;
    }

    public @Nonnull Color getColor() {
        return color;
    }

    public @Nonnull ColorMode[] getColorModesSupported() {
        return (colorModesSupported == null ? new ColorMode[0] : colorModesSupported);
    }

    public boolean isColorChangeSupported() {
        return colorChangeSupported;
    }

    public boolean isDimmable() {
        return dimmable;
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

    public void strobe() {
        // TODO: implement me
    }

    @Override
    public void update(@Nonnull JSONObject json) throws PersistenceException {
        try {
            Memento<Light> memento = new Memento<Light>(this);
            Map<String,Object> state = new HashMap<String, Object>();

            mapLight(getRelay(), json, state);
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
