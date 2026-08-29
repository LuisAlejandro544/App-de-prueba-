cmake_minimum_required(VERSION 3.22.1)

project("nativeaudio")

add_library(
    native_audio_engine
    SHARED
    native_audio_engine.cpp
    ffmpeg_bridge.cpp
)

find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

target_link_libraries(
    native_audio_engine
    ${log-lib}
    ${android-lib}
)
