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

public class CompareAlgorithms {
    /**
     * Defines, between other things, the time intervals
     * to keep Hosts CPU utilization history records.
     */
    private static final int SCHEDULING_INTERVAL = 1;
    private static final int HOSTS = 3;
    private static final int HOST_PES = 1;

    /** Indicates the time (in seconds) the Host takes to start up. */
    private static final double HOST_START_UP_DELAY = 0;

    /** Indicates the time (in seconds) the Host takes to shut down. */
    private static final double HOST_SHUT_DOWN_DELAY = 3;

    /** Indicates Host power consumption (in Watts) during startup. */
    private static final double HOST_START_UP_POWER = 5;

    /** Indicates Host power consumption (in Watts) during shutdown. */
    private static final double HOST_SHUT_DOWN_POWER = 3;

    private static final int VMS = 3;
    private static final int VM_PES = 1;

    private static final int CLOUDLETS = 9;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH_MIN = 100000;
    private static final int CLOUDLET_LENGTH_MAX = 500000;

    /**
     * Defines the power a Host uses, even if it's idle (in Watts).
     */
    private static final double STATIC_POWER = 35;

    /**
     * The max power a Host uses (in Watts).
     */
    private static final int MAX_POWER = 50;

//    private final CloudSimPlus simulation;
//    private final DatacenterBroker broker0;
    private static List<Vm> vmList;
    private List<Host> hostList;
    private static List<Cloudlet> cloudletList;

    public static void main(String[] args) {
        vmList = createVms();
        cloudletList = createCloudlets();

        List<Vm> vmListCopy = copyVMs(vmList);
        List<Cloudlet> cloudletListCopy = copyCloudlets(cloudletList);

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
        fcfs.run(data);


    }


    private static RelevantDataForAlgorithms cloneData(RelevantDataForAlgorithms data) {
        return new RelevantDataForAlgorithms(
                data.schedulingInterval(),
                data.hosts(),
                data.hostPes(),
                data.hostStartUpDelay(),
                data.hostShutDownDelay(),
                data.hostStartUpPower(),
                data.hostShutDownPower(),
                data.vms(),
                data.vmPes(),
                data.cloudlets(),
                data.cloudletPes(),
                data.cloudletLengthMin(),
                data.cloudletLengthMax(),
                data.staticPower(),
                data.maxPower(),
                copyVMs(data.vmList()),
                copyCloudlets(data.cloudletList())
        );
    }

    private static List<Cloudlet> copyCloudlets(List<Cloudlet> cloudletList) {
        List<Cloudlet> cloudletClone = new ArrayList<>(cloudletList.size());

        for (Cloudlet cloudlet : cloudletList) {
            Cloudlet clonedCloudlet = new CloudletSimple(cloudlet.getId(), cloudlet.getLength(), cloudlet.getPesNumber())
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

            clonedVm.setRam((long) vm.getRam().getCapacity())
                    .setBw((long) vm.getBw().getCapacity())
                    .setSize((long) vm.getStorage().getCapacity())
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

    /**
     * Creates a list of Cloudlets.
     */
    private static List<Cloudlet> createCloudlets() {
        final var cloudletList = new ArrayList<Cloudlet>(CLOUDLETS);
        final var utilization = new UtilizationModelDynamic(0.002);
        for (int i = 0; i < CLOUDLETS; i++) {
            //Sets half of the cloudlets with the defined length and the other half with the double of it
            Random r = new Random();
            int low = CLOUDLET_LENGTH_MIN;
            final long length = r.nextInt(CLOUDLET_LENGTH_MAX -low) + low;
//            final long length = CLOUDLET_LENGTH_MIN;
            final var cloudlet =
                    new CloudletSimple(i, length, CLOUDLET_PES)
                            .setFileSize(1024)
                            .setOutputSize(1024)
                            .setUtilizationModelCpu(new UtilizationModelFull())
                            .setUtilizationModelRam(utilization)
                            .setUtilizationModelBw(utilization);
            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }
}
