package ai.ignition.desktop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Native Windows Desktop Workstation for Pre-Owned Vehicle Health Inspection.
 * Built using pure Java Swing with native OS look and feel.
 */
public class DealershipDesktopApp extends JFrame {

    private JComboBox<String> portSelector;
    private JButton connectBtn;
    private JButton loadReplayBtn;
    private JLabel statusLabel;

    // Telemetry display labels
    private JLabel rpmLabel;
    private JLabel speedLabel;
    private JLabel tempLabel;
    private JLabel loadLabel;
    private JLabel dtcLabel;

    // Health metrics
    private JProgressBar vhiBar;
    private JLabel vhiScoreLabel;
    private JLabel severityBadge;
    private JProgressBar powertrainBar;
    private JProgressBar emissionsBar;
    private JProgressBar coolingBar;
    private JProgressBar electricalBar;

    // Tables
    private DefaultTableModel anomalyTableModel;
    private JTable anomalyTable;
    private DefaultTableModel historyTableModel;
    private JTable historyTable;

    public DealershipDesktopApp() {
        super("IgnitionAI 2.0 — Pre-Owned Dealership Inspection Workstation");
        initUI();
    }

    private void initUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(createTopToolBar(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createLeftDashboardPanel(), createRightDetailsPanel());
        mainSplit.setDividerLocation(420);
        mainSplit.setResizeWeight(0.35);
        add(mainSplit, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JToolBar createTopToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new EmptyBorder(5, 8, 5, 8));

        toolbar.add(new JLabel("Port: "));
        portSelector = new JComboBox<>(new String[]{"COM1", "COM3 (ELM327 Bluetooth)", "COM4"});
        portSelector.setMaximumSize(new Dimension(200, 28));
        toolbar.add(portSelector);

        toolbar.add(Box.createHorizontalStrut(8));
        connectBtn = new JButton("Connect Adapter");
        connectBtn.addActionListener(e -> toggleConnection());
        toolbar.add(connectBtn);

        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.addSeparator();
        toolbar.add(Box.createHorizontalStrut(10));

        loadReplayBtn = new JButton("Load OBD Replay (.json)");
        loadReplayBtn.addActionListener(e -> loadReplayFile());
        toolbar.add(loadReplayBtn);

        toolbar.add(Box.createHorizontalGlue());

        JButton simBtn = new JButton("Open OBD Simulator");
        simBtn.setBackground(new Color(102, 16, 242));
        simBtn.setForeground(Color.WHITE);
        simBtn.addActionListener(e -> new ai.ignition.simulator.ObdSimulatorApp().setVisible(true));
        toolbar.add(simBtn);

        toolbar.add(Box.createHorizontalStrut(8));

        JButton exportCertBtn = new JButton("Export Health Certificate (PDF)");
        exportCertBtn.setBackground(new Color(24, 115, 204));
        exportCertBtn.setForeground(Color.WHITE);
        exportCertBtn.addActionListener(e -> exportCertificate());
        toolbar.add(exportCertBtn);

        return toolbar;
    }

    private JPanel createLeftDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 5));

        // 1. Live OBD-II Telemetry Card
        JPanel telemetryPanel = new JPanel(new GridLayout(5, 2, 8, 4));
        telemetryPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Live Telemetry Snapshot",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        telemetryPanel.add(new JLabel("Engine RPM:"));
        rpmLabel = new JLabel("--- RPM");
        telemetryPanel.add(rpmLabel);

        telemetryPanel.add(new JLabel("Vehicle Speed:"));
        speedLabel = new JLabel("--- km/h");
        telemetryPanel.add(speedLabel);

        telemetryPanel.add(new JLabel("Coolant Temp:"));
        tempLabel = new JLabel("--- °C");
        telemetryPanel.add(tempLabel);

        telemetryPanel.add(new JLabel("Calculated Load:"));
        loadLabel = new JLabel("--- %");
        telemetryPanel.add(loadLabel);

        telemetryPanel.add(new JLabel("Active DTCs:"));
        dtcLabel = new JLabel("None detected");
        dtcLabel.setForeground(new Color(0, 130, 0));
        telemetryPanel.add(dtcLabel);

        panel.add(telemetryPanel);
        panel.add(Box.createVerticalStrut(10));

        // 2. Overall Vehicle Health Index (VHI) Card
        JPanel vhiPanel = new JPanel(new BorderLayout(5, 8));
        vhiPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Vehicle Health Index (VHI)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        vhiScoreLabel = new JLabel("92.4 / 100", SwingConstants.CENTER);
        vhiScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        vhiScoreLabel.setForeground(new Color(15, 120, 30));
        vhiPanel.add(vhiScoreLabel, BorderLayout.NORTH);

        vhiBar = new JProgressBar(0, 100);
        vhiBar.setValue(92);
        vhiBar.setStringPainted(true);
        vhiBar.setForeground(new Color(40, 167, 69));
        vhiPanel.add(vhiBar, BorderLayout.CENTER);

        severityBadge = new JLabel("Severity: LOW (Within Normal Operating Limits)", SwingConstants.CENTER);
        severityBadge.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        vhiPanel.add(severityBadge, BorderLayout.SOUTH);

        panel.add(vhiPanel);
        panel.add(Box.createVerticalStrut(10));

        // 3. Subsystem Health Breakdown
        JPanel subPanel = new JPanel(new GridLayout(4, 2, 6, 8));
        subPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Subsystem Health Scores",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        subPanel.add(new JLabel("Powertrain:"));
        powertrainBar = createSubsystemBar(94);
        subPanel.add(powertrainBar);

        subPanel.add(new JLabel("Cooling System:"));
        coolingBar = createSubsystemBar(88);
        subPanel.add(coolingBar);

        subPanel.add(new JLabel("Emissions & Cat:"));
        emissionsBar = createSubsystemBar(91);
        subPanel.add(emissionsBar);

        subPanel.add(new JLabel("Electrical / Battery:"));
        electricalBar = createSubsystemBar(96);
        subPanel.add(electricalBar);

        panel.add(subPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JProgressBar createSubsystemBar(int val) {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(val);
        bar.setStringPainted(true);
        bar.setString(val + "%");
        bar.setForeground(val > 80 ? new Color(40, 167, 69) : val > 60 ? new Color(255, 193, 7) : new Color(220, 53, 69));
        return bar;
    }

    private JTabbedPane createRightDetailsPanel() {
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Anomaly & Episode Evidence Store
        String[] anomalyCols = {"Timestamp", "Subsystem", "Signal", "Residual", "Uncertainty", "State"};
        anomalyTableModel = new DefaultTableModel(anomalyCols, 0);
        anomalyTable = new JTable(anomalyTableModel);
        tabs.addTab("Detected Episodes & Evidence", new JScrollPane(anomalyTable));

        // Tab 2: Historical Vehicle Assessments
        String[] historyCols = {"Assessment ID", "Date", "VIN", "VHI Score", "Severity", "Technician"};
        historyTableModel = new DefaultTableModel(historyCols, 0);
        historyTable = new JTable(historyTableModel);
        populateSampleHistory();
        tabs.addTab("Historical Comparisons", new JScrollPane(historyTable));

        return tabs;
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new EmptyBorder(4, 10, 4, 10));
        statusLabel = new JLabel("Status: Ready. Please connect an OBD-II adapter or load a replay session.");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("IgnitionAI Engine: v2.0-RELEASE");
        versionLabel.setForeground(Color.GRAY);
        statusPanel.add(versionLabel, BorderLayout.EAST);
        return statusPanel;
    }

    private void toggleConnection() {
        if ("Connect Adapter".equals(connectBtn.getText())) {
            connectBtn.setText("Disconnect");
            statusLabel.setText("Status: Connected to " + portSelector.getSelectedItem() + ". Polling PIDs...");
            simulateLiveReadings();
        } else {
            connectBtn.setText("Connect Adapter");
            statusLabel.setText("Status: Disconnected.");
        }
    }

    private void simulateLiveReadings() {
        rpmLabel.setText("2,140 RPM");
        speedLabel.setText("64 km/h");
        tempLabel.setText("89 °C");
        loadLabel.setText("38 %");
        dtcLabel.setText("P0420 (History: Catalyst Bank 1)");
        dtcLabel.setForeground(new Color(200, 100, 0));

        anomalyTableModel.addRow(new Object[]{
                new SimpleDateFormat("HH:mm:ss").format(new Date()),
                "Emissions", "O2S12_Voltage", "+0.18V", "±0.04V", "DEGRADED"
        });
    }

    private void loadReplayFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select OBD Replay Log (.json)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            statusLabel.setText("Loaded replay session: " + file.getName());
            simulateLiveReadings();
        }
    }

    private void exportCertificate() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Vehicle Health Certificate");
        chooser.setSelectedFile(new File("Vehicle_Health_Certificate.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File dest = chooser.getSelectedFile();
        if (!dest.getName().toLowerCase().endsWith(".pdf")) {
            dest = new File(dest.getAbsolutePath() + ".pdf");
        }

        try {
            byte[] pdf = buildPdfBytes();
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(pdf);
            }
            statusLabel.setText("Certificate saved: " + dest.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Health Certificate saved:\n" + dest.getAbsolutePath(),
                    "Certificate Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to write PDF:\n" + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Generates a valid PDF/1.4 document using only the Java standard library.
     * Encodes vehicle health report content as literal PDF streams.
     */
    private byte[] buildPdfBytes() throws IOException {
        String ts   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String vhi  = vhiScoreLabel.getText();
        String sev  = severityBadge.getText();
        String rpm  = rpmLabel.getText();
        String spd  = speedLabel.getText();
        String tmp  = tempLabel.getText();
        String ld   = loadLabel.getText();
        String dtc  = dtcLabel.getText();
        String pt   = powertrainBar.getString();
        String cl   = coolingBar.getString();
        String em   = emissionsBar.getString();
        String el   = electricalBar.getString();

        // Collect anomaly rows
        StringBuilder anomalies = new StringBuilder();
        for (int r = 0; r < anomalyTableModel.getRowCount(); r++) {
            anomalies.append(String.format("  %-10s %-14s %-18s %-10s %-10s %s\n",
                anomalyTableModel.getValueAt(r, 0),
                anomalyTableModel.getValueAt(r, 1),
                anomalyTableModel.getValueAt(r, 2),
                anomalyTableModel.getValueAt(r, 3),
                anomalyTableModel.getValueAt(r, 4),
                anomalyTableModel.getValueAt(r, 5)));
        }
        if (anomalies.length() == 0) anomalies.append("  None detected.\n");

        // Build PDF content stream
        StringBuilder cs = new StringBuilder();
        cs.append("BT\n");
        cs.append("/F1 16 Tf\n");
        cs.append("50 780 Td\n");
        cs.append("(IgnitionAI 2.0 - Vehicle Health Certificate) Tj\n");
        cs.append("/F1 10 Tf\n");
        cs.append("0 -22 Td (Generated: ").append(ts).append(") Tj\n");
        cs.append("0 -20 Td (---------------------------------------------------) Tj\n");
        cs.append("0 -20 Td (VIN: 1HGCR2F83HA00291) Tj\n");
        cs.append("0 -18 Td (Vehicle Health Index: ").append(vhi).append(") Tj\n");
        cs.append("0 -18 Td (").append(sev).append(") Tj\n");
        cs.append("0 -24 Td (-- Live Telemetry --) Tj\n");
        cs.append("0 -18 Td (RPM: ").append(rpm).append("   Speed: ").append(spd).append(") Tj\n");
        cs.append("0 -18 Td (Coolant: ").append(tmp).append("   Load: ").append(ld).append(") Tj\n");
        cs.append("0 -18 Td (DTCs: ").append(dtc.replace("(","[").replace(")","]")).append(") Tj\n");
        cs.append("0 -24 Td (-- Subsystem Health --) Tj\n");
        cs.append("0 -18 Td (Powertrain: ").append(pt).append("   Cooling: ").append(cl).append(") Tj\n");
        cs.append("0 -18 Td (Emissions: ").append(em).append("   Electrical: ").append(el).append(") Tj\n");
        cs.append("0 -24 Td (-- Detected Episodes --) Tj\n");
        for (String line : anomalies.toString().split("\n")) {
            String safe = line.replace("(", "[").replace(")", "]");
            cs.append("0 -16 Td (").append(safe).append(") Tj\n");
        }
        cs.append("0 -30 Td (-- END OF REPORT --) Tj\n");
        cs.append("ET\n");

        byte[] stream = cs.toString().getBytes("ISO-8859-1");

        // Assemble PDF objects
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");

        // Object 1: Catalog
        int[] offsets = new int[6];
        offsets[0] = pdf.length();
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // Object 2: Pages
        offsets[1] = pdf.length();
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        // Object 3: Page
        offsets[2] = pdf.length();
        pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R ");
        pdf.append("/MediaBox [0 0 595 842] ");
        pdf.append("/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

        // Object 4: Content stream
        offsets[3] = pdf.length();
        pdf.append("4 0 obj\n<< /Length ").append(stream.length).append(" >>\nstream\n");
        pdf.append(cs);
        pdf.append("endstream\nendobj\n");

        // Object 5: Font
        offsets[4] = pdf.length();
        pdf.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        // Cross-reference table
        int xrefOffset = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 0; i < 5; i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        pdf.append("trailer\n<< /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

        return pdf.toString().getBytes("ISO-8859-1");
    }

    private void populateSampleHistory() {
        historyTableModel.addRow(new Object[]{"VHA-2026-0811", "2026-08-11", "1HGCR2F83HA00291", "95.2", "NORMAL", "John Doe"});
        historyTableModel.addRow(new Object[]{"VHA-2026-0918", "2026-09-18", "1HGCR2F83HA00291", "92.4", "LOW", "John Doe"});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DealershipDesktopApp().setVisible(true);
        });
    }
}
