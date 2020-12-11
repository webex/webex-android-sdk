/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.phone;

/**
 * A AdvancedSetting represents a setting item for a call. Each subclass is a setting item.
 * <p>
 * These settings are for special use cases and usually do not need to be set.
 *
 * @see Phone#setAdvancedSetting(AdvancedSetting)
 * @since 2.6.0
 */
public abstract class AdvancedSetting<T> {

    private T value;

    private T defaultValue;

    /**
     * Returns the value of this setting item.
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns the default value of this setting item.
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    protected AdvancedSetting(T value, T defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return "AdvancedSetting{" +
                "name=" + this.getClass() +
                ", value=" + value +
                ", default=" + defaultValue +
                '}';
    }

    /**
     * Enable or disable the video mosaic for error-concealment when data loss in network. The default is enable.
     *
     * @since 2.6.0
     */
    public static class VideoEnableDecoderMosaic extends AdvancedSetting<Boolean> {

        public static boolean defaultVaule = true;

        public VideoEnableDecoderMosaic(boolean value) {
            super(value, defaultVaule);
        }

    }

    /**
     * Set the max sending fps for video for the call. If 0, default value of 30 is used.
     *
     * @since 2.6.0
     */
    public static class VideoMaxTxFPS extends AdvancedSetting<Integer> {

        public static int defaultVaule = 0;

        public VideoMaxTxFPS(int value) {
            super(value, defaultVaule);
        }
    }

    /**
     * Use android.hardware.camera2.CameraDevice or use android.hardware.Camera. The default is camera2.
     * This setting is invalid on Android API 21.
     *
     * @since 2.6.0
     */
    public static class VideoEnableCamera2 extends AdvancedSetting<Boolean> {

        public static boolean defaultVaule = true;

        public VideoEnableCamera2(boolean value) {
            super(value, defaultVaule);
        }
    }

    /**
     * Set the max screen capture fps for screen share for the call. If 0, default value of 5 is used.
     * Range of this value is 1 to 10
     * If set the value >5 and battery / CPU are not acceptable on the device, then should set the value <= 5.
     *
     * @since 2.7.0
     */
    public static class ShareMaxCaptureFPS extends AdvancedSetting<Integer> {
        public static int defaultValue = 0;

        public ShareMaxCaptureFPS(int value) {
            super(value, defaultValue);
        }

    }
}
