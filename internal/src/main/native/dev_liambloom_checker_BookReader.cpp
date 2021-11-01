#include<jni.h>
#include<filesystem>

JNIEXPORT void JNICALL Java_dev_liambloom_checker_BookReader_changeDirectory
  (JNIEnv* env, jclass thisClass, jstring str) {
    std::filesystem::current_path(env->GetStringUTFChars(str, NULL));
}