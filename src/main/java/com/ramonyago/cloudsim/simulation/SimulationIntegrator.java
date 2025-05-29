package com.ramonyago.cloudsim.simulation;

import com.ramonyago.cloudsim.VMAllocationOptimizer;
import com.ramonyago.cloudsim.OptimizationParameters;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.simulation.CloudSimSimulator.SimulationParameters;
import com.ramonyago.cloudsim.simulation.CloudSimSimulator.SimulationResults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Integra o sistema de otimização com simulação CloudSim.
 * Permite executar simulações reais das soluções otimizadas.
 */
public class SimulationIntegrator {
    private static final Logger logger = LoggerFactory.getLogger(SimulationIntegrator.class);
    
    private final ProblemInstance instance;
    private final OptimizationParameters optimizationParams;
    private final SimulationParameters simulationParams;
    
    public SimulationIntegrator(ProblemInstance instance, 
                               OptimizationParameters optimizationParams,
                               SimulationParameters simulationParams) {
        this.instance = instance;
        this.optimizationParams = optimizationParams;
        this.simulationParams = simulationParams;
        
        logger.info("Simulation Integrator initialized for instance: {}", instance.getInstanceName());
    }
    
    /**
     * Executa otimização seguida de simulação das melhores soluções
     */
    public IntegratedResults runOptimizationAndSimulation() {
        logger.info("Starting integrated optimization and simulation...");
        long startTime = System.currentTimeMillis();
        
        // 1. Executar otimização
        logger.info("Phase 1: Running optimization...");
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, optimizationParams);
        VMAllocationOptimizer.OptimizationResult optimizationResult = optimizer.optimize();
        
        // 2. Selecionar soluções para simulação
        List<AllocationSolution> solutionsToSimulate = selectSolutionsForSimulation(optimizationResult);
        logger.info("Selected {} solutions for simulation", solutionsToSimulate.size());
        
        // 3. Executar simulações
        logger.info("Phase 2: Running CloudSim simulations...");
        Map<AllocationSolution, SimulationResults> simulationResults = new HashMap<>();
        
        for (int i = 0; i < solutionsToSimulate.size(); i++) {
            AllocationSolution solution = solutionsToSimulate.get(i);
            logger.info("Simulating solution {} of {}", i + 1, solutionsToSimulate.size());
            
            try {
                CloudSimSimulator simulator = new CloudSimSimulator(instance, simulationParams);
                SimulationResults simResult = simulator.runSimulation(solution);
                simulationResults.put(solution, simResult);
                
                logger.info("Solution {} simulated - Cost: ${}, Energy: {} kWh", 
                           i + 1, 
                           String.format("%.2f", simResult.totalCost),
                           String.format("%.2f", simResult.totalEnergyConsumption));
                           
            } catch (Exception e) {
                logger.error("Failed to simulate solution {}", i + 1, e);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Integrated execution completed in {} ms", totalTime);
        
        return new IntegratedResults(optimizationResult, simulationResults, totalTime);
    }
    
    /**
     * Seleciona as melhores soluções para simulação
     */
    private List<AllocationSolution> selectSolutionsForSimulation(VMAllocationOptimizer.OptimizationResult result) {
        List<AllocationSolution> selected = new ArrayList<>();
        
        // Sempre incluir as soluções principais
        if (result.getBestCostSolution() != null) {
            selected.add(result.getBestCostSolution());
        }
        
        if (result.getBestReliabilitySolution() != null && 
            !selected.contains(result.getBestReliabilitySolution())) {
            selected.add(result.getBestReliabilitySolution());
        }
        
        if (result.getBalancedSolution() != null && 
            !selected.contains(result.getBalancedSolution())) {
            selected.add(result.getBalancedSolution());
        }
        
        // Adicionar algumas soluções do arquivo Pareto (máximo 5 total)
        List<AllocationSolution> archiveSolutions = result.getArchive().getSolutions();
        int maxAdditional = Math.min(2, Math.max(0, 5 - selected.size()));
        
        for (int i = 0; i < Math.min(maxAdditional, archiveSolutions.size()); i++) {
            AllocationSolution solution = archiveSolutions.get(i);
            if (!selected.contains(solution)) {
                selected.add(solution);
            }
        }
        
        return selected;
    }
    
    /**
     * Executa apenas simulação de uma solução específica
     */
    public SimulationResults simulateSolution(AllocationSolution solution) {
        logger.info("Running simulation for specific solution...");
        
        CloudSimSimulator simulator = new CloudSimSimulator(instance, simulationParams);
        return simulator.runSimulation(solution);
    }
    
    /**
     * Compara múltiplas soluções através de simulação
     */
    public ComparisonResults compareSolutions(List<AllocationSolution> solutions) {
        logger.info("Comparing {} solutions through simulation", solutions.size());
        
        Map<AllocationSolution, SimulationResults> results = new HashMap<>();
        
        for (int i = 0; i < solutions.size(); i++) {
            AllocationSolution solution = solutions.get(i);
            logger.info("Simulating solution {} of {}", i + 1, solutions.size());
            
            try {
                SimulationResults simResult = simulateSolution(solution);
                results.put(solution, simResult);
            } catch (Exception e) {
                logger.error("Failed to simulate solution {}", i + 1, e);
            }
        }
        
        return new ComparisonResults(results);
    }
    
    /**
     * Resultados integrados de otimização e simulação
     */
    public static class IntegratedResults {
        public final VMAllocationOptimizer.OptimizationResult optimizationResult;
        public final Map<AllocationSolution, SimulationResults> simulationResults;
        public final long totalExecutionTime;
        
        public IntegratedResults(VMAllocationOptimizer.OptimizationResult optimizationResult,
                               Map<AllocationSolution, SimulationResults> simulationResults,
                               long totalExecutionTime) {
            this.optimizationResult = optimizationResult;
            this.simulationResults = simulationResults;
            this.totalExecutionTime = totalExecutionTime;
        }
        
        /**
         * Gera relatório completo dos resultados
         */
        public String generateCompleteReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== INTEGRATED OPTIMIZATION & SIMULATION RESULTS ===\n");
            sb.append(String.format("Total Execution Time: %d ms\n", totalExecutionTime));
            sb.append(String.format("Solutions Optimized: %d\n", optimizationResult.getArchive().size()));
            sb.append(String.format("Solutions Simulated: %d\n", simulationResults.size()));
            
            sb.append("\n--- OPTIMIZATION SUMMARY ---\n");
            sb.append(String.format("BRKGA Generations: %d\n", optimizationResult.getReport().getTotalExecutionTime()));
            sb.append(String.format("Archive Size: %d\n", optimizationResult.getArchive().size()));
            
            if (optimizationResult.getBestCostSolution() != null) {
                AllocationSolution bestCost = optimizationResult.getBestCostSolution();
                sb.append(String.format("Best Cost Solution: %.2f (reliability: %.3f)\n", 
                    bestCost.getTotalCost(), bestCost.getTotalReliability()));
            }
            
            if (optimizationResult.getBestReliabilitySolution() != null) {
                AllocationSolution bestRel = optimizationResult.getBestReliabilitySolution();
                sb.append(String.format("Best Reliability Solution: %.3f (cost: %.2f)\n", 
                    bestRel.getTotalReliability(), bestRel.getTotalCost()));
            }
            
            sb.append("\n--- SIMULATION RESULTS ---\n");
            int solutionIndex = 1;
            for (Map.Entry<AllocationSolution, SimulationResults> entry : simulationResults.entrySet()) {
                AllocationSolution solution = entry.getKey();
                SimulationResults simResult = entry.getValue();
                
                sb.append(String.format("\nSolution %d:\n", solutionIndex++));
                sb.append(String.format("  Optimization - Cost: %.2f, Reliability: %.3f\n",
                    solution.getTotalCost(), solution.getTotalReliability()));
                sb.append(String.format("  Simulation - Cost: $%.2f, Energy: %.2f kWh, Time: %.2fs\n",
                    simResult.totalCost, simResult.totalEnergyConsumption, simResult.totalExecutionTime));
                sb.append(String.format("  VMs: %d, Hosts: %d, Cloudlets: %d\n",
                    simResult.vmStatistics.size(), simResult.hostStatistics.size(), simResult.cloudletStatistics.size()));
            }
            
            return sb.toString();
        }
        
        /**
         * Encontra a melhor solução baseada nos resultados de simulação
         */
        public AllocationSolution getBestSimulatedSolution() {
            if (simulationResults.isEmpty()) {
                return null;
            }
            
            // Critério: menor custo de simulação
            return simulationResults.entrySet().stream()
                .min((e1, e2) -> Double.compare(e1.getValue().totalCost, e2.getValue().totalCost))
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        /**
         * Calcula diferença entre otimização e simulação
         */
        public Map<AllocationSolution, CostDifference> getCostDifferences() {
            Map<AllocationSolution, CostDifference> differences = new HashMap<>();
            
            for (Map.Entry<AllocationSolution, SimulationResults> entry : simulationResults.entrySet()) {
                AllocationSolution solution = entry.getKey();
                SimulationResults simResult = entry.getValue();
                
                double optimizedCost = solution.getTotalCost();
                double simulatedCost = simResult.totalCost;
                double difference = simulatedCost - optimizedCost;
                double percentageDiff = optimizedCost > 0 ? (difference / optimizedCost) * 100 : 0;
                
                differences.put(solution, new CostDifference(optimizedCost, simulatedCost, difference, percentageDiff));
            }
            
            return differences;
        }
    }
    
    /**
     * Resultados de comparação entre soluções
     */
    public static class ComparisonResults {
        public final Map<AllocationSolution, SimulationResults> results;
        
        public ComparisonResults(Map<AllocationSolution, SimulationResults> results) {
            this.results = results;
        }
        
        /**
         * Gera relatório de comparação
         */
        public String generateComparisonReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== SOLUTION COMPARISON RESULTS ===\n");
            sb.append(String.format("Solutions Compared: %d\n", results.size()));
            
            // Ordenar por custo de simulação
            List<Map.Entry<AllocationSolution, SimulationResults>> sortedResults = 
                results.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e1.getValue().totalCost, e2.getValue().totalCost))
                    .toList();
            
            sb.append("\n--- RANKING BY SIMULATION COST ---\n");
            for (int i = 0; i < sortedResults.size(); i++) {
                Map.Entry<AllocationSolution, SimulationResults> entry = sortedResults.get(i);
                AllocationSolution solution = entry.getKey();
                SimulationResults simResult = entry.getValue();
                
                sb.append(String.format("%d. Cost: $%.2f, Energy: %.2f kWh, Reliability: %.3f\n",
                    i + 1, simResult.totalCost, simResult.totalEnergyConsumption, solution.getTotalReliability()));
                sb.append(String.format("   Hosts: %d, VMs: %d, Execution Time: %.2fs\n",
                    simResult.hostStatistics.size(), simResult.vmStatistics.size(), simResult.totalExecutionTime));
            }
            
            return sb.toString();
        }
        
        public AllocationSolution getBestByCost() {
            return results.entrySet().stream()
                .min((e1, e2) -> Double.compare(e1.getValue().totalCost, e2.getValue().totalCost))
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public AllocationSolution getBestByEnergy() {
            return results.entrySet().stream()
                .min((e1, e2) -> Double.compare(e1.getValue().totalEnergyConsumption, e2.getValue().totalEnergyConsumption))
                .map(Map.Entry::getKey)
                .orElse(null);
        }
    }
    
    /**
     * Diferença entre custos otimizado e simulado
     */
    public static class CostDifference {
        public final double optimizedCost;
        public final double simulatedCost;
        public final double absoluteDifference;
        public final double percentageDifference;
        
        public CostDifference(double optimizedCost, double simulatedCost, 
                             double absoluteDifference, double percentageDifference) {
            this.optimizedCost = optimizedCost;
            this.simulatedCost = simulatedCost;
            this.absoluteDifference = absoluteDifference;
            this.percentageDifference = percentageDifference;
        }
        
        @Override
        public String toString() {
            return String.format("Optimized: %.2f, Simulated: %.2f, Diff: %.2f (%.1f%%)",
                optimizedCost, simulatedCost, absoluteDifference, percentageDifference);
        }
    }
    
    /**
     * Cria integrador com parâmetros padrão
     */
    public static SimulationIntegrator createDefault(ProblemInstance instance) {
        OptimizationParameters optParams = OptimizationParameters.createDefault();
        SimulationParameters simParams = SimulationParameters.createDefault();
        return new SimulationIntegrator(instance, optParams, simParams);
    }
    
    /**
     * Cria integrador para execução rápida
     */
    public static SimulationIntegrator createQuick(ProblemInstance instance) {
        OptimizationParameters optParams = OptimizationParameters.createQuick();
        SimulationParameters simParams = SimulationParameters.createDefault();
        simParams.setCloudletsPerVm(3); // Menos cloudlets para execução mais rápida
        simParams.setCloudletLength(5000); // Cloudlets menores
        return new SimulationIntegrator(instance, optParams, simParams);
    }
} 