/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2013 enStratus Networks Inc - ALL RIGHTS RESERVED
 *
 * ====================================================================
 *  NOTICE: All information contained herein is, and remains the
 *  property of enStratus Networks Inc. The intellectual and technical
 *  concepts contained herein are proprietary to enStratus Networks Inc
 *  and may be covered by U.S. and Foreign Patents, patents in process,
 *  and are protected by trade secret or copyright law. Dissemination
 *  of this information or reproduction of this material is strictly
 *  forbidden unless prior written permission is obtained from
 *  enStratus Networks Inc.
 * ====================================================================
 */
package com.imaginary.hue;

/**
 * [Class Documentation]
 * <p>Created by George Reese: 1/7/13 9:57 AM</p>
 *
 * @author George Reese
 * @version 2012.02 (bugzid: [FOGBUGZID])
 * @since 2012.02
 */
public class HueException extends Exception {
    private int statusCode = 0;

    public HueException(String msg) {
        super(msg);
    }

    public HueException(Throwable cause) {
        super(cause);
    }

    public HueException(int statusCode, String msg) {
        super(msg);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
