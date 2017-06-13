package com.cisco.spark.android.model;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.cisco.spark.android.R;
import com.cisco.spark.android.util.CryptoUtils;

import java.io.IOException;
import java.text.ParseException;

public class Team extends ActivityObject {
    private String teamColor;
    private String generalConversationUuid;
    private Uri encryptionKeyUrl;   // Required to post update color activities but not actually needed

    private ItemCollection<Conversation> conversations;

    public static @ColorInt int getDefaultTeamColor(Context context) {
        if (context != null) {
            return ContextCompat.getColor(context, R.color.gray_dark_2);
        }

        return Color.DKGRAY;
    }

    public Team() {
        super();
        setObjectType(ObjectType.team);
    }

    public Team(String id) {
        this();
        setId(id);
    }

    public ItemCollection<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(ItemCollection<Conversation> conversations) {
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

    public void setEncryptionKeyUrl(Uri encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);
        encryptionKeyUrl = key.getKeyUrl();
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);
        if (!TextUtils.isEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }
    }
}
