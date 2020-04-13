/*
 * Copyright 2016-2020 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.internal.reachability;

public class NetworkConnectionStatus {
    private int networkType = -1;
    private String ipAddress;
    private boolean isBehindProxy = false;
    private boolean isConnected = false;
    private boolean proxyRequiresAuth = false;

    public NetworkConnectionStatus(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public NetworkConnectionStatus(boolean isConnected, boolean proxyRequiresAuth, int networkType, String ipAddress, boolean isBehindProxy) {
        this.isConnected = isConnected;
        this.proxyRequiresAuth = proxyRequiresAuth;
        this.networkType = networkType;
        this.ipAddress = ipAddress;
        this.isBehindProxy = isBehindProxy;
    }

    public int getNetworkType() {
        return networkType;
    }

    public void setNetworkType(int networkType) {
        this.networkType = networkType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isBehindProxy() {
        return isBehindProxy;
    }

    public void setBehindProxy(boolean behindProxy) {
        isBehindProxy = behindProxy;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isProxyRequiresAuth() {
        return proxyRequiresAuth;
    }

    public void setProxyRequiresAuth(boolean proxyRequiresAuth) {
        this.proxyRequiresAuth = proxyRequiresAuth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkConnectionStatus that = (NetworkConnectionStatus) o;
        return (isConnected == that.isConnected)
                && (networkType == that.networkType)
                && (ipAddress != null && ipAddress.equalsIgnoreCase(that.ipAddress))
                && (isBehindProxy == that.isBehindProxy);
    }

    public boolean isProxyChanged(NetworkConnectionStatus other) {
        if (other == null)
            return false;
        // proxy change detect 1) on => off 2) off => on 3) on => on but different proxy (different ip address)
        return (isBehindProxy != other.isBehindProxy) || (isBehindProxy && other.isBehindProxy && (ipAddress != null && !ipAddress.equalsIgnoreCase(other.ipAddress)));
    }
}

