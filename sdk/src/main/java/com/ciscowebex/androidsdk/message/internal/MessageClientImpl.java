/*
 * Copyright 2016-2017 Cisco Systems Inc
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

import java.util.List;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.message.MessageClient;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;

import me.helloworld.utils.collection.Maps;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created with IntelliJ IDEA.
 * User: zhiyuliu
 * Date: 28/09/2017
 * Time: 5:23 PM
 */

public class MessageClientImpl implements MessageClient {

    private Authenticator _authenticator;

    private MessageService _service;

    public MessageClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(MessageService.class);
    }

    public void list(@NonNull String roomId, @Nullable String before, @Nullable String beforeMessage, @Nullable String mentionedPeople, int max, @NonNull CompletionHandler<List<Message>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, roomId, before, beforeMessage, mentionedPeople, max <= 0 ? null : max).enqueue(new ListCallback<>(handler));
        });
    }

    public void post(@Nullable String roomId, @Nullable String personId, @Nullable String personEmail, @Nullable String text, @Nullable String markdown, @Nullable String[] files, @NonNull CompletionHandler<Message> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.post(s, Maps.makeMap("roomId", roomId, "toPersonId", personId, "toPersonEmail", personEmail, "text", text, "markdown", markdown, "files", files)).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void get(@NonNull String messageId, @NonNull CompletionHandler<Message> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, messageId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void delete(@NonNull String messageId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.delete(s, messageId).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface MessageService {
        @GET("messages")
        Call<ListBody<Message>> list(@Header("Authorization") String authorization,
                                     @Query("roomId") String roomId,
                                     @Query("before") String before,
                                     @Query("beforeMessage") String beforeMessage,
                                     @Query("mentionedPeople") String mentionedPeople,
                                     @Query("max") Integer max);

        @POST("messages")
        Call<Message> post(@Header("Authorization") String authorization, @Body Map parameters);

        @GET("messages/{messageId}")
        Call<Message> get(@Header("Authorization") String authorization, @Path("messageId") String messageId);

        @DELETE("messages/{messageId}")
        Call<Void> delete(@Header("Authorization") String authorization, @Path("messageId") String messageId);
    }
}
