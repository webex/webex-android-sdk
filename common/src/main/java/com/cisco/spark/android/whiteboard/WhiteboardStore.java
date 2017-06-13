package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.google.gson.JsonObject;

import java.util.List;

public interface WhiteboardStore extends Component {

    boolean createChannel(Channel channel);
    boolean createChannel(Channel channelRequest, List<Content> content);

    // Should be renamed. This is not a proper getter method as it sets the current Channel in the service
    void getChannel(String channelId);
    void updateChannel(Channel updatedChannel);
    void patchChannel(Channel updatedChannel, AbsRemoteWhiteboardStore.ClientCallback clientCallback);
    //Should probably be called getChannel, while getChannel should be called e.g. loadChannel
    void getChannelInfo(String channelId);

    void loadContent(String channelId);
    void saveContent(String channelId, List<Content> content, JsonObject originalMessage);

    void clear(String channelId, JsonObject originalMessage);

    void loadWhiteboardList(String conversationId, int channelsLimit);
    void loadNextPageWhiteboards(String conversationId, String url);

    void fetchHiddenSpaceUrl(Channel channel, OnHiddenSpaceUrlFetched listener);

    interface OnHiddenSpaceUrlFetched {
        void onSuccess(@NonNull Uri hiddenSpaceUrl);
        void onFailure(String errorMessage);
    }

    Channel getBoardById(String boardId);

    void clearCache();
    boolean removeFromCache(String channelId);
}
