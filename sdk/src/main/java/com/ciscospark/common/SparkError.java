/*
 * Copyright (c) 2016-2017 Cisco Systems Inc
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

package com.ciscospark.common;


/**
 * Common Error class for Spark
 */

public class SparkError {
    public enum ErrorCode {
        /* add error code here */
        UNKNOWN,
    }

    private final static String[] DEFAULT_ERROR_MESSAGE = {
            "Unknown Error",
    };

    protected ErrorCode mErrorCode = ErrorCode.UNKNOWN;
    protected String mErrorMessage = DEFAULT_ERROR_MESSAGE[0];

    public SparkError() {
    }

    public SparkError(ErrorCode errorCode) {
        mErrorCode = errorCode;
        switch(mErrorCode) {
            case UNKNOWN:
            default:
                mErrorMessage = DEFAULT_ERROR_MESSAGE[0];
        }
    }

    public SparkError(ErrorCode errorcode, String message) {
        mErrorCode = errorcode;
        mErrorMessage = message;
    }

    ErrorCode getErrorCode() {
        return mErrorCode;
    }

    String getErrorMessage() {
        return mErrorMessage;
    }

    @Override
    public String toString() {
        String code;
        String message;
        if(mErrorCode == null){
            code = "";
        }else
        {
            code =mErrorCode.toString();
        }

        if(mErrorMessage == null){
            message = "";
        }else
        {
            message =mErrorMessage.toString();
        }

        return code + ": " + message;
    }
}
