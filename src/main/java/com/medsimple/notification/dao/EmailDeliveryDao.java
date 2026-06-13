package com.medsimple.notification.dao;

// Boundary for sending the EMAIL-channel copy of a notification.
// A real SMTP/SendGrid adapter plugs in behind this interface (the @Primary seam),
// selected by notification.email.adapter, without touching NotificationService.
public interface EmailDeliveryDao {

    // Returns true if the email was accepted for delivery.
    boolean send(String toEmail, String subject, String body);
}
