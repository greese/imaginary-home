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
import com.imaginary.home.cloud.api.call.LocationCall;
import com.imaginary.home.cloud.user.ApiKey;
import com.imaginary.home.cloud.user.User;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        apiCalls.put("location", new LocationCall());
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

    public void authenticate(@Nonnull String method, @Nonnull HttpServletRequest request, Map<String,Object> headers) throws RestException {
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
            String customSalt;
            String secret;

            if( location == null ) {
                ApiKey key = ApiKey.getApiKey(apiKey);

                if( key == null ) {
                    throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.INVALID_KEY, "Invalid API key");
                }
                secret = key.getApiKeySecret();
                customSalt = key.getUserId();
            }
            else {
                secret = location.getApiKeySecret();
                customSalt = location.getPairingCode();
                if( customSalt == null ) {
                    throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.NOT_PAIRED, "Location is not paired");
                }
            }
            String stringToSign;

            if( location != null ) {
                stringToSign = method.toLowerCase() + ":" + request.getPathInfo().toLowerCase() + ":" + apiKey + ":" + location.getToken() + ":" + timestamp.longValue() + ":" + version;
            }
            else {
                stringToSign = method.toLowerCase() + ":" + request.getPathInfo().toLowerCase() + ":" + apiKey + ":" + timestamp.longValue() + ":" + version;
            }
            if( secret == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Illegal Access", "Illegal access to requested resource");
            }
            if( !signature.equals(sign(Configuration.decrypt(customSalt, secret).getBytes("utf-8"), stringToSign)) ) {
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

                authenticate("DELETE", req, headers);
                call.delete(requestId, path, req, resp, headers, parameters);
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

                authenticate("GET", req, headers);
                call.get(requestId, path, req, resp, headers, parameters);
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

                authenticate("HEAD", req, headers);
                call.head(requestId, path, req, resp, headers, parameters);
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
                String token = generateToken("POST", req, headers);
                HashMap<String,Object> json = new HashMap<String, Object>();

                json.put("token", token);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println((new JSONObject(json)).toString());
                resp.getWriter().flush();
            }
            else if( path[0].equals("pair") ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
                StringBuilder source = new StringBuilder();
                String line;

                while( (line = reader.readLine()) != null ) {
                    source.append(line);
                    source.append(" ");
                }
                JSONObject object = new JSONObject(source.toString());

                if( !object.has("pairingCode") ) {
                    throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.MISSING_PAIRING_CODE, "Pairing code is missing");
                }
                String code = object.getString("pairingCode");
                Location location = Location.findForPairing(code);

                if( location == null ) {
                    throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.INVALID_PAIRING_CODE, "Invalid pairing code; pairing did not occur");
                }
                String secret = location.pair(code);

                HashMap<String,Object> json = new HashMap<String, Object>();

                json.put("apiKeySecret", secret);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println((new JSONObject(json)).toString());
                resp.getWriter().flush();
            }
            else if( path[0].equals("user") ) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(req.getInputStream()));
                StringBuilder source = new StringBuilder();
                String line;

                while( (line = reader.readLine()) != null ) {
                    source.append(line);
                    source.append(" ");
                }
                String email = null, firstName = null, lastName = null, password = null;
                JSONObject object = new JSONObject(source.toString());

                if( object.has("email") && !object.isNull("email") ) {
                    email = object.getString("email").toLowerCase();
                }
                if( object.has("firstName") && !object.isNull("firstName") ) {
                    firstName = object.getString("firstName");
                }
                if( object.has("lastName") && !object.isNull("lastName") ) {
                    lastName = object.getString("lastName");
                }
                if( object.has("password") && !object.isNull("password") ) {
                    password = object.getString("password");
                }
                if( email == null || firstName == null || lastName == null || password == null ) {
                    throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.MISSING_DATA, "Required fields: email, firstName, lastName, password");
                }
                User user = User.create(email, firstName, lastName, password);
                ApiKey key = ApiKey.create(user);

                HashMap<String,Object> json = new HashMap<String, Object>();

                json.put("email", email);
                json.put("firstName", firstName);
                json.put("lastName", lastName);
                json.put("userId", user.getUserId());

                HashMap<String,Object> k = new HashMap<String, Object>();

                k.put("apiKeyId", key.getApiKeyId());
                k.put("apiKeySecret", Configuration.decrypt(user.getUserId(), key.getApiKeySecret()));
                k.put("userId", user.getUserId());

                json.put("apiKeys", k);

                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println((new JSONObject(json)).toString());
                resp.getWriter().flush();
            }
            else if( apiCalls.containsKey(path[0]) ) {
                Map<String,Object> parameters = parseParameters(req);
                APICall call = apiCalls.get(path[0]);

                authenticate("POST", req, headers);
                call.post(requestId, path, req, resp, headers, parameters);
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

                authenticate("PUT", req, headers);
                call.delete(requestId, path, req, resp, headers, parameters);
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

    public @Nonnull String generateToken(@Nonnull String method, @Nonnull HttpServletRequest request, Map<String,Object> headers) throws RestException {
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
            String secret, customSalt;

            if( location == null ) {
                ApiKey key = ApiKey.getApiKey(apiKey);

                if( key == null ) {
                    throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.INVALID_KEY, "Invalid API key");
                }
                throw new RestException(HttpServletResponse.SC_BAD_REQUEST, RestException.BAD_TOKEN, "User keys don't use token authentication");
            }
            else {
                secret = location.getApiKeySecret();
                customSalt = location.getPairingCode();
                if( customSalt == null ) {
                    throw new RestException(HttpServletResponse.SC_FORBIDDEN, RestException.NOT_PAIRED, "Location is not paired");
                }
            }

            String stringToSign = method.toLowerCase() + ":" + request.getPathInfo().toLowerCase() + ":" + apiKey + ":" + timestamp.longValue() + ":" + version;

            if( secret == null ) {
                throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Illegal Access", "Illegal access to requested resource");
            }
            if( signature.equals(sign(Configuration.decrypt(customSalt, secret).getBytes("utf-8"), stringToSign)) ) {
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
