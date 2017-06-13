package com.cisco.spark.android.content;

import com.github.benoitdion.ln.Ln;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ContentUploadMonitor {
    public static final int COMPETED_VALUE = 100;
    private HashMap<String, ContentUploadProgressProvider> progressProviders;
    private Set<String> compeletedListeners;
    private Set<String> removedListeners;

    public interface ContentUploadProgressProvider {
        int getProgress();

        boolean isCompleted();
    }

    @Inject
    public ContentUploadMonitor() {
        this.progressProviders = new HashMap<String, ContentUploadProgressProvider>();
        this.compeletedListeners = new HashSet<String>();
        this.removedListeners = new HashSet<String>();
    }

    public void addProgressProvider(String key, ContentUploadProgressProvider progressProvider) {
        Ln.d("ContentTest: Adding Listener for Key - " + key);
        this.progressProviders.put(key, progressProvider);
    }

    public boolean containsProgressListener(String key) {
        return this.progressProviders.containsKey(key);
    }

    public int getProgressForKey(String key) {
        if (this.progressProviders.get(key) != null) {
            return this.progressProviders.get(key).getProgress();
        } else {
            return -1;
        }
    }

    public boolean isCompleted(String key) {
        if (this.progressProviders.get(key) != null) {
            boolean completed = this.progressProviders.get(key).isCompleted();

            if (completed) {
                this.compeletedListeners.add(key);
                this.progressProviders.remove(key);
            }

            return completed;
        } else {
            return this.compeletedListeners.contains(key);
        }
    }

    public void removeKey(String key) {
        this.compeletedListeners.remove(key);
        this.removedListeners.add(key);
    }

    public boolean isRemoved(String key) {
        return this.removedListeners.contains(key);
    }
}
