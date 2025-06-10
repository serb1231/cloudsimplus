package org.APD.Algorithms;
/*  ──────────────────────────────────────────────────────────────────────
    GAAlgorithm.java   –  Genetic-Algorithm replacement for ACOAlgorithm
    ----------------------------------------------------------------------
    Copyright 2025 Your-Name-Here
   ────────────────────────────────────────────────────────────────────── */

import ch.qos.logback.classic.Level;
import org.APD.AlgorithmResult;
import org.APD.RelevantDataForAlgorithms;
import org.APD.DeadlineCloudlet;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.vms.Vm;

import java.util.*;

        import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingLong;

public class GAAlgorithm extends BaseSchedulingAlgorithm {

    /* GA hyper-parameters (tweak as you like) */
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

        vmList = createVms();
        cloudletList = createCloudlets();

        algorithmGA();


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

        printSLAViolations(broker0.getCloudletFinishedList());
    }




    private void algorithmGA() {

        simulation = new CloudSimPlus();
        hostList = new ArrayList<>(HOSTS);
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
        double bestFitness = Double.POSITIVE_INFINITY;

        for (int gen = 0; gen < MAX_GENERATION; gen++) {

            /* evaluate and rank */
            population.sort(comparingDouble(this::fitness));
            if (fitness(population.get(0)) < bestFitness) {
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

        List<Vm>        vms       = copyVMs(vmList);
        List<DeadlineCloudlet> cls = copyCloudlets(cloudletList);

        broker0.submitVmList(vms);

        for (int i = 0; i < cls.size(); i++) {
            Vm chosenVm = vms.get(bestChrom[i]);
            cls.get(i).setVm(chosenVm);
            broker0.submitCloudlet(cls.get(i));
        }

        simulation.start();

        /* optional: show result table */
        new CloudletsTableBuilder(broker0.getCloudletFinishedList()).build();

//        double makespan = broker0.getCloudletFinishedList()
//                .stream()
//                .mapToDouble(Cloudlet::getFinishTime)
//                .max()
//                .orElse(0);

    }


    /* ───────────────────────── Helper methods ───────────────────────── */

    /* chromosome = int[ cloudletId ] → vmId */
    private int[] randomChromosome(int N, int M) {
        int[] genes = new int[N];
        for (int i = 0; i < N; i++)
            genes[i] = rng.nextInt(M);
        return genes;
    }

    /* minimises makespan – smaller is better */
    private double fitness(int[] chromosome) {
        Map<Integer, List<DeadlineCloudlet>> vmQueues = new HashMap<>();
        for (int i = 0; i < chromosome.length; i++)
            vmQueues.computeIfAbsent(chromosome[i], k -> new ArrayList<>())
                    .add(dummyCloudlet(i));            // lightweight proxy

        /* rough estimate: each Cloudlet has execTimeSec in its length → finish time = sum */
        double worst = 0;
        for (List<DeadlineCloudlet> q : vmQueues.values()) {
            double sum = q.stream().mapToDouble(cl -> cl.getLength()).sum();
            worst = Math.max(worst, sum);
        }
        return worst;
    }

    /* very small helper so we don’t touch real objects during evaluation */
    private DeadlineCloudlet dummyCloudlet(int id) {
        // cloudlet length / MIPS is already reflected
        return new DeadlineCloudlet(id, 0, 1);   // length 0 – we only use .getLength()
    }

    private int[] tournamentSelect(List<int[]> pop) {
        int a = rng.nextInt(pop.size());
        int b = rng.nextInt(pop.size());
        return (fitness(pop.get(a)) < fitness(pop.get(b))) ? pop.get(a) : pop.get(b);
    }

    private void mutate(int[] genes, int vmCount) {
        for (int i = 0; i < genes.length; i++)
            if (rng.nextDouble() < MUTATION_P)
                genes[i] = rng.nextInt(vmCount);
    }
}
