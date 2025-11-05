Module 2 – Admin Verification
Base URL

https://localhost:8080/api/admin
Overview
This module allows the Admin to:

Log in to the system.

View all turf owners whose accounts are pending verification.

Approve or reject owners based on document validation.

Notify owners via automated email after approval or rejection.

Endpoints
1. GET /owners/pending
   Description:
   Fetch all turf owners whose status is PENDING.

Authorization:

Requires ROLE_ADMIN.

Response (200):

json
Copy code
[
{
"id": 12,
"name": "Rahul Mehta",
"email": "rahul@example.com",
"subscriptionAmount": 499.0,
"createdAt": "2025-11-02T14:25:00",
"documents": [
{
"fileName": "gst_certificate.pdf",
"filePath": "/uploads/owners/12/gst_certificate.pdf",
"uploadedAt": "2025-11-02T14:30:00"
}
]
}
]
Errors:

Status	Message
401	Unauthorized – Token missing or invalid
403	Forbidden – Not an admin
404	No pending owners found

2. PUT /owners/{id}/approve
   Description:
   Approve a pending owner. The owner’s subscriptionStatus changes to ACTIVE.

Authorization:

Requires ROLE_ADMIN.

Response (200):

json
Copy code
{
"message": "Owner approved successfully.",
"status": "ACTIVE"
}
Email Triggered:

makefile
Copy code
Subject: Owner Account Approved
Body:
Hello [OwnerName],
Your documents have been verified successfully.
You can now access your owner dashboard and add turfs.
Errors:

Status	Message
400	Invalid owner ID
401	Unauthorized
403	Access denied (non-admin)
404	Owner not found

3. PUT /owners/{id}/reject
   Description:
   Rejects a pending owner with a reason.
   The owner’s subscriptionStatus changes to REJECTED.

Authorization:

Requires ROLE_ADMIN.

Request Body:

json
Copy code
{
"reason": "Submitted document is unclear"
}
Response (200):

json
Copy code
{
"message": "Owner rejected successfully.",
"status": "REJECTED"
}
Email Triggered:

makefile
Copy code
Subject: Owner Account Rejected
Body:
Hello [OwnerName],
Your registration request has been rejected.
Reason: [Provided reason]
Please contact support for clarification.
Errors:

Status	Message
400	Reason field missing
401	Unauthorized
403	Access denied
404	Owner not found

Email Notifications
Action	Trigger	Subject	Content
Registration	Owner signs up	“Owner Registration Under Verification”	Confirms submission
Approval	Admin approves owner	“Owner Account Approved”	Grants access to platform
Rejection	Admin rejects owner	“Owner Account Rejected”	Includes rejection reason

Roles and Access
Role	Description	Access
ADMIN	Can approve/reject owners	 Full access
OWNER	Registers, uploads documents	No access
CUSTOMER	Can only browse/book turfs	 No access

Error Response Format (Unified)
All errors follow this structure:

json
Copy code
{
"status": 403,
"error": "Forbidden",
"message": "Access denied: admin only",
"timestamp": "2025-11-05T18:30:00"
}
✅ 