package org.framework.iterativeAlgorithm;

import org.domain.*;
import org.framework.Constraints;
import org.framework.DynamicVMP;
import org.framework.Parameter;
import org.framework.Utils;
import org.framework.comparator.BestComparator;
import org.framework.comparator.WorstComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.domain.VirtualMachine.getById;

/**
 * 启发式代码
 * <p>
 *     所有启发式算法的逻辑
 * </p>
 */
public class Heuristics {

    /**
     * 函数式接口 Algorithm
     */
    @FunctionalInterface
    public interface Algorithm {
        Boolean useHeuristic(VirtualMachine vm,
            List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines,
            List<VirtualMachine> derivedVMs,
            Boolean isMigration);
    }

    /**
     * 同的放置算法，采用函数式接口 Algorithm
     */
    private static Algorithm[] heuristics = new Algorithm[] {
            Heuristics::firstFit,
            Heuristics::bestFit,
            Heuristics::worstFit,
            Heuristics::firstFit,  // First Fit Decreasing
            Heuristics::bestFit,   // Worst Fit Decreasing
    };

    /**
     * 获取可用的放置算法。
     *
     * @return 放置算法数组
     */
    public static Algorithm[] getHeuristics() {
        return heuristics;
    }

    /**
     * 更新虚拟机信息，包括放置、迁移等操作。
     *
     * @param s                 场景对象
     * @param virtualMachines  虚拟机列表
     * @param derivedVMs       衍生的虚拟机列表
     * @param physicalMachines 物理机列表
     * @param isMigrationActive 是否激活迁移
     * @return 更新是否成功
     */
    public static Boolean updateVM(Scenario s, List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs,
            List<PhysicalMachine> physicalMachines, Boolean isMigrationActive) {
        Boolean success = false;
        PhysicalMachine physicalMachine;
        Resources utilization;
        // 如果激活迁移，更新资源利用率
        if(isMigrationActive) {
            utilization = new Resources(
                    s.getUtilization().getCpu() + Parameter.MIGRATION_FACTOR_LOAD,
                    s.getUtilization().getRam(),
                    s.getUtilization().getNet());
        } else {
            utilization = s.getUtilization();
        }

        // 创建更新后的虚拟机对象
        VirtualMachine updatedVM = new VirtualMachine(s.getVirtualMachineID(), s.getResources(), s.getRevenue(),
                s.getTinit(), s.getTend(), utilization, s.getDatacenterID(), s.getCloudServiceID(), null);
        //查找被分配的虚拟机
        VirtualMachine vm = getById(updatedVM.getId(), updatedVM.getCloudService(), virtualMachines);

        // 检查虚拟机是否被分
        if(vm != null) {
            // 获取物理机id
            physicalMachine = PhysicalMachine.getById(vm.getPhysicalMachine(), physicalMachines);
            // 检查资源状况
            if (Constraints.checkResources(physicalMachine, vm, updatedVM, virtualMachines,
                true)) {
                // 更新分配虚拟机列表
                physicalMachine.updatePMResources(vm, Utils.SUB);
                updateVmResources(virtualMachines, updatedVM);
                allocateVMToPM(updatedVM, physicalMachine);
                return true;
            } else {
                getViolation(s.getTime(), vm, updatedVM, physicalMachine);
                return false;
            }
        }

        // 如果虚拟机不在虚拟机列表中，则可能在衍生虚拟机列表中
        for(VirtualMachine derivedVM : derivedVMs){
            if(updatedVM.equals(derivedVM)) {
                updateVmResources(derivedVMs, updatedVM);
                success=true;
            }
        }
        return success;
    }

    /**
     * 将虚拟机分配到物理机上。
     *
     * @param vm 虚拟机对象
     * @param pm 物理机对象
     */
    private static void allocateVMToPM(VirtualMachine vm, PhysicalMachine pm) {
        pm.updatePMResources(vm, Utils.SUM);
    }

    /**
     * 获取违规信息并处理。
     *
     * @param timeViolation 违规时间
     * @param oldVm         旧的虚拟机对象
     * @param vm            更新后的虚拟机对象
     * @return 是否有违规
     */
    private static void getViolation(Integer timeViolation, VirtualMachine oldVm, VirtualMachine vm,
            PhysicalMachine pm) {

        Float cpuViolation = 0F;
        Float ramViolation = 0F;
        Float netViolation = 0F;

        Float cpu = pm.getResourcesRequested().get(0)
                - (oldVm.getResources().get(0) * oldVm.getUtilization().get(0)/100 )
                + (vm.getResources().get(0) * vm.getUtilization().get(0)/100);

        Float ram = pm.getResourcesRequested().get(1)
                - (oldVm.getResources().get(1) * oldVm.getUtilization().get(1)/100 )
                + (vm.getResources().get(1) * vm.getUtilization().get(1)/100);


        Float net = pm.getResourcesRequested().get(2)
                - (oldVm.getResources().get(2) * oldVm.getUtilization().get(2)/100 )
                + (vm.getResources().get(2) * vm.getUtilization().get(2)/100);

        if(pm.getResources().get(0) <= cpu) {
            cpuViolation = cpu - pm.getResources().get(0);
        }

        if(pm.getResources().get(1) <= ram) {
            ramViolation = ram - pm.getResources().get(1);
        }

        if(pm.getResources().get(2) <= net) {
            netViolation = net - pm.getResources().get(2);
        }

        Resources res = new Resources(cpuViolation, ramViolation, netViolation);
        Violation violation = new Violation(timeViolation, res);

        DynamicVMP.updateEconomicalPenalties(vm,res, timeViolation);
        DynamicVMP.unsatisfiedResources.put(vm.getId(), violation);
    }

    /**
     * 更新虚拟机的资源信息。
     *
     * @param virtualMachines 虚拟机列表
     * @param updatedVM       更新后的虚拟机对象
     */
    private static void updateVmResources(List<VirtualMachine> virtualMachines, VirtualMachine updatedVM) {

        virtualMachines.forEach(vm -> {

            if (vm.equals(updatedVM)) {
                vm.setUtilization(updatedVM.getUtilization());
                vm.setResources(updatedVM.getResources());
                vm.setRevenue(updatedVM.getRevenue());
            }
        });
    }

    /**
     * 根据时间单位移除不再需要的虚拟机。
     *
     * @param virtualMachines  虚拟机列表
     * @param timeUnit         时间单位
     * @param physicalMachines 物理机列表
     */
    public static void removeVMByTime(List<VirtualMachine> virtualMachines, Integer timeUnit,
            List<PhysicalMachine> physicalMachines) {

        List<VirtualMachine> toRemoveVM = new ArrayList<>();

        virtualMachines.forEach(vm -> {
            if (vm.getTend() <= timeUnit) {
                Integer pmId = vm.getPhysicalMachine();
                PhysicalMachine pm = PhysicalMachine.getById(pmId, physicalMachines);
                if(pm != null ) {
                    pm.updatePMResources(vm, Utils.SUB);
                    toRemoveVM.add(vm);
                }
            }
        });
        virtualMachines.removeAll(toRemoveVM);
    }

    /**
     * 根据时间单位移除不再需要的衍生虚拟机。
     *
     * @param derivatedVMs 衍生的虚拟机列表
     * @param timeUnit     时间单位
     */
    public static void removeDerivatedVMByTime(List<VirtualMachine> derivatedVMs, Integer timeUnit) {

        List<VirtualMachine> toRemoveVM = new ArrayList<>();

        derivatedVMs.forEach(vm -> {
            if (vm.getTend() <= timeUnit) {
                    toRemoveVM.add(vm);
            }
        });
        derivatedVMs.removeAll(toRemoveVM);
    }

    /**
     * 尝试使用 First Fit 算法将虚拟机放置到物理服务器上。
     *
     * @param vm              要放置的虚拟机
     * @param physicalMachines 物理服务器列表
     * @param virtualMachines  虚拟机列表
     * @param isMigration     是否为迁移操作
     * @return 如果成功放置返回 true，否则返回 false
     */
    private static Boolean firstFit(VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        if (allocateVMToDC(vm, physicalMachines, virtualMachines, isMigration)) {
            return true;
        }
        derivedVMs.add(vm);
        return false;
    }

    /**
     * 将虚拟机分配到数据中心
     * @param vm               虚拟机
     * @param physicalMachines 物理机列表
     * @param virtualMachines  虚拟机列表
     * @param isMigration      虚拟机是否正在迁移
     * @return <b>True</b>, if DC can host the VM <br> <b>False</b>, otherwise
     */
    private static boolean allocateVMToDC(final VirtualMachine vm, final List<PhysicalMachine> physicalMachines,
            final List<VirtualMachine> virtualMachines, Boolean isMigration) {

        // 如果正在迁移，不更新资源利用率
        if(!isMigration) {
            // 如果是新的虚拟机，利用率设为100%
            Resources uti = new Resources(100F, 100F, 100F);
            vm.setUtilization(Arrays.asList(uti.getCpu(), uti.getRam(), uti.getNet()));
        }

        for (PhysicalMachine pm : physicalMachines) {
            if (Constraints.checkResources(pm, null, vm, virtualMachines, false)) {
                // 将虚拟机分配到物理机上
                allocateVMToPM(vm, pm);
                vm.setPhysicalMachine(pm.getId());
                virtualMachines.add(vm);
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试使用 Best Fit 算法将虚拟机放置到物理服务器上。
     *
     * @param vm              要放置的虚拟机
     * @param physicalMachines 物理服务器列表
     * @param virtualMachines  虚拟机列表
     * @param derivedVMs       衍生虚拟机列表
     * @param isMigration     是否为迁移操作
     * @return 如果成功放置返回 true，否则返回 false
     */
    private static Boolean bestFit(VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        return bestOrWorstFit(true, vm, physicalMachines, virtualMachines, derivedVMs, isMigration);

    }

    /**
     * 尝试使用 Best/Worst Fit 算法将虚拟机放置到物理服务器上。
     *
     * @param vm              要放置的虚拟机
     * @param isBest           是否是Best Fit
     * @param physicalMachines 物理服务器列表
     * @param virtualMachines  虚拟机列表
     * @param derivedVMs       衍生虚拟机列表
     * @param isMigration     是否为迁移操作
     * @return 如果成功放置返回 true，否则返回 false
     */
    private static Boolean  bestOrWorstFit(Boolean isBest, VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        if (isBest) {
            Collections.sort(physicalMachines, new BestComparator());
        } else {
            Collections.sort(physicalMachines, new WorstComparator());
        }

        if (allocateVMToDC(vm, physicalMachines, virtualMachines, isMigration)) {
            return true;
        }

        derivedVMs.add(vm);

        return false;
    }

    /**
     * 尝试使用 Worst Fit 算法将虚拟机放置到物理服务器上。
     *
     * @param vm              要放置的虚拟机
     * @param physicalMachines 物理服务器列表
     * @param virtualMachines  虚拟机列表
     * @param derivedVMs       衍生虚拟机列表
     * @param isMigration     是否为迁移操作
     * @return 如果成功放置返回 true，否则返回 false
     */
    private static Boolean worstFit(VirtualMachine vm, List<PhysicalMachine> physicalMachines,
            List<VirtualMachine> virtualMachines, List<VirtualMachine> derivedVMs, Boolean isMigration) {

        return bestOrWorstFit(false, vm, physicalMachines, virtualMachines, derivedVMs, isMigration);

    }


}
