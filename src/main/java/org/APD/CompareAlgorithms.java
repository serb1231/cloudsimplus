package org.APD;

import ch.qos.logback.classic.Level;
import org.APD.Algorithms.*;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.power.models.PowerModelHostSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmResourceStats;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.util.Log;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingLong;

public class CompareAlgorithms extends AlgorithmBaseFunctionalities {

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new CompareAlgorithms().RunCompareAlgorithms();
    }

    public void RunCompareAlgorithms() {

        List<Vm> vmList = createVms();
        List<DeadlineCloudlet> cloudletList = createCloudlets();


        SchedulingAlgorithm fcfs = new FCFSAlgorithm_bin();
        SchedulingAlgorithm roundRobin = new RoundRobinAlgorithm();
        SchedulingAlgorithm powerAware = new ACOAlgorithm();

        AlgorithmResult resultFCFS = fcfs.run(createRelevantDataForAlgorithms(vmList, cloudletList));
        AlgorithmResult resultRoundRobin = roundRobin.run(createRelevantDataForAlgorithms(vmList, cloudletList));
        AlgorithmResult resultACO = powerAware.run(createRelevantDataForAlgorithms(vmList, cloudletList));


        System.out.println("\n\n\n----------------------------------------FCFS Algorithm Result:-----------------------------------------\n\n\n");
        printVmsCpuUtilizationAndPowerConsumption(resultFCFS.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultFCFS.hosts());

        double makespan = resultFCFS.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);

        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespan);

        // Print the SLA violations
        printSLAViolations(resultFCFS.cloudletFinishedList());

        System.out.println("\n\n\n----------------------------------------Round Robin Algorithm Result:-----------------------------------------\n\n\n");
        printVmsCpuUtilizationAndPowerConsumption(resultRoundRobin.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultRoundRobin.hosts());

        double makespanRoundRobin = resultRoundRobin.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);

        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanRoundRobin);

        // Print the SLA violations
        printSLAViolations(resultRoundRobin.cloudletFinishedList());

        System.out.println("\n\n\n----------------------------------------ACO Algorithm Result:-----------------------------------------\n\n\n");
        printVmsCpuUtilizationAndPowerConsumption(resultACO.vms());
        printHostsCpuUtilizationAndPowerConsumption(resultACO.hosts());

        double makespanACO = resultACO.cloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0.0);
        System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanACO);

        // Print the SLA violations
        printSLAViolations(resultACO.cloudletFinishedList());
    }

    RelevantDataForAlgorithms createRelevantDataForAlgorithms(List<Vm> vmList, List<DeadlineCloudlet> cloudletList) {
        var vmListClone = copyVMs(vmList);
        var cloudletListClone = copyCloudlets(cloudletList);
        return new RelevantDataForAlgorithms(
                SCHEDULING_INTERVAL,
                HOSTS,
                HOST_PES,
                HOST_START_UP_DELAY,
                HOST_SHUT_DOWN_DELAY,
                HOST_START_UP_POWER,
                HOST_SHUT_DOWN_POWER,
                VMS,
                VM_PES,
                CLOUDLETS,
                CLOUDLET_PES,
                CLOUDLET_LENGTH_MIN,
                CLOUDLET_LENGTH_MAX,
                STATIC_POWER,
                MAX_POWER,
                vmListClone,
                cloudletListClone
        );
    }

    protected void printVmsCpuUtilizationAndPowerConsumption(List<Vm> vmList) {
        vmList.sort(comparingLong(vm -> vm.getHost().getId()));
        for (Vm vm : vmList) {
            final var powerModel = vm.getHost().getPowerModel();
            final double hostStaticPower = powerModel instanceof PowerModelHostSimple powerModelHost ? powerModelHost.getStaticPower() : 0;
            final double hostStaticPowerByVm = hostStaticPower / vm.getHost().getVmCreatedList().size();

            //VM CPU utilization relative to the host capacity
            final double vmRelativeCpuUtilization = vm.getCpuUtilizationStats().getMean() / vm.getHost().getVmCreatedList().size();
            final double vmPower = powerModel.getPower(vmRelativeCpuUtilization) - hostStaticPower + hostStaticPowerByVm; // W
            final VmResourceStats cpuStats = vm.getCpuUtilizationStats();
            System.out.printf(
                    "Vm   %2d CPU Usage Mean: %6.1f%% | Power Consumption Mean: %8.0f W%n",
                    vm.getId(), cpuStats.getMean() * 100, vmPower);
        }
    }

    protected void printHostsCpuUtilizationAndPowerConsumption(List<Host> hostList) {
        System.out.println();
        for (final Host host : hostList) {
            printHostCpuUtilizationAndPowerConsumption(host);
        }
        System.out.println();
    }
}
