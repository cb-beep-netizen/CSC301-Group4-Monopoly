@echo off
rem CSC301 Monopoly - compile the game (Temurin JDK 25, target Java 21)
cd /d "%~dp0"
set "JAVAC=javac"
if defined JAVA_HOME set "JAVAC=%JAVA_HOME%\bin\javac"
if not exist bin mkdir bin
"%JAVAC%" --release 21 -encoding UTF-8 -d bin code\*.java
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)
echo Build OK - classes in bin\
exit /b 0
