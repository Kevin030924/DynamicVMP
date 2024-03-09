package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import java.util.List;
//从种群中选择个体，以用于后续的遗传算子（如交叉和变异）
public interface Selection {
    //选择多个个体
    List<Individual> select(Population population, int arity);
    //选择一个个体
    Individual select(Population population);

}

