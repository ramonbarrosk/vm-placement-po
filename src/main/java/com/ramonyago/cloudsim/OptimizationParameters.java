package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder;

/**
 * Parâmetros configuráveis para todo o sistema de otimização híbrida.
 */
public class OptimizationParameters {
    // Parâmetros gerais
    private final long randomSeed;
    private final int archiveSize;
    
    // Parâmetros do BRKGA
    private final int brkgaPopulationSize;
    private final int brkgaMaxGenerations;
    private final double brkgaEliteRatio;
    private final double brkgaMutantRatio;
    private final double brkgaInheritanceProbability;
    private final BRKGADecoder.DecodingStrategy decodingStrategy;
    
    // Parâmetros da Busca Tabu
    private final int tabuListSize;
    private final int tabuMaxIterations;
    private final boolean tabuUseIntensification;
    private final boolean tabuUseDiversification;
    
    private OptimizationParameters(Builder builder) {
        this.randomSeed = builder.randomSeed;
        this.archiveSize = builder.archiveSize;
        
        this.brkgaPopulationSize = builder.brkgaPopulationSize;
        this.brkgaMaxGenerations = builder.brkgaMaxGenerations;
        this.brkgaEliteRatio = builder.brkgaEliteRatio;
        this.brkgaMutantRatio = builder.brkgaMutantRatio;
        this.brkgaInheritanceProbability = builder.brkgaInheritanceProbability;
        this.decodingStrategy = builder.decodingStrategy;
        
        this.tabuListSize = builder.tabuListSize;
        this.tabuMaxIterations = builder.tabuMaxIterations;
        this.tabuUseIntensification = builder.tabuUseIntensification;
        this.tabuUseDiversification = builder.tabuUseDiversification;
    }
    
    // Getters
    public long getRandomSeed() { return randomSeed; }
    public int getArchiveSize() { return archiveSize; }
    
    public int getBrkgaPopulationSize() { return brkgaPopulationSize; }
    public int getBrkgaMaxGenerations() { return brkgaMaxGenerations; }
    public double getBrkgaEliteRatio() { return brkgaEliteRatio; }
    public double getBrkgaMutantRatio() { return brkgaMutantRatio; }
    public double getBrkgaInheritanceProbability() { return brkgaInheritanceProbability; }
    public BRKGADecoder.DecodingStrategy getDecodingStrategy() { return decodingStrategy; }
    
    public int getTabuListSize() { return tabuListSize; }
    public int getTabuMaxIterations() { return tabuMaxIterations; }
    public boolean isTabuUseIntensification() { return tabuUseIntensification; }
    public boolean isTabuUseDiversification() { return tabuUseDiversification; }
    
    /**
     * Cria parâmetros padrão para testes rápidos
     */
    public static OptimizationParameters createDefault() {
        return new Builder().build();
    }
    
    /**
     * Cria parâmetros para execução mais intensiva
     */
    public static OptimizationParameters createIntensive() {
        return new Builder()
                .brkgaPopulationSize(200)
                .brkgaMaxGenerations(2000)
                .tabuMaxIterations(1000)
                .archiveSize(200)
                .build();
    }
    
    /**
     * Cria parâmetros para testes rápidos
     */
    public static OptimizationParameters createQuick() {
        return new Builder()
                .brkgaPopulationSize(50)
                .brkgaMaxGenerations(100)
                .tabuMaxIterations(100)
                .archiveSize(50)
                .build();
    }
    
    @Override
    public String toString() {
        return String.format("OptimizationParameters{" +
                           "BRKGA(pop=%d, gen=%d, elite=%.2f, mutant=%.2f), " +
                           "Tabu(list=%d, iter=%d), " +
                           "archive=%d, seed=%d}",
                           brkgaPopulationSize, brkgaMaxGenerations, brkgaEliteRatio, brkgaMutantRatio,
                           tabuListSize, tabuMaxIterations,
                           archiveSize, randomSeed);
    }
    
    /**
     * Builder pattern para criar parâmetros
     */
    public static class Builder {
        // Valores padrão
        private long randomSeed = System.currentTimeMillis();
        private int archiveSize = 100;
        
        private int brkgaPopulationSize = 100;
        private int brkgaMaxGenerations = 500;
        private double brkgaEliteRatio = 0.15;
        private double brkgaMutantRatio = 0.10;
        private double brkgaInheritanceProbability = 0.7;
        private BRKGADecoder.DecodingStrategy decodingStrategy = BRKGADecoder.DecodingStrategy.BALANCED;
        
        private int tabuListSize = 50;
        private int tabuMaxIterations = 500;
        private boolean tabuUseIntensification = true;
        private boolean tabuUseDiversification = true;
        
        public Builder randomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }
        
        public Builder archiveSize(int archiveSize) {
            this.archiveSize = archiveSize;
            return this;
        }
        
        public Builder brkgaPopulationSize(int brkgaPopulationSize) {
            this.brkgaPopulationSize = brkgaPopulationSize;
            return this;
        }
        
        public Builder brkgaMaxGenerations(int brkgaMaxGenerations) {
            this.brkgaMaxGenerations = brkgaMaxGenerations;
            return this;
        }
        
        public Builder brkgaEliteRatio(double brkgaEliteRatio) {
            this.brkgaEliteRatio = brkgaEliteRatio;
            return this;
        }
        
        public Builder brkgaMutantRatio(double brkgaMutantRatio) {
            this.brkgaMutantRatio = brkgaMutantRatio;
            return this;
        }
        
        public Builder brkgaInheritanceProbability(double brkgaInheritanceProbability) {
            this.brkgaInheritanceProbability = brkgaInheritanceProbability;
            return this;
        }
        
        public Builder decodingStrategy(BRKGADecoder.DecodingStrategy decodingStrategy) {
            this.decodingStrategy = decodingStrategy;
            return this;
        }
        
        public Builder tabuListSize(int tabuListSize) {
            this.tabuListSize = tabuListSize;
            return this;
        }
        
        public Builder tabuMaxIterations(int tabuMaxIterations) {
            this.tabuMaxIterations = tabuMaxIterations;
            return this;
        }
        
        public Builder tabuUseIntensification(boolean tabuUseIntensification) {
            this.tabuUseIntensification = tabuUseIntensification;
            return this;
        }
        
        public Builder tabuUseDiversification(boolean tabuUseDiversification) {
            this.tabuUseDiversification = tabuUseDiversification;
            return this;
        }
        
        public OptimizationParameters build() {
            // Validate parameters
            if (brkgaPopulationSize <= 0) {
                throw new IllegalArgumentException("BRKGA population size must be positive");
            }
            if (brkgaMaxGenerations <= 0) {
                throw new IllegalArgumentException("BRKGA max generations must be positive");
            }
            if (brkgaEliteRatio < 0 || brkgaEliteRatio > 1) {
                throw new IllegalArgumentException("BRKGA elite ratio must be between 0 and 1");
            }
            if (brkgaMutantRatio < 0 || brkgaMutantRatio > 1) {
                throw new IllegalArgumentException("BRKGA mutant ratio must be between 0 and 1");
            }
            if (brkgaEliteRatio + brkgaMutantRatio > 1) {
                throw new IllegalArgumentException("BRKGA elite ratio + mutant ratio must not exceed 1");
            }
            if (brkgaInheritanceProbability < 0 || brkgaInheritanceProbability > 1) {
                throw new IllegalArgumentException("BRKGA inheritance probability must be between 0 and 1");
            }
            if (tabuListSize <= 0) {
                throw new IllegalArgumentException("Tabu list size must be positive");
            }
            if (tabuMaxIterations <= 0) {
                throw new IllegalArgumentException("Tabu max iterations must be positive");
            }
            if (archiveSize <= 0) {
                throw new IllegalArgumentException("Archive size must be positive");
            }
            
            return new OptimizationParameters(this);
        }
    }
} 