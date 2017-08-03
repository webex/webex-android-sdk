package com.cisco.spark.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 *
 */
public class BaseObservable<T> {

    protected final List<WeakReference<T>> observers;
    private final boolean synchronous;

    public BaseObservable() {
        this(false);
    }

    public BaseObservable(boolean synchronous) {
        this.synchronous = synchronous;
        observers = new ArrayList<>();
    }

    public void addObserver(@Nullable T t) {

        if (t != null) {
            synchronized (observers) {
                observers.add(new WeakReference<>(t));
            }
        }

        pruneDeceased();
    }

    public List<WeakReference<T>> getObserverList() {
        synchronized (observers) {
            return new ArrayList<>(observers);
        }
    }

    public void removeObserver(@Nullable T t) {
        if (t != null) {
            rx.Observable.from(getObserverList()).subscribeOn(Schedulers.computation())
                         .filter(o -> t.equals(o.get()))
                         .subscribe(this::removeObserver, Throwable::printStackTrace, () -> {

                         });
        }
    }

    private void removeObserver(@NonNull WeakReference<T> t) {
        synchronized (observers) {
            observers.remove(t);
        }
    }

    public void notify(Action1<? super T> action) {
        inform(action);
    }

    private void pruneDeceased() {
        rx.Observable.from(observers).subscribeOn(Schedulers.computation()).filter(o -> o.get() == null)
                     .subscribe(this::removeObserver, Throwable::printStackTrace);
    }

    @SuppressWarnings("Convert2MethodRef")
    private rx.Observable<T> getObservers() {
        return rx.Observable.from(getObserverList()).subscribeOn(synchronous ? Schedulers.immediate() : Schedulers.computation())
                            .map(Reference::get)
                            .filter(o -> o != null);
    }

    protected void inform(final Action1<? super T> action) {
        getObservers().subscribe(action, Throwable::printStackTrace);
        pruneDeceased();
    }
}
