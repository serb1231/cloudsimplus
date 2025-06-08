package org.APD.Algorithms;

import org.APD.AlgorithmResult;
import org.APD.RelevantDataForAlgorithms;

public interface SchedulingAlgorithm {
    AlgorithmResult run(RelevantDataForAlgorithms input);
    String getName();
}
