$ErrorActionPreference = 'Stop'
$jdkBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { $null }
$javac = if ($jdkBin -and (Test-Path (Join-Path $jdkBin 'javac.exe'))) { Join-Path $jdkBin 'javac.exe' } else { (Get-Command javac -ErrorAction Stop).Source }
$java = if ($jdkBin -and (Test-Path (Join-Path $jdkBin 'java.exe'))) { Join-Path $jdkBin 'java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$jarCommand = Get-Command jar -ErrorAction SilentlyContinue
$jar = if ($jdkBin -and (Test-Path (Join-Path $jdkBin 'jar.exe'))) { Join-Path $jdkBin 'jar.exe' } elseif ($jarCommand) { $jarCommand.Source } else { (Get-ChildItem -Path (Join-Path $env:ProgramFiles 'Java') -Filter jar.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName) }
if (-not (Test-Path $jar)) { throw 'jar.exe not found. Set JAVA_HOME to a JDK installation.' }
New-Item -ItemType Directory -Force -Path out/classes,out/test-classes | Out-Null
$sources = @(Get-ChildItem -Recurse -Filter '*.java' src/main/java | ForEach-Object FullName)
$tests = @(Get-ChildItem -Recurse -Filter '*.java' src/test/java | ForEach-Object FullName)
& $javac --release 8 -Xlint:-options -d out/classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $javac --release 8 -Xlint:-options -d out/test-classes ($sources + $tests)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $java -cp 'out/classes;out/test-classes' io.github.vaibhavkatyal.javask.GroqClientTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $jar cf out/javask-0.1.0.jar -C out/classes .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Built out/javask-0.1.0.jar'
