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
package com.imaginary.home.controller;

import org.json.JSONObject;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class ScheduledCommandList extends CommandList implements Iterable<JSONObject>, Comparable<ScheduledCommandList> {
    private long                  executeAfter;
    private String                scheduleId;

    public ScheduledCommandList(@Nonnull String serviceId, @Nonnull String scheduleId, @Nonnegative long when, @Nonnull JSONObject ... commands) {
        super(serviceId, commands);
        this.scheduleId = scheduleId;
        executeAfter = when;
    }

    @Override
    public int compareTo(ScheduledCommandList scheduledCommand) {
        if( scheduledCommand == null ) {
            return -1;
        }
        if( scheduledCommand == this ) {
            return 0;
        }
        int x = (new Long(executeAfter)).compareTo(scheduledCommand.executeAfter);

        if( x == 0 ) {
            x = scheduleId.compareTo(scheduledCommand.scheduleId);
        }
        return x;
    }

    @Override
    public boolean equals(Object ob) {
        if( ob == null ) {
            return false;
        }
        if( ob == this ) {
            return true;
        }
        if( !getClass().getName().equals(ob.getClass().getName()) ) {
            return false;
        }
        ScheduledCommandList l = (ScheduledCommandList)ob;

        return (executeAfter == l.executeAfter && scheduleId.equals(l.scheduleId));
    }

    public @Nonnegative long getExecuteAfter() {
        return executeAfter;
    }

    public @Nonnull String getScheduleId() {
        return scheduleId;
    }

    @Override
    public int hashCode() {
        return (executeAfter + ":" + scheduleId).hashCode();
    }
}
