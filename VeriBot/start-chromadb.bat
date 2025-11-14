@echo off
echo Starting ChromaDB for VeriBot RAG...
echo.

docker-compose up -d

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ChromaDB started successfully!
    echo ChromaDB is running at: http://localhost:8000
    echo.
    echo To check status: docker ps
    echo To view logs: docker logs veribot-chromadb
    echo To stop: docker-compose down
) else (
    echo.
    echo Failed to start ChromaDB. Please ensure Docker is running.
    echo.
    pause
)
