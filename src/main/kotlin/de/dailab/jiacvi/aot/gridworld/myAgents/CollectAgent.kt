package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act

class CollectAgent (private val collectID: String): Agent(overrideName=collectID) {
    /* TODO
        - this WorkerAgent has the ability to collect material
        - NOTE: can not walk on open repairpoints, can not drop material
        - find material, collect it, start a cnp instance
        - once your cnp is done, meet the RepairAgents and transfer the material
     */

    override fun preStart() {
    }


    override fun behaviour() = act {
        on<CurrentPosition> {
            currentPos ->
            log.info("Received current position: $currentPos")
            system.resolve("server") invoke ask<WorkerActionResponse>(WorkerActionRequest(collectID, WorkerAction.SOUTHEAST)) {
                actionResponse ->
                log.info("Received worker response: $actionResponse")
            }
        }
    }
}