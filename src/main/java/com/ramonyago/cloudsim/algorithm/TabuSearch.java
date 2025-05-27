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
 * Multi-Objective Tabu Search for VM allocation optimization.
 * Improves upon initial solutions by exploring neighborhoods through VM moves.
 */
public class TabuSearch {
    private static final Logger logger = LoggerFactory.getLogger(TabuSearch.class);
    
    private final ProblemInstance instance;
    private final TabuParameters parameters;
    private final Random random;
    
    private Set<TabuMove> tabuList;
    private ParetoArchive archive;
    private AllocationSolution currentSolution;
    
    // Execution statistics
    private long executionTime;
    private int iterations;
    private int improvementCount;
    
    public TabuSearch(ProblemInstance instance, TabuParameters parameters) {
        this.instance = instance;
        this.parameters = parameters;
        this.random = new Random(parameters.getRandomSeed());
        this.tabuList = new LinkedHashSet<>();
        this.archive = new ParetoArchive(parameters.getArchiveSize());
    }
    
    /**
     * Executes tabu search starting from initial solutions
     */
    public ParetoArchive run(ParetoArchive initialSolutions) {
        logger.info("Starting Tabu Search with {} initial solutions", initialSolutions.size());
        long startTime = System.currentTimeMillis();
        
        // Initialize archive with initial solutions
        for (AllocationSolution solution : initialSolutions.getSolutions()) {
            archive.add(solution);
        }
        
        // Run tabu search from each non-dominated solution
        for (AllocationSolution solution : initialSolutions.getSolutions()) {
            runFromSolution(solution);
        }
        
        executionTime = System.currentTimeMillis() - startTime;
        logger.info("Tabu Search completed in {} ms, {} iterations, {} improvements", 
                   executionTime, iterations, improvementCount);
        
        return archive;
    }
    
    /**
     * Runs tabu search starting from a specific solution
     */
    private void runFromSolution(AllocationSolution initialSolution) {
        currentSolution = new AllocationSolution(initialSolution);
        tabuList.clear();
        
        for (int iter = 0; iter < parameters.getMaxIterations(); iter++) {
            iterations++;
            
            // Generate neighborhood
            List<TabuMove> neighborhood = generateNeighborhood(currentSolution);
            
            // Find best non-tabu move
            TabuMove bestMove = selectBestMove(neighborhood);
            
            if (bestMove != null) {
                // Apply move
                AllocationSolution newSolution = applyMove(currentSolution, bestMove);
                
                // Check if it improves the archive
                if (archive.add(newSolution)) {
                    improvementCount++;
                    logger.debug("Iteration {}: Found improving solution (cost: {}, reliability: {})",
                               iter, String.format("%.2f", newSolution.getTotalCost()),
                               String.format("%.3f", newSolution.getTotalReliability()));
                }
                
                // Update current solution
                currentSolution = newSolution;
                
                // Update tabu list
                updateTabuList(bestMove);
            }
            
            // Diversification if no improvement for too long
            if (iter % parameters.getDiversificationFrequency() == 0 && iter > 0) {
                diversify();
            }
        }
    }
    
    /**
     * Generates neighborhood by considering VM moves
     */
    private List<TabuMove> generateNeighborhood(AllocationSolution solution) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Try moving each VM to different hosts
        for (VM vm : solution.getVmToHost().keySet()) {
            Host currentHost = solution.getHostForVM(vm);
            
            for (Host targetHost : instance.getHosts()) {
                if (!targetHost.equals(currentHost)) {
                    // Check if move is feasible
                    if (isMoveValid(solution, vm, targetHost)) {
                        moves.add(new TabuMove(vm, currentHost, targetHost));
                    }
                }
            }
            
            // Also consider removing VM from current host (if not required)
            if (canRemoveVM(solution, vm)) {
                moves.add(new TabuMove(vm, currentHost, null));
            }
        }
        
        // Try adding unallocated VMs to available hosts
        for (VM vm : instance.getVMs()) {
            if (solution.getHostForVM(vm) == null) {
                for (Host host : instance.getHosts()) {
                    if (isMoveValid(solution, vm, host)) {
                        moves.add(new TabuMove(vm, null, host));
                    }
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Selects the best non-tabu move from neighborhood
     */
    private TabuMove selectBestMove(List<TabuMove> neighborhood) {
        TabuMove bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (TabuMove move : neighborhood) {
            if (!isTabu(move) || satisfiesAspirationCriterion(move)) {
                double score = evaluateMove(move);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }
        
        return bestMove;
    }
    
    /**
     * Checks if a move is valid (resource constraints, reliability requirements)
     */
    private boolean isMoveValid(AllocationSolution solution, VM vm, Host targetHost) {
        if (targetHost == null) return true; // Removal is always valid
        
        // Check resource constraints using the correct API
        Map<ResourceType, Double> vmDemands = vm.getDemands();
        Map<ResourceType, Double> hostCapacities = targetHost.getCaps();
        
        // Calculate current usage on the target host
        Map<ResourceType, Double> currentUsage = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            currentUsage.put(type, 0.0);
        }
        
        // Sum demands of VMs already on this host
        for (VM allocatedVm : solution.getVmsOnHost(targetHost)) {
            for (ResourceType type : ResourceType.values()) {
                double current = currentUsage.get(type);
                currentUsage.put(type, current + allocatedVm.getDemand(type));
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
        
        // Check reliability requirement
        double hostReliability = 1.0 - targetHost.getFailProb();
        return hostReliability >= vm.getMinRel();
    }
    
    /**
     * Checks if a VM can be removed (not critical)
     */
    private boolean canRemoveVM(AllocationSolution solution, VM vm) {
        // For now, assume all VMs should be allocated
        // This could be enhanced with priority-based logic
        return vm.getPrio() < 1.0;
    }
    
    /**
     * Applies a move to create a new solution
     */
    private AllocationSolution applyMove(AllocationSolution solution, TabuMove move) {
        AllocationSolution newSolution = new AllocationSolution(solution);
        
        VM vm = move.getVm();
        Host newHost = move.getToHost();
        
        // Remove from current host
        if (move.getFromHost() != null) {
            newSolution.deallocateVM(vm);
        }
        
        // Add to new host
        if (newHost != null) {
            newSolution.allocateVM(vm, newHost);
        }
        
        return newSolution;
    }
    
    /**
     * Evaluates the quality of a move using weighted objectives
     */
    private double evaluateMove(TabuMove move) {
        AllocationSolution testSolution = applyMove(currentSolution, move);
        
        // Normalize objectives
        double costScore = -testSolution.getTotalCost() / 1000.0; // Minimize cost
        double reliabilityScore = testSolution.getTotalReliability(); // Maximize reliability
        
        // Weighted combination
        return parameters.getCostWeight() * costScore + 
               parameters.getReliabilityWeight() * reliabilityScore;
    }
    
    /**
     * Checks if a move is in the tabu list
     */
    private boolean isTabu(TabuMove move) {
        return tabuList.contains(move);
    }
    
    /**
     * Checks aspiration criterion (override tabu if solution is very good)
     */
    private boolean satisfiesAspirationCriterion(TabuMove move) {
        AllocationSolution testSolution = applyMove(currentSolution, move);
        
        // Check if it dominates current best solutions
        for (AllocationSolution archiveSolution : archive.getSolutions()) {
            if (testSolution.compareDominance(archiveSolution) < 0) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Updates the tabu list with the applied move
     */
    private void updateTabuList(TabuMove move) {
        tabuList.add(move);
        
        // Remove oldest move if tabu list is full
        if (tabuList.size() > parameters.getTabuListSize()) {
            Iterator<TabuMove> iterator = tabuList.iterator();
            iterator.next();
            iterator.remove();
        }
    }
    
    /**
     * Diversification strategy when search stagnates
     */
    private void diversify() {
        logger.debug("Applying diversification strategy");
        
        // Randomly move some VMs to different hosts
        List<VM> allocatedVMs = new ArrayList<>(currentSolution.getVmToHost().keySet());
        int movesToMake = Math.min(3, allocatedVMs.size());
        
        Collections.shuffle(allocatedVMs, random);
        
        for (int i = 0; i < movesToMake; i++) {
            VM vm = allocatedVMs.get(i);
            List<Host> availableHosts = new ArrayList<>(instance.getHosts());
            availableHosts.remove(currentSolution.getHostForVM(vm));
            
            if (!availableHosts.isEmpty()) {
                Host newHost = availableHosts.get(random.nextInt(availableHosts.size()));
                if (isMoveValid(currentSolution, vm, newHost)) {
                    TabuMove move = new TabuMove(vm, currentSolution.getHostForVM(vm), newHost);
                    currentSolution = applyMove(currentSolution, move);
                }
            }
        }
    }
    
    // Getters for statistics
    public long getExecutionTime() { return executionTime; }
    public int getIterations() { return iterations; }
    public int getImprovementCount() { return improvementCount; }
    
    /**
     * Parameters for Tabu Search
     */
    public static class TabuParameters {
        private final int maxIterations;
        private final int tabuListSize;
        private final int diversificationFrequency;
        private final int archiveSize;
        private final double costWeight;
        private final double reliabilityWeight;
        private final long randomSeed;
        
        public TabuParameters(int maxIterations, int tabuListSize, int diversificationFrequency,
                             int archiveSize, double costWeight, double reliabilityWeight, long randomSeed) {
            this.maxIterations = maxIterations;
            this.tabuListSize = tabuListSize;
            this.diversificationFrequency = diversificationFrequency;
            this.archiveSize = archiveSize;
            this.costWeight = costWeight;
            this.reliabilityWeight = reliabilityWeight;
            this.randomSeed = randomSeed;
        }
        
        // Getters
        public int getMaxIterations() { return maxIterations; }
        public int getTabuListSize() { return tabuListSize; }
        public int getDiversificationFrequency() { return diversificationFrequency; }
        public int getArchiveSize() { return archiveSize; }
        public double getCostWeight() { return costWeight; }
        public double getReliabilityWeight() { return reliabilityWeight; }
        public long getRandomSeed() { return randomSeed; }
    }
    
    /**
     * Represents a move in the search space
     */
    private static class TabuMove {
        private final VM vm;
        private final Host fromHost;
        private final Host toHost;
        
        public TabuMove(VM vm, Host fromHost, Host toHost) {
            this.vm = vm;
            this.fromHost = fromHost;
            this.toHost = toHost;
        }
        
        public VM getVm() { return vm; }
        public Host getFromHost() { return fromHost; }
        public Host getToHost() { return toHost; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TabuMove tabuMove = (TabuMove) obj;
            return Objects.equals(vm, tabuMove.vm) &&
                   Objects.equals(fromHost, tabuMove.fromHost) &&
                   Objects.equals(toHost, tabuMove.toHost);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(vm, fromHost, toHost);
        }
        
        @Override
        public String toString() {
            return String.format("Move VM%d from Host%s to Host%s", 
                               vm.getVmId(), 
                               fromHost != null ? fromHost.getHostId() : "null",
                               toHost != null ? toHost.getHostId() : "null");
        }
    }
} 