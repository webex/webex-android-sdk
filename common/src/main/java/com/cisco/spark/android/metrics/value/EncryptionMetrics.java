package com.cisco.spark.android.metrics.value;


import android.net.Uri;

import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Strings;

public class EncryptionMetrics {
    /**
     * These classes are serialized to json and passed to splunk
     */

    static public final int TIMEOUT_STATUS_CODE = 408;

    static public final String ERROR_TIMEOUT = "timeout";
    static public final String ERROR_SHARED_KEY = "shared_key_fail";
    static public final String ERROR_DECRYPT_FAILED = "fail";
    static public final String ERROR_UNBOUND_KEY_FETCH_TIMEOUT = "unbound_key_fetch_timeout";

    abstract public static class BaseMetric {
        public abstract String getKey();

        public boolean typeMatches(BaseMetric that) {
            return that != null && getKey().equals(that.getKey());
        }
    }

    public static class ContentMetric extends BaseMetric {
        public String getKey() {
            return "encryption_content";
        }

        public enum Direction {
            up, down
        }

        public enum Kind {
            image, video, doc, thumbnail, unknown;

            public static Kind fromFilename(String filename) {
                switch (MimeUtils.getContentTypeByFilename(filename)) {

                    case IMAGE:
                        return image;
                    case EXCEL:
                    case POWERPOINT:
                    case WORD:
                    case PDF:
                    case SKETCH:
                        return doc;
                    case VIDEO:
                        return video;
                    default:
                        return unknown;
                }
            }
        }

        private Direction direction;
        private long bytes;
        private long totalDuration;
        private Kind kind;

        public ContentMetric() {
        }

        public ContentMetric(Direction direction, long bytes, long totalDuration, Kind kind) {
            this.direction = direction;
            this.bytes = bytes;
            this.totalDuration = totalDuration;
            this.kind = kind;
        }

        @Override
        public boolean typeMatches(BaseMetric that) {
            return super.typeMatches(that)
                    && ((ContentMetric) that).direction == this.direction
                    && ((ContentMetric) that).kind == this.kind;
        }

        @Override
        public String toString() {
            return "EncryptionMetrics.Content: " + kind + " " + bytes + "b "
                    + (direction == Direction.down ? " decrypted in " : " encrypted in ")
                    + totalDuration + "ms incl transfer";
        }
    }

    public static class KeyFetchMetric extends BaseMetric {
        transient static public final String key = "encryption_key_fetch_bound";

        public String getKey() {
            return key;
        }

        private String keyId;
        private long duration;
        private Boolean initialSync;

        public KeyFetchMetric() {
        }

        public KeyFetchMetric(Uri keyId, long duration, boolean initialSync) {
            this.keyId = Strings.sha256(keyId.toString());
            this.duration = duration;

            if (initialSync)
                this.initialSync = Boolean.TRUE;
        }

        public long getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return "EncryptionMetrics.KeyFetchMetric: " + keyId + " : " + duration + "ms "
                    + (initialSync == Boolean.TRUE ? "initialSync" : "");
        }
    }

    public static class UxDecryptionWaitMetric extends BaseMetric {
        public String getKey() {
            return "encryption_ux_wait";
        }

        public enum WaitStatus {
            delayed, failed
        }

        private String keyId;
        private String roomId;
        private WaitStatus waitStatus;
        private String failReason;
        private Boolean initialSync;
        private Boolean isTitle;

        public UxDecryptionWaitMetric() {
        }

        public UxDecryptionWaitMetric(Uri keyId, String roomId, WaitStatus waitStatus, String failReason, boolean isTitle, boolean initialSync) {
            if (keyId != null)
                this.keyId = Strings.md5(keyId.toString());
            if (roomId != null)
                this.roomId = Strings.md5(roomId);
            this.waitStatus = waitStatus;
            this.failReason = failReason;

            if (isTitle)
                this.isTitle = Boolean.TRUE;

            if (initialSync)
                this.initialSync = Boolean.TRUE;
        }

        @Override
        public boolean typeMatches(BaseMetric that) {
            return super.typeMatches(that)
                    && ((UxDecryptionWaitMetric) that).waitStatus == this.waitStatus;
        }

        @Override
        public String toString() {
            return "EncryptionMetrics.UxDecryptionWaitMetric: key " + keyId +  " " + waitStatus + " "
                    + (failReason == null ? "" : failReason) + "' " + (initialSync == Boolean.TRUE ? "initialSync " : "");
        }
    }

    public static class UnboundKeyFetchMetric extends BaseMetric {
        public String getKey() {
            return "encryption_key_fetch_unbound";
        }

        private transient long fetchStartTime;

        private long duration;
        private long count;

        public UnboundKeyFetchMetric() {
        }

        public UnboundKeyFetchMetric(long keysToFetch) {
            this.count = keysToFetch;
            fetchStartTime = System.currentTimeMillis();
        }

        public void markFinishTime() {
            duration = System.currentTimeMillis() - fetchStartTime;
        }

        @Override
        public boolean typeMatches(BaseMetric that) {
            return super.typeMatches(that)
                    && ((UnboundKeyFetchMetric) that).count == this.count;
        }

        @Override
        public String toString() {
            return "EncryptionMetrics.UnboundKeyFetchMetric: " + count + " in " + duration + "ms";
        }
    }


    public static class SharedKeySetupTime extends BaseMetric {
        public String getKey() {
            return "encryption_key_shared_setup";
        }

        private long duration;

        public SharedKeySetupTime() {
        }

        public SharedKeySetupTime(long duration) {
            this.duration = duration;
        }

        @Override
        public String toString() {
            return "EncryptionMetrics.SharedKeySetupTime: " + duration;
        }
    }

    public static class Error extends BaseMetric {
        public String getKey() {
            return "encryption_error";
        }

        private int statusCode;
        private String description;

        public Error() {
        }

        public Error(int statusCode, String description) {
            this.statusCode = statusCode;
            this.description = description;
        }

        @Override
        public boolean typeMatches(BaseMetric that) {
            return super.typeMatches(that)
                    && ((Error) that).statusCode == this.statusCode
                    && ((Error) that).description == this.description;
        }

        @Override
        public String toString() {
            return "EncryptionMetrics.Error: " + statusCode + " " + description;
        }
    }
}
