package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.util.*
import kotlin.random.Random

class RepairAgent (val repairID: String): Agent(overrideName=repairID) {
    /* TODO
        - this WorkerAgent has the ability to drop material
        - NOTE: can walk on open repairpoints, can not collect material
        - participate in cnp instances, meet with CollectAgent, get material
        - go to repairpoint, drop material
     */

    lateinit var size: Position
    lateinit var currentPosition: Position
    lateinit var repairIds: List<String>
    lateinit var collectorIDs: List<String>
    lateinit var repairPoints: List<Position>
    private var randomWalk = true
    private var holdingMaterial = false
    private var visited = mutableSetOf<Position>()
    private var activeMaterials = mutableSetOf<Position>()
    var acceptedCFP: CFP? = null
    var sentCNPOffer : CNPRepairAgentOffer? = null

    private var currentPath = Stack<WorkerAction>()
    private

    var obstacles: List<Position>? = null


    override fun preStart() {

    }

    fun getNearestRepairPoint():Position{
        var min: Int = Int.MAX_VALUE
        var nearestRepairPoint: Position = Position(-1,-1)
        var shortestPath: Int
        for( repairPoint in repairPoints) {
            shortestPath = shortestPath(obstacles, size, currentPosition, repairPoint).size
            if( shortestPath < min){
                min = shortestPath
                nearestRepairPoint = repairPoint
            }
        }
        return nearestRepairPoint
    }

    fun getPathToMeetingPoint(collectorPos: Position, currentPosition: Position):List<WorkerAction>{
        val wa: List<WorkerAction> = shortestPath(obstacles, size, currentPosition, collectorPos)
        return wa.slice(0..wa.size/2)
    }


    fun calculateEfficiency(collectorPos: Position, overhead:Int): Int{
        val NearestRP: Int = shortestPath(obstacles, size, currentPosition, getNearestRepairPoint()).size
        val distanceCollector: Int = shortestPath(obstacles, size, currentPosition, collectorPos).size

        return NearestRP + distanceCollector
    }

    private fun getRandomTarget(currentPosition: Position): WorkerAction? {
        val possibleActions = mutableListOf<WorkerAction>()
        for ((action, movement) in getActionPositions()) {
            val newPosition = currentPosition + movement
            if (newPosition.x in 0 until size.x && newPosition.y in 0 until size.y
                && !obstacles!!.contains(newPosition) && !visited.contains(newPosition)) {
                possibleActions.add(action)
            }
        }
        return if (possibleActions.isNotEmpty()) {
            possibleActions[Random.nextInt(possibleActions.size)]
        } else {
            null
        }
    }

    fun getPathToNearestRepairPoint(currentPosition: Position): List<WorkerAction>? {
        var min: Int = Int.MAX_VALUE
        var shortestPath: List<WorkerAction>? = null
        for( materialPosition in activeMaterials) {
            shortestPath = shortestPath(obstacles, size, currentPosition, materialPosition)
            if( shortestPath.size < min ){
                min = shortestPath.size
            }
        }
        return shortestPath
    }

    override fun behaviour() = act {
        var overhead: Int  = 0
        var offer: Int = 0

        on<StartAgentMessage> {
            size = it.size
            repairIds = it.repairIds
            collectorIDs = it.collectorIDs
            repairPoints = it.repairPoints
            obstacles = it.obstacles
        }
        listen<CFP>(CFP_TOPIC_NAME) {
                cfpMessage ->
            log.info("Received the offer")
            if (acceptedCFP == null && overhead == 0) {
                offer = calculateEfficiency(cfpMessage.mypos, overhead)
                val CNPOffer = CNPRepairAgentOffer(repairID,currentPosition, offer+1, offer)
                system.resolve(cfpMessage.collectAgentID) tell CNPOffer
                sentCNPOffer = CNPOffer
                overhead += offer
                acceptedCFP = cfpMessage
            }
            else {
                system.resolve(cfpMessage.collectAgentID) tell CNPRepairAgentOffer(repairID,Position(0,0), 0, -1)
                log.debug("Already waiting for response from ${acceptedCFP?.collectAgentID}")

            }

//                log.info("Received current position: $currentPos")
            /*system.resolve("server") invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, WorkerAction.EAST)) {
                    actionResponse ->
                log.info("Received worker response: $actionResponse")
            }*/
        }
        on<CNPCollectAgentResponse> {
            cnpResponse ->
            if (cnpResponse.collectAgentID == acceptedCFP?.collectAgentID) {
                currentPath.clear()
                currentPath.addAll(shortestPath(obstacles, size, currentPosition, sentCNPOffer!!.offeredPosition))
            }

        }
        on<TransferInform> {
            // TODO: find path to closest hole, set
            val nearest: Position = getNearestRepairPoint()
            val path: List<WorkerAction> = shortestPath(obstacles, size, currentPosition, nearest)

        }
        on<CurrentPosition> { currentPos ->
            log.info("Received current position: $currentPos")
            currentPosition = currentPos.position
            if (acceptedCFP != null) {
                log.warn("New round started while CNP running, skipping round!")
            }
            else {
                var targetAction: WorkerAction? = null;

                if (currentPath.isNotEmpty()) {
                    targetAction = currentPath.pop()
                }

                // Got to the end of path
                // Either standing on meeting point or on repair point
                if (targetAction == null) {
                    val targetCollectAgent = acceptedCFP!!.collectAgentID
                    if (holdingMaterial && !repairPoints.contains(currentPos.position) && targetCollectAgent !== null) {
                        system.resolve("server") tell TransferMaterial(repairID, targetCollectAgent)
                        holdingMaterial = false
                        currentPath.clear()
                        currentPath.addAll(getPathToNearestRepairPoint(currentPos.position)!!)
                    } else if (holdingMaterial && repairPoints.contains(currentPos.position)) {
                        targetAction = WorkerAction.DROP
                    } else if (!holdingMaterial && targetCollectAgent == null) {
                        currentPath.addAll(getPathToNearestRepairPoint(currentPos.position)!!)
                    }
                    else {
                        log.error("Got to meeting point but no target agent or material found!")
                    }
                }
                if (targetAction != null) {
                    //log.info(targetAction.toString())
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
//        system.resolve(repairID) invoke ask<CNPCollectAgentReponse> {
//                resp ->
//            log.info("Received response.")
//            if(resp.response == true){
//            }
//        }

    }

}