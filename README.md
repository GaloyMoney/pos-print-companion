# POS Print Companion App

## Overview

This Android app is a wrapper for the [NyxPrinterClient](https://github.com/yyzz2333/NyxPrinterClient), an SDK used for printing receipts from Bitcoinzie POS machines. The app does not have a user interface, which fixes the problem of switching between apps while using Deeplink. It uses a deep link approach to receive data and initiate printing commands.

## Description

- The app is designed to work without a user interface, addressing the issue of app switching by running in the background.
- It integrates with the NyxPrinterClient SDK, ensuring compatibility and ease of use for printing receipts from Bitcoinze POS machines.
- By utilizing deep links, the app can process print commands directly from URLs, allowing for efficient communication between applications.

## Building the App

To build the app, you need to have the Java Development Kit (JDK-17) installed on your computer. Use the following command to compile the app:

```shell
./gradlew build
```

usage
```javascript
// Define the parameters
var username = 'blinkUsername';
var amount = 'TransactionAmount';
var paymentHash = 'UniquePaymentHash';

// Encode the parameters and construct the deep link URL
var deepLinkUrl = `blink-pos-companion://print?username=${encodeURIComponent(username)}&amount=${encodeURIComponent(amount)}&paymentHash=${encodeURIComponent(paymentHash)}`;

// Redirect to the deep link URL to initiate the printing process
window.location.href = deepLinkUrl;
```
