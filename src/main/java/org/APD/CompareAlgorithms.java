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
import java.util.concurrent.*;

import static java.util.Comparator.comparingLong;

public class CompareAlgorithms extends AlgorithmBaseFunctionalities {

    private static final double PERCENTAGE_OF_SLA_VIOLATIONS_NOT_ACCEPTABLE = 0.6; // 10% of SLA violations are not acceptable

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
                    new RoundRobinAlgorithm(), "Round Robin",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // FCFS
            results.addAll(runAlgorithmEnergyAware(
                    new FCFSAlgorithm_bin(), "FCFS",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // GA
            results.addAll(runAlgorithmEnergyAware(
                    new GAAlgorithm(), "GA",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // ACO
            results.addAll(runAlgorithmEnergyAware(
                    new ACOAlgorithm(), "ACO",
                    cloudletListInitial, createHostsInitialDistribution(), createVms()));

            // Write all SLA stats to a single CSV
            exportResultsToCsv(results, csvPath);

//            testACOHyperparametersParallel();

            System.err.println("✔ Summary written to: " + csvPath);

            runOneAlgorithmMultipleTimes(
                    RoundRobinAlgorithm.class, "Round Robin Multiple Runs", 128);
            runOneAlgorithmMultipleTimes(
                    FCFSAlgorithm_bin.class, "FCFS Multiple Runs", 128);
            runOneAlgorithmMultipleTimes(
                    GAAlgorithm.class, "GA Multiple Runs", 128);
            runOneAlgorithmMultipleTimes(
                    ACOAlgorithm.class, "ACO Multiple Runs", 128);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void testACOHyperparametersParallel() {
        int[] antOptions = {5, 10, 20, 30};
        int[] iterationOptions = {10, 20, 30, 50};
        double[] evaporationRates = {0.1, 0.2};
        int runsPerConfig = 16;

        // Parallel execution
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<List<AlgorithmResult>>> futures = new ArrayList<>();

        for (int ants : antOptions) {
            for (int iters : iterationOptions) {
                for (double evap : evaporationRates) {
                    // print the current configuration
                    System.out.printf("Running ACO with %d ants, %d iterations, evaporation %.2f%n", ants, iters, evap);
                    for (int run = 1; run <= runsPerConfig; run++) {
                        final int finalAnts = ants;
                        final int finalIters = iters;
                        final double finalEvap = evap;
                        final int finalRun = run;

                        Callable<List<AlgorithmResult>> task = () -> {
                            String label = String.format("ACO_Ants%d_Iters%d_Evap%.2f_iter_%d",
                                    finalAnts, finalIters, finalEvap, finalRun);
                            System.out.println(">>> Running: " + label);

                            List<DeadlineCloudlet> cloudlets = createCloudletsBurstyArrivalTightDeadlineHeavyTailoredBigGroupedJobs();
                            List<Vm> vms = createVms();
                            List<Host> hosts = createHostsInitialDistribution();

                            ACOAlgorithm aco = new ACOAlgorithm(finalAnts, finalIters, finalEvap);
                            return runAlgorithmEnergyAware(aco, label, cloudlets, hosts, vms);
                        };

                        futures.add(executor.submit(task));
                    }
                }
            }
        }

        // Gather results
        List<AlgorithmResult> allResults = new ArrayList<>();
        for (Future<List<AlgorithmResult>> future : futures) {
            try {
                List<AlgorithmResult> result = future.get(); // blocking wait
                if (result != null) {
                    allResults.addAll(result);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        // Export all to CSV
        Path outputPath = Paths.get("results_csv", "aco_param_sweep.csv");
        exportResultsToCsv(allResults, outputPath);
        System.out.println("✔ All ACO parameter runs saved to: " + outputPath);
    }



    public void runOneAlgorithmMultipleTimes(
            Class<? extends SchedulingAlgorithm> algorithmClass, String label, int iterations) {

        String cleanLabel = label.replaceAll("\\s+", "_");
        Path outputDir = Paths.get("results_csv", "results_multiple_iterations_algorithm_smaller_aco");
        Path outputFile = outputDir.resolve(cleanLabel + ".csv");

        try {
            Files.createDirectories(outputDir);

            int numThreads = Runtime.getRuntime().availableProcessors(); // use all logical cores
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            List<Future<List<AlgorithmResult>>> futures = new ArrayList<>();

            for (int i = 1; i <= iterations; i++) {
                final int runIndex = i;
                Callable<List<AlgorithmResult>> task = () -> {
                    String runLabel = String.format("%s_%dtimes_%d", cleanLabel, iterations, runIndex);
                    System.out.println(">>> Running: " + runLabel);

                    List<DeadlineCloudlet> cloudlets = createCloudletsBurstyArrivalTightDeadlineHeavyTailoredBigGroupedJobs();
                    List<Vm> vms = createVms();
                    List<Host> hosts = createHostsInitialDistribution();

                    long start = System.currentTimeMillis();
                    SchedulingAlgorithm algorithmInstance = algorithmClass.getDeclaredConstructor().newInstance();

                    List<AlgorithmResult> results = runAlgorithmEnergyAware(algorithmInstance, runLabel, cloudlets, hosts, vms);
                    long elapsed = System.currentTimeMillis() - start;

                    // Add total execution time to each result
                    return results.stream().map(r ->
                            new AlgorithmResult(
                                    r.algorithmName(),
                                    r.cloudlets(),
                                    r.hosts(),
                                    r.vms(),
                                    r.cloudletFinishedList(),
                                    elapsed
                            )
                    ).toList();
                };

                futures.add(executor.submit(task));
            }

            // Wait for all to finish and collect results
            List<AlgorithmResult> allRuns = new ArrayList<>();
            for (Future<List<AlgorithmResult>> future : futures) {
                try {
                    allRuns.addAll(future.get()); // blocking wait
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor.shutdown();
            exportResultsToCsv(allRuns, outputFile);

            System.out.println("✔ Parallel multiple-run results saved to: " + outputFile.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ Error running " + label + " multiple times (parallelized)");
            e.printStackTrace();
        }
    }



    public List<AlgorithmResult> runAlgorithmEnergyAware(SchedulingAlgorithm algorithmInstance, String label, List<DeadlineCloudlet> cloudletList, List<Host> hostList, List<Vm> vmList) {
        AlgorithmResult result = null;
        List<AlgorithmResult> results = new ArrayList<>();
        while(true) {

            result = runAlgorithmAndPrintStats(algorithmInstance, label, cloudletList, vmList, hostList);
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





    public AlgorithmResult runAlgorithmAndPrintStats(SchedulingAlgorithm algorithmInstance, String label, List<DeadlineCloudlet> cloudletListInitial, List<Vm> vmList, List<Host> hostList) {
        try {
            // Create instance of the algorithm (requires no-arg constructor)
//            T algorithm = algorithmClass.getDeclaredConstructor().newInstance();
            // start timer
            long startTime = System.currentTimeMillis();

            AlgorithmResult result = algorithmInstance.run(createRelevantDataForAlgorithms(vmList, cloudletListInitial, hostList));

            // end timer
            long total_time = System.currentTimeMillis() - startTime;

            System.out.println("\n\n-----------------------------------------------------------------");
            System.out.println("Violated Cloudlets for " + label + ":");
            System.out.println("-----------------------------------------------------------------\n");

            printSLAViolationsStatistics(result.cloudletFinishedList());
            printVmsCpuUtilizationAndPowerConsumption(result.vms());

            return new AlgorithmResult(
                    label,
                    result.cloudlets(),
                    result.hosts(),
                    result.vms(),
                    result.cloudletFinishedList(),
                    total_time
            );

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
