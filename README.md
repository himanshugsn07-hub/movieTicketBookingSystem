# Movie Ticket Booking System

A Spring Boot REST backend for booking movie tickets at seat level across many cities, theatres, screens and shows.

- **Seat selection with time-bound holds** that release automatically when they lapse
- **Pricing tiers** (regular, premium, weekend), **discount codes**, payment, booking confirmation
- **Refunds on cancellation** under configurable, tiered refund policies
- **Correct behaviour under concurrency**: many users can go for the same seat at once and it is only ever allocated once
- **Non-blocking notifications** (confirmation, cancellation, reminder)
- **Role-based access**: `ADMIN` manages the catalogue, `CUSTOMER` browses and books

Stack: Java 21, Spring Boot 4.1.1, Spring Data JPA (Hibernate), Spring Security, PostgreSQL, Maven.

---

## 1. Running it

### Prerequisites
- JDK 21
- PostgreSQL (tested on 16) running on `localhost:5432`

### Create the databases (once)
```bash
createdb movie_ticket_db      # the application
createdb movie_ticket_test    # used only by the integration tests
```

### Configure
Defaults live in `src/main/resources/application.properties`. Override any of them with environment variables:

| Setting | Default | Override with |
|---|---|---|
| JDBC URL | `jdbc:postgresql://localhost:5432/movie_ticket_db` | `SPRING_DATASOURCE_URL` |
| DB user / password | `hgusain` / *(none)* | `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Admin login | `admin` / `admin123` | `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` |
| Simulated notification latency (ms) | `0` | `APP_NOTIFICATION_SIMULATED_DELAY_MS` |
| Scheduled jobs on/off | `true` | `APP_SCHEDULING_ENABLED` |
| Longest wait for a row lock before a request fails with `503` | `5s` | `APP_DB_LOCK_TIMEOUT` |

Tables are created and updated automatically (`spring.jpa.hibernate.ddl-auto=update`). That is convenient for development, but it does not change existing check constraints (for example when a new status value is added), so use migrations (Flyway/Liquibase) before running this for real. **Change the default admin password.**

### Start
```bash
./mvnw spring-boot:run
```
On startup the app seeds (if missing) the admin account and a default refund policy called `Standard`. The API listens on `http://localhost:8080`.

### Quick walk-through
```bash
# a customer registers (no login needed for this call)
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"secret123"}'

# the admin builds a catalogue (HTTP Basic)
ADMIN='-u admin:admin123 -H Content-Type:application/json'
curl $ADMIN -X POST localhost:8080/api/admin/cities   -d '{"name":"Pune"}'
curl $ADMIN -X POST localhost:8080/api/admin/theatres -d '{"name":"PVR","cityId":1}'
curl $ADMIN -X POST localhost:8080/api/admin/screens  -d '{"name":"Screen 1","theatreId":1,"rows":5,"columns":10}'
curl $ADMIN -X POST localhost:8080/api/admin/movies   -d '{"id":"m1","title":"Inception","language":"EN","durationMin":148,"genre":"SciFi"}'
curl $ADMIN -X POST localhost:8080/api/admin/shows    -d '{"screenId":1,"movieId":"m1","basePrice":250,"pricingTier":"REGULAR","startTime":"2030-01-01T18:00:00"}'

# the customer books: hold seats, then confirm with payment (and an optional discount code)
CUST='-u alice:secret123 -H Content-Type:application/json'
curl $CUST localhost:8080/api/shows/1/seats/available
curl $CUST -X POST localhost:8080/api/bookings -d '{"showId":1,"seatIds":[1,2]}'
curl $CUST -X POST localhost:8080/api/bookings/<confirmationId>/confirm -d '{"paymentType":"CARD"}'
curl $CUST localhost:8080/api/bookings/<confirmationId>/refund-preview
curl $CUST -X POST localhost:8080/api/bookings/<confirmationId>/cancel
```

---

## 2. Tests

```bash
./mvnw test      # 65 unit tests: fast, no database, no Spring context
./mvnw verify    # unit tests + 41 integration tests (needs the movie_ticket_test database)
```

**Unit tests** (`*Test`) cover the rules that decide money, seats and state: seat reservation, the booking state machine, discount and refund calculation, pricing, `BookingService` / `AdminShowService` orchestration, the notification reminder rule and the dispatcher's retry logic. Time comes from an injected `Clock`, so they use a fixed clock and never sleep.

**Integration tests** (`*IT`, run by the failsafe plugin) start the whole application against `movie_ticket_test` and call the REST API with real logins: security, the admin catalogue, the full booking lifecycle, discounts, refund policies, admin show cancellation, hold expiry, notifications, and **real parallel requests** for the concurrency guarantees. Each test starts from an empty database. Background schedulers are switched off in the test profile and the tests trigger the sweeps themselves.

I checked that these tests can fail: removing the seat lock, the booking lock or the discount-code lock, sending a notification inside its database transaction, or dropping the `503` mapping each makes them fail.

---

## 3. Roles and security

- **Authentication:** HTTP Basic, stateless (no sessions), passwords hashed with BCrypt. Advanced schemes (OAuth, SSO, MFA) are out of scope.
- **Roles:**
  - `ADMIN` — everything under `/api/admin/**`. Seeded at startup.
  - `CUSTOMER` — browsing, booking, cancelling, history, notifications. Created through `POST /api/auth/register`, which can only ever create customers.
- `/api/**` requires a login except `POST /api/auth/register`. Sending wrong credentials gets `401`, even on the register call.
- A customer only ever sees and changes their **own** bookings and notifications (other people's bookings return `403`).
- The user id used for bookings always comes from the login, never from the request body.

---

## 4. API

All request and response bodies are JSON. Times are ISO local date-times (`2030-01-01T18:00:00`). Errors look like
`{"status":409,"error":"Conflict","message":"Seat 12 is not available","timestamp":"..."}`.

### Public
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a customer (`username`, `password` of 6–100 characters) |

### Customer (any logged-in user)
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/cities` | List cities |
| GET | `/api/cities/{cityId}/theatres` | Theatres in a city |
| GET | `/api/movies?title=&cityId=` | Search movies showing in a city (partial, case-insensitive title) |
| GET | `/api/shows?cityId=&theatreId=&movieId=&date=&page=&size=` | Upcoming, non-cancelled shows, earliest first, with available-seat count. All filters optional |
| GET | `/api/shows/{showId}` | One show with its available-seat count |
| GET | `/api/shows/{showId}/seats` | Full seat map (`A1`, `A2`, … with status) |
| GET | `/api/shows/{showId}/seats/available` | Only the free seats (empty for a cancelled show) |
| POST | `/api/bookings` | **Hold** seats: `{showId, seatIds[]}` → booking in `CREATED`, held for 5 minutes |
| POST | `/api/bookings/{id}/confirm` | Pay and confirm: `{paymentType: CARD\|UPI, discountCode?}` |
| GET | `/api/bookings/{id}/refund-preview` | What you would get back if you cancelled now, and under which policy |
| POST | `/api/bookings/{id}/cancel` | Cancel a confirmed booking → `{"cancelled": true}` |
| GET | `/api/bookings?status=&page=&size=` | My booking history, newest first |
| GET | `/api/bookings/{id}` | One of my bookings |
| GET | `/api/notifications?page=&size=` | My notifications, newest first (withdrawn reminders hidden) |

Paging: `page` starts at 0 (default 0), `size` is 1–100 (default 20); anything else is a `400`.

### Admin (`ADMIN` only, under `/api/admin`)
| Method | Path | Purpose |
|---|---|---|
| POST, GET, PUT, DELETE | `/cities`, `/cities/{id}` | Manage cities |
| POST, GET, PUT, DELETE | `/theatres`, `/theatres/{id}` | Manage theatres (`GET ?cityId=`) |
| POST, GET, PUT, DELETE | `/screens`, `/screens/{id}` | Manage screens and their seat layout: `rows` (1–26) × `columns` (1–50) (`GET ?theatreId=`) |
| POST, GET, PUT, DELETE | `/movies`, `/movies/{id}` | Manage movies (`id` optional; a UUID is generated if omitted) |
| POST, GET | `/shows` | Schedule a show (seats are generated from the screen layout); list shows (`GET ?screenId=`) |
| PUT | `/shows/{id}/pricing` | Change base price and pricing tier |
| PUT | `/shows/{id}/refund-policy` | Give the show its own refund policy (`{"refundPolicyId": null}` goes back to the default) |
| POST | `/shows/{id}/cancel` | Cancel a show: full refunds, expire holds, free seats, notify customers |
| POST, GET, PUT, DELETE | `/discount-codes`, `/discount-codes/{id}` | Manage discount codes |
| POST, GET, PUT, DELETE | `/refund-policies`, `/refund-policies/{id}` | Manage refund policies |
| POST | `/refund-policies/{id}/default` | Make a policy the default |

### Error mapping
| Situation | Status |
|---|---|
| Invalid input, blank fields, bad JSON, bad paging, invalid discount code | `400` |
| Not logged in / wrong credentials | `401` |
| Wrong role, or someone else's booking | `403` |
| Show, seat, booking, city… not found | `404` |
| Seat already taken or held, duplicate name/code, item still in use | `409` |
| Wrong state: booking not `CREATED`/`CONFIRMED`, hold lapsed, show started or cancelled | `409` |
| Could not get a row lock in time, or lost a deadlock (retry; the response has `Retry-After: 1`) | `503` |

---

## 5. How it works

### Booking lifecycle
```
                 hold seats            confirm (payment ok)          cancel
  (nothing) ───────────────► CREATED ─────────────────────► CONFIRMED ─────────► CANCELLED
                               │  │                                                (refund)
              hold lapses ─────┘  └── payment fails ──► PAYMENT_FAILED
                    ▼
                 EXPIRED
```
1. **Hold** (`POST /api/bookings`): the seats become `RESERVED` and a booking is created in `CREATED` with an expiry 5 minutes ahead. The price is calculated with the show's pricing tier, and the refund policy in force is copied onto the booking.
2. **Confirm**: checks the booking is still `CREATED`, the hold has not lapsed and the show is still on; applies the discount code if one is given; takes the payment; on success the seats become `BOOKED`.
3. **Cancel**: only for `CONFIRMED` bookings and only before the show starts. The refund policy decides the refund, the payment is marked `REFUNDED` and the seats are freed.

### No double allocation (concurrency)
Correctness comes from the database, not from in-memory locks, so it holds across several application instances too:

- **Seats:** the requested seats are read with `SELECT … FOR UPDATE`, **ordered by id**. A second request for the same seat waits, then sees it is already `RESERVED` and gets `409`. Locking in a fixed order means overlapping requests cannot deadlock.
- **Bookings:** confirm and cancel lock the booking row, so parallel confirms charge once and parallel cancels refund once.
- **Discount codes:** the code row is locked while it is checked and its use counted, so a single-use code is redeemed once.
- **Shows:** creating a booking takes a *shared* lock on the show and cancelling a show takes an *exclusive* one, so a booking cannot slip in while the show is being cancelled.
- **Screens:** scheduling a show locks the screen, so two admins cannot create overlapping shows.
- **Lock timeout:** a request waits at most 5 seconds (`app.db.lock-timeout`, applied as Postgres `lock_timeout` on every connection) for a lock another request holds. After that, and also if it loses a deadlock, it fails with **`503` + `Retry-After: 1`** and nothing is half-done, so clients simply retry. Without this, requests queued on a hot seat would each hold a database connection until the pool ran dry.
- The approach relies on PostgreSQL's default `READ COMMITTED` isolation: a request that waited for a lock re-reads the row and sees the winner's change.

### Hold expiry
A hold lasts **5 minutes**. It is released in three ways: the seats of a show are freed **just before a new booking is created for that show** (so a lapsed hold never blocks anyone), a scheduled sweep runs **every 30 seconds** across all shows, and confirming a lapsed hold is rejected regardless.

### Pricing
Set per show as a tier plus a base price per seat (Strategy pattern):
`REGULAR` = base × seats, `PREMIUM` = × 1.5, `WEEKEND` = × 1.5. Amounts use `BigDecimal`, 2 decimals, rounded half-up.

### Discount codes
`PERCENT` (above 0, up to 100) or `FLAT` (above 0), with a validity window, optional maximum number of uses and an on/off switch. Sent with the confirm request, case-insensitive. An unknown, inactive, expired or used-up code returns `400` and the booking stays `CREATED`, so the customer can retry. The discount can never exceed the price. If the payment fails the discount is undone and the use is not counted. Refunds are calculated on **what was actually paid**.

### Refund policies
A policy is a list of tiers: cancelling at least *N* hours before the show refunds *X* %.
Example `Flexible`: `48h → 100 %`, `12h → 50 %`, `0h → 0 %`.
Rules enforced: unique hours, a tier at `0` hours (so every cancellation is covered), and the refund may not *increase* as the show gets closer.
- One policy is the **default**; a show may have its own. The seeded default `Standard` gives a full refund 24 hours or more ahead and nothing after.
- **Each booking copies the policy's tiers when it is created**, so editing a policy later never changes what an existing booking will get.
- The default policy and policies in use cannot be deleted.

### Admin cancels a show
In one transaction: the show is marked cancelled; every `CONFIRMED` booking is cancelled with a **full refund of what was paid** (the refund policy is deliberately ignored — the theatre cancelled, not the customer); every `CREATED` hold is expired; all seats are freed; each customer is notified. Cancelled shows disappear from browsing and cannot be booked.

### Notifications (never block the booking flow)
- A notification is saved in the **same transaction** as the booking change, so a rolled-back request sends nothing (an outbox).
- Delivery happens **after commit, on a small separate thread pool**, so requests never wait for it.
- Delivery is **claim → send → record**: a short transaction locks the row and marks it `SENDING`; the provider is then called with **no transaction, row lock or database connection held**; a second short transaction records `SENT`, or a failure. A slow provider therefore cannot tie up the database.
- A failed delivery goes back to `PENDING` and is retried, up to 3 attempts, then `FAILED`. A sweep every 15 seconds delivers anything due or still pending, and also takes over a `SENDING` notification whose worker died (claimed more than 2 minutes ago). Delivery is therefore **at-least-once**: after a crash in that narrow window a message could be sent twice.
- Types: `CONFIRMATION`, `PAYMENT_FAILED`, `CANCELLATION` (also used when the admin cancels a show) and `REMINDER`.
- **Reminders** are scheduled when a booking is confirmed if the show is more than one hour away, and are due one hour before it starts. They are stored in the database, so they survive a restart, and are withdrawn if the booking or the show is cancelled.
- The "provider" only writes a log line (`[NOTIFY] …`); customers can read theirs through `GET /api/notifications`.

### Code layout
```
config/         security, async, scheduling, clock, startup seeders
constants/      Constants (numbers, paths, limits) and Queries (all JPQL)
controller/     REST controllers (thin: map DTOs, call services)
dto/            request / response records
entity/         JPA entities with their state-changing behaviour
enums/          statuses and types
exception/      exception types and the global error handler
notification/   sender interface, async dispatcher, events
repository/     Spring Data repositories (queries come from Queries)
security/       user details for HTTP Basic
service/        business logic and transactions
strategy/       pricing, payment and refund strategies
util/           paging helper
```
Design notes: constructor injection everywhere; one injected `Clock` for all time (no direct `now()` calls); all queries and constants in one place each.

---

## 6. Assumptions and scoping decisions

**Users and access**
- A user is either `ADMIN` or `CUSTOMER`. Only customers can self-register; the admin is created from configuration. There is no password reset, e-mail verification or account deletion.
- Bookings store the user id but have no foreign key to the users table.

**Catalogue**
- Seats belong to a **show**, not a screen. They are generated (`A1`, `A2`, … row letters A–Z) when the show is created from the screen's rows × columns. Changing a screen's layout later only affects shows created afterwards.
- A show ends when the movie ends (`start + movie duration`). Two shows on one screen may not overlap; there is no cleaning buffer between them.
- Catalogue items that are still referenced cannot be deleted (`409`). Shows are never deleted, only cancelled, so booking history stays intact.
- The pricing tier is chosen by the admin per show; the system does not detect weekends by itself. Tiers and their multipliers are fixed in code, and only base price and tier are configurable.
- One currency; no taxes or booking fees.

**Booking**
- One booking covers **one show** and one or more seats; there is no limit on the number of seats.
- The hold time (5 minutes) is a constant, not a setting.
- Only `CONFIRMED` bookings can be cancelled, and not once the show has started.
- Users can only see their own bookings; admins have no "view all bookings" API.

**Payment**
- There is **no real payment gateway**. `CARD` and `UPI` are strategies that record a successful payment for the payable amount. The "payment failed" path (booking → `PAYMENT_FAILED`, seats released, notification sent) is implemented and unit-tested, but nothing triggers it yet; a real provider integration would.
- Refunds are recorded instantly by marking the payment `REFUNDED`; no money actually moves.

**Discounts and refunds**
- One discount code per booking, applied when confirming, not when holding.
- A cancelled booking does **not** give its discount-code use back; there is no per-customer limit.
- Refund tiers are whole hours and whole percentages. Bookings that predate refund policies fall back to "full refund 24 hours or more ahead".

**Notifications**
- Delivery is simulated with a log line; a real e-mail/SMS sender only needs to implement `NotificationSender`.
- Reminders can be up to ~15 seconds late (sweep interval). There is no notification when an unpaid hold simply lapses. Delivery is at-least-once (see above).

**Technical**
- All times are server-local `LocalDateTime` values without a time zone; clients are assumed to be in the server's zone.
- Passwords in the seeded admin default are for development only.
- `ddl-auto=update` and SQL logging are development settings.

---

## 7. Not done / known limitations

- No real payment provider, e-mail/SMS provider or database migrations.
- The dashboard-style admin views (all bookings, revenue) are not built.
- `GET /api/shows` counts available seats with one query per show (bounded by the page size of at most 100).
- Expiry and reminders rely on the in-process scheduler. With several application instances every instance runs the sweeps; this is safe (the row locks make it idempotent) but not efficient.
- No rate limiting or request auditing.
