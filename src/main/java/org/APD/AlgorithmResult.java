package org.APD;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public record AlgorithmResult(
        String algorithmName,
        double makespan,
        double totalEnergy,
        double avgCpuUtilization,
        List<Cloudlet> finishedCloudlets,
        List<Host> hosts,
        List<Vm> vms
) {}
