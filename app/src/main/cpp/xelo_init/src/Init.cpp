#include <jni.h>
#include <string>
#include <dlfcn.h>
#include "Runtime.h"

std::string getCurrentSharedObjectPath() {
    Dl_info info{};
    if (dladdr(reinterpret_cast<void*>(&getCurrentSharedObjectPath), &info) && info.dli_fname) {
        return {info.dli_fname};
    }
    return {};
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_origin_launcher_Launcher_LauncherApplication_nativeSetupRuntime(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring modsPath
) {
    const char* path = env->GetStringUTFChars(modsPath, nullptr);

    runtime::init(getCurrentSharedObjectPath());
    runtime::addLdLibraryPaths({std::string(path)});

    env->ReleaseStringUTFChars(modsPath, path);
}

} // extern "C"