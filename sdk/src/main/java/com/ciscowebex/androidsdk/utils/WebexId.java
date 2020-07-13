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

import android.util.Base64;
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
            return new WebexId(subs[subs.length - 3], Type.getEnum(subs[subs.length - 2]), subs[subs.length - 1]);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String uuid;

    private Type type;

    private String cluster;

    public WebexId(Type type, String uuid) {
        this("us", type, uuid);
    }

    public WebexId(String cluster, Type type, String uuid) {
        this.type = type;
        this.uuid = uuid;
        this.cluster = cluster == null ? "us" : cluster;
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

    public boolean belong(String cluster) {
        return this.cluster.equals(cluster);
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
