package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.util.Action;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

public class ConversationStreamObservableMapper implements Func1<ResponseBody, Observable<Conversation>> {

    private final Gson gson;
    private final Action<Conversation> transform;

    public ConversationStreamObservableMapper(Gson gson) {
        this(gson, null);
    }

    public ConversationStreamObservableMapper(Gson gson, Action<Conversation> transform) {
        this.gson = gson;
        this.transform = transform;
    }

    @Override
    public Observable<Conversation> call(final ResponseBody responseBody) {
        return Observable.create(new Observable.OnSubscribe<Conversation>() {
            @Override
            public void call(Subscriber<? super Conversation> subscriber) {
                JsonReader jsonReader = null;
                try {
                    jsonReader = new JsonReader(new InputStreamReader(responseBody.source().inputStream()));

                    jsonReader.beginObject();
                    jsonReader.nextName();
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        Conversation conv = gson.fromJson(jsonReader, Conversation.class);
                        if (transform != null)
                            transform.call(conv);
                        subscriber.onNext(conv);
                    }
                    jsonReader.close();
                } catch (Throwable e) {
                    Ln.w(e);
                    subscriber.onError(e);
                } finally {
                    try {
                        if (jsonReader != null)
                            jsonReader.close();
                    } catch (IOException e) {
                        Ln.w(e);
                    }
                }
                subscriber.onCompleted();
            }
        });
    }
}
