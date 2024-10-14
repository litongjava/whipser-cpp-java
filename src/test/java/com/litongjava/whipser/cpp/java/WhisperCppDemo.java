package com.litongjava.whipser.cpp.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.litongjava.whipser.cpp.java.bean.WhisperSegment;
import com.litongjava.whipser.cpp.java.callbacks.WhisperProgressCallback;
import com.litongjava.whipser.cpp.java.params.CBool;
import com.litongjava.whipser.cpp.java.params.WhisperFullParams;
import com.litongjava.whipser.cpp.java.params.WhisperSamplingStrategy;

public class WhisperCppDemo {

  private static WhisperCpp whisper = new WhisperCpp();
  private static boolean modelInitialised = false;

  public static void main(String[] args) {

    // By default, models are loaded from ~/.cache/whisper/ and are usually named "ggml-${name}.bin"
    // or you can provide the absolute path to the model file.
    // String modelName = "ggml-base.en.bin"; // load ggml-base.en.bin
    String modelName = "ggml-large-v3-turbo.bin"; // load ggml-large-v3-turbo.bin
    try {
      whisper.initContext(modelName);
      modelInitialised = true;
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
      System.out.println("Model " + modelName + " not found");
      return;
    }
    if (!modelInitialised) {
      System.out.println("Model not initialised, skipping test");
      return;
    }

    WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
    WhisperProgressCallback callback = (ctx, state, progress, user_data) -> System.out.println("progress: " + progress);

    params.setProgressCallback(callback);
    params.print_progress = CBool.FALSE;

    // Given
    File file = new File(System.getProperty("user.dir"), "/samples/jfk.wav");

    List<WhisperSegment> segments = transcribe(params, file);
    System.out.println(segments.size());

    for (WhisperSegment segment : segments) {
      System.out.println(segment);
    }
  }

  private static List<WhisperSegment> transcribe(WhisperFullParams params, File file) {
    try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);) {
      byte[] b = new byte[audioInputStream.available()];
      float[] floats = new float[b.length / 2];
      audioInputStream.read(b);

      for (int i = 0, j = 0; i < b.length; i += 2, j++) {
        int intSample = (int) (b[i + 1]) << 8 | (int) (b[i]) & 0xFF;
        floats[j] = intSample / 32767.0f;
      }

      return whisper.fullTranscribeWithTime(params, floats);
    } catch (IOException e) {
      e.printStackTrace();

    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    }
    return null;
  }
}
