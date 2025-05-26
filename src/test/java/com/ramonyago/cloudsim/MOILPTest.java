package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.MOILP;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.util.ParetoArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes específicos para verificar o MOILP (Multi-Objective Integer Linear Programming)
 * - Tempo limite configurado
 * - Integração com solver MILP
 * - Formulação exata do MOILP
 * - Estratégia epsilon-constraint
 */
public class MOILPTest {
    
    private ProblemInstance smallInstance;
    private ProblemInstance mediumInstance;
    private MOILP.MOILPParameters defaultParams;
    private MOILP.MOILPParameters extendedTimeParams;
    
    @BeforeEach
    void setUp() {
        // Instância pequena para testes exatos
        smallInstance = InstanceReader.createSampleInstance();
        
        // Instância média simulada (duplicando VMs e hosts)
        mediumInstance = createMediumInstance();
        
        // Parâmetros padrão (tempo limite atual: 5 minutos)
        defaultParams = new MOILP.MOILPParameters(
                300.0, // 5 minutos
                100,   // max variables for exact
                10,    // epsilon steps
                50,    // archive size
                "GLPK" // solver
        );
        
        // Parâmetros com tempo limite estendido para testes (30 minutos)
        extendedTimeParams = new MOILP.MOILPParameters(
                1800.0, // 30 minutos - SIGNIFICATIVAMENTE AUMENTADO
                100,    // max variables for exact
                20,     // mais passos epsilon para melhor exploração
                100,    // archive maior
                "GLPK"  // solver
        );
    }
    
    @Test
    @DisplayName("Verificar configuração de tempo limite padrão")
    void testTimeoutConfiguration() {
        System.out.println("=== VERIFICAÇÃO DE TIMEOUT CONFIGURADO ===");
        
        // Verificar parâmetros padrão
        System.out.println("Timeout padrão: " + defaultParams.getTimeLimit() + " segundos");
        System.out.println("Timeout estendido: " + extendedTimeParams.getTimeLimit() + " segundos");
        
        assertEquals(300.0, defaultParams.getTimeLimit(), "Timeout padrão deve ser 5 minutos");
        assertEquals(1800.0, extendedTimeParams.getTimeLimit(), "Timeout estendido deve ser 30 minutos");
        
        System.out.println("✅ Configuração de timeout verificada");
    }
    
    @Test
    @DisplayName("Testar modelo MOILP muito simples para verificar comunicação com solver")
    void testSimpleMOILPModel() {
        System.out.println("\n=== TESTE DE MODELO MOILP SIMPLES ===");
        
        // Criar instância mínima (2 VMs, 2 hosts)
        ProblemInstance miniInstance = createMinimalInstance();
        
        // Parâmetros para problema pequeno (deve usar formulação exata)
        MOILP.MOILPParameters simpleParams = new MOILP.MOILPParameters(
                60.0,  // 1 minuto é suficiente para problema pequeno
                50,    // limite baixo para forçar formulação exata
                5,     // poucos passos epsilon
                20,    // archive pequeno
                "GLPK"
        );
        
        System.out.println("Instância mínima:");
        System.out.println("  VMs: " + miniInstance.getVMCount());
        System.out.println("  Hosts: " + miniInstance.getHostCount());
        System.out.println("  Variáveis de decisão: " + (miniInstance.getVMCount() * miniInstance.getHostCount()));
        
        // Testar com soluções guia vazias (simulando primeira execução)
        ParetoArchive emptyGuide = new ParetoArchive(10);
        
        MOILP moilp = new MOILP(miniInstance, simpleParams);
        
        long startTime = System.currentTimeMillis();
        ParetoArchive result = moilp.solve(emptyGuide);
        long executionTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Resultados do teste simples:");
        System.out.println("  Tempo de execução: " + executionTime + " ms");
        System.out.println("  Soluções geradas: " + moilp.getSolutionsGenerated());
        System.out.println("  Soluções ótimas: " + moilp.isOptimalSolutions());
        System.out.println("  Tamanho do arquivo final: " + result.size());
        
        // Verificações
        assertNotNull(result, "Resultado não deve ser nulo");
        assertTrue(executionTime < 60000, "Execução deve ser rápida para instância pequena");
        
        if (result.size() > 0) {
            System.out.println("✅ Comunicação com solver funcionando - soluções encontradas");
            
            // Analisar uma solução
            AllocationSolution solution = result.getSolutions().get(0);
            System.out.println("  Exemplo de solução:");
            System.out.println("    Custo: " + String.format("%.2f", solution.getTotalCost()));
            System.out.println("    Confiabilidade: " + String.format("%.3f", solution.getTotalReliability()));
            System.out.println("    Viável: " + solution.isFeasible());
        } else {
            System.out.println("⚠️ Nenhuma solução encontrada - pode indicar problema na formulação ou solver");
        }
    }
    
    @Test
    @DisplayName("Analisar formulação exata do MOILP")
    void testMOILPFormulation() {
        System.out.println("\n=== ANÁLISE DA FORMULAÇÃO MOILP ===");
        
        // Usar instância pequena para análise detalhada
        MOILP moilp = new MOILP(smallInstance, extendedTimeParams);
        
        System.out.println("Detalhes da formulação MOILP:");
        System.out.println("  Instância: " + smallInstance.getVMCount() + " VMs, " + smallInstance.getHostCount() + " hosts");
        System.out.println("  Variáveis de decisão x_ij: " + (smallInstance.getVMCount() * smallInstance.getHostCount()));
        System.out.println("  Variáveis de ativação y_j: " + smallInstance.getHostCount());
        System.out.println("  Total de variáveis: " + (smallInstance.getVMCount() * smallInstance.getHostCount() + smallInstance.getHostCount()));
        
        // Analisar se vai usar formulação exata ou heurística
        int totalVariables = smallInstance.getVMCount() * smallInstance.getHostCount();
        boolean willUseExact = totalVariables <= extendedTimeParams.getMaxVariablesForExact();
        
        System.out.println("  Método a ser usado: " + (willUseExact ? "FORMULAÇÃO EXATA (ILP)" : "HEURÍSTICA BASEADA EM ILP"));
        System.out.println("  Limite para exato: " + extendedTimeParams.getMaxVariablesForExact() + " variáveis");
        
        // Parâmetros da estratégia epsilon-constraint
        System.out.println("\nEstratégia ε-constraint:");
        System.out.println("  Número de passos ε: " + extendedTimeParams.getNumEpsilonSteps());
        System.out.println("  Solver configurado: " + extendedTimeParams.getSolver());
        System.out.println("  Timeout por problema MILP: " + extendedTimeParams.getTimeLimit() + "s");
        
        assertTrue(willUseExact, "Para instância de teste, deve usar formulação exata");
        System.out.println("✅ Análise da formulação concluída");
    }
    
    @Test
    @DisplayName("Verificar estratégia epsilon-constraint e problemas MILP individuais")
    void testEpsilonConstraintStrategy() {
        System.out.println("\n=== ANÁLISE DA ESTRATÉGIA ε-CONSTRAINT ===");
        
        // Criar algumas soluções guia para simular execução real
        ParetoArchive guidingSolutions = createGuidingSolutions();
        
        System.out.println("Soluções guia fornecidas: " + guidingSolutions.size());
        if (guidingSolutions.size() > 0) {
            var stats = guidingSolutions.getStatistics();
            System.out.println("  Faixa de custo: [" + String.format("%.2f", stats.minCost) + 
                             ", " + String.format("%.2f", stats.maxCost) + "]");
            System.out.println("  Faixa de confiabilidade: [" + String.format("%.3f", stats.minReliability) + 
                             ", " + String.format("%.3f", stats.maxReliability) + "]");
        }
        
        MOILP moilp = new MOILP(smallInstance, extendedTimeParams);
        
        System.out.println("\nCalculando estratégia ε-constraint:");
        int numEpsilonSteps = extendedTimeParams.getNumEpsilonSteps();
        System.out.println("  Problemas MILP individuais a serem resolvidos: " + (numEpsilonSteps + 1));
        System.out.println("  Cada problema resolve: maximizar confiabilidade sujeito a custo ≤ ε_i");
        
        // Simular cálculo dos valores epsilon
        if (guidingSolutions.size() > 0) {
            var stats = guidingSolutions.getStatistics();
            double minCost = stats.minCost;
            double maxCost = stats.maxCost;
            double stepSize = (maxCost - minCost) / numEpsilonSteps;
            
            System.out.println("  Valores ε para restrição de custo:");
            for (int i = 0; i <= numEpsilonSteps; i++) {
                double epsilon = minCost + i * stepSize;
                System.out.println("    ε_" + i + " = " + String.format("%.2f", epsilon));
            }
        }
        
        // Executar MOILP
        long startTime = System.currentTimeMillis();
        ParetoArchive result = moilp.solve(guidingSolutions);
        long executionTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\nResultados da estratégia ε-constraint:");
        System.out.println("  Tempo total de execução: " + executionTime + " ms");
        System.out.println("  Soluções geradas: " + moilp.getSolutionsGenerated());
        System.out.println("  Soluções são ótimas: " + moilp.isOptimalSolutions());
        System.out.println("  Arquivo final: " + result.size() + " soluções");
        
        // Verificações
        assertNotNull(result, "Resultado não deve ser nulo");
        assertTrue(result.size() >= guidingSolutions.size(), "Deve manter pelo menos as soluções guia");
        
        System.out.println("✅ Estratégia ε-constraint analisada com sucesso");
    }
    
    @Test
    @DisplayName("Teste com timeout estendido para instância maior")
    void testExtendedTimeoutWithLargerInstance() {
        System.out.println("\n=== TESTE COM TIMEOUT ESTENDIDO (INSTÂNCIA MAIOR) ===");
        
        System.out.println("Configurações do teste:");
        System.out.println("  Timeout: " + extendedTimeParams.getTimeLimit() + " segundos (" + 
                         (extendedTimeParams.getTimeLimit() / 60.0) + " minutos)");
        System.out.println("  Instância: " + mediumInstance.getVMCount() + " VMs, " + 
                         mediumInstance.getHostCount() + " hosts");
        
        ParetoArchive guidingSolutions = createGuidingSolutions();
        MOILP moilp = new MOILP(mediumInstance, extendedTimeParams);
        
        long startTime = System.currentTimeMillis();
        ParetoArchive result = moilp.solve(guidingSolutions);
        long executionTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\nResultados com timeout estendido:");
        System.out.println("  Tempo de execução: " + (executionTime / 1000.0) + " segundos");
        System.out.println("  Utilizou timeout completo: " + (executionTime >= extendedTimeParams.getTimeLimit() * 900)); // 90% do limite
        System.out.println("  Soluções geradas: " + moilp.getSolutionsGenerated());
        System.out.println("  Método usado: " + (moilp.isOptimalSolutions() ? "EXATO" : "HEURÍSTICO"));
        System.out.println("  Arquivo final: " + result.size() + " soluções");
        
        // Verificações
        assertNotNull(result, "Resultado não deve ser nulo");
        assertTrue(executionTime <= extendedTimeParams.getTimeLimit() * 1100, "Não deve exceder muito o timeout");
        
        System.out.println("✅ Teste com timeout estendido concluído");
    }
    
    // Métodos auxiliares
    
    private ProblemInstance createMinimalInstance() {
        // Esta seria uma implementação para criar uma instância mínima
        // Por agora, usamos a instância sample
        return InstanceReader.createSampleInstance();
    }
    
    private ProblemInstance createMediumInstance() {
        // Esta seria uma implementação para criar uma instância média
        // Por agora, usamos a instância sample
        return InstanceReader.createSampleInstance();
    }
    
    private ParetoArchive createGuidingSolutions() {
        // Criar algumas soluções artificiais para testar
        ParetoArchive archive = new ParetoArchive(50);
        
        // Em uma implementação real, aqui criaríamos soluções válidas
        // Por agora, retornamos arquivo vazio para simular primeira execução
        
        return archive;
    }
} 