#include "jni.h"
#include "miniz/zip.h"

JNIEXPORT jint JNICALL
Java_org_dweb_1browser_ziplib_AndroidMiniz_unzipFromJNI(JNIEnv *env, jobject thiz,
                                                        jstring zip_file_path, jstring dest_path) {
    return zip_extract(zip_file_path, dest_path, NULL, NULL);
}
