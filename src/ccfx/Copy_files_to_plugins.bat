@echo off
if exist "%ProgramFiles%\Eclipse" goto x64
if exist "%ProgramFiles(x86)%\Eclipse" goto x86
echo Can't find eclispe in %ProgramFiles% or %ProgramFiles(x86)%
pause
exit


:x64
echo Find Eclispe in %ProgramFiles
xcopy . "%ProgramFiles%\Eclipse\plugins\ccfx\" /s /y
del "%ProgramFiles%\Eclipse\plugins\ccfx\*.bat" /q
pause
exit

:x86
echo Find Eclispe in %ProgramFiles(x86)%
xcopy . "%ProgramFiles(x86)%\Eclipse\plugins\ccfx\" /s /y
del "%ProgramFiles(x86)%\Eclipse\plugins\ccfx\*.bat" /q
pause
exit
