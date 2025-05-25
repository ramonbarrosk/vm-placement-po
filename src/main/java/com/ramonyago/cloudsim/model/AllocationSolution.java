package com.ramonyago.cloudsim.model;

import java.util.*;

/**
 * Representa uma solução completa de alocação de VMs em hosts,
 * incluindo cálculos de múltiplos objetivos e verificação de restrições.
 */
public class AllocationSolution {
    private final Map<VM, Host> vmToHost; // x_vh: alocação de VMs
    private final Set<Host> activeHosts; // y_h: hosts ativos
    private final List<VM> vms;
    private final List<Host> hosts;
    
    // Valores dos objetivos
    private Double totalCost;
    private Double totalReliability;
    private Double energyConsumption;
    private Double loadBalance;
    
    // Métricas de violação de restrições
    private boolean feasible;
    private double constraintViolation;
    
    public AllocationSolution(List<VM> vms, List<Host> hosts) {
        this.vms = new ArrayList<>(vms);
        this.hosts = new ArrayList<>(hosts);
        this.vmToHost = new HashMap<>();
        this.activeHosts = new HashSet<>();
        this.feasible = true;
        this.constraintViolation = 0.0;
    }
    
    public AllocationSolution(AllocationSolution other) {
        this.vms = new ArrayList<>(other.vms);
        this.hosts = new ArrayList<>(other.hosts);
        this.vmToHost = new HashMap<>(other.vmToHost);
        this.activeHosts = new HashSet<>(other.activeHosts);
        this.totalCost = other.totalCost;
        this.totalReliability = other.totalReliability;
        this.energyConsumption = other.energyConsumption;
        this.loadBalance = other.loadBalance;
        this.feasible = other.feasible;
        this.constraintViolation = other.constraintViolation;
    }
    
    /**
     * Aloca uma VM a um host
     */
    public void allocateVM(VM vm, Host host) {
        vmToHost.put(vm, host);
        activeHosts.add(host);
        invalidateObjectiveCache();
    }
    
    /**
     * Remove a alocação de uma VM
     */
    public void deallocateVM(VM vm) {
        Host host = vmToHost.remove(vm);
        if (host != null) {
            // Verifica se o host ainda tem VMs alocadas
            boolean hostStillUsed = vmToHost.values().contains(host);
            if (!hostStillUsed) {
                activeHosts.remove(host);
            }
        }
        invalidateObjectiveCache();
    }
    
    /**
     * Realoca uma VM para outro host
     */
    public void reallocateVM(VM vm, Host newHost) {
        deallocateVM(vm);
        allocateVM(vm, newHost);
    }
    
    public Map<VM, Host> getVmToHost() {
        return new HashMap<>(vmToHost);
    }
    
    public Set<Host> getActiveHosts() {
        return new HashSet<>(activeHosts);
    }
    
    public Host getHostForVM(VM vm) {
        return vmToHost.get(vm);
    }
    
    public List<VM> getVmsOnHost(Host host) {
        return vmToHost.entrySet().stream()
                .filter(entry -> entry.getValue().equals(host))
                .map(Map.Entry::getKey)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Calcula o custo total (Objetivo 1: minimizar)
     */
    public double getTotalCost() {
        if (totalCost == null) {
            totalCost = activeHosts.stream()
                    .mapToDouble(Host::getActivationCost)
                    .sum();
        }
        return totalCost;
    }
    
    /**
     * Calcula a confiabilidade média do sistema (Objetivo 2: maximizar)
     */
    public double getTotalReliability() {
        if (totalReliability == null) {
            if (vmToHost.isEmpty()) {
                totalReliability = 0.0;
            } else {
                double sumReliability = 0.0;
                for (VM vm : vms) {
                    sumReliability += getVMReliability(vm);
                }
                totalReliability = sumReliability / vms.size();
            }
        }
        return totalReliability;
    }
    
    /**
     * Calcula a confiabilidade de uma VM específica
     */
    public double getVMReliability(VM vm) {
        Host host = vmToHost.get(vm);
        if (host == null) {
            return 0.0; // VM não alocada
        }
        return 1.0 - host.getFailureProbability();
    }
    
    /**
     * Calcula o consumo total de energia
     */
    public double getEnergyConsumption() {
        if (energyConsumption == null) {
            energyConsumption = activeHosts.stream()
                    .mapToDouble(Host::getEnergyConsumption)
                    .sum();
        }
        return energyConsumption;
    }
    
    /**
     * Calcula uma métrica de balanceamento de carga
     */
    public double getLoadBalance() {
        if (loadBalance == null) {
            if (activeHosts.isEmpty()) {
                loadBalance = 0.0;
            } else {
                // Calcula o desvio padrão da utilização de CPU entre hosts ativos
                double[] cpuUtilizations = new double[activeHosts.size()];
                int i = 0;
                for (Host host : activeHosts) {
                    double totalCpuDemand = getVmsOnHost(host).stream()
                            .mapToDouble(vm -> vm.getResourceDemand(ResourceType.CPU))
                            .sum();
                    cpuUtilizations[i++] = totalCpuDemand / host.getResourceCapacity(ResourceType.CPU);
                }
                
                double mean = Arrays.stream(cpuUtilizations).average().orElse(0.0);
                double variance = Arrays.stream(cpuUtilizations)
                        .map(x -> Math.pow(x - mean, 2))
                        .average()
                        .orElse(0.0);
                loadBalance = Math.sqrt(variance);
            }
        }
        return loadBalance;
    }
    
    /**
     * Verifica se a solução é viável (todas as restrições satisfeitas)
     */
    public boolean isFeasible() {
        calculateFeasibility();
        return feasible;
    }
    
    /**
     * Retorna a violação total das restrições
     */
    public double getConstraintViolation() {
        calculateFeasibility();
        return constraintViolation;
    }
    
    private void calculateFeasibility() {
        feasible = true;
        constraintViolation = 0.0;
        
        // Verifica restrições de capacidade dos hosts
        for (Host host : activeHosts) {
            Map<ResourceType, Double> hostUsage = new HashMap<>();
            for (ResourceType type : ResourceType.values()) {
                hostUsage.put(type, 0.0);
            }
            
            // Soma demandas das VMs no host
            for (VM vm : getVmsOnHost(host)) {
                for (ResourceType type : ResourceType.values()) {
                    double currentUsage = hostUsage.get(type);
                    hostUsage.put(type, currentUsage + vm.getResourceDemand(type));
                }
            }
            
            // Verifica violações de capacidade
            for (ResourceType type : ResourceType.values()) {
                double usage = hostUsage.get(type);
                double capacity = host.getResourceCapacity(type);
                if (usage > capacity) {
                    feasible = false;
                    constraintViolation += (usage - capacity) / capacity;
                }
            }
        }
        
        // Verifica restrições de confiabilidade mínima
        for (VM vm : vms) {
            double vmReliability = getVMReliability(vm);
            if (vmReliability < vm.getMinReliability()) {
                feasible = false;
                constraintViolation += vm.getMinReliability() - vmReliability;
            }
        }
    }
    
    private void invalidateObjectiveCache() {
        totalCost = null;
        totalReliability = null;
        energyConsumption = null;
        loadBalance = null;
    }
    
    /**
     * Compara dominância de Pareto considerando dois objetivos principais
     * Retorna: -1 se this domina other, 1 se other domina this, 0 se não-dominados
     */
    public int compareDominance(AllocationSolution other) {
        boolean thisDominates = false;
        boolean otherDominates = false;
        
        // Objetivo 1: Minimizar custo
        if (this.getTotalCost() < other.getTotalCost()) {
            thisDominates = true;
        } else if (this.getTotalCost() > other.getTotalCost()) {
            otherDominates = true;
        }
        
        // Objetivo 2: Maximizar confiabilidade
        if (this.getTotalReliability() > other.getTotalReliability()) {
            thisDominates = true;
        } else if (this.getTotalReliability() < other.getTotalReliability()) {
            otherDominates = true;
        }
        
        if (thisDominates && !otherDominates) {
            return -1; // this domina other
        } else if (otherDominates && !thisDominates) {
            return 1; // other domina this
        } else {
            return 0; // não-dominados
        }
    }
    
    /**
     * Calcula a distância de crowding para diversidade
     */
    public double calculateCrowdingDistance(List<AllocationSolution> solutions) {
        if (solutions.size() <= 2) {
            return Double.MAX_VALUE;
        }
        
        int index = solutions.indexOf(this);
        if (index == -1) {
            return 0.0;
        }
        
        double distance = 0.0;
        
        // Ordena por custo
        solutions.sort(Comparator.comparingDouble(AllocationSolution::getTotalCost));
        if (index == 0 || index == solutions.size() - 1) {
            return Double.MAX_VALUE; // Soluções nas extremidades
        }
        
        double costRange = solutions.get(solutions.size() - 1).getTotalCost() - 
                          solutions.get(0).getTotalCost();
        if (costRange > 0) {
            distance += (solutions.get(index + 1).getTotalCost() - 
                        solutions.get(index - 1).getTotalCost()) / costRange;
        }
        
        // Ordena por confiabilidade
        solutions.sort(Comparator.comparingDouble(AllocationSolution::getTotalReliability));
        index = solutions.indexOf(this);
        if (index == 0 || index == solutions.size() - 1) {
            return Double.MAX_VALUE;
        }
        
        double reliabilityRange = solutions.get(solutions.size() - 1).getTotalReliability() - 
                                 solutions.get(0).getTotalReliability();
        if (reliabilityRange > 0) {
            distance += (solutions.get(index + 1).getTotalReliability() - 
                        solutions.get(index - 1).getTotalReliability()) / reliabilityRange;
        }
        
        return distance;
    }
    
    @Override
    public String toString() {
        return String.format("Solution{cost=%.2f, reliability=%.3f, energy=%.2f, feasible=%s}",
                           getTotalCost(), getTotalReliability(), getEnergyConsumption(), isFeasible());
    }
} 