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

    protected final int numAnts = 10; // Number of ants
    protected final double evaporationRate = 0.2; // Pheromone evaporation rate
    protected final double MIN_PHEROMONE_LEVEL = 0.8;
    protected int TOTAL_CLOUDLETS = CLOUDLETS_PER_FRAME * TOTAL_FRAMES;
    protected final double MAX_PHEROMONE_LEVEL = 10.0; // Maximum pheromone level

    protected final int iterations = 50; // Number of iterations for the algorithm

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new ACOAlgorithm();
    }

    public ACOAlgorithm() {

        vmList = createVms();
        cloudletList = createCloudlets();
        algorithmACO();

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

//        printSLAViolations(broker0.getCloudletFinishedList());

    }

    @Override
    public AlgorithmResult run(RelevantDataForAlgorithms input) {
        copyGivenDataLocally(input);

        algorithmACO();

        return new AlgorithmResult(getName(),
                cloudletList,
                hostList,
                vmList,
                broker0.getCloudletFinishedList());
    }

    private void  algorithmACO() {

        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);
        Datacenter datacenter0 = createDatacenter(simulation, hostList);
        broker0 = new DatacenterBrokerSimple(simulation);

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
                    Vm selectedVm = selectVmBasedOnPheromone(cl, vmClone, pheromoneMatrix, cloudletClone);
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
            Vm selectedVm = selectVmBasedOnPheromone(cl, vmList, pheromoneMatrix, cloudletList);
            cl.setVm(selectedVm);
            ant.assign(cl, selectedVm);
            broker0.submitCloudlet(cl);
        }

        broker0.submitVmList(vmList);

        simulation.start();
    }

    public Vm selectVmBasedOnPheromone(DeadlineCloudlet cloudlet, List<Vm> vmList, double[][] pheromoneMatrix, List<DeadlineCloudlet> cloudletList) {

        // create a pheromone matrix copy
        double[][] pheromoneMatrixCopy = new double[pheromoneMatrix.length][pheromoneMatrix[0].length];
        for (int i = 0; i < pheromoneMatrix.length; i++) {
            System.arraycopy(pheromoneMatrix[i], 0, pheromoneMatrixCopy[i], 0, pheromoneMatrix[i].length);
        }

        int cloudletId = (int) cloudlet.getId();

        // last finish times for each VM
        double[] lastFinishTimes = new double[vmList.size()];
        // for each vm, get the number of cloudlets it has and when the last one finishes
        for (Vm vm : vmList) {
            // get all the cloudlets assigned to this VM
            List<DeadlineCloudlet> assignedCloudlets = new ArrayList<>();
            for (DeadlineCloudlet cl : cloudletList) {
                cl.getVm();
                if (cl.getVm().getId() == vm.getId() && !cl.isFinished()) {
                    assignedCloudlets.add(cl);
                }
            }
            // make the sum of the execution times for all the cloudlets assigned to this VM
            double lastFinishTime = getLastFinishTime(vm, assignedCloudlets);

            lastFinishTimes[(int) vm.getId()] = lastFinishTime;
            // print the last finish time for each VM
//            System.out.printf("VM %d last finish time: %.2f seconds%n", vm.getId(), lastFinishTime);
        }
        // Normalize probabilities again based on last finish times
        double lastFinishTotal = 0.0;
        for (double lastFinishTime : lastFinishTimes) {
            lastFinishTotal += lastFinishTime;
        }
        for (Vm vm : vmList) {
            int vmId = (int) vm.getId();
            if (lastFinishTotal > 0) {
                // if the last finish time is greater than the cloudlet's deadline, set the probability to 0.1
                if (lastFinishTimes[vmId] + cloudlet.getLength()/vm.getMips() > cloudlet.getDeadline()) {
                    pheromoneMatrixCopy[cloudletId][vmId] = 0.1;
                } else {
                    pheromoneMatrixCopy[cloudletId][vmId] = MAX_PHEROMONE_LEVEL; // If it wouldn't break the deadline, assign it
                    pheromoneMatrixCopy[cloudletId][vmId] += MAX_PHEROMONE_LEVEL * 3 * (lastFinishTimes[vmId] / lastFinishTotal);
                }
            }
        }

        // Step 1: Extract pheromone levels for this cloudlet
        double[] pheromones = new double[vmList.size()];
        double total = 0;

        for (int j = 0; j < vmList.size(); j++) {
            pheromones[j] = pheromoneMatrixCopy[cloudletId][j];
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

    private static double getLastFinishTime(Vm vm, List<DeadlineCloudlet> assignedCloudlets) {
        double lastFinishTime = 0.0;
        for (DeadlineCloudlet cl : assignedCloudlets) {
            if (lastFinishTime > cl.getSubmissionDelay())
                lastFinishTime += cl.getLength() / vm.getMips();
            else
                lastFinishTime = cl.getSubmissionDelay()+ cl.getLength() / vm.getMips();
        }
        return lastFinishTime;
    }

    protected double[][] initializePheromoneMatrix() {
        double[][] pheromoneMatrix = new double[cloudletList.size()][vmList.size()];
        for (int i = 0; i < cloudletList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                pheromoneMatrix[i][j] = 1.0; // Initialize pheromones uniformly
            }
        }
        return pheromoneMatrix;
    }

    protected void evaporatePheromones(double[][] pheromoneMatrix) {
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

    protected void updatePheromones(Ant bestAnt, double[][] pheromoneMatrix) {

        for (Map.Entry<DeadlineCloudlet, Vm> entry : bestAnt.getAllocation().entrySet()) {
            DeadlineCloudlet cl = entry.getKey();
            Vm vm = entry.getValue();
            int cloudletId = (int) cl.getId();
            int vmId = (int) vm.getId();

            // Deposit pheromone: inversely proportional to fitness (e.g., makespan or SLA violations)
            double deltaPheromone = 0.1 * ((TOTAL_CLOUDLETS) - bestAnt.getNumberViolations());
            pheromoneMatrix[cloudletId][vmId] += deltaPheromone;
            pheromoneMatrix[cloudletId][vmId] = Math.min(pheromoneMatrix[cloudletId][vmId], MAX_PHEROMONE_LEVEL); // Cap pheromone levels
        }
    }

    protected List<Ant> createAnts() {
        List<Ant> ants = new ArrayList<>();

        for (int i = 0; i < numAnts; i++) {
            Ant ant = new Ant();
            ants.add(ant);
        }

        return ants;
    }

    // higher is better. After the ant sends the jobs, and the simulation is done, we evaluate the ant's performance
    // based on the number of SLA violations and the makespan.
    protected double evaluateAnt(List<Cloudlet> finished, DatacenterBroker broker) {

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

    protected int numberOfViolations(List<DeadlineCloudlet> cloudlets, DatacenterBroker broker) {
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
}


class Ant {
    protected final Map<DeadlineCloudlet, Vm> allocation = new HashMap<>();
    protected double fitness;
    protected List<DeadlineCloudlet> cloudletsFinished;
    protected int numberViolations;

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
