# Weekly Narrative Report — Week 2 (May 11 to May 15)

Project Overview

The Firefighter Robot Controller project continues to integrate embedded firmware, sensor aggregation, and operator interfaces to support remote firefighting exercises. The second-week scope emphasized robust telemetry, command acknowledgement, and improving the reliability of motor and sensor subsystems across the ESP32 and Arduino components, alongside iterative enhancements to the Android controller.


Weekly Activities

Between May 11 and May 15 the team completed the sensor hub firmware that implements basic filtering and periodic telemetry bursts to the ESP32. A simple command acknowledgement protocol was added so the mobile app can confirm receipt of critical motor commands. The Android control UI received layout and state-management improvements to better present live sensor values and connection status. Bench testing included repeated motor actuation cycles to validate the updated enable/disable logic and a set of end-to-end tests that exercised command, acknowledgement, and telemetry round trips over the local network.

| Date | Day | Daily accomplishment | No. of working hours |
|---|---|---|---|
| 2026-05-11 | Monday | Completed sensor hub firmware with basic filtering and telemetry bursts. | 7 |
| 2026-05-12 | Tuesday | Implemented command acknowledgement protocol for critical motor commands. | 6 |
| 2026-05-13 | Wednesday | Improved Android UI layout and state management for live telemetry. | 6 |
| 2026-05-14 | Thursday | Performed bench testing with repeated motor actuation cycles. | 7 |
| 2026-05-15 | Friday | Added watchdog and reconnection logic; debounced UI inputs. | 6 |

Challenges Encountered

This week’s primary challenges included intermittent Wi‑Fi stability in one test environment, occasional sensor drift under extended runtime, and race conditions exposed by concurrent command inputs from the mobile UI. Stabilization steps included adding a watchdog timer and reconnection logic on the ESP32, applying a low-pass filter to sensor readings to mitigate drift, and serializing command handling in firmware to eliminate race conditions. The Android app was updated to debounce control inputs to prevent conflicting commands during transitional states.

Learnings & Insights

The team reinforced the necessity of end-to-end testing for distributed systems where embedded devices and mobile clients interact. Practical lessons included implementing lightweight reliability patterns (heartbeat, acknowledgements, reconnection), improving firmware robustness with watchdogs, and applying simple digital filtering techniques on sensor data. The Android changes clarified how UI state and network state should be decoupled to avoid user input-induced errors.

Next Steps

Planned work for the next interval includes extended stress testing under varied environmental conditions, adding logging hooks to capture edge-case failures, and preparing a draft of the user operational manual to capture setup and troubleshooting steps. The team will also prioritize battery and power-rail verification to ensure consistent performance during longer runs.

Conclusion

Week 2 delivered improved telemetry reliability and a pragmatic acknowledgement protocol that materially increases command confidence. The combined firmware and app changes reduced several classes of transient failures; remaining work centers on extended testing, logging, and finalizing documentation for field validation.
