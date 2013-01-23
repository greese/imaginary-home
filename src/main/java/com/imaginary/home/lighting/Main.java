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
package com.imaginary.home.lighting;

import com.imaginary.home.device.hue.Hue;
import org.dasein.util.uom.time.TimePeriod;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Future;

public class Main {

    static public void main(String ... args) throws Exception {
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        try {
            if( args.length < 2 ) {
                System.out.println("java " + Main.class.getName() + " [ip] [command]");
                System.out.println("------------------------------------------------");
                System.out.println("Valid commands are:");
                System.out.println("\tpair [app name]");
                System.out.println("\tflipon [access key] [bulb #|all]");
                System.out.println("\tflipoff [access key] [bulb #|all]");
                System.out.println("\tfadeon [access key] [bulb #|all] [transition time] [brightness]");
                System.out.println("\tfadeoff [access key] [bulb #|all] [transition time]");
                System.out.println("\tcolor [access key] [bulb #|all] [transition time] [color]");
                System.out.println("\tstrobe [access key] [bulb #|all] [interval time] [duration time] [color] ...");
                System.out.println("");
                System.out.println("Examples:");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 pair \"My App\"");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 flipon abcdef12345 2");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 flipoff abcdef12345 2");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 fadeon abcdef12345 3 20s 100");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 fadeoff abcdef12345 3 20s");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 color abcdef12345 4 20s RGB:100:0:0");
                System.out.println("\tjava " + Main.class.getName() + " 192.168.1.100 strobe abcdef12345 4 500ms 5m RGB:100:0:0 RGB:0:100:0 RGB:0:0:100");
                System.out.println("------------------------------------------------");
            }
            else {
                String command = args[1];
                String ip = args[0];

                if( command.equals("pair") ) {
                    Hue hue = new Hue(ip, "");
                    Properties p = hue.pair(args.length < 3 ? "Imaginary Home" : args[2]);

                    System.out.println("Key: " + p.get("accessKey"));
                }
                else {
                    if( args.length < 3 ) {
                        System.out.println("You must specify an access key for communicating with the hub");
                    }
                    else {
                        Hue hue = new Hue(ip, args[2]);

                        if( command.equals("flipon") ) {
                            String which = (args.length < 4 ? "all" : args[3]);

                            if( which.equals("all") ) {
                                for( Light bulb : hue.listLights() ) {
                                    results.add(bulb.flipOn());
                                }
                            }
                            else {
                                boolean found = false;

                                for( Light bulb : hue.listLights() ) {
                                    if( bulb.getProviderId().equals(which) ) {
                                        results.add(bulb.flipOn());
                                        found = true;
                                    }
                                }
                                if( !found ) {
                                    System.out.println("No such lightbulb: " + which);
                                }
                            }
                        }
                        else if( command.equals("flipoff") ) {
                            String which = (args.length < 4 ? "all" : args[3]);

                            if( which.equals("all") ) {
                                for( Light bulb : hue.listLights() ) {
                                    results.add(bulb.flipOff());
                                }
                            }
                            else {
                                boolean found = false;

                                for( Light bulb : hue.listLights() ) {
                                    if( bulb.getProviderId().equals(which) ) {
                                        results.add(bulb.flipOff());
                                        found = true;
                                    }
                                }
                                if( !found ) {
                                    System.out.println("No such lightbulb: " + which);
                                }
                            }
                        }
                        else if( command.equals("fadeon") ) {
                            String which = (args.length < 4 ? "all" : args[3]);
                            String t = (args.length < 5 ? "10s" : args[4]);
                            String b = (args.length < 6 ? "100" : args[5]);

                            TimePeriod transition = TimePeriod.valueOf(t);
                            float brightness = Float.parseFloat(b);

                            if( which.equals("all") ) {
                                for( Light bulb : hue.listLights() ) {
                                    results.add(bulb.fadeOn(transition, brightness));
                                }
                            }
                            else {
                                boolean found = false;

                                for( Light bulb : hue.listLights() ) {
                                    if( bulb.getProviderId().equals(which) ) {
                                        results.add(bulb.fadeOn(transition, brightness));
                                        found = true;
                                    }
                                }
                                if( !found ) {
                                    System.out.println("No such lightbulb: " + which);
                                }
                            }
                        }
                        else if( command.equals("fadeoff") ) {
                            String which = (args.length < 4 ? "all" : args[3]);
                            String t = (args.length < 5 ? "10s" : args[4]);

                            TimePeriod transition = TimePeriod.valueOf(t);

                            if( which.equals("all") ) {
                                for( Light bulb : hue.listLights() ) {
                                    results.add(bulb.fadeOff(transition));
                                }
                            }
                            else {
                                boolean found = false;

                                for( Light bulb : hue.listLights() ) {
                                    if( bulb.getProviderId().equals(which) ) {
                                        results.add(bulb.fadeOff(transition));
                                        found = true;
                                    }
                                }
                                if( !found ) {
                                    System.out.println("No such lightbulb: " + which);
                                }
                            }
                        }
                        else if( command.equals("color") ) {
                            String which = (args.length < 4 ? "all" : args[3]);
                            String t = (args.length < 5 ? "100ms" : args[4]);
                            Color color;

                            if( args.length < 6 ) {
                                color = Color.getRGB255(255, 0, 0);
                            }
                            else {
                                String[] tmp = args[5].split(":");

                                if( tmp.length < 2 ) {
                                    System.err.println("Invalid color: " + args[5] + " -> format: MODE:component1:...:componentn");
                                    return;
                                }
                                try {
                                    ColorMode mode = ColorMode.valueOf(tmp[0]);
                                    float[] components = new float[tmp.length-1];

                                    for( int i=0; i<components.length; i++ ) {
                                        components[i] = Float.parseFloat(tmp[i+1]);
                                    }
                                    color = new Color(mode, components);
                                }
                                catch( Throwable err ) {
                                    System.err.println("Invalid color: " + args[5] + " -> format: MODE:component1:...:componentn");
                                    return;
                                }
                            }
                            TimePeriod transition = TimePeriod.valueOf(t);

                            if( which.equals("all") ) {
                                for( Light bulb : hue.listLights() ) {
                                    results.add(bulb.changeColor(color, transition));
                                }
                            }
                            else {
                                boolean found = false;

                                for( Light bulb : hue.listLights() ) {
                                    if( bulb.getProviderId().equals(which) ) {
                                        results.add(bulb.changeColor(color, transition));
                                        found = true;
                                    }
                                }
                                if( !found ) {
                                    System.out.println("No such lightbulb: " + which);
                                }
                            }
                        }
                        else if( command.equals("strobe") ) {
                            String which = (args.length < 4 ? "all" : args[3]);
                            String t = (args.length < 5 ? "100ms" : args[4]);
                            String d = (args.length < 6 ? "2m" : args[5]);
                            Color[] colors;

                            if( args.length < 7 ) {
                                colors = new Color[] { Color.getRGB255(255, 0, 0), Color.getRGB255(0, 255, 0), Color.getRGB255(0,0,255) };
                            }
                            else {
                                colors = new Color[args.length-6];
                                for( int idx=0; idx<colors.length; idx++ ) {
                                    String[] tmp = args[6+idx].split(":");

                                    if( tmp.length < 2 ) {
                                        System.err.println("Invalid color " + (idx+1) + ": " + args[6+idx] + " -> format: MODE:component1:...:componentn");
                                        return;
                                    }
                                    try {
                                        ColorMode mode = ColorMode.valueOf(tmp[0]);
                                        float[] components = new float[tmp.length-1];

                                        for( int i=0; i<components.length; i++ ) {
                                            components[i] = Float.parseFloat(tmp[i+1]);
                                        }
                                        colors[idx] = new Color(mode, components);
                                    }
                                    catch( Throwable err ) {
                                        System.err.println("Invalid color " + (idx+1) + ": " + args[6+idx] + " -> format: MODE:component1:...:componentn");
                                        return;
                                    }
                                }
                            }
                            TimePeriod interval = TimePeriod.valueOf(t);
                            TimePeriod duration = TimePeriod.valueOf(d);

                            if( which.equals("all") ) {
                                for( Light bulb : hue.listLights() ) {
                                    results.add(bulb.strobe(interval, duration, colors));
                                }
                            }
                            else {
                                boolean found = false;

                                for( Light bulb : hue.listLights() ) {
                                    if( bulb.getProviderId().equals(which) ) {
                                        results.add(bulb.strobe(interval, duration, colors));
                                        found = true;
                                    }
                                }
                                if( !found ) {
                                    System.out.println("No such lightbulb: " + which);
                                }
                            }
                        }
                    }
                }
            }
        }
        finally {
            boolean done;

            do {
                try { Thread.sleep(500L); }
                catch( InterruptedException ignore ) { }
                done = true;
                for( Future<Boolean> f : results ) {
                    if( !f.isDone() ) {
                        done = false;
                        break;
                    }
                }
            } while( !done );
        }
    }
}
