LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := histogram_native
LOCAL_SRC_FILES := histogram_native.cpp
LOCAL_LDLIBS := -ljnigraphics -llog
LOCAL_CPP_FEATURES := exceptions
include $(BUILD_SHARED_LIBRARY)
