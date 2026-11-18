Title: PAN validator crashes with NullPointerException when input is null

Environment: Java 25, Eclipse IDE, Windows 10 (seeded defect in a practice project)

Steps to reproduce:
1. Open PanValidator.java and delete the line: if (pan == null) return false;
2. Run PanValidatorTest (it includes check(null, false))

Expected result: isValidPan(null) returns false
Actual result: program stops with
NullPointerException: Cannot invoke "String.matches(String)" because "pan" is null
at PanValidator.isValidPan(PanValidator.java:4)
The checks after it do not run.

Severity: High. The program crashes, and in a real KYC form an empty field would crash the whole check.
Priority: High. Empty input is common, so it should be fixed before release.

Fix: restore the null check at the start of isValidPan.
Status: Fixed and re-tested. All 13 checks pass.