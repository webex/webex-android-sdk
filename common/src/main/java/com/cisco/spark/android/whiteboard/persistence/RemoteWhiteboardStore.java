package com.cisco.spark.android.whiteboard.persistence;

import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.WhiteboardError;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelItems;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Response;

public class RemoteWhiteboardStore extends AbsRemoteWhiteboardStore {

    @Inject DeviceRegistration deviceRegistration;

    public RemoteWhiteboardStore(WhiteboardService whiteboardService, ApiClientProvider apiClientProvider,
                                 Injector injector, SchedulerProvider schedulerProvider) {
        super(whiteboardService, apiClientProvider, injector, schedulerProvider);
    }

    @Override
    protected Call<ChannelItems<Channel>> createGetChannelsCall(String aclLink, int channelsLimit) {
        return getWhiteboardPersistenceClient().getChannelsWithAcl(aclLink, channelsLimit);
    }

    @Override
    protected boolean isPrivateStore() {
        return false;
    }

    @Override
    public Channel getBoardById(String boardId) {
        if (TextUtils.isEmpty(boardId)) {
           return null;
        }
        final Channel[] result = new Channel[1];

        Call<Channel> call = getWhiteboardPersistenceClient().getChannel(boardId);
            call.enqueue(new retrofit2.Callback<Channel>() {

                @Override
                public void onResponse(Call<Channel> call, Response<Channel> response) {
                    if (response.isSuccessful()) {
                        Channel channel = response.body();
                        if (channel.getAclUrlLink() != null) {
                            whiteboardService.setAclUrlLink(channel.getAclUrlLink());
                        }

                        result[0] = channel;
                    } else {
                        result[0] = null;
                        sendBoardError(WhiteboardError.ErrorData.GET_BOARD_ERROR);
                    }
                }

                @Override
                public void onFailure(Call<Channel> call, Throwable t) {
                    sendBoardError(WhiteboardError.ErrorData.GET_BOARD_ERROR);
                    result[0] = null;
                }
            });
        return result[0];
    }
}
