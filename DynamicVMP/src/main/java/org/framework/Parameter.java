package org.framework;

public class Parameter {

    private Parameter() {
        // Default Constructor
    }

    // EXPERIMENTS PARAMETERS
    /**
     * 启发式算法代码
     */
    public static String HEURISTIC_CODE;

    /**
     * 物理机配置
     */
    public static String PM_CONFIG;

    /**
     * 衍生 VM 惩罚成本
     */
    public static Float DERIVE_COST;

    /**
     * FAULT_TOLERANCE：指示是否应用故障容忍。
     * <ul>
     *     <li>
     *         <b>True</b>，来自相同服务的 VM 不能放置在同一台 PM 上
     *     </li>
     *     <li>
     *         <b>False</b>，来自相同服务的 VM 可以放置在同一台 PM 上
     *     </li>
     * </ul>
     */
    public static Boolean FAULT_TOLERANCE;

    /**
     * 保护因子：
     * 可以取 0 到 1 之间的值，其中
     * <ul>
     *     <li>
     *         0 = 无过度预订
     *     </li>
     *     <li>
     *         1 = 完全过度预订（违反 SLA 的风险较高）
     *     </li>
     * </ul>
     */
    public static Float PROTECTION_FACTOR;

    /**
     * 间隔执行 Memetic 算法
     */
    public static Integer INTERVAL_EXECUTION_MEMETIC;

    /**
     * 种群大小
     */
    public static Integer POPULATION_SIZE;

    /**
     * 世代数
     */
    public static Integer NUMBER_GENERATIONS;

    /**
     * 执行持续时间
     */
    public static Integer EXECUTION_DURATION;

    /**
     * 链路容量，单位 Gbps
     */
    public static Float LINK_CAPACITY;

    /**
     * 迁移过程激活时的过载百分比
     */
    public static Float MIGRATION_FACTOR_LOAD;

    /**
     * 历史目标函数值的大小
     */
    public static Integer HISTORICAL_DATA_SIZE;

    /**
     * 要预测的值的数量
     */
    public static Integer FORECAST_SIZE;

    /**
     * 标量化方法映射
     * ED = 欧几里得距离
     * CD = 切比雪夫距离
     * MD = 曼哈顿距离
     * WS = 加权和
     */
    public static String SCALARIZATION_METHOD;


    public static Integer ALGORITHM;
}
