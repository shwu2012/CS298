import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class BPSOSearch {
	private static final Logger log = Logger.getLogger(BPSOSearch.class.getName());

	// this would be the same as the number of CPUs
	private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private static final double W = 1.2;
	private static final double C1 = 1.49;
	private static final double C2 = 1.49;
	private static final double ALPHA = 0.85;
	private static final double BETA = 0.15;

	private final ExecutorService pool;

	private final int numIterations;
	private final int numParticles;
	private int dimension;

	private ArrayList<ArrayList<Integer>> pbests = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Integer>> currentPositions = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Double>> velocities = new ArrayList<ArrayList<Double>>();
	private ArrayList<Integer> gbest = new ArrayList<Integer>();

	private final ArrayList<DataPoint> dataSets;

	public BPSOSearch(int numIterations, int numParticles, String inputFilePath) {
		this.numIterations = numIterations;
		this.numParticles = numParticles;
		this.dataSets = readFiles(inputFilePath);
		this.pool = Executors.newFixedThreadPool(POOL_SIZE, new ThreadFactoryBuilder()
				.setNameFormat("search-worker-%s").build());
		log.info("POOL_SIZE=" + POOL_SIZE);
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
				if (Math.random() >= 0.5) {
					position.add(0);
				} else {
					position.add(1);
				}
			}
			currentPositions.add(position);
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
	}

	private static double sigmoid(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private ArrayList<Double> calcFitnessForPositions(ArrayList<ArrayList<Integer>> positions,
			String taskNamePrefix) {
		List<Callable<Double>> calcFitnessTasks = new ArrayList<>();
		for (int i = 0; i < positions.size(); i++) {
			ArrayList<Integer> position = positions.get(i);
			calcFitnessTasks.add(new CalcFitnessTask(dataSets, position, taskNamePrefix + i));
		}
		ArrayList<Double> result = new ArrayList<>();
		try {
			for (Future<Double> taskFuture : pool.invokeAll(calcFitnessTasks)) {

				result.add(taskFuture.get());
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return result;
	}

	private void BPSO() {
		// Initialize the positions and velocities.
		initialization();

		log.info(">>>> start PSO iterations");
		double fitnessGbest = -1.0;
		ArrayList<Double> fitnessPbests = new ArrayList<Double>();
		for (int i = 0; i < numIterations; i++) {
			log.info(">>>> start iteration: " + i + "/" + numIterations);
			stopwatch.reset().start();

			// Calculate fitness of all particles on current positions.
			ArrayList<Double> fitnessValues = calcFitnessForPositions(currentPositions,
					"iteration-" + i + "-");

			if (i == 0) {
				for (int j = 0; j < numParticles; j++) {
					ArrayList<Integer> currentPosition = currentPositions.get(j);
					double fitnessValue = fitnessValues.get(j);

					// Initialize pbests.
					pbests.add(new ArrayList<Integer>(currentPosition));
					// Initialize fitnessPbests.
					fitnessPbests.add(fitnessValues.get(j));
					// Initialize gbest.
					if (fitnessValue > fitnessGbest) {
						gbest = new ArrayList<>(currentPosition);
						fitnessGbest = fitnessValue;
					}
				}
			} else {
				for (int j = 0; j < numParticles; j++) {
					ArrayList<Integer> currentPosition = currentPositions.get(j);
					double fitnessValue = fitnessValues.get(j);

					// Found a better position?
					if (fitnessValue > fitnessPbests.get(j)) {
						// Update pbests.
						pbests.set(j, new ArrayList<Integer>(currentPosition));
						// Update fitnessPbests.
						fitnessPbests.set(j, fitnessValue);
						// Update gbest.
						if (fitnessValue > fitnessGbest) {
							gbest = new ArrayList<>(currentPosition);
							fitnessGbest = fitnessValue;
						}
					}
				}
			}

			// Update the positions and velocities.
			for (int j = 0; j < numParticles; j++) {
				ArrayList<Integer> currentPosition = currentPositions.get(j);
				ArrayList<Integer> pbest = pbests.get(j);
				ArrayList<Double> velocity = velocities.get(j);

				for (int k = 0; k < dimension; k++) {
					// Update velocity
					velocity.set(k,
							sigmoid(W * velocity.get(k) + C1 * Math.random()
									* (pbest.get(k) - currentPosition.get(k)) + C2 * Math.random()
									* (gbest.get(k) - currentPosition.get(k))));
					// Update position
					if (velocity.get(k) > Math.random()) {
						currentPosition.set(k, 1);
					} else {
						currentPosition.set(k, 0);
					}
				}
			}
			stopwatch.stop();
			log.info("iteration " + i + " done. " + stopwatch);

			System.out.println("ITERATIONS: " + i);
			System.out.println("GBEST: " + gbest);
			System.out.println("FITNESS OF GBEST: " + fitnessGbest);
			System.out.println("PBESTS: " + pbests);
			System.out.print("FITNESS OF PBESTS: ");
			System.out.print(fitnessPbests);
			System.out.println();
			System.out.println("CURRENT POSITIONS: " + currentPositions);
			System.out.print("FITNESS OF LAST ROUND'S CURRENT POSITIONS: ");
			System.out.print(fitnessValues);
			System.out.println();
			System.out.println();
		}
	}

	public static void main(String[] args) {
		if (args.length == 3) {
			final int numIterations = Integer.parseInt(args[0]);
			final int numParticles = Integer.parseInt(args[1]);
			final String filePath = args[2];
			log.info("numIterations: " + numIterations + ", numParticles: " + numParticles
					+ ", filePath: " + filePath);
			BPSOSearch bs = new BPSOSearch(numIterations, numParticles, filePath);
			bs.BPSO();
			log.info("exit");
		} else {
			System.err.println("Usage:");
			System.err.println("BPSOSearch <numIterations> <numParticles> <filePath>");
		}
	}

	private static class CalcFitnessTask implements Callable<Double> {
		private static final Logger log = Logger.getLogger(CalcFitnessTask.class.getName());

		private final ArrayList<Integer> position;

		private final ArrayList<DataPoint> dataSets;

		private final String taskName;

		public CalcFitnessTask(ArrayList<DataPoint> dataSets, ArrayList<Integer> position,
				String taskName) {
			this.dataSets = dataSets;
			this.position = position;
			this.taskName = taskName;
		}

		@Override
		public Double call() throws Exception {
			Stopwatch stopwatch = Stopwatch.createStarted();

			// Change the following line for different fitness evaluator.
			AbstractFeatureSelectionEvaluator evaluator = new KNN(5, position, dataSets);

			double result = evaluator.calcFitness(ALPHA, BETA, -1 /* no sampling */);
			stopwatch.stop();
			log.info("CalcFitnessTask #" + taskName + " finished in " + stopwatch);
			return result;
		}
	}
}
