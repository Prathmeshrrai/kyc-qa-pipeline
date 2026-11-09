# KYC Onboarding QA Pipeline

A small banking KYC (Know Your Customer) onboarding app, built as a practice target for a complete QA pipeline: risk-based test design, API testing, UI automation and CI/CD.

> **Status:** Day 1, project just started. Nothing below is built yet; this README describes the plan and will be updated as each part is completed and run.

## Goal

Show how a tester designs and automates quality checks around a real-world-style workflow. The project is based on my experience handling KYC verification at a bank, so the test ideas come from real onboarding risks.

## The app under test

A simple onboarding flow:
- Register and log in
- Submit ID details (fake numbers only)
- Verification status: pending, approved or rejected
- Account tiers with transfer limits

The app is scaffolded with AI assistance. I review and explain every part, because the focus of this project is testing, not app development.

## KYC risks driving the test design

1. Name-spelling mismatch across documents
2. Duplicate customer
3. Expired document
4. Wrong document-number format
5. Too many verification attempts
6. Resubmission after rejection
7. Personal data masked in logs and API responses

## Scope

### In scope (version 1, target: 4 weeks)
- Requirements, risk register, test plan, test cases and requirements traceability matrix (RTM)
- API tests in Postman (Basic, API key, JWT/Bearer, OAuth; 401/403/expired-token cases)
- SQL data checks
- UI automation with Java, Selenium, TestNG and Page Object Model
- CI with GitHub Actions and Allure reports
- Real bug reports and a short quality report

### Later
- A few Playwright tests
- Optional AI-assisted bug-report drafting (reviewed by me)
- Testing public and open-source apps, with responsible bug reporting

## Tech plan

| Area | Tool |
|---|---|
| App | Node.js, Express, SQLite |
| API testing | Postman |
| UI automation | Java, Selenium, TestNG |
| Reports | Allure |
| CI/CD | GitHub Actions |
| Tracking | Jira-style bug reports |

## Important note

All data in this project is fake. No real personal or identity information is used anywhere.

## Progress log

Daily learning notes will be added to a learning log in this repo as work is completed.

## Author

Prathmesh Rai, aspiring QA / SDET. Building this as part of a 100-day learning journey.
