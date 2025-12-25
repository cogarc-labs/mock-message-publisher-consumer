package com.cogarc.notification.consumer;

import java.util.List;

public class MessageCountResponse {
    private String messageType;
    private int count;
    private List<String> identifiers;

    public MessageCountResponse() {
    }

    public MessageCountResponse(String messageType, int count, List<String> identifiers) {
        this.messageType = messageType;
        this.count = count;
        this.identifiers = identifiers;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<String> identifiers) {
        this.identifiers = identifiers;
    }
}

