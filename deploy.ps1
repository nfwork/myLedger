[CmdletBinding()]
param(
    [string]$Mode = "all",
    [string]$RootDeployPath = "D:\dbfound\webapps\ROOT",
    [string]$MavenProfile = "prod",
    [switch]$OnlyH5,
    [switch]$OnlyApi,
    [switch]$SkipNpmInstall,
    [switch]$InstallDeps,
    [switch]$SkipMavenClean
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ApiRoot = Join-Path $ProjectRoot "myledger-api"
$H5Root = Join-Path $ProjectRoot "myledger-h5"
$ApiTargetDir = Join-Path $ApiRoot "target"

switch ($Mode.ToLower()) {
    "all" {}
    "api" { $OnlyApi = $true }
    "backend" { $OnlyApi = $true }
    "h5" { $OnlyH5 = $true }
    default {
        throw "Unsupported deploy mode '$Mode'. Use one of: all, api, h5."
    }
}

function Write-Step {
    param($Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-Command {
    param($Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command '$Name' was not found. Please install it or add it to PATH."
    }
}

function Invoke-NativeCommand {
    param(
        $Command,
        $Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        $ArgumentText = [string]::Join(" ", $Arguments)
        $CommandLine = "$Command $ArgumentText"
        throw "Command '$CommandLine' failed with exit code $LASTEXITCODE."
    }
}

function Clear-Directory {
    param($Path)
    if (Test-Path -LiteralPath $Path) {
        Get-ChildItem -LiteralPath $Path -Force | Remove-Item -Recurse -Force
        return
    }

    New-Item -ItemType Directory -Path $Path -Force | Out-Null
}

function Clear-ApiArtifacts {
    param($Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        return
    }

    @("WEB-INF", "META-INF") | ForEach-Object {
        $ArtifactPath = Join-Path $Path $_
        if (Test-Path -LiteralPath $ArtifactPath) {
            Remove-Item -LiteralPath $ArtifactPath -Recurse -Force
        }
    }
}

function Clear-H5Artifacts {
    param($Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        return
    }

    Get-ChildItem -LiteralPath $Path -Force |
        Where-Object { @("WEB-INF", "META-INF") -notcontains $_.Name } |
        Remove-Item -Recurse -Force
}

function Copy-DirectoryContents {
    param(
        $Source,
        $Destination
    )

    if (-not (Test-Path -LiteralPath $Destination)) {
        New-Item -ItemType Directory -Path $Destination -Force | Out-Null
    }

    Get-ChildItem -LiteralPath $Source -Force | Copy-Item -Destination $Destination -Recurse -Force
}

Write-Step "Checking required commands"
if (-not $OnlyH5) {
    Assert-Command "mvn"
}
if (-not $OnlyApi) {
    Assert-Command "npm"
}

$IsFullDeploy = (-not $OnlyApi) -and (-not $OnlyH5)
if ($IsFullDeploy) {
    Write-Step "Preparing ROOT deploy directory"
    Clear-Directory $RootDeployPath
}

if (-not $OnlyH5) {
    Write-Step "Building API WAR"
    Push-Location $ApiRoot
    try {
        if (-not $SkipMavenClean) {
            Write-Step "Running mvn clean"
            Invoke-NativeCommand "mvn" @("clean")
        }

        $MavenArgs = @("package")
        if ($MavenProfile) {
            $MavenArgs += "-P$MavenProfile"
        }
        Write-Step "Running mvn $([string]::Join(' ', $MavenArgs))"
        Invoke-NativeCommand "mvn" $MavenArgs
    }
    finally {
        Pop-Location
    }

    $WarFile = Get-ChildItem -LiteralPath $ApiTargetDir -Filter "*.war" |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $WarFile) {
        throw "No WAR file was found in '$ApiTargetDir'."
    }

    Write-Step "Deploying API to $RootDeployPath"
    if (-not $IsFullDeploy) {
        Clear-ApiArtifacts $RootDeployPath
    }
    $TempPath = [System.IO.Path]::GetTempPath()
    $TempFileName = "myledger-api-" + [System.Guid]::NewGuid().ToString() + ".zip"
    $TempWarZip = Join-Path $TempPath $TempFileName
    try {
        Copy-Item -LiteralPath $WarFile.FullName -Destination $TempWarZip -Force
        Expand-Archive -LiteralPath $TempWarZip -DestinationPath $RootDeployPath -Force
    }
    finally {
        if (Test-Path -LiteralPath $TempWarZip) {
            Remove-Item -LiteralPath $TempWarZip -Force
        }
    }
}

if (-not $OnlyApi) {
    Write-Step "Building H5"
    Push-Location $H5Root
    try {
        $NodeModulesPath = Join-Path $H5Root "node_modules"
        $NeedInstallDeps = (-not $SkipNpmInstall) -and (-not (Test-Path -LiteralPath $NodeModulesPath))
        if ($InstallDeps) {
            Write-Step "Installing H5 dependencies"
            Invoke-NativeCommand "npm" @("install")
        }
        elseif ($NeedInstallDeps) {
            Write-Step "Installing H5 dependencies because node_modules is missing"
            Invoke-NativeCommand "npm" @("install")
        }

        Invoke-NativeCommand "npm" @("run", "build")
    }
    finally {
        Pop-Location
    }

    $H5Dist = Join-Path $H5Root "dist"
    if (-not (Test-Path -LiteralPath $H5Dist)) {
        throw "H5 build output was not found in '$H5Dist'."
    }

    Write-Step "Deploying H5 to $RootDeployPath"
    if (-not $IsFullDeploy) {
        Clear-H5Artifacts $RootDeployPath
    }
    Copy-DirectoryContents $H5Dist $RootDeployPath
}

Write-Step "Deploy completed"
Write-Host "ROOT: $RootDeployPath"
