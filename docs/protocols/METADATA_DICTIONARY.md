# WiFiTrustFusion Metadata Dictionary

Status: DRAFT

The following fields are required for controlled WiFiTrustFusion collection.

| Field | Example | Purpose |
|---|---|---|
| device_id | DEV01 | Pseudonymous acquisition-device identifier |
| device_model | Redmi Note 8 Pro | Device model |
| android_version | TBD | Android OS version |
| api_level | TBD | Android API level |
| collector_version | COLLECTOR_V1 | Frozen collector version |
| protocol_version | PROTOCOL_V1 | Collection protocol version |
| schema_version | OBS_SCHEMA_V1 | Observation schema version |
| network_code | NET01 | Pseudonymous network identifier |
| location_code | LOC01 | Controlled collection-location identifier |
| position_code | POS01 | Device-position identifier |
| orientation_code | ORI_N | Device orientation identifier |
| environment_condition | NORMAL | Controlled environment condition |
| scenario_id | N0 | Experimental scenario |
| ground_truth | NORMAL | Controlled experimental label |
| session_id | generated | Unique session identifier |
| start_time_utc | ISO 8601 UTC | Session start |
| end_time_utc | ISO 8601 UTC | Session end |
| operator_id | OP01 | Pseudonymous operator identifier |
| operator_notes | free text | Relevant observation notes |

## Device Identity

Do not store device serial numbers in public Git history.

ADB serial numbers may be retained privately if required for device
management.

Public or sanitized metadata should use identifiers such as:

DEV01  
DEV02  
DEV03

## Network Identity

Raw SSIDs and BSSIDs must not be placed in the public repository.

Use:

NET01  
NET02  
NET03

A private mapping between network codes and real network identifiers may
be maintained outside Git.

## Ground Truth Values

Initial controlled values:

NORMAL  
CONTROLLED_ABNORMAL  
UNDETERMINED

Additional scenario-specific labels may be introduced only through a
protocol revision or documented dataset specification.

## Environment Condition

Initial values may include:

NORMAL  
MOVEMENT  
ORIENTATION_CHANGE  
PEOPLE_NEARBY  
WEAK_SIGNAL  
INTERFERENCE  
NETWORK_CONGESTION  
MULTI_BSSID_NORMAL

These values describe conditions and must not automatically imply
security risk.
