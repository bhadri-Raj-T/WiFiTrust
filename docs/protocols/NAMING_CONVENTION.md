# WiFiTrustFusion Naming Convention

Status: DRAFT

## Device Codes

DEV01  
DEV02  
DEV03

Device codes must remain stable for the lifetime of the dataset.

## Network Codes

NET01  
NET02  
NET03

Raw SSID or BSSID values must not appear in Git-managed filenames.

## Location Codes

LOC01  
LOC02  
LOC03

## Position Codes

POS01  
POS02  
POS03

## Orientation Codes

ORI_N  
ORI_E  
ORI_S  
ORI_W

If exact compass orientation is not controlled, use:

ORI_FIXED  
ORI_VARIABLE

## Operator Codes

OP01  
OP02

## Scenario Families

Normal baseline:

N0 - Normal stable baseline

Normal nuisance conditions:

N1 - Position variation  
N2 - Orientation variation  
N3 - Movement  
N4 - Weak legitimate signal  
N5 - Ordinary interference  
N6 - Network congestion  
N7 - Legitimate multi-BSSID behaviour

Controlled abnormal scenarios will be assigned during D4 and must not be
performed before authorization and protocol approval.

Review scenarios will be assigned during D5.

## Session ID

Use:

YYYYMMDDTHHMMSSZ_DEVxx_NETxx_SCENARIO

Example:

20261011T083000Z_DEV01_NET01_N0

Rules:

- UTC time only
- no spaces
- no colon characters
- one unique ID per collection session
- never reuse a session ID
- session ID must match the corresponding manifest entry

## Version Identifiers

Collector:

COLLECTOR_V1

Protocol:

PROTOCOL_V1

Observation schema:

OBS_SCHEMA_V1

Scoring versions are separate and may later include:

STAT_V1  
TECH_V1  
REVIEW_V1  
CROWD_V1  
FUSION_V1
