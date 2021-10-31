#include<jni.h>
#include<filesystem>

JNIEXPORT void JNICALL Java_dev_liambloom_checker_BookReader_changeDirectory
  (JNIEnv* env, jclass thisClass, jstring str) {
  const char* strChars = env->GetStringUTFChars(str, NULL);
  const std::filesystem::path path = strChars;
  std::filesystem::current_path(path);
}