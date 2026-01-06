# Salesforce connectivity diagnostics for login.salesforce.com
# Measures DNS, TCP connect, TLS handshake, and HTTPS GET timings.

$ErrorActionPreference = 'Continue'
$targetHost = 'login.salesforce.com'
$targetPort = 443

function Write-Section($title) {
  Write-Host ""
  Write-Host "==== $title ===="
}

function Measure-Block($label, [scriptblock]$block) {
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  $result = $null
  $err = $null
  try {
    $result = & $block
  } catch {
    $err = $_
  }
  $sw.Stop()
  [pscustomobject]@{
    Label = $label
    ElapsedMs = $sw.ElapsedMilliseconds
    Result = $result
    Error = $err
  }
}

Write-Section "System"
Write-Host "Time: $(Get-Date -Format 'yyyy/MM/dd HH:mm:ss.fff')"
Write-Host "Host: $env:COMPUTERNAME"
Write-Host "User: $env:USERNAME"
Write-Host "PSVersion: $($PSVersionTable.PSVersion)"

Write-Section "Proxy (WinHTTP + .NET + Env)"
try {
  $winHttpProxy = netsh winhttp show proxy 2>$null
  Write-Host "WinHTTP proxy:"
  $winHttpProxy | ForEach-Object { Write-Host "  $_" }
} catch {
  Write-Host "WinHTTP proxy: (failed to query)"
}
try {
  $webProxy = [System.Net.WebRequest]::GetSystemWebProxy()
  $proxyUri = $webProxy.GetProxy("https://$targetHost/")
  Write-Host "SystemWebProxy: $proxyUri"
} catch {
  Write-Host "SystemWebProxy: (failed to query)"
}
Write-Host "Env HTTP_PROXY: $env:HTTP_PROXY"
Write-Host "Env HTTPS_PROXY: $env:HTTPS_PROXY"

Write-Section "DNS"
$dnsResults = Measure-Block "Resolve-DnsName" { Resolve-DnsName -Name $targetHost -Type A -ErrorAction Stop }
Write-Host "Resolve-DnsName: ${($dnsResults.ElapsedMs)} ms"
if ($dnsResults.Error) { Write-Host "Error: $($dnsResults.Error.Exception.Message)" }
else { $dnsResults.Result | Select-Object -First 5 | ForEach-Object { Write-Host "  $($_.Name) -> $($_.IPAddress)" } }

Write-Section "TCP Connect"
$tcpResult = Measure-Block "TcpClient.Connect" {
  $client = New-Object System.Net.Sockets.TcpClient
  $client.NoDelay = $true
  $client.Connect($targetHost, $targetPort)
  $client
}
Write-Host "TcpClient.Connect: ${($tcpResult.ElapsedMs)} ms"
if ($tcpResult.Error) { Write-Host "Error: $($tcpResult.Error.Exception.Message)" }

Write-Section "TLS Handshake"
$tlsResult = Measure-Block "SslStream.AuthenticateAsClient" {
  $client = New-Object System.Net.Sockets.TcpClient
  $client.Connect($targetHost, $targetPort)
  $ssl = New-Object System.Net.Security.SslStream($client.GetStream(), $false, ({ $true }))
  $ssl.AuthenticateAsClient($targetHost)
  $ssl
}
Write-Host "SslStream.AuthenticateAsClient: ${($tlsResult.ElapsedMs)} ms"
if ($tlsResult.Error) {
  Write-Host "Error: $($tlsResult.Error.Exception.Message)"
} else {
  Write-Host "Protocol: $($tlsResult.Result.SslProtocol)"
  Write-Host "Cipher: $($tlsResult.Result.CipherAlgorithm) $($tlsResult.Result.CipherStrength)"
  Write-Host "Hash: $($tlsResult.Result.HashAlgorithm) $($tlsResult.Result.HashStrength)"
}

Write-Section "HTTPS GET (no auth)"
$httpResult = Measure-Block "Invoke-WebRequest" {
  Invoke-WebRequest -Uri "https://$targetHost/" -Method Get -UseBasicParsing -TimeoutSec 30
}
Write-Host "Invoke-WebRequest: ${($httpResult.ElapsedMs)} ms"
if ($httpResult.Error) {
  Write-Host "Error: $($httpResult.Error.Exception.Message)"
} else {
  Write-Host "StatusCode: $($httpResult.Result.StatusCode)"
}

Write-Section "Done"
Write-Host "Completed at: $(Get-Date -Format 'yyyy/MM/dd HH:mm:ss.fff')"
