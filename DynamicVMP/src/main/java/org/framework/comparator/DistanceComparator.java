package org.framework.comparator;

import org.framework.reconfigurationAlgorithm.memeticAlgorithm.FitnessComparator;
import org.framework.reconfigurationAlgorithm.memeticAlgorithm.Individual;

import java.util.Comparator;

/**
 * 比较{@link Individual}的适应度值的比较器。
 */
public class DistanceComparator implements FitnessComparator, Comparator<Individual> {
    private final Boolean largerValuesPreferred;// 用于确定比较时是否更喜欢较大的适应度值
    /**
     * 构造函数，接受一个Boolean参数，表示是否更喜欢较大的适应度值
     * @param largerValuesPreferred 是否更喜欢较大的适应度值
     */
    public DistanceComparator(Boolean largerValuesPreferred) {

        this.largerValuesPreferred = largerValuesPreferred;
    }
    //比较两个个体的适应度值大小
    @Override
    public int compare(Individual individual1, Individual individual2) {
        // 根据largerValuesPreferred的值决定比较逻辑
        if (largerValuesPreferred) {
            return Double.compare(individual1.getFitness(), individual2.getFitness()) * -1;
        } else {
            return Double.compare(individual1.getFitness(), individual2.getFitness());
        }

    }
}
