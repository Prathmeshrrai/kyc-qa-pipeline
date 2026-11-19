# KYC Practice App

A small banking KYC onboarding backend that I use for testing practice. It is a practice app with seeded defects. It is not a real product, and any defect I log against it is a "seeded defect".

Purpose: to practise risk-based test design, API testing (Postman, then REST Assured and TestNG) and bug reporting on a KYC flow (register, submit documents, verify, status, transfers with tier limits).

The correct behaviour is written in API_CONTRACT.md.

## Run it
Java project in Eclipse, class KycApp, Run As Java Application. It starts on http://localhost:8080 (check http://localhost:8080/api/health).

## Status
In progress. Test design is done (risk list and test cases in the docs folder). API testing is next.
