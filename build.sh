#!/bin/sh
# TODO: BUILD CLIENT (needs port to Maven)
cd server/AsieLauncher/internal
zip -9 -r ../../../release/AsieLauncher-latest-server.zip *.js
