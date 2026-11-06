@echo off
rem CSC301 Monopoly - build, run the 93 tests (core + twists T1/T3/T7) and the constraint check.
cd /d "%~dp0"
set "JAVA=java"
set "JAVAC=javac"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java"
if defined JAVA_HOME set "JAVAC=%JAVA_HOME%\bin\javac"
call "%~dp0build.bat"
if errorlevel 1 goto end
if not exist tests-bin mkdir tests-bin
"%JAVAC%" --release 21 -encoding UTF-8 -cp bin -d tests-bin tests\GameTests.java
if errorlevel 1 goto end
"%JAVA%" -cp "bin;tests-bin" GameTests tests\data\test-board.txt code\data\board.txt code\data\cards.txt
echo.
python --version >nul 2>&1
if errorlevel 1 (
  echo Python not found on PATH - skipping tests\check_constraints.py
) else (
  python tests\check_constraints.py code
)
:end
pause
