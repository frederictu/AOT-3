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

    override fun preStart() {

    }

    override fun behaviour() = act {
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