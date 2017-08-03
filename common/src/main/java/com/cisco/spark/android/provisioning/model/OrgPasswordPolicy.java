package com.cisco.spark.android.provisioning.model;

import android.support.annotation.Nullable;

public class OrgPasswordPolicy {
    private Integer minimumLength;
    private Integer maximumLength;
    private Integer minimumNumeric;
    private Integer minimumCapAlpha;
    private Integer minimumLowAlpha;
    private Integer minimumSpecial;
    private String specialChars;
    private boolean filterString;
    private Integer repeatCharCount;
    private String notAcceptableStrings;

    /**
     * @return Minimum length of password
     */
    @Nullable
    public Integer getMinimumLength() {
        return minimumLength;
    }

    /**
     * @return Maximum length of password, value of "0" means unlimited
     */
    @Nullable
    public Integer getMaximumLength() {
        return maximumLength;
    }

    /**
     * @return Minimum number of numeric characters in password
     */
    @Nullable
    public Integer getMinimumNumeric() {
        return minimumNumeric;
    }

    /**
     * @return Minimum number of uppercase alphabetic characters in password
     */
    @Nullable
    public Integer getMinimumCapAlpha() {
        return minimumCapAlpha;
    }

    /**
     * @return Minimum number of lowercase alphabetic characters in password
     */
    @Nullable
    public Integer getMinimumLowAlpha() {
        return minimumLowAlpha;
    }

    /**
     * @return Minimum number of {@link OrgPasswordPolicy#specialChars special characters} in password
     */
    @Nullable
    public Integer getMinimumSpecial() {
        return minimumSpecial;
    }

    /**
     * @return what the policy considers special characters
     */
    public String getSpecialCharacters() {
        return specialChars;
    }

    /**
     * @return whether the user's name and/or email is allowed to be in their password
     */
    public boolean isFilterString() {
        return filterString;
    }

    /**
     * The password can not contain a substring which the same character is repeated more than "X" times.
     * Example: if value is "3", "aaaa" will not be acceptable.
     */
    @Nullable
    public Integer getRepeatCharCount() {
        return repeatCharCount;
    }

    /**
     * The password can not be any one in this string list (case insensitive)
     * @return a list of the unacceptable strings, concatenated with a comma
     */
    public String getNotAcceptableStrings() {
        return notAcceptableStrings;
    }
}
