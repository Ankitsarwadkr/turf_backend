
Admin Payout APIs — Frontend Integration Guide

This document explains how the admin payout system works, which APIs exist, when to call them, and how the frontend must behave.

⚠️ This system is strictly state-driven.
Frontend must not invent states, calculate money, or bypass rules.

This README covers admin payout lifecycle only.
Owner dashboard (“My Earnings”) is handled separately.


---

Base URL

/api/admin/payouts

All APIs require ADMIN role.


---

1. Core Concepts (Frontend must understand)

1.1 PayoutBatch

Weekly payout group (Mon → Sun earnings)

Created automatically by cron (Wednesday 2 AM)

Frontend cannot create or edit batches


1.2 PayoutExecution

One execution per owner per batch

Represents a single payout attempt

This is where admin actions happen (paid / failed / retry)


1.3 OwnerEarning

Ledger entry (already settled money)

Read-only

Frontend never touches this directly



---

2. Batch Lifecycle (Read-only for frontend)

Batch status is derived automatically from executions.

Status	Meaning

CREATED	Batch generated, not approved
APPROVED	Ready for execution
PROCESSING	Some executions pending
FAILED	At least one execution failed
COMPLETED	All executions paid


❌ Frontend must never manually set batch status.


---

3. API Overview

Available Admin APIs

Get all batches

Get batch details

Approve batch

List executions per batch

View execution detail

Mark execution paid

Mark execution failed

Retry execution



---

4. Get All Batches

Endpoint

GET /batches

Response

[
{
"batchId": 3,
"weekStart": "2025-12-09",
"weekEnd": "2025-12-15",
"totalAmount": 5400.00,
"totalOwners": 2,
"status": "FAILED",
"createdAt": "2025-12-16T02:00:00"
}
]

UI Usage

Batch listing page

Clicking a row opens batch details



---

5. Get Batch Details (Owner breakdown)

Endpoint

GET /batches/{batchId}

Response

{
"batchId": 3,
"weekStart": "2025-12-09",
"weekEnd": "2025-12-15",
"totalAmount": 5400.00,
"totalOwners": 2,
"status": "FAILED",
"createdAt": "2025-12-16T02:00:00",
"owners": [
{
"ownerId": 1,
"ownerName": "Ankit Sports",
"totalAmount": 2700.00,
"earnings": []
}
]
}

UI Usage

Batch summary

Owner-level totals

Link to execution list



---

6. Approve Batch

Endpoint

POST /batches/{batchId}/approve

Rules

Allowed only when status = CREATED

Moves batch into execution phase


UI Rules

Show Approve button only if status = CREATED



---

7. Get Executions by Batch (Primary admin screen)

Endpoint

GET /batches/{batchId}/executions

Response

[
{
"executionId": 2,
"ownerId": 1,
"ownerName": "Ankit Sports",
"amount": 2700.00,
"status": "FAILED",
"retryCount": 3,
"createdAt": "2025-12-16T11:25:01"
}
]

UI Rules

This is the main admin action screen.

Show:

Owner name

Amount

Status badge

Retry count


Clicking a row → execution detail page

❌ Do not allow actions directly from this list.


---

8. Get Execution Detail (Single execution page)

Endpoint

GET /executions/{executionId}

Response

{
"executionId": 2,
"batchId": 3,
"ownerId": 1,
"ownerName": "Ankit Sports",
"amount": 2700.00,
"status": "FAILED",

"failureCode": "TRANSFER_TIMEOUT",
"failureReason": "Server Down",
"failedAt": "2025-12-16T16:30:56",
"failedBy": 5,

"retryCount": 3,
"lastRetryAt": "2025-12-16T16:30:46",

"paidAt": null,
"paidBy": null,
"paymentReference": null,

"createdAt": "2025-12-16T11:25:01",
"lastActivity": "2025-12-16T16:30:56",

"failureHistory": [
{
"attemptNumber": 1,
"failureCode": "TRANSFER_TIMEOUT",
"failureReason": "Server Down",
"failedAt": "2025-12-16T11:41:28",
"failedBy": 5
}
]
}

UI Rules

All admin actions happen here

Show failure timeline

Show retry history

Disable buttons based on status



---

9. Mark Execution as PAID

Endpoint

POST /executions/{executionId}/paid

Request

{
"paymentReference": "NEFT-REF-12345"
}

Response

{
"executionId": 1,
"status": "PAID",
"paidBy": 5,
"paidAt": "2025-12-16T17:10:00",
"paymentReference": "NEFT-REF-12345"
}

UI Rules

Show button only if status = PENDING

After success → refresh execution + batch



---

10. Mark Execution as FAILED

Endpoint

POST /executions/{executionId}/failed

Request

{
"failureCode": "TRANSFER_TIMEOUT",
"failureReason": "Bank server down"
}

Rules

Allowed only if status = PENDING

Execution moves to FAILED

Batch auto-updates



---

11. Retry Execution

Endpoint

POST /executions/{executionId}/retry

Request

{
"note": "Retry after bank outage"
}

Response

{
"executionId": 2,
"status": "PENDING",
"retryCount": 3,
"lastRetryAt": "2025-12-16T16:30:46"
}

UI Rules

Show retry button only if:

status = FAILED

retryCount < MAX_RETRY_LIMIT


⚠️ Never allow retry directly from list view
→ always confirm on detail page


---

12. Critical UI Invariants (DO NOT BREAK)

❌ Never assume batch status
❌ Never modify payout amounts
❌ Never retry PAID executions
❌ Never mark FAILED → PAID without retry
❌ Never exceed retry limit

Backend enforces this — UI must respect it.


---

13. What Frontend Must NOT Do

Do NOT calculate payouts

Do NOT infer batch state

Do NOT hide failures

Do NOT reattempt paid executions



---

Final Notes

This document fully covers:

Admin payout lifecycle

Execution safety

Retry and failure handling

UI-safe integration boundaries


Owner dashboard (“My Earnings”) and reconciliation tooling are handled separately.


---

✅ Status

Approved for frontend implementation


---

If you want next:

Owner Dashboard (My Earnings) README

Admin UI wireframe rules

Error UX patterns


Say the word.