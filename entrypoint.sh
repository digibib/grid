#!/bin/bash
set -e

bash /opt/createConfigs.sh

sleep infinity

# Trick to keep dev running in docker
sbt "; runAll ; console"
