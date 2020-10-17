$ErrorActionPreference = 'Stop'; # stop on all errors
$packageName = 'bazel'

$toolsDir = Split-Path -parent $MyInvocation.MyCommand.Definition
$raw = get-content "$($toolsDir)\params.txt" | out-string
write-host "Content of $($toolsDir)\params.txt:"
write-host $raw
$params = $raw -split "`n"
write-host "url:  $($params[0].Trim())"
write-host "hash: $($params[1].Trim())"

$packageDir = Split-Path -parent $toolsDir

Install-ChocolateyZipPackage -PackageName "$packageName" `
  -Url64bit "$($params[0].Trim())" `
  -Checksum64 "$($params[1].Trim())" `
  -ChecksumType64 "sha256" `
  -UnzipLocation "$packageDir"

write-host @"
bazel installed to $packageDir

See also https://bazel.build/docs/windows.html
"@

