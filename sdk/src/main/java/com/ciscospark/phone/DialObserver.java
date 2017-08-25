package com.ciscospark.phone;

import com.ciscospark.SparkError;

import java.util.List;

/**
 * Created by lm on 6/16/17.
 */

public interface DialObserver {

    public enum ErrorCode {
        GENERAL_ERROR, ERROR_PARAMETER, ERROR_STATUS,  //for example, if in a ActivitedCall period, dial can not be called.
        ERROR_PERMISSION, ERROR_JOINTERROR
    }


    public final static String ErrorPermission = "Need to grant permission";
    public final static String ErrorGereral = "General Error";
    public final static String ErrorStatus = "Status Error";
    public final static String ErrorCallJoin = "Join call Error";

    public final static String ErrorParameter = "Wrong parameter";

    public void onSuccess(Call call);

    public void onFailed(SparkError error);

    /**
     * this function will be called while user need to grant permission
     *
     * @param permissions permission list
     * @return none
     */
    void onPermissionRequired(List<String> permissions);

}
