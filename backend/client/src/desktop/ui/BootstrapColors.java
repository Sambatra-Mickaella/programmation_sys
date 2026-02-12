package desktop.ui;

import java.awt.Color;

public final class BootstrapColors {
    private BootstrapColors() {}

    public static final Color BG_LIGHT = new Color(0xF8F9FA);
    public static final Color BG_BODY_TERTIARY = new Color(0xF8F9FA);
    public static final Color WHITE = Color.WHITE;

    public static final Color BORDER = new Color(0xDEE2E6);
    public static final Color TEXT_MUTED = new Color(0x6C757D);

    public static final Color PRIMARY = new Color(0x0D6EFD);
    public static final Color SECONDARY = new Color(0x6C757D);
    public static final Color SUCCESS = new Color(0x198754);
    public static final Color INFO = new Color(0x0DCAF0);
    public static final Color WARNING = new Color(0xFFC107);
    public static final Color DANGER = new Color(0xDC3545);

    public static final Color NAV_ACTIVE_BG = new Color(0x0D6EFD);
    public static final Color NAV_ACTIVE_FG = Color.WHITE;
    public static final Color NAV_FG = new Color(0x212529);

    public static final Color TABLE_STRIPE = new Color(0xF2F2F2);

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
