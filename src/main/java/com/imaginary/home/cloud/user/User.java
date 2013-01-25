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
import com.imaginary.home.cloud.Location;
import org.dasein.persist.Memento;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.SearchTerm;
import org.dasein.persist.Transaction;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;

/**
 * An individual user with one or more locations that they can manage.
 * <p>Created by George Reese: 1/14/13 9:48 AM</p>
 * @author George Reese
 */
public class User implements CachedItem {
    static private PersistentCache<User> cache;

    static public User create(String email, String firstName, String lastName, String password) throws PersistenceException {
        String userId;

        do {
            userId = UUID.randomUUID().toString();
        } while( getUserByUserId(userId) != null );
        HashMap<String,Object> state = new HashMap<String, Object>();

        state.put("email", email);
        state.put("firstName", firstName);
        state.put("lastName", lastName);
        state.put("password", Configuration.encrypt(password, userId + ":" + password));
        state.put("pairingPassword", Configuration.encrypt(password, userId + ":" + password));
        state.put("locationIds", new String[0]);

        Transaction xaction = Transaction.getInstance();

        try {
            User u = getCache().create(xaction, state);

            xaction.commit();
            return u;
        }
        finally {
            xaction.rollback();
        }
    }

    static private @Nonnull PersistentCache<User> getCache() throws PersistenceException {
        if( cache == null ) {
            //noinspection unchecked
            cache = (PersistentCache<User>)PersistentCache.getCache(User.class);
        }
        return cache;
    }

    static public @Nullable User getUserByEmail(@Nonnull String email) throws PersistenceException {
        return getCache().get(email.toLowerCase());
    }

    static public @Nullable User getUserByUserId(@Nonnull String userId) throws PersistenceException {
        Iterator<User> users = getCache().find(new SearchTerm("userId", userId)).iterator();

        if( !users.hasNext() ) {
            return null;
        }
        return users.next();
    }

    static public Iterable<User> listUsersForLocation(@Nonnull Location location) throws PersistenceException {
        return getCache().find(new SearchTerm("locationIds", location.getLocationId()));
    }

    @Index(type=IndexType.PRIMARY)
    private String   email;
    private String   firstName;
    @Index(type=IndexType.SECONDARY, multi = { "firstName" }, cascade = true)
    private String   lastName;
    @Index(type=IndexType.FOREIGN, identifies=Location.class)
    private String[] locationIds;
    private String   pairingPassword; // encrypted
    private String   password; // encrypted
    @Index(type= IndexType.SECONDARY)
    private String   userId;

    public User() { }

    public @Nonnull String getEmail() {
        return email;
    }

    public @Nonnull String getFirstName() {
        return firstName;
    }

    public @Nonnull String getLastName() {
        return lastName;
    }

    public @Nonnull Collection<Location> getLocations() throws PersistenceException {
        ArrayList<Location> locations = new ArrayList<Location>();

        if( locationIds != null ) {
            for( String id : locationIds ) {
                Location l = Location.getLocation(id);

                if( l != null ) {
                    locations.add(l);
                }
            }
        }
        return locations;
    }

    public @Nonnull String getUserId() {
        return userId;
    }

    public void grant(@Nonnull Location location) throws PersistenceException {
        TreeSet<String> ids = new TreeSet<String>();

        if( locationIds != null ) {
            Collections.addAll(ids, locationIds);
        }
        ids.add(location.getLocationId());
        String[] locationIds = ids.toArray(new String[ids.size()]);

        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<User> memento = new Memento<User>(this);

        memento.save(state);
        state.put("locationIds", locationIds);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.locationIds = locationIds;
    }

    @Override
    public boolean isValidForCache() {
        return false;
    }

    public boolean isPasswordMatch(@Nonnull String password) {
        return this.password.equals(Configuration.encrypt(password, userId + ":" + password));
    }

    public boolean isPairingPasswordMatch(@Nonnull String pairingCode) {
        return this.pairingPassword.equals(Configuration.encrypt(pairingCode, userId + ":" + pairingCode));
    }

    public void revoke(@Nonnull Location location) throws PersistenceException {
        TreeSet<String> ids = new TreeSet<String>();

        if( locationIds != null ) {
            for( String id : locationIds ) {
                if( !id.equals(location.getLocationId()) ) {
                    ids.add(id);
                }
            }
        }
        String[] locationIds = ids.toArray(new String[ids.size()]);

        HashMap<String,Object> state = new HashMap<String, Object>();
        Memento<User> memento = new Memento<User>(this);

        memento.save(state);
        state.put("locationIds", locationIds);
        Transaction xaction = Transaction.getInstance();

        try {
            getCache().update(xaction, this, state);
            xaction.commit();
        }
        finally {
            xaction.rollback();
        }
        this.locationIds = locationIds;
    }
}
