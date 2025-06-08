package org.APD.Algorithms;

import ch.qos.logback.classic.Level;
import org.APD.AlgorithmResult;
import org.APD.RelevantDataForAlgorithms;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.APD.DeadlineCloudlet;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;

import java.util.*;

import static java.util.Comparator.comparingLong;

public class ACOAlgorithm extends BaseSchedulingAlgorithm {

    private final int numAnts = 200; // Number of ants
    private final double evaporationRate = 0.2; // Pheromone evaporation rate
    private final double MIN_PHEROMONE_LEVEL = 0.8;
    private int TOTAL_CLOUDLETS = CLOUDLETS * TOTAL_FRAMES;

    private final int iterations = 100; // Number of iterations for the algorithm
    private final double Q = 1000.0; // Pheromone deposit constant

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new ACOAlgorithm();
    }

    public ACOAlgorithm() {

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

        for (int iter = 0; iter < iterations; iter++) {
            List<Ant> ants;
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
                double fitness = evaluateAnt(broker.getCloudletFinishedList(), broker);
                ant.setFitness(fitness);
                ant.setNumberViolations(numberOfViolations(cloudletClone, broker));
                ant.setCloudletsFinished(broker.getCloudletFinishedList());
            }
            // evaporate pheromones
            evaporatePheromones(pheromoneMatrix);

            Ant bestAnt = ants.stream()
                    .max(Comparator.comparingDouble(Ant::getFitness))
                    .orElseThrow();

            // updat pheromones based on the best ant's allocation
            updatePheromones(bestAnt, pheromoneMatrix);
        }


        // submit all the Vm's to the broker
        broker0.submitVmList(vmList);
        // send a normal new ant to create the allocation
        Ant ant = new Ant();
        for (DeadlineCloudlet cl : cloudletList) {
            Vm selectedVm = selectVmBasedOnPheromone(cl, vmList, pheromoneMatrix);
            cl.setVm(selectedVm);
            ant.assign(cl, selectedVm);
            broker0.submitCloudlet(cl);
        }

        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.start();

//        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL + " -------------------------------");
//        final List<DeadlineCloudlet> cloudletFinishedList = broker0.getCloudletFinishedList();
//        final Comparator<DeadlineCloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
//        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));

//        new CloudletsTableBuilder(cloudletFinishedList).build();
//        printHostsCpuUtilizationAndPowerConsumption();
//        printVmsCpuUtilizationAndPowerConsumption();

//        double makespan = cloudletFinishedList.stream()
//                .mapToDouble(Cloudlet::getFinishTime)
//                .max()
//                .orElse(0.0);
//
//        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespan);
//
//        printSLAViolations(broker0.getCloudletFinishedList());

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
                // Ensure pheromone levels do not go below a minimum threshold
                if (pheromoneMatrix[i][j] < MIN_PHEROMONE_LEVEL) {
                    pheromoneMatrix[i][j] = MIN_PHEROMONE_LEVEL; // Minimum pheromone level
                }
            }
        }
    }

    // higher is better
    private double evaluateAnt(List<Cloudlet> finished, DatacenterBroker broker) {

        int violations = 0;
        double makespan = 0.0;

        for (Cloudlet cl : finished) {
            makespan = Math.max(makespan, cl.getFinishTime());

            if (cl instanceof DeadlineCloudlet dc &&
                    dc.getFinishTime() > dc.getDeadline()) {
                violations++;
            }
        }

        return (double) 1 / (1 + violations) + 0.01 * (((double) (CLOUDLET_LENGTH_MAX * (TOTAL_CLOUDLETS)) / 1000) / makespan); // Fitness function
    }

    private int numberOfViolations(List<DeadlineCloudlet> cloudlets, DatacenterBroker broker) {
        int violations = 0;

        for (Cloudlet cl : broker.getCloudletFinishedList()) {
            if (cl instanceof DeadlineCloudlet dc) {
                double finish = dc.getFinishTime();
                double deadline = dc.getDeadline();
                boolean metDeadline = finish <= deadline;

                if (!metDeadline) violations++;
            }
        }

        return violations; // Lower fitness for fewer violations
    }

//    private double evaluateAnt(List<DeadlineCloudlet> cloudlets, DatacenterBroker broker) {
//        int violations = 0;
//        int total = 0;
//
//        for (Cloudlet cl : broker.getCloudletFinishedList()) {
//            if (cl instanceof DeadlineCloudlet dc) {
//                total++;
//                double finish = dc.getFinishTime();
//                double deadline = dc.getDeadline();
//                boolean metDeadline = finish <= deadline;
//
//                if (!metDeadline) violations++;
//            }
//        }
//
//        return total - violations; // Higher fitness for fewer violations
//    }

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
//        vmList = createVms();
//        cloudletList = createCloudlets();

        // set all the VMs to use the CloudletSchedulerSpaceShared scheduler
        for (Vm vm : vmList) {
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
        }

        double[][] pheromoneMatrix = initializePheromoneMatrix();

        for (int iter = 0; iter < iterations; iter++) {
            List<Ant> ants;
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
                double fitness = evaluateAnt(broker.getCloudletFinishedList(), broker);
                ant.setFitness(fitness);
                ant.setNumberViolations(numberOfViolations(cloudletClone, broker));
                ant.setCloudletsFinished(broker.getCloudletFinishedList());
            }
            // evaporate pheromones
            evaporatePheromones(pheromoneMatrix);

            Ant bestAnt = ants.stream()
                    .max(Comparator.comparingDouble(Ant::getFitness))
                    .orElseThrow();


            // updat pheromones based on the best ant's allocation
            updatePheromones(bestAnt, pheromoneMatrix);
        }


        // submit all the Vm's to the broker
        broker0.submitVmList(vmList);
        // send a normal new ant to create the allocation
        Ant ant = new Ant();
        for (DeadlineCloudlet cl : cloudletList) {
            Vm selectedVm = selectVmBasedOnPheromone(cl, vmList, pheromoneMatrix);
            cl.setVm(selectedVm);
            ant.assign(cl, selectedVm);
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
    private void updatePheromones(Ant bestAnt, double[][] pheromoneMatrix) {

        for (Map.Entry<DeadlineCloudlet, Vm> entry : bestAnt.getAllocation().entrySet()) {
            DeadlineCloudlet cl = entry.getKey();
            Vm vm = entry.getValue();
            int cloudletId = (int) cl.getId();
            int vmId = (int) vm.getId();

            // Deposit pheromone: inversely proportional to fitness (e.g., makespan or SLA violations)
            double deltaPheromone = 3 * ((TOTAL_CLOUDLETS) - bestAnt.getNumberViolations());
            pheromoneMatrix[cloudletId][vmId] += deltaPheromone;
            pheromoneMatrix[cloudletId][vmId] = Math.min(pheromoneMatrix[cloudletId][vmId], 10); // Cap pheromone levels
        }
    }

}


class Ant {
    private final Map<DeadlineCloudlet, Vm> allocation = new HashMap<>();
    private double fitness;
    private List<DeadlineCloudlet> cloudletsFinished;
    private int numberViolations;

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

    public List<DeadlineCloudlet> getCloudletsFinished() {
        return cloudletsFinished;
    }

    public void setCloudletsFinished(List<DeadlineCloudlet> cloudletsFinished) {
        this.cloudletsFinished = cloudletsFinished;
    }

    public int getNumberViolations() {
        return numberViolations;
    }

    public void setNumberViolations(int numberViolations) {
        this.numberViolations = numberViolations;
    }

}
