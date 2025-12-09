import javax.swing.*;
import java.awt.event.*;
import java.sql.*;
import java.security.MessageDigest;

public class LoginFrame extends JFrame {
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginFrame() {
        setTitle("Login");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setLocationRelativeTo(null);

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setBounds(30, 30, 60, 25);
        add(lblEmail);

        txtEmail = new JTextField();
        txtEmail.setBounds(100, 30, 150, 25);
        add(txtEmail);

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setBounds(30, 70, 70, 25);
        add(lblPassword);

        txtPassword = new JPasswordField();
        txtPassword.setBounds(100, 70, 150, 25);
        add(txtPassword);

        btnLogin = new JButton("Login");
        btnLogin.setBounds(50, 120, 100, 25);
        add(btnLogin);
        btnLogin.addActionListener(e -> login());

        btnRegister = new JButton("Register");
        btnRegister.setBounds(180, 120, 100, 25);
        add(btnRegister);
        btnRegister.addActionListener(e -> {
            RegisterFrame register = new RegisterFrame();
            register.setVisible(true);
        });
    }

    private void login() {
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in both Email and Password", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            JOptionPane.showMessageDialog(this, "Invalid email format", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String hashed = hashPassword(password);
        if (hashed == null) {
            JOptionPane.showMessageDialog(this, "Error hashing the password", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM members WHERE email=? AND password_hash=?")) {

            ps.setString(1, email);
            ps.setString(2, hashed);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                int memberId = rs.getInt("member_id");
                JOptionPane.showMessageDialog(this, "Login successful as " + role);
                dispose();
                new MainApplicationFrame(role, memberId).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid email or password", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
