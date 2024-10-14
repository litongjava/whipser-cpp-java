# Java JNI Bindings for Whisper

此包为 `whisper.cpp` 提供了 Java JNI 绑定。已在以下平台上成功测试：

- ~~Darwin (OS X) 12.6 on x64_64~~
- Ubuntu on x86_64
- Windows on x86_64

主要的“低级”绑定位于 `WhisperCppJnaLibrary`。其基本用法如下：

JNA 将尝试从以下路径加载 `whispercpp` 共享库：

- `jna.library.path`
- `jna.platform.library`
- `~/Library/Frameworks`
- `/Library/Frameworks`
- `/System/Library/Frameworks`
-  classpath

## 导入
maven
```
<dependency>
    <groupId>com.litongjava</groupId>
    <artifactId>whisper-cpp-java</artifactId>
    <version>1.0.0</version>
</dependency>
```
gradle
```
implementation "com.litongjava:whisper-cpp-java:1.0.0"
```
## 测试

### 加载模型

1. 下载 `whisper` 库或编译它以生成所需的库文件。来源：[Whisper GitHub 仓库](https://github.com/ggerganov/whisper.cpp)
2. 将 `whisper` 库放在适当的 [JNA 库路径](https://java-native-access.github.io/jna/4.2.1/com/sun/jna/NativeLibrary.html) 中。注意：对于 Windows 用户，`.dll` 文件已经包含在 `.jar` 中，但如果需要可以更新：

```bash
src\main\resources\win32-x86-64\whisper.dll
```

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

### 样本测试

1. 从以下位置下载模型：
   - [Hugging Face 模型](https://huggingface.co/ggerganov/whisper.cpp)
   - [GGML 模型](https://ggml.ggerganov.com)
  
2. 将模型存储在 `~/.cache/whisper/` 中。默认情况下，模型从此位置获取，通常命名为 "ggml-${name}.bin"。
3. 下载样本文件，如 [jfk.wav](https://github.com/ggerganov/whisper.cpp/blob/master/samples/jfk.wav)，并将其放在 `samples` 文件夹中。

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

```

**输出：**
```
[0 --> 1100]: And so, my fellow Americans, ask not what your country can do for you—ask what you can do for your country.
```
注意：这里的 1100 表示毫秒除以 10。

## 构建

要构建，请确保已安装 JDK 8 或更高版本。您可以使用以下命令运行测试：

```bash
git clone https://github.com/litongjava/whisper-cpp-java.git
cd whisper-cpp-java
mvn install -DskipTests -Dgpg.skip
```

## 许可证

这些 Java 绑定的许可证与整个 `whisper.cpp` 项目的许可证相匹配，该项目采用 MIT 许可证。请参考 [`LICENSE`](https://github.com/ggerganov/whisper.cpp/blob/master/LICENSE) 文件以获得详细信息。