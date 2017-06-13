package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.ui.conversation.ConversationResolver;


public class UpdateWhiteboardServiceTask extends AsyncTask<String, Integer, ConversationResolver> {

    private final WhiteboardService whiteboardService;
    private final Context context;
    private final Injector injector;

    @Nullable private WhiteboardService.WhiteboardServiceAvailableHandler handler;

    public UpdateWhiteboardServiceTask(WhiteboardService whiteboardService, @Nullable WhiteboardService.WhiteboardServiceAvailableHandler handler,
                                       Context context, Injector injector) {
        this.whiteboardService = whiteboardService;
        this.handler = handler;
        this.context = context;
        this.injector = injector;
    }

    @Override
    protected ConversationResolver doInBackground(String... params) {
        return ConversationContentProviderQueries.getConversationResolverById(context.getContentResolver(), params[0],
                                                                              injector);
    }

    @Override
    protected void onPostExecute(ConversationResolver resolver) {

        if (resolver == null)
            return;

        String aclUrl = resolver.getAclUrl();
        String kro = resolver.getKmsResourceObjectUrl();

        if (!TextUtils.isEmpty(aclUrl) && kro != null) {

            Uri aclUrlLink = Uri.parse(aclUrl);
            whiteboardService.setAclUrlLink(aclUrlLink);
            whiteboardService.setParentkmsResourceObjectUrl(Uri.parse(kro));

            if (handler != null) {
                handler.onWhiteboardServiceAvailable(resolver.getId(), resolver.getDefaultEncryptionKeyUrl(), aclUrlLink);
            }
        }
    }
}
