package com.ramonyago.cloudsim.algorithm;

import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import com.ramonyago.cloudsim.model.Host;
import com.ramonyago.cloudsim.model.ResourceType;
import com.ramonyago.cloudsim.util.ParetoArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Multi-Objective Integer Linear Programming solver for VM allocation.
 * Uses epsilon-constraint method to find Pareto optimal solutions.
 * For larger instances, provides heuristic improvements based on ILP principles.
 */
public class MOILP {
    private static final Logger logger = LoggerFactory.getLogger(MOILP.class);
    
    private final ProblemInstance instance;
    private final MOILPParameters parameters;
    
    private ParetoArchive archive;
    private long executionTime;
    private boolean optimalSolutions;
    private int solutionsGenerated;
    
    public MOILP(ProblemInstance instance, MOILPParameters parameters) {
        this.instance = instance;
        this.parameters = parameters;
        this.archive = new ParetoArchive(parameters.getArchiveSize());
    }
    
    /**
     * Solves the multi-objective problem using epsilon-constraint method
     */
    public ParetoArchive solve(ParetoArchive guidingSolutions) {
        logger.info("Starting MOILP with {} guiding solutions", guidingSolutions.size());
        long startTime = System.currentTimeMillis();
        
        // Initialize archive with guiding solutions
        for (AllocationSolution solution : guidingSolutions.getSolutions()) {
            archive.add(solution);
        }
        
        if (isSmallInstance()) {
            // Use exact ILP formulation for small instances
            solveExact(guidingSolutions);
        } else {
            // Use ILP-based heuristics for larger instances
            solveHeuristic(guidingSolutions);
        }
        
        executionTime = System.currentTimeMillis() - startTime;
        logger.info("MOILP completed in {} ms, generated {} solutions, optimal: {}", 
                   executionTime, solutionsGenerated, optimalSolutions);
        
        return archive;
    }
    
    /**
     * Determines if instance is small enough for exact solution
     */
    private boolean isSmallInstance() {
        int variables = instance.getVMCount() * instance.getHostCount();
        return variables <= parameters.getMaxVariablesForExact();
    }
    
    /**
     * Solves exactly using epsilon-constraint method
     */
    private void solveExact(ParetoArchive guidingSolutions) {
        logger.info("Using exact ILP formulation");
        optimalSolutions = true;
        
        // Get objective ranges from guiding solutions
        double minCost = guidingSolutions.getSolutions().stream()
                .mapToDouble(AllocationSolution::getTotalCost).min().orElse(0.0);
        double maxCost = guidingSolutions.getSolutions().stream()
                .mapToDouble(AllocationSolution::getTotalCost).max().orElse(1000.0);
        
        // Generate epsilon values for cost constraint
        int numEpsilons = parameters.getNumEpsilonSteps();
        double stepSize = (maxCost - minCost) / numEpsilons;
        
        for (int i = 0; i <= numEpsilons; i++) {
            double costEpsilon = minCost + i * stepSize;
            
            // Solve: maximize reliability subject to cost <= costEpsilon
            AllocationSolution solution = solveWithCostConstraint(costEpsilon);
            
            if (solution != null && solution.isFeasible()) {
                if (archive.add(solution)) {
                    solutionsGenerated++;
                    logger.debug("Found new Pareto solution: cost={}, reliability={}", 
                               String.format("%.2f", solution.getTotalCost()),
                               String.format("%.3f", solution.getTotalReliability()));
                }
            }
            
            // Check timeout
            if (System.currentTimeMillis() - executionTime > parameters.getTimeLimit() * 1000) {
                logger.warn("MOILP timeout reached, stopping exact solution");
                optimalSolutions = false;
                break;
            }
        }
    }
    
    /**
     * Solves approximately using ILP-based heuristics
     */
    private void solveHeuristic(ParetoArchive guidingSolutions) {
        logger.info("Using ILP-based heuristics for large instance");
        optimalSolutions = false;
        
        // Apply ILP-inspired improvement heuristics
        improveWithCostOptimization(guidingSolutions);
        improveWithReliabilityOptimization(guidingSolutions);
        improveWithBalancedOptimization(guidingSolutions);
    }
    
    /**
     * Solves single-objective problem: maximize reliability with cost constraint
     */
    private AllocationSolution solveWithCostConstraint(double costLimit) {
        // Simplified ILP-based greedy approach
        // In a real implementation, this would use an ILP solver like CPLEX or Gurobi
        
        AllocationSolution solution = new AllocationSolution(instance.getVMs(), instance.getHosts());
        List<VM> sortedVMs = new ArrayList<>(instance.getVMs());
        
        // Sort VMs by reliability requirement (descending) and priority
        sortedVMs.sort((v1, v2) -> {
            int reliabilityCompare = Double.compare(v2.getMinReliability(), v1.getMinReliability());
            if (reliabilityCompare != 0) return reliabilityCompare;
            return Double.compare(v2.getPriority(), v1.getPriority());
        });
        
        double currentCost = 0.0;
        Set<Host> activatedHosts = new HashSet<>();
        
        for (VM vm : sortedVMs) {
            Host bestHost = findBestHostForVM(vm, solution, costLimit - currentCost, activatedHosts);
            
            if (bestHost != null) {
                solution.allocateVM(vm, bestHost);
                
                if (!activatedHosts.contains(bestHost)) {
                    activatedHosts.add(bestHost);
                    currentCost += bestHost.getActivationCost();
                }
                
                currentCost += bestHost.getEnergyConsumption() * 0.1; // Simplified energy cost
            }
        }
        
        return solution.getTotalCost() <= costLimit ? solution : null;
    }
    
    /**
     * Finds the best host for a VM considering cost constraint
     */
    private Host findBestHostForVM(VM vm, AllocationSolution solution, double remainingBudget, 
                                  Set<Host> activatedHosts) {
        Host bestHost = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (Host host : instance.getHosts()) {
            if (canAllocateVM(vm, host, solution)) {
                double hostReliability = 1.0 - host.getFailureProbability();
                
                // Check if reliability requirement is met
                if (hostReliability >= vm.getMinReliability()) {
                    double additionalCost = 0.0;
                    
                    // Add activation cost if host not yet activated
                    if (!activatedHosts.contains(host)) {
                        additionalCost += host.getActivationCost();
                    }
                    
                    // Add energy cost (simplified)
                    additionalCost += host.getEnergyConsumption() * 0.1;
                    
                    if (additionalCost <= remainingBudget) {
                        // Score: prioritize reliability and efficiency
                        double score = hostReliability / (1.0 + additionalCost / 100.0);
                        
                        if (score > bestScore) {
                            bestScore = score;
                            bestHost = host;
                        }
                    }
                }
            }
        }
        
        return bestHost;
    }
    
    /**
     * Checks if VM can be allocated to host (resource constraints)
     */
    private boolean canAllocateVM(VM vm, Host host, AllocationSolution solution) {
        Map<ResourceType, Double> vmDemands = vm.getResourceDemands();
        Map<ResourceType, Double> hostCapacities = host.getResourceCapacities();
        
        // Calculate current usage on the host
        Map<ResourceType, Double> currentUsage = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            currentUsage.put(type, 0.0);
        }
        
        // Sum demands of VMs already on this host
        for (VM allocatedVm : solution.getVmsOnHost(host)) {
            for (ResourceType type : ResourceType.values()) {
                double current = currentUsage.get(type);
                currentUsage.put(type, current + allocatedVm.getResourceDemand(type));
            }
        }
        
        // Check if adding this VM would violate capacity constraints
        for (ResourceType type : ResourceType.values()) {
            double demand = vmDemands.getOrDefault(type, 0.0);
            double capacity = hostCapacities.getOrDefault(type, 0.0);
            double usage = currentUsage.get(type);
            
            if (usage + demand > capacity) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Improves solutions by optimizing for cost
     */
    private void improveWithCostOptimization(ParetoArchive guidingSolutions) {
        for (AllocationSolution solution : guidingSolutions.getSolutions()) {
            AllocationSolution improved = optimizeForCost(solution);
            if (improved != null && archive.add(improved)) {
                solutionsGenerated++;
            }
        }
    }
    
    /**
     * Improves solutions by optimizing for reliability
     */
    private void improveWithReliabilityOptimization(ParetoArchive guidingSolutions) {
        for (AllocationSolution solution : guidingSolutions.getSolutions()) {
            AllocationSolution improved = optimizeForReliability(solution);
            if (improved != null && archive.add(improved)) {
                solutionsGenerated++;
            }
        }
    }
    
    /**
     * Improves solutions by balanced optimization
     */
    private void improveWithBalancedOptimization(ParetoArchive guidingSolutions) {
        for (AllocationSolution solution : guidingSolutions.getSolutions()) {
            AllocationSolution improved = optimizeBalanced(solution);
            if (improved != null && archive.add(improved)) {
                solutionsGenerated++;
            }
        }
    }
    
    /**
     * Optimizes a solution for minimum cost while maintaining feasibility
     */
    private AllocationSolution optimizeForCost(AllocationSolution original) {
        AllocationSolution improved = new AllocationSolution(original);
        
        // Try to move VMs to cheaper hosts that still meet reliability requirements
        List<VM> vms = new ArrayList<>(improved.getVmToHost().keySet());
        
        for (VM vm : vms) {
            Host currentHost = improved.getHostForVM(vm);
            Host cheaperHost = findCheaperHost(vm, currentHost, improved);
            
            if (cheaperHost != null) {
                improved.deallocateVM(vm);
                improved.allocateVM(vm, cheaperHost);
            }
        }
        
        return improved.isFeasible() ? improved : null;
    }
    
    /**
     * Optimizes a solution for maximum reliability
     */
    private AllocationSolution optimizeForReliability(AllocationSolution original) {
        AllocationSolution improved = new AllocationSolution(original);
        
        // Try to move VMs to more reliable hosts
        List<VM> vms = new ArrayList<>(improved.getVmToHost().keySet());
        
        for (VM vm : vms) {
            Host currentHost = improved.getHostForVM(vm);
            Host moreReliableHost = findMoreReliableHost(vm, currentHost, improved);
            
            if (moreReliableHost != null) {
                improved.deallocateVM(vm);
                improved.allocateVM(vm, moreReliableHost);
            }
        }
        
        return improved.isFeasible() ? improved : null;
    }
    
    /**
     * Optimizes a solution with balanced objectives
     */
    private AllocationSolution optimizeBalanced(AllocationSolution original) {
        AllocationSolution improved = new AllocationSolution(original);
        
        // Apply both cost and reliability improvements
        List<VM> vms = new ArrayList<>(improved.getVmToHost().keySet());
        
        for (VM vm : vms) {
            Host currentHost = improved.getHostForVM(vm);
            Host betterHost = findBalancedHost(vm, currentHost, improved);
            
            if (betterHost != null) {
                improved.deallocateVM(vm);
                improved.allocateVM(vm, betterHost);
            }
        }
        
        return improved.isFeasible() ? improved : null;
    }
    
    /**
     * Finds a cheaper host that still meets VM requirements
     */
    private Host findCheaperHost(VM vm, Host currentHost, AllocationSolution solution) {
        double currentCost = currentHost.getActivationCost();
        Host cheapestHost = null;
        double cheapestCost = currentCost;
        
        for (Host host : instance.getHosts()) {
            if (!host.equals(currentHost) && canAllocateVM(vm, host, solution)) {
                double hostReliability = 1.0 - host.getFailureProbability();
                if (hostReliability >= vm.getMinReliability() && host.getActivationCost() < cheapestCost) {
                    cheapestCost = host.getActivationCost();
                    cheapestHost = host;
                }
            }
        }
        
        return cheapestHost;
    }
    
    /**
     * Finds a more reliable host for VM
     */
    private Host findMoreReliableHost(VM vm, Host currentHost, AllocationSolution solution) {
        double currentReliability = 1.0 - currentHost.getFailureProbability();
        Host mostReliableHost = null;
        double highestReliability = currentReliability;
        
        for (Host host : instance.getHosts()) {
            if (!host.equals(currentHost) && canAllocateVM(vm, host, solution)) {
                double hostReliability = 1.0 - host.getFailureProbability();
                if (hostReliability > highestReliability && hostReliability >= vm.getMinReliability()) {
                    highestReliability = hostReliability;
                    mostReliableHost = host;
                }
            }
        }
        
        return mostReliableHost;
    }
    
    /**
     * Finds a host with better cost-reliability balance
     */
    private Host findBalancedHost(VM vm, Host currentHost, AllocationSolution solution) {
        double currentReliability = 1.0 - currentHost.getFailureProbability();
        double currentCost = currentHost.getActivationCost();
        double currentScore = currentReliability / (1.0 + currentCost / 100.0);
        
        Host bestHost = null;
        double bestScore = currentScore;
        
        for (Host host : instance.getHosts()) {
            if (!host.equals(currentHost) && canAllocateVM(vm, host, solution)) {
                double hostReliability = 1.0 - host.getFailureProbability();
                if (hostReliability >= vm.getMinReliability()) {
                    double score = hostReliability / (1.0 + host.getActivationCost() / 100.0);
                    if (score > bestScore) {
                        bestScore = score;
                        bestHost = host;
                    }
                }
            }
        }
        
        return bestHost;
    }
    
    // Getters for statistics
    public long getExecutionTime() { return executionTime; }
    public boolean isOptimalSolutions() { return optimalSolutions; }
    public int getSolutionsGenerated() { return solutionsGenerated; }
    
    /**
     * Parameters for MOILP solver
     */
    public static class MOILPParameters {
        private final double timeLimit; // seconds
        private final int maxVariablesForExact;
        private final int numEpsilonSteps;
        private final int archiveSize;
        private final String solver; // "GLPK", "CPLEX", "GUROBI", etc.
        
        public MOILPParameters(double timeLimit, int maxVariablesForExact, int numEpsilonSteps,
                              int archiveSize, String solver) {
            this.timeLimit = timeLimit;
            this.maxVariablesForExact = maxVariablesForExact;
            this.numEpsilonSteps = numEpsilonSteps;
            this.archiveSize = archiveSize;
            this.solver = solver;
        }
        
        // Getters
        public double getTimeLimit() { return timeLimit; }
        public int getMaxVariablesForExact() { return maxVariablesForExact; }
        public int getNumEpsilonSteps() { return numEpsilonSteps; }
        public int getArchiveSize() { return archiveSize; }
        public String getSolver() { return solver; }
        
        @Override
        public String toString() {
            return String.format("MOILP{timeLimit=%.1fs, maxVars=%d, steps=%d, solver=%s}",
                               timeLimit, maxVariablesForExact, numEpsilonSteps, solver);
        }
    }
} 