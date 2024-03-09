package org.framework.algorithm.onlineApproach;

import org.domain.*;
import org.framework.Constant;
import org.framework.DynamicVMP;
import org.framework.ObjectivesFunctions;
import org.framework.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OnlineApproach {

	public static final String DYNAMIC_VMP_ONLINE = "DynamicVMP: 仅在线方法";

	private static Logger logger = DynamicVMP.getLogger();

	private OnlineApproach() {
		// 默认构造函数
	}

	/**
	 * 在线方法管理器
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
	 */
	public static void onlineApproachManager(List<Scenario> workload, List<PhysicalMachine> physicalMachines,
											 List<VirtualMachine>
													 virtualMachines, List<VirtualMachine> derivedVMs,
											 Map<Integer, Float> revenueByTime, List<Resources> wastedResources, Map<Integer, Float> wastedResourcesRatioByTime,
											 Map<Integer, Float> powerByTime, Map<Integer, Placement> placements, Integer code, Integer timeUnit,
											 Integer[] requestsProcess, Float maxPower, String scenarioFile)
			throws IOException {

		Integer actualTimeUnit;
		Integer nextTimeUnit;

		for (int iterator = 0; iterator < workload.size(); ++iterator) {
			Scenario request = workload.get(iterator);
			actualTimeUnit = request.getTime();
			// 如果是最后一个请求，则将nextTimeUnit赋值为-1。
			nextTimeUnit = iterator + 1 == workload.size() ? -1 : workload.get(iterator + 1).getTime();

			DynamicVMP.runHeuristics(request, code, physicalMachines, virtualMachines, derivedVMs, requestsProcess, false);

			// 检查是否是最后一个请求或将发生时间单位的变化。
			if (nextTimeUnit == -1 || !actualTimeUnit.equals(nextTimeUnit)) {

				ObjectivesFunctions.getObjectiveFunctionsByTime(physicalMachines,
						virtualMachines, derivedVMs, wastedResources,
						wastedResourcesRatioByTime, powerByTime, revenueByTime, timeUnit, actualTimeUnit);

				Float placementScore = ObjectivesFunctions.getDistanceOrigenByTime(request.getTime(),
						maxPower, powerByTime, revenueByTime, wastedResourcesRatioByTime);

				Utils.checkPathFolders(Constant.PLACEMENT_SCORE_BY_TIME_FILE);
				// 打印时间t的放置分数
				Utils.printToFile( Constant.PLACEMENT_SCORE_BY_TIME_FILE + scenarioFile, placementScore);

				timeUnit = actualTimeUnit;

				Placement heuristicPlacement = new Placement(PhysicalMachine.clonePMsList(physicalMachines),
						VirtualMachine.cloneVMsList(virtualMachines),
						VirtualMachine.cloneVMsList(derivedVMs), placementScore);
				placements.put(actualTimeUnit, heuristicPlacement);

			}
		}

	}
}
