package desktop;

import Service.ServeurService;
import desktop.ui.BootstrapButton;
import desktop.ui.BootstrapColors;
import desktop.ui.CardPanel;
import desktop.ui.NavPillButton;
import desktop.ui.StripedTableCellRenderer;
import desktop.ui.TableButtonColumn;
import desktop.ui.WrapLayout;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Swing desktop app that mirrors the JSP/Bootstrap layout (navbar + sidebar + pages).
 */
public final class SmartDriveBootstrapApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                new SmartDriveFrame().setVisible(true);
            }
        });
    }

    enum PageId {
        ACCUEIL,
        MES_FICHIERS,
        CORBEILLE,
        PARTAGES,
        STOCKAGE,
        ADMIN
    }

    static final class SmartDriveFrame extends JFrame {
        private final CardLayout cards = new CardLayout();
        private final JPanel root = new JPanel(cards);

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

            cards.show(root, "login");
        }

        void onLoggedIn(SmartDriveConnection conn, String username) {
            appView.setSession(conn, username);
            cards.show(root, "app");
        }

        void logout() {
            appView.clearSession();
            cards.show(root, "login");
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

            loginButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    doLogin();
                }
            });
            passwordField.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    doLogin();
                }
            });
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
            final String username = usernameField.getText().trim();
            final String password = new String(passwordField.getPassword());
            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Veuillez saisir utilisateur + mot de passe");
                return;
            }

            errorLabel.setText(" ");
            setBusy(true);

            SwingWorker<SmartDriveConnection, Void> worker = new SwingWorker<SmartDriveConnection, Void>() {
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

    static final class AppView extends JPanel {
        private final JPanel navbar = new JPanel(new BorderLayout());
        private final JPanel sidebar = new JPanel(new BorderLayout());

        private final CardLayout pages = new CardLayout();
        private final JPanel content = new JPanel(pages);

        private SmartDriveConnection conn;

        private final JLabel helloName = new JLabel("Bonjour, -");
        private final JLabel helloEmail = new JLabel("-@example.com");

        private final ButtonGroup navGroup = new ButtonGroup();
        private final NavPillButton navAccueil = new NavPillButton("Accueil");
        private final NavPillButton navMesFichiers = new NavPillButton("Mes fichiers");
        private final NavPillButton navCorbeille = new NavPillButton("Corbeille");
        private final NavPillButton navPartages = new NavPillButton("Partages");
        private final NavPillButton navStockage = new NavPillButton("Stockage");
        private final NavPillButton navAdmin = new NavPillButton("Admin");
        private final NavPillButton navParametres = new NavPillButton("Parametres");

        private final BootstrapButton logoutButton = new BootstrapButton("Déconnexion", BootstrapButton.Variant.OUTLINE_DANGER, true);

        private final AccueilPage accueilPage = new AccueilPage();
        private final MesFichiersPage mesFichiersPage = new MesFichiersPage();
        private final CorbeillePage corbeillePage = new CorbeillePage();
        private final PartagesPage partagesPage = new PartagesPage();
        private final StockagePage stockagePage = new StockagePage();
        private final AdminPage adminPage = new AdminPage();

        AppView(SmartDriveFrame frame) {
            super(new BorderLayout());
            setBackground(BootstrapColors.BG_LIGHT);

            buildNavbar();
            buildSidebar();
            buildContent();

            add(navbar, BorderLayout.NORTH);
            add(sidebar, BorderLayout.WEST);
            add(content, BorderLayout.CENTER);

            navAccueil.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(PageId.ACCUEIL);
                }
            });
            navMesFichiers.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(PageId.MES_FICHIERS);
                }
            });
            navCorbeille.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(PageId.CORBEILLE);
                }
            });
            navPartages.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(PageId.PARTAGES);
                }
            });
            navStockage.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(PageId.STOCKAGE);
                }
            });
            navAdmin.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(PageId.ADMIN);
                }
            });
            logoutButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    frame.logout();
                }
            });

            navParametres.setEnabled(false);
            navAccueil.setSelected(true);
            navigate(PageId.ACCUEIL);
        }

        void setSession(SmartDriveConnection conn, String username) {
            this.conn = conn;

            helloName.setText("Bonjour, " + username);
            helloEmail.setText(username + "@example.com");

            boolean isAdmin = false;
            try {
                isAdmin = conn.isAdmin();
            } catch (Exception ignored) {
            }
            navAdmin.setVisible(isAdmin);

            accueilPage.setSession(conn, username);
            mesFichiersPage.setSession(conn, username);
            corbeillePage.setSession(conn, username);
            partagesPage.setSession(conn, username);
            stockagePage.setSession(conn, username);
            adminPage.setSession(conn, username);

            navAccueil.setSelected(true);
            navigate(PageId.ACCUEIL);
        }

        void clearSession() {
            if (conn != null) {
                conn.close();
            }
            conn = null;
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
            navGroup.add(navAdmin);
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
            nav.add(navAdmin);
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
            content.add(adminPage, PageId.ADMIN.name());
        }

        private void navigate(PageId id) {
            pages.show(content, id.name());
            if (id == PageId.ACCUEIL) {
                accueilPage.onShow();
            } else if (id == PageId.MES_FICHIERS) {
                mesFichiersPage.onShow();
            } else if (id == PageId.CORBEILLE) {
                corbeillePage.onShow();
            } else if (id == PageId.PARTAGES) {
                partagesPage.onShow();
            } else if (id == PageId.STOCKAGE) {
                stockagePage.onShow();
            } else if (id == PageId.ADMIN) {
                adminPage.onShow();
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

        final void showErrorDialog(String title, Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), title, JOptionPane.ERROR_MESSAGE);
        }

        final void showInfoDialog(String title, String message) {
            JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
        }

        final JLabel alertLabel() {
            JLabel l = new JLabel(" ");
            l.setOpaque(true);
            l.setBackground(new Color(0xCFE2FF));
            l.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            l.setVisible(false);
            return l;
        }

        final void setAlert(JLabel alert, String text, boolean error) {
            alert.setText(text);
            alert.setBackground(error ? new Color(0xF8D7DA) : new Color(0xCFE2FF));
            alert.setVisible(true);
        }

        final void clearAlert(JLabel alert) {
            alert.setVisible(false);
            alert.setText(" ");
        }
    }

    static final class AccueilPage extends BasePage {
        private final JPanel root = new JPanel(new BorderLayout());
        private final JPanel notificationsPanel = new JPanel(new BorderLayout());
        private final JTextArea notificationsText = new JTextArea();

        private final JPanel usersWrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));

        AccueilPage() {
            root.setOpaque(false);
            root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            notificationsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            notificationsPanel.setBackground(new Color(0xCFE2FF));

            JLabel nTitle = new JLabel("Notifications");
            nTitle.setFont(nTitle.getFont().deriveFont(Font.BOLD, 13f));

            notificationsText.setEditable(false);
            notificationsText.setLineWrap(true);
            notificationsText.setWrapStyleWord(true);
            notificationsText.setOpaque(false);
            notificationsText.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

            notificationsPanel.add(nTitle, BorderLayout.NORTH);
            notificationsPanel.add(notificationsText, BorderLayout.CENTER);

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            JLabel h = new JLabel("Utilisateurs");
            h.setForeground(BootstrapColors.PRIMARY);
            h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));
            header.add(h, BorderLayout.WEST);

            BootstrapButton upload = new BootstrapButton("Upload", BootstrapButton.Variant.PRIMARY, true);
            upload.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (conn == null) return;
                    MesFichiersPage.doUpload(AccueilPage.this, conn);
                }
            });
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            actions.add(upload);
            header.add(actions, BorderLayout.EAST);

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(notificationsPanel);
            top.add(Box.createVerticalStrut(12));
            top.add(header);

            usersWrap.setOpaque(false);
            JScrollPane sp = new JScrollPane(usersWrap);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.getViewport().setBackground(BootstrapColors.BG_LIGHT);

            root.add(top, BorderLayout.NORTH);
            root.add(sp, BorderLayout.CENTER);
            add(root, BorderLayout.CENTER);
        }

        @Override
        void onShow() {
            if (conn == null) return;
            refresh();
        }

        private void refresh() {
            SwingWorker<AccueilData, Void> worker = new SwingWorker<AccueilData, Void>() {
                @Override
                protected AccueilData doInBackground() throws Exception {
                    List<String> users = conn.listUsers();
                    List<String> notifs = conn.listNotifications();
                    if (users == null) users = Collections.emptyList();
                    if (notifs == null) notifs = Collections.emptyList();
                    return new AccueilData(users, notifs);
                }

                @Override
                protected void done() {
                    try {
                        AccueilData data = get();
                        renderNotifs(data.notifications);
                        renderUsers(data.users);
                    } catch (Exception ex) {
                        renderNotifs(Collections.singletonList("Erreur: " + ex.getMessage()));
                        renderUsers(Collections.<String>emptyList());
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
            if (users == null) users = Collections.emptyList();
            for (final String u : users) {
                if (u == null || u.trim().isEmpty()) continue;
                if (username != null && u.equalsIgnoreCase(username)) continue;

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
                voir.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        JOptionPane.showMessageDialog(AccueilPage.this,
                                "Pour voir les fichiers de " + u + ", allez dans Partages.",
                                "Info",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
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
        private final JLabel alert = alertLabel();

        private final JTextField q = new JTextField();
        private final JComboBox<String> type = new JComboBox<String>(new String[]{"Tous", "PDF", "Images", "Vidéos", "Audio", "Documents", "Tableurs", "Texte", "Archives"});
        private final JTextField min = new JTextField();
        private final JTextField max = new JTextField();
        private final BootstrapButton search = new BootstrapButton("Rechercher", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
        private final BootstrapButton upload = new BootstrapButton("Upload", BootstrapButton.Variant.PRIMARY, true);

        private final JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));

        private List<SmartDriveConnection.RemoteFileRow> last = new ArrayList<SmartDriveConnection.RemoteFileRow>();

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

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(header);
            top.add(Box.createVerticalStrut(10));
            top.add(alert);
            top.add(Box.createVerticalStrut(10));
            top.add(buildSearchForm());

            grid.setOpaque(false);
            JScrollPane sp = new JScrollPane(grid);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.getViewport().setBackground(BootstrapColors.BG_LIGHT);

            root.add(top, BorderLayout.NORTH);
            root.add(sp, BorderLayout.CENTER);
            add(root, BorderLayout.CENTER);

            upload.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (conn == null) return;
                    doUpload(MesFichiersPage.this, conn);
                    onShow();
                }
            });
            search.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    render();
                }
            });
        }

        @Override
        void onShow() {
            if (conn == null) return;
            clearAlert(alert);

            SwingWorker<List<SmartDriveConnection.RemoteFileRow>, Void> worker = new SwingWorker<List<SmartDriveConnection.RemoteFileRow>, Void>() {
                @Override
                protected List<SmartDriveConnection.RemoteFileRow> doInBackground() throws Exception {
                    List<SmartDriveConnection.RemoteFileRow> rows = conn.listFiles();
                    if (rows == null) rows = Collections.emptyList();
                    rows = new ArrayList<SmartDriveConnection.RemoteFileRow>(rows);
                    Collections.sort(rows, new Comparator<SmartDriveConnection.RemoteFileRow>() {
                        @Override
                        public int compare(SmartDriveConnection.RemoteFileRow o1, SmartDriveConnection.RemoteFileRow o2) {
                            return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
                        }
                    });
                    return rows;
                }

                @Override
                protected void done() {
                    try {
                        last = get();
                        render();
                    } catch (Exception ex) {
                        last = new ArrayList<SmartDriveConnection.RemoteFileRow>();
                        setAlert(alert, ex.getMessage(), true);
                        render();
                    }
                }
            };
            worker.execute();
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
            form.add(search, c);

            return form;
        }

        private static JLabel mutedLabel(String t) {
            JLabel l = new JLabel(t);
            l.setForeground(BootstrapColors.TEXT_MUTED);
            return l;
        }

        static void doUpload(Component parent, final SmartDriveConnection conn) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choisir un fichier à uploader");
            int res = chooser.showOpenDialog(parent);
            if (res != JFileChooser.APPROVE_OPTION) return;
            final File file = chooser.getSelectedFile();
            if (file == null) return;

            final JDialog dlg = progressDialog(parent, "Upload", "Envoi du fichier...");
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
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
            if (rows == null) return Collections.emptyList();

            String qs = q.getText() == null ? "" : q.getText().trim().toLowerCase();
            String t = (String) type.getSelectedItem();
            long minV = parseLong(min.getText());
            long maxV = parseLong(max.getText());

            List<SmartDriveConnection.RemoteFileRow> out = new ArrayList<SmartDriveConnection.RemoteFileRow>();
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
            if ("PDF".equals(type)) return f.endsWith(".pdf");
            if ("Images".equals(type)) return f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".gif");
            if ("Vidéos".equals(type)) return f.endsWith(".mp4") || f.endsWith(".avi") || f.endsWith(".mkv");
            if ("Audio".equals(type)) return f.endsWith(".mp3") || f.endsWith(".wav");
            if ("Documents".equals(type)) return f.endsWith(".doc") || f.endsWith(".docx");
            if ("Tableurs".equals(type)) return f.endsWith(".xls") || f.endsWith(".xlsx");
            if ("Texte".equals(type)) {
                return f.endsWith(".txt") || f.endsWith(".md") || f.endsWith(".log") || f.endsWith(".csv")
                        || f.endsWith(".json") || f.endsWith(".xml") || f.endsWith(".yml") || f.endsWith(".yaml");
            }
            if ("Archives".equals(type)) return f.endsWith(".zip") || f.endsWith(".rar");
            return true;
        }

        private JComponent fileCard(final SmartDriveConnection.RemoteFileRow r) {
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
            BootstrapButton download = new BootstrapButton("Download", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
            BootstrapButton del = new BootstrapButton("Supprimer", BootstrapButton.Variant.OUTLINE_DANGER, true);

            voir.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    viewFile(r.name);
                }
            });
            versions.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    showVersions(r.name);
                }
            });
            download.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    downloadFile(r.name);
                }
            });
            del.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    deleteFile(r.name);
                }
            });

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

        private void viewFile(final String filename) {
            if (conn == null) return;
            SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {
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
                                showInfoDialog("Fichier", f.getAbsolutePath());
                            }
                        }
                    } catch (Exception ex) {
                        showErrorDialog("Voir", ex);
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
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                br.close();
            }

            final JTextArea area = new JTextArea(sb.toString());
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

        private void downloadFile(final String filename) {
            if (conn == null) return;
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Enregistrer sous...");
            chooser.setSelectedFile(new File(filename));
            int res = chooser.showSaveDialog(this);
            if (res != JFileChooser.APPROVE_OPTION) return;
            final File dest = chooser.getSelectedFile();
            if (dest == null) return;

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    conn.download(filename, dest);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        showInfoDialog("Download", "Téléchargé: " + dest.getAbsolutePath());
                    } catch (Exception ex) {
                        showErrorDialog("Download", ex);
                    }
                }
            };
            worker.execute();
        }

        private void deleteFile(final String filename) {
            if (conn == null) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Êtes-vous sûr de vouloir supprimer le fichier " + filename + " ?\n\nLe fichier sera déplacé dans la corbeille (restauration possible).",
                    "Confirmer la suppression",
                    JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;

            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return conn.deleteToTrash(filename);
                }

                @Override
                protected void done() {
                    try {
                        String resp = get();
                        setAlert(alert, resp, resp.startsWith("ERROR"));
                        onShow();
                    } catch (Exception ex) {
                        setAlert(alert, ex.getMessage(), true);
                    }
                }
            };
            worker.execute();
        }

        private void showVersions(final String filename) {
            if (conn == null) return;

            final JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Versions", Dialog.ModalityType.APPLICATION_MODAL);
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

            final VersionsTableModel model = new VersionsTableModel();
            final JTable table = new JTable(model);
            table.setFillsViewportHeight(true);
            table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());

            root.add(new JScrollPane(table), BorderLayout.CENTER);

            BootstrapButton restore = new BootstrapButton("Restaurer", BootstrapButton.Variant.SUCCESS, true);
            BootstrapButton close = new BootstrapButton("Fermer", BootstrapButton.Variant.OUTLINE_SECONDARY, true);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            bottom.setOpaque(false);
            bottom.add(restore);
            bottom.add(close);
            root.add(bottom, BorderLayout.SOUTH);

            close.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    dlg.dispose();
                }
            });
            restore.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int row = table.getSelectedRow();
                    if (row < 0) return;
                    final String id = model.getVersionId(row);
                    int ok = JOptionPane.showConfirmDialog(dlg, "Restaurer cette version ?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
                    if (ok != JOptionPane.OK_OPTION) return;

                    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
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
                }
            });

            SwingWorker<List<String>, Void> loader = new SwingWorker<List<String>, Void>() {
                @Override
                protected List<String> doInBackground() throws Exception {
                    return conn.listVersions(filename);
                }

                @Override
                protected void done() {
                    try {
                        model.setRows(get());
                    } catch (Exception ex) {
                        model.setRows(Collections.<String>emptyList());
                    }
                }
            };
            loader.execute();

            dlg.setContentPane(root);
            dlg.setVisible(true);
        }

        static final class VersionsTableModel extends AbstractTableModel {
            private final String[] cols = {"Version", "Taille", "Date"};
            private final List<String[]> rows = new ArrayList<String[]>();

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

            @Override
            public int getRowCount() {
                return rows.size();
            }

            @Override
            public int getColumnCount() {
                return cols.length;
            }

            @Override
            public String getColumnName(int column) {
                return cols[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                String[] r = rows.get(rowIndex);
                if (columnIndex == 0) return r[0];
                if (columnIndex == 1) return r[1] + " o";
                if (columnIndex == 2) return r[2];
                return "";
            }
        }
    }

    static final class CorbeillePage extends BasePage {
        private final JPanel root = new JPanel(new BorderLayout());
        private final JLabel alert = alertLabel();
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

            CardPanel card = new CardPanel();
            card.setLayout(new BorderLayout());
            table.setFillsViewportHeight(true);
            table.setRowHeight(28);
            table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
            card.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
            top.add(alert);
            top.add(Box.createVerticalStrut(10));
            top.add(header);

            root.add(top, BorderLayout.NORTH);
            root.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
            root.add(card, BorderLayout.SOUTH);

            add(root, BorderLayout.CENTER);

            vider.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    purgeAll();
                }
            });
        }

        @Override
        void onShow() {
            if (conn == null) return;
            refresh();
        }

        private void refresh() {
            clearAlert(alert);
            SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
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
                        model.setRows(Collections.<String>emptyList());
                        setAlert(alert, ex.getMessage(), true);
                    }
                }
            };
            worker.execute();
        }

        private void installActions() {
            if (table.getColumnCount() < 6) return;

            Action restoreAction = new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int row = Integer.parseInt(e.getActionCommand());
                    TrashRow tr = model.getRow(row);
                    if (tr == null) return;

                    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                        @Override
                        protected String doInBackground() throws Exception {
                            return conn.trashRestore(tr.id);
                        }

                        @Override
                        protected void done() {
                            try {
                                String resp = get();
                                setAlert(alert, resp, resp.startsWith("ERROR"));
                                refresh();
                            } catch (Exception ex) {
                                setAlert(alert, ex.getMessage(), true);
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

                    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                        @Override
                        protected String doInBackground() throws Exception {
                            return conn.trashPurge(tr.id);
                        }

                        @Override
                        protected void done() {
                            try {
                                String resp = get();
                                setAlert(alert, resp, resp.startsWith("ERROR"));
                                refresh();
                            } catch (Exception ex) {
                                setAlert(alert, ex.getMessage(), true);
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

            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return conn.trashPurge("ALL");
                }

                @Override
                protected void done() {
                    try {
                        String resp = get();
                        setAlert(alert, resp, resp.startsWith("ERROR"));
                        refresh();
                    } catch (Exception ex) {
                        setAlert(alert, ex.getMessage(), true);
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

        static final class TrashTableModel extends AbstractTableModel {
            private final String[] cols = {"Fichier", "Taille", "Supprimé le", " ", "Restaurer", "Supprimer"};
            private final List<TrashRow> rows = new ArrayList<TrashRow>();

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

            @Override
            public int getRowCount() {
                return rows.size();
            }

            @Override
            public int getColumnCount() {
                return cols.length;
            }

            @Override
            public String getColumnName(int column) {
                return cols[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                TrashRow r = rows.get(rowIndex);
                if (columnIndex == 0) return r.original;
                if (columnIndex == 1) return r.size + " o";
                if (columnIndex == 2) return r.deletedAt;
                if (columnIndex == 4) return "Restaurer";
                if (columnIndex == 5) return "Supprimer";
                return "";
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
            private final JLabel alert = alertLabel();
            private final JPanel wrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));

            PartagesUsersView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JPanel topbar = new JPanel(new BorderLayout());
                topbar.setOpaque(false);
                JLabel brand = new JLabel("SmartDrive - Partages");
                brand.setFont(brand.getFont().deriveFont(Font.BOLD, 14f));
                topbar.add(brand, BorderLayout.WEST);

                BootstrapButton demandes = new BootstrapButton("Demandes reçues", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
                demandes.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        localCards.show(localRoot, "demandes");
                        demandesView.onShow();
                    }
                });

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                actions.setOpaque(false);
                actions.add(demandes);
                topbar.add(actions, BorderLayout.EAST);

                JLabel h = new JLabel("Partages");
                h.setForeground(BootstrapColors.PRIMARY);
                h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(topbar);
                top.add(Box.createVerticalStrut(10));
                top.add(h);
                top.add(Box.createVerticalStrut(10));
                top.add(alert);

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
                clearAlert(alert);

                SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                    @Override
                    protected List<String> doInBackground() throws Exception {
                        return conn.listUsers();
                    }

                    @Override
                    protected void done() {
                        try {
                            renderUsers(get());
                        } catch (Exception ex) {
                            setAlert(alert, ex.getMessage(), true);
                            renderUsers(Collections.<String>emptyList());
                        }
                    }
                };
                worker.execute();
            }

            private void renderUsers(List<String> users) {
                wrap.removeAll();
                if (users != null) {
                    for (final String u : users) {
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
                        view.addActionListener(new AbstractAction() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                filesView.setOwner(u);
                                localCards.show(localRoot, "files");
                                filesView.onShow();
                            }
                        });

                        body.add(view);
                        card.add(body, BorderLayout.CENTER);
                        wrap.add(card);
                    }
                }

                if (wrap.getComponentCount() == 0) {
                    JLabel empty = new JLabel("Aucun autre utilisateur.");
                    empty.setForeground(BootstrapColors.TEXT_MUTED);
                    wrap.add(empty);
                }

                wrap.revalidate();
                wrap.repaint();
            }
        }

        final class PartagesFilesView extends BasePage {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();
            private final JPanel wrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
            private String owner;

            PartagesFilesView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JPanel navbar = new JPanel(new BorderLayout());
                navbar.setOpaque(false);

                final JLabel h = new JLabel("Fichiers");
                h.setForeground(BootstrapColors.PRIMARY);
                h.setFont(h.getFont().deriveFont(Font.BOLD, 22f));

                BootstrapButton back = new BootstrapButton("Retour", BootstrapButton.Variant.OUTLINE_SECONDARY, true);
                back.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        localCards.show(localRoot, "users");
                        usersView.onShow();
                    }
                });

                navbar.add(h, BorderLayout.WEST);
                navbar.add(back, BorderLayout.EAST);

                addPropertyChangeListener("owner", new java.beans.PropertyChangeListener() {
                    @Override
                    public void propertyChange(java.beans.PropertyChangeEvent evt) {
                        h.setText("Fichiers de " + owner);
                    }
                });

                wrap.setOpaque(false);
                JScrollPane sp = new JScrollPane(wrap);
                sp.setBorder(BorderFactory.createEmptyBorder());
                sp.getViewport().setBackground(BootstrapColors.BG_LIGHT);

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(navbar);
                top.add(Box.createVerticalStrut(10));
                top.add(alert);

                root.add(top, BorderLayout.NORTH);
                root.add(sp, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            void setOwner(String owner) {
                this.owner = owner;
                firePropertyChange("owner", false, true);
            }

            @Override
            void onShow() {
                if (conn == null || owner == null) return;
                clearAlert(alert);

                SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                    @Override
                    protected List<String> doInBackground() throws Exception {
                        return conn.listSharedFiles(owner);
                    }

                    @Override
                    protected void done() {
                        try {
                            render(get());
                        } catch (Exception ex) {
                            setAlert(alert, ex.getMessage(), true);
                            render(Collections.<String>emptyList());
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
                        final String filename = p[0];
                        final String size = p[1];
                        final String status = p[2];

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

                            voir.addActionListener(new AbstractAction() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                    sharedView(filename);
                                }
                            });
                            dl.addActionListener(new AbstractAction() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                    sharedDownload(filename);
                                }
                            });

                            actions.add(voir);
                            actions.add(dl);
                            body.add(actions);
                        } else if ("pending".equalsIgnoreCase(status)) {
                            BootstrapButton pending = new BootstrapButton("Demande en attente", BootstrapButton.Variant.WARNING, true);
                            pending.setEnabled(false);
                            body.add(pending);
                        } else {
                            BootstrapButton ask = new BootstrapButton("Demander lecture", BootstrapButton.Variant.INFO, true);
                            ask.addActionListener(new AbstractAction() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                    request(filename);
                                }
                            });
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

            private void request(final String filename) {
                SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
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

            private void sharedDownload(final String filename) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Enregistrer sous...");
                chooser.setSelectedFile(new File(filename));
                int res = chooser.showSaveDialog(this);
                if (res != JFileChooser.APPROVE_OPTION) return;
                final File dest = chooser.getSelectedFile();
                if (dest == null) return;

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
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

            private void sharedView(final String filename) {
                SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {
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
                back.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        localCards.show(localRoot, "users");
                        usersView.onShow();
                    }
                });

                JLabel title = new JLabel("Demandes reçues");
                title.setForeground(BootstrapColors.TEXT_MUTED);

                navbar.add(back, BorderLayout.WEST);
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
                card.add(new JScrollPane(table), BorderLayout.CENTER);

                root.add(navbar, BorderLayout.NORTH);
                root.add(card, BorderLayout.CENTER);

                add(root, BorderLayout.CENTER);
            }

            @Override
            void onShow() {
                if (conn == null) return;

                SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
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
                            model.setRows(Collections.<String>emptyList());
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

            private void respond(final String requester, final String file, final String action) {
                SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
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

            static final class RequestsTableModel extends AbstractTableModel {
                private final String[] cols = {"Demandeur", "Fichier", "Statut", " ", "Approuver", "Refuser"};
                private final List<RequestsRow> rows = new ArrayList<RequestsRow>();

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

                @Override
                public int getRowCount() {
                    return rows.size();
                }

                @Override
                public int getColumnCount() {
                    return cols.length;
                }

                @Override
                public String getColumnName(int column) {
                    return cols[column];
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    RequestsRow r = rows.get(rowIndex);
                    if (columnIndex == 0) return r.requester;
                    if (columnIndex == 1) return r.file;
                    if (columnIndex == 2) return r.status;
                    if (columnIndex == 4) return "Approuver";
                    if (columnIndex == 5) return "Refuser";
                    return "";
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
            usage.setForeground(BootstrapColors.TEXT_MUTED);

            progress.setStringPainted(true);

            body.add(h);
            body.add(Box.createVerticalStrut(6));
            body.add(usage);
            body.add(Box.createVerticalStrut(16));
            body.add(progress);

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

            SwingWorker<StockageData, Void> worker = new SwingWorker<StockageData, Void>() {
                @Override
                protected StockageData doInBackground() throws Exception {
                    List<SmartDriveConnection.RemoteFileRow> rows = conn.listFiles();
                    if (rows == null) rows = Collections.emptyList();

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

    static final class AdminPage extends BasePage {
        private enum AdminViewId {
            DASHBOARD,
            USERS,
            STORAGE,
            LOGS,
            MONITOR,
            USER_FILES
        }

        private final JPanel root = new JPanel(new BorderLayout());
        private final AdminTopBar topBar = new AdminTopBar();

        private final CardLayout cards = new CardLayout();
        private final JPanel content = new JPanel(cards);

        private final DashboardView dashboardView = new DashboardView();
        private final UsersView usersView = new UsersView();
        private final StorageView storageView = new StorageView();
        private final LogsView logsView = new LogsView();
        private final MonitorView monitorView = new MonitorView();
        private final UserFilesView userFilesView = new UserFilesView();

        private AdminViewId current = AdminViewId.DASHBOARD;

        AdminPage() {
            root.setOpaque(false);
            root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            content.setOpaque(false);
            content.setBackground(BootstrapColors.BG_LIGHT);

            content.add(dashboardView, AdminViewId.DASHBOARD.name());
            content.add(usersView, AdminViewId.USERS.name());
            content.add(storageView, AdminViewId.STORAGE.name());
            content.add(logsView, AdminViewId.LOGS.name());
            content.add(monitorView, AdminViewId.MONITOR.name());
            content.add(userFilesView, AdminViewId.USER_FILES.name());

            root.add(topBar, BorderLayout.NORTH);
            root.add(content, BorderLayout.CENTER);

            add(root, BorderLayout.CENTER);

            topBar.btnDashboard.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(AdminViewId.DASHBOARD, null);
                }
            });
            topBar.btnUsers.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(AdminViewId.USERS, null);
                }
            });
            topBar.btnStorage.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(AdminViewId.STORAGE, null);
                }
            });
            topBar.btnLogs.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(AdminViewId.LOGS, null);
                }
            });
            topBar.btnMonitor.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    navigate(AdminViewId.MONITOR, null);
                }
            });
        }

        @Override
        void setSession(SmartDriveConnection conn, String username) {
            super.setSession(conn, username);
            dashboardView.setSession(conn, username);
            usersView.setSession(conn, username);
            storageView.setSession(conn, username);
            logsView.setSession(conn, username);
            monitorView.setSession(conn, username);
            userFilesView.setSession(conn, username);
        }

        @Override
        void onShow() {
            if (conn == null) return;
            if (!conn.isAdmin()) {
                JPanel p = new JPanel(new BorderLayout());
                p.setOpaque(false);
                p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
                CardPanel card = new CardPanel();
                JLabel l = new JLabel("Accès admin refusé");
                l.setForeground(BootstrapColors.DANGER);
                l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
                card.add(l, BorderLayout.CENTER);
                p.add(card, BorderLayout.NORTH);
                content.add(p, "not_admin");
                cards.show(content, "not_admin");
                topBar.setSubtitle("Accès admin refusé");
                return;
            }
            navigate(AdminViewId.DASHBOARD, null);
        }

        private void navigate(AdminViewId id, String ownerForUserFiles) {
            current = id;
            cards.show(content, id.name());

            if (id == AdminViewId.DASHBOARD) {
                topBar.setModeDashboard();
                dashboardView.onShow();
            } else if (id == AdminViewId.USERS) {
                topBar.setModeSection("Gestion utilisateurs");
                usersView.onShow();
            } else if (id == AdminViewId.STORAGE) {
                topBar.setModeSection("Gestion du stockage");
                storageView.onShow();
            } else if (id == AdminViewId.LOGS) {
                topBar.setModeSection("Logs & Audit");
                logsView.onShow();
            } else if (id == AdminViewId.MONITOR) {
                topBar.setModeSection("Monitoring système");
                monitorView.onShow();
            } else if (id == AdminViewId.USER_FILES) {
                topBar.setModeSection("Fichiers utilisateur");
                userFilesView.onShow(ownerForUserFiles);
            }
        }

        private static Map<String, String> parseKv(String line) {
            Map<String, String> map = new HashMap<>();
            if (line == null) return map;
            String[] parts = line.split(";");
            for (String p : parts) {
                if (p == null) continue;
                int idx = p.indexOf('=');
                if (idx <= 0) continue;
                String k = p.substring(0, idx).trim();
                String v = p.substring(idx + 1).trim();
                if (!k.isEmpty()) map.put(k, v);
            }
            return map;
        }

        private static String kv(Map<String, String> map, String key, String def) {
            if (map == null) return def;
            String v = map.get(key);
            return (v == null || v.isBlank()) ? def : v;
        }

        private static String safe(String s) {
            return s == null ? "" : s;
        }

        private static boolean isTextLikeFilename(String name) {
            if (name == null) return false;
            String n = name.toLowerCase();
            return n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".log") || n.endsWith(".csv")
                    || n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".yml") || n.endsWith(".yaml")
                    || n.endsWith(".java") || n.endsWith(".js") || n.endsWith(".css") || n.endsWith(".html");
        }

        private final class AdminTopBar extends JPanel {
            private final JLabel brand = new JLabel("Admin");
            private final JLabel subtitle = new JLabel(" ");

            final BootstrapButton btnDashboard = new BootstrapButton("Dashboard", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
            final BootstrapButton btnUsers = new BootstrapButton("Utilisateurs", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
            final BootstrapButton btnStorage = new BootstrapButton("Stockage", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
            final BootstrapButton btnLogs = new BootstrapButton("Logs", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
            final BootstrapButton btnMonitor = new BootstrapButton("Monitoring", BootstrapButton.Variant.OUTLINE_PRIMARY, true);

            AdminTopBar() {
                super(new BorderLayout());
                setBackground(BootstrapColors.WHITE);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BootstrapColors.BORDER));

                brand.setFont(brand.getFont().deriveFont(Font.BOLD, 14f));
                brand.setForeground(BootstrapColors.NAV_FG);

                subtitle.setForeground(BootstrapColors.TEXT_MUTED);

                JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
                left.setOpaque(false);
                left.add(brand);
                left.add(subtitle);

                JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
                right.setOpaque(false);
                right.add(btnDashboard);
                right.add(btnUsers);
                right.add(btnStorage);
                right.add(btnLogs);
                right.add(btnMonitor);

                add(left, BorderLayout.WEST);
                add(right, BorderLayout.EAST);
            }

            void setSubtitle(String text) {
                subtitle.setText(text == null ? "" : text);
            }

            void setModeDashboard() {
                brand.setText("Admin Panel");
                setSubtitle("");
            }

            void setModeSection(String sub) {
                brand.setText("Admin");
                setSubtitle(sub);
            }
        }

        private abstract class AdminSubView extends JPanel {
            SmartDriveConnection conn;
            String username;

            AdminSubView() {
                super(new BorderLayout());
                setOpaque(false);
            }

            void setSession(SmartDriveConnection conn, String username) {
                this.conn = conn;
                this.username = username;
            }

            abstract void onShow();
        }

        private final class DashboardView extends AdminSubView {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();

            private final JLabel storageUsed = new JLabel("?");
            private final JLabel storageFiles = new JLabel("Fichiers: ?");
            private final JLabel storageSlave = new JLabel("Slave: ?");

            private final JLabel monCpu = new JLabel("CPU: ?%");
            private final JLabel monRam = new JLabel("RAM used: ?");
            private final JLabel monDisk = new JLabel("Disk used: ?");

            private final AuditTableModel auditModel = new AuditTableModel();
            private final JTable auditTable = new JTable(auditModel);

            DashboardView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(alert);
                top.add(Box.createVerticalStrut(10));

                JPanel cardsRow = new JPanel(new GridLayout(1, 2, 12, 12));
                cardsRow.setOpaque(false);

                CardPanel storageCard = new CardPanel();
                storageCard.setLayout(new BorderLayout());
                JPanel sBody = new JPanel();
                sBody.setOpaque(false);
                sBody.setLayout(new BoxLayout(sBody, BoxLayout.Y_AXIS));
                JLabel sMuted = new JLabel("Total storage utilisé");
                sMuted.setForeground(BootstrapColors.TEXT_MUTED);
                storageUsed.setFont(storageUsed.getFont().deriveFont(Font.BOLD, 18f));
                storageFiles.setForeground(BootstrapColors.TEXT_MUTED);
                storageSlave.setForeground(BootstrapColors.TEXT_MUTED);
                sBody.add(sMuted);
                sBody.add(Box.createVerticalStrut(6));
                sBody.add(storageUsed);
                sBody.add(Box.createVerticalStrut(8));
                sBody.add(storageFiles);
                sBody.add(Box.createVerticalStrut(2));
                sBody.add(storageSlave);
                storageCard.add(sBody, BorderLayout.CENTER);

                CardPanel monitorCard = new CardPanel();
                monitorCard.setLayout(new BorderLayout());
                JPanel mBody = new JPanel();
                mBody.setOpaque(false);
                mBody.setLayout(new BoxLayout(mBody, BoxLayout.Y_AXIS));
                JLabel mMuted = new JLabel("Monitoring système");
                mMuted.setForeground(BootstrapColors.TEXT_MUTED);
                monCpu.setFont(monCpu.getFont().deriveFont(Font.BOLD, 18f));
                monRam.setForeground(BootstrapColors.TEXT_MUTED);
                monDisk.setForeground(BootstrapColors.TEXT_MUTED);
                mBody.add(mMuted);
                mBody.add(Box.createVerticalStrut(6));
                mBody.add(monCpu);
                mBody.add(Box.createVerticalStrut(8));
                mBody.add(monRam);
                mBody.add(Box.createVerticalStrut(2));
                mBody.add(monDisk);
                monitorCard.add(mBody, BorderLayout.CENTER);

                cardsRow.add(storageCard);
                cardsRow.add(monitorCard);

                CardPanel auditCard = new CardPanel();
                auditCard.setLayout(new BorderLayout());

                JLabel auditHeader = new JLabel("Audit (dernières actions)");
                auditHeader.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                auditHeader.setFont(auditHeader.getFont().deriveFont(Font.BOLD, 13f));

                auditTable.setRowHeight(26);
                auditTable.setFillsViewportHeight(true);
                auditTable.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
                auditTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                            boolean hasFocus, int row, int column) {
                        java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if (c instanceof JComponent) {
                            ((JComponent) c).setToolTipText(value == null ? null : String.valueOf(value));
                        }
                        return c;
                    }
                });

                JScrollPane sp = new JScrollPane(auditTable);
                sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

                JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
                footer.setOpaque(false);
                BootstrapButton seeAll = new BootstrapButton("Voir tout", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
                seeAll.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        navigate(AdminViewId.LOGS, null);
                    }
                });
                footer.add(seeAll);

                auditCard.add(auditHeader, BorderLayout.NORTH);
                auditCard.add(sp, BorderLayout.CENTER);
                auditCard.add(footer, BorderLayout.SOUTH);

                JPanel center = new JPanel();
                center.setOpaque(false);
                center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
                center.add(cardsRow);
                center.add(Box.createVerticalStrut(14));
                center.add(auditCard);

                root.add(top, BorderLayout.NORTH);
                root.add(center, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            @Override
            void onShow() {
                if (conn == null) return;
                clearAlert(alert);

                SwingWorker<DashboardData, Void> worker = new SwingWorker<DashboardData, Void>() {
                    @Override
                    protected DashboardData doInBackground() throws Exception {
                        String storageLine = conn.adminStorage();
                        String monitorLine = conn.adminMonitor();
                        List<String> audit = conn.adminLogs(12);
                        return new DashboardData(storageLine, monitorLine, audit);
                    }

                    @Override
                    protected void done() {
                        try {
                            DashboardData d = get();
                            Map<String, String> storage = parseKv(d.storageLine);
                            Map<String, String> monitor = parseKv(d.monitorLine);

                            storageUsed.setText(kv(storage, "totalUsedBytes", "?") + " bytes");
                            storageFiles.setText("Fichiers: " + kv(storage, "totalFiles", "?"));
                            storageSlave.setText("Slave: " + kv(storage, "slaveStatus", "?"));

                            monCpu.setText("CPU: " + kv(monitor, "cpuPercent", "?") + "%");
                            monRam.setText("RAM used: " + kv(monitor, "ramUsedBytes", "?"));
                            monDisk.setText("Disk used: " + kv(monitor, "diskUsedBytes", "?"));

                            auditModel.setRows(d.auditRows);
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                worker.execute();
            }

            private final class DashboardData {
                final String storageLine;
                final String monitorLine;
                final List<String> auditRows;

                DashboardData(String storageLine, String monitorLine, List<String> auditRows) {
                    this.storageLine = storageLine;
                    this.monitorLine = monitorLine;
                    this.auditRows = auditRows == null ? Collections.<String>emptyList() : auditRows;
                }
            }
        }

        private final class UsersView extends AdminSubView {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();

            private final AdminUsersTableModel model = new AdminUsersTableModel();
            private final JTable table = new JTable(model);

            UsersView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JLabel h = new JLabel("Utilisateurs");
                h.setFont(h.getFont().deriveFont(Font.BOLD, 18f));

                JPanel head = new JPanel(new BorderLayout());
                head.setOpaque(false);
                head.add(h, BorderLayout.WEST);

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(alert);
                top.add(Box.createVerticalStrut(10));
                top.add(head);
                top.add(Box.createVerticalStrut(10));

                CardPanel card = new CardPanel();
                card.setLayout(new BorderLayout());

                table.setRowHeight(28);
                table.setFillsViewportHeight(true);
                table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
                table.setAutoCreateRowSorter(true);

                JScrollPane sp = new JScrollPane(table);
                sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                card.add(sp, BorderLayout.CENTER);

                Action blockAction = new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        int viewRow;
                        try {
                            viewRow = Integer.parseInt(e.getActionCommand());
                        } catch (Exception ex) {
                            return;
                        }
                        int row = table.convertRowIndexToModel(viewRow);
                        final AdminUserRow r = model.getRow(row);
                        if (r == null) return;
                        if (username != null && r.user.equalsIgnoreCase(username)) {
                            showInfoDialog("Admin", "Impossible d'appliquer cette action sur soi-même");
                            return;
                        }

                        clearAlert(alert);
                        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
                            @Override
                            protected String doInBackground() throws Exception {
                                return conn.adminBlock(r.user, !r.blocked);
                            }

                            @Override
                            protected void done() {
                                try {
                                    String resp = get();
                                    setAlert(alert, "Résultat: " + resp, false);
                                    onShow();
                                } catch (Exception ex) {
                                    setAlert(alert, "Erreur: " + ex.getMessage(), true);
                                }
                            }
                        };
                        w.execute();
                    }
                };

                Action quotaAction = new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        int viewRow;
                        try {
                            viewRow = Integer.parseInt(e.getActionCommand());
                        } catch (Exception ex) {
                            return;
                        }
                        int row = table.convertRowIndexToModel(viewRow);
                        final AdminUserRow r = model.getRow(row);
                        if (r == null) return;

                        long quota;
                        try {
                            Object v = model.getValueAt(row, 3);
                            quota = Long.parseLong(String.valueOf(v).trim());
                            if (quota < 0) throw new IllegalArgumentException();
                        } catch (Exception ex) {
                            setAlert(alert, "Quota invalide", true);
                            return;
                        }

                        clearAlert(alert);
                        final long q = quota;
                        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
                            @Override
                            protected String doInBackground() throws Exception {
                                return conn.adminSetQuota(r.user, q);
                            }

                            @Override
                            protected void done() {
                                try {
                                    String resp = get();
                                    setAlert(alert, "Résultat: " + resp, false);
                                    onShow();
                                } catch (Exception ex) {
                                    setAlert(alert, "Erreur: " + ex.getMessage(), true);
                                }
                            }
                        };
                        w.execute();
                    }
                };

                Action deleteAction = new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        int viewRow;
                        try {
                            viewRow = Integer.parseInt(e.getActionCommand());
                        } catch (Exception ex) {
                            return;
                        }
                        int row = table.convertRowIndexToModel(viewRow);
                        final AdminUserRow r = model.getRow(row);
                        if (r == null) return;
                        if (username != null && r.user.equalsIgnoreCase(username)) {
                            showInfoDialog("Admin", "Impossible de supprimer son propre compte");
                            return;
                        }

                        int ok = JOptionPane.showConfirmDialog(UsersView.this,
                                "Supprimer définitivement " + r.user + " ?",
                                "Confirmation",
                                JOptionPane.OK_CANCEL_OPTION);
                        if (ok != JOptionPane.OK_OPTION) return;

                        clearAlert(alert);
                        SwingWorker<String, Void> w = new SwingWorker<String, Void>() {
                            @Override
                            protected String doInBackground() throws Exception {
                                return conn.adminDelete(r.user);
                            }

                            @Override
                            protected void done() {
                                try {
                                    String resp = get();
                                    setAlert(alert, "Résultat: " + resp, false);
                                    onShow();
                                } catch (Exception ex) {
                                    setAlert(alert, "Erreur: " + ex.getMessage(), true);
                                }
                            }
                        };
                        w.execute();
                    }
                };

                Action filesAction = new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        int viewRow;
                        try {
                            viewRow = Integer.parseInt(e.getActionCommand());
                        } catch (Exception ex) {
                            return;
                        }
                        int row = table.convertRowIndexToModel(viewRow);
                        final AdminUserRow r = model.getRow(row);
                        if (r == null) return;
                        navigate(AdminViewId.USER_FILES, r.user);
                    }
                };

                new TableButtonColumn(table, blockAction, 4);
                new TableButtonColumn(table, quotaAction, 5);
                new TableButtonColumn(table, deleteAction, 6);
                new TableButtonColumn(table, filesAction, 7);

                card.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

                root.add(top, BorderLayout.NORTH);
                root.add(card, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            @Override
            void onShow() {
                if (conn == null) return;
                clearAlert(alert);

                SwingWorker<List<AdminUserRow>, Void> worker = new SwingWorker<List<AdminUserRow>, Void>() {
                    @Override
                    protected List<AdminUserRow> doInBackground() throws Exception {
                        List<String> rows = conn.adminUsers();
                        List<AdminUserRow> out = new ArrayList<>();
                        if (rows != null) {
                            for (String r : rows) {
                                if (r == null) continue;
                                String[] p = r.split(";", 4);
                                if (p.length < 4) continue;
                                String u = p[0].trim();
                                boolean isAdmin = Boolean.parseBoolean(p[1].trim());
                                boolean blocked = Boolean.parseBoolean(p[2].trim());
                                long quota = 0;
                                try {
                                    quota = Long.parseLong(p[3].trim());
                                } catch (Exception ignored) {
                                }
                                out.add(new AdminUserRow(u, isAdmin, blocked, quota));
                            }
                        }
                        return out;
                    }

                    @Override
                    protected void done() {
                        try {
                            List<AdminUserRow> rows = get();
                            model.setRows(rows);
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                worker.execute();
            }

            private final class AdminUsersTableModel extends AbstractTableModel {
                private final String[] cols = new String[] { "User", "Admin", "Blocked", "Quota (bytes)", "", "", "", "" };
                private final List<AdminUserRow> rows = new ArrayList<>();

                void setRows(List<AdminUserRow> newRows) {
                    rows.clear();
                    if (newRows != null) rows.addAll(newRows);
                    fireTableDataChanged();
                }

                AdminUserRow getRow(int idx) {
                    if (idx < 0 || idx >= rows.size()) return null;
                    return rows.get(idx);
                }

                @Override
                public int getRowCount() {
                    return rows.size();
                }

                @Override
                public int getColumnCount() {
                    return cols.length;
                }

                @Override
                public String getColumnName(int column) {
                    return cols[column];
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    AdminUserRow r = rows.get(rowIndex);
                    if (columnIndex == 0) return r.user;
                    if (columnIndex == 1) return r.isAdmin ? "Oui" : "Non";
                    if (columnIndex == 2) return r.blocked ? "Bloqué" : "Actif";
                    if (columnIndex == 3) return String.valueOf(r.quota);
                    if (columnIndex == 4) return r.blocked ? "Débloquer" : "Bloquer";
                    if (columnIndex == 5) return "Quota";
                    if (columnIndex == 6) return "Supprimer";
                    if (columnIndex == 7) return "Fichiers";
                    return "";
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex == 3 || columnIndex == 4 || columnIndex == 5 || columnIndex == 6 || columnIndex == 7;
                }

                @Override
                public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    if (columnIndex != 3) return;
                    AdminUserRow r = rows.get(rowIndex);
                    try {
                        long q = Long.parseLong(String.valueOf(aValue).trim());
                        r.quota = Math.max(0, q);
                    } catch (Exception ignored) {
                    }
                    fireTableCellUpdated(rowIndex, 3);
                }
            }

            private final class AdminUserRow {
                final String user;
                final boolean isAdmin;
                boolean blocked;
                long quota;

                AdminUserRow(String user, boolean isAdmin, boolean blocked, long quota) {
                    this.user = user;
                    this.isAdmin = isAdmin;
                    this.blocked = blocked;
                    this.quota = quota;
                }
            }
        }

        private final class StorageView extends AdminSubView {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();

            private final JLabel totalUsed = new JLabel("?");
            private final JLabel totalFiles = new JLabel("?");
            private final JLabel replication = new JLabel("?");

            private final JLabel slaveHost = new JLabel("?");
            private final JLabel slavePort = new JLabel("?");
            private final JLabel slaveStatus = new JLabel("?");

            StorageView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(alert);
                top.add(Box.createVerticalStrut(10));

                JPanel cardsRow = new JPanel(new GridLayout(1, 3, 12, 12));
                cardsRow.setOpaque(false);

                cardsRow.add(statCard("Total utilisé", totalUsed));
                cardsRow.add(statCard("Fichiers", totalFiles));
                cardsRow.add(statCard("Réplication", replication));

                CardPanel slaveCard = new CardPanel();
                slaveCard.setLayout(new BorderLayout());
                JLabel header = new JLabel("Slave status");
                header.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));

                JPanel body = new JPanel(new GridLayout(1, 3, 12, 12));
                body.setOpaque(false);
                body.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                body.add(kvBlock("Host", slaveHost));
                body.add(kvBlock("Port", slavePort));
                body.add(kvBlock("Status", slaveStatus));

                JLabel note = new JLabel("Note: la réplication peut être simulée (script) mais visible pour la sécurité cloud.");
                note.setForeground(BootstrapColors.TEXT_MUTED);
                note.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

                slaveCard.add(header, BorderLayout.NORTH);
                slaveCard.add(body, BorderLayout.CENTER);
                slaveCard.add(note, BorderLayout.SOUTH);

                JPanel center = new JPanel();
                center.setOpaque(false);
                center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
                center.add(cardsRow);
                center.add(Box.createVerticalStrut(14));
                center.add(slaveCard);

                root.add(top, BorderLayout.NORTH);
                root.add(center, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            private CardPanel statCard(String title, JLabel value) {
                CardPanel c = new CardPanel();
                c.setLayout(new BorderLayout());
                JPanel body = new JPanel();
                body.setOpaque(false);
                body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                JLabel muted = new JLabel(title);
                muted.setForeground(BootstrapColors.TEXT_MUTED);
                value.setFont(value.getFont().deriveFont(Font.BOLD, 18f));
                body.add(muted);
                body.add(Box.createVerticalStrut(6));
                body.add(value);
                c.add(body, BorderLayout.CENTER);
                return c;
            }

            private JPanel kvBlock(String k, JLabel v) {
                JPanel p = new JPanel();
                p.setOpaque(false);
                p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
                JLabel kk = new JLabel(k);
                kk.setForeground(BootstrapColors.TEXT_MUTED);
                v.setFont(v.getFont().deriveFont(Font.BOLD, 13f));
                p.add(kk);
                p.add(Box.createVerticalStrut(4));
                p.add(v);
                return p;
            }

            @Override
            void onShow() {
                if (conn == null) return;
                clearAlert(alert);

                SwingWorker<Map<String, String>, Void> worker = new SwingWorker<Map<String, String>, Void>() {
                    @Override
                    protected Map<String, String> doInBackground() throws Exception {
                        return parseKv(conn.adminStorage());
                    }

                    @Override
                    protected void done() {
                        try {
                            Map<String, String> storage = get();
                            totalUsed.setText(kv(storage, "totalUsedBytes", "?") + " bytes");
                            totalFiles.setText(kv(storage, "totalFiles", "?"));
                            replication.setText(kv(storage, "replication", "?"));

                            slaveHost.setText(kv(storage, "slaveHost", "?"));
                            slavePort.setText(kv(storage, "slavePort", "?"));
                            slaveStatus.setText(kv(storage, "slaveStatus", "?"));
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                worker.execute();
            }
        }

        private final class LogsView extends AdminSubView {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();

            private final AuditTableModel auditModel = new AuditTableModel();
            private final JTable table = new JTable(auditModel);

            LogsView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(alert);
                top.add(Box.createVerticalStrut(10));

                CardPanel card = new CardPanel();
                card.setLayout(new BorderLayout());

                JLabel header = new JLabel("Historique (upload / download / delete / share)");
                header.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));

                table.setRowHeight(26);
                table.setFillsViewportHeight(true);
                table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
                table.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                            boolean hasFocus, int row, int column) {
                        java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if (c instanceof JComponent) {
                            ((JComponent) c).setToolTipText(value == null ? null : String.valueOf(value));
                        }
                        return c;
                    }
                });

                JScrollPane sp = new JScrollPane(table);
                sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

                card.add(header, BorderLayout.NORTH);
                card.add(sp, BorderLayout.CENTER);

                root.add(top, BorderLayout.NORTH);
                root.add(card, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            @Override
            void onShow() {
                if (conn == null) return;
                clearAlert(alert);

                SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
                    @Override
                    protected List<String> doInBackground() throws Exception {
                        return conn.adminLogs(200);
                    }

                    @Override
                    protected void done() {
                        try {
                            auditModel.setRows(get());
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                worker.execute();
            }
        }

        private final class MonitorView extends AdminSubView {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();

            private final JLabel cpu = new JLabel("?");
            private final JLabel ram = new JLabel("?");
            private final JLabel disk = new JLabel("?");
            private final JLabel traffic = new JLabel("?");

            MonitorView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(alert);
                top.add(Box.createVerticalStrut(10));

                JPanel cardsRow = new JPanel(new GridLayout(1, 4, 12, 12));
                cardsRow.setOpaque(false);

                cardsRow.add(statCard("CPU", cpu, true));
                cardsRow.add(statCard("RAM utilisée", ram, false));
                cardsRow.add(statCard("Disque utilisé", disk, false));
                cardsRow.add(statCard("Trafic (simulé)", traffic, true));

                JLabel note = new JLabel("CPU peut être -1 si non disponible (fallback). Trafic est simulé (accepté pour la démo). ");
                note.setForeground(BootstrapColors.TEXT_MUTED);
                note.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

                JPanel center = new JPanel();
                center.setOpaque(false);
                center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
                center.add(cardsRow);
                center.add(note);

                root.add(top, BorderLayout.NORTH);
                root.add(center, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            private CardPanel statCard(String title, JLabel value, boolean big) {
                CardPanel c = new CardPanel();
                c.setLayout(new BorderLayout());
                JPanel body = new JPanel();
                body.setOpaque(false);
                body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
                JLabel muted = new JLabel(title);
                muted.setForeground(BootstrapColors.TEXT_MUTED);
                value.setFont(value.getFont().deriveFont(Font.BOLD, big ? 18f : 13f));
                body.add(muted);
                body.add(Box.createVerticalStrut(6));
                body.add(value);
                c.add(body, BorderLayout.CENTER);
                return c;
            }

            @Override
            void onShow() {
                if (conn == null) return;
                clearAlert(alert);

                SwingWorker<Map<String, String>, Void> worker = new SwingWorker<Map<String, String>, Void>() {
                    @Override
                    protected Map<String, String> doInBackground() throws Exception {
                        return parseKv(conn.adminMonitor());
                    }

                    @Override
                    protected void done() {
                        try {
                            Map<String, String> m = get();
                            cpu.setText(kv(m, "cpuPercent", "?") + "%");
                            ram.setText(kv(m, "ramUsedBytes", "?") + " bytes");
                            disk.setText(kv(m, "diskUsedBytes", "?") + " bytes");
                            traffic.setText(kv(m, "trafficKbps", "?") + " Kbps");
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                worker.execute();
            }
        }

        private final class UserFilesView extends AdminSubView {
            private final JPanel root = new JPanel(new BorderLayout());
            private final JLabel alert = alertLabel();

            private final JTextField ownerField = new JTextField(16);
            private final BootstrapButton loadButton = new BootstrapButton("Charger", BootstrapButton.Variant.OUTLINE_PRIMARY, true);
            private final BootstrapButton backButton = new BootstrapButton("Retour", BootstrapButton.Variant.OUTLINE_SECONDARY, true);
            private final JLabel title = new JLabel("Fichiers de ");

            private final UserFilesTableModel model = new UserFilesTableModel();
            private final JTable table = new JTable(model);

            UserFilesView() {
                root.setOpaque(false);
                root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

                title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

                JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                form.setOpaque(false);
                form.add(new JLabel("Owner:"));
                form.add(ownerField);
                form.add(loadButton);
                form.add(Box.createHorizontalStrut(16));
                form.add(backButton);

                JPanel top = new JPanel();
                top.setOpaque(false);
                top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
                top.add(alert);
                top.add(Box.createVerticalStrut(10));
                top.add(title);
                top.add(Box.createVerticalStrut(10));
                top.add(form);
                top.add(Box.createVerticalStrut(10));

                CardPanel card = new CardPanel();
                card.setLayout(new BorderLayout());

                table.setRowHeight(28);
                table.setFillsViewportHeight(true);
                table.setDefaultRenderer(Object.class, new StripedTableCellRenderer());
                JScrollPane sp = new JScrollPane(table);
                sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                card.add(sp, BorderLayout.CENTER);

                Action viewAction = new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        int viewRow;
                        try {
                            viewRow = Integer.parseInt(e.getActionCommand());
                        } catch (Exception ex) {
                            return;
                        }
                        int row = table.convertRowIndexToModel(viewRow);
                        UserFileRow r = model.getRow(row);
                        if (r == null) return;
                        viewFile(r.owner, r.name);
                    }
                };
                Action dlAction = new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        int viewRow;
                        try {
                            viewRow = Integer.parseInt(e.getActionCommand());
                        } catch (Exception ex) {
                            return;
                        }
                        int row = table.convertRowIndexToModel(viewRow);
                        UserFileRow r = model.getRow(row);
                        if (r == null) return;
                        downloadFile(r.owner, r.name);
                    }
                };
                new TableButtonColumn(table, viewAction, 1);
                new TableButtonColumn(table, dlAction, 2);

                loadButton.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        onShow(ownerField.getText().trim());
                    }
                });
                ownerField.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        onShow(ownerField.getText().trim());
                    }
                });
                backButton.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        navigate(AdminViewId.USERS, null);
                    }
                });

                root.add(top, BorderLayout.NORTH);
                root.add(card, BorderLayout.CENTER);
                add(root, BorderLayout.CENTER);
            }

            @Override
            void onShow() {
                onShow(ownerField.getText().trim());
            }

            void onShow(String owner) {
                if (conn == null) return;
                clearAlert(alert);

                String o = safe(owner).trim();
                ownerField.setText(o);
                title.setText("Fichiers de " + o);

                if (o.isBlank()) {
                    setAlert(alert, "Utilisateur non spécifié.", true);
                    model.setRows(Collections.<UserFileRow>emptyList());
                    return;
                }

                SwingWorker<List<UserFileRow>, Void> worker = new SwingWorker<List<UserFileRow>, Void>() {
                    @Override
                    protected List<UserFileRow> doInBackground() throws Exception {
                        List<String> rows = conn.adminListUserFiles(o);
                        List<UserFileRow> out = new ArrayList<>();
                        if (rows != null) {
                            for (String r : rows) {
                                if (r == null) continue;
                                String fileName = r;
                                int semi = r.indexOf(';');
                                if (semi >= 0) fileName = r.substring(0, semi);
                                fileName = fileName.trim();
                                if (!fileName.isBlank()) out.add(new UserFileRow(o, fileName));
                            }
                        }
                        return out;
                    }

                    @Override
                    protected void done() {
                        try {
                            List<UserFileRow> rows = get();
                            if (rows.isEmpty()) {
                                setAlert(alert, "Aucun fichier trouvé.", true);
                            }
                            model.setRows(rows);
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                            model.setRows(Collections.<UserFileRow>emptyList());
                        }
                    }
                };
                worker.execute();
            }

            private void downloadFile(String owner, String file) {
                if (conn == null) return;
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new File(file));
                int res = fc.showSaveDialog(this);
                if (res != JFileChooser.APPROVE_OPTION) return;
                File dest = fc.getSelectedFile();
                clearAlert(alert);

                SwingWorker<Void, Void> w = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        conn.adminDownloadAs(owner, file, dest);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            setAlert(alert, "Téléchargé: " + dest.getAbsolutePath(), false);
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                w.execute();
            }

            private void viewFile(String owner, String file) {
                if (conn == null) return;

                SwingWorker<File, Void> w = new SwingWorker<File, Void>() {
                    @Override
                    protected File doInBackground() throws Exception {
                        File tmp = File.createTempFile("smartdrive_admin_", "_" + file.replaceAll("[^a-zA-Z0-9._-]", "_"));
                        tmp.deleteOnExit();
                        conn.adminDownloadAs(owner, file, tmp);
                        return tmp;
                    }

                    @Override
                    protected void done() {
                        try {
                            File tmp = get();
                            if (isTextLikeFilename(file) && tmp.length() <= 200_000) {
                                StringBuilder sb = new StringBuilder();
                                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tmp), StandardCharsets.UTF_8))) {
                                    String line;
                                    int lines = 0;
                                    while ((line = br.readLine()) != null) {
                                        sb.append(line).append('\n');
                                        lines++;
                                        if (sb.length() > 200_000 || lines > 5000) break;
                                    }
                                }
                                JTextArea ta = new JTextArea(sb.toString());
                                ta.setEditable(false);
                                ta.setLineWrap(true);
                                ta.setWrapStyleWord(true);
                                JScrollPane sp = new JScrollPane(ta);
                                sp.setPreferredSize(new Dimension(720, 480));
                                JOptionPane.showMessageDialog(UserFilesView.this, sp, "Voir: " + file, JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                showInfoDialog("Voir", "Fichier téléchargé en temporaire: " + tmp.getAbsolutePath());
                            }
                        } catch (Exception ex) {
                            setAlert(alert, "Erreur: " + ex.getMessage(), true);
                        }
                    }
                };
                w.execute();
            }

            private final class UserFilesTableModel extends AbstractTableModel {
                private final String[] cols = new String[] { "Nom", "", "" };
                private final List<UserFileRow> rows = new ArrayList<>();

                void setRows(List<UserFileRow> newRows) {
                    rows.clear();
                    if (newRows != null) rows.addAll(newRows);
                    fireTableDataChanged();
                }

                UserFileRow getRow(int idx) {
                    if (idx < 0 || idx >= rows.size()) return null;
                    return rows.get(idx);
                }

                @Override
                public int getRowCount() {
                    return rows.size();
                }

                @Override
                public int getColumnCount() {
                    return cols.length;
                }

                @Override
                public String getColumnName(int column) {
                    return cols[column];
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    UserFileRow r = rows.get(rowIndex);
                    if (columnIndex == 0) return r.name;
                    if (columnIndex == 1) return "Voir";
                    if (columnIndex == 2) return "Télécharger";
                    return "";
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex == 1 || columnIndex == 2;
                }
            }

            private final class UserFileRow {
                final String owner;
                final String name;

                UserFileRow(String owner, String name) {
                    this.owner = owner;
                    this.name = name;
                }
            }
        }

        private final class AuditTableModel extends AbstractTableModel {
            private final String[] cols = new String[] { "Timestamp", "Actor", "Action", "Détails" };
            private final List<String[]> rows = new ArrayList<>();

            void setRows(List<String> auditRows) {
                rows.clear();
                if (auditRows != null) {
                    for (String r : auditRows) {
                        if (r == null) continue;
                        String[] p = r.split(";", 4);
                        if (p.length < 4) continue;
                        rows.add(new String[] { p[0], p[1], p[2], p[3] });
                    }
                }
                fireTableDataChanged();
            }

            @Override
            public int getRowCount() {
                return rows.size();
            }

            @Override
            public int getColumnCount() {
                return cols.length;
            }

            @Override
            public String getColumnName(int column) {
                return cols[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return rows.get(rowIndex)[columnIndex];
            }
        }
    }
}
