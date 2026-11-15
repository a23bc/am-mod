# curl 在本机对 api.github.com 偶发 schannel TLS 握手失败（exit 35）时的备用下载路径。
# .NET 的 Invoke-WebRequest 走另一套 TLS 栈，实测可用；跨主机重定向会自动去掉
# Authorization 头，所以能直接跟随 302 到 Azure Blob 预签名地址。
#
# 用法：
#   powershell -NoProfile -File tools/fetch-artifact.ps1 -RunId 43713549746
param(
    [Parameter(Mandatory = $true)][string]$RunId,
    [string]$Out = "E:\Projects\am-mod\tools\artifacts"
)

$ErrorActionPreference = 'Stop'
$token = (& gh auth token).Trim()
$headers = @{
    Authorization  = "Bearer $token"
    Accept         = "application/vnd.github+json"
    "User-Agent"   = "workbuddy"
}

$list = Invoke-RestMethod `
    -Uri "https://api.github.com/repos/a23bc/am-mod/actions/runs/$RunId/artifacts" `
    -Headers $headers -TimeoutSec 30
$id = ($list.artifacts | Where-Object { $_.name -eq 'am-mod-debug' }).id
if (-not $id) { throw "artifact am-mod-debug not found in run $RunId" }

New-Item -ItemType Directory -Force -Path $Out | Out-Null
$zip = Join-Path $Out 'am-mod-debug.zip'
Invoke-WebRequest `
    -Uri "https://api.github.com/repos/a23bc/am-mod/actions/artifacts/$id/zip" `
    -Headers $headers -OutFile $zip -TimeoutSec 120 `
    -UseBasicParsing -MaximumRedirection 5
Expand-Archive -Path $zip -DestinationPath $Out -Force
Write-Output "wrote $(Join-Path $Out 'app-debug.apk')"
