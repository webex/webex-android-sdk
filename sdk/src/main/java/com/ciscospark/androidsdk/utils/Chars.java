package com.ciscospark.androidsdk.utils;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Chars {

    private static final byte[] toUpper = new byte[256];

    private static final byte[] toLower = new byte[256];

    private static final boolean[] isAlpha = new boolean[256];

    private static final boolean[] isUpper = new boolean[256];

    private static final boolean[] isLower = new boolean[256];

    private static final boolean[] isWhite = new boolean[256];

    private static final boolean[] isDigit = new boolean[256];

    static {
        for (int i = 0; i < 256; i++) {
            toUpper[i] = (byte) i;
            toLower[i] = (byte) i;
        }
        for (int lc = 'a'; lc <= 'z'; lc++) {
            int uc = lc + 'A' - 'a';
            toUpper[lc] = (byte) uc;
            toLower[uc] = (byte) lc;
            isAlpha[lc] = true;
            isAlpha[uc] = true;
            isLower[lc] = true;
            isUpper[uc] = true;
        }
        isWhite[' '] = true;
        isWhite['\t'] = true;
        isWhite['\r'] = true;
        isWhite['\n'] = true;
        isWhite['\f'] = true;
        isWhite['\b'] = true;
        for (int d = '0'; d <= '9'; d++) {
            isDigit[d] = true;
        }
    }

    private static final String TOKEN = "-.!%*_+`'~";

    private static final String LWS = " \r\n\t";

    private static final String LB = "\r\n";

    private static final String MARK = "-_.!~*'()";

    private static final String USERUNRES = "&=+$,;?/";

    private static final String PASSUNRES = "&=+$,";

    private static final String PARAMUNRES = "[]/:&+$";

    private static final String HNVUNRES = "[]/?:+$";

    public static int toUpper(int c) {
        return toUpper[c & 0xff] & 0xff;
    }

    public static int toLower(int c) {
        return toLower[c & 0xff] & 0xff;
    }

    public static boolean isUpper(int c) {
        return isUpper[c & 0xff];
    }

    public static boolean isLower(int c) {
        return isLower[c & 0xff];
    }

    public static boolean isAscii(char ch) {
        return ch < 128;
    }

    public static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }

    public static boolean isAsciiControl(char ch) {
        return ch < 32 || ch == 127;
    }

    public static boolean isControl(final int c) {
        return c >= 0 && c < 32 || c == 127;
    }

    public static boolean isAlpha(int c) {
        return isAlpha[c & 0xff];
    }

    public static boolean isDigit(int c) {
        return isDigit[c & 0xff];
    }

    public static boolean isAlphaNum(int c) {
        return isAlpha(c) || isDigit(c);
    }

    public static boolean isWhitespace(int c) {
        return isWhite[c & 0xff];
    }

    public static boolean isToken(final int c) {
        return isAlphaNum(c) || TOKEN.indexOf(c) > -1;
    }

    public static boolean isLWS(final int c) {
        return LWS.indexOf(c) > -1;
    }

    public static boolean isLB(final int c) {
        return LB.indexOf(c) > -1;
    }
    public static boolean isMark(final int c) {
        return MARK.indexOf(c) > -1;
    }

    public static boolean isUnreserved(final int c) {
        return isAlphaNum(c) || c == '-' || c == '.' || c == '_' || c == '~';
    }

    public static boolean isReserved(char c) {
        return isGenericDelimiter(c) || isSubDelimiter(c);
    }

    public static boolean isGenericDelimiter(int c) {
        switch (c) {
            case ':':
            case '/':
            case '?':
            case '#':
            case '[':
            case ']':
            case '@':
                return true;
            default:
                return false;
        }
    }

    public static boolean isSubDelimiter(int c) {
        switch (c) {
            case '!':
            case '$':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case ';':
            case '=':
                return true;
            default:
                return false;
        }
    }

    public static boolean isPchar(char c) {
        return isUnreserved(c) || isSubDelimiter(c) || c == ':' || c == '@';
    }

    public static boolean isUserUnreserved(final int c) {
        return USERUNRES.indexOf(c) > -1;
    }

    public static boolean isPassUnreserved(final int c) {
        return PASSUNRES.indexOf(c) > -1;
    }

    public static boolean isParamUnreserved(final int c) {
        return PARAMUNRES.indexOf(c) > -1;
    }

    public static boolean isHNVUnreserved(final int c) {
        return HNVUNRES.indexOf(c) > -1;
    }

}
