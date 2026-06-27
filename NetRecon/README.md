# NetRecon 🔍

**Professional Android Network Analysis & Recon Toolkit**

![Build](https://github.com/your-username/netrecon/actions/workflows/build.yml/badge.svg)

## Features

### Standard Mode (Any Device)
- **Port Scanner** — TCP connect scan with banner grabbing, service detection
- **Host Discovery** — Subnet ping sweep to find live hosts
- **DNS Enumeration** — A/MX/NS/TXT records + subdomain brute force (50+ wordlist)
- **SSL Inspector** — Certificate details, expiry, weak ciphers, grading (A+ to F)
- **WiFi Scanner** — Nearby networks with SSID, BSSID, signal, encryption type
- **Traceroute** — Hop-by-hop path analysis

### Root Mode (Rooted Devices)
Everything above, plus:
- **ARP Table** — Connected clients on local network
- **Monitor Mode** — Enable WiFi monitor mode (wlan0)
- **Raw Socket Scanning** — Faster, stealthier port scans
- **Root Command Execution** — Direct shell access for advanced recon

## Onboarding Flow
```
App Launch
    └── Auto root detection (checks su binary, root apps, rw paths)
        ├── Root confirmed → offer Root Mode or Standard Mode
        └── No root → Standard Mode only
```

## Build

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Local Build
```bash
git clone https://github.com/your-username/netrecon
cd netrecon
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions (Auto Build)
Push to `main` → GitHub Actions builds both debug and release APKs automatically.
Download from the **Actions** tab → latest workflow run → **Artifacts**.

## Tech Stack
- **Kotlin** + Jetpack Compose (Material 3)
- **OkHttp** for DNS-over-HTTPS queries
- **Coroutines** for async scanning
- Dark monospace theme — professional red team aesthetic

## Legal
For authorized testing only. Use only on networks and systems you own or have explicit permission to test.
