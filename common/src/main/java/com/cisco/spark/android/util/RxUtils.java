package com.cisco.spark.android.util;

import android.support.v4.util.Pair;

import com.github.benoitdion.ln.Ln;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.functions.Action1;

public class RxUtils {

    public static Action1<Throwable> onError = Ln::e;

    public static <T> Observable.Transformer<T, T> retryOnHttpError(int numAttempts) {
        return observable -> observable.retryWhen(errors -> errors.zipWith(Observable.range(1, numAttempts), Pair::new)
                                                                  .flatMap(pair -> {
                                                                      if (pair.second < numAttempts && pair.first instanceof HttpException) {
                                                                          return Observable.just(null);
                                                                      } else {
                                                                          return Observable.error(pair.first);
                                                                      }
                                                                  }));
    }
}
