package com.bnpl.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:false}")
    private boolean isTwilioEnabled;

    @PostConstruct
    public void initTwilio() {
        if (isTwilioEnabled && !"YOUR_TWILIO_ACCOUNT_SID".equals(accountSid)) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio SMS Service Initialized successfully!");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
            }
        } else {
            log.info("Twilio SMS is disabled or credentials not set. Logging notifications to console.");
        }
    }

    public void sendSms(String toPhoneNumber, String messageBody) {
        if (!isTwilioEnabled || "YOUR_TWILIO_ACCOUNT_SID".equals(accountSid)) {
            log.info("[MOCK SMS SENT] To: {}, Body: {}", toPhoneNumber, messageBody);
            return;
        }

        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            log.info("Real Twilio SMS Sent! SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Error sending Twilio SMS to {}: {}", toPhoneNumber, e.getMessage());
        }
    }
}
