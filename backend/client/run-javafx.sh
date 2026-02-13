#!/bin/bash
set -euo pipefail

SRC_DIR="src"
RES_DIR="resources"
LIB_DIR="lib"
OUT_DIR="build-fx"
MAIN_CLASS="fx.SmartDriveFxApp"

JAVA_FX_LIB="${JAVA_FX_LIB:-/usr/share/openjfx/lib}"

if [ ! -d "$JAVA_FX_LIB" ]; then
  echo "JavaFX introuvable. Definis JAVA_FX_LIB (ex: /usr/share/openjfx/lib)"
  exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/classes"

{
  find "$SRC_DIR/fx" -name "*.java"
  find "$SRC_DIR/model" -name "*.java"
  printf "%s\n" "$SRC_DIR/controller/BackendConfig.java"
} > "$OUT_DIR/sources.txt"

javac \
  --module-path "$JAVA_FX_LIB" \
  --add-modules javafx.controls,javafx.graphics \
  -cp ".:$LIB_DIR/json-simple-1.1.1.jar" \
  -d "$OUT_DIR/classes" \
  @"$OUT_DIR/sources.txt"

if [ -d "$RES_DIR" ]; then
  cp -r "$RES_DIR/." "$OUT_DIR/classes/"
fi

java \
  --module-path "$JAVA_FX_LIB" \
  --add-modules javafx.controls,javafx.graphics \
  -cp "$OUT_DIR/classes:$LIB_DIR/json-simple-1.1.1.jar" \
  "$MAIN_CLASS"
