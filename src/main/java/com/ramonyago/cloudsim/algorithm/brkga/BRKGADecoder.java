package com.ramonyago.cloudsim.algorithm.brkga;

import com.ramonyago.cloudsim.model.*;

import java.util.*;

/**
 * Decodificador BRKGA que transforma um vetor de chaves aleatórias
 * em uma solução de alocação de VMs válida.
 */
public class BRKGADecoder {
    private final ProblemInstance instance;
    private final List<VM> vms;
    private final List<Host> hosts;
    private final Random random;
    
    // Estratégias de decodificação
    public enum DecodingStrategy {
        GREEDY_COST,        // Prioriza hosts com menor custo
        GREEDY_RELIABILITY, // Prioriza hosts com maior confiabilidade
        BALANCED,           // Balanceamento entre custo e confiabilidade
        FIRST_FIT           // Primeira posição disponível
    }
    
    private final DecodingStrategy strategy;
    
    public BRKGADecoder(ProblemInstance instance, DecodingStrategy strategy) {
        this.instance = instance;
        this.vms = instance.getVMs();
        this.hosts = instance.getHosts();
        this.strategy = strategy;
        this.random = new Random();
    }
    
    /**
     * Decodifica um vetor de chaves em uma solução de alocação
     * 
     * Encoding: 
     * - Primeiras |VMs| chaves: ordem de alocação das VMs
     * - Próximas |VMs|*|Hosts| chaves: preferências de alocação VM-Host
     */
    public AllocationSolution decode(double[] keys) {
        if (keys.length < vms.size() + vms.size() * hosts.size()) {
            throw new IllegalArgumentException("Insufficient keys for decoding");
        }
        
        AllocationSolution solution = new AllocationSolution(vms, hosts);
        
        // Cria lista de VMs ordenada pelas primeiras chaves
        List<VMOrder> vmOrder = new ArrayList<>();
        for (int i = 0; i < vms.size(); i++) {
            vmOrder.add(new VMOrder(vms.get(i), keys[i]));
        }
        vmOrder.sort(Comparator.comparingDouble(vo -> vo.key));
        
        // Aloca VMs em ordem
        for (VMOrder vmOrd : vmOrder) {
            VM vm = vmOrd.vm;
            Host selectedHost = selectHostForVM(vm, keys, solution);
            
            if (selectedHost != null) {
                solution.allocateVM(vm, selectedHost);
            }
            // Se não conseguir alocar, VM fica sem alocação (penalização no fitness)
        }
        
        return solution;
    }
    
    /**
     * Seleciona o melhor host para uma VM baseado nas chaves e estratégia
     */
    private Host selectHostForVM(VM vm, double[] keys, AllocationSolution currentSolution) {
        // Calcula índice base das preferências para esta VM
        int vmIndex = vms.indexOf(vm);
        int prefBase = vms.size() + vmIndex * hosts.size();
        
        // Cria lista de hosts com suas preferências
        List<HostPreference> hostPrefs = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            Host host = hosts.get(i);
            double preference = keys[prefBase + i];
            
            // Verifica se o host pode alocar a VM
            if (host.canAllocate(vm, currentSolution.getVmToHost())) {
                double score = calculateHostScore(vm, host, preference, currentSolution);
                hostPrefs.add(new HostPreference(host, preference, score));
            }
        }
        
        if (hostPrefs.isEmpty()) {
            return null; // Nenhum host pode alocar a VM
        }
        
        // Ordena hosts por score (dependendo da estratégia)
        hostPrefs.sort(Comparator.comparingDouble(hp -> -hp.score));
        
        // Retorna o host com melhor score
        return hostPrefs.get(0).host;
    }
    
    /**
     * Calcula o score de um host para uma VM baseado na estratégia
     */
    private double calculateHostScore(VM vm, Host host, double preference, AllocationSolution solution) {
        switch (strategy) {
            case GREEDY_COST:
                return preference + (1.0 / (1.0 + host.getActivationCost()));
                
            case GREEDY_RELIABILITY:
                return preference + host.getReliability();
                
            case BALANCED:
                double costNorm = 1.0 / (1.0 + host.getActivationCost());
                double reliability = host.getReliability();
                return preference + 0.5 * costNorm + 0.5 * reliability;
                
            case FIRST_FIT:
            default:
                return preference;
        }
    }
    
    /**
     * Repara uma solução para torná-la mais viável
     */
    public AllocationSolution repairSolution(AllocationSolution solution) {
        AllocationSolution repairedSolution = new AllocationSolution(solution);
        
        // Tenta realocar VMs não alocadas
        List<VM> unallocatedVMs = new ArrayList<>();
        for (VM vm : vms) {
            if (repairedSolution.getHostForVM(vm) == null) {
                unallocatedVMs.add(vm);
            }
        }
        
        // Ordena VMs não alocadas por prioridade
        unallocatedVMs.sort(Comparator.comparingDouble(VM::getPriority).reversed());
        
        for (VM vm : unallocatedVMs) {
            Host bestHost = findBestAvailableHost(vm, repairedSolution);
            if (bestHost != null) {
                repairedSolution.allocateVM(vm, bestHost);
            }
        }
        
        return repairedSolution;
    }
    
    private Host findBestAvailableHost(VM vm, AllocationSolution solution) {
        List<Host> availableHosts = new ArrayList<>();
        
        for (Host host : hosts) {
            if (host.canAllocate(vm, solution.getVmToHost())) {
                availableHosts.add(host);
            }
        }
        
        if (availableHosts.isEmpty()) {
            return null;
        }
        
        // Retorna o host com menor custo dentre os disponíveis
        return availableHosts.stream()
                .min(Comparator.comparingDouble(Host::getActivationCost))
                .orElse(null);
    }
    
    /**
     * Gera um vetor de chaves aleatórias válido
     */
    public double[] generateRandomKeys() {
        int keyCount = vms.size() + vms.size() * hosts.size();
        double[] keys = new double[keyCount];
        
        for (int i = 0; i < keyCount; i++) {
            keys[i] = random.nextDouble();
        }
        
        return keys;
    }
    
    /**
     * Calcula o número de chaves necessárias para a codificação
     */
    public int getRequiredKeyCount() {
        return vms.size() + vms.size() * hosts.size();
    }
    
    public DecodingStrategy getStrategy() {
        return strategy;
    }
    
    /**
     * Classe auxiliar para ordenação de VMs
     */
    private static class VMOrder {
        final VM vm;
        final double key;
        
        VMOrder(VM vm, double key) {
            this.vm = vm;
            this.key = key;
        }
    }
    
    /**
     * Classe auxiliar para preferências de hosts
     */
    private static class HostPreference {
        final Host host;
        final double preference;
        final double score;
        
        HostPreference(Host host, double preference, double score) {
            this.host = host;
            this.preference = preference;
            this.score = score;
        }
    }
} 