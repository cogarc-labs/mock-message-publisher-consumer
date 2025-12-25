package com.cogarc.notification.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class ConsumerController {

    @Autowired
    private MessageStorage messageStorage;

    @GetMapping("/count")
    public ResponseEntity<Map<String, MessageCountResponse>> getMessageCounts() {
        Map<String, MessageCountResponse> counts = messageStorage.getAllCounts();
        return ResponseEntity.ok(counts);
    }
}

