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
import com.imaginary.home.lighting.Color;
import com.imaginary.home.lighting.Light;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.Second;
import org.dasein.util.uom.time.TimePeriod;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Future;

public class HueTest {
    static private final Logger logger = Logger.getLogger("com.imaginary.home.device.hue.test");

    static public final Color RED   = Color.getRGB255(255,0,0);
    static public final Color GREEN = Color.getRGB255(0,255,0);
    static public final Color BLUE  = Color.getRGB255(0,0,255);

    private Iterable<Light> bulbList;

    private Hue getHue() {
        return new Hue(System.getProperty("ip"), System.getProperty("accessKey"));
    }

    @Before
    public void setUp() throws CommunicationException {
        bulbList = getHue().listLights();
    }

    @Test
    public void listBulbs() throws CommunicationException {
        int count = 0;

        for( Light bulb : bulbList ) {
            out("Bulb: " + bulb);
            count++;
        }
        out("Bulb count: " + count);
        Assert.assertTrue("No bulbs found for testing", count > 0);
    }

    @Test
    public void bulbInfo() throws CommunicationException {
        Iterator<Light> it = bulbList.iterator();

        if( it.hasNext() ) {
            Light bulb = it.next();

            out("ID:                     " + bulb.getProviderId());
            out("Name:                   " + bulb.getName());
            out("Model:                  " + bulb.getModel());
            out("On:                     " + bulb.isOn());
            out("Color mode:             " + bulb.getColorMode());
            out("Color:                  " + bulb.getColor());
            out("Brightness:             " + bulb.getBrightness() + "%");
            out("Supports color:         " + bulb.supportsColor());
            out("Supports color changes: " + bulb.supportsColorChanges());
            return;
        }
        Assert.fail("Unable to test bulb info for lack of a bulb to test");
    }

    @Test
    public void changeToBlue() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Light bulb : bulbList ) {
            results.add(bulb.changeColor(BLUE, null));
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
    }

    @Test
    public void flipOff() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Light bulb : bulbList ) {
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
    }

    @Test
    public void fadeOn() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        TimePeriod<Minute> t = new TimePeriod<Minute>(1, TimePeriod.MINUTE);

        for( Light bulb : bulbList ) {
            results.add(bulb.fadeOn(t, 100f));
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
    }

    @Test
    public void fadeOff() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        TimePeriod<Minute> t = new TimePeriod<Minute>(1, TimePeriod.MINUTE);

        for( Light bulb : bulbList ) {
            results.add(bulb.fadeOff(t));
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
    }

    @Test
    public void flipOn() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Light bulb : bulbList ) {
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
    }

    @Test
    public void changeToWhite() throws CommunicationException {
        try {
            TimePeriod<Second> t = new TimePeriod<Second>(1, TimePeriod.SECOND);

            for( Light bulb : bulbList ) {
                waitFor(bulb.fadeOn(t, 100f));
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }

        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Light bulb : bulbList ) {
            results.add(bulb.changeWhite(500, 100f, null));
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
    }

    @Test
    public void changeToRed() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        for( Light bulb : bulbList ) {
            results.add(bulb.changeColor(RED, null));
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
    }

    @Test
    public void strobe() throws CommunicationException {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();
        TimePeriod<Minute> duration = new TimePeriod<Minute>(1, TimePeriod.MINUTE);
        TimePeriod<Second> interval = new TimePeriod<Second>(1, TimePeriod.SECOND);

        for( Light bulb : bulbList ) {
            results.add(bulb.strobe(interval, duration, RED, GREEN, BLUE));
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
