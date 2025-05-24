package com.ramonyago.cloudsim;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSuitability;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyAbstract;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class AlocacaoBRKGA extends VmAllocationPolicyAbstract {

    private final List<? extends Host> listaHosts;

    public AlocacaoBRKGA(List<? extends Host> listaHosts) {
        super((policy, vm) -> Optional.empty()); // Passa uma função vazia temporária
        this.listaHosts = listaHosts;
    }

    @Override
    public HostSuitability allocateHostForVm(Vm vm) {
        // Aqui entra sua lógica do BRKGA
        return HostSuitability.NULL;
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        // Libera o host da VM
    }
}
