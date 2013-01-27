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

package com.imaginary.home.cloud.api.call;

import com.imaginary.home.cloud.PendingCommand;
import com.imaginary.home.cloud.api.APICall;
import com.imaginary.home.cloud.api.RestException;
import com.imaginary.home.cloud.device.Device;
import org.dasein.persist.PersistenceException;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/27/13 11:05 AM</p>
 *
 * @author George Reese
 */
public class DeviceCall extends APICall {
    @Override
    public void put(@Nonnull String requestId, @Nullable String userId, @Nonnull String[] path, @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Map<String,Object> headers, @Nonnull Map<String,Object> parameters) throws RestException, IOException {
        try {
            String deviceId = (path.length > 1 ? path[1] : null);
            Device device = null;

            if( deviceId != null ) {
                device = Device.getDevice(deviceId);
            }
            if( device == null ) {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_OBJECT, "The device " + deviceId + " does not exist.");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
            StringBuilder source = new StringBuilder();
            String line;

            while( (line = reader.readLine()) != null ) {
                source.append(line);
                source.append(" ");
            }
            JSONObject object = new JSONObject(source.toString());
            String action;

            if( object.has("action") && !object.isNull("action") ) {
                action = object.getString("action");
            }
            else {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_ACTION, "An invalid action was specified (or not specified) in the PUT");
            }
            if( action.equals("flipOn") || action.equals("flipOff") ) {
                HashMap<String,Object> json = new HashMap<String, Object>();
                TimePeriod p = null;

                if( object.has("timeout") && !object.isNull("timeout") ) {
                    try {
                        p = TimePeriod.valueOf(object.getString("timeout"));
                    }
                    catch( Throwable ignore ) {
                        // ignore
                    }
                }
                if( p == null ) {
                    p = new TimePeriod<Minute>(5, TimePeriod.MINUTE);
                }
                json.put("command", action);
                json.put("arguments", new HashMap<String,Object>());
                PendingCommand.queue(p, new String[] { (new JSONObject(json)).toString() }, device);
            }
            /*
            else if( action.equalsIgnoreCase("modify") ) {
                if( object.has("device") ) {
                    object = object.getJSONObject("device");
                }
                else {
                    throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_PUT, "No device was specified in the PUT");
                }
                String name, description;
                TimeZone timeZone;

                if( object.has("name") && !object.isNull("name") ) {
                    name = object.getString("name");
                }
                else {
                    name = location.getName();
                }
                if( object.has("description") && !object.isNull("description") ) {
                    description = object.getString("description");
                }
                else {
                    description = location.getName();
                }
                if( object.has("timeZone") && !object.isNull("timeZone") ) {
                    String tz = object.getString("timeZone");

                    timeZone = TimeZone.getTimeZone(tz);
                }
                else {
                    timeZone = location.getTimeZone();
                }
                location.modify(name, description, timeZone);
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            */
            else {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_ACTION, "The action " + action + " is not a valid action.");
            }
        }
        catch( JSONException e ) {
            throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_JSON, "Invalid JSON in request");
        }
        catch( PersistenceException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RestException.INTERNAL_ERROR, e.getMessage());
        }
    }
}
