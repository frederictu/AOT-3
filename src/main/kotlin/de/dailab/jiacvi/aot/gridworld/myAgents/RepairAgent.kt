package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act

class RepairAgent (val repairID: String): Agent(overrideName=repairID) {
    /* TODO
        - this WorkerAgent has the ability to drop material
        - NOTE: can walk on open repairpoints, can not collect material
        - participate in cnp instances, meet with CollectAgent, get material
        - go to repairpoint, drop material
     */

    lateinit var size: Position
    lateinit var repairIds: List<String>
    lateinit var collectorIDs: List<String>
    lateinit var repairPoints: List<Position>
    var obstacles: List<Position>? = null

    override fun preStart() {

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
            system.resolve("server") invoke ask<WorkerActionResponse>(WorkerActionRequest(repairID, WorkerAction.EAST)) {
                    actionResponse ->
                log.info("Received worker response: $actionResponse")
            }
        }
    }

}