---
version: 1
slug: "a-app-looplayer-ui-player-playerscreen-kt-2652403f"
primary_target: "app/src/main/java/app/looplayer/ui/player/PlayerScreen.kt"
related_targets: ["app/src/main/java/app/looplayer/ui/library/LibraryScreen.kt"]
---

# Player + Library surfaces

Mode: Operate. Platform: android (Compose, Material 3). Code-led (Operate surface).

Audience/job: anyone watching a local video with two subtitle files loaded, switching language mid-scene without leaving the picture. Gestures: double-tap = switch language, horizontal drag = seek, single tap = controls.

Decision: run unattended at the user's request (no pause for questions); the assigned direction was taken without a decision page. Re-roll remains open.

## Direction contract

THESIS: The player is a two-language pocket dictionary held open over the picture. Each loaded subtitle is a language "edition" with its own ink; switching is flipping to the other thumb-index tab. Refuses the category default of a buried "CC" track menu and ±10 s double-tap seek.

OWN-WORLD: Night-ink ground (warm near-black, tonal M3 surfaces). Two language inks own every language mark: Saffron (slot A) and Cobalt-blue (slot B); subtitle text carries a faint tint of its slot. Headwords in a bold serif (system Noto Serif) for screen titles and video titles; Roboto for UI and subtitles; language codes as tracked bold caps, like dictionary thumb-index tabs. Hairline rules, no cards-in-cards.

STORY: The viewer opens a video, attaches two subtitle "editions" on a setup sheet, presses Play, and the chrome falls away. A double-tap anywhere flips the edition; the edge tab confirms which language is now speaking.

FIRST VIEWPORT: Player, landscape, full-bleed video. Controls hidden. Subtitle centered low. On switch, a vertical thumb-index tab stack (TR / EN) slides in on the right edge, active tab filled with its ink, then retreats after ~1.2 s. With controls shown: top bar (back, title, subtitle sheet), center play/pause, bottom scrubber with tabular time and the two-tab language switch.

FORM: Bilingual pocket dictionary / phrasebook thumb-index; position 3 of 7 on the ordered list. seed:f7d494f658242484

Raises: from cassette j-card (declined) — two hard halves with a physical flip: slot A / slot B are fixed sides, the switch is a short y-axis flip of the tab, instant under Remove animations. From transit diagram (declined) — each line owns one color everywhere: a language's ink is identical in library, setup sheet, player tab and subtitle tint. From datamatics (declined) — tabular numerals for every time readout.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
