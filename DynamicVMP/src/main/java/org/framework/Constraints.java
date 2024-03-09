package org.framework;

import org.domain.PhysicalMachine;
import org.domain.VirtualMachine;
import org.framework.reconfigurationAlgorithm.configuration.ExperimentConf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Constraints {

    private Constraints() {
    }

    /**
     * 检查是否有任何 PM 能够承载 VM。
     * <p>
     * 对于过订阅的环境，必须考虑利用率。 <br>
     * 仅当 VM 请求的资源利用率小于 PM 的可用资源时，PM 才能承载 VM。
     * </p>
     *
     * @param pm           物理机
     * @param deprecatedVM 要使用新资源和利用率更新的虚拟机（VM）
     * @param vm           虚拟机
     * @param vms          虚拟机列表
     * @param isUpdate     <b>True</b>，如果需要更新 VM <br> <b>False</b>，否则
     * @return <b>True</b>，如果存在可以容纳 VM 的 PM。
     */
    public static Boolean checkResources(PhysicalMachine pm, VirtualMachine deprecatedVM, VirtualMachine vm,
            List<VirtualMachine> vms, Boolean isUpdate) {
        // 如果 oldVM 不为空，表示进行更新
        VirtualMachine oldVm;
        if(deprecatedVM == null) {
            oldVm = new VirtualMachine(new ArrayList<>(), new ArrayList<>());
        } else {
            oldVm = deprecatedVM;
        }

        Float toReserveCPU = pm.getResourcesRequested().get(0)
            - (oldVm.getResources().get(0) * oldVm.getUtilization().get(0)/100 )
            + (vm.getResources().get(0) * vm.getUtilization().get(0)/100)
            + (vm.getResources().get(0) * (1- vm.getUtilization().get(0)/100)*Parameter.PROTECTION_FACTOR);
        Boolean checkCPU = toReserveCPU < pm.getResources().get(0);

        Float toReserveRAM = pm.getResourcesRequested().get(1)
            - (oldVm.getResources().get(1) * oldVm.getUtilization().get(1) / 100)
            + (vm.getResources().get(1) * vm.getUtilization().get(1) / 100)
            + (vm.getResources().get(1) * (1 - vm.getUtilization().get(1) / 100) * Parameter.PROTECTION_FACTOR);
        Boolean checkRAM = toReserveRAM < pm.getResources().get(1);

        Float toReserveNET = pm.getResourcesRequested().get(2)
            - (oldVm.getResources().get(2) * oldVm.getUtilization().get(2) / 100)
            + (vm.getResources().get(2) * vm.getUtilization().get(2) / 100)
            + (vm.getResources().get(2) * (1 - vm.getUtilization().get(2) / 100) * Parameter.PROTECTION_FACTOR);
        Boolean checkNET = toReserveNET < pm.getResources().get(2);

        Boolean flag = checkCPU && checkRAM && checkNET;

        if (!isUpdate && flag && Parameter.FAULT_TOLERANCE) {
            for (VirtualMachine vmTmp : vms) {

                if (vmTmp.getCloudService().equals(vm.getCloudService()) &&
                        vmTmp.getPhysicalMachine().equals(pm.getId())) {
                    return false;
                }
            }
        }

        return flag;
    }


    /**
     * 检查 PM 是否过载。
     * <p>
     * 对于过订阅的环境，必须考虑利用率。 <br>
     * 如果资源利用率大于 PM 的资源容量，那么物理机就过载了。
     * </p>
     *
     * @param pm                   物理机
     * @param virtualMachinesAssoc 与物理机关联的虚拟机列表
     * @param protectionFactor     过订阅程度的标志
     * @return <b>True</b>，如果 PM 过载。
     */
    public static Boolean checkPMOverloaded(PhysicalMachine pm, List<VirtualMachine> virtualMachinesAssoc, Float protectionFactor ){

        float sumCpuResource = 0;
        float sumRamResource = 0;
        float sumNetResource = 0;

        for(VirtualMachine vm : virtualMachinesAssoc){

            sumCpuResource += (vm.getResources().get(0) * vm.getUtilization().get(0)/100)
                    + (vm.getResources().get(0) * (1- vm.getUtilization().get(0)/100)*protectionFactor);

            sumRamResource += (vm.getResources().get(1) * vm.getUtilization().get(1)/100)
                    + (vm.getResources().get(1) * (1- vm.getUtilization().get(1)/100)*protectionFactor);

            sumNetResource += (vm.getResources().get(2) * vm.getUtilization().get(2)/100)
                    + (vm.getResources().get(2) * (1- vm.getUtilization().get(2)/100)*protectionFactor);
        }

        return sumCpuResource > pm.getResources().get(0)
                || sumRamResource > pm.getResources().get(1)
                || sumNetResource > pm.getResources().get(2);
    }

    /**
     * @param virtualMachineList 虚拟机列表
     * @param cloudServiceId     云服务 ID
     * @return <b>True</b>，如果违反了容错性 <br> <b>False</b>，否则
     */
    public static Boolean isFaultToleranceViolated(List<VirtualMachine> virtualMachineList, int cloudServiceId){
        Predicate<VirtualMachine> vmFilter = vm -> vm.getCloudService() == cloudServiceId;
        return virtualMachineList.stream().filter(vmFilter).count() > 1L;
    }

    /**
     * @param pm 物理机
     * @return <b>True</b>，如果 PM 过载 <br> <b>False</b>，否则
     */
    public static Boolean isPMOverloaded(PhysicalMachine pm){

        return pm.getUtilization().get(0) * 100 > ExperimentConf.OVERLOAD_PM_THRESHOLD ||
               pm.getUtilization().get(1) * 100 > ExperimentConf.OVERLOAD_PM_THRESHOLD ||
               pm.getUtilization().get(2) * 100 > ExperimentConf.OVERLOAD_PM_THRESHOLD;
    }

    /**
     * @param pm 物理机
     * @return <b>True</b>，如果 PM 负载不足 <br> <b>False</b>，否则
     */
    public static Boolean isPMUnderloaded(PhysicalMachine pm){

        return pm.getUtilization().get(0) * 100 < ExperimentConf.UNDERLOAD_PM_THRESHOLD ||
               pm.getUtilization().get(1) * 100 < ExperimentConf.UNDERLOAD_PM_THRESHOLD ||
               pm.getUtilization().get(2) * 100 < ExperimentConf.UNDERLOAD_PM_THRESHOLD;
    }

}
