import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HoldsService {

     public static boolean holdExists(int memberId, int bookId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM holds WHERE member_id=? AND book_id=?")) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error checking existing hold: " + e.getMessage());
        }
        return false;
    }
     
    public static boolean isBookAvailable(int bookId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM copies WHERE book_id=? AND status='Available'")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error checking book availability: " + e.getMessage());
        }
        return false;
    }

   public static boolean placeHold(String userRole, int memberId, int bookId) {
    if (holdExists(memberId, bookId)) {
        if (userRole.equals("Member")) {
            JOptionPane.showMessageDialog(null, "You already have a hold on this book.");
        } else { // Librarian
            JOptionPane.showMessageDialog(null, "This member already has a hold on this book.");
        }
        return false;
    }

    if (isBookAvailable(bookId)) {
        if (userRole.equals("Member")) {
            JOptionPane.showMessageDialog(null, "Book is available. Borrow it instead of placing a hold.");
        } else {
            JOptionPane.showMessageDialog(null, "Book has available copies. No need to place a hold for this member.");
        }
        return false;
    }

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO holds(member_id, book_id) VALUES(?, ?)")) {
        ps.setInt(1, memberId);
        ps.setInt(2, bookId);
        ps.executeUpdate();

        if (userRole.equals("Member")) {
            JOptionPane.showMessageDialog(null, "Hold placed successfully!");
        } else {
            JOptionPane.showMessageDialog(null, "Hold placed successfully for this member!");
        }
        return true;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error placing hold: " + e.getMessage());
    }
    return false;
}

    public static boolean deleteHold(int holdId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM holds WHERE hold_id=?")) {
            ps.setInt(1, holdId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "Hold deleted successfully!");
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting hold: " + e.getMessage());
        }
        return false;
    }
    
    
public static int getFirstMemberInQueue(int bookId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT member_id FROM holds WHERE book_id=? ORDER BY hold_date ASC LIMIT 1")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("member_id");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error checking hold queue: " + e.getMessage());
        }
        return -1;
    }

    public static String getFirstMemberName(int bookId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT m.name FROM holds h " +
                 "JOIN members m ON h.member_id = m.member_id " +
                 "WHERE h.book_id=? ORDER BY h.hold_date ASC LIMIT 1")) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving member name: " + e.getMessage());
        }
        return "Unknown";
    }

    public static void removeHold(int memberId, int bookId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM holds WHERE member_id=? AND book_id=?")) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error removing hold: " + e.getMessage());
        }
    }
}

