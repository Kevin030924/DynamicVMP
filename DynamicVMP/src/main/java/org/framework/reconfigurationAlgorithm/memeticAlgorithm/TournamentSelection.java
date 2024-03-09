package org.framework.reconfigurationAlgorithm.memeticAlgorithm;

import org.framework.Utils;
import org.framework.comparator.DistanceComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * 个体选择接口的具体实现
 */
public class TournamentSelection implements Selection {
    private final FitnessComparator comparator;//个体适应度比较的比较器
    private int size;//每轮比较的个体数
    //默认构造函数，个体数为2，使用默认的适应度比较器
    public TournamentSelection() {
        this(2, new DistanceComparator(false));
    }
    /**
     * 带参数的构造函数，允许指定选择的比较大小和适应度比较器。
     * @param size       锦标赛选择的比较大小，即每轮比较的个体数量
     * @param comparator 用于比较个体适应度的比较器
     */
    public TournamentSelection(int size, FitnessComparator comparator) {
        this.size = size;
        this.comparator = comparator;
    }
    /**
     * 从种群中选择多个个体的方法。
     * @param population 种群对象
     * @param arity      选择的个体数量
     * @return 包含所选个体的列表
     */
    @Override
    public List<Individual> select(Population population, int arity) {
        List<Individual> parents = new ArrayList<>();
        //多轮选择，选择出arity个个体
        while (parents.size() <= arity) {
            Individual individual = select(population);
            parents.add(individual);
        }
        return parents;
    }
    /**
     * 从种群中选择一个个体的方法。
     * @param population 种群对象
     * @return 所选的单个个体
     */
    @Override
    public Individual select(Population population) {
        //随机选择一个个体作为初始选择个体
        Individual winner = population.getIndividual(Utils.getRandomtInt(population.size()));
        for (int iterator = 1; iterator < size; iterator++) {
            //随机选择候选者
            Individual candidate = population.getIndividual(Utils.getRandomtInt(population.size()));
            //利用适应度比较器进行比较
            int result = comparator.compare(winner, candidate);
            //选择适应度较好的个体
            if (result > 0) {
                winner = candidate;
            }
        }
        return winner;
    }
}