# Sudo Virtual Cards Sample App for Android

## Overview

This project provides examples of how to use the Sudo Virtual Cards Android SDK.

## Supported Android Versions

This app supports Android 7 (API level 24) or newer, built with Kotlin version 1.6.

If you are using Android Studio, version 4.0 or newer is required.

## Getting Started

To build this app you first need to obtain an SDK configuration file. If you are not using Federated Single Sign On (FSSO) you will also need a TEST registration private key and key identifier and add them to the project.

1. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain an SDK configuration file (sudoplatformconfig.json), TEST registration private key and TEST registration key identifier.

2. Add the SDK configuration file, TEST registration private key and TEST registration key identifier to the project in the following locations:

   Add the config file `sudoplatformconfig.json` to the assets folder of the app module.
   Add the private key file `register_key.private` to the assets folder of the app module.
   Add a text file `register_key.id` containing the test registration key ID.

   If the application does not already have an assets folder, create one under `exampleapp/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

3. Build the app

4. Run the app on an emulator (AVD) or Android device running Android 7 (API level 24) or later that is not rooted and does not have an unlocked bootloader. 

## More Documentation

Refer to the following documents for more information:

- [Sudo Virtual Cards Docs](https://docs.sudoplatform.com/guides/virtual-cards)
- [Getting Started on Sudo Platform](https://docs.sudoplatform.com/guides/getting-started)
- [Understanding Sudo Digital Identities](https://docs.sudoplatform.com/concepts/sudo-digital-identities)

## Issues and Support

File issues you find with this example app in this Github repository. Ensure that you do not include any Personally Identifiable Information(PII), API keys, custom endpoints, etc. when reporting an issue.

For general questions about the Sudo Platform, the Sudo Platform, please contact [partners@sudoplatform.com](mailto:partners@sudoplatform.com)
