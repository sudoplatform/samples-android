# Sudo Email Sample App for Android

## Overview

This project provides examples of how to use the Sudo Email Android SDK. The app is built with Jetpack Compose and demonstrates:

- Registering and signing in with a TEST registration key or Federated Single Sign On (FSSO), redeeming entitlements, viewing user details, and deregistering or signing out.
- Creating, editing and deleting Sudos. Email addresses belong to a Sudo.
- Provisioning and deprovisioning email addresses, checking an address's availability as you type.
- Email aliases, internal (forwarding to one of your addresses) and external (forwarding to an address outside the platform, once verified): provisioning, enabling, disabling, updating metadata and expiry, and deprovisioning.
- Reading email by folder, kept up to date by subscription, with custom folders, empty trash and blocked addresses.
- Composing, replying to and forwarding email with attachments, optionally sent from an alias, with an indicator showing when every recipient will receive it end-to-end encrypted. Drafts can be saved, edited and scheduled to send later.
- Exporting your Sudo and email keys to a password protected backup file and importing them on another device (FSSO only).
- Push notifications for received email, muted or unmuted per address from the address's messages screen. Tapping a notification opens the message.
- Choosing, before signing in, whether every email address and alias shares a single key pair, which keeps key backup simple, and whether deleting an email address also removes its keys from the device.

## Supported Android Versions

This app supports Android 8 (API level 26) or newer, built with Kotlin version 2.1 and Jetpack Compose.

If you are using Android Studio, Narwhal or newer is required.

## Getting Started

To build this app you first need to obtain an SDK configuration file. If you are not using Federated Single Sign On (FSSO) you will also need a TEST registration private key and key identifier and add them to the project.

1. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain an SDK configuration file (sudoplatformconfig.json), TEST registration private key and TEST registration key identifier.

2. Add the SDK configuration file, TEST registration private key and TEST registration key identifier to the project in the following locations:

   Add the config file `sudoplatformconfig.json` to the assets folder of the app module.
   Add the private key file `register_key.private` to the assets folder of the app module.
   Add a text file `register_key.id` containing the test registration key ID.

   If the application does not already have an assets folder, create one under `exampleapp/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

3. Optionally, to receive push notifications for incoming email, add a Firebase project's `google-services.json` for the package `com.sudoplatform.emailexample` to the `exampleapp` folder. Your environment must have notifications enabled for email. The Google services plugin is applied only when the file is present, so without it the app builds and runs without push notifications.

4. Build the app

5. Run the app on an emulator (AVD) or Android device running Android 8 (API level 26) or later that is not rooted and does not have an unlocked bootloader.

## Running the Tests

Unit tests run on the JVM and need no configuration:

```bash
./gradlew :exampleapp:testDebugUnitTest
```

Instrumented tests run on an emulator or device against your environment, using a TEST registration, so they need the files from step 2:

```bash
./gradlew :exampleapp:connectedDebugAndroidTest
```

## More Documentation

Refer to the following documents for more information:

- [Sudo Email Docs](https://docs.sudoplatform.com/guides/email)
- [Getting Started on Sudo Platform](https://docs.sudoplatform.com/guides/getting-started)
- [Understanding Sudo Digital Identities](https://docs.sudoplatform.com/concepts/sudo-digital-identities)

## Issues and Support

File issues you find with this example app in this Github repository. Ensure that you do not include any Personally Identifiable Information(PII), API keys, custom endpoints, etc. when reporting an issue.

For general questions about the Sudo Platform, please contact [partners@sudoplatform.com](mailto:partners@sudoplatform.com)
