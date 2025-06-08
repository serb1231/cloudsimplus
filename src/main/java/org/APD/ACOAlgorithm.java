package org.APD;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.APD.DeadlineCloudlet;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.*;

import static java.util.Comparator.comparingLong;
public class ACOAlgorithm extends BaseSchedulingAlgorithm {

    private final int numAnts = 10; // Number of ants
    private final double evaporationRate = 0.5; // Pheromone evaporation rate
    private final int iterations = 10; // Number of iterations for the algorithm

    public static void main(String[] args) {
        new org.APD.ACOAlgorithm();
    }

    ACOAlgorithm() {

        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);
        Datacenter datacenter0 = createDatacenter(simulation, hostList);
        broker0 = new DatacenterBrokerSimple(simulation);
        vmList = createVms();
        cloudletList = createCloudlets();

        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
        for (Vm vm : vmList) {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
        }

        double[][] pheromoneMatrix = initializePheromoneMatrix();
        List<Ant> ants = new ArrayList<>();

        for (int iter = 0; iter < iterations; iter++) {
            ants = createAnts();
            for (Ant ant : ants) {
                CloudSimPlus sim = new CloudSimPlus();
                List<Host> hostlst = new ArrayList<>(HOSTS);

                Datacenter dc = createDatacenter(sim, hostlst);
                DatacenterBroker broker = new DatacenterBrokerSimple(sim);

                List<Vm> vmClone = copyVMs(vmList);
                List<DeadlineCloudlet> cloudletClone = copyCloudlets(cloudletList);

                broker.submitVmList(vmClone);

                for (DeadlineCloudlet cl : cloudletClone) {
                    Vm selectedVm = selectVmBasedOnPheromone(cl, vmClone, pheromoneMatrix);
                    cl.setVm(selectedVm);
                    ant.assign(cl, selectedVm);
                    broker.submitCloudlet(cl);
                }

                sim.start();

                // Evaluate performance
                double fitness = evaluateAnt(broker.getCloudletFinishedList());
                ant.setFitness(fitness);
            }
            evaporatePheromones(pheromoneMatrix);
        }

        Ant bestAnt = ants.stream()
                .max(Comparator.comparingDouble(Ant::getFitness))
                .orElseThrow();


        // submit all the Vm's to the broker
        broker0.submitVmList(vmList);
        for (Map.Entry<DeadlineCloudlet, Vm> entry : bestAnt.getAllocation().entrySet()) {
            DeadlineCloudlet cl = entry.getKey();
            Vm vm = entry.getValue();
            cl.setVm(vm);
            broker0.submitCloudlet(cl);
        }

        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL + " -------------------------------");
        final List<DeadlineCloudlet> cloudletFinishedList = broker0.getCloudletFinishedList();
        final Comparator<DeadlineCloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));

        new CloudletsTableBuilder(cloudletFinishedList).build();
        printHostsCpuUtilizationAndPowerConsumption();
        printVmsCpuUtilizationAndPowerConsumption();

        double makespan = cloudletFinishedList.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);

        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespan);

    }

    private Datacenter createDatacenter(CloudSimPlus simulation, List<Host> hostList) {
        for (int i = 0; i < HOSTS; i++) {
            final var host = createPowerHost(i);
            hostList.add(host);
        }

        final var dc = new DatacenterSimple(simulation, hostList);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    private List<DeadlineCloudlet> copyCloudlets(List<DeadlineCloudlet> cloudletList) {
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

    private List<Vm> copyVMs(List<Vm> vmList) {
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

    private double[][] initializePheromoneMatrix() {
        double[][] pheromoneMatrix = new double[cloudletList.size()][vmList.size()];
        for (int i = 0; i < cloudletList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                pheromoneMatrix[i][j] = 1.0; // Initialize pheromones uniformly
            }
        }
        return pheromoneMatrix;
    }

    private List<Ant> createAnts() {
        List<Ant> ants = new ArrayList<>();

        for (int i = 0; i < numAnts; i++) {
            Ant ant = new Ant();
            ants.add(ant);
        }

        return ants;
    }

    private void evaporatePheromones(double[][] pheromoneMatrix) {
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            for (int j = 0; j < pheromoneMatrix[i].length; j++) {
                pheromoneMatrix[i][j] *= (1 - evaporationRate); // Evaporate pheromone
            }
        }
    }

    private double evaluateAnt(List<DeadlineCloudlet> cloudlets) {
        double makespan = cloudlets.stream()
                .mapToDouble(DeadlineCloudlet::getFinishTime)
                .max()
                .orElse(0);

        return 1.0 / makespan;  // Minimize makespan
    }

    public Vm selectVmBasedOnPheromone(DeadlineCloudlet cloudlet, List<Vm> vmList, double[][] pheromoneMatrix) {
        int cloudletId = (int) cloudlet.getId();

        // Step 1: Extract pheromone levels for this cloudlet
        double[] pheromones = new double[vmList.size()];
        double total = 0;

        for (int j = 0; j < vmList.size(); j++) {
            pheromones[j] = pheromoneMatrix[cloudletId][j];
            total += pheromones[j];
        }

        // Step 2: Normalize to probabilities
        double[] probabilities = new double[vmList.size()];
        for (int j = 0; j < vmList.size(); j++) {
            probabilities[j] = pheromones[j] / total;
        }

        // Step 3: Roulette wheel selection
        double rand = Math.random();
        double cumulative = 0.0;
        for (int j = 0; j < probabilities.length; j++) {
            cumulative += probabilities[j];
            if (rand <= cumulative) {
                return vmList.get(j);
            }
        }

        // Edge case: fallback
        return vmList.get(vmList.size() - 1);
    }

    @Override
    public AlgorithmResult run(RelevantDataForAlgorithms input) {
        copyGivenDataLocally(input);

        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);
        Datacenter datacenter0 = createDatacenter(simulation, hostList);
        broker0 = new DatacenterBrokerSimple(simulation);
        vmList = createVms();
        cloudletList = createCloudlets();

        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
        for (Vm vm : vmList) {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
        }

        double[][] pheromoneMatrix = initializePheromoneMatrix();
        List<Ant> ants = new ArrayList<>();

        for (int iter = 0; iter < iterations; iter++) {
            ants = createAnts();
            for (Ant ant : ants) {
                CloudSimPlus sim = new CloudSimPlus();
                List<Host> hostlst = new ArrayList<>(HOSTS);

                Datacenter dc = createDatacenter(sim, hostlst);
                DatacenterBroker broker = new DatacenterBrokerSimple(sim);

                List<Vm> vmClone = copyVMs(vmList);
                List<DeadlineCloudlet> cloudletClone = copyCloudlets(cloudletList);

                broker.submitVmList(vmClone);

                for (DeadlineCloudlet cl : cloudletClone) {
                    Vm selectedVm = selectVmBasedOnPheromone(cl, vmClone, pheromoneMatrix);
                    cl.setVm(selectedVm);
                    ant.assign(cl, selectedVm);
                    broker.submitCloudlet(cl);
                }

                sim.start();

                // Evaluate performance
                double fitness = evaluateAnt(broker.getCloudletFinishedList());
                ant.setFitness(fitness);
            }
            evaporatePheromones(pheromoneMatrix);
        }

        Ant bestAnt = ants.stream()
                .max(Comparator.comparingDouble(Ant::getFitness))
                .orElseThrow();


        // submit all the Vm's to the broker
        broker0.submitVmList(vmList);
        for (Map.Entry<DeadlineCloudlet, Vm> entry : bestAnt.getAllocation().entrySet()) {
            DeadlineCloudlet cl = entry.getKey();
            Vm vm = entry.getValue();
            cl.setVm(vm);
            broker0.submitCloudlet(cl);
        }

        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

        return new AlgorithmResult(getName(),
                cloudletList,
                hostList,
                vmList,
                broker0.getCloudletFinishedList());
    }
}


class Ant {
    private final Map<DeadlineCloudlet, Vm> allocation = new HashMap<>();
    private double fitness;

    public void assign(DeadlineCloudlet cl, Vm vm) {
        allocation.put(cl, vm);
    }

    public Map<DeadlineCloudlet, Vm> getAllocation() {
        return allocation;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fit) {
        this.fitness = fit;
    }
}
