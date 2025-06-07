package org.APD.IDK_IF_NEEDED;
//import org.cloudbus.cloudsim.examples.src.PowerModels.PowerModelPStateProcessor;
//import org.cloudbus.cloudsim.power.PowerHost;
//import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
//import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
//import org.cloudbus.cloudsim.power.models.*;
//import org.cloudbus.cloudsim.Pe;
//import org.cloudbus.cloudsim.VmScheduler;
//import org.cloudbus.cloudsim.examples.src.PowerModels.PerformanceState;
//import java.util.List;
import org.cloudsimplus.power.models.PowerModelHostSimple;
PowerModelHostSimple;


public class PStateAwareHost extends PowerModelHostSimple {
    /**
     * Instantiates a {@link PowerModelHostSimple} by specifying its static and max power usage.
     *
     * @param maxPower    power (in watts) the host consumes under full load.
     * @param staticPower power (in watts) the host consumes when idle.
     */
    public PStateAwareHost(double maxPower, double staticPower) {
        super(maxPower, staticPower);
    }

    @Override
    public double getPower(double utilization) {
        // Here you can implement your logic to calculate power based on P-states
        // For example, you could use a formula that relates utilization to power consumption
        // This is just a placeholder implementation
        return getStaticPower() + (getMaxPower() - getStaticPower()) * utilization;
    }
//    public PStateAwareHost(int id, RamProvisionerSimple ram, BwProvisionerSimple bw,
//                           long storage, List<Pe> peList,
//                           VmScheduler scheduler,
//                           PowerModelPStateProcessor model)
//    {
//        super(id, ram, bw, storage, peList, scheduler, model);
//    }
//
//    public int getCurrentPState() {
//        return ((PowerModelPStateProcessor) getPowerModel()).getCurrentPerformanceState();
//    }
//
//    public void setCurrentPState(int pState) {
//        ((PowerModelPStateProcessor) getPowerModel()).setCurrentPerformanceState(pState);
//    }
//
//    public PerformanceState[] getPossiblePStates() {
//        return ((PowerModelPStateProcessor) getPowerModel()).getPossiblePerformanceStates();
//    }
//
//    /** Energy at this state (shorthand, optional) */
//    public double EN(){ return ((PowerModelPStateProcessor) getPowerModel()).EN();}
//
//    /** Relative speed at this state (0…1) */
//    public float  RSP(){ return ((PowerModelPStateProcessor) getPowerModel()).RSP();}



}
