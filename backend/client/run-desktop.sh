#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

OUT_DIR="backend/client/build_desktop"
mkdir -p "$OUT_DIR"

javac -cp "backend/client/lib/*" \
  -d "$OUT_DIR" \
  backend/client/src/model/Serveur.java \
  backend/client/src/controller/BackendConfig.java \
  backend/client/src/Service/ServeurService.java \
  backend/client/src/desktop/ui/BootstrapColors.java \
  backend/client/src/desktop/ui/CardPanel.java \
  backend/client/src/desktop/ui/NavPillButton.java \
  backend/client/src/desktop/ui/WrapLayout.java \
  backend/client/src/desktop/ui/BootstrapButton.java \
  backend/client/src/desktop/ui/StripedTableCellRenderer.java \
  backend/client/src/desktop/ui/TableButtonColumn.java \
  backend/client/src/desktop/SmartDriveConnection.java \
  backend/client/src/desktop/SmartDriveBootstrapApp.java \
  backend/client/src/desktop/SmartDriveDesktopApp.java

java -cp "$OUT_DIR:backend/client/lib/*:backend/client/resources" desktop.SmartDriveDesktopApp
