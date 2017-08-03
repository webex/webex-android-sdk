package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Mentionable;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Place;
import com.cisco.spark.android.model.Provider;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;

public class Message {
    /**
     * text is always plain text
     */
    private String text;

    /**
     * content can contain markup.
     */
    private String content;

    private ActorRecord.ActorKey actorKey;
    private Provider provider;

    private HashSet<String> activities = new HashSet<String>();
    private String locationDisplayName;
    private String locationPosition;
    private ItemCollection<Person> mentions;
    public static final int MAX_MESSAGE_LENGTH = 10000;

    public String getText() {
        trimMessage();
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMentions(ItemCollection<Person> mentions) {
        this.mentions = mentions;
    }

    public ItemCollection<Person> getMentions() {
        return mentions;
    }

    public void setActorKey(ActorRecord.ActorKey actorKey) {
        this.actorKey = actorKey;
    }

    public ActorRecord.ActorKey getActorKey() {
        return actorKey;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Provider getProvider() {
        return provider;
    }

    public Message() {
    }

    public Message(String text, String content, ActorRecord.ActorKey actorKey, Activity activity, Provider provider) {
        this.text = Strings.nullToEmpty(text);
        this.content = Strings.nullToEmpty(content);
        this.actorKey = actorKey;
        this.provider = provider;
        addActivity(activity);
        trimMessage();

        Place location = activity.getLocation();
        if (location != null) {
            setLocationDisplayName(location.getDisplayName());
            setLocationPosition(location.getPosition());
        }

        if (activity.getObject() != null && activity.getObject() instanceof Mentionable) {
            setMentions(((Mentionable) activity.getObject()).getMentions());
        }
    }

    public Message(Activity activity) {
        this(activity.getObject().getDisplayName(), activity.getObject().getContent(), activity.getActor() != null ? activity.getActor().getKey() : null, activity, activity.getProvider());
    }

    private void trimMessage() {
        if (text != null && text.length() > MAX_MESSAGE_LENGTH) {
            this.text = text.substring(0, MAX_MESSAGE_LENGTH);
        }
    }

    public boolean appendText(String textToAppend) {
        if (textToAppend == null) {
            textToAppend = "";
        }
        if (text.length() + textToAppend.length() > MAX_MESSAGE_LENGTH) {
            return false;
        }
        if (Strings.isEmpty(text)) {
            text = textToAppend;
        } else {
            if (!Strings.isEmpty(textToAppend)) {
                text = text + "\n" + textToAppend;
            }
        }
        return true;
    }

    //$REVIEW jk How would we get multiple activities in one Message?
    public void addActivity(Activity activity) {
        if (activities == null) {
            activities = new HashSet<>();
        }
        activities.add(activity.getId());
    }

    public HashSet<String> getActivities() {
        return activities;
    }

    public void setLocationDisplayName(String locationDisplayName) {
        this.locationDisplayName = locationDisplayName;
    }

    public String getLocationDisplayName() {
        return locationDisplayName;
    }

    public void setLocationPosition(String locationPosition) {
        this.locationPosition = locationPosition;
    }

    /**
     * @see {@link Place#getPosition}
     */
    public String getLocationPosition() {
        return locationPosition;
    }

    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        text = CryptoUtils.decryptFromJwe(key, text);
        content = CryptoUtils.decryptFromJwe(key, content);
    }

    public void encrypt(KeyObject key) throws IOException {
        text = CryptoUtils.encryptToJwe(key, text);
        content = CryptoUtils.encryptToJwe(key, content);
    }
}
