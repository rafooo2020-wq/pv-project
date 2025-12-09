
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class ManageBooksPanel extends JPanel {

    private JTable tblBooks;
    private JLabel lTitle, lAuthor, lIsbn, lCategory;
    private JTextField tTitle, tAuthor, tIsbn, tCategory;
    private JButton bAdd, bUpdate, bRemove;

    public ManageBooksPanel() {

        setLayout(new BorderLayout());

        lTitle = new JLabel("Title:");
        tTitle = new JTextField(15);

        lAuthor = new JLabel("Author:");
        tAuthor = new JTextField(15);

        lIsbn = new JLabel("ISBN:");
        tIsbn = new JTextField(15);

        lCategory = new JLabel("Category:");
        tCategory = new JTextField(15);

        bAdd = new JButton("Add");
        bUpdate = new JButton("Update");
        bRemove = new JButton("Remove");

        JPanel gp = new JPanel(new GridLayout(2,4,5,3));

        gp.add(lTitle);
        gp.add(lAuthor);
        gp.add(lIsbn);
        gp.add(lCategory);

        gp.add(tTitle);
        gp.add(tAuthor);
        gp.add(tIsbn);
        gp.add(tCategory);

        JPanel bp = new JPanel();
        bp.add(bAdd);
        bp.add(bUpdate);
        bp.add(bRemove);

        tblBooks = new JTable();
        JScrollPane sp = new JScrollPane(tblBooks);

        add(gp, BorderLayout.NORTH);
        add(bp, BorderLayout.CENTER);
        add(sp, BorderLayout.SOUTH);

        loadBooks();

        bAdd.addActionListener(new addBook());
        bUpdate.addActionListener(new updateBook());
        bRemove.addActionListener(new removeBook());
        tblBooks.addMouseListener(new getRow());
    }

    private void loadBooks() {
        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM books";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            String[] columns = {"ID", "Title", "Author", "ISBN", "Category"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            while (rs.next()) {
              model.addRow(new Object[]{
                rs.getInt("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("isbn"),
                rs.getString("category")
              });
            }

            tblBooks.setModel(model);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null,"An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public class addBook implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String title = tTitle.getText();
                String author = tAuthor.getText();
                String isbn = tIsbn.getText();
                String category = tCategory.getText();
                
                if (title.isEmpty() || author.isEmpty() || isbn.isEmpty())
                    throw new emptyException("All fields are required");
                
                if(!title.matches(".*[a-zA-Z]+.*")){
                    JOptionPane.showMessageDialog(null,"Book title must contain at least one letter!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                
                if(!author.matches("^[a-zA-Z\\s._-]+$")){
                    JOptionPane.showMessageDialog(null,"Author name contains invalid characters!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;} 
                
                if(!isbn.matches("^[0-9-]+$")){
                    JOptionPane.showMessageDialog(null,"ISBN must contain numbers and - only!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                
                 if(isbn.length()<10 || isbn.length()>17){
                    JOptionPane.showMessageDialog(null,"ISBN length is invalid","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                 
                 if(!category.matches(".*[a-zA-Z]+.*")){
                    JOptionPane.showMessageDialog(null,"Book category must contain at least one letter!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}

                try (Connection con = DBConnection.getConnection()) {
                    PreparedStatement check = con.prepareStatement("SELECT * FROM books WHERE isbn=?");
                    check.setString(1,isbn);
                    if (check.executeQuery().next()) {
                        throw new duplicateException("ISBN already exists");
                    }

                    PreparedStatement insert = con.prepareStatement("INSERT INTO books(title,author,isbn,category) VALUES(?,?,?,?)");
                    insert.setString(1, title);
                    insert.setString(2, author);
                    insert.setString(3, isbn);
                    insert.setString(4, category);
                    insert.executeUpdate();

                    loadBooks();
                    clearFields();
                    JOptionPane.showMessageDialog(null,"Book added Successfully!");
                }

            } catch (emptyException ee) {
                JOptionPane.showMessageDialog(null,ee.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            } catch (duplicateException ed) {
                JOptionPane.showMessageDialog(null,ed.getMessage(),"Duplicate",JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,"An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public class updateBook implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
             try{
                int row = tblBooks.getSelectedRow();
                if (row == -1)
                    throw new emptySelectionException("Select a book from table first!");

                String id = tblBooks.getValueAt(row, 0).toString();
                String title = tTitle.getText();
                String author = tAuthor.getText();
                String isbn = tIsbn.getText();
                String category = tCategory.getText();
                
                if (title.isEmpty() || author.isEmpty() || isbn.isEmpty())
                    throw new emptyException("All fields are required");
                
                if(!title.matches(".*[a-zA-Z]+.*")){
                    JOptionPane.showMessageDialog(null,"Book title must contain at least one letter!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                
                if(!author.matches("^[a-zA-Z\\s._-]+$")){
                    JOptionPane.showMessageDialog(null,"Author name contains invalid characters!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;} 
                
                if(!isbn.matches("^[0-9-]+$")){
                    JOptionPane.showMessageDialog(null,"ISBN must contain numbers and - only!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                
                 if(isbn.length()<10 || isbn.length()>17){
                    JOptionPane.showMessageDialog(null,"ISBN length is invalid","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                 
                 if(!category.matches(".*[a-zA-Z]+.*")){
                    JOptionPane.showMessageDialog(null,"Book category must contain at least one letter!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                 
                try(Connection con = DBConnection.getConnection()){
                    String sql = "UPDATE books SET title=?, author=?, isbn=?, category=? WHERE book_id=?";
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setString(1, title);
                    ps.setString(2, author);
                    ps.setString(3, isbn);
                    ps.setString(4, category);
                    ps.setString(5, id);

                    ps.executeUpdate();
                    loadBooks();
                    clearFields();
                    JOptionPane.showMessageDialog(null, "Book updated Successfully!");
                }

            } catch (emptySelectionException es) {
                JOptionPane.showMessageDialog(null, es.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            } catch (emptyException ee) {
                JOptionPane.showMessageDialog(null, ee.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public class removeBook implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int row = tblBooks.getSelectedRow();
                if (row == -1)
                    throw new emptySelectionException("Select a book from table first!");
                
                int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this book?\nThis action cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice == 1) 
                    return; 
                
                int id = Integer.parseInt(tblBooks.getValueAt(row, 0).toString());

                try (Connection con = DBConnection.getConnection()) {

                    PreparedStatement check = con.prepareStatement("SELECT * FROM copies WHERE book_id=? AND status='Borrowed'");
                    check.setInt(1, id);
                    if (check.executeQuery().next())
                        throw new deleteException("Cannot delete: Book has active borrowed copies");

                    PreparedStatement delCopies = con.prepareStatement("DELETE FROM copies WHERE book_id=?");
                    delCopies.setInt(1, id);
                    delCopies.executeUpdate();

                    PreparedStatement delBook = con.prepareStatement("DELETE FROM books WHERE book_id=?");
                    delBook.setInt(1, id);
                    delBook.executeUpdate();

                    loadBooks();
                    clearFields();
                    JOptionPane.showMessageDialog(null, "Book deleted Successfully!");
                }

            } catch (emptySelectionException es) {
                JOptionPane.showMessageDialog(null, es.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            } catch (deleteException de) {
                JOptionPane.showMessageDialog(null, de.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public class getRow extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e){
            int row = tblBooks.getSelectedRow();
            tTitle.setText(tblBooks.getValueAt(row, 1).toString());
            tAuthor.setText(tblBooks.getValueAt(row, 2).toString());
            tIsbn.setText(tblBooks.getValueAt(row, 3).toString());
            tCategory.setText(tblBooks.getValueAt(row, 4).toString());
        }
    }

    private void clearFields(){
        tTitle.setText("");
        tAuthor.setText("");
        tIsbn.setText("");
        tCategory.setText("");
    }

    public class emptyException extends Exception {
        public emptyException(String message){
            super(message);
        }
    }

    public class duplicateException extends Exception {
        public duplicateException(String message){
            super(message);
        }
    }

    public class emptySelectionException extends Exception {
        public emptySelectionException(String message){
            super(message);
        }
    }

    public class deleteException extends Exception{
        public deleteException(String message) {
            super(message);
        }
    }
}
