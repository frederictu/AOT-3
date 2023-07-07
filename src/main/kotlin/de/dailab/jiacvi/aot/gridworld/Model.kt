package de.dailab.jiacvi.aot.gridworld

import java.io.File
import java.io.IOException
import kotlin.random.Random

enum class WorkerAction {
    NORTH,
    NORTHEAST,
    EAST,
    SOUTHEAST,
    SOUTH,
    SOUTHWEST,
    WEST,
    NORTHWEST,
    TAKE,       // take material
    DROP        // drop material at repair
}

enum class ActionFlag {
    NO_ACTIVE_GAME, // worker is not registered or no game started
    MAX_ACTIONS,    // workers can only do 1 action per turn
    OBSTACLE,       // border of grid, obstacle (#) in grid, or collector: active repairpoint
    NO_MATERIAL,        // worker has no material to drop or is no there is no material to take
    NO_REPAIRPOINT,        // worker is not at repair point while trying to drop
    HAS_MATERIAL,       // worker has material and can't take more
    TYPE_MISMATCH,      // this worker type not allowed current action
    NONE
}


data class GridworldGame(
    /** size of the grid */
    val size: Position,
    /** current game turn */
    var turn: Int,
    /** maximum number of turns in this game */
    val maxTurns: Int,
    /** workers both types mapped by id */
    val worker: MutableMap<String, Worker>,
    /** material mapped by ID */
    val materials: MutableMap<String, Material>,
    /** *repair points mapped by ID, closed have Position(-1,-1) */
    val repairPositions: MutableMap<String, Position>,
    /** set of cells that are blocked due to obstacles  */
    val obstacles: MutableSet<Position>,
    /** relative path to log file from project root */
    var logfile: String
) {
    /** Pretty-print the game state, only for debugging... */
    fun prettyPrint(): String {
        val buffer = StringBuffer()
        // some statistics about game turns, material, etc.
        buffer.append(if (turn > maxTurns) "FINAL\n" else String.format("TURN %d/%d\n", turn, maxTurns))


        buffer.append("Material Sources\n")
        materials.values.sortedBy { it.id }.forEach { buffer.append("$it\n") }

        buffer.append("Workers\n")
        worker.values.sortedBy { it.id }.forEach { buffer.append("$it\n") }



        // what is where?
        val elements = mutableMapOf<Position, Any>()
        worker.values.forEach { elements[it.position] = it }
        materials.values.filter { it.available > 0}
            .forEach { elements[it.position] = it }  // associateBy?



        // print the grid
        for (y in 0 until size.y) {
            for (x in 0 until size.x) {
                val p = Position(x, y)
                val at = elements[p]
                buffer.append(when {
                    at is Material    -> "\t${at.id}"
                    at is Worker   -> "\t${at.id}"
                    p in repairPositions.values -> "\to"
                    p in obstacles -> "\t#"
                    else           -> "\t."
                })
            }
            buffer.append("\n")
        }
        return buffer.toString()
    }

    /** Save the Pretty-printed game state, in log file */
    fun log() {
        try {
            File(this.logfile).appendText("${this.prettyPrint()}\n")
        } catch (e: IOException){
            System.err.println("Can't open logfile $e")
        }
    }



}


data class Worker(
    val id: String,
    /** agent type: false = repair */
    val collector: Boolean,
    /** current position in gridworld */
    var position: Position,
    /** turn when last WorkerAction/Transfer was executed */
    var lastTurn: Int,
    var hasMaterial: Boolean
)

data class Material(
    val id: String,
    val position: Position,
    /** initial value this material source had */
    val value: Int,
    /** amount of material not yet taken */
    var available: Int
)


data class Position(val x: Int, val y: Int) {
    companion object {
        /** get new random position within the bounds defined by the size parameter. */
        fun randomPosition(size: Position) = Position(Random.nextInt(size.x), Random.nextInt(size.y))
    }

    /**
     * Create and return new (optional) position for given move and grid size. The size
     * is optional and used for checking the maximum values, can be null if you know the
     * position will be valid (e.g. after a server confirm message)
     */
    fun applyMove(action: WorkerAction, size: Position? = null): Position? {
        var x2 = this.x
        var y2 = this.y
        when (action) {
            WorkerAction.NORTH -> y2--
            WorkerAction.NORTHEAST -> {y2--; x2++}
            WorkerAction.EAST  -> x2++
            WorkerAction.SOUTHEAST -> {y2++; x2++}
            WorkerAction.SOUTH -> y2++
            WorkerAction.SOUTHWEST -> {y2++; x2--}
            WorkerAction.WEST  -> x2--
            WorkerAction.NORTHWEST  -> {y2--; x2--}
            else -> {}
        }
        return if (size == null || (0 <= x2 && x2 < size.x && 0 <= y2 && y2 < size.y)) {
            Position(x2, y2)
        } else {
            null
        }
    }


    /** get list of all 8 surrounding positions including given, can contain invalid positions like border or obstacles */
    fun getSurroundingPositions(): List<Position> {
        val surPos = mutableSetOf<Position>()
        for (action in WorkerAction.values()) {
            surPos.add(this.applyMove(action)!!)
        }
        return surPos.toList()
    }
}
