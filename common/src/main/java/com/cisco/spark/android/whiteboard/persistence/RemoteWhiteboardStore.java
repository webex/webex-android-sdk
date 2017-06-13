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
import com.github.benoitdion.ln.Ln;

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
    protected Call<Channel> createCreateChannelCall(Channel channel) {
        return getWhiteboardPersistenceClient().createChannel(channel);
    }

    @Override
    protected Call<ChannelItems<Channel>> createGetChannelsCall(String aclLink, int channelsLimit) {
        Call<ChannelItems<Channel>> call;
        if (deviceRegistration.getFeatures().isWhiteboardWithAclEnabled()) {
            call = getWhiteboardPersistenceClient().getChannelsWithAcl(aclLink, channelsLimit);
        } else {
            call = getWhiteboardPersistenceClient().getChannels(aclLink, channelsLimit);
        }
        return call;
    }

    @Override
    protected boolean isPrivateStore() {
        return false;
    }

    @Override
    protected void processCreateChannelFailure() {
        Ln.e("Failed to create board");
        whiteboardService.boardReady();
        sendBoardError(WhiteboardError.ErrorData.NETWORK_ERROR);
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
