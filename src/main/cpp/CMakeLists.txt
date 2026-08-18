cmake_minimum_required(VERSION 3.22.1)

project(native_audio)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

add_library(
        native-audio
        SHARED
        aaudio_capture.cpp
        vad_jni.cpp
)

# اضافه کردن libfvad به صورت ماژول
add_subdirectory(third_party/libfvad)

find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)

target_link_libraries(
        native-audio
        fvad
        ${log-lib}
        ${android-lib}
        ${aaudio-lib}
)
