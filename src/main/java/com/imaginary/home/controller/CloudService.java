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

import com.imaginary.home.cloud.api.RestException;
import com.imaginary.home.device.hue.HueException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

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

    static public CloudService pair(@Nonnull String name, @Nonnull String endpoint, @Nullable String proxyHost, int proxyPort, @Nonnull String pairingToken) throws CommunicationException {
        HttpClient client = getClient(endpoint, proxyHost, proxyPort);
        HttpPost method = new HttpPost(endpoint + "/pair");

        method.addHeader("Content-Type", "application/json");
        method.addHeader("x-imaginary-version", "2013-01");
        method.addHeader("x-imaginary-timestamp", String.valueOf(System.currentTimeMillis()));

        HashMap<String,Object> body = new HashMap<String, Object>();

        body.put("pairingCode", pairingToken);
        try {
            //noinspection deprecation
            method.setEntity(new StringEntity((new JSONObject(body)).toString(), "application/json", "UTF-8"));
        }
        catch( UnsupportedEncodingException e ) {
            throw new HueException(e);
        }
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
            HttpEntity entity = response.getEntity();

            if( entity == null ) {
                throw new CommunicationException(status.getStatusCode(), "No error, but no body");
            }
            String json;

            try {
                json = EntityUtils.toString(entity);
            }
            catch( IOException e ) {
                throw new HueException(status.getStatusCode(), e.getMessage());
            }
            try {
                JSONObject ob = new JSONObject(json);
                String locationId = null, apiSecret = null;

                if( ob.has("locationId") && !ob.isNull("locationId") ) {
                    locationId = ob.getString("locationId");
                }
                if( ob.has("apiKeySecret") && !ob.isNull("apiKeySecret") ) {
                    apiSecret = ob.getString("apiKeySecret");
                }
                if( locationId == null || apiSecret == null ) {
                    throw new CommunicationException(status.getStatusCode(), "Invalid JSON response to pairing request");
                }
                return new CloudService(locationId, apiSecret, name, endpoint, proxyHost, proxyPort);
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
                throw new HueException(status.getStatusCode(), e.getMessage());
            }
            throw new CommunicationException(status.getStatusCode(), json);
        }
    }

    static public String sign(byte[] key, String stringToSign) throws RestException {
        try {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");

                mac.init(new SecretKeySpec(key, "HmacSHA256"));
                return new String(Base64.encodeBase64(mac.doFinal(stringToSign.getBytes("utf-8"))));
            }
            catch( NoSuchAlgorithmException e ) {
                throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Encryption error: " + e.getMessage());
            }
            catch( InvalidKeyException e ) {
                throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Encryption error: " + e.getMessage());
            }
            catch( IllegalStateException e ) {
                throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Encryption error: " + e.getMessage());
            }
            catch( UnsupportedEncodingException e ) {
                throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Encryption error: " + e.getMessage());
            }
        }
        catch( RuntimeException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Signature error: " + e.getMessage());
        }
    }

    private String apiKeySecret;
    private String endpoint;
    private String name;
    private String proxyHost;
    private int    proxyPort;
    private String serviceId;

    public CloudService(@Nonnull String serviceId, @Nonnull String apiKeySecret, @Nonnull String name, @Nonnull String endpoint, @Nullable String proxyHost, int proxyPort) {
        this.apiKeySecret = apiKeySecret;
        this.name = name;
        this.endpoint = endpoint;
        this.serviceId = serviceId;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public void fetchCommands() {
        // TODO: implement me
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

    public @Nullable String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean hasCommands() {
        // TODO: implement me
        return false;
    }

    public boolean postAlert() {
        // TODO: define API for posting an alert
        return false;
    }

    public boolean postState() {
        // TODO: implement me
        return false;
    }

    public boolean postResult(@Nonnull String cmdId, boolean stateChanged, @Nullable Map<String, Object> result, @Nullable Throwable exception) {
        // TODO: implement me
        return false;
    }
}
