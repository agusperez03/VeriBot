@echo off
echo ============================================
echo VeriBot RAG Setup Verification
echo ============================================
echo.

echo [1/4] Checking Docker...
docker --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Docker is not installed or not in PATH
    echo    Please install Docker Desktop from https://www.docker.com/
    exit /b 1
) else (
    echo ✓ Docker is installed
)
echo.

echo [2/4] Checking if ChromaDB is running...
docker ps | findstr "veribot-chromadb" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ⚠ ChromaDB is not running
    echo   Starting ChromaDB...
    docker-compose up -d
    timeout /t 3 >nul
    echo ✓ ChromaDB started
) else (
    echo ✓ ChromaDB is already running
)
echo.

echo [3/4] Checking ChromaDB health...
curl -s http://localhost:8000/api/v1/heartbeat >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ⚠ ChromaDB is not responding
    echo   Waiting 5 seconds for ChromaDB to start...
    timeout /t 5 >nul
    curl -s http://localhost:8000/api/v1/heartbeat >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ ChromaDB health check failed
        echo    Check logs with: docker logs veribot-chromadb
        exit /b 1
    ) else (
        echo ✓ ChromaDB is healthy
    )
) else (
    echo ✓ ChromaDB is healthy
)
echo.

echo [4/4] Checking Java and Maven...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java is not installed or not in PATH
    exit /b 1
) else (
    echo ✓ Java is installed
)

mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven is not installed or not in PATH
    exit /b 1
) else (
    echo ✓ Maven is installed
)
echo.

echo ============================================
echo ✓ All checks passed!
echo ============================================
echo.
echo ChromaDB Status:
docker ps --filter "name=veribot-chromadb" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.
echo Next steps:
echo 1. Build: mvn clean package
echo 2. Run: .\run-veribot.bat
echo 3. Test RAG with news queries!
echo.
echo Useful commands:
echo - View logs: docker logs veribot-chromadb
echo - Stop ChromaDB: docker-compose down
echo - Restart: docker-compose restart
echo.
