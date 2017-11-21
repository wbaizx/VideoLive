# 版本号
cmake_minimum_required(VERSION 3.4.1)

#liveudpnative                      表示库的名称
#SHARED                         设置库的一种模式  SHARED / STATIC
#src/main/cpp/liveudpnative.cpp     C/C++文件的具体路径

add_library(
            liveudpnative
            SHARED
            src/main/jni/liveudpnative.cpp
)

# CMake包含了系统库的文件，指定你需要的系统文件

find_library(
            log-lib
            log
)

# 把Cmake指定的系统库文件，链接到C/C++文件中

target_link_libraries(
            liveudpnative
            ${log-lib}
)
