package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.BrokerAgentRef
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*
import kotlin.random.Random

class CollectAgent(private val collectID: String) : Agent(overrideName = collectID) {
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

    // msgBroker only used for sending broadcasts
    private val msgBroker by resolve<BrokerAgentRef>()

    private lateinit var size: Position
    private lateinit var repairIds: List<String>
    private lateinit var collectorIDs: List<String>
    private lateinit var repairPoints: List<Position>
    private var obstacles: List<Position>? = null

    private var randomWalk = true
    private var holdingMaterial = false
    private var currentPath = Stack<WorkerAction>()
    private var activeMaterials = mutableSetOf<Position>()
    private var deadMaterials = mutableSetOf<Position>()
    private var visited = mutableSetOf<Position>()
    private var targetRepairAgentID: String? = null
    private var myPosition: Position? = null

    private var CNPStarted = false
    private var CNPOffers = mutableListOf<CNPRepairAgentOffer>()
    private var meetupDeadline: Int? = null

    override fun preStart() {
    }

    private fun getRandomTarget(currentPosition: Position): WorkerAction? {
        val possibleActions = mutableListOf<WorkerAction>()
        for ((action, movement) in getActionPositions()) {
            val newPosition = currentPosition + movement
            if (newPosition.x in 0 until size.x && newPosition.y in 0 until size.y
                && !obstacles!!.contains(newPosition) && !visited.contains(newPosition)
            ) {
                possibleActions.add(action)
            }
        }
        return if (possibleActions.isNotEmpty()) {
            possibleActions[Random.nextInt(possibleActions.size)]
        } else {
            null
        }
    }

    private fun getPathToNearestMaterial(currentPosition: Position): List<WorkerAction>? {
        var min: Int = Int.MAX_VALUE
        var shortestPath: List<WorkerAction>? = null
        for (materialPosition in activeMaterials) {
            shortestPath = shortestPath(obstacles, size, currentPosition, materialPosition)
            if (shortestPath.size < min) {
                min = shortestPath.size
            }
        }
        return shortestPath
    }

    private fun getBestOffer(): CNPRepairAgentOffer? {
        return CNPOffers.minBy { it.deadline }
    }

    override fun behaviour() = act {
        on<StartAgentMessage> {
            size = it.size
            repairIds = it.repairIds
            collectorIDs = it.collectorIDs
            repairPoints = it.repairPoints
            obstacles = it.obstacles
        }
        on<CNPRepairAgentOffer> { newOffer ->
            CNPOffers.add(newOffer)
            if (CNPOffers.size >= repairIds.size) {
                val bestOffer = getBestOffer()
                if (bestOffer == null) {
                    log.error("No best offer found!! This should not happen.")
                }
                CNPOffers.forEach { offer ->
                    val accepted = offer.repairAgentID == bestOffer!!.repairAgentID
                    log.debug("Sending offer response: {} {}", offer.repairAgentID, accepted)
                    system.resolve(offer.repairAgentID) tell CNPCollectAgentResponse(collectID, accepted)
                    if (accepted) {
                        currentPath.clear()
                        currentPath.addAll(shortestPath(obstacles, size, myPosition!!, offer.offeredPosition))
                        meetupDeadline = offer.deadline
                    }
                }
                CNPStarted = false
                CNPOffers.clear()
            }

        }
        on<CurrentPosition> { currentPositionMessage ->
            myPosition = currentPositionMessage.position
            log.info("Received current position: $currentPositionMessage")
            if (CNPStarted) {
                log.warn("New round started while CNP running! Skipping requests this round.")
            } else {
                visited.add(currentPositionMessage.position)
                if (randomWalk) {
                    currentPositionMessage.vision.forEach { materialStorage ->
                        if (!deadMaterials.contains(materialStorage)) {
                            activeMaterials.add(materialStorage)
                        }
                    }
                    if (!holdingMaterial && activeMaterials.isNotEmpty()) {
                        randomWalk = false
                        currentPath.clear()
                        currentPath.addAll(
                            shortestPath(
                                obstacles,
                                size,
                                currentPositionMessage.position,
                                activeMaterials.first()
                            )
                        )
                    } else {
                        val randomTarget = getRandomTarget(currentPositionMessage.position)
                        currentPath.clear()
                        currentPath.add(randomTarget)
                    }
                }

                var targetAction: WorkerAction? = null;

                if (currentPath.isNotEmpty()) {
                    targetAction = currentPath.pop()
                }

                // Got to the end of path
                // Either standing on meeting point or on material
                if (targetAction == null) {
                    if (holdingMaterial && targetRepairAgentID != null && meetupDeadline != null &&
                        meetupDeadline!! == currentPositionMessage.gameTurn
                    ) {
                        targetRepairAgentID = null
                        meetupDeadline = -1
                        currentPath.clear()
                        currentPath.addAll(getPathToNearestMaterial(currentPositionMessage.position)!!)
                    } else if (!holdingMaterial && activeMaterials.contains(currentPositionMessage.position)) {
                        targetAction = WorkerAction.TAKE
                    } else {
                        log.error("Got to meeting point but no target agent or material found! My id: $collectID, my pos: ${currentPositionMessage.position}")
                    }
                }
                if (targetAction != null) {
                    system.resolve("server") invoke ask<WorkerActionResponse>(
                        WorkerActionRequest(collectID, targetAction)
                    ) { actionResponse ->
                        log.info("Received worker response: $actionResponse")

                        // Tried to take material
                        if (targetAction == WorkerAction.TAKE) {
                            // Start CNP
                            if (actionResponse.state) {
                                msgBroker.publish(CFP_TOPIC_NAME, CFP(collectID, currentPositionMessage.position))
                                holdingMaterial = true
                                CNPStarted = true

                            } else {
                                activeMaterials.remove(currentPositionMessage.position)
                                deadMaterials.add(currentPositionMessage.position)
                                if (activeMaterials.isEmpty()) {
                                    randomWalk = true
                                } else {
                                    currentPath.clear()
                                    currentPath.addAll(
                                        shortestPath(
                                            obstacles,
                                            size,
                                            currentPositionMessage.position,
                                            activeMaterials.first()
                                        )
                                    )
                                }
                            }
                        }
                        // Tried to make a move
                        else {
                            if (actionResponse.state) {

                            } else {
                                log.error("Move failed!! This should not happen.")
                            }
                        }
                    }
                }

            }
            /*TODO: - GET POSITIONS FROM EACH REPAIR-AGENT
           - GET EFFICIENCIES OF EACH AGENT
           - ACCEPT MOST EFFICIENT AGENT AND GO TO HIM ON SHORTEST PATH
           - GET DISTANCES

        */
        }

    }
}