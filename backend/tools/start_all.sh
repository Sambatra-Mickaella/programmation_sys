#!/bin/bash
# filepath: /home/andriantsoa/IdeaProjects/programmation_sys/backend/tools/start_all.sh
# Start all: primaries, load balancer, and test clients

echo "Starting primaries..."
./start_primaries.sh

sleep 2

echo "Starting load balancer..."
cd ../loadbalancer
if [ ! -d "out" ]; then
    javac -cp ../lib/json-simple-1.1.1.jar -d out src/*.java
fi
java -cp out:../lib/json-simple-1.1.1.jar LoadBalancer &
echo $! > lb.pid

sleep 2

echo "Starting test clients..."
cd ../client
if [ ! -d "out" ]; then
    javac -cp ../lib/json-simple-1.1.1.jar -d out src/MainClient.java src/Service/ServeurService.java src/model/Serveur.java src/model/User.java
fi

# Start multiple clients in background
java -cp out:../lib/json-simple-1.1.1.jar MainClient &
echo $! > client1.pid

echo "All started. Use kill to stop."
