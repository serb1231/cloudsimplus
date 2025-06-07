/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.APD;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.examples.power.PowerSpecFileExample;
import org.cloudsimplus.examples.resourceusage.VmsCpuUsageExample;
import org.cloudsimplus.examples.resourceusage.VmsRamAndBwUsageExample;
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

import java.util.*;

import static java.util.Comparator.comparingLong;

/**
 * An example to show power consumption of Hosts and VMs.
 * Realize that for this goal, you define a {@link PowerModel}
 * for each Host by calling {@code host.setPowerModel(powerModel)}.
 *
 * <p>It creates the number of cloudlets defined in
 * {@link #CLOUDLETS}. All cloudlets will require 100% of PEs they are using all the time.
 * these cloudlets are created with the length defined by {@link #CLOUDLET_LENGTH_MIN} and
 * the other half with the double of this length, defined by {@link #CLOUDLET_LENGTH_MAX}.
 * and the other half will have the double of this length.
 * This way, it's possible to see that for the last half of the
 * simulation time, a Host doesn't use the entire CPU capacity,
 * and therefore doesn't consume the maximum power.</p>
 *
 * <p>However, you may notice in this case that the power usage isn't
 * half of the maximum consumption, because there is a static minimum
 * amount of power to use, even if the Host is idle,
 * which is defined by {@link #STATIC_POWER}.
 * Check {@link PowerModelHostSimple#PowerModelHostSimple(double, double)}.
 * </p>
 *
 * <p>Realize that the Host CPU Utilization History is only stored
 * if VMs utilization history is enabled by calling
 * {@code vm.getUtilizationHistory().enable()}</p>
 *
 * <p>Each line in the table with CPU utilization and power consumption shows
 * the data from the time specified in the line, up to the time before the value in the next line.
 * For instance, consider the scheduling interval is 10, the time in the first line is 1 and
 * it shows 100% CPU utilization and 100 W of power consumption.
 * Then, the next line contains data for time 10.
 * It means that for any point between time 1 to time 10,
 * the CPU utilization and power consumption is the one provided in that first line.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.2.4
 *
 * @see PowerSpecFileExample
 * @see VmsRamAndBwUsageExample
 * @see VmsCpuUsageExample
 */
public class FCFSAlgorithm_bin  extends BaseSchedulingAlgorithm {
//    /**
//     * Defines, between other things, the time intervals
//     * to keep Hosts CPU utilization history records.
//     */
//    private int SCHEDULING_INTERVAL = 1;
//    private int HOSTS = 3;
//    private int HOST_PES = 1;
//
//    /** Indicates the time (in seconds) the Host takes to start up. */
//    private double HOST_START_UP_DELAY = 0;
//
//    /** Indicates the time (in seconds) the Host takes to shut down. */
//    private double HOST_SHUT_DOWN_DELAY = 3;
//
//    /** Indicates Host power consumption (in Watts) during startup. */
//    private double HOST_START_UP_POWER = 5;
//
//    /** Indicates Host power consumption (in Watts) during shutdown. */
//    private double HOST_SHUT_DOWN_POWER = 3;
//
//    private int VMS = 3;
//    private int VM_PES = 1;
//
//    private int CLOUDLETS = 9;
//    private int CLOUDLET_PES = 1;
//    private int CLOUDLET_LENGTH_MIN = 100000;
//    private int CLOUDLET_LENGTH_MAX = 500000;
//
//    List<Cloudlet> cloudletList;
//
//    /**
//     * Defines the power a Host uses, even if it's idle (in Watts).
//     */
//    private double STATIC_POWER = 35;
//
//    /**
//     * The max power a Host uses (in Watts).
//     */
//    private int MAX_POWER = 50;
//
//    private CloudSimPlus simulation;
//    private DatacenterBroker broker0;
//    private List<Vm> vmList;
//    private List<Host> hostList;

    public static void main(String[] args) {
        new FCFSAlgorithm_bin();
    }

    FCFSAlgorithm_bin() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);
        Datacenter datacenter0 = createDatacenter();
        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);
        vmList = createVms();
        cloudletList = createCloudlets();
//        broker0.submitCloudletList(cloudletList);

        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
        for (Vm vm : vmList) {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            // print the VM allocation
            System.out.printf("Vm %d allocated to Broker %d%n", vm.getId(), broker0.getId());
        }

        broker0.submitVmList(vmList);
//        broker0.submitCloudlet(cloudletList.get(0));
        Queue<Cloudlet> queue = new ArrayDeque<>(cloudletList);
        Set<Vm> busyVms = new HashSet<>();
        simulation.addOnSimulationStartListener(evt -> {
            System.out.printf("Simulation clock: %.2f\n", simulation.clock());
            submitNextFCFS(queue, broker0, vmList, busyVms);
        });

        simulation.start();

        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL+" -------------------------------");
        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        final Comparator<Cloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));

        new CloudletsTableBuilder(cloudletFinishedList).build();
        printHostsCpuUtilizationAndPowerConsumption();
        printVmsCpuUtilizationAndPowerConsumption();
    }

    @Override
    public void run(RelevantDataForAlgorithms relevantDataForAlgorithms) {

        copyGivenDataLocally(relevantDataForAlgorithms);
        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);

        Datacenter datacenter0 = createDatacenter();
        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);
//        vmList = copyVMs(relevantDataForAlgorithms.vmList);
//        List<Cloudlet> cloudletList = copyCloudlets(relevantDataForAlgorithms.cloudletList);
//        broker0.submitCloudletList(cloudletList);

        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
        for (Vm vm : vmList) {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            // print the VM allocation
            System.out.printf("Vm %d allocated to Broker %d%n", vm.getId(), broker0.getId());
        }

        broker0.submitVmList(vmList);
//        broker0.submitCloudlet(cloudletList.get(0));
        Queue<Cloudlet> queue = new ArrayDeque<>(cloudletList);
        Set<Vm> busyVms = new HashSet<>();
        simulation.addOnSimulationStartListener(evt -> {
            System.out.printf("Simulation clock: %.2f\n", simulation.clock());
            submitNextFCFS(queue, broker0, vmList, busyVms);
        });

        simulation.start();

        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL+" -------------------------------");
        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        final Comparator<Cloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));

        new CloudletsTableBuilder(cloudletFinishedList).build();
        printHostsCpuUtilizationAndPowerConsumption();
        printVmsCpuUtilizationAndPowerConsumption();

//        return new AlgorithmResult(getName(), makespan, totalEnergy, avgVmUtil);
    }

//    private void copyGivenDataLocally(RelevantDataForAlgorithms relevantDataForAlgorithms) {
//        // copy all the relevant data from the record to local variables
//        SCHEDULING_INTERVAL = relevantDataForAlgorithms.schedulingInterval();
//        HOSTS = relevantDataForAlgorithms.hosts();
//        HOST_PES = relevantDataForAlgorithms.hostPes();
//        HOST_START_UP_DELAY = relevantDataForAlgorithms.hostStartUpDelay();
//        HOST_SHUT_DOWN_DELAY = relevantDataForAlgorithms.hostShutDownDelay();
//        HOST_START_UP_POWER = relevantDataForAlgorithms.hostStartUpPower();
//        HOST_SHUT_DOWN_POWER = relevantDataForAlgorithms.hostShutDownPower();
//        VMS = relevantDataForAlgorithms.vms();
//        VM_PES = relevantDataForAlgorithms.vmPes();
//        CLOUDLETS = relevantDataForAlgorithms.cloudlets();
//        CLOUDLET_PES = relevantDataForAlgorithms.cloudletPes();
//        CLOUDLET_LENGTH_MIN = relevantDataForAlgorithms.cloudletLengthMin();
//        CLOUDLET_LENGTH_MAX = relevantDataForAlgorithms.cloudletLengthMax();
//        STATIC_POWER = relevantDataForAlgorithms.staticPower();
//        MAX_POWER = relevantDataForAlgorithms.maxPower();
//        vmList = relevantDataForAlgorithms.vmList();
//        cloudletList = relevantDataForAlgorithms.cloudletList();
//    }


    private void submitNextFCFS(Queue<Cloudlet> queue, DatacenterBroker broker, List<Vm> vmList, Set<Vm> busyVms) {
        if (queue.isEmpty()) return;

        List<Cloudlet> submittedCloudlets = new ArrayList<>();

        while (!queue.isEmpty()) {
            Cloudlet cl;

            Vm freeVm = vmList.stream()
                    .filter(v -> !busyVms.contains(v)) // only use VMs not in use
                    .findFirst()
                    .orElse(null);

            if (freeVm == null)
                break;

            cl = queue.poll();
            cl.setVm(freeVm);
            busyVms.add(freeVm); // mark VM as busy

            System.out.printf("Submitting Cloudlet %d to Vm %d at time %.2f%n",
                    cl.getId(), freeVm.getId(), simulation.clock());

            broker.submitCloudlet(cl);
            submittedCloudlets.add(cl);
        }

        // Add listeners to free up the VMs and submit more
        for (Cloudlet cl : submittedCloudlets) {
            cl.addOnFinishListener(info -> {
                Vm finishedVm = cl.getVm();
                busyVms.remove(finishedVm); // mark VM as free
                submitNextFCFS(queue, broker, vmList, busyVms); // retry
            });
        }
    }
//
//    /**
//     * Prints the following information from VM's utilization stats:
//     * <ul>
//     *   <li>VM's mean CPU utilization relative to the total Host's CPU utilization.
//     *       For instance, if the CPU utilization mean of two equal VMs is 100% of their CPU, the utilization
//     *       of each one corresponds to 50% of the Host's CPU utilization.</li>
//     *   <li>VM's power consumption relative to the total Host's power consumption.</li>
//     * </ul>
//     *
//     * <p>A Host, even if idle, may consume a static amount of power.
//     * Lets say it consumes 20 W in idle state and that for each 1% of CPU use it consumes 1 W more.
//     * For the 2 VMs of the example above, each one using 50% of CPU will consume 50 W.
//     * That is 100 W for the 2 VMs, plus the 20 W that is static.
//     * Therefore we have a total Host power consumption of 120 W.
//     * </p>
//     *
//     * <p>
//     * If we computer the power consumption for a single VM by
//     * calling {@code vm.getHost().getPowerModel().getPower(hostCpuUsage)},
//     * we get the 50 W consumed by the VM, plus the 20 W of static power.
//     * This adds up to 70 W. If the two VMs are equal and using the same amount of CPU,
//     * their power consumption would be the half of the total Host's power consumption.
//     * This would be 60 W, not 70.
//     * </p>
//     *
//     * <p>This way, we have to compute VM power consumption by sharing a supposed Host static power
//     * consumption with each VM, as it's being shown here.
//     * Not all {@link PowerModel} have this static power consumption.
//     * However, the way the VM power consumption
//     * is computed here, that detail is abstracted.
//     * </p>
//     */
//    private void printVmsCpuUtilizationAndPowerConsumption() {
//        vmList.sort(comparingLong(vm -> vm.getHost().getId()));
//        for (Vm vm : vmList) {
//            final var powerModel = vm.getHost().getPowerModel();
//            final double hostStaticPower = powerModel instanceof PowerModelHostSimple powerModelHost ? powerModelHost.getStaticPower() : 0;
//            final double hostStaticPowerByVm = hostStaticPower / vm.getHost().getVmCreatedList().size();
//
//            //VM CPU utilization relative to the host capacity
//            final double vmRelativeCpuUtilization = vm.getCpuUtilizationStats().getMean() / vm.getHost().getVmCreatedList().size();
//            final double vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm; // W
//            final VmResourceStats cpuStats = vm.getCpuUtilizationStats();
//            System.out.printf(
//                    "Vm   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W%n",
//                    vm.getId(), cpuStats.getMean() *100, vmPower);
//        }
//    }
//
//    /**
//     * The Host CPU Utilization History is only computed
//     * if VMs utilization history is enabled by calling
//     * {@code vm.getUtilizationHistory().enable()}.
//     */
//    private void printHostsCpuUtilizationAndPowerConsumption() {
//        System.out.println();
//        for (final Host host : hostList) {
//            printHostCpuUtilizationAndPowerConsumption(host);
//        }
//        System.out.println();
//    }
//
//    private void printHostCpuUtilizationAndPowerConsumption(final Host host) {
//        final HostResourceStats cpuStats = host.getCpuUtilizationStats();
//
//        //The total Host's CPU utilization for the time specified by the map key
//        final double utilizationPercentMean = cpuStats.getMean();
//        final double watts = host.getPowerModel().getPower(utilizationPercentMean);
//        System.out.printf(
//                "Host %2d CPU Usage mean: %6.1f%% | Power Consumption mean: %8.0f W%n",
//                host.getId(), utilizationPercentMean * 100, watts);
//    }
//
//    /**
//     * Creates a {@link Datacenter} and its {@link Host}s.
//     */
//    private Datacenter createDatacenter() {
//
//        for(int i = 0; i < HOSTS; i++) {
//            final var host = createPowerHost(i);
//            hostList.add(host);
//        }
//
//        final var dc = new DatacenterSimple(simulation, hostList);
//        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
//        return dc;
//    }
//
//    private Host createPowerHost(final int id) {
//        final var peList = new ArrayList<Pe>(HOST_PES);
//        //List of Host's CPUs (Processing Elements, PEs)
//        for (int i = 0; i < HOST_PES; i++) {
//            peList.add(new PeSimple(1000));
//        }
//
//        final long ram = 2048; //in Megabytes
//        final long bw = 10000; //in Megabits/s
//        final long storage = 1000000; //in Megabytes
//        final var vmScheduler = new VmSchedulerSpaceShared();
//
//
//        final var host = new HostSimple(ram, bw, storage, peList);
//        host.setStartupDelay(HOST_START_UP_DELAY)
//                .setShutDownDelay(HOST_SHUT_DOWN_DELAY);
//
//        final var powerModel = new PowerModelHostSimple(MAX_POWER, STATIC_POWER);
//        powerModel
//                .setStartupPower(HOST_START_UP_POWER)
//                .setShutDownPower(HOST_SHUT_DOWN_POWER);
//
//        host.setId(id)
//                .setVmScheduler(vmScheduler)
//                .setPowerModel(powerModel);
//        host.enableUtilizationStats();
//
//        return host;
//    }
//
//    /**
//     * Creates a list of VMs.
//     */
//    private List<Vm> createVms() {
//        final var list = new ArrayList<Vm>(VMS);
//        for (int i = 0; i < VMS; i++) {
//            final var vm = new VmSimple(i, 1000, VM_PES);
//            vm.setRam(512).setBw(1000).setSize(10000).enableUtilizationStats();
//            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
//            list.add(vm);
//        }
//
//        return list;
//    }
//
//    /**
//     * Creates a list of Cloudlets.
//     */
//    private List<Cloudlet> createCloudlets() {
//        final var cloudletList = new ArrayList<Cloudlet>(CLOUDLETS);
//        final var utilization = new UtilizationModelDynamic(0.002);
//        for (int i = 0; i < CLOUDLETS; i++) {
//            //Sets half of the cloudlets with the defined length and the other half with the double of it
//            Random r = new Random();
//            int low = CLOUDLET_LENGTH_MIN;
//            final long length = r.nextInt(CLOUDLET_LENGTH_MAX -low) + low;
////            final long length = CLOUDLET_LENGTH_MIN;
//            final var cloudlet =
//                    new CloudletSimple(i, length, CLOUDLET_PES)
//                            .setFileSize(1024)
//                            .setOutputSize(1024)
//                            .setUtilizationModelCpu(new UtilizationModelFull())
//                            .setUtilizationModelRam(utilization)
//                            .setUtilizationModelBw(utilization);
//            cloudletList.add(cloudlet);
//        }
//
//        return cloudletList;
//    }

//    private List<Cloudlet> copyCloudlets(List<Cloudlet> cloudletList) {
//        List<Cloudlet> cloudletClone = new ArrayList<>(cloudletList.size());
//
//        for (Cloudlet cloudlet : cloudletList) {
//            Cloudlet clonedCloudlet = new CloudletSimple(cloudlet.getId(), cloudlet.getLength(), cloudlet.getPesNumber())
//                    .setFileSize(cloudlet.getFileSize())
//                    .setOutputSize(cloudlet.getOutputSize())
//                    .setUtilizationModelCpu(cloudlet.getUtilizationModelCpu())
//                    .setUtilizationModelRam(cloudlet.getUtilizationModelRam())
//                    .setUtilizationModelBw(cloudlet.getUtilizationModelBw());
//
//            cloudletClone.add(clonedCloudlet);
//        }
//
//        return cloudletClone;
//    }
//
//    private List<Vm> copyVMs(List<Vm> vmList) {
//        List<Vm> vmClone = new ArrayList<>(vmList.size());
//
//        for (Vm vm : vmList) {
//            Vm clonedVm = new VmSimple(vm.getMips(), vm.getPesNumber());
//
//            clonedVm.setRam((long) vm.getRam().getCapacity())
//                    .setBw((long) vm.getBw().getCapacity())
//                    .setSize((long) vm.getStorage().getCapacity())
//                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());
//
//            clonedVm.enableUtilizationStats(); // optional
//
//            vmClone.add(clonedVm);
//        }
//
//        return vmClone;
//    }

//    @Override
//    public AlgorithmResult run(RelevantDataForAlgorithms input) {
//        return null;
//    }
//    @Override
//    public String getName() {
//        return "FCFSAlgorithm_bin";
//    }
}
