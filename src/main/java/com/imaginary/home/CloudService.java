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
package com.imaginary.home;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * <p>
 * A cloud service is a web service on the public internet that provides some kind of interface (UI, API, both)
 * for people/applications to interact with on the public internet and then route requests down to the home
 * behind the user's home firewall. The IHA daemon running in the home talks to one or more cloud services,
 * notifying it about the current status of all home automation devices, and fetching any commands posted by
 * users or their tools.
 * </p>
 * <p>
 *     This is the typical architecture for most home automation solutions with one key difference: this is totally
 *     open. Your device can operate against any number of cloud services as long as they support this web services
 *     protocol. If you use multiple services, they can cooperate together nicely.
 * </p>
 * <p>
 *     When the IHA daemon starts up, it looks at the configuration file to identify any current cloud services
 *     it works with. You can then add more services through the service pairing protocol. Once added, this IHA
 *     daemon will periodically communicate with the cloud service.
 * </p>
 */
public class CloudService {
    private String endpoint;
    private String name;
    private String serviceId;

    public CloudService(@Nonnull String serviceId, @Nonnull String name, @Nonnull String endpoint) {
        this.name = name;
        this.endpoint = endpoint;
        this.serviceId = serviceId;
    }

    public void fetchCommands() {
        // TODO: implement me
    }

    public @Nonnull String getEndpoint() {
        return endpoint;
    }

    public @Nonnull String getName() {
        return name;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean hasCommands() {
        // TODO: implement me
        return false;
    }

    public void pair(@Nonnull String pairingToken) {
        // TODO: implement me
    }

    public boolean postAlert() {
        // TODO: define API for posting an alert
        return false;
    }

    public boolean postState() {
        // TODO: implement me
        return false;
    }

    public boolean postResult(@Nonnull String cmdId, boolean stateChanged, @Nullable Map<String, Object> result, @Nullable Throwable exception) {
        // TODO: implement me
        return false;
    }
}
