package org.APD.IDK_IF_NEEDED;

/**
 * Represents a performance state of a processor, including its power consumption and processing speed.
 * This class is used to model the performance characteristics of processors in cloud simulations.
 *
 * @param powerConsumption     in Watts
 * @param processingPercentage percentage of maximum processing speed
 */
public record PerformanceState(float powerConsumption, float processingPercentage) {

}
