package edumsg.loadBalancer.admin;

import javax.swing.*;
import java.awt.*;

public class AdminGUI extends JFrame {
    private JPanel MainPanel;

    public AdminGUI() {
        super("EduMsg Admin");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        MainPanel = new JPanel();
        MainPanel.setLayout(new BoxLayout(MainPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(MainPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane);
        MainPanel.setLayout(new GridLayout(0, 2));
        revalidate();
        repaint();

    }

    public void addPanel(AppInfo panel) {
        MainPanel.add(panel);
        revalidate();
        repaint();
    }
}