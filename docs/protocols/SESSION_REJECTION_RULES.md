# WiFiTrustFusion Session Rejection Rules

Status: DRAFT

A rejected session must never be deleted solely because it failed
quality checks.

It remains part of the acquisition audit trail.

## Possible Rejection Reasons

REJ_VERSION_MISMATCH  
Collector, protocol or schema version does not match the active collection round.

REJ_METADATA_MISSING  
Required metadata is missing or invalid.

REJ_SESSION_ID_INVALID  
Session identifier does not follow the frozen naming convention.

REJ_FILE_MISSING  
One or more mandatory output files are absent.

REJ_FILE_CORRUPT  
Output cannot be parsed or appears incomplete or corrupted.

REJ_TIMESTAMP_INVALID  
Required timestamps are absent, malformed or internally inconsistent.

REJ_EXCESSIVE_STALE_DATA  
The session exceeds the later-defined stale-observation acceptance threshold.

REJ_DUPLICATE_SESSION  
The same session data has already been registered.

REJ_WRONG_SCENARIO  
The actual controlled condition does not match the declared scenario.

REJ_INTERRUPTED  
Collection was interrupted in a way that prevents the session from satisfying protocol requirements.

REJ_DEVICE_MISMATCH  
The acquisition device does not match the declared device identifier.

REJ_OPERATOR_ERROR  
The operator deviated from the frozen collection procedure.

REJ_UNKNOWN  
The session cannot currently be validated.

Use REJ_UNKNOWN only when no more specific reason is justified.

## Important Rule

A measurement must not be rejected simply because it does not support the
expected hypothesis.

Quality rejection concerns acquisition validity, not whether the result
looks convenient.

## Required Rejection Record

For every rejected session record:

- session_id
- rejection_code
- rejection_reason
- reviewer or operator
- date
- collector_version
- protocol_version
- schema_version

## Reconsideration

A rejected session may later be reclassified only through a documented,
reproducible decision.

The original rejection record must remain available.
