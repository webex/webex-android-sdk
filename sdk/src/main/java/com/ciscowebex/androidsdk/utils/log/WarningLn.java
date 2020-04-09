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

package com.ciscowebex.androidsdk.utils.log;

import android.util.Log;

import com.github.benoitdion.ln.BaseLn;

public class WarningLn extends BaseLn {

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
        println(Log.WARN, true, throwable, null);
    }

    @Override
    public void w(Throwable throwable, String message, Object... args) {
        println(Log.WARN, true, throwable, message, args);
    }

    @Override
    public void w(String message, Object... args) {
        println(Log.WARN, true, null, message, args);
    }

    @Override
    public void w(boolean report, Throwable throwable) {
        println(Log.WARN, report, throwable, null);
    }

    @Override
    public void w(boolean report, Throwable throwable, String message, Object... args) {
        println(Log.WARN, report, throwable, message, args);
    }

    @Override
    public void w(boolean report, String message, Object... args) {
        println(Log.WARN, report, null, message, args);
    }

    @Override
    public void e(Throwable throwable) {
        println(Log.ERROR, true, throwable, null);
    }

    @Override
    public void e(Throwable throwable, String message, Object... args) {
        println(Log.ERROR, true, throwable, message, args);
    }

    @Override
    public void e(String message, Object... args) {
        println(Log.ERROR, true, null, message, args);
    }

    @Override
    public void e(boolean report, Throwable t) {
        println(Log.ERROR, report, null, null);
    }

    @Override
    public void e(boolean report, Throwable throwable, String message, Object... args) {
        println(Log.ERROR, report, throwable, message, args);
    }

    @Override
    public void e(boolean report, String message, Object... args) {
        println(Log.ERROR, report, null, message, args);
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
