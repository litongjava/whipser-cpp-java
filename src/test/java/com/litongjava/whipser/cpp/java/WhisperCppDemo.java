package com.litongjava.whipser.cpp.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.litongjava.whipser.cpp.java.bean.WhisperSegment;
import com.litongjava.whipser.cpp.java.params.CBool;
import com.litongjava.whipser.cpp.java.params.WhisperFullParams;
import com.litongjava.whipser.cpp.java.params.WhisperSamplingStrategy;

public class WhisperCppDemo {

  private static WhisperCpp whisper = new WhisperCpp();
  private static boolean modelInitialised = false;

  public static void main(String[] args) {

    // By default, models are loaded from ~/.cache/whisper/ and are usually named "ggml-${name}.bin"
    // or you can provide the absolute path to the model file.
    String modelName = "base.en";
    try {
      whisper.initContext(modelName);
      modelInitialised = true;
    } catch (FileNotFoundException ex) {
      System.out.println("Model " + modelName + " not found");
    }
    if (!modelInitialised) {
      System.out.println("Model not initialised, skipping test");
      return;
    }

    WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
    params.setProgressCallback((ctx, state, progress, user_data) -> System.out.println("progress: " + progress));
    params.print_progress = CBool.FALSE;

    // Given
    File file = new File(System.getProperty("user.dir"), "/samples/jfk.wav");

    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);) {
      byte[] b = new byte[audioInputStream.available()];
      float[] floats = new float[b.length / 2];
      audioInputStream.read(b);

      for (int i = 0, j = 0; i < b.length; i += 2, j++) {
        int intSample = (int) (b[i + 1]) << 8 | (int) (b[i]) & 0xFF;
        floats[j] = intSample / 32767.0f;
      }

      List<WhisperSegment> segments = whisper.fullTranscribeWithTime(params, floats);
      System.out.println(segments.size());

      for (WhisperSegment segment : segments) {
        System.out.println(segment);
      }
    } catch (IOException e) {
      e.printStackTrace();

    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    }
  }
}
