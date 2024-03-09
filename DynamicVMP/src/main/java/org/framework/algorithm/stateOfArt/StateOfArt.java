package org.framework.algorithm.stateOfArt;

import org.domain.*;
import org.framework.Constant;
import org.framework.DynamicVMP;
import org.framework.ObjectivesFunctions;
import org.framework.Utils;
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

public class StateOfArt {

    private static Logger logger = DynamicVMP.getLogger();

    private StateOfArt() {
    }

    /**
     * 最先进技术管理器
     *
     * @param workload                   工作负载跟踪
     * @param physicalMachines           物理机器列表
     * @param virtualMachines            虚拟机器列表
     * @param derivedVMs                 派生虚拟机器列表
     * @param revenueByTime              时间的收入
     * @param wastedResources            按时间浪费的资源
     * @param wastedResourcesRatioByTime 每个时间的资源浪费比
     * @param powerByTime                时间的功耗
     * @param placements                 时间的放置列表
     * @param code                       启发式算法代码
     * @param timeUnit                   时间初始化
     * @param requestsProcess            进程类型
     * @param maxPower                   最大功耗
     * @param scenarioFile               场景名称
     *                                   <p>
     *                                   <b>请求过程</b>：
     *                                   <ul>
     *                                       <li>Requests[0]: requestServed 已服务请求数量</li>
     *                                       <li>Requests[1]: requestRejected 已拒绝请求数量</li>
     *                                       <li>Requests[2]: requestUpdated 已更新请求数量</li>
     *                                       <li>Requests[3]: violation 违规数量</li>
     *                                   </ul>
     * @throws IOException          错误管理文件
     * @throws InterruptedException 多线程错误
     * @throws ExecutionException   多线程错误
     */
    public static void stateOfArtManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine>
            virtualMachines, List<VirtualMachine> derivedVMs,
            Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
            Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
            Integer[] requestsProcess, Float maxPower, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {
        // 存储先验值的列表
        List<APrioriValue> aPrioriValuesList = new ArrayList<>();
        //待迁移的虚拟机列表
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        //虚拟机迁移结束时间列表
        List<Integer> vmsMigrationEndTimes = new ArrayList<>();
        //单线程执行器
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        //获取MEMETIC配置
        MASettings memeConfig = Utils.getMemeConfig(true);
        //静态重配置任务
        Callable<Placement> staticReconfgTask;
        //重配置结果
        Future<Placement> reconfgResult = null;
        //重配置后的放置结果
        Placement reconfgPlacementResult;
        //虚拟机迁移状态标识
        Boolean isMigrationActive = false;
        //是否更新虚拟机利用率
        Boolean isUpdateVmUtilization = false;
        //当前时间单元
        Integer actualTimeUnit;
        //下一个时间单元
        Integer nextTimeUnit;
        //重配置开始时间
        Integer reconfigurationTimeInit = timeUnit + memeConfig.getExecutionInterval();
        //重配置结束时间
        Integer reconfigurationTimeEnd=-1;
        //虚拟机迁移结束时间
        Integer migrationTimeEnd =- 1;
        //虚拟机迁移结束时间点
        Integer vmEndTimeMigration = 0;
        //遍历工作负载
        for (int iterator = 0; iterator < workload.size(); ++iterator) {
            //获取当前请求
            Scenario request = workload.get(iterator);
            //当前时间单元
            actualTimeUnit = request.getTime();
            // 如果是最后一个请求，将下一个时间单元设置为-1
            nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

            //检查在迁移期间是否出现过载
            if (nextTimeUnit!= -1 && isMigrationActive && DynamicVMP.isVmBeingMigrated(request.getVirtualMachineID(),
                    request.getCloudServiceID(), vmsToMigrate)){
                // 检查是否需要迁移由于迁移而导致的过载
                VirtualMachine vmMigrating = getById(request.getVirtualMachineID(),request.getCloudServiceID(),
                    virtualMachines);
                // 检查虚拟机迁移结束的时间
                vmEndTimeMigration = Utils.updateVmEndTimeMigration(vmsToMigrate, vmsMigrationEndTimes,
                        vmEndTimeMigration,
                        vmMigrating);
                // 根据条件判断是否添加过载到CPU利用率
                isUpdateVmUtilization = actualTimeUnit <= vmEndTimeMigration;
            }
            // 运行启发式算法
            DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess,
                    isUpdateVmUtilization);
            // 检查是否是最后一个请求或者将发生时间单元的变化
            if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {
                // 获取目标函数值
                ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
                        virtualMachines, derivedVMs, wastedResources,
                        wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);
                // 基于距离原点法计算放置分数
                Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
                        maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);
                Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
                // 写入时间t的放置分数
                Utils.printToFile( Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile, placementScore);
                // 更新时间单元
                timeUnit = actualTimeUnit;

                // 创建当前时间单元的放置
                Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                        VirtualMachine.cloneVMsList(virtualMachines),
                        VirtualMachine.cloneVMsList(derivedVMs), placementScore);
                placements.put(actualTimeUnit, heuristicPlacement);
                // 如果下一个时间单元等于重配置开始时间，获取当前放置的快照以启动重配置
                if(nextTimeUnit!=-1 && nextTimeUnit.equals(reconfigurationTimeInit)){
                    // 如果在MEMETIC执行期间收到新的VM请求，则取消MEMETIC算法
                    if (isMigrationActive || StateOfArtUtils.newVmDuringMemeticExecution(workload, reconfigurationTimeInit, reconfigurationTimeInit +
                            memeConfig
                            .getExecutionDuration())) {
                        reconfigurationTimeInit = reconfigurationTimeInit + memeConfig.getExecutionInterval();
                    } else {

                        if(!virtualMachines.isEmpty()) {
                            // 获取先验值列表
                            aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);
                            // 克隆当前放置
                            Placement memeticPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
                                    VirtualMachine.cloneVMsList(virtualMachines),
                                    VirtualMachine.cloneVMsList(derivedVMs));

                            // 配置MEMETIC算法的调用
                            staticReconfgTask = new StaticReconfMemeCall(memeticPlacement, aPrioriValuesList,
                                    memeConfig);

                            // 在单独的线程中调用MEME算法
                            reconfgResult = executorService.submit(staticReconfgTask);

                            // 更新MEMETIC算法执行结束的时间
                            reconfigurationTimeEnd = reconfigurationTimeInit + memeConfig.getExecutionDuration();
                            Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE,nextTimeUnit);
                        }
                    }
                }else if(nextTimeUnit != -1 && actualTimeUnit.equals(reconfigurationTimeEnd)) {

                    try {

                        if(reconfgResult != null) {
                            // 获取MEMETIC算法执行的放置
                            reconfgPlacementResult = reconfgResult.get();
                            // 更新放置的虚拟机列表
                            Utils.removeDeadVMsFromPlacement(reconfgPlacementResult,actualTimeUnit,memeConfig.getNumberOfResources());
                            // 更新放置的虚拟机列表，更新VMs资源并添加新的VMs
                            Placement reconfgPlacementMerged = DynamicVMP.updatePlacementAfterReconf(workload, Constant.BFD,
                                    reconfgPlacementResult,
                                    reconfigurationTimeInit,
                                    reconfigurationTimeEnd);

                            aPrioriValuesList = Utils.getAprioriValuesList(actualTimeUnit);
                            // 更新放置的虚拟机列表
                            Utils.removeDeadVMsFromPlacement(reconfgPlacementMerged,actualTimeUnit,memeConfig.getNumberOfResources());
                            // 过滤掉死掉的虚拟机后更新放置分数
                            reconfgPlacementMerged.updatePlacementScore(aPrioriValuesList);
                            // 如果重配置放置的分数更好，接受它作为新的放置
                            if(DynamicVMP.isMememeticPlacementBetter(placements.get(actualTimeUnit), reconfgPlacementMerged)) {
                                // 获取需要迁移的虚拟机
                                vmsToMigrate  = Utils.getVMsToMigrate(reconfgPlacementMerged.getVirtualMachineList(),
                                        placements.get(reconfigurationTimeEnd).getVirtualMachineList());
                                // 更新迁移的虚拟机
                                Utils.removeDeadVMsMigrated(vmsToMigrate,actualTimeUnit);
                                // 获取虚拟机迁移结束的时间
                                vmsMigrationEndTimes = Utils.getTimeEndMigrationByVM(vmsToMigrate, actualTimeUnit);
                                // 更新迁移结束时间
                                migrationTimeEnd = Utils.getMigrationEndTime(vmsMigrationEndTimes);
                                isMigrationActive = !vmsToMigrate.isEmpty();
                                physicalMachines = new ArrayList<>(reconfgPlacementMerged.getPhysicalMachines());
                                virtualMachines = new ArrayList<>(reconfgPlacementMerged.getVirtualMachineList());
                                derivedVMs = new ArrayList<>(reconfgPlacementMerged.getDerivedVMs());

                                placements.put(actualTimeUnit, reconfgPlacementMerged);

                            }
                            // 更新MEMETIC算法的初始化时间,周期性触发
                            reconfigurationTimeInit = reconfigurationTimeInit + memeConfig.getExecutionInterval();
                        }
                    } catch (ExecutionException e) {
                        logger.log(Level.SEVERE, "Migration Failed!");
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
