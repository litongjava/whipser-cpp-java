# Java JNI Bindings for Whisper
[English](README.md)|[中文](readme_cn.md)
This package offers Java JNI bindings for `whisper.cpp`. The following platforms have been successfully tested:

- ~~Darwin (OS X) 12.6 on x64_64~~
- Ubuntu on x86_64
- Windows on x86_64

The primary "low-level" bindings can be found in `WhisperCppJnaLibrary`. A basic example of its usage is:

JNA will try to load the `whispercpp` shared library from the following paths:

- `jna.library.path`
- `jna.platform.library`
- `~/Library/Frameworks`
- `/Library/Frameworks`
- `/System/Library/Frameworks`
- classpath

## Testing

### Loading the Model

1. Download the `whisper` library or compile it to generate the required library files. Source: [Whisper GitHub Repository](https://github.com/ggerganov/whisper.cpp)
2. Place the `whisper` library in the appropriate [JNA library path](https://java-native-access.github.io/jna/4.2.1/com/sun/jna/NativeLibrary.html). Note: For Windows users, the `.dll` file is already included in the `.jar`, but can be updated if needed:

```bash
src\main\resources\win32-x86-64\whisper.dll
```

### Sample Test

```java
package com.litongjava.whipser.cpp.java;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WhisperJnaLibraryTest {

  @Test
  void testWhisperPrint_system_info() {
    String systemInfo = WhisperCppJnaLibrary.instance.whisper_print_system_info();
    // eg: "AVX = 1 | AVX2 = 1 | AVX512 = 0 | FMA = 1 | NEON = 0 | ARM_FMA = 0 | F16C = 1 | FP16_VA = 0
    // | WASM_SIMD = 0 | BLAS = 0 | SSE3 = 1 | VSX = 0 | COREML = 0 | "
    System.out.println("System info: " + systemInfo);
    assertTrue(systemInfo.length() > 10);
  }
}
```

### Using the Model

1. Download the model from:
   - [Hugging Face Model](https://huggingface.co/ggerganov/whisper.cpp)
   - [GGML Model](https://ggml.ggerganov.com)
  
2. Store the model in `~/.cache/whisper/`. By default, models are sourced from this location and typically have the naming convention "ggml-${name}.bin".
3. Download sample files like [jfk.wav](https://github.com/ggerganov/whisper.cpp/blob/master/samples/jfk.wav) and place them in the `samples` folder.

```java
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
```

**Output:**
```
[0 --> 1100]: And so, my fellow Americans, ask not what your country can do for you—ask what you can do for your country.
```
Note: Here, 1100 represents milliseconds divided by 10.

## Building

To build, ensure you have JDK 8 or higher installed. You can run the tests with the following commands:

```bash
git clone https://github.com/litongjava/whisper-cpp-java.git
cd whisper-cpp-java
mvn install -DskipTests -Dgpg.skip
```
## Java Whisper Cpp Server
[https://github.com/litongjava/ai-server](https://github.com/litongjava/ai-server)

## License

The licensing for these Java bindings matches that of the entire `whisper.cpp` project, which is under the MIT License. Please refer to the [`LICENSE`](https://github.com/ggerganov/whisper.cpp/blob/master/LICENSE) file for comprehensive details.