package com.cisco.spark.android.client;

import android.net.Uri;
import com.cisco.spark.android.model.LogMetadataRequest;
import com.cisco.spark.android.model.UrlResponse;
import retrofit.http.Body;
import retrofit.http.POST;

public interface AdminClient {

    public static class LogURLResponse {
        private Uri tempURL;
        private String logFilename;
        private String userId;

        public Uri getTempURL() {
            return tempURL;
        }

        public void setTempURL(Uri tempURL) {
            this.tempURL = tempURL;
        }

        public String getLogFilename() {
            return logFilename;
        }

        public void setLogFilename(String value) {
            logFilename = value;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String value) {
            userId = value;
        }
    }

    public static class LogUploadRequest {
        private String file;
        // public default constructor for GSON
        public LogUploadRequest() { }
        public LogUploadRequest(String file) {
            this.file = file;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }

    /**
     * Fetch a URL where we can upload a file (usually logs) to be saved next to this user in the admin service.
     * @param logRequest - the filename passed here should match the filename passed to the upload
     */
    @POST("/logs/url/")
    LogURLResponse getUploadFileUrl(@Body LogUploadRequest logRequest);

    /**
     * Associate name/value pair metadata with an uploaded log filename
     * @param request
     * @return
     */
    @POST("/logs/meta")
    public UrlResponse setLogMetadata(@Body LogMetadataRequest request);
}
