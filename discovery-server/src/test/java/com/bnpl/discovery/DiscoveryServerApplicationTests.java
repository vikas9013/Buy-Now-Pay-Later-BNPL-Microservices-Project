package com.bnpl.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DiscoveryServerApplicationTests {

    @Test
    @DisplayName("Verify DiscoveryServerApplication class exists and can be referenced")
    void applicationClassLoads() {
        DiscoveryServerApplication app = new DiscoveryServerApplication();
        assertNotNull(app);
    }
}
