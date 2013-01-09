/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2013 George Reese - ALL RIGHTS RESERVED
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
package com.imaginary.home;

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
