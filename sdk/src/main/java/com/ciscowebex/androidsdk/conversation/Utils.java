package com.ciscowebex.androidsdk.conversation;


import java.io.UnsupportedEncodingException;
import android.util.Base64;

public class Utils {

    public static String getUUID(String id) throws UnsupportedEncodingException {

        // Decode
        byte[] asBytes = Base64.decode(id.getBytes("UTF-8"),Base64.DEFAULT);
        String uuid = new String(asBytes, "utf-8");
        //
        return uuid.substring(uuid.lastIndexOf("/")+1);
    }

    public static String getPersonId(String id) throws UnsupportedEncodingException {
        String str = Utils.getIdFromUUID(id,"PEOPLE");
        return str.substring(0,str.length()-1);
    }

    public static String getMessageId(String id) throws UnsupportedEncodingException {
        return Utils.getIdFromUUID(id,"MESSAGE");
    }

    private static String getIdFromUUID(String id,String type) throws UnsupportedEncodingException {

        String fullUUID = String.format("ciscospark://us/%s/%s",type,id);
        // Encode
        byte[] bytesEncoded = Base64.encode(fullUUID.getBytes("UTF-8"),Base64.DEFAULT);
        String _webexObejctId = new String(bytesEncoded,"utf-8").replaceAll("\\r|\\n", "");;
        return _webexObejctId;//.substring(0,_webexObejctId.length()-1);
    }

}
