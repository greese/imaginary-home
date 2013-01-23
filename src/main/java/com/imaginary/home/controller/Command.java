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
package com.imaginary.home.controller;


import com.imaginary.home.lighting.Color;
import com.imaginary.home.lighting.ColorMode;
import com.imaginary.home.lighting.Light;
import com.imaginary.home.lighting.LightingService;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.time.Millisecond;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Command {
    private JSONObject arguments;
    private String     command;
    private String     commandId;
    private String[]   resourceIds;
    private String     systemId;
    private String     service;
    private long       timeout;

    public Command(JSONObject command) throws JSONException {
        commandId = UUID.randomUUID().toString();
        if( command.has("systemId") ) {
            systemId = command.getString("systemId");
        }
        if( command.has("service") ) {
            service = command.getString("service");
        }
        if( command.has("command") ) {
            this.command = command.getString("command");
        }
        if( command.has("arguments") ) {
            arguments = command.getJSONObject("arguments");
        }
        if( command.has("resourceIds") ) {
            JSONArray ids = command.getJSONArray("resourceIds");

            resourceIds = new String[ids.length()];
            for( int i=0; i<ids.length(); i++ ) {
                resourceIds[i] = ids.getString(i);
            }
        }
        if( command.has("timeout") ) {
            timeout = command.getLong("timeout");
        }
        else {
            timeout = CalendarWrapper.MINUTE * 5L;
        }
    }

    private boolean execute() throws CommunicationException, ControllerException {
        HomeAutomationSystem system = HomeController.getInstance().getSystem(systemId);

        if( system == null ) {
            throw new ControllerException("No such home automation system: " + systemId);
        }
        ArrayList<Future<Boolean>> results = new ArrayList<Future<Boolean>>();

        if( service.equalsIgnoreCase("lighting") ) {
            if( !(system instanceof LightingService) ) {
                throw new ControllerException("No lighting support exists in " + system);
            }
            LightingService svc = (LightingService)system;
            for( Light light : svc.listLights() ) {
                boolean included = (resourceIds == null);

                if( !included ) {
                    for( String id : resourceIds ) {
                        if( id.equals(light.getProviderId()) ) {
                            included = true;
                            break;
                        }
                    }
                }
                if( included ) {
                    results.add(executeLighting(light));
                }
            }
        }
        else {
            // TODO: support extensible capabilities
        }
        for( boolean b : waitFor(results) ) {
            if( b ) {
                return true;
            }
        }
        return false;
    }

    private @Nullable Future<Boolean> executeLighting(@Nonnull Light light) throws CommunicationException, ControllerException {
        try {
            if( command.equals("flipOn") ) {
                return light.flipOn();
            }
            else if( command.equals("flipOff") ) {
                return light.flipOff();
            }
            else if( command.equals("strobe") ) {
                TimePeriod<?> interval, duration = null;
                Color[] colors;

                if( arguments.has("interval") ) {
                    interval = TimePeriod.valueOf(arguments.getString("interval"));
                }
                else {
                    interval = new TimePeriod<Millisecond>(100, TimePeriod.MILLISECOND);
                }
                if( arguments.has("duration") ) {
                    duration = TimePeriod.valueOf(arguments.getString("duration"));
                }
                if( arguments.has("colors") ) {
                    JSONArray list = arguments.getJSONArray("colors");

                    colors = new Color[list.length()];
                    for( int i=0; i<list.length(); i++ ) {
                        JSONObject c = list.getJSONObject(i);
                        ColorMode mode = ColorMode.valueOf(c.getString("mode"));
                        JSONArray parts = c.getJSONArray("components");
                        float[] components = new float[parts.length()];

                        for( int j=0; j<parts.length(); j++ ) {
                            components[j] = (float)parts.getDouble(j);
                        }
                        colors[i] = new Color(mode, components);
                    }
                }
                else {
                    return null;
                }
                return light.strobe(interval, duration, colors);
            }
            else if( command.equals("changeColor") ) {
                TimePeriod<?> transition = null;
                Color color;

                if( arguments.has("color") ) {
                    JSONObject c = arguments.getJSONObject("color");
                    ColorMode mode = ColorMode.valueOf(c.getString("mode"));
                    JSONArray parts = c.getJSONArray("components");
                    float[] components = new float[parts.length()];

                    for( int j=0; j<parts.length(); j++ ) {
                        components[j] = (float)parts.getDouble(j);
                    }
                    color = new Color(mode, components);
                }
                else {
                    throw new ControllerException("No color was specified for the color command");
                }
                if( arguments.has("transitionTime") ) {
                    transition = TimePeriod.valueOf(arguments.getString("duration"));
                }
                return light.changeColor(color, transition);
            }
            else if( command.equals("fadeOn") ) {
                TimePeriod<?> transition;
                float brightness = -1f;

                if( arguments.has("transitionTime") ) {
                    transition = TimePeriod.valueOf(arguments.getString("duration"));
                }
                else {
                    transition = new TimePeriod<Minute>(1, TimePeriod.MINUTE);
                }
                if( arguments.has("brightness") ) {
                    brightness = (float)arguments.getDouble("brightness");
                }
                if( brightness < 0f ) {
                    return light.fadeOn(transition);
                }
                else {
                    return light.fadeOn(transition, brightness);
                }
            }
            else if( command.equals("fadeOff") ) {
                TimePeriod<?> transition;

                if( arguments.has("transitionTime") ) {
                    transition = TimePeriod.valueOf(arguments.getString("duration"));
                }
                else {
                    transition = new TimePeriod<Minute>(1, TimePeriod.MINUTE);
                }
                return light.fadeOff(transition);
            }
            else {
                throw new ControllerException("Unknown lighting command: " + command);
            }
        }
        catch( JSONException e ) {
            throw new ControllerException("Invalid JSON in command: " + e.getMessage());
        }
    }

    public @Nonnull String getCommandId() {
        return commandId;
    }

    public Future<Boolean> start() {
        return HomeController.executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return execute();
            }
        });
    }

    private @Nonnull boolean[] waitFor(@Nonnull ArrayList<Future<Boolean>> threads) throws ControllerException, CommunicationException {
        boolean[] results = new boolean[threads.size()];
        long t = System.currentTimeMillis() + timeout;
        boolean done = true;
        int i=0;

        for( Future<Boolean> f : threads ) {
            done = false;
            while( t > System.currentTimeMillis() ) {
                try {
                    if( f.isDone() ) {
                        results[i++] = f.get();
                        done = true;
                        break;
                    }
                    try { Thread.sleep(500L); }
                    catch( InterruptedException ignore ) { }
                }
                catch( ExecutionException e ) {
                    if( e.getCause() instanceof ControllerException ) {
                        throw (ControllerException)e.getCause();
                    }
                    else if( e.getCause() instanceof CommunicationException ) {
                        throw (CommunicationException)e.getCause();
                    }
                    throw new ControllerException(e);
                }
                catch( InterruptedException e ) {
                    throw new ControllerException(e);
                }
            }
            if( !done && !f.isDone() ) {
                f.cancel(true);
            }
        }
        if( !done ) {
            throw new ControllerException("Action timed out");
        }
        return results;
    }
}
