@echo off
rem CSC301 Monopoly - checks which Java a NEW command prompt picks up from PATH
set OUT=%~dp0java-path-check.txt
echo CSC301 Java PATH check %DATE% %TIME% > "%OUT%"
echo --- where java >> "%OUT%"
where java >> "%OUT%" 2>&1
echo --- where javac >> "%OUT%"
where javac >> "%OUT%" 2>&1
echo --- java -version >> "%OUT%"
java -version >> "%OUT%" 2>&1
echo --- javac -version >> "%OUT%"
javac -version >> "%OUT%" 2>&1
echo --- JAVA_HOME >> "%OUT%"
echo %JAVA_HOME% >> "%OUT%"
type "%OUT%"
pause
