package com.litongjava.whipser.cpp.java.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Callback for progress updates.
 */
public interface WhisperProgressCallback extends Callback {

  /**
   * Callback method for progress updates.
   *
   * @param ctx        The whisper context.
   * @param state      The whisper state.
   * @param progress   The progress value.
   * @param user_data  User data.
   */
  void callback(Pointer ctx, Pointer state, int progress, Pointer user_data);
}
