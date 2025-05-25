package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.util.ParetoArchive;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Relatório detalhado da execução do algoritmo de otimização.
 */
public class ExecutionReport {
    private final ProblemInstance instance;
    private final LocalDateTime executionTime;
    private final long totalExecutionTime;
    private final int finalArchiveSize;
    private final ParetoArchive.ArchiveStatistics archiveStatistics;
    
    // Estatísticas do BRKGA
    private final long brkgaExecutionTime;
    private final int brkgaGenerations;
    private final List<Integer> brkgaArchiveSizeHistory;
    
    // Estatísticas da Busca Tabu (futuro)
    private final long tabuExecutionTime;
    private final int tabuIterations;
    
    // Estatísticas do MOILP (futuro)
    private final long moilpExecutionTime;
    private final boolean moilpOptimal;
    
    private ExecutionReport(Builder builder) {
        this.instance = builder.instance;
        this.executionTime = LocalDateTime.now();
        this.totalExecutionTime = builder.totalExecutionTime;
        this.finalArchiveSize = builder.finalArchiveSize;
        this.archiveStatistics = builder.archiveStatistics;
        
        this.brkgaExecutionTime = builder.brkgaExecutionTime;
        this.brkgaGenerations = builder.brkgaGenerations;
        this.brkgaArchiveSizeHistory = builder.brkgaArchiveSizeHistory;
        
        this.tabuExecutionTime = builder.tabuExecutionTime;
        this.tabuIterations = builder.tabuIterations;
        
        this.moilpExecutionTime = builder.moilpExecutionTime;
        this.moilpOptimal = builder.moilpOptimal;
    }
    
    // Getters
    public ProblemInstance getInstance() { return instance; }
    public LocalDateTime getExecutionTime() { return executionTime; }
    public long getTotalExecutionTime() { return totalExecutionTime; }
    public int getFinalArchiveSize() { return finalArchiveSize; }
    public ParetoArchive.ArchiveStatistics getArchiveStatistics() { return archiveStatistics; }
    
    public long getBrkgaExecutionTime() { return brkgaExecutionTime; }
    public int getBrkgaGenerations() { return brkgaGenerations; }
    public List<Integer> getBrkgaArchiveSizeHistory() { return brkgaArchiveSizeHistory; }
    
    public long getTabuExecutionTime() { return tabuExecutionTime; }
    public int getTabuIterations() { return tabuIterations; }
    
    public long getMoilpExecutionTime() { return moilpExecutionTime; }
    public boolean isMoilpOptimal() { return moilpOptimal; }
    
    /**
     * Gera um relatório em formato texto
     */
    public String generateTextReport() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=".repeat(80)).append("\n");
        sb.append("               VM ALLOCATION OPTIMIZATION REPORT\n");
        sb.append("=".repeat(80)).append("\n");
        
        // Informações gerais
        sb.append("Execution Date: ").append(executionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Instance: ").append(instance.getInstanceName()).append("\n");
        sb.append("Total Execution Time: ").append(formatTime(totalExecutionTime)).append("\n");
        sb.append("\n");
        
        // Estatísticas da instância
        sb.append("INSTANCE STATISTICS:\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append("VMs: ").append(instance.getVMCount()).append("\n");
        sb.append("Hosts: ").append(instance.getHostCount()).append("\n");
        ProblemInstance.InstanceStatistics stats = instance.getStatistics();
        sb.append("Avg VM Reliability Requirement: ").append(String.format("%.3f", stats.avgVmReliabilityRequirement)).append("\n");
        sb.append("Avg Host Failure Probability: ").append(String.format("%.3f", stats.avgHostFailureProbability)).append("\n");
        sb.append("Total Host Activation Cost: ").append(String.format("%.2f", stats.totalHostCost)).append("\n");
        sb.append("\n");
        
        // Resultados do BRKGA
        sb.append("BRKGA RESULTS:\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append("Execution Time: ").append(formatTime(brkgaExecutionTime)).append("\n");
        sb.append("Generations: ").append(brkgaGenerations).append("\n");
        if (brkgaArchiveSizeHistory != null && !brkgaArchiveSizeHistory.isEmpty()) {
            sb.append("Archive Size Evolution: ").append(brkgaArchiveSizeHistory.size()).append(" checkpoints\n");
            sb.append("Final Archive Size: ").append(brkgaArchiveSizeHistory.get(brkgaArchiveSizeHistory.size() - 1)).append("\n");
        }
        sb.append("\n");
        
        // Resultados finais
        sb.append("FINAL RESULTS:\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append("Archive Size: ").append(finalArchiveSize).append("\n");
        if (archiveStatistics != null) {
            sb.append("Cost Range: [").append(String.format("%.2f", archiveStatistics.minCost))
              .append(", ").append(String.format("%.2f", archiveStatistics.maxCost)).append("]\n");
            sb.append("Reliability Range: [").append(String.format("%.3f", archiveStatistics.minReliability))
              .append(", ").append(String.format("%.3f", archiveStatistics.maxReliability)).append("]\n");
            sb.append("Average Cost: ").append(String.format("%.2f", archiveStatistics.avgCost)).append("\n");
            sb.append("Average Reliability: ").append(String.format("%.3f", archiveStatistics.avgReliability)).append("\n");
        }
        sb.append("\n");
        
        // Performance metrics
        sb.append("PERFORMANCE METRICS:\n");
        sb.append("-".repeat(40)).append("\n");
        double solutionsPerSecond = finalArchiveSize / (totalExecutionTime / 1000.0);
        sb.append("Solutions per Second: ").append(String.format("%.2f", solutionsPerSecond)).append("\n");
        
        if (brkgaExecutionTime > 0) {
            double brkgaEfficiency = (double) brkgaGenerations / (brkgaExecutionTime / 1000.0);
            sb.append("BRKGA Generations per Second: ").append(String.format("%.2f", brkgaEfficiency)).append("\n");
        }
        
        sb.append("\n");
        sb.append("=".repeat(80)).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Gera um relatório em formato CSV
     */
    public String generateCSVReport() {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("metric,value\n");
        
        // Dados básicos
        sb.append("instance_name,").append(instance.getInstanceName()).append("\n");
        sb.append("execution_time_ms,").append(totalExecutionTime).append("\n");
        sb.append("vm_count,").append(instance.getVMCount()).append("\n");
        sb.append("host_count,").append(instance.getHostCount()).append("\n");
        sb.append("final_archive_size,").append(finalArchiveSize).append("\n");
        
        // BRKGA
        sb.append("brkga_execution_time_ms,").append(brkgaExecutionTime).append("\n");
        sb.append("brkga_generations,").append(brkgaGenerations).append("\n");
        
        // Estatísticas do arquivo final
        if (archiveStatistics != null) {
            sb.append("min_cost,").append(archiveStatistics.minCost).append("\n");
            sb.append("max_cost,").append(archiveStatistics.maxCost).append("\n");
            sb.append("avg_cost,").append(archiveStatistics.avgCost).append("\n");
            sb.append("min_reliability,").append(archiveStatistics.minReliability).append("\n");
            sb.append("max_reliability,").append(archiveStatistics.maxReliability).append("\n");
            sb.append("avg_reliability,").append(archiveStatistics.avgReliability).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Gera um resumo conciso
     */
    public String generateSummary() {
        return String.format("Report{instance=%s, time=%s, archive=%d, cost=[%.2f-%.2f], reliability=[%.3f-%.3f]}",
                           instance.getInstanceName(),
                           formatTime(totalExecutionTime),
                           finalArchiveSize,
                           archiveStatistics != null ? archiveStatistics.minCost : 0.0,
                           archiveStatistics != null ? archiveStatistics.maxCost : 0.0,
                           archiveStatistics != null ? archiveStatistics.minReliability : 0.0,
                           archiveStatistics != null ? archiveStatistics.maxReliability : 0.0);
    }
    
    private String formatTime(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + " ms";
        } else if (milliseconds < 60000) {
            return String.format("%.2f s", milliseconds / 1000.0);
        } else {
            return String.format("%.2f min", milliseconds / 60000.0);
        }
    }
    
    @Override
    public String toString() {
        return generateSummary();
    }
    
    /**
     * Builder para criar relatório de execução
     */
    public static class Builder {
        private ProblemInstance instance;
        private long totalExecutionTime;
        private int finalArchiveSize;
        private ParetoArchive.ArchiveStatistics archiveStatistics;
        
        private long brkgaExecutionTime = 0;
        private int brkgaGenerations = 0;
        private List<Integer> brkgaArchiveSizeHistory;
        
        private long tabuExecutionTime = 0;
        private int tabuIterations = 0;
        
        private long moilpExecutionTime = 0;
        private boolean moilpOptimal = false;
        
        public Builder instance(ProblemInstance instance) {
            this.instance = instance;
            return this;
        }
        
        public Builder totalExecutionTime(long totalExecutionTime) {
            this.totalExecutionTime = totalExecutionTime;
            return this;
        }
        
        public Builder finalArchiveSize(int finalArchiveSize) {
            this.finalArchiveSize = finalArchiveSize;
            return this;
        }
        
        public Builder archiveStatistics(ParetoArchive.ArchiveStatistics archiveStatistics) {
            this.archiveStatistics = archiveStatistics;
            return this;
        }
        
        public Builder brkgaExecutionTime(long brkgaExecutionTime) {
            this.brkgaExecutionTime = brkgaExecutionTime;
            return this;
        }
        
        public Builder brkgaGenerations(int brkgaGenerations) {
            this.brkgaGenerations = brkgaGenerations;
            return this;
        }
        
        public Builder brkgaArchiveSizeHistory(List<Integer> brkgaArchiveSizeHistory) {
            this.brkgaArchiveSizeHistory = brkgaArchiveSizeHistory;
            return this;
        }
        
        public Builder tabuExecutionTime(long tabuExecutionTime) {
            this.tabuExecutionTime = tabuExecutionTime;
            return this;
        }
        
        public Builder tabuIterations(int tabuIterations) {
            this.tabuIterations = tabuIterations;
            return this;
        }
        
        public Builder moilpExecutionTime(long moilpExecutionTime) {
            this.moilpExecutionTime = moilpExecutionTime;
            return this;
        }
        
        public Builder moilpOptimal(boolean moilpOptimal) {
            this.moilpOptimal = moilpOptimal;
            return this;
        }
        
        public ExecutionReport build() {
            return new ExecutionReport(this);
        }
    }
} 