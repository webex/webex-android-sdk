package com.ciscowebex.androidsdk.phone;

public interface PhoneObserver {

    /**
     * Callback when device is registering
     *
     * @since 2.2.0
     */
    void onRegistering();

    /**
     * Callback after device has registered
     *
     * @since 2.2.0
     */
    void onRegistered();

    /**
     * Callback when device is deregistering
     *
     * @since 2.2.0
     */
    void onDeregistering();

    /**
     * Callback after device has deregistered
     *
     * @since 2.2.0
     */
    void onDeregistered();

}
