package com.cisco.spark.android.whiteboard.persistence;

import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class WhiteboardListCache {

    private static final int MAX_ENTRIES = 5;

    private Map<String, WhiteboardListCacheEntry> cache;

    @Inject
    public WhiteboardListCache() {
        cache = new LinkedHashMap<String, WhiteboardListCacheEntry>() {
            @Override
            protected boolean removeEldestEntry(Entry<String, WhiteboardListCacheEntry> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    public synchronized WhiteboardListCacheEntry get(final String conversationId) {
        WhiteboardListCacheEntry cachedEntry = cache.get(nonNullConversationId(conversationId));
        return cachedEntry;
    }

    public synchronized void put(final String conversationId, final List<Channel> channels, final String link) {
        cache.put(nonNullConversationId(conversationId), new WhiteboardListCacheEntry(channels, link));
    }

    public synchronized void add(final String conversationId, final List<Channel> newChannels, final String link) {
        WhiteboardListCacheEntry cacheEntry = get(conversationId);
        List<Channel> currentChannels;
        if (cacheEntry != null) {
            currentChannels = cacheEntry.getChannels();
            for (Channel newChannel : newChannels) {
                boolean alreadyPreset = false;
                for (Channel currentChannel : currentChannels) {
                    if (currentChannel.getChannelId().equalsIgnoreCase(newChannel.getChannelId()))
                        alreadyPreset = true;
                }
                if (!alreadyPreset)
                    currentChannels.add(newChannel);
            }
        } else {
            currentChannels = newChannels;
        }
        cache.put(nonNullConversationId(conversationId), new WhiteboardListCacheEntry(currentChannels, link));
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized void update(Channel updatedChannel) {
        Set<String> keySet = cache.keySet();
        for (String key : keySet) {
            WhiteboardListCacheEntry entry = cache.get(key);
            List<Channel> channels = entry.getChannels();
            boolean isRemoved = false;
            for (Iterator<Channel> it = channels.iterator(); it.hasNext();) {
                Channel channel = it.next();
                if (Strings.equals(updatedChannel.getChannelId(), channel.getChannelId())) {
                    it.remove();
                    isRemoved = true;
                    break;
                }
            }
            if (isRemoved) {
                channels.add(updatedChannel);
                break;
            }
        }
    }

    public synchronized boolean remove(String channelId) {
        boolean isRemoved = false;
        Set<String> keySet = cache.keySet();
        for (String key : keySet) {
            WhiteboardListCacheEntry entry = cache.get(key);
            List<Channel> channels = entry.getChannels();
            for (Iterator<Channel> it = channels.iterator(); it.hasNext();) {
                Channel channel = it.next();
                if (Strings.equals(channelId, channel.getChannelId())) {
                    it.remove();
                    isRemoved = true;
                    break;
                }
            }
        }
        return isRemoved;
    }

    class WhiteboardListCacheEntry {
        private List<Channel> mChannels;
        private String mLink;

        public WhiteboardListCacheEntry(List<Channel> channels, String link) {
            this.mChannels = channels;
            this.mLink = link;
        }

        List<Channel> getChannels() {
            return mChannels;
        }

        String getLink() {
            return mLink;
        }
    }

    private String nonNullConversationId(String conversationId) {
        return conversationId != null ? conversationId : "";
    }
}
