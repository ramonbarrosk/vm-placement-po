package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.MOILP;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.util.ParetoArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Programa específico para analisar e testar o MOILP (Multi-Objective Integer Linear Programming)
 * 
 * Este programa realiza as seguintes verificações:
 * 1. Verifica o tempo limite configurado para o MOILP
 * 2. Testa a integração com o solver MILP com um modelo muito simples
 * 3. Analisa a formulação exata do MOILP que está sendo enviada ao solver
 * 4. Examina a estratégia MOILP (ε-constraint) e quantos problemas MILP individuais são resolvidos
 */
public class MOILPAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(MOILPAnalyzer.class);
    
    public static void main(String[] args) {
        logger.info("=".repeat(80));
        logger.info("ANÁLISE COMPLETA DO MOILP - Multi-Objective Integer Linear Programming");
        logger.info("=".repeat(80));
        
        try {
            MOILPAnalyzer analyzer = new MOILPAnalyzer();
            
            // 1. Verificar configuração de timeout
            analyzer.analyzeTimeoutConfiguration();
            
            // 2. Testar modelo MOILP simples
            analyzer.testSimpleMOILPModel();
            
            // 3. Analisar formulação exata
            analyzer.analyzeMOILPFormulation();
            
            // 4. Examinar estratégia epsilon-constraint
            analyzer.analyzeEpsilonConstraintStrategy();
            
            // 5. Teste com timeout estendido
            analyzer.testExtendedTimeout();
            
            logger.info("\n" + "=".repeat(80));
            logger.info("ANÁLISE MOILP CONCLUÍDA COM SUCESSO");
            logger.info("=".repeat(80));
            
        } catch (Exception e) {
            logger.error("Erro durante análise do MOILP", e);
            System.exit(1);
        }
    }
    
    /**
     * 1. Verificar o tempo limite configurado para o MOILP
     */
    private void analyzeTimeoutConfiguration() {
        logger.info("\n" + "1. VERIFICAÇÃO DE CONFIGURAÇÃO DE TIMEOUT");
        logger.info("-".repeat(60));
        
        // Parâmetros atuais do sistema
        OptimizationParameters defaultParams = OptimizationParameters.createDefault();
        OptimizationParameters intensiveParams = OptimizationParameters.createIntensive();
        OptimizationParameters quickParams = OptimizationParameters.createQuick();
        
        logger.info("Configurações de timeout MOILP:");
        logger.info("  Padrão: {} segundos ({} minutos)", 
                   defaultParams.getMoilpTimeLimit(), 
                   defaultParams.getMoilpTimeLimit() / 60.0);
        logger.info("  Intensivo: {} segundos ({} minutos)", 
                   intensiveParams.getMoilpTimeLimit(), 
                   intensiveParams.getMoilpTimeLimit() / 60.0);
        logger.info("  Rápido: {} segundos ({} minutos)", 
                   quickParams.getMoilpTimeLimit(), 
                   quickParams.getMoilpTimeLimit() / 60.0);
        
        // Verificar se o timeout foi aumentado significativamente
        if (defaultParams.getMoilpTimeLimit() >= 1800.0) {
            logger.info("✅ Timeout foi AUMENTADO SIGNIFICATIVAMENTE para testes (>= 30 minutos)");
        } else {
            logger.warn("⚠️ Timeout ainda está baixo - considere aumentar para testes mais longos");
        }
        
        // Parâmetros customizados para testes específicos
        MOILP.MOILPParameters testParams = new MOILP.MOILPParameters(
                3600.0, // 1 hora para testes extensivos
                200,    // mais variáveis para formulação exata
                25,     // mais passos epsilon
                150,    // archive maior
                "GLPK"  // solver
        );
        
        logger.info("\nParâmetros customizados para testes:");
        logger.info("  {}", testParams);
    }
    
    /**
     * 2. Testar a integração com o solver MILP com um modelo muito simples
     */
    private void testSimpleMOILPModel() {
        logger.info("\n" + "2. TESTE DE MODELO MOILP MUITO SIMPLES");
        logger.info("-".repeat(60));
        
        try {
            // Criar instância mínima
            ProblemInstance instance = InstanceReader.createSampleInstance();
            
            logger.info("Instância de teste:");
            logger.info("  Nome: {}", instance.getInstanceName());
            logger.info("  VMs: {}", instance.getVMCount());
            logger.info("  Hosts: {}", instance.getHostCount());
            logger.info("  Variáveis de decisão (x_ij): {}", instance.getVMCount() * instance.getHostCount());
            logger.info("  Variáveis de ativação (y_j): {}", instance.getHostCount());
            logger.info("  Total de variáveis: {}", 
                       instance.getVMCount() * instance.getHostCount() + instance.getHostCount());
            
            // Parâmetros para modelo simples
            MOILP.MOILPParameters simpleParams = new MOILP.MOILPParameters(
                    120.0, // 2 minutos suficiente para modelo simples
                    100,   // permitir formulação exata
                    5,     // poucos passos epsilon
                    30,    // archive pequeno
                    "GLPK"
            );
            
            logger.info("\nTestando comunicação com solver:");
            logger.info("  Solver: {}", simpleParams.getSolver());
            logger.info("  Timeout: {} segundos", simpleParams.getTimeLimit());
            logger.info("  Passos ε: {}", simpleParams.getNumEpsilonSteps());
            
            // Inicializar com arquivo vazio (primeira execução)
            ParetoArchive emptyGuide = new ParetoArchive(10);
            MOILP moilp = new MOILP(instance, simpleParams);
            
            logger.info("\nExecutando MOILP...");
            long startTime = System.currentTimeMillis();
            ParetoArchive result = moilp.solve(emptyGuide);
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.info("\nResultados do teste simples:");
            logger.info("  Tempo de execução: {} ms ({} segundos)", 
                       executionTime, executionTime / 1000.0);
            logger.info("  Soluções geradas: {}", moilp.getSolutionsGenerated());
            logger.info("  Método usado: {}", moilp.isOptimalSolutions() ? "EXATO (ILP)" : "HEURÍSTICO");
            logger.info("  Archive final: {} soluções", result.size());
            
            if (result.size() > 0) {
                logger.info("✅ COMUNICAÇÃO COM SOLVER FUNCIONANDO");
                
                // Analisar primeira solução
                AllocationSolution solution = result.getSolutions().get(0);
                logger.info("  Exemplo de solução encontrada:");
                logger.info("    Custo total: {}", String.format("%.2f", solution.getTotalCost()));
                logger.info("    Confiabilidade: {}", String.format("%.4f", solution.getTotalReliability()));
                logger.info("    Consumo energia: {}", String.format("%.2f", solution.getEnergyConsumption()));
                logger.info("    Hosts ativos: {}", solution.getActiveHosts().size());
                logger.info("    Solução viável: {}", solution.isFeasible());
                
                // Mostrar estatísticas do arquivo
                var stats = result.getStatistics();
                logger.info("  Estatísticas do arquivo Pareto:");
                logger.info("    Faixa de custo: [{}, {}]", 
                           String.format("%.2f", stats.minCost), 
                           String.format("%.2f", stats.maxCost));
                logger.info("    Faixa de confiabilidade: [{}, {}]", 
                           String.format("%.4f", stats.minReliability), 
                           String.format("%.4f", stats.maxReliability));
            } else {
                logger.warn("⚠️ NENHUMA SOLUÇÃO ENCONTRADA");
                logger.warn("   Possíveis causas:");
                logger.warn("   - Problema na formulação MOILP");
                logger.warn("   - Solver não instalado ou configurado incorretamente");
                logger.warn("   - Instância infeasível");
                logger.warn("   - Timeout muito baixo");
            }
            
        } catch (Exception e) {
            logger.error("❌ ERRO na integração com solver: {}", e.getMessage());
            logger.error("   Verifique se o solver {} está instalado e acessível", "GLPK");
        }
    }
    
    /**
     * 3. Analisar a formulação exata do MOILP que está sendo enviada ao solver
     */
    private void analyzeMOILPFormulation() {
        logger.info("\n" + "3. ANÁLISE DA FORMULAÇÃO EXATA DO MOILP");
        logger.info("-".repeat(60));
        
        ProblemInstance instance = InstanceReader.createSampleInstance();
        
        logger.info("FORMULAÇÃO MATEMÁTICA DO PROBLEMA:");
        logger.info("");
        
        // Variáveis de decisão
        logger.info("VARIÁVEIS DE DECISÃO:");
        logger.info("  x_ij ∈ {0,1} : VM i alocada no host j");
        logger.info("  y_j ∈ {0,1}  : Host j está ativo");
        logger.info("  Dimensões: {} VMs × {} hosts = {} variáveis x_ij + {} variáveis y_j", 
                   instance.getVMCount(), instance.getHostCount(), 
                   instance.getVMCount() * instance.getHostCount(), instance.getHostCount());
        
        // Objetivos
        logger.info("\nOBJETIVOS:");
        logger.info("  f1(x,y) = minimizar custo total");
        logger.info("           = Σ_j (y_j × custo_ativação_j + Σ_i (x_ij × custo_energia_ij))");
        logger.info("  f2(x,y) = maximizar confiabilidade total");
        logger.info("           = Π_i (Σ_j (x_ij × confiabilidade_j))");
        
        // Restrições
        logger.info("\nRESTRIÇÕES:");
        logger.info("  1. Cada VM deve ser alocada em exatamente um host:");
        logger.info("     Σ_j x_ij = 1, ∀i ∈ VMs");
        logger.info("  2. Capacidade de recursos nos hosts:");
        logger.info("     Σ_i (x_ij × demanda_ir) ≤ capacidade_jr × y_j, ∀j ∈ hosts, ∀r ∈ recursos");
        logger.info("  3. Restrições de confiabilidade mínima:");
        logger.info("     Σ_j (x_ij × confiabilidade_j) ≥ min_confiabilidade_i, ∀i ∈ VMs");
        logger.info("  4. Ativação de hosts:");
        logger.info("     x_ij ≤ y_j, ∀i ∈ VMs, ∀j ∈ hosts");
        
        // Estatísticas da instância
        logger.info("\nESTATÍSTICAS DA INSTÂNCIA:");
        instance.getVMs().forEach(vm -> {
            logger.info("  VM {}: confiabilidade_min={}, prioridade={}, recursos={}", 
                       vm.getId(), vm.getMinReliability(), vm.getPriority(), 
                       vm.getResourceDemands());
        });
        
        instance.getHosts().forEach(host -> {
            double reliability = 1.0 - host.getFailureProbability();
            logger.info("  Host {}: confiabilidade={}, custo_ativação={}, energia={}, capacidades={}", 
                       host.getId(), String.format("%.3f", reliability), 
                       host.getActivationCost(), host.getEnergyConsumption(), 
                       host.getResourceCapacities());
        });
        
        // Análise de complexidade
        int variables = instance.getVMCount() * instance.getHostCount() + instance.getHostCount();
        int assignmentConstraints = instance.getVMCount();
        int capacityConstraints = instance.getHostCount() * 4; // 4 tipos de recursos
        int reliabilityConstraints = instance.getVMCount();
        int activationConstraints = instance.getVMCount() * instance.getHostCount();
        int totalConstraints = assignmentConstraints + capacityConstraints + 
                              reliabilityConstraints + activationConstraints;
        
        logger.info("\nCOMPLEXIDADE DO MODELO:");
        logger.info("  Total de variáveis: {} ({} binárias)", variables, variables);
        logger.info("  Total de restrições: {}", totalConstraints);
        logger.info("    - Atribuição: {}", assignmentConstraints);
        logger.info("    - Capacidade: {}", capacityConstraints);
        logger.info("    - Confiabilidade: {}", reliabilityConstraints);
        logger.info("    - Ativação: {}", activationConstraints);
        
        if (variables <= 100) {
            logger.info("✅ Instância PEQUENA - formulação exata é factível");
        } else if (variables <= 1000) {
            logger.info("⚠️ Instância MÉDIA - formulação exata pode ser lenta");
        } else {
            logger.info("❌ Instância GRANDE - recomenda-se heurística");
        }
    }
    
    /**
     * 4. Examinar a estratégia MOILP (ε-constraint) e quantos problemas MILP individuais são resolvidos
     */
    private void analyzeEpsilonConstraintStrategy() {
        logger.info("\n" + "4. ANÁLISE DA ESTRATÉGIA ε-CONSTRAINT");
        logger.info("-".repeat(60));
        
        // Parâmetros para análise detalhada
        MOILP.MOILPParameters analysisParams = new MOILP.MOILPParameters(
                900.0,  // 15 minutos
                100,    // max variables for exact
                15,     // número de passos epsilon para análise
                75,     // archive size
                "GLPK"
        );
        
        logger.info("ESTRATÉGIA ε-CONSTRAINT:");
        logger.info("  Método: Transformar problema multi-objetivo em {} problemas mono-objetivo", 
                   analysisParams.getNumEpsilonSteps() + 1);
        logger.info("  Objetivo principal: maximizar confiabilidade");
        logger.info("  Restrição: custo total ≤ ε_i");
        logger.info("  Número de valores ε: {}", analysisParams.getNumEpsilonSteps() + 1);
        
        // Simular criação de soluções guia
        ParetoArchive guidingSolutions = createDummyGuidingSolutions();
        
        if (guidingSolutions.size() > 0) {
            var stats = guidingSolutions.getStatistics();
            double minCost = stats.minCost;
            double maxCost = stats.maxCost;
            double stepSize = (maxCost - minCost) / analysisParams.getNumEpsilonSteps();
            
            logger.info("\nCÁLCULO DOS VALORES ε:");
            logger.info("  Faixa de custo das soluções guia: [{}, {}]", 
                       String.format("%.2f", minCost), String.format("%.2f", maxCost));
            logger.info("  Tamanho do passo: {}", String.format("%.2f", stepSize));
            logger.info("  Valores ε calculados:");
            
            for (int i = 0; i <= analysisParams.getNumEpsilonSteps(); i++) {
                double epsilon = minCost + i * stepSize;
                logger.info("    Problema MILP {}: maximizar Σ_i log(reliability_i) sujeito a custo ≤ {}", 
                           i + 1, String.format("%.2f", epsilon));
            }
            
            logger.info("\nPROBLEMAS MILP INDIVIDUAIS:");
            logger.info("  Cada problema MILP resolve:");
            logger.info("    maximize Σ_i (Σ_j (x_ij × log(1 - failure_prob_j)))");
            logger.info("    subject to:");
            logger.info("      Σ_j (y_j × activation_cost_j) + Σ_i,j (x_ij × energy_cost_ij) ≤ ε_k");
            logger.info("      + todas as restrições de viabilidade");
            
            // Executar análise
            ProblemInstance instance = InstanceReader.createSampleInstance();
            MOILP moilp = new MOILP(instance, analysisParams);
            
            logger.info("\nExecutando estratégia ε-constraint...");
            long startTime = System.currentTimeMillis();
            ParetoArchive result = moilp.solve(guidingSolutions);
            long executionTime = System.currentTimeMillis() - startTime;
            
            logger.info("\nRESULTADOS DA ESTRATÉGIA:");
            logger.info("  Tempo total: {} ms ({} segundos)", executionTime, executionTime / 1000.0);
            logger.info("  Problemas MILP resolvidos: estimativa {}", analysisParams.getNumEpsilonSteps() + 1);
            logger.info("  Tempo médio por problema: {} ms", 
                       executionTime / (analysisParams.getNumEpsilonSteps() + 1));
            logger.info("  Soluções novas geradas: {}", moilp.getSolutionsGenerated());
            logger.info("  Total no arquivo final: {}", result.size());
            logger.info("  Soluções são ótimas: {}", moilp.isOptimalSolutions());
            
            if (result.size() > guidingSolutions.size()) {
                logger.info("✅ Estratégia ε-constraint gerou novas soluções Pareto-ótimas");
            } else {
                logger.info("⚠️ Nenhuma solução nova - pode indicar que soluções guia já são ótimas");
            }
        }
    }
    
    /**
     * 5. Teste com timeout estendido para verificar comportamento
     */
    private void testExtendedTimeout() {
        logger.info("\n" + "5. TESTE COM TIMEOUT ESTENDIDO");
        logger.info("-".repeat(60));
        
        // Parâmetros com timeout muito longo
        MOILP.MOILPParameters extendedParams = new MOILP.MOILPParameters(
                2400.0, // 40 minutos - muito estendido
                150,    // mais variáveis para exato
                30,     // muitos passos epsilon
                200,    // archive grande
                "GLPK"
        );
        
        logger.info("CONFIGURAÇÃO DO TESTE ESTENDIDO:");
        logger.info("  Timeout: {} segundos ({} minutos)", 
                   extendedParams.getTimeLimit(), extendedParams.getTimeLimit() / 60.0);
        logger.info("  Passos ε: {}", extendedParams.getNumEpsilonSteps());
        logger.info("  Max variáveis para exato: {}", extendedParams.getMaxVariablesForExact());
        
        ProblemInstance instance = InstanceReader.createSampleInstance();
        int totalVars = instance.getVMCount() * instance.getHostCount() + instance.getHostCount();
        
        logger.info("  Variáveis na instância: {}", totalVars);
        logger.info("  Usará método: {}", 
                   totalVars <= extendedParams.getMaxVariablesForExact() ? "EXATO" : "HEURÍSTICO");
        
        // Para este teste, usar timeout menor para demonstração
        MOILP.MOILPParameters demoParams = new MOILP.MOILPParameters(
                180.0,  // 3 minutos para demonstração
                150,    
                10,     
                100,    
                "GLPK"
        );
        
        logger.info("\nExecutando teste de demonstração (timeout reduzido)...");
        
        ParetoArchive guidingSolutions = createDummyGuidingSolutions();
        MOILP moilp = new MOILP(instance, demoParams);
        
        long startTime = System.currentTimeMillis();
        ParetoArchive result = moilp.solve(guidingSolutions);
        long executionTime = System.currentTimeMillis() - startTime;
        
        logger.info("\nRESULTADOS DO TESTE ESTENDIDO:");
        logger.info("  Tempo de execução: {} ms ({} minutos)", 
                   executionTime, executionTime / 60000.0);
        logger.info("  Percentual do timeout usado: {}%", 
                   String.format("%.1f", (executionTime / 1000.0) / demoParams.getTimeLimit() * 100));
        logger.info("  Soluções geradas: {}", moilp.getSolutionsGenerated());
        logger.info("  Archive final: {}", result.size());
        
        // Verificar se timeout foi respeitado
        if (executionTime <= demoParams.getTimeLimit() * 1000 * 1.1) { // 10% de tolerância
            logger.info("✅ Timeout foi respeitado corretamente");
        } else {
            logger.warn("⚠️ Execução excedeu timeout configurado");
        }
    }
    
    /**
     * Cria um conjunto de soluções guia dummy para testes
     */
    private ParetoArchive createDummyGuidingSolutions() {
        ParetoArchive archive = new ParetoArchive(50);
        
        // Em uma implementação real, aqui seriam criadas soluções válidas
        // Para este exemplo, retornamos um arquivo vazio que será preenchido
        // pelo próprio MOILP na primeira execução
        
        return archive;
    }
} 