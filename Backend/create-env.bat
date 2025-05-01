@echo off
echo Creating .env file for VeriBot configuration...
echo # Azure OpenAI Configuration > .env
echo AZURE_OPENAI_ENDPOINT=https://your-azure-openai-instance.openai.azure.com/ >> .env
echo AZURE_OPENAI_API_KEY=your-azure-openai-api-key >> .env
echo AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name >> .env
echo AZURE_OPENAI_API_VERSION=2023-05-15 >> .env
echo. >> .env
echo # Azure Bing Search API Configuration >> .env
echo AZURE_BING_SEARCH_API_KEY=your-bing-search-api-key >> .env
echo AZURE_BING_SEARCH_ENDPOINT=https://api.bing.microsoft.com/v7.0/search >> .env
echo.
echo .env file created successfully!
echo Please edit the .env file and replace the placeholder values with your actual API keys and endpoints.
pause