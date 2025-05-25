package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Aplicação principal para demonstrar o sistema de otimização de alocação de VMs.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        logger.info("=== VM Allocation Optimization System ===");
        logger.info("Starting demonstration...");
        
        try {
            // Exemplo 1: Execução rápida com instância de exemplo
            runQuickExample();
            
            // Exemplo 2: Execução com arquivo JSON (se existir)
            if (args.length > 0) {
                runWithInputFile(args[0]);
            }
            
            // Exemplo 3: Comparação de estratégias de decodificação
            compareDecodingStrategies();
            
        } catch (Exception e) {
            logger.error("Error during execution", e);
            System.exit(1);
        }
        
        logger.info("Demonstration completed successfully!");
    }
    
    /**
     * Executa um exemplo rápido com instância padrão
     */
    private static void runQuickExample() {
        logger.info("\n" + "=".repeat(60));
        logger.info("QUICK EXAMPLE - Sample Instance");
        logger.info("=".repeat(60));
        
        // Parâmetros para execução rápida
        OptimizationParameters params = OptimizationParameters.createQuick();
        logger.info("Parameters: {}", params);
        
        // Cria otimizador com instância de exemplo
        VMAllocationOptimizer optimizer = VMAllocationOptimizer.withSampleInstance(params);
        
        // Executa otimização
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        
        // Mostra resultados
        displayResults(result);
    }
    
    /**
     * Executa com arquivo de entrada especificado
     */
    private static void runWithInputFile(String filePath) {
        logger.info("\n" + "=".repeat(60));
        logger.info("FILE INPUT EXAMPLE - {}", filePath);
        logger.info("=".repeat(60));
        
        try {
            // Parâmetros padrão
            OptimizationParameters params = OptimizationParameters.createDefault();
            
            // Cria otimizador a partir do arquivo
            VMAllocationOptimizer optimizer = VMAllocationOptimizer.fromFile(filePath, params);
            
            // Executa otimização
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            
            // Mostra resultados
            displayResults(result);
            
        } catch (IOException e) {
            logger.error("Failed to read input file: {}", filePath, e);
        }
    }
    
    /**
     * Compara diferentes estratégias de decodificação
     */
    private static void compareDecodingStrategies() {
        logger.info("\n" + "=".repeat(60));
        logger.info("STRATEGY COMPARISON");
        logger.info("=".repeat(60));
        
        ProblemInstance instance = InstanceReader.createSampleInstance();
        
        BRKGADecoder.DecodingStrategy[] strategies = {
            BRKGADecoder.DecodingStrategy.GREEDY_COST,
            BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            BRKGADecoder.DecodingStrategy.BALANCED,
            BRKGADecoder.DecodingStrategy.FIRST_FIT
        };
        
        for (BRKGADecoder.DecodingStrategy strategy : strategies) {
            logger.info("\nTesting strategy: {}", strategy);
            
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(50)
                    .brkgaMaxGenerations(100)
                    .archiveSize(50)
                    .decodingStrategy(strategy)
                    .randomSeed(42) // Seed fixo para comparação justa
                    .build();
            
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            
            logger.info("  Archive size: {}", result.getArchive().size());
            logger.info("  Execution time: {} ms", result.getReport().getTotalExecutionTime());
            
            AllocationSolution bestCost = result.getBestCostSolution();
            AllocationSolution bestReliability = result.getBestReliabilitySolution();
            
            if (bestCost != null) {
                logger.info("  Best cost: {:.2f} (reliability: {:.3f})", 
                          bestCost.getTotalCost(), bestCost.getTotalReliability());
            }
            
            if (bestReliability != null) {
                logger.info("  Best reliability: {:.3f} (cost: {:.2f})", 
                          bestReliability.getTotalReliability(), bestReliability.getTotalCost());
            }
        }
    }
    
    /**
     * Exibe os resultados da otimização
     */
    private static void displayResults(VMAllocationOptimizer.OptimizationResult result) {
        logger.info("\nOPTIMIZATION RESULTS:");
        logger.info("-".repeat(40));
        
        // Estatísticas gerais
        logger.info("Archive size: {}", result.getArchive().size());
        logger.info("Execution time: {} ms", result.getReport().getTotalExecutionTime());
        
        // Melhores soluções
        AllocationSolution bestCost = result.getBestCostSolution();
        AllocationSolution bestReliability = result.getBestReliabilitySolution();
        AllocationSolution balanced = result.getBalancedSolution();
        
        if (bestCost != null) {
            logger.info("\nBest Cost Solution:");
            logger.info("  Cost: {:.2f}", bestCost.getTotalCost());
            logger.info("  Reliability: {:.3f}", bestCost.getTotalReliability());
            logger.info("  Energy: {:.2f}", bestCost.getEnergyConsumption());
            logger.info("  Feasible: {}", bestCost.isFeasible());
            logger.info("  Active hosts: {}", bestCost.getActiveHosts().size());
        }
        
        if (bestReliability != null) {
            logger.info("\nBest Reliability Solution:");
            logger.info("  Reliability: {:.3f}", bestReliability.getTotalReliability());
            logger.info("  Cost: {:.2f}", bestReliability.getTotalCost());
            logger.info("  Energy: {:.2f}", bestReliability.getEnergyConsumption());
            logger.info("  Feasible: {}", bestReliability.isFeasible());
            logger.info("  Active hosts: {}", bestReliability.getActiveHosts().size());
        }
        
        if (balanced != null) {
            logger.info("\nBalanced Solution:");
            logger.info("  Cost: {:.2f}", balanced.getTotalCost());
            logger.info("  Reliability: {:.3f}", balanced.getTotalReliability());
            logger.info("  Energy: {:.2f}", balanced.getEnergyConsumption());
            logger.info("  Feasible: {}", balanced.isFeasible());
            logger.info("  Active hosts: {}", balanced.getActiveHosts().size());
        }
        
        // Relatório detalhado
        logger.info("\nDETAILED REPORT:");
        logger.info("-".repeat(40));
        System.out.println(result.getReport().generateTextReport());
        
        // Análise de alocações (apenas para a melhor solução balanceada)
        if (balanced != null) {
            analyzeAllocation(balanced);
        }
    }
    
    /**
     * Analisa uma solução de alocação específica
     */
    private static void analyzeAllocation(AllocationSolution solution) {
        logger.info("\nALLOCATION ANALYSIS:");
        logger.info("-".repeat(40));
        
        solution.getActiveHosts().forEach(host -> {
            logger.info("Host {} (cost: {:.2f}, fail_prob: {:.3f}):", 
                      host.getId(), host.getActivationCost(), host.getFailureProbability());
            
            solution.getVmsOnHost(host).forEach(vm -> {
                logger.info("  VM {} (reliability_req: {:.3f}, priority: {:.1f})", 
                          vm.getId(), vm.getMinReliability(), vm.getPriority());
            });
        });
        
        // VMs não alocadas (percorre todas as VMs da instância)
        boolean hasUnallocated = false;
        // Aqui assumimos que temos acesso às VMs da instância através da solução
        // Para simplificar, vamos apenas verificar VMs que estão no mapeamento mas não têm host
        for (VM vm : solution.getVmToHost().keySet()) {
            if (solution.getHostForVM(vm) == null) {
                if (!hasUnallocated) {
                    logger.info("\nUnallocated VMs:");
                    hasUnallocated = true;
                }
                logger.info("  VM {} (reliability_req: {:.3f})", vm.getId(), vm.getMinReliability());
            }
        }
        
        if (!hasUnallocated) {
            logger.info("\nAll VMs successfully allocated!");
        }
    }
}
