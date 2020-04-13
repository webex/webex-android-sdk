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

import java.util.List;

public class LocusControlModel {

    private String url;
    private LocusLockControlModel lock;
    private LocusRecordControlModel record;
    private List<LocusLinkModel> links;

    public LocusControlModel(String url, LocusLockControlModel lock, LocusRecordControlModel record, List<LocusLinkModel> links) {
        this.url = url;
        this.lock = lock;
        this.record = record;
        this.links = links;
    }

    public String getUrl() {
        return url;
    }

    public LocusLockControlModel getLock() {
        return lock;
    }

    public LocusRecordControlModel getRecord() {
        return record;
    }

    public List<LocusLinkModel> getLinks() {
        return links;
    }
}
