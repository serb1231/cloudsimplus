package org.APD;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.power.models.PowerModel;
import org.cloudsimplus.power.models.PowerModelHostSimple;
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

import static java.util.Comparator.comparingLong;

public abstract class BaseSchedulingAlgorithm implements SchedulingAlgorithm {
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

    protected int CLOUDLETS = 9;
    protected int CLOUDLET_PES = 1;
    protected int CLOUDLET_LENGTH_MIN = 100000;
    protected int CLOUDLET_LENGTH_MAX = 500000;

    List<Cloudlet> cloudletList;

    /**
     * Defines the power a Host uses, even if it's idle (in Watts).
     */
    protected double STATIC_POWER = 35;

    /**
     * The max power a Host uses (in Watts).
     */
    protected int MAX_POWER = 50;

    protected CloudSimPlus simulation;
    protected DatacenterBroker broker0;
    protected List<Vm> vmList;
    protected List<Host> hostList;

    protected void copyGivenDataLocally(RelevantDataForAlgorithms relevantDataForAlgorithms) {
        // copy all the relevant data from the record to local variables
        SCHEDULING_INTERVAL = relevantDataForAlgorithms.schedulingInterval();
        HOSTS = relevantDataForAlgorithms.hosts();
        HOST_PES = relevantDataForAlgorithms.hostPes();
        HOST_START_UP_DELAY = relevantDataForAlgorithms.hostStartUpDelay();
        HOST_SHUT_DOWN_DELAY = relevantDataForAlgorithms.hostShutDownDelay();
        HOST_START_UP_POWER = relevantDataForAlgorithms.hostStartUpPower();
        HOST_SHUT_DOWN_POWER = relevantDataForAlgorithms.hostShutDownPower();
        VMS = relevantDataForAlgorithms.vms();
        VM_PES = relevantDataForAlgorithms.vmPes();
        CLOUDLETS = relevantDataForAlgorithms.cloudlets();
        CLOUDLET_PES = relevantDataForAlgorithms.cloudletPes();
        CLOUDLET_LENGTH_MIN = relevantDataForAlgorithms.cloudletLengthMin();
        CLOUDLET_LENGTH_MAX = relevantDataForAlgorithms.cloudletLengthMax();
        STATIC_POWER = relevantDataForAlgorithms.staticPower();
        MAX_POWER = relevantDataForAlgorithms.maxPower();
        vmList = relevantDataForAlgorithms.vmList();
        cloudletList = relevantDataForAlgorithms.cloudletList();
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
    protected void printVmsCpuUtilizationAndPowerConsumption() {
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
    protected void printHostsCpuUtilizationAndPowerConsumption() {
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
        System.out.printf(
                "Host %2d CPU Usage mean: %6.1f%% | Power Consumption mean: %8.0f W%n",
                host.getId(), utilizationPercentMean * 100, watts);
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

    protected Host createPowerHost(final int id) {
        final var peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(1000));
        }

        final long ram = 2048; //in Megabytes
        final long bw = 10000; //in Megabits/s
        final long storage = 1000000; //in Megabytes
        final var vmScheduler = new VmSchedulerSpaceShared();


        final var host = new HostSimple(ram, bw, storage, peList);
        host.setStartupDelay(HOST_START_UP_DELAY)
                .setShutDownDelay(HOST_SHUT_DOWN_DELAY);

        final var powerModel = new PowerModelHostSimple(MAX_POWER, STATIC_POWER);
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
            final var vm = new VmSimple(i, 1000, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10000).enableUtilizationStats();
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            list.add(vm);
        }

        return list;
    }

    protected List<Cloudlet> createCloudlets() {
        final var cloudletList = new ArrayList<Cloudlet>(CLOUDLETS);
        final var utilization = new UtilizationModelDynamic(0.002);
        Random r = new Random();

        for (int i = 0; i < CLOUDLETS; i++) {
            int low = CLOUDLET_LENGTH_MIN;
            final long length = r.nextInt(CLOUDLET_LENGTH_MAX - low) + low;

            Cloudlet cloudlet = new CloudletSimple(i, length, CLOUDLET_PES)
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

    @Override
    public String getName() {
        return "FCFSAlgorithm_bin";
    }
}
