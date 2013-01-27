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

import com.imaginary.home.cloud.ControllerRelay;
import com.imaginary.home.cloud.PendingCommand;
import com.imaginary.home.cloud.api.APICall;
import com.imaginary.home.cloud.api.RestApi;
import com.imaginary.home.cloud.api.RestException;
import com.imaginary.home.cloud.device.Device;
import org.dasein.persist.PersistenceException;
import org.json.JSONArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/27/13 12:28 PM</p>
 *
 * @author George Reese
 */
public class CommandCall extends APICall {
    @Override
    public void get(@Nonnull String requestId, @Nullable String userId, @Nonnull String[] path, @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Map<String,Object> headers, @Nonnull Map<String,Object> parameters) throws RestException, IOException {
        try {
            if( userId != null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.USER_NOT_ALLOWED, "User keys cannot fetch commands");
            }
            String apiKey = (String)headers.get(RestApi.API_KEY);
            ControllerRelay relay = ControllerRelay.getRelay(apiKey);

            if( relay == null ) {
                throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RestException.INTERNAL_ERROR, "Relay was lost");
            }
            ArrayList<Map<String,Object>> list = new ArrayList<Map<String, Object>>();

            for( PendingCommand cmd : PendingCommand.getCommandsToSend(relay, true) ) {
                list.add(toJSON(cmd));
            }
            boolean hasCommands = PendingCommand.hasCommands(relay);

            resp.setHeader("x-imaginary-has-commands", String.valueOf(hasCommands));
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println((new JSONArray(list)).toString());
            resp.getWriter().flush();
        }
        catch( PersistenceException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RestException.INTERNAL_ERROR, e.getMessage());
        }
    }

    private Map<String,Object> toJSON(PendingCommand cmd) throws PersistenceException {
        HashMap<String,Object> json = new HashMap<String, Object>();

        json.put("id", cmd.getPendingCommandId());
        json.put("timeout", cmd.getTimeout());
        json.put("groupId", cmd.getGroupId());
        json.put("command", cmd.getCommand());
        ArrayList<Map<String,Object>> devices = new ArrayList<Map<String, Object>>();

        for( String id : cmd.getDeviceIds() ) {
            Device device = Device.getDevice(id);

            if( device != null ) {
                HashMap<String,Object> d = new HashMap<String, Object>();

                d.put("id", device.getVendorDeviceId());
                d.put("systemId", device.getHomeAutomationSystemId());
                devices.add(d);
            }
        }
        json.put("devices", devices);
        return json;
    }
}
