---
version: 1
slug: "a-app-looplayer-ui-player-playerscreen-kt-2652403f"
primary_target: "app/src/main/java/app/looplayer/ui/player/PlayerScreen.kt"
related_targets: ["app/src/main/java/app/looplayer/ui/library/LibraryScreen.kt"]
---

# Player + Library surfaces

Mode: Operate. Platform: android (Compose, Material 3). Code-led (Operate surface).

Audience/job: anyone watching a local video with two subtitle files loaded, switching language mid-scene without leaving the picture. Gestures: double-tap = switch language, horizontal drag = seek, single tap = controls.

Revision (user-pinned, 2026-11-10): the user rejected the saffron/cobalt palette and asked for professional, streaming-platform typography. The world was re-dealt with tasteskill's app draw (`true-black-flat`, seed 535282823) and the type moved from serif headwords to Inter. The thumb-index switch survives; per-language inks do not.

## Direction contract

THESIS: The player is the black around a film, with a thumb index on its edge. Two subtitle languages sit behind one gesture; switching is flipping to the other tab. Refuses the category default of a buried "CC" track menu and ±10 s double-tap seek.

OWN-WORLD: True-black flat. Ground #000, surfaces rise by lightness alone (#111 surface, #2a2a2a raised), no borders or shadows on anything at rest. The primary action is an inverted white pill with black text. One hue, progress blue #3EA6FF, spent only on how far you are into something (watched bar). Language tabs: the speaking language is inverted (white, black text), the other sits on #2a2a2a in #b4b4b4. Subtitles are white with a dark outline. Inter (bundled, 400/500/600/700) everywhere, tabular figures on every time readout.

STORY: The viewer opens a video, attaches two subtitles on a setup sheet, presses Play, and the chrome falls away. A double-tap anywhere flips the language; the edge tab confirms which language is now speaking.

FIRST VIEWPORT: Player, landscape, full-bleed video. Controls hidden. Subtitle centered low. On switch, a vertical tab stack (TR / EN) slides in on the right edge, the active tab inverted white and proud of the edge, then retreats after ~1.4 s. With controls shown: top bar (back, title, speed, subtitle sheet), centre ±10 s and play/pause, bottom scrubber with tabular times, the two-tab language switch at bottom left, rotation lock and fill/fit at bottom right.

FORM: Thumb-index over a true-black flat app style; original grounded position 3 of 7 (seed:f7d494f658242484), world re-dealt by the user's request (tasteskill true-black-flat).

Raises kept: from cassette j-card — two hard halves with a physical flip: slot 1 / slot 2 are fixed sides, the switch is a short x-axis flip of the tab, instant under Remove animations. From datamatics — tabular numerals for every time readout.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
