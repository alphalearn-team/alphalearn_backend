package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void healthReturnsOkStatus() {
        HealthController controller = new HealthController();

        Map<String, String> response = controller.health();

        assertEquals("ok", response.get("status"));
    }
}
