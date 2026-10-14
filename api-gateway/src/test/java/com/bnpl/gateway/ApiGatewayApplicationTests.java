package com.bnpl.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiGatewayApplicationTests {

    @Test
    @DisplayName("Verify ApiGatewayApplication class exists and can be referenced")
    void applicationClassLoads() {
        ApiGatewayApplication app = new ApiGatewayApplication();
        assertNotNull(app);
    }
}
