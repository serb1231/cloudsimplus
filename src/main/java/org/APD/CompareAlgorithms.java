package org.APD;

import ch.qos.logback.classic.Level;
import org.APD.Algorithms.*;

import org.APD.PowerModels.PowerModelPStateProcessor;
import org.APD.PowerModels.PowerModelPstateProcessor_2GHz_Via_C7_M;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.APD.Algorithms.SchedulingAlgorithm;
import org.cloudsimplus.vms.VmResourceStats;

import java.util.List;

import static java.util.Comparator.comparingLong;

public class CompareAlgorithms extends AlgorithmBaseFunctionalities {

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new CompareAlgorithms().RunCompareAlgorithms();
    }

    public void RunCompareAlgorithms() {
        try {
            PrintStream fileOut = new PrintStream(new File("algorithm_output.txt"));
            System.setOut(fileOut);  // Redirect console output to file

        PowerModelPStateProcessor currentPowerModel = new PowerModelPstateProcessor_2GHz_Via_C7_M(0);
        PowerModelPStateProcessor.PerformanceState[] performanceStates = currentPowerModel.getPossiblePerformanceStates();

        List<DeadlineCloudlet> cloudletListInitial = createCloudletsBurstyArrivalTightDeadlineHeavyTailoredBigGroupedJobs();

        // Create hosts and VMs with the initial performance state

//        for (int i = performanceStates.length - 1; i >= 0; i--) {
//            MIPS_PER_HOST_MAX = (int) (performanceStates[i].processingFraction() * MIPS_PER_HOST_INITIAL_MAX);
//            MIPS_PER_VM_MAX = (int) (performanceStates[i].processingFraction() * MIPS_PER_VM_INITIAL_MAX);
//
//
//            MIPS_PER_HOST_MIN = (int) (performanceStates[i].processingFraction() * MIPS_PER_HOST_INITIAL_MIN);
//            MIPS_PER_VM_MIN = (int) (performanceStates[i].processingFraction() * MIPS_PER_VM_INITIAL_MIN);
//            POWER_STATE = i;
//            System.out.printf("""
//
//
//
//                    -----------------------------------------------------------------------------------------------------------
//                    Performance State %d: MIPS_PER_HOST = %d, MIPS_PER_VM = %d
//                    -----------------------------------------------------------------------------------------------------------
//
//
//                    """, i, MIPS_PER_HOST_MAX, MIPS_PER_VM_MAX);
//
//            AlgorithmResult resultRR = runAlgorithmAndPrintStats(RoundRobinAlgorithm.class, "Round Robin", cloudletListInitial);
//            AlgorithmResult resultFCFS = runAlgorithmAndPrintStats(FCFSAlgorithm_bin.class, "FCFS", cloudletListInitial);
//            AlgorithmResult resultGA = runAlgorithmAndPrintStats(GAAlgorithm.class, "GA", cloudletListInitial);
//            AlgorithmResult resultACO = runAlgorithmAndPrintStats(ACOAlgorithm.class, "ACO", cloudletListInitial);
//        }

            // Create hosts and VMs with the initial performance state
            List<Vm> vmList = createVms();
            List<Host> hostList = createHostsInitialDistribution();
            AlgorithmResult resultRR = runAlgorithmEnergyAware(RoundRobinAlgorithm.class, "Round Robin", cloudletListInitial, hostList, vmList);

            // Create hosts and VMs with the initial performance state
            vmList = createVms();
            hostList = createHostsInitialDistribution();
            AlgorithmResult resultFCFS = runAlgorithmEnergyAware(FCFSAlgorithm_bin.class, "FCFS", cloudletListInitial, hostList, vmList);

            // Create hosts and VMs with the initial performance state
            vmList = createVms();
            hostList = createHostsInitialDistribution();
            AlgorithmResult resultGA = runAlgorithmEnergyAware(GAAlgorithm.class, "GA", cloudletListInitial, hostList, vmList);

            // Create hosts and VMs with the initial performance state
            vmList = createVms();
            hostList = createHostsInitialDistribution();
            AlgorithmResult resultACO = runAlgorithmEnergyAware(ACOAlgorithm.class, "ACO", cloudletListInitial, hostList, vmList);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // TODO right now every algorithms uses a copy of the global vm's and hosts, which is not ideal. This should be given as parrameter to functions

    public <T extends SchedulingAlgorithm> AlgorithmResult runAlgorithmEnergyAware(Class<T> algorithmClass, String label, List<DeadlineCloudlet> cloudletList, List<Host> hostList, List<Vm> vmList) {
        AlgorithmResult result = null;
        while(true) {
//            // print the current performance state, mips and power
//            for (Host host : hostList) {
//                PowerModelPStateProcessor powerModel = (PowerModelPStateProcessor) host.getPowerModel();
//                int currentStateIdx = powerModel.getCurrentPerformanceState();
//                PowerModelPStateProcessor.PerformanceState currentState = powerModel.getPossiblePerformanceStates()[currentStateIdx];
//                System.out.printf("Host %d: Performance State %d, ProcessingFraction = %.2f, Power = %.2f W, MIPS Host = %.2f%n",
//                        host.getId(), currentStateIdx, (currentState.processingFraction()), currentState.powerConsumption(), host.getMips());
//            }
//            // now print for each VM the mips consumption and power
//            for (Vm vm : vmList) {
//                System.out.printf("VM %d: MIPS = %.2f%n", vm.getId(), vm.getMips());
//            }


            result = runAlgorithmAndPrintStats(algorithmClass, label, cloudletList, vmList, hostList);
//            // print which vm is on which host
//            System.out.println("\n\n-----------------------------------------------------------------");
//            System.out.println("VMs and their Hosts:");
//            System.out.println("-----------------------------------------------------------------\n");
//            for (Vm vm : result.vms()) {
//                System.out.printf("VM %d is on Host %d%n", vm.getId(), vm.getHost().getId());
//            }
            // Check if the SLA violations are bigger than half
            int nrOfSlaViolations = 0;
            for (Cloudlet cl : result.cloudletFinishedList()) {
                if (cl instanceof DeadlineCloudlet dc) {
                    double finish = dc.getFinishTime();
                    double deadline = dc.getDeadline();
                    boolean metDeadline = finish <= deadline;

                    if (!metDeadline)
                        nrOfSlaViolations++;
                }
            }
            if (nrOfSlaViolations >= result.cloudletFinishedList().size() / 2) {
                System.out.println("SLA violations are not acceptable, stopping the algorithm.");
                return result; // Return the result if SLA violations are not acceptable
            } else {
                Vm vmToSlowdown = result.vms().get(0); // Get the first VM to modify its power model
                double cpuStatsToSlowdown = Double.MAX_VALUE;
                result.vms().sort(comparingLong(vm -> vm.getHost().getId()));
                for (Vm vm : result.vms()) {
                    final var powerModel = vm.getHost().getPowerModel();
                    final double hostStaticPower = powerModel instanceof PowerModelPStateProcessor powerModelHost ? powerModelHost.getStaticPower() : 0;
                    final double hostStaticPowerByVm = hostStaticPower / vm.getHost().getVmCreatedList().size();

                    //VM CPU utilization relative to the host capacity
                    final double vmRelativeCpuUtilization = vm.getCpuUtilizationStats().getMean() / vm.getHost().getVmCreatedList().size();
                    final double vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm; // W
                    final VmResourceStats cpuStats = vm.getCpuUtilizationStats();
                    // if this vm has had a lower CPU utilization than the best so far, then we can slow it down
                    if(cpuStats.getMean() < cpuStatsToSlowdown) {
                        cpuStatsToSlowdown = cpuStats.getMean();
                        vmToSlowdown = vm;
                    }
                }
                // slowdown the host that owns the VM
                int currentStateIdx = ((PowerModelPStateProcessor) (vmToSlowdown.getHost().getPowerModel())).getCurrentPerformanceState();
                if (currentStateIdx > 0) {
                    // print the vm mips
//                    System.out.printf("Slowing down VM %d on Host %d from Performance State %d to %d%n",
//                            vmToSlowdown.getId(), vmToSlowdown.getHost().getId(), currentStateIdx, currentStateIdx - 1);
                    // print the vm.getMips()
//                    System.out.printf("VM %d MIPS before slowdown: %.2f%n", vmToSlowdown.getId(), vmToSlowdown.getMips());
                    HostVmPair hostVmPair = modifyHostAndVmToHaveOneHostWithLowerPower(hostList, vmList, vmToSlowdown);
                    if (hostVmPair == null) {
                        System.out.println("Failed to modify host and VM to have lower power. Stopping the algorithm.");
                        break;
                    }
                    else {
                        // modify the hostlist and vmlist with the new host and vm
                        hostList = hostVmPair.hosts();
                        vmList = hostVmPair.vms();
//                        System.out.println("Slowing down host " + vmToSlowdown.getHost().getId() + " to performance state " + (currentStateIdx - 1));
                    }
                    // print all the vm's and all the hosts and their mips
//                    for (Host host : hostList) {
//                        System.out.printf("Host %d: MIPS = %.2f%n", host.getId(), host.getMips());
//                    }
//                    for (Vm vm : vmList) {
//                        System.out.printf("VM %d: MIPS = %.2f%n", vm.getId(), vm.getMips());
//                    }

                } else {
                    break;
                }
            }
        }
        return result;
    }





    public <T extends SchedulingAlgorithm> AlgorithmResult runAlgorithmAndPrintStats(Class<T> algorithmClass, String label, List<DeadlineCloudlet> cloudletListInitial, List<Vm> vmList, List<Host> hostList) {
        try {
//            List<DeadlineCloudlet> cloudletList = copyCloudlets(cloudletListInitial);
//            List<Vm> vmList = createVms();
//            List<Host> hostList = createHostsInitialDistribution();

            // Create instance of the algorithm (requires no-arg constructor)
            T algorithm = algorithmClass.getDeclaredConstructor().newInstance();

            AlgorithmResult result = algorithm.run(createRelevantDataForAlgorithms(vmList, cloudletListInitial, hostList));

            System.out.println("\n\n-----------------------------------------------------------------");
            System.out.println("Violated Cloudlets for " + label + ":");
            System.out.println("-----------------------------------------------------------------\n");

            printSLAViolationsStatistics(result.cloudletFinishedList());
            printVmsCpuUtilizationAndPowerConsumption(result.vms());

            return result;

        } catch (Exception e) {
            System.err.println("Failed to instantiate or run " + label + " algorithm.");
            e.printStackTrace();
        }
        return null;
    }


    RelevantDataForAlgorithms createRelevantDataForAlgorithms(List<Vm> vmList, List<DeadlineCloudlet> cloudletList, List<Host> hostList) {
        var vmListClone = copyVMs(vmList);
        var cloudletListClone = copyCloudlets(cloudletList);
        var hostListCopy = copyHosts(hostList);
        return new RelevantDataForAlgorithms(
                SCHEDULING_INTERVAL,
                HOSTS,
                HOST_PES,
                HOST_START_UP_DELAY,
                HOST_SHUT_DOWN_DELAY,
                HOST_START_UP_POWER,
                HOST_SHUT_DOWN_POWER,
                VMS,
                VM_PES,
                CLOUDLETS_PER_FRAME,
                CLOUDLET_PES,
                CLOUDLET_LENGTH_MIN,
                CLOUDLET_LENGTH_MAX,
                STATIC_POWER,
                MAX_POWER,
                vmListClone,
                cloudletListClone,
                hostListCopy
        );
    }
}
