package com.ramonyago.cloudsim.util;

import com.ramonyago.cloudsim.model.AllocationSolution;

import java.util.*;

/**
 * Arquivo de soluções Pareto-ótimas com tamanho limitado.
 * Mantém apenas soluções não-dominadas e remove soluções dominadas automaticamente.
 */
public class ParetoArchive {
    private final List<AllocationSolution> solutions;
    private final int maxSize;
    
    public ParetoArchive(int maxSize) {
        this.maxSize = maxSize;
        this.solutions = new ArrayList<>();
    }
    
    /**
     * Adiciona uma solução ao arquivo se ela for não-dominada
     */
    public boolean add(AllocationSolution newSolution) {
        // Verifica se a nova solução é dominada por alguma existente
        for (AllocationSolution existing : solutions) {
            if (existing.compareDominance(newSolution) < 0) {
                return false; // Nova solução é dominada
            }
        }
        
        // Remove soluções dominadas pela nova solução
        solutions.removeIf(existing -> newSolution.compareDominance(existing) < 0);
        
        // Adiciona a nova solução
        solutions.add(new AllocationSolution(newSolution));
        
        // Se excedeu o tamanho máximo, remove soluções com menor crowding distance
        if (solutions.size() > maxSize) {
            reduceSizeByDiversity();
        }
        
        return true;
    }
    
    /**
     * Reduz o tamanho do arquivo mantendo diversidade
     */
    private void reduceSizeByDiversity() {
        if (solutions.size() <= maxSize) {
            return;
        }
        
        // Calcula crowding distance para todas as soluções
        List<AllocationSolution> sortedSolutions = new ArrayList<>(solutions);
        calculateCrowdingDistances(sortedSolutions);
        
        // Ordena por crowding distance (maior primeiro)
        sortedSolutions.sort((s1, s2) -> Double.compare(
            s2.calculateCrowdingDistance(sortedSolutions),
            s1.calculateCrowdingDistance(sortedSolutions)
        ));
        
        // Mantém apenas as primeiras maxSize soluções
        solutions.clear();
        solutions.addAll(sortedSolutions.subList(0, maxSize));
    }
    
    /**
     * Calcula crowding distances para um conjunto de soluções
     */
    private void calculateCrowdingDistances(List<AllocationSolution> solutionList) {
        if (solutionList.size() <= 2) {
            return; // Crowding distance será MAX_VALUE para todas
        }
        
        // Para o objetivo custo (minimizar)
        solutionList.sort(Comparator.comparingDouble(AllocationSolution::getTotalCost));
        assignCrowdingDistance(solutionList, AllocationSolution::getTotalCost);
        
        // Para o objetivo confiabilidade (maximizar)
        solutionList.sort(Comparator.comparingDouble(AllocationSolution::getTotalReliability));
        assignCrowdingDistance(solutionList, AllocationSolution::getTotalReliability);
    }
    
    private void assignCrowdingDistance(List<AllocationSolution> sortedSolutions,
                                      java.util.function.Function<AllocationSolution, Double> objectiveExtractor) {
        if (sortedSolutions.size() < 3) {
            return;
        }
        
        double minValue = objectiveExtractor.apply(sortedSolutions.get(0));
        double maxValue = objectiveExtractor.apply(sortedSolutions.get(sortedSolutions.size() - 1));
        double range = maxValue - minValue;
        
        if (range > 0) {
            for (int i = 1; i < sortedSolutions.size() - 1; i++) {
                double distance = (objectiveExtractor.apply(sortedSolutions.get(i + 1)) -
                                 objectiveExtractor.apply(sortedSolutions.get(i - 1))) / range;
                // Note: não podemos modificar a crowding distance diretamente na AllocationSolution
                // Esta é uma simplificação para o exemplo
            }
        }
    }
    
    /**
     * Retorna todas as soluções no arquivo
     */
    public List<AllocationSolution> getSolutions() {
        return new ArrayList<>(solutions);
    }
    
    /**
     * Retorna o número de soluções no arquivo
     */
    public int size() {
        return solutions.size();
    }
    
    /**
     * Verifica se o arquivo está vazio
     */
    public boolean isEmpty() {
        return solutions.isEmpty();
    }
    
    /**
     * Remove todas as soluções do arquivo
     */
    public void clear() {
        solutions.clear();
    }
    
    /**
     * Retorna a solução com menor custo
     */
    public AllocationSolution getBestCost() {
        return solutions.stream()
                .min(Comparator.comparingDouble(AllocationSolution::getTotalCost))
                .orElse(null);
    }
    
    /**
     * Retorna a solução com maior confiabilidade
     */
    public AllocationSolution getBestReliability() {
        return solutions.stream()
                .max(Comparator.comparingDouble(AllocationSolution::getTotalReliability))
                .orElse(null);
    }
    
    /**
     * Retorna uma solução balanceada (soma ponderada normalizada)
     */
    public AllocationSolution getBalancedSolution(double costWeight, double reliabilityWeight) {
        if (solutions.isEmpty()) {
            return null;
        }
        
        // Normaliza os objetivos
        double minCost = solutions.stream().mapToDouble(AllocationSolution::getTotalCost).min().orElse(0.0);
        double maxCost = solutions.stream().mapToDouble(AllocationSolution::getTotalCost).max().orElse(1.0);
        double minReliability = solutions.stream().mapToDouble(AllocationSolution::getTotalReliability).min().orElse(0.0);
        double maxReliability = solutions.stream().mapToDouble(AllocationSolution::getTotalReliability).max().orElse(1.0);
        
        double costRange = maxCost - minCost;
        double reliabilityRange = maxReliability - minReliability;
        
        return solutions.stream()
                .min((s1, s2) -> {
                    // Normaliza e calcula score (menor é melhor)
                    double score1 = calculateNormalizedScore(s1, minCost, costRange, minReliability, reliabilityRange, costWeight, reliabilityWeight);
                    double score2 = calculateNormalizedScore(s2, minCost, costRange, minReliability, reliabilityRange, costWeight, reliabilityWeight);
                    return Double.compare(score1, score2);
                })
                .orElse(null);
    }
    
    private double calculateNormalizedScore(AllocationSolution solution, double minCost, double costRange,
                                          double minReliability, double reliabilityRange,
                                          double costWeight, double reliabilityWeight) {
        double normalizedCost = costRange > 0 ? (solution.getTotalCost() - minCost) / costRange : 0.0;
        double normalizedReliability = reliabilityRange > 0 ? (solution.getTotalReliability() - minReliability) / reliabilityRange : 0.0;
        
        // Para confiabilidade, queremos maximizar, então invertemos
        return costWeight * normalizedCost + reliabilityWeight * (1.0 - normalizedReliability);
    }
    
    /**
     * Calcula estatísticas do arquivo
     */
    public ArchiveStatistics getStatistics() {
        if (solutions.isEmpty()) {
            return new ArchiveStatistics(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        
        double minCost = solutions.stream().mapToDouble(AllocationSolution::getTotalCost).min().orElse(0.0);
        double maxCost = solutions.stream().mapToDouble(AllocationSolution::getTotalCost).max().orElse(0.0);
        double avgCost = solutions.stream().mapToDouble(AllocationSolution::getTotalCost).average().orElse(0.0);
        
        double minReliability = solutions.stream().mapToDouble(AllocationSolution::getTotalReliability).min().orElse(0.0);
        double maxReliability = solutions.stream().mapToDouble(AllocationSolution::getTotalReliability).max().orElse(0.0);
        double avgReliability = solutions.stream().mapToDouble(AllocationSolution::getTotalReliability).average().orElse(0.0);
        
        return new ArchiveStatistics(solutions.size(), minCost, maxCost, avgCost, 
                                   minReliability, maxReliability, avgReliability);
    }
    
    @Override
    public String toString() {
        return String.format("ParetoArchive{size=%d, maxSize=%d}", solutions.size(), maxSize);
    }
    
    /**
     * Classe para estatísticas do arquivo
     */
    public static class ArchiveStatistics {
        public final int size;
        public final double minCost;
        public final double maxCost;
        public final double avgCost;
        public final double minReliability;
        public final double maxReliability;
        public final double avgReliability;
        
        public ArchiveStatistics(int size, double minCost, double maxCost, double avgCost,
                               double minReliability, double maxReliability, double avgReliability) {
            this.size = size;
            this.minCost = minCost;
            this.maxCost = maxCost;
            this.avgCost = avgCost;
            this.minReliability = minReliability;
            this.maxReliability = maxReliability;
            this.avgReliability = avgReliability;
        }
        
        @Override
        public String toString() {
            return String.format("ArchiveStats{size=%d, cost=[%.2f, %.2f, %.2f], reliability=[%.3f, %.3f, %.3f]}",
                               size, minCost, maxCost, avgCost, minReliability, maxReliability, avgReliability);
        }
    }
} 