cmake_minimum_required(VERSION 3.0)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

include_directories(${CLASH_OUTPUT}/${ANDROID_ABI} ${CLASH_SOURCE})
link_directories(${CLASH_OUTPUT}/${CMAKE_ANDROID_ARCH_ABI})
link_libraries(log clash)

add_library(bridge SHARED bridge.c)