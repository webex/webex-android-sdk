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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import javax.inject.Inject;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.content.ContentUploadMonitor;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.*;
import com.cisco.spark.android.model.conversation.*;
import com.cisco.spark.android.model.crypto.scr.ContentReference;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.ContentDownloadMonitor;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.DatabaseProvider;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation.ContentItem;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation.ShareContentData;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.Operations;
import com.cisco.spark.android.util.*;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.message.*;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.space.Space;
import com.ciscowebex.androidsdk.utils.EmailAddress;
import com.ciscowebex.androidsdk.utils.Lists;
import com.ciscowebex.androidsdk.utils.http.ListBody;
import com.ciscowebex.androidsdk.utils.http.ObjectCallback;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk_commlib.SDKCommon;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Strings;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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

    @Inject
    ApiClientProvider _client;

    @Inject
    KeyManager keyManager;

    public MessageClientImpl(Context context, Authenticator authenticator, SDKCommon common) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(MessageService.class);
        _context = context;
        common.inject(this);
        //_bus.register(this);
        activityListener.register(activity -> {
            processorActivity(activity);
            return null;
        });
    }

    public void list(@NonNull String spaceId, @Nullable Before before, int max, @Nullable Mention[] mentions, @NonNull CompletionHandler<List<Message>> handler) {
        String id = WebexId.translate(spaceId);
        if (max == 0) {
            runOnUiThread(() -> handler.onComplete(ResultImpl.success(Collections.emptyList())), handler);
            return;
        }
        if (before == null) {
            list(id, null, mentions, max, new ArrayList<>(), handler);
        }
        else if (before instanceof Before.Date) {
            list(id, ((Before.Date) before).getDate(), mentions, max, new ArrayList<>(), handler);
        }
        else if (before instanceof Before.Message) {
            get(((Before.Message) before).getMessage(), false, result -> {
                Message message = result.getData();
                if (message == null) {
                    runOnUiThread(() -> handler.onComplete(ResultImpl.error(result.getError())), handler);
                }
                else {
                    list(id, message.getCreated(), mentions, max, new ArrayList<>(), handler);
                }
            });
        }
    }

    private void list(@NonNull String spaceId, @Nullable Date date, @Nullable Mention[] mentions, int max, @NonNull List<Activity> activities, @NonNull CompletionHandler<List<Message>> handler) {
        List<Activity> result = activities;

        Callback<ItemCollection<Activity>> callback = new Callback<ItemCollection<Activity>>() {

            @Override
            public void onResponse(Call<ItemCollection<Activity>> call, Response<ItemCollection<Activity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Activity activity : response.body().getItems()) {
                        if (activity.getVerb().equals(Verb.post) || activity.getVerb().equals(Verb.share)) {
                            result.add(activity);
                        }
                    }
                    if (result.size() >= max || response.body().size() < max) {
                        AsyncTask.execute(() -> {
                            CountDownLatch latch = new CountDownLatch(result.size());
                            List<Message> messages = new ArrayList<>(result.size());
                            for (Activity activity : result) {
                                decryptActivity(activity, new Action<Activity>() {
                                    @Override
                                    public void call(Activity activity) {
                                        Message message = createMessage(activity, false);
                                        if (message != null) {
                                            messages.add(message);
                                        }
                                        latch.countDown();
                                    }
                                });
                            }
                            try {
                                latch.await();
                            } catch (InterruptedException ignored) {
                            }
                            runOnUiThread(() -> handler.onComplete(ResultImpl.success(messages)), handler);
                        });
                    }
                    else {
                        Activity last = Lists.getLast(response.body().getItems());
                        list(spaceId, last == null ? null : last.getPublished(), mentions, max, result, handler);
                    }
                }
                else {
                    runOnUiThread(() -> handler.onComplete(ResultImpl.error(response)), handler);
                }
            }

            @Override
            public void onFailure(Call<ItemCollection<Activity>> call, Throwable t) {
                runOnUiThread(() -> handler.onComplete(ResultImpl.error(t)), handler);
            }
        };

        long time = (date == null ? new Date() : date).getTime();
        if (mentions != null && mentions.length > 0) {
            // TODO filter by conv Id
            // TODO just get method me for now
            _client.getConversationClient().getUserMentions(time, max).enqueue(callback);
        }
        else {
            _client.getConversationClient().getConversationActivitiesBefore(spaceId, time, max).enqueue(callback);
        }
    }

    @Override
    public void get(@NonNull String messageId, @NonNull CompletionHandler<Message> handler) {
        get(messageId, true, handler);
    }

    private void get(@NonNull String messageId, boolean decrypt, @NonNull CompletionHandler<Message> handler) {
        WebexId webexId = WebexId.from(messageId);
        _client.getConversationClient().getActivity("activities/" + webexId.getId()).enqueue(new Callback<Activity>() {
            @Override
            public void onResponse(Call<Activity> call, Response<Activity> response) {
                if (response.isSuccessful()) {
                    Activity activity = response.body();
                    if (decrypt) {
                        decryptActivity(activity, new Action<Activity>() {
                            @Override
                            public void call(Activity activity) {
                                runOnUiThread(() -> handler.onComplete(ResultImpl.success(createMessage(activity, false))), handler);
                            }
                        });
                    }
                    else {
                        handler.onComplete(ResultImpl.success(createMessage(activity, false)));
                    }
                }
                else {
                    handler.onComplete(ResultImpl.error(response));
                }
            }

            @Override
            public void onFailure(Call<Activity> call, Throwable t) {
                handler.onComplete(ResultImpl.error(t));
            }
        });
    }

    @Override
    public void postToPerson(@NonNull String personId, @Nullable String text, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(personId, createComment(text, null), files, handler);
    }

    @Override
    public void postToPerson(@NonNull EmailAddress personEmail, @Nullable String text, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(personEmail.toString(), createComment(text, null), files, handler);
    }

    @Override
    public void postToSpace(@NonNull String spaceId, @Nullable String text, @Nullable Mention[] mentions, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(spaceId, createComment(text, mentions), files, handler);
    }

    private void post(String personOrSpace, Comment comment, LocalFile[] localFiles, CompletionHandler<Message> handler) {
        Ln.d("Post to： " + personOrSpace);
        WebexId webexId = WebexId.from(personOrSpace);
        if (webexId == null) {
            if (EmailAddress.fromString(personOrSpace) == null) {
                doPost(personOrSpace, comment, localFiles, handler);
            }
            else {
                this.createSpaceWithPerson(personOrSpace, result -> {
                    if (result.getData() != null) {
                        doPost(result.getData(), comment, localFiles, handler);
                    }
                    else {
                        handler.onComplete(ResultImpl.error(result.getError()));
                    }
                });
            }
        }
        else if (webexId.is(WebexId.Type.ROOM_ID)) {
            doPost(webexId.getId(), comment, localFiles, handler);
        }
        else if (webexId.is(WebexId.Type.PEOPLE_ID)) {
            this.createSpaceWithPerson(webexId.getId(), result -> {
                if (result.getData() != null) {
                    doPost(result.getData(), comment, localFiles, handler);
                }
                else {
                    handler.onComplete(ResultImpl.error(result.getError()));
                }
            });
        }
        else {
            handler.onComplete(ResultImpl.error("Unknown target: " + personOrSpace));
        }
    }

    private void doPost(String conversationId, Comment comment, LocalFile[] localFiles, CompletionHandler<Message> handler) {
        if (TextUtils.isEmpty(conversationId)) {
            handler.onComplete(ResultImpl.error("Invalid person or id!"));
            return;
        }
        Action<ActivityOperation> callback = new Action<ActivityOperation>() {
            @Override
            public void call(ActivityOperation item) {
                Activity activity = item.getResult();
                if (activity == null) {
                    runOnUiThread(() -> handler.onComplete(ResultImpl.error(item.getErrorMessage())), handler);
                }
                else  {
                    decryptActivity(activity, new Action<Activity>() {
                        @Override
                        public void call(Activity activity) {
                            runOnUiThread(() -> handler.onComplete(ResultImpl.success(createMessage(activity, false))), handler);
                        }
                    });
                }
            }
        };
        if (localFiles != null && localFiles.length > 0) {
            ShareContentData shareContentData = new ShareContentData();
            for (LocalFile localFile : localFiles) {
                ContentItem item = new ContentItem(localFile.getFile(), FILE_PICKER.toString());
                File modleFile = createModleFile(localFile, conversationId, contentManager, db);
                if (modleFile != null) {
                    Operation uploadContent = operations.uploadContent(conversationId, modleFile);
                    item.setContentFile(modleFile);
                    item.setOperationId(uploadContent.getOperationId());
                    shareContentData.addContentItem(item);
                    executor.scheduleAtFixedRate(new CheckUploadProgressTask(localFile), 0, 1, TimeUnit.SECONDS);
                }
            }
            CallbackablePostContentActivityOperation postContent = new CallbackablePostContentActivityOperation(injector, conversationId,
                    shareContentData,
                    comment,
                    shareContentData.getContentFiles(),
                    shareContentData.getOperationIds(),
                    callback);
            operations.submit(postContent);
        } else {
            CallbackablePostCommentOperation postCommentOperation = new CallbackablePostCommentOperation(injector, conversationId, comment, callback);
            operations.submit(postCommentOperation);
        }
    }

    @Override
    public void delete(@NonNull String messageId, @NonNull CompletionHandler<Void> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> _service.delete(s, messageId), new ObjectCallback<>(handler));
    }

    @Override
    public void downloadFile(@NonNull RemoteFile file,
                             @Nullable java.io.File path,
                             @Nullable ProgressHandler progressHandler,
                             @NonNull CompletionHandler<Uri> handler) {
        download(((RemoteFileImpl) file).getFile(), file.getDisplayName(), false, path , progressHandler, handler);
    }

    @Override
    public void downloadThumbnail(@NonNull RemoteFile file,
                                  @Nullable java.io.File path,
                                  @Nullable ProgressHandler progressHandler,
                                  @NonNull CompletionHandler<Uri> handler) {
        RemoteFileImpl.ThumbnailImpl thumbnail = (RemoteFileImpl.ThumbnailImpl) file.getThumbnail();
        if (thumbnail == null || thumbnail.getImage() == null) {
            handler.onComplete(ResultImpl.error("No thumbnail for this remote file."));
            return;
        }
        download(thumbnail.getImage(), file.getDisplayName(), true, path, progressHandler, handler);
    }

    private void download(@NonNull ContentReference reference,
                          @Nullable String displayName,
                          boolean thnumnail,
                          @Nullable java.io.File path,
                          @Nullable ProgressHandler progressHandler,
                          @NonNull CompletionHandler<Uri> completionHandler) {
        Action<Long> callback = progressHandler == null ? null : new Action<Long>() {
            @Override
            public void call(Long item) {
                runOnUiThread(() -> progressHandler.onProgress(item), progressHandler);
            }
        };

        Action<ContentDataCacheRecord> action = new Action<ContentDataCacheRecord>() {
            @Override
            public void call(ContentDataCacheRecord item) {
                if (progressHandler != null) {
                    runOnUiThread(() -> progressHandler.onProgress(item.getDataSize()), progressHandler);
                }
                java.io.File target = path;
                if (target == null) {
                    target = new java.io.File(_context.getCacheDir(), "com.ciscowebex.sdk.downloads");
                    target.mkdirs();
                }
                String name = UUID.randomUUID().toString();
                if (displayName != null) {
                    name = name + "-" + displayName;
                }
                if (thnumnail) {
                    name = "thumb-" + name;
                }
                final java.io.File file = new java.io.File(target, name);
                try {
                    if (!file.createNewFile()) {
                        runOnUiThread(() -> completionHandler.onComplete(ResultImpl.error("failed to download File " + file.toString())), completionHandler);
                        return;
                    }
                    FileUtils.copyFile(item.getLocalUriAsFile(), file);
                    runOnUiThread(() -> completionHandler.onComplete(ResultImpl.success(Uri.fromFile(file))), completionHandler);
                }
                catch (Exception e) {
                    runOnUiThread(() -> completionHandler.onComplete(ResultImpl.error(e)), completionHandler);
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
        contentManager.getCacheRecord(ConversationContract.ContentDataCacheEntry.Cache.MEDIA, uri, reference.getSecureContentReference(), filename, action, new ContentDownloadMonitor(), callback);
    }

    @Override
    public void setMessageObserver(MessageObserver observer) {
        _observer = observer;
    }

    private void processorActivity(Activity activity) {
        if (_observer == null) {
            return;
        }
        MessageObserver.MessageEvent event;
        switch (activity.getVerb()) {
            case Verb.post:
            case Verb.share:
                event = new MessageObserver.MessageReceived(createMessage(activity, true));
                break;
            case Verb.delete:
                event = new MessageObserver.MessageDeleted(new WebexId(WebexId.Type.MESSAGE_ID, activity.getId()).toHydraId());
                break;
            default:
                Ln.e("unknown verb " + activity.getVerb());
                return;
        }
        runOnUiThread(() -> _observer.onEvent(event), _observer);
        // TODO Remove the deprecated event in next big release
        if (event instanceof MessageObserver.MessageReceived) {
            runOnUiThread(() -> _observer.onEvent(new MessageObserver.MessageArrived(((MessageObserver.MessageReceived) event).getMessage())), _observer);
        }
    }

    private void decryptActivity(Activity activity, Action<Activity> callback) {
        final Uri keyUrl = activity.getEncryptionKeyUrl();
        if (keyUrl == null) {
            callback.call(activity);
            return;
        }
        keyManager.getBoundKeySync(keyUrl).subscribe(keyObject -> {
            try {
                CryptoUtils.decryptActivity(keyObject, activity);
            } catch (Exception ignored) {
            }
            callback.call(activity);
        });
    }

    private void runOnUiThread(Runnable r, Object conditioner) {
        if (conditioner == null) return;
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
            }, file);
            if (progress >= 100) {
                t.cancel(false);
            }
        }
    }

    private void createSpaceWithPerson(String person, CompletionHandler<String> action) {
        EnumSet<NewConversationOperation.CreateFlags> createFlags = EnumSet.of(NewConversationOperation.CreateFlags.ONE_ON_ONE, NewConversationOperation.CreateFlags.PERSIST_WITHOUT_MESSAGES);
        operations.createConversationWithCallBack(Collections.singletonList(person), null, createFlags,
                new Action<NewConversationOperation>() {
                    @Override
                    public void call(NewConversationOperation item) {
                        Ln.d("createConversationWithCallBack: " + item.getConversationId());
                        action.onComplete(ResultImpl.success(item.getConversationId()));
                    }

                    @Override
                    public void onException(Exception e) {
                        super.onException(e);
                        action.onComplete(ResultImpl.error(e));
                    }
                });
    }

    private Message createMessage(Activity activity, boolean received) {
        return activity == null ? null : new MessageImpl(activity, _provider.getAuthenticatedUserOrNull(), received);
    }

    private static Comment createComment(@Nullable String text, @Nullable Mention[] mentions) {
        Comment comment = new Comment(text);
        comment.setContent(text);
        if (mentions != null && mentions.length > 0) {
            ItemCollection<Person> mentionedPersons = new ItemCollection<>();
            ItemCollection<GroupMention> mentionAll = new ItemCollection<>();
            for (Mention mention : mentions) {
                if (mention instanceof Mention.Person) {
                    WebexId personId = WebexId.from(((Mention.Person) mention).getPersonId());
                    if (personId != null) {
                        Person person = new Person(personId.getId());
                        mentionedPersons.addItem(person);
                    }
                } else if (mention instanceof Mention.All) {
                    mentionAll.addItem(new GroupMention(GroupMention.GroupType.ALL));
                }
            }
            if (mentionAll.size() > 0) {
                comment.setGroupMentions(mentionAll);
            }
            else if (mentionedPersons.size() > 0) {
                comment.setMentions(mentionedPersons);
            }
        }
        return comment;
    }

    // TODO use info in local file
    private static File createModleFile(LocalFile localFile, String conversationId, ContentManager contentManager, DatabaseProvider db) {
        try {
            Uri contentUri = Uri.fromFile(localFile.getFile());
            contentManager.addUploadedContent(new java.io.File(new URI(contentUri.toString())), contentUri, ConversationContract.ContentDataCacheEntry.Cache.MEDIA);

            File modelFile = new File();
            modelFile.setUri(contentUri);
            modelFile.setMimeType(MimeUtils.getMimeType(contentUri.toString()));
            modelFile.setDisplayName(contentUri.getLastPathSegment());
            if (localFile.getThumbnail() != null) {
                java.io.File thumbFile = localFile.getThumbnail().getFile();
                if (thumbFile.exists() && thumbFile.isFile()) {
                    Image newThumb = new Image(Uri.fromFile(thumbFile), localFile.getThumbnail().getWidth(), localFile.getThumbnail().getHeight(), true);
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

    @Override
    @Deprecated
    public void list(@NonNull String spaceId, @Nullable String before, @Nullable String beforeMessage, @Nullable String mentionedPeople, int max, @NonNull CompletionHandler<List<Message>> handler) {
        List<Mention> mentions = null;
        if (mentionedPeople != null) {
            List<String> peoples = Strings.split(mentionedPeople, ",", false);
            if (peoples != null && peoples.size() != 0) {
                mentions = new ArrayList<>(peoples.size());
                for (String people : peoples) {
                    mentions.add(new Mention.Person(people));
                }
            }
        }
        Before b = null;
        if (before != null) {
            try {
                Date date = DateUtils.buildIso8601Format().parse(before);
                if (date != null) {
                    b = new Before.Date(date);
                }
            } catch (Exception ignored) {

            }
        }
        else  if (beforeMessage != null) {
            b = new Before.Message(beforeMessage);
        }
        list(spaceId, b, max, mentions == null ? null : mentions.toArray(new Mention[mentions.size()]), handler);
    }

    @Override
    @Deprecated
    public void post(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, @Nullable String text, @Nullable String markdown, @Nullable String[] files, @NonNull CompletionHandler<Message> handler) {
        String idOrEmail = (spaceId == null ? (personId == null ? personEmail : personId) : spaceId);
        List<LocalFile> localFiles = null;
        if (files != null && files.length > 0) {
            localFiles = new ArrayList<>(files.length);
            for (String file : files){
                java.io.File f = new java.io.File(file);
                try {
                    localFiles.add(new LocalFile(f));
                }
                catch (Exception e) {
                    Ln.e(e);
                }
            }
        }
        post(idOrEmail, text, null, localFiles == null ? null : localFiles.toArray(new LocalFile[0]), handler);
    }

    @Override
    @Deprecated
    public void post(@NonNull String idOrEmail, @Nullable String text, @Nullable Mention[] mentions, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        WebexId webexId = WebexId.from(idOrEmail);
        if (webexId == null) {
            EmailAddress email = EmailAddress.fromString(idOrEmail);
            if (email == null) {
                postToPerson(idOrEmail, text, files, handler);
            }
            else {
                postToPerson(email, text, files, handler);
            }
        }
        else if (webexId.is(WebexId.Type.ROOM_ID)) {
            postToSpace(idOrEmail, text, mentions, files, handler);
        }
        else if (webexId.is(WebexId.Type.PEOPLE_ID)) {
            postToPerson(idOrEmail, text, files, handler);
        }
    }
}
