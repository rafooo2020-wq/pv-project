import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MyLoansPanel extends JPanel {

    private final int memberId;
    private final JTable tblLoans;
    private final DefaultTableModel model;
    private final JButton btnReturn = new JButton("Return");
    private final JButton btnRenew = new JButton("Renew");
    private final JLabel lblInfo = new JLabel(" ");

    private static final double FINE_PER_DAY = 2.0;

    public MyLoansPanel(int memberId) {
        this.memberId = memberId;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(new Object[]{
                "LoanID", "CopyID", "Title", "Loan Date", "Due Date", "Overdue Days", "Fine"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblLoans = new JTable(model);
        tblLoans.setRowHeight(26);
        tblLoans.setAutoCreateRowSorter(true);

        add(new JScrollPane(tblLoans), BorderLayout.CENTER);

        // Bottom panel
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(btnReturn);
        bottom.add(btnRenew);
        bottom.add(lblInfo);
        add(bottom, BorderLayout.SOUTH);

        // Button actions
        btnReturn.addActionListener(e -> returnSelected());
        btnRenew.addActionListener(e -> renewSelected());
        ///
        this.addComponentListener(new update());
    }

    public void loadLoans() {
        model.setRowCount(0);
        List<LendingService.Loan> loans = (List<LendingService.Loan>) LendingService.getActiveLoans(memberId);

        double totalFine = 0.0;

        for (LendingService.Loan loan : loans) {
            String title = getBookTitle(loan.copyId);

            long overdueDays = 0;
            if (loan.dueDate != null) {
                overdueDays = Math.max(0, ChronoUnit.DAYS.between(loan.dueDate.toLocalDate(), LocalDate.now()));
            }

            double fine = FinesService.calculateFine(
                    loan.dueDate,
                    new Date(System.currentTimeMillis()),
                    FINE_PER_DAY,
                    0
            );
            totalFine += fine;

            model.addRow(new Object[]{
                    loan.loanId,
                    loan.copyId,
                    title,
                    loan.loanDate,
                    loan.dueDate,
                    overdueDays,
                    String.format("%.2f", fine)
            });
        }

        lblInfo.setText("Total estimated fine: " + String.format("%.2f", totalFine));
    }

    private String getBookTitle(int copyId) {
        String title = "";
        try (var conn = DBConnection.getConnection();
             var ps = conn.prepareStatement(
                     "SELECT b.title FROM copies c JOIN books b ON c.book_id=b.book_id WHERE c.copy_id=?")) {
            ps.setInt(1, copyId);
            var rs = ps.executeQuery();
            if (rs.next()) title = rs.getString("title");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return title;
    }

   private void returnSelected() {
    int row = tblLoans.getSelectedRow();
    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Select a loan first.");
        return;
    }

    int modelRow = tblLoans.convertRowIndexToModel(row);
    int copyId = (int) model.getValueAt(modelRow, 1);
    String title = (String) model.getValueAt(modelRow, 2);

    int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to return: " + title + "?",
            "Confirm Return",
            JOptionPane.YES_NO_OPTION
    );

    if (confirm != JOptionPane.YES_OPTION) return;

    boolean success = LendingService.returnBook(copyId,false);////////false
    if (success) loadLoans();
}


    private void renewSelected() {
        int row = tblLoans.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a loan first."); return; }

        int modelRow = tblLoans.convertRowIndexToModel(row);
        int copyId = (int) model.getValueAt(modelRow, 1);

        String message = LendingService.renewBook(copyId, memberId);
        JOptionPane.showMessageDialog(this, message);
        loadLoans();
    }
    
     public class update extends ComponentAdapter{
    @Override
            public void componentShown(ComponentEvent e) {
                loadLoans();
            }
    }
}

