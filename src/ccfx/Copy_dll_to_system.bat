@echo off
if exist %SystemRoot%\SysWOW64\ goto x64
if exist %SystemRoot%\System32\ goto x86
echo can't find SysWOW64 or System32 in System Root.
pause
exit


:x64
if not exist .\bin\_CCFinderXLib.dll exit
echo Copy to %SystemRoot%\SysWOW64\
copy .\bin\*.dll %SystemRoot%\SysWOW64\ /y /v
pause
exit

:x86
if not exist .\bin\_CCFinderXLib.dll exit
echo Copy to %SystemRoot%\System32\
copy .\bin\*.dll %SystemRoot%\System32\ /y /v
pause
exit
