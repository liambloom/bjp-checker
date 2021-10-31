#include<jni.h>
#include<filesystem>

JNIEXPORT void JNICALL Java_dev_liambloom_checker_BookReader_changeDirectory
  (JNIEnv* env, jclass thisClass, jstring str) {
  const std::filesystem::path path = str;
  std::filesystem::current_path(path)
}