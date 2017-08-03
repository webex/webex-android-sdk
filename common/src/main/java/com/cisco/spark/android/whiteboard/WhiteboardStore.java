package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.google.gson.JsonObject;

import java.util.List;

import rx.Observable;

public interface WhiteboardStore extends Component {

    // Should be renamed. This is not a proper getter method as it sets the current Channel in the service
    Observable<Channel> getChannel(String channelId);
    void updateChannel(Channel updatedChannel);
    void patchChannel(Channel updatedChannel, AbsRemoteWhiteboardStore.ClientCallback clientCallback);
    //Should probably be called getChannel, while getChannel should be called e.g. loadChannel
    void getChannelInfo(String channelId, @Nullable AbsRemoteWhiteboardStore.ChannelInfoCallback clientCallback);
    @Deprecated // Use method with callback param
    void getChannelInfo(String channelId);

    void loadContent(String channelId);
    void saveContent(Channel channel, List<Content> content, JsonObject originalMessage);

    void clearPartialContents(String channelId, List<Content> content, JsonObject originalMessage);
    void clear(String channelId, JsonObject originalMessage);

    void loadWhiteboardList(String conversationId, int channelsLimit, WhiteboardListService.WhiteboardListCallback callback);
    void loadNextPageWhiteboards(String conversationId, String url, WhiteboardListService.WhiteboardListCallback callback);

    void fetchHiddenSpaceUrl(Channel channel, OnHiddenSpaceUrlFetched listener);

    interface OnHiddenSpaceUrlFetched {
        void onSuccess(@NonNull Uri hiddenSpaceUrl);
        void onFailure(String errorMessage);
    }

    Channel getBoardById(String boardId);

    void clearCache();
    boolean removeFromCache(String channelId);
}
