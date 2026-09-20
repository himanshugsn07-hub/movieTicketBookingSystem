# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state
I am building a movie ticket booking system in Spring Boot.
I want to build it step by step

Rules for what you give me:
1. Give class and interface definitions with fields and annotations only.
2. Every method has a signature and an empty body. Use a single line "throw new UnsupportedOperationException("TODO");" as the body, or an empty body for void methods.
3. Add a one line comment above each method saying what it should do.
4. Do not write business logic, queries or tests until I ask.
5. Keep explanations short. Do only the step I ask for and wait for my next instruction.


Start by giving me only the project structure and the entity and enum skeletons. 


Domain model

class City:
    - id: int
    - name: string
    - theatres: List<Theatre>

class Theatre:
    - id: int
    - name: string
    - city: City
    - screens: List<Screen>

class Screen:
    - id: int
    - name: string
    - theatre: Theatre
    - rows: int
    - columns: int
    - shows: List<Show>

class Movie:
    - id: string
    - title: string
    - language: string
    - durationMin: int
    - genre: string

class Show:
    - id: int
    - screen: Screen
    - movie: Movie
    - seats: List<Seat>
    - basePrice: BigDecimal
    - pricingTier: PricingTier
    - cancelled: boolean
    - startTime: DateTime
    - endTime: DateTime

    + hasStarted() : boolean
    + isCancelled() : boolean
    + getAvailableSeats() : List<Seat>

class Seat:
    - id: int
    - status: SeatStatus

    + getId() : int
    + isAvailable() : boolean
    + reserve() : boolean
    + book() : void
    + release() : void

class Booking:
    - confirmationId: string
    - userId: int
    - show: Show
    - seats: List<Seat>
    - createdAt: DateTime
    - bookingStatus: BookingStatus
    - amount: BigDecimal
    - refundAmount: BigDecimal
    - expiresAt: DateTime
    - payment: Payment

    + confirm(payment: Payment) : void
    + markPaymentFailed() : void
    + expire() : void
    + cancel(refundAmount: BigDecimal) : void
    - requireStatus(expected: BookingStatus) : void

class Payment:
    - id: string
    - booking: Booking
    - paymentType: PaymentType
    - amount: BigDecimal
    - status: PaymentStatus

    + markRefunded() : void


Enums
enum SeatStatus: AVAILABLE, RESERVED, BOOKED
enum BookingStatus: CREATED, CONFIRMED, CANCELLED, PAYMENT_FAILED, EXPIRED
enum PaymentStatus: SUCCESS, FAILED, REFUNDED
enum PaymentType: CARD, UPI
enum PricingTier: REGULAR, PREMIUM, WEEKEND


Services

class BookingService:
    - HOLD_DURATION: Duration = 5 minutes
    - bookingRepo: BookingRepository
    - movieRepo: MovieRepository
    - showRepo: ShowRepository
    - seatRepo: SeatRepository
    - paymentStrategyFactory: PaymentStrategyFactory
    - pricingStrategyFactory: PricingStrategyFactory
    - refundPolicy: RefundPolicy

    + searchMovies(title: string, cityId: int) : List<Movie>
    + getAvailableSeats(showId: int) : List<Seat>
    + createBooking(userId: int, showId: int, seatIds: List<Integer>) : Booking
    + confirmBooking(userId: int, confirmationId: string, paymentType: PaymentType) : Booking
    + cancelBooking(userId: int, confirmationId: string) : boolean

class BookingExpiryService:
    - bookingRepo: BookingRepository

    + releaseExpiredBookings() : void


Strategies

interface PricingStrategy:
    + calculatePrice(show: Show, seats: List<Seat>) : BigDecimal

class RegularPricing implements PricingStrategy      (basePrice x seat count)
class PremiumPricing implements PricingStrategy       (multiplier 1.5)
class WeekendPricing implements PricingStrategy       (multiplier 1.5)

class PricingStrategyFactory:
    + forTier(pricingTier: PricingTier) : PricingStrategy

interface PaymentStrategy:
    + pay(booking: Booking) : Payment

class CardPayment implements PaymentStrategy
class UpiPayment implements PaymentStrategy

class PaymentStrategyFactory:
    + forPayment(paymentType: PaymentType) : PaymentStrategy

interface RefundPolicy:
    + calculateRefund(booking: Booking, now: DateTime) : BigDecimal

class TimeBasedRefundPolicy implements RefundPolicy
    - fullRefundWindow: Duration = 24 hours


Repositories (Spring Data JPA interfaces)

interface BookingRepository:
    + save(booking: Booking) : Booking
    + findById(confirmationId: string) : Optional<Booking>
    + findExpiredBookings(now: DateTime) : List<Booking> (status CREATED and expiresAt before now)

interface MovieRepository:
    + findByTitleInCity(title: string, cityId: int) : List<Movie>

interface ShowRepository:
    + findById(showId: int) : Optional<Show>

interface SeatRepository:
    + findByShowIdAndIdIn(showId: int, seatIds: List<Integer>) : List<Seat> (ordered by id, with lock)
    + findByShowIdAndStatus(showId: int, status: SeatStatus) : List<Seat>


Exceptions
All extend BookingSystemException (unchecked):
ValidationException (bad input)
NotFoundException (show, seat or booking not found)
ConflictException
SeatUnavailableException extends ConflictException (seat already taken or held)
InvalidStateException (wrong status, show started, hold expired)
UnauthorizedException (not the booking's owner)

## Stack

- Java 21, Spring Boot 4.1.1, Maven 
- Base package: `com.himanshu.movieTicketBookingSystem`

