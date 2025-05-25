package com.ramonyago.cloudsim.algorithm.brkga;

/**
 * Parâmetros para o algoritmo BRKGA Multi-objetivo.
 */
public class BRKGAParameters {
    private final int populationSize;
    private final int maxGenerations;
    private final double eliteRatio;
    private final double mutantRatio;
    private final double inheritanceProbability;
    private final long randomSeed;
    private final int archiveSize;
    private final boolean useNSGA2Selection;
    
    public BRKGAParameters(Builder builder) {
        this.populationSize = builder.populationSize;
        this.maxGenerations = builder.maxGenerations;
        this.eliteRatio = builder.eliteRatio;
        this.mutantRatio = builder.mutantRatio;
        this.inheritanceProbability = builder.inheritanceProbability;
        this.randomSeed = builder.randomSeed;
        this.archiveSize = builder.archiveSize;
        this.useNSGA2Selection = builder.useNSGA2Selection;
        
        validateParameters();
    }
    
    private void validateParameters() {
        if (populationSize <= 0) {
            throw new IllegalArgumentException("Population size must be positive");
        }
        if (maxGenerations <= 0) {
            throw new IllegalArgumentException("Max generations must be positive");
        }
        if (eliteRatio < 0 || eliteRatio > 1) {
            throw new IllegalArgumentException("Elite ratio must be between 0 and 1");
        }
        if (mutantRatio < 0 || mutantRatio > 1) {
            throw new IllegalArgumentException("Mutant ratio must be between 0 and 1");
        }
        if (eliteRatio + mutantRatio > 1) {
            throw new IllegalArgumentException("Elite ratio + mutant ratio must not exceed 1");
        }
        if (inheritanceProbability < 0 || inheritanceProbability > 1) {
            throw new IllegalArgumentException("Inheritance probability must be between 0 and 1");
        }
        if (archiveSize <= 0) {
            throw new IllegalArgumentException("Archive size must be positive");
        }
    }
    
    public int getPopulationSize() {
        return populationSize;
    }
    
    public int getMaxGenerations() {
        return maxGenerations;
    }
    
    public double getEliteRatio() {
        return eliteRatio;
    }
    
    public double getMutantRatio() {
        return mutantRatio;
    }
    
    public double getInheritanceProbability() {
        return inheritanceProbability;
    }
    
    public long getRandomSeed() {
        return randomSeed;
    }
    
    public int getArchiveSize() {
        return archiveSize;
    }
    
    public boolean isUseNSGA2Selection() {
        return useNSGA2Selection;
    }
    
    public int getEliteSize() {
        return (int) Math.ceil(populationSize * eliteRatio);
    }
    
    public int getMutantSize() {
        return (int) Math.ceil(populationSize * mutantRatio);
    }
    
    public int getNonEliteSize() {
        return populationSize - getEliteSize() - getMutantSize();
    }
    
    /**
     * Cria parâmetros padrão para testes
     */
    public static BRKGAParameters createDefault() {
        return new Builder().build();
    }
    
    @Override
    public String toString() {
        return String.format("BRKGAParameters{" +
                           "popSize=%d, maxGen=%d, elite=%.2f, mutant=%.2f, " +
                           "inheritance=%.2f, archiveSize=%d, NSGA2=%s}",
                           populationSize, maxGenerations, eliteRatio, mutantRatio,
                           inheritanceProbability, archiveSize, useNSGA2Selection);
    }
    
    /**
     * Builder pattern para criar parâmetros do BRKGA
     */
    public static class Builder {
        private int populationSize = 100;
        private int maxGenerations = 1000;
        private double eliteRatio = 0.15;
        private double mutantRatio = 0.10;
        private double inheritanceProbability = 0.7;
        private long randomSeed = System.currentTimeMillis();
        private int archiveSize = 100;
        private boolean useNSGA2Selection = true;
        
        public Builder populationSize(int populationSize) {
            this.populationSize = populationSize;
            return this;
        }
        
        public Builder maxGenerations(int maxGenerations) {
            this.maxGenerations = maxGenerations;
            return this;
        }
        
        public Builder eliteRatio(double eliteRatio) {
            this.eliteRatio = eliteRatio;
            return this;
        }
        
        public Builder mutantRatio(double mutantRatio) {
            this.mutantRatio = mutantRatio;
            return this;
        }
        
        public Builder inheritanceProbability(double inheritanceProbability) {
            this.inheritanceProbability = inheritanceProbability;
            return this;
        }
        
        public Builder randomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }
        
        public Builder archiveSize(int archiveSize) {
            this.archiveSize = archiveSize;
            return this;
        }
        
        public Builder useNSGA2Selection(boolean useNSGA2Selection) {
            this.useNSGA2Selection = useNSGA2Selection;
            return this;
        }
        
        public BRKGAParameters build() {
            return new BRKGAParameters(this);
        }
    }
} 