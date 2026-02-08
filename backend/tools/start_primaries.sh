#!/bin/bash
# filepath: /home/andriantsoa/IdeaProjects/programmation_sys/backend/tools/start_primaries.sh
# Launch 3 primary servers on ports 2121, 2122, 2123
cd "$(dirname "$0")/../server"

# Kill existing MainServer processes
pkill -f MainServer
sleep 2

if [ ! -d "out" ]; then
    echo "Compiling server..."
    javac -cp ../lib/json-simple-1.1.1.jar -d out src/*.java
fi

echo "Starting primary server on port 2121..."
java -cp out:../lib/json-simple-1.1.1.jar MainServer 2121 &
echo $! > primary1.pid

echo "Starting primary server on port 2122..."
java -cp out:../lib/json-simple-1.1.1.jar MainServer 2122 &
echo $! > primary2.pid

echo "Starting primary server on port 2123..."
java -cp out:../lib/json-simple-1.1.1.jar MainServer 2123 &
echo $! > primary3.pid

echo "Primaries started. PIDs saved in *.pid files."
