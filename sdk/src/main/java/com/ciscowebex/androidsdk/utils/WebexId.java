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

package com.ciscowebex.androidsdk.utils;

import android.support.annotation.NonNull;
import android.util.Base64;
import com.ciscowebex.androidsdk.internal.Device;
import com.ciscowebex.androidsdk.internal.Service;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class WebexId {

    public enum Type {

        UNKNOWN("UNKNOWN"),
        ALARM("ALARM"),
        APPLICATION("APPLICATION"),
        APPLICATION_USAGE("APPLICATION_USAGE"),
        ATTACHMENT_ACTION("ATTACHMENT_ACTION"),
        AUTHORIZATION("AUTHORIZATION"),
        BOT("BOT"),
        CALL("CALL"),
        CALL_MEMBERSHIP("CALL_MEMBERSHIP"),
        CLUSTER("HYBRID_CLUSTER"),
        CONNECTOR("HYBRID_CONNECTOR"),
        CONTENT("CONTENT"),
        DEVICE("DEVICE"),
        EVENT("EVENT"),
        FILE("FILE"),
        FILE_TRANSCODING("FILE_TRANSCODING"),
        ISSUE("ISSUE"),
        LICENSE("LICENSE"),
        LICENSE_TEMPLATE("LICENSE_TEMPLATE"),
        LOCATION("LOCATION"),
        MEDIA_AGENT("MEDIA_AGENT"),
        MEMBERSHIP("MEMBERSHIP"),
        MESSAGE("MESSAGE"),
        ORGANIZATION("ORGANIZATION"),
        ORGANIZATION_GROUP("ORGANIZATION_GROUP"),
        PEOPLE("PEOPLE"),
        PLACE("PLACE"),
        POLICY("POLICY"),
        RESOURCE_GROUP("RESOURCE_GROUP"),
        RESOURCE_GROUP_MEMBERSHIP("RESOURCE_GROUP_MEMBERSHIP"),
        ROLE("ROLE"),
        ROOM("ROOM"),
        SITE("SITE"),
        SUBSCRIPTION("SUBSCRIPTION"),
        TEAM("TEAM"),
        TEAM_MEMBERSHIP("TEAM_MEMBERSHIP"),
        WEBHOOK("WEBHOOK"),
        WHITEBOARD("WHITEBOARD");

        private final String name;

        Type(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        public static Type getEnum(String value) {
            for (Type v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public static String uuid(String base64Id) {
        WebexId id = from(base64Id);
        return id == null ? base64Id : id.getUUID();
    }

    public static WebexId from(String base64Id) {
        try {
            byte[] bytes = Base64.decode(base64Id, Base64.URL_SAFE);
            if (Checker.isEmpty(bytes)) {
                return null;
            }
            String strings = new String(bytes, StandardCharsets.UTF_8);
            if (Checker.isEmpty(strings)) {
                return null;
            }
            String[] subs = strings.split("/");
            return new WebexId(Type.getEnum(subs[subs.length - 2]), subs[subs.length - 3], subs[subs.length - 1]);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String DEFAULT_CLUSTER = "us";

    public static String DEFAULT_CLUSTER_ID = "urn:TEAM:us-east-2_a";

    private String uuid;

    private Type type;

    private String cluster;

    public WebexId(@NonNull Type type, @NonNull String cluster, @NonNull String uuid) {
        this.type = type;
        this.uuid = uuid;
        this.cluster = (Checker.isEmpty(cluster) || cluster.equals(DEFAULT_CLUSTER_ID)) ? DEFAULT_CLUSTER : cluster;
    }

    public String getBase64Id() {
        String string = "ciscospark://" + cluster + "/" + type.toString() + "/" + uuid;
        return new String(Base64.encode(string.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE | Base64.NO_WRAP));
    }

    public String getUUID() {
        return this.uuid;
    }

    public boolean is(Type type) {
        return this.type == type;
    }

    public String getCluster() {
        return this.cluster;
    }

    public String getClusterId() {
        return this.cluster.equals(DEFAULT_CLUSTER) ? "urn:TEAM:us-east-2_a" : this.cluster;
    }

    public String getUrl(Device device) {
        String key = getClusterId() + ":identityLookup";
        String url = device.getServiceClusterUrl(key);
        if (url == null) {
            Ln.d("Cannot found cluster for " + key + ", use home cluster");
            url= Service.Conv.baseUrl(device);
        }
        if (this.is(WebexId.Type.ROOM)) {
            return url + "/conversations/" + getUUID();
        } else if (this.is(WebexId.Type.MESSAGE)) {
            return url + "/activities/" + getUUID();
        } else if (this.is(WebexId.Type.TEAM)) {
            return url + "/teams/" + getUUID();
        }
        return null;
    }

    @Override
    public String toString() {
        return getBase64Id();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof WebexId) {
            return getUUID().equals(((WebexId) o).getUUID());
        } else if (o instanceof String) {
            if (getUUID().equals(o)) {
                return true;
            } else {
                return this.equals(WebexId.from((String) o));
            }
        }
        return false;
    }
}
