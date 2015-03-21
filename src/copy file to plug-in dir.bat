@echo off
if exist "C:\Program Files\Eclipse\" goto x64
if exist "C:\Program Files (x86)\Eclipse\" goto x86
echo "can't find eclispe in C:\Program Files or C:\Program Files (x86)."
pause
exit


:x64
echo "find eclispe in C:\Program Files."
if not exist .\ccfx exit
xcopy .\ccfx "C:\Program Files\Eclipse\plugins\ccfx\" /s /y
pause
exit

:x86
echo "find eclispe in C:\Program Files (x86)."
if not exist ".\ccfx" exit
xcopy .\ccfx "C:\Program Files (x86)\Eclipse\plugins\ccfx\" /s /y
pause
exit
