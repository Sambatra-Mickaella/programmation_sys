#!/bin/bash
set -euo pipefail

APP_NAME="SmartDrive"
SRC_DIR="src"
WEB_DIR="views"
BUILD_DIR="build"
LIB_DIR="lib"

TOMCAT_WEBAPPS="/opt/tomcat/webapps"
STARTUP="/opt/tomcat/bin/startup.sh"
SHUTDOWN="/opt/tomcat/bin/shutdown.sh"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/assets" "$BUILD_DIR/pages" "$BUILD_DIR/WEB-INF/classes" "$BUILD_DIR/WEB-INF/lib"

find "$SRC_DIR" -name "*.java" > sources.txt
javac -cp ".:$LIB_DIR/*" -d "$BUILD_DIR/WEB-INF/classes" @sources.txt
rm sources.txt

cp -r "$WEB_DIR/pages/." "$BUILD_DIR/pages/"
cp -r "$WEB_DIR/assets/." "$BUILD_DIR/assets/"
cp -r "$WEB_DIR/"*.xml "$BUILD_DIR/WEB-INF/"

if [ -d "resources" ]; then
  cp -r "resources/." "$BUILD_DIR/WEB-INF/classes/"
fi

if [ -f "$LIB_DIR/json-simple-1.1.1.jar" ]; then
  cp "$LIB_DIR/json-simple-1.1.1.jar" "$BUILD_DIR/WEB-INF/lib/"
fi

(
  cd "$BUILD_DIR"
  jar -cvf "$APP_NAME.war" .
)

sudo cp -f "$BUILD_DIR/$APP_NAME.war" "$TOMCAT_WEBAPPS/"

echo "🔁 Restart Tomcat..."
if [ -x "$SHUTDOWN" ]; then
  sudo "$SHUTDOWN" || true
  sleep 3
fi
if [ -x "$STARTUP" ]; then
  sudo "$STARTUP"
else
  echo "Tomcat startup script not found at $STARTUP"
  echo "Please start Tomcat manually, then open: http://localhost:8080/$APP_NAME/"
fi

echo "✅ Deploy finished"
