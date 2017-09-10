package com.ciscospark.androidsdk.utils;

import android.util.Base64;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Converter {

    public static String DEFFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static long toLong(Object o, long defaultValue) {
        Long ret = toLong(o);
        return ret == null ? defaultValue : ret;
    }

    public static Long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        else if (o instanceof Date) {
            return ((Date) o).getTime();
        }
        else if (o instanceof Calendar) {
            return ((Calendar) o).getTimeInMillis();
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return Long.parseLong(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static long toLong(byte[] b, int off, int len) throws NumberFormatException {
        int c;
        if (b == null || len <= 0 || !Chars.isDigit(c = b[off++])) {
            throw new NumberFormatException();
        }
        long n = c - '0';
        long m;
        while (--len > 0) {
            if (!Chars.isDigit(c = b[off++])) {
                throw new NumberFormatException();
            }
            m = n * 10 + c - '0';
            if (m < n) {
                throw new NumberFormatException();
            }
            else {
                n = m;
            }
        }
        return n;
    }

    public static long toLong(char[] b, int off, int len) throws NumberFormatException {
        int c;
        if (b == null || len <= 0 || !Chars.isDigit(c = b[off++])) {
            throw new NumberFormatException();
        }
        long n = c - '0';
        long m;
        while (--len > 0) {
            if (!Chars.isDigit(c = b[off++])) {
                throw new NumberFormatException();
            }
            m = n * 10 + c - '0';
            if (m < n) {
                throw new NumberFormatException();
            }
            else {
                n = m;
            }
        }
        return n;
    }

    public static int toInt(Object o, int defaultValue) {
        Integer ret = toInt(o);
        return ret == null ? defaultValue : ret;
    }

    public static Integer toInt(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static int toInt(byte[] b, int off, int len) throws NumberFormatException {
        int c;
        if (b == null || len <= 0 || !Chars.isDigit(c = b[off++])) {
            throw new NumberFormatException();
        }
        int n = c - '0';
        while (--len > 0) {
            if (!Chars.isDigit(c = b[off++])) {
                throw new NumberFormatException();
            }
            n = n * 10 + c - '0';
        }
        return n;
    }

    public static int toInt(char[] b, int off, int len) throws NumberFormatException {
        int c;
        if (b == null || len <= 0 || !Chars.isDigit(c = b[off++])) {
            throw new NumberFormatException();
        }
        int n = c - '0';
        while (--len > 0) {
            if (!Chars.isDigit(c = b[off++])) {
                throw new NumberFormatException();
            }
            n = n * 10 + c - '0';
        }
        return n;
    }

    public static float toFloat(Object o, float defaultValue) {
        Float ret = toFloat(o);
        return ret == null ? defaultValue : ret;
    }

    public static Float toFloat(Object o) {
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return Float.parseFloat(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static Double toDouble(Object o, double defaultValue) {
        Double ret = toDouble(o);
        return ret == null ? defaultValue : ret;
    }

    public static Double toDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static boolean toBoolean(final Object o, boolean def) {
        Boolean ret = toBoolean(o);
        return ret == null ? def : ret;
    }

    public static Boolean toBoolean(final Object o) {
        String str = Objects.toString(o);
        if (Checker.isEmpty(str)) {
            return null;
        }
        if (str == "true") {
            return true;
        }
        switch (str.length()) {
            case 1: {
                char ch0 = str.charAt(0);
                if (ch0 == 'y' || ch0 == 'Y' ||
                        ch0 == 't' || ch0 == 'T' ||
                        ch0 == '1') {
                    return true;
                }
                if (ch0 == 'n' || ch0 == 'N' ||
                        ch0 == 'f' || ch0 == 'F') {
                    return false;
                }
                break;
            }
            case 2: {
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                if ((ch0 == 'o' || ch0 == 'O') &&
                        (ch1 == 'n' || ch1 == 'N')) {
                    return true;
                }
                if ((ch0 == 'n' || ch0 == 'N') &&
                        (ch1 == 'o' || ch1 == 'O')) {
                    return false;
                }
                break;
            }
            case 3: {
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char ch2 = str.charAt(2);
                if ((ch0 == 'y' || ch0 == 'Y') &&
                        (ch1 == 'e' || ch1 == 'E') &&
                        (ch2 == 's' || ch2 == 'S')) {
                    return true;
                }
                if ((ch0 == 'o' || ch0 == 'O') &&
                        (ch1 == 'f' || ch1 == 'F') &&
                        (ch2 == 'f' || ch2 == 'F')) {
                    return false;
                }
                break;
            }
            case 4: {
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char ch2 = str.charAt(2);
                char ch3 = str.charAt(3);
                if ((ch0 == 't' || ch0 == 'T') &&
                        (ch1 == 'r' || ch1 == 'R') &&
                        (ch2 == 'u' || ch2 == 'U') &&
                        (ch3 == 'e' || ch3 == 'E')) {
                    return true;
                }
                break;
            }
            case 5: {
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char ch2 = str.charAt(2);
                char ch3 = str.charAt(3);
                char ch4 = str.charAt(4);
                if ((ch0 == 'f' || ch0 == 'F') &&
                        (ch1 == 'a' || ch1 == 'A') &&
                        (ch2 == 'l' || ch2 == 'L') &&
                        (ch3 == 's' || ch3 == 'S') &&
                        (ch4 == 'e' || ch4 == 'E')) {
                    return false;
                }
                break;
            }
        }
        return null;
    }

    public static String toString(byte[] bytes, String encoding) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, encoding);
        }
        catch (Throwable e) {
            return null;
        }
    }

    public static byte[] toBytes(String string, String encoding) {
        if (string == null) {
            return null;
        }
        try {
            return string.getBytes(encoding);
        }
        catch (Throwable e) {
            return null;
        }
    }

    public static byte[] toBytes(Object o) {
        if (o instanceof byte[]) {
            return (byte[]) o;
        }
        return Base64.decode(Objects.toString(o), Base64.URL_SAFE);
    }

    public static byte[] toBytes(short s, final boolean asc) {
        final byte[] buf = new byte[2];
        if (asc) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        }
        else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00ff);
                s >>= 8;
            }
        }
        return buf;
    }

    public static byte[] toBytes(int s, final boolean asc) {
        final byte[] buf = new byte[4];
        if (asc) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x000000ff);
                s >>= 8;
            }
        }
        else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x000000ff);
                s >>= 8;
            }
        }
        return buf;
    }

    public static byte[] toBytes(long s, final boolean asc) {
        final byte[] buf = new byte[8];
        if (asc) {
            for (int i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 0x00000000000000ff);
                s >>= 8;
            }
        }
        else {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 0x00000000000000ff);
                s >>= 8;
            }
        }
        return buf;
    }

    public static byte toByte(Object o, byte def) {
        Byte ret = toByte(o);
        return ret == null ? def : ret;
    }

    public static Byte toByte(Object o) {
        if (o instanceof Number) {
            return ((Number) o).byteValue();
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return Byte.parseByte(s);
        }
        catch (Throwable t) {
            return null;
        }
    }
    public static short toShort(Object o, short def) {
        Short ret = toShort(o);
        return ret == null ? def : ret;
    }

    public static Short toShort(Object o) {
        if (o instanceof Number) {
            return ((Number) o).shortValue();
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return Short.parseShort(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static char toChar(Object o, char def) {
        Character ret = toChar(o);
        return ret == null ? def : ret;
    }

    public static Character toChar(Object o) {
        if (o instanceof Character) {
            return (Character) o;
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s) || s.length() != 1) {
            return null;
        }
        return s.charAt(0);
    }

    public static BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof BigInteger) {
            return new BigDecimal((BigInteger) o);
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return new BigDecimal(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static BigInteger toBigInteger(Object o) {
        if (o instanceof BigInteger) {
            return (BigInteger) o;
        }
        if (o instanceof Float || o instanceof Double) {
            return BigInteger.valueOf(((Number) o).longValue());
        }
        String s = Objects.toString(o);
        if (Checker.isEmpty(s)) {
            return null;
        }
        try {
            return new BigInteger(s);
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static Date toDate(Object value, Date def) {
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            try {
                return new Date(((Number) value).longValue());
            }
            catch (Throwable t) {
                return def;
            }
        }
        String strVal = Objects.toString(value);
        if (Checker.isEmpty(strVal)) {
            return def;
        }
        if (strVal.indexOf('-') != -1) {
            String format = null;
            if (strVal.length() == DEFFAULT_DATE_FORMAT.length()) {
                format = DEFFAULT_DATE_FORMAT;
            }
            else if (strVal.length() == 10) {
                format = "yyyy-MM-dd";
            }
            else {
                format = "yyyy-MM-dd HH:mm:ss.SSS";
            }
            try {
                return new SimpleDateFormat(format).parse(strVal);
            }
            catch (ParseException e) {
                return def;
            }
        }
        Long longVal = toLong(strVal);
        if (longVal == null) {
            return def;
        }
        try {
            return new Date(longVal);
        }
        catch (Throwable t) {
            return def;
        }
    }

    public static java.sql.Date toSQLDate(Object value, java.sql.Date def) {
        if (value instanceof Calendar) {
            return new java.sql.Date(((Calendar) value).getTimeInMillis());
        }
        if (value instanceof java.sql.Date) {
            return (java.sql.Date) value;
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime());
        }
        if (value instanceof Number) {
            try {
                return new java.sql.Date(((Number) value).longValue());
            }
            catch (Throwable t) {
                return def;
            }
        }
        String strVal = Objects.toString(value);
        if (Checker.isEmpty(strVal)) {
            return def;
        }
        try {
            return java.sql.Date.valueOf(strVal);
        }
        catch (Throwable t) {
            if (strVal.indexOf('-') != -1) {
                String format = null;
                if (strVal.length() == DEFFAULT_DATE_FORMAT.length()) {
                    format = DEFFAULT_DATE_FORMAT;
                }
                else if (strVal.length() == 10) {
                    format = "yyyy-MM-dd";
                }
                else {
                    format = "yyyy-MM-dd HH:mm:ss.SSS";
                }
                try {
                    return new java.sql.Date(new SimpleDateFormat(format).parse(strVal).getTime());
                }
                catch (ParseException e) {
                    return def;
                }
            }
            Long longVal = toLong(strVal);
            if (longVal == null) {
                return def;
            }
            try {
                return new java.sql.Date(longVal);
            }
            catch (Throwable t1) {
                return def;
            }
        }
    }

    public static java.sql.Timestamp toTimestamp(Object value, java.sql.Timestamp def) {
        if (value instanceof Calendar) {
            return new java.sql.Timestamp(((Calendar) value).getTimeInMillis());
        }
        if (value instanceof java.sql.Timestamp) {
            return (java.sql.Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) value).getTime());
        }
        if (value instanceof Number) {
            try {
                return new java.sql.Timestamp(((Number) value).longValue());
            }
            catch (Throwable t) {
                return def;
            }
        }
        String strVal = Objects.toString(value);
        if (Checker.isEmpty(strVal)) {
            return def;
        }
        try {
            return Timestamp.valueOf(strVal);
        }
        catch (Throwable t) {
            if (strVal.indexOf('-') != -1) {
                String format = null;
                if (strVal.length() == DEFFAULT_DATE_FORMAT.length()) {
                    format = DEFFAULT_DATE_FORMAT;
                }
                else if (strVal.length() == 10) {
                    format = "yyyy-MM-dd";
                }
                else {
                    format = "yyyy-MM-dd HH:mm:ss.SSS";
                }
                try {
                    return new java.sql.Timestamp(new SimpleDateFormat(format).parse(strVal).getTime());
                }
                catch (ParseException e) {
                    return def;
                }
            }
            Long longVal = toLong(strVal);
            if (longVal == null) {
                return def;
            }
            try {
                return new java.sql.Timestamp(longVal);
            }
            catch (Throwable t1) {
                return def;
            }
        }
    }
}
