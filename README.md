# THEOplayer Android SDK - Decoder selection extension

## Introduction

The new THEOplayer Android SDK (5.x) [was built from the ground up](https://docs.theoplayer.com/getting-started/01-sdks/02-what-is-new-in-theoplayer-5.md) using fully native, low-level Android APIs.

This gives fine-grained controls over the video playback experience and also brings new challanges.

## Fragmentation

One of the biggest challanges in the Android world is **fragmentation**.

This is a twofold problem. One on the **software** side, the other one on the **hardware** side.

Manufacturers can use any hardware parts and different Android versions when they release their devices.

These components play crucial role in media playback too.

Android provides a variety of built-in decoders that support different media formats. These decoders are typically implemented in **hardware or software, depending on the capabilities of the device** and the specific media codec being used.

When you play a video or audio file on your Android device, the decoder is responsible for taking the compressed media data (e.g., encoded in formats like H.264, AAC, or MP3) and converting it into a format that can be understood and rendered by the device's hardware or software.

Not all decoders are created equal, so the playback pipeline has to choose the best possible option to play the selected media.

### How to notice a buggy decoder?

- The application just crashes, so you can see it in your crash logs.
- Users are reporting strange playback behaviour on their devices, e.g:
  - no video, only audio
  - slow video rendering
  - artifacting

**THEOplayer has a built-in continuously improving decoder selection logic** which handles most of the use-cases, but **sometimes the player can not be aware of the exact capabilities or defects on a certain decoder, so it needs some guidance.**

This package aims to achieve that in a **community-based, open-source manner**.


## An existing THEOplayer API

THEOplayer has a [decoder selection helper API](https://docs.theoplayer.com/api-reference/android/com/theoplayer/android/api/settings/PlaybackSettings.html#setDecoderSelectionHelper(DecoderSelectionHelper)) on `THEOplayerGlobal.getSharedInstance(this).getPlaybackSettings().setDecoderSelectionHelper(...)`

This API let's you define your [`DecoderSelectionHelper`](https://docs.theoplayer.com/api-reference/android/com/theoplayer/android/api/settings/DecoderSelectionHelper.html) class to help the player's decoder selection algorithm.

```java
THEOplayerGlobal.getSharedInstance(context).getPlaybackSettings().setDecoderSelectionHelper(new DecoderSelectionHelper() {
    @Override
    public boolean shouldUseDecoder(DecoderType decoderType, String decoderName, MediaCodecInfo codecInfo) {
      
        // TCL Smart TV (32S5201X2)
        if ("OMX.MS.AVC.Decoder".equals(decoderName) && "C06".equals(Build.DEVICE) && "TCL".equals(Build.MANUFACTURER)) {
            return false;
        }
      
        return super.shouldUseDecoder(decoderType, decoderName, codecInfo);
    }

    @Override
    public boolean shouldApplySecureExtensionWorkaround(DecoderType decoderType, String decoderName, MediaCodecInfo codecInfo) {
        return super.shouldApplySecureExtensionWorkaround(decoderType, decoderName, codecInfo);
    }
});
```
For example the snippet above allows you to "blacklist" a certain decoder on a TCL Android TV (on 32S5201X2 to be exact, where the `Build.DEVICE` value is `C06`. (assuming this is a unique identification of the device within the TCL brand )

Otherwise it falls back to the original logic.

THEOplayer cannot compile a definitive list of every Android device/decoder combinations worldwide. With this new suggested way, instead of waiting for a new THEOplayer release, you can extend the support, yourself, outside of the player.

## Community-based Decoder Selection

#### This package is made with the goal of helping eachother and contributing to eachother's success by curating a "blacklist" for device and decoder (and sometimes Android versions) combinations for smooth playback experience on any Android device.

## How to use this module in your Android application?
**A**,

1. Within the directory of your Android project run:

```bash
git submodule add https://github.com/THEOplayer/android-decoder-selector
```
2. This command will link/clone the repository into your Android project so you can rely on it at build time.
3. In your `settings.gradle` file add:

```java
include ':android-decoder-selector'
```
4. This will tell to Gradle and Android Studio to recognize the directory as a module.
5. In your app-level `build.gradle` file you can add now `android-decoder-selector` as a module dependency

```java
implementation project(path: ':android-decoder-selector')
```

Now in your code you can use `ExternalDecoderSelectionHelper` class with the cummunity-based curated list to guide THEOplayer's decoder selection within your application.

```java
THEOplayerGlobal.getSharedInstance(context).getPlaybackSettings().setDecoderSelectionHelper(
	new ExternalDecoderSelectionHelper()
);

```

**B**,

Or you can just copy over [`ExternalDecoderSelectionHelper`](https://github.com/THEOplayer/android-decoder-selector/blob/main/src/main/java/com/theoplayer/android/decoderselector/ExternalDecoderSelectionHelper.kt) class to your project and use it.
But in this case you will lose the version control capability provided via the module and you always have to update the `ExternalDecoderSelectionHelper` manually.

## Contribution
If you found a device with a weirdly-acting decoder and you could manage to fix it via THEOplayer's `DecoderSelectionHelper` API, feel free to make a pull-request with the suggested change.


### Where to find the selected decoder?

#### Option 1

In the gathered device (crash) logs you should see a log line like:
```java
THEO_VideoDecoder, Decoder created successfully with name: OMX.MS.AVC.Decoder security: true,  Mime: video/mp4
```

In this case `OMX.MS.AVC.Decoder` was the used decoder, which maybe causes the issues.

#### Option 2

THEOplayer provides the [PlaybackSettings.getDecoderName(decoderType, mimeType, isSecure)](https://docs.theoplayer.com/api-reference/android/com/theoplayer/android/api/settings/PlaybackSettings.html#getDecoderName(DecoderType,String,boolean))
API that returns the decoder name that would be used given the decoder type, mime type and whether the media is DRM protected or not.

```java
String decoderName = THEOplayerGlobal.getSharedInstance(this).getPlaybackSettings().getDecoderName(DecoderType.VIDEO, "video/mp4", true);
```

Given the parameters above, the API would return the same decoder name as Option 1: `OMX.MS.AVC.Decoder`

### How to contribute?
1. Fork the project and add the new decoder exclusion into 
    `ExternalDecoderSelectionHelper`

2. Be as specific as possible. Use exact decoder names and unique device indentification techniques, and if you can test it with multiple Android verisons on the same device, add Android version restriction too.

3. Examples

   - **good** : 

     ```java
     ("OMX.MS.AVC.Decoder".equals(decoderName) && "C06".equals(Build.DEVICE) && "TCL".equals(Build.MANUFACTURER))
     ```

     will exclude the decoder on `C06` device made by `TCL` on all Android versions.

   - **bad**: 

     ```java
     "OMX.MS.AVC.Decoder" 
     ```

     will exclude the decoder on **all Android devices**

   - **good**: 

     ```java
     ("OMX.MS.AVC.Decoder".equals(decoderName) && "C06".equals(Build.DEVICE) && Build.VERSION.SDK_INT <= 23)
     ```

     will exclude the decoder on `C06` device and only on Android M and below.

6. Attach the original decoder selection and device info logs from THEOplayer (`THEO_VideoDecoder`  and `THEO_DeviceInfo` tag)

   - ``````
     THEO_VideoDecoder, Decoder created successfully with name: OMX.MS.AVC.Decoder security: true,  Mime: video/mp4
     ``````

   - ```
     THEO_DeviceInfo, Device Information
     os.version: 4.14.302-g1c5bb331fccc-ab9989803
     VERSION.SDK_INT (API level): 33
     DEVICE: sunfish
     MODEL: Pixel 4a
     PRODUCT: sunfish
     VERSION.RELEASE : 13
     VERSION.INCREMENTAL : 10161073
     BOARD : sunfish
     BOOTLOADER : s5-0.5-9825683
     BRAND : google
     SUPPORTED_ABIS : [arm64-v8a, armeabi-v7a, armeabi]
     DISPLAY : TQ3A.230605.011
     FINGERPRINT : google/sunfish/sunfish:13/TQ3A.230605.011/10161073:user/release-keys
     HARDWARE : sunfish
     HOST : abfarm-release-2004-0126
     ID : TQ3A.230605.011
     MANUFACTURER : Google
     TAGS : release-keys
     TIME : 1684427704000
     TYPE : user
     UNKNOWN : unknown
     USER : android-build
     ```

6. Add new unit tests.
7. Make a pull-request with the changes.

## Support

If you are having issues installing or using the package, first look for existing answers on our [documentation website](https://docs.theoplayer.com/),
and in particular our [FAQ](https://docs.theoplayer.com/faq/00-introduction.md).

You can also contact our technical support team by following the instructions on our [support page](https://docs.theoplayer.com/faq/00-introduction.md).
Note that your level of support depends on your selected [support plan](https://www.theoplayer.com/supportplans).

## License

```markdown
MIT License

Copyright (c) 2023 THEO Technologies

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```