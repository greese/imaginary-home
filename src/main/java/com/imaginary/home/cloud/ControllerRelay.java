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

import org.dasein.persist.DataIntegrityException;
import org.dasein.persist.Memento;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.Transaction;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

/**
 * Cloud service reference to a home controller relay. Each location must have one, but may (for whatever reason)
 * have more than one.
 * <p>Created by George Reese: 1/14/13 9:13 AM</p>
 * @author George Reese
 */
public class ControllerRelay implements CachedItem {
    static private PersistentCache<ControllerRelay> cache;

    static private PersistentCache<ControllerRelay> getCache() throws PersistenceException {
        if( cache == null) {
            //noinspection unchecked
            cache = (PersistentCache<ControllerRelay>)PersistentCache.getCache(ControllerRelay.class);
        }
        return cache;
    }

    static public @Nonnull ControllerRelay create(@Nonnull Location location, @Nonnull String name) throws PersistenceException {
        String id;

        do {
            id = UUID.randomUUID().toString();
        } while( getRelay(id) != null );

        String key = Configuration.encrypt(location.getLocationId(), Configuration.generateToken(20, 20));
        HashMap<String,Object> state = new HashMap<String, Object>();

        state.put("apiKeySecret", key);
        state.put("controllerRelayId", id);
        state.put("locationId", location.getLocationId());
        state.put("name", name);
        state.put("token", Configuration.encrypt(location.getLocationId(), Configuration.generateToken(20, 30)));

        Transaction xaction = Transaction.getInstance();

        try {
            ControllerRelay relay = getCache().create(xaction, state);

            xaction.commit();
            return relay;
        }
        finally {
            xaction.rollback();
        }
    }

    static public @Nullable ControllerRelay getRelay(@Nonnull String id) throws PersistenceException {
        return getCache().get(id);
    }

    private String apiKeySecret; // encrypted
    @Index(type= IndexType.PRIMARY)
    private String controllerRelayId;
    @Index(type=IndexType.FOREIGN, identifies = Location.class)
    private String locationId;
    private String name;
    private String token; // encrypted

    public ControllerRelay() { }

    public @Nonnull String getApiKeySecret() {
        return apiKeySecret;
    }

    public @Nonnull String getControllerRelayId() {
        return controllerRelayId;
    }

    public @Nonnull Location getLocation() throws PersistenceException {
        Location l = Location.getLocation(locationId);

        if( l == null ) {
            throw new DataIntegrityException("No such location for relay " + controllerRelayId + ": " + locationId);
        }
        return l;
    }

    public @Nonnull String getLocationId() {
        return locationId;
    }

    public @Nonnull String getName() {
        return name;
    }

    public @Nonnull String getToken() {
        return token;
    }

    public boolean isValidForCache() {
        return false;
    }

    public void modify(@Nonnull String name) throws PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<ControllerRelay> memento = new Memento<ControllerRelay>(this);

        memento.save(state);
        token = Configuration.encrypt(locationId, token);
        state.put("name", name);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.name = name;
    }

    public void setToken(@Nonnull String token) throws PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<ControllerRelay> memento = new Memento<ControllerRelay>(this);

        memento.save(state);
        token = Configuration.encrypt(locationId, token);
        state.put("token", token);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.token = token;
    }
}
