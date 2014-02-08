NerdzMessenger
==============

A Nerdz Api based Nerdz messaging app for Android.
Requires NerdzApi, android-support-v7-gridlayout and Google Play Services.

Also requires https://github.com/candrews/HttpResponseCache.

Supported versions from API 9 (Gingerbread) to 19 (KitKat).

Features
========

NerdzMessenger is an Android application capable to offer a full chat experience between two NERDZ users on Android devices.
Using mcilloni/nerdzapi-java, it can fetch, show and send messages, offering a nice and quick Holo experience.
The user is also able to chat with persons using the desktop or mobile website through "PM" functions on www.nerdz.eu (see its code on github.com/nerdzeu/nerdz.eu).

This application supports push notifications through GCM; the user is prompted with InboxStyle notifications for multiple messages, and also LocalBroadcastManager is used to update activity content while the application is running; all traffic is served via SSL (NerdzApi dependent)

Serverside, mcilloni/pushed dispatches concurrently and asynchronously requests to GCM.

Come to nerdz.eu to grab a recent signed build of NerdzMessenger.