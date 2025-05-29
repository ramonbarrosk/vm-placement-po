package com.ramonyago.cloudsim.model;

import java.util.Map;
import java.util.HashMap;

/**
 * Representa uma Máquina Virtual (VM) com demandas de múltiplos recursos
 * e requisitos de confiabilidade.
 */
public class VM {
    private final int vmId;
    private final Map<ResourceType, Double> demands;
    private final double minRel; // minimum reliability needed
    private final double prio;
    
    public VM(int vmId, double minRel) {
        this.vmId = vmId;
        this.minRel = minRel;
        this.demands = new HashMap<>();
        this.prio = 1.0;
    }
    
    public VM(int vmId, double minRel, double prio) {
        this.vmId = vmId;
        this.minRel = minRel;
        this.demands = new HashMap<>();
        this.prio = prio;
    }
    
    public int getVmId() {
        return vmId;
    }
    
    public double getMinRel() {
        return minRel;
    }
    
    public double getPrio() {
        return prio;
    }
    
    public Map<ResourceType, Double> getDemands() {
        return new HashMap<>(demands);
    }
    
    public double getDemand(ResourceType type) {
        return demands.getOrDefault(type, 0.0);
    }
    
    public void setDemand(ResourceType type, double demand) {
        if (demand < 0) {
            throw new IllegalArgumentException("demand cant be negative");
        }
        demands.put(type, demand);
    }
    
    /**
     * Calcula o valor de log(1 - R_min_v) usado na restrição linearizada de confiabilidade
     */
    public double getLogFailProb() {
        return Math.log(1 - minRel);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VM vm = (VM) o;
        return vmId == vm.vmId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(vmId);
    }
    
    @Override
    public String toString() {
        return String.format("VM{id=%d, minRel=%.3f, demands=%s}", 
                           vmId, minRel, demands);
    }
} 