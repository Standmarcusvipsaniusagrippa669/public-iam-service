#!/bin/bash
set -e
PROFILE=valiantech-sso
REGION=us-east-1
ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text --profile $PROFILE)
SERVICE=valiantech-core-iam

aws ecr get-login-password --region $REGION --profile $PROFILE | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com

echo "🚀 Building and pushing $SERVICE ..."
./gradlew clean bootBuildImage
docker tag valiantech/$SERVICE:latest $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$SERVICE:latest
docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$SERVICE:latest
