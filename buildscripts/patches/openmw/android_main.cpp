#if defined(stderr) && (!defined(__ANDROID_API__) || __ANDROID_API__ < 23)
int stderr = 0; // Hack: fix linker error
#endif

#include "SDL_main.h"
#include "engine.hpp"
#include <SDL_gamecontroller.h>
#include <SDL_mouse.h>
#include <SDL_events.h>
#include <SDL_hints.h>

/*******************************************************************************
 Functions called by JNI
 *******************************************************************************/
#include <jni.h>

/* Called before  to initialize JNI bindings  */

extern void SDL_Android_Init(JNIEnv* env, jclass cls);
extern int argcData;
extern const char **argvData;
void releaseArgv();

extern "C" int Java_org_libsdl_app_SDLActivity_nativeInit(JNIEnv* env, jclass cls, jobject obj) {
    setenv("OPENMW_DECOMPRESS_TEXTURES", "1", 1);

    // On Android, we use a virtual controller with guid="Virtual"
    SDL_GameControllerAddMapping("5669727475616c000000000000000000,Virtual,a:b0,b:b1,back:b15,dpdown:h0.4,dpleft:h0.8,dpright:h0.2,dpup:h0.1,guide:b16,leftshoulder:b6,leftstick:b13,lefttrigger:a5,leftx:a0,lefty:a1,rightshoulder:b7,rightstick:b14,righttrigger:a4,rightx:a2,righty:a3,start:b11,x:b3,y:b4");

    SDL_SetHint(SDL_HINT_ANDROID_BLOCK_ON_PAUSE, "0");
    SDL_SetHint(SDL_HINT_ORIENTATIONS, "LandscapeLeft LandscapeRight");

    return 0;
}

