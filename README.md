<p align="center">
  <img src="images/IshtarRF-logo.png" alt="IshtarRF logo" width="120" />
</p>

<h1 align="center">IshtarRF — Android</h1>

<p align="center">
  A native <b>Kotlin + Jetpack Compose</b> app that turns an <b>ESP32 + CC1101</b>
  module into a pocket Sub-GHz RF tool — receive, transmit, visualize, and store
  signals straight from your phone over a <b>USB-OTG cable</b>.
</p>

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4">
  <img alt="License" src="https://img.shields.io/badge/License-AGPL%20v3-blue.svg">
</p>

> This is the **mobile front-end** of the IshtarRF project. The device firmware
> (ESP32 + CC1101) lives in the main repo:
> **[CyberDuckyiq/IshtarRF](https://github.com/CyberDuckyiq/IshtarRF)**.

---

## ⚠ Legal / RF compliance

You are responsible for complying with **local RF regulations**. Use only
**permitted frequencies, power levels, and protocols**. The transmit and
brute-force tools are for testing devices **you own**.

---

## Features

- **USB-OTG link** to the ESP32 — supports CP210x / CH340 / FTDI / native-USB chips.
- **Radio config:** frequency, modulation (OOK / 2-FSK), bitrate, deviation, TX power.
- **Receive** in **RAW (OOK)** or **Packet** mode, with live **RSSI**.
- **Transmit:** raw `µs` pulse trains (repeat / gap / invert) and HEX bytes.
- **Waveform view** — pan & zoom the captured OOK pulse train.
- **Signal library** — Flipper/Bruce-compatible **`.sub`** files with folders,
  search, rename, share, and import (Storage Access Framework).
- **RF tools** — one-tap **replay**, live **RSSI graph**, **frequency scanner**,
  and OOK **brute-force**.
- **Reliability** — **Recover** button (re-inits the radio), **Clear** captured
  signal, and **auto-reconnect** if the link drops.
- **Favorite frequencies** as quick chips, and three themes (**IshtarRF / Dark /
  Light**) persisted across launches.

---

## Download

Grab the latest APK from the **[Releases](../../releases)** page, or use the
`IshtarRF-v0.1.0.apk` included in this repo. Enable *Install unknown apps* for
your file manager, then open the APK.

> Requires **Android 8.0 (API 26)+** and a phone that supports **USB host / OTG**.

## Hardware

- **ESP32** dev board + **CC1101** module (3.3 V only) running the IshtarRF firmware.
- A **USB-OTG cable** (USB-C or micro-USB to the board's USB port).

Flash the firmware from the [main repo](https://github.com/CyberDuckyiq/IshtarRF)
first; the app talks to it over newline-delimited JSON at 115200 baud.

---

## Build from source

Open the project in **Android Studio** (Koala or newer) and press Run, or:

```bash
./gradlew :app:assembleDebug      # debug APK  -> app/build/outputs/apk/debug/
./gradlew :app:assembleRelease    # release APK -> app/build/outputs/apk/release/
./gradlew :app:installDebug       # install onto a connected device
```

Requirements: **JDK 17+**, Android **compileSdk 35**, min Android **8.0 (API 26)**.
Android Studio writes `local.properties` (your SDK path) automatically.

---

## Architecture

MVVM with a single source-of-truth `UiState` exposed by `MainViewModel`.

```
data/
  serial/   UsbSerialManager — USB-OTG link; ConnectionState + a Flow of
            newline-delimited JSON lines (mik3y/usb-serial-for-android).
  protocol/ Commands (phone -> device) + DeviceEvent/EventParser (device -> phone).
  sub/      SubFile (.sub parse/export) + SubLibraryRepository (file storage).
  prefs/    SettingsRepository — DataStore (theme + favorite frequencies).
domain/     Plain models (RadioConfig, CapturedSignal, LogEntry, …).
ui/         MainViewModel + Compose screens:
            Control · Waveform · Library · Tools · Settings, plus theming.
```

### Serial protocol

The firmware hand-parses JSON (no library): it scans for `"key"` then reads the
value up to the next `,`/`}`, and reads string values (including `invert`)
between quotes. `Commands.kt` emits exactly that flat shape. The full
command/event contract is documented in the main repo's `CLAUDE.md`.

---

## Tech stack

Jetpack Compose · Material 3 · Kotlin Coroutines/Flow · kotlinx.serialization ·
AndroidX DataStore · [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android).

## License

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](LICENSE)

Licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0-only)**.
© 2025 Cyber Ducky. See [`LICENSE`](LICENSE).
