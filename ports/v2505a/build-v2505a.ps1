param(
    [ValidateSet("Probe", "Plant", "Both")]
    [string]$Mode = "Both",
    [string]$NdkRoot = $env:ANDROID_NDK_HOME
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$repo = Split-Path -Parent $root
$source = Join-Path $repo "exploit\src\t61269_plant.c"
$outDir = Join-Path $PSScriptRoot "bin"

if (-not $NdkRoot) {
    $candidate = Join-Path $PSScriptRoot "tools\android-ndk-r28c"
    if (Test-Path -LiteralPath $candidate) {
        $NdkRoot = $candidate
    }
}
if (-not $NdkRoot) {
    throw "未找到 NDK。请设置 ANDROID_NDK_HOME，或解压到 ports\v2505a\tools\android-ndk-r28c。"
}

$cc = Join-Path $NdkRoot "toolchains\llvm\prebuilt\windows-x86_64\bin\aarch64-linux-android31-clang.cmd"
if (-not (Test-Path -LiteralPath $cc)) {
    throw "NDK clang 不存在: $cc"
}

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Build-One([string]$Name, [string[]]$ExtraFlags) {
    $target = Join-Path $outDir $Name
    $ccArgs = @("-static", "-O2", "-pthread", "-DV2505A") + $ExtraFlags +
        @("-o", $target, $source)
    & $cc @ccArgs
    if ($LASTEXITCODE -ne 0) {
        throw "编译失败: $Name"
    }
    Get-Item -LiteralPath $target | Select-Object FullName, Length
}

if ($Mode -in @("Probe", "Both")) {
    Build-One "v2505a_probe" @("-DPROBE_ONLY")
}
if ($Mode -in @("Plant", "Both")) {
    Build-One "v2505a_plant" @()
}

Write-Output "BUILD_V2505A_DONE mode=$Mode"
