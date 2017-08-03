package com.cisco.spark.android.model;

public class BufferState {
    final static String BUFFERED = "BUFFERED";
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

    public boolean isConversationBuffered() {
        return BUFFERED.equals(conversation);
    }
    public boolean isLocusBuffered() {
        return BUFFERED.equals(locus);
    }
    public boolean isWhiteboardBuffered() {
        return BUFFERED.equals(board);
    }
}
