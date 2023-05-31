package edumsg.loadBalancer.admin;

import edumsg.loadBalancer.loadBalancerServer;

import javax.swing.*;
import java.awt.*;

public class AppInfo extends JPanel {
    private JPanel MainPanel;
    private String ip;
    private String id;
    private boolean in_service;
    private boolean run;
    private int incomplete_req;
    private double avg_response_time;
    private int capacity;
    private int maxCapacity;

    private JLabel ipLabel;
    private JLabel idLabel;
    private JLabel in_serviceLabel;
    private JLabel runLabel;
    private JLabel incomplete_reqLabel;
    private JLabel avg_response_timeLabel;
    private JLabel capacityLabel;
    private JLabel maxCapacityLabel;

    public AppInfo() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
        setVisible(true);

        //addHeader(appName);

        addScrollablePanel();

        ipLabel = new JLabel("Host ip address: " + ip);
        ipLabel.setBackground(new java.awt.Color(0, 0, 0));
        ipLabel.setForeground(new java.awt.Color(255, 255, 255));
        ipLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(ipLabel);

        idLabel = new JLabel("id: " + id);
        idLabel.setBackground(new java.awt.Color(0, 0, 0));
        idLabel.setForeground(new java.awt.Color(255, 255, 255));
        idLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(idLabel);

        in_serviceLabel = new JLabel("In service: " + String.valueOf(in_service));
        in_serviceLabel.setBackground(new java.awt.Color(0, 0, 0));
        in_serviceLabel.setForeground(new java.awt.Color(255, 255, 255));
        in_serviceLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(in_serviceLabel);

        runLabel = new JLabel("Run: " + String.valueOf(run));
        runLabel.setBackground(new java.awt.Color(0, 0, 0));
        runLabel.setForeground(new java.awt.Color(255, 255, 255));
        runLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(runLabel);

        incomplete_reqLabel = new JLabel("Incomplete requests: " + String.valueOf(incomplete_req));
        incomplete_reqLabel.setBackground(new java.awt.Color(0, 0, 0));
        incomplete_reqLabel.setForeground(new java.awt.Color(255, 255, 255));
        incomplete_reqLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(incomplete_reqLabel);

        avg_response_timeLabel = new JLabel("Average response time: " + String.valueOf(avg_response_time));
        avg_response_timeLabel.setBackground(new java.awt.Color(0, 0, 0));
        avg_response_timeLabel.setForeground(new java.awt.Color(255, 255, 255));
        avg_response_timeLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(avg_response_timeLabel);

        capacityLabel = new JLabel("Capacity: " + String.valueOf(capacity));
        capacityLabel.setBackground(new java.awt.Color(0, 0, 0));
        capacityLabel.setForeground(new java.awt.Color(255, 255, 255));
        capacityLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(capacityLabel);

        maxCapacityLabel = new JLabel("Maximum capacity: " + String.valueOf(maxCapacity));
        maxCapacityLabel.setBackground(new java.awt.Color(0, 0, 0));
        maxCapacityLabel.setForeground(new java.awt.Color(255, 255, 255));
        maxCapacityLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        MainPanel.add(maxCapacityLabel);
        loadBalancerServer.gui.addPanel(this);

        revalidate();
        repaint();
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        maxCapacityLabel.setText("Maximum capacity: " + String.valueOf(maxCapacity));
        revalidate();
        repaint();
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        capacityLabel.setText("Capacity: " + String.valueOf(capacity));
        revalidate();
        repaint();
    }

    public void setAvg_response_time(double avg_response_time) {
        this.avg_response_time = avg_response_time;
        avg_response_timeLabel.setText("Average response time: " + String.valueOf(avg_response_time));
        revalidate();
        repaint();
    }

    public void setRun(boolean run) {
        this.run = run;
        runLabel.setText("Run: " + String.valueOf(run));
        revalidate();
        repaint();
    }

    public void setIncomplete_req(int incomplete_req) {
        this.incomplete_req = incomplete_req;
        incomplete_reqLabel.setText("Incomplete requests: " + String.valueOf(incomplete_req));
        revalidate();
        repaint();
    }

    public void setIn_service(boolean in_service) {
        this.in_service = in_service;
        in_serviceLabel.setText("In service: " + String.valueOf(in_service));
        revalidate();
        repaint();
    }

    public void setIp(String ip) {
        this.ip = ip;
        ipLabel.setText("Host ip address: " + ip);
        revalidate();
        repaint();
    }

    public void setId(String id) {
        this.id = id;
        idLabel.setText("id: " + id);
        revalidate();
        repaint();
    }


    public void addScrollablePanel() {
        MainPanel = new JPanel();
        MainPanel.setLayout(new BoxLayout(MainPanel, BoxLayout.Y_AXIS));
        MainPanel.setBackground(new java.awt.Color(0, 0, 0));
        MainPanel.setForeground(new java.awt.Color(255, 255, 255));
        JScrollPane scrollPane = new JScrollPane(MainPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane);
    }


}