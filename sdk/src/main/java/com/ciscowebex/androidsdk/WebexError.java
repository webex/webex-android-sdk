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

package com.ciscowebex.androidsdk;

import android.text.TextUtils;

import com.github.benoitdion.ln.Ln;

import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The enumeration of error types in Cisco Webex Android SDK.
 *
 * @since 0.1
 */
public class WebexError<T> {
    private static final String[] CISCO_SPARK_ERROR_CODES = new String[]{
            "2423005",  // LOCUS_REQUIRES_MODERATOR_PIN_OR_GUEST
            "2423006", //LOCUS_REQUIRES_MODERATOR_PIN_OR_GUEST_PIN
            "2423016", //LOCUS_REQUIRES_MODERATOR_KEY_OR_MEETING_PASSWORD
            "2423017", //LOCUS_REQUIRES_MODERATOR_KEY_OR_GUEST
            "2423018" //LOCUS_REQUIRES_MEETING_PASSWORD
    };

    public static WebexError from(String message) {
        return new WebexError(WebexError.ErrorCode.UNEXPECTED_ERROR, message);
    }

    public static WebexError from(Throwable t) {
        return new WebexError(WebexError.ErrorCode.UNEXPECTED_ERROR, t.toString());
    }

    public static WebexError from(okhttp3.Response res) {
        StringBuilder message = new StringBuilder().append(res.code()).append("/").append(res.message());
        try {
            ResponseBody body = res.body();
            message.append("/").append(body == null ? "" : body.string());
        } catch (IOException e) {
            Ln.e(e);
        }
        String ciscoSparkErrorCodes = res.header("Cisco-Spark-Error-Codes");
        if (!TextUtils.isEmpty(ciscoSparkErrorCodes)) {
            if (Arrays.asList(CISCO_SPARK_ERROR_CODES).contains(ciscoSparkErrorCodes)) {
                return new WebexError(ErrorCode.HOST_PIN_OR_MEETING_PASSWORD_REQUIRED, message.toString());
            }
        }
        return new WebexError(WebexError.ErrorCode.SERVICE_ERROR, message.toString());
    }

    public enum ErrorCode {
        UNEXPECTED_ERROR,
        SERVICE_ERROR,
        PERMISSION_ERROR,
        /**
         * The host pin or meeting password is required while dialing.
         */
        HOST_PIN_OR_MEETING_PASSWORD_REQUIRED
    }

    @StringPart
    protected int errorCode = -7000;

    protected ErrorCode errorCodeType;

    @StringPart
    protected String message = "";

    protected T _data = null;

    /**
     * The default constructor
     */
    public WebexError() {
    }

    /**
     * The constructor with the error code
     *
     * @param errorCode the error code
     */
    public WebexError(ErrorCode errorCode) {
        this.errorCode = -7000 - errorCode.ordinal();
        this.errorCodeType = errorCode;
    }

    /**
     * The constructor with the error code and error message
     *
     * @param errorCode the error code
     * @param message   the error message
     */
    public WebexError(ErrorCode errorCode, String message) {
        this(errorCode);
        this.message = message;
    }

    /**
     * The constructor with the error code and error message
     *
     * @param errorCode the error code
     * @param message   the error message
     * @deprecated
     */
    public WebexError(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.errorCodeType = ErrorCode.UNEXPECTED_ERROR;
    }

    /**
     * The constructor with the error code and error message
     *
     * @param errorCode the error code
     * @param message   the error message
     * @param data      the error data
     */
    public WebexError(ErrorCode errorCode, String message, T data) {
        this(errorCode);
        this.message = message;
        _data = data;
    }

    /**
     * @return The code of this error.
     * @since 0.1
     * @deprecated
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @return The type of this error.
     * @since 2.6.0
     */
    public ErrorCode getErrorCodeType() {
        return errorCodeType;
    }

    /**
     * @return The message of this error.
     * @since 0.1
     */
    public String getErrorMessage() {
        return message;
    }

    /**
     * @return The data of this error.
     * @since 0.1
     */
    public T getData() {
        return _data;
    }

    @Override
    public String toString() {
        return Objects.toStringByAnnotation(this);
    }

}
