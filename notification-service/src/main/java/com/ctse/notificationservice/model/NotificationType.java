package com.ctse.notificationservice.model;

/**
 * NotificationType — The kinds of notifications this service can send.
 *
 * Each type maps to a message template in NotificationService.
 * Callers (User Service, Event Service, Registration Service) pass one of
 * these string values in CreateNotificationRequest.type.
 *
 * Templates:
 *   WELCOME               → Sent to a new user when their account is created.
 *   NEW_EVENT             → Sent to all users when a new event is published.
 *   REGISTRATION_CONFIRMED→ Sent to a user when they register for an event.
 */
public enum NotificationType {
    WELCOME,
    NEW_EVENT,
    REGISTRATION_CONFIRMED
}
