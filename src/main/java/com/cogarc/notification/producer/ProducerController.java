package com.cogarc.notification.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/producer")
public class ProducerController {

    @Autowired
    private ProducerService producerService;

    @PostMapping("/batch/{messageType}")
    public ResponseEntity<Map<String, Object>> publishBatch(@PathVariable String messageType) {
        int count = producerService.publishBatch(messageType);
        return ResponseEntity.ok(Map.of(
            "messageType", messageType,
            "publishedCount", count,
            "status", "success"
        ));
    }
}

