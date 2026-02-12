package desktop.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class NavPillButton extends JToggleButton {

    public NavPillButton(String text) {
        super(text);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(new EmptyBorder(8, 10, 8, 10));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.PLAIN, 13f));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (isSelected()) {
                g2.setColor(BootstrapColors.NAV_ACTIVE_BG);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                setForeground(BootstrapColors.NAV_ACTIVE_FG);
            } else {
                g2.setColor(new Color(0, 0, 0, getModel().isRollover() ? 10 : 0));
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                setForeground(BootstrapColors.NAV_FG);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
