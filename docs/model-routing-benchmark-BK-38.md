# BK-38 Model Routing Benchmark

Date: 2026-09-04

## Routing

- Build and reviewer: `opencode-go/gpt-5.6-luna`
- Explore, general, and plan: `opencode-go/glm-5.3-flash`
- Lightweight internal tasks: `opencode-go/glm-5.3-flash`

## Quality Comparison

The same read-only review prompt was run with the strong model against the
pre-change configuration at commit `520fb10` and the final configuration at
commit `006a3b3`.

- Before: the review reported that model routing was not configured and AC1
  was not met.
- After: the review reported that each agent was assigned to its intended tier
  and AC1 was met.
- Reviewer preservation and JSON validity were met in both runs.
- No additional reviewer-quality finding or rework was introduced by the
  routing change.

## Cost Comparison

The same read-only configuration-inspection prompt was run once per model.
Costs reported by OpenCode for the complete sessions were:

| Model | Session cost |
| --- | ---: |
| `opencode-go/gpt-5.6-luna` | `$0.00493301` |
| `opencode-go/qwen3.8-flash` | `$0.00554638` |
| `opencode-go/glm-5.3-flash` | `$0.00219860` |

The selected lightweight tier was approximately 55% cheaper than the strong
model in this probe. Both probes returned the same routing summary, and neither
required rework. These are controlled probes, not a claim about all future
sessions; session cost and rework should be monitored after merge.
