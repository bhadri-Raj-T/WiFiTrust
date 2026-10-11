# WiFiTrustFusion M2 Reference Collector Audit

## Source

Reference prototype:

WiFiTrust

The original project remains untouched.

## Decision

The old project is treated as a feasibility reference, not as the implementation base for WiFiTrustFusion.

## Proven concepts retained

- Foreground-only acquisition.
- Explicit scan request tracking.
- SCAN_RESULTS_AVAILABLE_ACTION handling.
- EXTRA_RESULTS_UPDATED recording.
- Scan request timeout handling.
- UTC event time.
- Monotonic elapsed time.
- Native ScanResult timestamp.
- Scan-result age calculation.
- FIRST_SEEN state.
- ADVANCED state.
- REPEATED state.
- REGRESSED state.
- UNTRACKABLE state.
- seen_after_request.
- Separate request records.
- Separate observation records.
- Device/environment metadata.
- Immediate evidence flushing.

## Designs to replace

- Monolithic MainActivity architecture.
- Public Downloads as the primary experiment storage location.
- WRITE_EXTERNAL_STORAGE runtime permission.
- Hard-coded E0 / E0-F experiment labels.
- J7-only permission handling.
- Fixed assumptions about scan frequency.
- Timestamp-only session identifiers.
- Direct coupling between UI, scanning and logging.

## M2 boundary

M2 implements measurement only.

M2 does not implement:

- WFAI.
- Security Risk.
- Network Quality.
- Review Trust.
- Reviewer Reputation.
- Crowd Risk.
- Final Risk Fusion.
- Backend synchronization.
- Room database.

## Experimental principle

Collector failures, stale results, repeated results and missing evidence must remain observable data.

They must not be silently converted into normal or safe evidence.

## Version state

COLLECTOR_DEV

PROTOCOL_DEV

OBS_SCHEMA_DEV

These identifiers remain development identifiers until G1 Collector Freeze.
