#!/bin/bash

# Définition des variables
APP_NAME="SmartDrive" #nom_projet
SRC_DIR="src"
WEB_DIR="views"
BUILD_DIR="build"
LIB_DIR="lib"
TOMCAT_WEBAPPS="/opt/tomcat/webapps"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"
JSON_SIMPLE_JAR="$LIB_DIR/json-simple-1.1.1.jar"

# Nettoyage et création du répertoire temporaire
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/assets
mkdir -p $BUILD_DIR/pages
mkdir -p $BUILD_DIR/WEB-INF/classes
mkdir -p $BUILD_DIR/WEB-INF/lib

# Compilation des fichiers Java avec le JAR des Servlets
find $SRC_DIR -name "*.java" > sources.txt
javac -cp ".:lib/*" -d $BUILD_DIR/WEB-INF/classes @sources.txt
rm sources.txt

# Copier les fichiers web (web.xml, JSP, etc.)
cp -r $WEB_DIR/pages/* $BUILD_DIR/pages/
cp -r $WEB_DIR/assets/* $BUILD_DIR/assets/
cp -r $WEB_DIR/*.xml $BUILD_DIR/WEB-INF/

# Copier les resources (config.json, etc.) dans le classpath webapp
if [ -d "resources" ]; then
  cp -r resources/* $BUILD_DIR/WEB-INF/classes/
fi

# Copier les fichiers (.jar) dans WEB-INF/lib
# Ne pas packager servlet-api.jar dans le WAR (fourni par Tomcat).
# Packager les dépendances runtime nécessaires.
if [ -f "$JSON_SIMPLE_JAR" ]; then
  cp -r "$JSON_SIMPLE_JAR" "$BUILD_DIR/WEB-INF/lib/"
fi

# Générer le fichier .war dans le dossier build
cd $BUILD_DIR || exit
jar -cvf $APP_NAME.war *
cd ..

# Déploiement dans Tomcat
cp -f $BUILD_DIR/$APP_NAME.war $TOMCAT_WEBAPPS/

echo ""
echo "🔁 Redémarrage de Tomcat..."
$STARTUP
sleep 10

echo "Déploiement terminé. Redémarrez Tomcat si nécessaire."

echo ""
