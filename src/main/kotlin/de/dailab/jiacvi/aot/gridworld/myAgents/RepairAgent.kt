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

    var offerCollectAgentID: String? = null

    var obstacles: List<Position>? = null


    override fun preStart() {

    }

    fun getNearestRepairPoint():Position{
        var min: Int = -1
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

    fun getMeetingPoint(collectorPos: Position, currentPosition: Position):List<WorkerAction>{
        val wa: List<WorkerAction> = shortestPath(obstacles, size, currentPosition, collectorPos)
        return wa.slice(0..wa.size/2)
    }


    fun calculateEfficiency(collectorPos: Position, overhead:Int): Int{
        val NearestRP: Int = shortestPath(obstacles, size, currentPosition, getNearestRepairPoint()).size
        val distanceCollector: Int = shortestPath(obstacles, size, currentPosition, collectorPos).size

        return NearestRP + distanceCollector
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
        on<CurrentPosition> {
                currentPos ->



            }
//        system.resolve(repairID) invoke ask<CNPCollectAgentReponse> {
//                resp ->
//            log.info("Received response.")
//            if(resp.response == true){
//            }
//        }

    }

}