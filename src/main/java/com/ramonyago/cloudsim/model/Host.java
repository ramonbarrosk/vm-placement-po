package com.ramonyago.cloudsim.model;

import java.util.Map;
import java.util.HashMap;

/**
 * Representa um Host físico com capacidades de múltiplos recursos,
 * custo de ativação e probabilidade de falha.
 */
public class Host {
    private final int id;
    private final Map<ResourceType, Double> resourceCapacities;
    private final double activationCost; // cost_h
    private final double failureProbability; // p_h
    private final double energyConsumption;
    
    public Host(int id, double activationCost, double failureProbability) {
        this.id = id;
        this.activationCost = activationCost;
        this.failureProbability = failureProbability;
        this.resourceCapacities = new HashMap<>();
        this.energyConsumption = 100.0; // Valor padrão
        
        // Validações
        if (failureProbability < 0 || failureProbability > 1) {
            throw new IllegalArgumentException("Failure probability must be between 0 and 1");
        }
        if (activationCost < 0) {
            throw new IllegalArgumentException("Activation cost cannot be negative");
        }
    }
    
    public Host(int id, double activationCost, double failureProbability, double energyConsumption) {
        this.id = id;
        this.activationCost = activationCost;
        this.failureProbability = failureProbability;
        this.resourceCapacities = new HashMap<>();
        this.energyConsumption = energyConsumption;
        
        // Validações
        if (failureProbability < 0 || failureProbability > 1) {
            throw new IllegalArgumentException("Failure probability must be between 0 and 1");
        }
        if (activationCost < 0) {
            throw new IllegalArgumentException("Activation cost cannot be negative");
        }
    }
    
    public int getId() {
        return id;
    }
    
    public double getActivationCost() {
        return activationCost;
    }
    
    public double getFailureProbability() {
        return failureProbability;
    }
    
    public double getEnergyConsumption() {
        return energyConsumption;
    }
    
    /**
     * Calcula a confiabilidade do host (1 - p_h)
     */
    public double getReliability() {
        return 1.0 - failureProbability;
    }
    
    /**
     * Retorna log(p_h) usado na restrição linearizada de confiabilidade
     */
    public double getLogFailureProbability() {
        return Math.log(failureProbability);
    }
    
    public Map<ResourceType, Double> getResourceCapacities() {
        return new HashMap<>(resourceCapacities);
    }
    
    public double getResourceCapacity(ResourceType type) {
        return resourceCapacities.getOrDefault(type, 0.0);
    }
    
    public void setResourceCapacity(ResourceType type, double capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Resource capacity cannot be negative");
        }
        resourceCapacities.put(type, capacity);
    }
    
    /**
     * Verifica se o host pode alocar uma VM considerando as demandas de recursos
     */
    public boolean canAllocate(VM vm, Map<VM, Host> currentAllocations) {
        // Calcula o uso atual de recursos
        Map<ResourceType, Double> currentUsage = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            currentUsage.put(type, 0.0);
        }
        
        // Soma o uso das VMs já alocadas neste host
        for (Map.Entry<VM, Host> entry : currentAllocations.entrySet()) {
            if (entry.getValue().equals(this)) {
                VM allocatedVm = entry.getKey();
                for (ResourceType type : ResourceType.values()) {
                    double currentVal = currentUsage.get(type);
                    double vmDemand = allocatedVm.getResourceDemand(type);
                    currentUsage.put(type, currentVal + vmDemand);
                }
            }
        }
        
        // Verifica se há capacidade suficiente para a nova VM
        for (ResourceType type : ResourceType.values()) {
            double totalDemand = currentUsage.get(type) + vm.getResourceDemand(type);
            if (totalDemand > getResourceCapacity(type)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Host host = (Host) o;
        return id == host.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    @Override
    public String toString() {
        return String.format("Host{id=%d, cost=%.2f, failProb=%.3f, capacities=%s}", 
                           id, activationCost, failureProbability, resourceCapacities);
    }
} 