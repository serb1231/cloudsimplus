package org.APD;

public interface SchedulingAlgorithm {
    AlgorithmResult run(RelevantDataForAlgorithms input);
    String getName();
}
