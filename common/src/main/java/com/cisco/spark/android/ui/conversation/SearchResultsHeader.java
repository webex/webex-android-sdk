package com.cisco.spark.android.ui.conversation;

public class SearchResultsHeader {
    private int firstPosition;
    private int headerPosition;
    private CharSequence title;

    public SearchResultsHeader(int firstPosition, CharSequence title) {
        this.firstPosition = firstPosition;
        this.title = title;
    }

    public int getFirstPosition() {
        return firstPosition;
    }

    public CharSequence getTitle() {
        return title;
    }

    public int getHeaderPosition() {
        return headerPosition;
    }

    public void setFirstPosition(int firstPosition) {
        this.firstPosition = firstPosition;
    }

    public void setHeaderPosition(int headerPosition) {
        this.headerPosition = headerPosition;
    }
}
