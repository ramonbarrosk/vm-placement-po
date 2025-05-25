package com.ramonyago.cloudsim.model;

/**
 * Enum que representa os diferentes tipos de recursos que uma VM pode demandar
 * e que um Host pode fornecer.
 */
public enum ResourceType {
    CPU("CPU", "vCPUs"),
    RAM("RAM", "GB"),
    STORAGE("Storage", "GB"),
    NETWORK("Network", "Mbps"),
    GPU("GPU", "units");
    
    private final String name;
    private final String unit;
    
    ResourceType(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUnit() {
        return unit;
    }
    
    @Override
    public String toString() {
        return name + " (" + unit + ")";
    }
} 