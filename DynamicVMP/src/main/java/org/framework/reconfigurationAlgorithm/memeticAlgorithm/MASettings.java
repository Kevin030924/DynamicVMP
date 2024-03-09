package org.framework.reconfigurationAlgorithm.memeticAlgorithm;


/**
 * MASettings类用于存储Memetic算法的相关设置。
 */
public class MASettings  {
	// 种群大小
	private Integer populationSize;
	// 迭代次数
	private Integer numberOfGenerations;
	// 资源数量
	private Integer numberOfResources;
	// 目标函数数量
	private Integer numberOfObjFunctions;
	// 交叉概率
	private Double crossoverProb;
	// 执行间隔
	private Integer executionInterval;
	// 执行持续时间
	private Integer executionDuration;
	// 容错性
	private Boolean faultTolerance;
	// 默认构造函数
	public MASettings() {
	}
	// 获取种群大小
	public Integer getPopulationSize() {
		return populationSize;
	}
	// 设置种群大小
	public void setPopulationSize(Integer populationSize) {
		this.populationSize = populationSize;
	}
	// 获取迭代次数
	public Integer getNumberOfGenerations() {
		return numberOfGenerations;
	}
	// 设置迭代次数
	public void setNumberOfGenerations(Integer numberOfGenerations) {
		this.numberOfGenerations = numberOfGenerations;
	}
	// 获取资源数量
	public Integer getNumberOfResources() {
		return numberOfResources;
	}
	// 设置资源数量
	public void setNumberOfResources(Integer numberOfResources) {
		this.numberOfResources = numberOfResources;
	}
	// 获取目标函数数量
	public Integer getNumberOfObjFunctions() {
		return numberOfObjFunctions;
	}
	// 设置目标函数数量
	public void setNumberOfObjFunctions(Integer numberOfObjFunctions) {
		this.numberOfObjFunctions = numberOfObjFunctions;
	}
	// 获取交叉概率
	public Double getCrossoverProb() {
		return crossoverProb;
	}
	// 设置交叉概率
	public void setCrossoverProb(Double crossoverProb) {
		this.crossoverProb = crossoverProb;
	}
	// 获取容错性
	public Boolean getFaultTolerance() {
		return faultTolerance;
	}
	// 设置容错性
	public void setFaultTolerance(Boolean faultTolerance) {
		this.faultTolerance = faultTolerance;
	}
	// 获取执行间隔
	public Integer getExecutionInterval() {
		return executionInterval;
	}
	// 设置执行间隔
	public void setExecutionInterval(Integer executionInterval) {
		this.executionInterval = executionInterval;
	}
	// 获取执行持续时间
	public Integer getExecutionDuration() {
		return executionDuration;
	}
	// 设置执行持续时间
	public void setExecutionDuration(Integer executionDuration) {
		this.executionDuration = executionDuration;
	}
}
