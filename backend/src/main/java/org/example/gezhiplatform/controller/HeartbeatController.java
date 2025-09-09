package org.example.gezhiplatform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class HeartbeatController {

    public record HeartbeatResponse(
        String message,
        LocalDateTime serverTime
    ) {
        HeartbeatResponse() {
            this("pong", LocalDateTime.now());
        }
    }

    @GetMapping("/")
    public HeartbeatResponse heartbeat() {
        return new HeartbeatResponse();
    }

}
