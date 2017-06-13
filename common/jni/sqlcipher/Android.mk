LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := sqlcipher-prebuilt
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libsqlcipher.so
include $(PREBUILT_SHARED_LIBRARY)

