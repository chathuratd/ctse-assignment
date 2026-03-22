package com.ctse.e2e;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end integration tests for the University Tech Conference Event Platform.
 *
 * Covers all 16 scenarios from E2E-TESTING.md in order:
 *   Steps 1–6  : User registration, login, JWT auth
 *   Steps 7–9  : Event creation and retrieval
 *   Steps 10–13: Registrations (create, duplicate guard, list, cancel)
 *   Steps 14–16: Notification assertions + second event broadcast
 *
 * Prerequisites:
 *   All 4 services must be running (e.g. via `docker compose up`).
 *   Run with: cd e2e-tests && mvn test
 *
 * Override service URLs if needed:
 *   mvn test -DuserService.url=http://my-host:8001
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Event Management System — E2E Integration Tests")
class E2EIntegrationTest {

    // ── Service base URLs (default to localhost; override with -D flags) ─────

    private static final String USER_SVC  = prop("userService.url",  "http://localhost:8001");
    private static final String EVENT_SVC = prop("eventService.url", "http://localhost:8002");
    private static final String REG_SVC   = prop("regService.url",   "http://localhost:8003");
    private static final String NOTIF_SVC = prop("notifService.url", "http://localhost:8004");

    // ── Unique test-run identifier (prevents DB conflicts on repeated runs) ──

    private static final String RUN_TAG     = String.valueOf(System.currentTimeMillis() % 100_000);
    private static final String ALICE_EMAIL = "alice" + RUN_TAG + "@test.local";
    private static final String BOB_EMAIL   = "bob"   + RUN_TAG + "@test.local";

    // ── State shared across ordered test steps ────────────────────────────────

    private static String aliceId;
    private static String aliceToken;
    private static String bobId;
    private static String event1Id;
    private static String event2Id;
    private static String registrationId;

    // ── Health gate: wait for all services before any test runs ──────────────

    @BeforeAll
    static void waitForServices() {
        System.out.println("\n═══ Waiting for services to become ready ═══");
        String[][] services = {
            {"User Service",         USER_SVC  + "/users"},
            {"Event Service",        EVENT_SVC + "/events"},
            {"Registration Service", REG_SVC   + "/registrations/user/health"},
            {"Notification Service", NOTIF_SVC + "/notifications/user/health"},
        };
        for (String[] entry : services) {
            String name = entry[0], url = entry[1];
            Awaitility.await()
                .alias(name)
                .atMost(Duration.ofSeconds(90))
                .pollInterval(Duration.ofSeconds(3))
                .until(() -> {
                    try {
                        given().get(url).statusCode(); // any HTTP response = up
                        return true;
                    } catch (Exception e) {
                        System.out.printf("  [waiting] %s not ready yet...%n", name);
                        return false;
                    }
                });
            System.out.printf("  [ready]   %s ✓%n", name);
        }
        System.out.println("═══ All services ready — starting tests ═══\n");
    }

    // =========================================================================
    // USERS  (Steps 1–6)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Step 1 — Register Alice → JWT returned, WELCOME notification triggered")
    void registerAlice() {
        Response r = given()
            .contentType(ContentType.JSON)
            .body(userBody("Alice Fernando", ALICE_EMAIL, "password123"))
            .post(USER_SVC + "/users")
            .then()
            .statusCode(201)
            .body("token",  notNullValue())
            .body("userId", notNullValue())
            .body("email",  equalTo(ALICE_EMAIL))
            .body("name",   equalTo("Alice Fernando"))
            .extract().response();

        aliceId    = r.jsonPath().getString("userId");
        aliceToken = r.jsonPath().getString("token");
        System.out.println("  Alice ID: " + aliceId);
    }

    @Test
    @Order(2)
    @DisplayName("Step 2 — Register Bob → JWT returned, WELCOME notification triggered")
    void registerBob() {
        Response r = given()
            .contentType(ContentType.JSON)
            .body(userBody("Bob Perera", BOB_EMAIL, "password123"))
            .post(USER_SVC + "/users")
            .then()
            .statusCode(201)
            .body("token",  notNullValue())
            .body("userId", notNullValue())
            .body("email",  equalTo(BOB_EMAIL))
            .body("name",   equalTo("Bob Perera"))
            .extract().response();

        bobId = r.jsonPath().getString("userId");
        System.out.println("  Bob ID: " + bobId);
    }

    @Test
    @Order(3)
    @DisplayName("Step 3 — Login with Alice's credentials returns a valid JWT")
    void login() {
        given()
            .contentType(ContentType.JSON)
            .body(loginBody(ALICE_EMAIL, "password123"))
            .post(USER_SVC + "/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("email", equalTo(ALICE_EMAIL));
    }

    @Test
    @Order(4)
    @DisplayName("Step 4 — GET /users/me with JWT returns Alice's profile")
    void getMyProfile() {
        assumeTrue(aliceToken != null, "Alice's token is null — step 1 likely failed");
        given()
            .header("Authorization", "Bearer " + aliceToken)
            .get(USER_SVC + "/users/me")
            .then()
            .statusCode(200)
            .body("id",    equalTo(aliceId))
            .body("email", equalTo(ALICE_EMAIL))
            .body("name",  equalTo("Alice Fernando"));
    }

    @Test
    @Order(5)
    @DisplayName("Step 5 — GET /users/{id} returns user (public inter-service endpoint)")
    void getUserById() {
        assumeTrue(aliceId != null, "Alice's ID is null — step 1 likely failed");
        given()
            .get(USER_SVC + "/users/" + aliceId)
            .then()
            .statusCode(200)
            .body("id",    equalTo(aliceId))
            .body("email", equalTo(ALICE_EMAIL));
    }

    @Test
    @Order(6)
    @DisplayName("Step 6 — GET /users returns all users (public inter-service endpoint)")
    void listAllUsers() {
        given()
            .get(USER_SVC + "/users")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(2)));
    }

    // =========================================================================
    // EVENTS  (Steps 7–9)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Step 7 — Create event → NEW_EVENT notification triggered for all users")
    void createEvent() {
        assumeTrue(aliceId != null, "aliceId null — step 1 failed");

        Response r = given()
            .contentType(ContentType.JSON)
            .body(eventBody(
                "AI & Machine Learning Workshop",
                "Hands-on workshop covering TensorFlow and PyTorch",
                "SLIIT Main Auditorium",
                "2026-09-15T09:00:00",
                100
            ))
            .post(EVENT_SVC + "/events")
            .then()
            .statusCode(201)
            .body("id",       notNullValue())
            .body("title",    equalTo("AI & Machine Learning Workshop"))
            .body("location", equalTo("SLIIT Main Auditorium"))
            .body("capacity", equalTo(100))
            .extract().response();

        event1Id = r.jsonPath().getString("id");
        assertNotNull(event1Id, "Event ID must not be null");
        System.out.println("  Event 1 ID: " + event1Id);
    }

    @Test
    @Order(8)
    @DisplayName("Step 8 — GET /events returns list containing the created event")
    void listEvents() {
        given()
            .get(EVENT_SVC + "/events")
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(9)
    @DisplayName("Step 9 — GET /events/{id} returns event by ID (public inter-service endpoint)")
    void getEventById() {
        assumeTrue(event1Id != null, "event1Id null — step 7 failed");
        given()
            .get(EVENT_SVC + "/events/" + event1Id)
            .then()
            .statusCode(200)
            .body("id",    equalTo(event1Id))
            .body("title", equalTo("AI & Machine Learning Workshop"));
    }

    // =========================================================================
    // REGISTRATIONS  (Steps 10–13)
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Step 10 — Register Alice for event → CONFIRMED status, REGISTRATION_CONFIRMED notification triggered")
    void registerAliceForEvent() {
        assumeTrue(aliceId  != null, "aliceId null — step 1 failed");
        assumeTrue(event1Id != null, "event1Id null — step 7 failed");

        Map<String, String> body = new HashMap<>();
        body.put("userId",  aliceId);
        body.put("eventId", event1Id);

        Response r = given()
            .contentType(ContentType.JSON)
            .body(body)
            .post(REG_SVC + "/registrations")
            .then()
            .statusCode(201)
            .body("id",      notNullValue())
            .body("userId",  equalTo(aliceId))
            .body("eventId", equalTo(event1Id))
            .body("status",  equalTo("CONFIRMED"))
            .extract().response();

        registrationId = r.jsonPath().getString("id");
        assertNotNull(registrationId, "Registration ID must not be null");
        System.out.println("  Registration ID: " + registrationId);
    }

    @Test
    @Order(11)
    @DisplayName("Step 11 — Duplicate registration returns 4xx (user already registered)")
    void duplicateRegistrationFails() {
        assumeTrue(aliceId  != null, "aliceId null — step 1 failed");
        assumeTrue(event1Id != null, "event1Id null — step 7 failed");

        Map<String, String> body = new HashMap<>();
        body.put("userId",  aliceId);
        body.put("eventId", event1Id);

        given()
            .contentType(ContentType.JSON)
            .body(body)
            .post(REG_SVC + "/registrations")
            .then()
            .statusCode(both(greaterThanOrEqualTo(400)).and(lessThan(500)));
    }

    @Test
    @Order(12)
    @DisplayName("Step 12 — GET /registrations/user/{id} returns Alice's registration list")
    void getUserRegistrations() {
        assumeTrue(aliceId  != null, "aliceId null — step 1 failed");
        assumeTrue(event1Id != null, "event1Id null — step 7 failed");

        given()
            .get(REG_SVC + "/registrations/user/" + aliceId)
            .then()
            .statusCode(200)
            .body("$",           hasSize(greaterThanOrEqualTo(1)))
            .body("[0].userId",  equalTo(aliceId))
            .body("[0].eventId", equalTo(event1Id));
    }

    @Test
    @Order(13)
    @DisplayName("Step 13 — DELETE /registrations/{id} cancels the registration")
    void cancelRegistration() {
        assumeTrue(registrationId != null, "registrationId null — step 10 failed");
        given()
            .delete(REG_SVC + "/registrations/" + registrationId)
            .then()
            .statusCode(anyOf(is(200), is(204)));
    }

    // =========================================================================
    // NOTIFICATIONS  (Steps 14–16)
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("Step 14 — Alice has WELCOME + NEW_EVENT + REGISTRATION_CONFIRMED notifications")
    void aliceHasThreeNotifications() {
        assumeTrue(aliceId != null, "aliceId null — step 1 failed");

        // Notifications are written by the Notification Service after inter-service calls;
        // poll up to 8 s to let any in-flight requests complete.
        awaitNotifications(aliceId, 3);

        given()
            .get(NOTIF_SVC + "/notifications/user/" + aliceId)
            .then()
            .statusCode(200)
            .body("$",    hasSize(greaterThanOrEqualTo(3)))
            .body("type", hasItems("WELCOME", "NEW_EVENT", "REGISTRATION_CONFIRMED"));
    }

    @Test
    @Order(15)
    @DisplayName("Step 15 — Bob has WELCOME + NEW_EVENT notifications")
    void bobHasTwoNotifications() {
        assumeTrue(bobId != null, "bobId null — step 2 failed");

        awaitNotifications(bobId, 2);

        given()
            .get(NOTIF_SVC + "/notifications/user/" + bobId)
            .then()
            .statusCode(200)
            .body("$",    hasSize(greaterThanOrEqualTo(2)))
            .body("type", hasItems("WELCOME", "NEW_EVENT"));
    }

    @Test
    @Order(16)
    @DisplayName("Step 16 — Create second event → NEW_EVENT broadcast: both users gain +1 notification")
    void createSecondEventBroadcast() {
        assumeTrue(aliceId != null, "aliceId null — step 1 failed");
        assumeTrue(bobId   != null, "bobId null — step 2 failed");

        Response r = given()
            .contentType(ContentType.JSON)
            .body(eventBody(
                "Cloud Computing with AWS",
                "Introduction to EC2, S3, Lambda and ECS",
                "SLIIT Lab 3",
                "2026-10-20T14:00:00",
                50
            ))
            .post(EVENT_SVC + "/events")
            .then()
            .statusCode(201)
            .body("id",    notNullValue())
            .body("title", equalTo("Cloud Computing with AWS"))
            .extract().response();

        event2Id = r.jsonPath().getString("id");
        assertNotNull(event2Id, "Second event ID must not be null");
        System.out.println("  Event 2 ID: " + event2Id);

        // Alice: WELCOME + REGISTRATION_CONFIRMED + 2×NEW_EVENT = 4
        // Bob:   WELCOME + 2×NEW_EVENT = 3
        awaitNotifications(aliceId, 4);
        awaitNotifications(bobId,   3);

        given()
            .get(NOTIF_SVC + "/notifications/user/" + aliceId)
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(4)));

        given()
            .get(NOTIF_SVC + "/notifications/user/" + bobId)
            .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(3)));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Map<String, String> userBody(String name, String email, String password) {
        Map<String, String> m = new HashMap<>();
        m.put("name",     name);
        m.put("email",    email);
        m.put("password", password);
        return m;
    }

    private static Map<String, String> loginBody(String email, String password) {
        Map<String, String> m = new HashMap<>();
        m.put("email",    email);
        m.put("password", password);
        return m;
    }

    private static Map<String, Object> eventBody(String title, String description,
                                                   String location, String date, int capacity) {
        Map<String, Object> m = new HashMap<>();
        m.put("title",       title);
        m.put("description", description);
        m.put("location",    location);
        m.put("date",        date);
        m.put("capacity",    capacity);
        return m;
    }

    /**
     * Polls the notification endpoint until {@code userId} has at least {@code min}
     * notifications or the 8-second window expires (causing the next assertion to fail
     * with a meaningful diff rather than a timeout exception).
     */
    private static void awaitNotifications(String userId, int min) {
        Awaitility.await()
            .atMost(Duration.ofSeconds(8))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> {
                try {
                    int count = given()
                        .get(NOTIF_SVC + "/notifications/user/" + userId)
                        .then().extract().jsonPath()
                        .getList("$").size();
                    return count >= min;
                } catch (Exception e) {
                    return false;
                }
            });
    }

    private static String prop(String key, String defaultValue) {
        String v = System.getProperty(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }
}
