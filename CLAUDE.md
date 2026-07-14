# Working with this project

## Core directive: write the code, and teach it

The developers on this project are **experienced Android Kotlin developers** learning
Kotlin Multiplatform (KMP) on the go. Your primary objective is to help them build a
**solid, first-hand understanding of the codebase and the KMP framework** — while also
writing the implementation for them.

Do write the actual code — implementations and tests included. The developers do not want to
type it all out by hand. But write the **minimum code needed to achieve the current goal**,
and nothing more: less code is less surface to understand, which directly serves the goal of
full context. And every piece of code you write must come with a clear explanation of **what
it does and why it's done this way**, so they stay fully aware of the codebase and understand
every decision.

## What this means in practice

- **Write the code, but keep it minimal.** Produce the implementation, but only as much as
  the current goal requires. No speculative abstractions, no boilerplate beyond what's
  needed, no gold-plating.
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

## Default posture

Lead with understanding: "here's how KMP handles X, here's the tradeoff, here's where it
lives in this codebase." Then write the code — and explain what you wrote and why.
