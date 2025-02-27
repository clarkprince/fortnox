# Fortnox Integration Service

A Spring Boot REST API service that integrates Fortnox with other systems.

## Overview

This service provides RESTful endpoints for managing users, tenants, settings, and various business operations including customers, parts, jobs, and invoices.

## Features

- User authentication and management
- Multi-tenant support
- Settings management
- Integration with external systems:
  - Fortnox API integration
  - Synchroteam integration
- Process monitoring
- Activity logging

## API Endpoints

### Authentication

- POST `/api/users/login` - User authentication
- GET `/api/users` - List all users
- POST `/api/users` - Create new user
- PUT `/api/users/{id}` - Update user
- DELETE `/api/users/{id}` - Delete user

### Tenants

- GET `/api/tenants` - List all tenants
- GET `/api/tenants/{id}` - Get tenant details
- POST `/api/tenants` - Create new tenant
- PUT `/api/tenants/{id}` - Update tenant
- DELETE `/api/tenants/{id}` - Delete tenant
- GET `/api/tenants/activate` - Activate tenant

### Business Operations

- Customers API (`/api/customers`)
- Parts API (`/api/parts`)
- Jobs API (`/api/jobs`)
- Invoices API (`/api/invoices`)
- Settings API (`/api/settings`)
- Process Monitoring (`/api/process-monitors`)
- Activity Logging (`/api/activities`)

## Technical Stack

- Java
- Spring Boot
- Spring Security
- JWT Authentication
- JPA/Hibernate
- BCrypt Password Encryption

## Getting Started

1. Clone the repository
2. Configure application properties
3. Run using Maven: `mvn spring-boot:run`

## Security

The API uses JWT tokens for authentication. Include the JWT token in the Authorization header for protected endpoints.

## Headers

Most endpoints require the following headers:

- `Authorization: Bearer {jwt-token}`
- `tenant: {tenant-domain}` (for tenant-specific operations)
