package com.cisco.spark.android.whiteboard.persistence;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelItems;

import retrofit2.Call;

public class PrivateRemoteWhiteboardStore extends AbsRemoteWhiteboardStore {

    public PrivateRemoteWhiteboardStore(WhiteboardService whiteboardService, ApiClientProvider apiClientProvider,
                                        Injector injector, SchedulerProvider schedulerProvider) {
        super(whiteboardService, apiClientProvider, injector, schedulerProvider);
    }

    @Override
    protected Call<ChannelItems<Channel>> createGetChannelsCall(String conversationId, int channelLimit) {
        return getWhiteboardPersistenceClient().getPrivateChannels(channelLimit);
    }

    @Override
    protected boolean isPrivateStore() {
        return true;
    }

    @Override
    public Channel getBoardById(String boardId) {
        return null;
    }
}
