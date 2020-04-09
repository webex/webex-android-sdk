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

import android.text.TextUtils;
import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;

public class TeamModel extends ObjectModel {

    private String teamColor;
    private String generalConversationUuid;
    private String encryptionKeyUrl;   // Required to post update color activities but not actually needed

    private ItemsModel<ConversationModel> conversations;

    public TeamModel() {
        super(ObjectModel.Type.team);
    }

    public TeamModel(String id) {
        this();
        setId(id);
    }

    public ItemsModel<ConversationModel> getConversations() {
        return conversations;
    }

    public void setConversations(ItemsModel<ConversationModel> conversations) {
        this.conversations = conversations;
    }

    public String getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(String teamColor) {
        this.teamColor = teamColor;
    }

    public String getGeneralConversationUuid() {
        return generalConversationUuid;
    }

    public void setGeneralConversationUuid(String generalConversationUuid) {
        this.generalConversationUuid = generalConversationUuid;
    }

    public void setEncryptionKeyUrl(String encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    @Override
    public void encrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.encrypt(key);
        encryptionKeyUrl = key.getKeyUrl();
    }

    @Override
    public void decrypt(KeyObject key) {
        if (key == null) {
            return;
        }
        super.decrypt(key);
        if (!TextUtils.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }
    }

}
