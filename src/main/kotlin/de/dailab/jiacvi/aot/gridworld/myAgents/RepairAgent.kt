package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*

class RepairAgent(val repairID: String) : Agent(overrideName = repairID) {
    lateinit var size: Position
    lateinit var currentPosition: Position
    lateinit var requestedPosition: Position
    lateinit var repairIds: List<String>
    lateinit var collectorIDs: List<String>
    lateinit var repairPoints: List<Position>
    private var holdingMaterial = false

    var acceptedCFP: CFP? = null
    var sentCNPOffer: CNPRepairAgentOffer? = null
    var meetupDeadline: Int? = null
    private var firstRound = true

    private var currentPath = Stack<WorkerAction>()
    private var currentRound = 0

    var obstacles: List<Position>? = null

    fun getNearestRepairPoint(): Position {
        var min: Int = Int.MAX_VALUE
        var nearestRepairPoint: Position = Position(-1, -1)
        var shortestPath: Int
        for (repairPoint in repairPoints) {
            shortestPath = shortestPath(obstacles, size, currentPosition, repairPoint).size
            if (shortestPath < min) {
                min = shortestPath
                nearestRepairPoint = repairPoint
            }
        }
        return nearestRepairPoint
    }

    fun getPathToNearestRepairPoint(currentPosition: Position): List<WorkerAction>? {
        var min: Int = Int.MAX_VALUE
        var shortestPath: List<WorkerAction>? = null
        for (repairPosition in repairPoints) {
            shortestPath = shortestPath(obstacles, size, currentPosition, repairPosition)
            if (shortestPath.size < min) {
                min = shortestPath.size
            }
        }
        return shortestPath
    }

    override fun behaviour() = act {
        var offer: Int = 0

        on<StartAgentMessage> {
            size = it.size
            repairIds = it.repairIds
            collectorIDs = it.collectorIDs
            repairPoints = it.repairPoints
            obstacles = it.obstacles
        }
        listen<CFP>(CFP_TOPIC_NAME) { cfpMessage ->
            log.info("Received CFP")
            if (acceptedCFP == null) {
                var deadline = 0
                var meetingPoint: Position? = null
                if (holdingMaterial) {
                    val nearestRepair = getNearestRepairPoint()
                    val pathToRepairPoint = shortestPath(obstacles, size, requestedPosition, nearestRepair)
                    val pathRepairToCollectAgent = shortestPath(obstacles, size, nearestRepair, cfpMessage.mypos)
                    val totalPathLength = pathToRepairPoint.size + pathRepairToCollectAgent.size

                    var middle = totalPathLength / 2
                    if (middle > 0) {
                        middle--
                    }
                    meetingPoint = if (middle > pathToRepairPoint.size) {
                        getPathPositions(
                            nearestRepair,
                            pathRepairToCollectAgent
                        )[middle - pathToRepairPoint.size - 1]
                    } else {
                        getPathPositions(requestedPosition, pathToRepairPoint)[middle]
                    }
                    // +1 to drop the material in one round, +1 to offset 0-index
                    val myDeadline = currentRound + middle + 2
                    val theirDeadline = currentRound + (totalPathLength - middle)
                    deadline = maxOf(myDeadline, theirDeadline)
                } else {
                    val pathRepairAgentToColectAgent =
                        shortestPath(obstacles, size, requestedPosition, cfpMessage.mypos)
                    var middle = pathRepairAgentToColectAgent.size / 2
                    if (middle > 0) {
                        middle--
                    }
                    meetingPoint = getPathPositions(requestedPosition, pathRepairAgentToColectAgent)[middle]
                    log.debug("Meeting point: {}", meetingPoint)
                    log.debug("Path: {}", pathRepairAgentToColectAgent)
                    log.debug("Current round: {}", currentRound)
                    log.debug("Middle: {}", middle)
                    val myDeadline = currentRound + middle + 1
                    val theirDeadline = currentRound + (pathRepairAgentToColectAgent.size - middle)
                    deadline = maxOf(myDeadline, theirDeadline)
                }
                log.debug("Making offer to {}: {}, {}", cfpMessage.collectAgentID, meetingPoint, deadline)
                val CNPOffer = CNPRepairAgentOffer(repairID, meetingPoint, deadline)
                system.resolve(cfpMessage.collectAgentID) tell CNPOffer
                sentCNPOffer = CNPOffer
                acceptedCFP = cfpMessage
                meetupDeadline = deadline
            } else {
                system.resolve(cfpMessage.collectAgentID) tell CNPRepairAgentOffer(repairID, Position(0, 0), 0)
                log.debug("Already waiting for response from ${acceptedCFP?.collectAgentID}")

            }
        }
        on<CNPCollectAgentResponse> { cnpResponse ->
            if (cnpResponse.collectAgentID == acceptedCFP?.collectAgentID) {
                log.debug("Got picked for the job! Pathfinding to: {}", sentCNPOffer!!.offeredPosition)
                currentPath.clear()
                currentPath.addAll(shortestPath(obstacles, size, requestedPosition, sentCNPOffer!!.offeredPosition))
                log.debug("Current path: {}", currentPath)
                sentCNPOffer = null
            }

        }
        on<TransferInform> {
            holdingMaterial = true
            currentPath.clear()
            currentPath.addAll(getPathToNearestRepairPoint(requestedPosition)!!)
            log.debug("Got material, pathfinding to: {}", currentPath.last())
        }
        on<CurrentPosition> { currentPositionMessage ->
            log.info("Received current position: $currentPositionMessage")
            currentPosition = currentPositionMessage.position
            currentRound = currentPositionMessage.gameTurn
            if (firstRound) {
                val path = getPathToNearestRepairPoint(currentPosition)
                log.debug(path.toString())
                currentPath.addAll(path!!)

                firstRound = false;
            }

            if (sentCNPOffer != null) {
                log.warn("New round started while CNP running, skipping round!")
            } else {
                var targetAction: WorkerAction? = null;

                if (currentPath.isNotEmpty()) {
                    targetAction = currentPath.pop()
                }

                // Got to the end of path
                // Either standing on meeting point or on repair point
                if (targetAction == null) {
                    if (!holdingMaterial && acceptedCFP != null && meetupDeadline != null &&
                        meetupDeadline!! == currentPositionMessage.gameTurn
                    ) {
                        log.info("Picking up material")
                        system.resolve("server") tell TransferMaterial(acceptedCFP?.collectAgentID!!, repairID)
                        acceptedCFP = null
                        holdingMaterial = true
                        meetupDeadline = null
                    } else if (holdingMaterial && repairPoints.contains(currentPositionMessage.position)) {
                        targetAction = WorkerAction.DROP
                    } else if (!holdingMaterial && acceptedCFP == null) {
                        currentPath.clear()
                        currentPath.addAll(getPathToNearestRepairPoint(currentPositionMessage.position)!!)
                    } else if (meetupDeadline!! > currentPositionMessage.gameTurn && acceptedCFP != null) {
                        log.debug("Waiting for other agent (${acceptedCFP?.collectAgentID!!}) to arrive at meeting point")
                    } else {
                        log.debug("Got to destination but no target agent or material found!")
                    }
                }
                if (targetAction != null) {
                    if (targetAction != WorkerAction.DROP) {
                        requestedPosition = positionFromDirection(currentPositionMessage.position, targetAction)
                    }
                    system.resolve("server") invoke ask<WorkerActionResponse>(
                        WorkerActionRequest(repairID, targetAction)
                    ) { actionResponse ->
                        log.info("Received worker response: $actionResponse")

                        // Tried to take material
                        if (targetAction == WorkerAction.DROP) {
                            // Start CNP
                            holdingMaterial = false
                            if (actionResponse.state) {
                                log.info("Dropped material")
                            } else {
                                log.error("Tried to drop material but failed!")
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

        }
    }

}