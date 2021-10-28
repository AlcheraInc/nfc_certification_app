// https://stackoverflow.com/questions/57698796/cannot-detect-opencv-libs-after-update-form-3-4-3-to-4-1-1/58224079#58224079

#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
whatever(
        JNIEnv *env,
        jobject /* this */){
    std::string hello = "Hello";
    return env->NewStringUTF(hello.c_str());
};
