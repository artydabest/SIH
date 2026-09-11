#!/bin/bash
# Starts the disaster-relay backend and emergency-dashboard frontend together.
# Closing this Terminal window stops both servers.

cd "$(dirname "$0")/disaster-relay" || exit 1
npm run dev &
BACKEND_PID=$!

cd "$(dirname "$0")/emergency-dashboard" || exit 1
npm run dev
FRONTEND_PID=$?

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null" EXIT
wait
