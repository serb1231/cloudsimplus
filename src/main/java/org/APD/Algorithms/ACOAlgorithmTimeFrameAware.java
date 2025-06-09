package org.APD.Algorithms;

import org.APD.DeadlineCloudlet;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public class ACOAlgorithmTimeFrameAware extends ACOAlgorithm{
    @Override
    public String getName() {
        return "ACOAlgorithmTimeFrameAware";
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

}
