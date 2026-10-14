package com.bnpl.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SmsServiceTest {

    private SmsService smsService;

    @BeforeEach
    void setUp() {
        smsService = new SmsService();
        ReflectionTestUtils.setField(smsService, "accountSid", "dummy_sid");
        ReflectionTestUtils.setField(smsService, "authToken", "dummy_token");
        ReflectionTestUtils.setField(smsService, "fromPhoneNumber", "+1234567890");
        ReflectionTestUtils.setField(smsService, "isTwilioEnabled", false);
    }

    @Test
    @DisplayName("Should fall back to mock logging without throwing exceptions when Twilio is disabled")
    void testSendSms_MockMode() {
        assertDoesNotThrow(() -> {
            smsService.sendSms("+919999999999", "Test notification message");
        });
    }

    @Test
    @DisplayName("Init should complete gracefully without throwing exceptions")
    void testInitTwilio_Disabled() {
        assertDoesNotThrow(() -> {
            smsService.initTwilio();
        });
    }
}
