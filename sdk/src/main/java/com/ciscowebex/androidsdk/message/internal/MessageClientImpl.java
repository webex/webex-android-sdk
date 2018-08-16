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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.content.ContentUploadMonitor;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.Mentionable;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.model.conversation.Comment;
import com.cisco.spark.android.model.conversation.Content;
import com.cisco.spark.android.model.conversation.File;
import com.cisco.spark.android.model.conversation.GroupMention;
import com.cisco.spark.android.model.conversation.Image;
import com.cisco.spark.android.model.crypto.scr.ContentReference;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.ContentDownloadMonitor;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.DatabaseProvider;
import com.cisco.spark.android.sync.operationqueue.NewConversationOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation.ContentItem;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation.ShareContentData;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.Operations;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.MimeUtils;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.message.LocalFile;
import com.ciscowebex.androidsdk.message.Mention;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.message.MessageClient;
import com.ciscowebex.androidsdk.message.MessageObserver;
import com.ciscowebex.androidsdk.message.RemoteFile;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ListCallback;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk_commlib.SDKCommon;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.collection.Maps;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static com.cisco.spark.android.content.ContentShareSource.FILE_PICKER;

/**
 * Created with IntelliJ IDEA.
 * User: zhiyuliu
 * Date: 28/09/2017
 * Time: 5:23 PM
 */

public class MessageClientImpl implements MessageClient {

    private Authenticator _authenticator;

    private MessageService _service;

    private MessageObserver _observer;

    private Context _context;

    @Inject
    Operations operations;

    @Inject
    Injector injector;

    @Inject
    EventBus _bus;

    @Inject
    ContentManager contentManager;

    @Inject
    ContentUploadMonitor uploadMonitor;

    @Inject
    DatabaseProvider db;

    @Inject
    ActivityListener activityListener;

    @Inject
    ApiTokenProvider _provider;

    public MessageClientImpl(Context context, Authenticator authenticator, SDKCommon common) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(MessageService.class);
        _context = context;
        common.inject(this);
        activityListener.register(activity -> {
            processorActivity(activity);
            return null;
        });
    }

    @Override
    public void list(@NonNull String spaceId, @Nullable String before, @Nullable String beforeMessage, @Nullable String mentionedPeople, int max, @NonNull CompletionHandler<List<Message>> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.list(s, spaceId, spaceId, before, beforeMessage, mentionedPeople, max <= 0 ? null : max), new ListCallback<>(handler));
    }

    @Override
    public void post(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, @Nullable String text, @Nullable String markdown, @Nullable String[] files, @NonNull CompletionHandler<Message> handler) {
        ServiceBuilder.async(_authenticator, handler, s ->
            _service.post(s, Maps.makeMap("roomId", spaceId, "spaceId", spaceId, "toPersonId", personId, "toPersonEmail", personEmail, "text", text, "markdown", markdown, "files", files)), new ObjectCallback<>(handler));
    }

    @Override
    public void post(@NonNull String idOrEmail, @Nullable String text, @Nullable Mention[] mentions, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        HydraId object = HydraId.decode(idOrEmail);
        if (object.type == null) {
            postToPerson(idOrEmail, text, files, handler);
        } else if (object.type == HydraId.HydraIdType.ROOM_ID) {
            postToSpace(object.id, text, mentions, files, handler);
        } else if (object.type == HydraId.HydraIdType.PEOPLE_ID) {
            postToPerson(object.id, text, files, handler);
        }
    }

    @Override
    public void get(@NonNull String messageId, @NonNull CompletionHandler<Message> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> _service.get(s, messageId), new ObjectCallback<>(handler));
    }

    @Override
    public void delete(@NonNull String messageId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> _service.delete(s, messageId), new ObjectCallback<>(handler));
    }

    @Override
    public void downloadFile(RemoteFile file, String to, ProgressHandler progressHandler, CompletionHandler<Uri> handler) {
        download(((RemoteFileImpl) file).getFile(), to, progressHandler, handler);
    }

    @Override
    public void downloadThumbnail(RemoteFile file, String to, ProgressHandler progressHandler, CompletionHandler<Uri> handler) {
        download(((RemoteFileImpl) file).getFile().getImage(), to, progressHandler, handler);
    }

    private void postComment(String conversationId, Comment comment) {
        PostCommentOperation postCommentOperation = new PostCommentOperation(injector, conversationId, comment);
        operations.submit(postCommentOperation);
    }

    private File createContentFile(LocalFile contentFile, String conversationId, ContentManager contentManager, DatabaseProvider db) {
        try {
            Uri contentUri = Uri.fromFile(contentFile.getFile());
            contentManager.addUploadedContent(new java.io.File(new URI(contentUri.toString())), contentUri, ConversationContract.ContentDataCacheEntry.Cache.MEDIA);

            File modelFile = new File();
            modelFile.setUri(contentUri);
            modelFile.setMimeType(MimeUtils.getMimeType(contentUri.toString()));
            modelFile.setDisplayName(contentUri.getLastPathSegment());
            if (contentFile.getThumbnail() != null) {
                java.io.File thumbFile = new java.io.File(contentFile.getThumbnail().getPath());
                if (thumbFile.exists() && thumbFile.isFile()) {
                    Image newThumb = new Image(Uri.fromFile(thumbFile), contentFile.getThumbnail().getWidth(), contentFile.getThumbnail().getHeight(), true);
                    modelFile.setImage(newThumb);
                }
            }
            return modelFile;
        } catch (URISyntaxException e) {
            Ln.e(e, "Failed parsing content URI.");
            return null;
        } finally {
            if (db != null && !TextUtils.isEmpty(conversationId)) {
                db.notifyChange(ConversationContract.ConversationEntry.getConversationActivitiesUri(conversationId));
            }
        }
    }

    private boolean postContent(String conversationId, Comment comment, LocalFile[] localFiles) {
        ShareContentData shareContentData = new ShareContentData();
        for (LocalFile file : localFiles) {
            ContentItem item = new ContentItem(file.getFile(), FILE_PICKER.toString());
            File contentFile = createContentFile(file, conversationId, contentManager, db);
            if (contentFile == null) {
                return false;
            }
            item.setContentFile(contentFile);
            Operation uploadContent = operations.uploadContent(conversationId, item.getContentFile());
            item.setOperationId(uploadContent.getOperationId());
            shareContentData.addContentItem(item);
            // Check upload progress
            executor.scheduleAtFixedRate(new CheckUploadProgressTask(file), 0, 1, TimeUnit.SECONDS);
        }
        PostContentActivityOperation postContent = new PostContentActivityOperation(
                injector,
                conversationId,
                shareContentData,
                comment,
                shareContentData.getContentFiles(),
                shareContentData.getOperationIds()
        );
        operations.submit(postContent);
        return true;
    }

    private void postContentOrComment(String conversationId, LocalFile[] files, Comment comment, CompletionHandler<Message> handler) {
        if (TextUtils.isEmpty(conversationId)) {
            handler.onComplete(ResultImpl.error("Invalid person or id!"));
            return;
        }
        if (files != null && files.length > 0) {
            boolean rst = postContent(conversationId, comment, files);
            runOnUiThread(() -> handler.onComplete(
                    rst ? ResultImpl.success(null) : ResultImpl.error("post content failed!")));
        } else {
            postComment(conversationId, comment);
            runOnUiThread(() -> handler.onComplete(ResultImpl.success(null)));
        }
    }

    private void createMention(Comment comment, Mention[] mentions) {
        if (mentions == null) return;
        ItemCollection<Person> mentionedPersons = new ItemCollection<>();
        for (Mention m : mentions) {
            if (m instanceof Mention.MentionPerson) {
                HydraId personId = HydraId.decode(((Mention.MentionPerson) m).getPersonId());
                if (personId.type != null && personId.type.equals(HydraId.HydraIdType.PEOPLE_ID)) {
                    Person person = new Person(personId.id);
                    mentionedPersons.addItem(person);
                }
            } else {
                ItemCollection<GroupMention> mentionAll = new ItemCollection<>();
                mentionAll.addItem(new GroupMention(GroupMention.GroupType.ALL));
                comment.setGroupMentions(mentionAll);
            }
        }
        comment.setMentions(mentionedPersons);
    }

    public void postToPerson(@NonNull String personIdOrEmail,
                             @Nullable String text,
                             @Nullable LocalFile[] files,
                             @NonNull CompletionHandler<Message> handler) {
        Comment comment = new Comment(text);
        comment.setContent(text);

        EnumSet<NewConversationOperation.CreateFlags> createFlags = EnumSet.noneOf(NewConversationOperation.CreateFlags.class);
        createFlags.addAll(Collections.singletonList(NewConversationOperation.CreateFlags.ONE_ON_ONE));

        operations.createConversationWithCallBack(
                Collections.singletonList(personIdOrEmail), null, createFlags,
                new Action<NewConversationOperation>() {
                    @Override
                    public void call(NewConversationOperation item) {
                        String conversationId = item.getConversationId();
                        postContentOrComment(conversationId, files, comment, handler);
                    }
                });
    }

    public void postToSpace(@NonNull String spaceId,
                           @Nullable String text,
                           @Nullable Mention[] mentions,
                           @Nullable LocalFile[] files,
                           @NonNull CompletionHandler<Message> handler) {
        Comment comment = new Comment(text);
        comment.setContent(text);
        createMention(comment, mentions);
        postContentOrComment(spaceId, files, comment, handler);
    }


    private class RemoteFileImpl extends RemoteFile {
        private File file;

        private RemoteFileImpl(File file) {
            this.file = file;
            this.size = file.getFileSize();
            this.displayName = file.getDisplayName();
            this.mimeType = file.getMimeType();
            this.url = file.getUrl().toString();
            if (file.getImage() != null) {
                this.thumbnail = new Thumbnail();
                this.thumbnail.setUrl(file.getImage().getUrl().toString());
                this.thumbnail.setWidth(file.getImage().getWidth());
                this.thumbnail.setHeight(file.getImage().getHeight());
            }
        }

        public File getFile() {
            return file;
        }

        public String toString() {
            return file.toString();
        }
    }

    private static class HydraId {
        enum HydraIdType {
            MESSAGE_ID,
            PEOPLE_ID,
            ROOM_ID,
        }

        private String id;
        private HydraIdType type;
        private static final List typeString = Arrays.asList("MESSAGE", "PEOPLE", "ROOM");

        // decode hydra id
        @NonNull
        private static HydraId decode(String hydraId) {
            HydraId object = new HydraId();
            try {
                String decodeStr = new String(Base64.decode(hydraId, Base64.URL_SAFE), "UTF-8");
                if (TextUtils.isEmpty(decodeStr)) {
                    return object;
                }
                String[] subs = decodeStr.split("/");
                object.id = subs[subs.length - 1];
                object.type = HydraIdType.values()[typeString.indexOf(subs[subs.length - 2])];
            } catch (Exception e) {
                Ln.d(e, "can't decode hydra id : " + hydraId);
            }
            return object;
        }

        // encode space_id, people_id, message_id to hydra_id
        private static String encode(String id, HydraIdType type) {
            String encodeString = "ciscospark://us/" + typeString.get(type.ordinal()) + "/" + id;
            return new String(Base64.encode(encodeString.getBytes(),
                    Base64.NO_PADDING | Base64.URL_SAFE | Base64.NO_WRAP));
        }
    }

    @Override
    public void setMessageObserver(MessageObserver observer) {
        _observer = observer;
    }

    private Message formMessage(Activity activity) {
        if (activity == null) {
            return null;
        }
        Message message = new Message();
        message.setId(HydraId.encode(activity.getId(), HydraId.HydraIdType.MESSAGE_ID));
        String spaceId = activity.getTarget().getId();
        message.setSpaceId(HydraId.encode(spaceId, HydraId.HydraIdType.ROOM_ID));
        message.setText(activity.getObject().getDisplayName());
        message.setCreated(activity.getPublished());
        message.setPersonId(HydraId.encode(activity.getActor().getUuid(), HydraId.HydraIdType.PEOPLE_ID));
        message.setPersonEmail(activity.getActor().getEmail());

        if (activity.isSelfMention(_provider.getAuthenticatedUser(), 0)) {
            message.setSelfMentioned(true);
        }

        formMessageFiles(message, activity);
        formMessageMentions(message, activity);

        return message;
    }

    private void formMessageMentions(Message message, Activity activity) {
        if (activity.getObject() instanceof Mentionable) {
            Mentionable mentionable = (Mentionable) activity.getObject();
            if (mentionable.getMentions() == null) {
                return;
            }
            ArrayList<String> arrayList = new ArrayList<>();
            for (Person p : mentionable.getMentions().getItems()) {
                arrayList.add(HydraId.encode(p.getId(), HydraId.HydraIdType.PEOPLE_ID));
            }
            String[] mentionPeoples = new String[arrayList.size()];
            message.setMentionedPeople(mentionPeoples);
        }
    }

    private void formMessageFiles(Message message, Activity activity) {
        if (activity.getObject().isContent()) {
            Content content = (Content) activity.getObject();
            ItemCollection<File> it = content.getContentFiles();
            ArrayList<RemoteFile> remoteFiles = new ArrayList<>();
            ArrayList<String> files = new ArrayList<>();
            for (File f : it.getItems()) {
                RemoteFile remoteFile = new RemoteFileImpl(f);
                remoteFiles.add(remoteFile);
                files.add(remoteFile.toString());
            }
            message.setRemoteFiles(remoteFiles);
            message.setFiles(files.toArray(new String[files.size()]));
        }
    }

    private void processorActivity(Activity activity) {
        Ln.v("activity processed: " + activity.toString());
        MessageObserver.MessageEvent event;
        switch (activity.getVerb()) {
            case "post":
            case "share":
                Message message = formMessage(activity);
                event = new MessageObserver.MessageArrived(message);
                break;
            case "delete":
                String messageId = HydraId.encode(activity.getId(), HydraId.HydraIdType.MESSAGE_ID);
                event = new MessageObserver.MessageDeleted(messageId);
                break;
            default:
                Ln.e("unknown verb " + activity.getVerb());
                return;
        }
        if (_observer != null) {
            runOnUiThread(() -> _observer.onEvent(event));
        }
    }

    private void runOnUiThread(Runnable r) {
        Handler handler = new Handler(_context.getMainLooper());
        handler.post(r);
    }

    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    private static ScheduledFuture<?> t;

    class CheckUploadProgressTask implements Runnable {
        private Uri contentUri;
        private LocalFile file;

        CheckUploadProgressTask(LocalFile file) {
            super();
            this.file = file;
            contentUri = Uri.fromFile(file.getFile());
        }

        public void run() {
            if (contentUri == null) {
                return;
            }
            int progress;
            progress = uploadMonitor.getProgressForKey(contentUri.toString());
            runOnUiThread(() -> {
                if (file.getProgressHandler() != null) {
                    file.getProgressHandler().onProgress(progress >= 0 ? progress : 0);
                }
            });
            if (progress >= 100) {
                t.cancel(false);
            }
        }
    }

    private void download(ContentReference reference, String to, ProgressHandler progressHandler, CompletionHandler<Uri> completionHandler) {
        Action<Long> callback = new Action<Long>() {
            @Override
            public void call(Long item) {
                if (progressHandler != null) {
                    runOnUiThread(() -> progressHandler.onProgress(item));
                }
            }
        };

        Action<ContentDataCacheRecord> action = new Action<ContentDataCacheRecord>() {
            @Override
            public void call(ContentDataCacheRecord item) {
                if (progressHandler != null) {
                    runOnUiThread(() -> progressHandler.onProgress(item.getDataSize()));
                }
                if (!TextUtils.isEmpty(to)) {
                    try {
                        java.io.File destFile = new java.io.File(to);
                        if (!destFile.createNewFile()) {
                            if (completionHandler != null) {
                                runOnUiThread(() -> completionHandler.onComplete(ResultImpl.error("failed to create File " + destFile.toString())));
                            }
                            return;
                        }
                        FileUtils.copyFile(item.getLocalUriAsFile(), destFile);
                        if (completionHandler != null) {
                            runOnUiThread(() -> completionHandler.onComplete(ResultImpl.success(Uri.fromFile(destFile))));
                        }
                    } catch (IOException e) {
                        Ln.e(e, "copy file exception");
                        if (completionHandler != null) {
                            runOnUiThread(() -> completionHandler.onComplete(ResultImpl.error("failed to copy File ")));
                        }
                    }
                } else {
                    if (completionHandler != null) {
                        runOnUiThread(() -> completionHandler.onComplete(ResultImpl.success(item.getLocalUri())));
                    }
                }
            }
        };

        Uri uri = null;
        String filename = null;
        if (reference instanceof File) {
            uri = ((File) reference).getUrl();
            filename = ((File) reference).getDisplayName();
        }

        if (reference instanceof Image) {
            uri = ((Image) reference).getUrl();
            filename = "thumbnail.png";
        }

        contentManager.getCacheRecord(ConversationContract.ContentDataCacheEntry.Cache.MEDIA,
                uri, reference.getSecureContentReference(), filename,
                action, new ContentDownloadMonitor(), callback);
    }


    private interface MessageService {
        @GET("messages")
        Call<ListBody<Message>> list(@Header("Authorization") String authorization,
                                     @Query("roomId") String roomId,
                                     @Query("spaceId") String spaceId,
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
