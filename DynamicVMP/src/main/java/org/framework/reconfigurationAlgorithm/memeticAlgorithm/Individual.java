package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.framework.Utils;

import java.util.Arrays;
import java.util.List;
//个体类
public class Individual {
    private Integer[] solution;//虚拟机分配给物理机的解决方案
    private Float[][] utilization;//每台物理机上的资源利用
    private Float[] objectiveFunctions;//各个目标函数的值
    private Float fitness;//适应度值

    public Individual() {
    }
    /**
     * 构造函数，用于使用指定的维度初始化个体。
     * @param numberOfObjFuncts 目标函数的数量。
     * @param numberOfVMs 虚拟机的数量。
     * @param numberOfPMs 物理机的数量。
     * @param numberOfRes 资源的数量。
     */
    public Individual(Integer numberOfObjFuncts, Integer numberOfVMs, Integer numberOfPMs, Integer numberOfRes) {
        this.solution = new Integer[numberOfVMs];
        this.utilization = new Float[numberOfPMs][numberOfRes];
        this.objectiveFunctions = new Float[numberOfObjFuncts];
        this.utilization = new Float[numberOfPMs][numberOfRes];
        Utils.initializeMatrix(utilization, numberOfPMs, numberOfRes);
        this.fitness = 0F;
    }
    //受保护的构造函数，用于复制个体
    protected Individual(Individual individual) {
        Integer numberOfVMs = individual.getSolution().length;
        Integer numberOfObjFuncts = individual.getObjectiveFunctions().length;
        Integer numberOfPMs = individual.getUtilization().length;
        Integer numberOfRes = individual.getUtilization()[0].length;
        this.solution = new Integer[numberOfVMs];
        this.objectiveFunctions = new Float[numberOfObjFuncts];
        this.utilization = new Float[numberOfPMs][numberOfRes];
        Utils.initializeMatrix(utilization, numberOfPMs, numberOfRes);
        this.solution = Arrays.copyOf(individual.getSolution(), numberOfVMs);
    }
    //返回适应度值
    public double getFitness() {
        return fitness;
    }
    //设置适应度值
    public void setFitness(Float fitness) {
        this.fitness = fitness;
    }
    //获取解决方案
    public Integer[] getSolution() {
        return solution;
    }
    //设置解决方案
    public void setSolution(Integer[] solution) {
        this.solution = solution;
    }
    //获取资源利用值
    public Float[][] getUtilization() {
        return utilization;
    }
    //设置资源利用值
    public void setUtilization(Float[][] utilization) {
        this.utilization = utilization;
    }
    //获取目标函数的值
    public Float[] getObjectiveFunctions() {
        return objectiveFunctions;
    }
    //设置目标函数的值
    public void setObjectiveFunctions(Float[] objectiveFunctions) {
        this.objectiveFunctions = objectiveFunctions;
    }
    //复制个体
    public Individual copy() {
        return new Individual(this);
    }
    //获取个体大小
    public Integer getSize() {
        return this.getSolution().length;
    }
    //将个体转换为虚拟机列表
    public List<VirtualMachine> convertToVMList(List<VirtualMachine> virtualMachineList) {
        Integer iteratorSolution, physicalMachine;
        for (iteratorSolution = 0; iteratorSolution < this.getSize(); iteratorSolution++) {
            physicalMachine = this.getSolution()[iteratorSolution];
            virtualMachineList.get(iteratorSolution).setPhysicalMachine(physicalMachine);
        }
        return virtualMachineList;
    }
    //将个体转换为物理机列表
    public List<PhysicalMachine> convertToPMList(List<PhysicalMachine> physicalMachineList, Integer numberOfResources) {
        Integer iteratorPhysical, iteratorResources;
        Float utilizationOfResource, utilizationPercentage, resource;
        for (iteratorPhysical = 1; iteratorPhysical <= physicalMachineList.size(); iteratorPhysical++) {
            for (iteratorResources = 0; iteratorResources < numberOfResources; iteratorResources++) {
                resource = PhysicalMachine.getById(iteratorPhysical, physicalMachineList).getResources().get(iteratorResources);
                utilizationOfResource = this.getUtilization()[iteratorPhysical - 1][iteratorResources];
                utilizationPercentage = (utilizationOfResource / resource) * 100;
                PhysicalMachine.getById(iteratorPhysical, physicalMachineList).getResourcesRequested().set(iteratorResources, utilizationOfResource);
                PhysicalMachine.getById(iteratorPhysical, physicalMachineList).getUtilization().set(iteratorResources, utilizationPercentage);
            }
        }
        return physicalMachineList;
    }
}