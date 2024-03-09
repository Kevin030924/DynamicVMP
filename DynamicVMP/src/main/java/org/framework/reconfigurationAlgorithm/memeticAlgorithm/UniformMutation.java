package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;
public class UniformMutation implements Mutation {


    @Override
    public Population mutate(Population population) {

        for (int iteratorIndividual = 0; iteratorIndividual < population.size(); iteratorIndividual++) {
            // 对每个个体进行变异
            Individual individualMutated = mutate(population.getIndividual(iteratorIndividual));
            // 更新种群中的个体
            population.setIndividual(individualMutated, iteratorIndividual);
        }
        return population;
    }

    @Override
    public Individual mutate(Individual individual) {
        int numberOfVMs = individual.getSolution().length;
        int numberOfPMs = individual.getUtilization().length;
        int oldPhysicalPosition, newPhysicalPosition;
        // 对每个解进行变异
        for (int iteratorSolution = 0; iteratorSolution < numberOfVMs; iteratorSolution++) {
            oldPhysicalPosition = individual.getSolution()[iteratorSolution];
            // 以1/numberOfVMs的概率进行变异
            if (Utils.getRandomDouble() < 1F / numberOfVMs) {
                do {
                    // 生成新的物理位置
                    newPhysicalPosition = Utils.getRandomInt(1, numberOfPMs);
                    // 如果新位置不等于旧位置，进行变异
                    if (newPhysicalPosition != oldPhysicalPosition) {
                        individual.getSolution()[iteratorSolution] = newPhysicalPosition;
                    }
                } while (newPhysicalPosition == oldPhysicalPosition && numberOfPMs > 1);
            }
        }
        return individual;
    }
}