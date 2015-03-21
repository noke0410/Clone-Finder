@echo off
if exist %SystemRoot%\SysWOW64\ goto x64
if exist %SystemRoot%\System32\ goto x86
echo can't find SysWOW64 or System32 in System Root.
pause
exit


:x64
if not exist .\ccfx\bin\_CCFinderXLib.dll exit
echo Copy to %SystemRoot%\SysWOW64\
xcopy .\ccfx\bin\_CCFinderXLib.dll %SystemRoot%\SysWOW64\ /s /y
pause
exit

:x86
if not exist .\ccfx\bin\_CCFinderXLib.dll exit
echo Copy to %SystemRoot%\System32\
xcopy .\ccfx\bin\_CCFinderXLib.dll %SystemRoot%\System32\ /s /y
pause
exit
