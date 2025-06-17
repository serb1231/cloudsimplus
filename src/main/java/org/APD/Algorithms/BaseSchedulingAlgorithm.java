package org.APD.Algorithms;

import org.APD.RelevantDataForAlgorithms;

public abstract class BaseSchedulingAlgorithm extends AlgorithmBaseFunctionalities implements SchedulingAlgorithm {


    protected void copyGivenDataLocally(RelevantDataForAlgorithms relevantDataForAlgorithms) {
        // copy all the relevant data from the record to local variables
        SCHEDULING_INTERVAL = relevantDataForAlgorithms.schedulingInterval();
        HOSTS = relevantDataForAlgorithms.hosts();
        HOST_PES = relevantDataForAlgorithms.hostPes();
        HOST_START_UP_DELAY = relevantDataForAlgorithms.hostStartUpDelay();
        HOST_SHUT_DOWN_DELAY = relevantDataForAlgorithms.hostShutDownDelay();
        HOST_START_UP_POWER = relevantDataForAlgorithms.hostStartUpPower();
        HOST_SHUT_DOWN_POWER = relevantDataForAlgorithms.hostShutDownPower();
        VMS = relevantDataForAlgorithms.vms();
        VM_PES = relevantDataForAlgorithms.vmPes();
//        CLOUDLETS = relevantDataForAlgorithms.cloudlets();
        CLOUDLET_PES = relevantDataForAlgorithms.cloudletPes();
        CLOUDLET_LENGTH_MIN = relevantDataForAlgorithms.cloudletLengthMin();
        CLOUDLET_LENGTH_MAX = relevantDataForAlgorithms.cloudletLengthMax();
        STATIC_POWER = relevantDataForAlgorithms.staticPower();
        MAX_POWER = relevantDataForAlgorithms.maxPower();
        vmList = relevantDataForAlgorithms.vmList();
        cloudletList = relevantDataForAlgorithms.cloudletList();
        hostList = relevantDataForAlgorithms.hostList();
    }

    @Override
    public String getName() {
        return "FCFSAlgorithm_bin";
    }
}
