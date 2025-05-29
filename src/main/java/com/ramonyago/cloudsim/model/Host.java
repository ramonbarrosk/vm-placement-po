package com.ramonyago.cloudsim.model;

import java.util.Map;
import java.util.HashMap;

// Host class - represents physical machines
public class Host {
    private final int hostId;
    private final Map<ResourceType, Double> caps;
    private final double cost; // activation cost
    private final double failProb; // failure probability
    private final double energy;
    
    public Host(int hostId, double cost, double failProb) {
        this.hostId = hostId;
        this.cost = cost;
        this.failProb = failProb;
        this.caps = new HashMap<>();
        this.energy = 100.0; // default value
        
        // basic validation
        if (failProb < 0 || failProb > 1) {
            throw new IllegalArgumentException("fail prob must be 0-1");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost cant be negative");
        }
    }
    
    public Host(int hostId, double cost, double failProb, double energy) {
        this.hostId = hostId;
        this.cost = cost;
        this.failProb = failProb;
        this.caps = new HashMap<>();
        this.energy = energy;
        
        // basic validation
        if (failProb < 0 || failProb > 1) {
            throw new IllegalArgumentException("fail prob must be 0-1");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("cost cant be negative");
        }
    }
    
    public int getHostId() {
        return hostId;
    }
    
    public double getCost() {
        return cost;
    }
    
    public double getFailProb() {
        return failProb;
    }
    
    public double getEnergy() {
        return energy;
    }
    
    // reliability = 1 - fail_prob
    public double getRel() {
        return 1.0 - failProb;
    }
    
    // log of failure prob for constraints
    public double getLogFailProb() {
        return Math.log(failProb);
    }
    
    public Map<ResourceType, Double> getCaps() {
        return new HashMap<>(caps);
    }
    
    public double getCap(ResourceType type) {
        return caps.getOrDefault(type, 0.0);
    }
    
    public void setCap(ResourceType type, double capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity cant be negative");
        }
        caps.put(type, capacity);
    }
    
    // check if host can fit this VM
    public boolean canFit(VM vm, Map<VM, Host> currentAllocs) {
        // calc current usage
        Map<ResourceType, Double> usage = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            usage.put(type, 0.0);
        }
        
        // sum up VMs already on this host
        for (Map.Entry<VM, Host> entry : currentAllocs.entrySet()) {
            if (entry.getValue().equals(this)) {
                VM allocVm = entry.getKey();
                for (ResourceType type : ResourceType.values()) {
                    double curr = usage.get(type);
                    double vmDemand = allocVm.getDemand(type);
                    usage.put(type, curr + vmDemand);
                }
            }
        }
        
        // check if new VM fits
        for (ResourceType type : ResourceType.values()) {
            double totalDemand = usage.get(type) + vm.getDemand(type);
            if (totalDemand > getCap(type)) {
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
        return hostId == host.hostId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(hostId);
    }
    
    @Override
    public String toString() {
        return String.format("Host{id=%d, cost=%.2f, failProb=%.3f, caps=%s}", 
                           hostId, cost, failProb, caps);
    }
} 