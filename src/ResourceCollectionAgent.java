/**
 *  Strategy Engine for Programming Intelligent Agents (SEPIA)
    Copyright (C) 2012 Case Western Reserve University

    This file is part of SEPIA.

    SEPIA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SEPIA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SEPIA.  If not, see <http://www.gnu.org/licenses/>.
 */
//package edu.cwru.sepia.agent;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.experiment.Configuration;
import edu.cwru.sepia.experiment.ConfigurationValues;
import edu.cwru.sepia.agent.Agent;

/**
 * This agent will first collect gold to produce a peasant,
 * then the two peasants will collect gold and wood separately until reach goal.
 */

public class ResourceCollectionAgent extends Agent {
	
	private static final long serialVersionUID = -4047208702628325380L;
	private static final Logger logger = Logger.getLogger(ResourceCollectionAgent.class.getCanonicalName());

	private int goldRequired;
	private int woodRequired;
	private int step;
	
	public ResourceCollectionAgent(int playernum, String[] arguments) {
		super(playernum);
		goldRequired = Integer.parseInt(arguments[0]);
		woodRequired = Integer.parseInt(arguments[1]);
	}

	StateView currentState;
	
	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) {
		step = 0;
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer,Action> middleStep(StateView newState, History.HistoryView statehistory) {
		step++;
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		Map<Integer,Action> builder = new HashMap<Integer,Action>();
		currentState = newState;
		
		int currentGold = currentState.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = currentState.getResourceAmount(0, ResourceType.WOOD);
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		
		List<Integer> allUnitIDs = currentState.getUnitIds(playernum);
		List<Integer> peasantIDs = new ArrayList<Integer>();
		List<Integer> townhallIDs = new ArrayList<Integer>();
		List<Integer> farmIDs = new ArrayList<Integer>();
		List<Integer> barracksIDs = new ArrayList<Integer>();
		List<Integer> footmanIDs = new ArrayList<Integer>();
		
		
		for(int i=0; i<allUnitIDs.size(); i++) {
			int id = allUnitIDs.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			if(unitTypeName.equals("TownHall"))
				townhallIDs.add(id);
			if(unitTypeName.equals("Peasant"))
				peasantIDs.add(id);
			if(unitTypeName.equals("Farm"))
				farmIDs.add(id);
			if(unitTypeName.equals("Barracks"))
				barracksIDs.add(id);
			if(unitTypeName.equals("Footman"))
				footmanIDs.add(id);
		}
		
		List<Integer> enemyUnitIDs = currentState.getAllUnitIds();
		enemyUnitIDs.removeAll(allUnitIDs);
		
		if(peasantIDs.size() >= 3) {  // collect resources
			
			if(farmIDs.size() < 1 && currentGold >= 500 && currentWood >= 250) {
				System.out.println("Building a Farm");
				int idOFpeasant = peasantIDs.get(0);
				Action action = Action.createPrimitiveBuild(idOFpeasant, currentState.getTemplate(playernum, "Farm").getID());
				builder.put(idOFpeasant, action);
			}
			else if(barracksIDs.size() < 1 && currentGold >= 700 && currentWood >= 400) {
				System.out.println("Build a Barracks");
				int idOFpeasant = peasantIDs.get(0);
				Action action = Action.createPrimitiveBuild(idOFpeasant, currentState.getTemplate(playernum, "Barracks").getID());
				builder.put(idOFpeasant, action);
			}
			else if(barracksIDs.size() > 0 && footmanIDs.size() < 2 && currentGold >= 600 ) {
				System.out.println("Build a Footman");
				int idOFbarrack = barracksIDs.get(0);
				Action action = Action.createCompoundProduction(idOFbarrack, currentState.getTemplate(playernum, "Footman").getID());
				builder.put(idOFbarrack, action);
			}
			else {
				if(footmanIDs.size() >= 2) { //attack enemies
					System.out.println("Attacking enemies");
					for(int i : footmanIDs) {
						Action action = Action.createCompoundAttack(i, enemyUnitIDs.get(0));
						builder.put(i, action);
					}
				}
				
				int idOFpeasant = peasantIDs.get(1);
				int idOFtownhall = townhallIDs.get(0);
				Action action = null;
				
				if(currentState.getUnit(idOFpeasant).getCargoAmount()>0) {
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDDEPOSIT, idOFtownhall);
				}
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.TREE);
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				
				builder.put(idOFpeasant, action);
				idOFpeasant = peasantIDs.get(0);
				
				if(currentState.getUnit(idOFpeasant).getCargoType() == ResourceType.GOLD && currentState.getUnit(idOFpeasant).getCargoAmount()>2) {
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDDEPOSIT, idOFtownhall);
				}
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				
				builder.put(idOFpeasant, action);
				idOFpeasant = peasantIDs.get(2);
				
				if(currentState.getUnit(idOFpeasant).getCargoType() == ResourceType.GOLD && currentState.getUnit(idOFpeasant).getCargoAmount()>0) {
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDDEPOSIT, idOFtownhall);
				}
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(idOFpeasant, action);
			}
     }
			
		
		else {  // build peasant
			if(currentGold>=400) {
				if(logger.isLoggable(Level.FINE))
				{
					logger.fine("already have enough gold to produce a new peasant.");
				}
				TemplateView peasanttemplate = currentState.getTemplate(playernum, "Peasant");
				int peasanttemplateID = peasanttemplate.getID();
				if(logger.isLoggable(Level.FINE))
				{
					logger.fine(String.valueOf(peasanttemplate.getID()));
				}
				int idOFtownhall = townhallIDs.get(0);
					builder.put(idOFtownhall, Action.createCompoundProduction(idOFtownhall, peasanttemplateID));
			} 
			else {
				// collecting gold
				int idOFpeasant = peasantIDs.get(0);
				int idOFtownhall = townhallIDs.get(0);
				Action action = null;
				if(currentState.getUnit(idOFpeasant).getCargoType() == ResourceType.GOLD && currentState.getUnit(idOFpeasant).getCargoAmount()>0)
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDDEPOSIT, idOFtownhall);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					action = new TargetedAction(idOFpeasant, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(idOFpeasant, action);
			}
		}
		return builder;
	}

	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
		step++;
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		int currentGold = newstate.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(0, ResourceType.WOOD);
		
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Congratulations! You have finished the task!");
		}
	}
	
	public static String getUsage() {
		return "Two arguments, amount of gold to gather and amount of wood to gather";
	}
	@Override
	public void savePlayerData(OutputStream os) {
		//this agent lacks learning and so has nothing to persist.
		
	}
	@Override
	public void loadPlayerData(InputStream is) {
		//this agent lacks learning and so has nothing to persist.
	}
}
