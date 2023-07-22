package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import de.dailab.jiacvi.dispatch.InternalMessage

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
    var overhead: Int = 0

    var obstacles: List<Position>? = null


    override fun preStart() {

    }

    fun getNearestRepairPoint():Position{
        var min: Int = -1
        var nearestRepairPoint: Position = Position(-1,-1)
        var shortestPath: Int
        for( repairPoint in repairPoints) {
            shortestPath = shortestPath(obstacles, size, currentPosition, repairPoint).size
            if( shortestPath < min || min != -1){
                min = shortestPath
                nearestRepairPoint = repairPoint
            }
        }
        return nearestRepairPoint
    }

    fun getMeetingPoint(collectorPos: Position, currentPosition: Position):List<WorkerAction>{
        val wa: List<WorkerAction> = shortestPath(obstacles, size, currentPosition, collectorPos)
        return wa.slice(0..wa.size/2)
    }

    fun calculateEfficiency(collectorPos: Position, overhead: Int): Int{
        val NearestRP: Int = shortestPath(obstacles, size, currentPosition, getNearestRepairPoint()).size
        val distanceCollector: Int = shortestPath(obstacles, size, currentPosition, collectorPos).size

        return NearestRP + distanceCollector + overhead
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
            print("Shortest path from ${currentPos.position} to ${repairPoints[0]}: ${shortestPath(obstacles, size, currentPos.position, repairPoints[0])}")
            /*system.resolve("server") invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, WorkerAction.EAST)) {
                    actionResponse ->
                log.info("Received worker response: $actionResponse")
            }*/
        }

        on<cfp> {
            log.info("Received cfp. Enhance!")
            respond<cfp, makeOffer> {
              cnpResponse ->
                return@respond makeOffer(calculateEfficiency(cnpResponse.mypos, overhead))


            }
        }
    }

}