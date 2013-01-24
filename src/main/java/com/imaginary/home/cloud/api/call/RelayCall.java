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

import com.imaginary.home.cloud.Configuration;
import com.imaginary.home.cloud.ControllerRelay;
import com.imaginary.home.cloud.Location;
import com.imaginary.home.cloud.api.APICall;
import com.imaginary.home.cloud.api.RestException;
import org.dasein.persist.PersistenceException;
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
 * <p>Created by George Reese: 1/24/13 3:07 PM</p>
 *
 * @author George Reese
 */
public class RelayCall extends APICall {
    @Override
    public void post(@Nonnull String requestId, @Nullable String userId, @Nonnull String[] path, @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Map<String,Object> headers, @Nonnull Map<String,Object> parameters) throws RestException, IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
            StringBuilder source = new StringBuilder();
            String line;

            while( (line = reader.readLine()) != null ) {
                source.append(line);
                source.append(" ");
            }
            JSONObject object = new JSONObject(source.toString());

            if( !object.has("pairingCode") || object.isNull("pairingCode") ) {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.MISSING_PAIRING_CODE, "Pairing code is missing");
            }
            String code = object.getString("pairingCode");

            if( code.equalsIgnoreCase("null") ) {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.MISSING_PAIRING_CODE, "Pairing code is missing");
            }
            Location location = Location.findForPairing(code);

            if( location == null ) {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_PAIRING_CODE, "Invalid pairing code; pairing did not occur");
            }
            String relayName;

            if( object.has("relayName") && !object.isNull("relayName") ) {
                relayName = object.getString("relayName");
            }
            else {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.MISSING_DATA, "Missing relay name from JSON");
            }
            ControllerRelay relay = location.pair(code, relayName);

            if( relay == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.PAIRING_FAILURE, "Pairing failed due to an invalid pairing code or expired pairing code");
            }
            HashMap<String,Object> json = new HashMap<String, Object>();

            json.put("apiKeyId", relay.getControllerRelayId());
            json.put("apiKeySecret", Configuration.decrypt(location.getLocationId(), relay.getApiKeySecret()));
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println((new JSONObject(json)).toString());
            resp.getWriter().flush();
        }
        catch( JSONException e ) {
            throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_JSON, "Invalid JSON in body");
        }
        catch( PersistenceException e ) {
            e.printStackTrace();
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RestException.INTERNAL_ERROR, "Internal database error");
        }
    }
}
