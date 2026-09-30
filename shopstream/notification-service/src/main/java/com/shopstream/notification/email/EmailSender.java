package com.shopstream.notification.email;

/** Swap LoggingEmailSender for an Amazon SES / SendGrid / SMTP implementation in production. */
public interface EmailSender {

    void send(String to, String subject, String body);
}
