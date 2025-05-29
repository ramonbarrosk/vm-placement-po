package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.brkga.*;
import com.ramonyago.cloudsim.algorithm.TabuSearch;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.util.ParetoArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Classe principal que orquestra o fluxo híbrido para otimização 
 * da alocação de VMs: BRKGA → Busca Tabu
 */
public class VMAllocationOptimizer {
    private static final Logger logger = LoggerFactory.getLogger(VMAllocationOptimizer.class);
    
    private final ProblemInstance instance;
    private final OptimizationParameters parameters;
    
    // Componentes dos algoritmos
    private MOBRKGA brkga;
    private TabuSearch tabuSearch;
    private ParetoArchive finalArchive;
    
    // Estatísticas de execução
    private long totalExecutionTime;
    private ExecutionReport report;
    
    public VMAllocationOptimizer(ProblemInstance instance, OptimizationParameters parameters) {
        this.instance = instance;
        this.parameters = parameters;
        this.finalArchive = new ParetoArchive(parameters.getArchiveSize());
        
        logger.info("VM Allocation Optimizer initialized for instance: {}", instance.getInstanceName());
        logger.info("Instance statistics: {}", instance.getStatistics());
    }
    
    /**
     * Executa o fluxo completo de otimização híbrida
     */
    public OptimizationResult optimize() {
        logger.info("Starting hybrid optimization process...");
        long startTime = System.currentTimeMillis();
        
        try {
            // Fase 1: BRKGA Multi-objetivo
            logger.info("=== Phase 1: Multi-objective BRKGA ===");
            ParetoArchive brkgaArchive = runBRKGA();
            
            // Fase 2: Busca Tabu
            logger.info("=== Phase 2: Multi-objective Tabu Search ===");
            ParetoArchive tabuArchive = runTabuSearch(brkgaArchive);
            
            // Combinação final
            finalArchive = combineParetoFronts(brkgaArchive, tabuArchive);
            
            totalExecutionTime = System.currentTimeMillis() - startTime;
            
            // Gera relatório
            report = generateExecutionReport();
            
            logger.info("Hybrid optimization completed in {} ms", totalExecutionTime);
            logger.info("Final archive statistics: {}", finalArchive.getStatistics());
            
            return new OptimizationResult(finalArchive, report);
            
        } catch (Exception e) {
            logger.error("Error during optimization", e);
            throw new RuntimeException("Optimization failed", e);
        }
    }
    
    /**
     * Executa o algoritmo BRKGA multi-objetivo
     */
    private ParetoArchive runBRKGA() {
        BRKGAParameters brkgaParams = new BRKGAParameters.Builder()
                .populationSize(parameters.getBrkgaPopulationSize())
                .maxGenerations(parameters.getBrkgaMaxGenerations())
                .eliteRatio(parameters.getBrkgaEliteRatio())
                .mutantRatio(parameters.getBrkgaMutantRatio())
                .inheritanceProbability(parameters.getBrkgaInheritanceProbability())
                .archiveSize(parameters.getArchiveSize())
                .randomSeed(parameters.getRandomSeed())
                .build();
        
        brkga = new MOBRKGA(instance, brkgaParams, parameters.getDecodingStrategy());
        ParetoArchive archive = brkga.run();
        
        logger.info("BRKGA completed. Archive size: {}", archive.size());
        return archive;
    }
    
    /**
     * Executa busca tabu multi-objetivo
     */
    private ParetoArchive runTabuSearch(ParetoArchive initialSolutions) {
        TabuSearch.TabuParameters tabuParams = new TabuSearch.TabuParameters(
                parameters.getTabuMaxIterations(),
                parameters.getTabuListSize(),
                50, // diversification frequency
                parameters.getArchiveSize(),
                0.4, // cost weight
                0.6, // reliability weight
                parameters.getRandomSeed()
        );
        
        tabuSearch = new TabuSearch(instance, tabuParams);
        ParetoArchive tabuArchive = tabuSearch.run(initialSolutions);
        
        logger.info("Tabu Search completed. Archive size: {}, Improvements: {}", 
                   tabuArchive.size(), tabuSearch.getImprovementCount());
        return tabuArchive;
    }
    
    /**
     * Combina múltiplas fronteiras de Pareto em uma única
     */
    private ParetoArchive combineParetoFronts(ParetoArchive... archives) {
        ParetoArchive combined = new ParetoArchive(parameters.getArchiveSize());
        
        for (ParetoArchive archive : archives) {
            for (AllocationSolution solution : archive.getSolutions()) {
                combined.add(solution);
            }
        }
        
        return combined;
    }
    
    /**
     * Gera relatório de execução
     */
    private ExecutionReport generateExecutionReport() {
        ExecutionReport.Builder reportBuilder = new ExecutionReport.Builder()
                .instance(instance)
                .totalExecutionTime(totalExecutionTime)
                .finalArchiveSize(finalArchive.size())
                .archiveStatistics(finalArchive.getStatistics());
        
        if (brkga != null) {
            reportBuilder.brkgaExecutionTime(brkga.getExecutionTime())
                        .brkgaGenerations(brkga.getCurrentGeneration())
                        .brkgaArchiveSizeHistory(brkga.getArchiveSizeHistory());
        }
        
        if (tabuSearch != null) {
            reportBuilder.tabuExecutionTime(tabuSearch.getExecutionTime())
                        .tabuIterations(tabuSearch.getIterations());
        }
        
        return reportBuilder.build();
    }
    
    // Getters para acesso aos resultados
    public ParetoArchive getFinalArchive() {
        return finalArchive;
    }
    
    public ExecutionReport getReport() {
        return report;
    }
    
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    /**
     * Método utilitário para criar otimizador a partir de arquivo de instância
     */
    public static VMAllocationOptimizer fromFile(String instanceFilePath, 
                                                OptimizationParameters parameters) throws IOException {
        InstanceReader reader = new InstanceReader();
        ProblemInstance instance = reader.readFromFile(instanceFilePath);
        
        // Valida a instância
        List<String> validationErrors = instance.validate();
        if (!validationErrors.isEmpty()) {
            logger.warn("Instance validation warnings: {}", validationErrors);
        }
        
        return new VMAllocationOptimizer(instance, parameters);
    }
    
    /**
     * Método utilitário para criar otimizador com instância de exemplo
     */
    public static VMAllocationOptimizer withSampleInstance(OptimizationParameters parameters) {
        ProblemInstance instance = InstanceReader.createSampleInstance();
        return new VMAllocationOptimizer(instance, parameters);
    }
    
    /**
     * Classe para encapsular os resultados da otimização
     */
    public static class OptimizationResult {
        private final ParetoArchive archive;
        private final ExecutionReport report;
        
        public OptimizationResult(ParetoArchive archive, ExecutionReport report) {
            this.archive = archive;
            this.report = report;
        }
        
        public ParetoArchive getArchive() {
            return archive;
        }
        
        public ExecutionReport getReport() {
            return report;
        }
        
        public AllocationSolution getBestCostSolution() {
            return archive.getBestCost();
        }
        
        public AllocationSolution getBestReliabilitySolution() {
            return archive.getBestReliability();
        }
        
        public AllocationSolution getBalancedSolution() {
            return archive.getBalancedSolution(0.5, 0.5);
        }
        
        public List<AllocationSolution> getAllSolutions() {
            return archive.getSolutions();
        }
        
        @Override
        public String toString() {
            return String.format("OptimizationResult{archiveSize=%d, executionTime=%d ms}", 
                               archive.size(), report.getTotalExecutionTime());
        }
    }
} 