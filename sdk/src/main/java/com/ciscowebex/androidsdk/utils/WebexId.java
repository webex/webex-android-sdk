package com.ciscowebex.androidsdk.utils;

import android.text.TextUtils;
import android.util.Base64;

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
            String decodeStr = new String(Base64.decode(hydraId, Base64.URL_SAFE), "UTF-8");
            if (TextUtils.isEmpty(decodeStr)) {
                return null;
            }
            String[] subs = decodeStr.split("/");
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
