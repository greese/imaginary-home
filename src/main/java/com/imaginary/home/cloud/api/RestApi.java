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

import com.imaginary.home.cloud.Location;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Map;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/12/13 3:47 PM</p>
 *
 * @author George Reese
 */
public class RestApi {
    static public final String[] VERSIONS = { "2013-01" };

    static public final String API_KEY   = "x-imaginary-api-key";
    static public final String SIGNATURE = "x-imaginary-signature";
    static public final String TIMESTAMP = "x-imaginary-timestamp";
    static public final String VERSION   = "x-imaginary-version";

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

    public boolean authenticate(@Nonnull String requestId, @Nonnull String method, @Nonnull HttpServletRequest request, Map<String,Object> headers, Map<String,Object> parameters) throws RestException {
        Number timestamp = (Number)headers.get(TIMESTAMP);
        String locationId = (String)headers.get(API_KEY);
        String signature = (String)headers.get(SIGNATURE);
        String version = (String)headers.get(VERSION);

        if( timestamp == null || locationId == null || signature == null || version == null ) {
            throw new RestException(HttpServletResponse.SC_BAD_REQUEST, "Incomplete authentication headers, requires: " + API_KEY + " - " + TIMESTAMP + " - " + SIGNATURE + " - " + VERSION);
        }
        if( signature.length() < 1 ) {
            throw new RestException(HttpServletResponse.SC_FORBIDDEN, "No signature was provided for authentication");
        }
        Location location = null; // TODO: lookup location using locationId

        if( location == null ) {
            throw new RestException(HttpServletResponse.SC_FORBIDDEN, "Access Denied", "Invalid API key");
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
}
