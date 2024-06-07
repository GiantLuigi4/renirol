    #include "tfc_renirol_util_windows_ReniUser32.h"
    #include "pch.h"

    JNIEXPORT jlong JNICALL Java_tfc_renirol_util_windows_ReniUser32_SetCapture
      (JNIEnv *, jclass, jlong hwnd) {
        return (jlong) SetCapture((HWND) hwnd);
    }
    JNIEXPORT jlong JNICALL Java_tfc_renirol_util_windows_ReniUser32_GetCapture
      (JNIEnv *, jclass) {
        return (jlong) GetCapture();
    }
    JNIEXPORT jboolean JNICALL Java_tfc_renirol_util_windows_ReniUser32_ReleaseCapture
      (JNIEnv *, jclass) {
        return (jboolean) ReleaseCapture();
    }
    JNIEXPORT jint JNICALL Java_tfc_renirol_util_windows_ReniUser32_GetDoubleClickTime
      (JNIEnv *, jclass) {
      return (jint) GetDoubleClickTime();
    }
    JNIEXPORT void JNICALL Java_tfc_renirol_util_windows_ReniUser32_GetClientRect
      (JNIEnv *, jclass, jlong hwnd, jlong rect) {
        GetClientRect((HWND) hwnd, (RECT*) rect);
    }
