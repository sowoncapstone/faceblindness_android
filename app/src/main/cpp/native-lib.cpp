#include <jni.h>
#include <string>
#include <iostream>
#include <fstream>


extern "C"
JNIEXPORT jstring JNICALL
Java_com_sowon_faceblindness_1android_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_sowon_faceblindness_1android_MainActivity_readBinFile(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "";

    return env->NewStringUTF(hello.c_str());
}
