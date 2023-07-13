# Decoding the Android media decoders

Today, most streaming services are supporting more than 10 platforms with close to the same number of apps in different stores. Supporting all these platforms is a hygiene factor. It just has to work. Large streaming providers such as Netflix, Youtube, Peacock and Twitch are setting the requirements, making viewers used to access content on all platforms.

However, supporting all these platforms can be hard, definitely for streaming services facing Android device fragmentation, a common challenge amongst media companies streaming into different countries/regions.

#### In this article we’ll cover:

- Challenges with Android device fragmentation.
- Detection of a faulty media decoder.
- The THEOplayer Android SDK decoder selection extension, to make it easy to select the right decoders, without having to republish your app.

## Android (media codec) fragmentation

When you enjoy watching videos, listening to music, or streaming content on your Android device, have you ever wondered how all these multimedia files are decoded and played? That's where media decoders come into play.

Media decoders are essential components responsible for decoding compressed media files, such as videos and audio (e.g., encoded in formats like H.264, AAC, or MP3), into formats that can be displayed or played by your Android device. There are different types of media decoders, including hardware-based, software-based, and hybrid decoders, each with its own advantages and characteristics, depending on the capabilities of the device.

Due to the open-source nature of Android, manufacturers are free to choose their hardware and software components when they build their Android devices. This causes fragmentation in the Android ecosystem which means it is really hard to develop a one size fits all solution (unlike in the Apple ecosystem).

As the media playback components (hardware and software) can vary from device to device a playback pipeline has to take into account the possible differences and limitations.

## THEOplayer Android SDK

The new THEOplayer Android SDK (5.x) [was built from the ground up](https://docs.theoplayer.com/getting-started/01-sdks/02-what-is-new-in-theoplayer-5.md) using fully native, low-level Android APIs to provide fine-grained control over the media playback experience.

This means the player will encounter the above-mentioned media decoder and playback differences firsthand.

**THEOplayer has a built-in continuously improving decoder selection logic** which handles most of the use cases, but **sometimes the player cannot be aware of the exact capabilities or defects of a certain decoder.** 

This issue is not specific to THEOplayer. **Any video player that does media playback via low-level APIs runs into similar issues.**

#### How can you detect a faulty decoder? 

- The application is crashing with some weird decoder-related error logs.
- Users are reporting strange playback behaviour on their devices, for example:
  - Audio is playing, but they don't see any video
  - The video playback is stuttering
  - There are some weird artifacts on the video (green pixels, blurred image)

## The Decoder Selection extension

It’s very difficult to compile a definitive list of the ever-growing Android device/decoder combinations worldwide.

That’s why with THEOplayer Android SDK 5.x introduced a [decoder selection helper API](https://docs.theoplayer.com/api-reference/android/com/theoplayer/android/api/settings/PlaybackSettings.html#setDecoderSelectionHelper(DecoderSelectionHelper)) on `THEOplayerGlobal.getSharedInstance(this).getPlaybackSettings().setDecoderSelectionHelper(...)`

This API lets you define your [`DecoderSelectionHelper`](https://docs.theoplayer.com/api-reference/android/com/theoplayer/android/api/settings/DecoderSelectionHelper.html) class to help the player's decoder selection algorithm.

```java
class MyDecoderSelectionHelper extends DecoderSelectionHelper {
  
    @Override
    public boolean shouldUseDecoder(DecoderType decoderType, String decoderName, MediaCodecInfo codecInfo) {
        return super.shouldUseDecoder(decoderType, decoderName, codecInfo);
    }

    @Override
    public boolean shouldApplySecureExtensionWorkaround(DecoderType decoderType, String decoderName, MediaCodecInfo codecInfo) {
        return super.shouldApplySecureExtensionWorkaround(decoderType, decoderName, codecInfo);
    }

    @Override
    public boolean shouldOverrideBuiltInDecoderSelectionLogic() {
        return super.shouldOverrideBuiltInDecoderSelectionLogic();
    }
  
}
```

When the player picks the decoder for the current source, it will iterate over the decoders available on the device and selects the first one that satisfies the needs. If a `DecoderSelectorHelper` is defined, it will call into the relevant methods too before selecting the final media decoder.

Let's look at the provided API in detail.

#### 1. `shouldUseDecoder` 

This is the one that you will use in most of the cases.

If you notice that there are playback issues with the selected decoder (see _["How can we detect a faulty decoder?](#how-can-we-detect-a-faulty-decoder)"_ section) , here you can "blacklist" it and return `false` inside the method.

<ins>**NOTE**</ins>: **Be aware this code will run on all of your customer's devices. Be as specific as possible when blacklisting a decoder!** The same decoder can be available on another device (from another manufacturer) too and there it could work without any problem.

For example:

```java
@Override
public boolean shouldUseDecoder(DecoderType decoderType, String decoderName, MediaCodecInfo codecInfo) {

    // TCL Smart TV (32S5201X2)
    if ("OMX.MS.AVC.Decoder".equals(decoderName) && "C06".equals(Build.DEVICE) && "TCL".equals(Build.MANUFACTURER)) {
        return false;
    }

    return super.shouldUseDecoder(decoderType, decoderName, codecInfo);
}
```

This snippet will blacklist the `OMX.MS.AVC.Decoder` decoder only on a `TCL` Smart TV that has the `C06` identifier.

**Don't forget to call the `super` method** to keep the support as is for the other devices.

#### 2. `shouldApplySecureExtensionWorkaround`

The secure decoder is primarily focused on decoding content that is encrypted or protected by digital rights management (DRM) technologies. It ensures that the content remains secure during the decoding process and prevents unauthorized access or tampering.

In most cases, this works fine, but sometimes the player could apply a workaround on the decoder to avoid playback issues (e.g. rendering glitches).

The idea is the same as with the `shouldUseDecoder `method, only use it when it is really needed.

**Don't forget to call the `super` method** to keep the support as is for the other devices.

#### 3. `shouldOverrideBuiltInDecoderSelectionLogic`

**THEOplayer has a built-in decoder selection logic, and that is used by default**. If a decoder falls through those internal checks, the `DecoderSelectionHelper` will be used.

If you return `true` in this method, the **default decoder selection logic will be skipped** and  **only the `DecoderSelectionHelper` will be used**.

<ins>**NOTE**</ins>: **You should <ins>not</ins> use this method!** We just show it for completeness. This is a preparation for a later iteration of the feature. You should use this method only if you have an extensive blacklist of decoders (covering THEOplayer's internal list too), otherwise you can break playback on some devices.

#### How do you know which decoder to blacklist?

After you gathered device logs from a customer or crash reports from an analytics backend, you should look for a log line like this with a tag `THEO_VideoDecoder`:

```java
THEO_VideoDecoder, Decoder created successfully with name: OMX.MS.AVC.Decoder security: true,  Mime: video/mp4
```

This means THEOplayer selected `OMX.MS.AVC.Decoder` decoder for the playback of the selected stream. (And probably it is buggy, that's why you see the issues).

The device details are logged with the tag `THEO_DeviceInfo`:

```
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

With the two information above you should be able to fabricate a device-specific exclusion list in your `DecoderSelectionHelper` class.

## Community-based decoder selection extension

You can always build your own `DecoderSelectionHelper` , but wouldn't be better if someone does it for you?

The THEOplayer Android SDK includes an always-evolving algorithm for decoder selection. However, it can only cover devices that the developers have access to and can test.

**We started an <ins>open-source initiative</ins> where we curate a blacklist of device and decoder combinations <ins>based on your feedback and contribution</ins>.**

The idea behind this crowdsourced list is to help everyone to achieve smooth playback on as many devices as possible without suffering from the same problems over and over again.

To know more about this project, please visit the GitHub page:  https://github.com/THEOplayer/android-decoder-selector

## Dynamic decoder selection

One of the hidden advantages of opening up an internal SDK logic through a public API is **on-the-fly adaptability**.

Mobile applications nowadays talk to a backend system every time. Getting the data to show to the user, sending analytics to calculate better metrics and drive business decisions, detecting crashes, and so on...

If there is a playback crash reported by the application, or video rendering glitches reported by the users, it is important to address these issues in a timely manner. Failure to do so can result in a negative user experience and potentially lead to loss of customers.

By maintaining an always up-to-date decoder selection blacklist on your backend or using [Firebase Remote Config](https://firebase.google.com/docs/remote-config) that you sync with every device at application start you can ensure your users that the same crash is not happening again.

**In this case you don't have to wait for a new release of a decoder selection helper module, a new release of THEOplayer and a new release of your application. The decoder selection fixes can be applied as soon as the app is restarted and the new list is fetched from the backend.**

With Firebase Remote Config you can even target the user who only encountered a crash and send them a new decoder selection configuration. (_Stay tuned for a comprehensive guide on this topic_!)

## Do you need any help?

Visit our [developer hub](https://docs.theoplayer.com/), and in particular our [FAQ](https://docs.theoplayer.com/faq/00-introduction.md), or [contact us](https://www.theoplayer.com/contact-us) to learn more about Android device fragmentation, and the decoder selection extension.

