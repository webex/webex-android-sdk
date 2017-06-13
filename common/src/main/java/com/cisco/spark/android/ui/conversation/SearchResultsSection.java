package com.cisco.spark.android.ui.conversation;


public class SearchResultsSection {
    /* Sections
    *  Each section contains a Header,
    *  if section.count = 0, set emptyResultPosition to header.headerPosition + 1
    *  if section.count > MAX_LIMIT, expandable item -- LoadMore
    *  items from baseAdapter
     */
    private SearchResultsHeader searchResultsHeader;
    private SearchResultsLoadMore expandItem;
    private String loadMoreText;
    private int count;
    private SectionType sectionType;
    private int emptyResultPosition = 0;
    private int expandedSectionStartPosition = 0;
    public static final int MAX_INIT_PEOPLE_LIMIT = 4;
    public static final int MAX_INIT_CONVERSATION_LIMIT = 3;
    public static final int MAX_INIT_ACTIVITY_RESULT_LIMIT = 2;

    public SearchResultsSection(SearchResultsHeader searchResultsHeader, int count, SectionType type, String loadMoreText, int expandedSectionStartPosition) {
        this.searchResultsHeader = searchResultsHeader;
        this.count = count;
        this.expandItem = null;
        this.sectionType = type;
        this.loadMoreText = loadMoreText;
        this.expandedSectionStartPosition = expandedSectionStartPosition;
    }

    public SearchResultsSection(SectionType savedExpandedSectionType) {
        this.sectionType = savedExpandedSectionType;
    }

    public SectionType getSectionType() {
        return sectionType;
    }

    public SearchResultsHeader getHeader() {
        return searchResultsHeader;
    }

    public SearchResultsLoadMore getExpandItem() {
        return expandItem;
    }

    public int getCount() {
        return count;
    }

    public int getEmptyResultPosition() {
        return emptyResultPosition;
    }

    public int getExpandedSectionStartPosition() {
        return expandedSectionStartPosition;
    }

    public String getLoadMoreText() {
        return loadMoreText;
    }

    public void setUpEmptyResult() {
        if (count == 0) {
            this.emptyResultPosition = this.searchResultsHeader.getHeaderPosition() + 1;
        }
    }

    public boolean setupExpandItems(int sectionNum) {
        switch (sectionType) {
            case SECTION_TYPE_PEOPLE:
                if (count > MAX_INIT_PEOPLE_LIMIT) {
                    this.expandItem = new SearchResultsLoadMore(searchResultsHeader.getHeaderPosition() + MAX_INIT_PEOPLE_LIMIT + 1, count - MAX_INIT_PEOPLE_LIMIT, loadMoreText, sectionNum);
                    return true;
                }
            case SECTION_TYPE_CONVERSATIONS:
                if (count > MAX_INIT_CONVERSATION_LIMIT) {
                    this.expandItem = new SearchResultsLoadMore(searchResultsHeader.getHeaderPosition() + MAX_INIT_CONVERSATION_LIMIT + 1, count - MAX_INIT_CONVERSATION_LIMIT, loadMoreText, sectionNum);
                    return true;
                }
            case SECTION_TYPE_MESSAGES:
            case SECTION_TYPE_CONTENT:
                if (count > MAX_INIT_ACTIVITY_RESULT_LIMIT) {
                    this.expandItem = new SearchResultsLoadMore(searchResultsHeader.getHeaderPosition() + MAX_INIT_ACTIVITY_RESULT_LIMIT + 1, count - MAX_INIT_ACTIVITY_RESULT_LIMIT, loadMoreText, sectionNum);
                    return true;
                }
        }

        return false;
    }

    public void setExpandedSectionStartPosition(int position) {
        this.expandedSectionStartPosition = position;
    }

    public void setExpandItem(SearchResultsLoadMore expandItem) {
        this.expandItem = expandItem;
    }

    public enum SectionType {
        SECTION_TYPE_PEOPLE,
        SECTION_TYPE_CONVERSATIONS,
        SECTION_TYPE_CONTENT,
        SECTION_TYPE_MESSAGES
    }
}

