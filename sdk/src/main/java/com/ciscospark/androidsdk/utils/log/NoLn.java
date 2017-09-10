package com.ciscospark.androidsdk.utils.log;

import com.github.benoitdion.ln.BaseLn;

/**
 * Created by zhiyuliu on 03/09/2017.
 */

public class NoLn extends BaseLn {

    @Override
    public void v(Throwable throwable) {
        clearExtra();
    }

    @Override
    public void v(String message, Object... args) {
        clearExtra();
    }

    @Override
    public void v(Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void d(Throwable throwable) {
        clearExtra();
    }

    @Override
    public void d(String message, Object... args) {
        clearExtra();
    }

    @Override
    public void d(Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void i(Throwable throwable) {
        clearExtra();
    }

    @Override
    public void i(Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void i(String message, Object... args) {
        clearExtra();
    }

    @Override
    public void w(Throwable throwable) {
        clearExtra();
    }

    @Override
    public void w(Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void w(String message, Object... args) {
        clearExtra();
    }

    @Override
    public void w(boolean report, Throwable throwable) {
        clearExtra();
    }

    @Override
    public void w(boolean report, Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void w(boolean report, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void e(Throwable t) {
        clearExtra();
    }

    @Override
    public void e(Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void e(String message, Object... args) {
        clearExtra();
    }

    @Override
    public void e(boolean report, Throwable throwable) {
        clearExtra();
    }

    @Override
    public void e(boolean report, Throwable throwable, String message, Object... args) {
        clearExtra();
    }

    @Override
    public void e(boolean report, String message, Object... args) {
        clearExtra();
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isVerboseEnabled() {
        return false;
    }
}
