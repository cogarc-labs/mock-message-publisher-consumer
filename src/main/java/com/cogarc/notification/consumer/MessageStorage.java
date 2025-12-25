package com.cogarc.notification.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageStorage {

    private static final Logger logger = LoggerFactory.getLogger(MessageStorage.class);

    // Store counts and identifiers per message type
    private final Map<String, MessageCountResponse> messageCounts = new ConcurrentHashMap<>();

    public void storeMessage(String messageType, String identifier) {
        messageCounts.compute(messageType, (key, value) -> {
            if (value == null) {
                value = new MessageCountResponse();
                value.setMessageType(messageType);
                value.setCount(0);
                value.setIdentifiers(new ArrayList<>());
            }
            value.setCount(value.getCount() + 1);
            value.getIdentifiers().add(identifier);
            logger.debug("Stored message type: {}, identifier: {}, total count: {}", 
                messageType, identifier, value.getCount());
            return value;
        });
    }

    public Map<String, MessageCountResponse> getAllCounts() {
        return new HashMap<>(messageCounts);
    }

    public MessageCountResponse getCount(String messageType) {
        return messageCounts.getOrDefault(messageType, 
            new MessageCountResponse(messageType, 0, new ArrayList<>()));
    }

    public void clear() {
        messageCounts.clear();
        logger.info("Message storage cleared");
    }
}

