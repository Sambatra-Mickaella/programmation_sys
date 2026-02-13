package fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class SmartDriveFxApp extends Application {

    private final BackendClient backend = new BackendClient();

    private Stage stage;
    private Scene scene;

    private String username;
    private String password;
    private boolean admin;

    private BorderPane shell;
    private Label titleLabel;
    private Label statusLabel;
    private Label identityLabel;
    private VBox sidebar;
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    private final DecimalFormat percentFormat = new DecimalFormat("0.0");
    private static final String HOST_HOME_DIR = "/host-home";

    private static final String NAV_ACCUEIL = "accueil";
    private static final String NAV_FILES = "fichiers";
    private static final String NAV_TRASH = "corbeille";
    private static final String NAV_SHARES = "partages";
    private static final String NAV_STORAGE = "stockage";
    private static final String NAV_ADMIN_DASH = "admin_dashboard";
    private static final String NAV_ADMIN_USERS = "admin_users";
    private static final String NAV_ADMIN_STORAGE = "admin_storage";
    private static final String NAV_ADMIN_LOGS = "admin_logs";
    private static final String NAV_ADMIN_MONITOR = "admin_monitor";

    @FunctionalInterface
    private interface UiWork<T> {
        T run() throws Exception;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(buildLoginRoot(), 1280, 820);
        applyStylesheet(this.scene);

        stage.setTitle("SmartDrive - JavaFX");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(700);
        stage.show();
    }

    private Parent buildLoginRoot() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-bg");

        VBox card = new VBox(16);
        card.getStyleClass().addAll("card", "login-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(440);

        Label brand = new Label("SmartDrive");
        brand.getStyleClass().add("login-brand");

        Label subtitle = new Label("Connexion JavaFX (sans navigateur, sans servlet)");
        subtitle.getStyleClass().add("muted-text");

        Label backendInfo = new Label(resolveBackendInfo());
        backendInfo.getStyleClass().add("small-muted");

        TextField userField = new TextField();
        userField.setPromptText("Nom d'utilisateur");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Mot de passe");

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-text");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Button loginBtn = new Button("Connexion");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Runnable loginAction = () -> {
            String u = userField.getText() == null ? "" : userField.getText().trim();
            String p = passField.getText() == null ? "" : passField.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Nom et mot de passe requis.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            setNodeBusy(card, true);
            runAsync(() -> backend.login(u, p), auth -> {
                this.username = auth.username();
                this.password = p;
                this.admin = auth.admin();
                buildMainShell();
                showAccueil();
            }, err -> {
                errorLabel.setText(normalizeError(err));
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                setNodeBusy(card, false);
            }, () -> setNodeBusy(card, false));
        };

        loginBtn.setOnAction(e -> loginAction.run());
        passField.setOnAction(e -> loginAction.run());

        card.getChildren().addAll(
                brand,
                subtitle,
                backendInfo,
                userField,
                passField,
                errorLabel,
                loginBtn
        );

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(28));
        root.setCenter(center);
        return root;
    }

    private void buildMainShell() {
        shell = new BorderPane();
        shell.getStyleClass().add("app-bg");

        titleLabel = new Label("SmartDrive");
        titleLabel.getStyleClass().add("page-title");

        identityLabel = new Label(username + (admin ? " (admin)" : ""));
        identityLabel.getStyleClass().add("small-muted");

        Button refreshBtn = new Button("Rafraichir");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setOnAction(e -> reloadCurrentPage());

        Button logoutBtn = new Button("Deconnexion");
        logoutBtn.getStyleClass().add("btn-danger-outline");
        logoutBtn.setOnAction(e -> logoutToLogin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12,
                labelAsBrand("SmartDrive"),
                new Separator(),
                titleLabel,
                spacer,
                identityLabel,
                refreshBtn,
                logoutBtn
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 18, 14, 18));
        topBar.getStyleClass().add("topbar");
        shell.setTop(topBar);

        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(220);

        VBox identityCard = new VBox(4);
        identityCard.getStyleClass().addAll("card", "identity-card");
        identityCard.setPadding(new Insets(10));
        Label hello = new Label("Bonjour, " + username);
        hello.getStyleClass().add("identity-title");
        Label mail = new Label(username + "@example.com");
        mail.getStyleClass().add("small-muted");
        identityCard.getChildren().addAll(hello, mail);

        sidebar.getChildren().add(identityCard);

        addNavButton(NAV_ACCUEIL, "Accueil", this::showAccueil);
        addNavButton(NAV_FILES, "Mes fichiers", this::showMesFichiers);
        addNavButton(NAV_TRASH, "Corbeille", this::showCorbeille);
        addNavButton(NAV_SHARES, "Partages", this::showPartages);
        addNavButton(NAV_STORAGE, "Stockage", this::showStockage);

        if (admin) {
            sidebar.getChildren().add(new Separator());
            addNavButton(NAV_ADMIN_DASH, "Admin dashboard", this::showAdminDashboard);
            addNavButton(NAV_ADMIN_USERS, "Admin users", this::showAdminUsers);
            addNavButton(NAV_ADMIN_STORAGE, "Admin storage", this::showAdminStorage);
            addNavButton(NAV_ADMIN_LOGS, "Admin logs", this::showAdminLogs);
            addNavButton(NAV_ADMIN_MONITOR, "Admin monitor", this::showAdminMonitor);
        }

        shell.setLeft(sidebar);

        statusLabel = new Label("Pret");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setPadding(new Insets(8, 16, 10, 16));
        shell.setBottom(statusLabel);

        scene.setRoot(shell);
    }

    private void addNavButton(String key, String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("nav-btn");
        btn.setOnAction(e -> action.run());
        navButtons.put(key, btn);
        sidebar.getChildren().add(btn);
    }

    private Label labelAsBrand(String value) {
        Label brand = new Label(value);
        brand.getStyleClass().add("brand-text");
        return brand;
    }

    private void activateNav(String key, String title) {
        titleLabel.setText(title);
        navButtons.forEach((k, b) -> {
            b.getStyleClass().remove("active");
            if (k.equals(key)) {
                b.getStyleClass().add("active");
            }
        });
    }

    private void showLoading(String message) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));

        ProgressIndicator pi = new ProgressIndicator();
        Label label = new Label(message);
        label.getStyleClass().add("muted-text");
        box.getChildren().addAll(pi, label);

        setCenter(box);
    }

    private void setCenter(Node node) {
        ScrollPane scroll = new ScrollPane(node);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        scroll.setPadding(new Insets(0));
        shell.setCenter(scroll);
    }

    private void showAccueil() {
        activateNav(NAV_ACCUEIL, "Accueil");
        showLoading("Chargement accueil...");
        setStatus("Chargement des notifications et utilisateurs...");

        runAsync(() -> {
            List<BackendClient.NotificationEntry> notifs = backend.listNotifications(username, password);
            List<String> users = backend.listUsers(username, password);
            return new AccueilData(users, notifs);
        }, data -> {
            VBox page = new VBox(18);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label("Utilisateurs");
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button clearNotifBtn = new Button("Vider notifications");
            clearNotifBtn.getStyleClass().add("btn-outline");
            clearNotifBtn.setOnAction(e -> clearNotificationsAndReload());
            header.getChildren().addAll(title, spacer, clearNotifBtn);

            if (!data.notifications().isEmpty()) {
                VBox notifCard = new VBox(8);
                notifCard.getStyleClass().add("card");
                notifCard.setPadding(new Insets(14));

                Label notifTitle = new Label("Notifications");
                notifTitle.getStyleClass().add("card-title");

                ListView<String> notifList = new ListView<>();
                notifList.setPrefHeight(Math.min(220, data.notifications().size() * 30.0 + 40.0));
                ObservableList<String> notifItems = FXCollections.observableArrayList();
                for (BackendClient.NotificationEntry n : data.notifications()) {
                    notifItems.add((n.message() == null || n.message().isBlank()) ? "(vide)" : n.message());
                }
                notifList.setItems(notifItems);

                notifCard.getChildren().addAll(notifTitle, notifList);
                page.getChildren().add(notifCard);
            }

            page.getChildren().add(header);

            FlowPane usersPane = new FlowPane();
            usersPane.setHgap(12);
            usersPane.setVgap(12);
            usersPane.setPrefWrapLength(900);

            for (String u : data.users()) {
                if (u == null || u.isBlank() || u.equalsIgnoreCase(username)) {
                    continue;
                }
                usersPane.getChildren().add(buildUserCard(u));
            }

            if (usersPane.getChildren().isEmpty()) {
                page.getChildren().add(infoBox("Aucun autre utilisateur disponible."));
            } else {
                page.getChildren().add(usersPane);
            }

            setCenter(page);
            setStatus("Accueil charge");
        });
    }

    private Node buildUserCard(String user) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));
        card.setPrefWidth(220);

        Label name = new Label(user);
        name.getStyleClass().add("card-title");

        Label sub = new Label("Fichiers et partages");
        sub.getStyleClass().add("small-muted");

        Button view = new Button("Voir fichiers");
        view.getStyleClass().add("btn-primary");
        view.setMaxWidth(Double.MAX_VALUE);
        view.setOnAction(e -> showPartagesFichiers(user));

        card.getChildren().addAll(name, sub, view);
        return card;
    }

    private void clearNotificationsAndReload() {
        runAsync(() -> backend.clearNotifications(username, password), resp -> {
            setStatus(resp);
            showAccueil();
        });
    }

    private void showMesFichiers() {
        activateNav(NAV_FILES, "Mes fichiers");
        showLoading("Chargement des fichiers...");
        setStatus("Recuperation de vos fichiers...");

        runAsync(() -> backend.listFiles(username, password), files -> renderMesFichiers(files, "", "all", "", ""));
    }

    private void renderMesFichiers(List<BackendClient.FileEntry> allFiles,
                                   String query,
                                   String type,
                                   String minRaw,
                                   String maxRaw) {
        VBox page = new VBox(14);
        page.getStyleClass().add("page-content");
        page.setPadding(new Insets(18));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Mes fichiers");
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button uploadBtn = new Button("Upload");
        uploadBtn.getStyleClass().add("btn-primary");
        uploadBtn.setOnAction(e -> chooseAndUpload());

        header.getChildren().addAll(title, spacer, uploadBtn);

        GridPane filters = new GridPane();
        filters.setHgap(8);
        filters.setVgap(8);

        TextField qField = new TextField(query);
        qField.setPromptText("Nom");

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("all", "pdf", "image", "video", "audio", "doc", "sheet", "txt", "zip");
        typeBox.setValue(type == null || type.isBlank() ? "all" : type);

        TextField minField = new TextField(minRaw);
        minField.setPromptText("Taille min");

        TextField maxField = new TextField(maxRaw);
        maxField.setPromptText("Taille max");

        Button applyBtn = new Button("Filtrer");
        applyBtn.getStyleClass().add("btn-outline");

        filters.add(new Label("Nom"), 0, 0);
        filters.add(qField, 0, 1);
        filters.add(new Label("Type"), 1, 0);
        filters.add(typeBox, 1, 1);
        filters.add(new Label("Min"), 2, 0);
        filters.add(minField, 2, 1);
        filters.add(new Label("Max"), 3, 0);
        filters.add(maxField, 3, 1);
        filters.add(applyBtn, 4, 1);

        ColumnConstraintsUtil.setGrow(filters, 0, Priority.ALWAYS);
        ColumnConstraintsUtil.setGrow(filters, 1, Priority.NEVER);
        ColumnConstraintsUtil.setGrow(filters, 2, Priority.NEVER);
        ColumnConstraintsUtil.setGrow(filters, 3, Priority.NEVER);
        ColumnConstraintsUtil.setGrow(filters, 4, Priority.NEVER);

        List<BackendClient.FileEntry> filtered = filterFiles(allFiles, query, typeBox.getValue(), minRaw, maxRaw);

        FlowPane fileCards = new FlowPane();
        fileCards.setHgap(12);
        fileCards.setVgap(12);
        fileCards.setPrefWrapLength(980);

        for (BackendClient.FileEntry f : filtered) {
            fileCards.getChildren().add(buildOwnFileCard(f));
        }

        if (filtered.isEmpty()) {
            fileCards.getChildren().add(infoBox("Aucun fichier trouve."));
        }

        applyBtn.setOnAction(e -> {
            renderMesFichiers(
                    allFiles,
                    safeText(qField),
                    typeBox.getValue(),
                    safeText(minField),
                    safeText(maxField)
            );
        });

        page.getChildren().addAll(header, filters, fileCards);
        setCenter(page);
        setStatus(filtered.size() + " fichier(s)");
    }

    private List<BackendClient.FileEntry> filterFiles(List<BackendClient.FileEntry> files,
                                                      String query,
                                                      String type,
                                                      String minRaw,
                                                      String maxRaw) {
        String q = query == null ? "" : query.trim().toLowerCase();
        long min = parseLong(minRaw, -1L);
        long max = parseLong(maxRaw, -1L);
        String t = type == null ? "all" : type.trim().toLowerCase();

        List<BackendClient.FileEntry> out = new ArrayList<>();
        for (BackendClient.FileEntry f : files) {
            String nameLower = f.name().toLowerCase();
            if (!q.isEmpty() && !nameLower.contains(q)) {
                continue;
            }
            if (!matchesType(nameLower, t)) {
                continue;
            }
            if (min >= 0 && f.size() < min) {
                continue;
            }
            if (max >= 0 && f.size() > max) {
                continue;
            }
            out.add(f);
        }
        return out;
    }

    private Node buildOwnFileCard(BackendClient.FileEntry file) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));
        card.setPrefWidth(240);

        Label icon = new Label(fileIcon(file.name()));
        icon.getStyleClass().add("file-icon");

        Label name = new Label(file.name());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);

        Label meta = new Label(formatSize(file.size()) + " - " + formatDate(file.modifiedAt()));
        meta.getStyleClass().add("small-muted");

        HBox actions = new HBox(6);
        Button view = new Button("Voir");
        view.getStyleClass().add("btn-primary");
        view.setOnAction(e -> viewOwnFile(file.name()));

        Button versions = new Button("Versions");
        versions.getStyleClass().add("btn-outline");
        versions.setOnAction(e -> showVersionsDialog(file.name()));

        Button download = new Button("DL");
        download.getStyleClass().add("btn-outline");
        download.setOnAction(e -> chooseAndDownloadOwn(file.name()));

        Button delete = new Button("Suppr");
        delete.getStyleClass().add("btn-danger-outline");
        delete.setOnAction(e -> deleteFile(file.name()));

        actions.getChildren().addAll(view, versions, download, delete);
        card.getChildren().addAll(icon, name, meta, actions);
        return card;
    }

    private void showVersionsDialog(String filename) {
        runAsync(() -> backend.listVersions(username, password, filename), versions -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Versions - " + filename);

            ListView<BackendClient.VersionEntry> list = new ListView<>();
            list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            list.setItems(FXCollections.observableArrayList(versions));
            list.setCellFactory(v -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(BackendClient.VersionEntry item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.id() + " | " + formatSize(item.size()) + " | " + item.createdAt());
                    }
                }
            });

            dialog.getDialogPane().setContent(list);
            ButtonType restoreType = new ButtonType("Restaurer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(restoreType, ButtonType.CLOSE);
            Node restoreNode = dialog.getDialogPane().lookupButton(restoreType);
            restoreNode.setDisable(true);
            list.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                restoreNode.setDisable(newVal == null);
            });

            dialog.setResultConverter(bt -> {
                if (bt == restoreType) {
                    BackendClient.VersionEntry selected = list.getSelectionModel().getSelectedItem();
                    return selected == null ? null : selected.id();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(id -> runAsync(
                    () -> backend.restoreVersion(username, password, filename, id),
                    resp -> {
                        info("Version", resp);
                        showMesFichiers();
                    }
            ));
        });
    }

    private void deleteFile(String filename) {
        if (!confirm("Suppression", "Supprimer " + filename + " (deplacement vers la corbeille) ?")) {
            return;
        }
        runAsync(() -> backend.deleteToTrash(username, password, filename), resp -> {
            setStatus(resp);
            showMesFichiers();
        });
    }

    private void chooseAndUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier a envoyer");
        java.io.File initialDir = resolveHostInitialDirectory();
        if (initialDir != null) {
            chooser.setInitialDirectory(initialDir);
        }
        java.io.File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }
        Path path = selected.toPath();
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            warn("Upload", "Fichier inaccessible depuis le conteneur. Ouvre le fichier depuis /host-home.");
            return;
        }
        runAsync(() -> backend.uploadFile(username, password, path), resp -> {
            info("Upload", resp);
            showMesFichiers();
        });
    }

    private void chooseAndDownloadOwn(String filename) {
        Path target = chooseTargetFile(filename);
        if (target == null) {
            return;
        }
        runAsync(() -> backend.downloadOwnFile(username, password, filename, target), done -> {
            setStatus("Telechargement termine: " + done);
        });
    }

    private void viewOwnFile(String filename) {
        runPreviewDownload(() -> {
            Path temp = Files.createTempFile("smartdrive_view_", "_" + sanitizeFilename(filename));
            backend.downloadOwnFile(username, password, filename, temp);
            return temp;
        }, filename);
    }

    private void showCorbeille() {
        activateNav(NAV_TRASH, "Corbeille");
        showLoading("Chargement corbeille...");

        runAsync(() -> backend.listTrash(username, password), rows -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label("Fichiers supprimes");
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button purgeAll = new Button("Vider corbeille");
            purgeAll.getStyleClass().add("btn-danger-outline");
            purgeAll.setOnAction(e -> purgeAllTrash());
            header.getChildren().addAll(title, spacer, purgeAll);

            TableView<BackendClient.TrashEntry> table = new TableView<>();
            table.setItems(FXCollections.observableArrayList(rows));

            TableColumn<BackendClient.TrashEntry, String> nameCol = new TableColumn<>("Fichier");
            nameCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().originalName()));
            nameCol.setPrefWidth(320);

            TableColumn<BackendClient.TrashEntry, String> sizeCol = new TableColumn<>("Taille");
            sizeCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatSize(d.getValue().size())));
            sizeCol.setPrefWidth(110);

            TableColumn<BackendClient.TrashEntry, String> dateCol = new TableColumn<>("Supprime le");
            dateCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().deletedAt()));
            dateCol.setPrefWidth(180);

            TableColumn<BackendClient.TrashEntry, Void> actionCol = new TableColumn<>("Actions");
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button restore = new Button("Restaurer");
                private final Button purge = new Button("Supprimer");
                private final HBox box = new HBox(6, restore, purge);

                {
                    restore.getStyleClass().add("btn-primary");
                    purge.getStyleClass().add("btn-danger-outline");
                    restore.setOnAction(e -> {
                        BackendClient.TrashEntry item = getTableView().getItems().get(getIndex());
                        runAsync(() -> backend.restoreTrash(username, password, item.id()), r -> showCorbeille());
                    });
                    purge.setOnAction(e -> {
                        BackendClient.TrashEntry item = getTableView().getItems().get(getIndex());
                        if (!confirm("Suppression definitive", "Supprimer definitivement " + item.originalName() + " ?")) {
                            return;
                        }
                        runAsync(() -> backend.purgeTrash(username, password, item.id()), r -> showCorbeille());
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
            actionCol.setPrefWidth(220);

            table.getColumns().add(nameCol);
            table.getColumns().add(sizeCol);
            table.getColumns().add(dateCol);
            table.getColumns().add(actionCol);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            if (rows.isEmpty()) {
                page.getChildren().addAll(header, infoBox("Corbeille vide."));
            } else {
                page.getChildren().addAll(header, table);
            }
            setCenter(page);
            setStatus(rows.size() + " element(s) en corbeille");
        });
    }

    private void purgeAllTrash() {
        if (!confirm("Corbeille", "Vider toute la corbeille ?")) {
            return;
        }
        runAsync(() -> backend.purgeTrash(username, password, "ALL"), resp -> {
            setStatus(resp);
            showCorbeille();
        });
    }

    private void showPartages() {
        activateNav(NAV_SHARES, "Partages");
        showLoading("Chargement partages...");

        runAsync(() -> backend.listUsers(username, password), users -> {
            VBox page = new VBox(14);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(8);
            Label title = new Label("Partages");
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button requests = new Button("Demandes recues");
            requests.getStyleClass().add("btn-outline");
            requests.setOnAction(e -> showPartagesDemandes());
            header.getChildren().addAll(title, spacer, requests);

            FlowPane usersPane = new FlowPane();
            usersPane.setHgap(12);
            usersPane.setVgap(12);
            usersPane.setPrefWrapLength(940);

            for (String u : users) {
                if (u == null || u.isBlank() || u.equalsIgnoreCase(username)) {
                    continue;
                }
                usersPane.getChildren().add(buildUserCard(u));
            }

            if (usersPane.getChildren().isEmpty()) {
                page.getChildren().addAll(header, infoBox("Aucun autre utilisateur."));
            } else {
                page.getChildren().addAll(header, usersPane);
            }

            setCenter(page);
            setStatus("Partages charges");
        });
    }

    private void showPartagesFichiers(String owner) {
        activateNav(NAV_SHARES, "Partages - " + owner);
        showLoading("Chargement des fichiers partages...");

        runAsync(() -> backend.listSharedFiles(username, password, owner), rows -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(8);
            Label title = new Label("Fichiers de " + owner);
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button back = new Button("Retour");
            back.getStyleClass().add("btn-outline");
            back.setOnAction(e -> showPartages());
            header.getChildren().addAll(title, spacer, back);

            FlowPane pane = new FlowPane();
            pane.setHgap(12);
            pane.setVgap(12);
            pane.setPrefWrapLength(940);

            for (BackendClient.SharedFileEntry row : rows) {
                pane.getChildren().add(buildSharedFileCard(owner, row));
            }

            if (rows.isEmpty()) {
                page.getChildren().addAll(header, infoBox("Aucun fichier partage."));
            } else {
                page.getChildren().addAll(header, pane);
            }
            setCenter(page);
            setStatus(rows.size() + " fichier(s) de " + owner);
        });
    }

    private Node buildSharedFileCard(String owner, BackendClient.SharedFileEntry row) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));
        card.setPrefWidth(250);

        Label name = new Label(row.name());
        name.getStyleClass().add("card-title");
        name.setWrapText(true);

        Label meta = new Label(formatSize(row.size()) + " | status: " + row.status());
        meta.getStyleClass().add("small-muted");

        HBox actions = new HBox(6);
        String status = row.status() == null ? "" : row.status().toLowerCase();

        if ("approved".equals(status)) {
            Button view = new Button("Voir");
            view.getStyleClass().add("btn-primary");
            view.setOnAction(e -> viewSharedFile(owner, row.name()));

            Button dl = new Button("Telecharger");
            dl.getStyleClass().add("btn-outline");
            dl.setOnAction(e -> chooseAndDownloadShared(owner, row.name()));
            actions.getChildren().addAll(view, dl);
        } else if ("pending".equals(status)) {
            Label pending = new Label("Demande en attente");
            pending.getStyleClass().add("small-muted");
            actions.getChildren().add(pending);
        } else {
            Button ask = new Button("Demander lecture");
            ask.getStyleClass().add("btn-outline");
            ask.setOnAction(e -> runAsync(
                    () -> backend.requestRead(username, password, owner, row.name()),
                    resp -> {
                        info("Partage", resp);
                        showPartagesFichiers(owner);
                    }
            ));
            actions.getChildren().add(ask);
        }

        card.getChildren().addAll(name, meta, actions);
        return card;
    }

    private void showPartagesDemandes() {
        activateNav(NAV_SHARES, "Demandes de partage");
        showLoading("Chargement des demandes...");

        runAsync(() -> backend.listIncomingRequests(username, password), rows -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(8);
            Label title = new Label("Demandes d'acces recues");
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button back = new Button("Retour partages");
            back.getStyleClass().add("btn-outline");
            back.setOnAction(e -> showPartages());
            header.getChildren().addAll(title, spacer, back);

            TableView<BackendClient.ShareRequestEntry> table = new TableView<>();
            table.setItems(FXCollections.observableArrayList(rows));

            TableColumn<BackendClient.ShareRequestEntry, String> reqCol = new TableColumn<>("Demandeur");
            reqCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().requester()));
            reqCol.setPrefWidth(150);

            TableColumn<BackendClient.ShareRequestEntry, String> fileCol = new TableColumn<>("Fichier");
            fileCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().file()));
            fileCol.setPrefWidth(300);

            TableColumn<BackendClient.ShareRequestEntry, String> statusCol = new TableColumn<>("Statut");
            statusCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().status()));
            statusCol.setPrefWidth(120);

            TableColumn<BackendClient.ShareRequestEntry, Void> actionCol = new TableColumn<>("Action");
            actionCol.setCellFactory(col -> new TableCell<>() {
                private final Button approve = new Button("Approuver");
                private final Button deny = new Button("Refuser");
                private final HBox box = new HBox(6, approve, deny);

                {
                    approve.getStyleClass().add("btn-primary");
                    deny.getStyleClass().add("btn-danger-outline");
                    approve.setOnAction(e -> respondCurrent("approve"));
                    deny.setOnAction(e -> respondCurrent("deny"));
                }

                private void respondCurrent(String action) {
                    BackendClient.ShareRequestEntry item = getTableView().getItems().get(getIndex());
                    runAsync(
                            () -> backend.respondRequest(username, password, item.requester(), item.file(), action),
                            resp -> {
                                setStatus(resp);
                                showPartagesDemandes();
                            }
                    );
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
            actionCol.setPrefWidth(220);

            table.getColumns().add(reqCol);
            table.getColumns().add(fileCol);
            table.getColumns().add(statusCol);
            table.getColumns().add(actionCol);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            if (rows.isEmpty()) {
                page.getChildren().addAll(header, infoBox("Aucune demande."));
            } else {
                page.getChildren().addAll(header, table);
            }

            setCenter(page);
            setStatus(rows.size() + " demande(s)");
        });
    }

    private void chooseAndDownloadShared(String owner, String filename) {
        Path target = chooseTargetFile(filename);
        if (target == null) {
            return;
        }
        runAsync(() -> backend.downloadSharedFile(username, password, owner, filename, target), done -> {
            setStatus("Telechargement termine: " + done);
        });
    }

    private void viewSharedFile(String owner, String filename) {
        runPreviewDownload(() -> {
            Path temp = Files.createTempFile("smartdrive_shared_view_", "_" + sanitizeFilename(filename));
            backend.downloadSharedFile(username, password, owner, filename, temp);
            return temp;
        }, filename);
    }

    private void showStockage() {
        activateNav(NAV_STORAGE, "Stockage");
        showLoading("Chargement stockage...");

        runAsync(() -> {
            List<BackendClient.FileEntry> files = backend.listFiles(username, password);
            long quota = backend.getQuota(username, password);
            return new StorageData(files, quota);
        }, data -> {
            long used = 0L;
            for (BackendClient.FileEntry f : data.files()) {
                used += Math.max(0, f.size());
            }
            long quota = Math.max(0, data.quota());
            long free = Math.max(0, quota - used);
            double pct = quota <= 0 ? 0.0 : Math.min(1.0, used / (double) quota);

            VBox page = new VBox(14);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            Label title = new Label("Mon stockage");
            title.getStyleClass().add("section-title");

            HBox cards = new HBox(12,
                    statCard("Utilise", formatSize(used)),
                    statCard("Disponible", formatSize(free)),
                    statCard("Quota", formatSize(quota)),
                    statCard("Fichiers", String.valueOf(data.files().size()))
            );

            VBox progressCard = new VBox(8);
            progressCard.getStyleClass().add("card");
            progressCard.setPadding(new Insets(12));
            Label pTitle = new Label("Progression");
            pTitle.getStyleClass().add("card-title");
            ProgressBar bar = new ProgressBar(pct);
            bar.setPrefWidth(540);
            Label pLabel = new Label(percentFormat.format(pct * 100.0) + "%");
            pLabel.getStyleClass().add("small-muted");
            progressCard.getChildren().addAll(pTitle, bar, pLabel);

            GridPane details = new GridPane();
            details.setHgap(12);
            details.setVgap(8);
            details.getStyleClass().add("card");
            details.setPadding(new Insets(12));
            addDetail(details, 0, "Quota total", formatSize(quota));
            addDetail(details, 1, "Espace utilise", formatSize(used));
            addDetail(details, 2, "Espace libre", formatSize(free));
            addDetail(details, 3, "Pourcentage", percentFormat.format(pct * 100.0) + "%");
            addDetail(details, 4, "Nombre de fichiers", String.valueOf(data.files().size()));

            if (pct >= 0.8) {
                page.getChildren().add(warnBox("Attention: votre espace de stockage est presque plein."));
            } else if (pct >= 0.6) {
                page.getChildren().add(warnBox("Votre espace de stockage commence a se remplir."));
            }

            page.getChildren().addAll(title, cards, progressCard, details);
            setCenter(page);
            setStatus("Stockage charge");
        });
    }

    private Node statCard(String label, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));
        box.setPrefWidth(220);
        Label l = new Label(label);
        l.getStyleClass().add("small-muted");
        Label v = new Label(value);
        v.getStyleClass().add("card-title");
        box.getChildren().addAll(l, v);
        return box;
    }

    private void addDetail(GridPane grid, int row, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("small-muted");
        Label v = new Label(value);
        v.getStyleClass().add("card-title");
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private void showAdminDashboard() {
        if (!admin) {
            showAccueil();
            return;
        }
        activateNav(NAV_ADMIN_DASH, "Admin dashboard");
        showLoading("Chargement dashboard admin...");

        runAsync(() -> {
            Map<String, String> storage = backend.adminStorage(username, password);
            Map<String, String> monitor = backend.adminMonitor(username, password);
            List<String> logs = backend.adminLogs(username, password, 20);
            return new AdminDashboardData(storage, monitor, logs);
        }, data -> {
            VBox page = new VBox(14);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox cards = new HBox(12,
                    statCard("Total storage", formatSize(parseLong(data.storage().get("totalUsedBytes"), 0L))),
                    statCard("Total files", data.storage().getOrDefault("totalFiles", "?")),
                    statCard("CPU", data.monitor().getOrDefault("cpuPercent", "?") + "%"),
                    statCard("Traffic", data.monitor().getOrDefault("trafficKbps", "?") + " Kbps")
            );

            VBox logsCard = new VBox(8);
            logsCard.getStyleClass().add("card");
            logsCard.setPadding(new Insets(12));
            Label logsTitle = new Label("Audit recent");
            logsTitle.getStyleClass().add("card-title");
            ListView<String> list = new ListView<>(FXCollections.observableArrayList(formatAuditRows(data.logs())));
            list.setPrefHeight(320);
            logsCard.getChildren().addAll(logsTitle, list);

            page.getChildren().addAll(cards, logsCard);
            setCenter(page);
            setStatus("Dashboard admin charge");
        });
    }

    private void showAdminUsers() {
        if (!admin) {
            showAccueil();
            return;
        }
        activateNav(NAV_ADMIN_USERS, "Admin users");
        showLoading("Chargement utilisateurs admin...");

        runAsync(() -> backend.adminListUsers(username, password), rows -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            Label title = new Label("Gestion utilisateurs");
            title.getStyleClass().add("section-title");

            TableView<BackendClient.AdminUserEntry> table = new TableView<>();
            table.setItems(FXCollections.observableArrayList(rows));

            TableColumn<BackendClient.AdminUserEntry, String> userCol = new TableColumn<>("User");
            userCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().username()));
            userCol.setPrefWidth(180);

            TableColumn<BackendClient.AdminUserEntry, String> adminCol = new TableColumn<>("Admin");
            adminCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().admin() ? "Oui" : "Non"));
            adminCol.setPrefWidth(90);

            TableColumn<BackendClient.AdminUserEntry, String> blockCol = new TableColumn<>("Blocked");
            blockCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().blocked() ? "Oui" : "Non"));
            blockCol.setPrefWidth(110);

            TableColumn<BackendClient.AdminUserEntry, String> quotaCol = new TableColumn<>("Quota");
            quotaCol.setCellValueFactory(d -> new ReadOnlyStringWrapper(String.valueOf(d.getValue().quota())));
            quotaCol.setPrefWidth(140);

            table.getColumns().add(userCol);
            table.getColumns().add(adminCol);
            table.getColumns().add(blockCol);
            table.getColumns().add(quotaCol);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            VBox actions = new VBox(8);
            actions.getStyleClass().add("card");
            actions.setPadding(new Insets(12));

            Label sel = new Label("Selectionnez un utilisateur dans le tableau.");
            sel.getStyleClass().add("small-muted");

            HBox firstRow = new HBox(8);
            Button toggle = new Button("Bloquer/Debloquer");
            toggle.getStyleClass().add("btn-outline");
            Button delete = new Button("Supprimer");
            delete.getStyleClass().add("btn-danger-outline");
            Button files = new Button("Voir fichiers");
            files.getStyleClass().add("btn-primary");
            firstRow.getChildren().addAll(toggle, delete, files);

            HBox quotaRow = new HBox(8);
            TextField quotaField = new TextField();
            quotaField.setPromptText("Nouveau quota (bytes)");
            Button applyQuota = new Button("Appliquer quota");
            applyQuota.getStyleClass().add("btn-outline");
            quotaRow.getChildren().addAll(quotaField, applyQuota);

            toggle.setDisable(true);
            delete.setDisable(true);
            files.setDisable(true);
            applyQuota.setDisable(true);

            table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
                boolean disabled = item == null;
                toggle.setDisable(disabled);
                delete.setDisable(disabled);
                files.setDisable(disabled);
                applyQuota.setDisable(disabled);
                if (!disabled) {
                    sel.setText("Selection: " + item.username() + " | blocked=" + item.blocked() + " | quota=" + item.quota());
                    quotaField.setText(String.valueOf(item.quota()));
                }
            });

            toggle.setOnAction(e -> {
                BackendClient.AdminUserEntry item = table.getSelectionModel().getSelectedItem();
                if (item == null) {
                    return;
                }
                if (item.username().equalsIgnoreCase(username)) {
                    warn("Action", "Vous ne pouvez pas vous bloquer vous-meme.");
                    return;
                }
                runAsync(() -> backend.adminSetBlocked(username, password, item.username(), !item.blocked()), resp -> showAdminUsers());
            });

            delete.setOnAction(e -> {
                BackendClient.AdminUserEntry item = table.getSelectionModel().getSelectedItem();
                if (item == null) {
                    return;
                }
                if (item.username().equalsIgnoreCase(username)) {
                    warn("Action", "Vous ne pouvez pas vous supprimer vous-meme.");
                    return;
                }
                if (!confirm("Suppression", "Supprimer definitivement " + item.username() + " ?")) {
                    return;
                }
                runAsync(() -> backend.adminDeleteUser(username, password, item.username()), resp -> showAdminUsers());
            });

            files.setOnAction(e -> {
                BackendClient.AdminUserEntry item = table.getSelectionModel().getSelectedItem();
                if (item != null) {
                    showAdminUserFiles(item.username());
                }
            });

            applyQuota.setOnAction(e -> {
                BackendClient.AdminUserEntry item = table.getSelectionModel().getSelectedItem();
                if (item == null) {
                    return;
                }
                long q = parseLong(quotaField.getText(), -1L);
                if (q < 0L) {
                    warn("Quota", "Valeur de quota invalide.");
                    return;
                }
                runAsync(() -> backend.adminSetQuota(username, password, item.username(), q), resp -> showAdminUsers());
            });

            actions.getChildren().addAll(sel, firstRow, quotaRow);

            page.getChildren().addAll(title, table, actions);
            setCenter(page);
            setStatus(rows.size() + " utilisateur(s)");
        });
    }

    private void showAdminUserFiles(String owner) {
        activateNav(NAV_ADMIN_USERS, "Fichiers de " + owner);
        showLoading("Chargement fichiers utilisateur...");

        runAsync(() -> backend.adminListUserFiles(username, password, owner), files -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(8);
            Label title = new Label("Fichiers de " + owner);
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button back = new Button("Retour users");
            back.getStyleClass().add("btn-outline");
            back.setOnAction(e -> showAdminUsers());
            header.getChildren().addAll(title, spacer, back);

            FlowPane pane = new FlowPane();
            pane.setHgap(12);
            pane.setVgap(12);
            pane.setPrefWrapLength(920);

            for (BackendClient.FileEntry f : files) {
                VBox card = new VBox(8);
                card.getStyleClass().add("card");
                card.setPadding(new Insets(12));
                card.setPrefWidth(240);

                Label name = new Label(f.name());
                name.getStyleClass().add("card-title");
                name.setWrapText(true);

                Label meta = new Label(formatSize(f.size()));
                meta.getStyleClass().add("small-muted");

                HBox actions = new HBox(6);
                Button view = new Button("Voir");
                view.getStyleClass().add("btn-primary");
                view.setOnAction(e -> viewAdminFile(owner, f.name()));

                Button dl = new Button("Telecharger");
                dl.getStyleClass().add("btn-outline");
                dl.setOnAction(e -> chooseAndDownloadAdmin(owner, f.name()));
                actions.getChildren().addAll(view, dl);

                card.getChildren().addAll(name, meta, actions);
                pane.getChildren().add(card);
            }

            if (files.isEmpty()) {
                page.getChildren().addAll(header, infoBox("Aucun fichier pour cet utilisateur."));
            } else {
                page.getChildren().addAll(header, pane);
            }

            setCenter(page);
            setStatus(files.size() + " fichier(s) admin");
        });
    }

    private void chooseAndDownloadAdmin(String owner, String filename) {
        Path target = chooseTargetFile(filename);
        if (target == null) {
            return;
        }
        runAsync(() -> backend.adminDownloadAs(username, password, owner, filename, target), done -> {
            setStatus("Telechargement admin termine: " + done);
        });
    }

    private void viewAdminFile(String owner, String filename) {
        runPreviewDownload(() -> {
            Path temp = Files.createTempFile("smartdrive_admin_view_", "_" + sanitizeFilename(filename));
            backend.adminDownloadAs(username, password, owner, filename, temp);
            return temp;
        }, filename);
    }

    private void showAdminStorage() {
        if (!admin) {
            showAccueil();
            return;
        }
        activateNav(NAV_ADMIN_STORAGE, "Admin storage");
        showLoading("Chargement stockage admin...");

        runAsync(() -> backend.adminStorage(username, password), storage -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            Label title = new Label("Stockage global");
            title.getStyleClass().add("section-title");

            HBox cards = new HBox(12,
                    statCard("Total utilise", formatSize(parseLong(storage.get("totalUsedBytes"), 0L))),
                    statCard("Total fichiers", storage.getOrDefault("totalFiles", "?")),
                    statCard("Replication", storage.getOrDefault("replication", "?"))
            );

            GridPane slave = new GridPane();
            slave.setHgap(10);
            slave.setVgap(8);
            slave.getStyleClass().add("card");
            slave.setPadding(new Insets(12));
            addDetail(slave, 0, "Slave host", storage.getOrDefault("slaveHost", "?"));
            addDetail(slave, 1, "Slave port", storage.getOrDefault("slavePort", "?"));
            addDetail(slave, 2, "Slave status", storage.getOrDefault("slaveStatus", "?"));

            page.getChildren().addAll(title, cards, slave);
            setCenter(page);
            setStatus("Stockage admin charge");
        });
    }

    private void showAdminLogs() {
        if (!admin) {
            showAccueil();
            return;
        }
        activateNav(NAV_ADMIN_LOGS, "Admin logs");
        showLoading("Chargement logs admin...");

        runAsync(() -> backend.adminLogs(username, password, 150), logs -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            HBox header = new HBox(8);
            Label title = new Label("Logs & audit");
            title.getStyleClass().add("section-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button refresh = new Button("Rafraichir");
            refresh.getStyleClass().add("btn-outline");
            refresh.setOnAction(e -> showAdminLogs());
            header.getChildren().addAll(title, spacer, refresh);

            ListView<String> list = new ListView<>(FXCollections.observableArrayList(formatAuditRows(logs)));
            list.setPrefHeight(560);

            if (logs.isEmpty()) {
                page.getChildren().addAll(header, infoBox("Aucun log."));
            } else {
                page.getChildren().addAll(header, list);
            }

            setCenter(page);
            setStatus(logs.size() + " log(s)");
        });
    }

    private void showAdminMonitor() {
        if (!admin) {
            showAccueil();
            return;
        }
        activateNav(NAV_ADMIN_MONITOR, "Admin monitor");
        showLoading("Chargement monitor admin...");

        runAsync(() -> backend.adminMonitor(username, password), monitor -> {
            VBox page = new VBox(12);
            page.getStyleClass().add("page-content");
            page.setPadding(new Insets(18));

            Label title = new Label("Monitoring systeme (sans graphes)");
            title.getStyleClass().add("section-title");

            HBox cards = new HBox(12,
                    statCard("CPU", monitor.getOrDefault("cpuPercent", "?") + "%"),
                    statCard("RAM", formatSize(parseLong(monitor.get("ramUsedBytes"), 0L))),
                    statCard("Disk", formatSize(parseLong(monitor.get("diskUsedBytes"), 0L))),
                    statCard("Traffic", monitor.getOrDefault("trafficKbps", "?") + " Kbps")
            );

            GridPane details = new GridPane();
            details.setHgap(10);
            details.setVgap(8);
            details.getStyleClass().add("card");
            details.setPadding(new Insets(12));
            addDetail(details, 0, "RAM total", formatSize(parseLong(monitor.get("ramTotalBytes"), 0L)));
            addDetail(details, 1, "Disk total", formatSize(parseLong(monitor.get("diskTotalBytes"), 0L)));
            addDetail(details, 2, "CPU raw", monitor.getOrDefault("cpuPercent", "-1"));
            addDetail(details, 3, "Traffic raw", monitor.getOrDefault("trafficKbps", "0"));

            page.getChildren().addAll(title, cards, details);
            setCenter(page);
            setStatus("Monitor admin charge");
        });
    }

    private List<String> formatAuditRows(List<String> logs) {
        List<String> out = new ArrayList<>();
        for (String line : logs) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] p = line.split(";", 4);
            if (p.length < 4) {
                out.add(line);
            } else {
                out.add(p[0] + " | " + p[1] + " | " + p[2] + " | " + p[3]);
            }
        }
        return out;
    }

    private Node infoBox(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-text");
        VBox box = new VBox(label);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));
        return box;
    }

    private Node warnBox(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("warn-text");
        VBox box = new VBox(label);
        box.getStyleClass().addAll("card", "warn-card");
        box.setPadding(new Insets(14));
        return box;
    }

    private void reloadCurrentPage() {
        String active = navButtons.entrySet().stream()
                .filter(e -> e.getValue().getStyleClass().contains("active"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(NAV_ACCUEIL);

        switch (active) {
            case NAV_FILES -> showMesFichiers();
            case NAV_TRASH -> showCorbeille();
            case NAV_SHARES -> showPartages();
            case NAV_STORAGE -> showStockage();
            case NAV_ADMIN_DASH -> showAdminDashboard();
            case NAV_ADMIN_USERS -> showAdminUsers();
            case NAV_ADMIN_STORAGE -> showAdminStorage();
            case NAV_ADMIN_LOGS -> showAdminLogs();
            case NAV_ADMIN_MONITOR -> showAdminMonitor();
            default -> showAccueil();
        }
    }

    private void logoutToLogin() {
        this.username = null;
        this.password = null;
        this.admin = false;
        scene.setRoot(buildLoginRoot());
    }

    private void runPreviewDownload(UiWork<Path> loader, String filename) {
        runAsync(loader, path -> {
            try {
                previewFile(path, filename);
            } catch (Exception e) {
                error("Preview", e);
            }
        });
    }

    private void previewFile(Path path, String filename) throws Exception {
        if (isPdfPreviewable(filename)) {
            showPdfPreview(path, filename);
            return;
        }

        if (isImagePreviewable(filename)) {
            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.setTitle("Apercu image - " + filename);

            Image image = new Image(path.toUri().toString(), false);
            if (image.isError()) {
                throw new IOException("Impossible de lire l'image.");
            }

            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setFitWidth(980);
            imageView.setFitHeight(760);

            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            BorderPane root = new BorderPane(scrollPane);
            root.setPadding(new Insets(10));
            Scene s = new Scene(root, 1020, 780);
            applyStylesheet(s);
            dialog.setScene(s);
            dialog.show();
            return;
        }

        if (isTextPreviewable(filename)) {
            String content;
            try (InputStream in = Files.newInputStream(path)) {
                byte[] head = in.readNBytes(32_000);
                content = new String(head, StandardCharsets.UTF_8);
            }
            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.setTitle("Apercu - " + filename);

            TextArea area = new TextArea(content);
            area.setEditable(false);
            area.setWrapText(true);

            BorderPane root = new BorderPane(area);
            root.setPadding(new Insets(10));
            Scene s = new Scene(root, 860, 620);
            applyStylesheet(s);
            dialog.setScene(s);
            dialog.show();
            return;
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(path.toFile());
            info("Apercu", "Fichier ouvert dans l'application systeme.");
        } else {
            info("Apercu", "Type non texte. Utilisez Telecharger.");
        }
    }

    private void showPdfPreview(Path pdfPath, String filename) throws Exception {
        Path tempDir = Files.createTempDirectory("smartdrive_pdf_preview_");
        List<Path> pages = renderPdfToPngPages(pdfPath, tempDir);
        if (pages.isEmpty()) {
            deleteDirectoryQuietly(tempDir);
            throw new IOException("Aucune page PDF convertie.");
        }

        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.setTitle("Apercu PDF - " + filename);

        VBox pagesBox = new VBox(12);
        pagesBox.setPadding(new Insets(10));
        for (Path p : pages) {
            Image img = new Image(p.toUri().toString(), false);
            if (img.isError()) {
                continue;
            }
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitWidth(980);
            pagesBox.getChildren().add(iv);
        }

        if (pagesBox.getChildren().isEmpty()) {
            deleteDirectoryQuietly(tempDir);
            throw new IOException("Impossible d'afficher les pages du PDF.");
        }

        ScrollPane scrollPane = new ScrollPane(pagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        BorderPane root = new BorderPane(scrollPane);
        root.setPadding(new Insets(10));
        Scene s = new Scene(root, 1020, 780);
        applyStylesheet(s);
        dialog.setScene(s);
        dialog.setOnHidden(e -> deleteDirectoryQuietly(tempDir));
        dialog.show();
    }

    private List<Path> renderPdfToPngPages(Path pdfPath, Path outDir) throws Exception {
        String prefix = outDir.resolve("page").toString();
        ProcessBuilder pb = new ProcessBuilder(
                "pdftoppm",
                "-png",
                "-f", "1",
                "-l", "50",
                pdfPath.toString(),
                prefix
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) {
            throw new IOException("Conversion PDF impossible (pdftoppm): " + output);
        }

        try (Stream<Path> stream = Files.list(outDir)) {
            return stream
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.startsWith("page-") && name.endsWith(".png");
                    })
                    .sorted()
                    .toList();
        }
    }

    private void deleteDirectoryQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private boolean isTextPreviewable(String filename) {
        String n = filename == null ? "" : filename.toLowerCase();
        return n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".log") || n.endsWith(".csv")
                || n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".yaml") || n.endsWith(".yml")
                || n.endsWith(".java") || n.endsWith(".js") || n.endsWith(".css") || n.endsWith(".html")
                || n.endsWith(".properties");
    }

    private boolean isImagePreviewable(String filename) {
        String n = filename == null ? "" : filename.toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp")
                || n.endsWith(".jfif");
    }

    private boolean isPdfPreviewable(String filename) {
        String n = filename == null ? "" : filename.toLowerCase();
        return n.endsWith(".pdf");
    }

    private Path chooseTargetFile(String filename) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir le fichier de destination");
        chooser.setInitialFileName(filename);
        java.io.File initialDir = resolveHostInitialDirectory();
        if (initialDir != null) {
            chooser.setInitialDirectory(initialDir);
        }
        java.io.File selected = chooser.showSaveDialog(stage);
        if (selected == null) {
            return null;
        }
        return selected.toPath();
    }

    private java.io.File resolveHostInitialDirectory() {
        java.io.File hostHome = new java.io.File(HOST_HOME_DIR);
        if (hostHome.exists() && hostHome.isDirectory()) {
            return hostHome;
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            java.io.File home = new java.io.File(userHome);
            if (home.exists() && home.isDirectory()) {
                return home;
            }
        }
        return null;
    }

    private void runAsync(UiWork<Void> work, Runnable onSuccess) {
        runAsync(work, v -> onSuccess.run());
    }

    private <T> void runAsync(UiWork<T> work, java.util.function.Consumer<T> onSuccess) {
        runAsync(work, onSuccess, err -> error("Erreur", err), null);
    }

    private <T> void runAsync(UiWork<T> work,
                              java.util.function.Consumer<T> onSuccess,
                              java.util.function.Consumer<Throwable> onError,
                              Runnable onFinally) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.run();
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
            if (onFinally != null) {
                onFinally.run();
            }
        });

        task.setOnFailed(e -> {
            if (onError != null) {
                onError.accept(task.getException());
            }
            if (onFinally != null) {
                onFinally.run();
            }
        });

        Thread t = new Thread(task, "smartdrive-ui-task");
        t.setDaemon(true);
        t.start();
    }

    private void setNodeBusy(Node node, boolean busy) {
        node.setDisable(busy);
        if (scene != null) {
            scene.setCursor(busy ? javafx.scene.Cursor.WAIT : javafx.scene.Cursor.DEFAULT);
        }
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text == null ? "" : text);
        }
    }

    private String resolveBackendInfo() {
        try {
            model.Serveur s = controller.BackendConfig.newServeur();
            return "Backend: " + s.getIp() + ":" + s.getPort();
        } catch (Exception e) {
            return "Backend: 127.0.0.1:2100";
        }
    }

    private void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.initOwner(stage);
        a.showAndWait();
    }

    private void warn(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.initOwner(stage);
        a.showAndWait();
    }

    private void error(String title, Throwable t) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText("Operation echouee");
        a.setContentText(normalizeError(t));
        a.initOwner(stage);
        a.showAndWait();
        setStatus(normalizeError(t));
    }

    private boolean confirm(String title, String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        a.setTitle(title);
        a.setHeaderText(null);
        a.initOwner(stage);
        Optional<ButtonType> result = a.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String normalizeError(Throwable t) {
        if (t == null) {
            return "Erreur inconnue";
        }
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            return t.getClass().getSimpleName();
        }
        if (msg.startsWith("ERROR ")) {
            return msg.substring("ERROR ".length());
        }
        return msg;
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "fichier.bin";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safeText(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " o";
        }
        if (bytes < 1024L * 1024L) {
            return String.format("%.1f Ko", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.1f Mo", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.2f Go", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatDate(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(epochMillis));
    }

    private String fileIcon(String filename) {
        String n = filename == null ? "" : filename.toLowerCase();
        if (n.endsWith(".pdf")) {
            return "PDF";
        }
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif")) {
            return "IMG";
        }
        if (n.endsWith(".doc") || n.endsWith(".docx")) {
            return "DOC";
        }
        if (n.endsWith(".xls") || n.endsWith(".xlsx")) {
            return "XLS";
        }
        if (n.endsWith(".mp3") || n.endsWith(".wav")) {
            return "AUD";
        }
        if (n.endsWith(".mp4") || n.endsWith(".avi") || n.endsWith(".mkv")) {
            return "VID";
        }
        return "FILE";
    }

    private boolean matchesType(String filename, String type) {
        if (type == null || type.isBlank() || "all".equals(type)) {
            return true;
        }
        return switch (type) {
            case "pdf" -> filename.endsWith(".pdf");
            case "image" -> filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")
                    || filename.endsWith(".gif") || filename.endsWith(".webp");
            case "video" -> filename.endsWith(".mp4") || filename.endsWith(".avi") || filename.endsWith(".mkv");
            case "audio" -> filename.endsWith(".mp3") || filename.endsWith(".wav");
            case "doc" -> filename.endsWith(".doc") || filename.endsWith(".docx") || filename.endsWith(".odt");
            case "sheet" -> filename.endsWith(".xls") || filename.endsWith(".xlsx") || filename.endsWith(".ods");
            case "txt" -> filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".log");
            case "zip" -> filename.endsWith(".zip") || filename.endsWith(".rar") || filename.endsWith(".7z");
            default -> true;
        };
    }

    private void applyStylesheet(Scene s) {
        try {
            String css = Objects.requireNonNull(getClass().getResource("/fx-smartdrive.css")).toExternalForm();
            s.getStylesheets().add(css);
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record AccueilData(List<String> users, List<BackendClient.NotificationEntry> notifications) {}
    private record StorageData(List<BackendClient.FileEntry> files, long quota) {}
    private record AdminDashboardData(Map<String, String> storage, Map<String, String> monitor, List<String> logs) {}

    private static final class ColumnConstraintsUtil {
        private ColumnConstraintsUtil() {
        }

        static void setGrow(GridPane grid, int columnIndex, Priority priority) {
            while (grid.getColumnConstraints().size() <= columnIndex) {
                grid.getColumnConstraints().add(new javafx.scene.layout.ColumnConstraints());
            }
            javafx.scene.layout.ColumnConstraints cc = grid.getColumnConstraints().get(columnIndex);
            cc.setHgrow(priority);
        }
    }
}
