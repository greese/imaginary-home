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

import org.dasein.persist.Memento;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.SearchTerm;
import org.dasein.persist.Transaction;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;
import org.dasein.util.CalendarWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

/**
 * A location such as your home or a cabin or apartment or 30,000 sq ft villa.
 * <p>Created by George Reese: 1/14/13 9:10 AM</p>
 * @author George Reese
 */
public class Location implements CachedItem {
    static private PersistentCache<Location> locations;

    static public Location create(@Nonnull String name, @Nonnull String description, @Nonnull TimeZone timeZone) throws PersistenceException {
        String locationId;

        do {
            locationId = UUID.randomUUID().toString();
        } while( getLocation(locationId) != null );

        HashMap<String,Object> state = new HashMap<String, Object>();

        state.put("name", name);
        state.put("description", description);
        state.put("timeZone", timeZone);
        state.put("locationId", locationId);
        state.put("apiKeySecret", null);
        state.put("paired", false);
        state.put("pairingCode", null);
        state.put("pairingExpiration", 0L);
        state.put("timeZone", timeZone);

        Transaction xaction = Transaction.getInstance();

        try {
            Location l = getCache().create(xaction, state);

            xaction.commit();
            return l;
        }
        finally {
            xaction.rollback();
        }
    }

    static private @Nullable Location findForPairing(@Nonnull String pairingCode) throws PersistenceException {
        for( Location l : getCache().find(new SearchTerm("pairingCode", pairingCode)) ) {
            if( !l.isPaired() ) {
                return l;
            }
        }
        return null;
    }

    static private @Nonnull PersistentCache<Location> getCache() throws PersistenceException {
        if( locations == null ) {
            //noinspection unchecked
            locations = (PersistentCache<Location>)PersistentCache.getCache(Location.class);
        }
        return locations;
    }

    static public @Nullable Location getLocation(@Nonnull String locationId) throws PersistenceException {
        return getCache().get(locationId);
    }

    private String   apiKeySecret; // encrypted value
    private String   description;
    @Index(type= IndexType.PRIMARY)
    private String   locationId;
    private String   name;
    private boolean  paired;
    @Index(type=IndexType.SECONDARY)
    private String   pairingCode;
    private long     pairingExpiration;
    private TimeZone timeZone;

    public @Nonnull String getDescription() {
        return description;
    }

    public @Nonnull String getLocationId() {
        return locationId;
    }

    public @Nonnull String getName() {
        return name;
    }

    public @Nonnull TimeZone getTimeZone() {
        return timeZone;
    }

    public boolean isPaired() {
        return paired;
    }

    @Override
    public boolean isValidForCache() {
        return false;
    }

    public @Nullable String pair(@Nonnull String code) throws PersistenceException {
        if( pairingExpiration < System.currentTimeMillis() ) {
            return null;
        }
        if( !code.equals(pairingCode) ) {
            return null;
        }
        String key = Configuration.encrypt(locationId, Configuration.generateToken(20, 20));
        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<Location> memento = new Memento<Location>(this);

        memento.save(state);
        state.put("paired", true);
        state.put("apiKeySecret", key);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        paired = true;
        apiKeySecret = key;
        return Configuration.decrypt(locationId, apiKeySecret);
    }

    public @Nonnull String readyForPairing() throws PersistenceException {
        String pairingCode;

        do {
            pairingCode = Configuration.generateToken(10, 20);
        } while( findForPairing(pairingCode) != null );

        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<Location> memento = new Memento<Location>(this);
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*5);

        memento.save(state);
        state.put("pairingCode", pairingCode);
        state.put("pairingExpiration", timeout);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.pairingCode = pairingCode;
        this.pairingExpiration = timeout;
        return pairingCode;
    }

    public void setDescription(String description) throws PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<Location> memento = new Memento<Location>(this);

        memento.save(state);
        state.put("description", description);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.description = description;
    }

    public void setName(String name) throws PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<Location> memento = new Memento<Location>(this);

        memento.save(state);
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

    public void setTimeZone(TimeZone tz) throws PersistenceException {
        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<Location> memento = new Memento<Location>(this);

        memento.save(state);
        state.put("timeZone", tz);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.timeZone = tz;
    }
}

