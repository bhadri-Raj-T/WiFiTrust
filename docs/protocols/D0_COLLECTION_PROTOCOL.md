# WiFiTrustFusion D0 Collection Protocol

Protocol status: DRAFT  
Protocol identifier: PROTOCOL_V1_DRAFT

## Purpose

This protocol defines how WiFiTrustFusion acquisition sessions are prepared,
identified, collected, transferred, checked and handed over.

No large-scale data collection is permitted until the G1 Collector Freeze
and D1 Pilot acceptance are complete.

## Project Roles

### Master Control Room
Responsible for:
- architecture
- scope
- version freezes
- merge gates
- GO / NO-GO decisions
- cross-team coordination

### Technical Track
Responsible for:
- Android collector
- database
- evidence-quality logic
- statistical engine
- technical tests
- scoring
- review system
- backend
- final integration

### Data Acquisition Track
Responsible for:
- collection protocol
- controlled acquisition
- metadata
- dataset manifests
- quality analysis
- rejected-session documentation
- sanitized handovers

## Data Stages

D0 - Protocol Preparation  
D1 - Pilot Collection  
D2 - Legitimate Baseline Collection  
D3 - Normal Nuisance Variation  
D4 - Controlled Abnormal Scenarios  
D5 - Controlled Review Scenarios

## Gate Sequence

G0 - Architecture and Protocol Planning  
G1 - Collector Freeze  
G2 - Dataset V1 Acceptance  
G3 - Statistical Freeze  
G4 - Technical Scenario Freeze  
G5 - Review Dataset Freeze  
G6 - Final Integration Freeze

## G1 Requirements

Before D1 collection begins, the following must be frozen:

- COLLECTOR_Vx
- PROTOCOL_Vx
- OBS_SCHEMA_Vx
- frozen APK or equivalent release artifact
- Git commit or release reference

The data-acquisition operator must not modify collector code during an
active collection round.

## Core Data Integrity Rules

1. Never overwrite raw measurements.
2. Never manually edit measurements to improve results.
3. Preserve failed sessions.
4. Record rejection reasons.
5. Do not silently mix collector versions.
6. Do not silently mix schema versions.
7. Raw SSIDs and BSSIDs must not be committed to GitHub.
8. Every accepted session must be traceable to its collection metadata.
9. Missing evidence must be reported as missing, not interpreted as safe.
10. Ambiguous observations must remain ambiguous.

## Ground Truth Principle

The operator records the controlled experimental condition.

The operator must not change labels because the measured data does not
match the expected hypothesis.

## Safety Boundary

Controlled abnormal scenarios must use only:

- team-owned equipment
- team-controlled networks
- explicitly authorized environments

No intrusive testing may be performed against third-party networks.

## Collection State Model

Every session has one logical state:

RAW  
ACCEPTED  
REJECTED

RAW: Original acquisition output.

ACCEPTED: Passed defined data-quality checks.

REJECTED: Retained but excluded from the accepted analysis dataset.

## D1 Pilot Rule

Large-scale acquisition must not begin until the D1 pilot has been
reviewed and accepted by the Master Control Room.

## Versioning

Protocol changes that alter acquisition procedure require a new protocol
version.

Collector changes that alter acquisition behaviour require a new collector
version.

Observation field changes require a new observation-schema version.
