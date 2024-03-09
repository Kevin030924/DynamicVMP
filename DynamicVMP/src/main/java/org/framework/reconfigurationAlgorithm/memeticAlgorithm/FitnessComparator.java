package org.framework.reconfigurationAlgorithm.memeticAlgorithm;
//定义一个用于比较个体适应度的比较器
public interface FitnessComparator {
     int compare(Individual individual1, Individual individual2);
}
