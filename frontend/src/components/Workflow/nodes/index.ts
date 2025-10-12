/**
 * W1: Custom Node Components Export
 */

export { default as TaskNode } from './TaskNode';
export type { TaskNodeData } from './TaskNode';

export { default as DecisionNode } from './DecisionNode';
export type { DecisionNodeData } from './DecisionNode';

export { default as StartNode } from './StartNode';
export type { StartNodeData } from './StartNode';

export { default as EndNode } from './EndNode';
export type { EndNodeData } from './EndNode';

// Node Types Registry pro React Flow
import TaskNode from './TaskNode';
import DecisionNode from './DecisionNode';
import StartNode from './StartNode';
import EndNode from './EndNode';

export const nodeTypes = {
  task: TaskNode,
  decision: DecisionNode,
  start: StartNode,
  end: EndNode,
};
