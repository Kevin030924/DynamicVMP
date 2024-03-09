package org.framework.algorithm.thresholdBasedApproach;

import org.domain.*;
import org.framework.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.domain.VirtualMachine.getById;

/**
 *
 * <b>算法2：基于阈值的方法</b>
 * <p>
 *     当数据中心处于负载不足或过载时启动迁移。
 * </p>
 *
 */
public class ThresholdBasedApproach {

    private ThresholdBasedApproach() {
        // 默认构造函数
    }

    /**
     * VMP管理器
     * @param workload                   工作负载跟踪
     * @param physicalMachines           物理机器列表
     * @param virtualMachines            虚拟机器列表
     * @param derivedVMs                 派生虚拟机器列表
     * @param revenueByTime              按时间计算的收入
     * @param wastedResources            按时间计算的浪费资源
     * @param wastedResourcesRatioByTime 按时间计算的浪费资源比率
     * @param powerByTime                按时间计算的能耗
     * @param placements                 按时间计算的放置列表
     * @param code                       启发式算法代码
     * @param timeUnit                   时间单位
     * @param requestsProcess            进程类型
     * @param maxPower                   最大功耗
     * @param scenarioFile               场景名称
     *
     * <b>RequestsProcess</b>:
     *  <ul>
     *      <li>Requests[0]: requestServed 已服务的请求数量</li>
     *      <li>Requests[1]: requestRejected 拒绝的请求数量</li>
     *      <li>Requests[2]: requestUpdated 更新的请求数量</li>
     *      <li>Requests[3]: violation 违规数量</li>
     *  </ul>
     *
     * @throws IOException          文件管理错误
     * @throws InterruptedException 多线程错误
     * @throws ExecutionException   多线程错误
     */
    public static void thresholdBasedApproachManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
                                                     List<VirtualMachine>
                                                             virtualMachines, List<VirtualMachine> derivedVMs,
                                                     Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
                                                     Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
                                                     Integer[] requestsProcess, Float maxPower, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        List<VirtualMachine> vmsToMigrateFromPM = new ArrayList<>();
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        List<Integer> vmsMigrationEndTimes = new ArrayList<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Boolean isMigrationActive = false;
        Boolean isUpdateVmUtilization = false;
        Integer actualTimeUnit;
        Integer nextTimeUnit;
        Integer migrationTimeEnd=-1;
        Integer vmEndTimeMigration = 0;

        Integer heuristicCode = Constant.HEURISTIC_MAP.get(Constant.FFD);

        for (int iterator = 0; iterator < workload.size(); ++iterator) {
            Scenario request = workload.get(iterator);
            actualTimeUnit = request.getTime();
            // 如果是最后一个请求，则将nextTimeUnit赋值为-1。
            nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

            // 检查请求是否对应于正在迁移的虚拟机
            if (nextTimeUnit!= -1 && isMigrationActive && DynamicVMP.isVmBeingMigrated(request.getVirtualMachineID(),
                    request.getCloudServiceID(), vmsToMigrate)) {

                VirtualMachine vmMigrating = getById(request.getVirtualMachineID(), request.getCloudServiceID(),
                        virtualMachines);
                vmEndTimeMigration = Utils.updateVmEndTimeMigration(vmsToMigrate, vmsMigrationEndTimes,
                        vmEndTimeMigration,
                        vmMigrating);

                /* 检查正在考虑的虚拟机的迁移时间。
                 * 如果虚拟机正在迁移，则添加利用率开销。
                 */
                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }

            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess,
                    isUpdateVmUtilization);

            // 检查是否是最后一个请求或将发生时间单位的变化。
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {
                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

                // 打印时间t的放置分数
                Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
                // 打印时间t的放置分数
                Utils.printToFile( Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile, placementScore);

                timeUnit = actualTimeUnit;

                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);

                // 如果虚拟机迁移操作未激活，则检查物理机器的状态
                if(nextTimeUnit!=-1 && !isMigrationActive) {

                    // 包含分配给特定物理机器的虚拟机器。
                    List<VirtualMachine> vmsInPM;

                    for (PhysicalMachine pm : physicalMachines) {
                        vmsInPM = Utils.filterVMsByPM(virtualMachines, pm.getId());
                        if (Constraints.isPMOverloaded(pm) && !vmsInPM.isEmpty()) {
                            vmsToMigrateFromPM.clear();
                            // 物理机器过载，选择要从该物理机器迁移的虚拟机
                            vmsToMigrateFromPM = Utils.getVMsToMigrate(pm,vmsInPM);
                            // 移动选择的虚拟机
                            DynamicVMP.runHeuristics(heuristicCode,physicalMachines,virtualMachines,derivedVMs,
                                    vmsToMigrateFromPM);
                            // 将选择的虚拟机添加到迁移列表中
                            vmsToMigrate.addAll(vmsToMigrateFromPM);
                        } else if (Constraints.isPMUnderloaded(pm) && !vmsInPM.isEmpty()) {
                            vmsToMigrateFromPM.clear();
                            // 物理机器负载不足，从该物理机器移动所有虚拟机
                            vmsToMigrateFromPM.addAll(vmsInPM);
                            // 移动虚拟机
                            DynamicVMP.runHeuristics(heuristicCode,physicalMachines,virtualMachines,
                                    derivedVMs,vmsToMigrateFromPM);
                            // 将虚拟机添加到迁移列表中
                            vmsToMigrate.addAll(vmsToMigrateFromPM);
                        }
                    }

                    if(!vmsToMigrate.isEmpty()){
                        // 如果有要迁移的虚拟机，则获取每个要迁移的虚拟机的迁移时间
                        vmsMigrationEndTimes  = Utils.getTimeEndMigrationByVM(vmsToMigrate,actualTimeUnit);
                        // 迁移的最终时间
                        migrationTimeEnd = Utils.getMigrationEndTime(vmsMigrationEndTimes);
                        isMigrationActive = true;
                    }else{
                        // 清理迁移变量
                        vmsToMigrate.clear();
                        vmsMigrationEndTimes.clear();
                        isMigrationActive = false;
                    }

                }else if(nextTimeUnit!=-1 && actualTimeUnit.equals(migrationTimeEnd)){
                    // 清理迁移变量
                    vmsToMigrate.clear();
                    vmsMigrationEndTimes.clear();
                    isMigrationActive = false;
                }
            }
        }
        Utils.executorServiceTermination(executorService);
    }

}
