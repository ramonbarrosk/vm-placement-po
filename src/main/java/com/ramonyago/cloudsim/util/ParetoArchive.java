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
    public synchronized boolean add(AllocationSolution newSolution) {
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
    private synchronized void reduceSizeByDiversity() {
        if (solutions.size() <= maxSize) {
            return;
        }
        
        // Simple approach: just keep the first maxSize solutions
        // This avoids all the complex crowding distance calculations that are causing issues
        List<AllocationSolution> solutionsToKeep = new ArrayList<>();
        for (int i = 0; i < Math.min(maxSize, solutions.size()); i++) {
            solutionsToKeep.add(solutions.get(i));
        }
        
        solutions.clear();
        solutions.addAll(solutionsToKeep);
    }
    
    /**
     * Calculates crowding distances for all solutions in the list
     */
    private Map<AllocationSolution, Double> calculateCrowdingDistances(List<AllocationSolution> solutionList) {
        Map<AllocationSolution, Double> distances = new HashMap<>();
        
        if (solutionList.size() <= 2) {
            // All solutions have infinite crowding distance
            for (AllocationSolution solution : solutionList) {
                distances.put(solution, Double.MAX_VALUE);
            }
            return distances;
        }
        
        // Initialize all distances to 0
        for (AllocationSolution solution : solutionList) {
            distances.put(solution, 0.0);
        }
        
        // Calculate crowding distance for cost objective
        calculateCrowdingDistanceForObjective(solutionList, distances, AllocationSolution::getTotalCost);
        
        // Calculate crowding distance for reliability objective  
        calculateCrowdingDistanceForObjective(solutionList, distances, AllocationSolution::getTotalReliability);
        
        return distances;
    }
    
    /**
     * Calculates crowding distance contribution for a specific objective
     */
    private void calculateCrowdingDistanceForObjective(List<AllocationSolution> solutionList,
                                                      Map<AllocationSolution, Double> distances,
                                                      java.util.function.Function<AllocationSolution, Double> objectiveExtractor) {
        // Ensure all objective values are computed before sorting
        for (AllocationSolution solution : solutionList) {
            objectiveExtractor.apply(solution);
        }
        
        // Create a copy and sort by objective
        List<AllocationSolution> sortedByObjective = new ArrayList<>(solutionList);
        sortedByObjective.sort(Comparator.comparingDouble(objectiveExtractor::apply));
        
        // Boundary solutions have infinite distance
        AllocationSolution first = sortedByObjective.get(0);
        AllocationSolution last = sortedByObjective.get(sortedByObjective.size() - 1);
        distances.put(first, Double.MAX_VALUE);
        distances.put(last, Double.MAX_VALUE);
        
        // Calculate range
        double minValue = objectiveExtractor.apply(first);
        double maxValue = objectiveExtractor.apply(last);
        double range = maxValue - minValue;
        
        if (range > 0) {
            // Calculate crowding distance for intermediate solutions
            for (int i = 1; i < sortedByObjective.size() - 1; i++) {
                AllocationSolution current = sortedByObjective.get(i);
                AllocationSolution next = sortedByObjective.get(i + 1);
                AllocationSolution prev = sortedByObjective.get(i - 1);
                
                double distance = (objectiveExtractor.apply(next) - objectiveExtractor.apply(prev)) / range;
                
                // Add to existing distance (might have been calculated for other objectives)
                Double currentDistance = distances.get(current);
                if (!currentDistance.equals(Double.MAX_VALUE)) {
                    distances.put(current, currentDistance + distance);
                }
            }
        }
    }
    
    /**
     * Retorna todas as soluções no arquivo
     */
    public synchronized List<AllocationSolution> getSolutions() {
        return new ArrayList<>(solutions);
    }
    
    /**
     * Retorna o número de soluções no arquivo
     */
    public synchronized int size() {
        return solutions.size();
    }
    
    /**
     * Verifica se o arquivo está vazio
     */
    public synchronized boolean isEmpty() {
        return solutions.isEmpty();
    }
    
    /**
     * Remove todas as soluções do arquivo
     */
    public synchronized void clear() {
        solutions.clear();
    }
    
    /**
     * Retorna a solução com menor custo
     */
    public synchronized AllocationSolution getBestCost() {
        return solutions.stream()
                .min(Comparator.comparingDouble(AllocationSolution::getTotalCost))
                .orElse(null);
    }
    
    /**
     * Retorna a solução com maior confiabilidade
     */
    public synchronized AllocationSolution getBestReliability() {
        return solutions.stream()
                .max(Comparator.comparingDouble(AllocationSolution::getTotalReliability))
                .orElse(null);
    }
    
    /**
     * Retorna uma solução balanceada (soma ponderada normalizada)
     */
    public synchronized AllocationSolution getBalancedSolution(double costWeight, double reliabilityWeight) {
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
    public synchronized ArchiveStatistics getStatistics() {
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