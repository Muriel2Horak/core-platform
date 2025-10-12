package cz.muriel.core.workflow.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 🔄 W6: WebSocket handler for real-time workflow collaboration
 * 
 * <p>
 * Enables multiple users to edit workflows simultaneously with real-time
 * updates.
 * 
 * <p>
 * <b>Protocol:</b>
 * 
 * <pre>
 * Client → Server:
 * - {"type":"JOIN", "entity":"Order", "userId":"user1", "username":"John Doe"}
 * - {"type":"LEAVE", "entity":"Order"}
 * - {"type":"NODE_UPDATE", "entity":"Order", "node":{...}}
 * - {"type":"EDGE_UPDATE", "entity":"Order", "edge":{...}}
 * - {"type":"NODE_DELETE", "entity":"Order", "nodeId":"n1"}
 * - {"type":"EDGE_DELETE", "entity":"Order", "edgeId":"e1"}
 * - {"type":"CURSOR", "entity":"Order", "x":100, "y":200}
 * - {"type":"HB"}
 * 
 * Server → Client:
 * - {"type":"USER_JOINED", "entity":"Order", "userId":"user1", "username":"John Doe", "users":[...]}
 * - {"type":"USER_LEFT", "entity":"Order", "userId":"user1", "users":[...]}
 * - {"type":"NODE_UPDATED", "entity":"Order", "node":{...}, "userId":"user2"}
 * - {"type":"EDGE_UPDATED", "entity":"Order", "edge":{...}, "userId":"user2"}
 * - {"type":"NODE_DELETED", "entity":"Order", "nodeId":"n1", "userId":"user2"}
 * - {"type":"EDGE_DELETED", "entity":"Order", "edgeId":"e1", "userId":"user2"}
 * - {"type":"CURSOR_MOVED", "entity":"Order", "userId":"user2", "username":"Jane", "x":100, "y":200}
 * - {"type":"ERROR", "message":"..."}
 * </pre>
 * 
 * <p>
 * <b>Session Management:</b>
 * <ul>
 * <li>Each WebSocketSession is mapped to a WorkflowSession (entity, userId,
 * username)</li>
 * <li>On JOIN: store session, broadcast USER_JOINED to all users in the same
 * workflow</li>
 * <li>On updates: broadcast to all users except sender</li>
 * <li>On disconnect: cleanup and broadcast USER_LEFT</li>
 * </ul>
 * 
 * @since W6
 */
@Slf4j @Component @RequiredArgsConstructor @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class WorkflowCollaborationHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper;

  // Session ID → WorkflowSession
  private final ConcurrentMap<String, WorkflowSession> sessions = new ConcurrentHashMap<>();

  // Session ID → WebSocketSession (to keep reference)
  private final ConcurrentMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

  // Entity name → Set of session IDs
  private final ConcurrentMap<String, Set<String>> entitySessions = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    activeSessions.put(session.getId(), session);
    log.info("Workflow collaboration connection established: sessionId={}", session.getId());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String payload = message.getPayload();
    log.debug("Workflow collaboration message: sessionId={}, payload={}", session.getId(), payload);

    try {
      JsonNode node = objectMapper.readTree(payload);
      String type = node.get("type").asText();

      switch (type) {
      case "JOIN" -> handleJoin(session, node);
      case "LEAVE" -> handleLeave(session, node);
      case "NODE_UPDATE" -> handleNodeUpdate(session, node);
      case "EDGE_UPDATE" -> handleEdgeUpdate(session, node);
      case "NODE_DELETE" -> handleNodeDelete(session, node);
      case "EDGE_DELETE" -> handleEdgeDelete(session, node);
      case "CURSOR" -> handleCursor(session, node);
      case "HB" -> handleHeartbeat(session);
      default -> {
        log.warn("Unknown message type: {}", type);
        sendError(session, "Unknown message type: " + type);
      }
      }
    } catch (Exception e) {
      log.error("Error handling workflow collaboration message", e);
      sendError(session, "Invalid message format");
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    log.info("Workflow collaboration connection closed: sessionId={}, status={}", session.getId(),
        status);
    activeSessions.remove(session.getId());
    cleanup(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    log.error("Workflow collaboration transport error: sessionId={}", session.getId(), exception);
    activeSessions.remove(session.getId());
    cleanup(session);
  }

  // ========== Message Handlers ==========

  private void handleJoin(WebSocketSession session, JsonNode node) throws IOException {
    String entity = node.get("entity").asText();
    String userId = node.get("userId").asText();
    String username = node.get("username").asText();

    // Store session
    WorkflowSession wfSession = new WorkflowSession(entity, userId, username);
    sessions.put(session.getId(), wfSession);

    // Add to entity sessions
    entitySessions.computeIfAbsent(entity, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

    // Get current users
    List<Map<String, String>> users = getCurrentUsers(entity);

    // Broadcast USER_JOINED
    Map<String, Object> joinMsg = Map.of("type", "USER_JOINED", "entity", entity, "userId", userId,
        "username", username, "users", users);
    broadcastToEntity(entity, joinMsg, session.getId());

    // Send current users to new joiner
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(joinMsg)));

    log.info("User joined workflow: entity={}, userId={}, username={}", entity, userId, username);
  }

  private void handleLeave(WebSocketSession session, JsonNode node) throws IOException {
    String entity = node.get("entity").asText();
    WorkflowSession wfSession = sessions.get(session.getId());

    if (wfSession == null) {
      log.warn("Leave request for unknown session: {}", session.getId());
      return;
    }

    // Remove from entity sessions
    Set<String> sessions = entitySessions.get(entity);
    if (sessions != null) {
      sessions.remove(session.getId());
      if (sessions.isEmpty()) {
        entitySessions.remove(entity);
      }
    }

    // Broadcast USER_LEFT
    List<Map<String, String>> users = getCurrentUsers(entity);
    Map<String, Object> leaveMsg = Map.of("type", "USER_LEFT", "entity", entity, "userId",
        wfSession.userId, "users", users);
    broadcastToEntity(entity, leaveMsg, session.getId());

    this.sessions.remove(session.getId());

    log.info("User left workflow: entity={}, userId={}", entity, wfSession.userId);
  }

  private void handleNodeUpdate(WebSocketSession session, JsonNode node) throws IOException {
    WorkflowSession wfSession = sessions.get(session.getId());
    if (wfSession == null)
      return;

    String entity = node.get("entity").asText();
    JsonNode nodeData = node.get("node");

    Map<String, Object> updateMsg = Map.of("type", "NODE_UPDATED", "entity", entity, "node",
        objectMapper.convertValue(nodeData, Map.class), "userId", wfSession.userId);
    broadcastToEntity(entity, updateMsg, session.getId());

    log.debug("Node updated: entity={}, userId={}", entity, wfSession.userId);
  }

  private void handleEdgeUpdate(WebSocketSession session, JsonNode node) throws IOException {
    WorkflowSession wfSession = sessions.get(session.getId());
    if (wfSession == null)
      return;

    String entity = node.get("entity").asText();
    JsonNode edgeData = node.get("edge");

    Map<String, Object> updateMsg = Map.of("type", "EDGE_UPDATED", "entity", entity, "edge",
        objectMapper.convertValue(edgeData, Map.class), "userId", wfSession.userId);
    broadcastToEntity(entity, updateMsg, session.getId());

    log.debug("Edge updated: entity={}, userId={}", entity, wfSession.userId);
  }

  private void handleNodeDelete(WebSocketSession session, JsonNode node) throws IOException {
    WorkflowSession wfSession = sessions.get(session.getId());
    if (wfSession == null)
      return;

    String entity = node.get("entity").asText();
    String nodeId = node.get("nodeId").asText();

    Map<String, Object> deleteMsg = Map.of("type", "NODE_DELETED", "entity", entity, "nodeId",
        nodeId, "userId", wfSession.userId);
    broadcastToEntity(entity, deleteMsg, session.getId());

    log.debug("Node deleted: entity={}, nodeId={}, userId={}", entity, nodeId, wfSession.userId);
  }

  private void handleEdgeDelete(WebSocketSession session, JsonNode node) throws IOException {
    WorkflowSession wfSession = sessions.get(session.getId());
    if (wfSession == null)
      return;

    String entity = node.get("entity").asText();
    String edgeId = node.get("edgeId").asText();

    Map<String, Object> deleteMsg = Map.of("type", "EDGE_DELETED", "entity", entity, "edgeId",
        edgeId, "userId", wfSession.userId);
    broadcastToEntity(entity, deleteMsg, session.getId());

    log.debug("Edge deleted: entity={}, edgeId={}, userId={}", entity, edgeId, wfSession.userId);
  }

  private void handleCursor(WebSocketSession session, JsonNode node) throws IOException {
    WorkflowSession wfSession = sessions.get(session.getId());
    if (wfSession == null)
      return;

    String entity = node.get("entity").asText();
    double x = node.get("x").asDouble();
    double y = node.get("y").asDouble();

    Map<String, Object> cursorMsg = Map.of("type", "CURSOR_MOVED", "entity", entity, "userId",
        wfSession.userId, "username", wfSession.username, "x", x, "y", y);
    broadcastToEntity(entity, cursorMsg, session.getId());
  }

  private void handleHeartbeat(WebSocketSession session) throws IOException {
    Map<String, String> hbAck = Map.of("type", "HB_ACK");
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(hbAck)));
  }

  // ========== Helper Methods ==========

  private void cleanup(WebSocketSession session) throws IOException {
    WorkflowSession wfSession = sessions.remove(session.getId());
    if (wfSession == null)
      return;

    // Remove from entity sessions
    Set<String> entitySessionSet = entitySessions.get(wfSession.entity);
    if (entitySessionSet != null) {
      entitySessionSet.remove(session.getId());
      if (entitySessionSet.isEmpty()) {
        entitySessions.remove(wfSession.entity);
      }
    }

    // Broadcast USER_LEFT
    List<Map<String, String>> users = getCurrentUsers(wfSession.entity);
    Map<String, Object> leaveMsg = Map.of("type", "USER_LEFT", "entity", wfSession.entity, "userId",
        wfSession.userId, "users", users);
    broadcastToEntity(wfSession.entity, leaveMsg, session.getId());

    log.info("Cleaned up workflow session: entity={}, userId={}", wfSession.entity,
        wfSession.userId);
  }

  private List<Map<String, String>> getCurrentUsers(String entity) {
    Set<String> sessionIds = entitySessions.get(entity);
    if (sessionIds == null)
      return List.of();

    return sessionIds.stream().map(sessions::get).filter(Objects::nonNull)
        .map(wfs -> Map.of("userId", wfs.userId, "username", wfs.username)).toList();
  }

  private void broadcastToEntity(String entity, Map<String, Object> message,
      String excludeSessionId) throws IOException {
    Set<String> sessionIds = entitySessions.get(entity);
    if (sessionIds == null)
      return;

    String json = objectMapper.writeValueAsString(message);
    TextMessage textMessage = new TextMessage(json);

    for (String sessionId : sessionIds) {
      if (sessionId.equals(excludeSessionId))
        continue;

      WebSocketSession session = activeSessions.get(sessionId);
      if (session != null && session.isOpen()) {
        try {
          session.sendMessage(textMessage);
        } catch (Exception e) {
          log.error("Failed to send message to session {}", sessionId, e);
        }
      }
    }
  }

  private void sendError(WebSocketSession session, String error) {
    try {
      Map<String, String> errorMsg = Map.of("type", "ERROR", "message", error);
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
    } catch (Exception e) {
      log.error("Failed to send error message", e);
    }
  }

  // ========== Inner Classes ==========

  private record WorkflowSession(String entity, String userId, String username) {
  }
}
