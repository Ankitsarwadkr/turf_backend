Module 4 :Customer Module

✔ Customer Module

✔ Slot Module

✔ Booking Module

✔ Payment Module

✔ Razorpay Integration

---

📘 TurfEra – Customer + Booking + Payment Module (Frontend API Documentation)

This guide explains all public + customer APIs needed to build the frontend for:

Listing turfs

Viewing turf details

Viewing available slots

Creating bookings

Starting Razorpay payment

Verifying payment

Handling payment success/failure flows


All endpoints and response formats are taken directly from your controller + DTO code.


---

1. Public Turf APIs (NO AUTH REQUIRED)


---

1.1 Get all turfs

GET /api/public/turfs

Response:

[
{
"id": 1,
"name": "TurfEra Arena",
"city": "Pune",
"shortAddress": "Baner",
"thumbnailUrl": "/uploads/turfs/t1.jpg",
"startingPrice": 500,
"available": true
}
]

Use this for the homepage turf cards.


---

1.2 Get full turf details

GET /api/public/turfs/{turfId}

Response:

{
"id": 1,
"name": "TurfEra Arena",
"description": "Premium turf",
"amenities": "Parking, Lights",
"turfType": "5v5",
"address": "Baner",
"city": "Pune",
"mapUrl": "...",
"images": ["url1", "url2"],
"openTime": "06:00",
"closeTime": "23:00",
"slotDurationMinutes": 60,
"priceSlots": [
{ "startTime": "06:00", "endTime": "10:00", "pricePerSlot": 600 }
],
"available": true
}

Use this for turf details page.


---

2. Slots API (AUTH REQUIRED – CUSTOMER)


---

2.1 Get available slots

GET /api/customer/turfs/{turfId}/slots?date=YYYY-MM-DD

Example:

/api/customer/turfs/1/slots?date=2025-01-21

Response:

[
{
"id": 101,
"date": "2025-01-21",
"startTime": "07:00",
"endTime": "08:00",
"price": 600
}
]

Use this for the slot selection UI.


---

3. Booking API (AUTH REQUIRED)


---

3.1 Create booking (locks slots for limited time)

POST /api/customer/bookings/create

Request:

{
"turfId": 1,
"slotIds": [101, 102]
}

Response:

{
"bookingId": "BK-001",
"turfId": 1,
"slots": [
{
"slotId": 101,
"date": "2025-01-21",
"startTime": "07:00",
"endTime": "08:00",
"price": 600
}
],
"amount": 600,
"status": "PENDING_PAYMENT",
"turfName": "TurfEra Arena",
"turfCity": "Pune",
"expiredAt": "2025-01-21T14:20:00",
"message": "Booking created. Complete payment before expiry."
}

Frontend must:

Start a countdown timer until expiredAt

Redirect user to payment page



---

4. Payment API (AUTH REQUIRED)


---

4.1 Create Razorpay Order

POST /api/customer/payments/order/{bookingId}

Response:

{
"bookingId": "BK-001",
"razorpayOrderId": "order_Lkj123",
"amount": 60000,
"currency": "INR",
"status": "PENDING",
"paymentId": null
}

Notes for frontend:

amount is already in paise
→ Do NOT multiply again


Use this payload in Razorpay Checkout.


---

4.2 Verify Payment

POST /api/customer/payments/verify

Frontend must send EXACT Razorpay response:

{
"bookingId": "BK-001",
"razorpayOrderId": "order_Lkj123",
"razorpayPaymentId": "pay_ABC123",
"razorpaySignature": "generated_signature"
}

Response:

{
"bookingId": "BK-001",
"paymentStatus": "SUCCESS",
"message": "Payment verified, booking confirmed.",
"paymentId": "pay_ABC123"
}

If failed:

{
"bookingId": "BK-001",
"paymentStatus": "FAILED",
"message": "Payment verification failed."
}


---

5. Razorpay Frontend Integration Flow


---

Step 1: Create booking

Call: POST /api/customer/bookings/create

Save:

bookingId

amount



---

Step 2: Create Razorpay order

Call: POST /api/customer/payments/order/{bookingId}

Save:

razorpayOrderId

amount (in paise)



---

Step 3: Run Razorpay Checkout

const options = {
key: "<RAZORPAY_KEY>",
order_id: order.razorpayOrderId,
amount: order.amount,  
currency: "INR",

handler: function (res) {
verifyPayment(res);
}
};

new Razorpay(options).open();


---

Step 4: Verify Payment

POST /api/customer/payments/verify

Body:

{
"bookingId": bookingId,
"razorpayOrderId": res.razorpay_order_id,
"razorpayPaymentId": res.razorpay_payment_id,
"razorpaySignature": res.razorpay_signature
}

If SUCCESS → Redirect to /payment-success
If FAILED → Redirect to /payment-failed


---

6. Webhook (Handled by Backend Only)

Frontend NEVER uses this.

Backend auto-processes:

payment.captured

payment.refunded.*


Serves as backup confirmation.


---

7. Status Codes

Code	Meaning

200	Success
400	Invalid input
401	Not logged in
403	Access another user’s booking
404	Booking/slot not found
409	Slot already booked



---

8. Emails (Automatic)

Backend automatically emails the customer on:

Booking confirmed

Payment failed

Booking expired


Frontend does nothing here.


---

9. Summary for Frontend Developer

Build the following UI screens:

1. Turf list


2. Turf details


3. Slot selection page


4. Review booking page


5. Razorpay checkout page


6. Payment success page


7. Payment failed page


8. Booking expired page



Mandatory checks:

Always send JWT to customer endpoints

Handle expired bookings

Show timer until expiredAt

Use order.amount directly in Razorpay

Always call verify API after checkout



---

If you want, I can also generate:

✔ Postman collection
✔ Swagger API docs
✔ Sequence diagram
✔ Frontend folder structure
✔ Payment UI wireframe

Just tell me.