@echo off
:: Note: %~dp0 doesn't work with symlinks, but that's not a standard or important thing on windows.
:: https://superuser.com/q/927411/1158654

if "%CHECKER_DEBUG%" == "1" echo Debug mode is ON

java -cp "%~dp0..\lib\*" io.github.liambloom.tests.book.bjp3.App %*