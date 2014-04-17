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

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.dasein.util.CalendarWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * <p>
 * A cloud service is a web service on the public internet that provides some kind of interface (UI, API, both)
 * for people/applications to interact with on the public internet and then route requests down to the home controller
 * behind the user's home firewall. The home controller running in the home talks to one or more cloud services,
 * notifying it about the current status of all home automation devices, and fetching any commands posted by
 * users or their tools.
 * </p>
 * <p>
 *     This is the typical architecture for most home automation solutions with one key difference: this is totally
 *     open. Your device can operate against any number of cloud services as long as they support this web services
 *     protocol. If you use multiple services, they can cooperate together nicely.
 * </p>
 * <p>
 *     When the home controller starts up, it looks at the configuration file to identify any current cloud services
 *     it works with. You can then add more services through the service pairing protocol. Once added, this home
 *     controller will periodically communicate with the cloud service.
 * </p>
 */
public class CloudService {
    static public final String VERSION = "2013-01";

    static private @Nonnull HttpClient getClient(@Nonnull String endpoint, @Nullable String proxyHost, int proxyPort) {
        boolean ssl = endpoint.startsWith("https");
        HttpParams params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        //noinspection deprecation
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUserAgent(params, "Imaginary Home");

        if( proxyHost != null ) {
            if( proxyPort < 0 ) {
                proxyPort = 0;
            }
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, proxyPort, ssl ? "https" : "http"));
        }
        return new DefaultHttpClient(params);
    }

    static CloudService pair(@Nonnull String name, @Nonnull String endpoint, @Nullable String proxyHost, int proxyPort, @Nonnull String pairingToken) throws CommunicationException, ControllerException {
        HttpPost method = new HttpPost(endpoint + "/relay");

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(System.currentTimeMillis()));

        HashMap<String,Object> body = new HashMap<String, Object>();

        body.put("pairingCode", pairingToken);
        body.put("name", HomeController.getInstance().getName());
        try {
            //noinspection deprecation
            method.setEntity(new StringEntity((new JSONObject(body)).toString(), "application/json", "UTF-8"));
        }
        catch( UnsupportedEncodingException e ) {
            throw new ControllerException(e);
        }
        HttpResponse response;
        StatusLine status;
        HttpClient client = null;

        try {
            client = getClient(endpoint, proxyHost, proxyPort);
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        if( status.getStatusCode() == HttpServletResponse.SC_CREATED ) {
            HttpEntity entity = response.getEntity();

            if( entity == null ) {
                throw new CommunicationException(status.getStatusCode(), "No error, but no body");
            }
            String json;

            try {
                json = EntityUtils.toString(entity);
            }
            catch( IOException e ) {
                throw new ControllerException(e);
            }
            try {
                JSONObject ob = new JSONObject(json);
                String apiKeyId = null, apiSecret = null;

                if( ob.has("apiKeyId") && !ob.isNull("apiKeyId") ) {
                    apiKeyId = ob.getString("apiKeyId");
                }
                if( ob.has("apiKeySecret") && !ob.isNull("apiKeySecret") ) {
                    apiSecret = ob.getString("apiKeySecret");
                }
                if( apiKeyId == null || apiSecret == null ) {
                    throw new CommunicationException(status.getStatusCode(), "Invalid JSON response to pairing request");
                }
                return new CloudService(apiKeyId, apiSecret, name, endpoint, proxyHost, proxyPort);
            }
            catch( JSONException e ) {
                throw new CommunicationException(e);
            }

        }
        else {
            HttpEntity entity = response.getEntity();

            if( entity == null ) {
                throw new CommunicationException(status.getStatusCode(), "An error was returned without explanation");
            }
            String json;

            try {
                json = EntityUtils.toString(entity);
            }
            catch( IOException e ) {
                throw new ControllerException(e);
            }
            throw new CommunicationException(status.getStatusCode(), json);
        }
    }

    static public String sign(byte[] key, String stringToSign) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return new String(Base64.encodeBase64(mac.doFinal(stringToSign.getBytes("utf-8"))));
    }

    private String apiKeySecret;
    private String endpoint;
    private String name;
    private String proxyHost;
    private int    proxyPort;
    private String serviceId;
    private String token;

    public CloudService(@Nonnull String serviceId, @Nonnull String apiKeySecret, @Nonnull String name, @Nonnull String endpoint, @Nullable String proxyHost, int proxyPort) {
        this.apiKeySecret = apiKeySecret;
        this.name = name;
        this.endpoint = endpoint;
        this.serviceId = serviceId;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.token = null;
    }

    private void authenticate() throws ControllerException, CommunicationException {
        HttpPut method = new HttpPut(endpoint + "/token");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", serviceId);

        String stringToSign = "PUT:/token:" + serviceId + ":" + timestamp + ":" + VERSION;

        try {
            method.addHeader("x-imaginary-signature", sign(apiKeySecret.getBytes("utf-8"), stringToSign));
        }
        catch( Exception e ) {
            throw new ControllerException(e);
        }
        HttpResponse response;
        HttpClient client = null;

        try {
            client = getClient(endpoint, proxyHost, proxyPort);
            response = client.execute(method);
        }
        catch( IOException e ) {
            throw new CommunicationException(e);
        }
        finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        if( response.getStatusLine().getStatusCode() == HttpServletResponse.SC_CREATED ) {
            HttpEntity entity = response.getEntity();

            if( entity == null ) {
                throw new CommunicationException(response.getStatusLine().getStatusCode(), "An error was returned without explanation");
            }
            String json;

            try {
                json = EntityUtils.toString(entity);
            }
            catch( IOException e ) {
                throw new ControllerException(e);
            }
            try {
                JSONObject ob = new JSONObject(json);

                if( ob.has("token") && !ob.isNull("token") ) {
                    token = ob.getString("token");
                }
                else {
                    throw new CommunicationException("No token was provided in a successful authentication");
                }
            }
            catch( JSONException e ) {
                throw new CommunicationException(e);
            }
        }
        else {
            parseError(response);
        }
    }

    public void fetchCommands() throws CommunicationException, ControllerException {
        boolean hasCommands;

        do {
            HttpPut method = new HttpPut(endpoint + "/command");
            long timestamp = System.currentTimeMillis();

            method.addHeader("Content-Type", "application/json");
            method.addHeader("x-imaginary-version", VERSION);
            method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
            method.addHeader("x-imaginary-api-key", serviceId);

            if( token == null ) {
                authenticate();
            }
            String stringToSign = "put:/command:" + serviceId + ":" + token + ":" + timestamp + ":" + VERSION;

            try {
                method.addHeader("x-imaginary-signature", sign(apiKeySecret.getBytes("utf-8"), stringToSign));
            }
            catch( Exception e ) {
                throw new ControllerException(e);
            }
            HttpResponse response;
            StatusLine status;

            HttpClient client = null;
            try {
                client = getClient(endpoint, proxyHost, proxyPort);
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                e.printStackTrace();
                throw new CommunicationException(e);
            }
            finally {
                if (client != null) {
                    client.getConnectionManager().shutdown();
                }
            }
            if( status.getStatusCode() != HttpServletResponse.SC_OK ) {
                parseError(response); // this will throw an exception
            }
            Header h = response.getFirstHeader("x-imaginary-has-commands");
            String val = (h == null ? "false" : h.getValue());

            hasCommands = (val != null && val.equalsIgnoreCase("true"));

            HttpEntity entity = response.getEntity();

            if( entity == null ) {
                throw new CommunicationException(response.getStatusLine().getStatusCode(), "An error was returned without explanation");
            }
            String json;

            try {
                json = EntityUtils.toString(entity);
            }
            catch( IOException e ) {
                throw new CommunicationException(e);
            }
            try {
                HashMap<String,Map<String,List<JSONObject>>> commandGroups = new HashMap<String, Map<String, List<JSONObject>>>();
                JSONArray list = new JSONArray(json);

                for( int i=0; i<list.length(); i++ ) {
                    JSONObject remoteCommand = list.getJSONObject(i);
                    String groupId;

                    if( remoteCommand.has("groupId") && !remoteCommand.isNull("groupId") ) {
                        groupId = remoteCommand.getString("groupId");
                    }
                    else {
                        continue;
                    }
                    long timeout = ((remoteCommand.has("timeout") && !remoteCommand.isNull("timeout")) ? remoteCommand.getLong("timeout") : (CalendarWrapper.MINUTE*5L));

                    String commandString;

                    if( remoteCommand.has("command") && !remoteCommand.isNull("command") ) {
                        commandString = remoteCommand.getString("command");
                    }
                    else {
                        continue;
                    }
                    JSONObject commandObject = new JSONObject(commandString);

                    String command = commandObject.getString("command");
                    JSONArray arguments = null;

                    if( commandObject.has("arguments") && !commandObject.isNull("arguments") ) {
                        arguments = commandObject.getJSONArray("arguments");
                    }
                    String commandId;

                    if( remoteCommand.has("commandId") && !remoteCommand.isNull("commandId") ) {
                        commandId = remoteCommand.getString("commandId");
                    }
                    else {
                        continue;
                    }

                    if( remoteCommand.has("devices") && !remoteCommand.isNull("devices") ) {
                        Map<String,TreeSet<String>> systemMapping = new HashMap<String, TreeSet<String>>();
                        JSONArray arr = remoteCommand.getJSONArray("devices");

                        for( int j=0; j<arr.length(); j++ ) {
                            JSONObject d = arr.getJSONObject(j);
                            String deviceId = d.getString("deviceId");
                            String systemId = d.getString("systemId");
                            TreeSet<String> deviceIds;

                            if( systemMapping.containsKey(systemId) ) {
                                deviceIds = systemMapping.get(systemId);
                            }
                            else {
                                deviceIds = new TreeSet<String>();
                                systemMapping.put(systemId, deviceIds);
                            }
                            deviceIds.add(deviceId);
                        }
                        Map<String, List<JSONObject>> groupMap;

                        if( commandGroups.containsKey(groupId) ) {
                            groupMap = commandGroups.get(groupId);
                        }
                        else {
                            groupMap = new HashMap<String, List<JSONObject>>();
                            commandGroups.put(groupId, groupMap);
                        }
                        for( String systemId : systemMapping.keySet() ) {
                            List<JSONObject> cmds;

                            if( groupMap.containsKey(systemId) ) {
                                cmds = groupMap.get(systemId);
                            }
                            else {
                                cmds = new ArrayList<JSONObject>();
                                groupMap.put(systemId, cmds);
                            }
                            HashMap<String,Object> map = new HashMap<String, Object>();

                            map.put("commandId", commandId);
                            map.put("resourceIds", systemMapping.get(systemId));
                            map.put("systemId", systemId);
                            map.put("timeout", timeout);
                            map.put("command", command);
                            if( arguments != null ) {
                                map.put("arguments", arguments);
                            }
                            else {
                                map.put("arguments", new ArrayList<JSONObject>());
                            }
                            cmds.add(new JSONObject(map));
                        }
                    }
                }
                for( String groupId : commandGroups.keySet() ) {
                    Map<String,List<JSONObject>> systemCommands = commandGroups.get(groupId);

                    for( String systemId : systemCommands.keySet() ) {
                        List<JSONObject> commands = systemCommands.get(systemId);

                        if( commands != null && commands.size() > 0 ) {
                            HomeController.getInstance().queueCommands(this, commands.toArray(new JSONObject[commands.size()]));
                        }
                    }
                }
            }
            catch( JSONException e ) {
                throw new CommunicationException(e);
            }
        } while( hasCommands );
    }

    public @Nonnull String getApiKeySecret() {
        return apiKeySecret;
    }

    public @Nonnull String getEndpoint() {
        return endpoint;
    }

    public @Nonnull String getName() {
        return name;
    }

    private void parseError(HttpResponse response)  throws CommunicationException {
        HttpEntity entity = response.getEntity();

        if( entity == null ) {
            throw new CommunicationException(response.getStatusLine().getStatusCode(), "An error was returned without explanation");
        }
        String json;

        try {
            json = EntityUtils.toString(entity);
        }
        catch( IOException e ) {
            throw new CommunicationException(e);
        }
        StringBuilder message = new StringBuilder();

        try {
            JSONObject error = new JSONObject(json);

            if( error.has("message") ) {
                message.append(error.getString("message"));
            }
            if( error.has("description") ) {
                if( message.length() > 0 ) {
                    message.append(": ");
                }
                message.append(error.getString("description"));
            }
        }
        catch( JSONException e ) {
            e.printStackTrace();
        }
        if( message.length() < 1 ) {
            message.append(json);
        }
        throw new CommunicationException(response.getStatusLine().getStatusCode(), message.toString());
    }

    public @Nullable String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean hasCommands() throws CommunicationException, ControllerException {
        HttpHead method = new HttpHead(endpoint + "/command");
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", serviceId);

        if( token == null ) {
            authenticate();
        }
        String stringToSign = "head:/command:" + serviceId + ":" + token + ":" + timestamp + ":" + VERSION;

        try {
            method.addHeader("x-imaginary-signature", sign(apiKeySecret.getBytes("utf-8"), stringToSign));
        }
        catch( Exception e ) {
            throw new ControllerException(e);
        }

        HttpResponse response;
        StatusLine status;
        HttpClient client = null;

        try {
            client = getClient(endpoint, proxyHost, proxyPort);
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        if( status.getStatusCode() != HttpServletResponse.SC_NO_CONTENT ) {
            parseError(response); // this will throw an exception
        }
        Header h = response.getFirstHeader("x-imaginary-has-commands");
        String val = (h == null ? "false" : h.getValue());

        return (val != null && val.equalsIgnoreCase("true"));
    }

    public boolean postAlert() {
        // TODO: define API for posting an alert
        return false;
    }

    public boolean postState() throws CommunicationException, ControllerException {
        HttpPut method = new HttpPut(endpoint + "/relay/" + serviceId);
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", serviceId);

        if( token == null ) {
            authenticate();
        }
        String stringToSign = "PUT:/relay" + serviceId + ":" + serviceId + ":" + token + ":" + timestamp + ":" + VERSION;

        try {
            method.addHeader("x-imaginary-signature", sign(apiKeySecret.getBytes("utf-8"), stringToSign));
        }
        catch( Exception e ) {
            throw new ControllerException(e);
        }
        ArrayList<Map<String,Object>> devices = new ArrayList<Map<String, Object>>();
        HashMap<String,Object> state = new HashMap<String,Object>();

        state.put("action", "update");
        for( ManagedResource resource : HomeController.getInstance().listResources() ) {
            HashMap<String,Object> json = new HashMap<String, Object>();

            resource.toMap(json);
            devices.add(json);
        }
        state.put("devices", devices);

        try {
            //noinspection deprecation
            method.setEntity(new StringEntity((new JSONObject(state)).toString(), "application/json", "UTF-8"));
        }
        catch( UnsupportedEncodingException e ) {
            throw new ControllerException(e);
        }
        HttpResponse response;
        StatusLine status;
        HttpClient client = null;

        try {
            client = getClient(endpoint, proxyHost, proxyPort);
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        if( status.getStatusCode() != HttpServletResponse.SC_NO_CONTENT ) {
            parseError(response); // this will throw an exception
        }
        Header h = response.getFirstHeader("x-imaginary-has-commands");
        String val = (h == null ? "false" : h.getValue());

        return (val != null && val.equalsIgnoreCase("true"));
    }

    public boolean postResult(@Nonnull String cmdId, boolean stateChanged, @Nullable Throwable exception) throws CommunicationException, ControllerException {
        HttpPut method = new HttpPut(endpoint + "/command/" + cmdId);
        long timestamp = System.currentTimeMillis();

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", VERSION);
        method.addHeader("x-imaginary-timestamp", String.valueOf(timestamp));
        method.addHeader("x-imaginary-api-key", serviceId);

        if( token == null ) {
            authenticate();
        }
        String stringToSign = "PUT:/command/" + cmdId + ":" + serviceId + ":" + token + ":" + timestamp + ":" + VERSION;

        try {
            method.addHeader("x-imaginary-signature", sign(apiKeySecret.getBytes("utf-8"), stringToSign));
        }
        catch( Exception e ) {
            throw new ControllerException(e);
        }
        HashMap<String,Object> state = new HashMap<String,Object>();

        state.put("action", "complete");
        Map<String,Object> result = new HashMap<String, Object>();

        result.put("result", stateChanged);
        if( exception != null ) {
            result.put("errorMessage", exception.getMessage());
        }
        state.put("result", result);
        try {
            //noinspection deprecation
            method.setEntity(new StringEntity((new JSONObject(state)).toString(), "application/json", "UTF-8"));
        }
        catch( UnsupportedEncodingException e ) {
            throw new ControllerException(e);
        }
        HttpResponse response;
        StatusLine status;
        HttpClient client = null;

        try {
            client = getClient(endpoint, proxyHost, proxyPort);
            response = client.execute(method);
            status = response.getStatusLine();
        }
        catch( IOException e ) {
            e.printStackTrace();
            throw new CommunicationException(e);
        }
        finally {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        }
        if( status.getStatusCode() != HttpServletResponse.SC_NO_CONTENT ) {
            parseError(response); // this will throw an exception
        }
        Header h = response.getFirstHeader("x-imaginary-has-commands");
        String val = (h == null ? "false" : h.getValue());

        return (val != null && val.equalsIgnoreCase("true"));
    }
}
