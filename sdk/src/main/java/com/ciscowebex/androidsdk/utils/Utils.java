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

package com.ciscowebex.androidsdk.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

import android.view.WindowManager;
import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.utils.log.DebugLn;
import com.ciscowebex.androidsdk.utils.log.NoLn;
import com.ciscowebex.androidsdk.utils.log.WarningLn;
import com.github.benoitdion.ln.InfoLn;
import com.github.benoitdion.ln.NaturalLog;
import com.github.benoitdion.ln.ReleaseLn;
import com.webex.wme.MediaSessionAPI;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;
import me.helloworld.utils.Strings;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * An utility class.
 *
 * @since 0.1
 */
public class Utils {

    private Utils(){}

    public static <T> T checkNotNull(@Nullable T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }

    public static Map<String, Object> toMap(Object o) {
        Map<String, Object> result = new HashMap<>();
        if (o != null) {
            try {
                Field[] declaredFields = o.getClass().getDeclaredFields();
                for (Field field : declaredFields) {
                    String name = field.getName();
                    Object value = "@ERROR";
                    boolean accessible = field.isAccessible();
                    try {
                        field.setAccessible(true);
                        value = field.get(o);
                    } catch (Throwable ignored) {
                    } finally {
                        try {
                            field.setAccessible(accessible);
                        } catch (Throwable ignored1) {
                        }
                    }
                    result.put(name, value);
                }
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return result;
    }

    public static <T> String join(final String delimiter, final Collection<T> objs) {
        if (objs == null || objs.isEmpty()) {
            return "";
        }
        final Iterator<T> iter = objs.iterator();
        final StringBuilder buffer = new StringBuilder(toString(iter.next()));

        while (iter.hasNext()) {
            final T obj = iter.next();
            if (!Checker.isEmpty(toString(obj))) buffer.append(delimiter).append(toString(obj));
        }
        return buffer.toString();
    }

    public static <T> String join(final String delimiter, final T... objects) {
        return join(delimiter, Arrays.asList(objects));
    }

    public static String toString(InputStream input) {
        StringWriter sw = new StringWriter();
        copy(new InputStreamReader(input), sw);
        return sw.toString();
    }

    public static String toString(Reader input) {
        StringWriter sw = new StringWriter();
        copy(input, sw);
        return sw.toString();
    }

    public static String toString(final Object o) {
        return toString(o, "");
    }

    public static String toString(final Object o, final String def) {
        return o == null ? def :
                o instanceof InputStream ? toString((InputStream) o) :
                        o instanceof Reader ? toString((Reader) o) :
                                o instanceof Object[] ? join(", ", (Object[]) o) :
                                        o instanceof Collection ? join(", ", (Collection<?>) o) : o.toString();
    }

    public static int copy(Reader input, Writer output) {
        long count = copyLarge(input, output);
        return count > Integer.MAX_VALUE ? -1 : (int) count;
    }

    public static long copyLarge(Reader input, Writer output) throws RuntimeException {
        try {
            char[] buffer = new char[1024 * 4];
            long count = 0;
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] chunk(String str, int chunkSize) {
        if (Checker.isEmpty(str) || chunkSize == 0) {
            return new String[0];
        }
        // return str.split("(?<=\\G.{" + chunkSize + "})");
        final int len = str.length();
        final int arrayLen = ((len - 1) / chunkSize) + 1;
        final String[] array = new String[arrayLen];
        for (int i = 0; i < arrayLen; ++i) {
            array[i] = str.substring(i * chunkSize, Math.min((i * chunkSize) + chunkSize, len));
        }
        return array;
    }

    public static int getScreenRotation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return windowManager != null ? windowManager.getDefaultDisplay().getRotation() : 0;
    }

    public static <T> T getFirst(T[] array) {
        if (array.length == 0) {
            return null;
        }
        return array[0];
    }

    public static HttpLoggingInterceptor.Level toHttpLogLevel(Webex.LogLevel logLevel) {
        if (logLevel == Webex.LogLevel.NO) {
            return HttpLoggingInterceptor.Level.NONE;
        }
        else if (logLevel == Webex.LogLevel.ERROR || logLevel == Webex.LogLevel.WARNING || logLevel == Webex.LogLevel.INFO) {
            return HttpLoggingInterceptor.Level.BASIC;
        }
        else if (logLevel == Webex.LogLevel.DEBUG_NO_HTTP_DETAILS) {
            return HttpLoggingInterceptor.Level.HEADERS;
        }
        else {
            return HttpLoggingInterceptor.Level.BODY;
        }
     }

    public static NaturalLog toLnLog(Webex.LogLevel logLevel) {
        NaturalLog logger = new DebugLn();
        if (logLevel != null) {
            switch (logLevel) {
                case NO:
                    logger = new NoLn();
                    break;
                case ERROR:
                    logger = new ReleaseLn();
                    break;
                case WARNING:
                    logger = new WarningLn();
                    break;
                case INFO:
                    logger = new InfoLn();
                    break;
                case DEBUG_NO_HTTP_DETAILS:
                case DEBUG:
                case VERBOSE:
                case ALL:
                    logger = new DebugLn();
                    break;
            }
        }
        return logger;
    }

    public static MediaSessionAPI.TraceLevelMask toTraceLevelMask(Webex.LogLevel level) {
        MediaSessionAPI.TraceLevelMask mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_WARNING;
        if (level != null) {
            switch (level) {
                case NO:
                case ERROR:
                case WARNING:
                case INFO:
                case DEBUG_NO_HTTP_DETAILS:
                case DEBUG:
                    mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_NOTRACE;
                    break;
                case VERBOSE:
                    mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_INFO;
                    break;
                case ALL:
                    mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DETAIL;
            }
        }
        return mask;
    }

    public static String readFile(File file) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            Ln.e(e, "Error reading file: " + file.getAbsolutePath());
            return null;
        }
    }

    public static File mkdir(String dirName) {
        File logDir = new File(dirName);
        if (!logDir.exists()) {
            if (!logDir.mkdirs())
                Ln.e("Failed to make directory");
        }
        return logDir;
    }

}
