package com.ramonyago.cloudsim.algorithm.brkga;

import com.ramonyago.cloudsim.model.AllocationSolution;

import java.util.Arrays;
import java.util.Random;

/**
 * Representa um indivíduo no algoritmo BRKGA com vetor de chaves aleatórias
 * e solução decodificada correspondente.
 */
public class Individual implements Comparable<Individual> {
    private final double[] keys;
    private AllocationSolution solution;
    private boolean evaluated;
    private int dominationRank;
    private double crowdingDistance;
    
    public Individual(int keyCount) {
        this.keys = new double[keyCount];
        this.evaluated = false;
        this.dominationRank = 0;
        this.crowdingDistance = 0.0;
    }
    
    public Individual(double[] keys) {
        this.keys = Arrays.copyOf(keys, keys.length);
        this.evaluated = false;
        this.dominationRank = 0;
        this.crowdingDistance = 0.0;
    }
    
    public Individual(Individual other) {
        this.keys = Arrays.copyOf(other.keys, other.keys.length);
        this.solution = other.solution != null ? new AllocationSolution(other.solution) : null;
        this.evaluated = other.evaluated;
        this.dominationRank = other.dominationRank;
        this.crowdingDistance = other.crowdingDistance;
    }
    
    /**
     * Inicializa o indivíduo com chaves aleatórias
     */
    public void randomize(Random random) {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = random.nextDouble();
        }
        invalidate();
    }
    
    /**
     * Realiza crossover com outro indivíduo (parent) para gerar offspring
     */
    public static Individual crossover(Individual elite, Individual nonElite, 
                                     double inheritanceProbability, Random random) {
        Individual offspring = new Individual(elite.keys.length);
        
        for (int i = 0; i < offspring.keys.length; i++) {
            if (random.nextDouble() < inheritanceProbability) {
                offspring.keys[i] = elite.keys[i];
            } else {
                offspring.keys[i] = nonElite.keys[i];
            }
        }
        
        return offspring;
    }
    
    public double[] getKeys() {
        return Arrays.copyOf(keys, keys.length);
    }
    
    public double getKey(int index) {
        return keys[index];
    }
    
    public void setKey(int index, double value) {
        keys[index] = Math.max(0.0, Math.min(1.0, value)); // Clamp to [0,1]
        invalidate();
    }
    
    public int getKeyCount() {
        return keys.length;
    }
    
    public AllocationSolution getSolution() {
        return solution;
    }
    
    public void setSolution(AllocationSolution solution) {
        this.solution = solution;
        this.evaluated = (solution != null);
    }
    
    public boolean isEvaluated() {
        return evaluated;
    }
    
    public void invalidate() {
        this.evaluated = false;
        this.solution = null;
    }
    
    public int getDominationRank() {
        return dominationRank;
    }
    
    public void setDominationRank(int dominationRank) {
        this.dominationRank = dominationRank;
    }
    
    public double getCrowdingDistance() {
        return crowdingDistance;
    }
    
    public void setCrowdingDistance(double crowdingDistance) {
        this.crowdingDistance = crowdingDistance;
    }
    
    /**
     * Gets the total cost from the solution
     */
    public double getTotalCost() {
        if (solution == null) {
            throw new IllegalStateException("Individual must be evaluated before accessing cost");
        }
        return solution.getTotalCost();
    }
    
    /**
     * Gets the total reliability from the solution
     */
    public double getTotalReliability() {
        if (solution == null) {
            throw new IllegalStateException("Individual must be evaluated before accessing reliability");
        }
        return solution.getTotalReliability();
    }
    
    /**
     * Compara dominância de Pareto com outro indivíduo
     * @return -1 se this domina other, 1 se other domina this, 0 se não-dominados
     */
    public int compareDominance(Individual other) {
        if (!this.isEvaluated() || !other.isEvaluated()) {
            throw new IllegalStateException("Cannot compare unevaluated individuals");
        }
        return this.solution.compareDominance(other.solution);
    }
    
    /**
     * Comparação para ordenação (NSGA-II): primeiro por rank, depois por crowding distance
     */
    @Override
    public int compareTo(Individual other) {
        // Primeiro compara por domination rank (menor é melhor)
        int rankComparison = Integer.compare(this.dominationRank, other.dominationRank);
        if (rankComparison != 0) {
            return rankComparison;
        }
        
        // Se ranks são iguais, compara por crowding distance (maior é melhor)
        return Double.compare(other.crowdingDistance, this.crowdingDistance);
    }
    
    /**
     * Calcula a distância Euclidiana entre os vetores de chaves
     */
    public double distanceTo(Individual other) {
        if (this.keys.length != other.keys.length) {
            throw new IllegalArgumentException("Individuals must have same key count");
        }
        
        double sum = 0.0;
        for (int i = 0; i < keys.length; i++) {
            double diff = this.keys[i] - other.keys[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Aplica mutação gaussiana a uma chave específica
     */
    public void mutateKey(int index, double sigma, Random random) {
        double noise = random.nextGaussian() * sigma;
        setKey(index, keys[index] + noise);
    }
    
    /**
     * Aplica mutação uniforme a uma chave específica
     */
    public void mutateKeyUniform(int index, Random random) {
        setKey(index, random.nextDouble());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Individual that = (Individual) o;
        return Arrays.equals(keys, that.keys);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(keys);
    }
    
    @Override
    public String toString() {
        if (solution != null) {
            return String.format("Individual{rank=%d, crowding=%.3f, cost=%.2f, reliability=%.3f}", 
                               dominationRank, crowdingDistance, 
                               solution.getTotalCost(), solution.getTotalReliability());
        } else {
            return String.format("Individual{keys=%s, unevaluated}", 
                               Arrays.toString(Arrays.copyOf(keys, Math.min(5, keys.length))));
        }
    }
} 