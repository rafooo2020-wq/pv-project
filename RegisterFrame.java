import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RegisterFrame extends JFrame {
    private final JTextField txtName = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final JButton btnRegister = new JButton("Register");

    public RegisterFrame() {
        setTitle("Register - Library");
        setSize(420, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(14,14,14,14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx=0; c.gridy=0; panel.add(new JLabel("Full Name:"), c);
        c.gridx=1; panel.add(txtName, c);

        c.gridx=0; c.gridy=1; panel.add(new JLabel("Email:"), c);
        c.gridx=1; panel.add(txtEmail, c);

        c.gridx=0; c.gridy=2; panel.add(new JLabel("Password:"), c);
        c.gridx=1; panel.add(txtPassword, c);

        c.gridx=0; c.gridy=3; panel.add(new JLabel("Confirm Password:"), c);
        c.gridx=1; panel.add(txtConfirm, c);

        c.gridx=0; c.gridy=4; panel.add(new JLabel(""), c);
        c.gridx=1; panel.add(btnRegister, c);

        add(panel);

        btnRegister.addActionListener(e -> onRegister());
    }

    private void onRegister() {
        String name = txtName.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String conf = new String(txtConfirm.getPassword());

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || conf.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Invalid email format.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pass.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pass.equals(conf)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String hash = sha256(pass);

        try (Connection con = DBConnection.getConnection()) {
            // duplicate email?
            try (PreparedStatement chk = con.prepareStatement("SELECT member_id FROM members WHERE email = ?")) {
                chk.setString(1, email);
                ResultSet rs = chk.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Email already registered.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            try (PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO members (name, email, password_hash, role) VALUES (?, ?, ?, 'Member')")) {
                ins.setString(1, name);
                ins.setString(2, email);
                ins.setString(3, hash);
                ins.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Registration successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}
