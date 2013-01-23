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

public class CommunicationException extends Exception {
    private int statusCode = 0;

    public CommunicationException(@Nonnull String msg) {
        super(msg);
    }

    public CommunicationException(@Nonnull Throwable cause) {
        super(cause);
    }

    public CommunicationException(int statusCode, @Nonnull String msg) {
        super(msg);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
