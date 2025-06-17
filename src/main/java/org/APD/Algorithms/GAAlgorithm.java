package org.APD.Algorithms;
/*  ──────────────────────────────────────────────────────────────────────
    GAAlgorithm.java   –  Genetic-Algorithm replacement for ACOAlgorithm
    ----------------------------------------------------------------------
    Copyright 2025 Ionescu Serban-Mihai
   ────────────────────────────────────────────────────────────────────── */

import ch.qos.logback.classic.Level;
import org.APD.AlgorithmResult;
import org.APD.RelevantDataForAlgorithms;
import org.APD.DeadlineCloudlet;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;

import java.util.*;

public class GAAlgorithm extends BaseSchedulingAlgorithm {

    /* GA hyperparameters (tweak as you like) */
    private static final int POP_SIZE       = 30;     // individuals
    private static final int MAX_GENERATION = 50;     // iterations
    private static final double CROSSOVER_P = 0.9;    // probability
    private static final double MUTATION_P  = 0.02;   // probability per gene

    /* Internals */
    private final Random rng = new Random();

    /* ─────────────────────────── Public entry point ─────────────────────────── */
    @Override
    public AlgorithmResult run(RelevantDataForAlgorithms relevantData) {

        copyGivenDataLocally(relevantData);

        algorithmGA();

        return new AlgorithmResult("GA", cloudletList, hostList, vmList, broker0.getCloudletFinishedList());
    }

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new GAAlgorithm();
    }

    public GAAlgorithm() {

//        vmList = createVms();
//        cloudletList = createCloudletsUniformDistribution();
//
//        algorithmGA();
//
//
//        System.out.println("------------------------------- SIMULATION FOR SCHEDULING INTERVAL = " + SCHEDULING_INTERVAL + " -------------------------------");
//        final List<DeadlineCloudlet> cloudletFinishedList = broker0.getCloudletFinishedList();
//        final Comparator<DeadlineCloudlet> hostComparator = comparingLong(cl -> cl.getVm().getHost().getId());
//        cloudletFinishedList.sort(hostComparator.thenComparing(cl -> cl.getVm().getId()));
//
//        new CloudletsTableBuilder(cloudletFinishedList).build();
//        printHostsCpuUtilizationAndPowerConsumption();
//        printVmsCpuUtilizationAndPowerConsumption();
//
//        double makespan = cloudletFinishedList.stream()
//                .mapToDouble(Cloudlet::getFinishTime)
//                .max()
//                .orElse(0.0);
//
//        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespan);
//
//        printSLAViolations(broker0.getCloudletFinishedList());
    }




    private void algorithmGA() {

        simulation = new CloudSimPlus();
//        hostList = new ArrayList<>(HOSTS);
        Datacenter datacenter0 = createDatacenter(simulation, hostList);
        broker0 = new DatacenterBrokerSimple(simulation);


        int N = cloudletList.size();
        int M = vmList.size();

        /* ---------- 1. Create initial population -------------------------------- */
        List<int[]> population = new ArrayList<>(POP_SIZE);
        for (int i = 0; i < POP_SIZE; i++)
            population.add(randomChromosome(N, M));

        /* ---------- 2. Evolution loop ------------------------------------------- */
        int[] bestChrom = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (int gen = 0; gen < MAX_GENERATION; gen++) {

            /* evaluate and rank (largest fitness first) */
            population.sort((c1, c2) -> Double.compare(fitness(c2), fitness(c1)));

            if (fitness(population.get(0)) > bestFitness) {          //  ↑ flipped sign
                bestFitness = fitness(population.get(0));
                bestChrom   = population.get(0).clone();
            }

            /* build next generation */
            List<int[]> next = new ArrayList<>(POP_SIZE);
            /* elitism: keep the top 2 */
            next.add(population.get(0));
            next.add(population.get(1));

            while (next.size() < POP_SIZE) {
                int[] parent1 = tournamentSelect(population);
                int[] parent2 = tournamentSelect(population);

                int[] child1 = parent1.clone();
                int[] child2 = parent2.clone();

                if (rng.nextDouble() < CROSSOVER_P) {
                    int cut = rng.nextInt(N);
                    for (int g = cut; g < N; g++) {
                        int tmp = child1[g];
                        child1[g] = child2[g];
                        child2[g] = tmp;
                    }
                }
                mutate(child1, M);
                mutate(child2, M);

                next.add(child1);
                if (next.size() < POP_SIZE) next.add(child2);
            }
            population = next;
        }

        /* ---------- 3. Build and run CloudSim using best chromosome ------------- */

        broker0.submitVmList(vmList);

        for (int i = 0; i < cloudletList.size(); i++) {
            assert bestChrom != null;
            Vm chosenVm = vmList.get(bestChrom[i]);
            cloudletList.get(i).setVm(chosenVm);
            broker0.submitCloudlet(cloudletList.get(i));
        }

        simulation.start();

    }


    /* ───────────────────────── Helper methods ───────────────────────── */

    /* chromosome = int[ cloudletId ] → vmId */
    private int[] randomChromosome(int N, int M) {
        int[] genes = new int[N];
        for (int i = 0; i < N; i++)
            genes[i] = rng.nextInt(M);
        return genes;
    }

    /* minimises makespan – bigger is better */
    private double fitness(int[] chromosome) {
        // iterate through the cloudlets
        double[] finishTimes = new double[vmList.size()];
        int nrOfViolations = 0;

        for (int i = 0; i < chromosome.length; i++) {
            DeadlineCloudlet currentCloudlet = cloudletList.get(i);
            Vm currentVm = vmList.get(chromosome[i]);
            if (finishTimes[chromosome[i]] > currentCloudlet.getSubmissionDelay()) {
                // if the cloudlet is submitted after the VM is already busy, we need to wait
                finishTimes[chromosome[i]] += currentCloudlet.getLength() / currentVm.getMips();
            } else {
                finishTimes[chromosome[i]] = currentCloudlet.getSubmissionDelay() + currentCloudlet.getLength() / currentVm.getMips();
            }

            if (finishTimes[chromosome[i]] > currentCloudlet.getDeadline()) {
                    // if the cloudlet is finished after the deadline, we count it as a violation
                    nrOfViolations++;
            }
        }
        // get the VM with the maximum finish time
        double makespan = Arrays.stream(finishTimes).max().orElse(0.0);

        return (double) 1 / (1 + nrOfViolations) + 0.01 * (((double) (CLOUDLET_LENGTH_MAX * (TOTAL_CLOUDLETS)) / 1000) / makespan); // Fitness function

    }

    private int[] tournamentSelect(List<int[]> pop) {
        int a = rng.nextInt(pop.size());
        int b = rng.nextInt(pop.size());
        /* pick the one with the *larger* fitness now */
        return fitness(pop.get(a)) > fitness(pop.get(b))
                ? pop.get(a)
                : pop.get(b);
    }

    private void mutate(int[] genes, int vmCount) {
        for (int i = 0; i < genes.length; i++)
            if (rng.nextDouble() < MUTATION_P)
                genes[i] = rng.nextInt(vmCount);
    }
}


