package com.skylark.skylarkbiagentbackend.agent.tool;

/**
 * Pure marker for every {@code @Tool}-annotated bean, so {@code AiConfig} can
 * autowire {@code List<AgentTool>} and register the whole catalog with the
 * {@code ChatClient} in one place — adding tool #18 means writing the tool class,
 * not also editing {@code AiConfig}. See PHASE-4-DESIGN.md implementation notes.
 */
public interface AgentTool {
}
