package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;

import java.util.List;
public class OnePointCrossover implements Crossover {
    private final Double probability;//交叉概率
    // 构造函数，接受交叉概率作为参数
    public OnePointCrossover(Double probability) {
        this.probability = probability;
    }
    // 实现Crossover接口的方法，对一组父代个体进行交叉操作
    @Override
    public Population crossover(List<Individual> parents, int arity) {
        Population population = new Population();//创建population存储新的子代群体
        Individual parent1, parent2;
        //遍历父代个体列表
        for (int iteratorIndividual = 0; iteratorIndividual < parents.size(); iteratorIndividual++) {
            parent1 = parents.get(iteratorIndividual);
            // 选择另一个父代个体，确保两两配对
            parent2 = iteratorIndividual % 2 == 0 ? parents.get(iteratorIndividual + 1) : parents.get(iteratorIndividual - 1);
            // 调用具体的交叉方法，得到两个子代个体并添加到population中
            Individual[] result = crossover(parent1, parent2);
            population.getIndividuals().add(result[0]);
            population.getIndividuals().add(result[1]);
            // 如果生成的子代个体数量达到要求的数量，停止
            if (population.size() >= arity) break;
        }
        // 截断Population，确保其大小不超过arity
        population.truncate(arity);
        return population;
    }
    // 实现Crossover接口的方法，对两个父代个体进行交叉操作
    @Override
    public Individual[] crossover(Individual individual1, Individual individual2) {
        int crossoverPoint, temp;
        int individualSize = individual1.getSize();
        // 复制两个父代个体，不能修改原始个体
        Individual result1 = individual1.copy();
        Individual result2 = individual2.copy();
        // 根据概率来确定是否进行交叉
        if (Utils.getRandomDouble() <= probability) {
            // 确定交叉点，取个体长度的一半
            if (individual1.getSize() % 2 == 0) {
                crossoverPoint = individualSize / 2;
            } else {
                crossoverPoint = individualSize / 2 + 1;
            }
            // 执行交叉操作
            for (int iterator = 0; iterator < crossoverPoint; iterator++) {
                temp = result1.getSolution()[iterator];
                result1.getSolution()[iterator] = result2.getSolution()[iterator];
                result2.getSolution()[iterator] = temp;
            }

        }
        return new Individual[]{result1, result2};
    }
}
