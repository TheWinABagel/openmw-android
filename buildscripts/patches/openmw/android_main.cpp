#if defined(stderr) && (!defined(__ANDROID_API__) || __ANDROID_API__ < 23)
int stderr = 0; // Hack: fix linker error
#endif

#include <SDL.h>
#include "SDL_main.h"
#include "engine.hpp"
#include <SDL_events.h>
#include <SDL_hints.h>

#include <jni.h>

/* Called before  to initialize JNI bindings  */

extern void SDL_Android_Init(JNIEnv* env, jclass cls);
extern int argcData;
extern const char **argvData;
void releaseArgv();

extern "C" int Java_org_libsdl_app_SDLActivity_nativeInit(JNIEnv* env, jclass cls, jobject obj) {
    setenv("OPENMW_DECOMPRESS_TEXTURES", "1", 1);
    SDL_SetHint(SDL_HINT_ANDROID_BLOCK_ON_PAUSE, "0");
    SDL_SetHint(SDL_HINT_ORIENTATIONS, "LandscapeLeft LandscapeRight");
    return 0;
}
