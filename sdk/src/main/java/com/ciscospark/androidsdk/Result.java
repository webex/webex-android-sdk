package com.ciscospark.androidsdk;

import com.ciscospark.androidsdk.utils.Objects;
import com.ciscospark.androidsdk.utils.annotation.StringPart;

import java.io.IOException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Result<T> {

    public static <T> Result<T> success(T data) {
        return new Result<T>(data, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>(null, new SparkError<SparkError.ErrorCode>(SparkError.ErrorCode.UNEXPECTED_ERROR, message));
    }

    public static <T> Result<T> error(Throwable t) {
        return new Result<T>(null, makeError(t));
    }

    public static <T> Result<T> error(SparkError error) {
        return new Result<T>(null, error);
    }

    public static <T> Result<T> error(Response response) {
        return new Result<T>(null, makeError(response));
    }

    @StringPart
    private T _data;

    @StringPart
    private SparkError _error;

    public Result(T data, SparkError error) {
        _data = data;
        _error = error;
    }

    public boolean isSuccessful() {
        return _error == null;
    }

    public SparkError getError() {
        return _error;
    }

    public T getData() {
        return _data;
    }

    public String toString() {
        return Objects.toStringByAnnotation(this);
    }

    private static SparkError<SparkError.ErrorCode> makeError(Throwable t) {
        return new SparkError<SparkError.ErrorCode>(SparkError.ErrorCode.UNEXPECTED_ERROR, t.toString());
    }

    private static SparkError<SparkError.ErrorCode> makeError(Response res) {
        StringBuilder message = new StringBuilder().append(res.code()).append("/").append(res.message());
        try {
            String body = res.errorBody().string();
            message.append("/").append(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new SparkError<>(SparkError.ErrorCode.SERVICE_ERROR, message.toString());
    }

}
