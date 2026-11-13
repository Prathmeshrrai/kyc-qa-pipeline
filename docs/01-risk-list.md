# KYC Risk List

Risk-based test design for the KYC onboarding project. Higher-priority risks are tested first.

| ID | KYC risk | Example | Impact | Priority |
|----|----------|---------|--------|----------|
| R1 | Name spelling mismatch across documents | PAN says "Prathmesh", Aadhaar says "Prathamesh" | | |
| R2 | Duplicate customer | Same person registers twice with different emails | | |
| R3 | Expired document | Passport or ID past its expiry date is accepted | | |
| R4 | Wrong document-number format | PAN with 9 characters accepted | | |
| R5 | Too many verification attempts | Unlimited retries with wrong details | | |
| R6 | Resubmission after rejection | Rejected customer resubmits and gets approved without a re-check | | |
| R7 | Personal data exposed in logs or responses | Full document number shown in API response or log | | |

## What I will test first and why
1.
2.
3.
