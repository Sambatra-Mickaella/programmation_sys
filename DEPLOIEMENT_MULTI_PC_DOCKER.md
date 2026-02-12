# Déploiement multi‑PC (Docker) — SmartDrive

Objectif : tester sur plusieurs PCs en n’envoyant **pas tout le projet** sur les machines serveurs (PC2/PC3). Oui c’est possible : un PC serveur peut avoir **uniquement** `backend/server/` (ou même juste son contenu Docker + resources + stockage).

## 0) Point important (à connaître pour la démo)

- Si chaque serveur a **son propre disque local**, alors selon le serveur choisi par le Load Balancer, on peut voir des fichiers différents.
- Pour une démo “cohérente” (mêmes fichiers quel que soit le serveur), il faut un **stockage partagé** (NFS/Samba) monté sur PC1/PC2/PC3 au même chemin (ex : `./shared_storage`).
- Sinon, c’est quand même testable, mais il faut l’assumer (ou forcer une affinité par utilisateur).

## 1) Ce que tu copies sur chaque PC

### PC2 (server2) et PC3 (server3)
Copie **uniquement** le dossier :
- `backend/server/`

Ce dossier contient déjà :
- `Dockerfile`, `src/`, `lib/`
- `resources/` (users/quotas/permissions/config)
- `shared_storage/` (stockage sur disque)

### PC1 (load balancer + server1)
Copie au minimum :
- `backend/loadbalancer/`
- `backend/server/` (pour server1)

## 2) Réseau / Ports à ouvrir

Sur le réseau local (LAN), il faut que PC1 puisse joindre PC2/PC3 sur :
- PC2 : `2122` (TCP) et `2222` (TCP notifications)
- PC3 : `2123` (TCP) et `2223` (TCP notifications)

Et côté PC1, exposer pour les tests :
- `2100` (Load Balancer)
- (optionnel) `2121` et `2221`

## 3) Compose par PC

> Les fichiers `docker-compose` **changent** selon le PC, parce que :
> - PC2/PC3 ne lancent que “server”
> - PC1 lance LB + web (et éventuellement server1)
> - le LB doit connaître les **IP réelles** des serveurs (pas `server2`, `server3` comme en Docker mono‑machine)

Note : sur certaines machines (comme la tienne), tu as `docker-compose` (Compose v1) mais pas `docker compose` (Compose v2). Dans ce cas, utilise toujours `docker-compose ...`.

### 3.1 PC2 : server2 uniquement

Sur PC2, place-toi **dans le dossier** `backend/server/` (copié) et crée `docker-compose.yml` avec :

```yaml
version: "3.8"

services:
  server2:
    build: .
    command: ["2122"]
    ports:
      - "2122:2122"
      - "2222:2222"
    volumes:
      - ./resources:/app/resources
      - ./shared_storage:/app/shared_storage
    restart: unless-stopped
```

Lancement :
- `docker-compose up -d --build`

### 3.2 PC3 : server3 uniquement

Sur PC3, même principe (dans `backend/server/`) :

```yaml
version: "3.8"

services:
  server3:
    build: .
    command: ["2123"]
    ports:
      - "2123:2123"
      - "2223:2223"
    volumes:
      - ./resources:/app/resources
      - ./shared_storage:/app/shared_storage
    restart: unless-stopped
```

Lancement :
- `docker-compose up -d --build`

### 3.3 PC1 : load balancer + server1

Sur PC1 (à la racine du projet, ou dans un dossier qui contient `backend/`), crée un fichier LB **spécial multi‑PC**.

#### a) Fichier LB : `backend/loadbalancer/resources/lb_config.multi.json`

Remplace `IP_PC1`, `IP_PC2`, `IP_PC3` par les IP LAN réelles.

```json
{
  "lb_port": 2100,
  "servers": [
    {"ip": "IP_PC1", "port": 2121},
    {"ip": "IP_PC2", "port": 2122},
    {"ip": "IP_PC3", "port": 2123}
  ],
  "max_clients_per_server": 1,
  "round_robin": true
}
```

#### b) Compose PC1 : `docker-compose.pc1.yml`

```yaml
version: "3.8"

services:
  server1:
    build:
      context: ./backend/server
    command: ["2121"]
    ports:
      - "2121:2121"
      - "2221:2221"
    volumes:
      - ./backend/server/resources:/app/resources
      - ./backend/server/shared_storage:/app/shared_storage
    restart: unless-stopped

  loadbalancer:
    build:
      context: ./backend/loadbalancer
    ports:
      - "2100:2100"
    volumes:
      - ./backend/loadbalancer/resources:/app/resources:ro
    environment:
      SMARTDRIVE_LB_CONFIG: /app/resources/lb_config.multi.json
    restart: unless-stopped

```

Lancement :
- `docker-compose -f docker-compose.pc1.yml up -d --build`

## 4) Test rapide

1) Sur PC2 et PC3 : vérifier que les ports répondent :
- depuis PC1 : `nc -vz IP_PC2 2122` et `nc -vz IP_PC3 2123`

2) Sur PC1 :
- lancer le client Swing sur un PC du LAN en pointant vers `IP_PC1:2100`

Exemple (sur la machine qui lance Swing) :
- `SMARTDRIVE_BACKEND_HOST=IP_PC1 SMARTDRIVE_BACKEND_PORT=2100 bash backend/client/run-desktop.sh`

## 5) Option “stockage partagé” (conseillé pour éviter des incohérences)

- Le plus simple : PC1 exporte `backend/server/shared_storage/` en NFS, PC2/PC3 montent ce dossier.
- Ensuite dans les compose PC2/PC3, le volume `./shared_storage:/app/shared_storage` pointe vers le dossier NFS monté.

---

Si tu me donnes les IP (PC1/PC2/PC3) et où tu veux héberger le Web (PC1 ou autre), je peux te fournir les fichiers finaux tout prêts (avec les IP réelles) + la checklist firewall exacte (Ubuntu/Windows). 
