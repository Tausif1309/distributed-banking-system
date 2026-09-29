\# Instructions for Codex



\## Project

This is a Java 21, Spring Boot microservices banking application.



Services:

\- Auth Service: 8081

\- User Service: 8082

\- Transaction Service: 8083

\- Notification Service: 8084



Infrastructure:

\- MySQL databases

\- Redis for idempotency

\- Kafka for asynchronous transaction events



\## User background

The developer is an MTech Cybersecurity fresher preparing for software engineering interviews.



Explain important changes in simple, detailed, beginner-friendly language.

Explain what the code does, why it is needed, how it works, and relevant interview questions.



\## Strict security restrictions

\- Never access MySQL, MySQL Workbench, or any database.

\- Never access Postman or its collections.

\- Never request or inspect credentials, passwords, JWTs, API keys, tokens, or secrets.

\- Never print secrets in terminal output or logs.

\- Never execute database migrations.

\- Never modify, delete, reset, or seed real database data.

\- Never make API calls that create users, transfer money, or modify account balances.

\- Never run destructive commands.

\- Never access files outside the approved project workspace.

\- Ask for approval before modifying files or executing commands.



\## Development rules

\- Preserve the existing microservice architecture.

\- Do not rewrite the entire project unnecessarily.

\- Prefer small, incremental changes.

\- Use DTOs, service layers, repositories, validation, and global exception handling.

\- Use BigDecimal for monetary values.

\- Maintain JWT authentication and role-based authorization.

\- Keep secrets in environment variables.

\- Do not hardcode credentials.

\- Use Flyway for schema versioning, but do not execute migrations.

\- Do not change existing database schemas without explaining the impact first.

\- Use simple HTML, CSS, and vanilla JavaScript for the frontend.

\- Avoid introducing unnecessary dependencies.



\## Testing rules

\- Do not connect to real databases or infrastructure.

\- Do not run integration tests that may modify real data.

\- Prefer isolated unit tests and mocked dependencies.

\- Explain any test that requires external services before proposing it.



\## Workflow

Before making changes:

1\. Explain the issue.

2\. Identify affected files.

3\. Propose the smallest safe change.

4\. Explain possible risks.

5\. Wait for approval.



After changes:

1\. Summarize modified files.

2\. Explain the implementation.

3\. State which tests were run.

4\. Clearly distinguish verified behavior from assumptions.

5\. Provide interview questions related to the implementation.

