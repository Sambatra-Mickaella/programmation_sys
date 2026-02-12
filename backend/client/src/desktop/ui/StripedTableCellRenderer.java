package desktop.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class StripedTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
            c.setBackground((row % 2 == 0) ? BootstrapColors.WHITE : BootstrapColors.TABLE_STRIPE);
            c.setForeground(new Color(0x212529));
        }
        return c;
    }
}
