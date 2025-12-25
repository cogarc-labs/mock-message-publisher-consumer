#!/bin/bash

# Startup script for the notification publisher application
# Ensures PUBSUB_EMULATOR_HOST is set before starting

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Notification Publisher Startup Script ===${NC}"

# Set emulator host if not already set
if [ -z "$PUBSUB_EMULATOR_HOST" ]; then
    export PUBSUB_EMULATOR_HOST=localhost:8085
    echo -e "${YELLOW}Set PUBSUB_EMULATOR_HOST to: $PUBSUB_EMULATOR_HOST${NC}"
else
    echo -e "${GREEN}Using existing PUBSUB_EMULATOR_HOST: $PUBSUB_EMULATOR_HOST${NC}"
fi

# Verify emulator is running
echo -e "\n${GREEN}Checking if Pub/Sub emulator is running...${NC}"
if ! nc -z localhost 8085 2>/dev/null; then
    echo -e "${RED}ERROR: Pub/Sub emulator does not appear to be running on localhost:8085${NC}"
    echo -e "${YELLOW}Please start it in another terminal with:${NC}"
    echo -e "  ${GREEN}gcloud beta emulators pubsub start --project=cogarc-notification-project${NC}"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}✓ Pub/Sub emulator is running on localhost:8085${NC}"
fi

# Verify topics exist
echo -e "\n${GREEN}Verifying topics exist in emulator...${NC}"
export PUBSUB_EMULATOR_HOST=localhost:8085
if gcloud pubsub topics describe order-status-topic --project=cogarc-notification-project &>/dev/null; then
    echo -e "${GREEN}✓ Topics exist in emulator${NC}"
else
    echo -e "${YELLOW}WARNING: Topics may not exist. Run: ./scripts/setup-pubsub.sh${NC}"
fi

# Start the application
echo -e "\n${GREEN}Starting application with PUBSUB_EMULATOR_HOST=$PUBSUB_EMULATOR_HOST${NC}"
echo -e "${YELLOW}Note: The environment variable MUST be set before the JVM starts!${NC}\n"

# Export the variable and start Maven
export PUBSUB_EMULATOR_HOST
mvn spring-boot:run

