package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.aot.gridworld.*
import java.util.*

val CFP_TOPIC_NAME: String = "CFP"

data class StartAgentMessage(val size: Position, val repairIds: List<String>, val collectorIDs: List<String>,val repairPoints: List<Position>, val obstacles: List<Position>?)
data class RepairAgentPosition(val position: Position)
data class CFP(val collectAgentID: String, val mypos: Position)
data class CNPCollectAgentResponse(val collectAgentID: String, val response: Boolean)
// When offer == -1, contract declined
data class CNPRepairAgentOffer(val repairAgentID: String, val offeredPosition: Position, val deadline: Int, val offer: Int)

data class CNPRepairAgentResult(val success:Boolean)



data class Node(val position: Position, val parent: Node?, val action: WorkerAction?, val cost: Int, val heuristic: Int) {
    val score get() = cost + heuristic
}

fun getActionPositions() : List<Pair<WorkerAction, Position>> {
    return listOf(
        WorkerAction.NORTH to Position(0, -1),
        WorkerAction.NORTHEAST to Position(1, -1),
        WorkerAction.EAST to Position(1, 0),
        WorkerAction.SOUTHEAST to Position(1, 1),
        WorkerAction.SOUTH to Position(0, 1),
        WorkerAction.SOUTHWEST to Position(-1, 1),
        WorkerAction.WEST to Position(-1, 0),
        WorkerAction.NORTHWEST to Position(-1, -1)
    )
}

fun shortestPath(obstacles: List<Position>?, gridSize: Position, currentPosition: Position, target: Position): List<WorkerAction> {


    val openList = PriorityQueue(compareBy<Node> { it.score })
    val closedList = mutableMapOf<Position, Node>()

    openList.add(Node(currentPosition, null, null, 0, currentPosition.distance(target)))

    while (openList.isNotEmpty()) {
        val current = openList.poll()

        if (current.position == target) {
            val path = mutableListOf<WorkerAction>()
            var node = current

            while (node.parent != null) {
                path.add(0, node.action!!)
                node = node.parent!!
            }

            return path
        }

        closedList[current.position] = current

        for ((action, movement) in getActionPositions()) {
            val newPosition = current.position + movement

            if (newPosition.x in 0 until gridSize.x && newPosition.y in 0 until gridSize.y && newPosition !in obstacles!! && newPosition !in closedList) {
                val cost = current.cost + 1
                val heuristic = newPosition.distance(target)
                openList.add(Node(newPosition, current, action, cost, heuristic))
            }
        }
    }

    throw IllegalStateException("No path found")
}

fun Position.distance(other: Position) = kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)

operator fun Position.minus(other: Position) = Position(x - other.x, y - other.y)

operator fun Position.plus(other: Position) = Position(x + other.x, y + other.y)
