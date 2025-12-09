
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class ManageMembersPanel extends JPanel {

    private JTable tblMembers;
    private JLabel lName, lEmail;
    private JTextField tName, tEmail;
    private JButton bAdd, bUpdate, bRemove;

    public ManageMembersPanel() {

        setLayout(new BorderLayout());

        lName = new JLabel("Name:");
        tName = new JTextField(15);

        lEmail = new JLabel("Email:");
        tEmail = new JTextField(15);

        bAdd = new JButton("Add");
        bUpdate = new JButton("Update");
        bRemove = new JButton("Remove");

        JPanel gp = new JPanel(new GridLayout(2, 2, 5, 3));
        gp.add(lName);
        gp.add(lEmail);
        gp.add(tName);
        gp.add(tEmail);
        
        JPanel bp = new JPanel();
        bp.add(bAdd);
        bp.add(bUpdate);
        bp.add(bRemove);

        JPanel top = new JPanel(new BorderLayout());
        top.add(gp, BorderLayout.CENTER);
        top.add(bp, BorderLayout.SOUTH);

        tblMembers = new JTable();
        JScrollPane sp = new JScrollPane(tblMembers);

        add(top, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);

        loadMembers();

        bAdd.addActionListener(new addMember());
        bUpdate.addActionListener(new updateMember());
        bRemove.addActionListener(new removeMember());
        tblMembers.addMouseListener(new getRow());
    }

    private void loadMembers() {
        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT * FROM members";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            String[] columns = {"ID", "Name", "Email"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("member_id"),
                    rs.getString("name"),
                    rs.getString("email"),});
            }

            tblMembers.setModel(model);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public class addMember implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String name = tName.getText();
                String email = tEmail.getText();

                if (name.isEmpty() || email.isEmpty()) {
                    throw new emptyException("All fields are required");
                }
                if(!name.matches("^[a-zA-Z\\s._-]+$")){
                    JOptionPane.showMessageDialog(null,"Name contains invalid characters!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                
                 if(!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")){
                    JOptionPane.showMessageDialog(null,"Invalid email format!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}

                try (Connection con = DBConnection.getConnection()) {

                    PreparedStatement check = con.prepareStatement("SELECT * FROM members WHERE email=?");
                    check.setString(1, email);
                    if (check.executeQuery().next()) {
                        throw new duplicateException("This email is already registered!");
                    }

                    String defaultPassHash = "8d969eef6ecad3c29a3a629280e686cff8cae2f5a708f5d22a02b19e";
                    
                    PreparedStatement insert = con.prepareStatement("INSERT INTO members(name, email, password_hash, role) VALUES(?, ?, ?, ?)");
                    insert.setString(1, name);
                    insert.setString(2, email);
                    insert.setString(3, defaultPassHash);
                    insert.setString(4, "Member");
                    
                    insert.executeUpdate();

                    loadMembers();
                    clearFields();
                    JOptionPane.showMessageDialog(null, "Member added Successfully!");
                }

            } catch (emptyException ee) {
                JOptionPane.showMessageDialog(null, ee.getMessage(), "Validation", JOptionPane.ERROR_MESSAGE);
            } catch (duplicateException de) {
                JOptionPane.showMessageDialog(null, de.getMessage(), "Duplicate", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public class updateMember implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int row = tblMembers.getSelectedRow();
                if (row == -1)
                    throw new emptySelectionException("Select a member from table first!");

                int id = Integer.parseInt(tblMembers.getValueAt(row, 0).toString());
                String name = tName.getText();
                String email = tEmail.getText();

                if (name.isEmpty() || email.isEmpty())
                    throw new emptyException("All fields are required");
                
                 if(!name.matches("^[a-zA-Z\\s._-]+$")){
                    JOptionPane.showMessageDialog(null,"Name contains invalid characters!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}
                
                 if(!email.matches("^[a-zA-Z0-9+_.-]+@(.+)$")){
                    JOptionPane.showMessageDialog(null,"Invalid email format!","Warning",JOptionPane.WARNING_MESSAGE);
                    return;}

                try (Connection con = DBConnection.getConnection()) {
                    String sql = "UPDATE members SET name=?, email=? WHERE member_id=?";
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setInt(3, id);

                    ps.executeUpdate();

                    loadMembers();
                    clearFields();
                    JOptionPane.showMessageDialog(null, "Member updated Successfully!");
                }

            } catch (emptySelectionException es) {
                JOptionPane.showMessageDialog(null, es.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            } catch (emptyException ee) {
                JOptionPane.showMessageDialog(null, ee.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "An unexpected error occurred while accessing the database.\nError Details:" +ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public class removeMember implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int row = tblMembers.getSelectedRow();
                if (row == -1)
                    throw new emptySelectionException("Select a member from table first!");

                int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this member?\nThis action cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice == 1) 
                    return; 
                int id = Integer.parseInt(tblMembers.getValueAt(row, 0).toString());

                try (Connection con = DBConnection.getConnection()) {

                    PreparedStatement check = con.prepareStatement("SELECT * FROM loans WHERE member_id=? AND return_date IS NULL");
                    check.setInt(1, id);
                    if (check.executeQuery().next())
                        throw new deleteException("Cannot delete member: This member still has unreturned books!");

                    PreparedStatement del = con.prepareStatement("DELETE FROM members WHERE member_id=?");
                    del.setInt(1, id);
                    del.executeUpdate();

                    loadMembers();
                    clearFields();
                    JOptionPane.showMessageDialog(null, "Member deleted Successfully!");
                }

            } catch (emptySelectionException es) {
                JOptionPane.showMessageDialog(null,es.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            } catch (deleteException de) {
                JOptionPane.showMessageDialog(null,de.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,"An unexpected error occurred while accessing the database.\nError Details: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public class getRow extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int row = tblMembers.getSelectedRow();
            tName.setText(tblMembers.getValueAt(row,1).toString());
            tEmail.setText(tblMembers.getValueAt(row,2).toString());
        }
    }

    private void clearFields(){
        tName.setText("");
        tEmail.setText("");
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

    public class deleteException extends Exception {
        public deleteException(String message){
            super(message); 
        }      
    }
}

