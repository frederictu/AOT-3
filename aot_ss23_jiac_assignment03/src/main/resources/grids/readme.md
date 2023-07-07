Example levels for Gridworld Game
=================================

This folder contains some example levels for the Gridworld game.


File Format
-----------

Each Grid file has to be formatted as follows:

* the first line consists of 6 integers separated by spaces or tabs: `width`, `height`, `turns`, `num_collect_agents`, `num_repair_agents`, `num_material`, `num_repair_points` 
* the next `height` lines consist of `width` characters describing the respective rows of the grid; here, a `#` marks an obstacle, and a `.` marks a free space; other characters can be used to indicate the positions of material, agents, holes, but will be _ignored_ here; only the obstacles are considered
* next `num_collect_agents` lines each have a string `agent_id` and two integers `x`, `y`
* next `num_repair_agents` lines each have a string `agent_id` and two integers `x`, `y`
* next `num_material` lines each have a string `material_id` and 3 integers `x`, `y`, `initial amount`
* next `num_repair_points` lines each have a string `material_id` and 2 integers `x`, `y`


All lines after that are ignored and can be used e.g. for documentation.

Example
-------

	10 10 20 2 1 3 1
	c.r.......
	c.........
	..xxx..o..
	..........
	....##....
	....##....
	..........
	..........
	..........
	..........
	c1 0 0
	c2 1 0
	r1 0 2
	x1 2 2  1
	x2 2 3  1
	x3 2 4  1
	o1 2 7
	
	
	
	This is a comment
