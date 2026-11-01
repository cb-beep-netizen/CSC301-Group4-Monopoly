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
rem  - Privacy: commits are signed with the GitHub no-reply e-mail (asked once,
rem    stored for this folder only); unpushed commits that carry a real e-mail
rem    are re-signed before the push.
rem  Output is written to sync-log.txt (not uploaded).
rem  AI log: Conversation 01, Prompt 01.16, updated in Prompts 01.17 and 01.18 (Claude).
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
call :privateemail

echo [3/5] Adding new and changed files ...
git add --ignore-removal . >> "%OUT%" 2>&1
git diff --cached --quiet
if not errorlevel 1 (
  echo       Nothing new to commit.
  goto rewrite
)
set "MSG="
set /p "MSG=[4/5] Short description of this update (Enter = default): "
if "%MSG%"=="" set "MSG=CS: sync Monopoly project (game code, tests, design, AI log)"
git commit -q -m "%MSG%" -m "Synced from C:\Claude\Monopoly with sync-to-github.bat. AI use is logged in AI-Usage-log.txt." -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" >> "%OUT%" 2>&1
if errorlevel 1 goto fail

:rewrite
rem Re-sign any commit not yet on GitHub that carries a real (non no-reply) e-mail.
set "AHEAD=0"
for /f %%N in ('git rev-list --count origin/%BRANCH%..HEAD 2^>nul') do set "AHEAD=%%N"
if "%AHEAD%"=="0" goto push
git log origin/%BRANCH%..HEAD --format=%%ae%%n%%ce | findstr /v /i /c:"@users.noreply.github.com" >nul
if errorlevel 1 goto push
echo       Re-signing %AHEAD% unpushed commit(s) with your private no-reply e-mail ...
git -c core.editor=true rebase -q origin/%BRANCH% --exec "git commit --amend --no-edit --reset-author -q" >> "%OUT%" 2>&1
if errorlevel 1 goto fail

:push
echo [5/5] Pushing to GitHub ...
git push -u origin %BRANCH% >> "%OUT%" 2>&1
if not errorlevel 1 goto pushed
findstr /i /c:"403" /c:"Permission to" "%OUT%" >nul 2>&1
if errorlevel 1 goto fail
echo.
echo GitHub refused the push: the GitHub account saved on this PC has no write
echo access to this repository (see sync-log.txt for the account name).
echo   Fix A: the repository owner (cb-beep-netizen) adds your account under
echo          Settings ^> Collaborators; accept the e-mail invitation, then run
echo          this script again. Your commit is saved and will be pushed then.
echo   Fix B: sign in with another GitHub account that already has access.
set "SW="
set /p "SW=Sign out the saved GitHub account and sign in with another one now? [Y/N]: "
if /i not "%SW%"=="Y" goto fail
(echo protocol=https& echo host=github.com& echo.) | git credential reject
echo A GitHub sign-in window should open now ...
git push -u origin %BRANCH% >> "%OUT%" 2>&1
if errorlevel 1 goto fail

:pushed
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

:privateemail
rem Commits in this public repository use the GitHub no-reply address, never a real e-mail.
set "CUR="
for /f "delims=" %%E in ('git config --local user.email 2^>nul') do set "CUR=%%E"
echo %CUR%| findstr /i /c:"@users.noreply.github.com" >nul
if not errorlevel 1 exit /b 0
echo.
echo   To keep your e-mail private, commits are signed with your GitHub no-reply
echo   address. Open https://github.com/settings/emails , tick "Keep my email
echo   addresses private" and copy the address shown, which looks like
echo   12345678+your-username@users.noreply.github.com
:askprivate
set "PE="
set /p "PE=  Paste your GitHub no-reply e-mail: "
echo %PE%| findstr /i /c:"@users.noreply.github.com" >nul
if errorlevel 1 (
  echo   That is not a ...@users.noreply.github.com address - please try again.
  goto askprivate
)
git config --local user.email "%PE%"
echo   Saved for this folder only. >> "%OUT%"
exit /b 0

:fail
echo.
echo SYNC FAILED - see sync-log.txt
echo FAILED >> "%OUT%"
pause
exit /b 1
