@echo off
set "JAVA_HOME=C:\Users\Melo\.antigravity-ide\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64"
cd /d "D:\Ekmicro-API-automation-main (3)\Ekmicro-API-automation-main"
call mvnw.cmd compile -q
echo Exit code: %ERRORLEVEL%
