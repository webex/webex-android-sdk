package com.cisco.spark.android.sync;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Place;
import com.cisco.spark.android.model.Provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


public class DisplayableFileSet extends Message implements Parcelable {
    private List<DisplayableFile> items;
    private String name;

    public DisplayableFileSet(String message, String content, ActorRecord.ActorKey actorKey, Activity activity, List<DisplayableFile> items, Provider provider) {
        super(message, content, actorKey, activity, provider);
        this.items = items;
    }

    public static DisplayableFileSet fromActivity(Activity activity) {
        List<File> files = ((Content) activity.getObject()).getFiles().getItems();

        List<DisplayableFile> items = new ArrayList<DisplayableFile>();
        for (File file : files) {
            final DisplayableFile displayableFile = DisplayableFile.from(file, activity.getConversationId());
            items.add(displayableFile);
        }

        DisplayableFileSet displayableFileSet = new DisplayableFileSet(activity.getObject().getDisplayName(), activity.getObject().getContent(), activity.getActor().getKey(), activity, items, activity.getProvider());
        Place location = activity.getLocation();
        if (location != null) {
            displayableFileSet.setLocationDisplayName(location.getDisplayName());
            displayableFileSet.setLocationPosition(location.getPosition());
        }

        return displayableFileSet;
    }

    public DisplayableFile item(int location) {
        if (items == null || items.isEmpty())
            return null;

        return items.get(location);
    }

    public void setPhotos(List<DisplayableFile> photolist) {
        items = photolist;
    }

    public @Nullable List<DisplayableFile> getItems() {
        return items;
    }

    public int size() {
        if (items == null)
            return 0;

        return items.size();
    }

    public String getName() {
        return name;
    }

    //
    // Implement Parcelable interface
    //
    public DisplayableFileSet(Parcel in) {
        items = new ArrayList<>();
        in.readList(items, DisplayableFile.class.getClassLoader());
        name = in.readString();
        if (TextUtils.isEmpty(name)) {
            name = items.get(0).getName();
        }
    }

    public static final Parcelable.Creator<DisplayableFileSet> CREATOR
            = new Parcelable.Creator<DisplayableFileSet>() {
        public DisplayableFileSet createFromParcel(Parcel in) {
            return new DisplayableFileSet(in);
        }

        public DisplayableFileSet[] newArray(int size) {
            return new DisplayableFileSet[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeList(items);
        destination.writeString(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException {
        super.decrypt(key);

        if (getItems() != null) {
            for (DisplayableFile file : getItems()) {
                file.decrypt(key);
            }
        }
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);
        if (getItems() != null) {
            for (DisplayableFile file : getItems()) {
                file.encrypt(key);
            }
        }
    }
}
