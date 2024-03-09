package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.Placement;
import org.domain.VirtualMachine;
import org.framework.Utils;

import java.util.List;
public class MoMaVMP {


    /**
     * 对实际放置进行重新配置，使用Memetic算法。
     * @param actualPlacement   当前的虚拟机放置
     * @param aPrioriValueList  先验值列表
     * @param settings          Memetic算法的设置
     * @return 重新配置后的放置
     */
    public Placement reconfiguration(Placement actualPlacement, List<APrioriValue> aPrioriValueList, MASettings settings) {
        // 通过搜索得到选择的个体
        Individual individualSelected = this.search(actualPlacement.getVirtualMachineList(), actualPlacement.getDerivedVMs(),
                actualPlacement.getPhysicalMachines(), aPrioriValueList, settings);
        // 复制原始的虚拟机、派生虚拟机和物理机列表
        List<VirtualMachine> newVirtualMachineList = VirtualMachine.cloneVMsList(actualPlacement.getVirtualMachineList());
        newVirtualMachineList = individualSelected.convertToVMList(newVirtualMachineList);
        List<VirtualMachine> newDerivedVMs = VirtualMachine.cloneVMsList(actualPlacement.getDerivedVMs());
        Utils.updateDerivedVMs(newVirtualMachineList, newDerivedVMs);
        List<PhysicalMachine> newPhysicalMachineList = PhysicalMachine.clonePMsList(actualPlacement.getPhysicalMachines());
        newPhysicalMachineList = individualSelected.convertToPMList(newPhysicalMachineList, settings.getNumberOfResources());
        // 创建新的放置对象
        Placement newPlacement = new Placement(newPhysicalMachineList, newVirtualMachineList, newDerivedVMs);
        // 计算放置分数并设置
        Float placementScore = Utils.calcPlacemenScore(individualSelected.getObjectiveFunctions(), aPrioriValueList);
        newPlacement.setPlacementScore(placementScore);
        return newPlacement;
    }

    /**
     * Memetic算法中的搜索过程。
     * @param virtualMachineList    虚拟机列表
     * @param derivedVMs           派生虚拟机列表
     * @param physicalMachineList   物理机列表
     * @param aPrioriValuesList     先验值列表
     * @param settings             Memetic算法的设置
     * @return 选择的个体
     */
    public Individual search(List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs,
                             List<PhysicalMachine> physicalMachineList, List<APrioriValue> aPrioriValuesList, MASettings settings) {
        // 初始化算子
        Selection selectionOperator = new TournamentSelection();
        Crossover crossoverOperator = new OnePointCrossover(settings.getCrossoverProb());
        Mutation mutationOperator = new UniformMutation();
        Initialization initialization = new Initialization();
        FitnessEvaluation fitnessEvaluator = new EvaluationByScalarizationMethod();
        Population populationQ, populationP;
        int generation = 0;
        // 初始化初始种群
        populationP = initialization.initialize(virtualMachineList.size(), physicalMachineList.size(), settings);
        // 加载初始种群的资源利用信息
        fitnessEvaluator.loadUtilization(populationP, virtualMachineList, settings.getNumberOfResources());
        // 修复初始种群
        populationP = Reparation.repairPopulation(populationP, virtualMachineList, physicalMachineList,
                settings.getNumberOfResources());
        // 计算初始种群的适应度值
        fitnessEvaluator.evaluate(populationP, virtualMachineList, derivedVMs, physicalMachineList, aPrioriValuesList,
                settings.getNumberOfResources(), settings.getNumberOfObjFunctions());
        //进入循环迭代
        while (generation < settings.getNumberOfGenerations()) {
            // 选择父代个体
            List<Individual> parents = selectionOperator.select(populationP, populationP.size());
            // 交叉操作生成子代种群
            populationQ = crossoverOperator.crossover(parents, populationP.size());
            // 变异操作对子代种群进行变异
            populationQ = mutationOperator.mutate(populationQ);
            // 加载子代种群的资源利用信息
            fitnessEvaluator.loadUtilization(populationQ, virtualMachineList, settings.getNumberOfResources());
            // 修复子代种群
            populationQ = Reparation.repairPopulation(populationQ, virtualMachineList, physicalMachineList,
                    settings.getNumberOfResources());
            // 计算子代种群的适应度值
            fitnessEvaluator.evaluate(populationQ, virtualMachineList, derivedVMs, physicalMachineList, aPrioriValuesList,
                    settings.getNumberOfResources(), settings.getNumberOfObjFunctions());
            // 获取下一代种群
            populationP = Evolution.getNextGeneration(populationP, populationQ);
            // 更新迭代次数
            generation += 1;
        }
        //返回选择的个体
        return populationP.getIndividual(0);
    }


}
