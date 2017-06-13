package com.cisco.spark.android.model;


import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;

/*
    ActivityObject representation of an ImageURI. This is how a sticker is sent to cloud-apps
    and received by a client in an activity.
 */
public class ImageURIObject extends ActivityObject {

    private String location;

    public ImageURIObject() {
        super(ObjectType.imageURI);
    }

    public ImageURIObject(String uri) {
        this();
        this.location = uri;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "ImageURIObject{" +
                "location=" + location +
                '}';
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(location)) {
            this.location = CryptoUtils.encryptToJwe(key, this.location.toString());
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (Strings.notEmpty(location)) {
            this.location = CryptoUtils.decryptFromJwe(key, this.location);
        }
    }
}
