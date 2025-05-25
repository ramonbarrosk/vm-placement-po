package com.ramonyago.cloudsim.model;

import java.util.*;

/**
 * Encapsula todos os dados de entrada de uma instância do problema de alocação de VMs.
 */
public class ProblemInstance {
    private final List<VM> vms;
    private final List<Host> hosts;
    private final String instanceName;
    private final Map<String, Object> metadata;
    
    public ProblemInstance(String instanceName) {
        this.instanceName = instanceName;
        this.vms = new ArrayList<>();
        this.hosts = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public ProblemInstance(String instanceName, List<VM> vms, List<Host> hosts) {
        this.instanceName = instanceName;
        this.vms = new ArrayList<>(vms);
        this.hosts = new ArrayList<>(hosts);
        this.metadata = new HashMap<>();
    }
    
    public void addVM(VM vm) {
        if (!vms.contains(vm)) {
            vms.add(vm);
        }
    }
    
    public void addHost(Host host) {
        if (!hosts.contains(host)) {
            hosts.add(host);
        }
    }
    
    public List<VM> getVMs() {
        return new ArrayList<>(vms);
    }
    
    public List<Host> getHosts() {
        return new ArrayList<>(hosts);
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public int getVMCount() {
        return vms.size();
    }
    
    public int getHostCount() {
        return hosts.size();
    }
    
    public VM getVM(int id) {
        return vms.stream()
                .filter(vm -> vm.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    public Host getHost(int id) {
        return hosts.stream()
                .filter(host -> host.getId() == id)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Adiciona metadados à instância
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Calcula estatísticas básicas da instância
     */
    public InstanceStatistics getStatistics() {
        return new InstanceStatistics();
    }
    
    /**
     * Valida a consistência da instância
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (vms.isEmpty()) {
            errors.add("Instance has no VMs");
        }
        
        if (hosts.isEmpty()) {
            errors.add("Instance has no hosts");
        }
        
        // Verifica IDs únicos das VMs
        Set<Integer> vmIds = new HashSet<>();
        for (VM vm : vms) {
            if (!vmIds.add(vm.getId())) {
                errors.add("Duplicate VM ID: " + vm.getId());
            }
        }
        
        // Verifica IDs únicos dos hosts
        Set<Integer> hostIds = new HashSet<>();
        for (Host host : hosts) {
            if (!hostIds.add(host.getId())) {
                errors.add("Duplicate Host ID: " + host.getId());
            }
        }
        
        // Verifica se há pelo menos um tipo de recurso definido
        boolean hasResourceTypes = false;
        for (VM vm : vms) {
            if (!vm.getResourceDemands().isEmpty()) {
                hasResourceTypes = true;
                break;
            }
        }
        
        if (!hasResourceTypes) {
            errors.add("No resource demands defined for VMs");
        }
        
        // Verifica se hosts têm capacidades suficientes
        for (ResourceType type : ResourceType.values()) {
            double totalDemand = vms.stream()
                    .mapToDouble(vm -> vm.getResourceDemand(type))
                    .sum();
            double totalCapacity = hosts.stream()
                    .mapToDouble(host -> host.getResourceCapacity(type))
                    .sum();
            
            if (totalDemand > totalCapacity) {
                errors.add(String.format("Total %s demand (%.2f) exceeds total capacity (%.2f)", 
                                       type.getName(), totalDemand, totalCapacity));
            }
        }
        
        return errors;
    }
    
    @Override
    public String toString() {
        return String.format("ProblemInstance{name='%s', VMs=%d, Hosts=%d}", 
                           instanceName, vms.size(), hosts.size());
    }
    
    /**
     * Classe interna para estatísticas da instância
     */
    public class InstanceStatistics {
        public final int vmCount;
        public final int hostCount;
        public final Map<ResourceType, Double> totalVmDemands;
        public final Map<ResourceType, Double> totalHostCapacities;
        public final Map<ResourceType, Double> avgVmDemands;
        public final Map<ResourceType, Double> avgHostCapacities;
        public final double avgVmReliabilityRequirement;
        public final double avgHostFailureProbability;
        public final double totalHostCost;
        
        private InstanceStatistics() {
            this.vmCount = vms.size();
            this.hostCount = hosts.size();
            this.totalVmDemands = new HashMap<>();
            this.totalHostCapacities = new HashMap<>();
            this.avgVmDemands = new HashMap<>();
            this.avgHostCapacities = new HashMap<>();
            
            // Calcula demandas totais e médias das VMs
            for (ResourceType type : ResourceType.values()) {
                double totalDemand = vms.stream()
                        .mapToDouble(vm -> vm.getResourceDemand(type))
                        .sum();
                totalVmDemands.put(type, totalDemand);
                avgVmDemands.put(type, vmCount > 0 ? totalDemand / vmCount : 0.0);
                
                double totalCapacity = hosts.stream()
                        .mapToDouble(host -> host.getResourceCapacity(type))
                        .sum();
                totalHostCapacities.put(type, totalCapacity);
                avgHostCapacities.put(type, hostCount > 0 ? totalCapacity / hostCount : 0.0);
            }
            
            this.avgVmReliabilityRequirement = vms.stream()
                    .mapToDouble(VM::getMinReliability)
                    .average()
                    .orElse(0.0);
            
            this.avgHostFailureProbability = hosts.stream()
                    .mapToDouble(Host::getFailureProbability)
                    .average()
                    .orElse(0.0);
            
            this.totalHostCost = hosts.stream()
                    .mapToDouble(Host::getActivationCost)
                    .sum();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Instance Statistics:\n");
            sb.append(String.format("  VMs: %d, Hosts: %d\n", vmCount, hostCount));
            sb.append(String.format("  Avg VM reliability requirement: %.3f\n", avgVmReliabilityRequirement));
            sb.append(String.format("  Avg Host failure probability: %.3f\n", avgHostFailureProbability));
            sb.append(String.format("  Total host activation cost: %.2f\n", totalHostCost));
            
            sb.append("  Resource demands vs capacities:\n");
            for (ResourceType type : ResourceType.values()) {
                sb.append(String.format("    %s: %.2f / %.2f (%.1f%%)\n", 
                                      type.getName(),
                                      totalVmDemands.get(type),
                                      totalHostCapacities.get(type),
                                      totalHostCapacities.get(type) > 0 ? 
                                          100.0 * totalVmDemands.get(type) / totalHostCapacities.get(type) : 0.0));
            }
            
            return sb.toString();
        }
    }
} 