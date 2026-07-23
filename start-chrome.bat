@echo off
echo =======================================================
echo MCA Local Chrome Auto-Login Utility
echo =======================================================
echo.
echo When you run the OTP API in AWS, it returns a long "data" 
echo string containing your authenticated session cookies.
echo.
set /p COOKIE="1. Paste the cookie string here: "
echo.
set /p DEVICE_ID="2. Enter your deviceId (optional, press Enter to skip): "
echo.

echo Compiling and running local Chrome injector...
REM This runs the existing Spring Boot application in a special CLI mode
REM which injects the cookies via CDP and immediately exits.
call mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dlocal.mca.cookie=\"%COOKIE%\" -Dlocal.mca.deviceId=\"%DEVICE_ID%\""

echo.
pause
