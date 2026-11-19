# Cashfree Payment Integration

A backend payment integration application built using **Java, Spring Boot, PostgreSQL, and the Cashfree Payment Gateway SDK**.

The project demonstrates how to integrate a third-party payment gateway into a Spring Boot application, manage payment orders, handle payment sessions, and maintain transaction records.

## 🚀 Tech Stack

- **Language:** Java
- **Backend Framework:** Spring Boot
- **Build Tool:** Maven
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Payment Gateway:** Cashfree Payments
- **Validation:** Jakarta Bean Validation
- **Libraries:** Lombok

## 📌 Project Overview

The application is designed to manage the payment lifecycle by maintaining internal payment orders and communicating with the Cashfree Payment Gateway.

The application maintains separate entities for payment orders and payment transactions to support multiple payment attempts for a single order.

## 🏗️ Architecture

The application follows a layered architecture:

```text
src/main/java/com/seema/cashfree_payment_integration
│
├── client
│   └── CashfreeClient
│
├── config
│   └── CashfreeConfig
│
├── controller
│   ├── PaymentController
│   └── WebhookController
│
├── dto
│   ├── CreatePaymentRequest
│   └── CreatePaymentResponse
│
├── entity
│   ├── PaymentOrder
│   └── PaymentTransaction
│
├── exception
│   └── GlobalExceptionHandler
│
├── properties
│   └── CashfreeProperties
│
├── repository
│   ├── PaymentOrderRepository
│   └── PaymentTransactionRepository
│
├── service
│   ├── PaymentService
│   └── WebhookService
│
└── util
```

## ✨ Features

### Implemented

- Create payment orders through a REST API.
- Validate incoming payment requests.
- Generate unique order IDs using UUID.
- Persist payment orders in PostgreSQL.
- Integrate the Cashfree Java SDK.
- Configure Cashfree credentials and environment through application properties.
- Separate payment order and transaction entities.
- Use constructor-based dependency injection.
- Return structured API responses using DTOs.

### Planned

- Complete Cashfree order creation and payment session handling.
- Persist Cashfree payment session details in the transaction table.
- Integrate Cashfree webhooks for asynchronous payment updates.
- Implement payment status verification.
- Handle API failures and safe retries.
- Implement idempotency to prevent duplicate order creation.
- Add comprehensive unit and integration tests.
- Add structured logging and improved exception handling.

## 🔄 Payment Flow

The intended payment lifecycle:

1. Client sends a payment creation request.
2. Spring Boot validates the incoming request.
3. Application creates and saves a payment order in PostgreSQL.
4. Application sends an order creation request to Cashfree.
5. Cashfree returns the order details and payment session ID.
6. Application stores transaction information.
7. Payment session ID is returned to the client.
8. Customer completes payment through Cashfree Checkout.
9. Cashfree webhook updates the payment status after verification.

## 🔌 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments` | Create a payment order |

### Create Payment Request

**Endpoint:** `POST /api/payments`

Example request:

```json
{
  "orderAmount": 1000.00,
  "orderCurrency": "INR",
  "customerId": "customer_001",
  "customerPhone": "9876543210",
  "customerEmail": "customer@example.com"
}
```

## 🛠️ Getting Started

### Prerequisites

- Java 21 or compatible configured JDK
- Maven
- PostgreSQL
- Cashfree Sandbox account and API credentials

### 1. Clone the repository

```bash
git clone https://github.com/seemaamola/cashfree-payment-integration.git

cd cashfree-payment-integration
```

### 2. Configure the database

Create a PostgreSQL database:

```sql
CREATE DATABASE cashfreeDB;
```

### 3. Configure environment variables

Set the following environment variables:

```bash
DB_USERNAME=postgres
DB_PASSWORD=your_database_password

CASHFREE_CLIENT_ID=your_cashfree_client_id
CASHFREE_CLIENT_SECRET=your_cashfree_client_secret
```

### 4. Configure application.yml

Update your database and Cashfree configuration using environment variables.

### 5. Run the application

```bash
./mvnw spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

## 🔐 Security

- API credentials are supplied through environment variables.
- Sensitive credentials should never be committed to GitHub.
- Cashfree Sandbox is used for development and testing.
- Payment status should be updated only after verifying trusted Cashfree payment confirmation.

## 📚 Learning Objectives

This project explores practical backend engineering concepts:

- REST API development using Spring Boot.
- Layered architecture and separation of concerns.
- Constructor-based dependency injection.
- Database relationships using JPA and Hibernate.
- Third-party API integration.
- Payment order and transaction lifecycle management.
- API validation and exception handling.
- Idempotency and reliable payment processing.

## 👩‍💻 Author

**Seema Amola**

GitHub: [@seemaamola](https://github.com/seemaamola)
