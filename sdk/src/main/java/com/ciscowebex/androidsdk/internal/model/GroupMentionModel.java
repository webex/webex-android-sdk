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

package com.ciscowebex.androidsdk.internal.model;

import android.support.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class GroupMentionModel extends ObjectModel {

    public enum Type {
        @SerializedName("all")
        ALL("all"),
        @SerializedName("here")
        HERE("here");

        String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static @Nullable GroupMentionModel.Type convertStringToType(String value) {
            if (value != null) {
                for (GroupMentionModel.Type type : GroupMentionModel.Type.values()) {
                    if (value.equalsIgnoreCase(type.getValue())) {
                        return type;
                    }
                }
            }
            return null;
        }
    }

    private GroupMentionModel.Type groupType;

    public GroupMentionModel() {
        super(ObjectModel.Type.groupMention);
    }

    public GroupMentionModel(GroupMentionModel.Type groupType) {
        this();
        this.groupType = groupType;
    }

    public GroupMentionModel.Type getGroupType() {
        return groupType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMentionModel)) return false;
        GroupMentionModel that = (GroupMentionModel) o;
        return groupType == that.groupType;
    }

    @Override
    public int hashCode() {
        return groupType.hashCode();
    }
}
