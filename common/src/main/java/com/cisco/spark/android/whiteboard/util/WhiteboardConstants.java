package com.cisco.spark.android.whiteboard.util;

import com.wacom.ink.path.PathBuilder;

public class WhiteboardConstants {

    public static final int MAX_TOUCH_POINTERS_DEFAULT = 2;
    public static final int MAX_TOUCH_POINTERS_PLENTY = 10;
    public static int maxTouchPointers = MAX_TOUCH_POINTERS_DEFAULT;
    public static void toggleMaxTouchPointers(boolean enableMoreTouches) {
        maxTouchPointers = enableMoreTouches ? MAX_TOUCH_POINTERS_PLENTY : MAX_TOUCH_POINTERS_DEFAULT;
    }

    public static final Object REMOTE_WRITER_KEY_SEPARATOR = "#";

    /**
     * Local drawing parameters
     */
    public static final float PATH_BUILDER_NORMALIZATION_CONFIG_MIN_VALUE = 100.0f;
    public static final float PATH_BUILDER_NORMALIZATION_CONFIG_MAX_VALUE = 4000.0f;
    public static final float PATH_BUILDER_MOVEMENT_THRESHOLD = 0.0f;
    public static final float PEN_INITIAL_WIDTH = 2f;
    public static final float PEN_MIN_WIDTH = 1.6f;
    public static final float PEN_MAX_WIDTH = 2.4f;
    public static final float ERASER_WIDTH = 28.0f;
    public static final PathBuilder.PropertyFunction PATH_FUNCTION = PathBuilder.PropertyFunction.Power;
    public static final float PATH_FUNCTION_PARAMETER = 2.1f;

    /**
     * Realtime message revisions
     */
    public static final String R0 = "r0";
    public static final String R1 = "r1";

    /**
     * Realtime message types
     */
    public static final String CONTENT_BEGIN = "contentBegin";
    public static final String CONTENT_UPDATE = "contentUpdate";
    public static final String CONTENT_COMMIT = "contentCommit";
    public static final String CONTENT_CANCEL = "contentCancel";
    public static final String EVENT_START_CLEARBOARD = "startClearBoard";
    public static final String EVENT_END_CLEARBOARD = "endClearBoard";

    /**
     * Realtime message fields
     */
    public static final String CONTENT_TYPE = "type";
    public static final String CURVE_TYPE = "curve";
    public static final String ACTION = "action";
    public static final String NAME = "name";
    public static final String CONTENT_ARRAY = "contentArray";
    public static final String SENDER = "sender";
    public static final String DISPLAY_NAME = "name";
    public static final String CONTENTS_BUFFER = "contentsBuffer";
    public static final String CURVE_POINTS = "curvePoints";
    public static final String POINTS = "points";
    public static final String COLOR = "color";
    public static final String STRIDE = "stride";
    public static final String STYLE = "style";
    public static final String DRAW_MODE = "drawMode";
    public static final String ERASE = "ERASE";
    public static final String NORMAL = "NORMAL";
    public static final String ID = "id";
    public static final String CURVE_ID = "curveId";
    public static final String LAST_COMMIT = "lastCommit";

    /**
     * Persistence strings
     */
    public static final String SAVE_CONTENT = "saveContent";
    public static final String CLEAR_BOARD = "clearBoard";

    /**
     * Misc strings
     */
    public static final String WB_SNAPSHOT_FILENAME_BASE = "wb_preview";
    public static final String WB_SNAPSHOT_FILENAME_EXTENSION = ".png";
    public static final String WB_EMPTY_SNAPSHOT_FILENAME = "wb_preview_empty.png";

    //Size of whiteboard previews
    public static final int SNAPSHOT_WIDTH = 756;
    public static final int SNAPSHOT_HEIGHT = 426;

    public static final int WHITEBOARD_LIST_PAGE_SIZE = 30;
    public static final int WHITEBOARD_LIST_MAX_SIZE = 100;

    public static final long SNAPSHOT_REFRESH_INTERVAL_ACTIVE_BOARD_MILLIS = 5 * 1000;
    public static final long SNAPSHOT_REFRESH_INTERVAL_INACTIVE_BOARD_MILLIS = 20 * 1000;
    public static final long SNAPSHOT_INACTIVE_BOARD_AGE_MILLIS = 1 * 60 * 60 * 1000; // Board considered inactive if older than 1 hour
    public static final long SNAPSHOT_UPLOAD_DELAY_MILLIS = 5 * 1000;

    public static final long WHITEBOARD_LOAD_BOARD_LIST_DELAY_MILLIS = 5 * 1000;

    public static final int WHITEBOARD_CONTENT_BATCH_SIZE = 1000;

    public static final int CURVE_STALE_TIMEOUT_MS = 4000;

    /**
     * Metrics: result message
     */
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String UNKNOWN = "unknown";
    public static final String NETWORK_ISSUE = "networkFailure";
    public static final String INVALID_SNAPSHOT = "invalidSnapshot";
    public static final String COPY_FILE_FAILURE = "copyFileFailure";
    public static final String MERCURY_CONNECTION_FAILURE = "mercuryConnnectionFailure";
    public static final String SHARED_MERCURY_REPLACE_FAILURE = "replaceSharedMercuryFailure";
    public static final String SHARED_MERCURY_ADD_FAILURE = "addSharedMercuryFailure";
    public static final String SHARED_MERCURY_REMOVE_FAILURE = "removeSharedMercuryFailure";
    public static final String SHARED_MERCURY_GET_REGISTRATION_BINDINGS_FAILURE = "getSharedMercuryRegistrationBindingsFailure";
    public static final String LOAD_BOARD_CONTENT_FAILURE = "loadBoardContentFailure";
    public static final String LOAD_BOARD_CONTENT_NETWORK_ISSUE = "loadBoardContentNetworkIssue";



    public static final int WHITEBOARD_CONTENT_POINTS_LIMIT = 1000;
    public static final float LOGICAL_WIDTH = 1600f;
    public static final float LOGICAL_HEIGHT = 900f;

}
