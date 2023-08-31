# Sudo Decentralized Identity Relay Sample App for Android

## Overview

This project provides examples of how to use the Sudo Decentralized Identity Relay Android SDK.

## Supported Android Versions

This app supports Android 8 (API level 26) or newer, built with Kotlin version 1.8.

If you are using Android Studio, version 4.0 or newer is required.

## Getting Started

To build this app you first need to obtain an SDK configuration file.

1. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain an SDK configuration file (sudoplatformconfig.json)

2. Add the SDK configuration file to the project in the following location:
   
   Add the config file `sudoplatformconfig.json` to the assets folder of the app module.
   
   If the application does not already have an assets folder, create one under `exampleapp/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

3. Apply the steps relevant to your registration method (FSSO, TEST, or SafetyNet) (see below).

4. Build the app

5. Run the app on an emulator (AVD) or Android device running Android 8 (API level 26) or later that is not rooted and does not have an unlocked bootloader.

### TEST Registration Set Up

To build and use this app with TEST registration, you will need a TEST registration private key and key identifier to add them to the project.

1. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain a TEST registration private key and TEST registration key identifier.

2. Add the TEST registration private key and TEST registration key identifier to the project in the following locations:

   Add the private key file `register_key.private` to the assets folder of the app module.
   Add a text file `register_key.id` containing the test registration key ID.

   If the application does not already have an assets folder, create one under `exampleapp/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

### FSSO Authentication Set Up

To build and use this app with FSSO for authentication, the SDK configuration file should contain all required fields to support the desired FSSO environment. No further set up should be required.

### SafetyNet Registration Set Up

To build and use this app with SafetyNet for registration, some additional set up is required:

1. Obtain a SafetyNet API key via the Google console and place it in a file at `/var/anonyome_key/safetynet_api_dev.key`.
   
2. Obtain your app signing certificate's fingerprint and provide it to the identity team to be whitelisted. To obtain this fingerprint, run the following command:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256 | tr -d : | awk '{print($2)}' | xxd -r -p | base64
   ```

## More Documentation

Refer to the following documents for more information:

- [Sudo Platform Docs](https://docs.sudoplatform.com/guides/decentralized-identity/relay-sdk)
- [Getting Started on Sudo Platform](https://docs.sudoplatform.com/guides/getting-started)

## Issues and Support

File issues you find with this example app in this Github repository. Ensure that you do not include any Personally Identifiable Information(PII), API keys, custom endpoints, etc. when reporting an issue.

For general questions about the Sudo Platform, the Sudo Platform, please contact [partners@sudoplatform.com](mailto:partners@sudoplatform.com)
