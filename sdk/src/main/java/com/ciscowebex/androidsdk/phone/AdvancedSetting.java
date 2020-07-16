package com.ciscowebex.androidsdk.phone;

public abstract class AdvancedSetting<T> {

    private T value;

    private T defaultValue;

    public T getValue() {
        return value;
    }

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
                ", defaultValue=" + defaultValue +
                '}';
    }

    public static class VideoEnableDecoderMosaic extends AdvancedSetting<Boolean> {

        public VideoEnableDecoderMosaic(boolean value) {
            super(value, true);
        }

    }

    public static class VideoMaxTxFPS extends AdvancedSetting<Integer> {

        public VideoMaxTxFPS(int value) {
            super(value, 0);
        }
    }

}
