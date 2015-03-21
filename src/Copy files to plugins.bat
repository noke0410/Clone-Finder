@echo off
if exist %ProgramFiles%\Eclipse\ goto x64
if exist %ProgramFiles(x86)%\Eclipse\ goto x86
echo Can't find eclispe in %ProgramFiles% or %ProgramFiles(x86)%.
pause
exit


:x64
echo Find Eclispe in %ProgramFiles%
if not exist .\ccfx exit
xcopy .\ccfx %ProgramFiles%\Eclipse\plugins\ccfx\ /s /y
pause
exit

:x86
echo Find Eclispe in %ProgramFiles(x86)%
if not exist ".\ccfx" exit
xcopy .\ccfx %ProgramFiles(x86)%\Eclipse\plugins\ccfx\ /s /y
pause
exit
