package org.framework;

import org.domain.*;
import org.framework.algorithm.cleverReconfiguration.CleverReconfiguration;
import org.framework.algorithm.onlineApproach.OnlineApproach;
import org.framework.algorithm.periodicMigration.PeriodicMigration;
import org.framework.algorithm.stateOfArt.StateOfArt;
import org.framework.algorithm.thresholdBasedApproach.ThresholdBasedApproach;
import org.framework.iterativeAlgorithm.Heuristics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * DynamicVMP 类是虚拟机放置的主要控制类。
 */
public class DynamicVMP {

    /**
     * 函数式接口，用于定义算法的使用方法
     */
    @FunctionalInterface
    interface Algorithm {
        void useAlgorithm(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
                List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
                Map<Integer, Float> revenueByTime, List<Resources> wastedResources,  Map<Integer, Float> wastedResourcesRatioByTime,
                Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
                Integer[] requestsProcess, Float maxPower, String scenarioFile)
                throws IOException, InterruptedException, ExecutionException;
    }

    /**
     * 算法列表
     */
    private static final Algorithm[] algorithms;//放置算法数组
    static {
        algorithms = new Algorithm[]{
                PeriodicMigration::periodicMigrationManager,            // Alg0 - 周期性更新
                StateOfArt::stateOfArtManager,                          // Alg1 - 周期性撤销
                ThresholdBasedApproach::thresholdBasedApproachManager,  // Alg2 - 基于阈值触发
                CleverReconfiguration::cleverReconfigurationgManager,   // Alg3 - 基于预测值更新
                OnlineApproach::onlineApproachManager,                  // Alg4 - iVMP
        };
    }

    public static final String DYNAMIC_VMP = "DynamicVMP";//类常量定义
    private static Logger logger = Logger.getLogger(DYNAMIC_VMP);

    public static Integer timeSimulated;// 模拟时间
    public static Integer initialTimeUnit;// 初始时间单位
    public static Integer vmUnique = 0;// 唯一虚拟机标识

    /**
     * 统计指标
     */
    public static Float maxPower = 0F;
    public static Float maxRevenueLost = 0F;
    public static Float economicalPenalties = 0F;
    public static Float leasingCosts = 0F;

    /**
     * 统计先验时间指标
     */
    static Map<Integer, Float> revenueAprioriTime = new HashMap<>();
    static Map<Integer, Float> migratedMemoryAprioriTime = new HashMap<>();

    /**
     * 未满足资源的虚拟机映射
     */
    public static Map<Integer, Violation> unsatisfiedResources = new HashMap<>();

    private DynamicVMP () {
    }

    /**
     * 运行启发式算法，根据场景选择不同的启发式算法处理虚拟机请求。
     *
     * @param s                 场景对象
     * @param code              算法代码
     * @param physicalMachines  物理机列表
     * @param virtualMachines   虚拟机列表
     * @param derivedVMs        派生虚拟机列表
     * @param requests          请求统计数组
     * @param isMigrationActive 是否激活迁移
     */
    public static void runHeuristics (Scenario s, Integer code, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Integer[] requests,
            Boolean isMigrationActive) {
        //创建新的虚拟机
        VirtualMachine vm = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                s.getTinit(), s.getTend(), s.getUtilization(),
                s.getDatacenterID(), s.getCloudServiceID(), null);
        // 根据时间戳选择合适的处理逻辑
        if (s.getTime() <= s.getTinit()) {
            if (Heuristics.getHeuristics()[code]
                    .useHeuristic(vm, physicalMachines, virtualMachines, derivedVMs, false)) {
                requests[0]++;// 增加成功处理请求计数
            } else {
                requests[1]++;// 增加拒绝请求计数
            }
        } else {
            if (s.getTime() <= s.getTend()) {
                if (Heuristics.updateVM(s, virtualMachines,derivedVMs, physicalMachines, isMigrationActive)) {
                    requests[2]++;// 增加更新请求计数
                } else {
                    requests[3]++;// 增加违规请求计数
                }
            } else {
                logger.log(Level.SEVERE, "WorkloadException!");
            }
        }
    }

    /**
     * 运行启发式算法，处理虚拟机迁移请求。
     *
     * @param code           算法代码
     * @param physicalMachines  物理机列表
     * @param virtualMachines   虚拟机列表
     * @param derivedVMs        派生虚拟机列表
     * @param vmToMigrate     待迁移的虚拟机列表
     */
    public static void runHeuristics (Integer code, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<VirtualMachine> vmToMigrate) {
        //按资源需求排序待迁移虚拟机列表
        Collections.sort(vmToMigrate);
        PhysicalMachine hostPm;

        for (VirtualMachine vm : vmToMigrate) {
            hostPm = PhysicalMachine.getById(vm.getPhysicalMachine(), physicalMachines);
            VirtualMachine migratedVM = vm.cloneVM();
            for (PhysicalMachine pm : physicalMachines) {

                if(hostPm != null && !pm.getId().equals(hostPm.getId())
                    && Constraints.checkResources(pm, null, vm, virtualMachines, true)
                    && Heuristics.getHeuristics()[code]
                        .useHeuristic(migratedVM, physicalMachines, virtualMachines, derivedVMs, true)) {

                    virtualMachines.remove(vm);
                    hostPm.updatePMResources(vm, Utils.SUB);
                    break;

                }
            }
        }
    }

    /**
     * 在重新配置后更新放置方案。
     *
     * @param workload       场景列表
     * @param heuristicCode  启发式算法代码
     * @param placement      当前放置方案
     * @param startTimeMemeticAlg  Memetic算法开始时间
     * @param endTimeMemeticAlg    Memetic算法结束时间
     * @return 更新后的放置方案
     */
    public static Placement updatePlacementAfterReconf (List<Scenario> workload, String heuristicCode, Placement placement,
            Integer startTimeMemeticAlg, Integer endTimeMemeticAlg) {

        Integer code = Constant.HEURISTIC_MAP.get(heuristicCode);
        Integer[] requestsProcessAfterReconf = initRequestProcess();
        // 复制场景列表
        List<Scenario> cloneScenario = Scenario.cloneScenario(workload, startTimeMemeticAlg, endTimeMemeticAlg);
        // 遍历复制的场景列表，运行启发式算法进行更新
        cloneScenario.forEach(request ->
            runHeuristics(request, code, placement.getPhysicalMachines(), placement.getVirtualMachineList(),
                    placement.getDerivedVMs(), requestsProcessAfterReconf, false)
        );

        return placement;
    }

    /**
     * 主程序入口，执行实验。
     *
     * @param args 输入参数
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static void main (String[] args) throws IOException, InterruptedException, ExecutionException {
        String[] argss={"inputs/parameters","DC1A3outputs/"};
        //存储实验场景文件路径的列表
        ArrayList<String> scenariosFiles  = new ArrayList<>();
        if(0 == argss.length) {
            logger.log(Level.INFO, "Some arguments are missing!");
        }
        String parameterFile = argss[0];
        String outputFolderPath = argss[1];
        // 预处理输入和输出路径
        Utils.preprocessInputOutputPaths(parameterFile,outputFolderPath);
        // 加载实验参数
        loadParameters(scenariosFiles, parameterFile);
        //执行实验
        logger.log(Level.INFO, "EXECUTING EXPERIMENTS");
        for (String scenarioFile : scenariosFiles) {

            launchExperiments(Parameter.HEURISTIC_CODE, Parameter.PM_CONFIG, scenarioFile);
        }
        logger.log(Level.INFO, "ENDING EXPERIMENTS");
    }

    /**
     * 加载实验参数的方法，从三个参数文件中读取内容，并调用工具类的loadParameter方法加载到scenariosFiles列表中
     * @param scenariosFiles 场景文件
     * @param parameterFile  配置文件
     * @throws IOException
     */
    private static void loadParameters(ArrayList<String> scenariosFiles, String parameterFile) throws IOException {

        try (Stream<String> stream = Files.lines(Paths.get(parameterFile))) {
            Utils.loadParameter(scenariosFiles, stream);
        } catch (IOException e) {
            Logger.getLogger(DynamicVMP.DYNAMIC_VMP).log(Level.SEVERE, "Error trying to load experiments parameters.");
            throw e;
        }
    }

    /**
     * 执行实验的方法
     * @param heuristicCode 启发式算法代码
     * @param pmConfig      物理机配置
     * @param scenarioFile  场景文件
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private static void launchExperiments(String heuristicCode, String pmConfig, String scenarioFile)
            throws IOException, InterruptedException, ExecutionException {

        //变量声明
        List<PhysicalMachine> physicalMachines = new ArrayList<>();
        List<Scenario> scenarios = new ArrayList<>();
        List<VirtualMachine> virtualMachines = new ArrayList<>();
        List<VirtualMachine> derivedVMs = new ArrayList<>();

        Integer[] requestsProcess = initRequestProcess();
        Float[] realRevenue = new Float[]{0F};
        Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
        Files.write(Paths.get(Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile),
                (scenarioFile + "\n" ).getBytes(), StandardOpenOption.CREATE);

        List<Resources> wastedResources = new ArrayList<>();
        Map<Integer, Float> wastedResourcesRatioByTime = new HashMap<>();
        Map<Integer, Float> powerByTime = new HashMap<>();
        Map<Integer, Float> revenueByTime = new HashMap<>();
        Map<Integer, Placement> placements = new HashMap<>();
        // 加载数据中心配置信息，初始化最大模拟时间
        maxPower = Utils.loadDatacenter(pmConfig, scenarioFile, physicalMachines, scenarios);
        timeSimulated = scenarios.get(scenarios.size() - 1).getTime();
        Integer code = Constant.HEURISTIC_MAP.get(heuristicCode);
        //检查启发式代码是否有效
        if(code == null) {
            logger.log(Level.SEVERE, "Is not a valid algorithm!");
            return;
        }
        //初始化时间单元
        Integer timeUnit = scenarios.get(0).getTime();
        initialTimeUnit = timeUnit;
        timeAdjustment(wastedResources, wastedResourcesRatioByTime, powerByTime, revenueByTime, scenarioFile);
        //加载先验时间点的值
        loadAprioriValuesByTime(scenarios);
        //如果是BFD或FFD则先进行排序
        if (Constant.FFD.equals(heuristicCode)) {
        Collections.sort(scenarios);
        }
        try{
            //调用选择的算法进行实验
            getAlgorithms()[Parameter.ALGORITHM]
                    .useAlgorithm(scenarios, physicalMachines, virtualMachines, derivedVMs,
                            revenueByTime, wastedResources, wastedResourcesRatioByTime, powerByTime,
                            placements, code, timeUnit, requestsProcess, maxPower, scenarioFile);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.log(Level.SEVERE, "Is not a valid algorithm!");
            throw e;
        }
        //计算场景得分
        Float scenarioScored = ObjectivesFunctions.getScenarioScore(revenueByTime, placements, realRevenue);
        //输出结果到文件中
        Utils.printToFile(Constant.POWER_CONSUMPTION_FILE, Utils.getAvgPwConsumptionNormalized(powerByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_FILE, Utils.getAvgResourcesWNormalized(wastedResourcesRatioByTime));
        Utils.printToFile(Constant.ECONOMICAL_REVENUE_FILE, Utils.getAvgRevenueNormalized(revenueByTime));
        Utils.printToFile(Constant.WASTED_RESOURCES_RATIO_FILE, wastedResources);
        Utils.printToFile(Constant.SCENARIOS_SCORES, scenarioScored);
        Utils.printToFile(Constant.RECONFIGURATION_CALL_TIMES_FILE,"\n");
        Utils.printToFile(Constant.ECONOMICAL_PENALTIES_FILE, DynamicVMP.economicalPenalties);
        Utils.printToFile(Constant.LEASING_COSTS_FILE, DynamicVMP.leasingCosts);
    }

    /**
     * 时间调整方法，用于在开始模拟前补充时间单元
     *
     * @param wastedResources            浪费资源列表
     * @param wastedResourcesRatioByTime 每个时间t浪费的资源列表
     * @param powerByTime                每个时间t的功耗
     * @param revenueByTime              每个时间t的经济收益
     */
    private static void timeAdjustment(List<Resources> wastedResources,
            Map<Integer, Float> wastedResourcesRatioByTime, Map<Integer, Float> powerByTime,
            Map<Integer, Float> revenueByTime, String scenarioFile) throws IOException {

        Integer timeAdjust = 0;
        if(initialTimeUnit != 0 ) {
            while (timeAdjust < initialTimeUnit) {
                powerByTime.put(timeAdjust, 0F);
                wastedResources.add(new Resources());
                wastedResourcesRatioByTime.put(timeAdjust, 0F);
                revenueByTime.put(timeAdjust, 0F);
                Utils.printToFile(Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile, 0);
                timeAdjust++;
                timeSimulated += 1;
            }
        }
    }

    /**
     * 加载先验时间点的值
     * @param workload 场景列表
     */
    public static void loadAprioriValuesByTime(List<Scenario> workload) {
        // 用于存储时间点对应的先验收入的映射
        Map<Integer, Float> revenueAPrioriByTime = new HashMap<>();
        // 用于存储时间点对应的先验迁移内存的映射
        Map<Integer, Float> migratedMemoryAPrioriByTime = new HashMap<>();
        // 记录唯一虚拟机数量
        Integer numberUniqueVm = 0;
        // 记录累积先验收入
        Float revenueAPriori=0F;
        // 记录累积先验迁移内存
        Float migratedMemoryAPriori = 0F;
        // 时间调整值
        Integer timeAdjust = 0;
        // 如果初始时间单位不为零，补充先验映射中的时间点
        if(initialTimeUnit != 0 ) {
            while (timeAdjust < initialTimeUnit) {
                revenueAPrioriByTime.put(timeAdjust, 0F);
                migratedMemoryAPrioriByTime.put(timeAdjust, 0F);
                timeAdjust++;
            }
        }
        // 遍历场景列表，计算先验收入和迁移内存
        for( int iteratroScenario=0; iteratroScenario<workload.size(); iteratroScenario++){
            // 获取当前场景
            Scenario request = workload.get(iteratroScenario);
            //计算垒起先验收入
            revenueAPriori += request.getRevenue().getCpu() * request.getResources().getCpu() *  Parameter.DERIVE_COST;
            revenueAPriori += request.getRevenue().getRam() * request.getResources().getRam() *  Parameter.DERIVE_COST;
            revenueAPriori += request.getRevenue().getNet() * request.getResources().getNet() *  Parameter.DERIVE_COST;
            // 累积先验迁移内存
	        migratedMemoryAPriori+= request.getResources().getRam();
            // 如果场景的时间小于等于初始化时间，增加虚拟机数量
            if(request.getTime() <= request.getTinit()) {
                numberUniqueVm++;
            }
            // 判断是否是最后一个场景或者下一个场景的时间不同，更新先验映射
            if((iteratroScenario + 1) == workload.size() || !request.getTime().equals(workload.get(iteratroScenario + 1).getTime())){
                revenueAPrioriByTime.put(request.getTime(), revenueAPriori);
                maxRevenueLost += revenueAPriori;
                migratedMemoryAPrioriByTime.put(request.getTime(), migratedMemoryAPriori);
                revenueAPriori = 0F;
                migratedMemoryAPriori = 0F;
            }
        }
        // 将计算得到的先验映射更新到全局变量中
        revenueAprioriTime = revenueAPrioriByTime;
        migratedMemoryAprioriTime = migratedMemoryAPrioriByTime;
        vmUnique = numberUniqueVm;

    }

    /**
     * 检查Memetic算法的放置结果是不是更好
     *
     * @param heuristicPlacement 启发式放置方案
     * @param memeticPlacement MA放置方案
     * @return <b>True</b>, MA更好 <br> <b>False</b>, 其他
     */
    public static Boolean isMememeticPlacementBetter(Placement heuristicPlacement, Placement memeticPlacement) {

        Boolean isBetter = false;
        if(memeticPlacement == null) {
            return false;
        }
        int compare = heuristicPlacement.getPlacementScore().compareTo(memeticPlacement.getPlacementScore());
        if (compare == 0) {
            isBetter = false;
        }
        if (compare < 0) {
            isBetter = false;
        }
        if (compare > 0) {
            isBetter = true;
        }
        return isBetter;
    }

    // 初始化请求处理过程的数组
    private static Integer[] initRequestProcess() {

        Integer[] requestsProcess = new Integer[4];
        requestsProcess[0] = 0;    // 已处理请求数
        requestsProcess[1] = 0;    // 拒绝的请求数
        requestsProcess[2] = 0;    // 更新的请求数
        requestsProcess[3] = 0;    // 违反约束的请求数
        return requestsProcess;
    }

    // 检查虚拟机是否正在迁移
    public static Boolean isVmBeingMigrated(Integer virtualMachineId, Integer cloudServiceId, List<VirtualMachine>
        vmsToMigrate) {
		Integer iteratorVM;
	    VirtualMachine vmMigrated;
	    for(iteratorVM=0;iteratorVM<vmsToMigrate.size();iteratorVM++){
			vmMigrated = vmsToMigrate.get(iteratorVM);
			if(vmMigrated.getId().equals(virtualMachineId) && vmMigrated.getCloudService().equals(cloudServiceId) ) {
				return true;
			}
		}
		return false;
    }

    // 更新经济消耗
    public static void updateEconomicalPenalties(VirtualMachine vm, Resources resourcesViolated, Integer timeViolation) {

        Float violationRevenue = 0F;
        violationRevenue += resourcesViolated.getCpu() * vm.getRevenue().getCpu();
        violationRevenue += resourcesViolated.getRam() * vm.getRevenue().getRam();
        violationRevenue += resourcesViolated.getNet() * vm.getRevenue().getNet();

        Float currentRevenue = DynamicVMP.revenueAprioriTime.get(timeViolation);
        Float newAPrioriRevenue = currentRevenue + violationRevenue;
        DynamicVMP.revenueAprioriTime.put(timeViolation, newAPrioriRevenue);

        economicalPenalties += violationRevenue;
    }
    //更新租赁成本
    public static void updateLeasingCosts(List<VirtualMachine> derivedVMs) {

        Float leasingCostRevenue = 0F;
        for (VirtualMachine dvm : derivedVMs) {
            leasingCostRevenue += dvm.getResources().get(0) * dvm.getRevenue().getCpu() * Parameter.DERIVE_COST;
            leasingCostRevenue += dvm.getResources().get(1) * dvm.getRevenue().getRam() * Parameter.DERIVE_COST;
            leasingCostRevenue += dvm.getResources().get(2) * dvm.getRevenue().getNet() * Parameter.DERIVE_COST;
        }

        leasingCosts += leasingCostRevenue;
    }
    // 获取可用的算法数组
    public static Algorithm[] getAlgorithms() {

        return algorithms;
    }

    public static Logger getLogger() {

        return logger;
    }
}

