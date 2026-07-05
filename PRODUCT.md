# Product

## Register

product

## Users

Royal Medical Services (RMS) Jordan — customs and procurement operations staff: officers who manage the daily flow of military/medical equipment imports. They work under time pressure, track multiple open transactions simultaneously, and are accountable to SLA deadlines. They use the app throughout the workday on Android devices, often while multitasking between physical documents and digital records.

## Product Purpose

Track medical equipment customs transactions through defined procurement phases (shipment → military clearance → customs clearance → delivery), monitor SLA compliance in real time, and surface KPI data to operations officers. Success means an officer can open the app, immediately see what needs attention today, and act on it without ambiguity.

## Brand Personality

Authoritative, precise, reliable. The institution trusts the data — the UI must earn that trust through density, accuracy, and zero visual noise. Arabic-first. The tone is formal operations, not consumer-friendly.

## Anti-references

- Consumer and social UI patterns (Material You playfulness, rounded pastel cards, bottom-sheet-everything)
- Generic government forms rendered digitally (PDF-on-screen, no hierarchy, grey on grey)
- SaaS startup aesthetic (cream/sand backgrounds, gradient heroes, growth-dashboard energy)
- Anything that feels like it was designed for general audiences rather than operations professionals

## Design Principles

1. **Urgency has a voice.** Status and SLA state must be immediately readable at a glance — color, weight, and position all work together to surface what needs action now.
2. **Density is a feature.** Operations staff read dense tables and lists fluently. Don't collapse information to protect whitespace; earn the space you use.
3. **Arabic first, layout second.** RTL is the primary direction. Every layout decision — alignment, reading flow, icon placement — must be made with Arabic typography and direction as the default.
4. **Precision over polish.** Accurate data representation beats decorative charts. Every number, label, and status indicator is functional information, not a design element.
5. **Institutional, not corporate.** The design should feel like a tool built for a specific professional context — not a generic enterprise SaaS product skinned with a logo.

## Accessibility & Inclusion

- WCAG AA minimum (4.5:1 contrast for body text, 3:1 for large/bold)
- RTL layout throughout; Arabic Google Fonts already in use
- Status information must never be color-only — always paired with icon or label
- Reduced motion support expected for Android system settings
