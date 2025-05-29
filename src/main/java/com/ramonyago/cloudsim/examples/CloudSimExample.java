package com.ramonyago.cloudsim.examples;

import com.ramonyago.cloudsim.OptimizationParameters;
import com.ramonyago.cloudsim.VMAllocationOptimizer;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import com.ramonyago.cloudsim.model.Host;
import com.ramonyago.cloudsim.model.ResourceType;
import com.ramonyago.cloudsim.simulation.CloudSimSimulator;
import com.ramonyago.cloudsim.simulation.SimulationIntegrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

/**
 * Exemplo prático de uso da integração CloudSim Plus.
 * Demonstra como usar o sistema completo de otimização + simulação.
 */
public class CloudSimExample {
    private static final Logger logger = LoggerFactory.getLogger(CloudSimExample.class);
    
    public static void main(String[] args) {
        logger.info("=== CloudSim Integration Example ===");
        
        try {
            // Exemplo 1: Simulação básica
            basicSimulationExample();
            
            // Exemplo 2: Comparação de soluções
            solutionComparisonExample();
            
            // Exemplo 3: Análise de performance
            performanceAnalysisExample();
            
        } catch (Exception e) {
            logger.error("Example execution failed", e);
        }
    }
    
    /**
     * Exemplo básico: otimização seguida de simulação
     */
    private static void basicSimulationExample() {
        logger.info("\n" + "=".repeat(50));
        logger.info("BASIC SIMULATION EXAMPLE");
        logger.info("=".repeat(50));
        
        // 1. Criar instância do problema
        ProblemInstance instance = createCustomInstance();
        logger.info("Created instance with {} VMs and {} hosts", 
                   instance.getVMCount(), instance.getHostCount());
        
        // 2. Configurar parâmetros de otimização
        OptimizationParameters optParams = new OptimizationParameters.Builder()
                .brkgaPopulationSize(30)
                .brkgaMaxGenerations(50)
                .archiveSize(20)
                .randomSeed(123)
                .build();
        
        // 3. Executar otimização
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, optParams);
        VMAllocationOptimizer.OptimizationResult optResult = optimizer.optimize();
        
        logger.info("Optimization completed - Archive size: {}", optResult.getArchive().size());
        
        // 4. Configurar simulação
        CloudSimSimulator.SimulationParameters simParams = 
            CloudSimSimulator.SimulationParameters.createDefault();
        simParams.setCloudletsPerVm(4);
        simParams.setCloudletLength(8000);
        simParams.setCostPerSecond(0.02);
        
        // 5. Simular melhor solução
        AllocationSolution bestSolution = optResult.getBestCostSolution();
        if (bestSolution != null) {
            logger.info("Simulating best cost solution...");
            
            CloudSimSimulator simulator = new CloudSimSimulator(instance, simParams);
            CloudSimSimulator.SimulationResults simResults = simulator.runSimulation(bestSolution);
            
            // 6. Mostrar resultados
            logger.info("\nSIMULATION RESULTS:");
            logger.info("Execution Time: {} seconds", String.format("%.2f", simResults.totalExecutionTime));
            logger.info("Total Cost: ${}", String.format("%.2f", simResults.totalCost));
            logger.info("Energy Consumption: {} kWh", String.format("%.2f", simResults.totalEnergyConsumption));
            logger.info("VMs Executed: {}", simResults.vmStatistics.size());
            logger.info("Hosts Used: {}", simResults.hostStatistics.size());
            
            // Análise de utilização
            analyzeResourceUtilization(simResults);
        }
    }
    
    /**
     * Exemplo de comparação entre múltiplas soluções
     */
    private static void solutionComparisonExample() {
        logger.info("\n" + "=".repeat(50));
        logger.info("SOLUTION COMPARISON EXAMPLE");
        logger.info("=".repeat(50));
        
        ProblemInstance instance = InstanceReader.createSampleInstance();
        
        // Gerar múltiplas soluções com diferentes estratégias
        List<AllocationSolution> solutions = generateMultipleSolutions(instance);
        logger.info("Generated {} solutions for comparison", solutions.size());
        
        // Usar integrador para comparar
        SimulationIntegrator integrator = SimulationIntegrator.createDefault(instance);
        SimulationIntegrator.ComparisonResults comparison = integrator.compareSolutions(solutions);
        
        // Mostrar resultados da comparação
        logger.info("\nCOMPARISON RESULTS:");
        System.out.println(comparison.generateComparisonReport());
        
        // Identificar melhores soluções
        AllocationSolution bestByCost = comparison.getBestByCost();
        AllocationSolution bestByEnergy = comparison.getBestByEnergy();
        
        if (bestByCost != null) {
            logger.info("Best solution by cost: {} active hosts", 
                       bestByCost.getActiveHosts().size());
        }
        
        if (bestByEnergy != null) {
            logger.info("Best solution by energy: {} active hosts", 
                       bestByEnergy.getActiveHosts().size());
        }
    }
    
    /**
     * Exemplo de análise de performance detalhada
     */
    private static void performanceAnalysisExample() {
        logger.info("\n" + "=".repeat(50));
        logger.info("PERFORMANCE ANALYSIS EXAMPLE");
        logger.info("=".repeat(50));
        
        ProblemInstance instance = createLargeInstance();
        logger.info("Created large instance with {} VMs and {} hosts", 
                   instance.getVMCount(), instance.getHostCount());
        
        // Executar análise integrada
        SimulationIntegrator integrator = SimulationIntegrator.createQuick(instance);
        SimulationIntegrator.IntegratedResults results = integrator.runOptimizationAndSimulation();
        
        // Análise detalhada
        logger.info("\nPERFORMANCE ANALYSIS:");
        logger.info("Total execution time: {} ms", results.totalExecutionTime);
        logger.info("Solutions optimized: {}", results.optimizationResult.getArchive().size());
        logger.info("Solutions simulated: {}", results.simulationResults.size());
        
        // Análise de diferenças de custo
        var costDifferences = results.getCostDifferences();
        if (!costDifferences.isEmpty()) {
            logger.info("\nCOST ACCURACY ANALYSIS:");
            double avgDifference = costDifferences.values().stream()
                .mapToDouble(diff -> Math.abs(diff.percentageDifference))
                .average()
                .orElse(0.0);
            
            logger.info("Average cost difference: {:.1f}%", avgDifference);
            
            double maxDifference = costDifferences.values().stream()
                .mapToDouble(diff -> Math.abs(diff.percentageDifference))
                .max()
                .orElse(0.0);
            
            logger.info("Maximum cost difference: {:.1f}%", maxDifference);
        }
        
        // Relatório completo
        System.out.println("\n" + results.generateCompleteReport());
    }
    
    /**
     * Cria uma instância customizada para demonstração
     */
    private static ProblemInstance createCustomInstance() {
        ProblemInstance instance = new ProblemInstance("custom-example");
        
        // Criar VMs com diferentes características
        VM vm1 = new VM(1, 0.99, 2.0); // Alta confiabilidade, alta prioridade
        vm1.setDemand(ResourceType.CPU, 4.0);
        vm1.setDemand(ResourceType.RAM, 8.0);
        vm1.setDemand(ResourceType.STORAGE, 50.0);
        
        VM vm2 = new VM(2, 0.95, 1.5); // Confiabilidade média
        vm2.setDemand(ResourceType.CPU, 2.0);
        vm2.setDemand(ResourceType.RAM, 4.0);
        vm2.setDemand(ResourceType.STORAGE, 25.0);
        
        VM vm3 = new VM(3, 0.90, 1.0); // Baixa confiabilidade
        vm3.setDemand(ResourceType.CPU, 1.0);
        vm3.setDemand(ResourceType.RAM, 2.0);
        vm3.setDemand(ResourceType.STORAGE, 15.0);
        
        VM vm4 = new VM(4, 0.98, 1.8); // Alta confiabilidade
        vm4.setDemand(ResourceType.CPU, 3.0);
        vm4.setDemand(ResourceType.RAM, 6.0);
        vm4.setDemand(ResourceType.STORAGE, 40.0);
        
        instance.addVM(vm1);
        instance.addVM(vm2);
        instance.addVM(vm3);
        instance.addVM(vm4);
        
        // Criar hosts com diferentes características
        Host host1 = new Host(1, 150.0, 0.01, 200.0); // Caro, confiável
        host1.setCap(ResourceType.CPU, 8.0);
        host1.setCap(ResourceType.RAM, 16.0);
        host1.setCap(ResourceType.STORAGE, 100.0);
        
        Host host2 = new Host(2, 100.0, 0.03, 150.0); // Médio
        host2.setCap(ResourceType.CPU, 6.0);
        host2.setCap(ResourceType.RAM, 12.0);
        host2.setCap(ResourceType.STORAGE, 80.0);
        
        Host host3 = new Host(3, 80.0, 0.05, 120.0); // Barato, menos confiável
        host3.setCap(ResourceType.CPU, 4.0);
        host3.setCap(ResourceType.RAM, 8.0);
        host3.setCap(ResourceType.STORAGE, 60.0);
        
        instance.addHost(host1);
        instance.addHost(host2);
        instance.addHost(host3);
        
        return instance;
    }
    
    /**
     * Cria uma instância maior para testes de performance
     */
    private static ProblemInstance createLargeInstance() {
        ProblemInstance instance = new ProblemInstance("large-example");
        
        // Criar 8 VMs
        for (int i = 1; i <= 8; i++) {
            double reliability = 0.90 + (i % 3) * 0.03; // 0.90, 0.93, 0.96
            double priority = 1.0 + (i % 4) * 0.5; // 1.0, 1.5, 2.0, 2.5
            
            VM vm = new VM(i, reliability, priority);
            vm.setDemand(ResourceType.CPU, 1.0 + (i % 3));
            vm.setDemand(ResourceType.RAM, 2.0 + (i % 4));
            vm.setDemand(ResourceType.STORAGE, 10.0 + (i % 5) * 5);
            
            instance.addVM(vm);
        }
        
        // Criar 5 hosts
        for (int i = 1; i <= 5; i++) {
            double cost = 80.0 + i * 20.0; // 100, 120, 140, 160, 180
            double failProb = 0.01 + (i % 3) * 0.01; // 0.01, 0.02, 0.03
            double energy = 100.0 + i * 25.0;
            
            Host host = new Host(i, cost, failProb, energy);
            host.setCap(ResourceType.CPU, 4.0 + i);
            host.setCap(ResourceType.RAM, 8.0 + i * 2);
            host.setCap(ResourceType.STORAGE, 50.0 + i * 10);
            
            instance.addHost(host);
        }
        
        return instance;
    }
    
    /**
     * Gera múltiplas soluções usando diferentes estratégias
     */
    private static List<AllocationSolution> generateMultipleSolutions(ProblemInstance instance) {
        List<AllocationSolution> solutions = new ArrayList<>();
        
        // Diferentes estratégias de decodificação
        var strategies = List.of(
            com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.GREEDY_COST,
            com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.BALANCED
        );
        
        for (var strategy : strategies) {
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(20)
                    .brkgaMaxGenerations(30)
                    .decodingStrategy(strategy)
                    .randomSeed(42)
                    .build();
            
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            
            if (result.getBestCostSolution() != null) {
                solutions.add(result.getBestCostSolution());
            }
            if (result.getBestReliabilitySolution() != null && 
                !solutions.contains(result.getBestReliabilitySolution())) {
                solutions.add(result.getBestReliabilitySolution());
            }
        }
        
        return solutions;
    }
    
    /**
     * Analisa utilização de recursos na simulação
     */
    private static void analyzeResourceUtilization(CloudSimSimulator.SimulationResults results) {
        logger.info("\nRESOURCE UTILIZATION ANALYSIS:");
        
        // Análise de VMs
        double avgCpuUtilization = results.vmStatistics.values().stream()
            .mapToDouble(stats -> stats.cpuUtilization)
            .average()
            .orElse(0.0);
        
        double avgRamUtilization = results.vmStatistics.values().stream()
            .mapToDouble(stats -> stats.ramUtilization)
            .average()
            .orElse(0.0);
        
        logger.info("Average VM CPU utilization: {:.1f}%", avgCpuUtilization * 100);
        logger.info("Average VM RAM utilization: {:.1f}%", avgRamUtilization * 100);
        
        // Análise de Hosts
        double avgHostCpuUtilization = results.hostStatistics.values().stream()
            .mapToDouble(stats -> stats.cpuUtilization)
            .average()
            .orElse(0.0);
        
        double totalPowerConsumption = results.hostStatistics.values().stream()
            .mapToDouble(stats -> stats.powerConsumption)
            .sum();
        
        logger.info("Average Host CPU utilization: {:.1f}%", avgHostCpuUtilization * 100);
        logger.info("Total power consumption: {:.2f} W", totalPowerConsumption);
        
        // Análise de Cloudlets
        long completedCloudlets = results.cloudletStatistics.stream()
            .filter(stats -> "FINISHED".equals(stats.status))
            .count();
        
        double avgExecutionTime = results.cloudletStatistics.stream()
            .mapToDouble(stats -> stats.executionTime)
            .average()
            .orElse(0.0);
        
        logger.info("Completed cloudlets: {}/{}", completedCloudlets, results.cloudletStatistics.size());
        logger.info("Average cloudlet execution time: {:.2f} seconds", avgExecutionTime);
    }
} 