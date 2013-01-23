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
import java.util.Properties;

public interface HomeAutomationSystem {
    public @Nonnull String getAPIEndpoint();

    public @Nonnull Properties getAuthenticationProperties();

    public @Nonnull Properties getCustomProperties();

    public @Nonnull String getId();

    public @Nonnull String getName();

    public @Nonnull String getVendor();

    public void init(@Nonnull String id, @Nonnull Properties auth, @Nonnull Properties custom);

    public @Nonnull Properties pair(@Nonnull String applicationName) throws CommunicationException;
}
