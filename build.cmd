@echo off
goto :%1

rem This should do the same thing as my local intellij settings

:bins
cd rust
cargo +nightly build -q --bin checker --release --features cli -Z unstable-options
cargo +nightly build -q --bin checker-gui --release --features gui -Z unstable-options
cd ..
rem build for other OSes?
exit /b

:lib

javac -sourcepath java\src -d out\production\java -source 8 -target 8 -cp build\lib\* -bootclasspath "C:\Program Files\Java\jdk1.8.0_281\jre\lib\rt.jar" java\src\dev\liambloom\tests\book\bjp\*.java java\src\dev\liambloom\tests\book\bjp\checker\*.java
xcopy /q /y /s java\rsc\* out\production\java >nul
jar cf build\lib\checker.jar -C out\production\java .
exit /b

:docs
rem javadoc
exit /b

:all
call :lib
call :docs
call :bins
exit /b
