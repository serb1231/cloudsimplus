//package org.cloudbus.cloudsim.examples.src;
//
//import org.cloudbus.cloudsim.examples.src.PowerModels.*;
//import org.cloudbus.cloudsim.examples.src.AlgorithmsRavikishaVersion.*;
//import org.cloudbus.cloudsim.*;
//import org.cloudbus.cloudsim.core.CloudSim;
//import org.cloudbus.cloudsim.power.PowerHost;
//import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
//import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
//import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
//
//import java.util.*;
//
//public class TryoutAlgorithmsBasic {
//    public static void main(String[] args) {
//        try {
//            // Initialize the CloudSim package
//            int numUser = 1; // number of cloud users
//            Calendar calendar = Calendar.getInstance();
//            boolean traceFlag = false; // mean trace events
//
//            // Initialize CloudSim
//            CloudSim.init(numUser, calendar, traceFlag);
//
//            int numberOfPEsPerHost = 1;
//            int numberOfHosts = 1;
//            int numberOfVMs = 1;
//            int numberOfCloudlets = 1;
//            int mipsVM = 1000; // MIPS of the VM
//            int mipsPE = 1000; // MIPS of the PE
//            int predefinedMeanSystemRespondTime = 1000; // in ms
//
//            // Create Datacenter
//            Datacenter datacenter0 = createDatacenter("Datacenter_0", numberOfPEsPerHost, numberOfHosts, mipsPE);
//
//            // Create DatacenterBroker
//            DatacenterBroker broker = createBroker();
//            assert broker != null;
//            int brokerId = broker.getId();
//
//            // Create VMs and Cloudlets
//            List<PStateAwareVm> vmList = createVMs(brokerId, numberOfVMs, mipsVM, new PowerModelPstateProcessor_2GHz_Via_C7_M(0));
//            // Print the created VMs
//            System.out.println("Created VMs:");
//            for (PStateAwareVm vm : vmList) {
//                System.out.println("VM ID: " + vm.getId() + ", MIPS: " + vm.getMips());
//            }
//            List<Cloudlet> cloudletList = createCloudlets(brokerId, numberOfCloudlets);
//
//            // Submit VM list to the broker
//            broker.submitGuestList(vmList);
//            // submit cloudlet list to the broker
//            broker.submitCloudletList(cloudletList);
//
//            Scanner sc = new Scanner(System.in);
//            System.out.println("Enter the algorithm to be used: (roundrobin, fcfs, ant, genetic, sjf): ");
//            String algorithm = sc.next();
//            switch (algorithm.toLowerCase()) {
//                // case "genetic":
//                case "roundrobin":
//                    RoundRobinAlgorithm rrAlgo = new RoundRobinAlgorithm();
//                    rrAlgo.runAlgorithm(broker, vmList, cloudletList);
//                    break;
////                case "fcfs":
////                    FCFSAlgorithm fcfsAlgo = new FCFSAlgorithm();
////                    fcfsAlgo.runAlgorithm(broker, vmList, cloudletList);
////                    break;
//                case "ant":
//                    ACOAlgorithm antAlgo = new ACOAlgorithm();
//                    antAlgo.runAlgorithm(broker, vmList, cloudletList);
//                    break;
//                case "genetic":
//                    GeneticAlgorithm geneticAlgo = new GeneticAlgorithm();
//                    geneticAlgo.runAlgorithm(broker, vmList, cloudletList);
//                    break;
//                case "sjf":
//                    SJFAlgorithm sjfAlgo = new SJFAlgorithm();
//                    sjfAlgo.runAlgorithm(broker, vmList, cloudletList);
//                    break;
//                default:
//                    Log.print("Invalid algorithm selection\n");
//                    return;
//            }
//
//            // Start the simulation
//            CloudSim.startSimulation();
//
//            // Stop the simulation
//            CloudSim.stopSimulation();
//
//            // Print results
//            List<Cloudlet> newList = broker.getCloudletReceivedList();
//            printCloudletList(newList);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//
//    // Create a simple Datacenter
//    private static Datacenter createDatacenter(String name, int numberOfPesPerHost, int numberOfHosts, int mipsPE) {
//
//        // List where to store our host machines
//        List<PStateAwareHost> hostList = new ArrayList<>();
//
//        // We don't care about the RAM, storage, BW, therefore, they should not be
//        // bottlenecks and should set them to the maximum values
//        int ram = 1_000_000; // 1 TB RAM, as we don't care about it
//        long storage = Long.MAX_VALUE; // maximum host storage
//        int bw = 1_000_000;
//
//        for (int h = 0; h < numberOfHosts; h++) {
//
//            List<Pe> hostPeList = new ArrayList<>();
//            for (int p = 0; p < numberOfPesPerHost; p++) {
//                hostPeList.add(new Pe(p, new PeProvisionerSimple(mipsPE))); // new PE each time
//            }
//
//            hostList.add(new PStateAwareHost(
//                    h,
//                    new RamProvisionerSimple(ram),
//                    new BwProvisionerSimple(bw),
//                    storage,
//                    hostPeList,                              // unique list
//                    new VmSchedulerTimeShared(hostPeList),   // scheduler gets the same unique list
//                    new PowerModelPstateProcessor_2GHz_Via_C7_M(0)  // no arg if you refactored ctor
//            ));
//        }
//
//        // print the hosts, together with their Ps's and their Id's,
//        // to see how many PEs are created
//        System.out.println("Created Hosts:");
//        for (PowerHost host : hostList) {
//            System.out.println("Host ID: " + host.getId() + ", Number of PEs: " + host.getNumberOfPes());
//            for (Pe pe : host.getPeList()) {
//                System.out.println("  PE ID: " + pe.getId() + ", MIPS: " + pe.getMips());
//            }
//        }
//
//
//
//        // Datacenter properties
//        String arch = "x86"; // system architecture
//        String os = "Linux"; // operating system
//        String vmm = "Xen";
//        double time_zone = 10.0; // time zone this resource located
//        double cost = 3.0; // the cost of using processing in this resource
//        double costPerMem = 0.05; // the cost of using memory in this resource
//        double costPerStorage = 0.001; // the cost of using storage in this
//        // resource
//        double costPerBw = 0.0; // the cost of using bw in this resource
//        LinkedList<Storage> storageList = new LinkedList<>(); // we are not adding SAN
//
//        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
//                arch, os, vmm, hostList, time_zone, cost, costPerMem,
//                costPerStorage, costPerBw);
//
//        // Create the Datacenter object
//        Datacenter datacenter = null;
//        try {
//            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return datacenter;
//    }
//
//    // Create Broker
//    private static  DatacenterBroker createBroker() {
//        try {
//            return new DatacenterBroker("Broker");
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    // Create VM
//    private static List<PStateAwareVm> createVMs(int brokerId, int count, int mips, PowerModelPStateProcessor powerModel) {
//        List<PStateAwareVm> list = new ArrayList<>();
//
//        // VM description
//        // These are the resources that a VM needs to be allocated on a host.
//        // We can set these values as being very small, and demand only 1 Pes to have
//        // the VM hosted on the Pe and use the Ps's mips
//        int ram = 512; // VM memory (MB)
//        long bw = 1000;
//        long size = 10000; // VM image size (GB)
//        String vmm = "Xen"; // VMM name
//
//        for (int i = 0; i < count; i++) {
//            list.add(new PStateAwareVm(i, brokerId, mips, 1, ram, bw, size, vmm, new CloudletSchedulerSpaceShared(), powerModel));
//        }
//
//        return list;
//    }
//
//    // Create Cloudlets
//    private static List<Cloudlet> createCloudlets(int brokerId, int count) {
//        List<Cloudlet> list = new ArrayList<>();
//        // Length is Nr of MI (million of instructions needed)
//        long length = 40000;
//        // pes the task needs in the VM it is assigned(in our case every time 1)
//        int pesNumber = 1;
//        // Depends on the bandwidts how fast the file is sent, so we put it to 0
//        long fileSize = 0;
//        // Depends on the bandwidth, so we put it to 0
//        long outputSize = 0;
//        UtilizationModel utilizationModel = new UtilizationModelFull();
//
//        for (int i = 0; i < count; i++) {
//            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel,
//                    utilizationModel, utilizationModel);
//            cloudlet.setUserId(brokerId);
//            cloudlet.setGuestId(0);
//            list.add(cloudlet);
//        }
//
//        return list;
//    }
//
//    // Print the results of Cloudlets execution
//    private static void printCloudletList(List<Cloudlet> list) {
//        System.out.println("========== OUTPUT ==========");
//        // System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center
//        // ID" + indent + "VM ID" + indent
//        // + "Time" + indent + "Start Time" + indent + "Finish Time");
//        System.out.printf("%-15s%-10s%-15s%-10s%-10s%-15s%-15s\n", "CloudletID", "STATUS", "DataCenterID", "VM ID",
//                "Time", "StartTime", "FinishTime");
//
//        for (Cloudlet cloudlet : list) {
//            // System.out.print(indent + cloudlet.getCloudletId() + indent + indent);
//
//            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
//                // System.out.println("SUCCESS" + indent + indent + cloudlet.getResourceId() +
//                // indent + indent
//                // + cloudlet.getVmId() + indent + indent + String.format("%.5f",
//                // cloudlet.getActualCPUTime()) + indent + indent
//                // + cloudlet.getExecStartTime() + indent + indent + cloudlet.getFinishTime());
//                System.out.printf("%-15d%-10s%-15d%-10d%-10.2f%-15.2f%-15.2f\n", cloudlet.getCloudletId(), "SUCCESS",
//                        cloudlet.getResourceId(), cloudlet.getVmId(), cloudlet.getActualCPUTime(),
//                        cloudlet.getExecStartTime(), cloudlet.getFinishTime());
//            }
//        }
//    }
//}