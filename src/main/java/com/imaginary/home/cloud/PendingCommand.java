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

import com.imaginary.home.cloud.device.Device;
import org.dasein.persist.Memento;
import org.dasein.persist.PersistenceException;
import org.dasein.persist.PersistentCache;
import org.dasein.persist.SearchTerm;
import org.dasein.persist.Transaction;
import org.dasein.persist.annotations.Index;
import org.dasein.persist.annotations.IndexType;
import org.dasein.util.CachedItem;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/26/13 9:30 PM</p>
 * @author George Reese
 */
public class PendingCommand implements CachedItem {
    static private PersistentCache<PendingCommand> cache;

    static private PersistentCache<PendingCommand> getCache() throws PersistenceException {
        if( cache == null ) {
            //noinspection unchecked
            cache = (PersistentCache<PendingCommand>)PersistentCache.getCache(PendingCommand.class);
        }
        return cache;
    }

    static public @Nullable PendingCommand getCommand(@Nonnull String id) throws PersistenceException {
        return getCache().get(id);
    }

    static public @Nonnull Collection<PendingCommand> getCommandsToSend(@Nonnull ControllerRelay forRelay, boolean markSent) throws PersistenceException {
        Collection<PendingCommand> list = getCache().find(new SearchTerm("state", PendingCommandState.WAITING), new SearchTerm("relayId", forRelay.getControllerRelayId()));

        if( markSent ) {
            ArrayList<PendingCommand> marked = new ArrayList<PendingCommand>();

            try {
                for( PendingCommand cmd : list ) {
                    Memento<PendingCommand> memento = new Memento<PendingCommand>(cmd);
                    Map<String,Object> state = new HashMap<String, Object>();

                    memento.save(state);
                    state.put("state", PendingCommandState.SENT);
                    state.put("sentAt", System.currentTimeMillis());
                    state = memento.getState();

                    Transaction xaction = Transaction.getInstance();

                    try {
                        getCache().update(xaction, cmd, state);
                        xaction.commit();
                        marked.add(cmd);
                    }
                    finally {
                        xaction.rollback();
                    }
                }
            }
            catch( Throwable t ) {
                t.printStackTrace();
            }
            list = marked;
        }
        return list;
    }

    static public boolean hasCommands(@Nonnull ControllerRelay relay) throws PersistenceException {
        return getCache().find(new SearchTerm("state", PendingCommandState.WAITING), new SearchTerm("relayId", relay.getControllerRelayId())).iterator().hasNext();
    }

    static public @Nonnull PendingCommand[] queue(@Nonnull TimePeriod<?> timeout, @Nonnull String[] commandsAsJSON, @Nonnull Device ... devices) throws PersistenceException {
        if( devices.length < 1 ) {
            throw new PersistenceException("No devices specified");
        }
        long timeoutMillis = timeout.convertTo(TimePeriod.MILLISECOND).longValue();
        HashMap<String,TreeSet<String>> relays = new HashMap<String, TreeSet<String>>();

        for( Device d : devices ) {
            TreeSet<String> deviceIds;

            if( relays.containsKey(d.getRelayId()) ) {
                deviceIds = relays.get(d.getRelayId());
            }
            else {
                deviceIds = new TreeSet<String>();
                relays.put(d.getRelayId(), deviceIds);
            }
            deviceIds.add(d.getDeviceId());
        }
        String groupId;

        do {
            groupId = UUID.randomUUID().toString();
        } while( getCache().find(new SearchTerm("groupId", groupId)).iterator().hasNext() );

        Set<Map.Entry<String,TreeSet<String>>> relaySet= relays.entrySet();
        PendingCommand[] results = new PendingCommand[relaySet.size() * commandsAsJSON.length];
        int i = 0;

        for( Map.Entry<String,TreeSet<String>> entry : relaySet ) {
            TreeSet<String> set = entry.getValue();
            String[] deviceIds = entry.getValue().toArray(new String[set.size()]);
            String relayId = entry.getKey();

            for( String cmd : commandsAsJSON ) {
                String id;

                do {
                    id = UUID.randomUUID().toString();
                } while( getCommand(id) != null );

                HashMap<String,Object> state = new HashMap<String, Object>();

                state.put("command", cmd);
                state.put("deviceIds", deviceIds);
                state.put("groupId", groupId);
                state.put("pendingCommandId", id);
                state.put("relayId", relayId);
                state.put("state", PendingCommandState.WAITING);
                state.put("timeout", timeoutMillis);
                state.put("sentAt", 0);

                Transaction xaction = Transaction.getInstance();

                try {
                    results[i++] = getCache().create(xaction, state);
                    xaction.commit();
                }
                finally {
                    xaction.rollback();
                }
            }
        }
        return results;
    }

    private String              command;
    @Index(type=IndexType.SECONDARY)
    private String[]            deviceIds;
    @Index(type=IndexType.SECONDARY)
    private String              groupId;
    @Index(type= IndexType.PRIMARY)
    private String              pendingCommandId;
    @Index(type=IndexType.FOREIGN, identifies= ControllerRelay.class)
    private String              relayId;
    private long                sentAt;
    @Index(type=IndexType.SECONDARY, multi={"relayId"})
    private PendingCommandState state;
    private long                timeout;

    public PendingCommand() { }

    public @Nonnull String getCommand() {
        return command;
    }

    public @Nonnull String[] getDeviceIds() {
        return deviceIds;
    }

    public @Nonnull String getGroupId() {
        return groupId;
    }

    public @Nonnull String getPendingCommandId() {
        return pendingCommandId;
    }

    public @Nonnull String getRelayId() {
        return relayId;
    }

    public @Nonnegative long getSentAt() {
        return sentAt;
    }

    public @Nonnegative long getTimeout() {
        return timeout;
    }

    public boolean isValidForCache() {
        return false;
    }
}
