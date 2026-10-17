# WiFiTrustFusion G1 Collector Freeze

## Freeze status

COLLECTOR_V1
PROTOCOL_V1
OBS_SCHEMA_V1

G1 candidate validation has passed on:

- Samsung SM-J710F, Android 8.1, API 27
- Xiaomi Redmi Note 8 Pro DEV01, Android 9, API 28

## Acquisition protocol

Requested post-completion interval:

35 seconds

D1 pilot session definition:

10 completed request cycles.

Expected duration:

Approximately 5.5 to 6 minutes depending on Android scan completion timing.

The collector-generated session_id is canonical.

External experimental metadata must reference the exact collector-generated session_id.

## Collector evidence files

Each clean session produces:

- metadata.csv
- requests.csv
- observations.csv
- events.csv
- session_completion.csv

Raw SSID and BSSID values may exist in observations.csv.

Raw experimental evidence must remain private and must not be committed to Git.

## Observation schema

Persisted observations.csv fields:

1. observation_id
2. session_id
3. request_id
4. observed_utc_iso
5. observed_elapsed_us
6. ssid
7. bssid
8. rssi_dbm
9. frequency_mhz
10. channel_number
11. channel_width
12. capabilities
13. scan_timestamp_us
14. scan_age_ms
15. timestamp_state
16. seen_after_request

Timestamp states:

- FIRST_SEEN
- ADVANCED
- REPEATED
- REGRESSED
- UNTRACKABLE

Connected-state information is NOT implemented in OBS_SCHEMA_V1.

## Session metadata

Collector-generated metadata includes:

- session_id
- collector_version
- protocol_version
- observation_schema_version
- app_version_name
- app_version_code
- device_alias
- manufacturer
- model
- device_codename
- android_release
- api_level
- started_utc_iso
- started_elapsed_us
- requested_scan_interval_ms
- run_label

Experimental labels remain in the external Data Track manifest:

- network_code
- location_code
- position_code
- orientation_code
- environment_condition
- scenario_id
- ground_truth
- operator_id
- operator_notes

## Acquisition freeze rule

Once COLLECTOR_V1 is handed to the Data Acquisition Track for D1:

Acquisition-affecting logic MUST NOT be changed during that D1 collection round.

Any acquisition-affecting modification requires:

- COLLECTOR_V2
- compatibility review
- new APK
- new checksum
- explicit decision on whether affected data must be recollected

## Known limitations

- Android Wi-Fi scanning is subject to platform throttling.
- Broadcast results may indicate results were not updated.
- ScanResult timestamps and freshness states are retained to identify repeated or stale observations.
- Captive portal, WFAI and risk scoring are not part of COLLECTOR_V1.
- Room storage is not part of COLLECTOR_V1.
- Network quality and security-risk scoring are not part of COLLECTOR_V1.
- Raw SSID/BSSID evidence is sensitive and must remain private.
- Xiaomi/MIUI may reject ADB installation while the device is locked.
- DEV01 compatibility validation required the device to be unlocked for APK installation.

## Validated devices

J7:

Samsung SM-J710F
Android 8.1
API 27

DEV01:

Xiaomi Redmi Note 8 Pro
Codename begoniain
Android 9
API 28
Build type user

## Data Track

D0 reconciliation commit:

fe07ed676d3f11241c9a6f87b3085aa3877f0ff6

Protocol candidate was reconciled before G1 freeze.

D1 must not begin before explicit G1 GO.
