package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para o sistema de otimização de alocação de VMs.
 */
public class AppTest {
    
    private ProblemInstance testInstance;
    private OptimizationParameters testParameters;
    
    @BeforeEach
    void setUp() {
        // Criar instância de teste
        testInstance = InstanceReader.createSampleInstance();
        
        // Parâmetros para testes rápidos
        testParameters = new OptimizationParameters.Builder()
                .brkgaPopulationSize(20)
                .brkgaMaxGenerations(10)
                .archiveSize(10)
                .randomSeed(42)
                .build();
    }
    
    @Test
    void testInstanceCreation() {
        assertNotNull(testInstance);
        assertTrue(testInstance.getVMCount() > 0);
        assertTrue(testInstance.getHostCount() > 0);
        
        // Verifica se a instância é válida
        assertTrue(testInstance.validate().isEmpty(), 
                  "Test instance should be valid");
    }
    
    @Test
    void testOptimizationExecution() {
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(testInstance, testParameters);
        
        // Executa otimização
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        
        assertNotNull(result);
        assertNotNull(result.getArchive());
        assertNotNull(result.getReport());
        assertTrue(result.getArchive().size() > 0, "Archive should contain solutions");
    }
    
    @Test
    void testMultipleStrategies() {
        BRKGADecoder.DecodingStrategy[] strategies = {
            BRKGADecoder.DecodingStrategy.GREEDY_COST,
            BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            BRKGADecoder.DecodingStrategy.BALANCED,
            BRKGADecoder.DecodingStrategy.FIRST_FIT
        };
        
        for (BRKGADecoder.DecodingStrategy strategy : strategies) {
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(10)
                    .brkgaMaxGenerations(5)
                    .archiveSize(5)
                    .decodingStrategy(strategy)
                    .randomSeed(42)
                    .build();
            
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(testInstance, params);
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            
            assertNotNull(result, "Result should not be null for strategy " + strategy);
            assertTrue(result.getArchive().size() > 0, 
                      "Archive should contain solutions for strategy " + strategy);
        }
    }
    
    @Test
    void testSolutionObjectives() {
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(testInstance, testParameters);
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        
        AllocationSolution bestCost = result.getBestCostSolution();
        AllocationSolution bestReliability = result.getBestReliabilitySolution();
        
        if (bestCost != null) {
            assertTrue(bestCost.getTotalCost() >= 0, "Cost should be non-negative");
            assertTrue(bestCost.getTotalReliability() >= 0, "Reliability should be non-negative");
            assertTrue(bestCost.getTotalReliability() <= 1, "Reliability should be at most 1");
        }
        
        if (bestReliability != null) {
            assertTrue(bestReliability.getTotalCost() >= 0, "Cost should be non-negative");
            assertTrue(bestReliability.getTotalReliability() >= 0, "Reliability should be non-negative");
            assertTrue(bestReliability.getTotalReliability() <= 1, "Reliability should be at most 1");
        }
    }
    
    @Test
    void testReportGeneration() {
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(testInstance, testParameters);
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        
        ExecutionReport report = result.getReport();
        assertNotNull(report);
        
        // Testa geração de relatórios
        String textReport = report.generateTextReport();
        assertNotNull(textReport);
        assertTrue(textReport.length() > 0);
        
        String csvReport = report.generateCSVReport();
        assertNotNull(csvReport);
        assertTrue(csvReport.contains(","));
        
        String summary = report.generateSummary();
        assertNotNull(summary);
        assertTrue(summary.length() > 0);
    }
    
    @Test
    void testParameterValidation() {
        // Testa parâmetros inválidos
        assertThrows(IllegalArgumentException.class, () -> {
            new OptimizationParameters.Builder()
                    .brkgaPopulationSize(-1)
                    .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new OptimizationParameters.Builder()
                    .brkgaEliteRatio(1.5)
                    .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new OptimizationParameters.Builder()
                    .archiveSize(0)
                    .build();
        });
    }
    
    @Test
    void testInstanceFromJSON() {
        // Testa criação de instância usando o método createSampleInstance
        ProblemInstance sampleInstance = InstanceReader.createSampleInstance();
        
        assertNotNull(sampleInstance);
        assertEquals("sample", sampleInstance.getInstanceName());
        
        // Verifica estatísticas básicas
        ProblemInstance.InstanceStatistics stats = sampleInstance.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.vmCount > 0);
        assertTrue(stats.hostCount > 0);
        assertTrue(stats.totalHostCost > 0);
    }
    
    @Test
    void testParetoArchive() {
        VMAllocationOptimizer optimizer = new VMAllocationOptimizer(testInstance, testParameters);
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        
        var archive = result.getArchive();
        var statistics = archive.getStatistics();
        
        assertNotNull(statistics);
        assertEquals(archive.size(), statistics.size);
        
        if (statistics.size > 0) {
            assertTrue(statistics.minCost <= statistics.maxCost);
            assertTrue(statistics.minReliability <= statistics.maxReliability);
        }
    }
}
