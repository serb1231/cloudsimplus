package org.APD;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public record AlgorithmResult(
        String algorithmName,
        List<DeadlineCloudlet> cloudlets,
        List<Host> hosts,
        List<Vm> vms,
        List<DeadlineCloudlet> cloudletFinishedList,
        long totalExecutionTime
) {}
