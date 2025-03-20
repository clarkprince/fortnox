# User Management API Documentation

## Create User

Creates a new user in the system.

### Endpoint

```http
POST /api/users/create
Content-Type: application/json
```

### Request Body

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "securePassword123",
  "tenant": "company-name"
}
```

### Required Fields

- `firstName` (string): User's first name
- `lastName` (string): User's last name
- `email` (string): Unique email address
- `password` (string): User password
- `tenant` (string): Company tenant domain (will be converted to lowercase)

### Optional Fields

- `role` (string): User role (defaults to "1" if not provided)
- `active` (boolean): User status (defaults to true)

### Response

#### Success (201 Created)

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "tenant": "company-name",
  "role": "1",
  "active": true,
  "createdAt": "2024-01-20T10:30:00"
}
```

#### Error Responses

##### Bad Request (400)

```json
{
  "message": "All fields (email, firstName, lastName, password, tenant) are required"
}
```

##### Invalid Tenant (400)

```json
{
  "message": "Invalid tenant specified"
}
```

##### Email Conflict (409)

```json
{
  "message": "Email already exists"
}
```

### Notes

1. Password will be encrypted before storage
2. Email must be unique across the system
3. Tenant domain will be validated against existing tenants
4. CreatedAt timestamp is automatically set
5. Response will not include the password field
