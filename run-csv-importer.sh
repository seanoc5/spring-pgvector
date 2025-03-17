#!/bin/bash

# Build the project
./gradlew build -x test

# Run the CSV importer using Groovy
# Pass all arguments to the Groovy script
groovy -cp "build/classes/groovy/main:build/resources/main:$(./gradlew -q printClasspath)" src/main/groovy/test/SamGovCsvImporter.groovy "$@"
