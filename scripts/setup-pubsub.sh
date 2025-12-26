#!/bin/bash

# Setup script for Google Pub/Sub topics and subscriptions
# Supports both Pub/Sub emulator and real GCP

set -e

PROJECT_ID="cogarc-notification-project"
EMULATOR_HOST="${PUBSUB_EMULATOR_HOST:-localhost:8085}"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Setting up Pub/Sub topics and subscriptions for project: ${PROJECT_ID}${NC}"

# Check if using emulator
if [[ -n "$PUBSUB_EMULATOR_HOST" ]]; then
    echo -e "${YELLOW}Using Pub/Sub Emulator at: ${EMULATOR_HOST}${NC}"
    export PUBSUB_EMULATOR_HOST="${EMULATOR_HOST}"
else
    echo -e "${GREEN}Using real GCP Pub/Sub${NC}"
    # Verify gcloud is installed and authenticated
    if ! command -v gcloud &> /dev/null; then
        echo "Error: gcloud CLI is not installed. Please install it first."
        exit 1
    fi
    gcloud config set project "${PROJECT_ID}"
fi

# Topic and subscription names
declare -a TOPICS=(
    "order-status-topic"
    "ucc-topic"
    "tour-appointment-topic"
    "truckload-topic"
)

declare -a SUBSCRIPTIONS=(
    "order-status-subscription:order-status-topic"
    "ucc-subscription:ucc-topic"
    "tour-appointment-subscription:tour-appointment-topic"
    "truckload-subscription:truckload-topic"
)

# Create topics
echo -e "\n${GREEN}Creating topics...${NC}"
for topic in "${TOPICS[@]}"; do
    if [[ -n "$PUBSUB_EMULATOR_HOST" ]]; then
        # Using emulator - use gcloud with emulator host
        gcloud pubsub topics create "${topic}" \
            --project="${PROJECT_ID}" \ 2>/dev/null || \
        echo "Topic ${topic} already exists or emulator not running"
    else
        # Using real GCP
        gcloud pubsub topics create "${topic}" \
            --project="${PROJECT_ID}" 2>/dev/null || \
        echo "Topic ${topic} already exists"
    fi
    echo "✓ Topic: ${topic}"
done

# Create subscriptions
echo -e "\n${GREEN}Creating subscriptions...${NC}"
for sub_info in "${SUBSCRIPTIONS[@]}"; do
    IFS=':' read -r subscription topic <<< "${sub_info}"
    if [[ -n "$PUBSUB_EMULATOR_HOST" ]]; then
        # Using emulator
        gcloud pubsub subscriptions create "${subscription}" \
            --topic="${topic}" \
            --project="${PROJECT_ID}" 2>/dev/null || \
        echo "Subscription ${subscription} already exists or emulator not running"
    else
        # Using real GCP
        gcloud pubsub subscriptions create "${subscription}" \
            --topic="${topic}" \
            --project="${PROJECT_ID}" 2>/dev/null || \
        echo "Subscription ${subscription} already exists"
    fi
    echo "✓ Subscription: ${subscription} -> Topic: ${topic}"
done

echo -e "\n${GREEN}Setup complete!${NC}"
echo -e "\nTo use the emulator, set:"
echo -e "  export PUBSUB_EMULATOR_HOST=localhost:8085"
echo -e "\nTo start the emulator:"
echo -e "  gcloud beta emulators pubsub start --project=${PROJECT_ID}"

