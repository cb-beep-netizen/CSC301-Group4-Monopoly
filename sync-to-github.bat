@echo off
rem ==========================================================================
rem  CSC301 Monopoly - sync this folder to GitHub
rem  Repo  : https://github.com/cb-beep-netizen/CSC301-Group4-Monopoly
rem  Branch: CS-Monopoly
rem  - First run: links this folder to the branch WITHOUT deleting anything on
rem    GitHub; teammates' files on the branch are copied into this folder.
rem  - Every run: adds new and changed files, commits, pushes.
rem    Files are never deleted from GitHub by this script (use "git rm" for that).
rem  - Local-only files (bin, logs, setup report with PC name, the brief) are
rem    excluded through .gitignore.
rem  Output is written to sync-log.txt (not uploaded).
rem  AI log: Conversation 01, Prompt 01.16 (Claude).
rem ==========================================================================
setlocal
cd /d "%~dp0"
set "REPO=https://github.com/cb-beep-netizen/CSC301-Group4-Monopoly.git"
set "BRANCH=CS-Monopoly"
set "OUT=%~dp0sync-log.txt"
echo CSC301 GitHub sync %DATE% %TIME% > "%OUT%"

where git >nul 2>&1
if errorlevel 1 (
  echo Git is not installed or not on PATH.>> "%OUT%"
  echo Git is not installed or not on PATH.
  goto fail
)

if exist ".git" goto linked
echo [1/5] Linking this folder to %BRANCH% ...
git init -q >> "%OUT%" 2>&1
git remote add origin %REPO% >> "%OUT%" 2>&1
git symbolic-ref HEAD refs/heads/%BRANCH% >> "%OUT%" 2>&1
echo       (a GitHub sign-in window may open - sign in with your own account)
git fetch origin %BRANCH% >> "%OUT%" 2>&1
if errorlevel 1 goto fail
git reset -q origin/%BRANCH% >> "%OUT%" 2>&1
if errorlevel 1 goto fail
git branch -q --set-upstream-to=origin/%BRANCH% >> "%OUT%" 2>&1
rem copy teammates' files from the branch into this folder (nothing is overwritten)
for /f "delims=" %%F in ('git ls-files -d') do git checkout -- "%%F" >> "%OUT%" 2>&1
goto ignore

:linked
for /f "delims=" %%B in ('git rev-parse --abbrev-ref HEAD') do set "CURRENT=%%B"
if /i not "%CURRENT%"=="%BRANCH%" (
  echo This folder is on branch %CURRENT%, not %BRANCH%. Stopping. >> "%OUT%"
  echo This folder is on branch %CURRENT%, not %BRANCH%. Stopping.
  goto fail
)
echo [1/5] Getting the latest %BRANCH% from GitHub ...
git fetch origin %BRANCH% >> "%OUT%" 2>&1
if errorlevel 1 goto fail
git merge --no-edit origin/%BRANCH% >> "%OUT%" 2>&1
if errorlevel 1 (
  echo Merge conflict with changes on GitHub - see sync-log.txt. >> "%OUT%"
  echo Merge conflict with changes on GitHub - see sync-log.txt.
  goto fail
)

:ignore
echo [2/5] Checking .gitignore ...
if not exist ".gitignore" type nul > ".gitignore"
findstr /c:"# --- CSC301 CS local files" ".gitignore" >nul 2>&1
if not errorlevel 1 goto identity
>> ".gitignore" echo.
>> ".gitignore" echo # --- CSC301 CS local files (added by sync-to-github.bat) ---
>> ".gitignore" echo bin/
>> ".gitignore" echo tests-bin/
>> ".gitignore" echo game-log.txt
>> ".gitignore" echo sync-log.txt
>> ".gitignore" echo setup/setup-verification.txt
>> ".gitignore" echo setup/java-path-check.txt
>> ".gitignore" echo *.lnk
>> ".gitignore" echo CSC301_Monopoly_Assignment.docx

:identity
git config user.name >nul 2>&1
if errorlevel 1 call :askname
git config user.email >nul 2>&1
if errorlevel 1 call :askemail

echo [3/5] Adding new and changed files ...
git add --ignore-removal . >> "%OUT%" 2>&1
git diff --cached --quiet
if not errorlevel 1 (
  echo       Nothing new to commit.
  goto push
)
set "MSG="
set /p "MSG=[4/5] Short description of this update (Enter = default): "
if "%MSG%"=="" set "MSG=CS: sync Monopoly project (game code, tests, design, AI log)"
git commit -q -m "%MSG%" -m "Synced from C:\Claude\Monopoly with sync-to-github.bat. AI use is logged in AI-Usage-log.txt." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" >> "%OUT%" 2>&1
if errorlevel 1 goto fail

:push
echo [5/5] Pushing to GitHub ...
git push -u origin %BRANCH% >> "%OUT%" 2>&1
if errorlevel 1 goto fail
echo. >> "%OUT%"
echo PUSHED: >> "%OUT%"
git log -1 --format="%%H %%an %%ad %%s" --date=iso >> "%OUT%" 2>&1
git status --short >> "%OUT%" 2>&1
echo.
echo Done. https://github.com/cb-beep-netizen/CSC301-Group4-Monopoly/tree/%BRANCH%
echo Details: sync-log.txt
pause
exit /b 0

:askname
set "GN="
set /p "GN=Name to show on your commits (e.g. your GitHub name): "
git config user.name "%GN%"
exit /b 0

:askemail
set "GE="
set /p "GE=Email for your commits (the one on your GitHub account): "
git config user.email "%GE%"
exit /b 0

:fail
echo.
echo SYNC FAILED - see sync-log.txt
echo FAILED >> "%OUT%"
pause
exit /b 1
