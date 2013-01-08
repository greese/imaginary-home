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
package com.imaginary.home.lighting;

import javax.annotation.Nonnull;
import java.awt.color.ColorSpace;
import java.util.Arrays;

public class Color {
    private ColorMode colorMode;
    private float[]   components;

    public Color(@Nonnull ColorMode colorMode, @Nonnull float ... components) {
        this.colorMode = colorMode;
        if( colorMode.equals(ColorMode.CIEXYZ) && components.length < 3 ) {
            float[] tmp = new float[3];

            tmp[0] = components[0];
            tmp[1] = components[1];
            tmp[2] = 1.0f - (components[0] + components[1]);
            this.components = tmp;
        }
        else {
            this.components = Arrays.copyOf(components, components.length);
        }
    }

    public @Nonnull ColorMode getColorMode() {
        return colorMode;
    }

    public @Nonnull float[] getComponents() {
        return Arrays.copyOf(components, components.length);
    }

    public @Nonnull Color convertTo(@Nonnull ColorMode mode) {
        switch( mode ) {
            case HSV:
                return convertToHSV();
            case RGB:
                return convertToRGB();
            case CIEXYZ:
                return convertToCIEXYZ();
        }
        throw new RuntimeException("Invalid color mode: " + mode);
    }

    public @Nonnull Color convertToCIEXYZ() {
        switch( colorMode ) {
            case RGB:
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

                float[] tmp = cs.toCIEXYZ(components);
                return new Color(ColorMode.CIEXYZ, tmp);
            case HSV:
                return convertToRGB().convertTo(ColorMode.CIEXYZ);
            case CIEXYZ:
                return this;
        }
        throw new RuntimeException("Invalid color mode: " + colorMode);
    }

    public @Nonnull Color convertToHSV() {
        switch( colorMode ) {
            case RGB:
                return new Color(ColorMode.HSV, java.awt.Color.RGBtoHSB((int)components[0], (int)components[1], (int)components[2], null));
            case HSV:
                return this;
            case CIEXYZ:
                return convertToRGB().convertTo(ColorMode.HSV);
        }
        throw new RuntimeException("Invalid color mode: " + colorMode);
    }

    public @Nonnull Color convertToRGB() {
        switch( colorMode ) {
            case RGB:
                return this;
            case HSV:
                java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(components[0] / 65535f, (components[1] * 100f) / 254f, 1f));

                return new Color(ColorMode.RGB, c.getRed(), c.getGreen(), c.getBlue());
            case CIEXYZ:
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

                float[] tmp = cs.toRGB(components);
                return new Color(ColorMode.RGB, tmp[0]*255, tmp[1]*255, (int)tmp[2]*255);
        }
        throw new RuntimeException("Invalid color mode: " + colorMode);
    }
}
