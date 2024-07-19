cmake_minimum_required(VERSION 3.2.0)

# https://github.com/libjpeg-turbo/libjpeg-turbo/releases
set(LIBJPEG_TURBO_VERSION 1.5.3)
set(LIBJPEG_TURBO_HASH SHA256=b24890e2bb46e12e72a79f7e965f409f4e16466d00e1dd15d93d73ee6b592523)

# http://www.libpng.org/pub/png/libpng.html
set(LIBPNG_VERSION 1.6.37)
set(LIBPNG_HASH SHA256=505e70834d35383537b6491e7ae8641f1a4bed1876dbfe361201fc80868d88ca)

# https://www.freetype.org/index.html#news
set(FREETYPE2_VERSION 2.10.4)
set(FREETYPE2_HASH SHA256=5eab795ebb23ac77001cfb68b7d4d50b5d6c7469247b0b01b2c953269f658dac)

# https://github.com/kcat/openal-soft/releases
set(OPENAL_VERSION 1.21.1)
set(OPENAL_HASH SHA256=8ac17e4e3b32c1af3d5508acfffb838640669b4274606b7892aa796ca9d7467f)

# http://www.boost.org/users/download/
set(BOOST_VERSION 1.83.0)
set(BOOST_HASH SHA256=8f32d4617390d1c2d16f26a27ab60d97807b35440d45891fa340fc2648b04406)

# https://www.ffmpeg.org/download.html
set(FFMPEG_VERSION 4.4)
set(FFMPEG_HASH SHA256=42093549751b582cf0f338a21a3664f52e0a9fbe0d238d3c992005e493607d0e)

# https://www.libsdl.org/download-2.0.php
set(SDL2_VERSION 2.24.0)
set(SDL2_HASH SHA256=349268f695c02efbc9b9148a70b85e58cefbbf704abd3e91be654db7f1e2c863)

# https://github.com/bulletphysics/bullet3/releases
set(BULLET_VERSION 3.17)
set(BULLET_HASH SHA256=baa642c906576d4d98d041d0acb80d85dd6eff6e3c16a009b1abf1ccd2bc0a61)

# https://github.com/MyGUI/mygui/releases
set(MYGUI_VERSION 3.4.3)
set(MYGUI_HASH SHA256=d1d5f294670ae71f7200ed4b30859018281d8cfd45d6a38d18b97a4aba604c42)

# https://github.com/lz4/lz4/releases
set(LZ4_VERSION 1.9.3)
set(LZ4_HASH SHA256=030644df4611007ff7dc962d981f390361e6c97a34e5cbc393ddfbe019ffe2c1)

# https://luajit.org/download.html
set(LUAJIT_VERSION 505e2c03de35e2718eef0d2d3660712e06dadf1f)
set(LUAJIT_HASH SHA256=1ad2e34b111c802f9d0cdf019e986909123237a28c746b21295b63c9e785d9c3)

set(LIBICU_VERSION 70-1)
set(LIBICU_HASH SHA256=4013d7aff8a34bae410664bdb16554d7eda03654b22627b9284ab55a685492d4)

# https://github.com/OpenMW/openmw/commits/master
set(VSGOPENMW_VERSION 0.3)
set(OPENMW_HASH SHA256=44d044ad0a5546216fe611fe92c37065bb07e2efeeeaac4f049dae03df313186)

# End of configurable options

project(openmw-android)

include(ExternalProject)

set(wrapper_command ${CMAKE_BINARY_DIR}/command_wrapper.sh)
set(ndk_triplet ${NDK_TRIPLET})
set(download_dir ${CMAKE_SOURCE_DIR}/downloads/)
set(prefix ${CMAKE_INSTALL_PREFIX})
set(host ${ndk_triplet})
set(android_platform android-${ANDROID_API})
set(app_abi ${ABI})

set(COMMON_CMAKE_ARGS
	-DCMAKE_TOOLCHAIN_FILE=${CMAKE_SOURCE_DIR}/toolchain/ndk/build/cmake/android.toolchain.cmake
	-DANDROID_ABI=${app_abi}
	-DANDROID_PLATFORM=${android_platform}
	-DANDROID_STL=c++_shared
	-DANDROID_LD=deprecated
	-DANDROID_CPP_FEATURES=rtti\ exceptions
	-DCMAKE_C_FLAGS=-DMYGUI_DONT_REPLACE_NULLPTR\ -I${prefix}/include/\ $ENV{CFLAGS}
	-DCMAKE_CXX_FLAGS=-DMYGUI_DONT_REPLACE_NULLPTR\ -I${prefix}/include/\ $ENV{CXXFLAGS}
	-DCMAKE_SHARED_LINKER_FLAGS=$ENV{LDFLAGS}
	-DCMAKE_BUILD_TYPE=${BUILD_TYPE}
	-DCMAKE_DEBUG_POSTFIX=
	-DCMAKE_INSTALL_PREFIX=${prefix}
	-DCMAKE_FIND_ROOT_PATH=${prefix}
)

set(COMMON_NDK_BUILD_FLAGS
	NDK_PROJECT_PATH=.
	APP_BUILD_SCRIPT=<SOURCE_DIR>/Android.mk
	APP_PLATFORM=${android_platform}
	APP_ABI=${app_abi}
	APP_LD=deprecated
	LOCAL_LDFLAGS=$ENV{LDFLAGS}
	-j4
)

set(COMMON_AUTOCONF_FLAGS
	--enable-static
	--disable-shared
	--prefix=${prefix}
)

# ffmpeg doesn't like --host, so sandwich it between adding --host to the flags
set(ffmpeg_configure_flags ${COMMON_AUTOCONF_FLAGS})
if (${BUILD_TYPE} STREQUAL "debug")
	# ffmpeg fails to compile if we have asm enabled and optimizations disabled, so disable both
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm --disable-optimizations)
endif()

if ((${ARCH} STREQUAL "x86_64") OR (${ARCH} STREQUAL "x86") OR (${ARCH} STREQUAL "arm64"))
	# ffmpeg fails to link with assembly optimizations on x86-64
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --disable-asm)
endif()

set(COMMON_AUTOCONF_FLAGS
	--host=${host}
	${COMMON_AUTOCONF_FLAGS}
)

if (${ARCH} STREQUAL "arm")
	set(ffmpeg_configure_flags ${ffmpeg_configure_flags} --enable-thumb)
endif()

# on x86 enabling simd results in text relocations, which are banned on Android
set(libjpeg_turbo_flags "")
if (${ARCH} STREQUAL "x86")
	set(libjpeg_turbo_flags ${libjpeg_turbo_flags} --without-simd)
endif()

ExternalProject_Add(libjpeg-turbo
	URL https://sourceforge.net/projects/libjpeg-turbo/files/${LIBJPEG_TURBO_VERSION}/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}.tar.gz/download
	https://github.com/xyzz/openmw-deps/releases/download/v0/libjpeg-turbo-${LIBJPEG_TURBO_VERSION}.tar.gz
	URL_HASH ${LIBJPEG_TURBO_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}
	${libjpeg_turbo_flags}

	BUILD_COMMAND ${wrapper_command} $(MAKE) PROGRAMS=

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install-libLTLIBRARIES install-data-am
)

ExternalProject_Add(libpng
	URL http://prdownloads.sourceforge.net/libpng/libpng-${LIBPNG_VERSION}.tar.xz
	https://github.com/xyzz/openmw-deps/releases/download/v0/libpng-${LIBPNG_VERSION}.tar.xz
	URL_HASH ${LIBPNG_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE) check_PROGRAMS= bin_PROGRAMS=

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install check_PROGRAMS= bin_PROGRAMS=
)

set(libicu_flags
	--disable-tests
	--disable-samples
	--disable-icuio
	--disable-extras
	--with-cross-build=${CMAKE_SOURCE_DIR}/build/icu-host-build)

ExternalProject_Add(libicu
	URL https://github.com/unicode-org/icu/archive/refs/tags/release-${LIBICU_VERSION}.zip
	URL_HASH ${LIBICU_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/icu4c/source/configure
	${COMMON_AUTOCONF_FLAGS}
	${libicu_flags}

	BUILD_COMMAND ${wrapper_command} $(MAKE) check_PROGRAMS= bin_PROGRAMS=

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install check_PROGRAMS= bin_PROGRAMS=
)

ExternalProject_Add(freetype2
	URL https://download.savannah.gnu.org/releases/freetype/freetype-${FREETYPE2_VERSION}.tar.gz
	http://prdownloads.sourceforge.net/freetype/freetype-${FREETYPE2_VERSION}.tar.gz
	URL_HASH ${FREETYPE2_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${COMMON_AUTOCONF_FLAGS}
	--with-png=no

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(openal
	URL https://github.com/kcat/openal-soft/archive/${OPENAL_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/openal-soft-${OPENAL_VERSION}.tar.gz
	URL_HASH ${OPENAL_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DALSOFT_EXAMPLES=OFF
	-DALSOFT_TESTS=OFF
	-DALSOFT_UTILS=OFF
	-DALSOFT_NO_CONFIG_UTIL=ON
	-DALSOFT_BACKEND_OPENSL=ON
	-DALSOFT_BACKEND_WAVE=OFF

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(boost
	URL https://github.com/boostorg/boost/releases/download/boost-1.83.0/boost-1.83.0.tar.gz
	#URL_HASH ${BOOST_HASH}
	DOWNLOAD_DIR ${download_dir}

	BUILD_IN_SOURCE 1

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/bootstrap.sh
	--prefix=${prefix}

	BUILD_COMMAND ""

	INSTALL_COMMAND ${wrapper_command} ./b2
	-j4
	--with-filesystem
	--with-program_options
	--with-system
	--with-iostreams
	--with-regex
	--ignore-site-config
	toolset=clang
	architecture=${BOOST_ARCH}
	address-model=${BOOST_ADDRESS_MODEL}
	cflags=$ENV{CFLAGS}
	cxxflags=$ENV{CXXFLAGS}
	variant=release
	target-os=android
	threading=multi
	threadapi=pthread
	link=static
	runtime-link=static
	install
	COMMAND llvm-ranlib ${prefix}/lib/libboost_filesystem.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_program_options.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_system.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_iostreams.a
	COMMAND llvm-ranlib ${prefix}/lib/libboost_regex.a
)

ExternalProject_Add(ffmpeg
	URL http://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.bz2
	https://github.com/xyzz/openmw-deps/releases/download/v0/ffmpeg-${FFMPEG_VERSION}.tar.bz2
	URL_HASH ${FFMPEG_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} <SOURCE_DIR>/configure
	${ffmpeg_configure_flags}
	--target-os=android
	--enable-cross-compile
	--cross-prefix=llvm-
	--cc=${host}-clang
	--arch=${ARCH}
	--cpu=${FFMPEG_CPU}
	--enable-version3
	--enable-pic
	--disable-everything
	--disable-doc
	--disable-programs
	--disable-autodetect
	--disable-iconv
	--enable-decoder=mp3
	--enable-demuxer=mp3
	--enable-decoder=bink
	--enable-decoder=binkaudio_rdft
	--enable-decoder=binkaudio_dct
	--enable-demuxer=bink
	--enable-demuxer=wav
	--enable-decoder=pcm_*
	--enable-decoder=vp8
	--enable-decoder=vp9
	--enable-decoder=opus
	--enable-decoder=vorbis
	--enable-demuxer=matroska
	--enable-demuxer=ogg

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(sdl2
	URL https://www.libsdl.org/release/SDL2-${SDL2_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/SDL2-${SDL2_VERSION}.tar.gz
#	URL_HASH ${SDL2_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND """"
	COMMAND """"

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} ndk-build
	${COMMON_NDK_BUILD_FLAGS}

	INSTALL_COMMAND mkdir -p ${prefix}/lib/
	COMMAND cp libs/${app_abi}/libSDL2.so ${prefix}/lib/
	COMMAND cp -r <SOURCE_DIR>/include ${prefix}/
)

ExternalProject_Add(bullet
	URL https://github.com/bulletphysics/bullet3/archive/${BULLET_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/bullet3-${BULLET_VERSION}.tar.gz
	URL_HASH ${BULLET_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/bullet-fix-cmake.patch

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DBUILD_BULLET2_DEMOS=OFF
	-DBUILD_CPU_DEMOS=OFF
	-DBUILD_UNIT_TESTS=OFF
	-DBUILD_EXTRAS=OFF
	-DUSE_DOUBLE_PRECISION=ON
	-DBULLET2_MULTITHREADING=ON

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND $(MAKE) install
)

ExternalProject_Add(mygui
	DEPENDS freetype2

	URL https://github.com/MyGUI/mygui/archive/MyGUI${MYGUI_VERSION}.tar.gz
        #URL_HASH ${MYGUI_HASH}
	DOWNLOAD_DIR ${download_dir}

        #PATCH_COMMAND patch -d <SOURCE_DIR> -p1 -t -N < ${CMAKE_SOURCE_DIR}/patches/mygui-dont-set-debug-postfix.patch

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DMYGUI_RENDERSYSTEM=1
	-DMYGUI_BUILD_DEMOS=OFF
	-DMYGUI_BUILD_TOOLS=OFF
	-DMYGUI_BUILD_PLUGINS=OFF
	-DMYGUI_DONT_USE_OBSOLETE=ON
	-DFREETYPE_FT2BUILD_INCLUDE_DIR=${prefix}/include/freetype2/

	-DMYGUI_STATIC=ON

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(lz4
	URL https://github.com/lz4/lz4/archive/v${LZ4_VERSION}.tar.gz
	https://github.com/xyzz/openmw-deps/releases/download/v0/v${LZ4_VERSION}.tar.gz

	URL_HASH ${LZ4_HASH}
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>/build/cmake/
	${COMMON_CMAKE_ARGS}
	-DBUILD_STATIC_LIBS=ON
	-DBUILD_SHARED_LIBS=OFF

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

set(LUAJIT_MAKE_FLAGS
	HOST_CC=${LUAJIT_HOST_CC}
	CFLAGS=
	TARGET_CFLAGS=$ENV{CFLAGS}
	CROSS=llvm-
	STATIC_CC=${host}-clang
	DYNAMIC_CC=${host}-clang\ -fPIC
	TARGET_LD=${host}-clang
	PREFIX=${prefix}
)

ExternalProject_Add(luajit
	URL https://github.com/luaJit/LuaJIT/archive/${LUAJIT_VERSION}.tar.gz
#	URL_HASH ${LUAJIT_HASH}
	DOWNLOAD_DIR ${download_dir}

	BUILD_IN_SOURCE 1

	CONFIGURE_COMMAND ""

	BUILD_COMMAND ${wrapper_command} $(MAKE) amalg
	${LUAJIT_MAKE_FLAGS}

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
	${LUAJIT_MAKE_FLAGS}
	COMMAND bash -c "rm ${prefix}/lib/libluajit*.so*"
)

ExternalProject_Add(zlib
	URL https://github.com/madler/zlib/archive/refs/tags/v1.2.11.tar.gz
	URL_HASH SHA256=629380c90a77b964d896ed37163f5c3a34f6e6d897311f1df2a7016355c45eff
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)
	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

ExternalProject_Add(libxml
	DEPENDS zlib

	URL https://github.com/GNOME/libxml2/archive/refs/tags/v2.9.12.tar.gz
	URL_HASH SHA256=8a4ddd706419c210b30b8978a51388937fd9362c34fc9a3d69e4fcc6f8055ee0
	DOWNLOAD_DIR ${download_dir}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DBUILD_SHARED_LIBS=OFF
	-DLIBXML2_WITH_CATALOG=OFF
	-DLIBXML2_WITH_ICONV=OFF
	-DLIBXML2_WITH_LZMA=OFF
	-DLIBXML2_WITH_PROGRAMS=OFF
	-DLIBXML2_WITH_PYTHON=OFF
	-DLIBXML2_WITH_TESTS=OFF
	-DLIBXML2_WITH_ZLIB=ON

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

set(LIBCOLLADA_PATCH patch -d <SOURCE_DIR> -p0 -t -N < ${CMAKE_SOURCE_DIR}/patches/libcollada-minizip-fix.patch)

ExternalProject_Add(libcollada
	DEPENDS libxml boost

	URL https://github.com/rdiankov/collada-dom/archive/v2.5.0.tar.gz
	URL_HASH SHA256=3be672407a7aef60b64ce4b39704b32816b0b28f61ebffd4fbd02c8012901e0d
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND ${LIBCOLLADA_PATCH}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	-DBoost_USE_STATIC_LIBS=ON
	-DBoost_USE_STATIC_RUNTIME=ON
	-DBoost_NO_SYSTEM_PATHS=ON
	-DBoost_INCLUDE_DIR=${prefix}/include
	-DHAVE_STRTOQ=0
	-DUSE_FILE32API=1
	-DCMAKE_CXX_FLAGS=-Dauto_ptr=unique_ptr\ -I${prefix}/include/\ $ENV{CXXFLAGS}

	BUILD_COMMAND ${wrapper_command} $(MAKE)

	INSTALL_COMMAND ${wrapper_command} $(MAKE) install
)

set(VSGOPENMW_COMMON
	-DBUILD_BSATOOL=0
	-DBUILD_NIFTEST=0
	-DBUILD_ESMTOOL=0
	-DBUILD_LAUNCHER=0
	-DBUILD_MWINIIMPORTER=0
	-DBUILD_ESSIMPORTER=0
	-DBUILD_OPENCS=0
	-DBUILD_NAVMESHTOOL=0
	-DBUILD_WIZARD=0
	-DBUILD_MYGUI_PLUGIN=0
	-DBUILD_BULLETOBJECTTOOL=0
	-DOPENMW_USE_SYSTEM_SQLITE3=OFF
	-DOPENMW_USE_SYSTEM_YAML_CPP=OFF
	-DOPENMW_USE_SYSTEM_ICU=ON
	-DVSG_INCLUDE_DIR=${PREFIX}/include/vsg/
	-DOPENMW_USE_SYSTEM_VSGXCHANGE=FALSE
	-DOPENMW_USE_SYSTEM_VSG=OFF
	-DOPENMW_USE_SYSTEM_RECASTNAVIGATION=FALSE
	-DUSE_SYSTEM_TINYXML=FALSE
	-DVulkan_FIND_VERSION=${CMAKE_SOURCE_DIR}/toolchain/ndk/sources/third_party/vulkan/src/include
	-DOPENAL_INCLUDE_DIR=${prefix}/include/AL/
	-DBullet_INCLUDE_DIR=${prefix}/include/bullet/
	-DOSG_STATIC=TRUE
	-DMyGUI_LIBRARY=${prefix}/lib/libMyGUIEngineStatic.a
)

set(VSGOPENMW_PATCH
	cp ${CMAKE_SOURCE_DIR}/patches/openmw/defaults.bin ${CMAKE_SOURCE_DIR}/build/arm64/vsgopenmw-prefix/src/vsgopenmw-build/defaults.bin	
)

ExternalProject_Add(vsgopenmw
	DEPENDS boost openal mygui ffmpeg sdl2 bullet lz4 luajit libicu

	URL https://github.com/Duron27/vsgopenmw/archive/refs/tags/${VSGOPENMW_VERSION}.tar.gz
#	URL_HASH ${OPENMW_HASH}
	DOWNLOAD_DIR ${download_dir}

	PATCH_COMMAND ${VSGOPENMW_PATCH}

	CONFIGURE_COMMAND ${wrapper_command} cmake <SOURCE_DIR>
	${COMMON_CMAKE_ARGS}
	${VSGOPENMW_COMMON}
	BUILD_COMMAND $(MAKE)

	INSTALL_COMMAND ""
)
