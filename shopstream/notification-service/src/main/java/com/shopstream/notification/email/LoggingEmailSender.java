package com.shopstream.notification.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Pretends to send email by writing it to the log. Check: docker compose logs -f notification-service */
@Component
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ---------------- EMAIL ----------------
                To:      {}
                Subject: {}

                {}
                ---------------------------------------""", to, subject, body);
    }
}
