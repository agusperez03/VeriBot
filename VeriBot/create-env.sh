#!/bin/bash

echo "Creating .env file for VeriBot configuration..."

cat > .env << EOL
# Azure OpenAI Configuration
AZURE_OPENAI_ENDPOINT=https://your-azure-openai-instance.openai.azure.com/
AZURE_OPENAI_API_KEY=your-azure-openai-api-key
AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name
AZURE_OPENAI_API_VERSION=2023-05-15

# Azure Bing Search API Configuration
AZURE_BING_SEARCH_API_KEY=your-bing-search-api-key
AZURE_BING_SEARCH_ENDPOINT=https://api.bing.microsoft.com/v7.0/search
EOL

echo
echo ".env file created successfully!"
echo "Please edit the .env file and replace the placeholder values with your actual API keys and endpoints."