[`dev_liambloom_checker_BookReader.cpp`](dev_liambloom_checker_BookReader.cpp) compilation notes.

[`libnative.so`](../resources/libnative.so) was compiled on `Ubuntu 20.04.3 LTS` on `x86_64` architecture using `g++ (Ubuntu 9.3.0-17ubuntu1~20.04) 9.3.0` and `OpenJDK 17 (build 17+35-Ubuntu-120.04)` headers with the following commands:
```bash
# Compilation
g++ -std=c++17 -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux dev_liambloom_checker_BookReader.cpp -o dev_liambloom_checker_BookReader.o

# Linkage
g++ -shared -fPIC -o ../resources/libnative.so dev_liambloom_checker_BookReader.o -lc
```

[`native.dll`](../resources/native.dll) was compiled on `Windows 11 21H2 (OS Build 22000.258)` on `x86_64` architecture using `g++ (x86_64-posix-seh, Built by strawberryperl.com project) 8.3.0` and `OpenJDK 17 (build 17+35-2724)` headers with the following commands:
```cmd
:: Compilation
g++ -c -I "%JAVA_HOME%\include" -I "%JAVA_HOME%\include\win32" dev_liambloom_checker_BookReader.cpp -o dev_liambloom_ch
ecker_BookReader.o -std=c++17

:: Linkage
g++ -shared -o ../resources/native.dll dev_liambloom_checker_BookReader.o -Wl,--add-stdcall-alias -std=c++17 -lstdc++fs
```
