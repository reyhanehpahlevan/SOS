# S.O.S 
This code is for RoboCup Rescue, Agent Simulation League and it's written for AmirKabir Robotic center, Amirkabir University of Technology. Many version of this code is released during the years in sourceforge but here is the latest and the most reliable version.

## Agent Simulation competition
The Agent Simulation competition involves primarily evaluating the performance of agent teams on different maps of the RoboCup Rescue Agent Simulation (RCRS) platform, a comprehensive simulation environment for research in disaster response management.. Specifically, this competition involves evaluating the effectiveness of Ambulances, Police Forces, and Fire Brigades agents on rescuing civilians and extinguishing fires in cities where an earthquake has just happened. This competition is composed of a preliminary round, a semi-final round, and a final round. Each round is composed of a set of maps representing different possible situations used for evaluating and scoring each agent team at each round. A consolidated score is assigned to each team per round.

## Agent Development Kit
One of the most important goals of Robocop Rescue simulation is providing a base to help researchers implement their strategies. So we suggested adding Agent Development Kit (ADK) as Infrastructure field competition in RoboCup 2014. Our multi-layer and state-base code design, helps teams to separate agents’ strategies, implement and test many high level strategies and AI methods.
![S.O.S Multi Layer Structure](/doc/imgs/0-ADK.png)

## Authors
Fatemeh Pahlevan Aghababa, Ali Modaresi, Morteza Faghani, Hesam Akbari, Amirreza Kabiri, Salim Malakouti

# Getting started!
## Requirements
The recommended Java version for this project is Java 8 and the latest tested version is Java SE 8 [1.8.0_25].

## Server/Client
The repository contains two Project (sos-server & sos-client) and each has their own run & setup scripts. 
1. you first need to setup the "sos-server" to load the simulated map GUI and kernel GUI
2. After the GUIs are up and running, you can build "sos-client" project and choose your main file like (LaunchAgents.java).
3. Now the SOS agent GUI will be shown where you can are asked to select how many agents per type you want to use in the simulation. After selecting the number, you need to press "ok" button for launching the agent on the map GUI. This GUI was added because we are dealing with multi-agent system where each map may have hundreds of agents and it may not be feasible in terms of ram usage to run all the agents on your personal laptop.
4. You can press the "run" or "step" button on your map GUI to start the simulation. Now you should see the earthquick has happend, roads are blocked, buildings are in fire and civilians are damaged. Note that the fisrt 3 cycles will be used for precompute planning and the agents will act aftrewards.

## SOS-server
The server is already compiled with "Apache Ant(TM) version 1.9.4" and you just need to run it using the "scriptedstartkernel.sh" which is located in "sos-server/boot/script" repository.

```
cd ./SOS_rescue_agent/sos-server/boot/script
sh scriptedstartkernel.sh -map Kobe1 -team sos
```

![run server command](/doc/imgs/3-run_server.png)

### GUIs
There are two main GUIs in the server side, The first one is "Map" GUI which is used to simulate the city's map and the other is "Kernel" GUI which could be used to manage agents during the runtime and adding more simulators such as smoke or aftershock to the simulation.

![Import Project in Eclipse](/doc/imgs/5-run_server_GUIs.png)

Kernel GUI is used to define the disaster scenario, manage agenet and show server logs.

![Import Project in Eclipse](/doc/imgs/8-run_server_GUI_kernel.png)

Map GUI contains two pannel, the information panel is used to show some data about the map's element after you select an element and the options panel is used to change the style of the map(only show buildings, only show roads,...)

![Import Project in Eclipse](/doc/imgs/6-run_server_GUI_map_panel1.png)
![Import Project in Eclipse](/doc/imgs/7-run_server_GUI_map_panel2.png)

### Maps
There are couple of maps available in the "sos-server/boot/maps" directory where you can use their directory's name in the previous command such as "Kobe1", "Paris1", "Maxico1", ..

![Import Project in Eclipse](/doc/imgs/4-run_server_map_options.png)



## SOS-client
### Import the project
If you are using Eclipse as an IDE, you can create "New project" and then change the default location of the project to the path of "sos-client" directory.
![Import Project in Eclipse](/doc/imgs/1-import_project.png)
