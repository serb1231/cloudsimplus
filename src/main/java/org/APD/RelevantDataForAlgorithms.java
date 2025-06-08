package org.APD;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public record RelevantDataForAlgorithms(
        int schedulingInterval,
        int hosts,
        int hostPes,
        double hostStartUpDelay,
        double hostShutDownDelay,
        double hostStartUpPower,
        double hostShutDownPower,
        int vms,
        int vmPes,
        int cloudlets,
        int cloudletPes,
        int cloudletLengthMin,
        int cloudletLengthMax,
        double staticPower,
        int maxPower,
        List<Vm> vmList,
        List<DeadlineCloudlet> cloudletList
) {}