///*  ──────────────────────────────────────────────────────────────────────
//    PSOAlgorithm.java – Discrete-PSO version of the GA/ACO scheduler
//    ----------------------------------------------------------------------
//    Copyright 2025  Ionescu Serban-Mihai
//   ────────────────────────────────────────────────────────────────────── */
//package org.APD.Algorithms;
//
//import ch.qos.logback.classic.Level;
//import org.APD.AlgorithmResult;
//import org.APD.DeadlineCloudlet;
//import org.APD.RelevantDataForAlgorithms;
//import org.cloudsimplus.brokers.DatacenterBroker;
//import org.cloudsimplus.brokers.DatacenterBrokerSimple;
//import org.cloudsimplus.core.CloudSimPlus;
//import org.cloudsimplus.datacenters.Datacenter;
//import org.cloudsimplus.hosts.Host;
//import org.cloudsimplus.util.Log;
//import org.cloudsimplus.vms.Vm;
//
//import java.util.*;
//
//import static java.util.stream.Collectors.toList;
//
//public class PSOAlgorithm extends BaseSchedulingAlgorithm {
//
//    /* ───────────────────────── 1.  PSO hyper-parameters ───────────────────────── */
//    private static final int SWARM_SIZE   = 30;
//    private static final int MAX_ITER     = 50;
//    private static final double W         = 0.6;     // inertia
//    private static final double C1        = 1.5;     // cognitive
//    private static final double C2        = 1.5;     // social
//
//    /* ───────────────────────── 2.  Book-keeping fields ───────────────────────── */
//    private final Random rng = new Random();
//
//    private final List<Vm> vmList;                       // reference list (size = M)
//    private final List<DeadlineCloudlet> cloudlets;      // reference cloudlets (size = N)
//    private final int[] submissionOrder;                 // idx[] sorted by arrival
//
//    /* convenience */
//    private final int N;        // number of cloudlets
//    private final int M;        // number of VMs
//
//    /* ───────────────────────── 3.  Ctor expects original lists ────────────────── */
//    public PSOAlgorithm(List<Vm> vmList, List<DeadlineCloudlet> clList) {
//        this.vmList   = vmList;
//        this.cloudlets = clList;
//        this.N = clList.size();
//        this.M = vmList.size();
//
//        /* sort once by arrival to use inside fitness() */
//        this.submissionOrder = clList.stream()
//                .sorted(Comparator.comparingDouble(DeadlineCloudlet::getSubmissionDelay))
//                .mapToInt(clList::indexOf)
//                .toArray();
//    }
//
//    /* ───────────────────────── 4.  Run entry point ───────────────────────────── */
//    @Override
//    public AlgorithmResult run(List<Host> HOSTS,
//                               List<Vm>   vmTemplate,
//                               List<DeadlineCloudlet> clTemplate,
//                               RelevantDataForAlgorithms data) {
//
//        Log.setLevel(Level.OFF);
//
//        /* ---------- 4.1 build initial swarm ------------------------------------ */
//        List<Particle> swarm = new ArrayList<>(SWARM_SIZE);
//        for (int i = 0; i < SWARM_SIZE; i++)
//            swarm.add(new Particle(randomChromosome(), new double[N]));
//
//        Particle gBest = null;
//        double gBestFit = Double.NEGATIVE_INFINITY;
//
//        /* ---------- 4.2 iterate ----------------------------------------------- */
//        for (int iter = 0; iter < MAX_ITER; iter++) {
//
//            /* evaluate every particle */
//            for (Particle p : swarm) {
//                double fit = fitness(p.pos);
//                if (fit > p.bestFit) {                        // update personal best
//                    p.bestFit = fit;
//                    p.pBest   = p.pos.clone();
//                }
//                if (fit > gBestFit) {                         // update global best
//                    gBestFit = fit;
//                    gBest    = new Particle(p.pos.clone(), p.vel.clone());
//                    gBest.bestFit = fit;
//                    gBest.pBest   = p.pBest.clone();
//                }
//            }
//
//            /* update velocity & position */
//            for (Particle p : swarm) {
//                for (int g = 0; g < N; g++) {
//                    double r1 = rng.nextDouble();
//                    double r2 = rng.nextDouble();
//
//                    /* classic PSO equation in discrete form ------------------- */
//                    p.vel[g] = W  * p.vel[g]
//                            + C1 * r1 * (p.pBest[g] - p.pos[g])
//                            + C2 * r2 * (gBest.pos[g] - p.pos[g]);
//
//                    /* position update: round and wrap into [0 … M-1] ---------- */
//                    int newGene = (int)Math.round(p.pos[g] + p.vel[g]);
//                    p.pos[g] = ((newGene % M) + M) % M;     // positive modulo
//                }
//            }
//        }
//
//        /* ---------- 4.3  Build real CloudSim run using gBest ------------------ */
//        CloudSimPlus sim = new CloudSimPlus();
//        Datacenter   dc  = createDatacenter(sim, new ArrayList<>(HOSTS));
//        DatacenterBroker broker = new DatacenterBrokerSimple(sim);
//
//        /* deep-copy templates */
//        List<Vm> vms = vmTemplate.stream().map(Vm::clone).collect(toList());
//        List<DeadlineCloudlet> cls = clTemplate.stream()
//                .map(DeadlineCloudlet::clone)
//                .collect(toList());
//
//        broker.submitVmList(vms);
//
//        /* bind each cloudlet to VM according to best position */
//        for (int i = 0; i < cls.size(); i++) {
//            cls.get(i).setVm( vms.get(gBest.pos[i]) );
//            broker.submitCloudlet(cls.get(i));
//        }
//
//        sim.start();
//
//        double makespan = broker.getCloudletFinishedList()
//                .stream()
//                .mapToDouble(c -> c.getFinishTime())
//                .max()
//                .orElse(0);
//
//        return new AlgorithmResult("PSO", makespan,
//                /*energy*/0,
//                sim.getLastCloudletProcessingTime());
//    }
//
//    /* ───────────────────────── 5.  Helper: chromosome generator ─────────────── */
//    private int[] randomChromosome() {
//        int[] genes = new int[N];
//        for (int i = 0; i < N; i++)
//            genes[i] = rng.nextInt(M);
//        return genes;
//    }
//
//    /* ───────────────────────── 6.  Fitness (higher = better) ────────────────── */
//    private double fitness(int[] chrom) {
//        double[] lastFin = new double[M];             // timeline per VM
//
//        for (int idx : submissionOrder) {
//            DeadlineCloudlet cl = cloudlets.get(idx);
//            int vmId = chrom[idx];
//
//            double arrival  = cl.getSubmissionDelay();
//            double execSec  = cl.getLength();         // you stored execTimeSec in length
//
//            double start = Math.max(arrival, lastFin[vmId]);
//            lastFin[vmId] = start + execSec;
//        }
//        /* we minimise makespan → flip sign so bigger = better */
//        double makespan = Arrays.stream(lastFin).max().orElse(0);
//        return -makespan;
//    }
//
//
//    /* ───────────────────────── 7.  Inner Particle class ─────────────────────── */
//    private static class Particle {
//        int[] pos;            // current chromosome (N genes)
//        double[] vel;         // real velocity per gene
//        int[] pBest;          // personal-best chromosome
//        double bestFit;       // fitness of personal best
//
//        Particle(int[] pos, double[] vel) {
//            this.pos = pos;
//            this.vel = vel;
//            this.pBest = pos.clone();
//            this.bestFit = Double.NEGATIVE_INFINITY;
//        }
//    }
//}
