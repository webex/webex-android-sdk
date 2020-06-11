/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.content.Context;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyManager;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.message.*;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.utils.*;
import com.github.benoitdion.ln.Ln;
import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.Checker;
import me.helloworld.utils.Strings;
import me.helloworld.utils.collection.Maps;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageClientImpl implements MessageClient, ActivityListener {

    private Context context;
    private PhoneImpl phone;
    private MessageObserver observer;
    private Map<String, Identifier> conversations = new ConcurrentHashMap<>();
    private String uuid = UUID.randomUUID().toString();

    public MessageClientImpl(Context context, PhoneImpl phone) {
        this.context = context;
        this.phone = phone;
    }

    public void setMessageObserver(MessageObserver observer) {
        this.observer = observer;
    }

    public void processActivity(ActivityModel activity) {
        if (observer == null) {
            return;
        }
        // TODO Get cluster id from activity url
        if (activity.getVerb() == ActivityModel.Verb.post || activity.getVerb() == ActivityModel.Verb.share) {
            String conversationId = activity.getConversationId();
            if (conversationId == null) {
                Ln.d("The activity without conversation");
                return;
            }
            String clientTempId = activity.getClientTempId();
            if (clientTempId != null && clientTempId.startsWith(uuid)) {
                Ln.d("The activity is sent by self");
                return;
            }
            Identifier identifier = new Identifier(new WebexId(WebexId.Type.ROOM, conversationId));
            KeyManager.shared.tryRefresh(identifier, activity.getEncryptionKeyUrl());
            KeyManager.shared.getKey(identifier, phone.getCredentials(), phone.getDevice(), keyResult -> {
                if (keyResult.getData() != null) {
                    activity.decrypt(keyResult.getData());
                }
                Message message = createMessage(activity, true);
                MessageObserver.MessageReceived event = new InternalMessage.InternalMessageReceived(message, activity);
                Queue.main.run(() -> observer.onEvent(event));
                // TODO Remove the deprecated event in next big release
                Queue.main.run(() -> observer.onEvent(new InternalMessage.InternalMessageArrived(message, activity)));
            });
        }
        else if (activity.getVerb() == ActivityModel.Verb.delete) {
            String id = new WebexId(WebexId.Type.MESSAGE, activity.getObject().getId()).getBase64Id();
            MessageObserver.MessageEvent event = new InternalMessage.InternalMessageDeleted(id, activity);
            Queue.main.run(() -> observer.onEvent(event));
        }
    }

    public void list(@NonNull String spaceId, @Nullable Before before, @IntRange(from = 0, to = Integer.MAX_VALUE) int max, @Nullable Mention[] mentions, @NonNull CompletionHandler<List<Message>> handler) {
        String id = WebexId.uuid(spaceId);
        if (max == 0) {
            ResultImpl.inMain(handler, Collections.emptyList());
            return;
        }
        if (before == null) {
            doList(id, null, mentions, max, new ArrayList<>(), handler);
        } else if (before instanceof Before.Date) {
            doList(id, ((Before.Date) before).getDate(), mentions, max, new ArrayList<>(), handler);
        } else if (before instanceof Before.Message) {
            get(((Before.Message) before).getMessage(), false, result -> {
                Message message = result.getData();
                if (message == null) {
                    ResultImpl.errorInMain(handler, result);
                } else {
                    doList(id, message.getCreated(), mentions, max, new ArrayList<>(), handler);
                }
            });
        }
    }

    private void doList(@NonNull String spaceId,
                      @Nullable Date date,
                      @Nullable Mention[] mentions,
                      @IntRange(from = 0, to = Integer.MAX_VALUE) int max,
                      @NonNull List<ActivityModel> activities,
                      @NonNull CompletionHandler<List<Message>> handler) {
        int queryMax = Math.max(max, max * 2);
        Identifier conversation = new Identifier(spaceId);
        // TODO Find the cluster for the identifier instead of use home cluster always.
        Service.Conv.homed(phone.getDevice())
                .get(Checker.isEmpty(mentions) ? "activities" : "mentions")
                .with("conversationId", conversation.uuid())
                .with("limit", String.valueOf(queryMax))
                .with(Checker.isEmpty(mentions) ? "maxDate" : "sinceDate", String.valueOf((date == null ? new Date() : date).getTime()))
                .auth(phone.getAuthenticator())
                .model(new TypeToken<ItemsModel<ActivityModel>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<ActivityModel>>) items -> {
                    for (ActivityModel model : items.getItems()) {
                        if (model.getVerb().equals(ActivityModel.Verb.post) || model.getVerb().equals(ActivityModel.Verb.share)) {
                            activities.add(model);
                            if (activities.size() >= max) {
                                break;
                            }
                        }
                    }
                    if (activities.size() >= max || items.size() < queryMax) {
                        KeyManager.shared.getKey(conversation, phone.getCredentials(), phone.getDevice(), keyResult -> {
                            List<Message> messages = new ArrayList<>(activities.size());
                            for (ActivityModel model : activities) {
                                if (keyResult.getData() != null) {
                                    model.decrypt(keyResult.getData());
                                }
                                messages.add(createMessage(model, false));
                            }
                            ResultImpl.inMain(handler, messages);
                        });
                    }
                    else {
                        ActivityModel last = Lists.getLast(items.getItems());
                        doList(spaceId, last == null ? null : last.getPublished(), mentions, max, activities, handler);
                    }
                });
    }

    public void get(@NonNull String messageId, @NonNull CompletionHandler<Message> handler) {
        get(messageId, true, handler);
    }

    private void get(@NonNull String messageId, boolean decrypt, @NonNull CompletionHandler<Message> handler) {
        // TODO Find the cluster for the identifier instead of use home cluster always.
        Identifier activity = new Identifier(messageId);
        Service.Conv.homed(phone.getDevice())
                .get("activities/" + activity.uuid())
                .auth(phone.getAuthenticator())
                .model(ActivityModel.class)
                .error(handler)
                .async((Closure<ActivityModel>) model -> {
                    if (!decrypt) {
                        ResultImpl.inMain(handler, createMessage(model, false));
                        return;
                    }
                    KeyManager.shared.getKey(new Identifier(new WebexId(WebexId.Type.ROOM, model.getConversationId())), phone.getCredentials(), phone.getDevice(), keyResult -> {
                        if (keyResult.getData() != null) {
                            model.decrypt(keyResult.getData());
                        }
                        ResultImpl.inMain(handler, createMessage(model, false));
                    });
                });
    }

    public void delete(@NonNull String messageId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.global().delete("messages/" + messageId)
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) data -> handler.onComplete(ResultImpl.success(null)));
    }

    @Override
    public void postToPerson(@NonNull String personId, @Nullable String text, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(personId, Message.draft(Message.Text.html(text, text)).addAttachments(files), handler);
    }

    @Override
    public void postToPerson(@NonNull String personId, @Nullable Message.Text text, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(personId, Message.draft(text).addAttachments(files), handler);
    }

    @Override
    public void postToPerson(@NonNull EmailAddress personEmail, @Nullable String text, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(personEmail.toString(), Message.draft(Message.Text.html(text, text)).addAttachments(files), handler);
    }

    @Override
    public void postToPerson(@NonNull EmailAddress personEmail, @Nullable Message.Text text, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(personEmail.toString(), Message.draft(text).addAttachments(files), handler);
    }

    @Override
    public void postToSpace(@NonNull String spaceId, @Nullable String text, @Nullable Mention[] mentions, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(spaceId, Message.draft(Message.Text.html(text, text)).addAttachments(files).addMentions(mentions), handler);
    }

    @Override
    public void postToSpace(@NonNull String spaceId, @Nullable Message.Text text, @Nullable Mention[] mentions, @Nullable LocalFile[] files, @NonNull CompletionHandler<Message> handler) {
        post(spaceId, Message.draft(text).addAttachments(files).addMentions(mentions), handler);
    }

    public void post(@NonNull String target, @NonNull Message.Draft draft, @NonNull CompletionHandler<Message> handler) {
        WebexId webexId = WebexId.from(target);
        if (webexId == null) {
            if (EmailAddress.fromString(target) == null) {
                doPost(new Identifier(new WebexId(WebexId.Type.ROOM, target)), (DraftImpl) draft, handler);
            } else {
                getOrCreateConversationWithPerson(target, result -> {
                    Identifier conversation = result.getData();
                    if (conversation == null) {
                        ResultImpl.errorInMain(handler, result);
                        return;
                    }
                    doPost(conversation, (DraftImpl) draft, handler);
                });
            }
        } else if (webexId.is(WebexId.Type.ROOM)) {
            doPost(new Identifier(webexId), (DraftImpl) draft, handler);
        } else if (webexId.is(WebexId.Type.PEOPLE)) {
            getOrCreateConversationWithPerson(webexId.getUUID(), result -> {
                Identifier conversation = result.getData();
                if (conversation == null) {
                    ResultImpl.errorInMain(handler, result);
                    return;
                }
                doPost(conversation, (DraftImpl) draft, handler);
            });
        }
        else {
            ResultImpl.errorInMain(handler, "Unknown target: " + target);
        }
    }

    public void downloadFile(@NonNull RemoteFile file,
                             @Nullable java.io.File path,
                             @Nullable ProgressHandler progressHandler,
                             @NonNull CompletionHandler<Uri> handler) {
        doDownload((RemoteFileImpl) file, path, false, progressHandler, handler);
    }

    public void downloadThumbnail(@NonNull RemoteFile file,
                                  @Nullable java.io.File path,
                                  @Nullable ProgressHandler progressHandler,
                                  @NonNull CompletionHandler<Uri> handler) {
        RemoteFileImpl.ThumbnailImpl thumbnail = (RemoteFileImpl.ThumbnailImpl) file.getThumbnail();
        if (thumbnail == null || thumbnail.getImage() == null) {
            ResultImpl.errorInMain(handler, "No thumbnail for this remote file.");
            return;
        }
        doDownload((RemoteFileImpl) file, path, true, progressHandler, handler);
    }

    public void markAsRead(@NonNull String spaceId) {
        this.list(spaceId, null, 1, null, messages -> {
            Message message = Lists.getFirst(messages.getData());
            if (message != null) {
                markAsRead(spaceId, message.getId());
            }
        });
    }

    public void markAsRead(@NonNull String spaceId, @NonNull String messageId) {
        // TODO Find the cluster for the identifier instead of use home cluster always.
        Map<String, Object> body = new HashMap<>();
        body.put("objectType", ObjectModel.Type.activity);
        body.put("verb", ActivityModel.Verb.acknowledge);
        body.put("object", Maps.makeMap("id", WebexId.uuid(messageId), "objectType", ObjectModel.Type.activity));
        body.put("target", Maps.makeMap("id", WebexId.uuid(spaceId), "objectType", ObjectModel.Type.conversation));
        Service.Conv.homed(phone.getDevice()).post(body).to("activities").auth(phone.getAuthenticator()).async(null);
    }

    private void doDownload(@NonNull RemoteFileImpl file,
                            @Nullable java.io.File path,
                            boolean thumbnail,
                            @Nullable ProgressHandler progressHandler,
                            @NonNull CompletionHandler<Uri> completionHandler) {
        File outFile = path;
        if (outFile == null) {
            outFile = new java.io.File(context.getCacheDir(), "com.ciscowebex.sdk.downloads");
            outFile.mkdirs();
        }
        String name = UUID.randomUUID().toString();
        if (file.getDisplayName() != null) {
            name = name + "-" + file.getDisplayName();
        }
        if (thumbnail) {
            name = "thumb-" + name;
        }
        outFile = new File(outFile, name);
        try {
            if (!outFile.createNewFile()) {
                ResultImpl.errorInMain(completionHandler, "failed to download File " + file.toString());
                return;
            }
            DownloadFileOperation operation = new DownloadFileOperation(phone.getCredentials().getAuthenticator(), file.getFile(), progressHandler);
            operation.run(outFile, thumbnail, completionHandler);
        } catch (Throwable t) {
            ResultImpl.errorInMain(completionHandler, t);
        }
    }

    private void doPost(Identifier conversation, DraftImpl draft, CompletionHandler<Message> handler) {
        Message.Text text = draft.getText();
        Map<String, Object> parent = draft.getParent() == null ? null : Maps.makeMap("id", WebexId.uuid(draft.getParent().getId()), "type", "reply");
        Map<String, Object> target = Maps.makeMap("id", conversation.uuid(), "objectType", ObjectModel.Type.conversation);
        Map<String, Object> object = new HashMap<>();
        object.put("objectType", ObjectModel.Type.comment);
        if (text != null) {
            object.put("displayName", text.getPlain());
            object.put("content", text.getHtml());
            object.put("markdown", text.getMarkdown());
        }
        if (!Checker.isEmpty(draft.getMentions())) {
            List<Map<String, Object>> mentionedPeople = new ArrayList<>();
            List<Map<String, Object>> mentionedGroup = new ArrayList<>();
            for (Mention mention : draft.getMentions()) {
                if (mention instanceof Mention.Person) {
                    mentionedPeople.add(Maps.makeMap("objectType", ObjectModel.Type.person, "id", WebexId.uuid(((Mention.Person) mention).getPersonId())));
                }
                else if (mention instanceof Mention.All) {
                    mentionedGroup.add(Maps.makeMap("objectType", ObjectModel.Type.groupMention, "groupType", "all"));
                }
            }
            if (mentionedPeople.size() > 0) {
                object.put("mentions", Maps.makeMap("items", mentionedPeople));
            }
            if (mentionedGroup.size() > 0) {
                object.put("groupMentions", Maps.makeMap("items", mentionedGroup));
            }
        }
        KeyManager.shared.getKey(conversation, phone.getCredentials(), phone.getDevice(), keyResult -> {
            if (keyResult.getError() != null) {
                ResultImpl.errorInMain(handler, keyResult);
                return;
            }
            KeyObject key = keyResult.getData();
            if (key != null && text != null) {
                object.put("displayName", CryptoUtils.encryptToJwe(key, text.getPlain()));
                object.put("content", CryptoUtils.encryptToJwe(key, text.getHtml()));
                object.put("markdown", CryptoUtils.encryptToJwe(key, text.getMarkdown()));
            }
            UploadFileOperations operations = new UploadFileOperations(draft.getFiles());
            operations.run(phone.getCredentials().getAuthenticator(), phone.getDevice(), conversation, key, fileModels -> {
                if (fileModels.getError() != null) {
                    ResultImpl.errorInMain(handler, fileModels);
                    return;
                }
                ActivityModel.Verb verb = ActivityModel.Verb.post;
                if (!Checker.isEmpty(fileModels.getData())) {
                    object.put("objectType", ObjectModel.Type.content);
                    object.put("contentCategory", "documents");
                    object.put("files", Maps.makeMap("items", fileModels.getData()));
                    verb = ActivityModel.Verb.share;
                }
                Map<String, Object> activityMap = new HashMap<>();
                activityMap.put("verb", verb.name());
                activityMap.put("clientTempId", uuid + ":" + UUID.randomUUID().toString());
                activityMap.put("encryptionKeyUrl", key == null ? null :key.getKeyUrl());
                activityMap.put("object", object);
                activityMap.put("target", target);
                activityMap.put("parent", parent);
                // TODO Find the cluster for the conv instead of use home cluster always.
                Service.Conv.specific(conversation.url(phone.getDevice()))
                        .post(activityMap)
                        .to("activities")
                        .auth(phone.getAuthenticator())
                        .model(ActivityModel.class)
                        .error(handler)
                        .async((Closure<ActivityModel>) model -> {
                            model.decrypt(key);
                            ResultImpl.inMain(handler, createMessage(model, false));
                        });
            });
        });
    }

    private void getOrCreateConversationWithPerson(String person, CompletionHandler<Identifier> handler) {
        Queue.background.run(() -> {
            Identifier conversion = conversations.get(person);
            if (conversion != null) {
                handler.onComplete(ResultImpl.success(conversion));
                return;
            }
            Service.Conv.homed(phone.getDevice()).put().to("conversations/user/" + person)
                    .with("activitiesLimit", String.valueOf(0))
                    .with("compact", String.valueOf(true))
                    .auth(phone.getAuthenticator())
                    .queue(Queue.background)
                    .model(ConversationModel.class)
                    .error(handler)
                    .async((Closure<ConversationModel>) model -> {
                        if (model == null || model.getId() == null) {
                            handler.onComplete(ResultImpl.error("Cannot get conversion with the people: " + person));
                            return;
                        }
                        Identifier identifier = new Identifier(new WebexId(WebexId.Type.ROOM, model.getId()), model.getUrl());
                        conversations.put(person, identifier);
                        handler.onComplete(ResultImpl.success(identifier));
                    });
        });
    }

    private Message createMessage(ActivityModel activity, boolean received) {
        return activity == null ? null : new InternalMessage(activity, phone.getCredentials(), received);
    }

    @Override
    @Deprecated
    public void list(@NonNull String spaceId, @Nullable String before, @Nullable String beforeMessage, @Nullable String mentionedPeople, @IntRange(from = 0, to = Integer.MAX_VALUE) int max, @NonNull CompletionHandler<List<Message>> handler) {
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
        } else if (beforeMessage != null) {
            b = new Before.Message(beforeMessage);
        }
        list(spaceId, b, max, mentions == null ? null : mentions.toArray(new Mention[0]), handler);
    }

    @Override
    @Deprecated
    public void post(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, @Nullable String text, @Nullable String markdown, @Nullable String[] files, @NonNull CompletionHandler<Message> handler) {
        String idOrEmail = (spaceId == null ? (personId == null ? personEmail : personId) : spaceId);
        List<LocalFile> localFiles = null;
        if (files != null && files.length > 0) {
            localFiles = new ArrayList<>(files.length);
            for (String file : files) {
                java.io.File f = new java.io.File(file);
                try {
                    localFiles.add(new LocalFile(f));
                } catch (Exception e) {
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
            } else {
                postToPerson(email, text, files, handler);
            }
        } else if (webexId.is(WebexId.Type.ROOM)) {
            postToSpace(idOrEmail, text, mentions, files, handler);
        } else if (webexId.is(WebexId.Type.PEOPLE)) {
            postToPerson(idOrEmail, text, files, handler);
        }
    }


}
