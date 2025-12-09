import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class HoldsPanel extends JPanel {
    private JTable tblHolds;
    private DefaultTableModel model;
    private JButton btnPlaceHold, btnDeleteHold;
    private String userRole;
    private int memberId;

    public HoldsPanel(String userRole, int memberId) {
        this.userRole = userRole;
        this.memberId = memberId;

        setLayout(new BorderLayout(10, 10));

        model = new DefaultTableModel(new Object[]{"Hold ID", "Member ID", "Book ID", "Hold Date"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };

        tblHolds = new JTable(model);
        add(new JScrollPane(tblHolds), BorderLayout.CENTER);

        btnPlaceHold = new JButton("Place Hold");
        btnDeleteHold = new JButton("Delete Hold");

        JPanel bp = new JPanel();
        bp.add(btnPlaceHold);
        bp.add(btnDeleteHold);
        add(bp, BorderLayout.SOUTH);

        btnPlaceHold.addActionListener(e -> handlePlaceHold());
        btnDeleteHold.addActionListener(e -> handleDeleteHold());
        ///
        this.addComponentListener(new update());
    }

    public void loadHolds() {
        model.setRowCount(0);
        String sql = userRole.equals("Member") ?
                "SELECT * FROM holds WHERE member_id=? ORDER BY hold_date ASC" :
                "SELECT * FROM holds ORDER BY hold_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userRole.equals("Member")) ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("hold_id"));
                row.add(rs.getInt("member_id"));
                row.add(rs.getInt("book_id"));
                row.add(rs.getTimestamp("hold_date"));
                model.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading holds: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePlaceHold() {
        try {
            int bookId;
            int targetMemberId = memberId;

            if (userRole.equals("Member")) {
                String input = JOptionPane.showInputDialog(this, "Enter Book ID:");
                if (input == null || input.isEmpty()) return;
                bookId = Integer.parseInt(input.trim());
            } else {
                JTextField bookField = new JTextField();
                JTextField memberField = new JTextField();
                Object[] message = {"Book ID:", bookField, "Member ID:", memberField};
                int option = JOptionPane.showConfirmDialog(this, message, "Place Hold", JOptionPane.OK_CANCEL_OPTION);
                if (option != JOptionPane.OK_OPTION) return;
                bookId = Integer.parseInt(bookField.getText().trim());
                targetMemberId = Integer.parseInt(memberField.getText().trim());
            }

         if (HoldsService.placeHold(userRole, targetMemberId, bookId)) {
    loadHolds();
}


        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "IDs must be numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteHold() {
        int row = tblHolds.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a hold first.");
            return;
        }

        int holdId = (int) model.getValueAt(row, 0);
        int holdMemberId = (int) model.getValueAt(row, 1);

        if (userRole.equals("Member") && holdMemberId != memberId) {
            JOptionPane.showMessageDialog(this, "You can only delete your own holds.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure to delete this hold?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (HoldsService.deleteHold(holdId)) {
            loadHolds();
        }
    }
    
    ///
    public class update extends ComponentAdapter{
    @Override
            public void componentShown(ComponentEvent e) {
                loadHolds();
            }
    }
}

