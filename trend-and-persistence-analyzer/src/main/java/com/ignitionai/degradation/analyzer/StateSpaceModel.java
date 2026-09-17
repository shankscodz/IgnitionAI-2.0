package com.ignitionai.degradation.analyzer;

public class StateSpaceModel {
    private double x; // latent state (degradation level)
    private double v; // latent velocity (slope)
    private double p11, p12, p21, p22; // Covariance matrix
    
    private final double processNoiseVarX;
    private final double processNoiseVarV;
    private final double defaultMeasurementNoiseVar;

    public StateSpaceModel(double initialX, double processNoiseVarX, double processNoiseVarV, double defaultMeasurementNoiseVar) {
        this.x = initialX;
        this.v = 0.0;
        
        this.processNoiseVarX = processNoiseVarX;
        this.processNoiseVarV = processNoiseVarV;
        this.defaultMeasurementNoiseVar = defaultMeasurementNoiseVar;
        
        // Initial high uncertainty
        this.p11 = 1.0;
        this.p12 = 0.0;
        this.p21 = 0.0;
        this.p22 = 1.0;
    }

    public void update(double dt, double measurement, double confidence) {
        // 1. Predict Step
        // X = A * X
        // x = x + v * dt; v = v
        x = x + v * dt;
        
        // P = A * P * A^T + Q
        double nextP11 = p11 + dt * p21 + dt * (p12 + dt * p22) + processNoiseVarX;
        double nextP12 = p12 + dt * p22;
        double nextP21 = p21 + dt * p22;
        double nextP22 = p22 + processNoiseVarV;

        p11 = nextP11;
        p12 = nextP12;
        p21 = nextP21;
        p22 = nextP22;

        // 2. Update Step
        // Measurement noise scaled by confidence (lower confidence = higher noise)
        double R = defaultMeasurementNoiseVar / Math.max(0.01, confidence);
        
        // Innovation y = z - H * X
        double y = measurement - x;
        
        // Innovation covariance S = H * P * H^T + R = p11 + R
        double S = p11 + R;
        
        // Kalman Gain K = P * H^T / S
        double K1 = p11 / S;
        double K2 = p21 / S;
        
        // Update State: X = X + K * y
        x = x + K1 * y;
        v = v + K2 * y;
        
        // Update Covariance: P = (I - K * H) * P
        double newP11 = (1 - K1) * p11;
        double newP12 = (1 - K1) * p12;
        double newP21 = -K2 * p11 + p21;
        double newP22 = -K2 * p12 + p22;
        
        p11 = newP11;
        p12 = newP12;
        p21 = newP21;
        p22 = newP22;
    }

    public void resetUncertainty() {
        this.p11 = 1.0;
        this.p12 = 0.0;
        this.p21 = 0.0;
        this.p22 = 1.0;
        this.v = 0.0; // Reset velocity on repair or discontinuity
    }

    public double getState() { return x; }
    public double getVelocity() { return v; }
    public double getUncertainty() { return p11; }
}
