package de.dailab.jiacvi.aot.gridworld

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.behaviour.act
import java.time.Duration

const val SERVER_NAME = "server"
const val BROADCAST_TOPIC = "broadcast"


class ServerAgent(
		private val revealObstacles: Boolean = true,
		private val logGames: Boolean = false,
		private val logFile: String = ""
): Agent(overrideName = SERVER_NAME) {

	// msgBroker only used for sending broadcasts
	private val msgBroker by resolve<BrokerAgentRef>()
	private var activeGame: GridworldGame? = null
	private var running = false

	override fun behaviour() = act {

		// start or restart a new game
		respond<SetupGameMessage, SetupGameResponse> { msg ->
			log.info("Received $msg")
			if (activeGame != null) log.warn("Restarting game with game still running.")

			// read gridfile, start with turn=-1 so that after increment in execute the first turn is 0
			val game = Util.loadGameFromFile(msg.gridfile)

			// set up logging if active
			if (logFile.isNotEmpty()) game.logfile=logFile


			activeGame = game

			// send response
			SetupGameResponse(game.size, game.worker.filter { it.value.collector }.keys.toList(),
				game.worker.filter { !it.value.collector }.keys.toList(), game.repairPositions.values.toList(),
				if (revealObstacles) game.obstacles.toList() else null
			)
		}

		// actually start the loaded game
		respond<StartGame, Boolean> { msg ->
			log.info("Received $msg")
			if (activeGame == null || running) {return@respond false}
			running = true
			true
		}



		// manage active game
		every(Duration.ofMillis(200)) {
			if (activeGame != null && running) {
				val game = activeGame!!
				game.turn++
				// turns over or everything repaired
				if (game.turn > game.maxTurns || game.repairPositions.all { it.value == Position(-1,-1) }) {
					finishGame(game)
				}
				// broadcast current turn, redundant but maybe necessary for syncing
				msgBroker.publish(BROADCAST_TOPIC, GameTurnInform(game.turn))

				// tell each worker turn, position and vision of materials
				publishPosition(game)

				// logging if active
				if (logGames) game.log()

				// print grid to debug
				println("\n${game.prettyPrint()}\n")
			}
		}


		// handle incoming requests from workers, check model and message files for details
		respond<WorkerActionRequest, WorkerActionResponse> { msg ->
			log.info("Received $msg")
			if (!hasActiveGame(msg.workerId)) { return@respond WorkerActionResponse(false, ActionFlag.NO_ACTIVE_GAME) }
			if (!allowedToAct(msg.workerId)) { return@respond WorkerActionResponse(false, ActionFlag.MAX_ACTIONS) }

			val game = activeGame!!
			val worker = game.worker[msg.workerId]!!

			worker.lastTurn = game.turn
			when (msg.action) {
				WorkerAction.TAKE -> tryTake(worker)
				WorkerAction.DROP -> tryDrop(worker)
				else -> tryMove(worker, msg.action)
			}
		}

		// handle transfer request, informs are repairAgent responsibility
		on<TransferMaterial> { msg ->
			log.info("Received $msg")
			if (!hasActiveGame(msg.fromID) || !hasActiveGame(msg.toID)) { return@on }
			if (!allowedToAct(msg.fromID) || !allowedToAct(msg.toID)) { return@on }

			val game = activeGame!!
			val colAgent = game.worker[msg.fromID]!!
			val repAgent = game.worker[msg.toID]!!

			// check for transfer conditions, abort if not met
			if (!colAgent.collector || repAgent.collector) { return@on }
			if (!colAgent.hasMaterial || repAgent.hasMaterial) { return@on }
			if (colAgent.position != repAgent.position) { return@on }

			// transfer material
			colAgent.hasMaterial = false
			repAgent.hasMaterial = true
			colAgent.lastTurn = game.turn
			repAgent.lastTurn = game.turn

			// inform repairAgent about transfer
			val ref = system.resolve(repAgent.id)
			ref tell TransferInform(colAgent.id)
		}

	}

	/** each turn inform all agents about current positions and available material in direct neighbourhood */
	private fun publishPosition (game: GridworldGame) {
		for ((id, worker) in game.worker) {
			val pos = worker.position
			val materialPos = pos.getSurroundingPositions().filter {
				p -> game.materials.any { it.value.position == p && it.value.available > 0} }
			val ref = system.resolve(id)
			ref tell CurrentPosition(game.turn, pos, materialPos)
		}
	}

	/** check if worker move is possible */
	private fun tryMove(worker: Worker, move: WorkerAction): WorkerActionResponse {
		if (!movePossible(worker, move)) { return WorkerActionResponse(false, ActionFlag.OBSTACLE) }

		worker.position = worker.position.applyMove(move, activeGame!!.size)!!
		return WorkerActionResponse(true, ActionFlag.NONE)
	}

	/** check if worker is collector, at material and has space to take it */
	private fun tryTake(worker: Worker): WorkerActionResponse {
		val material = activeGame!!.materials.filterValues { fs -> fs.position == worker.position && fs.available > 0}
		if (!worker.collector) { return WorkerActionResponse(false, ActionFlag.TYPE_MISMATCH) }
		if (worker.hasMaterial) { return WorkerActionResponse(false, ActionFlag.HAS_MATERIAL) }
		if (material.isEmpty()) { return WorkerActionResponse(false, ActionFlag.NO_MATERIAL) }


		val materialID = material.keys.first()
		activeGame!!.materials[materialID]!!.available--
		worker.hasMaterial = true

		return WorkerActionResponse(true)
	}

	/** check if worker is repair, at repair position and has material to drop */
	private fun tryDrop(worker: Worker): WorkerActionResponse {
		if (worker.collector) { return WorkerActionResponse(false, ActionFlag.TYPE_MISMATCH) }
		if (!worker.hasMaterial) { return WorkerActionResponse(false, ActionFlag.NO_MATERIAL) }
		val rep = activeGame!!.repairPositions.entries.find { it.value == worker.position }?.key
				?: return WorkerActionResponse(false, ActionFlag.NO_REPAIRPOINT)


		activeGame!!.repairPositions[rep] = Position(-1,-1)
		worker.hasMaterial = false

		return WorkerActionResponse(true)
	}

	/*
	* ---- collection of guard functions ----
	*/
	private fun hasActiveGame(id: String): Boolean {
		if (activeGame == null || !running) return false
		return activeGame!!.worker[id] != null
	}

	private fun allowedToAct(id: String): Boolean {
		return activeGame!!.worker[id]!!.lastTurn < activeGame!!.turn
	}


	private fun movePossible(worker: Worker, move: WorkerAction): Boolean {
		val newPos = worker.position.applyMove(move,  activeGame!!.size)
		if (worker.collector && newPos != null && activeGame!!.repairPositions.containsValue(newPos)) {
			return false
		}
		return (newPos != null && newPos !in  activeGame!!.obstacles)
	}


	/** shutdown currently running game */
	private fun finishGame(game: GridworldGame) {
		log.info("Finishing game...")
		running = false

		val repairPointsOpen = game.repairPositions.filter { it.value != Position(-1,-1) }.size
		val message = if (repairPointsOpen == 0) "all repaired :)" else "gridworld still broken :("

		system.resolve("setup") tell EndGameMessage(game.turn, repairPointsOpen, message)
		activeGame = null
	}
}
