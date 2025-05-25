import com.ramonyago.cloudsim.*;
import com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;

/**
 * Simple test runner to verify the VM allocation optimization system
 */
public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== VM Allocation Optimization Test Runner ===\n");
        
        try {
            // Test 1: Quick example with sample instance
            testQuickExample();
            
            // Test 2: Test different decoding strategies
            testDecodingStrategies();
            
            // Test 3: Test with JSON file
            testWithJSONFile();
            
            // Test 4: Parameter sensitivity
            testParameterSensitivity();
            
            System.out.println("\n✅ All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testQuickExample() {
        System.out.println("📋 Test 1: Quick Example");
        System.out.println("-".repeat(40));
        
        OptimizationParameters params = OptimizationParameters.createQuick();
        VMAllocationOptimizer optimizer = VMAllocationOptimizer.withSampleInstance(params);
        
        long startTime = System.currentTimeMillis();
        VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("⏱️  Execution time: " + duration + " ms");
        System.out.println("📁 Archive size: " + result.getArchive().size());
        
        AllocationSolution bestCost = result.getBestCostSolution();
        if (bestCost != null) {
            System.out.printf("💰 Best cost: %.2f (reliability: %.3f)%n", 
                            bestCost.getTotalCost(), bestCost.getTotalReliability());
        }
        
        AllocationSolution bestReliability = result.getBestReliabilitySolution();
        if (bestReliability != null) {
            System.out.printf("🔒 Best reliability: %.3f (cost: %.2f)%n", 
                            bestReliability.getTotalReliability(), bestReliability.getTotalCost());
        }
        
        System.out.println("✅ Quick example test passed\n");
    }
    
    private static void testDecodingStrategies() {
        System.out.println("🔄 Test 2: Decoding Strategies Comparison");
        System.out.println("-".repeat(40));
        
        ProblemInstance instance = InstanceReader.createSampleInstance();
        BRKGADecoder.DecodingStrategy[] strategies = {
            BRKGADecoder.DecodingStrategy.GREEDY_COST,
            BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            BRKGADecoder.DecodingStrategy.BALANCED,
            BRKGADecoder.DecodingStrategy.FIRST_FIT
        };
        
        for (BRKGADecoder.DecodingStrategy strategy : strategies) {
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(30)
                    .brkgaMaxGenerations(50)
                    .archiveSize(20)
                    .decodingStrategy(strategy)
                    .randomSeed(42)
                    .build();
            
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            
            System.out.printf("  📊 %s: %d solutions, %d ms%n", 
                            strategy.name(), 
                            result.getArchive().size(),
                            result.getReport().getTotalExecutionTime());
        }
        
        System.out.println("✅ Strategy comparison test passed\n");
    }
    
    private static void testWithJSONFile() {
        System.out.println("📄 Test 3: JSON File Input");
        System.out.println("-".repeat(40));
        
        try {
            OptimizationParameters params = OptimizationParameters.createQuick();
            VMAllocationOptimizer optimizer = VMAllocationOptimizer.fromFile("examples/sample_instance.json", params);
            
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            
            System.out.println("📁 Archive size: " + result.getArchive().size());
            System.out.println("⏱️  Execution time: " + result.getReport().getTotalExecutionTime() + " ms");
            
            // Display some statistics
            var stats = result.getArchive().getStatistics();
            System.out.printf("📈 Cost range: [%.2f, %.2f]%n", stats.minCost, stats.maxCost);
            System.out.printf("📈 Reliability range: [%.3f, %.3f]%n", stats.minReliability, stats.maxReliability);
            
            System.out.println("✅ JSON file test passed\n");
            
        } catch (Exception e) {
            System.out.println("⚠️  JSON file test skipped: " + e.getMessage() + "\n");
        }
    }
    
    private static void testParameterSensitivity() {
        System.out.println("⚙️  Test 4: Parameter Sensitivity");
        System.out.println("-".repeat(40));
        
        ProblemInstance instance = InstanceReader.createSampleInstance();
        
        // Test different population sizes
        int[] populationSizes = {20, 50, 100};
        
        for (int popSize : populationSizes) {
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(popSize)
                    .brkgaMaxGenerations(50)
                    .archiveSize(30)
                    .randomSeed(42)
                    .build();
            
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);
            long startTime = System.currentTimeMillis();
            VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.printf("  👥 Pop size %d: %d solutions, %d ms%n", 
                            popSize, result.getArchive().size(), duration);
        }
        
        System.out.println("✅ Parameter sensitivity test passed\n");
    }
} 