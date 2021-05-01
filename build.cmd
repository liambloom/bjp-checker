@echo off
goto :%1

rem I should be using intellij build tools.
rem But I couldn't set them up the way I wanted, and I couldn't get them back to how the were after I gave up.
rem So here's a batch file that builds stuff

:bins
cargo +nightly build --bin checker --release --features cli -Z unstable-options
cargo +nightly build --bin checker-gui --release --features gui -Z unstable-options
rem build for other OSes?
exit /b

:lib
javac -sourcepath src\java -d out\java\production\bjp3-tests -source 8 -target 8 -cp build\lib\* -bootclasspath "C:\Program Files\Java\jdk1.8.0_281\jre\lib\rt.jar" src\java\dev\liambloom\tests\book\bjp\*.java src\java\dev\liambloom\tests\book\bjp\checker\*.java
xcopy /q /y /s rsc\* out\java\production\bjp3-tests\ 1>nul
jar cf build\lib\checker.jar -C out\java\production\bjp3-tests .
exit /b

:docs
rem javadoc
exit /b

:all
call :lib
call :docs
call :bins
exit /b