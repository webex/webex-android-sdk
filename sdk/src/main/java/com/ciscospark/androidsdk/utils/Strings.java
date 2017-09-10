package com.ciscospark.androidsdk.utils;

import com.ciscospark.androidsdk.utils.collection.Range;

import java.io.File;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Strings {

    private static final int PAD_LIMIT = 8192;

    private static final Pattern WHITESPACE_BLOCK = Pattern.compile("\\s+");

    private static final String FILE_SEPARATOR = File.separator;

    private static final String PATH_SEPARATOR = File.pathSeparator;

    private static final String FILE_SEPARATOR_ALIAS = "/";

    private static final String PATH_SEPARATOR_ALIAS = ":";

    private static final int NORMAL = 0;

    private static final int SEEN_DOLLAR = 1;

    private static final int IN_BRACKET = 2;

    public static String getString(String name, String defaultVal) {
        String value = System.getProperty(name);
        return value == null ? defaultVal : value;
    }

    public static int nthIndexOf(final String string, final String token, final int index) {
        int j = 0;
        for (int i = 0; i < index; i++) {
            j = string.indexOf(token, j + 1);
            if (j == -1) {
                break;
            }
        }
        return j;
    }

    public static int indexOfIgnoreCase(String value, String searchString) {
        return indexOfIgnoreCase(value, searchString, 0);
    }

    public static int indexOfIgnoreCase(String value, String searchString, int start) {
        if (value == null || searchString == null) {
            return -1;
        }
        for (int i = start; i <= value.length() - searchString.length(); i++) {
            boolean match = true;
            for (int j = 0; j < searchString.length(); j++) {
                char c1 = value.charAt(i + j);
                char c2 = searchString.charAt(j);
                if (Character.toUpperCase(c1) != Character.toUpperCase(c2)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return i;
            }
        }
        return -1;
    }

    public static Range rangeOf(final String beginToken, final String endToken, final String string, final int fromIndex) {
        final int begin = string.indexOf(beginToken, fromIndex);
        if (begin != -1) {
            final int end = string.indexOf(endToken, begin + 1);
            if (end != -1) {
                return new Range(begin, end);
            }
        }
        return null;
    }

    public static Range rangeOf(final String beginToken, final String endToken, final String string) {
        return rangeOf(beginToken, endToken, string, 0);
    }

    public static int count(final String string, final String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = string.indexOf(substring, idx)) != -1) {
            idx++;
            count++;
        }
        return count;
    }

    public static int count(final String string, final char c) {
        return count(string, String.valueOf(c));
    }

    public static boolean endsWithIgnoreCase(String lhs, String rhs) {
        if (lhs.length() >= rhs.length()) {
            boolean match = true;
            for (int j = 0; j < rhs.length(); j++) {
                char c1 = lhs.charAt(lhs.length() - rhs.length() + j);
                char c2 = rhs.charAt(j);
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    match = false;
                    break;
                }
            }
            return match;
        }
        return false;
    }

    public static boolean startsWithIgnoreCase(String lhs, String rhs) {
        if (lhs.length() >= rhs.length()) {
            boolean match = true;
            for (int j = 0; j < rhs.length(); j++) {
                char c1 = lhs.charAt(j);
                char c2 = rhs.charAt(j);
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    match = false;
                    break;
                }
            }
            return match;
        }
        return false;
    }

    public static boolean containsWithIgnoreCase(final String base, final String string) {
        return base.toLowerCase().contains(string.toLowerCase());
    }

    public static boolean containsWhitespace(final String str) {
        if (Checker.isEmpty(str)) {
            return false;
        }
        final int strLen = str.length();
        for (int i = 0; i < strLen; ++i) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String format(String message, Object... args) {
        for (int i = 0; i < args.length; i++) {
            Object o = args[i];
            if ((o != null) && (o.getClass().isArray())) {
                args[i] = Arrays.asList((Object[]) o);
            }
        }
        return String.format(message, args);
    }

    public static String format2(String text, Object... params) {
        if (params != null) {
            final Locale l = Locale.getDefault();
            final NumberFormat numberFormat = NumberFormat.getInstance(l);
            numberFormat.setGroupingUsed(false);
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Number) {
                    params[i] = numberFormat.format(params[i]);
                }
            }
            final MessageFormat mf = new MessageFormat(text, l);
            text = mf.format(params, new StringBuffer(), null).toString();
        }
        return text;
    }

    public static String camelToPretty(String value) {
        String result = null;
        if (value != null) {
            StringBuilder builder = new StringBuilder(value.length() + 10);
            char last = '\000';
            for (char ch : value.toCharArray()) {
                if (Character.isUpperCase(ch)) {
                    if ((builder.length() > 0) && (last != 0) && (!Character.isUpperCase(last))) {
                        builder.append(' ');
                    }
                    builder.append(ch);
                }
                else if (Character.isLowerCase(ch)) {
                    if ((last == 0) || (Character.isWhitespace(last))) {
                        builder.append(Character.toUpperCase(ch));
                    }
                    else {
                        builder.append(ch);
                    }
                }
                else {
                    builder.append(ch);
                }
                last = ch;
            }
            result = builder.toString();
        }
        return result;
    }

    public static String camel(String value) {
        String result = null;
        if (value != null) {
            StringBuilder builder = new StringBuilder(value.length());
            boolean nextShouldUpper = false;
            for (char ch : value.toCharArray()) {
                if (builder.length() == 0) {
                    if (Character.isUpperCase(ch)) {
                        ch = Character.toLowerCase(ch);
                    }
                }
                if (ch == '_' || ch == '-' || ch == ' ') {
                    nextShouldUpper = true;
                    continue;
                }
                else if (nextShouldUpper) {
                    if (Character.isLowerCase(ch)) {
                        ch = Character.toUpperCase(ch);
                    }
                    nextShouldUpper = false;
                }
                builder.append(ch);
            }
            result = builder.toString();
        }
        return result;
    }

    public static String capitalize(final String str) {
        return changeFirstCharacterCase(str, true);
    }

    public static String uncapitalize(final String str) {
        return changeFirstCharacterCase(str, false);
    }

    public static String swapCase(String str) {
        if (Checker.isEmpty(str)) {
            return str;
        }
        char[] buffer = str.toCharArray();

        boolean whitespace = true;

        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (Character.isUpperCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
                whitespace = false;
            }
            else if (Character.isTitleCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
                whitespace = false;
            }
            else if (Character.isLowerCase(ch)) {
                if (whitespace) {
                    buffer[i] = Character.toTitleCase(ch);
                    whitespace = false;
                }
                else {
                    buffer[i] = Character.toUpperCase(ch);
                }
            }
            else {
                whitespace = Character.isWhitespace(ch);
            }
        }
        return new String(buffer);
    }

    public static String left(String str, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return Consts.EMPTY;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }

    public static String right(String str, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return Consts.EMPTY;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(str.length() - len);
    }

    public static String mid(String str, int pos, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0 || pos > str.length()) {
            return Consts.EMPTY;
        }
        if (pos < 0) {
            pos = 0;
        }
        if (str.length() <= pos + len) {
            return str.substring(pos);
        }
        return str.substring(pos, pos + len);
    }

    public static String leftBefore(String str, String separator) {
        if (Checker.isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.length() == 0) {
            return Consts.EMPTY;
        }
        int pos = str.indexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    public static String leftBeforeLast(String str, String separator) {
        if (Checker.isEmpty(str) || Checker.isEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return Consts.EMPTY;
        }
        return str.substring(0, pos);
    }

    public static String rightAfter(String str, String separator) {
        if (Checker.isEmpty(str) || Checker.isEmpty(separator)) {
            return str;
        }
        int pos = str.indexOf(separator);
        if (pos == -1) {
            return Consts.EMPTY;
        }
        return str.substring(pos + separator.length());
    }

    public static String rightAfterLast(String str, String separator) {
        if (Checker.isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.length() == 0) {
            return Consts.EMPTY;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1 || pos == str.length() - separator.length()) {
            return Consts.EMPTY;
        }
        return str.substring(pos + separator.length());
    }

    public static String midBetween(String str, String open, String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        int start = str.indexOf(open);
        if (start != -1) {
            int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    public static String midBetween(String str, String open, int count, String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        int start = -1;
        for (int i =0; i < count; i++) {
            start = str.indexOf(open, start+1);
            if (start == -1) {
                break;
            }
        }
        if (start != -1) {
            int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    public static List<String> split(String str, String separatorChars, boolean wholeSeparator) {
        if (wholeSeparator) {
            return splitByWholeSeparatorWorker(str, separatorChars, -1, false);
        }
        else {
            return splitWorker(str, separatorChars, -1, false);
        }
    }

    public static String[] trimAll(String... strs) {
        return trimAll(strs, null);
    }

    public static String[] trimAll(String[] strs, String stripChars) {
        int strsLen;
        if (strs == null || (strsLen = strs.length) == 0) {
            return strs;
        }
        String[] newArr = new String[strsLen];
        for (int i = 0; i < strsLen; i++) {
            newArr[i] = trim(strs[i], stripChars);
        }
        return newArr;
    }

    public static String trim(String str) {
        return trim(str, null);
    }

    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        str = trim(str, null);
        return str.length() == 0 ? null : str;
    }

    public static String trimToEmpty(String str) {
        return str == null ? Consts.EMPTY : trim(str, null);
    }

    public static String trim(String str, String stripChars) {
        if (Checker.isEmpty(str)) {
            return str;
        }
        str = trimLeading(str, stripChars);
        return trimTrailing(str, stripChars);
    }

    public static String trimLeading(String str, String stripChars) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        if (stripChars == null) {
            while (start != strLen && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        }
        else if (stripChars.length() == 0) {
            return str;
        }
        else {
            while (start != strLen && stripChars.indexOf(str.charAt(start)) != -1) {
                start++;
            }
        }
        return str.substring(start);
    }

    public static String trimTrailing(String str, String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        }
        else if (stripChars.length() == 0) {
            return str;
        }
        else {
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != -1) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    public static String deleteWhitespace(String str) {
        if (Checker.isEmpty(str)) {
            return str;
        }
        int sz = str.length();
        char[] chs = new char[sz];
        int count = 0;
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                chs[count++] = str.charAt(i);
            }
        }
        if (count == sz) {
            return str;
        }
        return new String(chs, 0, count);
    }

    public static String deleteStart(String str, String remove, boolean ignoreCase) {
        if (Checker.isEmpty(str) || Checker.isEmpty(remove)) {
            return str;
        }
        if ((ignoreCase && startsWithIgnoreCase(str, remove))
                || (!ignoreCase && str.startsWith(remove))) {
            return str.substring(remove.length());
        }
        return str;
    }

    public static String deleteEnd(String str, String remove, boolean ignoreCase) {
        if (Checker.isEmpty(str) || Checker.isEmpty(remove)) {
            return str;
        }
        if ((ignoreCase && endsWithIgnoreCase(str, remove))
                || (!ignoreCase && str.endsWith(remove))) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    public static String delete(String str, char remove) {
        if (Checker.isEmpty(str) || str.indexOf(remove) == -1) {
            return str;
        }
        char[] chars = str.toCharArray();
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != remove) {
                chars[pos++] = chars[i];
            }
        }
        return new String(chars, 0, pos);
    }

    public static String delete(String str, String remove) {
        if (Checker.isEmpty(str) || Checker.isEmpty(remove)) {
            return str;
        }
        return replace(str, remove, Consts.EMPTY, -1);
    }

    public static String replaceOnce(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, 1);
    }

    public static String replace(String text, String searchString, String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    public static String replace(String text, String searchString, String replacement, int max) {
        if (Checker.isEmpty(text) || Checker.isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == -1) {
            return text;
        }
        int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != -1) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static String replaceEach(String text, String[] searchList, String[] replacementList) {
        return replaceEach(text, searchList, replacementList, false, 0);
    }

    public static String replaceEachRepeatedly(String text, String[] searchList, String[] replacementList) {
        int timeToLive = searchList == null ? 0 : searchList.length;
        return replaceEach(text, searchList, replacementList, true, timeToLive);
    }

    public static String repeat(String str, int repeat) {
        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return Consts.EMPTY;
        }
        int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return repeat(str.charAt(0), repeat);
        }

        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1:
                return repeat(str.charAt(0), repeat);
            case 2:
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default:
                StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    public static String repeat(String str, String separator, int repeat) {
        if (str == null || separator == null) {
            return repeat(str, repeat);
        }
        else {
            String result = repeat(str + separator, repeat);
            return deleteEnd(result, separator, false);
        }
    }

    public static String repeat(char ch, int repeat) {
        char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    public static String padRight(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str;
        }
        if (pads > PAD_LIMIT) {
            return padRight(str, size, String.valueOf(padChar));
        }
        return str.concat(repeat(padChar, pads));
    }

    public static String padRight(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (Checker.isEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return padRight(str, size, padStr.charAt(0));
        }

        if (pads == padLen) {
            return str.concat(padStr);
        }
        else if (pads < padLen) {
            return str.concat(padStr.substring(0, pads));
        }
        else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return str.concat(new String(padding));
        }
    }

    public static String padLeft(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str;
        }
        if (pads > PAD_LIMIT) {
            return padLeft(str, size, String.valueOf(padChar));
        }
        return repeat(padChar, pads).concat(str);
    }

    public static String padLeft(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (Checker.isEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return padLeft(str, size, padStr.charAt(0));
        }

        if (pads == padLen) {
            return padStr.concat(str);
        }
        else if (pads < padLen) {
            return padStr.substring(0, pads).concat(str);
        }
        else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding).concat(str);
        }
    }

    public static String padCenter(String str, int size, char padChar) {
        if (str == null || size <= 0) {
            return str;
        }
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        str = padLeft(str, strLen + pads / 2, padChar);
        str = padRight(str, size, padChar);
        return str;
    }

    public static String padCenter(String str, int size, String padStr) {
        if (str == null || size <= 0) {
            return str;
        }
        if (Checker.isEmpty(padStr)) {
            padStr = " ";
        }
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        str = padLeft(str, strLen + pads / 2, padStr);
        str = padRight(str, size, padStr);
        return str;
    }

    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    public static String quote(final String str) {
        return str != null ? "'" + str + "'" : null;
    }

    public static String replaceProperties(final String string) {
        return replaceProperties(string, null);
    }

    public static String replaceProperties(final String string, final Map<Object, Object> props) {
        if (string == null) {
            return string;
        }
        final char[] chars = string.toCharArray();
        final StringBuilder buffer = new StringBuilder();
        boolean found = false;
        int state = NORMAL;
        int start = 0;
        for (int i = 0; i < chars.length; ++i) {
            final char c = chars[i];
            if (c == '$' && state != IN_BRACKET) {
                state = SEEN_DOLLAR;
            }
            else if (c == '{' && state == SEEN_DOLLAR) {
                buffer.append(string.substring(start, i - 1));
                state = IN_BRACKET;
                start = i - 1;
            }
            else if (state == SEEN_DOLLAR) {
                state = NORMAL;
            }
            else if (c == '}' && state == IN_BRACKET) {
                if (start + 2 == i) {
                    buffer.append("${}");
                }
                else {
                    // ${key} || ${key1,key2} || ${key:default}
                    String value = null;
                    String defaultValue = null;
                    String keyPart = string.substring(start + 2, i);
                    final int colon = keyPart.indexOf(':');
                    if (colon > 0) {
                        defaultValue = keyPart.substring(colon + 1);
                        keyPart = keyPart.substring(0, colon);
                    }

                    String[] keys = keyPart.split(",");
                    for (String key : keys) {
                        if (!Checker.isEmpty(key)) {
                            if (FILE_SEPARATOR_ALIAS.equals(key)) {
                                value = FILE_SEPARATOR;
                            }
                            else if (PATH_SEPARATOR_ALIAS.equals(key)) {
                                value = PATH_SEPARATOR;
                            }
                            else {
                                if (props == null) {
                                    value = System.getProperty(key);
                                }
                                else if (props instanceof Properties) {
                                    value = ((Properties) props).getProperty(key);
                                }
                                else {
                                    Object o = props.get(key);
                                    if (o instanceof Callable) {
                                        try {
                                            o = ((Callable) o).call();
                                        }
                                        catch (final Exception ignored) {
                                        }
                                    }
                                    if (o != null) {
                                        value = String.valueOf(o);
                                    }
                                }
                            }
                        }
                        if (value != null) {
                            break;
                        }
                    }
                    if (value == null) {
                        value = defaultValue;
                    }
                    if (value != null) {
                        found = true;
                        buffer.append(value);
                    }
                }
                start = i + 1;
                state = NORMAL;
            }
        }
        // No properties
        if (!found) {
            return string;
        }
        // Collect the trailing characters
        if (start != chars.length) {
            buffer.append(string.substring(start, chars.length));
        }
        return buffer.toString();
    }

    public static String wrap(String str, int wrapLength) {
        return wrap(str, wrapLength, null, false);
    }

    public static String wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
        if (str == null) {
            return null;
        }
        if (newLineStr == null) {
            newLineStr = "\n";
        }
        if (wrapLength < 1) {
            wrapLength = 1;
        }
        int inputLineLength = str.length();
        int offset = 0;
        StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);

        while (inputLineLength - offset > wrapLength) {
            if (str.charAt(offset) == ' ') {
                offset++;
                continue;
            }
            int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);

            if (spaceToWrapAt >= offset) {
                wrappedLine.append(str.substring(offset, spaceToWrapAt));
                wrappedLine.append(newLineStr);
                offset = spaceToWrapAt + 1;

            }
            else {
                if (wrapLongWords) {
                    wrappedLine.append(str.substring(offset, wrapLength + offset));
                    wrappedLine.append(newLineStr);
                    offset += wrapLength;
                }
                else {
                    spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
                    if (spaceToWrapAt >= 0) {
                        wrappedLine.append(str.substring(offset, spaceToWrapAt));
                        wrappedLine.append(newLineStr);
                        offset = spaceToWrapAt + 1;
                    }
                    else {
                        wrappedLine.append(str.substring(offset));
                        offset = inputLineLength;
                    }
                }
            }
        }
        wrappedLine.append(str.substring(offset));
        return wrappedLine.toString();
    }

    public static boolean match(String str, String pattern) {
        String regex = ("\\Q" + pattern + "\\E").replace("*", "\\E.*\\Q");
        return str.matches(regex);
    }

    private static String changeFirstCharacterCase(final String str, final boolean capitalize) {
        if (str == null || str.length() == 0) {
            return str;
        }
        final StringBuilder buf = new StringBuilder(str.length());
        if (capitalize) {
            buf.append(Character.toUpperCase(str.charAt(0)));
        }
        else {
            buf.append(Character.toLowerCase(str.charAt(0)));
        }
        buf.append(str.substring(1));
        return buf.toString();
    }

    private static List<String> splitByWholeSeparatorWorker(String str, String separator, int max, boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return Collections.emptyList();
        }
        if (separator == null || Consts.EMPTY.equals(separator)) {
            return splitWorker(str, null, max, preserveAllTokens);
        }
        int separatorLength = separator.length();
        ArrayList<String> substrings = new ArrayList<String>();
        int numberOfSubstrings = 0;
        int beg = 0;
        int end = 0;
        while (end < len) {
            end = str.indexOf(separator, beg);
            if (end > -1) {
                if (end > beg) {
                    numberOfSubstrings += 1;
                    if (numberOfSubstrings == max) {
                        end = len;
                        substrings.add(str.substring(beg));
                    }
                    else {
                        substrings.add(str.substring(beg, end));
                        beg = end + separatorLength;
                    }
                }
                else {
                    if (preserveAllTokens) {
                        numberOfSubstrings += 1;
                        if (numberOfSubstrings == max) {
                            end = len;
                            substrings.add(str.substring(beg));
                        }
                        else {
                            substrings.add(Consts.EMPTY);
                        }
                    }
                    beg = end + separatorLength;
                }
            }
            else {
                substrings.add(str.substring(beg));
                end = len;
            }
        }
        return substrings;
    }

    private static List<String> splitWorker(String str, String separatorChars, int max, boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<String>();
        int sizePlus1 = 1;
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null) {
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        else if (separatorChars.length() == 1) {
            char sep = separatorChars.charAt(0);
            while (i < len) {
                if (str.charAt(i) == sep) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        else {
            while (i < len) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        if (match || preserveAllTokens && lastMatch) {
            list.add(str.substring(start, i));
        }
        return list;
    }

    private static String replaceEach(String text, String[] searchList, String[] replacementList, boolean repeat, int timeToLive) {
        if (text == null || text.length() == 0 || searchList == null
                || searchList.length == 0 || replacementList == null || replacementList.length == 0) {
            return text;
        }
        if (timeToLive < 0) {
            throw new IllegalStateException("Aborting to protect against StackOverflowError - " +
                    "output of one loop is the input of another");
        }
        int searchLength = searchList.length;
        int replacementLength = replacementList.length;
        if (searchLength != replacementLength) {
            throw new IllegalArgumentException("Search and Replace array lengths don't match: "
                    + searchLength
                    + " vs "
                    + replacementLength);
        }
        boolean[] noMoreMatchesForReplIndex = new boolean[searchLength];
        int textIndex = -1;
        int replaceIndex = -1;
        int tempIndex = -1;

        for (int i = 0; i < searchLength; i++) {
            if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                    searchList[i].length() == 0 || replacementList[i] == null) {
                continue;
            }
            tempIndex = text.indexOf(searchList[i]);

            if (tempIndex == -1) {
                noMoreMatchesForReplIndex[i] = true;
            }
            else {
                if (textIndex == -1 || tempIndex < textIndex) {
                    textIndex = tempIndex;
                    replaceIndex = i;
                }
            }
        }
        if (textIndex == -1) {
            return text;
        }
        int start = 0;
        int increase = 0;
        for (int i = 0; i < searchList.length; i++) {
            if (searchList[i] == null || replacementList[i] == null) {
                continue;
            }
            int greater = replacementList[i].length() - searchList[i].length();
            if (greater > 0) {
                increase += 3 * greater; // assume 3 matches
            }
        }
        increase = Math.min(increase, text.length() / 5);
        StringBuilder buf = new StringBuilder(text.length() + increase);
        while (textIndex != -1) {
            for (int i = start; i < textIndex; i++) {
                buf.append(text.charAt(i));
            }
            buf.append(replacementList[replaceIndex]);
            start = textIndex + searchList[replaceIndex].length();
            textIndex = -1;
            replaceIndex = -1;
            tempIndex = -1;
            for (int i = 0; i < searchLength; i++) {
                if (noMoreMatchesForReplIndex[i] || searchList[i] == null ||
                        searchList[i].length() == 0 || replacementList[i] == null) {
                    continue;
                }
                tempIndex = text.indexOf(searchList[i], start);
                if (tempIndex == -1) {
                    noMoreMatchesForReplIndex[i] = true;
                }
                else {
                    if (textIndex == -1 || tempIndex < textIndex) {
                        textIndex = tempIndex;
                        replaceIndex = i;
                    }
                }
            }
        }
        int textLength = text.length();
        for (int i = start; i < textLength; i++) {
            buf.append(text.charAt(i));
        }
        String result = buf.toString();
        if (!repeat) {
            return result;
        }
        return replaceEach(result, searchList, replacementList, repeat, timeToLive - 1);
    }

}
