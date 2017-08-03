package com.cisco.spark.android.ui.conversation;

public class SearchResultsLoadMore {
    private int position;
    private int extraCount;
    private String loadMoreText;
    private int sectionNum;

    public SearchResultsLoadMore(int position, int extraCount, String loadMoreText, int sectionNum) {
        this.position = position;
        this.extraCount = extraCount;
        this.loadMoreText = loadMoreText;
        this.sectionNum = sectionNum;
    }

    public int getPosition() {
        return position;
    }

    public int getExtraCount() {
        return extraCount;
    }

    public String getLoadMoreText() {
        return loadMoreText;
    }


    public int getSectionNum() {
        return sectionNum;
    }

}
