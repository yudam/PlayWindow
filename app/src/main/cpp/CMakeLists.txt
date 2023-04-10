# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.

project("window")

# 设置so库生成的路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})


# 设置头文件搜索路径，这样可以直接添加头文件，而不需要加上头文件的路径
include_directories(${CMAKE_SOURCE_DIR}/include/)
include_directories(${CMAKE_SOURCE_DIR}/thirdparty/ffmpeg/)
include_directories(${CMAKE_SOURCE_DIR}/thirdparty/ffmpeg/include/)
include_directories(${CMAKE_SOURCE_DIR}/thirdparty/ffmpeg/libyuv/)

# 输出msg

message("CMAKE_CURRENT_SOURCE_DIR:"+${CMAKE_CURRENT_SOURCE_DIR})
message("CMAKE_SOURCE_DIR:"+${CMAKE_SOURCE_DIR})
message("PROJECT_SOURCE_DIR:"+${PROJECT_SOURCE_DIR})
message("PROJECT_NAME:"+${PROJECT_NAME})
message("ANDROID_ABI:"+${ANDROID_ABI})


# 对ffmpeg相关的库设置别名 方便链接
set(FFMPEG_libavcodec ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libavcodec.a)
set(FFMPEG_libavfilter ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libavfilter.a)
set(FFMPEG_libavformat ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libavformat.a)
set(FFMPEG_libavutil ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libavutil.a)
set(FFMPEG_libcrypto ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libcrypto.a)
set(FFMPEG_libfdk_aac ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libfdk-aac.a)
set(FFMPEG_libpostproc ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libpostproc.a)
set(FFMPEG_librtmp ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/librtmp.a)
set(FFMPEG_libssl ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libssl.a)
set(FFMPEG_libswresample ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libswresample.a)
set(FFMPEG_libswscale ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libswscale.a)
set(FFMPEG_libx264 ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libx264.a)
set(FFMPEG_libyuv ${CMAKE_SOURCE_DIR}/thirdparty/prebuilt/${ANDROID_ABI}/libyuv.a)



message("FFMPEG_libavcodec:"+${FFMPEG_libavcodec})


# 设置cpp文件的位置 ，方便后面添加

file(GLOB FILE_C "${PROJECT_SOURCE_DIR}/src/*.cpp")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        window

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp
        # 要编译的c/c++文件
        ${FILE_C}
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        window

        # 不加入下面两个，直接编译报错，第三方库找不到，原因未知
        z
        android

        # 链接ffmpeg相关的库
        ${FFMPEG_libavfilter}
        ${FFMPEG_libavformat}
        ${FFMPEG_libavcodec}
        ${FFMPEG_libpostproc}
        ${FFMPEG_libswresample}
        ${FFMPEG_libswscale}
        ${FFMPEG_libavutil}
        ${FFMPEG_libfdk_aac}
        ${FFMPEG_libx264}
        ${FFMPEG_librtmp}
        ${FFMPEG_libyuv}
        ${FFMPEG_libssl}
        ${FFMPEG_libcrypto}

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})