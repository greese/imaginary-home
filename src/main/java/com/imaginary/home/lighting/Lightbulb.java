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

import com.imaginary.home.CommunicationException;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Future;

/**
 * A light bulb that may be individually controlled in a home automation system.
 */
public interface Lightbulb {
    /**
     * General purpose method for changing the color of a bulb regardless of the underlying color support mechanism.
     * If the new color is non-white (not {@link ColorMode#CT}) and the underlying light bulb is not color, this
     * method will simply return a future with a false return value.
     * @param newColor the new color to which the bulb should be set
     * @param transitionTime the time period over which the transition should be made
     * @return true if the bulb changed state as a result of this call
     * @throws CommunicationException an error occurred talking with the API call
     */
    public @Nonnull Future<Boolean> changeColor(@Nonnull Color newColor, @Nullable TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Convenience operation for setting color in the {@link ColorMode#CIEXYZ} color space. Note that specifying a Z value
     * is neither expected nor necessary.
     * @param x the X value in the CIEXYZ color space (valid values are sorta 0.0 to 1.0)
     * @param y the Y value in the CIEXYZ color space (valid values are sorta 0.0 to 1.0)
     * @param transitionTime the time period over which the transition should be made
     * @return true if the bulb changed state as a result of this call
     * @throws CommunicationException an error occurred talking with the API call
     */
    public @Nonnull Future<Boolean> changeColorCIEXYZ(@Nonnegative float x, @Nonnegative float y, @Nullable TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Convenience operation for setting color in the {@link ColorMode#HSV} color space.
     * @param hue the hue as a number of degrees from 0.0 to 360.0
     * @param saturation saturation as a percentage from 0.0 to 100.0
     * @param transitionTime the time period over which the transition should be made
     * @return true if the bulb changed state as a result of this call
     * @throws CommunicationException an error occurred talking with the API call
     */
    public @Nonnull Future<Boolean> changeColorHSV(@Nonnegative float hue, @Nonnegative float saturation, @Nullable TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Convenience operation for setting color in the {@link ColorMode#RGB} color space.
     * @param red a percentage value between 0.0 and 100.0 representing the amount of red
     * @param green a percentage value between 0.0 and 100.0 representing the amount of green
     * @param blue a percentage value between 0.0 and 100.0 representing the amount of blue
     * @param transitionTime the time period over which the transition should be made
     * @return true if the bulb changed state as a result of this call
     */
    public @Nonnull Future<Boolean> changeColorRGB(@Nonnegative float red, @Nonnegative float green, @Nonnegative float blue, @Nullable TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Convenience operation for setting &quot;color&quot; in the {@link ColorMode#CT} color space.
     * @param warmthInMireds the new warmth for the bulb in mireds (valid values are between 154 and 500)
     * @param brightness percent brightness (valid values are 0.0 to 100.0)
     * @param transitionTime the time period over which the transition should be made
     * @return true if the bulb changed state as a result of this call
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> changeWhite(final @Nonnegative int warmthInMireds, final @Nonnegative float brightness, final @Nullable TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Fades a bulb's brightness within it's current color space from the current value to 0 and then turns the bulb off.
     * The fading process will take place evenly over the course of the specified transition time.
     * @param transitionTime the time period over which the fade out will occur
     * @return true if the fade resulted in a change of state to the bulb
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> fadeOff(final @Nonnull  TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Fades a bulb's brightness up from the current value to 100% within the current color space. The fading process will
     * occur evenly over the course of the specified transition time.
     * @param transitionTime the time period over which the fade will occur
     * @return true if the fade resulted in a change of state to the bulb
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> fadeOn(final @Nonnull TimePeriod<?> transitionTime) throws CommunicationException;

    /**
     * Fades a bulb's brightness up or down from the current value to the target percentage. The fading process will
     * occur evenly over the course of the specified transition time.
     * @param transitionTime the time period over which the fade will occur
     * @param targetBrightness the target brightness as a percentage (0.0% to 100.0%)
     * @return true if the fade resulted in a change of state to the bulb
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> fadeOn(final @Nonnull TimePeriod<?> transitionTime, final @Nonnegative float targetBrightness) throws CommunicationException;

    /**
     * Flips the light off into the off state without changing any other values.
     * @return true if the fade resulted in a change of state to the bulb
     * @throws CommunicationException an error occurred talking with the API
     */
    public @Nonnull Future<Boolean> flipOff() throws CommunicationException;

    public @Nonnull Future<Boolean> flipOn() throws CommunicationException;

    public @Nonnegative float getBrightness() throws CommunicationException;

    public @Nonnull Color getColor() throws CommunicationException;

    public @Nonnull ColorMode getColorMode() throws CommunicationException;

    public @Nonnull String getName() throws CommunicationException;

    public @Nonnull String getProviderId();

    public boolean isOn() throws CommunicationException;

    public Future<Boolean> strobe(final @Nonnull TimePeriod<?> interval, final @Nullable TimePeriod<?> duration, final @Nonnull Color ... colors) throws CommunicationException;

    public boolean supportsBrightnessChanges() throws CommunicationException;

    public boolean supportsColor() throws CommunicationException;

    public boolean supportsColorChanges() throws CommunicationException;
}