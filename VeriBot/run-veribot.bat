@echo off
echo Running VeriBot News Verification Agent...
echo.

:: Check if target directory exists
if not exist "target" (
  echo Building VeriBot with Maven...
  call mvn clean package -DskipTests
)

:: Check if .env file exists
if not exist ".env" (
  echo .env file not found.
  echo Please run create-env.bat first and configure your API keys.
  echo.
  pause
  exit /b 1
)

:: Run the application
echo Starting VeriBot...
echo.
java -jar target/veribot-1.0-SNAPSHOT-jar-with-dependencies.jar

pause