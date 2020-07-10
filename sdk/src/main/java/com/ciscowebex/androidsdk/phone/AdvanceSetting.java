package com.ciscowebex.androidsdk.phone;

public abstract class AdvanceSetting<T> {

    private T value;

    private T defaultValue;

    public T getValue() {
        return value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    protected AdvanceSetting(T value, T defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return "AdvanceSetting{" +
                "name=" + this.getClass() +
                ", value=" + value +
                ", defaultValue=" + defaultValue +
                '}';
    }

    public static class VideoEnableDecoderMosaic extends AdvanceSetting<Boolean> {

        public VideoEnableDecoderMosaic(boolean value) {
            super(value, true);
        }

    }
}
