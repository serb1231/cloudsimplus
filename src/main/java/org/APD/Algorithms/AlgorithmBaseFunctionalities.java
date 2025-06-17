package org.APD.Algorithms;

import org.APD.DeadlineCloudlet;
import org.APD.PowerModels.PowerModelPStateProcessor;
import org.apache.commons.math3.analysis.function.Pow;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.APD.PowerModels.PowerModelPstateProcessor_2GHz_Via_C7_M;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.HostResourceStats;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmResourceStats;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingLong;

public class AlgorithmBaseFunctionalities {
    /**
     * Defines, between other things, the time intervals
     * to keep Hosts CPU utilization history records.
     */
    protected int SCHEDULING_INTERVAL = 1;
    protected int HOSTS = 5;
    protected int HOST_PES = 1;

    /** Indicates the time (in seconds) the Host takes to start up. */
    protected double HOST_START_UP_DELAY = 0;

    /** Indicates the time (in seconds) the Host takes to shut down. */
    protected double HOST_SHUT_DOWN_DELAY = 3;

    /** Indicates Host power consumption (in Watts) during startup. */
    protected double HOST_START_UP_POWER = 5;

    /** Indicates Host power consumption (in Watts) during shutdown. */
    protected double HOST_SHUT_DOWN_POWER = 3;

    protected int VMS = 5;
    protected int VM_PES = 1;

    protected int CLOUDLETS_PER_FRAME = 5;
    protected int CLOUDLET_PES = 1;
    protected int CLOUDLET_LENGTH_MIN = 1000;
    protected int CLOUDLET_LENGTH_MAX = 5000;

    List<DeadlineCloudlet> cloudletList;

    /**
     * Defines the power a Host uses, even if it's idle (in Watts).
     */
    protected double STATIC_POWER = 500;

    /**
     * The max power a Host uses (in Watts).
     */
    protected int MAX_POWER = 5000;

    protected CloudSimPlus simulation;
    protected DatacenterBroker broker0;
    protected List<Vm> vmList;
    protected List<Host> hostList;

    int TOTAL_FRAMES = 30; // how long you want the simulation to run in 10s chunks
    protected static int MIPS_PER_VM_MAX = 5000; // Adjust this to your VM's actual MIPS capacity
    protected static int MIPS_PER_HOST_MAX = 5000; // Adjust this to your Host's actual MIPS capacity

    protected static int MIPS_PER_VM_MIN = 2000; // Adjust this to your VM's actual MIPS capacity
    protected static int MIPS_PER_HOST_MIN = 2000; // Adjust this to your Host's actual MIPS capacity

    protected static int MIPS_PER_VM_INITIAL_MAX = 5000; // Adjust this to your VM's actual MIPS capacity
    protected static int MIPS_PER_HOST_INITIAL_MAX = 5000; // Adjust this to your Host's actual MIPS capacity


    protected static int MIPS_PER_VM_INITIAL_MIN = 2000; // Adjust this to your VM's actual MIPS capacity
    protected static int MIPS_PER_HOST_INITIAL_MIN = 2000; // Adjust this to your Host's actual MIPS capacity

    protected static int POWER_STATE = 7;

    protected static int TOTAL_CLOUDLETS = 0; // total number of cloudlets to be created, set after creating the cloudlet list

    /**
     * Creates a {@link Datacenter} and its {@link Host}s.
     */
    protected Datacenter createDatacenter() {
//        List<Host> hostList = createHostsInitialDistribution();
        final var dc = new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    /**
     * Creates a {@link Datacenter} and its {@link Host}s.
     * This method is used when the hosts are already created and passed as a parameter.
     */
    protected Datacenter createDatacenter(CloudSimPlus simulation, List<Host> hostList) {
//        hostList = createHostsInitialDistribution();
        final var dc = new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    protected List<Host> createHostsInitialDistribution() {
        final var list = new ArrayList<Host>(HOSTS);
        int mips_per_host = MIPS_PER_HOST_MAX;
        int step = (MIPS_PER_HOST_MAX - MIPS_PER_HOST_MIN) / (HOSTS - 1);
        for (int i = 0; i < HOSTS; i++) {
            final var host = createPowerHost(i, mips_per_host, (double) mips_per_host / MIPS_PER_HOST_MAX);
            list.add(host);
            mips_per_host -= step; // Decrease MIPS for each host
        }
        return list;
    }

    // the procent slower is done for making the initial host respect a linear distribution. Eachone is a little
    // slower than the previous one, so that the first one is the fastest and the last one is the slowest
    // also the first one is consuming the most power, and the last one is consuming the least power
    // this way, eachh one of them has basically some power states that are by some percentage slower than the previous one
    protected Host createPowerHost(final int id, final int mipsPerHost, final double procentSlowerAffectsOnlyPStateNotMIPS) {
        final var peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(mipsPerHost));
        }

        final long ram = 2048; //in Megabytes
        final long bw = 10000; //in Megabits/s
        final long storage = 1000000; //in Megabytes
        final var vmScheduler = new VmSchedulerSpaceShared();


        final var host = new HostSimple(ram, bw, storage, peList);
        host.setStartupDelay(HOST_START_UP_DELAY)
                .setShutDownDelay(HOST_SHUT_DOWN_DELAY);

        final var powerModel = new PowerModelPstateProcessor_2GHz_Via_C7_M(POWER_STATE);
        powerModel
                .setStartupPower(HOST_START_UP_POWER)
                .setShutDownPower(HOST_SHUT_DOWN_POWER);

        powerModel.modifyPerformanceStatesByPercentageSlower(procentSlowerAffectsOnlyPStateNotMIPS);

        host.setId(id)
                .setVmScheduler(vmScheduler)
                .setPowerModel(powerModel);
        host.enableUtilizationStats();

        return host;
    }

    protected List<Host> copyHosts(List<Host> originalHosts) {
        List<Host> copiedHosts = new ArrayList<>();

        for (Host original : originalHosts) {
            int id = (int) original.getId();

            // Extract MIPS from the first PE (assuming all PEs have the same MIPS)
            int mips = (int) original.getPeList().get(0).getCapacity();

            // Extract the power model to get current performance level (P-state)
            PowerModelPStateProcessor powerModel = new PowerModelPStateProcessor((PowerModelPStateProcessor) original.getPowerModel());

            // Create a new Host using your existing method
            Host copy = createPowerHost(id, mips, 1);
            copy.setPowerModel(powerModel);

            copiedHosts.add(copy);
        }

        return copiedHosts;
    }

    protected HostVmPair modifyHostAndVmToHaveOneHostWithLowerPower(List<Host> hostList, List<Vm> vmList, Vm vmToModify) {
        // The vm we received is from the last iteration, hence has a different place in memory. We should look at id, as they are indexed
        // Find the host of the VM to modify
        Host hostToModify = vmToModify.getHost();
        if (hostToModify == null) {
            throw new IllegalArgumentException("The VM does not have a host assigned.");
        }
        // Create a new host with lower MIPS and power consumption
        double getCurentHostMips = hostToModify.getMips();
        int currentPerformanceState = ((PowerModelPStateProcessor) hostToModify.getPowerModel()).getCurrentPerformanceState();
        double currentProcessingFraction = ((PowerModelPStateProcessor) hostToModify.getPowerModel()).getPossiblePerformanceStates()[currentPerformanceState].processingFraction();
        // if this processingFraction is already the minimum, return false
//        if (currentPerformanceState == 0) {
//            System.out.println("The host is already at the minimum performance state, cannot modify further.");
//            return null; // No modification possible
//        }
        int futurePerformanceState = currentPerformanceState - 1; // Move to the next performance state
        double newProcessingFraction = ((PowerModelPStateProcessor) hostToModify.getPowerModel()).getPossiblePerformanceStates()[futurePerformanceState].processingFraction();

        // create a new host with lower MIPS and power consumption
        PowerModelPStateProcessor powerModel = new PowerModelPStateProcessor((PowerModelPStateProcessor) hostToModify.getPowerModel());
        powerModel.setCurrentPerformanceState(futurePerformanceState);
        // print the currentProcessingFraction and the newProcessingFraction
//        System.out.printf("Current Processing Fraction: %.2f, New Processing Fraction: %.2f%n", currentProcessingFraction, newProcessingFraction);
        Host hostToModifyNew = createPowerHost(
                (int) hostToModify.getId(),
                (int) (getCurentHostMips / currentProcessingFraction * newProcessingFraction),
                1
        );
        hostToModifyNew.setPowerModel(powerModel);
        Vm vmToModifyNew = new VmSimple(vmToModify.getId(), (int) (vmToModify.getMips() / currentProcessingFraction * newProcessingFraction), vmToModify.getPesNumber());

        List<Host> newHostList = new ArrayList<>();
        for (Host h : hostList) {
            if (h.getId() == hostToModify.getId()) {
                newHostList.add(hostToModifyNew); // replace the host
            } else {
                newHostList.add(h); // copy unchanged
            }
        }

        List<Vm> newVmList = new ArrayList<>();
        for (Vm v : vmList) {
            if (v.getId() == vmToModify.getId()) {
                newVmList.add(vmToModifyNew); // replace the VM
            } else {
                newVmList.add(v); // copy unchanged
            }
        }


        // Return the modified host and VM
        return new HostVmPair(newHostList, newVmList);
    }

    protected  List<DeadlineCloudlet> createCloudletsUniformDistribution_Outdated() {
        final List<DeadlineCloudlet> cloudletList = new ArrayList<>();
        final var utilization = new UtilizationModelDynamic(0.002);
        final Random random = new Random();

        int id = 0;
        int pes = 1;

        for (int frame = 0; frame < TOTAL_FRAMES; frame++) {
            double frameStartTime = frame * 10;
            int cloudletsThisFrame = CLOUDLETS_PER_FRAME - 2 + random.nextInt(5); // between 8 and 12 cloudlets

            // during the simulation, for 10% of the frames, inject HOST_NR tasks that have 10 times the length of a normal cloudlet, and the deadline is huge
            boolean injectBurst = random.nextDouble() < 0.1; // 10% chance to inject a burst of cloudlets
            if (injectBurst) {
                cloudletsThisFrame = HOSTS; // inject a burst of 10 extra cloudlets
            }

            for (int i = 0; i < cloudletsThisFrame; i++) {
                double execTimeSec;
                double submissionDelay;
                double deadline;
                long length;
                if (injectBurst) {
                    execTimeSec = 100 * (1.0 + random.nextDouble() * 2.0); // 1–5s
                    length = (long) (10 * Math.min(CLOUDLET_LENGTH_MIN + random.nextDouble() * CLOUDLET_LENGTH_MAX, CLOUDLET_LENGTH_MAX));
                }
                else {
                    execTimeSec = 1.0 + random.nextDouble() * 2.0; // 1–3s
                    // length = time × MIPS
                    length = (long) Math.min(CLOUDLET_LENGTH_MIN + random.nextDouble() * CLOUDLET_LENGTH_MAX, CLOUDLET_LENGTH_MAX);
                }

                submissionDelay = frameStartTime + random.nextDouble() * 10;
                deadline = submissionDelay + execTimeSec * 10 + 5.0; // 1s margin

                DeadlineCloudlet cloudlet = (DeadlineCloudlet) new DeadlineCloudlet(id++, length, pes)
                        .setFileSize(1024)
                        .setOutputSize(1024)
                        .setUtilizationModelCpu(new UtilizationModelFull())
                        .setUtilizationModelRam(utilization)
                        .setUtilizationModelBw(utilization);

                cloudlet.setSubmissionDelay(submissionDelay);
                cloudlet.setDeadline(deadline);


                cloudletList.add(cloudlet);
            }
        }

        // sort the cloudlets by submission delay
        cloudletList.sort(comparingDouble(Cloudlet::getSubmissionDelay));
        // set the TOTAL_CLOUDLETS to the size of the cloudletList
        TOTAL_CLOUDLETS = cloudletList.size();
        return cloudletList;
    }

    protected List<DeadlineCloudlet> createCloudletsBurstyArrivalTightDeadlineHeavyTailoredBigGroupedJobs() {
        final List<DeadlineCloudlet> cloudletList = new ArrayList<>();
        final var utilization = new UtilizationModelDynamic(0.002);
        final Random random = new Random();

        int id = 0;
        int pes = 1;

        for (int frame = 0; frame < TOTAL_FRAMES; frame++) {
            double frameStartTime = frame * 10;

            // Bursty pattern: every 5th frame has a spike in jobs
            int cloudletsThisFrame = CLOUDLETS_PER_FRAME - 2 + random.nextInt(5);
            if (frame % 5 == 0) {
                cloudletsThisFrame += 10; // burst every 5 frames
            }

            // Inject a cluster of tight jobs that should be put each one on a different VM, to simulate a tight cluster, and respect the deadline
            boolean injectTightCluster = random.nextDouble() < 0.2; // 20% chance to inject a tight group
            if (injectTightCluster) {
                double clusterStartTime = frameStartTime + random.nextDouble() * 5; // random time within first half of frame
                for (int j = 0; j < 4; j++) {
                    double execTimeSec = 1.0 + random.nextDouble(); // short jobs
                    long length = (long) (execTimeSec * CLOUDLET_LENGTH_MIN);

                    double submissionDelay = clusterStartTime + j * 0.01; // very close submissions (10ms apart)
                    double deadline = submissionDelay + execTimeSec * 1.2 + 1.0; // tight deadline

                    DeadlineCloudlet cloudlet = (DeadlineCloudlet) new DeadlineCloudlet(id++, length, pes)
                            .setFileSize(1024)
                            .setOutputSize(1024)
                            .setUtilizationModelCpu(new UtilizationModelFull())
                            .setUtilizationModelRam(utilization)
                            .setUtilizationModelBw(utilization);

                    cloudlet.setSubmissionDelay(submissionDelay);
                    cloudlet.setDeadline(deadline);
                    cloudletList.add(cloudlet);
                }
            }


            // Generate a burst of 10 HOST cloudlets with long execution times
            boolean injectBigCloudlets = random.nextDouble() < 0.1; // 10% chance to inject long cloudlets
            if (injectBigCloudlets) {
                for (int j = 0; j < HOSTS; j++) {
                    double execTimeSec = 10.0 + random.nextDouble() * 300.0; // long jobs between 10s and 20s
                    long length = (long) (execTimeSec * CLOUDLET_LENGTH_MIN);

                    double submissionDelay = frameStartTime + random.nextDouble() * 5; // random time within first half of frame
                    double deadline = submissionDelay + execTimeSec * HOSTS + 2.0; // tight deadline

                    DeadlineCloudlet cloudlet = (DeadlineCloudlet) new DeadlineCloudlet(id++, length, pes)
                            .setFileSize(1024)
                            .setOutputSize(1024)
                            .setUtilizationModelCpu(new UtilizationModelFull())
                            .setUtilizationModelRam(utilization)
                            .setUtilizationModelBw(utilization);

                    cloudlet.setSubmissionDelay(submissionDelay);
                    cloudlet.setDeadline(deadline);
                    cloudletList.add(cloudlet);
                }
            }



            for (int i = 0; i < cloudletsThisFrame; i++) {
                // Log-normal-like execution time: most short, some very long
                double mean = 1.5, stdDev = 1.0;
                double logNormalExecTime = Math.exp(mean + stdDev * random.nextGaussian());
                double execTimeSec = Math.min(logNormalExecTime, 20.0); // cap at 20s

                long length = (long) (execTimeSec * CLOUDLET_LENGTH_MIN);
                double submissionDelay = frameStartTime + random.nextDouble() * 10;

                double jitter = 1.0 + random.nextDouble() * 2.0;
                double deadline = submissionDelay + execTimeSec * 1.5 + jitter;

                DeadlineCloudlet cloudlet = (DeadlineCloudlet) new DeadlineCloudlet(id++, length, pes)
                        .setFileSize(1024)
                        .setOutputSize(1024)
                        .setUtilizationModelCpu(new UtilizationModelFull())
                        .setUtilizationModelRam(utilization)
                        .setUtilizationModelBw(utilization);

                cloudlet.setSubmissionDelay(submissionDelay);
                cloudlet.setDeadline(deadline);

                cloudletList.add(cloudlet);
            }
        }

        // Sort cloudlets by submission time
        cloudletList.sort(comparingDouble(Cloudlet::getSubmissionDelay));

        // Update total
        TOTAL_CLOUDLETS = cloudletList.size();
        return cloudletList;
    }

    protected List<DeadlineCloudlet> copyCloudlets(List<DeadlineCloudlet> cloudletList) {
        List<DeadlineCloudlet> cloudletClone = new ArrayList<>(cloudletList.size());

        for (DeadlineCloudlet cloudlet : cloudletList) {
            DeadlineCloudlet clonedCloudlet = (DeadlineCloudlet) new DeadlineCloudlet(cloudlet.getId(), cloudlet.getLength(), cloudlet.getPesNumber())
                    .setFileSize(cloudlet.getFileSize())
                    .setOutputSize(cloudlet.getOutputSize())
                    .setUtilizationModelCpu(cloudlet.getUtilizationModelCpu())
                    .setUtilizationModelRam(cloudlet.getUtilizationModelRam())
                    .setUtilizationModelBw(cloudlet.getUtilizationModelBw());

            // Set the deadline for the cloned cloudlet
            clonedCloudlet.setDeadline(cloudlet.getDeadline());
            clonedCloudlet.setSubmissionDelay(cloudlet.getSubmissionDelay());
            cloudletClone.add(clonedCloudlet);
        }

        return cloudletClone;
    }

    /**
     * Creates a list of VMs.
     */
    protected List<Vm> createVms() {
        final var list = new ArrayList<Vm>(VMS);
        int mips_per_vm = MIPS_PER_VM_MAX;
        int step = (MIPS_PER_VM_MAX - MIPS_PER_VM_MIN) / (VMS - 1);
        for (int i = 0; i < VMS; i++) {
            final var vm = new VmSimple(i, mips_per_vm, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10000).enableUtilizationStats();
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
            mips_per_vm -= step; // Decrease MIPS for each VM
        }

        return list;
    }

    protected List<Vm> copyVMs(List<Vm> vmList) {
        List<Vm> vmClone = new ArrayList<>(vmList.size());

        for (Vm vm : vmList) {
            Vm clonedVm = new VmSimple(vm.getMips(), vm.getPesNumber());

            clonedVm.setRam(vm.getRam().getCapacity())
                    .setBw(vm.getBw().getCapacity())
                    .setSize(vm.getStorage().getCapacity())
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());

            clonedVm.enableUtilizationStats(); // optional

            vmClone.add(clonedVm);
        }

        return vmClone;
    }

    protected boolean wereSLAViolations(List<DeadlineCloudlet> cloudletListFinished) {

        for (Cloudlet cl : cloudletListFinished) {
            if (cl instanceof DeadlineCloudlet dc) {
                double finish = dc.getFinishTime();
                double deadline = dc.getDeadline();
                boolean metDeadline = finish <= deadline;

                if (!metDeadline) return true;
            }
        }
        return false;
    }

    protected void printSLAViolations(List<DeadlineCloudlet> cloudletListFinished) {
        System.out.println("\n------------------------------- SLA VIOLATIONS -------------------------------");

        int violations = 0;
        int total = 0;

        for (Cloudlet cl : cloudletListFinished) {
            if (cl instanceof DeadlineCloudlet dc) {
                total++;
                double finish = dc.getFinishTime();
                double deadline = dc.getDeadline();
                boolean metDeadline = finish <= deadline;
                double executionRequirement = (double) cl.getLength() / MIPS_PER_VM_MIN;
                double arrivalTime = dc.getSubmissionDelay();

                System.out.printf("Cloudlet %d: Finish Time = %.2f, Deadline = %.2f -> %s, Arrival Time: %.2f, Execution Requirement = %.2f%n",
                        dc.getId(), finish, deadline, metDeadline ? "OK" : "VIOLATED", arrivalTime,executionRequirement);

                if (!metDeadline) violations++;
            }
        }

        System.out.printf("Total SLA violations: %d out of %d cloudlets (%.2f%%)%n",
                violations, total, violations * 100.0 / total);
    }

    protected void printSLAViolationsStatistics(List<DeadlineCloudlet> cloudletListFinished) {
        System.out.println("\n------------------------------- SLA VIOLATIONS -------------------------------");

        int violations = 0;
        int total = 0;

        double totalTardiness = 0;
        double maxTardiness = 0;

        double totalExecutionViolated = 0;
        double totalExecution = 0;

        // Tiers data
        int tier1 = 0, tier2 = 0, tier3 = 0;
        double tier1Total = 0, tier2Total = 0, tier3Total = 0;
        double tier1Min = Double.MAX_VALUE, tier2Min = Double.MAX_VALUE, tier3Min = Double.MAX_VALUE;
        double tier1Max = 0, tier2Max = 0, tier3Max = 0;

        for (Cloudlet cl : cloudletListFinished) {
            if (cl instanceof DeadlineCloudlet dc) {
                total++;
                double finish = dc.getFinishTime();
                double deadline = dc.getDeadline();
                double executionRequirement = (double) cl.getLength() / MIPS_PER_VM_MIN;

                boolean metDeadline = finish <= deadline;
                double tardiness = Math.max(0, finish - deadline);
                double relativeViolation = tardiness / deadline;

                totalExecution += executionRequirement;

                if (!metDeadline) {
                    violations++;
                    totalTardiness += tardiness;
                    maxTardiness = Math.max(maxTardiness, tardiness);
                    totalExecutionViolated += executionRequirement;

                    if (relativeViolation <= 0.10) {
                        tier1++;
                        tier1Total += tardiness;
                        tier1Min = Math.min(tier1Min, tardiness);
                        tier1Max = Math.max(tier1Max, tardiness);
                    } else if (relativeViolation <= 0.50) {
                        tier2++;
                        tier2Total += tardiness;
                        tier2Min = Math.min(tier2Min, tardiness);
                        tier2Max = Math.max(tier2Max, tardiness);
                    } else {
                        tier3++;
                        tier3Total += tardiness;
                        tier3Min = Math.min(tier3Min, tardiness);
                        tier3Max = Math.max(tier3Max, tardiness);
                    }
                }
            }
        }

        System.out.println("\n------------------------------- SLA STATISTICS -------------------------------");

        double violationRatio = violations * 100.0 / total;
        double avgTardiness = violations > 0 ? totalTardiness / violations : 0;
        double execViolatedRatio = totalExecution > 0 ? totalExecutionViolated * 100.0 / totalExecution : 0;

        System.out.printf("Total SLA violations: %d out of %d cloudlets (%.2f%%)%n", violations, total, violationRatio);
        System.out.printf("Average tardiness: %.2f seconds%n", avgTardiness);
        System.out.printf("Maximum tardiness: %.2f seconds%n", maxTardiness);
        System.out.printf("Total execution demand affected by violations: %.2f%% of total%n", execViolatedRatio);

        if (violations > 0) {
            System.out.println("\n----------------------- SLA VIOLATION SEVERITY TIERS ------------------------");

            if (tier1 > 0) {
                System.out.printf("Tier 1 (≤10%% over deadline): %d (%.2f%% of violations)%n", tier1, tier1 * 100.0 / violations);
                System.out.printf("  Avg Lateness: %.2f s | Min: %.2f s | Max: %.2f s%n",
                        tier1Total / tier1, tier1Min, tier1Max);
            }

            if (tier2 > 0) {
                System.out.printf("Tier 2 (>10%% and ≤50%%):     %d (%.2f%% of violations)%n", tier2, tier2 * 100.0 / violations);
                System.out.printf("  Avg Lateness: %.2f s | Min: %.2f s | Max: %.2f s%n",
                        tier2Total / tier2, tier2Min, tier2Max);
            }

            if (tier3 > 0) {
                System.out.printf("Tier 3 (>50%%):               %d (%.2f%% of violations)%n", tier3, tier3 * 100.0 / violations);
                System.out.printf("  Avg Lateness: %.2f s | Min: %.2f s | Max: %.2f s%n",
                        tier3Total / tier3, tier3Min, tier3Max);
            }
        }
    }

    /**
     * Prints the following information from VM's utilization stats:
     * <ul>
     *   <li>VM's mean CPU utilization relative to the total Host's CPU utilization.
     *       For instance, if the CPU utilization mean of two equal VMs is 100% of their CPU, the utilization
     *       of each one corresponds to 50% of the Host's CPU utilization.</li>
     *   <li>VM's power consumption relative to the total Host's power consumption.</li>
     * </ul>
     *
     * <p>A Host, even if idle, may consume a static amount of power.
     * Let's say it consumes 20 W in idle state and that for each 1% of CPU use it consumes 1 W more.
     * For the 2 VMs of the example above, each one using 50% of CPU will consume 50 W.
     * That is 100 W for the 2 VMs, plus the 20 W that is static.
     * Therefore, we have a total Host power consumption of 120 W.
     * </p>
     *
     * <p>
     * If we compute the power consumption for a single VM by
     * calling {@code vm.getHost().getPowerModel().getPower(hostCpuUsage)},
     * we get the 50 W consumed by the VM, plus the 20 W of static power.
     * This adds up to 70 W. If the two VMs are equal and using the same amount of CPU,
     * their power consumption would be the half of the total Host's power consumption.
     * This would be 60 W, not 70.
     * </p>
     *
     * <p>This way, we have to compute VM power consumption by sharing a supposed Host static power
     * consumption with each VM, as it's being shown here.
     * Not all {@link PowerModelPstateProcessor_2GHz_Via_C7_M} have this static power consumption.
     * However, the way the VM power consumption
     * is computed here, that detail is abstracted.
     * </p>
     */
    protected void printVmsCpuUtilizationAndPowerConsumption(List<Vm> vmList) {
        vmList.sort(comparingLong(vm -> vm.getHost().getId()));
        double totalPowerConsumption = 0;
        for (Vm vm : vmList) {
            //vm.getUtilizationHistory().enable(); // Enable utilization history if not already enabled
            final var powerModel = vm.getHost().getPowerModel();
            final double hostStaticPower = powerModel instanceof PowerModelPStateProcessor powerModelHost ? powerModelHost.getStaticPower() : 0;
            final double hostStaticPowerByVm = hostStaticPower / vm.getHost().getVmCreatedList().size();

            //VM CPU utilization relative to the host capacity
            final double vmRelativeCpuUtilization = vm.getCpuUtilizationStats().getMean() / vm.getHost().getVmCreatedList().size();
            final double vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm; // W
            totalPowerConsumption += vmPower;
            final VmResourceStats cpuStats = vm.getCpuUtilizationStats();
            // also print the hostStaticPower, hostStaticPowerByVM, and vmRelativeCpuUtilization
            System.out.printf(
                    "Vm   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W (Host Static Power: %.1f W, Host Static Power by VM: %.1f W, VM Relative CPU Utilization: %.2f)%n",
                    vm.getId(), cpuStats.getMean() * 100, vmPower, hostStaticPower, hostStaticPowerByVm, vmRelativeCpuUtilization);
            // print vm.getHost().getVmCreatedList().size()
            System.out.printf("Host %2d VMs: %d%n", vm.getHost().getId(), vm.getHost().getVmCreatedList().size());
        }
        // print the total power consumption of the host
        System.out.println();
        System.out.printf("Total Host Power Consumption: %.0f W%n", totalPowerConsumption);

    }

    /**
     * The Host CPU Utilization History is only computed
     * if VMs utilization history is enabled by calling
     * {@code vm.getUtilizationHistory().enable()}.
     */
    protected void printHostsCpuUtilizationAndPowerConsumption(List<Host> hostList) {
        System.out.println();
        for (final Host host : hostList) {
            printHostCpuUtilizationAndPowerConsumption(host);
        }
        System.out.println();
    }

    protected void printHostCpuUtilizationAndPowerConsumption(final Host host) {
        final HostResourceStats cpuStats = host.getCpuUtilizationStats();

        //The total Host's CPU utilization for the time specified by the map key
        final double utilizationPercentMean = cpuStats.getMean();
        final double watts = host.getPowerModel().getPower(utilizationPercentMean);
        final double maxWattsPossible = host.getPowerModel().getPower(1);
        System.out.printf(
                "Host %2d CPU Usage mean: %6.1f%% | Power Consumption : %8.0f W%n, | Max Power: %8.0f W%n",
                host.getId(), utilizationPercentMean * 100, watts, maxWattsPossible);

    }

    public record HostVmPair(List<Host> hosts, List<Vm> vms) {}
}
