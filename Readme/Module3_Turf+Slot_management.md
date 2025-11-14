This module covers everything an owner can do on the platform:

Add turf

Update turf

Upload/delete images

View own turfs

Delete turf

Create/update schedule

Generate slots

View slots

Update slot status

Authentication is via HTTP-only cookies, and all APIs require OWNER role.

1. Base URL
   /api/owners/turfs

2. Turf Management APIs
   2.1 Add Turf

POST

/api/owners/turfs/addturf


Request (multipart/form-data):

name: string (required)
address: string (required)
location: string (required)
city: string (required)
description: string
amenities: string
turfType: string (required)
available: boolean (required)
images: list of image files


Response (200):

{
"id": 12,
"name": "Green Arena",
"address": "MG Road",
"location": "Near Park",
"city": "Pune",
"description": "Premium 7-a-side turf",
"amenities": "Washroom, Drinking Water",
"turfType": "7-a-side",
"available": true,
"imageUrls": [
"https://cdn.com/turfs/12/1.jpg",
"https://cdn.com/turfs/12/2.jpg"
],
"createdAt": "2025-11-12T10:20:15"
}

2.2 Update Turf Info

PUT

/api/owners/turfs/update/{turfId}


Request Body (JSON):

{
"name": "Green Arena Updated",
"address": "MG Road",
"location": "Near Metro",
"city": "Pune",
"description": "Updated desc",
"amenities": "Washroom, Parking",
"turfType": "5-a-side",
"available": true
}


Response (200):

{
"id": 12,
"name": "Green Arena Updated",
"address": "MG Road",
"location": "Near Metro",
"city": "Pune",
"description": "Updated desc",
"amenities": "Washroom, Parking",
"turfType": "5-a-side",
"available": true,
"imageUrls": [...],
"updatedAt": "2025-11-12T10:40:22",
"message": "Turf updated successfully"
}

2.3 Add Images to Turf

POST

/api/owners/turfs/{turfId}/images


Request:

images: List<MultipartFile>


Response:

{
"turfId": 12,
"message": "Images added successfully",
"changedCount": 2,
"totalImages": 5
}

2.4 Delete Image

DELETE

/api/owners/turfs/{turfId}/images/{imageId}


Response:

{
"turfId": 12,
"message": "Image deleted successfully",
"changedCount": 1,
"totalImages": 4
}

2.5 Delete Turf

DELETE

/api/owners/turfs/delete/{turfId}


Response:

{
"message": "Turf deleted successfully",
"turfId": "12"
}

2.6 Get Owner’s Turfs

GET

/api/owners/turfs/me


Response:

[
{
"id": 12,
"name": "Green Arena",
"city": "Pune",
"available": true,
"imageUrls": [...],
"createdAt": "2025-10-10T08:11:23"
}
]

2.7 Get Single Turf by ID

GET

/api/owners/turfs/me/{turfId}

3. Turf Schedule APIs
   3.1 Create or Update Schedule

POST

/api/owners/turfs/{turfId}/schedule


Request Example:

{
"openTime": "07:00",
"closeTime": "01:00",
"slotDurationMinutes": 60,
"priceSlots": [
{
"startTime": "07:00",
"endTime": "16:00",
"pricePerSlot": 1050
},
{
"startTime": "16:00",
"endTime": "01:00",
"pricePerSlot": 1400
}
]
}


Response:

{
"turfId": 12,
"openTime": "07:00",
"closeTime": "01:00",
"slotDurationMinutes": 60,
"priceSlots": [
{
"startTime": "07:00",
"endTime": "16:00",
"pricePerSlot": 1050
},
{
"startTime": "16:00",
"endTime": "01:00",
"pricePerSlot": 1400
}
],
"message": "Turf Schedule saved successfully"
}

4. Slot Management APIs
   4.1 Generate Slots (7-Day Window)

POST

/api/owners/turfs/slots/generate/{turfId}


Response:

Slots generated successfully for next 7 days

4.2 View Slots for a Given Day

GET

/api/owners/turfs/slots?turfId=12&date=2025-11-10


Response:

[
{
"id": 140,
"date": "2025-11-10",
"startTime": "07:00",
"endTime": "08:00",
"price": 1050,
"status": "AVAILABLE"
},
{
"id": 141,
"date": "2025-11-10",
"startTime": "08:00",
"endTime": "09:00",
"price": 1050,
"status": "UNAVAILABLE"
}
]

4.3 Update Slot Status (Available / Unavailable)

PATCH

/api/owners/turfs/slots/status/{turfId}


Request:

{
"slotIds": [141, 142, 143],
"status": "UNAVAILABLE"
}


Response:

Slot Status updated successfully

5. Error Codes

The backend returns consistent error responses in this format:

{
"error": "Bad Request",
"message": "No slots found for the given date",
"status": 400,
"timestamp": "2025-11-12T09:21:45.102Z"
}

Common Errors
400 — Bad Request

Missing required fields

Invalid status in request (must be AVAILABLE or UNAVAILABLE)

Price slot overlaps in schedule

Image upload invalid

401 — Unauthorized

User not authenticated

Cookie missing or expired

403 — Forbidden

Turf does not belong to this owner

Trying to update BOOKED slots

404 — Not Found

Turf not found

Schedule not found

Slot IDs not found

409 — Conflict

Duplicate images

Slot already exists (unique constraint)

500 — Internal Server Error

Unexpected backend failure