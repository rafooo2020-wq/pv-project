import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SearchPanel extends JPanel {
    private final JTextField txtSearch = new JTextField(20);
    private final JComboBox<String> cmbFilter = new JComboBox<>(new String[]{"Title", "Author", "ISBN", "Category"});
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnBorrow = new JButton("Borrow");
    private final JButton btnHold = new JButton("Place Hold");
    private final JTable tblResults;
    private final DefaultTableModel model;
    private final int memberId;

    public SearchPanel(int memberId) {
        this.memberId = memberId;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top panel: search bar + filter
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(txtSearch);
        top.add(cmbFilter);
        top.add(btnSearch);
        add(top, BorderLayout.NORTH);

        // Table model
        model = new DefaultTableModel(new Object[]{"Book ID", "Title", "Author", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblResults = new JTable(model);
        tblResults.setRowHeight(26);
        add(new JScrollPane(tblResults), BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(btnBorrow);
        bottom.add(btnHold);
        add(bottom, BorderLayout.SOUTH);

        // Action listeners
        btnSearch.addActionListener(e -> searchBooks());
        btnBorrow.addActionListener(e -> borrowSelected());
        btnHold.addActionListener(e -> holdSelected());

        tblResults.getSelectionModel().addListSelectionListener(e -> updateButtonState());

        searchBooks();
    }

    private void searchBooks() {
        model.setRowCount(0);
        String keyword = "%" + txtSearch.getText().trim() + "%";
        String filterColumn = switch (cmbFilter.getSelectedItem().toString()) {
            case "Author" -> "author";
            case "ISBN" -> "isbn";
            case "Category" -> "category";
            default -> "title";
        };

        String sql = "SELECT b.book_id, b.title, b.author, " +
                     "(SELECT COUNT(*) FROM copies c WHERE c.book_id=b.book_id AND c.status='Available') AS available_count " +
                     "FROM books b WHERE b." + filterColumn + " LIKE ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, keyword);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int available = rs.getInt("available_count");
                String status = available > 0 ? "Available" : "Not Available";
                model.addRow(new Object[]{rs.getInt("book_id"), rs.getString("title"), rs.getString("author"), status});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage());
        }
        updateButtonState();
    }

    private void updateButtonState() {
        int row = tblResults.getSelectedRow();
        if (row == -1) {
            btnBorrow.setEnabled(false);
            btnHold.setEnabled(false);
            return;
        }
        String status = (String) model.getValueAt(tblResults.convertRowIndexToModel(row), 3);
        btnBorrow.setEnabled("Available".equalsIgnoreCase(status));
        btnHold.setEnabled(!"Available".equalsIgnoreCase(status));
    }

    private void borrowSelected() {
        int row = tblResults.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a book."); return; }

        int modelRow = tblResults.convertRowIndexToModel(row);
        int bookId = (int) model.getValueAt(modelRow, 0);

        try {
            int copyId = getAvailableCopyId(bookId);
            if (copyId == -1) {
                JOptionPane.showMessageDialog(this, "No available copies to borrow.");
                return;
            }
            java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
            java.sql.Date due = new java.sql.Date(System.currentTimeMillis() + 7L * 86400000);
            boolean success = LendingService.borrowBook(memberId, copyId, today, due);
            if (success) {
                JOptionPane.showMessageDialog(this, "Book borrowed successfully!");
                searchBooks();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Borrow failed: " + ex.getMessage());
        }
    }

    private void holdSelected() {
        int row = tblResults.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a book."); return; }

        int modelRow = tblResults.convertRowIndexToModel(row);
        int bookId = (int) model.getValueAt(modelRow, 0);
        String status = (String) model.getValueAt(modelRow, 3);

        if ("Available".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Copies are available. You can borrow directly!");
            return;
        }

        try {
            boolean success = HoldsService.placeHold("Member", memberId, bookId);
            if (success) {
                searchBooks();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to place hold: " + ex.getMessage());
        }
    }

    private int getAvailableCopyId(int bookId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT copy_id FROM copies WHERE book_id=? AND status='Available' LIMIT 1")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("copy_id");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error getting available copy: " + ex.getMessage());
        }
        return -1;
    }
}


