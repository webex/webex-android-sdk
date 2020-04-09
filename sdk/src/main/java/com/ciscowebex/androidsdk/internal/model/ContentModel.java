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

import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import me.helloworld.utils.Checker;

import java.util.ArrayList;

public class ContentModel extends ObjectModel implements MentionableModel, MarkdownableModel {

    public static class Category {
        public static final String IMAGES = "images";
        public static final String DOCUMENTS = "documents";
        public static final String VIDEOS = "videos";
        public static final String LINKS = "links";
    }

    private ItemsModel<FileModel> files;
    private String contentCategory;
    private ItemsModel<PersonModel> mentions;
    private ItemsModel<GroupMentionModel> groupMentions;
    // TODO private ItemsModel<Link> links;
    private ArrayList<String> contentOrder;
    private String markdown;

    public ContentModel(String category) {
        super(ObjectModel.Type.content);
        this.contentCategory = category;
    }

    public ItemsModel<FileModel> getFiles() {
        return files;
    }

    public String getContentCategory() {
        return contentCategory;
    }

    public boolean isImage() {
        return (ContentModel.Category.IMAGES.equals(contentCategory));
    }

    public boolean isFile() {
        return (ContentModel.Category.DOCUMENTS.equals(contentCategory));
    }

    public boolean isVideo() {
        return ContentModel.Category.VIDEOS.equals(contentCategory);
    }

    public boolean isLink() {
        return ContentModel.Category.LINKS.equals(contentCategory);
    }

    public void setFiles(ItemsModel<FileModel> files) {
        this.files = files;
    }

    public ItemsModel<FileModel> getContentFiles() {
        return files;
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

    public ArrayList<String> getContentOrder() {
        return contentOrder;
    }

    public void setContentOrder(ArrayList<String> contentOrder) {
        this.contentOrder = contentOrder;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    @Override
    public String toString() {
        return "contentCategory:" + contentCategory;
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
        if (getFiles() != null) {
            for (FileModel file : getFiles().getItems()) {
                file.encrypt(key);
            }
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
        if (getFiles() != null) {
            for (FileModel file : getFiles().getItems()) {
                file.decrypt(key);
            }
        }
    }

}
