package de.dailab.jiacvi.aot.gridworld.myAgents

import de.dailab.jiacvi.Agent
import de.dailab.jiacvi.aot.gridworld.*
import de.dailab.jiacvi.behaviour.act
import java.io.File
import java.io.IOException


data class StartAgentMessage(val size: Position, val repairIds: List<String>, val collectorIDs: List<String>,val repairPoints: List<Position>, val obstacles: List<Position>)

