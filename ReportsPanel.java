import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class ReportsPanel extends JPanel {

    private JTabbedPane tabbedPane;
    private JTable tblOverdue;
    private JTable tblMostBorrowed;
    private JTable tblFines;
    private JButton btnExportCSV;

    public ReportsPanel() {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        tblOverdue = new JTable();
        tblMostBorrowed = new JTable();
        tblFines = new JTable();

        tabbedPane.addTab("Overdue Books", new JScrollPane(tblOverdue));
        tabbedPane.addTab("Most Borrowed", new JScrollPane(tblMostBorrowed));
        tabbedPane.addTab("Active Fines", new JScrollPane(tblFines));

        btnExportCSV = new JButton("Export CSV");

        add(tabbedPane, BorderLayout.CENTER);
        add(btnExportCSV, BorderLayout.SOUTH);
        btnExportCSV.addActionListener(e -> exportCurrentTableToCSV());
        ///
        this.addComponentListener(new update());
    }

    private void loadAllReports() {
        loadOverdueTable();
        loadMostBorrowedTable();
        loadFinesTable();
    }

    // 1) (Overdue)
    private void loadOverdueTable() {
       String sql = "SELECT " +
    "books.title AS Title, " +
    "members.name AS Member, " +
    "loans.due_date AS DueDate, " +
    "DATEDIFF(CURDATE(), loans.due_date) AS DaysLate " +
    "FROM loans " +
    "JOIN copies ON loans.copy_id = copies.copy_id " +
    "JOIN books ON copies.book_id = books.book_id " +
    "JOIN members ON loans.member_id = members.member_id " +
    "WHERE loans.return_date IS NULL " +
    "AND loans.due_date < CURDATE()";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            tblOverdue.setModel(buildTableModel(rs));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading overdue report:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // 2) (Most Borrowed)
    private void loadMostBorrowedTable() {
        String sql = "SELECT " +
                "books.title AS Title, " +
                "books.author AS Author, " +
                "COUNT(loans.loan_id) AS TotalBorrows " +
                "FROM books " +
                "LEFT JOIN copies ON books.book_id = copies.book_id " +
                "LEFT JOIN loans ON copies.copy_id = loans.copy_id " +
                "GROUP BY books.book_id, books.title, books.author " +
                "ORDER BY TotalBorrows DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            tblMostBorrowed.setModel(buildTableModel(rs));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading most borrowed report:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // 3) (Active Fines)
    private void loadFinesTable() {
        String sql = "SELECT " +
                "members.name AS Member, " +
                "fines.amount AS Amount, " +
                "fines.is_paid AS Paid " +
                "FROM fines " +
                "JOIN members ON fines.member_id = members.member_id " +
                "WHERE fines.is_paid = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            tblFines.setModel(buildTableModel(rs));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading fines report:\n" + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();

        int columnCount = metaData.getColumnCount();
        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnLabel(i);
        }

        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        Object[] rowData = new Object[columnCount];

        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                rowData[i - 1] = rs.getObject(i);
            }
            model.addRow(rowData.clone());
        }

        return model;
    }

    // CSV
    private void exportCurrentTableToCSV() {
        Component selected = tabbedPane.getSelectedComponent();
        if (!(selected instanceof JScrollPane)) {
            JOptionPane.showMessageDialog(this,
                    "No table to export.",
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JScrollPane scrollPane = (JScrollPane) selected;
        JViewport viewport = scrollPane.getViewport();
        Component view = viewport.getView();

        if (!(view instanceof JTable)) {
            JOptionPane.showMessageDialog(this,
                    "No table to export.",
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTable table = (JTable) view;

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV File");

        int userSelection = chooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return; 
        }

        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            int colCount = model.getColumnCount();
            int rowCount = model.getRowCount();

          
            for (int i = 0; i < colCount; i++) {
                writer.write(escapeCSV(model.getColumnName(i)));
                if (i < colCount - 1) writer.write(",");
            }
            writer.write("\n");

          
            for (int r = 0; r < rowCount; r++) {
                for (int c = 0; c < colCount; c++) {
                    Object value = model.getValueAt(r, c);
                    writer.write(escapeCSV(value == null ? "" : value.toString()));
                    if (c < colCount - 1) writer.write(",");
                }
                writer.write("\n");
            }

            JOptionPane.showMessageDialog(this,
                    "Export completed successfully.",
                    "Export CSV",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error writing CSV file:\n" + ex.getMessage(),
                    "I/O Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

       private String escapeCSV(String field) {
        String escaped = field.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
       
       ///
       public class update extends ComponentAdapter{
    @Override
            public void componentShown(ComponentEvent e) {
                loadAllReports();
            }
    }
}
