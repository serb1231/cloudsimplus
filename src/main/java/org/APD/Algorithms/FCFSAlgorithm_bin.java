package org.APD.Algorithms;

import ch.qos.logback.classic.Level;
import org.APD.AlgorithmResult;
import org.APD.RelevantDataForAlgorithms;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;

import java.util.*;

public class FCFSAlgorithm_bin  extends BaseSchedulingAlgorithm {

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new FCFSAlgorithm_bin();
    }

    // Run the algorothm using the default data from the BaseSchedulingAlgorithm class
    public FCFSAlgorithm_bin() {

//        simulation = new CloudSimPlus();
//        hostList = new ArrayList<>(HOSTS);
//        Datacenter datacenter0 = createDatacenter();
//        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
//        broker0 = new DatacenterBrokerSimple(simulation);
//        vmList = createVms();
//        cloudletList = createCloudletsUniformDistribution();
//
//        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
//        for (Vm vm : vmList) {
//            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
//            // print the VM allocation
////            System.out.printf("Vm %d allocated to Broker %d%n", vm.getId(), broker0.getId());
//        }
//
//        broker0.submitVmList(vmList);
//        Queue<Cloudlet> queue = new ArrayDeque<>(cloudletList);
//        Set<Vm> busyVms = new HashSet<>();
//        simulation.addOnSimulationStartListener(evt -> {
////            System.out.printf("Simulation clock: %.2f\n", simulation.clock());
//            submitNextFCFS(queue, broker0, vmList, busyVms);
//        });
//
//        simulation.start();
//
//        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL+" -------------------------------");
//        final var cloudletFinishedList = broker0.getCloudletFinishedList();
//        final Comparator<Cloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
//        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));
//
//        new CloudletsTableBuilder(cloudletFinishedList).build();
//        printHostsCpuUtilizationAndPowerConsumption();
//        printVmsCpuUtilizationAndPowerConsumption();
//
//        printSLAViolations(broker0.getCloudletFinishedList());

    }

    @Override
    public AlgorithmResult run(RelevantDataForAlgorithms relevantDataForAlgorithms) {

        copyGivenDataLocally(relevantDataForAlgorithms);
        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);

        Datacenter datacenter0 = createDatacenter();
        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        broker0 = new DatacenterBrokerSimple(simulation);

        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
        for (Vm vm : vmList) {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            // print the VM allocation
//            System.out.printf("Vm %d allocated to Broker %d%n", vm.getId(), broker0.getId());
        }

        broker0.submitVmList(vmList);
        Queue<Cloudlet> queue = new ArrayDeque<>(cloudletList);
        Set<Vm> busyVms = new HashSet<>();
        simulation.addOnSimulationStartListener(evt -> {
//            System.out.printf("Simulation clock: %.2f\n", simulation.clock());
            submitNextFCFS(queue, broker0, vmList, busyVms);
        });

        simulation.start();

        return new AlgorithmResult(getName(),
                cloudletList,
                hostList,
                vmList,
                broker0.getCloudletFinishedList());
    }

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
            double submissionDelay = cl.getSubmissionDelay();
            // if the current time is grater than the submission delay, set it to 0
            if (submissionDelay > simulation.clock()) {
                cl.setSubmissionDelay(submissionDelay - simulation.clock());
            } else {
                cl.setSubmissionDelay(0);
            }
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

}
