package org.APD;

import ch.qos.logback.classic.Level;
//import org.cloudsimplus.cloudlets.DeadlineCloudlet;
//import org.cloudsimplus.cloudlets.CloudletSimple;
import org.APD.DeadlineCloudlet;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.power.models.PowerModel;
import org.cloudsimplus.power.models.PowerModelHostSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.HostResourceStats;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmResourceStats;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Comparator.comparingLong;

public class CompareAlgorithms {
    /**
     * Defines, between other things, the time intervals
     * to keep Hosts CPU utilization history records.
     */
    private static final int SCHEDULING_INTERVAL = 1;
    private static final int HOSTS = 8;
    private static final int HOST_PES = 1;

    /** Indicates the time (in seconds) the Host takes to start up. */
    private static final double HOST_START_UP_DELAY = 0;

    /** Indicates the time (in seconds) the Host takes to shut down. */
    private static final double HOST_SHUT_DOWN_DELAY = 3;

    /** Indicates Host power consumption (in Watts) during startup. */
    private static final double HOST_START_UP_POWER = 5;

    /** Indicates Host power consumption (in Watts) during shutdown. */
    private static final double HOST_SHUT_DOWN_POWER = 3;

    private static final int VMS = 8;
    private static final int VM_PES = 1;

    private static final int CLOUDLETS = 300;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH_MIN = 1000;
    private static final int CLOUDLET_LENGTH_MAX = 5000;

    /**
     * Defines the power a Host uses, even if it's idle (in Watts).
     */
    private static final double STATIC_POWER = 35;

    /**
     * The max power a Host uses (in Watts).
     */
    private static final int MAX_POWER = 50;

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);

        List<Vm> vmList = createVms();
        List<DeadlineCloudlet> cloudletList = createCloudlets();

        List<Vm> vmListCopy = copyVMs(vmList);
        List<DeadlineCloudlet> cloudletListCopy = copyCloudlets(cloudletList);

        RelevantDataForAlgorithms data = new RelevantDataForAlgorithms(
                SCHEDULING_INTERVAL,
                HOSTS,
                HOST_PES,
                HOST_START_UP_DELAY,
                HOST_SHUT_DOWN_DELAY,
                HOST_START_UP_POWER,
                HOST_SHUT_DOWN_POWER,
                VMS,
                VM_PES,
                CLOUDLETS,
                CLOUDLET_PES,
                CLOUDLET_LENGTH_MIN,
                CLOUDLET_LENGTH_MAX,
                STATIC_POWER,
                MAX_POWER,
                vmListCopy,
                cloudletListCopy
        );

        SchedulingAlgorithm fcfs = new FCFSAlgorithm_bin();
        SchedulingAlgorithm roundRobin = new RoundRobinAlgorithm();
        SchedulingAlgorithm powerAware = new ACOAlgorithm();

        AlgorithmResult resultFCFS = fcfs.run(data);
        AlgorithmResult resultRoundRobin = roundRobin.run(data);
        AlgorithmResult resultPowerAware = powerAware.run(data);
        AlgorithmResult resultACO = powerAware.run(data);


        System.out.println("FCFS Algorithm Result:");
        printVmsCpuUtilizationAndPowerConsumption(resultFCFS.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultFCFS.hosts());

        double makespan = resultFCFS.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);

        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespan);

        System.out.println("Round Robin Algorithm Result:");
        printVmsCpuUtilizationAndPowerConsumption(resultRoundRobin.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultRoundRobin.hosts());

        double makespanRoundRobin = resultRoundRobin.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);

        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanRoundRobin);

        System.out.println("Power Aware Algorithm Result:");
        printVmsCpuUtilizationAndPowerConsumption(resultPowerAware.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultPowerAware.hosts());

        double makespanPowerAware = resultPowerAware.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);
        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanPowerAware);

        System.out.println("ACO Algorithm Result:");
        printVmsCpuUtilizationAndPowerConsumption(resultACO.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultACO.hosts());

        double makespanACO = resultACO.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);
        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanACO);



    }

    private static List<DeadlineCloudlet> copyCloudlets(List<DeadlineCloudlet> cloudletList) {
        List<DeadlineCloudlet> cloudletClone = new ArrayList<>(cloudletList.size());

        for (DeadlineCloudlet cloudlet : cloudletList) {
            DeadlineCloudlet clonedCloudlet = (DeadlineCloudlet) new DeadlineCloudlet(cloudlet.getId(), cloudlet.getLength(), cloudlet.getPesNumber())
                    .setFileSize(cloudlet.getFileSize())
                    .setOutputSize(cloudlet.getOutputSize())
                    .setUtilizationModelCpu(cloudlet.getUtilizationModelCpu())
                    .setUtilizationModelRam(cloudlet.getUtilizationModelRam())
                    .setUtilizationModelBw(cloudlet.getUtilizationModelBw());

            cloudletClone.add(clonedCloudlet);
        }

        return cloudletClone;
    }

    private static List<Vm> copyVMs(List<Vm> vmList) {
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

    /**
     * Creates a list of VMs.
     */
    private static List<Vm> createVms() {
        final var list = new ArrayList<Vm>(VMS);
        for (int i = 0; i < VMS; i++) {
            final var vm = new VmSimple(i, 1000, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10000).enableUtilizationStats();
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }

        return list;
    }

    private static List<DeadlineCloudlet> createCloudlets() {
        final var cloudletList = new ArrayList<DeadlineCloudlet>(CLOUDLETS);
        final var utilization = new UtilizationModelDynamic(0.002);
        Random r = new Random();

        for (int i = 0; i < CLOUDLETS; i++) {
            int low = CLOUDLET_LENGTH_MIN;
            final long length = r.nextInt(CLOUDLET_LENGTH_MAX - low) + low;

            DeadlineCloudlet cloudlet = (DeadlineCloudlet) new DeadlineCloudlet(i, length, CLOUDLET_PES)
                    .setFileSize(1024)
                    .setOutputSize(1024)
                    .setUtilizationModelCpu(new UtilizationModelFull())
                    .setUtilizationModelRam(utilization)
                    .setUtilizationModelBw(utilization);

            // Set submission delay to simulate staggered arrival times
            double delay = i * 2; // each cloudlet arrives 2 seconds after the previous
            cloudlet.setSubmissionDelay(delay);

            cloudletList.add(cloudlet);
        }

        return cloudletList;
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
     * Lets say it consumes 20 W in idle state and that for each 1% of CPU use it consumes 1 W more.
     * For the 2 VMs of the example above, each one using 50% of CPU will consume 50 W.
     * That is 100 W for the 2 VMs, plus the 20 W that is static.
     * Therefore we have a total Host power consumption of 120 W.
     * </p>
     *
     * <p>
     * If we computer the power consumption for a single VM by
     * calling {@code vm.getHost().getPowerModel().getPower(hostCpuUsage)},
     * we get the 50 W consumed by the VM, plus the 20 W of static power.
     * This adds up to 70 W. If the two VMs are equal and using the same amount of CPU,
     * their power consumption would be the half of the total Host's power consumption.
     * This would be 60 W, not 70.
     * </p>
     *
     * <p>This way, we have to compute VM power consumption by sharing a supposed Host static power
     * consumption with each VM, as it's being shown here.
     * Not all {@link PowerModel} have this static power consumption.
     * However, the way the VM power consumption
     * is computed here, that detail is abstracted.
     * </p>
     */
    protected static void printVmsCpuUtilizationAndPowerConsumption(List<Vm> vmList) {
        vmList.sort(comparingLong(vm -> vm.getHost().getId()));
        for (Vm vm : vmList) {
            final var powerModel = vm.getHost().getPowerModel();
            final double hostStaticPower = powerModel instanceof PowerModelHostSimple powerModelHost ? powerModelHost.getStaticPower() : 0;
            final double hostStaticPowerByVm = hostStaticPower / vm.getHost().getVmCreatedList().size();

            //VM CPU utilization relative to the host capacity
            final double vmRelativeCpuUtilization = vm.getCpuUtilizationStats().getMean() / vm.getHost().getVmCreatedList().size();
            final double vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm; // W
            final VmResourceStats cpuStats = vm.getCpuUtilizationStats();
            System.out.printf(
                    "Vm   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W%n",
                    vm.getId(), cpuStats.getMean() *100, vmPower);
        }
    }

    /**
     * The Host CPU Utilization History is only computed
     * if VMs utilization history is enabled by calling
     * {@code vm.getUtilizationHistory().enable()}.
     */
    protected static void printHostsCpuUtilizationAndPowerConsumption(List<Host> hostList) {
        System.out.println();
        for (final Host host : hostList) {
            printHostCpuUtilizationAndPowerConsumption(host);
        }
        System.out.println();
    }

    protected static void printHostCpuUtilizationAndPowerConsumption(final Host host) {
        final HostResourceStats cpuStats = host.getCpuUtilizationStats();

        //The total Host's CPU utilization for the time specified by the map key
        final double utilizationPercentMean = cpuStats.getMean();
        final double watts = host.getPowerModel().getPower(utilizationPercentMean);
        System.out.printf(
                "Host %2d CPU Usage mean: %6.1f%% | Power Consumption mean: %8.0f W%n",
                host.getId(), utilizationPercentMean * 100, watts);
    }
}
