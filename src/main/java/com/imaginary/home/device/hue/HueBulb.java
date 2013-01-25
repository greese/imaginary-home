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
import com.imaginary.home.controller.ControllerException;
import com.imaginary.home.controller.HomeController;
import com.imaginary.home.lighting.Color;
import com.imaginary.home.lighting.ColorMode;
import com.imaginary.home.lighting.Light;
import org.apache.log4j.Logger;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class HueBulb implements Light {
    static public final Logger logger = Hue.getLogger(HueBulb.class);

    private String bulbId;
    private Hue    hue;
    private String name;

    public HueBulb(@Nonnull Hue hue, @Nonnull String id, @Nonnull String name) {
        bulbId = id;
        this.hue = hue;
        this.name = name;
    }

    @Override
    public @Nonnull Future<Boolean> changeColor(final @Nonnull Color newColor, final @Nullable TimePeriod<?> transitionTime) {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long millis = (transitionTime == null ? 0L : transitionTime.convertTo(TimePeriod.MILLISECOND).longValue());

                return changeColor(newColor, millis);
            }
        });
    }

    @Override
    public @Nonnull Future<Boolean> changeColorCIEXYZ(@Nonnegative float x, @Nonnegative float y, @Nullable TimePeriod<?> transitionTime) throws CommunicationException {
        return changeColor(new Color(ColorMode.CIEXYZ, x, y, 1.0f-(x+y)), transitionTime);
    }

    @Override
    public @Nonnull Future<Boolean> changeColorHSV(@Nonnegative float hue, @Nonnegative float saturation, @Nullable TimePeriod<?> transitionTime) throws CommunicationException {
        return changeColor(new Color(ColorMode.HSV, hue, saturation), transitionTime);
    }

    @Override
    public @Nonnull Future<Boolean> changeColorRGB(@Nonnegative float red, @Nonnegative float green, @Nonnegative float blue, @Nullable TimePeriod<?> transitionTime) throws CommunicationException {
        return changeColor(new Color(ColorMode.RGB, red, green, blue), transitionTime);
    }

    private boolean changeColor(@Nonnull Color newColor, @Nonnegative long millis) throws HueException {
        Map<String,Object> state = new HashMap<String,Object>();
        String resource = "lights/" + bulbId + "/state";
        float[] components = newColor.getComponents();

        if( millis >= 100 ) {
            state.put("transitiontime", millis/100);
        }
        state.put("on", true);
        if( newColor.getColorMode().equals(ColorMode.HSV) ) {
            float h = components[0];
            int s = (int)components[1];

            if( h < 0 ) {
                h = 0;
            }
            else if( h > 360 ) {
                h = 360;
            }
            if( s < 0 ) {
                s = 0;
            }
            else if( s > 100 ) {
                s = 100;
            }

            state.put("hue", (int)(182.04 * h));
            state.put("sat", (s * 254)/100);
        }
        else if( newColor.getColorMode().equals(ColorMode.CT) ) {
            state.put("ct", (int)components[0]);
            state.put("bri", (int)(components[1]*254/100));
        }
        else {
            if( !newColor.getColorMode().equals(ColorMode.CIEXYZ) ) {
                newColor = newColor.convertToCIEXYZ();
                components = newColor.getComponents();
            }
            state.put("xy", new float[] { components[0], components[1] });
        }
        HueMethod method = new HueMethod(hue);

        method.put(resource, new JSONObject(state));
        return true;
    }

    @Override
    public @Nonnull Future<Boolean> changeWhite(final @Nonnegative int warmthInMireds, final @Nonnegative float brightness, final @Nullable TimePeriod<?> transitionTime) {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long millis = (transitionTime == null ? 0L : transitionTime.convertTo(TimePeriod.MILLISECOND).longValue());

                return changeWhite(warmthInMireds, brightness, millis);
            }
        });
    }

    private boolean changeWhite(int warmth, float brightness, @Nonnegative long millis) throws HueException {
        return changeColor(new Color(ColorMode.CT, warmth, brightness), millis);
    }

    @Override
    public @Nonnull Future<Boolean> fadeOff(final @Nonnull  TimePeriod<?> transitionTime) throws CommunicationException {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws CommunicationException {
                return fade(transitionTime, 0);
            }
        });
    }

    @Override
    public @Nonnull Future<Boolean> fadeOn(@Nonnull TimePeriod<?> transitionTime) throws CommunicationException {
        return fadeOn(transitionTime, getBrightness());
    }

    @Override
    public @Nonnull Future<Boolean> fadeOn(final @Nonnull TimePeriod<?> transitionTime, final @Nonnegative float targetBrightness) {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws CommunicationException {
                return fade(transitionTime, (int)((targetBrightness*254)/100));
            }
        });
    }

    private @Nonnegative boolean fade(@Nonnull TimePeriod<?> transitionTime, @Nonnegative int targetBrightness) throws CommunicationException {
        logger.debug("Fading to " + targetBrightness + " over " + transitionTime + " for " + bulbId);
        if( targetBrightness < 0 ) {
            targetBrightness = 0;
        }
        int currentBrightness = (int)((getBrightness()*254)/100);
        boolean on = isOn();

        if( (on && targetBrightness == currentBrightness) || (!on && targetBrightness == 0) ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("Nothing to do for " + bulbId);
            }
            return false;
        }
        if( !on ) {
            logger.debug("Bulb is currently off");
            currentBrightness = 0;
        }
        if( logger.isDebugEnabled() ) {
            logger.debug(bulbId + ".currentBrightness=" + currentBrightness);
        }
        int distance;

        if( targetBrightness == 0 ) {
            distance = currentBrightness;
        }
        else if( targetBrightness > currentBrightness ) {
            distance = targetBrightness - currentBrightness;
        }
        else {
            distance = currentBrightness - targetBrightness;
        }
        if( logger.isDebugEnabled() ) {
            logger.debug(bulbId + ".distance=" + distance);
        }
        int conversionTime = transitionTime.convertTo(TimePeriod.MILLISECOND).intValue()/100;
        Map<String,Object> state = new HashMap<String,Object>();
        long startTimestamp = System.currentTimeMillis();
        String resource = "lights/" + bulbId + "/state";
        HueMethod method = new HueMethod(hue);

        if( conversionTime < 1 ) {
            conversionTime = 1;
        }
        if( !on ) {
            state.put("on", true);
            state.put("bri", 0);
            if( logger.isDebugEnabled() ) {
                logger.debug("Turning " + bulbId + " on...");
            }
            try {
                method.put(resource, new JSONObject(state));
            }
            catch( Throwable t ) {
                t.printStackTrace();
            }
        }

        int interval = 100;
        int step = 1;

        if( conversionTime < distance ) {
            step = distance/conversionTime;
        }
        else {
            interval = (conversionTime/distance) * 100;
        }
        if( logger.isDebugEnabled() ) {
            logger.debug(bulbId + " -> interval + =" + interval + ", step=" + step);
        }
        int stepCount = distance/step;
        if( logger.isDebugEnabled() ) {
            logger.debug(bulbId + ".stepCount=" + stepCount);
        }
        if( targetBrightness < currentBrightness ) {
            step = -step;
        }
        state.clear();
        for( int i=0; i<stepCount; i++ ) {
            try { Thread.sleep(interval); }
            catch( InterruptedException ignore ) { }
            // adjust for slow network
            int actual = (int)(System.currentTimeMillis()-startTimestamp)/interval;
            int bri = currentBrightness + (actual*step);

            if( bri <= 254 && bri >= 0 ) {
                state.put("bri", bri);
                if( logger.isTraceEnabled() ) {
                    logger.debug(bulbId + " -> setting brightness to " + bri);
                }
                try { method.put(resource, new JSONObject(state)); }
                catch( HueException ignore ) { }
            }
            if( bri == targetBrightness ) {
                break;
            }
        }
        if( getBrightness() != targetBrightness ) {
            if( logger.isDebugEnabled() ) {
                logger.debug(bulbId + " -> cleaning up");
            }
            state.clear();
            state.put("bri", targetBrightness);
            try { method.put(resource, new JSONObject(state)); }
            catch( HueException ignore ) { }
        }
        if( targetBrightness == 0 ) {
            if( logger.isDebugEnabled() ) {
                logger.debug(bulbId + " -> turning off");
            }
            flip(false);
        }
        int brightness = (int)((getBrightness()*254)/100);

        if( logger.isDebugEnabled() ) {
            logger.debug(bulbId + ".brightness=" + brightness);
        }
        return true;
    }

    @Override
    public @Nonnull Future<Boolean> flipOff() {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws CommunicationException {
                return flip(false);
            }
        });
    }

    @Override
    public @Nonnull Future<Boolean> flipOn() throws CommunicationException {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws CommunicationException {
                return flip(true);
            }
        });
    }

    private boolean flip(boolean on) throws CommunicationException {
        if( isOn() == on ) {
            return false;
        }
        Map<String,Object> state = new HashMap<String,Object>();

        state.put("on", on);
        String resource = "lights/" + bulbId + "/state";
        HueMethod method = new HueMethod(hue);

        method.put(resource, new JSONObject(state));
        return true;
    }

    @Override
    public @Nonnegative float getBrightness() throws CommunicationException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return 0;
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( !state.has("bri") ) {
                return 0f;
            }
            return ((float)state.getInt("bri") * 100f)/254;
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    @Override
    public @Nonnull Color getColor() throws CommunicationException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return new Color(ColorMode.CIEXYZ, 0.4448f, 0.4066f);
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( state.has("colormode") ) {
                String mode = state.getString("colormode");


                if( mode.equalsIgnoreCase("xy") ) {
                    JSONArray arr = state.getJSONArray("xy");

                    new Color(ColorMode.CIEXYZ, (float)arr.getDouble(0), (float)arr.getDouble(1));
                }
                else if( mode.equalsIgnoreCase("ct") ) {
                    int warmth = (state.has("ct") ? state.getInt("ct") : 154);
                    int brightness = (state.has("bri") ? state.getInt("bri") : 0);

                    return new Color(ColorMode.CT, warmth, brightness/254);
                }
                else {
                    float h = (state.has("hue") ? (float)state.getDouble("hue")/182.04f : 360f);
                    int s = (state.has("sat") ? (state.getInt("sat")*100)/255 : 100);

                    return new Color(ColorMode.HSV, h, s);
                }
            }
            return new Color(ColorMode.CIEXYZ, 0.4448f, 0.4066f);
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    @Override
    public @Nonnull ColorMode getColorMode() throws CommunicationException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return ColorMode.CIEXYZ;
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( state.has("colormode") ) {
                String mode = state.getString("colormode");

                if( mode.equalsIgnoreCase("hue") ) {
                    return ColorMode.HSV;
                }
                else if( mode.equalsIgnoreCase("ct") ) {
                    return ColorMode.CT;
                }
                return ColorMode.CIEXYZ;
            }
            return ColorMode.CIEXYZ;
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    @Override
    public @Nullable String getModel() throws CommunicationException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("modelid") ) {
            return null;
        }
        try {
            return json.getString("modelid");
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    @Override
    public @Nonnull String getName() {
        return name;
    }

    @Override
    public @Nonnull String getProviderId() {
        return bulbId;
    }

    @Override
    public boolean isOn() throws CommunicationException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return false;
        }
        try {
            JSONObject state = json.getJSONObject("state");

            return (state.has("on") && state.getBoolean("on"));
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    @Override
    public Future<Boolean> strobe(final @Nonnull TimePeriod<?> interval, final @Nullable TimePeriod<?> duration, final @Nonnull Color ... colors) throws CommunicationException {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long end = (duration == null ? 0L : (System.currentTimeMillis() + duration.convertTo(TimePeriod.MILLISECOND).longValue()));
                long millis = interval.convertTo(TimePeriod.MILLISECOND).longValue();
                int currentColor = 0;

                if( millis < 100 ) {
                    millis = 100;
                }
                while( end < 1 || System.currentTimeMillis() < end ) {
                    Color color = colors[currentColor++];

                    if( currentColor >= colors.length ) {
                        currentColor = 0;
                    }
                    changeColor(color, null);
                    try { Thread.sleep(millis); }
                    catch( InterruptedException ignore ) { }
                }
                return true;
            }
        });
    }

    @Override
    public boolean supportsBrightnessChanges() {
        return true;
    }

    @Override
    public boolean supportsColor() {
        return true;
    }

    @Override
    public boolean supportsColorChanges() {
        return true;
    }

    @Override
    public void toMap(@Nonnull Map<String,Object> map) throws CommunicationException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            throw new CommunicationException("No state found for " + bulbId);
        }
        try {
            JSONObject state = json.getJSONObject("state");

            map.put("on", state.has("on") && !state.isNull("on") && state.getBoolean("on"));
            if( state.has("modelid") && !state.isNull("modelid") ) {
                map.put("model", state.getString("modelid"));
            }
            if( state.has("colormode") ) {
                String mode = state.getString("colormode");


                if( mode.equalsIgnoreCase("xy") ) {
                    JSONArray arr = state.getJSONArray("xy");

                    map.put("color", new Color(ColorMode.CIEXYZ, (float)arr.getDouble(0), (float)arr.getDouble(1)));
                }
                else if( mode.equalsIgnoreCase("ct") ) {
                    int warmth = (state.has("ct") ? state.getInt("ct") : 154);
                    int brightness = (state.has("bri") ? state.getInt("bri") : 0);

                    map.put("color", new Color(ColorMode.CT, warmth, brightness/254));
                }
                else {
                    float h = (state.has("hue") ? (float)state.getDouble("hue")/182.04f : 360f);
                    int s = (state.has("sat") ? (state.getInt("sat")*100)/255 : 100);

                    map.put("color", new Color(ColorMode.HSV, h, s));
                }
            }
            else {
                throw new CommunicationException("No color information is present");
            }
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
        map.put("deviceId", bulbId);
        map.put("supportsColorChanges", supportsColorChanges());
        map.put("supportsBrightnessChanges", supportsBrightnessChanges());
        map.put("name", name);
        map.put("description", name);
        map.put("deviceType", "light");
        ArrayList<ColorMode> modes = new ArrayList<ColorMode>();

        for( ColorMode m : hue.listNativeColorModes() ) {
            modes.add(m);
        }
        map.put("colorModes", modes);
    }

    public String toString() {
        return (name + " [#" + bulbId + "]");
    }
}
