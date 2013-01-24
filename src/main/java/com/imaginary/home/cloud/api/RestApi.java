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

package com.imaginary.home.cloud.api;

import com.imaginary.home.cloud.Configuration;
import com.imaginary.home.cloud.Location;
import org.apache.commons.codec.binary.Base64;
import org.dasein.persist.PersistenceException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/12/13 3:47 PM</p>
 *
 * @author George Reese
 */
public class RestApi extends HttpServlet {
    static public final String[] VERSIONS = { "2013-01" };

    static public final String API_KEY    = "x-imaginary-api-key";
    static public final String SIGNATURE  = "x-imaginary-signature";
    static public final String TIMESTAMP  = "x-imaginary-timestamp";
    static public final String VERSION    = "x-imaginary-version";

    static private final HashMap<String,APICall> apiCalls = new HashMap<String,APICall>();

    static {

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

    static public boolean supports(@Nonnull String requiredVersion, @Nonnull String clientVersion) {
        if( clientVersion.equals(requiredVersion) ) {
            return true;
        }
        for( String v : VERSIONS ) {
            if( v.equals(clientVersion) ) {
                return true;
            }
            if( v.equals(requiredVersion) ) {
                return false;
            }
        }
        return false;
    }

    public void authenticate(@Nonnull String requestId, @Nonnull String method, @Nonnull HttpServletRequest request, Map<String,Object> headers) throws RestException {
        Number timestamp = (Number)headers.get(TIMESTAMP);
        String apiKey = (String)headers.get(API_KEY);
        String signature = (String)headers.get(SIGNATURE);
        String version = (String)headers.get(VERSION);

        if( timestamp == null || apiKey == null || signature == null || version == null ) {
            throw new RestException(HttpServletResponse.SC_BAD_REQUEST, "Incomplete authentication headers, requires: " + API_KEY + " - " + TIMESTAMP + " - " + SIGNATURE + " - " + VERSION);
        }
        if( signature.length() < 1 ) {
            throw new RestException(HttpServletResponse.SC_FORBIDDEN, "No signature was provided for authentication");
        }
        try {
            Location location = Location.getLocation(apiKey);

            if( location == null ) {
                // TODO: non-hub access
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Access Denied", "Invalid API key");
            }
            String stringToSign = method.toLowerCase() + ":" + request.getPathInfo().toLowerCase() + ":" + apiKey + ":" + location.getToken() + ":" + timestamp.longValue() + ":" + version;
            String secret = location.getApiKeySecret();

            if( secret == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Illegal Access", "Illegal access to requested resource");
            }
            if( !signature.equals(sign(Configuration.decrypt(apiKey, secret).getBytes("utf-8"), stringToSign)) ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Invalid Signature", "String to sign was: " + stringToSign);
            }
        }
        catch( PersistenceException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
        catch( UnsupportedEncodingException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    @Override
    public void doDelete(@Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp) throws IOException, ServletException {
        String requestId = request(req);

        try {
            Map<String,Object> headers = parseHeaders(req);
            String[] path = getPath(req);

            if( path.length < 1 ) {
                throw new RestException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, RestException.INVALID_OPERATION, "No DELETE is allowed against /");
            }
            else if( apiCalls.containsKey(path[0]) ) {
                Map<String,Object> parameters = parseParameters(req);
                APICall call = apiCalls.get(path[0]);

                authenticate(requestId, "DELETE", req, headers);
                call.delete(requestId, req, resp, headers, parameters);
            }
            else {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_RESOURCE, "No " + path[0] + " resource exists in this API");
            }
        }
        catch( RestException e ) {
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", e.getStatus());
            error.put("message", e.getMessage());
            error.put("description", e.getDescription());
            resp.setStatus(e.getStatus());
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
        catch( Throwable t ) {
            t.printStackTrace();
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            error.put("message", RestException.INTERNAL_ERROR);
            error.put("description", t.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
    }

    @Override
    public void doGet(@Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp) throws IOException, ServletException {
        String requestId = request(req);

        try {
            Map<String,Object> headers = parseHeaders(req);
            String[] path = getPath(req);

            if( path.length < 1 ) {
                // TODO: documentation
                throw new RestException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, RestException.INVALID_OPERATION, "No GET is allowed against /");
            }
            else if( apiCalls.containsKey(path[0]) ) {
                Map<String,Object> parameters = parseParameters(req);
                APICall call = apiCalls.get(path[0]);

                authenticate(requestId, "GET", req, headers);
                call.get(requestId, req, resp, headers, parameters);
            }
            else {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_RESOURCE, "No " + path[0] + " resource exists in this API");
            }
        }
        catch( RestException e ) {
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", e.getStatus());
            error.put("message", e.getMessage());
            error.put("description", e.getDescription());
            resp.setStatus(e.getStatus());
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
        catch( Throwable t ) {
            t.printStackTrace();
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            error.put("message", RestException.INTERNAL_ERROR);
            error.put("description", t.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
    }

    @Override
    public void doHead(@Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp) throws IOException, ServletException {
        String requestId = request(req);

        try {
            Map<String,Object> headers = parseHeaders(req);
            String[] path = getPath(req);

            if( path.length < 1 ) {
                // TODO: documentation
                throw new RestException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, RestException.INVALID_OPERATION, "No HEAD is allowed against /");
            }
            else if( apiCalls.containsKey(path[0]) ) {
                Map<String,Object> parameters = parseParameters(req);
                APICall call = apiCalls.get(path[0]);

                authenticate(requestId, "HEAD", req, headers);
                call.head(requestId, req, resp, headers, parameters);
            }
            else {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_RESOURCE, "No " + path[0] + " resource exists in this API");
            }
        }
        catch( RestException e ) {
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", e.getStatus());
            error.put("message", e.getMessage());
            error.put("description", e.getDescription());
            resp.setStatus(e.getStatus());
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
        catch( Throwable t ) {
            t.printStackTrace();
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            error.put("message", RestException.INTERNAL_ERROR);
            error.put("description", t.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
    }

    @Override
    public void doPost(@Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp) throws IOException {
        String requestId = request(req);

        try {
            Map<String,Object> headers = parseHeaders(req);
            String[] path = getPath(req);

            if( path.length < 1 ) {
                throw new RestException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, RestException.INVALID_OPERATION, "No POST is allowed against /");
            }
            else if( path[0].equals("token") ) {
                String token = generateToken(requestId, "POST", req, headers, new HashMap<String, Object>());
                HashMap<String,Object> json = new HashMap<String, Object>();

                json.put("token", token);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println((new JSONObject(json)).toString());
                resp.getWriter().flush();
            }
            else if( path[0].equals("pair") ) {
                // TODO: implement pairing
            }
            else if( apiCalls.containsKey(path[0]) ) {
                Map<String,Object> parameters = parseParameters(req);
                APICall call = apiCalls.get(path[0]);

                authenticate(requestId, "POST", req, headers);
                call.post(requestId, req, resp, headers, parameters);
            }
            else {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_RESOURCE, "No " + path[0] + " resource exists in this API");
            }
        }
        catch( RestException e ) {
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", e.getStatus());
            error.put("message", e.getMessage());
            error.put("description", e.getDescription());
            resp.setStatus(e.getStatus());
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
        catch( Throwable t ) {
            t.printStackTrace();
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            error.put("message", RestException.INTERNAL_ERROR);
            error.put("description", t.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
    }

    @Override
    public void doPut(@Nonnull HttpServletRequest req, @Nonnull HttpServletResponse resp) throws IOException, ServletException {
        String requestId = request(req);

        try {
            Map<String,Object> headers = parseHeaders(req);
            String[] path = getPath(req);

            if( path.length < 1 ) {
                throw new RestException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, RestException.INVALID_OPERATION, "No PUT is allowed against /");
            }
            else if( apiCalls.containsKey(path[0]) ) {
                Map<String,Object> parameters = parseParameters(req);
                APICall call = apiCalls.get(path[0]);

                authenticate(requestId, "PUT", req, headers);
                call.delete(requestId, req, resp, headers, parameters);
            }
            else {
                throw new RestException(HttpServletResponse.SC_NOT_FOUND, RestException.NO_SUCH_RESOURCE, "No " + path[0] + " resource exists in this API");
            }
        }
        catch( RestException e ) {
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", e.getStatus());
            error.put("message", e.getMessage());
            error.put("description", e.getDescription());
            resp.setStatus(e.getStatus());
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
        catch( Throwable t ) {
            t.printStackTrace();
            HashMap<String,Object> error = new HashMap<String, Object>();

            error.put("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            error.put("message", RestException.INTERNAL_ERROR);
            error.put("description", t.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println((new JSONObject(error)).toString());
            resp.getWriter().flush();
        }
    }

    public @Nonnull String generateToken(@Nonnull String requestId, @Nonnull String method, @Nonnull HttpServletRequest request, Map<String,Object> headers, Map<String,Object> parameters) throws RestException {
        Number timestamp = (Number)headers.get(TIMESTAMP);
        String apiKey = (String)headers.get(API_KEY);
        String signature = (String)headers.get(SIGNATURE);
        String version = (String)headers.get(VERSION);

        if( timestamp == null || apiKey == null || signature == null || version == null ) {
            throw new RestException(HttpServletResponse.SC_BAD_REQUEST, "Incomplete authentication headers, requires: " + API_KEY + " - " + TIMESTAMP + " - " + SIGNATURE + " - " + VERSION);
        }
        if( signature.length() < 1 ) {
            throw new RestException(HttpServletResponse.SC_FORBIDDEN, "No signature was provided for authentication");
        }
        try {
            Location location = Location.getLocation(apiKey);

            if( location == null ) {
                // TODO: non-hub access
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Access Denied", "Invalid API key");
            }
            String stringToSign = method.toLowerCase() + ":" + request.getPathInfo().toLowerCase() + ":" + apiKey + ":" + timestamp.longValue() + ":" + version;
            String secret = location.getApiKeySecret();

            if( secret == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Illegal Access", "Illegal access to requested resource");
            }
            if( signature.equals(sign(Configuration.decrypt(apiKey, secret).getBytes("utf-8"), stringToSign)) ) {
                String token = Configuration.generateToken(30, 45);

                location.setToken(token);
                return token;
            }
            throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Illegal Access", "Illegal access to requested resource");
        }
        catch( PersistenceException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
        catch( UnsupportedEncodingException e ) {
            throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    private @Nullable  Object getHeader(@Nonnull String key, Enumeration<String> values) throws RestException {
        if( values == null || !values.hasMoreElements() ) {
            return null;
        }
        if( key.equalsIgnoreCase(API_KEY) || key.equals(SIGNATURE) ) {
            return values.nextElement();
        }
        if( key.equalsIgnoreCase(TIMESTAMP) ) {
            try {
                return Long.parseLong(values.nextElement());
            }
            catch( NumberFormatException e ) {
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, "Timestamps are the UNIX timestamp as the number of seconds since the Unix epoch.");
            }
        }
        return values;
    }

    private String[] getPath(@Nonnull HttpServletRequest req) {
        String p = req.getPathInfo().toLowerCase();

        while( p.startsWith("/") && !p.equals("/") ) {
            p = p.substring(1);
        }
        while( p.endsWith("/") && !p.equals("/") ) {
            p = p.substring(p.length()-1);
        }
        String[] parts = p.split("/");

        if( parts.length < 1 ) {
            if( p.equals("") || p.equals("/") ) {
                parts = new String[0];
            }
            else {
                parts = new String[] { p };
            }
        }
        return parts;
    }

    private @Nonnull Map<String,Object> parseHeaders(@Nonnull HttpServletRequest req) throws RestException {
        @SuppressWarnings("unchecked") Enumeration<String> names = (Enumeration<String>)req.getHeaderNames();
        HashMap<String,Object> headers = new HashMap<String, Object>();

        while( names.hasMoreElements() ) {
            String name = names.nextElement();

            //noinspection unchecked
            headers.put(name, getHeader(name, req.getHeaders(name)));
        }
        return headers;
    }

    private Map<String,Object> parseParameters(@Nonnull HttpServletRequest req) throws RestException {
        // TODO: implement me
        return new HashMap<String, Object>();
    }

    private String request(HttpServletRequest req) {
        // TODO: implement request tracking
        return UUID.randomUUID().toString();
    }

}
