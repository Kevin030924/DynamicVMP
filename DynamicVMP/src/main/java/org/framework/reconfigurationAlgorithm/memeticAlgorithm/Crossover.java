package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import java.util.List;
/**
 * 定义MA算法中的交叉操作
 */
public interface Crossover {
    Population crossover(List<Individual> parents, int arity);//对父代个体群体进行交叉操作返回子代群体
    Individual[] crossover(Individual individual1, Individual individual2);//对两个父代个体进行交叉操作返回一个自带个体
}

