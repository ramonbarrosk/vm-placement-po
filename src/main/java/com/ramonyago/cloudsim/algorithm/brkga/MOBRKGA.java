package com.ramonyago.cloudsim.algorithm.brkga;

import com.ramonyago.cloudsim.model.AllocationSolution;
import com.ramonyago.cloudsim.model.ProblemInstance;
import com.ramonyago.cloudsim.util.ParetoArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementação do algoritmo BRKGA Multi-objetivo para alocação de VMs.
 * Utiliza conceitos de dominância de Pareto e crowding distance para seleção.
 */
public class MOBRKGA {
    private static final Logger logger = LoggerFactory.getLogger(MOBRKGA.class);
    
    private final BRKGAParameters parameters;
    private final BRKGADecoder decoder;
    private final ProblemInstance instance;
    private final Random random;
    
    private List<Individual> population;
    private ParetoArchive archive;
    private int currentGeneration;
    
    // Estatísticas de execução
    private long startTime;
    private long endTime;
    private List<Double> hyperVolumeHistory;
    private List<Integer> archiveSizeHistory;
    
    public MOBRKGA(ProblemInstance instance, BRKGAParameters parameters, 
                   BRKGADecoder.DecodingStrategy strategy) {
        this.instance = instance;
        this.parameters = parameters;
        this.decoder = new BRKGADecoder(instance, strategy);
        this.random = new Random(parameters.getRandomSeed());
        this.archive = new ParetoArchive(parameters.getArchiveSize());
        this.hyperVolumeHistory = new ArrayList<>();
        this.archiveSizeHistory = new ArrayList<>();
        
        logger.info("MOBRKGA initialized with parameters: {}", parameters);
    }
    
    /**
     * Executa o algoritmo BRKGA multi-objetivo
     */
    public ParetoArchive run() {
        logger.info("Starting MOBRKGA execution...");
        startTime = System.currentTimeMillis();
        
        // Inicialização
        initializePopulation();
        evaluatePopulation();
        updateArchive();
        
        // Loop evolutivo principal
        for (currentGeneration = 1; currentGeneration <= parameters.getMaxGenerations(); currentGeneration++) {
            // Classificação por dominância e crowding distance
            if (parameters.isUseNSGA2Selection()) {
                performNSGA2Selection();
            } else {
                performSimpleSelection();
            }
            
            // Geração da próxima população
            generateNextPopulation();
            
            // Avaliação
            evaluatePopulation();
            
            // Atualização do arquivo
            updateArchive();
            
            // Logging de progresso
            if (currentGeneration % 100 == 0 || currentGeneration == parameters.getMaxGenerations()) {
                logProgress();
            }
        }
        
        endTime = System.currentTimeMillis();
        logger.info("MOBRKGA completed in {} ms", endTime - startTime);
        
        return archive;
    }
    
    /**
     * Inicializa a população com indivíduos aleatórios
     */
    private void initializePopulation() {
        population = new ArrayList<>(parameters.getPopulationSize());
        int keyCount = decoder.getRequiredKeyCount();
        
        for (int i = 0; i < parameters.getPopulationSize(); i++) {
            Individual individual = new Individual(keyCount);
            individual.randomize(random);
            population.add(individual);
        }
        
        logger.debug("Population initialized with {} individuals", population.size());
    }
    
    /**
     * Avalia toda a população
     */
    private void evaluatePopulation() {
        for (Individual individual : population) {
            if (!individual.isEvaluated()) {
                AllocationSolution solution = decoder.decode(individual.getKeys());
                individual.setSolution(solution);
            }
        }
    }
    
    /**
     * Atualiza o arquivo de soluções não-dominadas
     */
    private void updateArchive() {
        for (Individual individual : population) {
            if (individual.isEvaluated()) {
                archive.add(individual.getSolution());
            }
        }
        
        archiveSizeHistory.add(archive.size());
        // TODO: Implementar cálculo de hipervolume
        hyperVolumeHistory.add(0.0);
    }
    
    /**
     * Seleção baseada em NSGA-II (ranking + crowding distance)
     */
    private void performNSGA2Selection() {
        // Classificação por frentes de não-dominância
        List<List<Individual>> fronts = nonDominatedSort(population);
        
        // Calcula crowding distance para cada frente
        for (List<Individual> front : fronts) {
            calculateCrowdingDistance(front);
        }
        
        // Atribui ranks aos indivíduos
        int rank = 0;
        for (List<Individual> front : fronts) {
            for (Individual individual : front) {
                individual.setDominationRank(rank);
            }
            rank++;
        }
        
        // Ordena população por rank e crowding distance
        population.sort(Individual::compareTo);
    }
    
    /**
     * Seleção simples baseada apenas em dominância
     */
    private void performSimpleSelection() {
        // Identifica soluções não-dominadas
        List<Individual> nonDominated = new ArrayList<>();
        
        for (Individual candidate : population) {
            boolean isDominated = false;
            for (Individual other : population) {
                if (!candidate.equals(other) && other.compareDominance(candidate) < 0) {
                    isDominated = true;
                    break;
                }
            }
            if (!isDominated) {
                nonDominated.add(candidate);
                candidate.setDominationRank(0);
            } else {
                candidate.setDominationRank(1);
            }
        }
        
        // Ordena por rank
        population.sort(Comparator.comparingInt(Individual::getDominationRank));
    }
    
    /**
     * Gera a próxima população usando operadores genéticos do BRKGA
     */
    private void generateNextPopulation() {
        List<Individual> nextPopulation = new ArrayList<>(parameters.getPopulationSize());
        
        // Determina tamanhos das subpopulações
        int eliteSize = parameters.getEliteSize();
        int mutantSize = parameters.getMutantSize();
        int nonEliteSize = parameters.getNonEliteSize();
        
        // Elite: melhores indivíduos da geração atual
        for (int i = 0; i < eliteSize && i < population.size(); i++) {
            nextPopulation.add(new Individual(population.get(i)));
        }
        
        // Mutantes: indivíduos completamente aleatórios
        for (int i = 0; i < mutantSize; i++) {
            Individual mutant = new Individual(decoder.getRequiredKeyCount());
            mutant.randomize(random);
            nextPopulation.add(mutant);
        }
        
        // Não-elite: offspring de crossover entre elite e não-elite
        List<Individual> elites = population.subList(0, Math.min(eliteSize, population.size()));
        List<Individual> nonElites = population.subList(Math.min(eliteSize, population.size()), 
                                                        population.size());
        
        for (int i = 0; i < nonEliteSize; i++) {
            Individual elite = elites.get(random.nextInt(elites.size()));
            Individual nonElite = nonElites.isEmpty() ? elite : 
                                 nonElites.get(random.nextInt(nonElites.size()));
            
            Individual offspring = Individual.crossover(elite, nonElite, 
                                                      parameters.getInheritanceProbability(), random);
            nextPopulation.add(offspring);
        }
        
        // Completa a população se necessário
        while (nextPopulation.size() < parameters.getPopulationSize()) {
            Individual extra = new Individual(decoder.getRequiredKeyCount());
            extra.randomize(random);
            nextPopulation.add(extra);
        }
        
        population = nextPopulation;
    }
    
    /**
     * Classificação por frentes de não-dominância (NSGA-II)
     */
    private List<List<Individual>> nonDominatedSort(List<Individual> individuals) {
        List<List<Individual>> fronts = new ArrayList<>();
        Map<Individual, List<Individual>> dominatedSolutions = new HashMap<>();
        Map<Individual, Integer> dominationCount = new HashMap<>();
        
        // Inicialização
        for (Individual individual : individuals) {
            dominatedSolutions.put(individual, new ArrayList<>());
            dominationCount.put(individual, 0);
        }
        
        // Primeira frente
        List<Individual> firstFront = new ArrayList<>();
        
        for (Individual p : individuals) {
            for (Individual q : individuals) {
                if (!p.equals(q)) {
                    int dominance = p.compareDominance(q);
                    if (dominance < 0) { // p domina q
                        dominatedSolutions.get(p).add(q);
                    } else if (dominance > 0) { // q domina p
                        dominationCount.put(p, dominationCount.get(p) + 1);
                    }
                }
            }
            
            if (dominationCount.get(p) == 0) {
                firstFront.add(p);
            }
        }
        
        fronts.add(firstFront);
        
        // Frentes subsequentes
        int frontIndex = 0;
        while (!fronts.get(frontIndex).isEmpty()) {
            List<Individual> nextFront = new ArrayList<>();
            
            for (Individual p : fronts.get(frontIndex)) {
                for (Individual q : dominatedSolutions.get(p)) {
                    dominationCount.put(q, dominationCount.get(q) - 1);
                    if (dominationCount.get(q) == 0) {
                        nextFront.add(q);
                    }
                }
            }
            
            if (!nextFront.isEmpty()) {
                fronts.add(nextFront);
                frontIndex++;
            } else {
                break;
            }
        }
        
        return fronts;
    }
    
    /**
     * Calcula crowding distance para diversidade
     */
    private void calculateCrowdingDistance(List<Individual> front) {
        if (front.size() <= 2) {
            for (Individual individual : front) {
                individual.setCrowdingDistance(Double.MAX_VALUE);
            }
            return;
        }
        
        // Inicializa distâncias
        for (Individual individual : front) {
            individual.setCrowdingDistance(0.0);
        }
        
        // Para cada objetivo
        calculateCrowdingDistanceForObjective(front, Individual::getTotalCost, true);
        calculateCrowdingDistanceForObjective(front, Individual::getTotalReliability, false);
    }
    
    private void calculateCrowdingDistanceForObjective(List<Individual> front, 
                                                     java.util.function.Function<Individual, Double> objectiveExtractor,
                                                     boolean minimize) {
        // Ordena por objetivo
        front.sort(Comparator.comparingDouble(objectiveExtractor::apply));
        
        // Extremos têm distância infinita
        front.get(0).setCrowdingDistance(Double.MAX_VALUE);
        front.get(front.size() - 1).setCrowdingDistance(Double.MAX_VALUE);
        
        // Calcula range do objetivo
        double minValue = objectiveExtractor.apply(front.get(0));
        double maxValue = objectiveExtractor.apply(front.get(front.size() - 1));
        double range = maxValue - minValue;
        
        if (range > 0) {
            for (int i = 1; i < front.size() - 1; i++) {
                double distance = (objectiveExtractor.apply(front.get(i + 1)) - 
                                 objectiveExtractor.apply(front.get(i - 1))) / range;
                Individual individual = front.get(i);
                individual.setCrowdingDistance(individual.getCrowdingDistance() + distance);
            }
        }
    }
    
    private double getTotalCost(Individual individual) {
        return individual.getSolution().getTotalCost();
    }
    
    private double getTotalReliability(Individual individual) {
        return individual.getSolution().getTotalReliability();
    }
    
    private void logProgress() {
        double avgCost = population.stream()
                .mapToDouble(ind -> ind.getSolution().getTotalCost())
                .average().orElse(0.0);
        
        double avgReliability = population.stream()
                .mapToDouble(ind -> ind.getSolution().getTotalReliability())
                .average().orElse(0.0);
        
        logger.info("Generation {}: Archive size = {}, Avg cost = {:.2f}, Avg reliability = {:.3f}",
                   currentGeneration, archive.size(), avgCost, avgReliability);
    }
    
    // Getters para estatísticas
    public List<Individual> getPopulation() {
        return new ArrayList<>(population);
    }
    
    public ParetoArchive getArchive() {
        return archive;
    }
    
    public long getExecutionTime() {
        return endTime - startTime;
    }
    
    public List<Double> getHyperVolumeHistory() {
        return new ArrayList<>(hyperVolumeHistory);
    }
    
    public List<Integer> getArchiveSizeHistory() {
        return new ArrayList<>(archiveSizeHistory);
    }
    
    public int getCurrentGeneration() {
        return currentGeneration;
    }
} 