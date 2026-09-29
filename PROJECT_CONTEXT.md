\# Banking Microservices Project Context



\## Project Overview

A Java Spring Boot microservices banking application developed as an MTech cybersecurity/fresher portfolio project.



The application supports admin-managed user creation, authentication, account management, money transfers, transaction history, profile change approvals, and asynchronous notifications.



\## Technology Stack

\- Java 21

\- Spring Boot 4.1.1

\- Spring Security

\- JWT authentication

\- MySQL

\- Spring Data JPA

\- Flyway

\- Redis

\- Apache Kafka

\- Maven

\- Docker Compose

\- HTML, CSS, vanilla JavaScript (planned frontend)



\## Microservices



\### 1. Auth Service

Port: 8081  

Database: banking\_auth



Responsibilities:

\- User authentication

\- JWT generation and validation

\- BCrypt password hashing

\- ADMIN and USER roles

\- Account status management

\- Admin-controlled credential creation



\### 2. User Service

Port: 8082  

Database: banking\_user



Responsibilities:

\- User and account management

\- Admin creates users and initial balances

\- User profile retrieval

\- Profile change requests

\- Admin approval or rejection of profile changes

\- Internal account debit and credit operations



\### 3. Transaction Service

Port: 8083  

Database: banking\_transaction



Responsibilities:

\- Money transfer orchestration

\- Transaction history

\- Idempotency handling using Redis

\- Transaction records and status management

\- Publishing transaction completion events to Kafka



\### 4. Notification Service

Port: 8084  

Database: banking\_notification



Responsibilities:

\- Consume transaction events from Kafka

\- Persist notification records

\- Avoid duplicate notification processing



\## Infrastructure

Docker Compose runs:

\- Redis on localhost:6379

\- Kafka on localhost:9092



MySQL is installed and running natively on Windows.



Spring Boot services are run through IntelliJ IDEA.



\## Current Implementation

The four Spring Boot services already exist.



Implemented areas include:

\- JWT authentication

\- Role-based authorization

\- Admin-controlled user creation

\- Account management

\- Profile change request workflow

\- Transaction orchestration

\- Redis-based idempotency

\- Kafka transaction events

\- Flyway database migrations

\- Global exception handling



Do not assume every feature is production-ready. Audit the actual source code before making claims.



\## Known Engineering Concerns

\- Cross-service money transfer consistency needs review.

\- Idempotency must be safe during retries, timeouts, and service crashes.

\- Kafka event publishing and database updates need reliability review.

\- Security and authorization rules need review.

\- Automated tests need improvement.

\- API Gateway and frontend are planned work.



\## Development Priorities

1\. Audit existing code without modifying it.

2\. Identify security and correctness issues.

3\. Prioritize safe transaction processing.

4\. Improve unit and integration test coverage using isolated test infrastructure.

5\. Complete the frontend using plain HTML, CSS, and JavaScript.

6\. Prepare interview explanations for each important component.



\## Important Constraints

Never access real MySQL databases, Workbench, Postman, credentials, tokens, or secrets.



Never perform real transfers or alter existing banking data.



All changes must be incremental, reviewed, and explained before implementation.

