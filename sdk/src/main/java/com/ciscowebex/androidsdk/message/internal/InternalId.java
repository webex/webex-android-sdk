package com.ciscowebex.androidsdk.message.internal;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import com.github.benoitdion.ln.Ln;

import java.util.Arrays;
import java.util.List;

public class InternalId {

    enum Type {

        MESSAGE_ID("MESSAGE"),
        PEOPLE_ID("PEOPLE"),
        ROOM_ID("ROOM");

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
            for(Type v : values()) {
                if (v.getKeyword().equalsIgnoreCase(value)) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private String id;

    private Type type;

    public InternalId(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    public static String translate(String hydraId) {
        InternalId id = from(hydraId);
        return id == null ? hydraId : id.getId();
    }

    public static InternalId from(String hydraId) {
        try {
            String decodeStr = new String(Base64.decode(hydraId, Base64.URL_SAFE), "UTF-8");
            if (TextUtils.isEmpty(decodeStr)) {
                return null;
            }
            String[] subs = decodeStr.split("/");
            return new InternalId(Type.getEnum(subs[subs.length - 2]), subs[subs.length - 1]);
        } catch (Exception e) {
            Ln.d(e, "can't decode hydra id : " + hydraId);
        }
        return null;
    }

    public String toHydraId() {
        String string = "ciscospark://us/" + type.getKeyword() + "/" + id;
        return new String(Base64.encode(string.getBytes(),Base64.NO_PADDING | Base64.URL_SAFE | Base64.NO_WRAP));
    }

    public String getId() {
        return this.id;
    }

    public boolean is(Type type) {
        return this.type == type;
    }
}
