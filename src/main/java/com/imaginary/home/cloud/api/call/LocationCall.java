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
import com.imaginary.home.cloud.Location;
import com.imaginary.home.cloud.api.APICall;
import com.imaginary.home.cloud.api.RestApi;
import com.imaginary.home.cloud.api.RestException;
import com.imaginary.home.cloud.user.User;
import com.imaginary.home.controller.CloudService;
import com.imaginary.home.controller.CommunicationException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.dasein.persist.PersistenceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * API call for access to {@link com.imaginary.home.cloud.Location} resources.
 * <p>Created by George Reese: 1/23/13 9:30 PM</p>
 * @author George Reese
 */
public class LocationCall extends APICall {
    @Override
    public void get(@Nonnull String requestId, @Nullable String userId, @Nonnull String[] path, @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Map<String,Object> headers, @Nonnull Map<String,Object> parameters) throws RestException, IOException {
        try {
            String locationId = (path.length > 1 ? path[1] : null);

            if( locationId != null ) {
                Location location = Location.getLocation(locationId);

                if( location == null ) {
                    throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_OBJECT, "The location " + locationId + " does not exist.");
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println((new JSONObject(toJSON(location))).toString());
                resp.getWriter().flush();
            }
            else {
                Collection<Location> locations;

                if( userId == null ) {
                    String apiKey = (String)headers.get(RestApi.API_KEY);
                    Location location = null;

                    if( apiKey != null ) {
                        ControllerRelay relay = ControllerRelay.getRelay(apiKey);

                        if( relay != null ) {
                            location = relay.getLocation();
                        }
                    }
                    if( location == null ) {
                        locations = Collections.emptyList();
                    }
                    else {
                        locations = Collections.singletonList(location);
                    }
                }
                else {
                    User user = User.getUserByUserId(userId);

                    if( user == null ) {
                        locations = Collections.emptyList();
                    }
                    else {
                        locations = user.getLocations();
                    }
                }
                ArrayList<Map<String,Object>> list = new ArrayList<Map<String, Object>>();

                for( Location l : locations ) {
                    list.add(toJSON(l));
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println((new JSONArray(list)).toString());
                resp.getWriter().flush();
            }
        }
        catch( PersistenceException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RestException.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void post(@Nonnull String requestId, @Nullable String userId, @Nonnull String[] path, @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Map<String,Object> headers, @Nonnull Map<String,Object> parameters) throws RestException, IOException {
        try {
            if( userId == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.RELAY_NOT_ALLOWED, "A relay cannot add locations");
            }
            User user = User.getUserByUserId(userId);

            if( user == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.NO_SUCH_USER, "An error occurred identifying the user record for this key");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
            StringBuilder source = new StringBuilder();
            String line;

            while( (line = reader.readLine()) != null ) {
                source.append(line);
                source.append(" ");
            }
            String name = null, description = null, tz = null;
            JSONObject object = new JSONObject(source.toString());

            if( object.has("name") && !object.isNull("name") ) {
                name = object.getString("name");
            }
            if( object.has("description") && !object.isNull("description") ) {
                description = object.getString("description");
            }
            if( object.has("timeZone") && !object.isNull("timeZone") ) {
                tz = object.getString("timeZone");
            }
            if( name == null || description == null ) {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.MISSING_DATA, "Required fields: name, description");
            }

            TimeZone timeZone = (tz == null ? TimeZone.getTimeZone("UTC") : TimeZone.getTimeZone(tz));
            Location location = Location.create(userId, name, description, timeZone);

            user.grant(location);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println((new JSONObject(toJSON(location))).toString());
            resp.getWriter().flush();
        }
        catch( JSONException e ) {
            throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_JSON, "Invalid JSON in request");
        }
        catch( PersistenceException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, RestException.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void put(@Nonnull String requestId, @Nullable String userId, @Nonnull String[] path, @Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp, @Nonnull Map<String,Object> headers, @Nonnull Map<String,Object> parameters) throws RestException, IOException {
        try {
            String locationId = (path.length > 1 ? path[1] : null);
            Location location = null;

            if( locationId != null ) {
                location = Location.getLocation(locationId);
            }
            if( location == null ) {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_OBJECT, "The location " + locationId + " does not exist.");
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
            if( object.has("location") ) {
                object = object.getJSONObject("location");
            }
            else {
                object = null;
            }
            if( action.equalsIgnoreCase("initializePairing") ) {
                HashMap<String,Object> json = new HashMap<String, Object>();
                String pairingCode = location.readyForPairing();

                json.put("pairingCode", pairingCode);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println((new JSONObject(json)).toString());
                resp.getWriter().flush();
            }
            else if( action.equalsIgnoreCase("modify") ) {
                if( object == null ) {
                    throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_PUT, "No location was specified in the PUT");
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

    private Map<String,Object> toJSON(Location location) {
        HashMap<String,Object> json = new HashMap<String, Object>();

        json.put("name", location.getName());
        json.put("description", location.getDescription());
        json.put("timeZone", location.getTimeZone().getID());
        json.put("locationId", location.getLocationId());
        return json;
    }

    static public void main(String ... args) throws Exception {
        if( args.length < 1 ) {
            System.err.println("You must specify an action");
            System.exit(-1);
            return;
        }
        String action = args[0];

        if( action.equalsIgnoreCase("initializePairing") ) {
            if( args.length < 5 ) {
                System.err.println("You must specify a location ID");
                System.exit(-2);
                return;
            }
            String endpoint = args[1];
            String locationId = args[2];
            String apiKeyId = args[3];
            String apiKeySecret = args[4];

            HashMap<String,Object> act = new HashMap<String, Object>();

            act.put("action", "initializePairing");

            HttpParams params = new BasicHttpParams();

            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            //noinspection deprecation
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            HttpProtocolParams.setUserAgent(params, "Imaginary Home");

            HttpClient client = new DefaultHttpClient(params);

            HttpPut method = new HttpPut(endpoint + "/location/" + locationId);
            long timestamp = System.currentTimeMillis();

            method.addHeader("Content-Type", "application/json");
            method.addHeader("x-imaginary-version", CloudService.VERSION);
            method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
            method.addHeader("x-imaginary-api-key", apiKeyId);
            method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "put:/location/" + locationId + ":" + apiKeyId + ":" + timestamp + ":" + CloudService.VERSION));

            //noinspection deprecation
            method.setEntity(new StringEntity((new JSONObject(act)).toString(), "application/json", "UTF-8"));

            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                e.printStackTrace();
                throw new CommunicationException(e);
            }
            if( status.getStatusCode() == HttpServletResponse.SC_OK ) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject u = new JSONObject(json);

                System.out.println((u.has("pairingCode") && !u.isNull("pairingCode")) ? u.getString("pairingCode") : "--no code--");
            }
            else {
                System.err.println("Failed to initialize pairing  (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
                System.exit(status.getStatusCode());
            }
        }
        else if( action.equalsIgnoreCase("create") ) {
            if( args.length < 7 ) {
                System.err.println("create ENDPOINT NAME DESCRIPTION TIMEZONE API_KEY_ID API_KEY_SECRET");
                System.exit(-2);
                return;
            }
            String endpoint = args[1];
            String name = args[2];
            String description = args[3];
            String tz = args[4];
            String apiKeyId = args[5];
            String apiKeySecret = args[6];

            HashMap<String,Object> lstate = new HashMap<String, Object>();

            lstate.put("name", name);
            lstate.put("description", description);
            lstate.put("timeZone", tz);

            HttpParams params = new BasicHttpParams();

            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            //noinspection deprecation
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            HttpProtocolParams.setUserAgent(params, "Imaginary Home");

            HttpClient client = new DefaultHttpClient(params);

            HttpPost method = new HttpPost(endpoint + "/location");
            long timestamp = System.currentTimeMillis();

            System.out.println("Signing: " + "post:/location:" + apiKeyId + ":" + timestamp + ":" + CloudService.VERSION);
            method.addHeader("Content-Type", "application/json");
            method.addHeader("x-imaginary-version", CloudService.VERSION);
            method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
            method.addHeader("x-imaginary-api-key", apiKeyId);
            method.addHeader("x-imaginary-signature", CloudService.sign(apiKeySecret.getBytes("utf-8"), "post:/location:" + apiKeyId + ":" + timestamp + ":" + CloudService.VERSION));

            //noinspection deprecation
            method.setEntity(new StringEntity((new JSONObject(lstate)).toString(), "application/json", "UTF-8"));

            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                e.printStackTrace();
                throw new CommunicationException(e);
            }
            if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject u = new JSONObject(json);

                System.out.println((u.has("locationId") && !u.isNull("locationId")) ? u.getString("locationId") : "--no location--");
            }
            else {
                System.err.println("Failed to create location  (" + status.getStatusCode() + ": " + EntityUtils.toString(response.getEntity()));
                System.exit(status.getStatusCode());
            }
        }
        else {
            System.err.println("No such action: " + action);
            System.exit(-3);
        }
    }
}
