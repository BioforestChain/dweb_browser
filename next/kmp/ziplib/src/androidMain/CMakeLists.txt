cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_STANDARD 99)

project("miniz")

file(GLOB_RECURSE sources "../../native/*.c")

add_library(miniz SHARED ${sources})

target_link_libraries(miniz)
