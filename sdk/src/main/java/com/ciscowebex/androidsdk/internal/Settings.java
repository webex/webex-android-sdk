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

package com.ciscowebex.androidsdk.internal;

import android.content.Context;
import android.content.SharedPreferences;
import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.utils.Json;
import me.helloworld.utils.Objects;

import static android.content.Context.MODE_PRIVATE;

public class Settings {

    public static Settings shared = new Settings();

    private SharedPreferences store;
    private SharedPreferences keep;

    private Settings() {}

    public void init(Context context) {
        this.store = context.getSharedPreferences(Webex.class.getPackage().getName(), MODE_PRIVATE);
        this.keep = context.getSharedPreferences(Webex.class.getPackage().getName() + ".keep", MODE_PRIVATE);
    }

    public void store(String key, String value) {
        if (store != null) {
            store.edit().putString(key, value).apply();
        }
    }

    public void store(String key, boolean value) {
        if (store != null) {
            store.edit().putBoolean(key, value).apply();
        }
    }

    public void storeKeep(String key, String value) {
        if (keep != null) {
            keep.edit().putString(key, value).apply();
        }
    }

    public void storeKeep(String key, boolean value) {
        if (keep != null) {
            keep.edit().putBoolean(key, value).apply();
        }
    }

    public void clear(String key) {
        if (store != null) {
            store.edit().remove(key).apply();
        }
    }

    public void clearKeep(String key) {
        if (keep != null) {
            keep.edit().remove(key).apply();
        }
    }

    public void clear() {
        if (store != null) {
            store.edit().clear().apply();
        }
    }

    public void clearKeep() {
        if (keep != null) {
            keep.edit().clear().apply();
        }
    }

    public String get(String key, String defaultValue) {
        if (store != null) {
            return store.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public boolean get(String key, boolean defaultValue) {
        if (store != null) {
            return store.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public <T> T get(String key, Class<T> clazz, T defaultValue) {
        if (store != null) {
            String value = store.getString(key, null);
            if (value != null) {
                return Objects.defaultIfNull(Json.fromJson(value, clazz), defaultValue);
            }
        }
        return defaultValue;
    }

    public String getKeep(String key, String defaultValue) {
        if (keep != null) {
            return keep.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public boolean getKeep(String key, boolean defaultValue) {
        if (keep != null) {
            return keep.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public <T> T getKeep(String key, Class<T> clazz, T defaultValue) {
        if (keep != null) {
            String value = keep.getString(key, null);
            if (value != null) {
                return Objects.defaultIfNull(Json.fromJson(value, clazz), defaultValue);
            }
        }
        return defaultValue;
    }

}
