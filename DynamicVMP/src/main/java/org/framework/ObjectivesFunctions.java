package org.framework;

import org.domain.*;
import org.framework.iterativeAlgorithm.Heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObjectivesFunctions {

    /**
     * 最小功耗百分比
     * 面向能效的资源分配启发式算法，用于云计算中数据中心的高效管理。
     * </p>
     */
    public static final Float MIN_POWER_PERCENTAGE = 0.6F;

    /**
     * 最小功耗为 0（所有 PM 均已关闭）
     */
    static final Float MIN_POWER = 0F;

    /**
     * 最小收入为 0（所有 VM 均已提供服务）
     */
    static final Float MIN_REVENUE = 0F;

    private ObjectivesFunctions() {
        // Default Constructor
    }

    /**
     * 计算功耗
     *
     * @param physicalMachines 物理机列表
     * @return 总功耗
     */
    public static Float powerConsumption(List<PhysicalMachine> physicalMachines) {

        Float utilidad;
        Float powerConsumption = 0F;

        for (PhysicalMachine pm : physicalMachines) {
            if (pm.getResourcesRequested().get(0) > 0.0001) {
                // 计算资源利用率
                utilidad = pm.getResourcesRequested().get(0) / pm.getResources().get(0);
                //计算功耗
                powerConsumption += (pm.getPowerMax() - pm.getPowerMax() * MIN_POWER_PERCENTAGE)
                        * utilidad + pm.getPowerMax() * MIN_POWER_PERCENTAGE;
            }
        }
        return powerConsumption;
    }

    /**
     * 计算经济收益
     *
     * @param virtualMachines 虚拟机列表
     * @param derivedVMs      衍生虚拟机列表
     * @param timeUnit        时间单位
     * @return 总经济收益
     */
    public static Float economicalRevenue(List<VirtualMachine> virtualMachines, List<VirtualMachine>
            derivedVMs, Integer timeUnit) {

        Float totalRevenue = 0F;
        Float violationRevenue = 0F;

        Violation violation;
        Resources resources;

        for (VirtualMachine vm : virtualMachines) {

            // 获取每个虚拟机每个时间单位的违规（如果存在）
            violation = DynamicVMP.unsatisfiedResources.get(vm.getId());
            if (violation != null && timeUnit != null) {
                resources = violation.getResourcesViolated().get(timeUnit);
                // 获取违规资源
                if(resources != null) {
                    violationRevenue += resources.getCpu() * vm.getRevenue().getCpu();
                    violationRevenue += resources.getRam() * vm.getRevenue().getRam();
                    violationRevenue += resources.getNet() * vm.getRevenue().getNet();
                }
                totalRevenue += violationRevenue;
                violationRevenue = 0F;
            }
        }

        for (VirtualMachine dvm : derivedVMs) {
            totalRevenue += dvm.getResources().get(0) * dvm.getRevenue().getCpu() * Parameter.DERIVE_COST;
            totalRevenue += dvm.getResources().get(1) * dvm.getRevenue().getRam() * Parameter.DERIVE_COST;
            totalRevenue += dvm.getResources().get(2) * dvm.getRevenue().getNet() * Parameter.DERIVE_COST;
        }

        return totalRevenue;
    }

    /**
     * 计算浪费的资源比例
     *
     * @param physicalMachines 工作中的物理机列表
     * @param wastedResources  存储浪费资源的列表
     * @return 浪费的资源比例
     */
    public static Float wastedResources(List<PhysicalMachine> physicalMachines,
            List<Resources> wastedResources) {

        float wastedCPU = 0F;
        float wastedRAM = 0F;
        float wastedNET = 0F;

        float wastedCpuResourcesRatio;
        float wastedRamResourcesRatio;
        float wastedNetResourcesRatio;

        float alpha = 1F;
        float beta = 1F;
        float gamma = 1F;
        float wastedResourcesRatio;

        int workingPms = 0;

        for (PhysicalMachine pm : physicalMachines) {

            if (pm.getResourcesRequested().get(0) > 0.0001
                    || pm.getResourcesRequested().get(1) > 0.0001
                    || pm.getResourcesRequested().get(2) > 0.0001) {

                workingPms++;
                float wcpu = 1 - pm.getResourcesRequested().get(0) / pm.getResources().get(0);
                float wram = 1 - pm.getResourcesRequested().get(1) / pm.getResources().get(1);
                float wnet = 1 - pm.getResourcesRequested().get(2) / pm.getResources().get(2);

                if(wcpu > 0) {
                    wastedCPU += wcpu;
                } else {
                    wastedCPU += 0;
                }

                if(wram > 0) {
                    wastedRAM += wram;
                } else {
                    wastedRAM += 0;
                }

                if(wnet > 0) {
                    wastedNET += wnet;
                } else {
                    wastedNET += 0;
                }

            }
        }

        // 如果没有工作的 PM，则返回 0
        if (workingPms == 0) {
            return 0F;
        }

        // 所有 PM 的总浪费资源 / 工作的 PM 数量
        wastedCpuResourcesRatio = wastedCPU / workingPms;
        wastedRamResourcesRatio = wastedRAM / workingPms;
        wastedNetResourcesRatio = wastedNET / workingPms;

        Resources wasted = new Resources(wastedCpuResourcesRatio, wastedRamResourcesRatio, wastedNetResourcesRatio);

        if (wastedResources != null) {
            wastedResources.add(wasted);
        }

        // 将浪费资源比例相加并除以考虑的资源数量（在此情况下为 3）
        wastedResourcesRatio = (
                wastedCpuResourcesRatio * alpha
                + wastedRamResourcesRatio * beta
                + wastedNetResourcesRatio * gamma ) / 3;

        return wastedResourcesRatio;
    }

    /**
     * 计算迁移的虚拟机数量
     *
     * @param oldVirtualMachineList 旧的虚拟机列表
     * @param newVirtualMachineList 新的虚拟机列表
     * @return 迁移的虚拟机数量
     */
    public static Float migrationCount(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList){
        int iterator;
        int oldPosition;
        int newPosition;
        float migrationCounter = 0;
        for(iterator=0;iterator<oldVirtualMachineList.size();iterator++){
            oldPosition = oldVirtualMachineList.get(iterator).getPhysicalMachine();
            newPosition = newVirtualMachineList.get(iterator).getPhysicalMachine();
            // 如果旧位置和新位置不同且新位置不为 0，则增加迁移计数
            if(oldPosition!=newPosition && newPosition!=0){
                migrationCounter+=1;
            }
        }

        return migrationCounter;
    }

    /**
     * 计算内存迁移总量
     *
     * @param oldVirtualMachineList 旧的虚拟机列表
     * @param newVirtualMachineList 新的虚拟机列表
     * @return 内存迁移总量
     */
    public static Float memoryMigrated(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList){
        int iterator;
        int oldPosition;
        int newPosition;
        float memoryMigrated = 0;
        for(iterator=0;iterator<oldVirtualMachineList.size();iterator++){
            oldPosition = oldVirtualMachineList.get(iterator).getPhysicalMachine();
            newPosition = newVirtualMachineList.get(iterator).getPhysicalMachine();
            // 如果旧位置和新位置不同且新位置不为 0，则增加内存迁移总量
            if(oldPosition!=newPosition && newPosition!=0){
                memoryMigrated+=newVirtualMachineList.get(iterator).getResources().get(1);
            }
        }

        return memoryMigrated;
    }

    /**
     * 计算两个物理机之间迁移的内存总量
     *
     * @param oldVirtualMachineList 旧的虚拟机列表
     * @param newVirtualMachineList 新的虚拟机列表
     * @param numberOfPMs 物理机的数量
     * @return 内存迁移的最大总量
     */
    public static Float migratedMemoryBtwPM(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList, Integer numberOfPMs){

        // 获取物理机之间内存迁移矩阵
        Float[][] migrationMatrix  = Utils.getMigratedMemoryByPM(oldVirtualMachineList,newVirtualMachineList,numberOfPMs);
        // 设置最大内存迁移总量初始值
        Float maxMigratedMemory = 0F;
        for(int iteratorRow=0;iteratorRow<numberOfPMs;iteratorRow++){
            for(int iteratorColumn=0;iteratorColumn<numberOfPMs;iteratorColumn++){
                // 检查两个物理机之间的内存迁移总量
                if(migrationMatrix[iteratorRow][iteratorColumn] > maxMigratedMemory){
                    maxMigratedMemory = migrationMatrix[iteratorRow][iteratorColumn];
                }
            }
        }
        return maxMigratedMemory;
    }

    /**
     * 获取标量化方法的值
     *
     * @param objFunctValues 目标函数值列表
     * @param weight         权重值
     * @return 标量化方法的值
     */
    public static Float getScalarizationMethod(List<Float> objFunctValues, Float weight){

        if("ED".equals(Parameter.SCALARIZATION_METHOD)) {
            return getEuclideanDistance(objFunctValues);
        } else if("CD".equals(Parameter.SCALARIZATION_METHOD)) {
            return getChebyshevDistance(objFunctValues);
        } else if("MD".equals(Parameter.SCALARIZATION_METHOD)) {
            return getManhattanDistance(objFunctValues);
        } else {
            return getWeightedSum(objFunctValues, weight);
        }

    }

    /**
     * 计算欧几里得距离
     *
     * @param objFunctValues 目标函数值列表
     * @return 欧几里得距离
     */
    private static Float getEuclideanDistance(final List<Float> objFunctValues) {

        float tempSum = 0;
        for (Float objFunctValue : objFunctValues) {
            // 对每个目标函数值进行平方求和
            tempSum += Math.pow(objFunctValue,2);
        }
        Double distance = Math.sqrt(tempSum);
        return distance.floatValue();
    }

    /**
     * 计算切比雪夫距离
     *
     * @param objFunctValues 目标函数值列表
     * @return 切比雪夫距离
     */
    private static Float getChebyshevDistance(final List<Float> objFunctValues) {
        // 返回目标函数值列表中的最大值
        return objFunctValues.stream().max(Float::compareTo).get();
    }

    /**
     * 计算曼哈顿距离
     *
     * @param objFunctValues 目标函数值列表
     * @return 曼哈顿距离
     */
    private static Float getManhattanDistance(final List<Float> objFunctValues) {
        // 将目标函数值列表中的值相加
        Double d = objFunctValues.stream().mapToDouble(Float::doubleValue).sum();
        return d.floatValue();

    }

    /**
     * 计算加权和
     *
     * @param objFunctValues 目标函数值列表
     * @param weight         权重
     * @return 加权和
     */
    private static Float getWeightedSum(final List<Float> objFunctValues, Float weight) {
        float tempSum = 0F;
        for (Float objFunctValue : objFunctValues) {
            // 对每个目标函数值进行加权求和
            tempSum += weight * objFunctValue;
        }
        return tempSum;
    }

    /**
     * 根据时间获取目标函数值
     *
     * @param physicalMachines        物理机列表
     * @param virtualMachines         虚拟机列表
     * @param derivedVMs              派生虚拟机列表
     * @param wastedResources         浪费资源列表
     * @param wastedResourcesRatioByTime 按时间的浪费资源比例映射
     * @param powerByTime             按时间的功耗映射
     * @param revenueByTime           按时间的经济收益映射
     * @param timeUnit                时间单元
     * @param currentTimeUnit         当前时间单元
     */
    public static void getObjectiveFunctionsByTime(List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime,  Map<Integer, Float> revenueByTime,
            Integer timeUnit, Integer currentTimeUnit ) {
        // 移除上一个时间单元的虚拟机
        Heuristics.removeVMByTime(virtualMachines, timeUnit, physicalMachines);
        Heuristics.removeDerivatedVMByTime(derivedVMs, timeUnit);
        // 计算经济收益
        revenueByTime.put(currentTimeUnit, ObjectivesFunctions
                .economicalRevenue(virtualMachines, derivedVMs, currentTimeUnit));
        // 计算功耗
        powerByTime.put(currentTimeUnit, ObjectivesFunctions.powerConsumption(physicalMachines));
        // 计算浪费资源比例
        wastedResourcesRatioByTime
                .put(currentTimeUnit, ObjectivesFunctions.wastedResources(physicalMachines, wastedResources));

    }

    /**
     * 获取放置的当前得分
     * <p>
     * 标准化目标函数的值，并使用 {@link Parameter#SCALARIZATION_METHOD} 将它们合并为一个值（放置分数）。
     * </p>
     *
     * @param timeUnit                   时间单元
     * @param maxPower                   由 PM 可能消耗的最大功耗
     * @param wastedResourcesRatioByTime 按时间 t 的浪费资源比例
     * @param powerByTime                按时间 t 的功耗
     * @param revenueByTime              按时间 t 的收益
     * @return 在时间 t 的到原点的距离
     */
    public static Float getDistanceOrigenByTime (Integer timeUnit, Float maxPower,  Map<Integer, Float> powerByTime,
            Map<Integer, Float> revenueByTime,  Map<Integer, Float> wastedResourcesRatioByTime) {

        // 所有结果在每个时间 t 的总和。 （已标准化）
        Float powerConsumptionResult = 0F;
        Float revenueResult = 0F;
        Float wastedResourcesResult = 0F;
        Float normalizedPowerConsumption;
        Float normalizedRevenue;
        // 如果功耗为空，则将功耗设置为零
        if (powerByTime.get(timeUnit) == null ) {
            powerByTime.put(timeUnit, 0F);
            revenueByTime.put(timeUnit, 0F);
            wastedResourcesRatioByTime.put(timeUnit, 0F);
        }
        // 功耗
        normalizedPowerConsumption = Utils.normalizeValue(powerByTime.get(timeUnit), MIN_POWER, maxPower);
        // 收益
        if(revenueByTime.get(timeUnit) != null && revenueByTime.get(timeUnit) > 0) {
            normalizedRevenue = Utils.normalizeValue(revenueByTime.get(timeUnit), MIN_REVENUE,
                    DynamicVMP.revenueAprioriTime.get(timeUnit));

        }else{
            normalizedRevenue = 0F;
        }

        powerConsumptionResult += normalizedPowerConsumption;
        revenueResult += normalizedRevenue;
        wastedResourcesResult += wastedResourcesRatioByTime.get(timeUnit);

        List<Float> objectiveFunctionsResult = new ArrayList<>();
        objectiveFunctionsResult.add(powerConsumptionResult);
        objectiveFunctionsResult.add(revenueResult);
        objectiveFunctionsResult.add(wastedResourcesResult);

        return ObjectivesFunctions.getScalarizationMethod(objectiveFunctionsResult, Constant.WEIGHT_ONLINE);
    }

    /**
     * 加载目标函数值
     *
     * @param virtualMachineList   虚拟机列表
     * @param derivedVMs           派生虚拟机列表
     * @param physicalMachineList  物理机列表
     * @return 包含功耗、经济收益和浪费资源的目标函数值的数组
     */
    public static Float[] loadObjectiveFunctions(List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs, List<PhysicalMachine> physicalMachineList){

		Float[] objectiveFunctions = new Float[Constant.NUM_OBJ_FUNCT_COMP];
        //功耗
	    objectiveFunctions[0]=ObjectivesFunctions.powerConsumption(physicalMachineList);
        //经济收益
        objectiveFunctions[1]=ObjectivesFunctions.economicalRevenue(virtualMachineList,derivedVMs,null);
        //浪费资源
        objectiveFunctions[2]= ObjectivesFunctions.wastedResources(physicalMachineList,null);
	    return objectiveFunctions;

    }

    /**
     * 计算场景得分
     *
     * @param revenueByTime 按时间单位的收益映射
     * @param placements     放置映射
     * @param realRevenue    实际总收益数组（索引0处的值将存储总收益）
     * @return 场景得分
     */
    public static Float getScenarioScore( Map<Integer, Float> revenueByTime, Map<Integer, Placement> placements,
            final Float[] realRevenue) {

        // 计算总收益
        for (Map.Entry<Integer, Float> entry : revenueByTime.entrySet()) {
            realRevenue[0] += entry.getValue();
        }

        // 计算场景得分
        Float scenarioScored = 0F;
        for (Map.Entry<Integer, Placement> entry : placements.entrySet()) {
            scenarioScored += entry.getValue().getPlacementScore();
        }
        return scenarioScored;
    }
}

