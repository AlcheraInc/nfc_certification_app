# https://stackoverflow.com/questions/57698796/cannot-detect-opencv-libs-after-update-form-3-4-3-to-4-1-1/58224079#58224079

cmake_minimum_required(VERSION 3.4.1)


add_library( # Sets the name of the library.
        dummy-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        dummy-lib.cpp)


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        dummy-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
