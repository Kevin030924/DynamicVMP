package org.framework.algorithm.stateOfArt;

import org.domain.Scenario;

import java.util.List;
/**
 * StateOfArt算法的实用工具类，包含与VM请求在memetic执行期间相关的方法。
 */
public class StateOfArtUtils {
    /**
     * 检查在memetic执行时间段内是否有新的VM请求。
     *
     * @param workload        工作负载场景
     * @param memeticTimeInit Memetic执行时间起始
     * @param memeticTimeEnd  Memetic执行时间结束
     * @return <b>True</b> 如果在迁移期间有VM请求，<b>False</b> 否则
     */
    public static boolean newVmDuringMemeticExecution(List<Scenario> workload, Integer memeticTimeInit,
            Integer memeticTimeEnd) {

        List<Scenario> cloneScenario = Scenario.cloneScenario(workload, memeticTimeInit, memeticTimeEnd);

        for (Scenario request : cloneScenario) {
            if (request.getTime() <= request.getTinit()) {
                return true;
            }
        }

        return false;
    }

}
