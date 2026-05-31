# OMNI Framework — OpenCode Multi-phase Navigation Intelligence

Auto-routing framework for AI-assisted software engineering. Detects project type, tracks state, routes to the right skill at every phase — from vision to shipping.

---

## Configuration

```yaml
# OMNI_AGENTS.md — Static routing rules (READ at every agent start)
# OMNI_STATE.yaml — Dynamic project state (READ/WRITE after every action)
version: 1.0.0
detection:
  android: ["build.gradle.kts", "AndroidManifest.xml", "app/src/"]
  web_backend: ["app.py", "requirements.txt", "main.py"]
  web_frontend: ["package.json", "vite.config.ts", "next.config.js"]
  ios: ["*.xcworkspace", "Podfile", "*.swift"]
  mixed: multiple detectors match
```

---

## Fundamental Rules

### Rule 1 — Project Type Detection
On every agent start, scan the project root for detector files. Set `project.type` in `OMNI_STATE.yaml`:
- `android` → load `android-*` skills + routing
- `web` → standard phase routing
- `mixed` → load all matching skill sets
- `unknown` → ask user via bmad-help

### Rule 2 — Primary → Secondary Verification
Every critical action has two skills:
- **Primary** — fast, first execution
- **Secondary** — verification pass ensuring quality
If secondary fails, loop back to primary.

### Rule 3 — State-Driven Progression
1. Read `OMNI_STATE.yaml` at agent start
2. Resume at `current_phase`
3. Execute next pending skill in phase
4. After each action → write updated state
5. When all skills in phase done → advance `current_phase`

### Rule 4 — Taste Decisions Log
When a close approach exists or scope is borderline, log the decision with rationale in `OMNI_STATE.yaml.taste_decisions`. Never silently choose.

---

## Phase 0 — INIT

**Trigger:** New project directory / first agent invocation

### Steps
1. `graphify update .` — Index the codebase
2. Detect project type (Rule 1)
3. **If android**: `android-tooling` — verify JDK, SDK, ADB, Gradle wrapper
4. `bmad-document-project` — Generate `project-context.md`
5. `gstack-setup-deploy` — Detect deploy platform, write config
6. `bmad-generate-project-context` — AI rules for the project
7. `bmad-index-docs` — Index of all documentation files

### Artifacts
- `project-context.md`
- `OMNI_STATE.yaml` (initialized)
- `docs/index.md`

### Transition
Auto → Phase 1 unless blocked (missing tools → log blocker first)

---

## Phase 1 — VISION

**Trigger:** "j'ai une idée" / "nouveau projet" / user asks for brainstorming

### Steps
1. **Primary:** `/office-hours` — YC six forcing questions
2. **Secondary:** `bmad-brainstorming` — Creative ideation expansion
3. `bmad-market-research` — Analyse marché (si produit externe)
4. `bmad-domain-research` — Recherche domaine (si niche spécifique)
5. `bmad-technical-research` — Recherche technique (si choix tech critique)
6. **Choice:** `bmad-product-brief` (brief classique) OR `bmad-prfaq` (PRFAQ challenge)

### Artifacts
- `product-brief.md` or `prfaq.md`

### Transition
→ Phase 2 when a brief/PRFAQ exists with user approval

---

## Phase 2 — STRATEGY

**Trigger:** `product-brief.md` or `prfaq.md` exists and approved

### Sub-phases

#### 2a — CEO REVIEW
**Primary:** `/plan-ceo-review` — Challenge premises, find the 10-star product
**Mode:** Ask user: SCOPE EXPANSION / SELECTIVE EXPANSION / HOLD SCOPE / SCOPE REDUCTION
**Artifact:** Decision logged in `OMNI_STATE.yaml.taste_decisions`

#### 2b — PRD
**Primary:** `bmad-create-prd` — Generate full PRD from brief
**Secondary:** `bmad-validate-prd` — Validate against standards
**Loop:** `bmad-edit-prd` if validation fails
**Artifact:** `prd.md`

#### 2c — ENG REVIEW
**Primary:** `/plan-eng-review` — Architecture, data flow, edge cases, test coverage
**Artifact:** Review notes in `OMNI_STATE.yaml.taste_decisions`

#### 2d — DESIGN REVIEW (if UI project)
**Primary:** `/plan-design-review` — Design plan evaluation
**Artifact:** Design score + improvement plan

#### 2e — DX REVIEW (if dev-facing product)
**Primary:** `/plan-devex-review` — Developer experience audit
**Triggers:** API, SDK, CLI, library projects
**Artifact:** DX scorecard

#### 2f — ARCHITECTURE
**Primary:** `bmad-create-architecture` — Formal architecture / ADRs
**Android+:** `android-architecture` — Multi-module Clean Architecture plans
  - Load references from `OMNI_STATE.yaml.references.android.*`
  - Generate module structure, convention plugins, DI setup
**Artifact:** `architecture.md`

#### 2g — EPICS & STORIES
**Primary:** `bmad-create-epics-and-stories` — Breakdown into epics and stories
**Artifact:** `epics.md`, stories in `stories/` directory

### Transition
→ Phase 3 when `epics.md` exists with ≥1 `ready-for-dev` story

---

## Phase 3 — DESIGN

**Trigger:** `epics.md` exists | Product has UI components

### Steps
1. **Primary:** `bmad-create-ux-design` — UX specifications, user flows, wireframes
2. **Primary:** `/design-consultation` — Full design system proposal → `DESIGN.md`
3. **Optional:** `/design-shotgun` — Visual exploration, multiple variants (for critical screens)
4. **Primary:** `/plan-design-review` — Review design against product goals
5. **Secondary:** `bmad-agent-ux-designer` (Sally) — UX deep-dive consultation
6. **Android+:** `android-compose-ui` — Material 3 theming, adaptive layouts
7. **Build sprint plan:** `bmad-sprint-planning` → `sprint-status.yaml`
8. **Create first stories:** `bmad-create-story` for each epic
9. **Optional HTML:** `/design-html` — If web project, generate production HTML/CSS

### Artifacts
- `ux-design-spec.md`
- `DESIGN.md`
- `sprint-status.yaml`
- Stories in `stories/` (status: `ready-for-dev`)

### Transition
→ Phase 4 when ≥1 story is `ready-for-dev`

---

## Phase 4 — BUILD

**Trigger:** Stories in `ready-for-dev` status

### Loop (for each story)

#### 4a — Implement
**Primary:** `bmad-dev-story` — Full RED→GREEN→REFACTOR implementation
**Secondary (quick):** `bmad-quick-dev` — Fast path for small changes / hotfixes

**Android+ skills (activated automatically when `project.type = android`):**
  - `android-compose-ui` — Screen implementation, Navigation 3, state management
  - `android-build-ci` — Build config, convention plugins, dependency management
  - `android-testing` — Unit tests, Compose UI tests, Flow tests

#### 4b — Checkpoint
`/context-save` — Save progress, decisions, blockers

#### 4c — Update Graph
`graphify update .` — Refresh knowledge graph after code changes

#### 4d — Safety (if needed)
`gstack-freeze` or `gstack-guard` — Restrict edits to module scope

#### 4e — Architecture Consultation (if needed)
`bmad-agent-architect` (Winston) — Architecture decision support
`bmad-agent-dev` (Amelia) — Senior dev consultation

**Standalone patterns libraries (auto-loaded as needed):**
- `architecture-patterns` — Clean Architecture, Hexagonal, DDD
- `api-design-patterns` — REST/GraphQL design
- `error-handling-patterns` — Exceptions, Result types, graceful degradation
- `workflow-orchestration-patterns` — Temporal, sagas, distributed processes

### Transition per story
Story done → Phase 5 (review) OR next story → Phase 4

---

## Phase 5 — QUALITY

**Trigger:** Story status changes to "review" / code has been written

### Sub-phases

#### 5a — CODE REVIEW
**Primary:** `bmad-code-review` — Three parallel layers:
  1. Blind Hunter — Spot obvious bugs + logic errors
  2. Edge Case Hunter — Walk every branching path + boundary
  3. Acceptance Auditor — Verify against story acceptance criteria
**Secondary (pre-landing):** `/review` — SQL safety, LLM trust boundaries, conditional side effects

#### 5b — STATIC ANALYSIS
**Primary:** `/health` — Composite quality score (0-10)
**Android+:** `android-build-ci` — `./gradlew detekt lint` run
Log score + trend in `OMNI_STATE.yaml`

#### 5c — QA TESTING
**Primary:** `/qa` — Live browser QA testing
**Levels:** Ask user: Quick (critical only) / Standard (+ medium) / Exhaustive (+ cosmetic)
**Secondary (report only):** `/qa-only` — Bug report without fixes

#### 5d — DESIGN QA (if UI project)
**Primary:** `/design-review` — Visual audit, spacing, hierarchy, AI slop

#### 5e — SECURITY AUDIT
**Trigger:** Sensitive data, auth, payments, or user request
**Primary:** `/cso` — OWASP + STRIDE threat modeling
**Modes:** Daily (8/10 confidence) or Comprehensive (2/10 bar)

#### 5f — E2E TESTS (if automation needed)
**Primary:** `bmad-qa-generate-e2e-tests` — Generate E2E test suite

#### 5g — INVESTIGATE (if bugs found)
**Primary:** `/investigate` — Root cause → hypothesis → fix (Iron Law: no fix without root cause)

#### 5h — ACCESSIBILITY (if UI project)
**Primary:** `wcag-audit-patterns` — WCAG 2.2 audit
**Secondary:** `accessibility-compliance` — Fix violations

### Decision Gate
- Issues found → back to Phase 4 (with investigation report)
- All clear → advance to Phase 6

---

## Phase 6 — SHIP

**Trigger:** All stories in epic done / release candidate ready

### Steps
1. **Pre-landing review:** `/review` — Final safety check (SQL, trust boundaries, side effects)
2. **Primary:** `/ship` — Bump VERSION, update CHANGELOG, commit, push, create PR
3. **Primary:** `/land-and-deploy` — Merge, wait CI, deploy, production verify
4. **Primary:** `/canary` — Post-deploy monitoring (screenshots, console errors, performance)
5. **Secondary:** `/benchmark` — Core Web Vitals, bundle size, load time (if web)
6. **Docs:** `/document-release` — Sync README, ARCHITECTURE, CHANGELOG to match shipped code
7. **Landing check:** `/landing-report` — Verify no conflicting PRs in queue

### Artifacts
- `VERSION` bump
- GitHub Release
- `CHANGELOG.md` update
- Deploy verification report

### Transition
→ Phase 7 (retro) or Phase 4 (next sprint cycle)

---

## Phase 7 — RETRO

**Trigger:** Epic "done" / end of sprint / user asks for retro

### Steps
1. **Primary:** `bmad-retrospective` — Lessons learned, action items, next epic preview
2. **Secondary:** `gstack-retro` — Weekly engineering retro (commits, patterns, quality trends)
3. **Persist:** `gstack-learn` — Save cross-session learnings
4. **Discuss:** `bmad-party-mode` — Multi-agent discussion (optional, for complex retros)
5. **Correct:** `bmad-correct-course` — If retrospective reveals course corrections needed

### Artifacts
- `retro-{date}.md`
- Updated `OMNI_STATE.yaml.learnings`

### Next Cycle
→ Phase 4 (new sprint/next epic) or Phase 1 (new product/feature)

---

## Cross-Cutting — Permanent Auto-Routing

These rules are always active, regardless of current phase:

| Trigger | Action | Skills |
|---------|--------|--------|
| "help" / "que faire" / "what next" | Analyse state + recommend | `bmad-help` |
| "sauvegarde" / "/context-save" | Snapshot state + decisions | `/context-save` |
| "reprendre" / "ou j'en étais" | Restore last session | `/context-restore` |
| "erreur" / "bug" / "ne marche pas" | Systematic investigation | `/investigate` |
| "checkpoint" / "revue humaine" | Human review gate | `bmad-checkpoint-preview` |
| "discussion" / "avis multiple" | Multi-agent debate | `bmad-party-mode` |
| "sécurité" / "audit security" | Security scan | `/cso` |
| "solo" / "attention" / "safety" | Restrict edit scope | `/freeze` or `/guard` |
| "modèle" / "comparer modèles" | Cross-model benchmark | `/benchmark-models` |
| "tune" / "trop de questions" | Adjust question frequency | `/plan-tune` |
| "deep critique" / "challenge" | Adversarial deep-dive | `bmad-advanced-elicitation` |
| "edge cases" | Exhaustive boundary analysis | `bmad-review-edge-case-hunter` |

---

## Android Auto-Routing

Activated when `project.type = android` OR detector files match.

### Detection
- `build.gradle.kts` with `com.android.application` plugin
- `AndroidManifest.xml` in any subdirectory
- `app/src/` directory structure
- `gradlew` wrapper present

### Skills Activated
| Skill | Phase | When |
|-------|-------|------|
| `android-tooling` | INIT | Project setup, toolchain verification |
| `android-architecture` | STRATEGY (2f) | Module structure, convention plugins, DI |
| `android-compose-ui` | DESIGN (3) + BUILD (4) | Screen implementation, theming, navigation |
| `android-build-ci` | BUILD (4) + SHIP (6) | Gradle config, CI/CD, code quality |
| `android-testing` | BUILD (4) + QUALITY (5) | Unit tests, Compose tests, Flow tests |

### Project-Specific Config for App2 (Android App)
When `App2/` is detected in the workspace root:
- Use `android-architecture` with multi-module Clean Architecture template
- Initialize Gradle wrapper (`gradle wrapper`)
- Set up version catalog (`gradle/libs.versions.toml`)
- Create convention plugins in `build-logic/convention/`
- Reference repos: `references/android/nowinandroid`, `android-showcase`, `MVVMTemplate`

### Android-Specific Commands (Cross-Phase)
```bash
./gradlew tasks                    # All available tasks
./gradlew assembleDebug            # Quick build
./gradlew test                     # Unit tests
./gradlew connectedDebugAndroidTest # Instrumentation tests
./gradlew lint detekt              # Static analysis
adb devices                        # Check connected devices
adb logcat *:W                     # Watch warnings+
```

---

## Skill Registry

### Format
```
name | phase(s) | framework | trigger | description
```

### Phase 0 — INIT (6 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `graphify update .` | 0 | graphify | First run | Index codebase into knowledge graph |
| `bmad-document-project` | 0 | BMad | New project | Generate project-context.md |
| `gstack-setup-deploy` | 0 | gstack | New project | Detect + configure deploy platform |
| `bmad-generate-project-context` | 0 | BMad | After doc | AI rules for project conventions |
| `bmad-index-docs` | 0 | BMad | Docs created | Index of all documentation |
| `android-tooling` | 0 | standalone | Android project | JDK/SDK/Gradle/ADB setup + verification |

### Phase 1 — VISION (5 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `/office-hours` | 1 | gstack | "idée", "brainstorm" | YC six forcing questions |
| `bmad-brainstorming` | 1 | BMad | After office-hours | Creative ideation expansion |
| `bmad-market-research` | 1 | BMad | External product | Analyse marché concurrentiel |
| `bmad-domain-research` | 1 | BMad | Niche domain | Recherche domaine spécialisé |
| `bmad-technical-research` | 1 | BMad | Tech choice | Recherche solutions techniques |
| `bmad-product-brief` | 1 | BMad | Brief needed | Product brief from discovery |
| `bmad-prfaq` | 1 | BMad | Challenge needed | Working Backwards PRFAQ |

### Phase 2 — STRATEGY (8 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `/plan-ceo-review` | 2 | gstack | brief approved | CEO review: scope/ambition challenge |
| `bmad-create-prd` | 2 | BMad | CEO review done | Full PRD generation |
| `bmad-validate-prd` | 2 | BMad | PRD done | Validate against standards |
| `bmad-edit-prd` | 2 | BMad | Validation failed | PRD corrections |
| `/plan-eng-review` | 2 | gstack | PRD approved | Architecture, data flow, edge cases |
| `/plan-design-review` | 2 | gstack | UI product | Design plan evaluation |
| `/plan-devex-review` | 2 | gstack | Dev-facing product | Developer experience audit |
| `bmad-create-architecture` | 2 | BMad | Eng review done | Formal architecture + ADRs |
| `bmad-create-epics-and-stories` | 2 | BMad | Architecture done | Breakdown into epics + stories |
| `android-architecture` | 2 | standalone | Android project | Multi-module Clean Architecture |

### Phase 3 — DESIGN (7 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `bmad-create-ux-design` | 3 | BMad | epics exist | UX specs, flows, wireframes |
| `/design-consultation` | 3 | gstack | Need design system | Full design system → DESIGN.md |
| `/design-shotgun` | 3 | gstack | Critical screens | Visual exploration / variants |
| `/plan-design-review` | 3 | gstack | Design done | Review design vs product goals |
| `bmad-agent-ux-designer` (Sally) | 3 | BMad | UX consultation | UX deep-dive |
| `/design-html` | 3 | gstack | Web project | Production HTML/CSS generation |
| `bmad-sprint-planning` | 3 | BMad | Design approved | Sprint plan → sprint-status.yaml |
| `bmad-create-story` | 3 | BMad | Sprint planned | Create individual stories |
| `android-compose-ui` | 3 | standalone | Android project | Compose, Material 3, Navigation |

### Phase 4 — BUILD (10 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `bmad-dev-story` | 4 | BMad | Story ready-for-dev | Full RED→GREEN→REFACTOR |
| `bmad-quick-dev` | 4 | BMad | Small change | Fast implementation path |
| `/context-save` | 4 | gstack | After story | Checkpoint state + decisions |
| `graphify update .` | 4 | graphify | After code change | Refresh knowledge graph |
| `bmad-agent-architect` (Winston) | 4 | BMad | Archi question | Architecture consultation |
| `bmad-agent-dev` (Amelia) | 4 | BMad | Dev question | Senior dev consultation |
| `bmad-correct-course` | 4 | BMad | Scope change | Sprint correction management |
| `android-compose-ui` | 4 | standalone | Android code | Compose screen implementation |
| `android-build-ci` | 4 | standalone | Build config | Gradle, CI/CD, code quality |
| `android-testing` | 4 | standalone | Tests needed | Unit + Compose + Flow tests |

### Phase 5 — QUALITY (11 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `bmad-code-review` | 5 | BMad | Story in review | 3-layer parallel review |
| `/review` | 5 | gstack | Before merge | Pre-landing safety check |
| `/health` | 5 | gstack | Code written | Composite quality score 0-10 |
| `/qa` | 5 | gstack | Features done | Live browser QA testing |
| `/qa-only` | 5 | gstack | Report only | Bug report without fixes |
| `/design-review` | 5 | gstack | UI project | Visual audit |
| `/cso` | 5 | gstack | Security needed | OWASP + STRIDE audit |
| `/investigate` | 5 | gstack | Bug found | Root cause → fix |
| `bmad-qa-generate-e2e-tests` | 5 | BMad | Auto needed | E2E test generation |
| `/benchmark` | 5 | gstack | Performance | Web Vitals, bundle size |
| `android-testing` | 5 | standalone | Android project | Android-specific test execution |

### Phase 6 — SHIP (7 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `/review` | 6 | gstack | Pre-ship | Final pre-landing safety check |
| `/ship` | 6 | gstack | Approved | Bump, CHANGELOG, commit, push, PR |
| `/land-and-deploy` | 6 | gstack | PR merged | Deploy + verify |
| `/canary` | 6 | gstack | Deployed | Post-deploy monitoring |
| `/document-release` | 6 | gstack | Shipped | Sync docs to shipped code |
| `/landing-report` | 6 | gstack | Before ship | Check queue, conflicts |
| `/benchmark` | 6 | gstack | Post-deploy | Performance regression check |
| `android-build-ci` | 6 | standalone | Android ship | Gradle bundle, CI/CD verification |

### Phase 7 — RETRO (5 skills)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `bmad-retrospective` | 7 | BMad | Epic done | Lessons learned, action items |
| `gstack-retro` | 7 | gstack | Weekly | Commits, patterns, quality trends |
| `gstack-learn` | 7 | gstack | After retro | Persist cross-session learnings |
| `bmad-party-mode` | 7 | BMad | Complex retro | Multi-agent discussion |
| `bmad-correct-course` | 7 | BMad | Corrections needed | Sprint correction |

### Cross-Cutting (always active)
| Skill | Phase | Framework | Trigger | Description |
|-------|-------|-----------|---------|-------------|
| `bmad-help` | X | BMad | "help", "que faire" | Analyse + recommend |
| `/context-restore` | X | gstack | "reprendre", "resume" | Restore session state |
| `/freeze` / `/guard` | X | gstack | "safety", "solo" | Restrict edits |
| `/plan-tune` | X | gstack | "tune", "trop de questions" | Question frequency |
| `bmad-advanced-elicitation` | X | BMad | "deep critique" | Adversarial deep-dive |
| `bmad-review-edge-case-hunter` | X | BMad | "edge cases" | Exhaustive boundary analysis |
| `bmad-checkpoint-preview` | X | BMad | "checkpoint", "human review" | Human review gate |
| `bmad-distillator` | X | BMad | "distill" | Lossless compression |

### Standalone Patterns Libraries (loaded implicitly)
| Skill | Trigger | Description |
|-------|---------|-------------|
| `architecture-patterns` | Architecture phase | Clean Arch, Hexagonal, DDD |
| `api-design-principles` | API design | REST, GraphQL standards |
| `error-handling-patterns` | Error handling | Exceptions, Result types |
| `design-system-patterns` | UI design | Tokens, theming, components |
| `responsive-design` | Layout | Container queries, fluid typography |
| `interaction-design` | Animations | Microinteractions, motion |
| `visual-design-foundations` | Visual | Typography, color, spacing |
| `auth-implementation-patterns` | Auth | JWT, OAuth2, RBAC |
| `code-review-excellence` | Code review | Best practices |
| `debugging-strategies` | Debugging | Systematic debugging |
| `kpi-dashboard-design` | Dashboard | Metrics, visualization |
| `mobile-ios-design` | iOS | SwiftUI, HIG patterns |
| `screen-reader-testing` | Accessibility | VoiceOver, NVDA, JAWS |
| `wcag-audit-patterns` | Accessibility | WCAG 2.2 audit |
| `workflow-orchestration-patterns` | Background | Temporal, sagas |
| `accessibility-compliance` | Accessibility | WCAG compliant interfaces |

---

## Phase Transition Summary

```
PHASE 0 (INIT)
  ↓ auto
PHASE 1 (VISION) ──→ product-brief.md
  ↓ approval
PHASE 2 (STRATEGY) ──→ prd.md + architecture.md + epics.md
  ↓ stories ready
PHASE 3 (DESIGN) ──→ DESIGN.md + ux-spec + stories
  ↓ story ready-for-dev
PHASE 4 (BUILD) ──→ code + tests + graph update
  ↓ loop per story
PHASE 5 (QUALITY) ──→ health score + review report
  ↓ all clear
PHASE 6 (SHIP) ──→ VERSION bump + deploy + verify
  ↓ epic done
PHASE 7 (RETRO) ──→ learnings + action items
  ↓
  back to PHASE 4 (next sprint) or PHASE 1 (new product)
```

---

## How to Use OMNI_AGENTS.md

1. **At agent start:** Read this file → detect project type → load `OMNI_STATE.yaml` → resume
2. **User asks "que faire":** `bmad-help` analyses current phase + state → recommends next step
3. **During a phase:** Execute primary skill → verify with secondary → update state → advance
4. **Cross-cutting triggers:** Always active, can interrupt current phase for safety/debugging

---

*OMNI Framework v1.0.0 — 62 skills across 8 phases + cross-cutting*
