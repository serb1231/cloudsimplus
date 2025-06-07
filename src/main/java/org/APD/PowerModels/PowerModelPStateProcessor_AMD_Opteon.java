package org.APD.PowerModels;

/**
 * Power model for the 2 GHz VIA C7-M processor (Table 2 in the paper).
 * <p>
 * Inherits every behaviour from {@link PowerModelPStateProcessor}.
 */
public final class PowerModelPStateProcessor_AMD_Opteon
        extends PowerModelPStateProcessor {

    private static final double PEAK_FREQ_GHZ = 2.0;

    private static final PerformanceState[] PSTATES = {
            new PerformanceState(20f, 1.0f),                // P0  2.0 GHz
            new PerformanceState(18f, 1.8f / 2.0f),         // P1  1.8 GHz
            new PerformanceState(15f, 1.6f / 2.0f),         // P2  1.6 GHz
            new PerformanceState(13f, 1.4f / 2.0f),         // P3  1.4 GHz
            new PerformanceState(10f, 1.0f / 2.0f),         // P4  1.0 GHz
            new PerformanceState( 7f, 0.8f / 2.0f),         // P5  0.8 GHz
            new PerformanceState( 6f, 0.6f / 2.0f),         // P6  0.6 GHz
            new PerformanceState( 5f, 0.4f / 2.0f)          // P7  0.4 GHz
    };

    public PowerModelPStateProcessor_AMD_Opteon(int startingState) {
        super(startingState, PSTATES);
    }
}
