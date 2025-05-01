#!/bin/bash

echo "Running VeriBot News Verification Agent..."
echo

# Check if target directory exists
if [ ! -d "target" ]; then
  echo "Building VeriBot with Maven..."
  mvn clean package -DskipTests
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
  echo ".env file not found."
  echo "Please create a .env file and configure your API keys."
  echo "Use the .env.template file as a reference."
  echo
  exit 1
fi

# Run the application
echo "Starting VeriBot..."
echo
java -jar target/veribot-1.0-SNAPSHOT-jar-with-dependencies.jar