package com.ramonyago.cloudsim.simulation;

import com.ramonyago.cloudsim.OptimizationParameters;
import com.ramonyago.cloudsim.VMAllocationOptimizer;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import com.ramonyago.cloudsim.model.Host;
import com.ramonyago.cloudsim.model.ResourceType;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudsimplus.hosts.HostSuitability;
import org.cloudsimplus.vms.Vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Integrador que recebe diretamente objetos Host e VM do CloudSim como entrada
 * para fazer alocação otimizada em tempo real durante a simulação.
 * 
 * Esta classe converte objetos CloudSim para nosso modelo interno, executa
 * a otimização, e aplica o resultado diretamente na simulação.
 */
public class CloudSimRealTimeAllocator extends VmAllocationPolicyAbstract {
    private static final Logger logger = LoggerFactory.getLogger(CloudSimRealTimeAllocator.class);
    
    private final OptimizationParameters optimizationParams;
    
    // Mapeamentos entre modelos CloudSim e internos
    private final Map<org.cloudsimplus.hosts.Host, Host> cloudSimToInternalHost;
    private final Map<Vm, VM> cloudSimToInternalVm;
    private final Map<Host, org.cloudsimplus.hosts.Host> internalToCloudSimHost;
    private final Map<VM, Vm> internalToCloudSimVm;
    
    // Cache da última solução calculada
    private AllocationSolution lastOptimalSolution;
    private long lastOptimizationTime;
    private static final long OPTIMIZATION_CACHE_TTL = 5000; // 5 segundos
    
    public CloudSimRealTimeAllocator(OptimizationParameters optimizationParams) {
        super();
        this.optimizationParams = optimizationParams;
        this.cloudSimToInternalHost = new HashMap<>();
        this.cloudSimToInternalVm = new HashMap<>();
        this.internalToCloudSimHost = new HashMap<>();
        this.internalToCloudSimVm = new HashMap<>();
        this.lastOptimizationTime = 0;
        
        logger.info("CloudSim Real-Time Allocator initialized");
    }
    
    /**
     * Método principal chamado pelo CloudSim quando uma VM precisa ser alocada
     */
    @Override
    public HostSuitability allocateHostForVm(Vm vm) {
        logger.debug("Allocating host for VM {}", vm.getId());
        
        try {
            // Verificar se a VM já está mapeada, se não, criar mapeamento
            if (!cloudSimToInternalVm.containsKey(vm)) {
                VM internalVm = convertCloudSimVmToInternal(vm);
                cloudSimToInternalVm.put(vm, internalVm);
                internalToCloudSimVm.put(internalVm, vm);
                logger.debug("Created new internal mapping for VM {}", vm.getId());
            }
            
            // 1. Converter todos os objetos CloudSim para nosso modelo interno
            ProblemInstance instance = createProblemInstanceFromCloudSim();
            
            // Adicionar a VM que está sendo alocada à instância se não estiver presente
            VM targetInternalVm = cloudSimToInternalVm.get(vm);
            if (!instance.getVMs().contains(targetInternalVm)) {
                instance.addVM(targetInternalVm);
                logger.debug("Added target VM {} to problem instance", vm.getId());
            }
            
            // 2. Executar otimização (com cache se recente)
            AllocationSolution solution = getOptimalAllocation(instance);
            
            // 3. Aplicar a solução encontrada
            return applyAllocationSolution(vm, solution);
            
        } catch (Exception e) {
            logger.error("Error during real-time allocation for VM {}", vm.getId(), e);
            // Fallback para alocação simples
            return allocateHostForVmSimple(vm);
        }
    }
    
    /**
     * Implementação obrigatória do método abstrato
     */
    @Override
    protected Optional<org.cloudsimplus.hosts.Host> defaultFindHostForVm(Vm vm) {
        // Usar nossa lógica otimizada
        HostSuitability suitability = allocateHostForVm(vm);
        return suitability.fully() ? Optional.of(vm.getHost()) : Optional.empty();
    }
    
    /**
     * Método chamado quando uma VM é desalocada
     */
    @Override
    public void deallocateHostForVm(Vm vm) {
        logger.debug("Deallocating VM {} from host {}", vm.getId(), 
                    vm.getHost() != null ? vm.getHost().getId() : "none");
        
        // Remover do mapeamento
        VM internalVm = cloudSimToInternalVm.remove(vm);
        if (internalVm != null) {
            internalToCloudSimVm.remove(internalVm);
        }
        
        // Limpar cache de otimização para recalcular com nova configuração
        invalidateOptimizationCache();
        
        super.deallocateHostForVm(vm);
    }
    
    /**
     * Converte o estado atual do CloudSim para nosso modelo interno
     */
    private ProblemInstance createProblemInstanceFromCloudSim() {
        ProblemInstance instance = new ProblemInstance("realtime-" + System.currentTimeMillis());
        
        // Converter hosts do CloudSim
        for (org.cloudsimplus.hosts.Host cloudSimHost : getHostList()) {
            Host internalHost = convertCloudSimHostToInternal(cloudSimHost);
            instance.addHost(internalHost);
            
            // Atualizar mapeamentos
            cloudSimToInternalHost.put(cloudSimHost, internalHost);
            internalToCloudSimHost.put(internalHost, cloudSimHost);
        }
        
        // Converter apenas VMs já alocadas nos hosts (evita VMs órfãs)
        Set<Vm> allocatedVms = new HashSet<>();
        
        // Adicionar VMs já alocadas nos hosts
        for (org.cloudsimplus.hosts.Host cloudSimHost : getHostList()) {
            allocatedVms.addAll(cloudSimHost.getVmList());
        }
        
        for (Vm cloudSimVm : allocatedVms) {
            // Verificar se já está mapeada para evitar duplicatas
            if (!cloudSimToInternalVm.containsKey(cloudSimVm)) {
                VM internalVm = convertCloudSimVmToInternal(cloudSimVm);
                instance.addVM(internalVm);
                
                // Atualizar mapeamentos
                cloudSimToInternalVm.put(cloudSimVm, internalVm);
                internalToCloudSimVm.put(internalVm, cloudSimVm);
            } else {
                // VM já mapeada, apenas adicionar à instância
                VM internalVm = cloudSimToInternalVm.get(cloudSimVm);
                instance.addVM(internalVm);
            }
        }
        
        logger.debug("Created problem instance with {} hosts and {} VMs", 
                    instance.getHostCount(), instance.getVMCount());
        
        return instance;
    }
    
    /**
     * Converte Host do CloudSim para nosso modelo interno
     */
    private Host convertCloudSimHostToInternal(org.cloudsimplus.hosts.Host cloudSimHost) {
        // Calcular custo baseado nas características do host
        double activationCost = calculateHostActivationCost(cloudSimHost);
        
        // Simular probabilidade de falha baseada na utilização
        double failureProbability = calculateHostFailureProbability(cloudSimHost);
        
        // Estimar consumo de energia
        double energyConsumption = calculateHostEnergyConsumption(cloudSimHost);
        
        Host internalHost = new Host((int) cloudSimHost.getId(), activationCost, 
                                   failureProbability, energyConsumption);
        
        // Converter capacidades de recursos
        double totalMips = cloudSimHost.getPeList().stream()
                .mapToDouble(pe -> pe.getCapacity())
                .sum();
        
        internalHost.setCap(ResourceType.CPU, totalMips / 1000.0); // MIPS -> units
        internalHost.setCap(ResourceType.RAM, cloudSimHost.getRam().getCapacity() / 1024.0); // MB -> GB
        internalHost.setCap(ResourceType.STORAGE, cloudSimHost.getStorage().getCapacity() / 1024.0); // MB -> GB
        internalHost.setCap(ResourceType.NETWORK, cloudSimHost.getBw().getCapacity() / 1000.0); // Mbps -> Gbps
        
        return internalHost;
    }
    
    /**
     * Converte VM do CloudSim para nosso modelo interno
     */
    private VM convertCloudSimVmToInternal(Vm cloudSimVm) {
        // Calcular requisito mínimo de confiabilidade baseado no tipo da VM
        double minReliability = calculateVmMinReliability(cloudSimVm);
        
        // Calcular prioridade baseada nas características da VM
        double priority = calculateVmPriority(cloudSimVm);
        
        VM internalVm = new VM((int) cloudSimVm.getId(), minReliability, priority);
        
        // Converter demandas de recursos
        double totalMips = cloudSimVm.getPesNumber() * cloudSimVm.getMips();
        
        internalVm.setDemand(ResourceType.CPU, totalMips / 1000.0); // MIPS -> units
        internalVm.setDemand(ResourceType.RAM, cloudSimVm.getRam().getCapacity() / 1024.0); // MB -> GB
        internalVm.setDemand(ResourceType.STORAGE, cloudSimVm.getStorage().getCapacity() / 1024.0); // MB -> GB
        internalVm.setDemand(ResourceType.NETWORK, cloudSimVm.getBw().getCapacity() / 1000.0); // Mbps -> Gbps
        
        return internalVm;
    }
    
    /**
     * Executa otimização (com cache se disponível)
     */
    private AllocationSolution getOptimalAllocation(ProblemInstance instance) {
        long currentTime = System.currentTimeMillis();
        
        // Verificar se podemos usar a solução em cache
        if (lastOptimalSolution != null && 
            (currentTime - lastOptimizationTime) < OPTIMIZATION_CACHE_TTL) {
            logger.debug("Using cached optimization solution");
            return lastOptimalSolution;
        }
        
        logger.debug("Running new optimization for {} VMs and {} hosts", 
                    instance.getVMCount(), instance.getHostCount());
        
        // Executar otimização
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, optimizationParams);
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        
        // Armazenar em cache
        lastOptimalSolution = result.getBestCostSolution();
        lastOptimizationTime = currentTime;
        
        logger.debug("Optimization completed - found solution with cost: {}", 
                    lastOptimalSolution != null ? 
                    String.format("%.2f", lastOptimalSolution.getTotalCost()) : "none");
        
        return lastOptimalSolution;
    }
    
    /**
     * Aplica a solução de alocação encontrada
     */
    private HostSuitability applyAllocationSolution(Vm targetVm, AllocationSolution solution) {
        if (solution == null) {
            logger.warn("No optimal solution found, using fallback allocation");
            return allocateHostForVmSimple(targetVm);
        }
        
        // Encontrar onde a VM alvo deve ser alocada na solução
        VM internalTargetVm = cloudSimToInternalVm.get(targetVm);
        if (internalTargetVm == null) {
            logger.error("Target VM not found in internal mapping");
            // Use fallback allocation instead of returning unsuitable
            return allocateHostForVmSimple(targetVm);
        }
        
        Host targetHost = solution.getVmToHost().get(internalTargetVm);
        if (targetHost == null) {
            logger.warn("VM {} not allocated in optimal solution", targetVm.getId());
            return allocateHostForVmSimple(targetVm);
        }
        
        // Encontrar o host correspondente no CloudSim
        org.cloudsimplus.hosts.Host cloudSimTargetHost = internalToCloudSimHost.get(targetHost);
        if (cloudSimTargetHost == null) {
            logger.error("Target host not found in CloudSim mapping");
            // Use fallback allocation instead of returning unsuitable
            return allocateHostForVmSimple(targetVm);
        }
        
        // Verificar se o host pode alocar a VM
        HostSuitability suitability = cloudSimTargetHost.createVm(targetVm);
        if (suitability.fully()) {
            logger.debug("VM {} allocated to host {} using optimal solution", 
                       targetVm.getId(), cloudSimTargetHost.getId());
            return suitability;
        }
        
        logger.warn("Optimal host {} cannot allocate VM {}, using fallback", 
                   cloudSimTargetHost.getId(), targetVm.getId());
        return allocateHostForVmSimple(targetVm);
    }
    
    /**
     * Alocação fallback simples (first-fit)
     */
    private HostSuitability allocateHostForVmSimple(Vm vm) {
        logger.debug("Using fallback allocation for VM {}", vm.getId());
        
        for (org.cloudsimplus.hosts.Host host : getHostList()) {
            HostSuitability suitability = host.createVm(vm);
            if (suitability.fully()) {
                logger.debug("VM {} allocated to host {} using fallback allocation", 
                           vm.getId(), host.getId());
                return suitability;
            }
        }
        
        // Log detailed resource information for debugging
        logger.error("VM {} allocation failed - no suitable host found", vm.getId());
        logger.error("VM {} requires: CPU {} MIPS, RAM {} MB, Storage {} MB", 
                    vm.getId(), 
                    vm.getPesNumber() * vm.getMips(),
                    vm.getRam().getCapacity(),
                    vm.getStorage().getCapacity());
        
        for (org.cloudsimplus.hosts.Host host : getHostList()) {
            long availableRam = host.getRam().getAvailableResource();
            long availableStorage = host.getStorage().getAvailableResource();
            double availableMips = host.getPeList().stream()
                .mapToDouble(pe -> pe.getCapacity())
                .sum();
            
            logger.error("Host {} available: CPU {} MIPS, RAM {} MB, Storage {} MB", 
                        host.getId(), availableMips, availableRam, availableStorage);
        }
        
        // Return HostSuitability.NULL to indicate failure without causing infinite loops
        return HostSuitability.NULL;
    }
    
    /**
     * Calcula custo de ativação do host baseado em suas características
     */
    private double calculateHostActivationCost(org.cloudsimplus.hosts.Host host) {
        // Custo baseado na capacidade total do host
        double totalMips = host.getPeList().stream().mapToDouble(pe -> pe.getCapacity()).sum();
        double cpuFactor = totalMips / 1000.0; // MIPS -> units
        double ramFactor = host.getRam().getCapacity() / 1024.0; // MB -> GB
        double storageFactor = host.getStorage().getCapacity() / (1024.0 * 100.0); // MB -> 100GB units
        
        return 50.0 + (cpuFactor * 10.0) + (ramFactor * 5.0) + (storageFactor * 2.0);
    }
    
    /**
     * Calcula probabilidade de falha baseada na utilização atual
     */
    private double calculateHostFailureProbability(org.cloudsimplus.hosts.Host host) {
        // Probabilidade base
        double baseProbability = 0.01;
        
        // Aumentar baseado na utilização
        double cpuUtilization = host.getCpuPercentUtilization();
        double ramUtilization = host.getRam().getPercentUtilization();
        
        double utilizationFactor = (cpuUtilization + ramUtilization) / 2.0;
        
        // Máximo de 5% de probabilidade de falha
        return Math.min(0.05, baseProbability + (utilizationFactor * 0.04));
    }
    
    /**
     * Calcula consumo de energia do host
     */
    private double calculateHostEnergyConsumption(org.cloudsimplus.hosts.Host host) {
        // Consumo base + consumo baseado na capacidade
        double baseConsumption = 100.0;
        double totalMips = host.getPeList().stream().mapToDouble(pe -> pe.getCapacity()).sum();
        double capacityFactor = totalMips / 10000.0; // MIPS
        
        return baseConsumption + (capacityFactor * 50.0);
    }
    
    /**
     * Calcula requisito mínimo de confiabilidade da VM
     */
    private double calculateVmMinReliability(Vm vm) {
        // VMs com mais recursos precisam de maior confiabilidade
        double totalMips = vm.getPesNumber() * vm.getMips();
        double resourceFactor = (totalMips + vm.getRam().getCapacity()) / 10000.0;
        
        // Entre 0.90 e 0.99
        return Math.min(0.99, Math.max(0.90, 0.90 + (resourceFactor * 0.09)));
    }
    
    /**
     * Calcula prioridade da VM
     */
    private double calculateVmPriority(Vm vm) {
        // Prioridade baseada na demanda de recursos
        double totalMips = vm.getPesNumber() * vm.getMips();
        double totalDemand = totalMips + vm.getRam().getCapacity() + vm.getStorage().getCapacity();
        
        // Entre 1.0 e 3.0
        return Math.min(3.0, Math.max(1.0, 1.0 + (totalDemand / 50000.0)));
    }
    
    /**
     * Invalida o cache de otimização
     */
    private void invalidateOptimizationCache() {
        lastOptimalSolution = null;
        lastOptimizationTime = 0;
        logger.debug("Optimization cache invalidated");
    }
    
    /**
     * Método para forçar reotimização
     */
    public void forceReoptimization() {
        invalidateOptimizationCache();
        logger.info("Forced reoptimization triggered");
    }
    
    /**
     * Obtém estatísticas da alocação atual
     */
    public AllocationStatistics getCurrentAllocationStatistics() {
        int totalVms = 0;
        int allocatedVms = 0;
        int activeHosts = 0;
        double totalCost = 0.0;
        double totalEnergyConsumption = 0.0;
        
        for (org.cloudsimplus.hosts.Host host : getHostList()) {
            if (!host.getVmList().isEmpty()) {
                activeHosts++;
                Host internalHost = cloudSimToInternalHost.get(host);
                if (internalHost != null) {
                    totalCost += internalHost.getCost();
                    totalEnergyConsumption += internalHost.getEnergy();
                }
            }
            
            totalVms += host.getVmList().size();
            allocatedVms += host.getVmList().size();
        }
        
        return new AllocationStatistics(totalVms, allocatedVms, activeHosts, 
                                      totalCost, totalEnergyConsumption);
    }
    
    /**
     * Classe para encapsular estatísticas da alocação
     */
    public static class AllocationStatistics {
        public final int totalVms;
        public final int allocatedVms;
        public final int activeHosts;
        public final double totalCost;
        public final double totalEnergyConsumption;
        
        public AllocationStatistics(int totalVms, int allocatedVms, int activeHosts, 
                                  double totalCost, double totalEnergyConsumption) {
            this.totalVms = totalVms;
            this.allocatedVms = allocatedVms;
            this.activeHosts = activeHosts;
            this.totalCost = totalCost;
            this.totalEnergyConsumption = totalEnergyConsumption;
        }
        
        public double getAllocationRate() {
            return totalVms > 0 ? (double) allocatedVms / totalVms : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("AllocationStats{VMs: %d/%d (%.1f%%), ActiveHosts: %d, Cost: %.2f, Energy: %.2f kWh}", 
                               allocatedVms, totalVms, getAllocationRate() * 100, 
                               activeHosts, totalCost, totalEnergyConsumption);
        }
    }
    
    /**
     * Factory method para criar com parâmetros otimizados para tempo real
     */
    public static CloudSimRealTimeAllocator createRealTimeOptimized() {
        OptimizationParameters params = new OptimizationParameters.Builder()
                .brkgaPopulationSize(20)  // Menor para ser mais rápido
                .brkgaMaxGenerations(30)  // Menos gerações para tempo real
                .archiveSize(10)          // Archive menor
                .decodingStrategy(com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.GREEDY_COST)
                .randomSeed(System.currentTimeMillis())
                .build();
        
        return new CloudSimRealTimeAllocator(params);
    }
    
    /**
     * Factory method para criar com parâmetros balanceados
     */
    public static CloudSimRealTimeAllocator createBalanced() {
        OptimizationParameters params = new OptimizationParameters.Builder()
                .brkgaPopulationSize(40)
                .brkgaMaxGenerations(50)
                .archiveSize(20)
                .decodingStrategy(com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.BALANCED)
                .randomSeed(System.currentTimeMillis())
                .build();
        
        return new CloudSimRealTimeAllocator(params);
    }
} 