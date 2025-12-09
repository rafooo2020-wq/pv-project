import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
public class LendingService {
    private static final int MAX_BORROW_LIMIT = 3;
    private static final double FINE_RATE = 2;
    private static final int GRACE_DAYS = 3;  
   
    public static boolean borrowBook(int memberId, int copyId, Date loanDate, Date dueDate) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement psFines = conn.prepareStatement(
                "SELECT COUNT(*) FROM fines WHERE member_id=? AND is_paid=FALSE"
            );
            psFines.setInt(1, memberId);
            ResultSet rs1 = psFines.executeQuery();
            rs1.next();
            if (rs1.getInt(1) > 0) {
                JOptionPane.showMessageDialog(null,
                        "Member has unpaid fines. Borrowing denied.");
                return false;
            }
            PreparedStatement psLoans = conn.prepareStatement(
                "SELECT COUNT(*) FROM loans WHERE member_id=? AND return_date IS NULL"
            );
            psLoans.setInt(1, memberId);
            ResultSet rs2 = psLoans.executeQuery();
            rs2.next();
            if (rs2.getInt(1) >= MAX_BORROW_LIMIT) {
                JOptionPane.showMessageDialog(null,
                        "Member reached the maximum borrowing limit.");
                return false;
            }
            PreparedStatement psStatus = conn.prepareStatement(
                "SELECT status, book_id FROM copies WHERE copy_id=?"
            );
            psStatus.setInt(1, copyId);
            ResultSet rs3 = psStatus.executeQuery();
            if (!rs3.next() || !rs3.getString("status").equals("Available")) {
                JOptionPane.showMessageDialog(null,
                        "This copy is not available.");
                return false;
            }
            
            int bookId = rs3.getInt("book_id");
            
            int firstHolderId = HoldsService.getFirstMemberInQueue(bookId);
      
            if (firstHolderId != -1 && firstHolderId != memberId) {
                JOptionPane.showMessageDialog(null,"This book is currently reserved by another member\nYou cannot borrow it now","Warning", JOptionPane.WARNING_MESSAGE);
                return false;
            }            
            
            PreparedStatement psInsert = conn.prepareStatement(
                "INSERT INTO loans(copy_id, member_id, loan_date, due_date) VALUES (?, ?, ?, ?)"
            );
            psInsert.setInt(1, copyId);
            psInsert.setInt(2, memberId);
            psInsert.setDate(3, loanDate);
            psInsert.setDate(4, dueDate);
            psInsert.executeUpdate();
            PreparedStatement psUpdateCopy = conn.prepareStatement(
                "UPDATE copies SET status='Borrowed' WHERE copy_id=?"
            );
            psUpdateCopy.setInt(1, copyId);
            psUpdateCopy.executeUpdate();
            
            if (firstHolderId == memberId) {
                HoldsService.removeHold(memberId, bookId);
            }
            
            return true;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "ERROR: " + ex.getMessage());
            return false;
        }
    }
 public static boolean returnBook(int copyId,boolean isLibrarian) { 
    try (Connection conn = DBConnection.getConnection()) {
        PreparedStatement psLoan = conn.prepareStatement(
            "SELECT loan_id, member_id, due_date, c.book_id " +
            "FROM loans l " +
            "JOIN copies c ON l.copy_id = c.copy_id " +
            "WHERE l.copy_id=? AND l.return_date IS NULL"
        );
        psLoan.setInt(1, copyId);
        ResultSet rs = psLoan.executeQuery();

        if (!rs.next()) {
            JOptionPane.showMessageDialog(null, "No active loan found for this copy.");
            return false;
        }

        int loanId = rs.getInt("loan_id");
        int memberId = rs.getInt("member_id");
        int bookId = rs.getInt("book_id");
        Date dueDate = rs.getDate("due_date");
        Date today = new Date(System.currentTimeMillis());

        double fineAmount = FinesService.calculateFine(dueDate, today, FINE_RATE, GRACE_DAYS);
        if (fineAmount > 0) {
            PreparedStatement psFine = conn.prepareStatement(
                "INSERT INTO fines(member_id,amount, is_paid) VALUES (?, ?, FALSE)"
            );
            psFine.setInt(1, memberId);
            psFine.setDouble(2, fineAmount);
            psFine.executeUpdate();
        }

        PreparedStatement psUpdateLoan = conn.prepareStatement(
            "UPDATE loans SET return_date=? WHERE loan_id=?"
        );
        psUpdateLoan.setDate(1, today);
        psUpdateLoan.setInt(2, loanId);
        psUpdateLoan.executeUpdate();

        PreparedStatement psUpdateCopy = conn.prepareStatement(
            "UPDATE copies SET status='Available' WHERE copy_id=?"
        );
        psUpdateCopy.setInt(1, copyId);
        psUpdateCopy.executeUpdate();
        
            String msg = "Book returned successfully!";
            if (isLibrarian) {
                int firstHolderId = HoldsService.getFirstMemberInQueue(bookId);
                
                if (firstHolderId != -1) {
                    String holderName = HoldsService.getFirstMemberName(bookId);
                    
                    JOptionPane.showMessageDialog(null,"Return Successful\nThis book is reserved for: " + holderName + " (ID: " + firstHolderId + ").\nPlease send a notification to this member","Reservation notification", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, msg);
                }
            } else {
                JOptionPane.showMessageDialog(null, msg);
            }
            return true;

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null, "Error returning book: " + ex.getMessage());
        return false;
    }
}

public static String renewBook(int copyId, int memberId) {
    try (Connection conn = DBConnection.getConnection()) {

        PreparedStatement psLoan = conn.prepareStatement(
            "SELECT l.loan_id, l.due_date, l.member_id, c.book_id " +
            "FROM loans l " +
            "JOIN copies c ON l.copy_id = c.copy_id " +
            "WHERE l.copy_id=? AND l.member_id=? AND l.return_date IS NULL"
        );
        psLoan.setInt(1, copyId);
        psLoan.setInt(2, memberId);
        ResultSet rs = psLoan.executeQuery();

        if (!rs.next()) {
            return "No active loan found for this copy.";
        }

        int loanId = rs.getInt("loan_id");
        Date currentDue = rs.getDate("due_date");  
        int bookId = rs.getInt("book_id");

            int firstHolderId = HoldsService.getFirstMemberInQueue(bookId);
            
            if (firstHolderId != -1) {
                return "Cannot renew: Another member has a hold on this book.";
            }

        Date today = new Date(System.currentTimeMillis());
        double fineAmount = FinesService.calculateFine(currentDue, today, FINE_RATE, GRACE_DAYS);
        if (fineAmount > 0) {
            PreparedStatement psFine = conn.prepareStatement(
                "INSERT INTO fines(member_id amount, is_paid) VALUES (?, ?, FALSE)"
            );
            psFine.setInt(1, memberId);
            psFine.setDouble(2, fineAmount);
            psFine.executeUpdate();
            return "Cannot renew: Book is overdue and fine added.";
        }

        Date newDue = new Date(currentDue.getTime() + 7L * 24 * 60 * 60 * 1000);

        PreparedStatement psUpdateLoan = conn.prepareStatement(
            "UPDATE loans SET due_date=? WHERE loan_id=?"
        );
        psUpdateLoan.setDate(1, newDue);
        psUpdateLoan.setInt(2, loanId);
        psUpdateLoan.executeUpdate();

        return "Book renewed successfully! New due date: " + newDue;

    } catch (SQLException ex) {
        ex.printStackTrace();
        return "Error renewing the book: " + ex.getMessage();
    }
}
    public static List<Loan> getActiveLoans(int memberId) {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT l.loan_id, l.copy_id, l.member_id, l.loan_date, l.due_date " +
                     "FROM loans l " +
                     "WHERE l.member_id=? AND l.return_date IS NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Loan loan = new Loan(
                    rs.getInt("loan_id"),
                    rs.getInt("copy_id"),
                    rs.getInt("member_id"),
                    rs.getDate("loan_date"),
                    rs.getDate("due_date")
                );
                loans.add(loan);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return loans;
    }

    public static class Loan {
        public int loanId;
        public int copyId;
        public int memberId;
        public Date loanDate;
        public Date dueDate;

        public Loan(int loanId, int copyId, int memberId, Date loanDate, Date dueDate) {
            this.loanId = loanId;
            this.copyId = copyId;
            this.memberId = memberId;
            this.loanDate = loanDate;
            this.dueDate = dueDate;
        }
    }
}
