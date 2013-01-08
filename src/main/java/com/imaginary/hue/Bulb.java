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
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class Bulb {
    static public final Logger logger = Hue.getLogger(Bulb.class);

    private String bulbId;
    private Hue    hue;
    private String name;

    public Bulb(@Nonnull Hue hue, @Nonnull String id, @Nonnull String name) {
        bulbId = id;
        this.hue = hue;
        this.name = name;
    }

    public @Nonnull Future<Boolean> changeColor(final @Nonnull ColorSpace fromColorSpace, final @Nonnull float[] nativeValues, final @Nullable TimePeriod<?> transitionTime) {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long millis = (transitionTime == null ? 0L : transitionTime.convertTo(TimePeriod.MILLISECOND).longValue());

                return changeColor(fromColorSpace, nativeValues, millis);
            }
        });
    }

    private boolean changeColor(@Nonnull ColorSpace fromColorSpace, @Nonnull float[] nativeValues, @Nonnegative long millis) throws HueException {
        if( fromColorSpace.getType() == ColorSpace.TYPE_HSV ) {
            Map<String,Object> state = new HashMap<String,Object>();

            state.put("on", true);
            state.put("hue", 182.04 * nativeValues[0]);
            state.put("sat", nativeValues[1] * 254);
            if( millis >= 100 ) {
                state.put("transitiontime", millis/100);
            }

            String resource = "lights/" + bulbId + "/state";
            HueMethod method = new HueMethod(hue);

            method.put(resource, new JSONObject(state));
        }
        else {
            Map<String,Object> state = new HashMap<String,Object>();
            float[] tmp = fromColorSpace.toCIEXYZ(nativeValues);

            state.put("on", true);
            state.put("xy", new float[] { tmp[0], tmp[1] });
            if( millis >= 100 ) {
                state.put("transitiontime", millis/100);
            }

            String resource = "lights/" + bulbId + "/state";
            HueMethod method = new HueMethod(hue);

            method.put(resource, new JSONObject(state));
        }
        return true;
    }

    private @Nonnull Future<Boolean> changeColorHSV(final @Nonnegative float hueValue, final @Nonnegative int saturation, final @Nullable TimePeriod<?> transitionTime) throws HueException {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long millis = (transitionTime == null ? 0L : transitionTime.convertTo(TimePeriod.MILLISECOND).longValue());
                Map<String,Object> state = new HashMap<String,Object>();
                float h = hueValue;
                int s = saturation;

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
                state.put("on", true);
                state.put("hue", 182.04 * h);
                state.put("sat", s * 254);
                if( millis >= 100 ) {
                    state.put("transitiontime", millis/100);
                }

                String resource = "lights/" + bulbId + "/state";
                HueMethod method = new HueMethod(hue);

                method.put(resource, new JSONObject(state));
                return true;
            }
        });
    }

    public @Nonnull Future<Boolean> changeColorRGB(@Nonnegative int r, @Nonnegative int g, @Nonnegative int b, @Nullable TimePeriod<?> transitionTime) throws HueException {
        return changeColor(ColorSpace.getInstance(ColorSpace.CS_sRGB), new float[] { ((float)r)/255f, ((float)g)/255f, ((float)b)/255f }, transitionTime);
    }

    public @Nonnull Future<Boolean> changeColorXY(@Nonnegative float x, @Nonnegative float y, @Nullable TimePeriod<?> transitionTime) throws HueException {
        return changeColor(ColorSpace.getInstance(ColorSpace.CS_CIEXYZ), new float[] { x, y }, transitionTime);
    }

    public @Nonnull Future<Integer> changeWhite(final @Nonnegative int warmthInMireds, final @Nonnegative int brightness, final @Nullable TimePeriod<?> transitionTime) {
        return Hue.executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws HueException {
                long millis = (transitionTime == null ? 0L : transitionTime.convertTo(TimePeriod.MILLISECOND).longValue());

                return changeWhite(warmthInMireds, brightness, millis);
            }
        });
    }

    private int changeWhite(int warmth, int brightness, @Nonnegative long millis) throws HueException {
        Map<String,Object> state = new HashMap<String,Object>();

        state.put("on", true);
        state.put("ct", warmth);
        state.put("bri", brightness);
        if( millis >= 100 ) {
            state.put("transitiontime", millis/100);
        }

        String resource = "lights/" + bulbId + "/state";
        HueMethod method = new HueMethod(hue);

        method.put(resource, new JSONObject(state));
        return getWarmth();
    }

    private int[] convertHSVToRGB(JSONObject state) throws HueException {
        try {
            float h = (float)state.getDouble("hue");
            int s = state.getInt("sat");

            Color c = new Color(Color.HSBtoRGB(h/65535f, (s*100f)/254f, 1f));

            return new int[] { c.getRed(), c.getGreen(), c.getBlue() };
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    private float[] convertHSVToXY(JSONObject state) throws HueException {
        try {
            float h = (float)state.getDouble("hue");
            int s = state.getInt("sat");

            // TODO: convert to XY
            return new float[] { h, s};
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    private int[] convertXYToRGB(JSONObject state) throws HueException {
        try {
            JSONArray arr = state.getJSONArray("xy");
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
            float x = (float)arr.getDouble(0);
            float y = (float)arr.getDouble(1);
            float z = 1.0f - (x+y);

            float[] tmp = cs.toRGB(new float[] { x, y, z });
            return new int[] { (int)tmp[0]*255, (int)tmp[1]*255, (int)tmp[2]*255 };
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    private float[] convertXYToHSV(JSONObject state) throws HueException {
        try {
            JSONArray arr = state.getJSONArray("xy");
            float x = (float)arr.getDouble(0);
            float y = (float)arr.getDouble(1);

            // TODO: convert to HSV
            return new float[] { x, y};
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public @Nonnull Future<Integer> fadeOff(final @Nonnull  TimePeriod<?> transitionTime) throws HueException {
        return Hue.executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws HueException {
                return fade(transitionTime, 0);
            }
        });
    }

    public @Nonnull Future<Boolean> flipOff() {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                flip(false);
                return true;
            }
        });
    }

    public void fadeOn(@Nonnull TimePeriod<?> transitionTime) throws HueException {
        fadeOn(transitionTime, getBrightness());
    }

    public @Nonnull Future<Integer> fadeOn(final @Nonnull TimePeriod<?> transitionTime, final @Nonnegative int targetBrightness) {
        return Hue.executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws HueException {
                return fade(transitionTime, targetBrightness);
            }
        });
    }

    private @Nonnegative int fade(@Nonnull TimePeriod<?> transitionTime, @Nonnegative int targetBrightness) throws HueException {
        logger.debug("Fading to " + targetBrightness + " over " + transitionTime + " for " + bulbId);
        if( targetBrightness < 0 ) {
            targetBrightness = 0;
        }
        int currentBrightness = getBrightness();
        boolean on = isOn();

        if( (on && targetBrightness == currentBrightness) || (!on && targetBrightness == 0) ) {
            if( logger.isDebugEnabled() ) {
                logger.debug("Nothing to do for " + bulbId);
            }
            return getBrightness();
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
        int brightness = getBrightness();

        if( logger.isDebugEnabled() ) {
            logger.debug(bulbId + ".brightness=" + brightness);
        }
        return brightness;
    }

    public @Nonnull Future<Boolean> flipOn() {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                flip(true);
                return true;
            }
        });
    }

    private void flip(boolean on) throws HueException {
        if( isOn() == on ) {
            return;
        }
        Map<String,Object> state = new HashMap<String,Object>();

        state.put("on", on);
        String resource = "lights/" + bulbId + "/state";
        HueMethod method = new HueMethod(hue);

        method.put(resource, new JSONObject(state));
    }

    public @Nonnegative int getBrightness() throws HueException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return 0;
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( !state.has("bri") ) {
                return 0;
            }
            return state.getInt("bri");
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public @Nonnull String getBulbId() {
        return bulbId;
    }

    public float[] getColorHSV() throws HueException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return new float[] { 360f, 100f };
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( state.has("colormode") && state.getString("colormode").equalsIgnoreCase("xy") ) {
                return convertXYToHSV(state);
            }
            else {
                float h = (state.has("hue") ? (float)state.getDouble("hue")/182.04f : 360f);
                int s = (state.has("sat") ? (state.getInt("sat")*100)/255 : 100);

                return new float[] { h, s };
            }
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public int[] getColorRGB() throws HueException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return new int[] { 255, 255, 255 };
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( state.has("colormode") && state.getString("colormode").equalsIgnoreCase("hue") ) {
                return convertHSVToRGB(state);
            }
            else {
                return convertXYToRGB(state);
            }
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public float[] getColorXY() throws HueException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return new float[] { 0.4448f, 0.4066f };
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( state.has("colormode") && state.getString("colormode").equalsIgnoreCase("hue") ) {
                return convertHSVToXY(state);
            }
            else {
                JSONArray arr = state.getJSONArray("xy");

                return new float[] { (float)arr.getDouble(0), (float)arr.getDouble(1) };
            }
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public @Nonnull String getName() {
        return name;
    }

    public @Nonnegative int getWarmth() throws HueException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return 0;
        }
        try {
            JSONObject state = json.getJSONObject("state");

            if( !state.has("ct") ) {
                return 0;
            }
            return state.getInt("ct");
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public boolean isColor() throws HueException {
        String resource = "lights/" + bulbId;
        HueMethod method = new HueMethod(hue);

        JSONObject json = method.get(resource);

        if( json == null || !json.has("state") ) {
            return false;
        }
        try {
            JSONObject state = json.getJSONObject("state");

            return (state.has("colormode") && !state.getString("colormode").equalsIgnoreCase("ct"));
        }
        catch( JSONException e ) {
            throw new HueException(e);
        }
    }

    public boolean isOn() throws HueException {
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

    public Future<Boolean> strobeHSV(final @Nonnull TimePeriod<?> interval, final @Nullable TimePeriod<?> duration, final @Nonnull float[] ... hsvValues) {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long end = (duration == null ? 0L : (System.currentTimeMillis() + duration.convertTo(TimePeriod.MILLISECOND).longValue()));
                long millis = interval.convertTo(TimePeriod.MILLISECOND).longValue();
                int currentColor = 0;

                if( millis < 100 ) {
                    millis = 100;
                }
                while( end < 1 || System.currentTimeMillis() < end ) {
                    float[] color = hsvValues[currentColor++];

                    if( currentColor >= hsvValues.length ) {
                        currentColor = 0;
                    }
                    changeColorHSV(color[0], (int)color[1], null);
                    try { Thread.sleep(millis); }
                    catch( InterruptedException ignore ) { }
                }
                return true;
            }
        });
    }

    public Future<Boolean> strobeRGB(final @Nonnull TimePeriod<?> interval, final @Nullable TimePeriod<?> duration, final @Nonnull int[] ... rgbValues) {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long end = (duration == null ? 0L : (System.currentTimeMillis() + duration.convertTo(TimePeriod.MILLISECOND).longValue()));
                long millis = interval.convertTo(TimePeriod.MILLISECOND).longValue();
                int currentColor = 0;

                if( millis < 100 ) {
                    millis = 100;
                }
                while( end < 1 || System.currentTimeMillis() < end ) {
                    int[] color = rgbValues[currentColor++];

                    if( currentColor >= rgbValues.length ) {
                        currentColor = 0;
                    }
                    changeColorRGB(color[0], color[1], color[2], null);
                    try { Thread.sleep(millis); }
                    catch( InterruptedException ignore ) { }
                }
                return true;
            }
        });
    }

    public Future<Boolean> strobeXY(final @Nonnull TimePeriod<?> interval, final @Nullable TimePeriod<?> duration, final @Nonnull float[] ... xyValues) {
        return Hue.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HueException {
                long end = (duration == null ? 0L : (System.currentTimeMillis() + duration.convertTo(TimePeriod.MILLISECOND).longValue()));
                long millis = interval.convertTo(TimePeriod.MILLISECOND).longValue();
                int currentColor = 0;

                if( millis < 100 ) {
                    millis = 100;
                }
                while( end < 1 || System.currentTimeMillis() < end ) {
                    float[] color = xyValues[currentColor++];

                    if( currentColor >= xyValues.length ) {
                        currentColor = 0;
                    }
                    changeColorXY(color[0], color[1], null);
                    try { Thread.sleep(millis); }
                    catch( InterruptedException ignore ) { }
                }
                return true;
            }
        });
    }

    public boolean supportsColor() {
        return true;
    }

    public String toString() {
        return (name + " [#" + bulbId + "]");
    }
}
