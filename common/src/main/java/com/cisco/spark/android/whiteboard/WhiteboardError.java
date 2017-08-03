package com.cisco.spark.android.whiteboard;

public class WhiteboardError {

    public enum ErrorData {
        NONE(10000, "Succeed"),
        NETWORK_ERROR(10001, "NetWork Issue"),
        LOAD_BOARD_ERROR(10002, "Can't Load Board"),
        CREATE_BOARD_ERROR(10003, "Can't Create Board"),
        CLEAR_BOARD_ERROR(10004, "Can't Clear Board"),
        SAVE_CONTENT_ERROR(10005, "Can't Save Content"),
        LOSE_NETWORK_CONNECTION_ERROR(10006, "Losing network connection"),
        LOAD_BOARD_LIST_ERROR(10007, "Load whiteboard list failed"),
        PATCH_BOARD_ERROR(10008, "Patching the whiteboard failed"),
        UPDATE_BOARD_ERROR(10009, "Updating the whiteboard failed"),
        GET_BOARD_SPACEURL_ERROR(10010, "Getting the hidden space url failed"),
        GET_BOARD_ERROR(10011, "Getting the channel data failed"),
        CLEAR_PARTIAL_CONTENTS_ERROR(10012, "Clear partial contents failed"),
        DECRYPT_CONTENTS_ERROR(10013, "Decrypting the content of the board failed");

        private int errorCode;
        private String message;


        ErrorData(int errorCode, String message) {
            this.errorCode = errorCode;
            this.message = message;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

    private ErrorData errorData;
    private String channelId;
    private WhiteboardOriginalData originalData;
    private String errorMessage;
    private int httpCode;

    public WhiteboardError(ErrorData errorData) {
        this.errorData = errorData;
    }

    public WhiteboardError(ErrorData errorData, WhiteboardOriginalData originalData) {
        this(errorData);
        this.originalData = originalData;
    }

    public WhiteboardError(ErrorData errorData, WhiteboardOriginalData originalData, String errorMessage) {
        this(errorData, originalData);
        this.errorMessage = errorMessage;
    }

    public WhiteboardError(ErrorData errorData, WhiteboardOriginalData originalData, String errorMessage, int httpCode) {
        this(errorData, originalData, errorMessage);
        this.httpCode = httpCode;
    }

    public WhiteboardError(ErrorData errorData, String channelId) {
        this(errorData);
        this.channelId = channelId;
    }

    public ErrorData getErrorData() {
        return errorData;
    }

    public WhiteboardOriginalData getOriginalData() {
        return originalData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getChannelId() {
        return channelId;
    }
}

