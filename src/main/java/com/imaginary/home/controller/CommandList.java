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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class CommandList implements Iterable<JSONObject> {
    private ArrayList<JSONObject> commands;
    private String                serviceId;

    public CommandList(@Nonnull String serviceId, @Nonnull JSONObject ... commands) {
        this.serviceId = serviceId;
        this.commands = new ArrayList<JSONObject>();
        Collections.addAll(this.commands, commands);
    }

    public @Nonnull JSONObject get(int which) {
        return commands.get(which);
    }

    @Override
    public Iterator<JSONObject> iterator() {
        return commands.iterator();
    }

    public @Nonnull String getServiceId() {
        return serviceId;
    }
}
