#include "audiopairing-jni.h"


#define __STDC_FORMAT_MACROS


#include <jni.h>
#include <assert.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <iostream>
#include <fstream>
#include <inttypes.h>

#include "AudioMessageReader.hpp"
#include "MessageRetainer.hpp"

#include "AudioPairingEncoder.hpp"
#include "AudioPairingFileGenerator.hpp"


/// for log
#include <android/log.h>
#define TAG "AUDIOPAIRING_JNI"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)


MessageRetainer retainer;
AudioMessageReader audioMessageReader(&retainer, AudioParams(48000.0f));

ReceptionInfo receptionInfo;


DEFINE_AUDIOPAIRING_JNI(jobject, checkForToken)(JNIEnv* env, jclass clazz, jobject buffer)
{
    jstring  tokenjString;

    uint64_t token = 0;

    jfloat* audioData = (float*) env->GetDirectBufferAddress (buffer);
    jsize length = env->GetDirectBufferCapacity(buffer);

    jclass metricsClass = env->FindClass("com/cisco/spark/android/room/audiopairing/UltrasoundMetrics");
    if (metricsClass == 0) {
        LOGE("checkForToken, failed to find class UltrasoundMetrics");
        return 0;
    }

    jobject metricsObject = env->AllocObject(metricsClass);
    if (metricsObject == 0) {
        LOGE("checkForToken, could not create the object");
        return 0;
    }


    jfieldID noiseLevelId = env->GetFieldID(metricsClass, "noiseLevel", "F");
    if (noiseLevelId == 0) {
        LOGE("checkForToken did not find field noiseLevel float");
        return 0;
    }
    jfieldID signalLevelId = env->GetFieldID(metricsClass, "signalLevel", "F");
    if (signalLevelId == 0) {
        LOGE("checkForToken did not find field signalLevel float");
        return 0;
    }
    jfieldID errorCorrectionCountId = env->GetFieldID(metricsClass, "errorCorrectionCount", "I");
    if (errorCorrectionCountId == 0) {
        LOGE("checkForToken did not find field signalLevel float");
        return 0;
    }

    jfieldID tokenFieldId = env->GetFieldID(metricsClass, "token", "Ljava/lang/String;");
    if (tokenFieldId == 0) {
        LOGE("checkForToken no field found: String token");
        return 0;
    }

    jfieldID typeFieldId = env->GetFieldID(metricsClass, "tokenType", "I");
    if (typeFieldId == 0) {
        LOGE("checkForToken did not find field tokenType int");
        return 0;
    }


    retainer.get_and_reset_message_flag();
    audioMessageReader.receiveAudioData(audioData, length*sizeof(float));

    env->SetFloatField(metricsObject, noiseLevelId, audioMessageReader.getUltrasoundNoiseLevel());
    env->SetFloatField(metricsObject, signalLevelId, audioMessageReader.getUltrasoundSignalLevel());

    bool gotMessage = retainer.get_and_reset_message_flag();

    receptionInfo = retainer.reception_info();

    if ( gotMessage ) {
        int messageLength = retainer.messageLength();
        // Error correction count is only set if we got a message
        env->SetIntField(metricsObject, errorCorrectionCountId, receptionInfo.errorCorrectionCount);


        if  ( messageLength == 7 ) {
            const unsigned char *message = retainer.message();

            token = ((uint64_t)message[0] << 40) | ((uint64_t)message[1] << 32) | ((uint64_t)message[2] << 24)
             | ((uint64_t)message[3] << 16) | ((uint64_t)message[4] << 8) | ((uint64_t)message[5]);

            char tokenString [sizeof(uint64_t)*8+1];
            sprintf(tokenString, "%012llX", token);
            LOGI("checkForToken(), tokenString = %s noiseLevel = %1.6f signalLevel = %1.6f ECC = %d type = %s", tokenString, receptionInfo.noiseLevel, receptionInfo.signalLevel, receptionInfo.errorCorrectionCount,
                 message[6] == 1 ? "Alto" : "Spark");
            tokenjString = env->NewStringUTF(tokenString);

            // Set the bit flag for Alto or Spark token.  Using an int currently
            // but might want this to be a flag or boolean.
            env->SetIntField(metricsObject, typeFieldId, message[6]);
        }
    }
    else {
        tokenjString = env->NewStringUTF("");
    }
    env->SetObjectField(metricsObject, tokenFieldId, tokenjString);
    return metricsObject;
}



DEFINE_AUDIOPAIRING_JNI(jint, generateTokenWavFile)(JNIEnv* env, jclass clazz, jstring jstringTokenString, jstring jstringPlayoutFileName, jstring jstringPairingSoundFilesPath)
{
    const char *nativeTokenString = env->GetStringUTFChars(jstringTokenString, NULL);
    std::string tokenString(nativeTokenString);

    const char *nativePlayoutFileName = env->GetStringUTFChars(jstringPlayoutFileName, NULL);
    std::string playoutFileName(nativePlayoutFileName);

    const char *nativePairingSoundFilesPath = env->GetStringUTFChars(jstringPairingSoundFilesPath, NULL);
    std::string pairingSoundFilesPath(nativePairingSoundFilesPath);

    std::string playout_string = AudioPairing::generatePlayoutString(tokenString);

    int ret_code = AudioPairing::generatePairingSequenceFile(0U, playout_string, playoutFileName, pairingSoundFilesPath);

    LOGD("generateTokenWavFile(), return code = %d", ret_code);


    env->ReleaseStringUTFChars(jstringTokenString, nativeTokenString);
    env->ReleaseStringUTFChars(jstringPlayoutFileName, nativePlayoutFileName);
    env->ReleaseStringUTFChars(jstringPairingSoundFilesPath, nativePairingSoundFilesPath);

    return ret_code;
}

DEFINE_AUDIOPAIRING_JNI(jstring, encodeProximityToken)(JNIEnv *env, jclass clazz, jstring jstringTokenString)
{
    const char *nativeTokenString = env->GetStringUTFChars(jstringTokenString, NULL);
    std::string tokenString(nativeTokenString);

    std::string encodedToken = AudioPairing::generatePlayoutString(tokenString);

    jstring jstringEncodedToken = env->NewStringUTF(encodedToken.c_str());

    env->ReleaseStringUTFChars(jstringTokenString, nativeTokenString);

    return jstringEncodedToken;
}

