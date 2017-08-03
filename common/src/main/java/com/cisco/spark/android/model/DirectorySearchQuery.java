package com.cisco.spark.android.model;


public class DirectorySearchQuery {
    private boolean includeRooms;
    private int size;
    private String queryString;

    public DirectorySearchQuery(String query, boolean includeRooms) {
        this.queryString = query;
        this.includeRooms = includeRooms;
    }

    public void setIncludeRooms(boolean includeRooms) {
        this.includeRooms = includeRooms;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getQueryString() {
        return queryString;
    }
}
