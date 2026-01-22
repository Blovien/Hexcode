---
active: true
iteration: 3
max_iterations: 0
completion_promise: null
started_at: "2026-01-22T23:40:33Z"
---

You are implementing the Hexcode Glyph System Rework as defined in TODO.md. This is a Hytale mod using Java.

CRITICAL RULES:
1. Read TODO.md at the start of EVERY iteration to check current phase status
2. Work through phases 1-10 IN ORDER - do not skip ahead
3. Mark tasks complete in TODO.md as you finish them using checkboxes [x]
4. After completing ALL tasks in a phase, update the Summary Checklist status
5. If you encounter Hytale API questions, check docs/ folder or use web search for Hytale modding docs
6. Test compilation after each major change with './gradlew build' or equivalent
7. Create skills in .claude/skills/ for any reusable Hytale modding patterns learned

KEY ARCHITECTURE (from TODO.md):
- Hex = nested glyphs that influence each other (context flows through)
- Chain = sequential glyphs separated by ':' (each gets COPY of original context)
- SpellContext is the central execution state object
- All glyph properties come from JSON asset files, NO hard-coded values
- Power decays: cast decay (1/castNumber) and glyph repetition decay (1/executionCount)

WORKFLOW PER ITERATION:
1. Read TODO.md to find current incomplete phase
2. Identify next unchecked task in that phase
3. Implement the task following completion criteria exactly
4. Mark task [x] complete in TODO.md
5. If phase complete, update Summary Checklist
6. Continue until all phases show 'Complete'

When ALL 10 phases in the Summary Checklist show 'Complete', output: HEXCODE_REWORK_COMPLETE --completion-phrase=HEXCODE_REWORK_COMPLETE --max-iterations=50
