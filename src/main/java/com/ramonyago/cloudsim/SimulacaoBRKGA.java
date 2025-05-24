package com.ramonyago.cloudsim;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicy;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSuitability;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudsimplus.core.Simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.cloudsimplus.core.*;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.datacenters.DatacenterCharacteristics;
import org.cloudsimplus.cloudlets.Cloudlet;


public class SimulacaoBRKGA {
    public static void main(String[] args) {
        CloudSimPlus simulation = new CloudSimPlus();

        List<Host> hosts = new ArrayList<>();
        // criar hosts com CPU, RAM, armazenamento...

        DatacenterSimple datacenter = new DatacenterSimple(simulation, hosts, new AlocacaoBRKGA(hosts) {
            @Override
            protected Optional<Host> defaultFindHostForVm(Vm vm) {
                return Optional.empty();
            }
        });

        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        List<Vm> vmList = new ArrayList<>();
        List<Cloudlet> cloudletList = new ArrayList<>();
        // criar VMs e cloudlets...

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        simulation.start();

        // analisar resultados
    }
}
