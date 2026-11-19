# KYC Practice API: contract and business rules

This is the **specification**. When the app does something different from what is written here, that is a defect. Read it before you test.

Base URL: `http://localhost:8080`
Format: JSON in, JSON out. Errors look like `{"error": "message"}`.
Auth: log in first, then send `Authorization: Bearer <token>` on every other call.

Users (password for all: `Pass@123`): `customer1`, `customer2` (role customer), `officer1` (role officer).

## Endpoints

| Method | Path | Who | What it does |
|---|---|---|---|
| GET | /api/health | anyone | Returns `{"status":"UP"}` |
| POST | /api/reset | anyone | Clears all data (use before a test run) |
| POST | /api/login | anyone | Body: `username`, `password`. Returns `token`, `role` |
| POST | /api/customers | customer | Register. Body: `fullName`, `dob` (yyyy-MM-dd), `mobile`, `email`, `pan` |
| POST | /api/customers/{id}/documents | owner | Submit a document. Body: `type` (PAN, AADHAAR, PASSPORT), `number`, `nameOnDocument`, `expiry` (passport only) |
| POST | /api/customers/{id}/verify | owner | Run the automatic check |
| GET | /api/customers/{id}/status | owner or officer | Status, attempts, tier, reasons |
| GET | /api/customers/{id} | owner or officer | Full profile with masked document numbers |
| POST | /api/customers/{id}/review | officer | Body: `decision` (APPROVE or REJECT) |
| POST | /api/transfers | owner | Body: `customerId`, `toAccount`, `amount` |

## Status codes

200 OK, 201 Created, 400 Bad Request (invalid input), 401 Unauthorized (no or wrong token), 403 Forbidden (not allowed), 404 Not Found, 409 Conflict (duplicate or wrong state), 422 Unprocessable (over the limit), 423 Locked.

## Statuses

`PENDING` then `APPROVED`, or `REJECTED`, or `LOCKED`.

## Business rules (the correct behaviour)

1. **Registration:** all five fields are required. Customer must be at least 18. Mobile is 10 digits starting with 6 to 9. PAN is 5 capital letters, 4 digits, 1 capital letter. Returns 201 with `id`, `status` PENDING and a masked PAN.
2. **Duplicates at registration:** PAN, mobile and email must be unique across customers. Email is compared ignoring case and spaces. A duplicate returns 409 and no customer is created.
3. **Duplicate Aadhaar:** the same Aadhaar number cannot be submitted for two different customers. The second one returns 409.
4. **Document number formats:** PAN as above and it must equal the PAN used at registration. Aadhaar is exactly 12 digits and the first digit is 2 to 9 (spaces are allowed and ignored). Passport is one letter (A to Z except Q, X, Z) followed by 7 digits. A wrong format returns 400.
5. **Expiry (passport):** required, format yyyy-MM-dd. A passport is valid **through** its expiry date. Expiring today is valid. Expiring yesterday returns 400 "Document expired".
6. **Name matching:** ignoring capital letters and extra spaces, the **full** name on every submitted document must equal the registered full name. Otherwise verification fails with reason `NAME_MISMATCH`.
7. **Verification:** a PAN document is required (400 if missing). If every document passes, status becomes APPROVED. If any fails, the status becomes REJECTED and the attempt counter goes up by one.
8. **Attempt limit:** after the **3rd** failed attempt the status becomes LOCKED. A locked customer gets 423 on documents and verify.
9. **Resubmission:** a REJECTED customer can submit documents again. A new document **replaces** the old one of the same type and the status goes back to PENDING, so verification can run again.
10. **Masking:** every document number and the PAN is shown as `X` characters except the last 4, in every response.
11. **Authorisation:** a customer can only read or change their own record. Another customer gets 403. Officers can read any record. Only officers can call review.
12. **Tiers and transfers:** TIER1 means a PAN document only, with a limit of 50,000 per transfer. TIER2 means PAN and Aadhaar, with a limit of 2,00,000. A transfer needs status APPROVED (otherwise 403), an amount above 0 (otherwise 400) and a 9 to 18 digit `toAccount`. An amount over the limit returns 422. An amount equal to the limit is allowed.
13. **Officer review:** an officer can set PENDING or REJECTED to APPROVED or REJECTED. APPROVED returns 409 and LOCKED returns 423.

## Note on your 38 test cases
Some expected results in `02-kyc-test-cases.md` were written before these rules existed (for example TC-02 expects "manual review"). Here a name mismatch means REJECTED. Update the expected result of any test case that disagrees with these rules **before** you run it.
