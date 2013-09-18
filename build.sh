#!/bin/sh
rm -rf release/*.zip
# Client
cd release
mkdir al
cd al
unzip ../AsieLauncher-latest.jar
rm ../AsieLauncher-latest.jar
rm -rf resources
zip -0 -r ../AsieLauncher-latest.jar .
cd ..
rm -rf al
cd ..
# Server
cd server/AsieLauncher/internal
zip -9 -r ../../../release/AsieLauncher-latest-server.zip *.js mod*.json
cd ../..
zip -9 -r ../release/AsieLauncher-latest-server.zip package.json
# Bootstrap
rm -rf AsieLauncher/internal/launcher/*
rm -rf AsieLauncher/temp/*
mv AsieLauncher/internal/info.json temp-info.json
zip -9 -r ../release/AsieLauncher-latest-bootstrap.zip AsieLauncher also-config.json package.json also.js
mv temp-info.json AsieLauncher/internal/info.json
