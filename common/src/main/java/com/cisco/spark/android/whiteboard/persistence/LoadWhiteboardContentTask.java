package com.cisco.spark.android.whiteboard.persistence;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.whiteboard.WhiteboardError;
import com.cisco.spark.android.whiteboard.persistence.model.ContentItems;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Response;

public class LoadWhiteboardContentTask extends AsyncTask<Void, Void, Void> {

    @Inject
    ApiClientProvider apiClientProvider;

    private static final int DEFAULT_LIMIT = 1000;

    private int limit;
    private String channelId;
    private Callback callback;

    private ContentItems contentItems;

    public LoadWhiteboardContentTask(String channelId, @NonNull Callback callback, Injector injector) {
        this(channelId, DEFAULT_LIMIT, callback, injector);
    }

    public LoadWhiteboardContentTask(String channelId, int limit, @NonNull Callback callback, Injector injector) {
        this.channelId = channelId;
        this.limit = limit;
        this.callback = callback;
        injector.inject(this);
    }

    @Override
    protected Void doInBackground(Void[] params) {

        contentItems = new ContentItems();
        Call<ContentItems> call = apiClientProvider.getWhiteboardPersistenceClient().getContents(channelId, limit);
        while (call != null) {
            Response<ContentItems> response;
            try {
                response =  call.execute();
            } catch (IOException e) {
                callback.onFailure(channelId, WhiteboardError.ErrorData.NETWORK_ERROR);
                return null;
            }

            if (!response.isSuccessful() && response.code() != HttpURLConnection.HTTP_NOT_FOUND) {
                callback.onFailure(channelId, WhiteboardError.ErrorData.LOAD_BOARD_LIST_ERROR);
                return null;
            } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                // There are not contents for this board, contentItems should be empty
            } else {
                contentItems.getItems().addAll(response.body().getItems());
            }

            if (hasMoreContents(response)) {
                String nextPageContentsLink = WhiteboardUtils.extractNextPageLinkFromResponse(response);
                call = apiClientProvider.getWhiteboardPersistenceClient().getContents(nextPageContentsLink);
            } else {
                call = null;
            }
        }

        callback.onSuccess(channelId, contentItems);
        return null;
    }

    private boolean hasMoreContents(Response response) {
        String nextPageContentsLink = WhiteboardUtils.extractNextPageLinkFromResponse(response);
        return !TextUtils.isEmpty(nextPageContentsLink);
    }

    public interface Callback {
        void onSuccess(String channelId, ContentItems contentItems);
        void onFailure(String channelId, WhiteboardError.ErrorData errorData);
    }
}
