package de.dailab.jiacvi.aot.gridworld


import de.dailab.jiacvi.aot.gridworld.myAgents.SetupAgent
import de.dailab.jiacvi.communication.LocalBroker
import de.dailab.jiacvi.dsl.agentSystem

fun main() {

    agentSystem("Gridworld") {
        enable(LocalBroker)
        agents {
            // you can set logGames=true, logFile="logs/<name>.log" here
            add(ServerAgent())

            // this is your Agent but don't change the ID
            add(SetupAgent("setup"))
        }
    }.start()
}
