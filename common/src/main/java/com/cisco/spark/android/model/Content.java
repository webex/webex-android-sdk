package com.cisco.spark.android.model;

import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class Content extends ActivityObject implements Mentionable {
    private ItemCollection<File> files = new ItemCollection<File>();
    private String contentCategory;
    private ItemCollection<Person> mentions;

    public Content(String contentCategory) {
        super(ObjectType.content);
        this.contentCategory = contentCategory;
    }

    public ItemCollection<File> getFiles() {
        return files;
    }

    public String getContentCategory() {
        return contentCategory;
    }

    public boolean isImage() {
        return (contentCategory != null && contentCategory.equals(ContentCategory.IMAGES) && !isWhiteboard());
    }

    public boolean isFile() {
        return (contentCategory != null && contentCategory.equals(ContentCategory.DOCUMENTS));
    }

    public boolean isVideo() {
        return (contentCategory != null && contentCategory.equals(ContentCategory.VIDEOS));
    }

    /** Whitebaord activity:
     * https://sqbu-github.cisco.com/ghewett/whiteboard-integration-notes/blob/master/board_activity%20V2.md
     * In the future we may add values for actions.mimeType of kanbanboard, scrumboard, etc.
     For a given object.files.items[n] all the actions[*].mimeType should have the same value
     The client should validate that a given object.files.items[n] is for a board by the actions key is present and it is an array
     The client can determine the type of board (whiteboard, kanbanboard, scrumboard, etc.) using actions[0].mimeType
     */
    public boolean isWhiteboard() {
        if (getFiles().getItems().size() > 0) {
            File file = getFiles().getItems().get(0);
            List<ContentAction> actions = file.getActions();
            if ((actions != null) && (actions.size() > 0) && (ActionMimeType.WHITEBOARD.equals(actions.get(0).getMimeType()))) {
                return true;
            }
        }
        return false;
    }

    public void setFiles(ItemCollection<File> files) {
        this.files = files;
    }

    public ItemCollection<File> getContentFiles() {
        return files;
    }

    @Override
    public ItemCollection<Person> getMentions() {
        return mentions;
    }

    public void setMentions(ItemCollection<Person> mentions) {
        this.mentions = mentions;
    }

    @Override
    public String toString() {
        return "contentCategory:" + contentCategory;
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
        }

        if (getFiles() != null) {
            for (File file : getFiles().getItems()) {
                file.encrypt(key);
            }
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }

        if (getFiles() != null) {
            for (File file : getFiles().getItems()) {
                file.decrypt(key);
            }
        }
    }
}
