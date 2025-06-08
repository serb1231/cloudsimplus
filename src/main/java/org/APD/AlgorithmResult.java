package org.APD;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.List;

public record AlgorithmResult(
        String algorithmName,
        List<Cloudlet> cloudlets,
        List<Host> hosts,
        List<Vm> vms,
        List<Cloudlet> cloudletFinishedList
) {}
