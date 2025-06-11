package org.APD.Algorithms;

import org.APD.DeadlineCloudlet;
import org.APD.PowerModels.PowerModelPStateProcessor;
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
    protected int HOSTS = 3;
    protected int HOST_PES = 1;

    /** Indicates the time (in seconds) the Host takes to start up. */
    protected double HOST_START_UP_DELAY = 0;

    /** Indicates the time (in seconds) the Host takes to shut down. */
    protected double HOST_SHUT_DOWN_DELAY = 3;

    /** Indicates Host power consumption (in Watts) during startup. */
    protected double HOST_START_UP_POWER = 5;

    /** Indicates Host power consumption (in Watts) during shutdown. */
    protected double HOST_SHUT_DOWN_POWER = 3;

    protected int VMS = 3;
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

    int TOTAL_FRAMES = 3; // how long you want the simulation to run in 10s chunks
    protected static int MIPS_PER_VM = 1000; // Adjust this to your VM's actual MIPS capacity
    protected static int MIPS_PER_HOST = 1000; // Adjust this to your Host's actual MIPS capacity

    protected static int MIPS_PER_VM_INITIAL = 1000; // Adjust this to your VM's actual MIPS capacity
    protected static int MIPS_PER_HOST_INITIAL = 1000; // Adjust this to your Host's actual MIPS capacity
    double MIPS_PER_CLOUDLET_COMPLETION_ORDER_OF_10 = Math.log10(CLOUDLET_LENGTH_MIN);
    protected static int POWER_STATE = 0;

    protected int TOTAL_CLOUDLETS = CLOUDLETS_PER_FRAME * TOTAL_FRAMES;

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
        for (Vm vm : vmList) {
            //vm.getUtilizationHistory().enable(); // Enable utilization history if not already enabled
            final var powerModel = vm.getHost().getPowerModel();
            final double hostStaticPower = powerModel instanceof PowerModelPStateProcessor powerModelHost ? powerModelHost.getStaticPower() : 0;
            final double hostStaticPowerByVm = hostStaticPower / vm.getHost().getVmCreatedList().size();

            //VM CPU utilization relative to the host capacity
            final double vmRelativeCpuUtilization = vm.getCpuUtilizationStats().getMean() / vm.getHost().getVmCreatedList().size();
            final double vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm; // W
            final VmResourceStats cpuStats = vm.getCpuUtilizationStats();
            // also print the hostStaticPower, hostStaticPowerByVM, and vmRelativeCpuUtilization
            System.out.printf(
                    "Vm   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W (Host Static Power: %.1f W, Host Static Power by VM: %.1f W, VM Relative CPU Utilization: %.2f)%n",
                    vm.getId(), cpuStats.getMean() * 100, vmPower, hostStaticPower, hostStaticPowerByVm, vmRelativeCpuUtilization);
            // print vm.getHost().getVmCreatedList().size()
            System.out.printf("Host %2d VMs: %d%n", vm.getHost().getId(), vm.getHost().getVmCreatedList().size());
        }

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

    /**
     * Creates a {@link Datacenter} and its {@link Host}s.
     */
    protected Datacenter createDatacenter() {

        for(int i = 0; i < HOSTS; i++) {
            final var host = createPowerHost(i);
            hostList.add(host);
        }

        final var dc = new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    protected Datacenter createDatacenter(CloudSimPlus simulation, List<Host> hostList) {
        for (int i = 0; i < HOSTS; i++) {
            final var host = createPowerHost(i);
            hostList.add(host);
        }

        final var dc = new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    protected Host createPowerHost(final int id) {
        final var peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(MIPS_PER_HOST));
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

        host.setId(id)
                .setVmScheduler(vmScheduler)
                .setPowerModel(powerModel);
        host.enableUtilizationStats();

        return host;
    }

    /**
     * Creates a list of VMs.
     */
    protected List<Vm> createVms() {
        final var list = new ArrayList<Vm>(VMS);
        for (int i = 0; i < VMS; i++) {
            final var vm = new VmSimple(i, MIPS_PER_VM, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10000).enableUtilizationStats();
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }

        return list;
    }

    protected  List<DeadlineCloudlet> createCloudlets() {
        final List<DeadlineCloudlet> cloudletList = new ArrayList<>();
        final var utilization = new UtilizationModelDynamic(0.002);
        final Random random = new Random();

        int id = 0;
        int pes = 1;

        for (int frame = 0; frame < TOTAL_FRAMES; frame++) {
            double frameStartTime = frame * 10;
            int cloudletsThisFrame = CLOUDLETS_PER_FRAME - 2 + random.nextInt(5); // between 8 and 12 cloudlets

            for (int i = 0; i < cloudletsThisFrame; i++) {
                double execTimeSec = 1.0 + random.nextDouble() * 2.0; // 1–5s
//                long length = (long) (execTimeSec * MIPS_PER_CLOUDLET_COMPLETION); // length = time × MIPS
                long length = (long) Math.min(CLOUDLET_LENGTH_MIN + random.nextDouble() * CLOUDLET_LENGTH_MAX, CLOUDLET_LENGTH_MAX);

                double submissionDelay = frameStartTime + random.nextDouble() * 10;
                double deadline = submissionDelay + execTimeSec * 10 + 5.0; // 1s margin

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
                double executionRequirement = cl.getLength() / cl.getVm().getMips();
                double arrivalTime = dc.getSubmissionDelay();

                System.out.printf("Cloudlet %d: Finish Time = %.2f, Deadline = %.2f -> %s, Arrival Time: %.2f, Execution Requirement = %.2f%n",
                        dc.getId(), finish, deadline, metDeadline ? "OK" : "VIOLATED", arrivalTime,executionRequirement);

                if (!metDeadline) violations++;
            }
        }

        System.out.printf("Total SLA violations: %d out of %d cloudlets (%.2f%%)%n",
                violations, total, violations * 100.0 / total);
    }

    protected boolean wereSLAViolations(List<DeadlineCloudlet> cloudletListFinished) {

        int violations = 0;
        int total = 0;

        for (Cloudlet cl : cloudletListFinished) {
            if (cl instanceof DeadlineCloudlet dc) {
                total++;
                double finish = dc.getFinishTime();
                double deadline = dc.getDeadline();
                boolean metDeadline = finish <= deadline;
                double executionRequirement = cl.getLength() / cl.getVm().getMips();
                double arrivalTime = dc.getSubmissionDelay();

                if (!metDeadline) return true;
            }
        }
        return false;
    }
}
