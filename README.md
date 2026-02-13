# SmartDrive_prog_sys

Projet L3 “partage de fichiers” basé sur des **sockets TCP Java** (pas du WebSocket).

## 1) Ce que c’est (et ce que ce n’est pas)

- **Oui : projet sockets (TCP)** avec `java.net.ServerSocket` côté serveur et `java.net.Socket` côté client.
- **Non : WebSocket**. Il n’y a pas de handshake HTTP “Upgrade: websocket”, ni d’API `javax.websocket` / `jakarta.websocket`.

Le client principal est une interface **JavaFX native** qui parle directement au backend TCP.

## 2) Architecture (dossiers)

- `backend/server/` : serveur de stockage (TCP), gestion utilisateurs/quota/demandes de partage
- `backend/loadbalancer/` : répartiteur TCP (redirige un client vers un serveur primaire)
- `backend/client/` : client JavaFX (GUI native) + code Java socket
- `backend/server/shared_storage/` : stockage fichiers par utilisateur

## 3) Protocole applicatif (texte + binaire)

Après connexion TCP, le client envoie une commande texte (une ligne, séparateur `;`).

Authentification (obligatoire en premier) :

- `LOGIN;<username>;<password>`

Commandes principales :

- `LIST` → liste les fichiers de l’utilisateur connecté
- `USERS` → liste les utilisateurs existants (dossiers sous `shared_storage/users/`)
- `UPLOAD;<filename>;<size>` puis envoi **binaire** de `<size>` octets
- `DOWNLOAD;<filename>` → réponse `FILE;<size>` puis envoi **binaire**

Partage (accès lecture sur fichier d’un autre utilisateur) :

- `LIST_SHARED;<owner>` → liste les fichiers de `<owner>` + statut (`approved/pending/denied/none`)
- `REQUEST_READ;<owner>;<file>` → crée/relance une demande (`pending`)
- `LIST_REQUESTS` → liste les demandes reçues par l’utilisateur connecté (owner)
- `RESPOND_REQUEST;<requester>;<file>;<approve|deny>`
- `DOWNLOAD_AS;<owner>;<file>` → télécharge si la demande est `approved`

Quota :

- `QUOTA` → renvoie la valeur du quota (en octets) pour l’utilisateur

## 4) Ports par défaut

- Load balancer : `2100` (configurable)
- Primaires (exemple) : `2121`, `2122`, `2123`
- Notifications inter-primaires : port du primaire `+ 100` (ex: `2221`)

Les adresses/ports du LB sont définis dans :

- `backend/loadbalancer/resources/lb_config.json`

## 5) Lancer en local (CLI TCP + Load Balancer)

Prérequis : JDK 17+ (ou JDK compatible avec ton projet), `bash`.

### Démarrer 3 serveurs primaires

Depuis : `backend/server/tools/`

- `./start_primaries.sh`

Ce script compile et lance 3 instances `MainServer` sur 2121/2122/2123.

### Démarrer Load Balancer + un client de test

Depuis : `backend/server/tools/`

- `./start_all.sh`

Ça démarre : primaires + load balancer (2100) + un `MainClient` de test.

## 6) Lancer le client JavaFX pur (sans navigateur, sans servlet)

La version JavaFX se connecte **directement** au backend TCP (load balancer), sans Tomcat et sans interface web.

Depuis : `backend/client/`

- `./run-javafx.sh`

Prérequis :

- JDK 17+ (testé avec JDK 21)
- JavaFX installé (par défaut: `/usr/share/openjfx/lib`)

Si JavaFX est ailleurs, définir :

- `JAVA_FX_LIB=/chemin/vers/javafx/lib ./run-javafx.sh`

Le client lit la config backend depuis :

- `backend/client/resources/config.json`
- ou variables d’environnement `SMARTDRIVE_BACKEND_HOST` / `SMARTDRIVE_BACKEND_PORT`


## 6bis) Option Docker / docker-compose (backend + JavaFX, sans web)

Si tes amis ont des versions différentes de JDK/JavaFX, le plus simple est d’utiliser Docker.

Depuis la racine du repo :

Option A (si tu as Docker Compose v2) :

- Build + run : `docker compose up -d --build`
- Stop : `docker compose down --remove-orphans`

Option B (sans Compose, juste Docker) :

- Build + run : `./docker-up.sh`
- Stop : `./docker-down.sh`

Note : `docker-compose` (Compose v1, binaire `docker-compose`) est souvent **incompatible** avec Docker Engine récents (ex: 28+) et peut planter avec `ContainerConfig`.

Services exposés :

- `loadbalancer` : `localhost:2100`
- `javafx` : client GUI (pas de port HTTP)

Important pour Linux/X11 :

- autoriser Docker à afficher des fenêtres : `xhost +local:docker`
- s’assurer que `DISPLAY` est défini (ex: `:0`)
- les fichiers du PC hôte sont montés dans le conteneur JavaFX sous `/host-home`
- pour upload/download dans l’app JavaFX Docker, naviguer dans `/host-home`
- si tu lances Docker avec `sudo`, exporte `HOME` avant (`export HOME=/home/<ton_user>`)
- les noms de fichiers accentués (ex: `é`, `à`) sont supportés via locale UTF-8 dans le conteneur

Les serveurs primaires (`server1/2/3`) ne sont pas exposés sur l’hôte (pas de mapping de ports) : ils sont accessibles uniquement depuis le réseau Docker par le `loadbalancer`.

La config du LB en Docker est : `backend/loadbalancer/resources/lb_config.docker.json` (utilise les noms `server1/server2/server3`).


## 7) Configuration (users, permissions, quotas)

Backend server :

- Utilisateurs/mots de passe : `backend/server/resources/user.json`
- Permissions : `backend/server/resources/permissions.json`
- Quotas : `backend/server/resources/quotas.json`

Comptes de démo (par défaut) :

- `alice` / `1234`
- `bob` / `1234`
- `micka` / `1234`
- `Tsoa` / `1234` (admin)

### Admin Panel (JavaFX)

Un compte admin (ex: `micka`) peut accéder au panneau admin dans l’application JavaFX :

Fonctionnalités :

- **Gestion utilisateurs** : voir tous les users, bloquer/débloquer, supprimer, changer quota
- **Gestion stockage** : total utilisé, statut “slave” (reachable/unreachable), réplication (script/simulation)
- **Logs & Audit** : historique upload/download/delete/share
- **Monitoring** : CPU/RAM/disque + trafic (trafic simulé)

Important : le panneau admin JavaFX parle **directement** au backend via **TCP socket** (toujours pas de WebSocket).

Le code supporte aussi des overrides via variables d’environnement / propriétés Java (selon les fichiers) :

- `SMARTDRIVE_USERS_PATH` / `-Dsmartdrive.usersPath=...`
- `SMARTDRIVE_QUOTAS_PATH` / `-Dsmartdrive.quotasPath=...`
- `SMARTDRIVE_PERMISSIONS_PATH` / `-Dsmartdrive.permissionsPath=...`
- `SMARTDRIVE_LB_CONFIG` / `-Dsmartdrive.lbConfig=...`

## 8) “Ça marche déjà bien ?” (état)

Sur le papier, l’architecture est cohérente (TCP + protocole texte + transferts binaires + partage + quotas).
Par contre je n’ai **pas exécuté** ton projet ici : le bon verdict “ça marche” dépend de ton environnement (JDK/JavaFX, chemins, ports, droits fichiers).

## 9) Checklist démo (pour le prof)

1. Démarrer 3 primaires + LB
2. Se connecter avec 2 users (ex: `alice` et `bob`)
3. Upload chez `alice`
4. `bob` → liste fichiers partagés de `alice`, demande lecture
5. `alice` → accepte la demande
6. `bob` → `DOWNLOAD_AS` du fichier de `alice`




echo $DISPLAY
xhost +SI:localuser:root
docker-compose restart javafx
docker logs --tail 50 smartdrive_prog_sys_javafx_1
