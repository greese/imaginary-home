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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Any resource managed within a home automation system.
 */
public interface ManagedResource {
    /**
     * @return the manufacturer's model number that identifies what this resource is
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nullable String getModel() throws CommunicationException;

    /**
     * @return the name of the resource as it is recognized in the home automation system
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull String getName() throws CommunicationException;

    /**
     * @return the unique ID of this resource in the home automation system
     */
    public @Nonnull String getProviderId();

    public void toMap(Map<String,Object> map) throws CommunicationException;
}
