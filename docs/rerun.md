# Activity Rerun Documentation

## Overview

The rerun functionality allows you to reprocess previously failed or incomplete activities. It supports the following processes:

- Jobs
- Invoices
- Articles
- Customers

## Usage

Send a POST request to `/api/activities/rerun` with:

- A list of activity IDs in the request body
- Required `tenant` header specifying the tenant domain

## Example

```http
POST /api/activities/rerun
Content-Type: application/json
tenant: example-tenant

[123, 124, 125]
```

## Process Types

Each activity type is processed differently:

- `jobs`: Reprocesses Synchroteam jobs
- `invoices`: Reprocesses Synchroteam invoices
- `articles`: Reprocesses Fortnox articles
- `customers`: Reprocesses Fortnox customers

## Error Handling

Failed reruns are logged with error messages but don't stop the processing of remaining activities.
