package org.APD;

import ch.qos.logback.classic.Level;
import org.APD.Algorithms.*;

import org.APD.PowerModels.PowerModelPStateProcessor;
import org.APD.PowerModels.PowerModelPstateProcessor_2GHz_Via_C7_M;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.util.Log;

import java.util.List;

public class CompareAlgorithms extends AlgorithmBaseFunctionalities {

    public static void main(String[] args) {
        Log.setLevel(Level.OFF);
        new CompareAlgorithms().RunCompareAlgorithms();
    }

    public void RunCompareAlgorithms() {

        PowerModelPStateProcessor currentPowerModel = new PowerModelPstateProcessor_2GHz_Via_C7_M(0);
        PowerModelPStateProcessor.PerformanceState[] performanceStates = currentPowerModel.getPossiblePerformanceStates();

        List<DeadlineCloudlet> cloudletListInitial = createCloudletsBurstyArrivalTightDeadlineHeavyTayloredBigGroupedJobs();

        for (int i = performanceStates.length - 1; i >= 0; i--) {
            MIPS_PER_HOST_MAX = (int) (performanceStates[i].processingFraction() * MIPS_PER_HOST_INITIAL_MAX);
            MIPS_PER_VM_MAX = (int) (performanceStates[i].processingFraction() * MIPS_PER_VM_INITIAL_MAX);


            MIPS_PER_HOST_MIN = (int) (performanceStates[i].processingFraction() * MIPS_PER_HOST_INITIAL_MIN);
            MIPS_PER_VM_MIN = (int) (performanceStates[i].processingFraction() * MIPS_PER_VM_INITIAL_MIN);
            POWER_STATE = i;
            System.out.printf("\n\n\n" +
                    "-----------------------------------------------------------------------------------------------------------\n" +
                    "Performance State %d: MIPS_PER_HOST = %d, MIPS_PER_VM = %d\n" +
                    "-----------------------------------------------------------------------------------------------------------\n\n\n", i, MIPS_PER_HOST_MAX, MIPS_PER_VM_MAX);

            List<DeadlineCloudlet> cloudletListRR = copyCloudlets(cloudletListInitial);
            List<DeadlineCloudlet> cloudletListFCFS = copyCloudlets(cloudletListInitial);
            List<DeadlineCloudlet> cloudletListACO = copyCloudlets(cloudletListInitial);
            List<DeadlineCloudlet> cloudletListGA = copyCloudlets(cloudletListInitial);

            List<Vm> vmListRR = createVms();
            List<Vm> vmListFCFS = createVms();
            List<Vm> vmListACO = createVms();
            List<Vm> vmListGA = createVms();

            SchedulingAlgorithm fcfs = new FCFSAlgorithm_bin();
            SchedulingAlgorithm roundRobin = new RoundRobinAlgorithm();
            SchedulingAlgorithm powerAware = new ACOAlgorithm();
            SchedulingAlgorithm gaAlgorithm = new GAAlgorithm();

            AlgorithmResult resultRoundRobin = roundRobin.run(createRelevantDataForAlgorithms(vmListRR, cloudletListRR));
            AlgorithmResult resultFCFS = fcfs.run(createRelevantDataForAlgorithms(vmListFCFS, cloudletListFCFS));
            AlgorithmResult resultACO = powerAware.run(createRelevantDataForAlgorithms(vmListACO, cloudletListACO));
            AlgorithmResult resultGA = gaAlgorithm.run(createRelevantDataForAlgorithms(vmListGA, cloudletListGA));

            // if any of the SLA violations are broken, break the loop
            if (wereSLAViolations(resultFCFS.cloudletFinishedList()) ||
                wereSLAViolations(resultRoundRobin.cloudletFinishedList()) ||
                wereSLAViolations(resultACO.cloudletFinishedList()) ||
                wereSLAViolations(resultGA.cloudletFinishedList())) {
                System.out.println("\n\n\n" +
                        "-----------------------------------------------------------------------------------------------------------\n" +
                        "SLA violations detected, stopping the simulation.\n" +
                        "-----------------------------------------------------------------------------------------------------------\n\n\n");

                // print the violated cloudlets and for which algorithm
                System.out.println("\n\n-----------------------------------------------------------------" +
                        "Violated Cloudlets for FCFS:");
                printSLAViolations(resultFCFS.cloudletFinishedList());
                System.out.println("\n\n-----------------------------------------------------------------" +
                        "Violated Cloudlets for Round Robin:");
                printSLAViolations(resultRoundRobin.cloudletFinishedList());
                System.out.println("\n\n-----------------------------------------------------------------" +
                        "Violated Cloudlets for ACO:");
                printSLAViolations(resultACO.cloudletFinishedList());
                System.out.println("\n\n-----------------------------------------------------------------" +
                        "Violated Cloudlets for GA:");
                printSLAViolations(resultGA.cloudletFinishedList());
                return;
            }

//
//            System.out.println("\n\n\n----------------------------------------FCFS Algorithm Result:-----------------------------------------\n\n\n");
//            printVmsCpuUtilizationAndPowerConsumption(resultFCFS.vms());
//            printHostsCpuUtilizationAndPowerConsumption(resultFCFS.hosts());
//            double makespan = resultFCFS.cloudletFinishedList().stream()
//                    .mapToDouble(Cloudlet::getFinishTime)
//                    .max()
//                    .orElse(0.0);
//            System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespan);
//            // Print the SLA violations
//            printSLAViolations(resultFCFS.cloudletFinishedList());
//
//            System.out.println("\n\n\n----------------------------------------Round Robin Algorithm Result:-----------------------------------------\n\n\n");
//            printVmsCpuUtilizationAndPowerConsumption(resultRoundRobin.vms());
//            printHostsCpuUtilizationAndPowerConsumption(resultRoundRobin.hosts());
//            double makespanRoundRobin = resultRoundRobin.cloudletFinishedList().stream()
//                    .mapToDouble(Cloudlet::getFinishTime)
//                    .max()
//                    .orElse(0.0);
//            System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanRoundRobin);
//            // Print the SLA violations
//            printSLAViolations(resultRoundRobin.cloudletFinishedList());
//
//            System.out.println("\n\n\n----------------------------------------ACO Algorithm Result:-----------------------------------------\n\n\n");
//            printVmsCpuUtilizationAndPowerConsumption(resultACO.vms());
//            printHostsCpuUtilizationAndPowerConsumption(resultACO.hosts());
//            double makespanACO = resultACO.cloudletFinishedList().stream()
//                    .mapToDouble(Cloudlet::getFinishTime)
//                    .max()
//                    .orElse(0.0);
//            System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanACO);
//            // Print the SLA violations
//            printSLAViolations(resultACO.cloudletFinishedList());
//
//            System.out.println("\n\n\n----------------------------------------GA Algorithm Result:-----------------------------------------\n\n\n");
//            printVmsCpuUtilizationAndPowerConsumption(resultGA.vms());
//            printHostsCpuUtilizationAndPowerConsumption(resultGA.hosts());
//            double makespanGA = resultGA.cloudletFinishedList().stream()
//                    .mapToDouble(Cloudlet::getFinishTime)
//                    .max()
//                    .orElse(0.0);
//            System.out.printf("📌 Makespan (time of last cloudlet finish): %.2f seconds\n", makespanGA);
//            // Print the SLA violations
//            printSLAViolations(resultGA.cloudletFinishedList());
        }
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
                CLOUDLETS_PER_FRAME,
                CLOUDLET_PES,
                CLOUDLET_LENGTH_MIN,
                CLOUDLET_LENGTH_MAX,
                STATIC_POWER,
                MAX_POWER,
                vmListClone,
                cloudletListClone
        );
    }
}
