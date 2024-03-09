基础设施即服务（IaaS）提供商必须在复杂动态的云计算环境中支持对虚拟资源的请求，考虑到服务弹性和物理资源的超售情况。由于客户请求的随机性，虚拟机放置（VMP）问题应该在不确定性下进行表述。本文提出了一个针对VMP问题的两阶段优化方案的实验评估，研究了不同的在线启发式方法、超售保护因子以及目标函数标量化方法。所提出的实验评估考虑了不确定的VMP表述，用于优化以下三个目标函数：（i）功耗、（ii）经济收入和（iii）资源利用率。实验考虑了96种不同场景，代表复杂的云计算环境。实验结果表明，在增量VMP（iVMP）阶段中推荐使用Best-Fit和Best-Fit Decreasing启发式算法，同时在低CPU负载场景下将保护因子调整为0.00，在高CPU负载场景下调整为0.75，并使用考虑到目标函数的欧几里德距离的标量化方法。

开发和执行以下项目需要：

开发所需：

Maven 3或更高版本
Java 8（JDK 1.8）
Java IDE（例如eclipse、intellij等）
运行所需：

框架可以使用maven编译。

进入项目根目录并执行：

bash
Copy code
$ mvn clean package
在target目录中是编译后的框架，要执行，请使用以下命令：

bash
Copy code
$ java -jar target/DynamicVMPFramework.jar parameter
输入文件：

parameter：配置文件和场景

参数文件结构：

APPROACH = 算法方法
CENTRALIZED
DISTRIBUTED（此方法将自动启动分布式方法，您无需指定以下输入：VMPr、VMPr_TRIGGERING、VMPr_RECOVERING。）
iVMP = 增量阶段的算法（iVMP）。
FF → First Fit
BF → Best Fit
WF → Worst Fit
FFD → First Fit Decreasing
BFD → Best Fit Decreasing
VMPr = 重新配置阶段的算法（VMPr）。
MEMETIC → Memetic Algorithm
ACO → Ant Colony Optimization
VMPr_TRIGGERING = VMPr触发策略
PERIODICALLY
PREDICTION-BASED
VMPr_RECOVERING = VMPr恢复策略
CANCELLATION
UPDATE-BASED
PM_CONFIG = 负载CPU配置
LOW → (<10%)
MED → (<30%)
HIGH → (<80%)
FULL → (<95%)
SATURATED → (<120%)
DERIVE_COST = 每个派生VM的成本
PROTECTION_FACTOR_01 = 资源1保护因子[0;1]
PROTECTION_FACTOR_02 = 资源2保护因子[0;1]
PROTECTION_FACTOR_03 = 资源3保护因子[0;1]
PENALTY_FACTOR_01 = 资源1处罚因子（大于1）
PENALTY_FACTOR_02 = 资源1处罚因子（大于1）
PENALTY_FACTOR_03 = 资源1处罚因子（大于1）
INTERVAL_EXECUTION_MEMETIC = MA执行的周期时间
POPULATION_SIZE = MA的种群大小
NUMBER_GENERATIONS = MA的世代大小
EXECUTION_DURATION = 持续时间
LINK_CAPACITY = 迁移的链接容量
MIGRATION_FACTOR_LOAD = 迁移的负载因子
HISTORICAL_DATA_SIZE = 历史数据大小
FORECAST_SIZE = 预测大小
SCALARIZATION_METHOD = 标量化方法
ED → 欧几里德距离
MD → 曼哈顿距离
CD → 切比雪夫距离
WS → 加权和
MAX_PHEROMONE = ACO中允许的最大信息素
PHEROMONE_CONSTANT = ACO的信息素常数，范围[0,1]，确定信息素蒸发的速度。随着信息素常数的增加，信息素的蒸发速度变快
N_ANTS = ACO中使用的蚂蚁数量
ACO_ITERATIONS = ACO中执行的迭代次数以返回解决方案
SCENARIOS = 请求列表
输出文件：

框架生成以下文件：

economical_penalties：每个SLA违规的平均经济惩罚。
economical_revenue：主要提供商托管的每个VM的平均经济收入。
leasing_costs：来自联合体备用提供商托管的每个VM的平均经济收入损失。
power_consumption：平均耗电量
reconfiguration_call_times：重新配置调用次数。
wasted_resources：资源浪费的平均值（每个资源一列）
wasted_resources_ratio：资源浪费的平均值（考虑所有资源）
scenarios_scores：每个执行场景的分数。
