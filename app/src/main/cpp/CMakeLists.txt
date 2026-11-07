cmake_minimum_required(VERSION 3.22.1)
project(sidekit_emulator C CXX)
include(FetchContent)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(LIBMGBA_ONLY ON)
FetchContent_Declare(mgba
    URL "${CMAKE_CURRENT_SOURCE_DIR}/vendor/mgba-0.10.5.tar.gz"
    URL_HASH SHA256=9475c26e9fa2f4b30c07ab6636e4b0a5b62e4baee2109ede7b2fecc52edae366)
FetchContent_MakeAvailable(mgba)
add_library(sidekit_emulator SHARED emulator.c)
target_link_libraries(sidekit_emulator mgba jnigraphics)
target_link_options(sidekit_emulator PRIVATE "-Wl,-z,max-page-size=16384")
