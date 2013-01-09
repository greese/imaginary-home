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

import com.imaginary.home.lighting.LightingService;

import java.util.Properties;

public interface HomeAutomationSystem {
    public String getAPIEndpoint();

    public Properties getAuthenticationProperties();

    public Properties getCustomProperties();

    public LightingService getLightingService();

    public String getName();

    public String getVendor();
}
