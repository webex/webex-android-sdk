package com.cisco.spark.android.model;


import java.util.ArrayList;
import java.util.List;

public class KmsRequestResponseComplete {

    private List<String> kmsMessages = new ArrayList<>();
    private String destination;
    private boolean success;
    private boolean synchronous;

    public List<String> getKmsMessages() {
        return kmsMessages;
    }

    public void addKmsMessages(List<String> kmsMessages) {
        this.kmsMessages.addAll(kmsMessages);
    }

    public void addKmsMessage(String kmsMessage) {
        this.kmsMessages.add(kmsMessage);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

}
