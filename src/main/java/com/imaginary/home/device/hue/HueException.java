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
package com.imaginary.home.device.hue;

import com.imaginary.home.controller.CommunicationException;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/7/13 9:57 AM</p>
 *
 * @author George Reese
 * @version 2012.02 (bugzid: [FOGBUGZID])
 * @since 2012.02
 */
public class HueException extends CommunicationException {
    public HueException(String msg) {
        super(msg);
    }

    public HueException(Throwable cause) {
        super(cause);
    }

    public HueException(int statusCode, String msg) {
        super(statusCode, msg);
    }
}
