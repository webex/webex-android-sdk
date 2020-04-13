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
import me.helloworld.utils.annotation.StringPart;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class WebexId {

    public enum Type {

        MESSAGE_ID("MESSAGE"),
        PEOPLE_ID("PEOPLE"),
        ROOM_ID("ROOM"),
        MEMBERSHIP_ID("MEMBERSHIP"),
        ORGANIZATION_ID("ORGANIZATION");

        private String keyword;

        Type(String keyword) {
            this.keyword = keyword;
        }

        public String getKeyword() {
            return keyword;
        }

        @Override
        public String toString() {
            return this.getKeyword();
        }

        public static Type getEnum(String value) {
            for (Type v : values()) {
                if (v.getKeyword().equalsIgnoreCase(value)) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private String id;

    private Type type;

    public WebexId(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    public static String translate(String hydraId) {
        WebexId id = from(hydraId);
        return id == null ? hydraId : id.getId();
    }

    public static WebexId from(String hydraId) {
        try {
            byte[] bytes = Base64.decode(hydraId, Base64.URL_SAFE);
            if (Checker.isEmpty(bytes)) {
                return null;
            }
            String strings = new String(bytes, StandardCharsets.UTF_8);
            if (Checker.isEmpty(strings)) {
                return null;
            }
            String[] subs = strings.split("/");
            return new WebexId(Type.getEnum(subs[subs.length - 2]), subs[subs.length - 1]);
        } catch (Exception ignored) {
        }
        return null;
    }

    public String toHydraId() {
        String string = "ciscospark://us/" + type.getKeyword() + "/" + id;
        return new String(Base64.encode(string.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE | Base64.NO_WRAP));
    }

    public String getId() {
        return this.id;
    }

    public boolean is(Type type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return "WebexId{" +
                "id='" + id + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof WebexId) {
            return getId().equals(((WebexId) o).getId());
        } else if (o instanceof String) {
            if (getId().equals(o)) {
                return true;
            } else {
                return this.equals(WebexId.from((String) o));
            }
        }
        return false;
    }
}
