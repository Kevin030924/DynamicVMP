package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

//变异操作的接口
public interface Mutation {
    Population mutate(Population population);//对种群进行变异操作
    Individual mutate(Individual individual);//对个体进行变异操作
}
