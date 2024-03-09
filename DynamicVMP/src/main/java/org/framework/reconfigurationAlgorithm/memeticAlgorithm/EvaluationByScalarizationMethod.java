package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.framework.Constant;
import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.util.ArrayList;
import java.util.List;
public class EvaluationByScalarizationMethod implements FitnessEvaluation {
    public EvaluationByScalarizationMethod() {
        super();
    }
    //评估整个种群的适应度，基于Scalarization Method
    @Override
    public void evaluate(Population population, List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs, List<PhysicalMachine> physicalMachineList, List<APrioriValue> aPrioriValuesList, int numberOfResources, int numberOfObjFunctions) {

        // 辅助列表用于获取个体信息和计算目标函数
        List<VirtualMachine> individualVMsList = VirtualMachine.cloneVMsList(virtualMachineList);
        List<PhysicalMachine> individualPMsList = PhysicalMachine.clonePMsList(physicalMachineList);
        List<VirtualMachine> individualDerivedVMs = VirtualMachine.cloneVMsList(derivedVMs);
        //遍历每个个体
        for (Individual individual : population.getIndividuals()) {
            individualVMsList = individual.convertToVMList(individualVMsList);
            individualPMsList = individual.convertToPMList(individualPMsList, numberOfResources);
            // 载入目标函数和适应度
            loadObjectiveFunctions(individual, individualVMsList, virtualMachineList, individualDerivedVMs, individualPMsList, numberOfResources);
            loadFitness(individual, aPrioriValuesList, numberOfObjFunctions);
        }

    }
    //载入目标函数的值
    @Override
    public void loadObjectiveFunctions(Individual individual, List<VirtualMachine> virtualMachineList, List<VirtualMachine> previousVirtualMachineList, List<VirtualMachine> derivedVMs, List<PhysicalMachine> physicalMachineList, int numberOfResources) {

        Float economicalRevenue;
        Float powerConsumption;
        Float wastedResources;
        Float memoryMigrated;
        //计算目标函数的值
        memoryMigrated = ObjectivesFunctions.migratedMemoryBtwPM(previousVirtualMachineList, virtualMachineList, physicalMachineList.size());
        powerConsumption = ObjectivesFunctions.powerConsumption(physicalMachineList);
        wastedResources = ObjectivesFunctions.wastedResources(physicalMachineList, null);
        Utils.updateDerivedVMs(virtualMachineList, derivedVMs);
        economicalRevenue = ObjectivesFunctions.economicalRevenue(virtualMachineList, derivedVMs, null);
        //将目标函数值设置到individual对象中
        individual.getObjectiveFunctions()[0] = powerConsumption;
        individual.getObjectiveFunctions()[1] = economicalRevenue;
        individual.getObjectiveFunctions()[2] = wastedResources;
        individual.getObjectiveFunctions()[3] = memoryMigrated;

    }
    //载入适应度值
    @Override
    public void loadFitness(Individual individual, List<APrioriValue> aPrioriValuesList, int numberOfObjFunctions) {

        int iteratorObjFunctions;
        APrioriValue aPrioriValue;
        List<Float> normalizedOjbFunctions = new ArrayList<>();
        Float normalizedValue;
        //标准化目标函数的值
        for (iteratorObjFunctions = 0; iteratorObjFunctions < numberOfObjFunctions; iteratorObjFunctions++) {
            aPrioriValue = aPrioriValuesList.get(iteratorObjFunctions);
            normalizedValue = Utils.normalizeValue(individual.getObjectiveFunctions()[iteratorObjFunctions], aPrioriValue.getMinValue(), aPrioriValue.getMaxValue());
            normalizedOjbFunctions.add(iteratorObjFunctions, normalizedValue);
        }
        // 通过Scalarization Method计算适应度
        Float distance = ObjectivesFunctions.getScalarizationMethod(normalizedOjbFunctions, Constant.WEIGHT_OFFLINE);
        individual.setFitness(distance);
    }
    //载入资源利用率
    @Override
    public void loadUtilization(Population population, List<VirtualMachine> virtualMachineList, int numberOfResources) {
        int iteratorSolution;
        int iteratorResource;
        int physicalMachineId;
        VirtualMachine vm;
        //遍历种群中的每个个体
        for (Individual individual : population.getIndividuals()) {
            //遍历个体的每个解
            for (iteratorSolution = 0; iteratorSolution < individual.getSize(); iteratorSolution++) {
                physicalMachineId = individual.getSolution()[iteratorSolution];
                if (physicalMachineId != 0) {
                    vm = virtualMachineList.get(iteratorSolution);
                    //更新资源利用率
                    for (iteratorResource = 0; iteratorResource < numberOfResources; iteratorResource++) {
                        individual.getUtilization()[physicalMachineId - 1][iteratorResource] += vm.getResources().get(iteratorResource) * (vm.getUtilization().get(iteratorResource) / 100);
                    }
                }
            }
        }
    }

}