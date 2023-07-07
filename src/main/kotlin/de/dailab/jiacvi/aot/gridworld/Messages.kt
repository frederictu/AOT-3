package de.dailab.jiacvi.aot.gridworld


/** SetupAgent -> Server (ask), replied with SetupGameResponse */
data class SetupGameMessage(val setupId: String, val gridfile: String)
data class SetupGameResponse(
        /** width and height of current grid */
        val size: Position,
        /** list of different types of workers */
        val collectorIDs: List<String>,
        val repairIDs: List<String>,
        /** positions of repairpoints */
        val repairPoints: List<Position>,
        /** only if activated: list of obstacle positions */
        val obstacles: List<Position>?
)

/** SetupAgent -> Server (tell), starts the game, replied with boolean */
data class StartGame(val setupId: String)
/** Server -> SetupAgent (tell), result = succes if 0 open repairs */
data class EndGameMessage(val turn: Int, val openRepairs: Int, val result: String)

/** Server -> "broadcast", current turn */
data class GameTurnInform(val gameTurn: Int)
/** Server -> WorkerAgent (tell) current Position of Agent and surrounding material positions if any */
data class CurrentPosition(val gameTurn: Int, val position: Position, val vision: List<Position>)

/** WorkerAgent -> Server (ask), replied with WorkerActionResponse */
data class WorkerActionRequest(val workerId: String, val action: WorkerAction)
/** flags: see Model.kt comments **/
data class WorkerActionResponse(val state: Boolean, val flag: ActionFlag = ActionFlag.NONE)

/** WorkerAgent -> Server (tell) try material transfer collector to repair */
data class TransferMaterial(val fromID: String, val toID: String)
/** Server -> RepairAgent (tell), only to on success inform repair from which collector material taken */
data class TransferInform(val fromID: String)
