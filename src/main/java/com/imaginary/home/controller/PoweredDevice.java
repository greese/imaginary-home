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
import java.util.concurrent.Future;

/**
 * A powered device is a managed resource that may be turned off and on.
 */
public interface PoweredDevice extends ManagedResource {
    /**
     * Flips the resource into the off state without changing any other values.
     * @return true if the operation resulted in a change in state
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> flipOff() throws CommunicationException;

    /**
     * Flips the resource into an on state without changing anything else about the resource.
     * @return true if the operation resulted in a change in state
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> flipOn() throws CommunicationException;


    /**
     * @return true if this resource is on
     * @throws CommunicationException an error occurred talking with the API
     */
    public boolean isOn() throws CommunicationException;
}
