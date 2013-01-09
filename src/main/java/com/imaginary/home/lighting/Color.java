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
package com.imaginary.home.lighting;

import javax.annotation.Nonnull;
import java.awt.color.ColorSpace;
import java.util.Arrays;

/**
 * A color in a particular color mode with one or more quantitative components. The components are stored internally
 * as float values and derive their meaning from the target color mode. Specifically, the stored components have the
 * following meanings:
 * <ul>
 *   <li>{@link ColorMode#CIEXYZ}
 *     <ol>
 *         <li>x - the CIE value from the X axis</li>
 *         <li>y - the CIE value from the Y axis</li>
 *         <li>z - the CIE value from the Z axis (optional)</li>
 *     </ol>
 *   </li>
 *   <li>{@link ColorMode#CT}
 *     <ol>
 *         <li>ct - the color temperature in mireds, from 154 to 500</li>
 *         <li>brightness - the brightness as a percentage value (optional)</li>
 *     </ol>
 *   </li>
 *   <li>
 *       {@link ColorMode#HSV}
 *       <ol>
 *           <li>hue - the hue as a degree value from 0.0 to 360.0</li>
 *           <li>saturation - the saturation as a percentage from 0.0 to 100.0</li>
 *       </ol>
 *   </li>
 *   <li>
 *       {@link ColorMode#RGB}
 *       <ol>
 *           <li>red - the amount of red as a percentage from 0.0 to 100.0</li>
 *           <li>green - the amount of green as a percentage from 0.0 to 100.0</li>
 *           <li>blue - the amount of blue as a percentage from 0.0 to 100.0</li>
 *       </ol>
 *   </li>
 * </ul>
 * <p>
 *     It's important to note that while this library supports operating using any arbitrary color space,
 *     the underlying home automation system likely supports only a subset of these color spaces. It's always
 *     best to use a native color space than rely on mapping being done by this library.
 * </p>
 */
public class Color {
    /**
     * Convenience method for constructing a color object from RGB values in the 0-255 range.
     * @param red the amount of red (1-255)
     * @param green the amount of green (1-255)
     * @param blue the amount of blue (1-255)
     * @return an RGB color object with the color components translated from 0-255 values to percentages
     */
    static public @Nonnull Color getRGB255(int red, int green, int blue) {
        return new Color(ColorMode.RGB, ((float)red*100)/255, ((float)green*100)/255, ((float)blue*100)/255);
    }

    /**
     * Convenience method for constructing a color object from fractional RGB values (0.0-1.0).
     * @param red the amount of red (0.0-1.0)
     * @param green the amount of green (0.0-1.0)
     * @param blue the amount of blue (0.0-1.0)
     * @return an RGB color object with the color components translated from 0-255 values to percentages
     */
    static public @Nonnull Color getRGBFraction(float red, float green, float blue) {
        return new Color(ColorMode.RGB, red*100f, green*100f, blue*100f);
    }

    /**
     * Convenience method for constructing a color object using the native percentage values expected by the Color class.
     * @param red the amount of red (0.0-100.0)
     * @param green the amount of green (0.0-100.0)
     * @param blue the amount of blue (0.0-100.0)
     * @return an RGB color object with the color components provided
     */
    static public @Nonnull Color getRGBPercent(float red, float green, float blue) {
        return new Color(ColorMode.RGB, red, green, blue);
    }

    private ColorMode colorMode;
    private float[]   components;

    /**
     * Constructs a new color object from the specified values. No validation occurs, so it is up to the caller
     * to provide valid values in this color space. See the class documentation for {@link Color} for a full
     * description of what component values are expected for different color spaces. Or use any of the convenience
     * factory methods to leverage expressing the color values in a manner familiar to you.
     * @param colorMode the color mode in which this color operates
     * @param components the component values that identify a color in this color space
     */
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

    /**
     * @return the color mode for this color
     */
    public @Nonnull ColorMode getColorMode() {
        return colorMode;
    }

    /**
     * @return the component values for this color
     */
    public @Nonnull float[] getComponents() {
        return Arrays.copyOf(components, components.length);
    }

    /**
     * Converts the color represented by this color object to a color in another color space.
     * @param mode the target color space
     * @return a converted color representing this color in the target space
     */
    public @Nonnull Color convertTo(@Nonnull ColorMode mode) {
        switch( mode ) {
            case HSV:
                return convertToHSV();
            case RGB:
                return convertToRGB();
            case CIEXYZ:
                return convertToCIEXYZ();
            case CT:
                return convertToCT();
        }
        throw new RuntimeException("Invalid color mode: " + mode);
    }

    /**
     * Converts this color to {@link ColorMode#CIEXYZ}.
     * @return the CIEXYZ representation of this color
     */
    public @Nonnull Color convertToCIEXYZ() {
        switch( colorMode ) {
            case RGB:
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);

                return new Color(ColorMode.CIEXYZ, cs.toCIEXYZ(new float[] { (components[0]*255)/100, (components[1]*255)/100, (components[2]*255)/100 }));
            case HSV:
                return convertToRGB().convertTo(ColorMode.CIEXYZ);
            case CT:
                return convertToRGB().convertTo(ColorMode.CIEXYZ);
            case CIEXYZ:
                return this;
        }
        throw new RuntimeException("Invalid color mode: " + colorMode);
    }

    /**
     * Identifies a version of whiteness that best approximates this color. Obviously, the idea of converting from
     * a color in another color space to white is nonsensical. This method, however, will attempt to approximate
     * the warmth of the current color as a white value.
     * @return this color represented as a white color temperature value
     */
    public @Nonnull Color convertToCT() {
        switch( colorMode ) {
            case CT:
                return this;
            case RGB:
                // this is made up nonsense
                int range = 500-154;
                float red = (components[0] * (range/2));
                float blue = (components[2] * (range/2));

                return new Color(ColorMode.CT, (154 + (range/2) + red)-blue, (components[0] + components[1] + components[2])/3);
            default:
                return convertToRGB().convertToCT();
        }
    }

    /**
     * Converts this color to {@link ColorMode#HSV}.
     * @return the HSV representation of this color
     */
    public @Nonnull Color convertToHSV() {
        switch( colorMode ) {
            case RGB:
                return new Color(ColorMode.HSV, java.awt.Color.RGBtoHSB((int)(components[0]*255), (int)(components[1]*255), (int)(components[2]*255), null));
            case HSV:
                return this;
            case CIEXYZ:
                return convertToRGB().convertTo(ColorMode.HSV);
            case CT:
                return convertToRGB().convertTo(ColorMode.HSV);
        }
        throw new RuntimeException("Invalid color mode: " + colorMode);
    }

    /**
     * Converts this color to {@link ColorMode#RGB}.
     * @return the RGB representation of this color
     */
    public @Nonnull Color convertToRGB() {
        switch( colorMode ) {
            case RGB:
                return this;
            case HSV:
                java.awt.Color c = new java.awt.Color(java.awt.Color.HSBtoRGB(components[0] / 65535f, (components[1] * 100f) / 254f, 1f));

                return new Color(ColorMode.RGB, ((float)c.getRed())/255f, ((float)c.getGreen())/255f, ((float)c.getBlue())/255f);
            case CIEXYZ:
                ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

                float[] tmp = cs.toRGB(components);
                return new Color(ColorMode.RGB, (tmp[0]*100f)/255f, (tmp[1]*255f)/100f, (tmp[2]*255f)/100f);
            case CT:
                // this is made up nonsense
                return new Color(ColorMode.RGB, 100f, 100f, 100f);
        }
        throw new RuntimeException("Invalid color mode: " + colorMode);
    }

    public String toString() {
        return (colorMode + " " + Arrays.toString(components));
    }
}
