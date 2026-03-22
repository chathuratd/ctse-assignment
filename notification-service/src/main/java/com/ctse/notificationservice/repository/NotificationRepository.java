package com.ctse.notificationservice.repository;

import com.ctse.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * NotificationRepository
 * ======================
 * Provides data access for Notification entities.
 *
 * findByUserId: Spring Data JPA auto-generates:
 *   SELECT * FROM notifications WHERE user_id = ?
 * Used by GET /notifications/user/{userId}
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Returns all notifications for a given user, ordered by newest first
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}
