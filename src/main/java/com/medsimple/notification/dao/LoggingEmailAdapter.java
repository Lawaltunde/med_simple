package com.medsimple.notification.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback email channel — logs the message instead of sending.
 * Default adapter; lets every environment run without an SMTP provider configured.
 * A real SmtpEmailAdapter (JavaMailSender / SendGrid) plugs in behind EmailDeliveryDao
 * as @Primary @ConditionalOnProperty(notification.email.adapter=smtp) when provisioned.
 */
@Slf4j
@Component
public class LoggingEmailAdapter implements EmailDeliveryDao {

    @Override
    public boolean send(String toEmail, String subject, String body) {
        log.info("[EMAIL → {}] {} :: {}", toEmail, subject, body);
        return true;
    }
}
