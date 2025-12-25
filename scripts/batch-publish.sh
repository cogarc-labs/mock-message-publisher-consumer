#!/bin/bash

# Batch publishing script
# Usage: ./batch-publish.sh <message-type>
# Message types: order-status, ucc, tour-appointment, truckload

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if message type is provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Message type is required${NC}"
    echo "Usage: $0 <message-type>"
    echo "Message types: order-status, ucc, tour-appointment, truckload"
    exit 1
fi

MESSAGE_TYPE="$1"

# Validate message type
case "$MESSAGE_TYPE" in
    order-status|ucc|tour-appointment|truckload)
        ;;
    *)
        echo -e "${RED}Error: Invalid message type: ${MESSAGE_TYPE}${NC}"
        echo "Valid types: order-status, ucc, tour-appointment, truckload"
        exit 1
        ;;
esac

# Application URL (default to localhost)
APP_URL="${APP_URL:-http://localhost:8080}"

echo -e "${GREEN}Starting batch publish for message type: ${MESSAGE_TYPE}${NC}"
echo -e "${YELLOW}Using application URL: ${APP_URL}${NC}"

# Check if application is running
if ! curl -s -f "${APP_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Application may not be running at ${APP_URL}${NC}"
    echo "Attempting to publish anyway..."
fi

# Call the REST endpoint
RESPONSE=$(curl -s -X POST "${APP_URL}/api/producer/batch/${MESSAGE_TYPE}" \
    -H "Content-Type: application/json" 2>&1)

# Check if curl was successful
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Batch publish request sent successfully${NC}"
    echo "Response: ${RESPONSE}"
    
    # Try to parse and display the count
    PUBLISHED_COUNT=$(echo "${RESPONSE}" | grep -o '"publishedCount":[0-9]*' | grep -o '[0-9]*' || echo "unknown")
    echo -e "${GREEN}Published ${PUBLISHED_COUNT} messages of type: ${MESSAGE_TYPE}${NC}"
else
    echo -e "${RED}Error: Failed to send batch publish request${NC}"
    echo "Response: ${RESPONSE}"
    exit 1
fi

echo -e "${GREEN}Batch publishing completed${NC}"

