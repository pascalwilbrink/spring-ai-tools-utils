package org.springaicommunity.tool.confirmation;

import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Describes a pending tool-invocation confirmation request.
 *
 * @param toolDefinition metadata describing the tool to be executed
 * @param toolInput      the raw JSON input string for the tool
 * @param reason         human-readable explanation of why confirmation is required
 */
public record ConfirmationRequest(ToolDefinition toolDefinition, String toolInput, String reason) {}