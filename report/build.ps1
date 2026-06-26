$ErrorActionPreference = "Stop"

$reportDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $reportDir

$miktexBin = "C:\Users\ACER\AppData\Local\Programs\MiKTeX\miktex\bin\x64"
$bundledXelatex = Join-Path $miktexBin "xelatex.exe"
if (Test-Path $bundledXelatex) {
    $xelatex = $bundledXelatex
} else {
    $cmd = Get-Command xelatex.exe -ErrorAction SilentlyContinue
    if (-not $cmd) {
        $cmd = Get-Command xelatex -ErrorAction SilentlyContinue
    }
    if (-not $cmd) {
        throw "xelatex was not found. Install MiKTeX or add xelatex to PATH."
    }
    $xelatex = $cmd.Source
}

for ($i = 1; $i -le 3; $i++) {
    & $xelatex -interaction=nonstopmode -synctex=1 main.tex
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

if (-not (Test-Path "main.pdf")) {
    throw "Build finished without creating main.pdf."
}

Write-Host "Built report/main.pdf"
