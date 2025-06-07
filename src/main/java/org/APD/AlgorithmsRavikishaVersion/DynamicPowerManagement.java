package org.cloudbus.cloudsim.examples.src.AlgorithmsRavikishaVersion;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.APD.IDK_IF_NEEDED.PStateAwareVm;
import org.APD.IDK_IF_NEEDED.PStateAwareVmStatePair;
import org.cloudbus.cloudsim.examples.src.PowerModels.PerformanceState;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class DynamicPowerManagement {
    private List<Cloudlet> cloudletList;
    private List<PStateAwareVm> vmList;
    private DatacenterBroker broker;

    // the current state S(t) of VMs and their power levels
    private Dictionary<PStateAwareVm, Integer> currentState_S = new Hashtable<>();

    // the future state S(t+1) of VMs and their power levels
    private Dictionary<PStateAwareVm, Integer> futureState_S = new Hashtable<>();

    // current device utilization U(t) of VMs
    private Dictionary<PStateAwareVm, Double> currentUtilization_U = new Hashtable<>();

    // current system throughput
    private double currentThroughput;

    // current system response time R(t)
    private double currentResponseTime;

    // Rmax is the maximum response time that doesn't violate the SLA
    // Rlow < Rhigh < Rmax. Reconfiguration if R(t) > Rhigh or R(t) < Rlow
    // These will be set after we have the jobs generation mechanism ready.
    double Rhigh;
    double Rmax;
    double Rlow;

    private void ComputeSystemParameters() {
        // This method would compute the current system parameters such as throughput, response time, etc.
        // It could involve analyzing the current state of VMs and Cloudlets

        for (PStateAwareVm vm : vmList) {
            // Assuming we have a method to get the current performance state of a VM
            int state = vm.getCurrentPState();
            currentState_S.put(vm, state);
        }

        // get current device utilization U(t) of VMs
        for (PStateAwareVm vm : vmList) {
            // Assuming we have a method to get the current utilization of a VM
            double utilization = vm.getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            currentUtilization_U.put(vm, utilization);
        }

        // get system throughput
        List<Cloudlet> finished = broker.getCloudletReceivedList();
        int totalJobs = finished.size();

        double lastFinish = finished.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);        // assumes sim starts at 0.0

        currentThroughput = totalJobs / lastFinish;       // cloudlets per second

//        get system response time R(t)
        currentResponseTime = lastFinish;
    }

    private Dictionary<PStateAwareVm, Integer> SpeedUp(
            Dictionary<PStateAwareVm, Integer> currentState_S,
            Dictionary<PStateAwareVm, Double> currentUtilization_U,
            double currentThroughput,
            double currentResponseTime
        ) {
        // This method would adjust the performance states of VMs to speed up the system
        // It could involve changing the P-states of VMs to higher performance states

        // Compute D[k, j] using (5), for all k ∈ K, j ∈ L[k]
        Dictionary<PStateAwareVmStatePair, Double> vmStatePairsServiceDemand = new Hashtable<>();
        Dictionary<PStateAwareVm, Double> serviceDemand_S = new Hashtable<>();
        for (PStateAwareVm vm : vmList) {
//          for every VM, get the current performance state and utilization
            for (PerformanceState state : vm.getPossiblePStates()) {
                // RSP[k, S[k](t)]
                double currentRSP = vm.getPossiblePStates()[currentState_S.get(vm)].processingPercentage();
                double stateRSP = state.processingPercentage();
                // U[k](t)
                double utilization = currentUtilization_U.get(vm);
                // free the vmStatePairsServiceDemand dictionary

                // TODO: idk if this is correct, but it seems like the right way to do it
                vmStatePairsServiceDemand.put(new PStateAwareVmStatePair(vm, state), (currentRSP / stateRSP) * utilization / currentThroughput);
            }
        }

        // S' = S
        for (PStateAwareVm vm : vmList) {
            // Set the future state to the current state initially
            futureState_S.put(vm, currentState_S.get(vm));
        }

        // candidate set C
        // C := {k ∈ K | S'[k] > 1}
        List<PStateAwareVm> candidateSet = new java.util.ArrayList<>();
        // This set contains VMs that can be sped up
        for (PStateAwareVm vm : vmList) {
            // Check if the current performance state is not the maximum
            int currentState = futureState_S.get(vm);
            if (currentState != vm.getPossiblePStates().length - 1) {
                // If not, we can speed up this VM
                candidateSet.add(vm);
            }
        }

        // while C != ∅
        while (!candidateSet.isEmpty()) {
            // find the highest value for D[k, S'[k]] / EN[k, S'[k]]
            PStateAwareVm vmToSpeedUp = candidateSet.getFirst();
            double maxSpeedUp = -1;
            for (PStateAwareVm vm : candidateSet) {
                //D[k,S'[k]]
                double serviceDemandFutureState = vmStatePairsServiceDemand.get(new PStateAwareVmStatePair(vm, vm.getPossiblePStates()[futureState_S.get(vm)]));
                double energyConsumptionFutureState = vm.getPossiblePStates()[futureState_S.get(vm)].powerConsumption();
                double speedUpPotential = serviceDemandFutureState / energyConsumptionFutureState;
                if (speedUpPotential > maxSpeedUp) {
                    maxSpeedUp = speedUpPotential;
                    vmToSpeedUp = vm;
                }
            }
            // S'[B] : = S'[B] + 1
            int powerState = futureState_S.get(vmToSpeedUp);
            futureState_S.put(vmToSpeedUp, powerState + 1); // Increase the performance state of the VM

            double Rest = EstimateFutureResponseTime(currentState_S, futureState_S, currentUtilization_U, currentThroughput, currentResponseTime);

            if (Rest < Rhigh) {
                break;
            }
            // candidate set C
            // C := {k ∈ K | S'[k] > 1}
            candidateSet = new java.util.ArrayList<>();
            // This set contains VMs that can be sped up
            for (PStateAwareVm vm : vmList) {
                // Check if the current performance state is not the maximum
                int currentState = futureState_S.get(vm);
                if (currentState != vm.getPossiblePStates().length - 1) {
                    // If not, we can speed up this VM
                    candidateSet.add(vm);
                }
            }
        }

        return futureState_S;
    }

    private Dictionary<PStateAwareVm, Integer> SpeedDown(
            Dictionary<PStateAwareVm, Integer> currentState_S,
            Dictionary<PStateAwareVm, Double> currentUtilization_U,
            double currentThroughput,
            double currentResponseTime
    ) {
        // This method would adjust the performance states of VMs to speed up the system
        // It could involve changing the P-states of VMs to higher performance states

        // Compute D[k, j] using (5), for all k ∈ K, j ∈ L[k]
        Dictionary<PStateAwareVmStatePair, Double> vmStatePairsServiceDemand = new Hashtable<>();
        Dictionary<PStateAwareVm, Double> serviceDemand_S = new Hashtable<>();
        for (PStateAwareVm vm : vmList) {
//          for every VM, get the current performance state and utilization
            for (PerformanceState state : vm.getPossiblePStates()) {
                // RSP[k, S[k](t)]
                double currentRSP = vm.getPossiblePStates()[currentState_S.get(vm)].processingPercentage();
                double stateRSP = state.processingPercentage();
                // U[k](t)
                double utilization = currentUtilization_U.get(vm);
                // free the vmStatePairsServiceDemand dictionary

                // TODO: idk if this is correct, but it seems like the right way to do it
                vmStatePairsServiceDemand.put(new PStateAwareVmStatePair(vm, state), (currentRSP / stateRSP) * utilization / currentThroughput);
            }
        }

        // S' = S
        for (PStateAwareVm vm : vmList) {
            // Set the future state to the current state initially
            futureState_S.put(vm, currentState_S.get(vm));
        }

        // candidate set C
        // C := {k ∈ K | S'[k] > 1}
        List<PStateAwareVm> candidateSet = new java.util.ArrayList<>();
        // This set contains VMs that can be sped up
        for (PStateAwareVm vm : vmList) {
            // Check if the current performance state is not the maximum
            int currentState = futureState_S.get(vm);
            if (currentState != 0) {
                // If not, we can speed up this VM
                candidateSet.add(vm);
            }
        }

        // while C != ∅
        while (!candidateSet.isEmpty()) {
            // find the highest value for D[k, S'[k]] / EN[k, S'[k]]
            PStateAwareVm vmToSpeedDown = candidateSet.getFirst();
            double maxSpeedUp = -1;
            for (PStateAwareVm vm : candidateSet) {
                //D[k,S'[k]]
                double serviceDemandFutureState = vmStatePairsServiceDemand.get(new PStateAwareVmStatePair(vm, vm.getPossiblePStates()[futureState_S.get(vm) - 1]));
                double energyConsumptionFutureState = vm.getPossiblePStates()[futureState_S.get(vm) - 1].powerConsumption();
                double speedUpPotential = serviceDemandFutureState / energyConsumptionFutureState;
                if (speedUpPotential > maxSpeedUp) {
                    maxSpeedUp = speedUpPotential;
                    vmToSpeedDown = vm;
                }
            }
            // S'[B] : = S'[B] - 1
            int powerState = futureState_S.get(vmToSpeedDown);
            futureState_S.put(vmToSpeedDown, powerState - 1); // Decrease the performance state of the VM

            double Rest = EstimateFutureResponseTime(currentState_S, futureState_S, currentUtilization_U, currentThroughput, currentResponseTime);

            if (Rest > Rmax) {
//                S'[U] : = S'[U] + 1
                int currentState = futureState_S.get(vmToSpeedDown);
                futureState_S.put(vmToSpeedDown, currentState + 1); // Increase the performance state of the VM
//                C := C \ {U}
                candidateSet.remove(vmToSpeedDown); // Remove the Vm from the candidate set
            }
            // else if S'[U] =- L[U] − 1
            else if (futureState_S.get(vmToSpeedDown) == 0) {
                // If the VM is already at the minimum performance state, we can stop speeding down
                candidateSet.remove(vmToSpeedDown);
            }
        }
        return futureState_S;
    }

    private double EstimateFutureResponseTime(
            Dictionary<PStateAwareVm, Integer> currentState_S,
            Dictionary<PStateAwareVm, Integer> futureState_S,
            Dictionary<PStateAwareVm, Double> currentUtilization_U,
            double currentThroughput,
            double currentResponseTime
            ) {
        // This method would create future configurations based on the current state of VMs and Cloudlets
        // It could involve predicting future workloads and adjusting VM configurations accordingly


        // Service Demand for future state S(t+1)
        // for all k {D[k] = D[k, S'[k]] = ... }
        Dictionary<PStateAwareVm, Double> serviceDemand_S1 = new Hashtable<>();
        for (PStateAwareVm vm : vmList) {
            double currentRSP = (vm.getPossiblePStates())[currentState_S.get(vm)].processingPercentage();
            double futureRSP = (vm.getPossiblePStates())[futureState_S.get(vm)].processingPercentage();

            serviceDemand_S1.put(vm, (currentRSP / futureRSP) * currentUtilization_U.get(vm) / currentThroughput);
        }

        // Maximum Service Demand
        // Dmax:= max{D[k] | k ∈ K}
        double maxServiceDemand = -1;

        for (PStateAwareVm vm : vmList) {
            double demand = serviceDemand_S1.get(vm);
            if (demand > maxServiceDemand) {
                maxServiceDemand = demand;
            }
        }

        // total Service Demand
        // D := ∑_{k ∈ K} D[k]
        double totalServiceDemand = 0;
        for (PStateAwareVm vm : vmList) {
            totalServiceDemand += serviceDemand_S1.get(vm);
        }

        // Average Service Demand
        // Davg := D / |K|
        double averageServiceDemand = totalServiceDemand / vmList.size();

        // N
        double N = currentThroughput * currentResponseTime;

        // Lower bound for response time
        // Rmin := max{NDmax, Dtot + (N-1) * Dave}
        double lowerBoundResponseTime = Math.max(N * maxServiceDemand, totalServiceDemand + (N-1) * averageServiceDemand);

        // upper bound for response time
        // Rmax := Dtot + (N-1) * Dmax
        double upperBoundResponseTime = totalServiceDemand + (N - 1) * maxServiceDemand;

        // future system response time R(t+1)

        return (lowerBoundResponseTime + upperBoundResponseTime) / 2.0;
    }



    public void runAlgorithm(DatacenterBroker broker, List<PStateAwareVm> vmlist, List<Cloudlet> cloudletList) {
        this.vmList = vmlist;
        this.cloudletList = cloudletList;
        this.broker = broker;

//            broker.bindCloudletToPStateAwareVm(ant.getCloudlet().getCloudletId(), ant.getPStateAwareVm().getId());
    }
}
