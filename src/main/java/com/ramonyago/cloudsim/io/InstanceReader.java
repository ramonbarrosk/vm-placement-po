package com.ramonyago.cloudsim.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramonyago.cloudsim.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável por ler instâncias do problema a partir de arquivos JSON.
 */
public class InstanceReader {
    private final ObjectMapper objectMapper;
    
    public InstanceReader() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Lê uma instância a partir de um arquivo
     */
    public ProblemInstance readFromFile(String filePath) throws IOException {
        return readFromFile(new File(filePath));
    }
    
    /**
     * Lê uma instância a partir de um objeto File
     */
    public ProblemInstance readFromFile(File file) throws IOException {
        JsonNode rootNode = objectMapper.readTree(file);
        return parseInstance(rootNode, file.getName());
    }
    
    /**
     * Lê uma instância a partir de um InputStream
     */
    public ProblemInstance readFromStream(InputStream inputStream, String instanceName) throws IOException {
        JsonNode rootNode = objectMapper.readTree(inputStream);
        return parseInstance(rootNode, instanceName);
    }
    
    /**
     * Lê uma instância a partir de uma string JSON
     */
    public ProblemInstance readFromString(String jsonContent, String instanceName) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        return parseInstance(rootNode, instanceName);
    }
    
    private ProblemInstance parseInstance(JsonNode rootNode, String instanceName) {
        ProblemInstance instance = new ProblemInstance(instanceName);
        
        // Parse metadata se existir
        if (rootNode.has("metadata")) {
            JsonNode metadataNode = rootNode.get("metadata");
            metadataNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode valueNode = metadataNode.get(fieldName);
                Object value = parseJsonValue(valueNode);
                instance.addMetadata(fieldName, value);
            });
        }
        
        // Parse VMs
        if (rootNode.has("vms")) {
            JsonNode vmsNode = rootNode.get("vms");
            for (JsonNode vmNode : vmsNode) {
                VM vm = parseVM(vmNode);
                instance.addVM(vm);
            }
        }
        
        // Parse Hosts
        if (rootNode.has("hosts")) {
            JsonNode hostsNode = rootNode.get("hosts");
            for (JsonNode hostNode : hostsNode) {
                Host host = parseHost(hostNode);
                instance.addHost(host);
            }
        }
        
        return instance;
    }
    
    private VM parseVM(JsonNode vmNode) {
        int id = vmNode.get("id").asInt();
        double minReliability = vmNode.get("minReliability").asDouble();
        double priority = vmNode.has("priority") ? vmNode.get("priority").asDouble() : 1.0;
        
        VM vm = new VM(id, minReliability, priority);
        
        // Parse resource demands
        if (vmNode.has("resourceDemands")) {
            JsonNode demandsNode = vmNode.get("resourceDemands");
            for (ResourceType type : ResourceType.values()) {
                String fieldName = type.name().toLowerCase();
                if (demandsNode.has(fieldName)) {
                    double demand = demandsNode.get(fieldName).asDouble();
                    vm.setResourceDemand(type, demand);
                }
            }
        }
        
        return vm;
    }
    
    private Host parseHost(JsonNode hostNode) {
        int id = hostNode.get("id").asInt();
        double activationCost = hostNode.get("activationCost").asDouble();
        double failureProbability = hostNode.get("failureProbability").asDouble();
        double energyConsumption = hostNode.has("energyConsumption") ? 
                                  hostNode.get("energyConsumption").asDouble() : 100.0;
        
        Host host = new Host(id, activationCost, failureProbability, energyConsumption);
        
        // Parse resource capacities
        if (hostNode.has("resourceCapacities")) {
            JsonNode capacitiesNode = hostNode.get("resourceCapacities");
            for (ResourceType type : ResourceType.values()) {
                String fieldName = type.name().toLowerCase();
                if (capacitiesNode.has(fieldName)) {
                    double capacity = capacitiesNode.get(fieldName).asDouble();
                    host.setResourceCapacity(type, capacity);
                }
            }
        }
        
        return host;
    }
    
    private Object parseJsonValue(JsonNode node) {
        if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode element : node) {
                list.add(parseJsonValue(element));
            }
            return list;
        } else {
            return node.toString();
        }
    }
    
    /**
     * Cria uma instância de exemplo para testes
     */
    public static ProblemInstance createSampleInstance() {
        ProblemInstance instance = new ProblemInstance("sample");
        
        // Criar VMs de exemplo
        VM vm1 = new VM(1, 0.95, 1.0);
        vm1.setResourceDemand(ResourceType.CPU, 2.0);
        vm1.setResourceDemand(ResourceType.RAM, 4.0);
        vm1.setResourceDemand(ResourceType.STORAGE, 20.0);
        
        VM vm2 = new VM(2, 0.90, 1.5);
        vm2.setResourceDemand(ResourceType.CPU, 1.0);
        vm2.setResourceDemand(ResourceType.RAM, 2.0);
        vm2.setResourceDemand(ResourceType.STORAGE, 10.0);
        
        VM vm3 = new VM(3, 0.98, 2.0);
        vm3.setResourceDemand(ResourceType.CPU, 4.0);
        vm3.setResourceDemand(ResourceType.RAM, 8.0);
        vm3.setResourceDemand(ResourceType.STORAGE, 40.0);
        
        instance.addVM(vm1);
        instance.addVM(vm2);
        instance.addVM(vm3);
        
        // Criar hosts de exemplo
        Host host1 = new Host(1, 100.0, 0.02, 150.0);
        host1.setResourceCapacity(ResourceType.CPU, 8.0);
        host1.setResourceCapacity(ResourceType.RAM, 16.0);
        host1.setResourceCapacity(ResourceType.STORAGE, 100.0);
        
        Host host2 = new Host(2, 80.0, 0.05, 120.0);
        host2.setResourceCapacity(ResourceType.CPU, 4.0);
        host2.setResourceCapacity(ResourceType.RAM, 8.0);
        host2.setResourceCapacity(ResourceType.STORAGE, 50.0);
        
        Host host3 = new Host(3, 120.0, 0.01, 180.0);
        host3.setResourceCapacity(ResourceType.CPU, 12.0);
        host3.setResourceCapacity(ResourceType.RAM, 24.0);
        host3.setResourceCapacity(ResourceType.STORAGE, 200.0);
        
        instance.addHost(host1);
        instance.addHost(host2);
        instance.addHost(host3);
        
        // Adicionar metadados
        instance.addMetadata("description", "Sample instance for testing");
        instance.addMetadata("difficulty", "easy");
        
        return instance;
    }
} 