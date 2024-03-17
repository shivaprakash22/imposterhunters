package com.wellsfargo.imposterhunters.imposterhunters.api;


import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.wellsfargo.imposterhunters.imposterhunters.SpeechAnalyzeResponse;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;
@RestController
public class SpeechAnalyzeController {

    @Value("${azure.speech.subscription-key}")
    private String subscriptionKey;
    @Value("${azure.speech.region}")
    private String region;
    @Value("${azure.textanalytics.enpoint}")
    private String apiEndPoint;
    @Value("${azure.textanalytics.apikey}")
    private String apiKey;


    @PostMapping("/ping")
    public String pingurl(){
        return "Success";
    }

    @PostMapping("/voice/analyze")
    public SpeechAnalyzeResponse analyzeAudio(@RequestParam("file") MultipartFile file) {
        String analyseText=null;
        String textanalayse=null;
        SpeechAnalyzeResponse speechAnalyzeResponse= new SpeechAnalyzeResponse();
        try {
            File audioFile=null;

            // Convert MultipartFile to File
            audioFile = convertMultipartFileToFile(file);

            if("audio/mpeg".equalsIgnoreCase(file.getContentType())) {
                String ouputPath= audioFile.getAbsolutePath().replace(".mp3",".wav");
              audioFile= convertMP3ToWAV(audioFile.getAbsolutePath(),ouputPath);
            }

            SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, region);
            System.out.println("Path"+audioFile.getAbsolutePath());
            AudioConfig audioInput = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
            SpeechRecognizer recognizer = new SpeechRecognizer(config, audioInput);
            Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
            SpeechRecognitionResult result = task.get();
             textanalayse= result.getText();
            System.out.println("Resutlt:"+textanalayse);


            System.out.println("Recognized: " + textanalayse);
            AzureKeyCredential credential = new AzureKeyCredential(apiKey);
            TextAnalyticsClient textAnalyticsClient = new TextAnalyticsClientBuilder()
                    .credential(credential)
                    .endpoint(apiEndPoint)
                    .buildClient();
            DocumentSentiment documentSentiment = textAnalyticsClient.analyzeSentiment(textanalayse);
            double positiveScore = documentSentiment.getConfidenceScores().getPositive();
            double negativeScore = documentSentiment.getConfidenceScores().getNegative();
            double neutralScore = documentSentiment.getConfidenceScores().getNeutral();
            System.out.println("positiveScore"+positiveScore);
            System.out.println("negativeScore"+negativeScore);
            System.out.println("neutralScore"+neutralScore);
            // Analyze emotional tone probability
            String emotionalTone = null;
            //String emotionalTone = analyzeEmotionalTone(result);
            System.out.println("Reason"+result.getReason());

            speechAnalyzeResponse.setNegativescore(negativeScore);
            speechAnalyzeResponse.setPositivescore(positiveScore);
            speechAnalyzeResponse.setNeutralscore(positiveScore);
            speechAnalyzeResponse.setText(textanalayse);
            speechAnalyzeResponse.setStatus("success");

        } catch (Exception ex) {
            ex.printStackTrace();
            speechAnalyzeResponse.setStatus("Error in processing");
            return speechAnalyzeResponse;
        }
        return speechAnalyzeResponse;
    }


    public static File convertMP3ToWAV(String mp3FilePath, String wavFilePath)
            throws IOException, JavaLayerException, UnsupportedAudioFileException {
        // Convert MP3 to WAV using JLayer
        Converter converter = new Converter();
        converter.convert(mp3FilePath, wavFilePath);

        // Convert WAV to PCM format
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(wavFilePath));
        AudioFormat format = audioInputStream.getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false);
            audioInputStream = AudioSystem.getAudioInputStream(format, audioInputStream);
        }

        File wavFile = new File(wavFilePath);
        // Write PCM data to a new WAV file
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE,wavFile);
        audioInputStream.close();
        return wavFile;
    }
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convertedFile = new File(file.getOriginalFilename());
        file.transferTo(convertedFile);
        return convertedFile;
    }

    /*public static File convertMP3ToWAV(String mp3FilePath, String wavFilePath)
            throws UnsupportedAudioFileException, IOException {
        // Read MP3 file
        File mp3File = new File(mp3FilePath);
        AudioInputStream mp3Stream = AudioSystem.getAudioInputStream(mp3File);

        // Get audio format of the MP3 file
        AudioFormat mp3Format = mp3Stream.getFormat();

        // Specify WAV format
        AudioFormat wavFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                mp3Format.getSampleRate(),
                16, // sample size in bits
                mp3Format.getChannels(),
                mp3Format.getChannels() * 2,
                mp3Format.getSampleRate(),
                false // big endian
        );

        // Create audio input stream in WAV format
        AudioInputStream wavStream = AudioSystem.getAudioInputStream(wavFormat, mp3Stream);

        // Write WAV file
        File wavFile = new File(wavFilePath);
        AudioSystem.write(wavStream, AudioFileFormat.Type.WAVE, wavFile);

        // Close streams
        mp3Stream.close();
        wavStream.close();
        return wavFile;
    }*/

    /*private static File convertMp3ToWav(File mp3File) throws IOException, InterruptedException {
        // Convert MP3 to WAV using external tool or library
        // For example, you can use ffmpeg: ffmpeg -i input.mp3 output.wav
        // Make sure to include the ffmpeg binary in your project and update the command accordingly
        ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", mp3File.getAbsolutePath(), mp3File.getAbsolutePath() + ".wav");
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to convert MP3 to WAV");
            }
            return new File(mp3File.getAbsolutePath() + ".wav");
        } finally {
            process.destroy();
        }
    }*/

}

