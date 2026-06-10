# DB Schema Audit

## Scope

This audit covers `stockops-api-server` Flyway migrations and Java/JPA usage as of the
public-mirror cleanup (2026-06-10).

## Changes Applied In Current Plan

| Object | Action | Migration | Reason |
|---|---|---|---|
| `sensor_readings` | Drop | `V39__remove_sensor_measurement_storage.sql` | Live measurements are no longer stored in bulk; real-time values are viewed client-side (admin-web MQTT/session). |
| `sensor_latest` | Drop | `V39__remove_sensor_measurement_storage.sql` | Latest-value projection removed with bulk measurement storage. |
| `environment_alerts.resolved_at` | Add (nullable) | `V40__environment_alert_lifecycle.sql` | `environment_alerts` becomes the event store. An alert is ACTIVE while `resolved_at IS NULL AND acknowledged = false`; it auto-resolves when the sensor normalizes or is cleared when an admin acknowledges. Drives the dashboard normal/warning/danger view. |
| `sms_send_history` | Create | `V41__sms_send_history.sql` | Backfills the missing table for the `SmsSendHistory` JPA entity (schema mismatch fix). |
| `environment_alerts.acknowledgement_note` | Add (nullable) | `V42__environment_alert_acknowledgement_note.sql` | Stores the administrator handling/action note recorded when acknowledging an alert via `POST /api/v1/environment/alerts/{id}/acknowledge`. |

## Event Model (replaces measurement storage)

- The API still ingests live MQTT telemetry, but only to **evaluate thresholds**, not to store readings.
- A `WARNING`/`CRITICAL` reported status opens (or escalates) one active `environment_alerts` row per sensor.
- A normal reading sets `resolved_at` (auto-resolve). An administrator acknowledgement sets `acknowledged = true`.
- The dashboard computes danger/warning/normal counts from currently active alerts.

## Retained Tables

| Table | Reason |
|---|---|
| `sensor_devices` | Sensor master data for admin configuration and topic mapping. |
| `environment_controllers` | Controller master data and command target mapping. |
| `environment_alerts` | Operational event store (now with lifecycle). |
| `controller_commands` | Operational command history. |
| `analytics.ai_forecast_snapshots` | Used by AI recommendation flow. |
| `analytics.ai_reorder_recommendations` | Used by admin AI recommendation flow. |
| `analytics.ai_suggestions` | Used by admin AI suggestion workflow. |
| `analytics.ai_suggestion_audits` | Used by AI suggestion audit workflow. |
| `analytics.ai_model_evaluations` | Used by stockops-ai-module evaluation history. |

## Resolved Mismatch

| Object | Finding | Fix |
|---|---|---|
| `sms_send_history` | `SmsSendHistory` entity + repository existed, but no `CREATE TABLE` migration was present (`ddl-auto: none`, so the table was genuinely absent). | Added `V41__sms_send_history.sql` matching the entity's columns and `uk_sms_history_message_id` constraint. |

## Defer To Separate Decision

- Removing AI recommendation and suggestion tables.
- Removing analytics read-model tables.
- Removing notification, escalation, or webhook features.
