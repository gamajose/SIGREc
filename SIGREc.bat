@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-21.0.11
set PATH=%JAVA_HOME%\bin;%PATH%
set DB_HOST=192.168.42.128
set DB_PORT=5432
set DB_NAME=segrec
set DB_USER=jose
set DB_PASSWORD=Joseluiz1
set SIGREC_OPEN_BROWSER=true

cd /d "%~dp0"

if exist "target\tfd-apac-0.1.0.jar" (
  "%JAVA_HOME%\bin\java.exe" -jar "target\tfd-apac-0.1.0.jar"
) else (
  "C:\Program Files\NetBeans-21\netbeans\java\maven\bin\mvn.cmd" spring-boot:run
)

endlocal
