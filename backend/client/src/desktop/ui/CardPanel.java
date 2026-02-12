package desktop.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CardPanel extends JPanel {
    private final int arc;

    public CardPanel() {
        this(12);
    }

    public CardPanel(int arc) {
        super();
        this.arc = arc;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(2, 3, w - 4, h - 4, arc, arc);

            g2.setColor(BootstrapColors.WHITE);
            g2.fillRoundRect(0, 0, w - 4, h - 4, arc, arc);

            g2.setColor(BootstrapColors.BORDER);
            g2.drawRoundRect(0, 0, w - 4, h - 4, arc, arc);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
