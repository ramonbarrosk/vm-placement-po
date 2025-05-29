package com.ramonyago.cloudsim.examples;

import com.ramonyago.cloudsim.simulation.CloudSimRealTimeAllocator;

import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModel;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Exemplo completo demonstrando o uso do CloudSimRealTimeAllocator.
 * 
 * Este exemplo mostra como integrar o algoritmo de otimização diretamente
 * com os objetos Host e VM do CloudSim para alocação em tempo real.
 */
public class RealTimeAllocationExample {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeAllocationExample.class);
    
    private static final int HOSTS = 6;
    private static final int VMS = 10;
    private static final int CLOUDLETS_PER_VM = 3;
    
    public static void main(String[] args) {
        logger.info("=== CloudSim Real-Time Allocation Example ===");
        
        // Executar diferentes cenários
        runBasicRealTimeExample();
        
        logger.info("=== Examples completed ===");
    }
    
    /**
     * Exemplo básico de alocação em tempo real
     */
    private static void runBasicRealTimeExample() {
        logger.info("\n" + "=".repeat(60));
        logger.info("BASIC REAL-TIME ALLOCATION EXAMPLE");
        logger.info("=".repeat(60));
        
        // 1. Criar simulação CloudSim
        CloudSimPlus simulation = new CloudSimPlus();
        
        // 2. Criar alocador otimizado em tempo real
        CloudSimRealTimeAllocator allocator = CloudSimRealTimeAllocator.createRealTimeOptimized();
        
        // 3. Criar hosts CloudSim
        List<Host> hostList = createHosts();
        
        // 4. Criar datacenter com alocador otimizado
        Datacenter datacenter = new DatacenterSimple(simulation, hostList, allocator);
        datacenter.getCharacteristics()
                .setCostPerSecond(0.01)
                .setCostPerMem(0.02)
                .setCostPerStorage(0.001)
                .setCostPerBw(0.0);
        
        // 5. Criar broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
        
        // 6. Criar VMs CloudSim
        List<Vm> vmList = createVms();
        
        // 7. Criar cloudlets
        List<Cloudlet> cloudletList = createCloudlets(vmList);
        
        // 8. Submeter VMs e cloudlets ao broker
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        
        logger.info("Starting simulation with {} hosts, {} VMs, and {} cloudlets", 
                   hostList.size(), vmList.size(), cloudletList.size());
        
        // 9. Executar simulação
        simulation.start();
        
        // 10. Mostrar resultados
        showBasicResults(broker, allocator);
    }
    
    /**
     * Cria lista de hosts CloudSim com diferentes características
     */
    private static List<Host> createHosts() {
        List<Host> hostList = new ArrayList<>();
        
        for (int i = 0; i < HOSTS; i++) {
            // Criar PEs com capacidades variadas
            int peCount = 4 + (i % 3); // 4-6 PEs por host
            double mipsPerPe = 1000 + (i * 200); // 1000-2000 MIPS
            List<Pe> peList = new ArrayList<>();
            
            for (int j = 0; j < peCount; j++) {
                peList.add(new PeSimple(mipsPerPe));
            }
            
            // RAM e Storage variados
            long ram = 4096 + (i * 2048); // 4-14 GB RAM
            long storage = 100000 + (i * 50000); // 100-350 GB Storage
            long bw = 1000 + (i * 500); // 1-3.5 Gbps
            
            Host host = new HostSimple(ram, bw, storage, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
            host.setId(i);
            
            hostList.add(host);
        }
        
        logger.info("Created {} hosts with varying capacities", hostList.size());
        return hostList;
    }
    
    /**
     * Cria lista de VMs CloudSim com diferentes demandas
     */
    private static List<Vm> createVms() {
        List<Vm> vmList = new ArrayList<>();
        
        for (int i = 0; i < VMS; i++) {
            // Diferentes perfis de VMs
            double mips = 500 + (i % 4) * 300; // 500-1400 MIPS
            long ram = 1024 + (i % 3) * 512;   // 1-2 GB RAM
            long storage = 10000 + (i % 5) * 5000; // 10-30 GB Storage
            long bw = 100 + (i % 4) * 50;      // 100-250 Mbps
            
            Vm vm = new VmSimple(mips, 1); // 1 PE por VM
            vm.setRam(ram)
              .setSize(storage)
              .setBw(bw)
              .setCloudletScheduler(new CloudletSchedulerTimeShared());
            
            vm.setId(i);
            vmList.add(vm);
        }
        
        logger.info("Created {} VMs with varying demands", vmList.size());
        return vmList;
    }
    
    /**
     * Cria cloudlets para as VMs
     */
    private static List<Cloudlet> createCloudlets(List<Vm> vmList) {
        List<Cloudlet> cloudletList = new ArrayList<>();
        
        UtilizationModel utilizationModel = new UtilizationModelFull();
        
        int cloudletId = 0;
        for (Vm vm : vmList) {
            for (int i = 0; i < CLOUDLETS_PER_VM; i++) {
                long length = 5000 + (cloudletId % 10) * 1000; // 5-15k MI
                long fileSize = 300 + (cloudletId % 5) * 100;  // 300-700 MB
                long outputSize = fileSize / 2;
                
                Cloudlet cloudlet = new CloudletSimple(length, 1); // 1 PE
                cloudlet.setFileSize(fileSize)
                        .setOutputSize(outputSize)
                        .setUtilizationModel(utilizationModel)
                        .setVm(vm);
                
                cloudlet.setId(cloudletId++);
                cloudletList.add(cloudlet);
            }
        }
        
        logger.info("Created {} cloudlets for {} VMs", cloudletList.size(), vmList.size());
        return cloudletList;
    }
    
    /**
     * Mostra resultados da simulação básica
     */
    private static void showBasicResults(DatacenterBroker broker, CloudSimRealTimeAllocator allocator) {
        logger.info("\nBASIC SIMULATION RESULTS:");
        logger.info("-".repeat(40));
        
        // Estatísticas de alocação
        CloudSimRealTimeAllocator.AllocationStatistics stats = allocator.getCurrentAllocationStatistics();
        logger.info("Allocation Statistics: {}", stats);
        
        // Resultados das cloudlets
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        logger.info("Cloudlets completed: {}", finishedCloudlets.size());
        
        if (!finishedCloudlets.isEmpty()) {
            double totalExecutionTime = finishedCloudlets.stream()
                    .mapToDouble(cloudlet -> cloudlet.getFinishTime())
                    .sum();
            
            double avgExecutionTime = totalExecutionTime / finishedCloudlets.size();
            
            logger.info("Average cloudlet execution time: {:.2f} seconds", avgExecutionTime);
            logger.info("Total simulation cost: ${:.2f}", stats.totalCost);
            logger.info("Total energy consumption: {:.2f} kWh", stats.totalEnergyConsumption);
        }
    }
} 