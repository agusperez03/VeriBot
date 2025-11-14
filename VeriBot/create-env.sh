#!/bin/bash

echo "Creating .env file for VeriBot configuration..."

cat > .env << EOL
# Azure OpenAI Configuration
AZURE_OPENAI_ENDPOINT=https://your-azure-openai-instance.cognitiveservices.azure.com/
AZURE_OPENAI_API_KEY=your-azure-openai-api-key
AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name
AZURE_OPENAI_API_VERSION=2024-12-01-preview

# SerpAPI Configuration
SERPAPI_KEY=your-serpapi-key

EOL

echo
echo ".env file created successfully!"
echo "Please edit the .env file and replace the placeholder values with your actual API keys and endpoints."