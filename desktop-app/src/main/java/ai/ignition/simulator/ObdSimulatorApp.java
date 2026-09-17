package ai.ignition.simulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * IgnitionAI 2.0 — OBD-II Signal Simulator
 *
 * Simulates a live vehicle OBD-II data stream with configurable fault injection,
 * noise levels, and driving scenarios. Feeds the Phase 2-6 analysis pipeline.
 */
public class ObdSimulatorApp extends JFrame {

    // ── Scenario definitions ──────────────────────────────────────────────────
    private static final String[] SCENARIOS = {
        "Normal City Driving",
        "Highway Cruise",
        "Cold Start Warmup",
        "Degraded O2 Sensor",
        "Coolant Leak (Overheating)",
        "Misfiring Cylinder 3",
        "Weak Battery / Electrical Fault",
        "Catalytic Converter Aging"
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private String currentScenario = SCENARIOS[0];
    private volatile boolean running = false;
    private Timer pollTimer;
    private int tickCount = 0;
    private final List<Map<String, Object>> sessionLog = new ArrayList<>();

    // ── UI components ─────────────────────────────────────────────────────────
    private JComboBox<String> scenarioPicker;
    private JSlider noiseSlider;
    private JButton startBtn;
    private JButton stopBtn;
    private JButton exportBtn;
    private JLabel tickLabel;
    private JLabel sessionLabel;

    // Live gauges
    private JProgressBar rpmGauge;
    private JProgressBar speedGauge;
    private JProgressBar tempGauge;
    private JProgressBar loadGauge;
    private JProgressBar o2Gauge;
    private JProgressBar voltGauge;

    private JLabel rpmVal;
    private JLabel speedVal;
    private JLabel tempVal;
    private JLabel loadVal;
    private JLabel o2Val;
    private JLabel voltVal;
    private JLabel dtcLabel;
    private JLabel anomalyLabel;

    // Signal log table
    private DefaultTableModel logModel;

    public ObdSimulatorApp() {
        super("IgnitionAI 2.0 — OBD-II Signal Simulator");
        initUI();
    }

    private void initUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(createControlBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createGaugePanel(), createLogPanel());
        split.setDividerLocation(420);
        split.setResizeWeight(0.38);
        add(split, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONTROL BAR
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        bar.add(new JLabel("Scenario:"));
        scenarioPicker = new JComboBox<>(SCENARIOS);
        scenarioPicker.setPreferredSize(new Dimension(220, 26));
        scenarioPicker.addActionListener(e -> currentScenario = (String) scenarioPicker.getSelectedItem());
        bar.add(scenarioPicker);

        bar.add(new JLabel("  Noise:"));
        noiseSlider = new JSlider(0, 100, 20);
        noiseSlider.setPreferredSize(new Dimension(120, 26));
        noiseSlider.setToolTipText("Signal noise level (0 = clean, 100 = very noisy)");
        bar.add(noiseSlider);

        bar.add(Box.createHorizontalStrut(10));

        startBtn = new JButton("▶  Start Simulation");
        startBtn.setBackground(new Color(40, 167, 69));
        startBtn.setForeground(Color.WHITE);
        startBtn.addActionListener(e -> startSimulation());
        bar.add(startBtn);

        stopBtn = new JButton("■  Stop");
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopSimulation());
        bar.add(stopBtn);

        bar.add(Box.createHorizontalStrut(10));

        exportBtn = new JButton("Export Session Log (.json)");
        exportBtn.addActionListener(e -> exportLog());
        bar.add(exportBtn);

        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GAUGE PANEL
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createGaugePanel() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(new EmptyBorder(8, 10, 8, 6));

        outer.add(makeGaugeGroup("OBD-II Live Signal Gauges"));

        JPanel grid = new JPanel(new GridLayout(6, 3, 6, 10));
        grid.setBorder(new EmptyBorder(8, 4, 8, 4));

        // RPM  0-8000
        rpmGauge = makeBar(0, 8000, new Color(30, 120, 220)); rpmVal = new JLabel("0 RPM");
        grid.add(new JLabel("Engine RPM")); grid.add(rpmGauge); grid.add(rpmVal);

        // Speed 0-200
        speedGauge = makeBar(0, 200, new Color(60, 180, 75)); speedVal = new JLabel("0 km/h");
        grid.add(new JLabel("Speed")); grid.add(speedGauge); grid.add(speedVal);

        // Coolant 0-130
        tempGauge = makeBar(0, 130, new Color(220, 80, 30)); tempVal = new JLabel("--- °C");
        grid.add(new JLabel("Coolant Temp")); grid.add(tempGauge); grid.add(tempVal);

        // Engine Load 0-100%
        loadGauge = makeBar(0, 100, new Color(180, 130, 30)); loadVal = new JLabel("--- %");
        grid.add(new JLabel("Engine Load")); grid.add(loadGauge); grid.add(loadVal);

        // O2 voltage  0-1000 (maps to 0.000-1.000V)
        o2Gauge = makeBar(0, 1000, new Color(100, 60, 200)); o2Val = new JLabel("--- V");
        grid.add(new JLabel("O2 Sensor")); grid.add(o2Gauge); grid.add(o2Val);

        // Battery 8-16V  mapped to 0-800
        voltGauge = makeBar(0, 800, new Color(40, 167, 120)); voltVal = new JLabel("--- V");
        grid.add(new JLabel("Battery Volt")); grid.add(voltGauge); grid.add(voltVal);

        outer.add(grid);
        outer.add(Box.createVerticalStrut(10));

        // DTC display
        JPanel dtcPanel = new JPanel(new BorderLayout(4, 4));
        dtcPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Active Fault Codes (DTCs)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11)));
        dtcLabel = new JLabel("No DTCs");
        dtcLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        dtcLabel.setForeground(new Color(0, 130, 0));
        dtcPanel.add(dtcLabel, BorderLayout.CENTER);
        outer.add(dtcPanel);

        outer.add(Box.createVerticalStrut(8));

        // Anomaly indicator
        JPanel anomPanel = new JPanel(new BorderLayout());
        anomPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "IgnitionAI Phase 4 Anomaly Decision",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11)));
        anomalyLabel = new JLabel("NOMINAL — no anomalies detected", SwingConstants.CENTER);
        anomalyLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        anomalyLabel.setForeground(new Color(0, 140, 0));
        anomalyLabel.setBorder(new EmptyBorder(6, 4, 6, 4));
        anomPanel.add(anomalyLabel, BorderLayout.CENTER);
        outer.add(anomPanel);

        outer.add(Box.createVerticalGlue());
        return outer;
    }

    private JPanel makeGaugeGroup(String title) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(lbl);
        return p;
    }

    private JProgressBar makeBar(int min, int max, Color color) {
        JProgressBar bar = new JProgressBar(min, max);
        bar.setForeground(color);
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(160, 18));
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOG PANEL
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(8, 4, 8, 10));

        String[] cols = {"Tick", "Time", "RPM", "Speed", "Coolant", "Load%", "O2V", "BatV", "Scenario", "Anomaly"};
        logModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(logModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFont(new Font("Monospaced", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Signal Log (last 500 ticks)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 11)));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS BAR
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EmptyBorder(3, 10, 3, 10));
        tickLabel = new JLabel("Stopped");
        sessionLabel = new JLabel("Session: none");
        sessionLabel.setForeground(Color.GRAY);
        bar.add(tickLabel, BorderLayout.WEST);
        bar.add(sessionLabel, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SIMULATION ENGINE
    // ─────────────────────────────────────────────────────────────────────────
    private void startSimulation() {
        if (running) return;
        running = true;
        tickCount = 0;
        sessionLog.clear();
        String sessionId = "SIM-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        sessionLabel.setText("Session: " + sessionId);
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        pollTimer = new Timer("obd-sim-timer", true);
        pollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> tick());
            }
        }, 0, 500);  // every 500 ms
    }

    private void stopSimulation() {
        running = false;
        if (pollTimer != null) { pollTimer.cancel(); pollTimer = null; }
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        tickLabel.setText("Stopped at tick " + tickCount + "  |  " + sessionLog.size() + " samples recorded.");
    }

    private void tick() {
        if (!running) return;
        tickCount++;
        double noise = noiseSlider.getValue() / 100.0;

        // Generate signals based on scenario
        SignalSet s = generateSignals(currentScenario, tickCount, noise);

        // Update gauges
        rpmGauge.setValue(s.rpm);    rpmVal.setText(s.rpm + " RPM");
        speedGauge.setValue(s.speed); speedVal.setText(s.speed + " km/h");
        tempGauge.setValue(s.temp);   tempVal.setText(s.temp + " °C");
        loadGauge.setValue(s.load);   loadVal.setText(s.load + " %");
        o2Gauge.setValue((int)(s.o2 * 1000));  o2Val.setText(String.format("%.3f V", s.o2));
        voltGauge.setValue((int)((s.voltage - 8.0) / 8.0 * 800)); voltVal.setText(String.format("%.1f V", s.voltage));

        // DTC
        dtcLabel.setText(s.dtcs.isEmpty() ? "No DTCs" : String.join("  |  ", s.dtcs));
        dtcLabel.setForeground(s.dtcs.isEmpty() ? new Color(0,130,0) : new Color(200, 80, 0));

        // Anomaly state
        anomalyLabel.setText(s.anomalyState);
        anomalyLabel.setForeground(
            s.anomalyState.startsWith("NOMINAL") ? new Color(0,140,0) :
            s.anomalyState.startsWith("DEGRADED") ? new Color(200,120,0) :
            new Color(200,30,30));

        // Log table (keep last 500)
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logModel.addRow(new Object[]{
            tickCount, ts, s.rpm, s.speed, s.temp + "°C",
            s.load + "%", String.format("%.3f", s.o2),
            String.format("%.1f", s.voltage),
            abbreviate(currentScenario), s.anomalyState
        });
        if (logModel.getRowCount() > 500) logModel.removeRow(0);

        // JSON log
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tick", tickCount); row.put("ts", ts); row.put("scenario", currentScenario);
        row.put("rpm", s.rpm); row.put("speed", s.speed); row.put("coolant_c", s.temp);
        row.put("load_pct", s.load); row.put("o2_v", s.o2); row.put("battery_v", s.voltage);
        row.put("dtcs", s.dtcs); row.put("anomaly", s.anomalyState);
        sessionLog.add(row);

        tickLabel.setText("Tick " + tickCount + "  |  " + currentScenario);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SIGNAL GENERATION MODEL
    // ─────────────────────────────────────────────────────────────────────────
    private SignalSet generateSignals(String scenario, int tick, double noise) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        SignalSet s = new SignalSet();
        switch (scenario) {
            case "Normal City Driving":
                s.rpm    = clamp((int)(1400 + 400 * Math.sin(tick * 0.15) + rng.nextGaussian() * 60 * noise), 800, 4000);
                s.speed  = clamp((int)(40 + 20 * Math.sin(tick * 0.1) + rng.nextGaussian() * 5 * noise), 0, 80);
                s.temp   = clamp((int)(90 + rng.nextGaussian() * 2 * noise), 80, 100);
                s.load   = clamp((int)(30 + 10 * Math.sin(tick * 0.2) + rng.nextGaussian() * 5 * noise), 10, 70);
                s.o2     = clamp01(0.45 + 0.4 * Math.sin(tick * 0.5) + rng.nextGaussian() * 0.05 * noise);
                s.voltage = clamp(13.8 + rng.nextGaussian() * 0.1 * noise, 12.5, 14.8);
                s.anomalyState = "NOMINAL";
                break;

            case "Highway Cruise":
                s.rpm    = clamp((int)(2200 + rng.nextGaussian() * 50 * noise), 1800, 3000);
                s.speed  = clamp((int)(105 + rng.nextGaussian() * 3 * noise), 90, 130);
                s.temp   = clamp((int)(88 + rng.nextGaussian() * 1.5 * noise), 82, 96);
                s.load   = clamp((int)(55 + rng.nextGaussian() * 4 * noise), 40, 70);
                s.o2     = clamp01(0.45 + 0.38 * Math.sin(tick * 0.8) + rng.nextGaussian() * 0.03 * noise);
                s.voltage = clamp(14.0 + rng.nextGaussian() * 0.05 * noise, 13.5, 14.8);
                s.anomalyState = "NOMINAL";
                break;

            case "Cold Start Warmup":
                double warmFrac = Math.min(1.0, tick / 120.0);
                s.rpm    = clamp((int)(1800 - 600 * warmFrac + rng.nextGaussian() * 80 * noise), 600, 2200);
                s.speed  = 0;
                s.temp   = clamp((int)(20 + 72 * warmFrac + rng.nextGaussian() * 2 * noise), 20, 95);
                s.load   = clamp((int)(20 + 15 * warmFrac + rng.nextGaussian() * 4 * noise), 10, 50);
                s.o2     = clamp01(0.1 + 0.8 * warmFrac + rng.nextGaussian() * 0.1 * noise);
                s.voltage = clamp(12.4 + 1.4 * warmFrac + rng.nextGaussian() * 0.15 * noise, 11.8, 14.8);
                s.anomalyState = tick < 30 ? "DEGRADED — Cold enrichment" : "NOMINAL";
                break;

            case "Degraded O2 Sensor":
                s.rpm    = clamp((int)(1600 + rng.nextGaussian() * 80 * noise), 1000, 3000);
                s.speed  = clamp((int)(50 + rng.nextGaussian() * 8 * noise), 20, 80);
                s.temp   = clamp((int)(91 + rng.nextGaussian() * 2 * noise), 85, 100);
                s.load   = clamp((int)(40 + rng.nextGaussian() * 5 * noise), 20, 65);
                // O2 sensor stuck high — lazy switching
                s.o2     = clamp01(0.75 + 0.05 * Math.sin(tick * 0.05) + rng.nextGaussian() * 0.04 * noise);
                s.voltage = clamp(13.9 + rng.nextGaussian() * 0.1 * noise, 13.0, 14.8);
                s.dtcs.add("P0136");
                s.anomalyState = "DEGRADED — O2B2S1 lazy switch anomaly";
                break;

            case "Coolant Leak (Overheating)":
                double heatFrac = Math.min(1.0, tick / 60.0);
                s.rpm    = clamp((int)(1400 + rng.nextGaussian() * 100 * noise), 800, 3000);
                s.speed  = clamp((int)(45 + rng.nextGaussian() * 10 * noise), 0, 80);
                s.temp   = clamp((int)(90 + 45 * heatFrac + rng.nextGaussian() * 3 * noise), 90, 135);
                s.load   = clamp((int)(50 + 20 * heatFrac + rng.nextGaussian() * 6 * noise), 30, 90);
                s.o2     = clamp01(0.45 + 0.3 * Math.sin(tick * 0.5) + rng.nextGaussian() * 0.04 * noise);
                s.voltage = clamp(13.5 + rng.nextGaussian() * 0.2 * noise, 12.0, 14.8);
                if (heatFrac > 0.7) s.dtcs.add("P0217");
                s.anomalyState = s.temp > 110 ? "CRITICAL — Overtemperature" :
                                 s.temp > 100 ? "DEGRADED — Coolant temp rising" : "NOMINAL";
                break;

            case "Misfiring Cylinder 3":
                s.rpm    = clamp((int)(1600 + rng.nextGaussian() * 250 * (1 + noise)), 900, 3000);
                s.speed  = clamp((int)(50 + rng.nextGaussian() * 8 * noise), 20, 80);
                s.temp   = clamp((int)(90 + rng.nextGaussian() * 2 * noise), 83, 100);
                s.load   = clamp((int)(45 + rng.nextGaussian() * 8 * noise), 20, 80);
                s.o2     = clamp01(0.45 + 0.5 * Math.sin(tick * 0.4) + rng.nextGaussian() * 0.1 * noise);
                s.voltage = clamp(13.7 + rng.nextGaussian() * 0.15 * noise, 12.5, 14.8);
                s.dtcs.add("P0303");
                s.anomalyState = "DEGRADED — Cylinder 3 misfire detected";
                break;

            case "Weak Battery / Electrical Fault":
                s.rpm    = clamp((int)(1500 + rng.nextGaussian() * 60 * noise), 800, 3000);
                s.speed  = clamp((int)(55 + rng.nextGaussian() * 6 * noise), 20, 80);
                s.temp   = clamp((int)(89 + rng.nextGaussian() * 2 * noise), 83, 100);
                s.load   = clamp((int)(38 + rng.nextGaussian() * 5 * noise), 20, 65);
                s.o2     = clamp01(0.45 + 0.38 * Math.sin(tick * 0.5) + rng.nextGaussian() * 0.04 * noise);
                // Sagging voltage with occasional drops
                s.voltage = clamp(11.8 + rng.nextGaussian() * 0.5 * noise - (tick % 10 == 0 ? 1.5 : 0), 9.0, 14.0);
                if (s.voltage < 11.5) s.dtcs.add("U0100");
                s.anomalyState = s.voltage < 11.5 ? "CRITICAL — Battery undervoltage" :
                                 s.voltage < 12.5 ? "DEGRADED — Weak battery / alternator" : "NOMINAL";
                break;

            case "Catalytic Converter Aging":
                s.rpm    = clamp((int)(1800 + rng.nextGaussian() * 60 * noise), 1200, 3000);
                s.speed  = clamp((int)(65 + rng.nextGaussian() * 5 * noise), 40, 100);
                s.temp   = clamp((int)(91 + rng.nextGaussian() * 1.5 * noise), 85, 100);
                s.load   = clamp((int)(48 + rng.nextGaussian() * 5 * noise), 30, 70);
                // Post-cat O2 mimics pre-cat (poor conversion)
                s.o2     = clamp01(0.45 + 0.4 * Math.sin(tick * 0.6) + rng.nextGaussian() * 0.04 * noise);
                s.voltage = clamp(14.0 + rng.nextGaussian() * 0.08 * noise, 13.5, 14.8);
                s.dtcs.add("P0420");
                s.anomalyState = "DEGRADED — Catalyst efficiency below threshold";
                break;

            default:
                s.rpm = 800; s.speed = 0; s.temp = 70; s.load = 10;
                s.o2 = 0.45; s.voltage = 13.5;
                s.anomalyState = "NOMINAL";
        }
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT
    // ─────────────────────────────────────────────────────────────────────────
    private void exportLog() {
        if (sessionLog.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No session data to export. Run a simulation first.",
                    "Nothing to export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("obd_session.json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File f = chooser.getSelectedFile();
            try (java.io.PrintWriter pw = new java.io.PrintWriter(f, "UTF-8")) {
                pw.println("[");
                for (int i = 0; i < sessionLog.size(); i++) {
                    Map<String, Object> row = sessionLog.get(i);
                    StringBuilder sb = new StringBuilder("  {");
                    row.forEach((k, v) -> {
                        if (v instanceof String)      sb.append("\"").append(k).append("\":\"").append(v).append("\",");
                        else if (v instanceof List)   sb.append("\"").append(k).append("\":").append(v).append(",");
                        else                          sb.append("\"").append(k).append("\":").append(v).append(",");
                    });
                    if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);
                    sb.append("}");
                    pw.print(sb);
                    if (i < sessionLog.size() - 1) pw.print(",");
                    pw.println();
                }
                pw.println("]");
                JOptionPane.showMessageDialog(this,
                        "Exported " + sessionLog.size() + " samples to:\n" + f.getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private static int clamp(int v, int lo, int hi)       { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double clamp01(double v)               { return clamp(v, 0.0, 1.0); }
    private static String abbreviate(String s)            { return s.length() > 18 ? s.substring(0, 17) + "…" : s; }

    /** Simple value object for one tick of OBD signals. */
    private static class SignalSet {
        int rpm, speed, temp, load;
        double o2, voltage;
        List<String> dtcs = new ArrayList<>();
        String anomalyState = "NOMINAL";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ObdSimulatorApp().setVisible(true));
    }
}
