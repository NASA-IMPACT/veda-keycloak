#!/bin/bash

# Set KEYCLOAK_VERSION to the provided environment variable or default to 'latest'
KEYCLOAK_VERSION=${KEYCLOAK_VERSION:-latest}

# Find all subdirectories excluding '.' and '.jars'
SUBDIRS=$(find . -maxdepth 1 -type d ! -name '.' ! -name '.jars')

# Loop over each subdirectory
for dir in $SUBDIRS; do
  echo "Building in $dir..."
  # Navigate into the subdirectory and run the Maven build command
  (cd "$dir" && mvn clean package "-Dkeycloak.version=$KEYCLOAK_VERSION")
done

# Create the .jars directory if it doesn't exist
mkdir -p .jars

# Find all JAR files in the 'target' directories and copy them to '.jars/'
find $SUBDIRS -type f -path '*/target/*.jar' -exec cp {} .jars/ \;
