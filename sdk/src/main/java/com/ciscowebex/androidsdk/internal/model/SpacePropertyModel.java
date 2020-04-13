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

package com.ciscowebex.androidsdk.internal.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.Set;

public class SpacePropertyModel extends ObjectModel {

    public enum Tag {
        ANNOUNCEMENT;

        public static SpacePropertyModel.Tag safeValueOf(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
    @SerializedName("tags")
    private Set<String> tagNames;

    public SpacePropertyModel() {
        super(ObjectModel.Type.spaceProperty);
        tagNames = new HashSet<>();
    }

    public SpacePropertyModel(SpacePropertyModel.Tag tag) {
        this();
        setTag(tag, true);
    }

    public Set<SpacePropertyModel.Tag> getTags() {
        Set<SpacePropertyModel.Tag> ret = new HashSet<>();
        for (String tagName : tagNames) {
            SpacePropertyModel.Tag tag = SpacePropertyModel.Tag.safeValueOf(tagName);
            if (tagName != null) {
                ret.add(tag);
            }
        }

        return ret;
    }

    public void setTag(SpacePropertyModel.Tag tag, boolean shouldExist) {
        if (shouldExist) {
            if (!tagNames.contains(tag.name()))
                tagNames.add(tag.name());
        } else {
            tagNames.remove(tag.name());
        }
    }

    public boolean containsTag(SpacePropertyModel.Tag tag) {
        return tagNames.contains(tag.name());
    }
}
