package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;
//初始化类，负责种群初始化
public class Initialization {
    /**
     * 初始化Memetic算法的种群。
     * @param numberOfVMs     虚拟机的数量
     * @param numberOfPMs     物理机的数量
     * @param maSettings      Memetic算法的设置
     * @return 初始化后的种群
     */
    public Population initialize(int numberOfVMs, int numberOfPMs, MASettings maSettings) {
        Population population = new Population();//创建种群对象
        //循环创建种群中的个体
        for (int iteratorIndividual = 0; iteratorIndividual < maSettings.getPopulationSize(); iteratorIndividual++) {
            Individual individual = new Individual(maSettings.getNumberOfObjFunctions(), numberOfVMs, numberOfPMs, maSettings.getNumberOfResources());
            // 为个体的每个解生成位置
            for (int iteratorSolution = 0; iteratorSolution < numberOfVMs; iteratorSolution++) {
                individual.getSolution()[iteratorSolution] = generateSolutionPosition(numberOfPMs, false);
            }
            population.getIndividuals().add(individual);
        }
        return population;
    }
    /**
     * 生成解的位置。
     * @param maxPossible 最大可能的位置
     * @param includeZero 是否包括零在内
     * @return 生成的解的位置
     */
    public int generateSolutionPosition(int maxPossible, boolean includeZero) {
        //根据条件生成随机位置
        if (includeZero) {
            return Utils.getRandomInt(0, maxPossible);
        } else {
            return Utils.getRandomInt(1, maxPossible);
        }
    }
}
