# WiFiTrustFusion Data Handover Checklist

Status: DRAFT

This checklist must be completed before a dataset is handed from the
Data Acquisition Track to the Technical Track or Master Control Room.

## Version Identity

- [ ] Collector version recorded
- [ ] Protocol version recorded
- [ ] Observation schema version recorded
- [ ] Git commit or release reference recorded

## Session Freeze

- [ ] Session list frozen
- [ ] No sessions added after freeze without a new handover revision
- [ ] Every session has a unique session ID
- [ ] Every session has an ACCEPTED or REJECTED state

## Dataset Summary

Record:

- total sessions
- accepted sessions
- rejected sessions
- total requests
- total observations
- scenario counts
- device counts
- network-code counts

## Data Quality

- [ ] Required metadata checked
- [ ] Missing-data report generated
- [ ] Timestamp validity checked
- [ ] Duplicate observations checked
- [ ] Stale observations checked
- [ ] Interrupted sessions documented
- [ ] Rejected sessions documented with reason codes
- [ ] Collector-version mixing checked
- [ ] Schema-version mixing checked

## Privacy and Sanitization

- [ ] Raw SSIDs removed from Git-managed data
- [ ] Raw BSSIDs removed from Git-managed data
- [ ] ADB serial numbers removed from Git-managed data
- [ ] Network identifiers replaced with network codes
- [ ] Device identifiers replaced with device codes
- [ ] Sensitive private mappings stored outside Git

## Reproducibility

- [ ] Manifest committed
- [ ] Quality report committed
- [ ] Protocol reference included
- [ ] Known issues documented
- [ ] Excluded or rejected sessions remain traceable

## Handover Record

The final handover must include:

- collector version
- protocol version
- schema version
- session manifest
- number of sessions
- number of observations
- scenario counts
- device counts
- missing-data report
- stale or duplicate timestamp report
- rejected-session report
- known issues
- Git commit reference

## Gate Decision

Possible outcomes:

GO  
GO WITH DOCUMENTED LIMITATIONS  
NO-GO

Only the Master Control Room may issue the final merge-gate decision.
