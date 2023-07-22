package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*

class CollectAgent (private val collectID: String): Agent(overrideName=collectID) {
    /* TODO
        - this WorkerAgent has the ability to collect material
        - NOTE: can not walk on open repairpoints, can not drop material
        - find material, collect it, start a cnp instance
        - once your cnp is done, meet the RepairAgents and transfer the material
        CNP
        - notify all repair Agents as soon as Materials are found
        - get offers from Agents (offers are calculated execution times)
        - accept most efficient agent and go to dropoff
     */

    lateinit var size: Position
    lateinit var repairIds: List<String>
    lateinit var collectorIDs: List<String>
    lateinit var repairPoints: List<Position>
    var obstacles: List<Position>? = null

    var randomWalk = true
    var holdingMaterial = false
    var currentPath = Stack<WorkerAction>()
    var activeMaterials = mutableSetOf<Position>()
    var deadMaterials = mutableSetOf<Position>()
    var visited = mutableSetOf<Position>()

    override fun preStart() {
    }


    fun getRandomTarget(currentPosition: Position) {
        var possiblePositions = mutableListOf<Position>(
            currentPosition + Position(0, 1),
            currentPosition + Position(1, 0),
            currentPosition + Position(1, 1),
            currentPosition + Position(0, -1),
            currentPosition + Position(-1, 0),
            currentPosition + Position(-1, -1),
            currentPosition + Position(1, -1),
            currentPosition + Position(-1, 1)
            )
        return possiblePositions.filter {
            position ->
            position.
        }
    }

    override fun behaviour() = act {
        on<StartAgentMessage> {
            size = it.size
            repairIds = it.repairIds
            collectorIDs = it.collectorIDs
            repairPoints = it.repairPoints
            obstacles = it.obstacles
        }
        on<CurrentPosition> {
            currentPos ->
            log.info("Received current position: $currentPos")
            visited.add(currentPos.position)
            if (randomWalk) {
                currentPos.vision.forEach {
                    materialStorage ->
                    if (!deadMaterials.contains(materialStorage)) {
                        activeMaterials.add(materialStorage)
                    }
                }
                if (!holdingMaterial && activeMaterials.isNotEmpty()) {
                    randomWalk = false
                    currentPath.clear()
                    currentPath.addAll(shortestPath(obstacles, size, currentPos.position, activeMaterials.first()))
                }
                else {
                    randomTarget = getRandomTarget(currentPos.position)
                }
            }

            system.resolve("server") invoke ask<WorkerActionResponse>(WorkerActionRequest(collectID, WorkerAction.SOUTHEAST)) {
                actionResponse ->
                log.info("Received worker response: $actionResponse")
            }
        }
        /*TODO: - GET POSITIONS FROM EACH REPAIR-AGENT
           - GET EFFICIENCIES OF EACH AGENT
           - ACCEPT MOST EFFICIENT AGENT AND GO TO HIM ON SHORTEST PATH
           - GET DISTANCES

        */

    }
}