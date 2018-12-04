# Chat Demo Application Overview

## This is Kotlin version of the Demo app

For the Java version you can look [here](https://github.com/twilio/twilio-chat-demo-android/tree/java).

This demo app SDK version: ![](https://img.shields.io/badge/SDK%20version-3.1.0-blue.svg)

Latest available SDK version: [ ![Latest SDK version](https://api.bintray.com/packages/twilio/releases/chat-android/images/download.svg) ](https://bintray.com/twilio/releases/chat-android/_latestVersion)

## Getting Started

Welcome to the Chat Demo application.  This application demonstrates a basic chat client with the ability to create and join channels, invite other members into the channels and exchange messages.

What you'll minimally need to get started:

- A clone of this repository
- [A way to create a Chat Service Instance and generate client tokens](https://www.twilio.com/docs/api/chat/guides/identity)
- Gradle installation
- Google Play Services library : [Follow the instructions here](https://developers.google.com/android/guides/setup)

## Building

### Make sure correct Google packages are installed

In Android SDK Manager make sure you have installed `Android Support Repository` and `Google Repository` under Extras.

### Set up gradle wrapper to use correct gradle version.

Run
```
./gradlew wrapper
```

### Set the value of `ACCESS_TOKEN_SERVICE_URL`

Set the value of `ACCESS_TOKEN_SERVICE_URL` in chat-demo-android/gradle.properties file to point to a valid Access-Token server.

Create that file if it doesn't exist with the following contents:

```
ACCESS_TOKEN_SERVICE_URL=http://example.com/get-token/
```

NOTE: no need for quotes around the URL, they will be added automatically.

You can also pass this parameter to gradle during build without need to create a properties file, as follows:

```
gradle -PACCESS_TOKEN_SERVICE_URL=http://example.com/get-token/ build
```

### Add google-services.json

[Generate google-services.json](https://developers.google.com/mobile/add) file and place it under `chat-demo-android/`.

### Build

Run `gradle build` to fetch Twilio SDK files and build application.

### Android Studio

You can import this project into Android Studio if you so desire by selecting `Import Project (Eclipse ADT, Gradle, etc)` from the menu and then build using Studio's Build menu.

## License

MIT
