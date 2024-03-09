package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Population类表示Memetic算法中的一个种群，包含多个个体。
 */
public class Population {
    private List<Individual> individuals;
    public Population() {
        this.individuals = new ArrayList<>();
    }
    /**
     * 获取种群中的个体列表。
     * @return 种群中的个体列表
     */
    public List<Individual> getIndividuals() {
        return individuals;
    }
    /**
     * 设置种群中的个体列表。
     * @param individuals 新的个体列表
     */
    public void setIndividuals(List<Individual> individuals) {
        this.individuals = individuals;
    }
    /**
     * 使用指定的比较器对种群中的个体进行排序。
     * @param comparator 比较器
     */
    public void sort(Comparator<? super Individual> comparator) {
        Collections.sort(individuals, comparator);
    }
    /**
     * 获取种群中指定索引位置的个体。
     * @param index 索引位置
     * @return 种群中的个体
     */
    public Individual getIndividual(int index) {
        return individuals.get(index);
    }
    /**
     * 设置种群中指定索引位置的个体。
     * @param individual 新的个体
     * @param index      索引位置
     */
    public void setIndividual(Individual individual, int index) {
        this.getIndividuals().set(index, individual);
    }
    /**
     * 截断种群，使其大小不超过指定的大小。
     * @param size 指定的大小
     */
    public void truncate(int size) {
        while (individuals.size() > size) {
            individuals.remove(individuals.size() - 1);
        }
    }
    /**
     * 获取种群的大小（包含的个体数量）。
     * @return 种群的大小
     */
    public int size() {
        return individuals.size();
    }
}
