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

import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

/**
 * The enumeration of error types in Cisco Webex Android SDK.
 *
 * @since 0.1
 */
public class WebexError<T> {

    public enum ErrorCode {
        UNEXPECTED_ERROR,
        SERVICE_ERROR,
        PERMISSION_ERROR
    }

    @StringPart
    protected ErrorCode _code = null;

    @StringPart
    protected String _message = "";

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
        _code = errorCode;
    }

    /**
     * The constructor with the error code and error message
     *
     * @param errorCode the error code
     * @param message   the error message
     */
    public WebexError(ErrorCode errorCode, String message) {
        _code = errorCode;
        _message = message;
    }

    /**
     * The constructor with the error code and error message
     *
     * @param errorCode the error code
     * @param message   the error message
     * @param data      the error data
     */
    public WebexError(ErrorCode errorCode, String message, T data) {
        _code = errorCode;
        _message = message;
        _data = data;
    }

    /**
     * @return The code of this error.
     * @since 0.1
     */
    public ErrorCode getErrorCode() {
        return _code;
    }

    /**
     * @return The message of this error.
     * @since 0.1
     */
    public String getErrorMessage() {
        return _message;
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
