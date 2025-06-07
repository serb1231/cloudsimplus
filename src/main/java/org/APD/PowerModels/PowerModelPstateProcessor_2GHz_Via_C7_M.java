package org.APD.PowerModels;

/**
 * Power model for the 2 GHz VIA C7-M processor (Table 2 in the paper).
 * <p>
 * Inherits every behaviour from {@link PowerModelPStateProcessor}.
 */
public final class PowerModelPstateProcessor_2GHz_Via_C7_M
        extends PowerModelPStateProcessor {

    private static final double PEAK_FREQ_GHZ = 2.0;

    private static final PerformanceState[] PSTATES = {
//            new PerformanceState(200f, 1.0f),                // P0  2.0 GHz
//            new PerformanceState(180f, 1.8f / 2.0f),         // P1  1.8 GHz
//            new PerformanceState(150f, 1.6f / 2.0f),         // P2  1.6 GHz
//            new PerformanceState(130f, 1.4f / 2.0f),         // P3  1.4 GHz
//            new PerformanceState(100f, 1.0f / 2.0f),         // P4  1.0 GHz
//            new PerformanceState( 70f, 0.8f / 2.0f),         // P5  0.8 GHz
            new PerformanceState( 60f, 0.6f / 2.0f),         // P6  0.6 GHz
            new PerformanceState( 50f, 0.4f / 2.0f),          // P7  0.4 GHz
    };

    public PowerModelPstateProcessor_2GHz_Via_C7_M(int startingState) {
        super(startingState, PSTATES);
    }
}
