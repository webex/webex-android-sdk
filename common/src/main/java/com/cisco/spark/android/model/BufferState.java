package com.cisco.spark.android.model;

public class BufferState {
    private String locus;
    private String conversation;
    private String board;

    public String getLocus() {
        return locus;
    }

    public String getConversation() {
        return conversation;
    }

    public String getBoard() {
        return board;
    }

    public BufferState(String locus, String conversation, String board) {
        this.locus = locus;
        this.conversation = conversation;
        this.board = board;
    }
}
