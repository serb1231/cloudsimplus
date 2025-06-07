package org.APD.IDK_IF_NEEDED;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.src.PowerModels.PerformanceState;
import org.cloudbus.cloudsim.examples.src.PowerModels.PowerModelPStateProcessor;

public class PStateAwareVm extends Vm {
    PowerModelPStateProcessor powerModel;
    public PStateAwareVm(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler, PowerModelPStateProcessor model) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
        this.powerModel = model;
    }
    public int getCurrentPState() {
        return ((PowerModelPStateProcessor) ((PStateAwareHost)this.getHost()).getPowerModel()).getCurrentPerformanceState();
    }

    public void setCurrentPState(int pState) {
        ((PowerModelPStateProcessor) ((PStateAwareHost)this.getHost()).getPowerModel()).setCurrentPerformanceState(pState);
    }

    public PerformanceState[] getPossiblePStates() {
        return ((PowerModelPStateProcessor) ((PStateAwareHost)this.getHost()).getPowerModel()).getPossiblePerformanceStates();
    }

    /** Energy at this state (shorthand, optional) */
    public double EN(){ return ((PowerModelPStateProcessor) ((PStateAwareHost)this.getHost()).getPowerModel()).EN();}

    /** Relative speed at this state (0…1) */
    public float  RSP(){ return ((PowerModelPStateProcessor) ((PStateAwareHost)this.getHost()).getPowerModel()).RSP();}

}
