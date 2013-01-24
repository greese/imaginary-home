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

package com.imaginary.home.cloud.user;

import com.imaginary.home.cloud.Configuration;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.Transaction;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/23/13 8:07 PM</p>
 *
 * @author George Reese
 */
public class ApiKey implements CachedItem {
    static private PersistentCache<ApiKey> cache;

    static public @Nonnull ApiKey create(@Nonnull User forUser, @Nonnull String application) throws PersistenceException {
        String keyId;

        do {
            keyId = Configuration.generateToken(10, 10);
        } while( getApiKey(keyId) != null );
        HashMap<String,Object> state = new HashMap<String, Object>();

        state.put("apiKeyId", keyId);
        state.put("apiKeySecret", Configuration.encrypt(forUser.getUserId(), Configuration.generateToken(20, 20)));
        state.put("userId", forUser.getUserId());
        state.put("application", application);

        Transaction xaction = Transaction.getInstance();

        try {
            ApiKey k = getCache().create(xaction, state);

            xaction.commit();
            return k;
        }
        finally {
            xaction.rollback();
        }
    }

    static private PersistentCache<ApiKey> getCache() throws PersistenceException {
        if( cache == null ) {
            //noinspection unchecked
            cache = (PersistentCache<ApiKey>)PersistentCache.getCache(ApiKey.class);
        }
        return cache;
    }

    static public @Nullable ApiKey getApiKey(@Nonnull String apiKeyId) throws PersistenceException {
        return getCache().get(apiKeyId);
    }

    @Index(type= IndexType.PRIMARY)
    private String apiKeyId;
    private String apiKeySecret; // encrypted
    @Index(type=IndexType.SECONDARY)
    private String application;
    @Index(type=IndexType.FOREIGN, identifies = User.class)
    private String userId;

    public ApiKey() { }

    public @Nonnull String getApiKeyId() {
        return apiKeyId;
    }

    public @Nonnull String getApiKeySecret() {
        return apiKeySecret;
    }

    public @Nonnull String getApplication() {
        return application;
    }

    public @Nonnull String getUserId() {
        return userId;
    }

    @Override
    public boolean isValidForCache() {
        return false;
    }
}
