cmake_minimum_required(VERSION 3.22.1)
project(mosh_pty C)

set(CMAKE_C_STANDARD 17)

add_library(mosh_pty SHARED mosh_jni.c)

target_link_libraries(mosh_pty log)
