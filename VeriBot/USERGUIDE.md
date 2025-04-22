# VeriBot - User Guide

VeriBot is a Java-based news verification and summarization agent that helps users verify and summarize news content. This guide will help you set up and use VeriBot effectively.

## Setup Instructions

1. **Prerequisites**
   - Java 21 or higher installed
   - Maven installed
   - Azure OpenAI API access
   - Azure Bing Search API access

2. **Configuration**
   - Run `create-env.bat` to create a template `.env` file
   - Edit the `.env` file with your Azure OpenAI and Bing Search API credentials:
     ```
     # Azure OpenAI Configuration
     AZURE_OPENAI_ENDPOINT=https://your-azure-openai-instance.openai.azure.com/
     AZURE_OPENAI_API_KEY=your-azure-openai-api-key
     AZURE_OPENAI_DEPLOYMENT_NAME=your-deployment-name
     AZURE_OPENAI_API_VERSION=2023-05-15

     # Azure Bing Search API Configuration
     AZURE_BING_SEARCH_API_KEY=your-bing-search-api-key
     AZURE_BING_SEARCH_ENDPOINT=https://api.bing.microsoft.com/v7.0/search
     ```

3. **Building the Application**
   - The application will be built automatically when you run it
   - Alternatively, you can build it manually with: `mvn clean package`

## Running VeriBot

1. **Start the Application**
   - Run `run-veribot.bat` to start the application
   - The script will build the application if needed and check for the `.env` file

2. **Entering Queries**
   - When prompted, enter news-related queries, such as:
     - "What happened yesterday in New York?"
     - "Is it true that [some claim]?"
     - "Give me the latest news on climate change"
     - "What's going on with the elections in Brazil?"

3. **Understanding Results**
   - VeriBot will provide:
     - A concise summary of the news
     - A truthfulness percentage (0-100%)
     - A justification for the truthfulness rating
     - A list of sources used for verification

## Truthfulness Scale

VeriBot uses a four-tier truthfulness scale:
- **High (90-100%)**: Content is well-supported by multiple reliable sources
- **Moderate (60-89%)**: Content is mostly accurate with some uncertainties
- **Doubtful (30-59%)**: Content has significant inconsistencies or lacks verification
- **Low (0-29%)**: Content is mostly unsupported or contradicted by reliable sources

## Troubleshooting

- **API Key Issues**: Ensure your Azure API keys are correctly entered in the `.env` file
- **Connection Issues**: Check your internet connection and firewall settings
- **No Results**: Try rephrasing your query to be more specific or news-focused

## Example Output

```json
{
  "summary": "NASA announced yesterday that its Mars rover discovered evidence of ancient microbial life on Mars.",
  "truthfulness": "35%",
  "justification": "While NASA has made discoveries related to Mars, no confirmed evidence of microbial life has been announced. The claim exaggerates recent findings about water-related minerals.",
  "sources_used": ["nasa.gov", "space.com", "sciencedaily.com"]
}
```

For more information, please refer to the README.md file in the project repository.