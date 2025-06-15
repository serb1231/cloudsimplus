package org.APD.Algorithms;

import ch.qos.logback.classic.Level;
import org.APD.AlgorithmResult;
import org.APD.DeadlineCloudlet;
import org.APD.RelevantDataForAlgorithms;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;

public class RoundRobinAlgorithm extends BaseSchedulingAlgorithm {

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new RoundRobinAlgorithm();
    }

    public RoundRobinAlgorithm() {

//        simulation = new CloudSimPlus();
//        hostList = new ArrayList<>(HOSTS);
//        Datacenter datacenter0 = createDatacenter();
//        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
//        broker0 = new DatacenterBrokerSimple(simulation);
//        vmList = createVms();
//        cloudletList = createCloudletsUniformDistribution();
//        broker0.submitVmList(vmList);
//        broker0.submitCloudletList(cloudletList);
//
//        for (int i = 0; i < cloudletList.size(); i++) {
//            DeadlineCloudlet cloudlet = cloudletList.get(i);
//            Vm vm = vmList.get(i % vmList.size()); // Allocate in a sequential manne
//            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
//            // print the cloudlet and vm allocation
////            vm.setShutDownDelay(20);
////            System.out.printf("Binding DeadlineCloudlet %d to Vm %d%n", cloudlet.getId(), vm.getId());
//
//            broker0.bindCloudletToVm(cloudlet, vm);
//        }
//
//        simulation.start();

//        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL+" -------------------------------");
//        final List<DeadlineCloudlet> cloudletFinishedList = broker0.getCloudletFinishedList();
//        final Comparator<DeadlineCloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
//        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));
//
//        new CloudletsTableBuilder(cloudletFinishedList).build();
//        printHostsCpuUtilizationAndPowerConsumption();
//        printVmsCpuUtilizationAndPowerConsumption();
//
//        printSLAViolations(broker0.getCloudletFinishedList());
    }

    @Override
    public AlgorithmResult run(RelevantDataForAlgorithms input) {
        copyGivenDataLocally(input);

        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);
        Datacenter datacenter0 = createDatacenter();
        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        for (int i = 0; i < cloudletList.size(); i++) {
            DeadlineCloudlet cloudlet = cloudletList.get(i);
            Vm vm = vmList.get(i % vmList.size()); // Allocate in a sequential manne
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            // print the cloudlet and vm allocation
//            vm.setShutDownDelay(20);

            broker0.bindCloudletToVm(cloudlet, vm);
        }

        simulation.start();

        return new AlgorithmResult(
                "Round Robin",
                cloudletList,
                hostList,
                vmList,
                broker0.getCloudletFinishedList()
        );
    }
}
