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

import javax.annotation.Nonnull;
import java.util.TimeZone;

/**
 * A location such as your home or a cabin or apartment or 30,000 sq ft villa.
 * <p>Created by George Reese: 1/14/13 9:10 AM</p>
 * @author George Reese
 */
public class Location {
    private String   apiAccessId;
    private String   apiAccessSecret; // encrypted
    private String   description;
    private String   locationId;
    private String   name;
    private String   pairingCode;
    private long     pairingExpiration;
    private TimeZone timeZone;

    public @Nonnull String[] pair(@Nonnull String code) {
        if( pairingExpiration < System.currentTimeMillis() ) {
            // disallow
        }
        if( !code.equals(pairingCode) ) {
            // disallow
        }
        String[] keys = new String[2];

        keys[0] = apiAccessId; // TODO: generate random ID
        keys[1] = apiAccessSecret; // TODO: generate random secret
        // TODO: encrypt access key
        // TODO: save
        return keys;
    }
}

