#!/bin/bash

set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

export API="24"
export NDK_VERSION="r26b"

export SHARED_LIBRARIES=20240812
export OPENMW_VERSION=317aee134f18e5fe3594a4f5ce76df1ed1a34f78

export BUILD_TYPE="release"
export CXXFLAGS="-fPIC -O3 -frtti -fexceptions -flto=thin"
export LDFLAGS="-fPIC -Wl,--undefined-version"

# Download NDK and unzip
if [[ -d android-ndk-${NDK_VERSION} ]]; then
	echo "NDK version ${NDK_VERSION} already installed!"
else
	echo "Downloading and unzipping the NDK"
	wget -q https://dl.google.com/android/repository/android-ndk-${NDK_VERSION}-linux.zip && unzip android-ndk-${NDK_VERSION}-linux.zip -d $DIR/
	rm android-ndk-${NDK_VERSION}-linux.zip
fi

# Download Prebuilt Libraries and extract
if [[ -d $DIR/android-ndk-r26b/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/man ]]; then
	echo "shared libraries openmw-android-deps-${SHARED_LIBRARIES} already installed!"
else
	echo "==> Installing shared libraries"
	wget -c https://gitlab.com/cavebros/openmw-deps/-/raw/main/android/openmw-android-deps-${SHARED_LIBRARIES}.tar.xz -O - | tar -xJ -C $DIR/android-ndk-${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr
fi

# Download openmw
if [[ -f $DIR/openmw-${OPENMW_VERSION}/build/libopenmw.so ]]; then
	echo "OpenMW already installed using NDK version ${NDK_VERSION}!"
else
	echo "Downloading, applying patches and compiling openmw for android!"
	rm -rf $DIR/openmw-${OPENMW_VERSION}
	wget -c https://github.com/OpenMW/openmw/archive/${OPENMW_VERSION}.tar.gz -O - | tar -xz -C .

	# Elsids Android Library Patch
	wget -qO- https://gitlab.com/OpenMW/openmw/-/merge_requests/4221.patch | patch -d $DIR/openmw-${OPENMW_VERSION}/ -p1 -t -N

	# Removing the top 4 lines since after API 23 int cant be used for sdtout
	sed -i '1,4d' $DIR/openmw-${OPENMW_VERSION}/apps/openmw/android_main.cpp

	mkdir -p $DIR/openmw-${OPENMW_VERSION}/build && cd $_
	cmake ../ \
		-DANDROID_ABI=arm64-v8a \
		-DANDROID_ALLOW_UNDEFINED_VERSION_SCRIPT_SYMBOLS=ON \
		-DANDROID_CPP_FEATURES= \
		-DANDROID_PLATFORM=${API} \
		-DANDROID_STL=c++_shared \
		-DCMAKE_BUILD_TYPE=${BUILD_TYPE} \
		-DCMAKE_CXX_FLAGS="${CXXFLAGS}" \
		-DCMAKE_TOOLCHAIN_FILE=$DIR/android-ndk-${NDK_VERSION}/build/cmake/android.toolchain.cmake \
		-DBUILD_BSATOOL=0 \
		-DBUILD_BULLETOBJECTTOOL=0 \
		-DBUILD_ESMTOOL=0 \
		-DBUILD_ESSIMPORTER=0 \
		-DBUILD_LAUNCHER=0 \
		-DBUILD_MWINIIMPORTER=0 \
		-DBUILD_NAVMESHTOOL=0 \
		-DBUILD_NIFTEST=0 \
		-DBUILD_OPENCS=0 \
		-DBUILD_WIZARD=0 \
		-DMyGUI_LIBRARY=$DIR/android-ndk-${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/libMyGUIEngineStatic.a \
		-DOPENMW_USE_SYSTEM_ICU=ON \
		-DOPENMW_USE_SYSTEM_SQLITE3=OFF \
		-DOPENMW_USE_SYSTEM_YAML_CPP=OFF \
		-DOSG_STATIC=TRUE

	make -j $(nproc)
	cd ..
fi

rm -rf ../app/src/main/jniLibs/$ABI/
mkdir -p ../app/src/main/jniLibs/$ABI/

# libopenmw.so is a special case
find $DIR/openmw-${OPENMW_VERSION}/build -iname "libopenmw.so" -exec cp "{}" $DIR/../app/src/main/jniLibs/$ABI/libopenmw.so \;

# copy over libs we compiled
cp $DIR/android-ndk-${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/{libopenal,libSDL2,libGL,libcollada-dom2.5-dp}.so ../app/src/main/jniLibs/$ABI/
# copy over libc++_shared
find $DIR/android-ndk-${NDK_VERSION}/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/$NDK_TRIPLET -iname "libc++_shared.so" -exec cp "{}" ../app/src/main/jniLibs/$ABI/ \;

echo "==> Deploying resources"
DST=$DIR/../app/src/main/assets/libopenmw/
SRC=$DIR/openmw-${OPENMW_VERSION}/build/

rm -rf "$DST" && mkdir -p "$DST"

# resources
cp -r "$SRC/resources" "$DST"

# global config
mkdir -p "$DST/openmw/"
cp "$SRC/defaults.bin" "$DST/openmw/"
cp "$SRC/gamecontrollerdb.txt" "$DST/openmw/"
cat "$SRC/openmw.cfg" | grep -v "data=" | grep -v "data-local=" >> "$DST/openmw/openmw.base.cfg"
cat "$DIR/../app/openmw.base.cfg" >> "$DST/openmw/openmw.base.cfg"

echo "==> Success"
