Base URL
https://Localhost:8080/api/auth

Endpoints
1. POST /register/customer

Registers a new customer.

Request:

{
"name": "John Doe",
"email": "john@example.com",
"password": "securePassword123"
}


Response (200):


Customer registration successful. Please login to continue.



Errors:

Status	Message
400	Missing or invalid fields
409	Email already exists

2. POST /register/owner

Registers a new turf owner with document upload.

Request (multipart/form-data):

Field	Type	Description
name	string	Owner full name
email	string	Owner’s email
password	string	Account password
subscriptionAmount	number	Subscription plan amount
document	file	PDF/JPG/PNG proof document

Response (200):


Owner registration successful. Your account is pending for approval



Errors:

Status	Message
400	Invalid or missing document
409	Email already exists
500	Document upload failed
3. POST /login

Authenticates a user (customer or owner) and returns a JWT.

Request:

{
"email": "john@example.com",
"password": "securePassword123"
}


Response (200):

{
"role": "CUSTOMER",
"message": "Login successful"
}


Notes:

Token is sent back as HTTP-only cookie in frontend integration.

Valid for 24 hours.

Errors:

Status	Message
400	Invalid or missing credentials
401	Unauthorized (Bad credentials)
403	Account pending or rejected
500	Internal server error
Token Handling

JNote:
The JWT token is not returned in the JSON body.
It is stored securely as an HttpOnly cookie in the browser.
All subsequent requests to protected routes automatically include this cookie.
Frontend cannot and should not access this token manually.

Roles
Role	Description
CUSTOMER	Can browse turfs, book slots, view bookings
OWNER	Can manage owned turfs, view bookings
ADMIN	Approves/rejects owner accounts, manages platform
Account States (for Owners)
Status	Meaning
PENDING	Owner registered, waiting for admin approval
ACTIVE	Owner verified and can manage turfs
REJECTED	Application rejected by admin
Error Response Format (Unified)

All errors follow this structure:

{
{
"error": "Conflict",
"message": "Email already exists",
"status": 409,
"timestamp": "2025-11-05T12:46:08.988379100"
}