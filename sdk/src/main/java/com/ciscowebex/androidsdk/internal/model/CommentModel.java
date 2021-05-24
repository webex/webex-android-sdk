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

import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import me.helloworld.utils.Checker;

public class CommentModel extends ObjectModel implements MentionableModel, MarkdownableModel {

    private ItemsModel<PersonModel> mentions;
    private ItemsModel<GroupMentionModel> groupMentions;
    private String markdown;

    public CommentModel() {
        super(ObjectModel.Type.comment);
    }

    public CommentModel(String message) {
        this();
        setDisplayName(message);
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    @Override
    public void encrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.encrypt(key);
        if (!Checker.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
        }
        if (!Checker.isEmpty(getMarkdown())) {
            setMarkdown(CryptoUtils.encryptToJwe(key, getMarkdown()));
        }
    }

    @Override
    public void decrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.decrypt(key);
        if (!Checker.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }
        if (!Checker.isEmpty(getMarkdown())) {
            setMarkdown(CryptoUtils.decryptFromJwe(key, getMarkdown()));
        }
    }

    public ItemsModel<PersonModel> getMentions() {
        return mentions;
    }

    public ItemsModel<GroupMentionModel> getGroupMentions() {
        return groupMentions;
    }

    public void setMentions(ItemsModel<PersonModel> mentions) {
        this.mentions = mentions;
    }

    public void setGroupMentions(ItemsModel<GroupMentionModel> groupMentions) {
        this.groupMentions = groupMentions;
    }
}
