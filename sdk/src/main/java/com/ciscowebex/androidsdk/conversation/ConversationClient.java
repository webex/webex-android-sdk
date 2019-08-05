package com.ciscowebex.androidsdk.conversation;

import android.support.annotation.NonNull;

import com.ciscowebex.androidsdk.CompletionHandler;

import java.io.UnsupportedEncodingException;

import okhttp3.ResponseBody;

public interface ConversationClient {


    /**
     * sendReadReceipt.
     *
     * @param personId    The identifier of the person who read the message.
     * @param spaceId     The identifier of the space where the person belongs.
     * @param messageId   The identifier of the message.
     * @param handler     A closure to be executed once the request has finished.
     * Adding by Orel
     */

    void sendReadReceipt(@NonNull String personId, @NonNull String messageId, @NonNull String spaceId, @NonNull CompletionHandler<ResponseBody> handler);

    /**
     * getSpaceInfo.
     *
     * @param spaceId    The identifier of the person who read the message
     * @param handler     A closure to be executed once the request has finished.
     * Adding by Orel
     */

    void getSpaceInfo(@NonNull String spaceId, @NonNull CompletionHandler<Activity> handler);


    }