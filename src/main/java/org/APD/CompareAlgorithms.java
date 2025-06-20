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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingLong;

public class CompareAlgorithms extends AlgorithmBaseFunctionalities {

    private static final double PERCENTAGE_OF_SLA_VIOLATIONS_NOT_ACCEPTABLE = 0.5; // 10% of SLA violations are not acceptable

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new CompareAlgorithms().RunCompareAlgorithms(args);
    }

    public void RunCompareAlgorithms(String[] args) {
        try (
                PrintStream fileOut = new PrintStream("algorithm_output.txt")
        ) {
            System.setOut(fileOut); // All log output goes here

            // Define where to write the CSV summary
            Path csvPath = Paths.get(args.length > 0 ? args[0] : "sla_summary.csv");

            PowerModelPStateProcessor currentPowerModel = new PowerModelPstateProcessor_2GHz_Via_C7_M(0);
            PowerModelPStateProcessor.PerformanceState[] performanceStates = currentPowerModel.getPossiblePerformanceStates();

            // Generate cloudlets (shared across all runs)
            List<DeadlineCloudlet> cloudletListInitial = createCloudletsBurstyArrivalTightDeadlineHeavyTailoredBigGroupedJobs();

            // Collect all results
            List<AlgorithmResult> results = new ArrayList<>();

            // Round Robin
            results.addAll(runAlgorithmEnergyAware(
                    RoundRobinAlgorithm.class, "Round Robin",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // FCFS
            results.addAll(runAlgorithmEnergyAware(
                    FCFSAlgorithm_bin.class, "FCFS",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // GA
            results.addAll(runAlgorithmEnergyAware(
                    GAAlgorithm.class, "GA",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // ACO
            results.addAll(runAlgorithmEnergyAware(
                    ACOAlgorithm.class, "ACO",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // Write all SLA stats to a single CSV
            exportResultsToCsv(results, csvPath);

            System.err.println("✔ Summary written to: " + csvPath);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public <T extends SchedulingAlgorithm> List<AlgorithmResult> runAlgorithmEnergyAware(Class<T> algorithmClass, String label, List<DeadlineCloudlet> cloudletList, List<Host> hostList, List<Vm> vmList) {
        AlgorithmResult result = null;
        List<AlgorithmResult> results = new ArrayList<>();
        while(true) {

            result = runAlgorithmAndPrintStats(algorithmClass, label, cloudletList, vmList, hostList);
            results.add(result);
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
            if (nrOfSlaViolations >= result.cloudletFinishedList().size() * PERCENTAGE_OF_SLA_VIOLATIONS_NOT_ACCEPTABLE) {
                System.out.println("SLA violations are not acceptable, stopping the algorithm.");
                return results; // Return the result if SLA violations are not acceptable
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
                    if(cpuStats.getMean() < cpuStatsToSlowdown && ((PowerModelPStateProcessor) (vm.getHost().getPowerModel())).getCurrentPerformanceState() > 0) {
                        cpuStatsToSlowdown = cpuStats.getMean();
                        vmToSlowdown = vm;
                    }
                }
                // slowdown the host that owns the VM
                int currentStateIdx = ((PowerModelPStateProcessor) (vmToSlowdown.getHost().getPowerModel())).getCurrentPerformanceState();
                if (currentStateIdx > 0) {
                    HostVmPair hostVmPair = modifyHostAndVmToHaveOneHostWithLowerPower(hostList, vmList, vmToSlowdown);
                    if (hostVmPair == null) {
                        System.out.println("Failed to modify host and VM to have lower power. Stopping the algorithm.");
                        break;
                    }
                    else {
                        // modify the hostlist and vmlist with the new host and vm
                        hostList = hostVmPair.hosts();
                        vmList = hostVmPair.vms();
                    }
                } else {
                    break;
                }
            }
        }
        return results;
    }





    public <T extends SchedulingAlgorithm> AlgorithmResult runAlgorithmAndPrintStats(Class<T> algorithmClass, String label, List<DeadlineCloudlet> cloudletListInitial, List<Vm> vmList, List<Host> hostList) {
        try {
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

    private AlgorithmSummary runAlgorithmAndPrintStatsForCSV(
            Class<? extends SchedulingAlgorithm> algClass,
            String label,
            List<DeadlineCloudlet> cloudlets,
            List<Vm> vms,
            List<Host> hosts) {

        try {
            var alg = algClass.getDeclaredConstructor().newInstance();
            AlgorithmResult res = alg.run(createRelevantDataForAlgorithms(vms, cloudlets, hosts));

            System.out.printf("%n%n---------------- %s ----------------%n", label);

            /* ---------- SLA stats ---------- */
            int violations = 0;
            double tardinessSum = 0;
            double tardinessMax = 0;

            for (Cloudlet cl : res.cloudletFinishedList()) {
                if (cl instanceof DeadlineCloudlet dc) {
                    double miss = dc.getFinishTime() - dc.getDeadline();
                    if (miss > 0) {
                        violations++;
                        tardinessSum += miss;
                        tardinessMax = Math.max(tardinessMax, miss);
                    }
                }
            }
            int finished = res.cloudletFinishedList().size();
            double violationPct = 100.0 * violations / finished;
            double avgTardiness = violations == 0 ? 0 : tardinessSum / violations;

            System.out.printf("SLA violations: %d/%d (%.2f%%)%n"
                            + "Average tardiness: %.2f s | Max: %.2f s%n",
                    violations, finished, violationPct, avgTardiness, tardinessMax);

            /* ---------- Power stats ---------- */
            double totalPower = printVmsCpuUtilizationAndPowerConsumptionSmallerDataForCSV(res.vms());

            return new AlgorithmSummary(label, totalPower, violationPct,
                    avgTardiness, tardinessMax);

        } catch (Exception e) {
            throw new RuntimeException("Failed to run " + label, e);
        }
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

    public record AlgorithmSummary(
            String name,
            double totalPowerW,
            double slaViolationPct,
            double avgTardiness,
            double maxTardiness) {

        /** Returns one CSV row, no header. */
        String toCsv() {
            return "%s,%.0f,%.2f,%.2f,%.2f".formatted(
                    name, totalPowerW, slaViolationPct, avgTardiness, maxTardiness);
        }
    }
}
