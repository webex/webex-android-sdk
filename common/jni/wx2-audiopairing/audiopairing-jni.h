#ifndef _AUDIOPAIRING_JNI_H_
#define _AUDIOPAIRING_JNI_H_

#include <jni.h>

#ifdef __cplusplus
#define EXTERNC extern "C"
#else
#define EXTERNC
#endif

#ifndef DEFINE_AUDIOPAIRING_JNI
#define DEFINE_AUDIOPAIRING_JNI(r, f) EXTERNC JNIEXPORT r JNICALL Java_com_cisco_spark_android_room_audiopairing_AudioPairingNative_##f
#endif


DEFINE_AUDIOPAIRING_JNI(jobject, checkForToken)(JNIEnv* env, jclass clazz, jobject audioDataArray);

DEFINE_AUDIOPAIRING_JNI(jint, generateTokenWavFile)(JNIEnv* env, jclass clazz, jstring tokenString, jstring playoutFileName, jstring jstringPairingSoundFilesPath);


#endif // _AUDIOPAIRING_JNI_H_

