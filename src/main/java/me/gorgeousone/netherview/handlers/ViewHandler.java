package me.gorgeousone.netherview.handlers;

import me.gorgeousone.netherview.ConfigSettings;
import me.gorgeousone.netherview.NetherViewPlugin;
import me.gorgeousone.netherview.blockcache.BlockCache;
import me.gorgeousone.netherview.blockcache.ProjectionCache;
import me.gorgeousone.netherview.blockcache.Transform;
import me.gorgeousone.netherview.geometry.AxisAlignedRect;
import me.gorgeousone.netherview.geometry.BlockVec;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustum;
import me.gorgeousone.netherview.geometry.viewfrustum.ViewFrustumFactory;
import me.gorgeousone.netherview.packet.PacketHandler;
import me.gorgeousone.netherview.portal.Portal;
import me.gorgeousone.netherview.portal.ProjectionEntity;
import me.gorgeousone.netherview.wrapper.Axis;
import me.gorgeousone.netherview.wrapper.blocktype.BlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handler class for running and updating the portal projections for players.
 */
public class ViewHandler {
	
	private final ConfigSettings configSettings;
	private final PortalHandler portalHandler;
	private final PacketHandler packetHandler;
	
	private final Map<UUID, Boolean> portalViewEnabled;
	private final Map<UUID, Map<Portal, PlayerViewSession>> viewSessions;
	
	public ViewHandler(ConfigSettings configSettings, PortalHandler portalHandler,
	                   PacketHandler packetHandler) {
		
		this.configSettings = configSettings;
		this.portalHandler = portalHandler;
		this.packetHandler = packetHandler;
		
		portalViewEnabled = new HashMap<>();
		viewSessions = new HashMap<>();
	}
	
	public void reload() {
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			hidePortalProjection(player);
		}
		
		viewSessions.clear();
	}
	
	/**
	 * Returns true if the player has portal viewing enabled with the /togglenetherview command.
	 */
	public boolean hasPortalViewEnabled(Player player) {
		
		if (!player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
			return false;
		}
		
		UUID playerId = player.getUniqueId();
		portalViewEnabled.putIfAbsent(playerId, true);
		return portalViewEnabled.get(player.getUniqueId());
	}
	
	/**
	 * Sets whether the player wants to see portal projections or not if they have the permission to do so.
	 */
	public void setPortalViewEnabled(Player player, boolean viewingEnabled) {
		
		if (player.hasPermission(NetherViewPlugin.VIEW_PERM)) {
			portalViewEnabled.put(player.getUniqueId(), viewingEnabled);
			
			if (!viewingEnabled) {
				hidePortalProjection(player);
			}
		}
	}
	
	public Collection<PlayerViewSession> getViewSessions() {
		
		Set<PlayerViewSession> sessions = new HashSet<>();
		
		for (Map<Portal, PlayerViewSession> playerSessions : viewSessions.values()) {
			sessions.addAll(playerSessions.values());
		}
		
		return sessions;
	}
	
	public PlayerViewSession getViewSession(Player player) {
		
		Map<Portal, PlayerViewSession> playerSessions = viewSessions.get(player.getUniqueId());
		
		if (playerSessions == null || playerSessions.isEmpty()) {
			return null;
		}
		
		return playerSessions.values().iterator().next();
	}
	
	public Collection<PlayerViewSession> getViewSessions(Player player) {
		
		Map<Portal, PlayerViewSession> playerSessions = viewSessions.get(player.getUniqueId());
		return playerSessions == null ? Collections.emptySet() : new HashSet<>(playerSessions.values());
	}
	
	public PlayerViewSession getViewSession(Player player, Portal portal) {
		
		Map<Portal, PlayerViewSession> playerSessions = viewSessions.get(player.getUniqueId());
		return playerSessions == null ? null : playerSessions.get(portal);
	}
	
	public PlayerViewSession createViewSession(Player player, Portal portal) {
		
		PlayerViewSession session = new PlayerViewSession(player, portal);
		viewSessions.computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>());
		viewSessions.get(player.getUniqueId()).put(portal, session);
		return session;
	}
	
	/**
	 * Returns true if currently a portal projection is being displayed to the player.
	 */
	public boolean hasViewSession(Player player) {
		
		Map<Portal, PlayerViewSession> playerSessions = viewSessions.get(player.getUniqueId());
		return playerSessions != null && !playerSessions.isEmpty();
	}
	
	/**
	 * Removes the player's view session and removes all sent fake blocks.
	 */
	public void hidePortalProjection(Player player) {
		
		if (!hasViewSession(player)) {
			return;
		}
		
		for (PlayerViewSession session : getViewSessions(player)) {
			hidePortalProjection(player, session.getViewedPortal());
		}
	}
	
	private void hidePortalProjection(Player player, Portal portal) {
		
		PlayerViewSession session = getViewSession(player, portal);
		
		if (session == null) {
			return;
		}
		
		packetHandler.removeFakeBlocks(player, session.getProjectedBlocks());
		packetHandler.showEntities(player, session.getHiddenEntities());
		packetHandler.hideProjectedEntities(player, session.getProjectedEntities());
		
		unregisterPortalProjection(player, portal);
	}
	
	public void projectEntity(PlayerViewSession session, ProjectionEntity projectionEntity, Transform transform) {
		
		session.getProjectedEntities().add(projectionEntity);
		packetHandler.showProjectedEntity(session.getPlayer(), projectionEntity, transform);
	}
	
	public void destroyProjectedEntity(PlayerViewSession session, ProjectionEntity entity) {
		
		session.getProjectedEntities().remove(entity);
		packetHandler.hideProjectedEntity(session.getPlayer(), entity);
	}
	
	public void showEntity(PlayerViewSession session, Entity entity) {
		
		Player player = session.getPlayer();
		
		if (player == null || player.equals(entity)) {
			return;
		}
		
		session.getHiddenEntities().remove(entity);
		packetHandler.showEntity(player, entity, entity.getEntityId(), new Transform(), false);
	}
	
	public void hideEntity(PlayerViewSession session, Entity entity) {
		
		Player player = session.getPlayer();
		
		if (player == null || player.equals(entity)) {
			return;
		}
		
		session.getHiddenEntities().add(entity);
		packetHandler.hideEntities(player, Collections.singleton(entity));
	}
	
	/**
	 * Removes any portal view related data of the player
	 */
	public void unregisterPlayer(Player player) {
		
		portalViewEnabled.remove(player.getUniqueId());
		unregisterPortalProjection(player);
	}
	
	/**
	 * Removes the player's portal projection from the system.
	 */
	public void unregisterPortalProjection(Player player) {
		viewSessions.remove(player.getUniqueId());
	}
	
	public void unregisterPortalProjection(Player player, Portal portal) {
		
		Map<Portal, PlayerViewSession> playerSessions = viewSessions.get(player.getUniqueId());
		
		if (playerSessions == null) {
			return;
		}
		
		playerSessions.remove(portal);
		
		if (playerSessions.isEmpty()) {
			viewSessions.remove(player.getUniqueId());
		}
	}
	
	/**
	 * Locates the nearest portal to a player and displays a portal projection to them (if in view range) with fake block packets.
	 */
	public void displayClosestPortalTo(Player player, Location playerEyeLoc) {
		
		if (!portalHandler.hasPortals(playerEyeLoc.getWorld())) {
			hidePortalProjection(player);
			return;
		}
		
		Set<Portal> visiblePortals = new HashSet<>();
		
		for (Portal portal : new HashSet<>(portalHandler.getPortals(playerEyeLoc.getWorld()))) {
			
			if (!portal.isLinked() || portalHandler.portalDoesNotExist(portal)) {
				hidePortalProjection(player, portal);
				continue;
			}
			
			Vector portalDistance = portal.getLocation().subtract(playerEyeLoc).toVector();
			
			if (portalDistance.lengthSquared() > configSettings.getPortalDisplayRangeSquared()) {
				hidePortalProjection(player, portal);
				continue;
			}
			
			AxisAlignedRect portalRect = portal.getPortalRect();
			
			if (portalRect.contains(playerEyeLoc.toVector())) {
				hidePortalProjection(player, portal);
				continue;
			}
			
			boolean displayFrustum = getDistanceToPortal(playerEyeLoc, portalRect) > 0.5;
			displayPortalTo(player, playerEyeLoc, portal, displayFrustum, configSettings.hidePortalBlocksEnabled());
			visiblePortals.add(portal);
		}
		
		for (PlayerViewSession session : new HashSet<>(getViewSessions(player))) {
			if (!visiblePortals.contains(session.getViewedPortal())) {
				hidePortalProjection(player, session.getViewedPortal());
			}
		}
	}
	
	/**
	 * Returns the distance of the location to the rectangle on the axis orthogonal to the axis of the rectangle.
	 */
	private double getDistanceToPortal(Location playerEyeLoc, AxisAlignedRect portalRect) {
		
		double distanceToPortal;
		
		if (portalRect.getAxis() == Axis.X) {
			distanceToPortal = portalRect.getMin().getZ() - playerEyeLoc.getZ();
		} else {
			distanceToPortal = portalRect.getMin().getX() - playerEyeLoc.getX();
		}
		
		return Math.abs(distanceToPortal);
	}
	
	private void displayPortalTo(Player player,
	                             Location playerEyeLoc,
	                             Portal portal,
	                             boolean displayFrustum,
	                             boolean hidePortalBlocks) {
		
		if (!portal.isLinked()) {
			return;
		}
		
		if (!portal.projectionsAreLoaded()) {
			portalHandler.loadProjectionCachesOf(portal);
		}
		
		portalHandler.updateExpirationTime(portal);
		portalHandler.updateExpirationTime(portal.getCounterPortal());
		
		ProjectionCache projection = ViewFrustumFactory.isPlayerBehindPortal(player, portal) ? portal.getFrontProjection() : portal.getBackProjection();
		ViewFrustum playerFrustum = ViewFrustumFactory.createFrustum(playerEyeLoc.toVector(), portal.getPortalRect(), projection.getCacheLength());
		
		PlayerViewSession session = getViewSession(player, portal);
		
		if (session == null) {
			session = createViewSession(player, portal);
		}
		
		session.setViewedPortalSide(projection);
		
		if (!displayFrustum) {
			
			if (session.getLastViewFrustum() != null) {
				
				session.setLastViewFrustum(null);
				packetHandler.showEntities(player, session.getHiddenEntities());
				packetHandler.hideProjectedEntities(player, session.getProjectedEntities());
				session.getHiddenEntities().clear();
				session.getProjectedEntities().clear();
			}
			
		} else {
			session.setLastViewFrustum(playerFrustum);
		}
		
		displayProjectionBlocks(player, session, portal, projection, playerFrustum, displayFrustum, hidePortalBlocks);
	}
	
	/**
	 * Collects and displays fake blocks for player to view the portal projection.
	 */
	private void displayProjectionBlocks(Player player,
	                                     PlayerViewSession session,
	                                     Portal portal,
	                                     ProjectionCache projection,
	                                     ViewFrustum playerFrustum,
	                                     boolean displayFrustum,
	                                     boolean hidePortalBlocks) {
		
		Map<BlockVec, BlockType> visibleBlocks = new HashMap<>();
		
		if (playerFrustum != null && displayFrustum) {
			visibleBlocks = playerFrustum.getContainedBlocks(projection);
		}
		
		if (hidePortalBlocks) {
			
			for (Block portalBlock : portal.getPortalBlocks()) {
				visibleBlocks.put(new BlockVec(portalBlock), BlockType.of(Material.AIR));
			}
		}
		
		updateDisplayedBlocks(player, session, visibleBlocks);
	}
	
	/**
	 * Forwards the changes made in a block cache to all the linked projection caches. This also live-updates what players see.
	 */
	public void updateProjections(BlockCache cache, Map<BlockVec, BlockType> updatedBlocks) {
		
		Map<ProjectionCache, Set<PlayerViewSession>> sortedSessions = getSessionsSortedByPortalSides();
		
		for (ProjectionCache projection : portalHandler.getProjectionsLinkedTo(cache)) {
			
			Map<BlockVec, BlockType> projectionUpdates = updateProjection(projection, updatedBlocks);
			
			if (!sortedSessions.containsKey(projection)) {
				continue;
			}
			
			for (PlayerViewSession session : sortedSessions.get(projection)) {
				
				ViewFrustum playerFrustum = session.getLastViewFrustum();
				
				if (playerFrustum == null) {
					continue;
				}
				
				Map<BlockVec, BlockType> newBlocksInFrustum = getBlocksInFrustum(playerFrustum, projectionUpdates);
				Player player = session.getPlayer();
				
				session.getProjectedBlocks().putAll(newBlocksInFrustum);
				packetHandler.displayFakeBlocks(player, newBlocksInFrustum);
			}
		}
	}
	
	private Map<BlockVec, BlockType> updateProjection(ProjectionCache projection,
	                                                  Map<BlockVec, BlockType> updatedBlocks) {
		
		Map<BlockVec, BlockType> projectionUpdates = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : updatedBlocks.entrySet()) {
			
			Transform blockTransform = projection.getLinkTransform();
			BlockVec projectionBlockPos = blockTransform.transformVec(entry.getKey());
			BlockType sourceBlockType = entry.getValue();
			
			if (sourceBlockType == null) {
				projection.removeBlockDataAt(projectionBlockPos);
				continue;
			}
			
			BlockType projectionBlockType = sourceBlockType.rotate(blockTransform.getQuarterTurns());
			projection.setBlockTypeAt(projectionBlockPos, projectionBlockType);
			projectionUpdates.put(projectionBlockPos, projectionBlockType);
		}
		
		return projectionUpdates;
	}
	
	/**
	 * Returns a map of all the blocks in a block cache that are visible with the player's view frustum through the portal frame.
	 */
	private Map<BlockVec, BlockType> getBlocksInFrustum(ViewFrustum playerFrustum,
	                                                    Map<BlockVec, BlockType> projectionUpdates) {
		
		Map<BlockVec, BlockType> blocksInFrustum = new HashMap<>();
		
		for (Map.Entry<BlockVec, BlockType> entry : projectionUpdates.entrySet()) {
			
			BlockVec blockPos = entry.getKey();
			BlockType blockType = entry.getValue();
			
			if (blockType != null && playerFrustum.containsBlock(blockPos.toVector())) {
				blocksInFrustum.put(blockPos, blockType);
			}
		}
		
		return blocksInFrustum;
	}
	
	public Map<ProjectionCache, Set<PlayerViewSession>> getSessionsSortedByPortalSides() {
		
		Map<ProjectionCache, Set<PlayerViewSession>> sortedViewers = new HashMap<>();
		
		for (PlayerViewSession session : getViewSessions()) {
			
			ProjectionCache projection = session.getViewedPortalSide();
			sortedViewers.computeIfAbsent(projection, set -> new HashSet<>());
			sortedViewers.get(projection).add(session);
		}
		
		return sortedViewers;
	}
	
	/**
	 * Adding new blocks to the portal animation for a player.
	 * But first redundant blocks are filtered out and outdated blocks are refreshed for the player.
	 */
	private void updateDisplayedBlocks(Player player, PlayerViewSession session, Map<BlockVec, BlockType> newBlocksToDisplay) {
		
		Map<BlockVec, BlockType> lastDisplayedBlocks = session.getProjectedBlocks();
		Map<BlockVec, BlockType> removedBlocks = new HashMap<>();
		
		Iterator<BlockVec> blockIter = lastDisplayedBlocks.keySet().iterator();
		
		while (blockIter.hasNext()) {
			
			BlockVec blockPos = blockIter.next();
			
			if (!newBlocksToDisplay.containsKey(blockPos)) {
				removedBlocks.put(blockPos, lastDisplayedBlocks.get(blockPos));
				blockIter.remove();
			}
		}
		
		newBlocksToDisplay.keySet().removeIf(lastDisplayedBlocks::containsKey);
		lastDisplayedBlocks.putAll(newBlocksToDisplay);
		
		packetHandler.removeFakeBlocks(player, removedBlocks);
		packetHandler.displayFakeBlocks(player, newBlocksToDisplay);
	}
	
	public Map<BlockVec, BlockType> getProjectedBlocks(Player player) {
		
		Map<BlockVec, BlockType> projectedBlocks = new HashMap<>();
		
		for (PlayerViewSession session : getViewSessions(player)) {
			projectedBlocks.putAll(session.getProjectedBlocks());
		}
		
		return projectedBlocks;
	}
	
	/**
	 * Stops all portal projections that are from this portal or from portals connected to it.
	 */
	public void removePortal(Portal portal) {
		
		Set<Portal> affectedPortals = portalHandler.getPortalsLinkedTo(portal);
		affectedPortals.add(portal);
		
		Set<PlayerViewSession> viewPortalCopy = new HashSet<>(getViewSessions());
		
		for (PlayerViewSession session : viewPortalCopy) {
			
			if (affectedPortals.contains(session.getViewedPortal())) {
				hidePortalProjection(session.getPlayer());
			}
		}
	}
}
