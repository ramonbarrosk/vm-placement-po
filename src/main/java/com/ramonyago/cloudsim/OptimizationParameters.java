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
    
    // Parâmetros da Busca Tabu (para implementação futura)
    private final int tabuListSize;
    private final int tabuMaxIterations;
    private final boolean tabuUseIntensification;
    private final boolean tabuUseDiversification;
    
    // Parâmetros do MOILP (para implementação futura)
    private final double moilpTimeLimit;
    private final boolean useMOILPRefinement;
    private final String solverType;
    
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
        
        this.moilpTimeLimit = builder.moilpTimeLimit;
        this.useMOILPRefinement = builder.useMOILPRefinement;
        this.solverType = builder.solverType;
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
    
    public double getMoilpTimeLimit() { return moilpTimeLimit; }
    public boolean isUseMOILPRefinement() { return useMOILPRefinement; }
    public String getSolverType() { return solverType; }
    
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
                           "MOILP(time=%.1f, solver=%s), " +
                           "archive=%d, seed=%d}",
                           brkgaPopulationSize, brkgaMaxGenerations, brkgaEliteRatio, brkgaMutantRatio,
                           tabuListSize, tabuMaxIterations,
                           moilpTimeLimit, solverType,
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
        
        private double moilpTimeLimit = 300.0; // 5 minutos
        private boolean useMOILPRefinement = false;
        private String solverType = "GLPK";
        
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
        
        public Builder moilpTimeLimit(double moilpTimeLimit) {
            this.moilpTimeLimit = moilpTimeLimit;
            return this;
        }
        
        public Builder useMOILPRefinement(boolean useMOILPRefinement) {
            this.useMOILPRefinement = useMOILPRefinement;
            return this;
        }
        
        public Builder solverType(String solverType) {
            this.solverType = solverType;
            return this;
        }
        
        public OptimizationParameters build() {
            return new OptimizationParameters(this);
        }
    }
} 