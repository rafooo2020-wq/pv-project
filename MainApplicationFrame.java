import javax.swing.*;

public class MainApplicationFrame extends JFrame {
    private JTabbedPane tabs;

    public MainApplicationFrame(String userRole,int memberId) {
        setTitle("Library System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabs = new JTabbedPane();

        if (userRole.equals("Librarian")) {
            tabs.addTab("Manage Books", new ManageBooksPanel());
            tabs.addTab("Manage Members", new ManageMembersPanel());
            tabs.addTab("Manage Copies", new ManageCopiesPanel(userRole));
            tabs.addTab("Manage Holds", new HoldsPanel(userRole, memberId));
            tabs.addTab("Reports", new ReportsPanel());
            
        }

        if (userRole.equals("Member")) {
         tabs.addTab("Search Books", new SearchPanel(memberId));
         tabs.addTab("My Loans", new MyLoansPanel(memberId));
         tabs.addTab("My Holds", new HoldsPanel(userRole, memberId)); 
          
        }

        add(tabs);
    }
}
