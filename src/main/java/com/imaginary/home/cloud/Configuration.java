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

package com.imaginary.home.cloud;

import org.apache.commons.codec.binary.Base64;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.Transaction;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;

import javax.annotation.Nonnull;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Random;

/**
 * Represents the configuration of the cloud service. The rest of the system can fetch configuration data and
 * interact with general utility operations via this singleton class.
 * <p>Created by George Reese: 1/23/13 2:50 PM</p>
 * @author George Reese
 */
public class Configuration implements CachedItem {
    static private PersistentCache<Configuration> cache;

    static public @Nonnull String decrypt(@Nonnull String keySalt, @Nonnull String value) {
        try {
            SecretKeySpec spec = new SecretKeySpec(getConfiguration().getCustomSalt(keySalt), "AES");
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, spec);

            byte[] b64 = value.getBytes("utf-8");
            byte[] raw = Base64.decodeBase64(b64);
            byte[] decrypted = cipher.doFinal(raw);
            return new String(decrypted, "utf-8");
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    static public @Nonnull String encrypt(@Nonnull String keySalt, @Nonnull String value) {
        try {
            SecretKeySpec spec = new SecretKeySpec(getConfiguration().getCustomSalt(keySalt), "AES");
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, spec);

            byte[] raw = value.getBytes("utf-8");
            byte[] encrypted = cipher.doFinal(raw);
            byte[] b64 = Base64.encodeBase64(encrypted);
            return new String(b64, "utf-8");
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    static private @Nonnull PersistentCache<Configuration> getCache() throws PersistenceException {
        if( cache == null ) {
            //noinspection unchecked
            cache = (PersistentCache<Configuration>)PersistentCache.getCache(Configuration.class);
        }
        return cache;
    }

    static public @Nonnull Configuration getConfiguration() throws PersistenceException {
        Configuration c = getCache().get("configuration");

        if( c == null ) {
            HashMap<String,Object> state = new HashMap<String, Object>();

            state.put("key", "configuration");
            state.put("salt", generateToken(16, 16));

            Transaction xaction = Transaction.getInstance();

            try {
                c = getCache().create(xaction, state);
                xaction.commit();
            }
            finally {
                xaction.rollback();
            }
        }
        return c;
    }

    static private final Random random = new Random();

    static public @Nonnull String generateToken(int minLen, int maxLen) {
        int len = (maxLen > minLen ? minLen + random.nextInt(maxLen-minLen) : minLen);
        StringBuilder str = new StringBuilder();

        while( str.length() < len ) {
            char c = (char)random.nextInt(255);

            if( c >= 'a' && c <= 'z' && c != 'l' && c != 'o' && c != 'i' ) {
                str.append(c);
            }
            else if( c >= 'A' && c <= 'Z' && c != 'I' && c != 'O' ) {
                str.append(c);
            }
            else if( c >= '2' && c <= '9' ) {
                str.append(c);
            }
        }
        return str.toString();
    }

    @Index(type=IndexType.PRIMARY)
    private String key;
    private String salt;

    public Configuration() { }

    private @Nonnull byte[] getCustomSalt(@Nonnull String locationId) throws UnsupportedEncodingException {
        StringBuilder customSalt = new StringBuilder();
        int i = 0;

        while( customSalt.length() < 32 ) {
            if( i < salt.length() ) {
                customSalt.append(this.salt.charAt(i));
            }
            if( i < locationId.length() ) {
                char c = locationId.charAt(i);

                if( c != '-' ) {
                    customSalt.append(locationId.charAt(i));
                }
            }
            i++;
            if( i >= salt.length() && i >= locationId.length() ) {
                break;
            }
        }
        byte[] s = customSalt.toString().getBytes("utf-8");

        if( s.length > 16 ) {
            byte[] k = new byte[16];

            for( i=0; i<k.length; i++ ) {
                k[i] = s[i];
            }
            return k;
        }
        return s;
    }

    @Override
    public boolean isValidForCache() {
        return false;
    }
}
