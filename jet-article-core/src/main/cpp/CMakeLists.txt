cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

# Add Gumbo as a subdirectory. Its CMakeLists.txt will define gumbo_static.
add_subdirectory(gumbo-parser)

add_library( # Sets the name of the library.
             nativelib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp )

# Searches for a JNI library and provides the path to the CMake build system.
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies directories to include in the build.
target_include_directories(nativelib PUBLIC
                           ${CMAKE_CURRENT_SOURCE_DIR}/gumbo-parser)

# Links your native library with the JNI library and Gumbo static library.
target_link_libraries( # Specifies the target library.
                       nativelib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       gumbo_static ) # Link against the gumbo_static library defined in gumbo-parser/CMakeLists.txt
