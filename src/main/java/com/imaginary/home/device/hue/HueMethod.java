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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
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
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

public class HueMethod {
    private Hue    hue;

    @SuppressWarnings("UnusedDeclaration")
    private HueMethod() { }

    public HueMethod(Hue hue) {
        this.hue = hue;
    }

    public void delete(@Nonnull String resource) throws HueException {
        Logger std = Hue.getLogger(HueMethod.class);
        Logger wire = Hue.getWireLogger(HueMethod.class);

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + HueMethod.class.getName() + ".delete(" +resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("");
            wire.debug(">>> [DELETE (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource);
        }
        try {
            HttpClient client = getClient();
            HttpDelete method = new HttpDelete(hue.getAPIEndpoint() + resource);

            method.addHeader("Content-Type", "application/json");
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("DELETE: Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new HueException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("DELETE: HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();

            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_NO_CONTENT && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                std.error("DELETE: Expected OK or NO_CONTENT or ACCEPTED for DELETE request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    throw new HueException(status.getStatusCode(), "An error was returned without explanation");
                }
                String body;

                try {
                    body = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(body);
                }
                wire.debug("");
                throw new HueException(status.getStatusCode(), body);
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + HueMethod.class.getName() + ".delete()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("<<< [DELETE (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource + " <--------------------------------------------------------------------------------------");
                wire.debug("");
            }
        }
    }

    public JSONObject get(@Nonnull String resource) throws HueException {
        Logger std = Hue.getLogger(HueMethod.class);
        Logger wire = Hue.getWireLogger(HueMethod.class);

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + HueMethod.class.getName() + ".get(" +resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("");
            wire.debug(">>> [GET (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource);
        }
        try {
            HttpClient client = getClient();
            HttpGet method = new HttpGet(hue.getAPIEndpoint() + resource);

            method.addHeader("Content-Type", "application/json");

            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("GET: Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new HueException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("GET: HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();

            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            if( status.getStatusCode() == HttpServletResponse.SC_NOT_FOUND ) {
                return null;
            }
            else if( status.getStatusCode() != HttpServletResponse.SC_OK ) {
                std.error("GET: Expected OK or NOT_FOUND for GET request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    throw new HueException(status.getStatusCode(), "An error was returned without explanation");
                }
                String json;

                try {
                    json = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(json);
                    wire.debug("");
                }
                throw new HueException(status.getStatusCode(), json);
            }
            else {
                try {
                    String json = EntityUtils.toString(response.getEntity());

                    if( wire.isDebugEnabled() ) {
                        wire.debug(json);
                        wire.debug("");
                    }
                    return new JSONObject(json);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                catch( JSONException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + HueMethod.class.getName() + ".get()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("<<< [GET (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource + " <--------------------------------------------------------------------------------------");
                wire.debug("");
            }
        }
    }

    protected @Nonnull HttpClient getClient() {
        boolean ssl = hue.getAPIEndpoint().startsWith("https");
        HttpParams params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        //noinspection deprecation
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUserAgent(params, "Imaginary Home");

        Properties p = hue.getCustomProperties();
        String proxyHost = p.getProperty("proxyHost");
        String proxyPort = p.getProperty("proxyPort");

        if( proxyHost != null ) {
            int port = 0;

            if( proxyPort != null && proxyPort.length() > 0 ) {
                port = Integer.parseInt(proxyPort);
            }
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, port, ssl ? "https" : "http"));
        }
        return new DefaultHttpClient(params);
    }

    public JSONObject post(@Nonnull String resource, JSONObject body) throws HueException {
        Logger std = Hue.getLogger(HueMethod.class);
        Logger wire = Hue.getWireLogger(HueMethod.class);

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + HueMethod.class.getName() + ".post(" +resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("");
            wire.debug(">>> [POST (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource);
        }
        try {
            HttpClient client = getClient();
            HttpPost method = new HttpPost(hue.getAPIEndpoint() + resource);

            method.addHeader("Content-Type", "application/json");

            try {
                if( body != null ) {
                    //noinspection deprecation
                    method.setEntity(new StringEntity(body.toString(), "application/json", "UTF-8"));
                }
            }
            catch( UnsupportedEncodingException e ) {
                throw new HueException(e);
            }
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    try { wire.debug(EntityUtils.toString(method.getEntity())); }
                    catch( IOException ignore ) { }

                    wire.debug("");
                }
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("POST: Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new HueException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("POST: HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();

            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_CREATED && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                std.error("POST: Expected CREATED or OK or ACCEPTED for POST request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    throw new HueException(status.getStatusCode(), "An error was returned without explanation");
                }
                String json;

                try {
                    json = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(json);
                    wire.debug("");
                }
                throw new HueException(status.getStatusCode(), json);
            }
            else {
                try {
                    String json = EntityUtils.toString(response.getEntity());

                    if( wire.isDebugEnabled() ) {
                        wire.debug(json);
                        wire.debug("");
                    }
                    if( json.startsWith("[") ) {
                        JSONArray arr = new JSONArray(json);

                        if( arr.length() > 0 ) {
                            JSONObject ob = arr.getJSONObject(0);

                            if( ob.has("error") ) {
                                ob = ob.getJSONObject("error");
                                if( ob.has("description") ) {
                                    throw new HueException(ob.getString("description"));
                                }
                            }
                            return ob;
                        }
                        return null;
                    }
                    return new JSONObject(json);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                catch( JSONException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + HueMethod.class.getName() + ".post()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("<<< [POST (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource + " <--------------------------------------------------------------------------------------");
                wire.debug("");
            }
        }
    }

    public JSONObject put(@Nonnull String resource, JSONObject body) throws HueException {
        Logger std = Hue.getLogger(HueMethod.class);
        Logger wire = Hue.getWireLogger(HueMethod.class);

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + HueMethod.class.getName() + ".put(" +resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("");
            wire.debug(">>> [PUT (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource);
        }
        try {
            HttpClient client = getClient();
            HttpPut method = new HttpPut(hue.getAPIEndpoint() + resource);

            method.addHeader("Content-Type", "application/json");

            try {
                if( body != null ) {
                    //noinspection deprecation
                    method.setEntity(new StringEntity(body.toString(), "application/json", "UTF-8"));
                }
            }
            catch( UnsupportedEncodingException e ) {
                throw new HueException(e);
            }
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    try { wire.debug(EntityUtils.toString(method.getEntity())); }
                    catch( IOException ignore ) { }

                    wire.debug("");
                }
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("PUT: Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new HueException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("PUT: HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();

            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            if( status.getStatusCode() == HttpServletResponse.SC_NO_CONTENT ) {
                return null;
            }
            else if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_CREATED && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                std.error("PUT: Expected NO_CONTENT or CREATED or OK or ACCEPTED for PUT request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    throw new HueException(status.getStatusCode(), "An error was returned without explanation");
                }
                String json;

                try {
                    json = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(json);
                    wire.debug("");
                }
                throw new HueException(status.getStatusCode(), json);
            }
            else {
                try {
                    String json = EntityUtils.toString(response.getEntity());

                    if( wire.isDebugEnabled() ) {
                        wire.debug(json);
                        wire.debug("");
                    }
                    if( json.startsWith("[") ) {
                        JSONArray arr = new JSONArray(json);

                        if( arr.length() > 0 ) {
                            JSONObject ob = arr.getJSONObject(0);

                            if( ob.has("error") ) {
                                ob = ob.getJSONObject("error");
                                if( ob.has("description") ) {
                                    throw new HueException(ob.getString("description"));
                                }
                            }
                            return ob;
                        }
                        return null;
                    }
                    return new JSONObject(json);
                }
                catch( IOException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
                catch( JSONException e ) {
                    throw new HueException(status.getStatusCode(), e.getMessage());
                }
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + HueMethod.class.getName() + ".put()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("<<< [PUT (" + (new Date()) + ")] -> " + hue.getAPIEndpoint() + resource + " <--------------------------------------------------------------------------------------");
                wire.debug("");
            }
        }
    }
}
