package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import com.ramonyago.cloudsim.model.Host;
import com.ramonyago.cloudsim.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Main app - quick demo of VM allocation system
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        log.info("=== VM Allocation Optimization System ===");
        log.info("Starting demo...");
        
        try {
            // quick example with sample data
            runQuickDemo();
            
            // file input if provided
            if (args.length > 0) {
                runFromFile(args[0]);
            }
            
            // compare different strategies
            compareStrats();
            
        } catch (Exception e) {
            log.error("Error during execution", e);
            System.exit(1);
        }
        
        log.info("Demo completed!");
    }
    
    // quick demo with default instance
    private static void runQuickDemo() {
        log.info("\n" + "=".repeat(60));
        log.info("QUICK DEMO - Sample Instance");
        log.info("=".repeat(60));
        
        // quick params
        OptimizationParameters params = OptimizationParameters.createQuick();
        log.info("Params: {}", params);
        
        // create optimizer with sample data
        VMAllocationOptimizer opt = VMAllocationOptimizer.withSampleInstance(params);
        
        // run optimization
        VMAllocationOptimizer.OptimizationResult result = opt.optimize();
        
        // show results
        showResults(result);
    }
    
    // run with input file
    private static void runFromFile(String filePath) {
        log.info("\n" + "=".repeat(60));
        log.info("FILE INPUT - {}", filePath);
        log.info("=".repeat(60));
        
        try {
            // default params
            OptimizationParameters params = OptimizationParameters.createDefault();
            
            // create optimizer from file
            VMAllocationOptimizer opt = VMAllocationOptimizer.fromFile(filePath, params);
            
            // run optimization
            VMAllocationOptimizer.OptimizationResult result = opt.optimize();
            
            // show results
            showResults(result);
            
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
        }
    }
    
    // compare different decoding strategies
    private static void compareStrats() {
        log.info("\n" + "=".repeat(60));
        log.info("STRATEGY COMPARISON");
        log.info("=".repeat(60));
        
        ProblemInstance inst = InstanceReader.createSampleInstance();
        
        BRKGADecoder.DecodingStrategy[] strats = {
            BRKGADecoder.DecodingStrategy.GREEDY_COST,
            BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            BRKGADecoder.DecodingStrategy.BALANCED,
            BRKGADecoder.DecodingStrategy.FIRST_FIT
        };
        
        for (BRKGADecoder.DecodingStrategy strat : strats) {
            log.info("\nTesting strategy: {}", strat);
            
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(50)
                    .brkgaMaxGenerations(100)
                    .archiveSize(50)
                    .decodingStrategy(strat)
                    .randomSeed(42) // fixed seed for fair comparison
                    .build();
            
            VMAllocationOptimizer opt = new VMAllocationOptimizer(inst, params);
            VMAllocationOptimizer.OptimizationResult result = opt.optimize();
            
            log.info("  Archive size: {}", result.getArchive().size());
            log.info("  Exec time: {} ms", result.getReport().getTotalExecutionTime());
            
            AllocationSolution bestCost = result.getBestCostSolution();
            AllocationSolution bestRel = result.getBestReliabilitySolution();
            
            if (bestCost != null) {
                log.info("  Best cost: {} (rel: {})", 
                          String.format("%.2f", bestCost.getTotalCost()), 
                          String.format("%.3f", bestRel.getTotalReliability()));
            }
            
            if (bestRel != null) {
                log.info("  Best rel: {} (cost: {})", 
                          String.format("%.3f", bestRel.getTotalReliability()), 
                          String.format("%.2f", bestRel.getTotalCost()));
            }
        }
    }
    
    // show optimization results
    private static void showResults(VMAllocationOptimizer.OptimizationResult result) {
        log.info("\nOPTIMIZATION RESULTS:");
        log.info("-".repeat(40));
        
        // general stats
        log.info("Archive size: {}", result.getArchive().size());
        log.info("Exec time: {} ms", result.getReport().getTotalExecutionTime());
        
        // best solutions
        AllocationSolution bestCost = result.getBestCostSolution();
        AllocationSolution bestRel = result.getBestReliabilitySolution();
        AllocationSolution balanced = result.getBalancedSolution();
        
        if (bestCost != null) {
            log.info("\nBest Cost Solution:");
            log.info("  Cost: {}", String.format("%.2f", bestCost.getTotalCost()));
            log.info("  Reliability: {}", String.format("%.3f", bestCost.getTotalReliability()));
            log.info("  Energy: {}", String.format("%.2f", bestCost.getEnergyConsumption()));
            log.info("  Feasible: {}", bestCost.isFeasible());
            log.info("  Active hosts: {}", bestCost.getActiveHosts().size());
        }
        
        if (bestRel != null) {
            log.info("\nBest Reliability Solution:");
            log.info("  Reliability: {}", String.format("%.3f", bestRel.getTotalReliability()));
            log.info("  Cost: {}", String.format("%.2f", bestRel.getTotalCost()));
            log.info("  Energy: {}", String.format("%.2f", bestRel.getEnergyConsumption()));
            log.info("  Feasible: {}", bestRel.isFeasible());
            log.info("  Active hosts: {}", bestRel.getActiveHosts().size());
        }
        
        if (balanced != null) {
            log.info("\nBalanced Solution:");
            log.info("  Cost: {}", String.format("%.2f", balanced.getTotalCost()));
            log.info("  Reliability: {}", String.format("%.3f", balanced.getTotalReliability()));
            log.info("  Energy: {}", String.format("%.2f", balanced.getEnergyConsumption()));
            log.info("  Feasible: {}", balanced.isFeasible());
            log.info("  Active hosts: {}", balanced.getActiveHosts().size());
        }
        
        // detailed report
        log.info("\nDETAILED REPORT:");
        log.info("-".repeat(40));
        System.out.println(result.getReport().generateTextReport());
        
        // analyze allocations (only for balanced solution)
        if (balanced != null) {
            analyzeAlloc(balanced);
        }
    }
    
    // analyze specific allocation solution
    private static void analyzeAlloc(AllocationSolution sol) {
        log.info("\nALLOCATION ANALYSIS:");
        log.info("-".repeat(40));
        
        Map<Host, List<VM>> hostToVms = new HashMap<>();
        for (Map.Entry<VM, Host> entry : sol.getVmToHost().entrySet()) {
            Host h = entry.getValue();
            VM v = entry.getKey();
            hostToVms.computeIfAbsent(h, k -> new ArrayList<>()).add(v);
        }
        
        for (Map.Entry<Host, List<VM>> entry : hostToVms.entrySet()) {
            Host h = entry.getKey();
            List<VM> vms = entry.getValue();
            
            log.info("Host {} (cost={}, failProb={}):", 
                    h.getHostId(), 
                    String.format("%.2f", h.getCost()),
                    String.format("%.3f", h.getFailProb()));
            
            for (VM vm : vms) {
                log.info("  VM {} (minRel={}, prio={})", 
                        vm.getVmId(),
                        String.format("%.3f", vm.getMinRel()),
                        String.format("%.1f", vm.getPrio()));
            }
            
            // calc resource usage
            Map<ResourceType, Double> usage = new HashMap<>();
            for (ResourceType type : ResourceType.values()) {
                double totalUsage = vms.stream()
                    .mapToDouble(vm -> vm.getDemand(type))
                    .sum();
                usage.put(type, totalUsage);
                
                double utilization = totalUsage / h.getCap(type);
                log.info("  {} usage: {}/{} ({:.1f}%)", 
                        type, 
                        String.format("%.1f", totalUsage),
                        String.format("%.1f", h.getCap(type)),
                        utilization * 100);
            }
        }
    }
}
