package com.cisco.spark.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Patterns;

import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class Strings {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final String SEARCH_MODIFIER_DELIMITER = ":";

    // Modified from Patterns.EMAIL_ADDRESS which as-is does not permit several special but legal characters
    public static final Pattern EMAIL_ADDRESS_LENIENT
            = Pattern.compile(
            "[a-zA-Z0-9\\+\\!\\#\\$\\%\\&\\'\\*\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~\\.]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    );

    public static final Pattern UUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");


    /**
     * Originally from RoboGuice: https://github.com/roboguice/roboguice/blob/master/roboguice/src/main/java/roboguice/util/Strings.java
     * Like join, but allows for a distinct final delimiter.  For english sentences such
     * as "Alice, Bob and Charlie" use ", " and " and " as the delimiters.
     *
     * @param delimiter     usually ", "
     * @param lastDelimiter usually " and "
     * @param objs          the objects
     * @param <T>           the type
     * @return a string
     */
    public static <T> String joinAnd(final String delimiter, final String lastDelimiter, final Collection<T> objs) {
        if (objs == null || objs.isEmpty())
            return "";

        final Iterator<T> iter = objs.iterator();
        final StringBuilder buffer = new StringBuilder(Strings.toString(iter.next()));
        int i = 1;
        while (iter.hasNext()) {
            final T obj = iter.next();
            if (notEmpty(toString(obj)))
                buffer.append(++i == objs.size() ? lastDelimiter : delimiter).append(Strings.toString(obj));
        }
        return buffer.toString();
    }

    public static <T> String joinAnd(final String delimiter, final String lastDelimiter, final T... objs) {
        return joinAnd(delimiter, lastDelimiter, Arrays.asList(objs));
    }

    public static <T> String join(final String delimiter, final Collection<T> objs) {
        if (objs == null || objs.isEmpty())
            return "";

        final Iterator<T> iter = objs.iterator();
        final StringBuilder buffer = new StringBuilder(Strings.toString(iter.next()));

        while (iter.hasNext()) {
            final T obj = iter.next();
            if (notEmpty(toString(obj))) buffer.append(delimiter).append(Strings.toString(obj));
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

    public static int copy(Reader input, Writer output) {
        long count = copyLarge(input, output);
        return count > Integer.MAX_VALUE ? -1 : (int) count;
    }

    public static long copyLarge(Reader input, Writer output) throws RuntimeException {
        try {
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
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

    public static String toString(final Object o) {
        return toString(o, "");
    }

    public static String toString(final Object o, final String def) {
        return o == null ? def :
                o instanceof InputStream ? toString((InputStream) o) :
                        o instanceof Reader ? toString((Reader) o) :
                                o instanceof Object[] ? Strings.join(", ", (Object[]) o) :
                                        o instanceof Collection ? Strings.join(", ", (Collection<?>) o) : o.toString();
    }

    public static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }

    public static boolean notEmpty(final CharSequence s) {
        return !isEmpty(s);
    }

    public static String md5(String s) {
        // http://stackoverflow.com/questions/1057041/difference-between-java-and-php5-md5-hash
        // http://code.google.com/p/roboguice/issues/detail?id=89
        try {

            final byte[] hash = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
            return hexify(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha256(String s) {
        if (TextUtils.isEmpty(s))
            return null;

        try {
            final byte[] hash = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            return hexify(hash);
        } catch (Exception e) {
            Ln.e(e, "Hash failed! Falling back to MD5");
            return md5(s);
        }
    }

    private static String hexify(byte[] hash) {
        final StringBuilder hashString = new StringBuilder();

        for (byte aHash : hash) {
            String hex = Integer.toHexString(aHash);

            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }

        return hashString.toString();
    }

    public static String capitalize(String s) {
        final String c = Strings.toString(s);
        return c.length() >= 2 ? c.substring(0, 1).toUpperCase(Locale.getDefault()) + c.substring(1) : c.length() >= 1 ? c.toUpperCase(Locale.getDefault()) : c;
    }

    public static boolean equals(Object a, Object b) {
        return Strings.toString(a).equals(Strings.toString(b));
    }

    public static boolean equalsIgnoreCase(Object a, Object b) {
        return Strings.toString(a).toLowerCase(Locale.getDefault()).equals(Strings.toString(b).toLowerCase(Locale.getDefault()));
    }

    public static String[] chunk(String str, int chunkSize) {
        if (isEmpty(str) || chunkSize == 0)
            return new String[0];

        final int len = str.length();
        final int arrayLen = ((len - 1) / chunkSize) + 1;
        final String[] array = new String[arrayLen];
        for (int i = 0; i < arrayLen; ++i)
            array[i] = str.substring(i * chunkSize, (i * chunkSize) + chunkSize < len ? (i * chunkSize) + chunkSize : len);

        return array;
    }

    public static String namedFormat(String str, Map<String, String> substitutions) {
        for (String key : substitutions.keySet())
            str = str.replace('$' + key, substitutions.get(key));

        return str;
    }

    public static String namedFormat(String str, Object... nameValuePairs) {
        if (nameValuePairs.length % 2 != 0)
            throw new InvalidParameterException("You must include one value for each parameter");

        final HashMap<String, String> map = new HashMap<String, String>(nameValuePairs.length / 2);
        for (int i = 0; i < nameValuePairs.length; i += 2)
            map.put(Strings.toString(nameValuePairs[i]), Strings.toString(nameValuePairs[i + 1]));

        return namedFormat(str, map);
    }

    @SuppressLint("NewApi")
    public static String escapeHtml(String html) {
        return Html.escapeHtml(html);
    }

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    public static boolean isEmailWorthValidating(String email) {
        if (TextUtils.isEmpty(email))
            return false;
        return email.split("@").length == 2;
    }


    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static CharSequence defaultIfEmpty(CharSequence... strings) {
        // Return the first non-empty string (or the last string if they are all empty.
        for (CharSequence s : strings) {
            if (!isEmpty(s)) {
                return s;
            }
        }
        return strings[strings.length - 1];
    }

    public static boolean isEmailAddress(String s) {
        return isEmailWorthValidating(s);
    }

    public static boolean isPhoneNumber(String s) {
        return s != null && Patterns.PHONE.matcher(s).matches();
    }

    // from guava
    public static String repeat(String string, int count) {
        if (string == null)
            throw new NullPointerException();
        if (count <= 1) {
            if (count < 0)
                throw new IllegalArgumentException("invalid count: " + count);
            return count == 0 ? "" : string;
        } else {
            int len = string.length();
            long longSize = (long) len * (long) count;
            int size = (int) longSize;
            if ((long) size != longSize) {
                throw new ArrayIndexOutOfBoundsException("Required array size too large: " + String.valueOf(longSize));
            } else {
                char[] array = new char[size];
                string.getChars(0, len, array, 0);

                int n;
                for (n = len; n < size - n; n <<= 1) {
                    System.arraycopy(array, 0, array, n, n);
                }

                System.arraycopy(array, 0, array, n, size - n);
                return new String(array);
            }
        }
    }

    // Returns the last path segment from a URL
    public static String getLastBitFromUrl(final String url) {
        return url.replaceFirst(".*/([^/?]+).*", "$1");
    }

    public static Spanned underline(Context context, int stringResource) {
        return Html.fromHtml("<u>" + context.getString(stringResource) + "</u>");
    }

    public static String removeSpacesAroundSearchModifierDelimiter(String string) {
        if (TextUtils.isEmpty(string)) {
            return string;
        }
        return string.replaceFirst("\\s*" + SEARCH_MODIFIER_DELIMITER + "\\s*", SEARCH_MODIFIER_DELIMITER);
    }

    public static int countMatches(String str, String sub) {
        if (isEmpty(str) || isEmpty(sub))
            return 0;
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    // This pattern mirrors the pattern defined by the okhttp3 Headers class
    private static final Pattern pattern = Pattern.compile("[^\\x20-\\x7E]");

    public static String stripInvalidHeaderChars(String str) {
        return pattern.matcher(str).replaceAll("");
    }

    public static String coalesce(String... strings) {
        for (String string : strings) {
            if (string != null)
                return string;
        }
        return null;
    }

    public static boolean isBlank(String string) {
        if (string == null || string.length() == 0)
            return true;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!isWhitespace(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    public static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    public static boolean isUUID(String s) {
        return UUID_PATTERN.matcher(s).matches();
    }

    // A meeting number "123456789" will be formatted to "123 456 789"
    public static String formatMeetingNumber(String meetingNumber) {
        StringBuilder sb = new StringBuilder();
        int meetingNumberLength = meetingNumber.length();

        for (int index = 0; index < meetingNumberLength; index++) {
            int interval = index + 3;
            if (interval >= meetingNumberLength) {
                sb.append(meetingNumber.substring(index, meetingNumberLength)).append(" ");
                break;
            }
            sb.append(meetingNumber.substring(index, interval)).append(" ");
            index += 2;
        }
        return sb.toString();
    }

    public static String makeInClausePlaceholders(int len) {
        if (len > 0) {
            final StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }

        throw new InvalidParameterException("You must pass a positive number.");
    }
}
