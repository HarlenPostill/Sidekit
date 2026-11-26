#include <jni.h>
#include <android/bitmap.h>
#include <mgba/flags.h>
#include <mgba/core/core.h>
#include <mgba/core/blip_buf.h>
#include <stdlib.h>
#include <string.h>

struct Session { struct mCore* core; color_t pixels[256 * 224]; unsigned width, height; };
#define JNI_METHOD(name) Java_hrln_interactive_sidekit_apps_emulator_EmulatorCore_##name

JNIEXPORT jlong JNICALL JNI_METHOD(open)(JNIEnv* env, jobject self, jstring path) {
    const char* filename = (*env)->GetStringUTFChars(env, path, NULL);
    struct mCore* core = mCoreFind(filename);
    struct Session* s = NULL;
    if (core && core->init(core)) {
        mCoreInitConfig(core, "sidekit");
        mCoreConfigSetDefaultIntValue(&core->config, "useBios", 0);
        mCoreLoadConfig(core);
        if (mCoreLoadFile(core, filename)) {
            s = calloc(1, sizeof(*s));
            if (s) {
                s->core = core;
                core->setVideoBuffer(core, s->pixels, 256);
                core->setAudioBufferSize(core, 2048);
                core->reset(core);
                core->desiredVideoDimensions(core, &s->width, &s->height);
                for (int ch = 0; ch < 2; ++ch)
                    blip_set_rates(core->getAudioChannel(core, ch), core->frequency(core), 48000);
            }
        }
        if (!s) { mCoreConfigDeinit(&core->config); core->deinit(core); }
    }
    (*env)->ReleaseStringUTFChars(env, path, filename);
    return (jlong)(intptr_t)s;
}
JNIEXPORT jint JNICALL JNI_METHOD(dimensions)(JNIEnv* env, jobject self, jlong handle) {
    struct Session* s = (void*)(intptr_t)handle;
    return (s->width << 16) | s->height;
}
JNIEXPORT jint JNICALL JNI_METHOD(frame)(JNIEnv* env, jobject self, jlong handle, jint keys, jobject bitmap, jshortArray audio) {
    struct Session* s = (void*)(intptr_t)handle;
    s->core->setKeys(s->core, keys);
    s->core->runFrame(s->core);
    AndroidBitmapInfo info;
    void* pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) == 0 && AndroidBitmap_lockPixels(env, bitmap, &pixels) == 0) {
        for (unsigned y = 0; y < s->height; ++y) {
            uint32_t* row = (uint32_t*)((char*)pixels + y * info.stride);
            for (unsigned x = 0; x < s->width; ++x) row[x] = s->pixels[y * 256 + x] | 0xFF000000;
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
    short samples[4096];
    int count = blip_read_samples(s->core->getAudioChannel(s->core, 0), samples, 2048, 1);
    blip_read_samples(s->core->getAudioChannel(s->core, 1), samples + 1, count, 1);
    (*env)->SetShortArrayRegion(env, audio, 0, count * 2, samples);
    return count * 2;
}
JNIEXPORT jbyteArray JNICALL JNI_METHOD(save)(JNIEnv* env, jobject self, jlong handle) {
    struct Session* s = (void*)(intptr_t)handle;
    void* data = NULL;
    size_t size = s->core->savedataClone(s->core, &data);
    jbyteArray bytes = (*env)->NewByteArray(env, size);
    if (size) (*env)->SetByteArrayRegion(env, bytes, 0, size, data);
    free(data);
    return bytes;
}
JNIEXPORT jboolean JNICALL JNI_METHOD(restore)(JNIEnv* env, jobject self, jlong handle, jbyteArray bytes) {
    struct Session* s = (void*)(intptr_t)handle;
    jbyte* data = (*env)->GetByteArrayElements(env, bytes, NULL);
    bool ok = s->core->savedataRestore(s->core, data, (*env)->GetArrayLength(env, bytes), false);
    (*env)->ReleaseByteArrayElements(env, bytes, data, JNI_ABORT);
    return ok;
}
JNIEXPORT void JNICALL JNI_METHOD(close)(JNIEnv* env, jobject self, jlong handle) {
    struct Session* s = (void*)(intptr_t)handle;
    mCoreConfigDeinit(&s->core->config);
    s->core->deinit(s->core);
    free(s);
}
