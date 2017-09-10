package com.ciscospark.androidsdk.utils.http;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class ListBody<T> {

    @SerializedName("items")
    private List<T> _items;

    List<T> getItems() {
        return _items;
    }
}
