#include<jni.h>
#include<filesystem>

// TODO: compile
JNIEXPORT void JNICALL Java_dev_liambloom_checker_internal_Native_changeDirectory
  (JNIEnv *, jclass, jstring) {
    std::filesystem::current_path(env->GetStringUTFChars(str, NULL));
}