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

package com.ciscowebex.androidsdk.message.internal;

import com.ciscowebex.androidsdk.message.LocalFile;
import com.ciscowebex.androidsdk.message.Mention;
import com.ciscowebex.androidsdk.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DraftImpl implements Message.Draft {

    private Message.Text text;
    private List<LocalFile> files;
    private List<Mention> mentions;
    private Message parent;

    public DraftImpl(Message.Text text) {
        this.text = text;
    }

    public Message.Draft addAttachments(LocalFile... files) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.addAll(Arrays.asList(files));
        return this;
    }

    public Message.Draft addMentions(Mention... mentions) {
        if (this.mentions == null) {
            this.mentions = new ArrayList<>();
        }
        this.mentions.addAll(Arrays.asList(mentions));
        return this;
    }

    public Message.Draft setParent(Message parent) {
        this.parent = parent;
        return this;
    }

    public Message.Text getText() {
        return text;
    }

    public List<LocalFile> getFiles() {
        return files;
    }

    public List<Mention> getMentions() {
        return mentions;
    }

    public Message getParent() {
        return parent;
    }

}
