LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE	:= arm

LOCAL_MODULE    := audiopairing

LOCAL_CPPFLAGS := -g   -std=c++11
LOCAL_CFLAGS :=  -std=c99


LOCAL_SRC_FILES += \
	./audiopairing-jni.cpp \
	./src/AudioAnalyzer.cpp \
	./src/AudioMessageReader.cpp \
	./src/MessageAssembler.cpp \
	./src/MessageRetainer.cpp \
	./src/RingBuffer.cpp \
	./src/crc.c \
	./src/fft.c \
	./src/AudioPairingEncoder.cpp \
	./src/AudioPairingFileGenerator.cpp



LOCAL_C_INCLUDES := \
	${LOCAL_PATH}/include/


LOCAL_LDLIBS = -llog

include $(BUILD_SHARED_LIBRARY)

