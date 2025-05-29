package com.ramonyago.cloudsim;

import com.ramonyago.cloudsim.algorithm.brkga.BRKGADecoder;
import com.ramonyago.cloudsim.io.InstanceReader;
import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.model.VM;
import com.ramonyago.cloudsim.model.Host;
import com.ramonyago.cloudsim.model.ResourceType;
import com.ramonyago.cloudsim.simulation.SimulationIntegrator;
import com.ramonyago.cloudsim.simulation.CloudSimSimulator;
import com.ramonyago.cloudsim.simulation.CloudSimRealTimeAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Main app - quick demo of VM allocation system with CloudSim integration
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        log.info("=== VM Allocation Optimization System with CloudSim ===");
        log.info("Starting demo...");
        
        try {
            // quick example with sample data
            runQuickDemo();
            
            // CloudSim integration demo
            runCloudSimDemo();
            
            // NEW: Real-time allocation demo
            runRealTimeAllocationDemo();
            
            // file input if provided
            if (args.length > 0) {
                runFromFile(args[0]);
            }
            
            // Test with real instance files
            testRealInstances();
            
            // compare different strategies
            compareStrats();
            
            // integrated optimization and simulation
            runIntegratedDemo();
            
        } catch (Exception e) {
            log.error("Error during execution", e);
            System.exit(1);
        }
        
        log.info("Demo completed!");
    }
    
    // quick demo with default instance
    private static void runQuickDemo() {
        log.info("\n" + "=".repeat(60));
        log.info("QUICK DEMO - Sample Instance");
        log.info("=".repeat(60));
        
        // quick params
        OptimizationParameters params = OptimizationParameters.createQuick();
        log.info("Params: {}", params);
        
        // create optimizer with sample data
        VMAllocationOptimizer opt = VMAllocationOptimizer.withSampleInstance(params);
        
        // run optimization
        VMAllocationOptimizer.OptimizationResult result = opt.optimize();
        
        // show results
        showResults(result);
    }
    
    // CloudSim integration demo
    private static void runCloudSimDemo() {
        log.info("\n" + "=".repeat(60));
        log.info("CLOUDSIM INTEGRATION DEMO");
        log.info("=".repeat(60));
        
        try {
            // Create sample instance
            ProblemInstance instance = InstanceReader.createSampleInstance();
            
            // Run optimization first
            OptimizationParameters optParams = OptimizationParameters.createQuick();
            VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, optParams);
            VMAllocationOptimizer.OptimizationResult optResult = optimizer.optimize();
            
            // Get best solution for simulation
            AllocationSolution bestSolution = optResult.getBestCostSolution();
            if (bestSolution != null) {
                log.info("Running CloudSim simulation for best cost solution...");
                
                // Configure simulation parameters
                CloudSimSimulator.SimulationParameters simParams = 
                    CloudSimSimulator.SimulationParameters.createDefault();
                simParams.setCloudletsPerVm(3); // Quick demo
                simParams.setCloudletLength(5000);
                
                // Run simulation
                CloudSimSimulator simulator = new CloudSimSimulator(instance, simParams);
                CloudSimSimulator.SimulationResults simResults = simulator.runSimulation(bestSolution);
                
                // Show simulation results
                log.info("\nCLOUDSIM SIMULATION RESULTS:");
                log.info("-".repeat(40));
                System.out.println(simResults.generateReport());
                
                // Compare optimization vs simulation costs
                log.info("COST COMPARISON:");
                log.info("  Optimization Cost: {}", String.format("%.2f", bestSolution.getTotalCost()));
                log.info("  Simulation Cost: ${}", String.format("%.2f", simResults.totalCost));
                log.info("  Energy Consumption: {} kWh", String.format("%.2f", simResults.totalEnergyConsumption));
                
            } else {
                log.warn("No feasible solution found for simulation");
            }
            
        } catch (Exception e) {
            log.error("CloudSim demo failed", e);
        }
    }
    
    // integrated optimization and simulation demo
    private static void runIntegratedDemo() {
        log.info("\n" + "=".repeat(60));
        log.info("INTEGRATED OPTIMIZATION & SIMULATION DEMO");
        log.info("=".repeat(60));
        
        try {
            // Create sample instance
            ProblemInstance instance = InstanceReader.createSampleInstance();
            
            // Create integrator for quick execution
            SimulationIntegrator integrator = SimulationIntegrator.createQuick(instance);
            
            // Run integrated optimization and simulation
            SimulationIntegrator.IntegratedResults results = integrator.runOptimizationAndSimulation();
            
            // Show complete results
            log.info("\nINTEGRATED RESULTS:");
            log.info("-".repeat(40));
            System.out.println(results.generateCompleteReport());
            
            // Show cost differences
            Map<AllocationSolution, SimulationIntegrator.CostDifference> costDiffs = results.getCostDifferences();
            if (!costDiffs.isEmpty()) {
                log.info("\nCOST DIFFERENCES (Optimization vs Simulation):");
                log.info("-".repeat(50));
                int i = 1;
                for (Map.Entry<AllocationSolution, SimulationIntegrator.CostDifference> entry : costDiffs.entrySet()) {
                    log.info("Solution {}: {}", i++, entry.getValue());
                }
            }
            
            // Find best simulated solution
            AllocationSolution bestSimulated = results.getBestSimulatedSolution();
            if (bestSimulated != null) {
                log.info("\nBEST SOLUTION BY SIMULATION:");
                log.info("  Cost: {}", String.format("%.2f", bestSimulated.getTotalCost()));
                log.info("  Reliability: {}", String.format("%.3f", bestSimulated.getTotalReliability()));
                log.info("  Active Hosts: {}", bestSimulated.getActiveHosts().size());
            }
            
        } catch (Exception e) {
            log.error("Integrated demo failed", e);
        }
    }
    
    // run with input file
    private static void runFromFile(String filePath) {
        log.info("\n" + "=".repeat(60));
        log.info("FILE INPUT - {}", filePath);
        log.info("=".repeat(60));
        
        try {
            // default params
            OptimizationParameters params = OptimizationParameters.createDefault();
            
            // create optimizer from file
            VMAllocationOptimizer opt = VMAllocationOptimizer.fromFile(filePath, params);
            
            // run optimization
            VMAllocationOptimizer.OptimizationResult result = opt.optimize();
            
            // show results
            showResults(result);
            
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
        }
    }
    
    // compare different decoding strategies
    private static void compareStrats() {
        log.info("\n" + "=".repeat(60));
        log.info("STRATEGY COMPARISON");
        log.info("=".repeat(60));
        
        ProblemInstance inst = InstanceReader.createSampleInstance();
        
        BRKGADecoder.DecodingStrategy[] strats = {
            BRKGADecoder.DecodingStrategy.GREEDY_COST,
            BRKGADecoder.DecodingStrategy.GREEDY_RELIABILITY,
            BRKGADecoder.DecodingStrategy.BALANCED,
            BRKGADecoder.DecodingStrategy.FIRST_FIT
        };
        
        for (BRKGADecoder.DecodingStrategy strat : strats) {
            log.info("\nTesting strategy: {}", strat);
            
            OptimizationParameters params = new OptimizationParameters.Builder()
                    .brkgaPopulationSize(50)
                    .brkgaMaxGenerations(100)
                    .archiveSize(50)
                    .decodingStrategy(strat)
                    .randomSeed(42) // fixed seed for fair comparison
                    .build();
            
            VMAllocationOptimizer opt = new VMAllocationOptimizer(inst, params);
            VMAllocationOptimizer.OptimizationResult result = opt.optimize();
            
            log.info("  Archive size: {}", result.getArchive().size());
            log.info("  Exec time: {} ms", result.getReport().getTotalExecutionTime());
            
            AllocationSolution bestCost = result.getBestCostSolution();
            AllocationSolution bestRel = result.getBestReliabilitySolution();
            
            if (bestCost != null) {
                log.info("  Best cost: {} (rel: {})", 
                          String.format("%.2f", bestCost.getTotalCost()), 
                          String.format("%.3f", bestRel.getTotalReliability()));
            }
            
            if (bestRel != null) {
                log.info("  Best rel: {} (cost: {})", 
                          String.format("%.3f", bestRel.getTotalReliability()), 
                          String.format("%.2f", bestRel.getTotalCost()));
            }
        }
    }
    
    // show optimization results
    private static void showResults(VMAllocationOptimizer.OptimizationResult result) {
        log.info("\nOPTIMIZATION RESULTS:");
        log.info("-".repeat(40));
        
        // general stats
        log.info("Archive size: {}", result.getArchive().size());
        log.info("Exec time: {} ms", result.getReport().getTotalExecutionTime());
        
        // best solutions
        AllocationSolution bestCost = result.getBestCostSolution();
        AllocationSolution bestRel = result.getBestReliabilitySolution();
        AllocationSolution balanced = result.getBalancedSolution();
        
        if (bestCost != null) {
            log.info("\nBest Cost Solution:");
            log.info("  Cost: {}", String.format("%.2f", bestCost.getTotalCost()));
            log.info("  Reliability: {}", String.format("%.3f", bestCost.getTotalReliability()));
            log.info("  Energy: {}", String.format("%.2f", bestCost.getEnergyConsumption()));
            log.info("  Feasible: {}", bestCost.isFeasible());
            log.info("  Active hosts: {}", bestCost.getActiveHosts().size());
        }
        
        if (bestRel != null) {
            log.info("\nBest Reliability Solution:");
            log.info("  Reliability: {}", String.format("%.3f", bestRel.getTotalReliability()));
            log.info("  Cost: {}", String.format("%.2f", bestRel.getTotalCost()));
            log.info("  Energy: {}", String.format("%.2f", bestRel.getEnergyConsumption()));
            log.info("  Feasible: {}", bestRel.isFeasible());
            log.info("  Active hosts: {}", bestRel.getActiveHosts().size());
        }
        
        if (balanced != null) {
            log.info("\nBalanced Solution:");
            log.info("  Cost: {}", String.format("%.2f", balanced.getTotalCost()));
            log.info("  Reliability: {}", String.format("%.3f", balanced.getTotalReliability()));
            log.info("  Energy: {}", String.format("%.2f", balanced.getEnergyConsumption()));
            log.info("  Feasible: {}", balanced.isFeasible());
            log.info("  Active hosts: {}", balanced.getActiveHosts().size());
        }
        
        // detailed report
        log.info("\nDETAILED REPORT:");
        log.info("-".repeat(40));
        System.out.println(result.getReport().generateTextReport());
        
        // analyze allocations (only for balanced solution)
        if (balanced != null) {
            analyzeAlloc(balanced);
        }
    }
    
    // analyze specific allocation solution
    private static void analyzeAlloc(AllocationSolution sol) {
        log.info("\nALLOCATION ANALYSIS:");
        log.info("-".repeat(40));
        
        Map<Host, List<VM>> hostToVms = new HashMap<>();
        for (Map.Entry<VM, Host> entry : sol.getVmToHost().entrySet()) {
            Host h = entry.getValue();
            VM v = entry.getKey();
            hostToVms.computeIfAbsent(h, k -> new ArrayList<>()).add(v);
        }
        
        for (Map.Entry<Host, List<VM>> entry : hostToVms.entrySet()) {
            Host h = entry.getKey();
            List<VM> vms = entry.getValue();
            
            log.info("Host {} (cost={}, failProb={}):", 
                    h.getHostId(), 
                    String.format("%.2f", h.getCost()),
                    String.format("%.3f", h.getFailProb()));
            
            for (VM vm : vms) {
                log.info("  VM {} (minRel={}, prio={})", 
                        vm.getVmId(),
                        String.format("%.3f", vm.getMinRel()),
                        String.format("%.1f", vm.getPrio()));
            }
            
            // calc resource usage
            Map<ResourceType, Double> usage = new HashMap<>();
            for (ResourceType type : ResourceType.values()) {
                double totalUsage = vms.stream()
                    .mapToDouble(vm -> vm.getDemand(type))
                    .sum();
                usage.put(type, totalUsage);
                
                double utilization = totalUsage / h.getCap(type);
                log.info("  {} usage: {}/{} ({:.1f}%)", 
                        type, 
                        String.format("%.1f", totalUsage),
                        String.format("%.1f", h.getCap(type)),
                        utilization * 100);
            }
        }
    }
    
    // Test with real instance files from examples folder
    private static void testRealInstances() {
        log.info("\n" + "=".repeat(60));
        log.info("REAL INSTANCE FILES TEST");
        log.info("=".repeat(60));
        
        String[] instanceFiles = {
            "examples/small_instance.json",
            "examples/sample_instance.json", 
            "examples/large_instance.json"
        };
        
        InstanceReader reader = new InstanceReader();
        
        for (String filePath : instanceFiles) {
            try {
                log.info("\nTesting instance: {}", filePath);
                
                // Load instance
                ProblemInstance instance = reader.readFromFile(filePath);
                log.info("Loaded: {} VMs, {} hosts", instance.getVMCount(), instance.getHostCount());
                
                // Quick optimization
                OptimizationParameters params = OptimizationParameters.createQuick();
                VMAllocationOptimizer optimizer = new VMAllocationOptimizer(instance, params);
                VMAllocationOptimizer.OptimizationResult result = optimizer.optimize();
                
                log.info("Optimization completed - Archive size: {}", result.getArchive().size());
                
                // Quick simulation
                AllocationSolution bestSolution = result.getBestCostSolution();
                if (bestSolution != null) {
                    CloudSimSimulator.SimulationParameters simParams = 
                        CloudSimSimulator.SimulationParameters.createDefault();
                    simParams.setCloudletsPerVm(3);
                    simParams.setCloudletLength(5000);
                    
                    CloudSimSimulator simulator = new CloudSimSimulator(instance, simParams);
                    CloudSimSimulator.SimulationResults simResults = simulator.runSimulation(bestSolution);
                    
                    log.info("Simulation completed:");
                    log.info("  Optimization cost: {}", String.format("%.2f", bestSolution.getTotalCost()));
                    log.info("  Simulation cost: ${}", String.format("%.2f", simResults.totalCost));
                    log.info("  Energy: {} kWh", String.format("%.2f", simResults.totalEnergyConsumption));
                    log.info("  Active hosts: {}", bestSolution.getActiveHosts().size());
                }
                
            } catch (IOException e) {
                log.error("Failed to load instance: {}", filePath, e);
            } catch (Exception e) {
                log.error("Error testing instance: {}", filePath, e);
            }
        }
    }
    
    // NEW: Real-time allocation demo with CloudSim objects as input
    private static void runRealTimeAllocationDemo() {
        log.info("\n" + "=".repeat(60));
        log.info("REAL-TIME ALLOCATION DEMO - CloudSim Objects as Input");
        log.info("=".repeat(60));
        
        try {
            // 1. Criar simulação CloudSim
            CloudSimPlus simulation = new CloudSimPlus();
            
            // 2. Criar alocador otimizado em tempo real
            CloudSimRealTimeAllocator allocator = CloudSimRealTimeAllocator.createRealTimeOptimized();
            
            // 3. Criar hosts CloudSim diretamente
            List<org.cloudsimplus.hosts.Host> cloudSimHosts = createCloudSimHosts();
            
            // 4. Criar datacenter com alocador otimizado
            Datacenter datacenter = new DatacenterSimple(simulation, cloudSimHosts, allocator);
            datacenter.getCharacteristics()
                    .setCostPerSecond(0.015)
                    .setCostPerMem(0.03)
                    .setCostPerStorage(0.002)
                    .setCostPerBw(0.001);
            
            // 5. Criar broker
            org.cloudsimplus.brokers.DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
            
            // 6. Criar VMs CloudSim diretamente
            List<org.cloudsimplus.vms.Vm> cloudSimVms = createCloudSimVMs();
            
            // 7. Criar cloudlets
            List<org.cloudsimplus.cloudlets.Cloudlet> cloudlets = createCloudSimCloudlets(cloudSimVms);
            
            // 8. Submeter VMs e cloudlets
            broker.submitVmList(cloudSimVms);
            broker.submitCloudletList(cloudlets);
            
            log.info("Starting real-time allocation simulation:");
            log.info("  Hosts: {} (created directly in CloudSim)", cloudSimHosts.size());
            log.info("  VMs: {} (created directly in CloudSim)", cloudSimVms.size());
            log.info("  Cloudlets: {}", cloudlets.size());
            log.info("  Allocation Policy: CloudSimRealTimeAllocator");
            
            // 9. Executar simulação
            long startTime = System.currentTimeMillis();
            simulation.start();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 10. Mostrar resultados da alocação em tempo real
            showRealTimeAllocationResults(broker, allocator, executionTime);
            
        } catch (Exception e) {
            log.error("Real-time allocation demo failed", e);
        }
    }
    
    /**
     * Cria hosts CloudSim diretamente (sem usar nosso modelo interno)
     */
    private static List<org.cloudsimplus.hosts.Host> createCloudSimHosts() {
        List<org.cloudsimplus.hosts.Host> hostList = new ArrayList<>();
        
        // Host 1: Servidor de alta performance
        List<Pe> peList1 = List.of(
            new PeSimple(3000), new PeSimple(3000), 
            new PeSimple(3000), new PeSimple(3000),
            new PeSimple(3000), new PeSimple(3000)
        );
        org.cloudsimplus.hosts.Host host1 = new HostSimple(16384, 10000, 500000, peList1);
        host1.setVmScheduler(new VmSchedulerTimeShared()).setId(1);
        hostList.add(host1);
        
        // Host 2: Servidor médio
        List<Pe> peList2 = List.of(
            new PeSimple(2000), new PeSimple(2000), 
            new PeSimple(2000), new PeSimple(2000)
        );
        org.cloudsimplus.hosts.Host host2 = new HostSimple(8192, 5000, 250000, peList2);
        host2.setVmScheduler(new VmSchedulerTimeShared()).setId(2);
        hostList.add(host2);
        
        // Host 3: Servidor econômico
        List<Pe> peList3 = List.of(
            new PeSimple(1500), new PeSimple(1500), 
            new PeSimple(1500)
        );
        org.cloudsimplus.hosts.Host host3 = new HostSimple(4096, 2500, 100000, peList3);
        host3.setVmScheduler(new VmSchedulerTimeShared()).setId(3);
        hostList.add(host3);
        
        // Host 4: Servidor especializado
        List<Pe> peList4 = List.of(
            new PeSimple(2500), new PeSimple(2500), 
            new PeSimple(2500), new PeSimple(2500),
            new PeSimple(2500)
        );
        org.cloudsimplus.hosts.Host host4 = new HostSimple(12288, 8000, 300000, peList4);
        host4.setVmScheduler(new VmSchedulerTimeShared()).setId(4);
        hostList.add(host4);
        
        return hostList;
    }
    
    /**
     * Cria VMs CloudSim diretamente (sem usar nosso modelo interno)
     */
    private static List<org.cloudsimplus.vms.Vm> createCloudSimVMs() {
        List<org.cloudsimplus.vms.Vm> vmList = new ArrayList<>();
        
        // VM 1: Aplicação crítica (alta demanda)
        org.cloudsimplus.vms.Vm vm1 = new VmSimple(4000, 2);
        vm1.setRam(4096).setSize(50000).setBw(1000)
           .setCloudletScheduler(new CloudletSchedulerTimeShared()).setId(1);
        vmList.add(vm1);
        
        // VM 2: Aplicação web (demanda média)
        org.cloudsimplus.vms.Vm vm2 = new VmSimple(2000, 1);
        vm2.setRam(2048).setSize(25000).setBw(500)
           .setCloudletScheduler(new CloudletSchedulerTimeShared()).setId(2);
        vmList.add(vm2);
        
        // VM 3: Serviço de background (baixa demanda)
        org.cloudsimplus.vms.Vm vm3 = new VmSimple(1000, 1);
        vm3.setRam(1024).setSize(15000).setBw(200)
           .setCloudletScheduler(new CloudletSchedulerTimeShared()).setId(3);
        vmList.add(vm3);
        
        // VM 4: Banco de dados (demanda balanceada)
        org.cloudsimplus.vms.Vm vm4 = new VmSimple(3000, 2);
        vm4.setRam(6144).setSize(80000).setBw(800)
           .setCloudletScheduler(new CloudletSchedulerTimeShared()).setId(4);
        vmList.add(vm4);
        
        // VM 5: Processamento científico (reduced RAM to fit available capacity)
        org.cloudsimplus.vms.Vm vm5 = new VmSimple(5000, 3);
        vm5.setRam(4096).setSize(40000).setBw(600)
           .setCloudletScheduler(new CloudletSchedulerTimeShared()).setId(5);
        vmList.add(vm5);
        
        // VM 6: Desenvolvimento (demanda variada)
        org.cloudsimplus.vms.Vm vm6 = new VmSimple(1500, 1);
        vm6.setRam(3072).setSize(35000).setBw(400)
           .setCloudletScheduler(new CloudletSchedulerTimeShared()).setId(6);
        vmList.add(vm6);
        
        return vmList;
    }
    
    /**
     * Cria cloudlets para as VMs CloudSim
     */
    private static List<org.cloudsimplus.cloudlets.Cloudlet> createCloudSimCloudlets(
            List<org.cloudsimplus.vms.Vm> vmList) {
        List<org.cloudsimplus.cloudlets.Cloudlet> cloudletList = new ArrayList<>();
        
        UtilizationModelFull utilizationModel = new UtilizationModelFull();
        
        int cloudletId = 0;
        for (org.cloudsimplus.vms.Vm vm : vmList) {
            // Criar 2-4 cloudlets por VM dependendo do seu perfil
            int cloudletsCount = vm.getId() <= 2 ? 4 : 2; // VMs críticas têm mais workload
            
            for (int i = 0; i < cloudletsCount; i++) {
                long length = 5000 + (vm.getId() * 2000) + (i * 1000); // Variado por VM
                long fileSize = 300 + (vm.getId() * 100);
                long outputSize = fileSize / 2;
                
                org.cloudsimplus.cloudlets.Cloudlet cloudlet = new CloudletSimple(length, 1);
                cloudlet.setFileSize(fileSize)
                        .setOutputSize(outputSize)
                        .setUtilizationModel(utilizationModel)
                        .setVm(vm);
                
                cloudlet.setId(cloudletId++);
                cloudletList.add(cloudlet);
            }
        }
        
        return cloudletList;
    }
    
    /**
     * Mostra resultados da simulação com alocação em tempo real
     */
    private static void showRealTimeAllocationResults(
            org.cloudsimplus.brokers.DatacenterBroker broker,
            CloudSimRealTimeAllocator allocator,
            long executionTime) {
        
        log.info("\nREAL-TIME ALLOCATION RESULTS:");
        log.info("-".repeat(45));
        
        // Estatísticas do alocador
        CloudSimRealTimeAllocator.AllocationStatistics stats = allocator.getCurrentAllocationStatistics();
        log.info("Allocation Statistics: {}", stats);
        
        // Resultados das cloudlets
        List<org.cloudsimplus.cloudlets.Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        log.info("Cloudlets completed: {}", finishedCloudlets.size());
        
        if (!finishedCloudlets.isEmpty()) {
            double totalExecutionTime = finishedCloudlets.stream()
                    .mapToDouble(cloudlet -> cloudlet.getFinishTime())
                    .sum();
            
            double avgExecutionTime = totalExecutionTime / finishedCloudlets.size();
            
            log.info("Average cloudlet finish time: {:.2f} seconds", avgExecutionTime);
            log.info("Total cloudlets completed: {}", finishedCloudlets.size());
            log.info("Simulation execution time: {} ms", executionTime);
        }
        
        // Detalhes da alocação
        log.info("\nVM ALLOCATION DETAILS:");
        log.info("-".repeat(30));
        
        Map<Long, List<org.cloudsimplus.vms.Vm>> vmsByHost = new HashMap<>();
        for (org.cloudsimplus.vms.Vm vm : broker.getVmExecList()) {
            if (vm.getHost() != null) {
                long hostId = vm.getHost().getId();
                vmsByHost.computeIfAbsent(hostId, k -> new ArrayList<>()).add(vm);
            }
        }
        
        for (Map.Entry<Long, List<org.cloudsimplus.vms.Vm>> entry : vmsByHost.entrySet()) {
            log.info("Host {}: {} VMs allocated", entry.getKey(), entry.getValue().size());
            
            double totalHostCpuDemand = 0;
            long totalHostRamDemand = 0;
            
            for (org.cloudsimplus.vms.Vm vm : entry.getValue()) {
                totalHostCpuDemand += vm.getPesNumber() * vm.getMips();
                totalHostRamDemand += vm.getRam().getCapacity();
                
                log.info("  VM {} (CPU: {} MIPS, RAM: {} MB, Storage: {} MB)", 
                        vm.getId(),
                        String.format("%.0f", vm.getPesNumber() * vm.getMips()),
                        vm.getRam().getCapacity(),
                        vm.getStorage().getCapacity());
            }
            
            // Calcular utilização do host
            org.cloudsimplus.hosts.Host host = entry.getValue().get(0).getHost();
            double totalHostMips = host.getPeList().stream().mapToDouble(pe -> pe.getCapacity()).sum();
            double cpuUtilization = totalHostCpuDemand / totalHostMips;
            double ramUtilization = (double) totalHostRamDemand / host.getRam().getCapacity();
            
            log.info("  Host utilization - CPU: {:.1f}%, RAM: {:.1f}%", 
                    cpuUtilization * 100, ramUtilization * 100);
        }
        
        // Vantagens do sistema
        log.info("\nREAL-TIME ALLOCATION ADVANTAGES:");
        log.info("-".repeat(35));
        log.info("✓ Direct integration with CloudSim objects");
        log.info("✓ No need for external data files or conversion");
        log.info("✓ Real-time optimization during simulation");
        log.info("✓ Dynamic reallocation support");
        log.info("✓ Cost and reliability optimization");
        log.info("✓ Energy efficiency considerations");
    }
}
