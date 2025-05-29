package com.ramonyago.cloudsim.simulation;

import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import com.ramonyago.cloudsim.model.Host;
import com.ramonyago.cloudsim.model.ResourceType;

import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModel;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Integração com CloudSim Plus para simulação real de datacenter
 * baseada nas soluções de alocação otimizadas.
 */
public class CloudSimSimulator {
    private static final Logger logger = LoggerFactory.getLogger(CloudSimSimulator.class);
    
    private final CloudSimPlus simulation;
    private final ProblemInstance instance;
    private final SimulationParameters parameters;
    
    // Componentes da simulação
    private Datacenter datacenter;
    private DatacenterBroker broker;
    private List<org.cloudsimplus.hosts.Host> cloudSimHosts;
    private List<org.cloudsimplus.vms.Vm> cloudSimVms;
    private List<Cloudlet> cloudlets;
    
    // Mapeamento entre modelos
    private Map<Host, org.cloudsimplus.hosts.Host> hostMapping;
    private Map<VM, org.cloudsimplus.vms.Vm> vmMapping;
    
    // Resultados da simulação
    private SimulationResults results;
    
    public CloudSimSimulator(ProblemInstance instance, SimulationParameters parameters) {
        this.instance = instance;
        this.parameters = parameters;
        this.simulation = new CloudSimPlus();
        this.hostMapping = new HashMap<>();
        this.vmMapping = new HashMap<>();
        
        logger.info("CloudSim Simulator initialized for instance: {}", instance.getInstanceName());
    }
    
    /**
     * Executa simulação baseada em uma solução de alocação
     */
    public SimulationResults runSimulation(AllocationSolution solution) {
        logger.info("Starting CloudSim simulation...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Criar datacenter com hosts
            createDatacenter(solution);
            
            // 2. Criar broker
            createBroker();
            
            // 3. Criar VMs baseadas na solução
            createVMs(solution);
            
            // 4. Criar cloudlets (workloads)
            createCloudlets();
            
            // 5. Executar simulação
            simulation.start();
            
            // 6. Coletar resultados
            collectResults(solution);
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("CloudSim simulation completed in {} ms", executionTime);
            
            return results;
            
        } catch (Exception e) {
            logger.error("Error during CloudSim simulation", e);
            throw new RuntimeException("Simulation failed", e);
        }
    }
    
    /**
     * Cria o datacenter com hosts baseados na solução
     */
    private void createDatacenter(AllocationSolution solution) {
        cloudSimHosts = new ArrayList<>();
        
        // Criar apenas hosts ativos na solução
        Set<Host> activeHosts = solution.getActiveHosts();
        logger.info("Creating datacenter with {} active hosts", activeHosts.size());
        
        for (Host host : activeHosts) {
            org.cloudsimplus.hosts.Host cloudSimHost = createCloudSimHost(host);
            cloudSimHosts.add(cloudSimHost);
            hostMapping.put(host, cloudSimHost);
        }
        
        // Criar datacenter
        datacenter = new DatacenterSimple(simulation, cloudSimHosts);
        datacenter.getCharacteristics()
                .setCostPerSecond(parameters.getCostPerSecond())
                .setCostPerMem(parameters.getCostPerMem())
                .setCostPerStorage(parameters.getCostPerStorage())
                .setCostPerBw(parameters.getCostPerBw());
        
        logger.info("Datacenter created with {} hosts", cloudSimHosts.size());
    }
    
    /**
     * Converte Host do modelo para CloudSim Host
     */
    private org.cloudsimplus.hosts.Host createCloudSimHost(Host host) {
        // Criar PEs (Processing Elements) baseados na capacidade de CPU
        double cpuCapacity = host.getCap(ResourceType.CPU);
        int peCount = Math.max(1, (int) Math.ceil(cpuCapacity));
        List<Pe> peList = new ArrayList<>();
        
        for (int i = 0; i < peCount; i++) {
            peList.add(new PeSimple(cpuCapacity / peCount * 1000)); // MIPS
        }
        
        // Converter capacidades para unidades CloudSim
        long ram = (long) (host.getCap(ResourceType.RAM) * 1024); // MB
        long storage = (long) (host.getCap(ResourceType.STORAGE) * 1024); // MB
        long bw = 10000; // Bandwidth padrão
        
        org.cloudsimplus.hosts.Host cloudSimHost = new HostSimple(ram, bw, storage, peList);
        cloudSimHost.setVmScheduler(new VmSchedulerTimeShared());
        cloudSimHost.setId(host.getHostId());
        
        // Configurar características de energia e falha
        cloudSimHost.enableUtilizationStats();
        
        return cloudSimHost;
    }
    
    /**
     * Cria o broker para gerenciar VMs e cloudlets
     */
    private void createBroker() {
        broker = new DatacenterBrokerSimple(simulation);
        logger.info("Datacenter broker created");
    }
    
    /**
     * Cria VMs baseadas na solução de alocação
     */
    private void createVMs(AllocationSolution solution) {
        cloudSimVms = new ArrayList<>();
        
        for (Map.Entry<VM, Host> entry : solution.getVmToHost().entrySet()) {
            VM vm = entry.getKey();
            Host host = entry.getValue();
            
            org.cloudsimplus.vms.Vm cloudSimVm = createCloudSimVM(vm);
            cloudSimVms.add(cloudSimVm);
            vmMapping.put(vm, cloudSimVm);
        }
        
        // Submeter VMs ao broker
        broker.submitVmList(cloudSimVms);
        logger.info("Created and submitted {} VMs", cloudSimVms.size());
    }
    
    /**
     * Converte VM do modelo para CloudSim VM
     */
    private org.cloudsimplus.vms.Vm createCloudSimVM(VM vm) {
        // Converter demandas para unidades CloudSim
        double mips = vm.getDemand(ResourceType.CPU) * 1000; // MIPS
        long ram = (long) (vm.getDemand(ResourceType.RAM) * 1024); // MB
        long storage = (long) (vm.getDemand(ResourceType.STORAGE) * 1024); // MB
        long bw = 1000; // Bandwidth padrão
        
        org.cloudsimplus.vms.Vm cloudSimVm = new VmSimple(mips, 1); // 1 PE por VM
        cloudSimVm.setRam(ram)
                .setSize(storage)
                .setBw(bw)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
        
        cloudSimVm.setId(vm.getVmId());
        
        return cloudSimVm;
    }
    
    /**
     * Cria cloudlets (workloads) para as VMs
     */
    private void createCloudlets() {
        cloudlets = new ArrayList<>();
        
        for (org.cloudsimplus.vms.Vm vm : cloudSimVms) {
            // Criar múltiplos cloudlets por VM para simular workload
            int cloudletsPerVm = parameters.getCloudletsPerVm();
            
            for (int i = 0; i < cloudletsPerVm; i++) {
                Cloudlet cloudlet = createCloudlet(vm, i);
                cloudlets.add(cloudlet);
            }
        }
        
        // Submeter cloudlets ao broker
        broker.submitCloudletList(cloudlets);
        logger.info("Created and submitted {} cloudlets", cloudlets.size());
    }
    
    /**
     * Cria um cloudlet individual
     */
    private Cloudlet createCloudlet(org.cloudsimplus.vms.Vm vm, int index) {
        // Parâmetros do cloudlet baseados na VM
        long length = parameters.getCloudletLength(); // MI (Million Instructions)
        long fileSize = 300; // bytes
        long outputSize = 300; // bytes
        int pesNumber = 1;
        
        UtilizationModel utilizationModel = new UtilizationModelDynamic(0.1, 1.0);
        
        Cloudlet cloudlet = new CloudletSimple(length, pesNumber);
        cloudlet.setFileSize(fileSize)
                .setOutputSize(outputSize)
                .setUtilizationModelCpu(utilizationModel)
                .setUtilizationModelRam(new UtilizationModelFull())
                .setUtilizationModelBw(new UtilizationModelFull());
        
        // Definir VM preferida
        cloudlet.setVm(vm);
        
        return cloudlet;
    }
    
    /**
     * Coleta resultados da simulação
     */
    private void collectResults(AllocationSolution solution) {
        logger.info("Collecting simulation results...");
        
        // Estatísticas básicas
        double totalExecutionTime = simulation.clock();
        double totalCost = calculateTotalCost();
        double totalEnergyConsumption = calculateEnergyConsumption();
        
        // Estatísticas de VMs
        Map<VM, VmStatistics> vmStats = new HashMap<>();
        for (Map.Entry<VM, org.cloudsimplus.vms.Vm> entry : vmMapping.entrySet()) {
            VM originalVm = entry.getKey();
            org.cloudsimplus.vms.Vm cloudSimVm = entry.getValue();
            
            VmStatistics stats = new VmStatistics(
                cloudSimVm.getCpuUtilizationStats().getMean(),
                0.8, // RAM utilization simulada
                0.6, // BW utilization simulada
                cloudSimVm.getTotalExecutionTime(),
                cloudSimVm.getHost().getId()
            );
            
            vmStats.put(originalVm, stats);
        }
        
        // Estatísticas de Hosts
        Map<Host, HostStatistics> hostStats = new HashMap<>();
        for (Map.Entry<Host, org.cloudsimplus.hosts.Host> entry : hostMapping.entrySet()) {
            Host originalHost = entry.getKey();
            org.cloudsimplus.hosts.Host cloudSimHost = entry.getValue();
            
            HostStatistics stats = new HostStatistics(
                cloudSimHost.getCpuUtilizationStats().getMean(),
                0.7, // RAM utilization simulada
                0.5, // BW utilization simulada
                originalHost.getEnergy(), // Power consumption do modelo original
                cloudSimHost.getVmList().size()
            );
            
            hostStats.put(originalHost, stats);
        }
        
        // Estatísticas de Cloudlets
        List<CloudletStatistics> cloudletStats = cloudlets.stream()
            .map(cloudlet -> new CloudletStatistics(
                cloudlet.getId(),
                cloudlet.getVm().getId(),
                cloudlet.isFinished() ? cloudlet.getFinishTime() : 0.0,
                0.0, // Waiting time simplificado
                cloudlet.getStatus().name()
            ))
            .collect(Collectors.toList());
        
        results = new SimulationResults(
            totalExecutionTime,
            totalCost,
            totalEnergyConsumption,
            vmStats,
            hostStats,
            cloudletStats,
            solution
        );
        
        logger.info("Results collected - Execution time: {}, Total cost: {}, Energy: {}", 
                   String.format("%.2f", totalExecutionTime),
                   String.format("%.2f", totalCost),
                   String.format("%.2f", totalEnergyConsumption));
    }
    
    /**
     * Calcula custo total da simulação
     */
    private double calculateTotalCost() {
        // Calcular custo baseado no tempo de execução e recursos utilizados
        double totalCost = 0.0;
        
        for (org.cloudsimplus.vms.Vm vm : cloudSimVms) {
            double executionTime = vm.getTotalExecutionTime();
            double cpuCost = executionTime * parameters.getCostPerSecond();
            double ramCost = vm.getRam().getCapacity() * parameters.getCostPerMem() * executionTime / 3600; // por hora
            double storageCost = vm.getStorage().getCapacity() * parameters.getCostPerStorage() * executionTime / 3600;
            
            totalCost += cpuCost + ramCost + storageCost;
        }
        
        return totalCost;
    }
    
    /**
     * Calcula consumo total de energia
     */
    private double calculateEnergyConsumption() {
        return cloudSimHosts.stream()
            .mapToDouble(host -> {
                // Usar energia do modelo original
                Host originalHost = hostMapping.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(host))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
                
                if (originalHost != null) {
                    return originalHost.getEnergy() * simulation.clock() / 3600; // kWh
                }
                return 100.0 * simulation.clock() / 3600; // Default
            })
            .sum();
    }
    
    /**
     * Parâmetros de configuração da simulação
     */
    public static class SimulationParameters {
        private double costPerSecond = 0.01;
        private double costPerMem = 0.05;
        private double costPerStorage = 0.001;
        private double costPerBw = 0.0;
        private int cloudletsPerVm = 5;
        private long cloudletLength = 10000; // MI
        
        // Getters e setters
        public double getCostPerSecond() { return costPerSecond; }
        public void setCostPerSecond(double costPerSecond) { this.costPerSecond = costPerSecond; }
        
        public double getCostPerMem() { return costPerMem; }
        public void setCostPerMem(double costPerMem) { this.costPerMem = costPerMem; }
        
        public double getCostPerStorage() { return costPerStorage; }
        public void setCostPerStorage(double costPerStorage) { this.costPerStorage = costPerStorage; }
        
        public double getCostPerBw() { return costPerBw; }
        public void setCostPerBw(double costPerBw) { this.costPerBw = costPerBw; }
        
        public int getCloudletsPerVm() { return cloudletsPerVm; }
        public void setCloudletsPerVm(int cloudletsPerVm) { this.cloudletsPerVm = cloudletsPerVm; }
        
        public long getCloudletLength() { return cloudletLength; }
        public void setCloudletLength(long cloudletLength) { this.cloudletLength = cloudletLength; }
        
        public static SimulationParameters createDefault() {
            return new SimulationParameters();
        }
    }
    
    /**
     * Estatísticas de VM
     */
    public static class VmStatistics {
        public final double cpuUtilization;
        public final double ramUtilization;
        public final double bwUtilization;
        public final double executionTime;
        public final long hostId;
        
        public VmStatistics(double cpuUtilization, double ramUtilization, double bwUtilization, 
                           double executionTime, long hostId) {
            this.cpuUtilization = cpuUtilization;
            this.ramUtilization = ramUtilization;
            this.bwUtilization = bwUtilization;
            this.executionTime = executionTime;
            this.hostId = hostId;
        }
    }
    
    /**
     * Estatísticas de Host
     */
    public static class HostStatistics {
        public final double cpuUtilization;
        public final double ramUtilization;
        public final double bwUtilization;
        public final double powerConsumption;
        public final int vmCount;
        
        public HostStatistics(double cpuUtilization, double ramUtilization, double bwUtilization,
                             double powerConsumption, int vmCount) {
            this.cpuUtilization = cpuUtilization;
            this.ramUtilization = ramUtilization;
            this.bwUtilization = bwUtilization;
            this.powerConsumption = powerConsumption;
            this.vmCount = vmCount;
        }
    }
    
    /**
     * Estatísticas de Cloudlet
     */
    public static class CloudletStatistics {
        public final long cloudletId;
        public final long vmId;
        public final double executionTime;
        public final double waitingTime;
        public final String status;
        
        public CloudletStatistics(long cloudletId, long vmId, double executionTime, 
                                 double waitingTime, String status) {
            this.cloudletId = cloudletId;
            this.vmId = vmId;
            this.executionTime = executionTime;
            this.waitingTime = waitingTime;
            this.status = status;
        }
    }
    
    /**
     * Resultados completos da simulação
     */
    public static class SimulationResults {
        public final double totalExecutionTime;
        public final double totalCost;
        public final double totalEnergyConsumption;
        public final Map<VM, VmStatistics> vmStatistics;
        public final Map<Host, HostStatistics> hostStatistics;
        public final List<CloudletStatistics> cloudletStatistics;
        public final AllocationSolution originalSolution;
        
        public SimulationResults(double totalExecutionTime, double totalCost, double totalEnergyConsumption,
                               Map<VM, VmStatistics> vmStatistics, Map<Host, HostStatistics> hostStatistics,
                               List<CloudletStatistics> cloudletStatistics, AllocationSolution originalSolution) {
            this.totalExecutionTime = totalExecutionTime;
            this.totalCost = totalCost;
            this.totalEnergyConsumption = totalEnergyConsumption;
            this.vmStatistics = vmStatistics;
            this.hostStatistics = hostStatistics;
            this.cloudletStatistics = cloudletStatistics;
            this.originalSolution = originalSolution;
        }
        
        /**
         * Gera relatório textual dos resultados
         */
        public String generateReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== CloudSim Simulation Results ===\n");
            sb.append(String.format("Total Execution Time: %.2f seconds\n", totalExecutionTime));
            sb.append(String.format("Total Cost: $%.2f\n", totalCost));
            sb.append(String.format("Total Energy Consumption: %.2f kWh\n", totalEnergyConsumption));
            sb.append(String.format("VMs Simulated: %d\n", vmStatistics.size()));
            sb.append(String.format("Hosts Used: %d\n", hostStatistics.size()));
            sb.append(String.format("Cloudlets Executed: %d\n", cloudletStatistics.size()));
            
            sb.append("\n--- VM Statistics ---\n");
            for (Map.Entry<VM, VmStatistics> entry : vmStatistics.entrySet()) {
                VM vm = entry.getKey();
                VmStatistics stats = entry.getValue();
                sb.append(String.format("VM %d: CPU=%.1f%%, RAM=%.1f%%, Host=%d\n",
                    vm.getVmId(), stats.cpuUtilization * 100, stats.ramUtilization * 100, stats.hostId));
            }
            
            sb.append("\n--- Host Statistics ---\n");
            for (Map.Entry<Host, HostStatistics> entry : hostStatistics.entrySet()) {
                Host host = entry.getKey();
                HostStatistics stats = entry.getValue();
                sb.append(String.format("Host %d: CPU=%.1f%%, RAM=%.1f%%, VMs=%d, Power=%.2fW\n",
                    host.getHostId(), stats.cpuUtilization * 100, stats.ramUtilization * 100, 
                    stats.vmCount, stats.powerConsumption));
            }
            
            return sb.toString();
        }
    }
} 