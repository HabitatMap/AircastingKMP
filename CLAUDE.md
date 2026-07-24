# Working with this project

## Docs live in /docs/ (gitignored)

Design docs, specs, and context live in the `/docs/` folder at repo root,
covering **AirBeam** and **legacy codebase** knowledge. It's gitignored, so it
won't show in git status — **read it at the start of any non-trivial task** to
pick up context that isn't in the tracked code.

## Core directive: propose the code in chat, and teach it

The developers on this project are **experienced Android Kotlin developers** learning
Kotlin Multiplatform (KMP) on the go. Your primary objective is to help them build a
**solid, first-hand understanding of the codebase and the KMP framework**.

Do write the actual code — implementations and tests included — but **provide it in the chat
window, not by editing files or committing.** The developer copies and pastes it in
themselves. That deliberate copy step is where they read the code and its explanation
side by side and internalize it. So: **do not use Write/Edit on production or test files, and
do not commit.** Present the code as chat code blocks instead.

Write the **minimum code needed to achieve the current goal**, and nothing more: less code is
less surface to understand, which directly serves the goal of full context. And every piece of
code you provide must come with a clear explanation of **what it does and why it's done this
way**, so they stay fully aware of the codebase and understand every decision.

## Develop test-first (TDD)

Follow **test-driven development**: write the unit tests *before* the implementation.

1. **Tests first.** Given a goal, propose the unit tests that pin down the desired behavior,
   and explain what each one asserts and why. These tests should fail against the
   not-yet-written (or incomplete) implementation — red.
2. **Then the implementation.** Once the tests are agreed on, propose the minimum
   implementation that makes them pass — green.
3. **Refactor if needed**, keeping the tests green.

State the expected red/green transition when you present each step, so the developer sees
what the tests prove and how the implementation satisfies them.

## What this means in practice

- **Propose code in chat, don't apply it.** Never edit or create project files, and never
  commit. Output the code as a code block the developer can copy. (Reading files to
  understand the codebase is fine and encouraged.)
- **Tests before implementation.** For any new behavior, present the failing tests first,
  then the code that makes them pass.
- **Always reference the target file path.** Every code block must state the exact file path
  the code belongs in, so the developer knows where to paste it.
- **Keep it minimal.** Only as much code as the current goal requires. No speculative
  abstractions, no boilerplate beyond what's needed, no gold-plating.
- **Always explain what you wrote and why.** Never drop code without walking through it:
  what each part does, the tradeoffs, and why this approach over alternatives.
- **Explain, advise, and guide.** Help with planning, architecture, debugging reasoning,
  tradeoffs, and understanding what the code does and why.
- **Assume Android/Kotlin fluency.** Skip beginner Kotlin/Android explanations. Focus on
  what's *different* or *new* in KMP: expect/actual, shared vs platform source sets,
  Ktor/SQLDelight/Koin multiplatform, iOS interop, Compose Multiplatform, etc.
- **Use your Android and KMP skills** to advise and educate.
- **Fetch online resources when useful** — link official KMP docs, KEEPs, library guides
  rather than guessing.

## Logging

Use **Kermit** (`co.touchlab:kermit`, already a dependency) for all logging — never
`println`. Kermit is multiplatform (routes to Logcat on Android, `os_log`/console on iOS)
and supports tags and log levels. Prefer a tagged logger, e.g.
`Logger.withTag("Home").d { "..." }`, over the global `Logger`.

## Default posture

Lead with understanding: "here's how KMP handles X, here's the tradeoff, here's where it
lives in this codebase." Then present the code in chat — with its file path — and explain
what it does and why.
