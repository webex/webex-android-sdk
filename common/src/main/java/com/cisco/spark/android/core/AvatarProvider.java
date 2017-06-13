package com.cisco.spark.android.core;

import android.content.res.Resources;
import android.net.Uri;

import com.cisco.spark.android.R;
import com.cisco.spark.android.wdm.DeviceRegistration;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AvatarProvider {
    private final DeviceRegistration deviceRegistration;

    @Inject
    public AvatarProvider(DeviceRegistration deviceRegistration, Resources resources) {
        this.deviceRegistration = deviceRegistration;

        AvatarSize.BIG.pxSize = (long) resources.getDimension(R.dimen.big_avatar_size);
        AvatarSize.MEDIUM.pxSize = (long) resources.getDimension(R.dimen.avatar_callparticipant_height);
        AvatarSize.SMALL.pxSize = (long) resources.getDimension(R.dimen.small_avatar_size);
        AvatarSize.TINY.pxSize = (long) resources.getDimension(R.dimen.avatar_2up_size);
    }

    public Uri getUri(String id, String size) {
        if (id == null) {
            return null;
        }

        Uri baseAvatarUri = deviceRegistration.getAvatarServiceUrl();
        if (baseAvatarUri == null)
            return null;

        return baseAvatarUri.buildUpon()
                .appendPath("profile")
                .appendPath(id)
                .appendQueryParameter("s", size)
                .build();
    }

    public Uri getUri(String id, AvatarSize size) {
        return getUri(id, String.valueOf(size.pxSize));
    }

    public enum AvatarSize {
        BIG,
        MEDIUM,
        SMALL,
        TINY;

        long pxSize;

        public long getSize() {
            return pxSize;
        }
    }
}
