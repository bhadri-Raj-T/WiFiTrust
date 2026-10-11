# WiFiTrustFusion M2 Collector Requirements

## R1

Acquisition is foreground controlled.

## R2

Every scan request has a unique request identifier within its session.

## R3

Monotonic elapsed time is used for freshness and timing relationships.

## R4

Native Android ScanResult timestamps are retained.

## R5

Observation timestamp states include:

FIRST_SEEN

ADVANCED

REPEATED

REGRESSED

UNTRACKABLE

## R6

seen_after_request is recorded independently of timestamp state.

## R7

Request-level data and observation-level data remain separate.

## R8

Missing or failed evidence remains explicitly unavailable.

## R9

The M2 collector performs no risk scoring.

## R10

Raw SSID/BSSID evidence is treated as sensitive local experimental data.

## R11

Sessions are uniquely identifiable across multiple collection devices.

## R12

Every session records:

COLLECTOR_VERSION

PROTOCOL_VERSION

OBS_SCHEMA_VERSION

application version

device alias

device model

Android version

API level

## R13

Scan interval is an explicit protocol parameter and is recorded with actual timing.

## R14

Collector failures and completion reasons are retained as evidence.

## R15

Experiment storage uses app-specific storage and does not require broad storage permission.

## Freeze Scope

These requirements define the M2 acquisition architecture.

They do not freeze:

scan interval

experimental protocol values

dataset size

WFAI thresholds

risk weights

risk thresholds

confidence thresholds

review thresholds

crowd weights

final fusion parameters
