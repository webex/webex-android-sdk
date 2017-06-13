package com.cisco.spark.android.sync;


import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ImageURIObject;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;


public class ImageURI extends Message {

    private String location;

    public ImageURI(final Activity activity) {
        super(activity.getObject().getDisplayName(), activity.getObject().getContent(), activity.getActor().getKey(), activity, activity.getProvider());
        this.location = ((ImageURIObject) activity.getObject()).getLocation();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String uri) {
        this.location = uri;
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException {
        super.decrypt(key);

        if (Strings.notEmpty(location)) {
            this.location = CryptoUtils.decryptFromJwe(key, this.location);
        }
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(location)) {
            this.location = CryptoUtils.encryptToJwe(key, this.location.toString());
        }
    }
}
