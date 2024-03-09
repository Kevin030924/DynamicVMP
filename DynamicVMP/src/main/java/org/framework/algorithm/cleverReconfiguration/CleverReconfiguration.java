package org.framework.algorithm.cleverReconfiguration;

import org.domain.*;
import org.framework.*;
import org.framework.reconfigurationAlgorithm.concurrent.StaticReconfMemeCall;
import org.framework.reconfigurationAlgorithm.memeticAlgorithm.MASettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.domain.VirtualMachine.getById;

/**
 * <b>算法3：基于更新的VMPr恢复</b>
 * <p>
 *     基于部分重新计算VMPr计算的放置重新配置的恢复方法，根据放置计算期间发生的变化，
 *     应用基本操作来更新可能过时的放置。
 * </p>
 *
 */
public class CleverReconfiguration {

    private static Logger logger = DynamicVMP.getLogger();

    private CleverReconfiguration() {
        // 默认构造函数
    }

    /**
     * 重配置VMP
     * @param workload                   工作负载跟踪
     * @param physicalMachines           物理机器列表
     * @param virtualMachines            虚拟机器器列表
     * @param derivedVMs                 派生虚拟机器列表
     * @param revenueByTime              按时间计算的收入
     * @param wastedResources            按时间计算的浪费资源
     * @param wastedResourcesRatioByTime 按时间计算的浪费资源比率
     * @param powerByTime                按时间计算的能耗
     * @param placements                 按时间计算的放置列表
     * @param code                       启发式算法代码
     * @param timeUnit                   时间初始化
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
    public static void cleverReconfigurationgManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
                                                     List<VirtualMachine>
                                                             virtualMachines, List<VirtualMachine> derivedVMs,
                                                     Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
                                                     Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
                                                     Integer[] requestsProcess, Float maxPower, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        List<APrioriValue> aPrioriValuesList = new ArrayList<>();
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        List<Integer> vmsMigrationEndTimes = new ArrayList<>();
        List<Float> valuesSelectedForecast = new ArrayList<>();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        MASettings memeConfig = Utils.getMemeConfig(true);
        Callable<Placement> staticReconfgTask;
        Future<Placement> reconfgResult = null;
        Placement reconfgPlacementResult = null;

        Boolean isMigrationActive = false;
        Boolean isUpdateVmUtilization = false;
        Boolean isReconfigurationActive = false;
        Integer actualTimeUnit;
        Integer nextTimeUnit;

        Integer reconfigurationTimeInit = -1;
        Integer reconfigurationTimeEnd=-1;

        Integer migrationTimeInit=-1;
        Integer migrationTimeEnd=-1;

        Integer vmEndTimeMigration = 0;

        for (int iterator = 0; iterator < workload.size(); ++iterator) {
            Scenario request = workload.get(iterator);
            actualTimeUnit = request.getTime();
            // 如果是最后一个请求，则将nextTimeUnit赋值为-1。
            nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

            // 检查是否需要迁移引起的过载
            if (nextTimeUnit!= -1 && isMigrationActive && DynamicVMP.isVmBeingMigrated(request.getVirtualMachineID(),
                    request.getCloudServiceID(), vmsToMigrate)){

                // 获取正在迁移的虚拟机
                VirtualMachine vmMigrating = getById(request.getVirtualMachineID(),request.getCloudServiceID(),
                        virtualMachines);

                // 检查虚拟机迁移结束的时间
                vmEndTimeMigration = Utils.updateVmEndTimeMigration(vmsToMigrate, vmsMigrationEndTimes,
                        vmEndTimeMigration,
                        vmMigrating);

                // 添加或不添加到CPU利用率的重载的布尔条件
                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }
            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess,
                    isUpdateVmUtilization);

            // 检查是否是最后一个请求或时间单元的变化是否发生。
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {

                // 获取目标函数
                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);
                // 根据距离原点方法获取放置得分
                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

                DynamicVMP.updateLeasingCosts(derivedVMs);
                Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
                // 将时间t的放置得分打印到文件中
                Utils.printToFile( Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile, placementScore);

                timeUnit = actualTimeUnit;

                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);

                // 检查历史信息
                if(nextTimeUnit!=-1 && placements.size() > Parameter.HISTORICAL_DATA_SIZE &&
                        !isReconfigurationActive && !isMigrationActive ){

                    // 收集O.F.历史值
                    valuesSelectedForecast.clear();
                    for(int timeIterator = nextTimeUnit - Parameter.HISTORICAL_DATA_SIZE; timeIterator<=actualTimeUnit;
                        timeIterator++){
                        if(placements.get(timeIterator)!=null){
                            valuesSelectedForecast.add(placements.get(timeIterator).getPlacementScore());
                        }else{
                            valuesSelectedForecast.add(0F);
                        }
                    }

                    // 检查是否需要重新配置并设置初始化时间
                    if(Utils.callToReconfiguration(valuesSelectedForecast, Parameter.FORECAST_SIZE)){
                        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE,nextTimeUnit);
                        reconfigurationTimeInit = nextTimeUnit;
                        isReconfigurationActive=true;
                    }else{
                        reconfigurationTimeInit=-1;
                    }
                }

                // 获取当前放置的快照以启动重新配置
                if(nextTimeUnit!=-1 && nextTimeUnit.equals(reconfigurationTimeInit)){

                    if(!virtualMachines.isEmpty()) {
                        // 获取先验值列表
                        aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);
                        // 克隆当前放置
                        Placement reconfgPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                                VirtualMachine.cloneVMsList(virtualMachines),
                                VirtualMachine.cloneVMsList(derivedVMs));

                        // 配置调用Memetic算法
                        staticReconfgTask = new StaticReconfMemeCall(reconfgPlacement,aPrioriValuesList,memeConfig);

                        // 在单独的线程中调用Memetic算法
                        reconfgResult = executorService.submit(staticReconfgTask);

                        // 更新Memetic算法执行的结束时间
                        reconfigurationTimeEnd = reconfigurationTimeInit + memeConfig.getExecutionDuration();
                    }
                    // 重置由预测触发的重新配置初始化
                    reconfigurationTimeInit = -1;

                }else if(nextTimeUnit != -1 && actualTimeUnit.equals(reconfigurationTimeEnd)) {
                    try {
                        isReconfigurationActive=false;
                        if(reconfgResult != null) {
                            // 从Memetic算法执行中获取放置
                            reconfgPlacementResult = reconfgResult.get();

                            // 更新放置的虚拟机列表
                            Utils.removeDeadVMsFromPlacement(reconfgPlacementResult,actualTimeUnit,memeConfig.getNumberOfResources());

                            /* 更新放置的虚拟机列表，更新虚拟机资源并添加新的虚拟机
                             */
                            Placement reconfgPlacementMerged = DynamicVMP.updatePlacementAfterReconf(workload, Constant.BFD,
                                    reconfgPlacementResult,
                                    reconfigurationTimeInit,
                                    reconfigurationTimeEnd);

                            aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);

                            // 更新放置的虚拟机列表
                            Utils.removeDeadVMsFromPlacement(reconfgPlacementMerged,actualTimeUnit,memeConfig.getNumberOfResources());
                            // 过滤掉死亡虚拟机后更新放置得分。
                            reconfgPlacementMerged.updatePlacementScore(aPrioriValuesList);

                            // 如果重新配置放置的得分更好，则接受它作为新的放置
                            if(DynamicVMP.isMememeticPlacementBetter(placements.get(actualTimeUnit), reconfgPlacementMerged)) {
                                // 获取需要迁移的虚拟机
                                vmsToMigrate  = Utils.getVMsToMigrate(reconfgPlacementMerged.getVirtualMachineList(),
                                        placements.get(reconfigurationTimeEnd).getVirtualMachineList());

                                // 更新迁移的虚拟机
                                Utils.removeDeadVMsMigrated(vmsToMigrate,actualTimeUnit);
                                // 获取虚拟机迁移的结束时间
                                vmsMigrationEndTimes = Utils.getTimeEndMigrationByVM(vmsToMigrate, actualTimeUnit);
                                // 更新迁移结束时间
                                migrationTimeEnd = Utils.getMigrationEndTime(vmsMigrationEndTimes);
                                isMigrationActive = !vmsToMigrate.isEmpty();

                                physicalMachines = new ArrayList<>(reconfgPlacementMerged.getPhysicalMachines());
                                virtualMachines = new ArrayList<>(reconfgPlacementMerged.getVirtualMachineList());
                                derivedVMs = new ArrayList<>(reconfgPlacementMerged.getDerivedVMs());

                                placements.put(actualTimeUnit, reconfgPlacementMerged);

                            }
                        }
                    } catch (ExecutionException e) {
                        logger.log(Level.SEVERE, "迁移失败！");
                        throw e;
                    }

                } else if(nextTimeUnit != -1 && actualTimeUnit.equals(migrationTimeEnd)) {
                    // 结束迁移状态
                    isMigrationActive = false;
                }
            }
        }
        Utils.executorServiceTermination(executorService);
    }
}
