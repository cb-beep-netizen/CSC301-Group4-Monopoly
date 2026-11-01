<#
  CSC301 Monopoly - SOFTWARE ENVIRONMENT SETUP
  File   : C:\Claude\Monopoly\setup\Setup-Environment.ps1
  Run via: Setup-Environment.bat (double-click, approve the UAC prompt)
  Does   : 1) installs Eclipse Temurin JDK 25 (LTS) with winget
           2) sets JAVA_HOME and PATH for the current user
           3) installs the VS Code Extension Pack for Java
           4) checks the AI-off workspace settings (.vscode\settings.json)
           5) runs java/javac checks and a compile smoke test
           6) writes setup\setup-verification.txt
  Safe to re-run: every step checks before it changes anything.
#>
$ErrorActionPreference = 'Continue'
$SetupDir = $PSScriptRoot
$Root     = Split-Path -Parent $SetupDir
$Report   = Join-Path $SetupDir 'setup-verification.txt'
$JdkId    = 'EclipseAdoptium.Temurin.25.JDK'
$JdkBase  = 'C:\Program Files\Eclipse Adoptium'
$ExtId    = 'vscjava.vscode-java-pack'
$CodeCli  = Join-Path $env:LOCALAPPDATA 'Programs\Microsoft VS Code\bin\code.cmd'

function Log([string]$m) {
    $line = '[{0}] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $m
    Write-Host $line
    Add-Content -Path $Report -Value $line
}
function Capture($block) {
    & $block 2>&1 | ForEach-Object { $t = "$_"; Write-Host "    $t"; Add-Content -Path $Report -Value "    $t" }
}
function Find-Jdk {
    Get-ChildItem -Path $JdkBase -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
}

Set-Content -Path $Report -Value 'CSC301 Monopoly - Software Environment Setup - verification report'
Log ("Computer={0}  User={1}  PowerShell={2}" -f $env:COMPUTERNAME, $env:USERNAME, $PSVersionTable.PSVersion)
Log "Project folder: $Root"

# ---- Step 1: winget --------------------------------------------------------
Log 'STEP 1  Check winget'
if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Log 'ERROR: winget not found. Install "App Installer" from the Microsoft Store, then re-run.'
    Read-Host 'Press Enter to close'; exit 1
}
Capture { winget --version }

# ---- Step 2: JDK -----------------------------------------------------------
Log 'STEP 2  Eclipse Temurin JDK 25 (LTS)'
$jdk = Find-Jdk
if ($jdk) {
    Log "JDK already installed: $($jdk.FullName) - skipping install"
} else {
    Log "Installing $JdkId (approve the Windows UAC prompt if it appears)..."
    Capture { winget install --id $JdkId -e --source winget --accept-source-agreements --accept-package-agreements }
    $jdk = Find-Jdk
}
if (-not $jdk) {
    Log "ERROR: no jdk-25* folder found under $JdkBase after install. See report and re-run."
} else {
    $javaHome = $jdk.FullName
    $javaBin  = Join-Path $javaHome 'bin'
    Log "STEP 3  JAVA_HOME = $javaHome (user scope)"
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'User')
    $env:JAVA_HOME = $javaHome
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if ([string]::IsNullOrEmpty($userPath)) { $userPath = '' }
    if ($userPath -notlike "*$javaBin*") {
        [Environment]::SetEnvironmentVariable('Path', ($javaBin + ';' + $userPath).TrimEnd(';'), 'User')
        Log "Added $javaBin to user PATH"
    } else { Log 'JDK bin already on user PATH' }
    $env:Path = $javaBin + ';' + $env:Path
}

# ---- Step 4: VS Code Java extensions ---------------------------------------
Log "STEP 4  VS Code extension $ExtId"
if (Test-Path $CodeCli) {
    Capture { & $CodeCli --install-extension $ExtId }
} else {
    Log "WARNING: VS Code CLI not found at $CodeCli - install the extension from the VS Code Extensions view."
}

# ---- Step 5: AI-off workspace settings -------------------------------------
Log 'STEP 5  Workspace settings (.vscode\settings.json)'
$ws = Join-Path $Root '.vscode\settings.json'
if (Test-Path $ws) {
    if ((Get-Content $ws -Raw) -match '"chat.disableAIFeatures"\s*:\s*true') { Log 'OK: VS Code AI features disabled for this workspace' }
    else { Log 'WARNING: settings.json exists but chat.disableAIFeatures is not true' }
} else { Log "WARNING: $ws missing" }

# ---- Step 6: verification --------------------------------------------------
Log 'STEP 6  Verification'
Log 'java -version'   ; Capture { java -version }
Log 'javac -version'  ; Capture { javac -version }
Log 'where java'      ; Capture { where.exe java }
Log 'JAVA_HOME (user)'; Capture { [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User') }
if (Test-Path $CodeCli) { Log 'VS Code Java extensions'; Capture { & $CodeCli --list-extensions | Select-String -Pattern 'java' } }
Log 'git --version'   ; Capture { git --version }

Log 'Compile smoke test (javac --release 21)'
$tmp = Join-Path $env:TEMP 'csc301_envcheck'
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
Set-Content -Path (Join-Path $tmp 'EnvCheck.java') -Value @'
public class EnvCheck {
    public static void main(String[] args) {
        System.out.println("ENVCHECK|OK|" + System.getProperty("java.version"));
    }
}
'@
Capture { javac --release 21 -d $tmp (Join-Path $tmp 'EnvCheck.java') }
Capture { java -cp $tmp EnvCheck }

Log 'DONE. Close and reopen VS Code and any command prompts so PATH changes take effect.'
Read-Host 'Press Enter to close'
