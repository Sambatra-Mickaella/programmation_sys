package desktop.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class BootstrapButton extends JButton {

    public enum Variant {
        PRIMARY,
        OUTLINE_PRIMARY,
        OUTLINE_SECONDARY,
        OUTLINE_DANGER,
        SUCCESS,
        WARNING,
        INFO,
        SECONDARY,
        DANGER
    }

    private final Variant variant;
    private final boolean small;

    public BootstrapButton(String text, Variant variant, boolean small) {
        super(text);
        this.variant = variant;
        this.small = small;
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(small ? 5 : 8, small ? 10 : 14, small ? 5 : 8, small ? 10 : 14));
        setFont(getFont().deriveFont(Font.PLAIN, small ? 12f : 13f));
        setOpaque(true);
        setContentAreaFilled(true);
        setBorderPainted(true);
        applyVariant();
    }

    private void applyVariant() {
        Color fg = Color.WHITE;
        Color bg = BootstrapColors.PRIMARY;
        Color border = BootstrapColors.PRIMARY;
        boolean filled = true;

        switch (variant) {
            case PRIMARY -> {
                fg = Color.WHITE;
                bg = BootstrapColors.PRIMARY;
                border = BootstrapColors.PRIMARY;
                filled = true;
            }
            case SECONDARY -> {
                fg = Color.WHITE;
                bg = BootstrapColors.SECONDARY;
                border = BootstrapColors.SECONDARY;
                filled = true;
            }
            case SUCCESS -> {
                fg = Color.WHITE;
                bg = BootstrapColors.SUCCESS;
                border = BootstrapColors.SUCCESS;
                filled = true;
            }
            case INFO -> {
                fg = new Color(0x052C65);
                bg = BootstrapColors.INFO;
                border = BootstrapColors.INFO;
                filled = true;
            }
            case WARNING -> {
                fg = new Color(0x664D03);
                bg = BootstrapColors.WARNING;
                border = BootstrapColors.WARNING;
                filled = true;
            }
            case DANGER -> {
                fg = Color.WHITE;
                bg = BootstrapColors.DANGER;
                border = BootstrapColors.DANGER;
                filled = true;
            }
            case OUTLINE_PRIMARY -> {
                fg = BootstrapColors.PRIMARY;
                bg = BootstrapColors.WHITE;
                border = BootstrapColors.PRIMARY;
                filled = false;
            }
            case OUTLINE_SECONDARY -> {
                fg = BootstrapColors.SECONDARY;
                bg = BootstrapColors.WHITE;
                border = BootstrapColors.SECONDARY;
                filled = false;
            }
            case OUTLINE_DANGER -> {
                fg = BootstrapColors.DANGER;
                bg = BootstrapColors.WHITE;
                border = BootstrapColors.DANGER;
                filled = false;
            }
        }

        setForeground(fg);
        setBackground(filled ? bg : BootstrapColors.WHITE);
        setBorder(BorderFactory.createLineBorder(border, 1, true));
    }
}
