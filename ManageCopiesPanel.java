 import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
public class ManageCopiesPanel extends JPanel {
    private JTable tblCopies;
    private DefaultTableModel model;
    private JComboBox<String> cmbSelectBook;
    private JTextField txtStatus, txtMember, txtDueDate;
    private JButton btnAddCopy, btnUpdateStatus, btnRenew,btnDeleteCopy;
    private String userRole;
    public ManageCopiesPanel(String role) {
        this.userRole = role;
        setLayout(new BorderLayout(10, 10));
     
        model = new DefaultTableModel(
                new Object[]{"CopyID", "Book Title", "Status", "Loaned To", "Due Date", "LoanID"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCopies = new JTable(model);
        tblCopies.setRowHeight(25);
        JScrollPane sp = new JScrollPane(tblCopies);
       
        JLabel lBook = new JLabel("Book:");
        JLabel lStatus = new JLabel("Status:");
        JLabel lMember = new JLabel("Loaned To:");
        JLabel lDue = new JLabel("Due Date:");
        cmbSelectBook = new JComboBox<>();
        txtStatus = new JTextField(12);
        txtMember = new JTextField(12);
        txtDueDate = new JTextField(12);
        txtStatus.setEditable(false);
        txtMember.setEditable(false);
        txtDueDate.setEditable(false);
        JPanel gp = new JPanel(new GridLayout(2, 4, 5, 3));
        gp.add(lBook);
        gp.add(lStatus);
        gp.add(lMember);
        gp.add(lDue);
        gp.add(cmbSelectBook);
        gp.add(txtStatus);
        gp.add(txtMember);
        gp.add(txtDueDate);
       
        btnAddCopy = new JButton("Add Copy");
        btnUpdateStatus = new JButton("Update Status");
        btnRenew = new JButton("Renew");
        btnDeleteCopy = new JButton("Delete Copy");
        JPanel bp = new JPanel();
        if (userRole.equals("Librarian")) {
            bp.add(btnAddCopy);
            bp.add(btnUpdateStatus);
            bp.add(btnRenew); 
            bp.add(btnDeleteCopy);
        } else {
            btnAddCopy.setEnabled(false);
            btnDeleteCopy.setEnabled(false);
            bp.add(btnUpdateStatus);
            bp.add(btnRenew); 
        }
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(gp, BorderLayout.CENTER);
        topPanel.add(bp, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
       
        loadBooks();
        btnAddCopy.addActionListener(e -> { try { addCopy(); }
        catch (Exception ex) { showError(ex); }});
        btnUpdateStatus.addActionListener(e -> { try { updateCopyStatus(); }
        catch (Exception ex) { showError(ex); }});
btnRenew.addActionListener(e -> {
    int row = tblCopies.getSelectedRow();
    if (row == -1) {
        showError(new Exception("Select a copy first!"));
        return;
    }

    String status = (String) model.getValueAt(row, 2);
    if (!"Borrowed".equalsIgnoreCase(status)) {
        showError(new Exception("Only borrowed copies can be renewed."));
        return;
    }

    int copyId = (int) model.getValueAt(row, 0);
    Object memberObj = model.getValueAt(row, 3);
    if (memberObj == null || memberObj.toString().isEmpty()) {
        showError(new Exception("Cannot renew: No member found for this copy."));
        return;
    }

    int memberId = Integer.parseInt(memberObj.toString());

    String message = LendingService.renewBook(copyId, memberId);

    loadCopies();

    JOptionPane.showMessageDialog(this, message);
});








        btnDeleteCopy.addActionListener(e -> { try { deleteCopy(); }
        catch (Exception ex) { showError(ex); }});
        tblCopies.getSelectionModel().addListSelectionListener(e -> fillSelectedRow());
    }
    private void fillSelectedRow() {
        int row = tblCopies.getSelectedRow();
        if (row == -1) {
            btnUpdateStatus.setEnabled(false);
            return;
        }
        txtStatus.setText(String.valueOf(model.getValueAt(row, 2)));
        txtMember.setText(String.valueOf(model.getValueAt(row, 3)));
        txtDueDate.setText(String.valueOf(model.getValueAt(row, 4)));
        String status = (String) model.getValueAt(row, 2);
        if ("Available".equalsIgnoreCase(status)) {
            btnUpdateStatus.setText("Borrow");
        } else {
            btnUpdateStatus.setText("Return");
        }
        btnUpdateStatus.setEnabled(true);
    }
    private void loadBooks() {
        cmbSelectBook.removeAllItems();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement
        ("SELECT book_id, title FROM books ORDER BY book_id ASC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cmbSelectBook.addItem(rs.getInt("book_id") + " - " + rs.getString("title"));
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        cmbSelectBook.addActionListener(e -> loadCopies());
        loadCopies();
    }
private void loadCopies() {
    model.setRowCount(0);
    if (cmbSelectBook.getSelectedItem() == null) return;

    int bookId = Integer.parseInt(cmbSelectBook.getSelectedItem().toString().split(" - ")[0]);

    String sql =
        "SELECT c.copy_id, b.title, c.status, l.member_id, l.due_date, l.loan_id " +
        "FROM copies c " +
        "JOIN books b ON c.book_id = b.book_id " +
        "LEFT JOIN loans l ON c.copy_id = l.copy_id AND l.return_date IS NULL " +
        "WHERE c.book_id = ? " +
        "ORDER BY c.copy_id ASC";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setInt(1, bookId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Object memberObj = rs.getObject("member_id");
            Object dueObj = rs.getObject("due_date");

            model.addRow(new Object[]{
                rs.getInt("copy_id"),
                rs.getString("title"),
                rs.getString("status"),
                memberObj != null ? memberObj : "",
                dueObj != null ? dueObj : "",
                rs.getObject("loan_id")
            });
        }

        if (tblCopies.getColumnCount() > 5) {
            tblCopies.getColumnModel().getColumn(5).setMinWidth(0);
            tblCopies.getColumnModel().getColumn(5).setMaxWidth(0);
            tblCopies.getColumnModel().getColumn(5).setWidth(0);
        }

        model.fireTableDataChanged();
        tblCopies.repaint();

    } catch (SQLException ex) {
        showError(ex);
    }
}

    private void addCopy() throws emptyException, duplicateException, SQLException {
        if (!userRole.equals("Librarian")) return;
        if (cmbSelectBook.getSelectedItem() == null)
            throw new emptyException("Please select a book first.");
        int bookId = Integer.parseInt(cmbSelectBook.getSelectedItem().toString().split(" - ")[0]);
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO copies(book_id, status) VALUES (?, 'Available')");
            ps.setInt(1, bookId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Copy added successfully!");
            loadCopies();
        }
    }
  private void updateCopyStatus() throws emptySelectionException, SQLException {
    int row = tblCopies.getSelectedRow();
    if (row == -1)
        throw new emptySelectionException("Select a copy first!");
    int copyId = (int) model.getValueAt(row, 0);
    String status = (String) model.getValueAt(row, 2);
    if ("Available".equalsIgnoreCase(status)) {
        try {
            borrowCopy(copyId);
        } catch (emptyException ex) {
            showError(ex);
            return;
        }
    } else {
        LendingService.returnBook(copyId,true); ///////true
        loadCopies();
    }
}
private void borrowCopy(int copyId) throws emptyException {
String input = JOptionPane.showInputDialog(this, "Enter Member ID:");
if (input == null) {
    return;
}
if (input.trim().isEmpty()) {
    showError(new Exception("Member ID is required"));
    return;
}

    try {
        int memberId = Integer.parseInt(input.trim());
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM members WHERE member_id=?")) {
            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                throw new emptyException("Member ID does not exist!");
            }
        }
        Date today = new Date(System.currentTimeMillis());
        Date due = new Date(System.currentTimeMillis() + 7L * 86400000);
        boolean ok = LendingService.borrowBook(memberId, copyId, today, due);
        if (ok) loadCopies();
    } catch (NumberFormatException ex) {
        throw new emptyException("Member ID must be a number");
    } catch (SQLException ex) {
        showError(ex);
    }
}
    private void deleteCopy() throws emptySelectionException, deleteException, SQLException {
        int row = tblCopies.getSelectedRow();
        if (row == -1)
            throw new emptySelectionException("Select a copy first!");
        String status = (String) model.getValueAt(row, 2);
        if ("Borrowed".equalsIgnoreCase(status))
            throw new deleteException("Cannot delete a borrowed copy!");
        int copyId = (int) model.getValueAt(row, 0);
       int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this copy?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
    );
    if (confirm != JOptionPane.YES_OPTION) return;
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement("DELETE FROM copies WHERE copy_id=?")) {
        ps.setInt(1, copyId);
        ps.executeUpdate();
        JOptionPane.showMessageDialog(this, "Copy deleted successfully!");
        loadCopies();
    }
}
    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    public class emptyException extends Exception {
        public emptyException(String message) { super(message); }
    }
    public class duplicateException extends Exception {
        public duplicateException(String message) { super(message); }
    }
    public class emptySelectionException extends Exception {
        public emptySelectionException(String message) { super(message); }
    }
    public class deleteException extends Exception {
        public deleteException(String message) { super(message); }
    }
}

