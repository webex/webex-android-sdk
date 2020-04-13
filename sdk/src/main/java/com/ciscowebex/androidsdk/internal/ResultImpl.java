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

package com.ciscowebex.androidsdk.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.WebexError;

import com.ciscowebex.androidsdk.internal.queue.Queue;
import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

public class ResultImpl<T> implements Result<T> {

    public static <T> Result<T> success(T data) {
        return new ResultImpl<>(data, null);
    }

    public static <T> Result<T> error(String message) {
        return new ResultImpl<>(null, WebexError.from(message));
    }

    public static <T> Result<T> error(Throwable t) {
        return new ResultImpl<>(null, WebexError.from(t));
    }

    public static <T> Result<T> error(WebexError error) {
        return new ResultImpl<>(null, error);
    }

    public static <T> Result<T> error(okhttp3.Response response) {
        return new ResultImpl<>(null, WebexError.from(response));
    }

    public static <T1, T2> Result<T2> error(Result<T1> result) {
        WebexError error = result == null ? WebexError.from("No Result") : result.getError();
        if (error == null) {
            error = WebexError.from("No Result");
        }
        return new ResultImpl<>(null, error);
    }

    public static <T> void inMain(@NonNull CompletionHandler<T> handler, @Nullable T data) {
        Queue.main.run(() -> handler.onComplete(ResultImpl.success(data)));
    }

    public static <T> void inMain(@NonNull CompletionHandler<T> handler, @Nullable Result<T> result) {
        if (result == null) {
            errorInMain(handler, "No Result");
        }
        else if (result.getError() != null) {
            errorInMain(handler, result);
        }
        else {
            inMain(handler, result.getData());
        }
    }

    public static <T> void errorInMain(@NonNull CompletionHandler<T> handler, @NonNull Throwable t) {
        errorInMain(handler, WebexError.from(t));
    }

    public static <T> void errorInMain(@NonNull CompletionHandler<T> handler, @NonNull String message) {
        errorInMain(handler, WebexError.from(message));
    }

    public static <T1, T2> void errorInMain(@NonNull CompletionHandler<T2> handler, @NonNull Result<T1> result) {
        Queue.main.run(() -> handler.onComplete(ResultImpl.error(result)));
    }

    public static <T> void errorInMain(@NonNull CompletionHandler<T> handler, @NonNull WebexError error) {
        Queue.main.run(() -> handler.onComplete(ResultImpl.error(error)));
    }

    @StringPart
    private T _data;

    @StringPart
    private WebexError _error;

    public ResultImpl(T data, WebexError error) {
        _data = data;
        _error = error;
    }

    public boolean isSuccessful() {
        return _error == null;
    }

    public WebexError getError() {
        return _error;
    }

    public T getData() {
        return _data;
    }

    public String toString() {
        return Objects.toStringByAnnotation(this);
    }

}
