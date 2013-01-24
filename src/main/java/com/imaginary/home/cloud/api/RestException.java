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

package com.imaginary.home.cloud.api;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/16/13 9:03 AM</p>
 *
 * @author George Reese
 */
public class RestException extends Exception {
    static public final String INTERNAL_ERROR    = "InternalError";
    static public final String INVALID_OPERATION = "InvalidOperation";
    static public final String NO_SUCH_RESOURCE  = "NoSuchResource";

    private String description;
    private int    status;

    public RestException(@Nonnegative int status, @Nonnull String description) {
        super(description);
        this.status = status;
        this.description = description;
    }

    public RestException(@Nonnegative int status, @Nonnull String description, @Nonnull String message) {
        super(message);
        this.status = status;
        this.description = description;
    }

    public RestException(@Nonnegative int status, @Nonnull String description, @Nonnull Throwable cause) {
        super(cause);
        this.status = status;
        this.description = description;
    }

    public @Nonnull String getDescription() {
        return description;
    }

    public @Nonnegative int getStatus() {
        return status;
    }
}
