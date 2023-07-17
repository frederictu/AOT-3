package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.io.File

class SetupAgent (private val setupID: String): Agent(overrideName=setupID) {


    /* TODO
        - setup the game using the SetupGameMessage, you need to define a gridfile
        - use the list of ids to spawn Repair & Collect Agents i.e. system.spawn(CollectAgent("x"))
        - if you need to, do some more prep work
        - start the game by telling the server "StartGame(setupID)"
     */



    override fun behaviour() = act {
        lateinit var grid: String
        grid = readFileLineByLineUsingForEachLine("example.grid", grid)

        ask<SetupGameResponse>(SetupGameMessage(setupID, grid)) {
            res ->
                log.info("Received SetupGameResponse: $res")

        }

        on<EndGameMessage> {
            log.info("Received $it")
        }
    }
}


fun readFileLineByLineUsingForEachLine(fileName: String, grid: String) {
    File(fileName).forEachLine {  }
}