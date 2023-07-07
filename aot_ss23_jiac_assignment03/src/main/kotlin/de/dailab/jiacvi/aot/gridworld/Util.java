package de.dailab.jiacvi.aot.gridworld;

import java.io.InputStream;
import java.util.*;



/**
 * Helper class to read gridfiles.
 */
public class Util {

	public static final Random random = new Random();



	/**
	 * Load gridworld game from the given file, to be found on the class path,
	 * more specifically in the resources/grids directory; the filename must
	 * be absolute (relative to the class-path root) and should include the
	 * root "/", e.g. "/grid/somegridfile.grid".
	 */
	public static GridworldGame loadGameFromFile(String filename) {
		InputStream is = Util.class.getResourceAsStream(filename);
		if (is == null) {
			throw new IllegalArgumentException("Invalid grid file: " + filename);
		}
		try (Scanner scanner = new Scanner(is)) {

			// first line: general game parameters
			int width = scanner.nextInt();
			int height = scanner.nextInt();
			int turns = scanner.nextInt();
			int numCollectAgents = scanner.nextInt();
			int numRepairAgents = scanner.nextInt();
			int numMaterial = scanner.nextInt();
			int numHoles = scanner.nextInt();

			final GridworldGame game = new GridworldGame(
					new Position(width, height), -1, turns, new HashMap<>(),
					new HashMap<>(), new HashMap<>(), new HashSet<>(), "logs/game-"+random.nextInt(Integer.MAX_VALUE)+".log"
			);

			// next height lines: grid and obstacles
			for (int y = 0; y < height; y++) {
				String line = scanner.next(String.format(".{%d}", width));
				for (int x = 0; x < width; x ++) {
					if (line.charAt(x) == '#') {
						game.getObstacles().add(new Position(x, y));
					}
				}
			}

			// next numCollectAgents lines: the collectors
			for (int a = 0; a < numCollectAgents; a++) {
				String id = scanner.next("\\w+");
				int x = scanner.nextInt();
				int y = scanner.nextInt();

				Worker worker = new Worker(id, true, new Position(x, y), -1, false);
				game.getWorker().put(worker.getId(), worker);
			}

			// next numRepairAgents lines: the repairs
			for (int a = 0; a < numRepairAgents; a++) {
				String id = scanner.next("\\w+");
				int x = scanner.nextInt();
				int y = scanner.nextInt();

				Worker worker = new Worker(id, false, new Position(x, y), -1, false);
				game.getWorker().put(worker.getId(), worker);
			}

			// next numMaterial lines: the materials
			for (int f = 0; f < numMaterial; f++) {
				String id = scanner.next("\\w+");
				int x = scanner.nextInt();
				int y = scanner.nextInt();
				int value    = scanner.nextInt();

				Material material = new Material(id, new Position(x, y), value, value);
				game.getMaterials().put(material.getId(), material);
			}

			// next numHoles lines: the repairPoints
			for (int a = 0; a < numHoles; a++) {
				String id = scanner.next("\\w+");
				int x = scanner.nextInt();
				int y = scanner.nextInt();

				Position hole = new Position(x, y);
				game.getRepairPositions().put(id, hole);
			}


			return game;
		}
	}

}
