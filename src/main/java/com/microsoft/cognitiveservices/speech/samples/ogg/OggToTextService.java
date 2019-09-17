//MIT License
//
//Copyright (c) Microsoft Corporation. All rights reserved.
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE

package com.microsoft.cognitiveservices.speech.samples.ogg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PullAudioInputStreamCallback;


/**
 * @author malantin
 * Transcode ogg (Vorbis and Opus) files and transcribe using Microsoft Speech Service
 *
 */
public class OggToTextService {
	
	public final static String MS_SPEECH_KEY = "your-microsoft-speech-key";
	public final static String MS_SPEECH_REGION = "westeurope";
	public final static String MS_SPEECH_RECOGNITION_LANG = "de-de";

	public String transcribeOGGFile(String path) throws IOException, InterruptedException, ExecutionException {
		File inputFile = new File(path);

		byte[] fileContent = Files.readAllBytes(inputFile.toPath());

		return transcribeOGGFileFromByteArray(fileContent);
	}

	public String transcribeOGGFileFromByteArray(byte[] media)
			throws InterruptedException, IOException, ExecutionException {

		// Create ffmpeg process to decode from opus to wave
		// Learn more and get ffmpeg from https://ffmpeg.org/
		// Legal information on ffmpeg: https://ffmpeg.org/legal.html
		// Download ffmpeg and set the path here
		// ffmpeg we decode opus from stdin and output wave to stdout
		Process process = Runtime.getRuntime().exec("ffmpeg.exe -i - -c:a pcm_s16le -ar 16000 -ac 1 -f wav - ");

		InputStream oggStream = new ByteArrayInputStream(media);
		// Get stdin for ffmpeg process
		OutputStream processStdinStream = process.getOutputStream();
		// Start moving bytes from our input to stdin
		Thread threadIn = DataPipe.start(oggStream, processStdinStream);

		// Get stdin for ffmpeg process
		InputStream processStdoutStream = process.getInputStream();
		ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
		// Start moving bytes from from stdout to our output stream
		Thread threadOut = DataPipe.start(processStdoutStream, wavStream);

		// Get error stream for ffmpeg process
		InputStream processStderrStream = process.getErrorStream();
		ByteArrayOutputStream loggingStream = new ByteArrayOutputStream();
		// Start moving bytes from error stream
		Thread threadErr = DataPipe.start(processStderrStream, loggingStream);

		// Wait for threads to finish
		threadIn.join();
		int exitCode = process.waitFor();
		threadOut.join();
		threadErr.join();

		// Fix header of the wave file as ffmpeg misses to write parts of the
		// information when converting from stdin to stdout
		byte[] wavByteArray = wavStream.toByteArray();

		long l1 = wavByteArray.length - 8;
		wavByteArray[4] = (byte) ((l1 >> 0) & 0xff);
		wavByteArray[5] = (byte) ((l1 >> 8) & 0xff);
		wavByteArray[6] = (byte) ((l1 >> 16) & 0xff);
		wavByteArray[7] = (byte) ((l1 >> 24) & 0xff);

		long l2 = wavByteArray.length - 78;
		wavByteArray[0x4a] = (byte) ((l2 >> 0) & 0xff);
		wavByteArray[0x4b] = (byte) ((l2 >> 8) & 0xff);
		wavByteArray[0x4c] = (byte) ((l2 >> 16) & 0xff);
		wavByteArray[0x4d] = (byte) ((l2 >> 24) & 0xff);

		System.out.println("Exit Code: " + exitCode);
		System.out.println(new String(loggingStream.toByteArray()));

		String recognizedText = "";

		// Set Microsoft Speech subscription key and region, see Speech SDK documentation for more information
		// https://docs.microsoft.com/en-us/java/api/com.microsoft.cognitiveservices.speech?view=azure-java-stable
		SpeechConfig config = SpeechConfig.fromSubscription(MS_SPEECH_KEY, MS_SPEECH_REGION);
		config.setSpeechRecognitionLanguage(MS_SPEECH_RECOGNITION_LANG);

		ByteArrayInputStream waveByteArrayInput = new ByteArrayInputStream(wavByteArray);

		PullAudioInputStreamCallback callback = new WavStream(waveByteArrayInput);
		AudioConfig audioInput = AudioConfig.fromStreamInput(callback);

		SpeechRecognizer recognizer = new SpeechRecognizer(config, audioInput);

		// Do one time recognition, might fail for longer speech files, see SDK documentation
		Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();

		assert (task != null);

		System.out.println(String.format("Sending file to Microsoft Speech for transcription to %s",
				config.getSpeechRecognitionLanguage()));

		SpeechRecognitionResult result;

		result = task.get();
		assert (result != null);

		if (result.getReason() == ResultReason.RecognizedSpeech) {
			recognizedText = result.getText();
			System.out.println("Recognized Speech: Transcription was successful.");
		} else if (result.getReason() == ResultReason.NoMatch) {
			System.out.println("No Match: Speech could not be recognized.");
		} else if (result.getReason() == ResultReason.Canceled) {
			CancellationDetails cancellation = CancellationDetails.fromResult(result);
			System.out.println("Canceled: Reason=" + cancellation.getReason());

			if (cancellation.getReason() == CancellationReason.Error) {
				System.out.println("Canceled: ErrorCode=" + cancellation.getErrorCode());
				System.out.println("Canceled: ErrorDetails=" + cancellation.getErrorDetails());
				System.out.println("Canceled: Did you update the subscription info?");
			}
		}

		recognizer.close();
		audioInput.close();

		return recognizedText;
	}
}
