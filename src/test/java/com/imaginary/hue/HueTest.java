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

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.Second;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.Future;

public class HueTest {
    static private final Logger logger = Logger.getLogger("com.imaginary.hue.test");

    private Iterable<Bulb> bulbList;

    private Hue getHue() {
        return new Hue(System.getProperty("ip"), System.getProperty("accessKey"));
    }

    @Before
    public void setUp() throws HueException {
        bulbList = getHue().listBulbs();
    }

    @Test
    public void listBulbs() throws HueException {
        int count = 0;

        for( Bulb bulb : bulbList ) {
            out("Bulb: " + bulb);
            count++;
        }
        out("Bulb count: " + count);
        Assert.assertTrue("No bulbs found for testing", count > 0);
    }

    @Test
    public void changeToBlue() throws HueException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Bulb bulb : bulbList ) {
            results.add(bulb.changeColorRGB(0, 0, 255, null));
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Boolean> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }

        for( Bulb bulb : bulbList ) {
            assertSimilar(bulb + " X is not properly blue", 0.1425f, bulb.getColorXY()[0]);
            assertSimilar(bulb + " Y is not properly blue", 0.1425f, bulb.getColorXY()[1]);
        }
    }

    @Test
    public void flipOff() throws HueException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Bulb bulb : bulbList ) {
            results.add(bulb.flipOff());
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Boolean> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }
        for( Bulb bulb : bulbList ) {
            Assert.assertFalse(bulb + " is still on", bulb.isOn());
        }
    }

    @Test
    public void fadeOn() throws HueException {
        ArrayList<Future<Integer>> results = new ArrayList<Future<Integer>>();
        TimePeriod<Minute> t = new TimePeriod<Minute>(1, TimePeriod.MINUTE);

        for( Bulb bulb : bulbList ) {
            results.add(bulb.fadeOn(t, 254));
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Integer> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }
        for( Bulb bulb : bulbList ) {
            Assert.assertTrue(bulb + " is still off", bulb.isOn());
            Assert.assertEquals(bulb + " has an invalid brightness", 254, bulb.getBrightness());
        }
    }

    @Test
    public void fadeOff() throws HueException {
        ArrayList<Future<Integer>> results = new ArrayList<Future<Integer>>();
        TimePeriod<Minute> t = new TimePeriod<Minute>(1, TimePeriod.MINUTE);

        for( Bulb bulb : bulbList ) {
            results.add(bulb.fadeOff(t));
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Integer> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }
        for( Bulb bulb : bulbList ) {
            int brightness = bulb.getBrightness();

            Assert.assertFalse(bulb + " is still on", bulb.isOn());
            Assert.assertTrue(bulb + " has an invalid brightness: " + brightness, brightness <= 1);
        }
    }

    @Test
    public void flipOn() throws HueException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Bulb bulb : bulbList ) {
            results.add(bulb.flipOn());
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Boolean> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }
        for( Bulb bulb : bulbList ) {
            Assert.assertTrue(bulb + " is still off", bulb.isOn());
        }
    }

    @Test
    public void changeToWhite() throws HueException {
        try {
            TimePeriod<Second> t = new TimePeriod<Second>(1, TimePeriod.SECOND);

            for( Bulb bulb : bulbList ) {
                waitFor(bulb.fadeOn(t, 254));
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }

        ArrayList<Future<Integer>> results = new ArrayList<Future<Integer>>();

        for( Bulb bulb : bulbList ) {
            results.add(bulb.changeWhite(500, 254, null));
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Integer> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }
        for( Bulb bulb : bulbList ) {
            Assert.assertEquals(bulb + " is not properly warm", 500, bulb.getWarmth());
        }
    }

    @Test
    public void changeToRed() throws HueException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Bulb bulb : bulbList ) {
            results.add(bulb.changeColorRGB(255, 0, 0, null));
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Boolean> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
        try { Thread.sleep(4000L); }
        catch( InterruptedException ignore ) { }
        for( Bulb bulb : bulbList ) {
            assertSimilar(bulb + " is not properly red", 0.4344f, bulb.getColorXY()[0]);
            assertSimilar(bulb + " is not properly red", 0.2219f, bulb.getColorXY()[1]);
        }
    }

    @Test
    public void strobe() throws HueException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        TimePeriod<Minute> duration = new TimePeriod<Minute>(1, TimePeriod.MINUTE);
        TimePeriod<Second> interval = new TimePeriod<Second>(1, TimePeriod.SECOND);

        for( Bulb bulb : bulbList ) {
            results.add(bulb.strobeRGB(interval, duration, new int[] { 255, 0, 0 }, new int[] { 0, 255, 0 }, new int[] { 0, 0, 255 }));
        }

        boolean done;

        do {
            done = true;
            try { Thread.sleep(1000L); }
            catch( InterruptedException ignore ) { }
            for( Future<Boolean> f : results ) {
                if( !f.isDone() ) {
                    done = false;
                    break;
                }
            }
        } while( !done );
    }

    private void assertSimilar(@Nonnull String message, float expected, float actual) {
        float diff = expected - actual;

        if( diff > 0.01f || diff < -0.01f ) {
            Assert.fail(message + " (expected: " + expected + ", got " + actual + ")");
        }
    }

    private void out(String msg) {
        if( logger.isDebugEnabled() ) {
            logger.debug("> " + msg);
        }
    }

    private void waitFor(Future<?> f) {
        while( !f.isDone() ) {
            try { Thread.sleep(1000L); }
            catch( InterruptedException e ) { }
        }
    }
}
