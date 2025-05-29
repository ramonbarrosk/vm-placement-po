package com.ramonyago.cloudsim.examples;

import com.ramonyago.cloudsim.OptimizationParameters;
import com.ramonyago.cloudsim.VMAllocationOptimizer;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.simulation.CloudSimSimulator;
import com.ramonyago.cloudsim.simulation.SimulationIntegrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive test using real instance files from examples folder.
 * Demonstrates CloudSim integration with small, medium, and large instances.
 */
public class RealInstanceCloudSimTest {
    private static final Logger logger = LoggerFactory.getLogger(RealInstanceCloudSimTest.class);
    
    public static void main(String[] args) {
        logger.info("=== Real Instance CloudSim Integration Test ===");
        
        try {
            // Test all three instance sizes
            testSmallInstance();
            testSampleInstance();
            testLargeInstance();
            
            // Comparative analysis
            runComparativeAnalysis();
            
        } catch (Exception e) {
            logger.error("Test execution failed", e);
        }
    }
    
    /**
     * Test with small instance (3 VMs, 2 hosts)
     */
    private static void testSmallInstance() {
        logger.info("\n" + "=".repeat(60));
        logger.info("SMALL INSTANCE TEST (3 VMs, 2 hosts)");
        logger.info("=".repeat(60));
        
        try {
            // Load instance from file
            InstanceReader reader = new InstanceReader();
            ProblemInstance instance = reader.readFromFile("examples/small_instance.json");
            logger.info("Loaded small instance: {} VMs, {} hosts", 
                       instance.getVMCount(), instance.getHostCount());
            
            // Run integrated optimization and simulation
            SimulationIntegrator integrator = createOptimizedIntegrator(instance, "small");
            SimulationIntegrator.IntegratedResults results = integrator.runOptimizationAndSimulation();
            
            // Display results
            displayResults("SMALL INSTANCE", results);
            
        } catch (IOException e) {
            logger.error("Failed to load small instance", e);
        }
    }
    
    /**
     * Test with sample instance (5 VMs, 4 hosts)
     */
    private static void testSampleInstance() {
        logger.info("\n" + "=".repeat(60));
        logger.info("SAMPLE INSTANCE TEST (5 VMs, 4 hosts)");
        logger.info("=".repeat(60));
        
        try {
            // Load instance from file
            InstanceReader reader = new InstanceReader();
            ProblemInstance instance = reader.readFromFile("examples/sample_instance.json");
            logger.info("Loaded sample instance: {} VMs, {} hosts", 
                       instance.getVMCount(), instance.getHostCount());
            
            // Run integrated optimization and simulation
            SimulationIntegrator integrator = createOptimizedIntegrator(instance, "medium");
            SimulationIntegrator.IntegratedResults results = integrator.runOptimizationAndSimulation();
            
            // Display results
            displayResults("SAMPLE INSTANCE", results);
            
        } catch (IOException e) {
            logger.error("Failed to load sample instance", e);
        }
    }
    
    /**
     * Test with large instance (8 VMs, 6 hosts)
     */
    private static void testLargeInstance() {
        logger.info("\n" + "=".repeat(60));
        logger.info("LARGE INSTANCE TEST (8 VMs, 6 hosts)");
        logger.info("=".repeat(60));
        
        try {
            // Load instance from file
            InstanceReader reader = new InstanceReader();
            ProblemInstance instance = reader.readFromFile("examples/large_instance.json");
            logger.info("Loaded large instance: {} VMs, {} hosts", 
                       instance.getVMCount(), instance.getHostCount());
            
            // Run integrated optimization and simulation
            SimulationIntegrator integrator = createOptimizedIntegrator(instance, "large");
            SimulationIntegrator.IntegratedResults results = integrator.runOptimizationAndSimulation();
            
            // Display results
            displayResults("LARGE INSTANCE", results);
            
        } catch (IOException e) {
            logger.error("Failed to load large instance", e);
        }
    }
    
    /**
     * Run comparative analysis across all instances
     */
    private static void runComparativeAnalysis() {
        logger.info("\n" + "=".repeat(60));
        logger.info("COMPARATIVE ANALYSIS");
        logger.info("=".repeat(60));
        
        List<InstanceResult> results = new ArrayList<>();
        
        try {
            // Test each instance with multiple strategies
            results.add(analyzeInstance("examples/small_instance.json", "Small"));
            results.add(analyzeInstance("examples/sample_instance.json", "Sample"));
            results.add(analyzeInstance("examples/large_instance.json", "Large"));
            
            // Generate comparative report
            generateComparativeReport(results);
            
        } catch (Exception e) {
            logger.error("Comparative analysis failed", e);
        }
    }
    
    /**
     * Analyze a single instance with multiple optimization strategies
     */
    private static InstanceResult analyzeInstance(String filePath, String name) throws IOException {
        InstanceReader reader = new InstanceReader();
        ProblemInstance instance = reader.readFromFile(filePath);
        
        // Test different strategies
        var strategies = List.of(
            com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.GREEDY_COST,
            com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder.DecodingStrategy.BALANCED
        );
        
        List<StrategyResult> strategyResults = new ArrayList<>();
        
        for (var strategy : strategies) {
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(30)
                    .brkgaMaxGenerations(50)
                    .archiveSize(30)
                    .decodingStrategy(strategy)
                    .randomSeed(42)
                    .build();
            
            // Run optimization
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);
            VMAllocationOptimizer.OptimizationResult optResult = optimizer.optimize();
            
            // Run simulation on best solution
            AllocationSolution bestSolution = optResult.getBestCostSolution();
            if (bestSolution != null) {
                CloudSimSimulator.SimulationParameters simParams = 
                    CloudSimSimulator.SimulationParameters.createDefault();
                simParams.setCloudletsPerVm(3);
                simParams.setCloudletLength(5000);
                
                CloudSimSimulator simulator = new CloudSimSimulator(instance, simParams);
                CloudSimSimulator.SimulationResults simResult = simulator.runSimulation(bestSolution);
                
                strategyResults.add(new StrategyResult(
                    strategy.name(),
                    bestSolution.getTotalCost(),
                    bestSolution.getTotalReliability(),
                    simResult.totalCost,
                    simResult.totalEnergyConsumption,
                    optResult.getReport().getTotalExecutionTime()
                ));
            }
        }
        
        return new InstanceResult(name, instance.getVMCount(), instance.getHostCount(), strategyResults);
    }
    
    /**
     * Create optimized integrator based on instance size
     */
    private static SimulationIntegrator createOptimizedIntegrator(ProblemInstance instance, String size) {
        OptimizationParameters optParams;
        CloudSimSimulator.SimulationParameters simParams;
        
        switch (size) {
            case "small":
                optParams = new OptimizationParameters.Builder()
                        .brkgaPopulationSize(30)
                        .brkgaMaxGenerations(50)
                        .archiveSize(20)
                        .randomSeed(42)
                        .build();
                simParams = CloudSimSimulator.SimulationParameters.createDefault();
                simParams.setCloudletsPerVm(3);
                simParams.setCloudletLength(5000);
                break;
                
            case "medium":
                optParams = new OptimizationParameters.Builder()
                        .brkgaPopulationSize(50)
                        .brkgaMaxGenerations(100)
                        .archiveSize(30)
                        .randomSeed(42)
                        .build();
                simParams = CloudSimSimulator.SimulationParameters.createDefault();
                simParams.setCloudletsPerVm(4);
                simParams.setCloudletLength(8000);
                break;
                
            case "large":
                optParams = new OptimizationParameters.Builder()
                        .brkgaPopulationSize(80)
                        .brkgaMaxGenerations(150)
                        .archiveSize(50)
                        .randomSeed(42)
                        .build();
                simParams = CloudSimSimulator.SimulationParameters.createDefault();
                simParams.setCloudletsPerVm(5);
                simParams.setCloudletLength(10000);
                break;
                
            default:
                return SimulationIntegrator.createDefault(instance);
        }
        
        return new SimulationIntegrator(instance, optParams, simParams);
    }
    
    /**
     * Display detailed results for an instance test
     */
    private static void displayResults(String instanceName, SimulationIntegrator.IntegratedResults results) {
        logger.info("\n{} RESULTS:", instanceName);
        logger.info("-".repeat(40));
        
        // Optimization summary
        logger.info("Optimization completed in {} ms", results.totalExecutionTime);
        logger.info("Solutions in archive: {}", results.optimizationResult.getArchive().size());
        
        if (results.optimizationResult.getBestCostSolution() != null) {
            AllocationSolution bestCost = results.optimizationResult.getBestCostSolution();
            logger.info("Best cost solution: {} (reliability: {})", 
                       String.format("%.2f", bestCost.getTotalCost()),
                       String.format("%.3f", bestCost.getTotalReliability()));
        }
        
        // Simulation summary
        logger.info("Solutions simulated: {}", results.simulationResults.size());
        
        for (var entry : results.simulationResults.entrySet()) {
            AllocationSolution solution = entry.getKey();
            CloudSimSimulator.SimulationResults simResult = entry.getValue();
            
            logger.info("Simulation result:");
            logger.info("  Optimization cost: {}", String.format("%.2f", solution.getTotalCost()));
            logger.info("  Simulation cost: ${}", String.format("%.2f", simResult.totalCost));
            logger.info("  Energy consumption: {} kWh", String.format("%.2f", simResult.totalEnergyConsumption));
            logger.info("  Execution time: {} seconds", String.format("%.2f", simResult.totalExecutionTime));
            logger.info("  VMs: {}, Hosts: {}, Cloudlets: {}", 
                       simResult.vmStatistics.size(),
                       simResult.hostStatistics.size(),
                       simResult.cloudletStatistics.size());
        }
        
        // Cost differences
        var costDiffs = results.getCostDifferences();
        if (!costDiffs.isEmpty()) {
            logger.info("\nCost Analysis:");
            for (var diff : costDiffs.values()) {
                logger.info("  {}", diff.toString());
            }
        }
    }
    
    /**
     * Generate comparative report across all instances
     */
    private static void generateComparativeReport(List<InstanceResult> results) {
        logger.info("\nCOMPARATIVE ANALYSIS REPORT:");
        logger.info("=".repeat(50));
        
        for (InstanceResult result : results) {
            logger.info("\n{} Instance ({} VMs, {} hosts):", 
                       result.name, result.vmCount, result.hostCount);
            
            for (StrategyResult strategy : result.strategies) {
                logger.info("  {} Strategy:", strategy.strategyName);
                logger.info("    Optimization: cost={}, reliability={}, time={}ms", 
                           String.format("%.2f", strategy.optimizationCost),
                           String.format("%.3f", strategy.optimizationReliability),
                           strategy.executionTime);
                logger.info("    Simulation: cost=${}, energy={}kWh", 
                           String.format("%.2f", strategy.simulationCost),
                           String.format("%.2f", strategy.simulationEnergy));
                
                double costDiff = ((strategy.simulationCost - strategy.optimizationCost) / strategy.optimizationCost) * 100;
                logger.info("    Cost difference: {}%", String.format("%.1f", costDiff));
            }
        }
        
        // Summary statistics
        logger.info("\nSUMMARY STATISTICS:");
        logger.info("-".repeat(30));
        
        double avgOptTime = results.stream()
            .flatMap(r -> r.strategies.stream())
            .mapToLong(s -> s.executionTime)
            .average()
            .orElse(0.0);
        
        double avgSimCost = results.stream()
            .flatMap(r -> r.strategies.stream())
            .mapToDouble(s -> s.simulationCost)
            .average()
            .orElse(0.0);
        
        double avgEnergy = results.stream()
            .flatMap(r -> r.strategies.stream())
            .mapToDouble(s -> s.simulationEnergy)
            .average()
            .orElse(0.0);
        
        logger.info("Average optimization time: {} ms", String.format("%.1f", avgOptTime));
        logger.info("Average simulation cost: ${}", String.format("%.2f", avgSimCost));
        logger.info("Average energy consumption: {} kWh", String.format("%.2f", avgEnergy));
    }
    
    /**
     * Result data structures
     */
    private static class InstanceResult {
        final String name;
        final int vmCount;
        final int hostCount;
        final List<StrategyResult> strategies;
        
        InstanceResult(String name, int vmCount, int hostCount, List<StrategyResult> strategies) {
            this.name = name;
            this.vmCount = vmCount;
            this.hostCount = hostCount;
            this.strategies = strategies;
        }
    }
    
    private static class StrategyResult {
        final String strategyName;
        final double optimizationCost;
        final double optimizationReliability;
        final double simulationCost;
        final double simulationEnergy;
        final long executionTime;
        
        StrategyResult(String strategyName, double optimizationCost, double optimizationReliability,
                      double simulationCost, double simulationEnergy, long executionTime) {
            this.strategyName = strategyName;
            this.optimizationCost = optimizationCost;
            this.optimizationReliability = optimizationReliability;
            this.simulationCost = simulationCost;
            this.simulationEnergy = simulationEnergy;
            this.executionTime = executionTime;
        }
    }
} 