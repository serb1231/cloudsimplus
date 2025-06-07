package org.APD;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public interface SchedulingAlgorithm {
    void run(RelevantDataForAlgorithms input);
    String getName();
}
