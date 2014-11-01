import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

public class BPSOSearch {
	private static final Logger log = Logger.getLogger(BPSOSearch.class.getName());
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private static final double W = 1.2;
	private static final double C1 = 1.49;
	private static final double C2 = 1.49;
	private static final double ALPHA = 0.85;
	private static final double BETA = 0.15;

	private int numIterations;
	private int numParticles;
	private int dimension;

	private ArrayList<ArrayList<Integer>> pbests = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Integer>> positions = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Double>> velocities = new ArrayList<ArrayList<Double>>();
	private ArrayList<Integer> gbest = new ArrayList<Integer>();

	private ArrayList<DataPoint> dataSets;

	public BPSOSearch(int numIterations, int numParticles, String inputFilePath) {
		this.numIterations = numIterations;
		this.numParticles = numParticles;
		this.dataSets = readFiles(inputFilePath);
	}

	private ArrayList<DataPoint> readFiles(String inputFilePath) {
		stopwatch.reset().start();
		BufferedReader in = null;
		ArrayList<DataPoint> dataPoints = new ArrayList<>();
		try {
			in = new BufferedReader(new FileReader(inputFilePath));
			String line = null;
			String[] terms = null;
			DataPoint dataPoint = null;
			ArrayList<Double> featureValues = null;
			while ((line = in.readLine()) != null) {
				terms = line.split(",");
				dataPoint = new DataPoint();
				dataPoint.setClassName(terms[0]);
				featureValues = new ArrayList<Double>(terms.length - 1);
				for (int i = 1; i < terms.length; i++) {
					featureValues.add(Double.parseDouble(terms[i]));
				}
				dataPoint.setFeatureValues(featureValues);
				dataPoints.add(dataPoint);
			}
			this.dimension = terms.length - 1;
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		stopwatch.stop();
		log.info("dataset read. " + stopwatch);
		return dataPoints;
	}

	private void initialization() {
		// Initialize the positions
		for (int i = 0; i < numParticles; i++) {
			ArrayList<Integer> position = new ArrayList<>(dimension);
			for (int j = 0; j < dimension; j++) {
				// TODO: maybe should think of a better random method
				if (Math.random() >= 0.50) {
					position.add(0);
				} else {
					position.add(1);
				}
			}
			positions.add(position);
		}

		// Initialize the velocities
		for (int i = 0; i < numParticles; i++) {
			ArrayList<Double> velocity = new ArrayList<>(dimension);
			for (int j = 0; j < dimension; j++) {
				// TODO: maybe should think of a better random method
				velocity.add(Math.random());
			}
			velocities.add(velocity);
		}

		// Initialize the pbest to equal to initial positions
		for (int i = 0; i < numParticles; i++) {
			// TODO: is this deep copy
			pbests.add(new ArrayList<Integer>(positions.get(i)));
		}

		// Initialize the gbest
		for (int i = 0; i < dimension; i++) {
			gbest.add(0);
		}
	}

	private double fitness(ArrayList<Integer> position) {
		KNN knn = new KNN(5, position, this.dataSets);
		return knn.calcFitness(ALPHA, BETA);
	}

	private String printFitness(ArrayList<Double> fitnesses) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < numParticles; i++) {
			sb.append(fitnesses.get(i));
			sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private void BPSO() {
		initialization();

		double fitnessGbest = 0.0;
		ArrayList<Double> fitnessPbests = new ArrayList<Double>(dimension);
		ArrayList<Double> fitnessPositions = new ArrayList<Double>(dimension);

		// calculate fitnessPbests and fitnessGbest, fill fitnessPositions
		int bestIndex = -1;
		for (int i = 0; i < numParticles; i++) {
			log.info("initial calculation on fitness Pbests/Gbests start. particle: " + i + "/"
					+ numParticles);
			stopwatch.reset().start();
			double fitnessPbest = fitness(pbests.get(i));
			fitnessPbests.add(fitnessPbest);
			fitnessPositions.add(0.0);
			if (fitnessPbest > fitnessGbest) {
				bestIndex = i;
				fitnessGbest = fitnessPbest;
			}
			stopwatch.stop();
			log.info("initial calculation on fitness Pbests/Gbests is done. " + stopwatch);
		}
		log.info("initialization done.");

		// set gbest
		ArrayList<Integer> al = pbests.get(bestIndex);
		for (int i = 0; i < dimension; i++) {
			// Because Integer is in the stack, so a new Integer will be created
			// when doing the following operation.
			gbest.set(i, al.get(i));
		}

		log.info("start PSO iterations");
		stopwatch.reset();
		for (int i = 0; i < numIterations; i++) {
			log.info("start iteration: " + i + "/" + numIterations);
			for (int j = 0; j < numParticles; j++) {
				log.info("start particle: " + j + "/" + numParticles);
				stopwatch.start();

				ArrayList<Integer> position = positions.get(j);
				ArrayList<Double> velocity = velocities.get(j);
				ArrayList<Integer> pbest = pbests.get(j);
				// Update pbest
				double fitnessPosition = fitness(position);
				fitnessPositions.set(j, fitnessPosition);
				if (fitnessPosition > fitnessPbests.get(j)) {
					// update pbests, deep copy
					pbests.set(j, new ArrayList<Integer>(position));
					// update fitnessPbests
					fitnessPbests.set(j, fitnessPosition);
					// update gbest if necessary
					if (fitnessPosition > fitnessGbest) {
						for (int k = 0; k < dimension; k++) {
							// Because Integer is in the stack, so a new Integer
							// will be created when doing the following
							// operation.
							gbest.set(k, position.get(k));
						}
						fitnessGbest = fitnessPosition;
					}
				}

				for (int k = 0; k < dimension; k++) {
					// Update velocity
					velocity.set(k,
							sigmoid(W * velocity.get(k) + C1 * Math.random()
									* (pbest.get(k) - position.get(k)) + C2 * Math.random()
									* (gbest.get(k) - position.get(k))));
					// Update position
					if (velocity.get(k) > Math.random()) {
						position.set(k, 1);
					} else {
						position.set(k, 0);
					}
				}

				stopwatch.stop();
				log.info("iteration " + i + " of particle " + j + " done. " + stopwatch);
			}

			System.out.println("ITERATIONS: " + i);
			System.out.println("GBEST: " + gbest);
			System.out.println("FITNESS OF GBEST: " + fitnessGbest);
			System.out.println("PBESTS: " + pbests);
			System.out.print("FITNESS OF PBESTS: ");
			System.out.print(printFitness(fitnessPbests));
			System.out.println();
			System.out.println("CURRENT POSITIONS: " + positions);
			System.out.print("FITNESS OF LAST ROUND'S CURRENT POSITIONS: ");
			System.out.print(printFitness(fitnessPositions));
			System.out.println();
			System.out.println();
		}
	}

	public static void main(String[] args) {
		BPSOSearch bs = new BPSOSearch(20, 10, args[0]);
		bs.BPSO();
		log.info("exit");
	}
}
