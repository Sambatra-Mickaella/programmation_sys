package desktop;

public final class SmartDriveDesktopApp {

    public static void main(String[] args) {
        SmartDriveBootstrapApp.main(args);
    }
}

/*
 * Legacy / broken content kept temporarily during refactor.
 * The new Swing UI that matches the JSP design lives in SmartDriveBootstrapApp.
 

    static final class SmartDriveFrame extends JFrame {
        private final CardLayout cards = new CardLayout();
        private final JPanel root = new JPanel(cards);

        private final JTextField usernameField = new JTextField(20);
        private final JPasswordField passwordField = new JPasswordField(20);
        private final JButton loginButton = new JButton("Connexion");
        private final JLabel loginStatus = new JLabel(" ");

        private final FilesTableModel filesModel = new FilesTableModel();
        private final JTable filesTable = new JTable(filesModel);

        private final JButton refreshButton = new JButton("Rafraîchir");
        private final JButton uploadButton = new JButton("Upload");
        private final JButton downloadButton = new JButton("Download");
        private final JButton deleteButton = new JButton("Corbeille");
        private final JButton logoutButton = new JButton("Déconnexion");

        private final JLabel quotaLabel = new JLabel("Quota: -");
        private final JProgressBar storageBar = new JProgressBar(0, 100);
        private final JLabel statusLabel = new JLabel(" ");

        private SmartDriveConnection conn;

        SmartDriveFrame() {
            super("SmartDrive - Client Desktop (Swing)");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setSize(900, 600);
            setLocationRelativeTo(null);

            root.add(buildLoginPanel(), "login");
            root.add(buildMainPanel(), "main");
            setContentPane(root);

            cards.show(root, "login");
            wireActions();

            storageBar.setStringPainted(true);
            storageBar.setValue(0);
            storageBar.setString("-");

            filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filesTable.setFillsViewportHeight(true);
        }

        private JPanel buildLoginPanel() {
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridx = 0; c.gridy = 0;
            p.add(new JLabel("Utilisateur"), c);
            c.gridx = 1;
            p.add(usernameField, c);

            c.gridx = 0; c.gridy = 1;
            p.add(new JLabel("Mot de passe"), c);
            c.gridx = 1;
            p.add(passwordField, c);

            c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
            p.add(loginButton, c);

            c.gridy = 3;
            loginStatus.setForeground(new Color(160, 0, 0));
            p.add(loginStatus, c);

            JLabel hint = new JLabel("Connexion au backend via SMARTDRIVE_BACKEND_HOST/PORT ou resources/config.json");
            hint.setForeground(new Color(90, 90, 90));
            c.gridy = 4;
            p.add(hint, c);

            return p;
        }

        private JPanel buildMainPanel() {
            JPanel top = new JPanel(new BorderLayout());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            actions.add(refreshButton);
            actions.add(uploadButton);
            actions.add(downloadButton);
            actions.add(deleteButton);
            actions.add(logoutButton);

            JPanel quotaPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            quotaPanel.add(quotaLabel);

            top.add(actions, BorderLayout.WEST);
            top.add(quotaPanel, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout());
            center.add(new JScrollPane(filesTable), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.add(storageBar, BorderLayout.CENTER);
            bottom.add(statusLabel, BorderLayout.SOUTH);

            JPanel main = new JPanel(new BorderLayout());
            main.add(top, BorderLayout.NORTH);
            main.add(center, BorderLayout.CENTER);
            main.add(bottom, BorderLayout.SOUTH);
            return main;
        }

        private void wireActions() {
            loginButton.addActionListener(e -> doLogin());
            passwordField.addActionListener(e -> doLogin());

            refreshButton.addActionListener(e -> refresh());
            uploadButton.addActionListener(e -> upload());
            downloadButton.addActionListener(e -> download());
            deleteButton.addActionListener(e -> deleteToTrash());
            logoutButton.addActionListener(e -> logout());
        }

        private void setBusy(boolean busy, String status) {
            loginButton.setEnabled(!busy);
            refreshButton.setEnabled(!busy);
            uploadButton.setEnabled(!busy);
            downloadButton.setEnabled(!busy);
            deleteButton.setEnabled(!busy);
            logoutButton.setEnabled(!busy);
            statusLabel.setText(status == null ? " " : status);
        }

        private void doLogin() {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                loginStatus.setText("Veuillez saisir utilisateur + mot de passe");
                return;
            }

            loginStatus.setText(" ");
            setBusy(true, "Connexion...");

            SwingWorker<SmartDriveConnection, Void> worker = new SwingWorker<>() {
                @Override
                protected SmartDriveConnection doInBackground() throws Exception {
                    return SmartDriveConnection.connectAndLogin(username, password);
                }

                @Override
                protected void done() {
                    try {
                        conn = get();
                        cards.show(root, "main");
                        setBusy(false, "Connecté: " + conn.getWelcomeLine());
                        refresh();
                    } catch (Exception ex) {
                        setBusy(false, " ");
                        loginStatus.setText(ex.getMessage());
                        if (conn != null) {
                            conn.close();
                            conn = null;
                        }
                    }

* /
                }
            };
            worker.execute();
        }

                            new SmartDriveFrame().setVisible(true);
            if (conn == null) return;
            setBusy(true, "Récupération de la liste...");

            SwingWorker<RefreshResult, Void> worker = new SwingWorker<>() {
                        private final CardLayout rootCards = new CardLayout();
                        private final JPanel root = new JPanel(rootCards);

                        private final LoginView loginView;
                        private final AppView appView;

                        SmartDriveFrame() {
                            super("SmartDrive");
                            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                            setSize(1100, 720);
                            setLocationRelativeTo(null);

                            loginView = new LoginView(this);
                            appView = new AppView(this);

                            root.add(loginView, "login");
                            root.add(appView, "app");
                            setContentPane(root);
                            rootCards.show(root, "login");
                        }

                        void onLoggedIn(SmartDriveConnection conn, String username) {
                            appView.setSession(conn, username);
                            rootCards.show(root, "app");
                        }

                        void logout() {
                            appView.clearSession();
                            rootCards.show(root, "login");
                        }
                    }

                    static final class LoginView extends JPanel {
                        private final SmartDriveFrame frame;
                        private final JTextField usernameField = new JTextField(20);
                        private final JPasswordField passwordField = new JPasswordField(20);
                        private final BootstrapButton loginButton = new BootstrapButton("Connexion", BootstrapButton.Variant.PRIMARY, false);
                        private final JLabel errorLabel = new JLabel(" ");

                        LoginView(SmartDriveFrame frame) {
                            super(new GridBagLayout());
                            this.frame = frame;
                            setBackground(BootstrapColors.BG_LIGHT);

                            CardPanel card = new CardPanel();
                            card.setPreferredSize(new Dimension(520, 360));

                            JPanel inner = new JPanel();
                            inner.setOpaque(false);
                            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

                            JLabel title = new JLabel("SmartDrive", SwingConstants.CENTER);
                            title.setAlignmentX(Component.CENTER_ALIGNMENT);
                            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

                            JLabel subtitle = new JLabel("Connexion", SwingConstants.CENTER);
                            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
                            subtitle.setForeground(BootstrapColors.TEXT_MUTED);

                            inner.add(title);
                            inner.add(Box.createVerticalStrut(4));
                            inner.add(subtitle);
                            inner.add(Box.createVerticalStrut(18));

                            JPanel form = new JPanel(new GridBagLayout());
                            form.setOpaque(false);
                            GridBagConstraints c = new GridBagConstraints();
                            c.insets = new Insets(6, 6, 6, 6);
                            c.fill = GridBagConstraints.HORIZONTAL;
                            c.weightx = 1;

                            c.gridx = 0;
                            c.gridy = 0;
                            form.add(labelMuted("Nom"), c);
                            c.gridy = 1;
                            form.add(usernameField, c);

                            c.gridy = 2;
                            form.add(labelMuted("Mot de passe"), c);
                            c.gridy = 3;
                            form.add(passwordField, c);

                            c.gridy = 4;
                            form.add(loginButton, c);

                            c.gridy = 5;
                            errorLabel.setForeground(BootstrapColors.DANGER);
                            form.add(errorLabel, c);

                            inner.add(form);
                            card.add(inner, BorderLayout.CENTER);

                            add(card);

                            loginButton.addActionListener(e -> doLogin());
                            passwordField.addActionListener(e -> doLogin());
                        }

                        private static JLabel labelMuted(String text) {
                            JLabel l = new JLabel(text);
                            l.setForeground(BootstrapColors.TEXT_MUTED);
                            return l;
                        }

                        private void setBusy(boolean busy) {
                            loginButton.setEnabled(!busy);
                            usernameField.setEnabled(!busy);
                            passwordField.setEnabled(!busy);
                        }

                        private void doLogin() {
                            String username = usernameField.getText().trim();
                            String password = new String(passwordField.getPassword());
                            if (username.isEmpty() || password.isEmpty()) {
                                errorLabel.setText("Veuillez saisir utilisateur + mot de passe");
                                return;
                            }

                            errorLabel.setText(" ");
                            setBusy(true);

                            SwingWorker<SmartDriveConnection, Void> worker = new SwingWorker<>() {
                                @Override
                                protected SmartDriveConnection doInBackground() throws Exception {
                                    return SmartDriveConnection.connectAndLogin(username, password);
                                }

                                @Override
                                protected void done() {
                                    try {
                                        SmartDriveConnection conn = get();
                                        setBusy(false);
                                        frame.onLoggedIn(conn, username);
                                    } catch (Exception ex) {
                                        setBusy(false);
                                        errorLabel.setText(ex.getMessage());
                                    }
                                }
                            };
                            worker.execute();
                        }
                    }

                    enum PageId {
                        ACCUEIL,
                        MES_FICHIERS,
                        CORBEILLE,
                        PARTAGES,
                        STOCKAGE
                        quotaLabel.setText("Utilisé: " + usedStr + " | Restant: " + remainingStr + " | Total≈ " + totalStr);

                    static final class AppView extends JPanel {
                        private final SmartDriveFrame frame;
                        private final JPanel navbar = new JPanel(new BorderLayout());
                        private final JPanel sidebar = new JPanel();
                        private final CardLayout pages = new CardLayout();
                        private final JPanel content = new JPanel(pages);

                        private SmartDriveConnection conn;
                        private String username;

                        private final JLabel helloName = new JLabel("Bonjour, -");
                        private final JLabel helloEmail = new JLabel("-@example.com");

                        private final ButtonGroup navGroup = new ButtonGroup();
                        private final NavPillButton navAccueil = new NavPillButton("Accueil");
                        private final NavPillButton navMesFichiers = new NavPillButton("Mes fichiers");
                        private final NavPillButton navCorbeille = new NavPillButton("Corbeille");
                        private final NavPillButton navPartages = new NavPillButton("Partages");
                        private final NavPillButton navStockage = new NavPillButton("Stockage");
                        private final NavPillButton navParametres = new NavPillButton("Parametres");

                        private final BootstrapButton logoutButton = new BootstrapButton("Déconnexion", BootstrapButton.Variant.OUTLINE_DANGER, true);

                        private final AccueilPage accueilPage = new AccueilPage();
                        private final MesFichiersPage mesFichiersPage = new MesFichiersPage();
                        private final CorbeillePage corbeillePage = new CorbeillePage();
                        private final PartagesPage partagesPage = new PartagesPage();
                        private final StockagePage stockagePage = new StockagePage();

                        AppView(SmartDriveFrame frame) {
                            super(new BorderLayout());
                            this.frame = frame;
                            setBackground(BootstrapColors.BG_LIGHT);

                            buildNavbar();
                            buildSidebar();
                            buildContent();

                            add(navbar, BorderLayout.NORTH);
                            add(sidebar, BorderLayout.WEST);
                            add(content, BorderLayout.CENTER);

                            navAccueil.addActionListener(e -> navigate(PageId.ACCUEIL));
                            navMesFichiers.addActionListener(e -> navigate(PageId.MES_FICHIERS));
                            navCorbeille.addActionListener(e -> navigate(PageId.CORBEILLE));
                            navPartages.addActionListener(e -> navigate(PageId.PARTAGES));
                            navStockage.addActionListener(e -> navigate(PageId.STOCKAGE));
                            logoutButton.addActionListener(e -> frame.logout());

                            navParametres.setEnabled(false);
                            navAccueil.setSelected(true);
                            navigate(PageId.ACCUEIL);
                        }

                        void setSession(SmartDriveConnection conn, String username) {
                            this.conn = conn;
                            this.username = username;
                            helloName.setText("Bonjour, " + username);
                            helloEmail.setText(username + "@example.com");

                            accueilPage.setSession(conn, username);
                            mesFichiersPage.setSession(conn, username);
                            corbeillePage.setSession(conn, username);
                            partagesPage.setSession(conn, username);
                            stockagePage.setSession(conn, username);

                            SwingUtilities.invokeLater(() -> {
                                navAccueil.setSelected(true);
                                navigate(PageId.ACCUEIL);
                            });
                        }

                        void clearSession() {
                            if (conn != null) {
                                conn.close();
                            }
                            conn = null;
                            username = null;
                        }

                        private void buildNavbar() {
                            navbar.setBackground(BootstrapColors.WHITE);
                            navbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BootstrapColors.BORDER));

                            JLabel brand = new JLabel("SmartDrive");
                            brand.setFont(brand.getFont().deriveFont(Font.BOLD, 14f));
                            brand.setForeground(BootstrapColors.NAV_FG);

                            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
                            left.setOpaque(false);
                            left.add(brand);

                            navbar.add(left, BorderLayout.WEST);
                        }

                        private void buildSidebar() {
                            sidebar.setLayout(new BorderLayout());
                            sidebar.setPreferredSize(new Dimension(240, 1));
                            sidebar.setBackground(BootstrapColors.BG_BODY_TERTIARY);
                            sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BootstrapColors.BORDER));

                            JPanel container = new JPanel();
                            container.setOpaque(false);
                            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
                            container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

                            CardPanel helloCard = new CardPanel(12);
                            JPanel helloInner = new JPanel();
                            helloInner.setOpaque(false);
                            helloInner.setLayout(new BoxLayout(helloInner, BoxLayout.Y_AXIS));
                            helloName.setFont(helloName.getFont().deriveFont(Font.BOLD, 13f));
                            helloEmail.setForeground(BootstrapColors.TEXT_MUTED);
                            helloInner.add(helloName);
                            helloInner.add(Box.createVerticalStrut(2));
                            helloInner.add(helloEmail);
                            helloCard.add(helloInner, BorderLayout.CENTER);

                            JPanel nav = new JPanel();
                            nav.setOpaque(false);
                            nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

                            navGroup.add(navAccueil);
                            navGroup.add(navMesFichiers);
                            navGroup.add(navCorbeille);
                            navGroup.add(navPartages);
                            navGroup.add(navStockage);
                            navGroup.add(navParametres);

                            nav.add(navAccueil);
                            nav.add(Box.createVerticalStrut(4));
                            nav.add(navMesFichiers);
                            nav.add(Box.createVerticalStrut(4));
                            nav.add(navCorbeille);
                            nav.add(Box.createVerticalStrut(4));
                            nav.add(navPartages);
                            nav.add(Box.createVerticalStrut(4));
                            nav.add(navStockage);
                            nav.add(Box.createVerticalStrut(4));
                            nav.add(navParametres);

                            JPanel bottom = new JPanel();
                            bottom.setOpaque(false);
                            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
                            bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BootstrapColors.BORDER));
                            bottom.add(Box.createVerticalStrut(12));
                            logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                            bottom.add(logoutButton);

                            container.add(helloCard);
                            container.add(Box.createVerticalStrut(12));
                            container.add(nav);
                            container.add(Box.createVerticalGlue());
                            container.add(bottom);

                            sidebar.add(container, BorderLayout.CENTER);
                        }

                        private void buildContent() {
                            content.setBackground(BootstrapColors.BG_LIGHT);
                            content.add(accueilPage, PageId.ACCUEIL.name());
                            content.add(mesFichiersPage, PageId.MES_FICHIERS.name());
                            content.add(corbeillePage, PageId.CORBEILLE.name());
                            content.add(partagesPage, PageId.PARTAGES.name());
                            content.add(stockagePage, PageId.STOCKAGE.name());
                        }

                        private void navigate(PageId id) {
                            pages.show(content, id.name());
                            switch (id) {
                                case ACCUEIL -> accueilPage.onShow();
                                case MES_FICHIERS -> mesFichiersPage.onShow();
                                case CORBEILLE -> corbeillePage.onShow();
                                case PARTAGES -> partagesPage.onShow();
                                case STOCKAGE -> stockagePage.onShow();
                            }
                        }
                    }

                    static abstract class BasePage extends JPanel {
                        SmartDriveConnection conn;
                        String username;

                        BasePage() {
                            super(new BorderLayout());
                            setBackground(BootstrapColors.BG_LIGHT);
                        }

                        void setSession(SmartDriveConnection conn, String username) {
                            this.conn = conn;
                            this.username = username;
                        }

                        abstract void onShow();
                    }

                    static final class AccueilPage extends BasePage {
                        private final JPanel main = new JPanel(new BorderLayout());
                        private final JPanel notificationsPanel = new JPanel();
                        private final JLabel notificationsTitle = new JLabel("Notifications");
                        private final JTextArea notificationsText = new JTextArea();

                        private final JPanel usersWrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
                        private final JScrollPane scroll;

                        AccueilPage() {
                            main.setOpaque(false);
                            main.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                            JPanel top = new JPanel();
                            top.setOpaque(false);
                            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

                            notificationsPanel.setLayout(new BorderLayout());
                            notificationsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                            notificationsPanel.setBackground(new Color(0xCFE2FF));

                            notificationsTitle.setFont(notificationsTitle.getFont().deriveFont(Font.BOLD, 13f));
                            notificationsText.setEditable(false);
                            notificationsText.setLineWrap(true);
                            notificationsText.setWrapStyleWord(true);
                            notificationsText.setOpaque(false);
                            notificationsText.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

                            notificationsPanel.add(notificationsTitle, BorderLayout.NORTH);
                            notificationsPanel.add(notificationsText, BorderLayout.CENTER);

                            JPanel header = new JPanel(new BorderLayout());
                            header.setOpaque(false);
                            JLabel h = new JLabel("Utilisateurs");
                            h.setForeground(BootstrapColors.PRIMARY);
                            h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
                            header.add(h, BorderLayout.WEST);

                            BootstrapButton upload = new BootstrapButton("Upload", BootstrapButton.Variant.PRIMARY, true);
                            upload.addActionListener(e -> firePropertyChange("upload", false, true));
                            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                            actions.setOpaque(false);
                            actions.add(upload);
                            header.add(actions, BorderLayout.EAST);

                            top.add(notificationsPanel);
                            top.add(Box.createVerticalStrut(12));
                            top.add(header);

                            usersWrap.setOpaque(false);
                            scroll = new JScrollPane(usersWrap);
                            scroll.setBorder(BorderFactory.createEmptyBorder());
                            scroll.getViewport().setBackground(BootstrapColors.BG_LIGHT);

                            main.add(top, BorderLayout.NORTH);
                            main.add(scroll, BorderLayout.CENTER);
                            add(main, BorderLayout.CENTER);

                            addPropertyChangeListener("upload", evt -> {
                                if (conn != null) {
                                    MesFichiersPage.doUpload(this, conn);
                                }
                            });
                        }

                        @Override
                        void onShow() {
                            if (conn == null) return;
                            refresh();
                        }

                        private void refresh() {
                            SwingWorker<AccueilData, Void> worker = new SwingWorker<>() {
                                @Override
                                protected AccueilData doInBackground() throws Exception {
                                    List<String> users = conn.listUsers();
                                    List<String> notifs = conn.listNotifications();
                                    return new AccueilData(users, notifs);
                                }

                                @Override
                                protected void done() {
                                    try {
                                        AccueilData data = get();
                                        renderNotifs(data.notifications);
                                        renderUsers(data.users);
                                    } catch (Exception ex) {
                                        renderNotifs(List.of("Erreur: " + ex.getMessage()));
                                        renderUsers(List.of());
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private void renderNotifs(List<String> rows) {
                            if (rows == null || rows.isEmpty()) {
                                notificationsPanel.setVisible(false);
                                return;
                            }
                            notificationsPanel.setVisible(true);
                            StringBuilder sb = new StringBuilder();
                            for (String n : rows) {
                                if (n == null) continue;
                                String[] p = n.split(";", 3);
                                String msg = (p.length >= 3) ? p[2] : n;
                                sb.append("- ").append(msg).append("\n");
                            }
                            notificationsText.setText(sb.toString().trim());
                        }

                        private void renderUsers(List<String> users) {
                            usersWrap.removeAll();
                            if (users == null) users = List.of();
                            for (String u : users) {
                                if (u == null || u.isBlank()) continue;
                                CardPanel card = new CardPanel();
                                card.setPreferredSize(new Dimension(260, 110));
                                JPanel body = new JPanel();
                                body.setOpaque(false);
                                body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                                JLabel name = new JLabel(u);
                                name.setForeground(BootstrapColors.PRIMARY);
                                name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
                                body.add(name);
                                body.add(Box.createVerticalGlue());
                                BootstrapButton voir = new BootstrapButton("Voir", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
                                voir.addActionListener(e -> {
                                    if (conn == null) return;
                                    JOptionPane.showMessageDialog(this,
                                            "Voir les fichiers de " + u + " : allez dans Partages.",
                                            "Info",
                                            JOptionPane.INFORMATION_MESSAGE);
                                });
                                JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                actions.setOpaque(false);
                                actions.add(voir);
                                body.add(actions);
                                card.add(body, BorderLayout.CENTER);
                                usersWrap.add(card);
                            }
                            usersWrap.revalidate();
                            usersWrap.repaint();
                        }

                        static final class AccueilData {
                            final List<String> users;
                            final List<String> notifications;

                            AccueilData(List<String> users, List<String> notifications) {
                                this.users = users;
                                this.notifications = notifications;
                            }
                        }
                    }

                    static final class MesFichiersPage extends BasePage {
                        private final JPanel root = new JPanel(new BorderLayout());
                        private final JLabel message = new JLabel(" ");
                        private final JTextField q = new JTextField();
                        private final JComboBox<String> type = new JComboBox<>(new String[]{"Tous", "PDF", "Images", "Vidéos", "Audio", "Documents", "Tableurs", "Texte", "Archives"});
                        private final JTextField min = new JTextField();
                        private final JTextField max = new JTextField();
                        private final BootstrapButton search = new BootstrapButton(" ", BootstrapButton.Variant.OUTLINE_PRIMARY, false);
                        private final BootstrapButton upload = new BootstrapButton("Upload", BootstrapButton.Variant.PRIMARY, true);

                        private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
                        private final JScrollPane scroll;

                        private List<SmartDriveConnection.RemoteFileRow> last = new ArrayList<>();

                        MesFichiersPage() {
                            root.setOpaque(false);
                            root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                            JPanel header = new JPanel(new BorderLayout());
                            header.setOpaque(false);
                            JLabel h = new JLabel("Mes fichiers");
                            h.setForeground(BootstrapColors.PRIMARY);
                            h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
                            header.add(h, BorderLayout.WEST);

                            JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                            headerActions.setOpaque(false);
                            headerActions.add(upload);
                            header.add(headerActions, BorderLayout.EAST);

                            message.setOpaque(true);
                            message.setBackground(new Color(0xCFE2FF));
                            message.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                            message.setVisible(false);

                            JPanel searchForm = buildSearchForm();

                            JPanel top = new JPanel();
                            top.setOpaque(false);
                            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                            top.add(header);
                            top.add(Box.createVerticalStrut(10));
                            top.add(message);
                            top.add(Box.createVerticalStrut(10));
                            top.add(searchForm);

                            grid.setOpaque(false);
                            scroll = new JScrollPane(grid);
                            scroll.setBorder(BorderFactory.createEmptyBorder());
                            scroll.getViewport().setBackground(BootstrapColors.BG_LIGHT);

                            root.add(top, BorderLayout.NORTH);
                            root.add(scroll, BorderLayout.CENTER);
                            add(root, BorderLayout.CENTER);

                            upload.addActionListener(e -> {
                                if (conn == null) return;
                                doUpload(this, conn);
                                onShow();
                            });
                            search.addActionListener(e -> render());
                        }

                        static void doUpload(Component parent, SmartDriveConnection conn) {
                            JFileChooser chooser = new JFileChooser();
                            chooser.setDialogTitle("Choisir un fichier à uploader");
                            int res = chooser.showOpenDialog(parent);
                            if (res != JFileChooser.APPROVE_OPTION) return;
                            File file = chooser.getSelectedFile();
                            if (file == null) return;

                            JDialog dlg = progressDialog(parent, "Upload", "Envoi du fichier...");
                            SwingWorker<String, Void> worker = new SwingWorker<>() {
                                @Override
                                protected String doInBackground() throws Exception {
                                    return conn.upload(file);
                                }

                                @Override
                                protected void done() {
                                    dlg.dispose();
                                    try {
                                        String resp = get();
                                        JOptionPane.showMessageDialog(parent, resp, "Upload", JOptionPane.INFORMATION_MESSAGE);
                                    } catch (Exception ex) {
                                        JOptionPane.showMessageDialog(parent, ex.getMessage(), "Upload", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            };
                            worker.execute();
                            dlg.setVisible(true);
                        }

                        private static JDialog progressDialog(Component parent, String title, String text) {
                            Window w = SwingUtilities.getWindowAncestor(parent);
                            JDialog dlg = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
                            dlg.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                            JPanel p = new JPanel(new BorderLayout(10, 10));
                            p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
                            p.add(new JLabel(text), BorderLayout.NORTH);
                            JProgressBar bar = new JProgressBar();
                            bar.setIndeterminate(true);
                            p.add(bar, BorderLayout.CENTER);
                            dlg.setContentPane(p);
                            dlg.pack();
                            dlg.setLocationRelativeTo(parent);
                            return dlg;
                        }

                        private JPanel buildSearchForm() {
                            JPanel form = new JPanel(new GridBagLayout());
                            form.setOpaque(false);
                            GridBagConstraints c = new GridBagConstraints();
                            c.insets = new Insets(6, 6, 6, 6);
                            c.fill = GridBagConstraints.HORIZONTAL;

                            c.gridx = 0;
                            c.gridy = 0;
                            c.weightx = 1;
                            form.add(mutedLabel("Nom"), c);
                            c.gridy = 1;
                            form.add(q, c);

                            c.gridx = 1;
                            c.gridy = 0;
                            c.weightx = 0.5;
                            form.add(mutedLabel("Type"), c);
                            c.gridy = 1;
                            form.add(type, c);

                            c.gridx = 2;
                            c.gridy = 0;
                            c.weightx = 0.35;
                            form.add(mutedLabel("Taille min (octets)"), c);
                            c.gridy = 1;
                            form.add(min, c);

                            c.gridx = 3;
                            c.gridy = 0;
                            form.add(mutedLabel("Taille max (octets)"), c);
                            c.gridy = 1;
                            form.add(max, c);

                            c.gridx = 4;
                            c.gridy = 1;
                            c.weightx = 0;
                            search.setText("Rechercher");
                            form.add(search, c);

                            return form;
                        }

                        private static JLabel mutedLabel(String t) {
                            JLabel l = new JLabel(t);
                            l.setForeground(BootstrapColors.TEXT_MUTED);
                            return l;
                        }

                        @Override
                        void onShow() {
                            if (conn == null) return;
                            message.setVisible(false);
                            SwingWorker<List<SmartDriveConnection.RemoteFileRow>, Void> worker = new SwingWorker<>() {
                                @Override
                                protected List<SmartDriveConnection.RemoteFileRow> doInBackground() throws Exception {
                                    List<SmartDriveConnection.RemoteFileRow> rows = conn.listFiles();
                                    rows.sort(Comparator.comparing(a -> a.name.toLowerCase()));
                                    return rows;
                                }

                                @Override
                                protected void done() {
                                    try {
                                        last = get();
                                        render();
                                    } catch (Exception ex) {
                                        last = new ArrayList<>();
                                        showMessage(ex.getMessage(), true);
                                        render();
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private void showMessage(String text, boolean error) {
                            message.setText(text);
                            message.setBackground(error ? new Color(0xF8D7DA) : new Color(0xCFE2FF));
                            message.setVisible(true);
                        }

                        private void render() {
                            grid.removeAll();
                            List<SmartDriveConnection.RemoteFileRow> filtered = applyFilters(last);

                            if (filtered.isEmpty()) {
                                JPanel empty = new JPanel();
                                empty.setOpaque(false);
                                empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
                                JLabel icon = new JLabel("Aucun fichier trouvé");
                                icon.setForeground(BootstrapColors.TEXT_MUTED);
                                icon.setFont(icon.getFont().deriveFont(Font.BOLD, 16f));
                                icon.setAlignmentX(Component.CENTER_ALIGNMENT);
                                JLabel hint = new JLabel("Commencez par uploader un fichier");
                                hint.setForeground(BootstrapColors.TEXT_MUTED);
                                hint.setAlignmentX(Component.CENTER_ALIGNMENT);
                                empty.add(Box.createVerticalStrut(40));
                                empty.add(icon);
                                empty.add(Box.createVerticalStrut(6));
                                empty.add(hint);
                                grid.add(empty);
                            } else {
                                for (SmartDriveConnection.RemoteFileRow r : filtered) {
                                    grid.add(fileCard(r));
                                }
                            }

                            grid.revalidate();
                            grid.repaint();
                        }

                        private List<SmartDriveConnection.RemoteFileRow> applyFilters(List<SmartDriveConnection.RemoteFileRow> rows) {
                            if (rows == null) return List.of();
                            String qs = q.getText() == null ? "" : q.getText().trim().toLowerCase();
                            String t = (String) type.getSelectedItem();
                            long minV = parseLong(min.getText());
                            long maxV = parseLong(max.getText());

                            List<SmartDriveConnection.RemoteFileRow> out = new ArrayList<>();
                            for (SmartDriveConnection.RemoteFileRow r : rows) {
                                if (r == null) continue;
                                if (!qs.isEmpty() && !r.name.toLowerCase().contains(qs)) continue;
                                if (minV >= 0 && r.size < minV) continue;
                                if (maxV >= 0 && r.size > maxV) continue;
                                if (t != null && !"Tous".equalsIgnoreCase(t) && !matchesType(r.name, t)) continue;
                                out.add(r);
                            }
                            return out;
                        }

                        private static long parseLong(String s) {
                            if (s == null) return -1;
                            String t = s.trim();
                            if (t.isEmpty()) return -1;
                            try {
                                return Long.parseLong(t);
                            } catch (Exception e) {
                                return -1;
                            }
                        }

                        private static boolean matchesType(String filename, String type) {
                            String f = filename.toLowerCase();
                            return switch (type) {
                                case "PDF" -> f.endsWith(".pdf");
                                case "Images" -> f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".gif");
                                case "Vidéos" -> f.endsWith(".mp4") || f.endsWith(".avi") || f.endsWith(".mkv");
                                case "Audio" -> f.endsWith(".mp3") || f.endsWith(".wav");
                                case "Documents" -> f.endsWith(".doc") || f.endsWith(".docx");
                                case "Tableurs" -> f.endsWith(".xls") || f.endsWith(".xlsx");
                                case "Texte" -> f.endsWith(".txt") || f.endsWith(".md") || f.endsWith(".log") || f.endsWith(".csv") || f.endsWith(".json") || f.endsWith(".xml") || f.endsWith(".yml") || f.endsWith(".yaml");
                                case "Archives" -> f.endsWith(".zip") || f.endsWith(".rar");
                                default -> true;
                            };
                        }

                        private JComponent fileCard(SmartDriveConnection.RemoteFileRow r) {
                            CardPanel card = new CardPanel();
                            card.setPreferredSize(new Dimension(260, 170));

                            JPanel body = new JPanel();
                            body.setOpaque(false);
                            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

                            JLabel icon = new JLabel(fileIconText(r.name), SwingConstants.CENTER);
                            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
                            icon.setFont(icon.getFont().deriveFont(Font.BOLD, 26f));
                            icon.setForeground(fileIconColor(r.name));

                            JLabel name = new JLabel(r.name);
                            name.setAlignmentX(Component.CENTER_ALIGNMENT);
                            name.setMaximumSize(new Dimension(230, 22));
                            name.setHorizontalAlignment(SwingConstants.CENTER);

                            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
                            actions.setOpaque(false);
                            BootstrapButton voir = new BootstrapButton("Voir", BootstrapButton.Variant.PRIMARY, true);
                            BootstrapButton versions = new BootstrapButton("Versions", BootstrapButton.Variant.OUTLINE_SECONDARY, true);
                            BootstrapButton download = new BootstrapButton(" ", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
                            download.setText("Download");
                            BootstrapButton del = new BootstrapButton(" ", BootstrapButton.Variant.OUTLINE_DANGER, true);
                            del.setText("Supprimer");

                            voir.addActionListener(e -> viewFile(r.name));
                            download.addActionListener(e -> downloadFile(r.name));
                            del.addActionListener(e -> deleteFile(r.name));
                            versions.addActionListener(e -> showVersions(r.name));

                            actions.add(voir);
                            actions.add(versions);
                            actions.add(download);
                            actions.add(del);

                            body.add(icon);
                            body.add(Box.createVerticalStrut(10));
                            body.add(name);
                            body.add(Box.createVerticalGlue());
                            body.add(actions);
                            card.add(body, BorderLayout.CENTER);
                            return card;
                        }

                        private static String fileIconText(String name) {
                            String f = name.toLowerCase();
                            if (f.endsWith(".pdf")) return "PDF";
                            if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".gif")) return "IMG";
                            if (f.endsWith(".doc") || f.endsWith(".docx")) return "DOC";
                            if (f.endsWith(".xls") || f.endsWith(".xlsx")) return "XLS";
                            if (f.endsWith(".txt") || f.endsWith(".md") || f.endsWith(".log") || f.endsWith(".csv") || f.endsWith(".json") || f.endsWith(".xml") || f.endsWith(".yml") || f.endsWith(".yaml")) return "TXT";
                            if (f.endsWith(".zip") || f.endsWith(".rar")) return "ZIP";
                            if (f.endsWith(".mp3") || f.endsWith(".wav")) return "AUD";
                            if (f.endsWith(".mp4") || f.endsWith(".avi") || f.endsWith(".mkv")) return "VID";
                            return "FILE";
                        }

                        private static Color fileIconColor(String name) {
                            String f = name.toLowerCase();
                            if (f.endsWith(".pdf")) return BootstrapColors.DANGER;
                            if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".gif")) return BootstrapColors.SUCCESS;
                            if (f.endsWith(".doc") || f.endsWith(".docx")) return BootstrapColors.PRIMARY;
                            if (f.endsWith(".xls") || f.endsWith(".xlsx")) return BootstrapColors.SUCCESS;
                            if (f.endsWith(".txt") || f.endsWith(".md") || f.endsWith(".log") || f.endsWith(".csv") || f.endsWith(".json") || f.endsWith(".xml") || f.endsWith(".yml") || f.endsWith(".yaml")) return BootstrapColors.INFO;
                            if (f.endsWith(".zip") || f.endsWith(".rar")) return BootstrapColors.WARNING;
                            if (f.endsWith(".mp3") || f.endsWith(".wav")) return BootstrapColors.INFO;
                            if (f.endsWith(".mp4") || f.endsWith(".avi") || f.endsWith(".mkv")) return BootstrapColors.DANGER;
                            return BootstrapColors.SECONDARY;
                        }

                        private void viewFile(String filename) {
                            if (conn == null) return;
                            SwingWorker<File, Void> worker = new SwingWorker<>() {
                                @Override
                                protected File doInBackground() throws Exception {
                                    File tmp = File.createTempFile("smartdrive_view_", "_" + filename.replaceAll("[^a-zA-Z0-9._-]", "_"));
                                    tmp.deleteOnExit();
                                    conn.download(filename, tmp);
                                    return tmp;
                                }

                                @Override
                                protected void done() {
                                    try {
                                        File f = get();
                                        if (looksLikeText(filename)) {
                                            showTextDialog(filename, f);
                                        } else {
                                            if (Desktop.isDesktopSupported()) {
                                                Desktop.getDesktop().open(f);
                                            } else {
                                                JOptionPane.showMessageDialog(MesFichiersPage.this, f.getAbsolutePath(), "Fichier", JOptionPane.INFORMATION_MESSAGE);
                                            }
                                        }
                                    } catch (Exception ex) {
                                        JOptionPane.showMessageDialog(MesFichiersPage.this, ex.getMessage(), "Voir", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private static boolean looksLikeText(String filename) {
                            String f = filename.toLowerCase();
                            return f.endsWith(".txt") || f.endsWith(".md") || f.endsWith(".log") || f.endsWith(".csv")
                                    || f.endsWith(".json") || f.endsWith(".xml") || f.endsWith(".yml") || f.endsWith(".yaml")
                                    || f.endsWith(".java") || f.endsWith(".js") || f.endsWith(".css") || f.endsWith(".html");
                        }

                        private void showTextDialog(String title, File file) throws Exception {
                            StringBuilder sb = new StringBuilder();
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = br.readLine()) != null) {
                                    sb.append(line).append("\n");
                                }
                            }
                            JTextArea area = new JTextArea(sb.toString());
                            area.setEditable(false);
                            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                            JScrollPane sp = new JScrollPane(area);
                            sp.setPreferredSize(new Dimension(860, 520));

                            Object[] options = {"Copier", "Fermer"};
                            int r = JOptionPane.showOptionDialog(this, sp, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                                    null, options, options[1]);
                            if (r == 0) {
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(area.getText()), null);
                            }
                        }

                        private void downloadFile(String filename) {
                            if (conn == null) return;
                            JFileChooser chooser = new JFileChooser();
                            chooser.setDialogTitle("Enregistrer sous...");
                            chooser.setSelectedFile(new File(filename));
                            int res = chooser.showSaveDialog(this);
                            if (res != JFileChooser.APPROVE_OPTION) return;
                            File dest = chooser.getSelectedFile();
                            if (dest == null) return;

                            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                                @Override
                                protected Void doInBackground() throws Exception {
                                    conn.download(filename, dest);
                                    return null;
                                }

                                @Override
                                protected void done() {
                                    try {
                                        get();
                                        JOptionPane.showMessageDialog(MesFichiersPage.this, "Téléchargé: " + dest.getAbsolutePath(), "Download", JOptionPane.INFORMATION_MESSAGE);
                                    } catch (Exception ex) {
                                        JOptionPane.showMessageDialog(MesFichiersPage.this, ex.getMessage(), "Download", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private void deleteFile(String filename) {
                            if (conn == null) return;
                            int ok = JOptionPane.showConfirmDialog(this,
                                    "Êtes-vous sûr de vouloir supprimer le fichier " + filename + " ?\n\nLe fichier sera déplacé dans la corbeille (restauration possible).",
                                    "Confirmer la suppression",
                                    JOptionPane.OK_CANCEL_OPTION);
                            if (ok != JOptionPane.OK_OPTION) return;

                            SwingWorker<String, Void> worker = new SwingWorker<>() {
                                @Override
                                protected String doInBackground() throws Exception {
                                    return conn.deleteToTrash(filename);
                                }

                                @Override
                                protected void done() {
                                    try {
                                        String resp = get();
                                        showMessage(resp, resp.startsWith("ERROR"));
                                        onShow();
                                    } catch (Exception ex) {
                                        showMessage(ex.getMessage(), true);
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private void showVersions(String filename) {
                            if (conn == null) return;
                            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Versions", Dialog.ModalityType.APPLICATION_MODAL);
                            dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                            dlg.setSize(860, 520);
                            dlg.setLocationRelativeTo(this);

                            JPanel root = new JPanel(new BorderLayout());
                            root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
                            root.setBackground(BootstrapColors.BG_LIGHT);

                            JLabel title = new JLabel("Historique: " + filename);
                            title.setForeground(BootstrapColors.PRIMARY);
                            title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
                            root.add(title, BorderLayout.NORTH);

                            VersionsTableModel model = new VersionsTableModel();
                            JTable table = new JTable(model);
                            table.setFillsViewportHeight(true);
                            table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());

                            JScrollPane sp = new JScrollPane(table);
                            root.add(sp, BorderLayout.CENTER);

                            BootstrapButton restore = new BootstrapButton("Restaurer", BootstrapButton.Variant.SUCCESS, true);
                            BootstrapButton close = new BootstrapButton("Fermer", BootstrapButton.Variant.OUTLINE_SECONDARY, true);
                            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                            bottom.setOpaque(false);
                            bottom.add(restore);
                            bottom.add(close);
                            root.add(bottom, BorderLayout.SOUTH);

                            close.addActionListener(e -> dlg.dispose());
                            restore.addActionListener(e -> {
                                int row = table.getSelectedRow();
                                if (row < 0) return;
                                String id = model.getVersionId(row);
                                int ok = JOptionPane.showConfirmDialog(dlg, "Restaurer cette version ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                                if (ok != JOptionPane.OK_OPTION) return;
                                SwingWorker<String, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected String doInBackground() throws Exception {
                                        return conn.restoreVersion(filename, id);
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            String resp = get();
                                            JOptionPane.showMessageDialog(dlg, resp, "Versions", JOptionPane.INFORMATION_MESSAGE);
                                            onShow();
                                            dlg.dispose();
                                        } catch (Exception ex) {
                                            JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Versions", JOptionPane.ERROR_MESSAGE);
                                        }
                                    }
                                };
                                worker.execute();
                            });

                            SwingWorker<List<String>, Void> loader = new SwingWorker<>() {
                                @Override
                                protected List<String> doInBackground() throws Exception {
                                    return conn.listVersions(filename);
                                }

                                @Override
                                protected void done() {
                                    try {
                                        model.setRows(get());
                                    } catch (Exception ex) {
                                        model.setRows(List.of());
                                    }
                                }
                            };
                            loader.execute();

                            dlg.setContentPane(root);
                            dlg.setVisible(true);
                        }

                        static final class VersionsTableModel extends javax.swing.table.AbstractTableModel {
                            private final String[] cols = {"Version", "Taille", "Date"};
                            private final List<String[]> rows = new ArrayList<>();

                            void setRows(List<String> lines) {
                                rows.clear();
                                if (lines != null) {
                                    for (String l : lines) {
                                        if (l == null) continue;
                                        String[] p = l.split(";", 3);
                                        if (p.length >= 3) rows.add(p);
                                    }
                                }
                                fireTableDataChanged();
                            }

                            String getVersionId(int row) {
                                return rows.get(row)[0];
                            }

                            @Override public int getRowCount() { return rows.size(); }
                            @Override public int getColumnCount() { return cols.length; }
                            @Override public String getColumnName(int column) { return cols[column]; }

                            @Override
                            public Object getValueAt(int rowIndex, int columnIndex) {
                                String[] r = rows.get(rowIndex);
                                return switch (columnIndex) {
                                    case 0 -> r[0];
                                    case 1 -> r[1] + " o";
                                    case 2 -> r[2];
                                    default -> "";
                                };
                            }
                        }
                    }

                    static final class CorbeillePage extends BasePage {
                        private final JPanel root = new JPanel(new BorderLayout());
                        private final JLabel info = new JLabel(" ");
                        private final BootstrapButton vider = new BootstrapButton("Vider", BootstrapButton.Variant.OUTLINE_DANGER, true);

                        private final TrashTableModel model = new TrashTableModel();
                        private final JTable table = new JTable(model);

                        CorbeillePage() {
                            root.setOpaque(false);
                            root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                            JPanel header = new JPanel(new BorderLayout());
                            header.setOpaque(false);
                            JLabel h = new JLabel("Fichiers supprimés");
                            h.setForeground(BootstrapColors.PRIMARY);
                            h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
                            header.add(h, BorderLayout.WEST);
                            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                            actions.setOpaque(false);
                            actions.add(vider);
                            header.add(actions, BorderLayout.EAST);

                            info.setOpaque(true);
                            info.setBackground(new Color(0xCFE2FF));
                            info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                            info.setVisible(false);

                            CardPanel card = new CardPanel();
                            card.setLayout(new BorderLayout());
                            table.setFillsViewportHeight(true);
                            table.setRowHeight(28);
                            table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
                            JScrollPane sp = new JScrollPane(table);
                            card.add(sp, BorderLayout.CENTER);

                            JPanel top = new JPanel();
                            top.setOpaque(false);
                            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                            top.add(info);
                            top.add(Box.createVerticalStrut(10));
                            top.add(header);

                            root.add(top, BorderLayout.NORTH);
                            root.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
                            root.add(card, BorderLayout.SOUTH);

                            add(root, BorderLayout.CENTER);

                            vider.addActionListener(e -> purgeAll());
                        }

                        @Override
                        void onShow() {
                            if (conn == null) return;
                            refresh();
                        }

                        private void showInfo(String text, boolean error) {
                            info.setText(text);
                            info.setBackground(error ? new Color(0xF8D7DA) : new Color(0xCFE2FF));
                            info.setVisible(true);
                        }

                        private void refresh() {
                            info.setVisible(false);
                            SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                                @Override
                                protected List<String> doInBackground() throws Exception {
                                    return conn.listTrash();
                                }

                                @Override
                                protected void done() {
                                    try {
                                        model.setRows(get());
                                        installActions();
                                    } catch (Exception ex) {
                                        model.setRows(List.of());
                                        showInfo(ex.getMessage(), true);
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private void installActions() {
                            if (table.getColumnCount() < 6) return;
                            table.getColumnModel().getColumn(4).setPreferredWidth(110);
                            table.getColumnModel().getColumn(5).setPreferredWidth(110);

                            Action restoreAction = new AbstractAction() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                    int row = Integer.parseInt(e.getActionCommand());
                                    TrashRow tr = model.getRow(row);
                                    if (tr == null) return;
                                    SwingWorker<String, Void> worker = new SwingWorker<>() {
                                        @Override
                                        protected String doInBackground() throws Exception {
                                            return conn.trashRestore(tr.id);
                                        }

                                        @Override
                                        protected void done() {
                                            try {
                                                String resp = get();
                                                showInfo(resp, resp.startsWith("ERROR"));
                                                refresh();
                                            } catch (Exception ex) {
                                                showInfo(ex.getMessage(), true);
                                            }
                                        }
                                    };
                                    worker.execute();
                                }
                            };

                            Action purgeAction = new AbstractAction() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                    int row = Integer.parseInt(e.getActionCommand());
                                    TrashRow tr = model.getRow(row);
                                    if (tr == null) return;
                                    int ok = JOptionPane.showConfirmDialog(CorbeillePage.this, "Supprimer définitivement ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                                    if (ok != JOptionPane.OK_OPTION) return;
                                    SwingWorker<String, Void> worker = new SwingWorker<>() {
                                        @Override
                                        protected String doInBackground() throws Exception {
                                            return conn.trashPurge(tr.id);
                                        }

                                        @Override
                                        protected void done() {
                                            try {
                                                String resp = get();
                                                showInfo(resp, resp.startsWith("ERROR"));
                                                refresh();
                                            } catch (Exception ex) {
                                                showInfo(ex.getMessage(), true);
                                            }
                                        }
                                    };
                                    worker.execute();
                                }
                            };

                            new TableButtonColumn(table, restoreAction, 4);
                            new TableButtonColumn(table, purgeAction, 5);
                        }

                        private void purgeAll() {
                            if (conn == null) return;
                            int ok = JOptionPane.showConfirmDialog(this, "Vider la corbeille définitivement ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                            if (ok != JOptionPane.OK_OPTION) return;
                            SwingWorker<String, Void> worker = new SwingWorker<>() {
                                @Override
                                protected String doInBackground() throws Exception {
                                    return conn.trashPurge("ALL");
                                }

                                @Override
                                protected void done() {
                                    try {
                                        String resp = get();
                                        showInfo(resp, resp.startsWith("ERROR"));
                                        refresh();
                                    } catch (Exception ex) {
                                        showInfo(ex.getMessage(), true);
                                    }
                                }
                            };
                            worker.execute();
                        }

                        static final class TrashRow {
                            final String id;
                            final String original;
                            final String size;
                            final String deletedAt;

                            TrashRow(String id, String original, String size, String deletedAt) {
                                this.id = id;
                                this.original = original;
                                this.size = size;
                                this.deletedAt = deletedAt;
                            }
                        }

                        static final class TrashTableModel extends javax.swing.table.AbstractTableModel {
                            private final String[] cols = {"Fichier", "Taille", "Supprimé le", " ", "Restaurer", "Supprimer"};
                            private final List<TrashRow> rows = new ArrayList<>();

                            void setRows(List<String> lines) {
                                rows.clear();
                                if (lines != null) {
                                    for (String l : lines) {
                                        if (l == null) continue;
                                        String[] p = l.split(";", 4);
                                        if (p.length < 4) continue;
                                        rows.add(new TrashRow(p[0], p[1], p[2], p[3]));
                                    }
                                }
                                fireTableDataChanged();
                            }

                            TrashRow getRow(int idx) {
                                if (idx < 0 || idx >= rows.size()) return null;
                                return rows.get(idx);
                            }

                            @Override public int getRowCount() { return rows.size(); }
                            @Override public int getColumnCount() { return cols.length; }
                            @Override public String getColumnName(int column) { return cols[column]; }

                            @Override
                            public Object getValueAt(int rowIndex, int columnIndex) {
                                TrashRow r = rows.get(rowIndex);
                                return switch (columnIndex) {
                                    case 0 -> r.original;
                                    case 1 -> r.size + " o";
                                    case 2 -> r.deletedAt;
                                    case 4 -> "Restaurer";
                                    case 5 -> "Supprimer";
                                    default -> "";
                                };
                            }

                            @Override
                            public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return columnIndex == 4 || columnIndex == 5;
                            }
                        }
                    }

                    static final class PartagesPage extends BasePage {
                        private final CardLayout localCards = new CardLayout();
                        private final JPanel localRoot = new JPanel(localCards);

                        private final PartagesUsersView usersView = new PartagesUsersView();
                        private final PartagesFilesView filesView = new PartagesFilesView();
                        private final PartagesDemandesView demandesView = new PartagesDemandesView();

                        PartagesPage() {
                            localRoot.setOpaque(false);
                            localRoot.add(usersView, "users");
                            localRoot.add(filesView, "files");
                            localRoot.add(demandesView, "demandes");
                            add(localRoot, BorderLayout.CENTER);
                        }

                        @Override
                        void setSession(SmartDriveConnection conn, String username) {
                            super.setSession(conn, username);
                            usersView.setSession(conn, username);
                            filesView.setSession(conn, username);
                            demandesView.setSession(conn, username);
                        }

                        @Override
                        void onShow() {
                            localCards.show(localRoot, "users");
                            usersView.onShow();
                        }

                        final class PartagesUsersView extends BasePage {
                            private final JPanel root = new JPanel(new BorderLayout());
                            private final JLabel info = new JLabel(" ");
                            private final JPanel wrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));

                            PartagesUsersView() {
                                root.setOpaque(false);
                                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                                JPanel navbar = new JPanel(new BorderLayout());
                                navbar.setOpaque(false);
                                JLabel brand = new JLabel("SmartDrive - Partages");
                                brand.setFont(brand.getFont().deriveFont(Font.BOLD, 14f));
                                navbar.add(brand, BorderLayout.WEST);

                                BootstrapButton demandes = new BootstrapButton("Demandes reçues", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
                                demandes.addActionListener(e -> {
                                    localCards.show(localRoot, "demandes");
                                    demandesView.onShow();
                                });
                                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                                actions.setOpaque(false);
                                actions.add(demandes);
                                navbar.add(actions, BorderLayout.EAST);

                                JLabel h = new JLabel("Partages");
                                h.setForeground(BootstrapColors.PRIMARY);
                                h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));

                                info.setOpaque(true);
                                info.setBackground(new Color(0xCFE2FF));
                                info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                                info.setVisible(false);

                                JPanel top = new JPanel();
                                top.setOpaque(false);
                                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                                top.add(navbar);
                                top.add(Box.createVerticalStrut(10));
                                top.add(h);
                                top.add(Box.createVerticalStrut(10));
                                top.add(info);

                                wrap.setOpaque(false);
                                JScrollPane sp = new JScrollPane(wrap);
                                sp.setBorder(BorderFactory.createEmptyBorder());
                                sp.getViewport().setBackground(BootstrapColors.BG_LIGHT);

                                root.add(top, BorderLayout.NORTH);
                                root.add(sp, BorderLayout.CENTER);
                                add(root, BorderLayout.CENTER);
                            }

                            @Override
                            void onShow() {
                                if (conn == null) return;
                                info.setVisible(false);
                                SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected List<String> doInBackground() throws Exception {
                                        return conn.listUsers();
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            List<String> users = get();
                                            renderUsers(users);
                                        } catch (Exception ex) {
                                            info.setText(ex.getMessage());
                                            info.setBackground(new Color(0xF8D7DA));
                                            info.setVisible(true);
                                            renderUsers(List.of());
                                        }
                                    }
                                };
                                worker.execute();
                            }

                            private void renderUsers(List<String> users) {
                                wrap.removeAll();
                                if (users != null) {
                                    for (String u : users) {
                                        if (u == null) continue;
                                        if (username != null && u.equalsIgnoreCase(username)) continue;
                                        CardPanel card = new CardPanel();
                                        card.setPreferredSize(new Dimension(260, 140));
                                        JPanel body = new JPanel();
                                        body.setOpaque(false);
                                        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                                        JLabel name = new JLabel(u);
                                        name.setForeground(BootstrapColors.PRIMARY);
                                        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));
                                        body.add(name);
                                        body.add(Box.createVerticalGlue());
                                        BootstrapButton view = new BootstrapButton("Voir les fichiers", BootstrapButton.Variant.PRIMARY, true);
                                        view.addActionListener(e -> {
                                            filesView.setOwner(u);
                                            localCards.show(localRoot, "files");
                                            filesView.onShow();
                                        });
                                        body.add(view);
                                        card.add(body, BorderLayout.CENTER);
                                        wrap.add(card);
                                    }
                                }
                                if (wrap.getComponentCount() == 0) {
                                    JLabel empty = new JLabel("Aucun autre utilisateur (ou backend injoignable).", SwingConstants.CENTER);
                                    empty.setForeground(BootstrapColors.TEXT_MUTED);
                                    wrap.add(empty);
                                }
                                wrap.revalidate();
                                wrap.repaint();
                            }
                        }

                        final class PartagesFilesView extends BasePage {
                            private final JPanel root = new JPanel(new BorderLayout());
                            private final JLabel info = new JLabel(" ");
                            private final JPanel wrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
                            private String owner;

                            PartagesFilesView() {
                                root.setOpaque(false);
                                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                                JPanel navbar = new JPanel(new BorderLayout());
                                navbar.setOpaque(false);
                                BootstrapButton back = new BootstrapButton("Retour", BootstrapButton.Variant.OUTLINE_SECONDARY, true);
                                back.addActionListener(e -> {
                                    localCards.show(localRoot, "users");
                                    usersView.onShow();
                                });
                                navbar.add(back, BorderLayout.EAST);

                                JLabel h = new JLabel();
                                h.setForeground(BootstrapColors.PRIMARY);
                                h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
                                navbar.add(h, BorderLayout.WEST);

                                info.setOpaque(true);
                                info.setBackground(new Color(0xCFE2FF));
                                info.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                                info.setVisible(false);

                                wrap.setOpaque(false);
                                JScrollPane sp = new JScrollPane(wrap);
                                sp.setBorder(BorderFactory.createEmptyBorder());
                                sp.getViewport().setBackground(BootstrapColors.BG_LIGHT);

                                root.add(navbar, BorderLayout.NORTH);
                                root.add(info, BorderLayout.CENTER);
                                root.add(sp, BorderLayout.SOUTH);

                                add(root, BorderLayout.CENTER);

                                addPropertyChangeListener("owner", evt -> h.setText("Fichiers de " + owner));
                            }

                            void setOwner(String owner) {
                                this.owner = owner;
                                firePropertyChange("owner", false, true);
                            }

                            @Override
                            void onShow() {
                                if (conn == null || owner == null) return;
                                info.setVisible(false);
                                SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected List<String> doInBackground() throws Exception {
                                        return conn.listSharedFiles(owner);
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            render(get());
                                        } catch (Exception ex) {
                                            info.setText(ex.getMessage());
                                            info.setBackground(new Color(0xF8D7DA));
                                            info.setVisible(true);
                                            render(List.of());
                                        }
                                    }
                                };
                                worker.execute();
                            }

                            private void render(List<String> rows) {
                                wrap.removeAll();
                                if (rows != null) {
                                    for (String row : rows) {
                                        if (row == null) continue;
                                        String[] p = row.split(";", 3);
                                        if (p.length < 3) continue;
                                        String filename = p[0];
                                        String size = p[1];
                                        String status = p[2];

                                        CardPanel card = new CardPanel();
                                        card.setPreferredSize(new Dimension(260, 150));
                                        JPanel body = new JPanel();
                                        body.setOpaque(false);
                                        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                                        JLabel name = new JLabel(filename);
                                        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
                                        JLabel s = new JLabel(size + " octets");
                                        s.setForeground(BootstrapColors.TEXT_MUTED);
                                        body.add(name);
                                        body.add(Box.createVerticalStrut(6));
                                        body.add(s);
                                        body.add(Box.createVerticalGlue());

                                        if ("approved".equalsIgnoreCase(status)) {
                                            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                                            actions.setOpaque(false);
                                            BootstrapButton voir = new BootstrapButton("Voir", BootstrapButton.Variant.PRIMARY, true);
                                            BootstrapButton dl = new BootstrapButton("Download", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
                                            voir.addActionListener(e -> sharedView(filename));
                                            dl.addActionListener(e -> sharedDownload(filename));
                                            actions.add(voir);
                                            actions.add(dl);
                                            body.add(actions);
                                        } else if ("pending".equalsIgnoreCase(status)) {
                                            BootstrapButton pending = new BootstrapButton("Demande en attente", BootstrapButton.Variant.WARNING, true);
                                            pending.setEnabled(false);
                                            body.add(pending);
                                        } else {
                                            BootstrapButton ask = new BootstrapButton("Demander lecture", BootstrapButton.Variant.INFO, true);
                                            ask.addActionListener(e -> request(filename));
                                            body.add(ask);
                                        }

                                        card.add(body, BorderLayout.CENTER);
                                        wrap.add(card);
                                    }
                                }
                                if (wrap.getComponentCount() == 0) {
                                    JLabel empty = new JLabel("Aucun fichier trouvé.");
                                    empty.setForeground(BootstrapColors.TEXT_MUTED);
                                    wrap.add(empty);
                                }
                                wrap.revalidate();
                                wrap.repaint();
                            }

                            private void request(String filename) {
                                SwingWorker<String, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected String doInBackground() throws Exception {
                                        return conn.requestRead(owner, filename);
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            String resp = get();
                                            JOptionPane.showMessageDialog(PartagesFilesView.this, resp, "Partages", JOptionPane.INFORMATION_MESSAGE);
                                            onShow();
                                        } catch (Exception ex) {
                                            JOptionPane.showMessageDialog(PartagesFilesView.this, ex.getMessage(), "Partages", JOptionPane.ERROR_MESSAGE);
                                        }
                                    }
                                };
                                worker.execute();
                            }

                            private void sharedDownload(String filename) {
                                JFileChooser chooser = new JFileChooser();
                                chooser.setDialogTitle("Enregistrer sous...");
                                chooser.setSelectedFile(new File(filename));
                                int res = chooser.showSaveDialog(this);
                                if (res != JFileChooser.APPROVE_OPTION) return;
                                File dest = chooser.getSelectedFile();
                                if (dest == null) return;

                                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected Void doInBackground() throws Exception {
                                        conn.downloadAs(owner, filename, dest);
                                        return null;
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            get();
                                            JOptionPane.showMessageDialog(PartagesFilesView.this, "Téléchargé: " + dest.getAbsolutePath(), "Download", JOptionPane.INFORMATION_MESSAGE);
                                        } catch (Exception ex) {
                                            JOptionPane.showMessageDialog(PartagesFilesView.this, ex.getMessage(), "Download", JOptionPane.ERROR_MESSAGE);
                                        }
                                    }
                                };
                                worker.execute();
                            }

                            private void sharedView(String filename) {
                                SwingWorker<File, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected File doInBackground() throws Exception {
                                        File tmp = File.createTempFile("smartdrive_shared_", "_" + filename.replaceAll("[^a-zA-Z0-9._-]", "_"));
                                        tmp.deleteOnExit();
                                        conn.downloadAs(owner, filename, tmp);
                                        return tmp;
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            File f = get();
                                            if (Desktop.isDesktopSupported()) {
                                                Desktop.getDesktop().open(f);
                                            } else {
                                                JOptionPane.showMessageDialog(PartagesFilesView.this, f.getAbsolutePath(), "Fichier", JOptionPane.INFORMATION_MESSAGE);
                                            }
                                        } catch (Exception ex) {
                                            JOptionPane.showMessageDialog(PartagesFilesView.this, ex.getMessage(), "Voir", JOptionPane.ERROR_MESSAGE);
                                        }
                                    }
                                };
                                worker.execute();
                            }
                        }

                        final class PartagesDemandesView extends BasePage {
                            private final JPanel root = new JPanel(new BorderLayout());
                            private final RequestsTableModel model = new RequestsTableModel();
                            private final JTable table = new JTable(model);

                            PartagesDemandesView() {
                                root.setOpaque(false);
                                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                                JPanel navbar = new JPanel(new BorderLayout());
                                navbar.setOpaque(false);
                                BootstrapButton back = new BootstrapButton("Partages", BootstrapButton.Variant.OUTLINE_SECONDARY, true);
                                back.addActionListener(e -> {
                                    localCards.show(localRoot, "users");
                                    usersView.onShow();
                                });
                                navbar.add(back, BorderLayout.WEST);
                                JLabel title = new JLabel("Demandes reçues");
                                title.setForeground(BootstrapColors.TEXT_MUTED);
                                navbar.add(title, BorderLayout.EAST);

                                CardPanel card = new CardPanel();
                                card.setLayout(new BorderLayout());
                                JLabel header = new JLabel("Demandes d'accès à vos fichiers");
                                header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                                header.setFont(header.getFont().deriveFont(Font.BOLD));
                                card.add(header, BorderLayout.NORTH);

                                table.setFillsViewportHeight(true);
                                table.setRowHeight(28);
                                table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());

                                JScrollPane sp = new JScrollPane(table);
                                card.add(sp, BorderLayout.CENTER);

                                root.add(navbar, BorderLayout.NORTH);
                                root.add(card, BorderLayout.CENTER);
                                add(root, BorderLayout.CENTER);
                            }

                            @Override
                            void onShow() {
                                if (conn == null) return;
                                SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected List<String> doInBackground() throws Exception {
                                        return conn.listIncomingRequests();
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            model.setRows(get());
                                            installActions();
                                        } catch (Exception ex) {
                                            model.setRows(List.of());
                                        }
                                    }
                                };
                                worker.execute();
                            }

                            private void installActions() {
                                if (table.getColumnCount() < 6) return;
                                Action approve = new AbstractAction() {
                                    @Override
                                    public void actionPerformed(java.awt.event.ActionEvent e) {
                                        int row = Integer.parseInt(e.getActionCommand());
                                        RequestsRow rr = model.getRow(row);
                                        if (rr == null) return;
                                        respond(rr.requester, rr.file, "approve");
                                    }
                                };
                                Action deny = new AbstractAction() {
                                    @Override
                                    public void actionPerformed(java.awt.event.ActionEvent e) {
                                        int row = Integer.parseInt(e.getActionCommand());
                                        RequestsRow rr = model.getRow(row);
                                        if (rr == null) return;
                                        respond(rr.requester, rr.file, "deny");
                                    }
                                };
                                new TableButtonColumn(table, approve, 4);
                                new TableButtonColumn(table, deny, 5);
                            }

                            private void respond(String requester, String file, String action) {
                                SwingWorker<String, Void> worker = new SwingWorker<>() {
                                    @Override
                                    protected String doInBackground() throws Exception {
                                        return conn.respondRequest(requester, file, action);
                                    }

                                    @Override
                                    protected void done() {
                                        try {
                                            String resp = get();
                                            JOptionPane.showMessageDialog(PartagesDemandesView.this, resp, "Demandes", JOptionPane.INFORMATION_MESSAGE);
                                            onShow();
                                        } catch (Exception ex) {
                                            JOptionPane.showMessageDialog(PartagesDemandesView.this, ex.getMessage(), "Demandes", JOptionPane.ERROR_MESSAGE);
                                        }
                                    }
                                };
                                worker.execute();
                            }

                            static final class RequestsRow {
                                final String requester;
                                final String file;
                                final String status;

                                RequestsRow(String requester, String file, String status) {
                                    this.requester = requester;
                                    this.file = file;
                                    this.status = status;
                                }
                            }

                            static final class RequestsTableModel extends javax.swing.table.AbstractTableModel {
                                private final String[] cols = {"Demandeur", "Fichier", "Statut", " ", "Approuver", "Refuser"};
                                private final List<RequestsRow> rows = new ArrayList<>();

                                void setRows(List<String> lines) {
                                    rows.clear();
                                    if (lines != null) {
                                        for (String l : lines) {
                                            if (l == null) continue;
                                            String[] p = l.split(";", 4);
                                            if (p.length < 3) continue;
                                            rows.add(new RequestsRow(p[0], p[1], p[2]));
                                        }
                                    }
                                    fireTableDataChanged();
                                }

                                RequestsRow getRow(int idx) {
                                    if (idx < 0 || idx >= rows.size()) return null;
                                    return rows.get(idx);
                                }

                                @Override public int getRowCount() { return rows.size(); }
                                @Override public int getColumnCount() { return cols.length; }
                                @Override public String getColumnName(int column) { return cols[column]; }

                                @Override
                                public Object getValueAt(int rowIndex, int columnIndex) {
                                    RequestsRow r = rows.get(rowIndex);
                                    return switch (columnIndex) {
                                        case 0 -> r.requester;
                                        case 1 -> r.file;
                                        case 2 -> r.status;
                                        case 4 -> "Approuver";
                                        case 5 -> "Refuser";
                                        default -> "";
                                    };
                                }

                                @Override
                                public boolean isCellEditable(int rowIndex, int columnIndex) {
                                    return columnIndex == 4 || columnIndex == 5;
                                }
                            }
                        }
                    }

                    static final class StockagePage extends BasePage {
                        private final JPanel root = new JPanel(new BorderLayout());
                        private final JLabel title = new JLabel("Mon Stockage");
                        private final JLabel usage = new JLabel("Utilisation actuelle: -");
                        private final JProgressBar progress = new JProgressBar(0, 100);

                        private long usedBytes;
                        private long quotaBytes;
                        private int fileCount;

                        StockagePage() {
                            root.setOpaque(false);
                            root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                            title.setForeground(BootstrapColors.PRIMARY);
                            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

                            CardPanel mainCard = new CardPanel();
                            mainCard.setLayout(new BorderLayout());
                            JPanel body = new JPanel();
                            body.setOpaque(false);
                            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                            JLabel h = new JLabel("Évolution du stockage - ");
                            h.setFont(h.getFont().deriveFont(Font.BOLD, 14f));
                            body.add(h);
                            usage.setForeground(BootstrapColors.TEXT_MUTED);
                            body.add(Box.createVerticalStrut(6));
                            body.add(usage);
                            body.add(Box.createVerticalStrut(16));

                            progress.setStringPainted(true);
                            body.add(progress);

                            body.add(Box.createVerticalStrut(10));
                            JLabel stats = new JLabel("Stockage actuel / Quota total / Espace libre");
                            stats.setForeground(BootstrapColors.TEXT_MUTED);
                            body.add(stats);
                            mainCard.add(body, BorderLayout.CENTER);

                            JPanel top = new JPanel(new BorderLayout());
                            top.setOpaque(false);
                            top.add(title, BorderLayout.WEST);

                            root.add(top, BorderLayout.NORTH);
                            root.add(mainCard, BorderLayout.CENTER);

                            add(root, BorderLayout.CENTER);
                        }

                        @Override
                        void onShow() {
                            if (conn == null) return;
                            SwingWorker<StockageData, Void> worker = new SwingWorker<>() {
                                @Override
                                protected StockageData doInBackground() throws Exception {
                                    List<SmartDriveConnection.RemoteFileRow> rows = conn.listFiles();
                                    long remaining = conn.quotaRemaining();
                                    long used = 0;
                                    for (SmartDriveConnection.RemoteFileRow r : rows) used += Math.max(0, r.size);
                                    long quota = remaining >= 0 ? remaining + used : -1;
                                    return new StockageData(used, quota, rows.size());
                                }

                                @Override
                                protected void done() {
                                    try {
                                        StockageData d = get();
                                        usedBytes = d.used;
                                        quotaBytes = d.quota;
                                        fileCount = d.files;
                                        render();
                                    } catch (Exception ex) {
                                        usage.setText("Erreur: " + ex.getMessage());
                                    }
                                }
                            };
                            worker.execute();
                        }

                        private void render() {
                            String usedStr = ServeurService.formatSize(usedBytes);
                            String quotaStr = quotaBytes > 0 ? ServeurService.formatSize(quotaBytes) : "-";
                            int pct = (quotaBytes > 0) ? (int) Math.min(100, (usedBytes * 100) / quotaBytes) : 0;
                            usage.setText("Utilisation actuelle: " + usedStr + " sur " + quotaStr + " (" + pct + "%) | Fichiers: " + fileCount);
                            progress.setValue(pct);
                            progress.setString(pct + "%");
                            if (pct > 80) {
                                progress.setForeground(BootstrapColors.DANGER);
                            } else if (pct > 60) {
                                progress.setForeground(BootstrapColors.WARNING);
                            } else if (pct > 40) {
                                progress.setForeground(BootstrapColors.INFO);
                            } else {
                                progress.setForeground(BootstrapColors.SUCCESS);
                            }
                        }

                        static final class StockageData {
                            final long used;
                            final long quota;
                            final int files;

                            StockageData(long used, long quota, int files) {
                                this.used = used;
                                this.quota = quota;
                                this.files = files;
                            }
                        }
                    }

*/
