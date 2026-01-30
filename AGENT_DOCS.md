# Antigravity: Agent Internal Workings

This document explains the internal cognitive architecture and operational flow of the **Antigravity** agent using Mermaid diagrams.

## 1. The Core Cognitive Loop

Antigravity operates on a continuous feedback loop inspired by the **OODA** (Observe, Orient, Decide, Act) loop.

```mermaid
graph TD
    subgraph Observation ["1. Observe"]
        Inputs["User Input + Metadata"]
        State["File System + Context"]
    end

    subgraph Orientation ["2. Orient"]
        Thought["Internal Monologue (Reasoning)"]
        Plan["Task List Update"]
    end

    subgraph Decision ["3. Decide"]
        ToolSelection["Select Tool & Arguments"]
    end

    subgraph Action ["4. Act"]
        ToolCall["Execute Tool Call"]
        Result["Receive Tool Output"]
    end

    Inputs --> Thought
    State --> Thought
    Thought --> Plan
    Plan --> ToolSelection
    ToolSelection --> ToolCall
    ToolCall --> Result
    Result --> Observation
```

## 2. Collaborative Interaction Model

The agent interacts with the user through a structured communication channel, ensuring transparency and alignment.

```mermaid
sequenceDiagram
    participant User
    participant Agent
    participant Env as Environment (Files/Tools)

    User->>Agent: Send Request
    Agent->>Env: Exploration (ls, view_file)
    Env-->>Agent: Data
    Agent->>Agent: Cognitive Processing
    Agent->>Env: Implementation (write_file, replace_content)
    Agent->>Agent: Verification
    Agent->>User: Notify with Artifacts (Walkthrough/Plan)
    User->>Agent: Feedback / Approval
```

## 3. State Management through Artifacts

To maintain consistency over long sessions, the agent uses persistent artifacts stored in the `.gemini/antigravity/brain/` directory.

```mermaid
graph LR
    UserRequest((User Request)) --> Plan[implementation_plan.md]
    Plan --> Task[task.md]
    Task --> Execution{Execution}
    Execution --> Fixes[Code Base]
    Execution --> Status[Task Updates]
    Execution --> Walkthrough[walkthrough.md]
    Walkthrough --> Notification((Notify User))
```

## 4. Operational Modes

| Mode | Purpose | Primary Activity |
| :--- | :--- | :--- |
| **PLANNING** | Research and design | Creating `implementation_plan.md` |
| **EXECUTION** | Implementation | Modifying code, creating features |
| **VERIFICATION** | Quality assurance | Testing, creating `walkthrough.md` |

---

> [!NOTE]
> This architecture ensures that every action is grounded in context, planned for success, and verified for quality.
