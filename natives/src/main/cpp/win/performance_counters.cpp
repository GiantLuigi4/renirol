    #include "tfc_renirol_util_windows_PerformanceCounters.h"
    #include "pch.h"

    JNIEXPORT jlong JNICALL Java_tfc_renirol_util_windows_PerformanceCounters_QueryPerformanceFrequency
      (JNIEnv *, jclass) {
      LARGE_INTEGER freq;
      QueryPerformanceFrequency(&freq);
      return (jlong) freq.QuadPart;
    }
    JNIEXPORT jlong JNICALL Java_tfc_renirol_util_windows_PerformanceCounters_QueryPerformanceCounter
      (JNIEnv *, jclass) {
      LARGE_INTEGER time;
      QueryPerformanceCounter(&time);
      return (jlong) time.QuadPart;
    }
    JNIEXPORT jlong JNICALL Java_tfc_renirol_util_windows_PerformanceCounters_GetTickCount
      (JNIEnv *, jclass) {
      return (jlong) GetTickCount();
    }
