@echo off
rem CSC301 Monopoly - build if needed, then play on the console board.
rem Examples:  run.bat --players Sara,Omar,Ali
rem            run.bat --players Sara,Omar,Ali --seed 42
rem            run.bat --players Sara,Omar,Ali --seed 42 --auto --pause
rem            run.bat --players Sara,Omar,Ali --text          (plain log mode)
rem Colours: each player has a colour and the squares they own take it.
rem Works in the classic console (Windows 10/11), Windows Terminal and VS Code.
rem If escape codes appear instead of colours, add --no-color.
rem Widen the window to 100+ columns and about 40 rows.
cd /d "%~dp0"
chcp 65001 >nul
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java"
call "%~dp0build.bat" >nul
if errorlevel 1 (
  call "%~dp0build.bat"
  exit /b 1
)
"%JAVA%" -cp bin Main code\data\board.txt code\data\cards.txt %*
