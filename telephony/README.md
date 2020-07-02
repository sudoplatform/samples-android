# Sudo Telephony Example App for Android

## Overview

This project provides examples of how to use the Sudo Telephony Android SDK.

## Supported Android Versions

This app supports Android version 23 or newer, built with Kotlin version 1.3.

If using Android Studio, version 3.6 or newer is required.

## Getting Started

To build this app you first need to obtain an SDK configuration file and a TEST registration keypair and add them to the project.

1. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain an SDK configuration file (sudoplatformconfig.json) and a TEST registration key, respectively.

2. Add the SDK configuration file and TEST registration keys to the project in the following locations:

   Add the config file `sudoplatformconfig.json` to the assets folder of the app module.
   Add the private key file `register_key.private` to the assets folder of the app module.
   Add a text file `register_key.id` containing the test registration key ID.

   If the application does not already have an assets folder, create one under `TelephonyExample/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

3. Build the app

### Using the App

- Change the area code by selecting the button labeled "US"
- Type an area code into the text field and select the "Search" button
- Select a phone number from the search results for the option to provision the number
- Select "Provision Number" which will assign it to your Sudo and enable sending and receiving messages with that number
- Use the upper-left corner menu button to view your provisioned numbers
- Select a number to see options to delete the number or send a message with it
- Selecting "Delete" will remove that number from the Sudo
- Selecting "Send Message" will show a pop-up that lets you enter in a phone number and type a message to send.

## More Documentation

Refer to the following documents for more information:

- [Sudo Telephony Docs](https://docs.sudoplatform.com/guides/telephony)
- [Getting Started on Sudo Platform](https://docs.sudoplatform.com/guides/getting-started)
- [Understanding Sudo Digital Identities](https://docs.sudoplatform.com/concepts/sudo-digital-identities)

## Issues and Support

File issues you find with this sample app in this Github repository. Ensure that you do not include any Personally Identifiable Information(PII), API keys, custom endpoints, etc. when reporting an issue.

For general questions about the Sudo Platform, the Sudo Platform, please contact [partners@sudoplatform.com](mailto:partners@sudoplatform.com)
