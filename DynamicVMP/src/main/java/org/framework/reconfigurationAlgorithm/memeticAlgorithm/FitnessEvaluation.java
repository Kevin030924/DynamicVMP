package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.APrioriValue;
import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import java.util.List;
//定义一组方法，用于评估个体的适应度和相关的信息
public interface FitnessEvaluation {
    /**
     * 评估整个群体的适应度，根据给定的信息计算每个个体的适应度值
     * @param population Population对象，包含待评估的个体群体
     * @param virtualMachineList 虚拟机的列表
     * @param derivedVMs 派生虚拟机的列表
     * @param physicalMachineList 物理机的列表
     * @param aPrioriValuesList 先验值的列表
     * @param numberOfResources 资源的数量
     * @param numberOfObjFunctions 目标函数的数量
     */
    void evaluate(Population population, List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs,
                  List<PhysicalMachine> physicalMachineList, List<APrioriValue> aPrioriValuesList, int numberOfResources,
                  int numberOfObjFunctions);
    /**
     * 计算个体的资源利用率，以用于适应度的评估
     * @param population Population对象，包含待评估的个体群体。
     * @param virtualMachineList 虚拟机的列表。
     * @param numberOfResources 资源的数量。
     */
    void loadUtilization(Population population, List<VirtualMachine> virtualMachineList, int numberOfResources);

    /**
     * 计算个体的目标函数值，以用于适应度的评估
     * @param individual 一个个体
     * @param virtualMachineList 虚拟机的列表
     * @param previousVirtualMachineList 前一个时间步的虚拟机列表
     * @param derivedMVs 派生虚拟机的列表
     * @param physicalMachineList 物理机的列表
     * @param numberOfResources 资源的数量
     */
    void loadObjectiveFunctions(Individual individual, List<VirtualMachine> virtualMachineList,
                                List<VirtualMachine> previousVirtualMachineList, List<VirtualMachine> derivedMVs,
                                List<PhysicalMachine> physicalMachineList, int numberOfResources);

    /**
     * 计算个体的适应度值，以用于适应度的评估
     * @param individual 一个Individual对象，表示一个个体
     * @param aPrioriValuesList 先验值的列表
     * @param numberOfObjFunctions 目标函数的数量
     */
    void loadFitness(Individual individual, List<APrioriValue> aPrioriValuesList, int numberOfObjFunctions);
}
