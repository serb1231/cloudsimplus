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

        for (int i = performanceStates.length - 1; i >= 0; i--) {
            MIPS_PER_HOST_MAX = (int) (performanceStates[i].processingFraction() * MIPS_PER_HOST_INITIAL_MAX);
            MIPS_PER_VM_MAX = (int) (performanceStates[i].processingFraction() * MIPS_PER_VM_INITIAL_MAX);


            MIPS_PER_HOST_MIN = (int) (performanceStates[i].processingFraction() * MIPS_PER_HOST_INITIAL_MIN);
            MIPS_PER_VM_MIN = (int) (performanceStates[i].processingFraction() * MIPS_PER_VM_INITIAL_MIN);
            POWER_STATE = i;
            System.out.printf("""
                    
                    
                    
                    -----------------------------------------------------------------------------------------------------------
                    Performance State %d: MIPS_PER_HOST = %d, MIPS_PER_VM = %d
                    -----------------------------------------------------------------------------------------------------------
                    
                    
                    """, i, MIPS_PER_HOST_MAX, MIPS_PER_VM_MAX);

            AlgorithmResult resultRR = runAlgorithmAndPrintStats(RoundRobinAlgorithm.class, "Round Robin", cloudletListInitial);
            AlgorithmResult resultFCFS = runAlgorithmAndPrintStats(FCFSAlgorithm_bin.class, "FCFS", cloudletListInitial);
            AlgorithmResult resultGA = runAlgorithmAndPrintStats(GAAlgorithm.class, "GA", cloudletListInitial);
            AlgorithmResult resultACO = runAlgorithmAndPrintStats(ACOAlgorithm.class, "ACO", cloudletListInitial);
        }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public <T extends SchedulingAlgorithm> AlgorithmResult runAlgorithmEnergyAware(Class<T> algorithmClass, String label, List<DeadlineCloudlet> cloudletList) {
        AlgorithmResult result = null;
        while(true) {
            result = runAlgorithmAndPrintStats(algorithmClass, label, cloudletList);
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
                return result; // Return the result if SLA violations are acceptable
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
                if (currentStateIdx < ((PowerModelPStateProcessor) (vmToSlowdown.getHost().getPowerModel())).getPossiblePerformanceStates().length - 1) {
                    ((PowerModelPStateProcessor) (vmToSlowdown.getHost().getPowerModel())).setCurrentPerformanceState(currentStateIdx + 1);
                        // TODO modify the host and vm mips!! Right now, they are linearly distributed, which doesn't make sense
//                    vmToSlowdown.set
                } else {
                    break;
                }

            }
        }
        return result;
    }





    public <T extends SchedulingAlgorithm> AlgorithmResult runAlgorithmAndPrintStats(Class<T> algorithmClass, String label, List<DeadlineCloudlet> cloudletListInitial) {
        try {
            List<DeadlineCloudlet> cloudletList = copyCloudlets(cloudletListInitial);
            List<Vm> vmList = createVms();
            List<Host> hostList = createHostsInitialDistribution();

            // Create instance of the algorithm (requires no-arg constructor)
            T algorithm = algorithmClass.getDeclaredConstructor().newInstance();

            AlgorithmResult result = algorithm.run(createRelevantDataForAlgorithms(vmList, cloudletList, hostList));

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
