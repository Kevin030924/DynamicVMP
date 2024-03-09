package org.framework;

import org.domain.*;
import org.framework.comparator.MemoryComparator;
import org.framework.reconfigurationAlgorithm.enums.ResourcesEnum;
import org.framework.reconfigurationAlgorithm.memeticAlgorithm.MASettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.*;

public class Utils {

    public static String OUTPUT = "outputs/";

    public static String INPUT = "inputs/";

    public static final String SUM = "SUM";
    public static final String SUB = "SUB";
    public static final String SCENARIOS = "SCENARIOS";
    private static Random  random;

    private static Logger logger = Logger.getLogger("Utils");

    private Utils() {
        // Default Constructor
    }

    static{
        random = new Random();
    }

    /**
     * 从输入流中加载物理机信息到物理机列表，并计算物理机的最大功耗。
     *
     * @param physicalMachines  物理机列表
     * @param stream            输入流
     * @return 物理机的最大功耗
     */
    private static Float loadPhysicalMachines(List<PhysicalMachine> physicalMachines,
                                              Stream<String> stream) {
        Float[] maxPower = new Float[1];
        maxPower[0] = 0F;
        // 遍历输入流的每一行
        stream.forEach(line -> {
            List<Float> resources = new ArrayList<>();
            // 按制表符分割行数据
            String[] splitLine = line.split("\t");
            // 解析每个资源的值和物理机的最大功耗
            Float r1 = Float.parseFloat(splitLine[0]);
            Float r2 = Float.parseFloat(splitLine[1]);
            Float r3 = Float.parseFloat(splitLine[2]);
            Integer pmax = Integer.parseInt(splitLine[3]);
            resources.add(r1);
            resources.add(r2);
            resources.add(r3);
            // 创建物理机对象并添加到物理机列表中
            PhysicalMachine pm = new PhysicalMachine(physicalMachines.size(), pmax, resources);
            physicalMachines.add(pm);
            // 累加物理机的最大功耗
            maxPower[0] += pm.getPowerMax();
        });
        return maxPower[0];
    }

    /**
     * 从输入流中加载场景信息到场景列表。
     *
     * @param scenarios 场景列表
     * @param stream    输入流
     */
    private static void loadScenario(List<Scenario> scenarios,
                                     Stream<String> stream) {
        stream.forEach(line -> {
            // 创建资源、利用率和收入对象
            Resources resources = new Resources();
            Resources utilization = new Resources();
            Revenue revenue = new Revenue();
            // 按制表符分割行数据
            String[] splitLine = line.split("\t");
            // 解析行中的各个字段
            Integer time = Integer.parseInt(splitLine[0]);
            Integer service = Integer.parseInt(splitLine[1]);
            Integer datacenter = Integer.parseInt(splitLine[2]);
            Integer virtualMachine = Integer.parseInt(splitLine[3]);
            // 调用方法加载资源、利用率和收入信息
            loadResources(resources, splitLine);
            loadUtilization(utilization, splitLine);
            loadRevenue(revenue, splitLine);
            Integer tinit = Integer.parseInt(splitLine[13]);
            Integer tend = Integer.parseInt(splitLine[14]);
            // 创建场景对象并添加到场景列表中
            Scenario scenario = new Scenario(time, service, datacenter, virtualMachine, resources,
                    utilization, revenue, tinit, tend);

            scenarios.add(scenario);
        });
    }

    /**
     * 从字符串数组中加载利用率信息。
     *
     * @param utilization 利用率对象
     * @param splitLine   字符串数组
     */
    private static void loadUtilization(Resources utilization, String[] splitLine) {
        // 解析字符串数组中的各个字段并设置到利用率对象中
        Float u1 = Float.parseFloat(splitLine[7]);
        Float u2 = Float.parseFloat(splitLine[8]);
        Float u3 = Float.parseFloat(splitLine[9]);
        utilization.setCpu(u1);
        utilization.setRam(u2);
        utilization.setNet(u3);
    }

    /**
     * 从字符串数组中加载资源信息。
     *
     * @param resources 资源对象
     * @param splitLine 字符串数组
     */
    private static void loadResources(Resources resources, String[] splitLine) {
        Float r1 = Float.parseFloat(splitLine[4]);
        Float r2 = Float.parseFloat(splitLine[5]);
        Float r3 = Float.parseFloat(splitLine[6]);
        resources.setCpu(r1);
        resources.setRam(r2);
        resources.setNet(r3);
    }

    /**
     * 从字符串数组中加载收入信息。
     *
     * @param revenue   收入对象
     * @param splitLine 字符串数组
     */
    private static void loadRevenue(Revenue revenue, String[] splitLine) {
        Float r1 = Float.parseFloat(splitLine[10]);
        Float r2 = Float.parseFloat(splitLine[11]);
        Float r3 = Float.parseFloat(splitLine[12]);
        revenue.setCpu(r1);
        revenue.setRam(r2);
        revenue.setNet(r3);
    }

    /**
     * 生成指定范围内的随机整数。
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 在指定范围内的随机整数
     */
    public static int getRandomInt(int min, int max) {

        return min + random.nextInt(max - min + 1);
    }

    /**
     * 生成小于指定值的随机整数。
     *
     * @param n 上限值（不包含）
     * @return 小于指定值的随机整数
     */
    public static int getRandomtInt(int n) {

        return random.nextInt(n);
    }

    /**
     * 生成 0 到 1 之间的随机双精度浮点数。
     *
     * @return 0 到 1 之间的随机双精度浮点数
     */
    public static double getRandomDouble() {

        return random.nextDouble();
    }

    /**
     * 更新衍生虚拟机列表。
     *
     * @param virtualMachineList    虚拟机列表
     * @param derivedVMs           衍生虚拟机列表
     */
    public static void updateDerivedVMs(List<VirtualMachine> virtualMachineList, List<VirtualMachine> derivedVMs) {
        List<VirtualMachine> vmsToRemove = new ArrayList<>();
        // 将物理机ID为0的虚拟机添加到衍生虚拟机列表中，并记录待移除的虚拟机
        virtualMachineList.forEach(vm -> {
            if (vm.getPhysicalMachine() == 0) {
                derivedVMs.add(vm);
                vmsToRemove.add(vm);
            }
        });
        // 移除已添加到衍生虚拟机列表的虚拟机
        vmsToRemove.forEach(virtualMachineList::remove);
    }

    /**
     * 通过物理机ID过滤虚拟机列表。
     *
     * @param virtualMachineList    虚拟机列表
     * @param physicalMachineId     物理机ID
     * @return 符合指定物理机ID的虚拟机列表
     */
    public static List<VirtualMachine> filterVMsByPM(List<VirtualMachine> virtualMachineList, Integer physicalMachineId) {
        // 使用Predicate过滤虚拟机列表，保留物理机ID匹配的虚拟机
        Predicate<VirtualMachine> vmFilter = vm -> vm.getPhysicalMachine().equals(physicalMachineId);
        return virtualMachineList.stream().filter(vmFilter).collect(Collectors.toList());
    }
    /**
     * 获取由物理机之间的虚拟机迁移引起的内存迁移矩阵。
     *
     * @param oldVirtualMachineList 旧的虚拟机列表
     * @param newVirtualMachineList 新的虚拟机列表
     * @param numberOfPM           物理机的数量
     * @return 内存迁移矩阵，表示每对物理机之间的内存迁移量
     */
    public static Float[][] getMigratedMemoryByPM(List<VirtualMachine> oldVirtualMachineList, List<VirtualMachine> newVirtualMachineList, int numberOfPM) {
        Float[][] memoryMigrationByPM = new Float[numberOfPM][numberOfPM];
        Utils.initializeMatrix(memoryMigrationByPM, numberOfPM, numberOfPM);
        int iteratorVM;
        int oldVMPosition;
        int newVMPosition;
        VirtualMachine vm;
        Integer ramIndex = ResourcesEnum.RAM.getIndex();
        for (iteratorVM = 0; iteratorVM < oldVirtualMachineList.size(); iteratorVM++) {
            oldVMPosition = oldVirtualMachineList.get(iteratorVM).getPhysicalMachine();
            newVMPosition = newVirtualMachineList.get(iteratorVM).getPhysicalMachine();
            if (oldVMPosition != newVMPosition && newVMPosition != 0) {
                vm = oldVirtualMachineList.get(iteratorVM);
                // 计算内存迁移量，并更新矩阵
                memoryMigrationByPM[oldVMPosition - 1][newVMPosition - 1] += vm.getResources().get(ramIndex) * (vm.getUtilization().get(ramIndex) / 100);
            }
        }
        return memoryMigrationByPM;
    }

    /**
     * 从指定的 PM 配置和场景文件加载数据中心信息。
     *
     * @param pmConfig      物理机配置文件的路径
     * @param scenarioFile  场景文件的路径
     * @param physicalMachines 用于存储物理机信息的列表
     * @param scenarios     用于存储场景信息的列表
     * @return 数据中心的最大功耗值
     * @throws IOException 如果在加载 PM 配置或场景文件时发生 I/O 异常
     */
    public static Float loadDatacenter(String pmConfig, String scenarioFile, List<PhysicalMachine> physicalMachines,
                                       List<Scenario> scenarios) throws IOException {
        // 从 PM 配置文件加载物理机信息
        Float maxPower;
        try (Stream<String> stream = lines(Paths.get(INPUT + pmConfig))) {
            maxPower = Utils.loadPhysicalMachines(physicalMachines, stream);
        } catch (IOException e) {
            Logger.getLogger(DynamicVMP.DYNAMIC_VMP).log(Level.SEVERE, "Error trying to load PM Configuration!");
            throw e;
        }
        // 从场景文件加载场景信息
        try (Stream<String> stream = lines(Paths.get(INPUT + scenarioFile))) {
            Utils.loadScenario(scenarios, stream);
        } catch (IOException e) {
            Logger.getLogger(DynamicVMP.DYNAMIC_VMP).log(Level.SEVERE, "Error trying to load Scenario: " +
                    scenarioFile);
            throw e;
        }
        return maxPower;
    }


    /**
     * 将对象写入到文件。
     *
     * @param file     要写入的文件路径
     * @param toPrint  要打印到文件的对象
     * @throws IOException 如果发生写入文件时的 I/O 异常
     */
    public static void printToFile(String file, Object toPrint) throws IOException {
        // 检查输出目录是否存在，如果不存在，则创建
        if (!(Paths.get(Utils.OUTPUT).toFile().exists())) {
            createDirectory(Paths.get(Utils.OUTPUT));
        }
        if (toPrint instanceof Collection<?>) {
            // 如果是集合类型，则将每个元素逐行写入文件
            List<?> toPrintList = (ArrayList<String>) toPrint;
            toPrintList.forEach(consumer -> {
                try {
                    write(Paths.get(file), consumer.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    write(Paths.get(file), "\n".getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            });
        } else if (toPrint instanceof Map<?, ?>) {
            // 如果是映射类型，则将每个映射项的值逐行写入文件
            Map<Integer, Float> toPrintMap = (Map<Integer, Float>) toPrint;
            for (Map.Entry<Integer, Float> entry : toPrintMap.entrySet()) {
                try {
                    write(Paths.get(file), entry.getValue().toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    write(Paths.get(file), "\n".getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }
        } else {
            // 对于其他类型的对象，将其转换为字符串并写入文件
            try {
                write(Paths.get(file), toPrint.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                write(Paths.get(file), "\n".getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }


    /**
     * 复制浮点数列表。
     *
     * @param floatList 要复制的浮点数列表
     * @return 复制后的浮点数列表
     */
    public static List<Float> getListClone(List<Float> floatList) {
        List<Float> cloneList = new ArrayList<>();
        // 遍历原列表并逐个添加复制元素到新列表
        floatList.forEach(x -> cloneList.add(new Float(x)));
        return cloneList;
    }

    /**
     * Apriori 值列表:
     * <ul>
     *     <li> 功耗 </li>
     *     <li> 经济收益 </li>
     *     <li> 服务质量 </li>
     *     <li> 资源浪费 </li>
     *     <li> 迁移次数 </li>
     *     <li> 迁移的内存 </li>
     * </ul>
     *
     * @param timeUnit 时间单位
     * @return Apriori 值列表
     */
    public static List<APrioriValue> getAprioriValuesList(Integer timeUnit) {
        List<APrioriValue> aPrioriValuesList = new ArrayList<>();
        // 添加 Apriori 值到列表中
        aPrioriValuesList.add(new APrioriValue(ObjectivesFunctions.MIN_POWER, DynamicVMP.maxPower));
        aPrioriValuesList.add(new APrioriValue(ObjectivesFunctions.MIN_REVENUE, DynamicVMP.revenueAprioriTime.get(timeUnit)));
        aPrioriValuesList.add(new APrioriValue(0F, 1F));
        aPrioriValuesList.add(new APrioriValue(0F, DynamicVMP.migratedMemoryAprioriTime.get(timeUnit)));

        return aPrioriValuesList;
    }

    /**
     * 终止执行器服务，等待任务完成或超时后进行强制终止。
     *
     * @param pool 要终止的 ExecutorService
     */
    public static void executorServiceTermination(ExecutorService pool) {
        pool.shutdown(); //禁用提交新任务
        try {
            //等待现有任务终止
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); //取消当前正在执行的任务
                //等待任务响应取消的一段时间
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    logger.log(Level.INFO, "Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // 如果当前线程也被中断，则（重新）取消
            pool.shutdownNow();
            // 保留中断状态
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取具有指定配置的多目标膜算法设置。
     *
     * @param isFullMeme 指示是否使用完整的膜算法配置
     * @return 多目标膜算法设置
     */
    public static MASettings getMemeConfig(boolean isFullMeme) {
        MASettings settings = new MASettings();
        //设置交叉概率
        settings.setCrossoverProb(1.0);
        //设置资源数量
        settings.setNumberOfResources(3);
        //设置目标函数数量
        settings.setNumberOfObjFunctions(4);
        //设置算法的预计执行市场
        settings.setExecutionDuration(Parameter.EXECUTION_DURATION);
        //设置算法执行的间隔
        settings.setExecutionInterval(Parameter.INTERVAL_EXECUTION_MEMETIC);
        //设置是否考虑容错约束
        settings.setFaultTolerance(Parameter.FAULT_TOLERANCE);
        if (isFullMeme) {
            //使用完整的ma算法配置
            settings.setPopulationSize(Parameter.POPULATION_SIZE);
            settings.setNumberOfGenerations(Parameter.NUMBER_GENERATIONS);
        } else {
            //使用默认的ma算法配置
            settings.setPopulationSize(10);
            settings.setNumberOfGenerations(10);
        }
        return settings;
    }
    /**
     * 将给定值标准化到指定范围内。
     *
     * @param value    待标准化的值
     * @param minValue 最小值
     * @param maxValue 最大值
     * @return 标准化后的值，范围在 [0, 1] 之间
     */
    public static Float normalizeValue(Float value, Float minValue, Float maxValue) {
        // 处理特殊情况：当值为 0 或最大值等于最小值时，返回 0
        if (value == 0 || maxValue.equals(minValue)) {
            return 0F;
        }
        //标准化公式返回值
        return (value - minValue) / (maxValue - minValue);
    }


    /**
     * 从放置方案中移除已过期的虚拟机。
     *
     * @param placement 放置方案
     * @param currentTimeUnit 当前时间单元
     * @param numberOfResources 资源数量
     */
    public static void removeDeadVMsFromPlacement(Placement placement, Integer currentTimeUnit, Integer numberOfResources) {
        Integer iteratorResource;
        List<VirtualMachine> toRemoveVMs = new ArrayList<>();
        List<PhysicalMachine> physicalMachineList = placement.getPhysicalMachines();
        PhysicalMachine pm;
        Float resourceUpdate;
        // 收集需要从放置方案中移除的虚拟机
        for (VirtualMachine vm : placement.getVirtualMachineList()) {
            //如果虚拟机过期
            if (vm.getTend() < currentTimeUnit) {
                toRemoveVMs.add(vm);
                //更新虚拟机占用资源的物理机资源请求量
                pm = PhysicalMachine.getById(vm.getPhysicalMachine(), physicalMachineList);
                for (iteratorResource = 0; iteratorResource < numberOfResources; iteratorResource++) {
                    resourceUpdate = vm.getResources().get(iteratorResource) * (vm.getUtilization().get(iteratorResource) / 100);
                    updatePMResRequested(pm, iteratorResource, resourceUpdate, false);
                }
            }
        }
        // 从放置方案中移除已过期的虚拟机
        placement.getVirtualMachineList().removeAll(toRemoveVMs);
        toRemoveVMs.clear();
        // 收集需要从放置方案中移除的衍生虚拟机
        placement.getDerivedVMs().forEach(dvm -> {
            if (dvm.getTend() <= currentTimeUnit) {
                toRemoveVMs.add(dvm);
            }
        });
        // 从放置方案中移除已过期的衍生虚拟机
        placement.getDerivedVMs().removeAll(toRemoveVMs);
        toRemoveVMs.clear();
    }


    /**
     * 移除已迁移的虚拟机中已经过期的虚拟机。
     *
     * @param vmsToMigrate 虚拟机迁移列表
     * @param currentTimeUnit 当前时间单元
     */
    public static void removeDeadVMsMigrated(List<VirtualMachine> vmsToMigrate, Integer currentTimeUnit) {
        // 用于存储需要移除的虚拟机列表
        List<VirtualMachine> toRemoveVMs = new ArrayList<>();
        // 遍历虚拟机迁移列表
        vmsToMigrate.forEach(mvm -> {
            // 如果虚拟机已过期（结束时间小于等于当前时间单元），则添加到移除列表中
            if (mvm.getTend() <= currentTimeUnit) {
                toRemoveVMs.add(mvm);
            }
        });
        // 移除已过期的虚拟机
        vmsToMigrate.removeAll(toRemoveVMs);
    }


    /**
     * 获取虚拟机迁移结束的时间列表。
     *
     * @param migratedVirtualMachines 已迁移的虚拟机列表
     * @param currentTimeUnit 当前时间单元
     * @return 包含虚拟机迁移结束时间的列表
     */
    public static List<Integer> getTimeEndMigrationByVM(final List<VirtualMachine> migratedVirtualMachines, final Integer currentTimeUnit) {
        final Integer byteToBitsFactor = 8;
        // 用于存储虚拟机迁移结束时间的列表
        List<Integer> timeEndMigrationList = new ArrayList<>();
        Integer timeEndMigrationSec;
        Integer vmEndTimeMigration;
        // 遍历已迁移的虚拟机列表
        for (VirtualMachine vm : migratedVirtualMachines) {
            // 计算迁移结束所需的时间（以秒为单位）
            timeEndMigrationSec = (int) Math.ceil((double) vm.getResources().get(ResourcesEnum.RAM.getIndex()) * byteToBitsFactor / Parameter.LINK_CAPACITY);
            // 计算虚拟机迁移结束的时间单元
            vmEndTimeMigration = currentTimeUnit + secondsToTimeUnit(timeEndMigrationSec, Constant.TIMEUNIT_DURATION);
            // 将虚拟机迁移结束时间添加到列表中
            timeEndMigrationList.add(vmEndTimeMigration);
        }
        return timeEndMigrationList;
    }
    /**
     * 获取需要迁移的虚拟机列表。
     *
     * @param reconfPlacement 重新配置的虚拟机放置方案列表
     * @param actualPlacement 当前实际的虚拟机放置方案列表
     * @return 需要迁移的虚拟机列表
     */
    public static List<VirtualMachine> getVMsToMigrate(List<VirtualMachine> reconfPlacement, List<VirtualMachine> actualPlacement) {
        int iterator;
        int actualPosition;
        int newPosition;
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        // 遍历虚拟机列表
        for (iterator = 0; iterator < (actualPlacement.size()); iterator++) {
            if (iterator == reconfPlacement.size()) break;
            //   System.out.printf("\n%d %d %d", actualPlacement.size(), reconfPlacement.size(), iterator);
            // 获取当前虚拟机的实际位置和重新配置后的位置
            actualPosition = actualPlacement.get(iterator).getPhysicalMachine();
            newPosition = reconfPlacement.get(iterator).getPhysicalMachine();
            // 如果实际位置和重新配置后的位置不同且重新配置位置不为零，则需要迁移
            if (actualPosition != newPosition && newPosition != 0) {
                vmsToMigrate.add(reconfPlacement.get(iterator));
            }
        }
        return vmsToMigrate;
    }

    /**
     * 计算放置方案的得分。
     *
     * @param objectiveFuntions 放置方案的目标函数值数组
     * @param aPrioriValuesList  各目标函数的先验值列表
     * @return 计算得到的放置方案得分
     */
    public static Float calcPlacemenScore(Float[] objectiveFuntions, List<APrioriValue> aPrioriValuesList) {
        Float normalizedValue;
        APrioriValue aPrioriValue;
        List<Float> normalizedValues = new ArrayList<>();
        // 遍历各个目标函数
        int iteratorObjFuncts;
        for (iteratorObjFuncts = 0; iteratorObjFuncts < Constant.NUM_OBJ_FUNCT_COMP; iteratorObjFuncts++) {
            aPrioriValue = aPrioriValuesList.get(iteratorObjFuncts);
            // 根据先验值对目标函数值进行归一化
            normalizedValue = Utils.normalizeValue(objectiveFuntions[iteratorObjFuncts], aPrioriValue.getMinValue()
                    , aPrioriValue.getMaxValue());
            normalizedValues.add(normalizedValue);
        }
        // 使用标量化方法计算最终得分
        return ObjectivesFunctions.getScalarizationMethod(normalizedValues, Constant.WEIGHT_OFFLINE);
    }

    /**
     * 更新物理机的资源请求量。
     *
     * @param physicalMachine 物理机对象
     * @param resourceIndex 资源索引
     * @param resource 资源量
     * @param add 是否为加操作（true 表示加，false 表示减）
     */
    public static void updatePMResRequested(PhysicalMachine physicalMachine, Integer resourceIndex, Float resource, Boolean add) {
        Float actualResourceRequested;
        Float newResourceRequested;
        // 获取当前资源请求量
        actualResourceRequested = physicalMachine.getResourcesRequested().get(resourceIndex);
        // 计算新的资源请求量
        newResourceRequested = add.equals(true) ? actualResourceRequested + resource : actualResourceRequested - resource;
        // 更新物理机的资源请求量
        physicalMachine.getResourcesRequested().set(resourceIndex, newResourceRequested);
    }


    /**
     * 获取虚拟机迁移结束时间列表中的最大结束迁移时间。
     *
     * @param endTimesMigration 虚拟机迁移结束时间的列表
     * @return 最大结束迁移时间，如果列表为空则返回0
     */
    public static Integer getMigrationEndTime(final List<Integer> endTimesMigration) {
        Integer largestEndTime = 0;
        for (Integer endTime : endTimesMigration) {
            if (endTime > largestEndTime) {
                largestEndTime = endTime;
            }
        }
        return largestEndTime;
    }

    /**
     * 获取虚拟机迁移结束时间列表中指定虚拟机的结束迁移时间。
     *
     * @param virtualMachineId 要查找的虚拟机的唯一标识符
     * @param vmsToMigrate     虚拟机列表，包含了将要进行迁移的虚拟机
     * @param endTimesMigration 虚拟机迁移结束时间的列表
     * @return 指定虚拟机的结束迁移时间，如果未找到则返回0
     */
    public static Integer getEndTimeMigrationByVm(Integer virtualMachineId, final List<VirtualMachine> vmsToMigrate, final List<Integer> endTimesMigration) {
        VirtualMachine mvm;
        for (int iteratorVM = 0; iteratorVM < vmsToMigrate.size(); iteratorVM++) {
            mvm = vmsToMigrate.get(iteratorVM);
            if (mvm.getId().equals(virtualMachineId)) {
                return endTimesMigration.get(iteratorVM);
            }
        }
        return 0;
    }

    /**
     * 将给定的秒数转换为与时间单位持续时间相对应的时间单位。
     *
     * @param seconds          要转换的秒数
     * @param timeUnitDuration 时间单位的持续时间
     * @return 转换后的时间单位
     */
    public static Integer secondsToTimeUnit(Integer seconds, Float timeUnitDuration) {
        // 使用四舍五入将秒数转换为时间单位
        return Math.round((float) seconds / timeUnitDuration);
    }


    /**
     * 初始化二维矩阵为指定的行数和列数，并将所有元素设置为初始值 0F。
     *
     * @param matrix  要初始化的二维矩阵
     * @param rows    矩阵的行数
     * @param columns 矩阵的列数
     */
    public static void initializeMatrix(Float[][] matrix, Integer rows, Integer columns) {
        for (int iteratorRow = 0; iteratorRow < rows; iteratorRow++) {
            for (int iteratorColumns = 0; iteratorColumns < columns; iteratorColumns++) {
                matrix[iteratorRow][iteratorColumns] = 0F;
            }
        }
    }

    /**
     * 从参数文件流中加载参数，并根据加载的参数进行相应设置。
     *
     * @param scenariosFiles 存储场景文件路径的列表
     * @param stream         参数文件的流
     */
    public static void loadParameter(List<String> scenariosFiles, Stream<String> stream) {
        // 从文件流中过滤非空行并收集为列表
        List<String> parameter = stream.filter(s -> s.length() > 0).collect(Collectors.toList());
        //创建参数映射
        Map<String, Object> parameterMap = new HashMap<>();
        // 解析参数行，将键值对存入参数映射
        parameter.stream()
                 .filter(line -> line.split("=").length > 1)
                 .forEach(line ->
            ((HashMap) parameterMap).put(line.split("=")[0], line.split("=")[1])
        );

        Parameter.ALGORITHM = Integer.parseInt( (String) parameterMap.get("ALGORITHM"));
        Parameter.HEURISTIC_CODE = (String) parameterMap.get("HEURISTIC_CODE");
        Parameter.PM_CONFIG = (String) parameterMap.get("PM_CONFIG");
        Parameter.DERIVE_COST = new Float ((String) parameterMap.get("DERIVE_COST"));
        Parameter.FAULT_TOLERANCE = Boolean.getBoolean( (String) parameterMap.get("FAULT_TOLERANCE"));
        Parameter.PROTECTION_FACTOR =   new Float ((String) parameterMap.get("PROTECTION_FACTOR"));
        Parameter.INTERVAL_EXECUTION_MEMETIC = Integer.parseInt( (String) parameterMap.get
                ("INTERVAL_EXECUTION_MEMETIC"));
        Parameter.POPULATION_SIZE = Integer.parseInt( (String) parameterMap.get("POPULATION_SIZE"));
        Parameter.NUMBER_GENERATIONS = Integer.parseInt( (String) parameterMap.get("NUMBER_GENERATIONS"));
        Parameter.EXECUTION_DURATION = Integer.parseInt( (String) parameterMap.get("EXECUTION_DURATION"));
        Parameter.LINK_CAPACITY =  new Float ((String) parameterMap.get("LINK_CAPACITY"));
        Parameter.MIGRATION_FACTOR_LOAD =  new Float ((String) parameterMap.get("MIGRATION_FACTOR_LOAD"));
        Parameter.HISTORICAL_DATA_SIZE = Integer.parseInt( (String) parameterMap.get("HISTORICAL_DATA_SIZE"));
        Parameter.FORECAST_SIZE =Integer.parseInt( (String)  parameterMap.get("FORECAST_SIZE"));
        Parameter.SCALARIZATION_METHOD = (String) parameterMap.get("SCALARIZATION_METHOD");

        parameter.stream()
                 .filter(line -> line.split("=").length == 1 && !line.equals(SCENARIOS))
                 .forEach(scenariosFiles::add);
    }


    /**
     * 使用双指数平滑方法对给定的时间序列进行平滑，并预测未来 N 个点的值。
     *
     * @param series     输入的时间序列
     * @param alpha      平滑系数 alpha（0 <= alpha <= 1）
     * @param beta       趋势系数 beta（0 <= beta <= 1）
     * @param nForecast  预测的点数
     * @return 经过平滑的序列和未来 N 个点的预测值列表
     */
	public static List<Float>  doubleExponentialSmooth(List<Float> series, Float alpha, Float beta, Integer nForecast){
		List<Float> result = new ArrayList<>();
		Float level = 0F;
		Float trend = 0F;
		Float lastLevel;
        Float lastTrend;
        Float value;

		result.add(series.get(0));
		for(int iterator=1;iterator<series.size(); iterator++){
			if(iterator==1){
				level = series.get(0);
				trend = series.get(1)-series.get(0);
			}

			value = series.get(iterator);
			lastLevel = level;
			level = alpha*value + (1-alpha)*(level + trend);
			lastTrend = trend;
			trend = beta*(level - lastLevel) + (1-beta)*trend;
			result.add(lastLevel+lastTrend);
		}
        // 预测未来 N 个点的值
		lastLevel = level;
		lastTrend = trend;
		for(int n=1;n<=nForecast;n++){
			result.add(lastLevel + n*lastTrend);
		}

		return result;
	}

	/**
	 * 使用双指数平滑计算未来 N 个点的预测值。
     *
	 * @param series    浮点数列表
	 * @param nForecast 预测的点数
	 * @return 未来 N 个点的预测值列表
	 */
	public static List<Float> calculateNForecast(List<Float> series, Integer nForecast){
        // 设置平滑系数
        Float alpha = 0.5F;
        Float beta = 0.5F;
        // 调用双指数平滑方法
        List<Float> result = doubleExponentialSmooth(series, alpha, beta, nForecast);
        // 返回未来 N 个点的预测值列表
        return result.subList(series.size(), result.size());
	}

    /**
     * 根据给定的时间序列和预测大小，判断是否调用重新配置。
     *
     * @param series        用于预测的时间序列
     * @param forecastSize  预测的大小
     * @return 如果满足条件 "f1(x)<f2(x)<...<fn(x)"，则返回 true，否则返回 false
     */
    public static Boolean callToReconfiguration(List<Float> series, Integer forecastSize) {
        List<Float> resultForecasting = calculateNForecast(series, forecastSize);
        // 检查是否满足调用重新配置的条件 "f1(x)<f2(x)<...<fn(x)"
        for (int i = 1; i < resultForecasting.size(); i++) {
            if (resultForecasting.get(i - 1).compareTo(resultForecasting.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算给定浮点数列表的平均值。
     *
     * @param listNumbers 包含浮点数的列表
     * @return 列表中浮点数的平均值，如果列表为空则返回0
     */
    public static Float average(List<Float> listNumbers) {
        Float sum = 0F;
        for (Float n : listNumbers) {
            sum += n;
        }
        return listNumbers.isEmpty() ? 0 : sum / listNumbers.size();
    }

    /**
     * 计算按时间分组的功耗值的归一化平均值。
     *
     * @param pwConsumptionByTime Map，表示按时间分组的功耗值
     * @return 归一化的平均功耗值
     */
    public static Float getAvgPwConsumptionNormalized(Map<Integer, Float> pwConsumptionByTime) {
        // 将 Map 中的功耗值按时间分组，然后进行归一化处理，并存储在列表中
        List<Float> pwConsumptionNormalizedList = pwConsumptionByTime.entrySet().stream().map(pw -> {
            Float revenue = pw.getValue();
            return normalizeValue(revenue, ObjectivesFunctions.MIN_POWER, DynamicVMP.maxPower);
        }).collect(Collectors.toList());
        // 计算列表的平均值并返回
        return average(pwConsumptionNormalizedList);
    }

    /**
     * 计算按时间分组的原始收入值的归一化平均值。
     *
     * @param revenueByTime Map，表示按时间分组的原始收入值
     * @return 归一化的平均收入值
     */
    public static Float getAvgRevenueNormalized(Map<Integer, Float> revenueByTime) {
        // 将 Map 中的原始收入值按时间分组，然后进行归一化处理，并存储在列表中
        List<Float> revenueNormalizedList = revenueByTime.entrySet().stream().map(r -> {
            Integer timeUnit = r.getKey();
            Float revenue = r.getValue();
            Float maxRevenueValue = DynamicVMP.revenueAprioriTime.get(timeUnit);
            return normalizeValue(revenue, ObjectivesFunctions.MIN_REVENUE, maxRevenueValue);
        }).collect(Collectors.toList());
        // 计算列表的平均值并返回
        return average(revenueNormalizedList);
    }


    /**
     * 计算归一化的平均浪费资源值。
     *
     * @param wastedResourcesByTime Map，表示按时间分组的浪费资源值
     * @return 归一化的平均浪费资源值
     */
    public static Float getAvgResourcesWNormalized(Map<Integer, Float> wastedResourcesByTime) {
        // 将 Map 中的浪费资源值提取为列表
        List<Float> wastedResourcesList = wastedResourcesByTime.entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return average(wastedResourcesList);// 计算列表的平均值并返回
    }

    /**
     * 获取需要迁移的虚拟机列表。
     *
     * @param pm       物理机实例
     * @param vmsInPM  物理机上的虚拟机列表
     * @return 需要迁移的虚拟机列表
     */
    public static List<VirtualMachine> getVMsToMigrate(PhysicalMachine pm, List<VirtualMachine> vmsInPM) {
        //复制物理机，以免影响原有数据
        PhysicalMachine pmCopy = new PhysicalMachine(pm.getId(),pm.getPowerMax(),pm.getResources(),pm.getResourcesRequested(),
                pm.getUtilization());
        //存储需要迁移的虚拟机列表
        List<VirtualMachine> vmsToMigrate = new ArrayList<>();
        // 创建内存比较器，用于按照内存大小排序虚拟机
        MemoryComparator comparator =  new MemoryComparator();
        VirtualMachine vm;
        Integer vmIterator=0;
        Collections.sort(vmsInPM, comparator);
        //循环遍历物理机直到所有物理机都不过载
        while (!Constraints.isPMOverloaded(pmCopy) || vmIterator.equals(vmsInPM.size())){
            vm = vmsInPM.get(vmIterator);
            vmsToMigrate.add(vm);
            pmCopy.updatePMResources(vm,Utils.SUB);
            vmIterator++;
        }

        return vmsToMigrate;
    }

    /**
     * 更新虚拟机迁移结束时间。
     *
     * @param vmsToMigrate        待迁移的虚拟机列表
     * @param vmsMigrationEndTimes 虚拟机迁移结束时间列表
     * @param vmEndTimeMigration   虚拟机迁移的结束时间
     * @param vmMigrating          正在迁移的虚拟机实例
     * @return 更新后的虚拟机迁移结束时间
     */
    public static Integer updateVmEndTimeMigration(final List<VirtualMachine> vmsToMigrate,
                                                   final List<Integer> vmsMigrationEndTimes, Integer vmEndTimeMigration,
                                                   final VirtualMachine vmMigrating) {
        // 用于存储更新后的虚拟机迁移结束时间
        Integer newEndTimeMigration;
        // 如果正在迁移的虚拟机实例不为空，获取其新的结束时间
        if (vmMigrating != null) {
            newEndTimeMigration = Utils.getEndTimeMigrationByVm(vmMigrating.getId(), vmsToMigrate, vmsMigrationEndTimes);
        } else {
            // 否则，使用传入的虚拟机迁移结束时间
            newEndTimeMigration = vmEndTimeMigration;
        }
        return newEndTimeMigration;
    }

	/**
	 * 检查路径是否存在，不存在就创建
	 * @param pathFolders
	 */
    public static void checkPathFolders(String pathFolders) {
        Path path = Paths.get(pathFolders);

        if (!Files.exists(path)) {
            File dir = new File(pathFolders);
            dir.mkdirs();
        }
    }


    /**
     * 预处理输入输出路径，设置全局静态变量 INPUT 和 OUTPUT。
     *
     * @param parametersFilePath 参数文件的路径
     * @param outputFolderPath   输出文件夹的路径
     */
    public static void preprocessInputOutputPaths(String parametersFilePath, String outputFolderPath) {
        // 获取输入参数文件的路径
        Path inputParametersPath = Paths.get(parametersFilePath);
        checkPathFolders(outputFolderPath);//检查路径是否存在，不存在就创建
        // 设置全局静态变量 INPUT 和 OUTPUT
        Utils.INPUT = inputParametersPath.getParent().toString() + "/";
        Utils.OUTPUT = outputFolderPath;
    }

}