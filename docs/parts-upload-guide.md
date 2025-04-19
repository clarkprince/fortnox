# Parts Upload Integration Guide

## Overview

The parts upload system implements a queue-based approach with rate limiting for uploading large numbers of parts to Synchroteam. This guide explains the API endpoints and expected behaviors.

## API Endpoints

### 1. Upload Parts

```http
POST /api/parts/synchroteam/parts/upload
Content-Type: multipart/form-data
Headers:
  - tenant: [tenant-domain]
  - email: [user-email]
```

**Request:**

- Form data with file field named "file"
- File should be CSV/Excel format with part details

**Response:**

```json
{
  "message": "Parts queued for processing. Queue ID: 123. Total parts: 1000"
}
```

### 2. List Synchroteam Parts

```http
GET /api/parts/synchroteam/list
Headers:
  - tenant: [tenant-domain]
Query Parameters:
  - size: (default: 100) Number of items per page
  - page: (default: 1) Page number
```

### 3. List Queued Parts

```http
GET /api/parts/synchroteam/queued
Headers:
  - tenant: [tenant-domain]
Query Parameters:
  - size: (default: 100) Number of items per page
  - page: (default: 1) Page number
```

**Response:**

```json
{
  "content": [
    {
      "queueId": 123,
      "createdAt": "2024-01-20T14:30:00",
      "fileName": "parts-upload.csv",
      "totalParts": 1000,
      "processedParts": 455,
      "failedParts": 10,
      "progress": 45.5,
      "details": [
        {
          "part": {
            "reference": "ABC123",
            "name": "Part Name"
          },
          "status": "COMPLETED|PENDING|FAILED",
          "errorDetails": "Error message if failed",
          "processedAt": "2024-01-20T14:35:00"
        }
      ]
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 1
}
```

### 4. Detailed Status Check

```http
GET /api/parts/synchroteam/upload/status
Headers:
  - tenant: [tenant-domain]
  - email: [user-email]
```

**Response:**

```json
[
  {
    "id": 123,
    "fileName": "parts-upload.csv",
    "progress": 45.5,
    "totalParts": 1000,
    "processedParts": 455,
    "failedParts": 10,
    "createdAt": "2024-01-20 14:30:00",
    "processedAt": "2024-01-20 14:35:00"
  }
]
```

## Implementation Notes

1. **Queue Structure**

   - Parts are queued individually for processing
   - Each part has its own status tracking
   - Queue entries show overall progress and failed parts
   - Detailed error information available per failed part

2. **Rate Limiting**

   - System processes 1000 requests per minute
   - Daily quota is tracked per tenant
   - Frontend should handle rate limit errors gracefully

3. **Status Monitoring**

   - Monitor overall queue progress
   - View individual part processing status
   - Access detailed error information for failed parts
   - Status updates reflect both queue and part-level progress
   - Poll the status endpoint every 30-60 seconds while uploads are pending
   - Show progress percentage: `(processedParts / totalParts) * 100`
   - Display appropriate messages based on status:
     - PENDING: "Waiting to be processed"
     - PROCESSING: "Processing... {progress}%"
     - COMPLETED: "Upload completed"
     - FAILED: Show errorDetails

4. **UI Recommendations**

   - Implement a file upload component with drag-and-drop
   - Show upload progress bar
   - Display queue status in a table/list view
   - Allow filtering by status
   - Add refresh button for status updates
   - Show error messages prominently
   - Enable cancel/retry functionality

5. **Error Handling**
   - Handle network errors gracefully
   - Show meaningful error messages to users
   - Implement retry mechanism for failed uploads
   - Cache upload progress locally if possible

## Examples

### Status Polling Implementation

```javascript
const pollStatus = async (queueId) => {
  const interval = setInterval(async () => {
    const status = await fetchStatus();
    if (status.status === "COMPLETED" || status.status === "FAILED") {
      clearInterval(interval);
    }
    updateUI(status);
  }, 30000); // Poll every 30 seconds
};
```

### Progress Calculation

```javascript
const calculateProgress = (queue) => {
  const progress = (queue.processedParts / queue.totalParts) * 100;
  return progress.toFixed(1);
};
```
