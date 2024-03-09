package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.framework.Constraints;
import org.framework.Parameter;
import org.framework.Utils;

import java.util.List;

/**
 * Reparation类定义了Memetic算法中的修复操作，用于处理种群中个体的不合理解。
 */
public class Reparation {
    private Reparation() {
    }
    /**
     * 对种群中的个体进行修复操作，确保其满足约束条件。
     * @param population            待修复的种群
     * @param virtualMachineList    虚拟机列表
     * @param physicalMachineList   物理机列表
     * @param numberOfResources     资源数量
     * @return 修复后的种群
     */
    public static Population repairPopulation(Population population, List<VirtualMachine> virtualMachineList, List<PhysicalMachine> physicalMachineList,
                                              int numberOfResources) {
        List<VirtualMachine> individualVmList = VirtualMachine.cloneVMsList(virtualMachineList);
        List<PhysicalMachine> individualPmList = PhysicalMachine.clonePMsList(physicalMachineList);
        // 遍历种群中的每个个体，进行修复操作
        for (Individual individual : population.getIndividuals()) {
            individualVmList = individual.convertToVMList(individualVmList);
            individualPmList = individual.convertToPMList(individualPmList, numberOfResources);
            checkAndRepair(individual, individualVmList, individualPmList, numberOfResources);
        }
        return population;
    }
    /**
     * 检查并修复个体，确保其满足约束条件。
     * @param individual           待修复的个体
     * @param individualVmList     个体对应的虚拟机列表
     * @param individualPmList     个体对应的物理机列表
     * @param numberOfResources    资源数量
     */
    public static void checkAndRepair(Individual individual, List<VirtualMachine> individualVmList, List<PhysicalMachine> individualPmList,
                                      int numberOfResources) {
        int iteratorSolution;
        int physicalMachineId;
        PhysicalMachine pm;
        VirtualMachine vm;
        List<VirtualMachine> vmsInPM;
        // 遍历个体中的每个解，检查并修复
        for (iteratorSolution = 0; iteratorSolution < individual.getSize(); iteratorSolution++) {
            physicalMachineId = individual.getSolution()[iteratorSolution];
            if (physicalMachineId != 0) {
                pm = PhysicalMachine.getById(physicalMachineId, individualPmList);
                vmsInPM = Utils.filterVMsByPM(individualVmList, physicalMachineId);
                vm = individualVmList.get(iteratorSolution);
                // 如果物理机过载，则移动虚拟机
                if (Constraints.checkPMOverloaded(pm, vmsInPM, Parameter.PROTECTION_FACTOR)) {
                    moveVM(individual, iteratorSolution, vm, individualVmList, individualPmList, numberOfResources);
                }
            }
        }
    }

    /**
     * 移动虚拟机，以修复物理机的过载情况。
     * @param individual            待修复的个体
     * @param iteratorSolution      虚拟机在个体中的索引
     * @param vm                    待移动的虚拟机
     * @param virtualMachineList    虚拟机列表
     * @param physicalMachineList   物理机列表
     * @param numberOfResources     资源数量
     * @return 是否成功移动虚拟机
     */
    private static Boolean moveVM(Individual individual, int iteratorSolution, VirtualMachine vm,
                                  List<VirtualMachine> virtualMachineList, List<PhysicalMachine> physicalMachineList, int numberOfResources) {
        int pmIdCandidate;
        int iteratorPhysical;
        int iteratorResources;
        Float vmResource;
        Float resourceRequested;
        Float newResourceRequested;
        int numberOfPMs = physicalMachineList.size();
        int actualPMId = vm.getPhysicalMachine();
        PhysicalMachine pmCandidate;
        // 随机选择一个物理机作为目标
        pmIdCandidate = Utils.getRandomInt(1, numberOfPMs);
        for (iteratorPhysical = 0; iteratorPhysical < numberOfPMs; iteratorPhysical++) {
            pmCandidate = PhysicalMachine.getById(pmIdCandidate, physicalMachineList);
            // 如果目标物理机满足资源约束，则进行移动操作
            if (Constraints.checkResources(pmCandidate, null, vm, virtualMachineList, false)) {
                // 对每个资源进行更新
                for (iteratorResources = 0; iteratorResources < numberOfResources; iteratorResources++) {
                    vmResource = vm.getResources().get(iteratorResources) * (vm.getUtilization().get(iteratorResources) / 100);
                    // 更新原物理机的资源利用情况
                    resourceRequested = individual.getUtilization()[actualPMId - 1][iteratorResources];
                    newResourceRequested = resourceRequested - vmResource;
                    individual.getUtilization()[actualPMId - 1][iteratorResources] = newResourceRequested;
                    physicalMachineList.get(actualPMId - 1).getResourcesRequested().set(iteratorResources, newResourceRequested);
                    // 更新目标物理机的资源利用情况
                    resourceRequested = individual.getUtilization()[pmIdCandidate - 1][iteratorResources];
                    newResourceRequested = resourceRequested + vmResource;
                    individual.getUtilization()[pmIdCandidate - 1][iteratorResources] = newResourceRequested;
                    physicalMachineList.get(pmIdCandidate - 1).getResourcesRequested().set(iteratorResources, newResourceRequested);
                }
                // 更新个体中虚拟机的位置
                individual.getSolution()[iteratorSolution] = pmIdCandidate;
                virtualMachineList.get(iteratorSolution).setPhysicalMachine(pmIdCandidate);
                return true;
            }
            // 循环选择下一个目标物理机
            if (pmIdCandidate < numberOfPMs) {
                pmIdCandidate += 1;
            } else {
                pmIdCandidate = 1;
            }
        }
        // 移动失败
        return false;
    }
}
