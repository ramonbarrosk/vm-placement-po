package com.ramonyago.cloudsim.model;

import java.util.Map;
import java.util.HashMap;

/**
 * Representa uma Máquina Virtual (VM) com demandas de múltiplos recursos
 * e requisitos de confiabilidade.
 */
public class VM {
    private final int id;
    private final Map<ResourceType, Double> resourceDemands;
    private final double minReliability; // R_min_v
    private final double priority;
    
    public VM(int id, double minReliability) {
        this.id = id;
        this.minReliability = minReliability;
        this.resourceDemands = new HashMap<>();
        this.priority = 1.0;
    }
    
    public VM(int id, double minReliability, double priority) {
        this.id = id;
        this.minReliability = minReliability;
        this.resourceDemands = new HashMap<>();
        this.priority = priority;
    }
    
    public int getId() {
        return id;
    }
    
    public double getMinReliability() {
        return minReliability;
    }
    
    public double getPriority() {
        return priority;
    }
    
    public Map<ResourceType, Double> getResourceDemands() {
        return new HashMap<>(resourceDemands);
    }
    
    public double getResourceDemand(ResourceType type) {
        return resourceDemands.getOrDefault(type, 0.0);
    }
    
    public void setResourceDemand(ResourceType type, double demand) {
        if (demand < 0) {
            throw new IllegalArgumentException("Resource demand cannot be negative");
        }
        resourceDemands.put(type, demand);
    }
    
    /**
     * Calcula o valor de log(1 - R_min_v) usado na restrição linearizada de confiabilidade
     */
    public double getLogFailureProbability() {
        return Math.log(1 - minReliability);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VM vm = (VM) o;
        return id == vm.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    @Override
    public String toString() {
        return String.format("VM{id=%d, minReliability=%.3f, demands=%s}", 
                           id, minReliability, resourceDemands);
    }
} 