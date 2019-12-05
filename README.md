# Transcribe .ogg speech files with the Microsoft Speech Java SDK
This project demonstrates how use ffmpeg to convert .ogg files (Vorbis and Opus) to the right format for Speech-to-Text transcription using the Microsoft Cognitive Services Speech Service. This could be used to transcribe voice messages encoded using the Opus (https://en.wikipedia.org/wiki/Opus_(audio_format)) codec or other codecs using the .ogg container format.

One use for this project is the transcription of WhatsApp voice messages received through the [WhatsApp Business API] (https://www.whatsapp.com/business/api)

To make this sample work, you need the [Cognitive Services Speech Service Java SDK](https://docs.microsoft.com/en-us/java/api/com.microsoft.cognitiveservices.speech?view=azure-java-stable) which has been already added to the pom file.
```java
public final static String MS_SPEECH_KEY = "your-microsoft-speech-key";
public final static String MS_SPEECH_REGION = "westeurope";
public final static String MS_SPEECH_RECOGNITION_LANG = "de-de";
```

You also need to download [ffmpeg](https://ffmpeg.org/) which is used for transcoding and set the right path to it in the source. An audio file can be read from disk or passed as a byte array. It will then, in memory, be transcoded to wav / pcm format for transcription using the Cognitive Services Speech Service.

Also check out the [Microsoft Speech SDK Sample Repository](https://github.com/Azure-Samples/cognitive-services-speech-sdk) to learn more and use more of it's functionality.

Thank you [@chgeuer](https://github.com/chgeuer) for your contributions.
