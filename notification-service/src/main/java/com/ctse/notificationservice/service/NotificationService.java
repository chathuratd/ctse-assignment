package com.ctse.notificationservice.service;

import com.ctse.notificationservice.dto.CreateNotificationRequest;
import com.ctse.notificationservice.dto.EventMirrorResponse;
import com.ctse.notificationservice.dto.NotificationResponse;
import com.ctse.notificationservice.dto.UserMirrorResponse;
import com.ctse.notificationservice.model.Notification;
import com.ctse.notificationservice.model.NotificationType;
import com.ctse.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationService
 * ===================
 * Builds personalised notification messages from templates and persists them.
 *
 * INTER-SERVICE CALLS (this service is an active caller):
 * ────────────────────────────────────────────────────────
 * → User Service  GET /users/{userId}   — fetch user name for personalisation
 * → Event Service GET /events/{eventId} — fetch event title/location/date for context
 *
 * NOTIFICATION TEMPLATES:
 * ────────────────────────
 * WELCOME:
 *   "Welcome to the University Tech Conference Platform, {name}!
 *    Your account has been successfully created."
 *
 * NEW_EVENT:
 *   "Hi {name}, a new event has been scheduled: '{title}'
 *    at {location} on {date}. Register now!"
 *
 * REGISTRATION_CONFIRMED:
 *   "Hi {name}, you have successfully registered for '{title}'
 *    at {location} on {date}."
 *
 * Failure strategy: if User/Event Service is unreachable, the notification
 * is still saved with a fallback message. Notifications must not block the
 * caller (registration, account creation, event creation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;

    @Value("${app.services.user-url}")
    private String userServiceUrl;

    @Value("${app.services.event-url}")
    private String eventServiceUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Build a personalised message from a template, then persist the notification.
     *
     * Callers (User Service, Event Service, Registration Service) send only IDs.
     * This service fetches the details it needs and builds the message itself.
     */
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        // Fetch user details first — needed for both message building and email sending
        UserMirrorResponse user = fetchUser(request.getUserId());
        String message = buildMessage(request, user);

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .eventId(request.getEventId())
                .message(message)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Saved {} notification for user {}", request.getType(), request.getUserId());

        // Send email (non-blocking — failure doesn't roll back the saved notification)
        sendEmail(user, request.getType(), message);

        return toResponse(saved);
    }

    /**
     * Retrieve all notifications for a given user, newest first.
     */
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Template engine ───────────────────────────────────────────────────────

    /**
     * Resolves the correct template for the notification type and fills it in
     * by calling Event Service as needed. User is already fetched.
     */
    private String buildMessage(CreateNotificationRequest request, UserMirrorResponse user) {
        NotificationType type;
        try {
            type = NotificationType.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification type '{}', using fallback message", request.getType());
            return "You have a new notification.";
        }

        return switch (type) {
            case WELCOME -> String.format(
                    "Welcome to the University Tech Conference Platform, %s! " +
                    "Your account has been successfully created.",
                    user.getName()
            );
            case NEW_EVENT -> {
                EventMirrorResponse event = fetchEvent(request.getEventId());
                yield String.format(
                    "Hi %s, a new event has been scheduled: '%s' at %s on %s. Register now!",
                    user.getName(),
                    event.getTitle(),
                    event.getLocation(),
                    event.getDate() != null ? event.getDate().toLocalDate() : "TBD"
                );
            }
            case REGISTRATION_CONFIRMED -> {
                EventMirrorResponse event = fetchEvent(request.getEventId());
                yield String.format(
                    "Hi %s, you have successfully registered for '%s' at %s on %s.",
                    user.getName(),
                    event.getTitle(),
                    event.getLocation(),
                    event.getDate() != null ? event.getDate().toLocalDate() : "TBD"
                );
            }
        };
    }

    // ── Email sending ─────────────────────────────────────────────────────────

    /**
     * Sends a notification email to the user via Gmail SMTP.
     * Non-critical — failure is logged but doesn't affect the API response.
     */
    private void sendEmail(UserMirrorResponse user, String type, String message) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send email for user {} — no email address available", user.getId());
            return;
        }

        String subject = switch (type) {
            case "WELCOME" -> "Welcome to University Tech Conference Platform!";
            case "NEW_EVENT" -> "New Event Available — University Tech Conference";
            case "REGISTRATION_CONFIRMED" -> "Registration Confirmed — University Tech Conference";
            default -> "Notification — University Tech Conference";
        };

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            mailSender.send(mailMessage);
            log.info("Email sent to {} for {} notification", user.getEmail(), type);
        } catch (Exception e) {
            log.warn("Failed to send email to {} (non-critical): {}", user.getEmail(), e.getMessage());
        }
    }

    // ── Inter-service helpers ─────────────────────────────────────────────────

    /**
     * Calls User Service: GET /users/{userId}
     * Returns a fallback with name="User" if the call fails.
     */
    private UserMirrorResponse fetchUser(String userId) {
        String url = userServiceUrl + "/users/" + userId;
        try {
            UserMirrorResponse user = restTemplate.getForObject(url, UserMirrorResponse.class);
            return user != null ? user : fallbackUser();
        } catch (RestClientException e) {
            log.warn("Could not fetch user {} from User Service: {}", userId, e.getMessage());
            return fallbackUser();
        }
    }

    /**
     * Calls Event Service: GET /events/{eventId}
     * Returns a fallback with title="Unknown Event" if the call fails.
     */
    private EventMirrorResponse fetchEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return fallbackEvent();
        }
        String url = eventServiceUrl + "/events/" + eventId;
        try {
            EventMirrorResponse event = restTemplate.getForObject(url, EventMirrorResponse.class);
            return event != null ? event : fallbackEvent();
        } catch (RestClientException e) {
            log.warn("Could not fetch event {} from Event Service: {}", eventId, e.getMessage());
            return fallbackEvent();
        }
    }

    private UserMirrorResponse fallbackUser() {
        return new UserMirrorResponse(null, "User", null);
    }

    private EventMirrorResponse fallbackEvent() {
        return new EventMirrorResponse(null, "Unknown Event", "TBA", LocalDateTime.now());
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .message(n.getMessage())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
