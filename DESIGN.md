---
name: Looplayer
description: A local video player whose two subtitle languages sit behind one double-tap, on a true-black flat ground.
colors:
  ground: "#000000"
  surface: "#111111"
  surface-high: "#1C1C1C"
  raised: "#2A2A2A"
  control: "#444444"
  text: "#FFFFFF"
  muted: "#B4B4B4"
  progress: "#3EA6FF"
typography:
  headline:
    fontFamily: "Inter, sans-serif"
    fontSize: "28px"
    fontWeight: 600
    lineHeight: "36px"
    letterSpacing: "-0.5px"
  headline-small:
    fontFamily: "Inter, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: "32px"
    letterSpacing: "-0.3px"
  title:
    fontFamily: "Inter, sans-serif"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: "28px"
    letterSpacing: "-0.2px"
  title-medium:
    fontFamily: "Inter, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: "24px"
    letterSpacing: "-0.1px"
  title-small:
    fontFamily: "Inter, sans-serif"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: "20px"
    letterSpacing: "0px"
  body-large:
    fontFamily: "Inter, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0px"
  body:
    fontFamily: "Inter, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0px"
  body-small:
    fontFamily: "Inter, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: "16px"
    letterSpacing: "0px"
  label:
    fontFamily: "Inter, sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: "20px"
    letterSpacing: "0px"
  index-tab:
    fontFamily: "Inter, sans-serif"
    fontSize: "13px"
    fontWeight: 600
    letterSpacing: "0.4px"
  subtitle:
    fontFamily: "Inter, sans-serif"
    fontSize: "19px"
    fontWeight: 600
    lineHeight: 1.25
rounded:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "20px"
  xl: "28px"
  mark: "5px"
  tab-inner: "7px"
  tab: "10px"
  overlay: "16px"
  full: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.text}"
    textColor: "{colors.ground}"
    typography: "{typography.title-small}"
    rounded: "{rounded.full}"
    height: "56px"
  fab-primary:
    backgroundColor: "{colors.text}"
    textColor: "{colors.ground}"
    typography: "{typography.label}"
    rounded: "{rounded.lg}"
    height: "56px"
  button-tonal:
    backgroundColor: "{colors.raised}"
    textColor: "{colors.text}"
    typography: "{typography.label}"
    rounded: "{rounded.full}"
    height: "40px"
  edge-tab-active:
    backgroundColor: "{colors.text}"
    textColor: "{colors.ground}"
    typography: "{typography.index-tab}"
    rounded: "{rounded.tab}"
    width: "52px"
    height: "44px"
  edge-tab-idle:
    backgroundColor: "{colors.raised}"
    textColor: "{colors.muted}"
    typography: "{typography.index-tab}"
    rounded: "{rounded.tab}"
    width: "52px"
    height: "44px"
  language-switch:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.tab}"
    padding: "3px"
  language-switch-tab-active:
    backgroundColor: "{colors.text}"
    textColor: "{colors.ground}"
    typography: "{typography.index-tab}"
    rounded: "{rounded.tab-inner}"
    width: "52px"
    height: "42px"
  language-switch-tab-idle:
    textColor: "{colors.muted}"
    typography: "{typography.index-tab}"
    rounded: "{rounded.tab-inner}"
    width: "52px"
    height: "42px"
  slot-mark-filled:
    backgroundColor: "{colors.text}"
    textColor: "{colors.ground}"
    rounded: "{rounded.mark}"
    padding: "3px 7px"
  slot-mark-idle:
    backgroundColor: "{colors.raised}"
    textColor: "{colors.muted}"
    rounded: "{rounded.mark}"
    padding: "3px 7px"
  seek-readout:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    typography: "{typography.headline}"
    rounded: "{rounded.overlay}"
    padding: "14px 24px"
  bottom-sheet:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.xl}"
    padding: "0 24px 16px"
  thumbnail:
    backgroundColor: "{colors.surface-high}"
    rounded: "{rounded.sm}"
  watched-bar:
    backgroundColor: "{colors.progress}"
    height: "3px"
---

# Design System: Looplayer

## Overview

**Creative North Star: "The Black Around the Film"**

Looplayer's ground is the black that surrounds a picture in a dark room. Nothing on screen competes with the video: surfaces rise out of the black by lightness alone, chrome is white glyphs over a gradient scrim, and a single hue (progress blue) is spent only on how far you are into something. The signature is a thumb index on the picture's edge: one tab per loaded subtitle language, the speaking one inverted to white and standing proud of the edge, flipping over like a page tab when a double-tap changes language.

The system is dark-only by decision. There is no light scheme and none should be added: the app is a cinema surface, and the theme (`LooplayerTheme`) wires only a `darkColorScheme`, with the window background also #000. Density is calm: generous row heights, one action per region, and controls that appear when asked for and leave after 3 s.

Type is Inter throughout, bundled with the app (Inter 4.1, SIL Open Font License 1.1; files `app/src/main/res/font/inter_{regular,medium,semibold,bold}.ttf`, license text at `app/src/main/assets/licenses/Inter-OFL.txt`). It was chosen for a professional, streaming-platform feel, full Turkish coverage and tabular figures. Dimensions in this file are dp and type sizes sp, written as px for portability.

**Key Characteristics:**
- True-black ground (#000) with tonal steps for surfaces; no borders, no divider lines, no drop shadows at rest.
- The primary action is an inverted white pill with black text.
- One hue, progress blue, reserved for watched progress.
- Language marks are inverted-white when speaking, muted on a dark step when not, in every context (edge tabs, control-bar switch, list marks).
- Inter everywhere, semibold for titles, tracking at zero or tightened, tabular figures on every time readout.

## Colors

A monochrome ladder from black to white with one blue reserved for progress.

### Primary
- **Projector White** (text): the primary action colour as well as the text colour. Fills the primary pill, the FAB, the active language tab and the filled slot mark, always with black text on it. All player chrome icons and the scrubber's active track and thumb are white.

### Tertiary
- **Progress Blue** (progress): only the watched bar along the bottom of a library thumbnail (3px). It never marks selection, links or buttons.

### Neutral
- **Theatre Black** (ground): app background, top app bar, the player behind the video, the playback-error screen, and the text on every inverted white element.
- **Booth Grey** (surface): bottom sheets; the language switch container (at 80% opacity), and the seek readout and hint pill (88%) over the picture.
- **Slate Step** (surface-high): thumbnail placeholder ground.
- **Raised Grey** (raised): idle edge tabs, idle slot marks and the tonal "Add" button.
- **Control Grey** (control): placeholder film glyph in empty thumbnails.
- **Dim Silver** (muted): secondary text (durations, file names, timing offsets, helper copy) and idle language marks. About 10.6:1 on ground and 7:1 on raised.

### Named Rules
**The One Hue Rule.** Progress blue is spent only on how far into a video you are. Selection, focus and emphasis are expressed by inversion to white, never by colour.

**The Inversion Rule.** "Active" or "primary" means a white fill with black text. The speaking language, the main action, and the filled slot mark all use it; nothing else does.

## Typography

**Display Font:** Inter (bundled, weights 400/500/600/700)
**Body Font:** Inter
**Label/Mono Font:** Inter with the `tnum` feature for figures

**Character:** One screen-first sans used the way streaming apps use it: semibold, slightly tightened headings over regular-weight body copy, no decorative pairing.

### Hierarchy
- **Headline** (600, 28px, 36px line): the Library title (large app bar), the empty-library title, and the seek-readout delta.
- **Headline Small** (600, 24px, 32px line): the video title on the setup sheet.
- **Title** (600, 22px, 28px line): sheet headings and the playback-error title.
- **Title Medium** (600, 16px, 24px line): library row titles, the player's top-bar title, loaded subtitle language names.
- **Title Small** (600, 14px, 20px line): label of the primary Play / Resume pill.
- **Body Large / Body / Body Small** (400; 16/24, 14/20, 12/16): settings rows, helper and error copy, row metadata.
- **Label** (500, 14px, 20px line): buttons, the scrubber times, the speed readout, the sheet's section label (sentence case).
- **Index Tab** (600, 13px, +0.4px tracking; 18px on wide screens ≥840dp): two-letter language codes on edge tabs and the control-bar switch; slot marks use it at 11px.
- **Subtitle** (600, 19px at <600dp, 24px at 600–1000dp, 30px above, 1.25 line height, centred): white with a 5px black round-joined stroke at 90% and a 6px soft shadow.

### Named Rules
**The Tabular Clock Rule.** Every time, duration, percentage, offset or line count uses tabular figures (`tnum`) so digits do not jitter while playing.

**The Zero Tracking Rule.** Titles and headlines tighten (−0.1 to −0.5px); body and labels sit at zero. Only the language index codes are spaced (+0.4px). Material's default positive tracking does not return.

## Layout

Phone and tablet, portrait and landscape. The library is a single column capped at 720dp, centred on tablets, with the large app bar constrained to the same column so heading and rows share one left edge. Rows are 16dp in from the start, with a 128dp 16:9 thumbnail, 16dp gap, and a text column of title, metadata and language marks spaced 6dp apart. Rows have 14dp vertical padding and no dividers: separation is spacing alone. The FAB leaves 96dp of bottom clearance on the list.

Sheets use 24dp horizontal padding; slot rows use 14dp vertical padding with an 8dp gap between rows (and before the show-subtitles row), with secondary lines indented 58dp to align under the language name. Empty and error states are a 420dp-max column, left-aligned (library) or centred (error), with 32dp side padding.

The player is full-bleed under the system bars, with controls inside the safe-drawing insets: a top row (back, title, speed, subtitles), a centre row (−10 s, a 76dp play/pause disc, +10 s, 40dp apart, set slightly above centre), and a bottom block (scrubber with 52dp-min time columns, then the language switch at the start and rotation lock and fill/fit at the end). The subtitle sits 28dp above the bottom edge and lifts to 116dp when controls show. Edge tabs are centred on the end edge. Touch targets are at least 48dp.

Spacing rhythm is 4dp-based (4, 8, 12, 16, 24, 32), with 6dp and 14dp as recurring in-component steps.

## Elevation & Depth

Flat. Surfaces rise by lightness alone (#000 → #111 → #1C1C1C → #2A2A2A) and carry no drop shadows and no outlines at rest. Over the picture, depth is translucency: dark surface panels at 80–88% opacity, and two black gradient scrims behind visible controls (top 140dp from 70% to clear, bottom 200dp from clear to 80%) so white chrome stays legible over bright footage. The subtitle line is drawn above the scrims, so it keeps full white while the controls are up.

### Shadow Vocabulary
- **Subtitle outline** (5px round-joined stroke at rgba(0,0,0,0.9) plus a 6px blur shadow at rgba(0,0,0,0.6)): subtitle text only. This is how white captions stay readable on any footage, and it is part of the world, not an exception to remove.

### Named Rules
**The Lightness Not Shadow Rule.** A surface that needs to sit above another gets a lighter grey step or a translucent dark panel, never a drop shadow or a border. Lists separate items by space, not lines.

## Shapes

Softly rounded, never sharp and never bubbly. Thumbnails use 8px corners; the bottom sheet's top corners are 28px; the FAB is 20px; primary and tonal buttons are full pills. The language vocabulary has its own small radii: edge tabs are rounded on the inner side only (10px top-start and bottom-start, square against the screen edge, so they read as tabs attached to the edge), the control-bar switch is a 10px tray holding 7px tabs, and slot marks are 5px. Overlays on the picture (seek readout) are 16px. The play/pause control is a circle.

## Components

### Buttons
- **Shape:** full pill (9999px); the FAB at 20px.
- **Primary:** white fill, black text, 56dp tall and full width on the setup sheet ("Play" or "Resume from 1:08:20", with a leading play icon). The library FAB ("Open video") uses the same inversion.
- **Tonal:** raised grey (#2A2A2A) with white text and a leading 18dp add icon; used for "Add" on an empty subtitle slot.
- **Text:** white text buttons for "Replace" and hint actions; muted text for a secondary "Back" on the error screen.
- **Player icon buttons:** white Material Rounded icons; ±10 s at 32dp in 56dp targets; play/pause at 44dp inside a 76dp disc of white at 14%.

### Chips
- **Slot mark:** a small static language tab (two-letter code, index-tab type at 11px, 5px corners, 3×7dp padding). White with black text when that language is the one speaking; raised grey with muted text otherwise. Used in library rows and slot rows.

### Cards / Containers
- **Corner Style:** 28px top corners on bottom sheets; 8px on thumbnails.
- **Background:** sheets on surface (#111111); thumbnails on surface-high with a control-grey film glyph until a frame loads.
- **Shadow Strategy:** none (see Elevation & Depth).
- **Border:** none; rows are separated by spacing, never by divider lines.
- **Internal Padding:** 24dp horizontal in sheets.

### Inputs / Fields
- **Switch:** Material 3 switch on the theme: white track, black thumb when on. A full row is the toggle target.
- **Scrubber:** 4dp track, white active track and thumb, white at 28% inactive, no gap or stop indicator; tabular times either side.

### Navigation
- **Library:** large collapsing top app bar on ground, title in headline style.
- **Player top bar:** back arrow, single-line title, speed readout (`1×`, tabular), subtitles icon; white over the top scrim.

### Edge Index Tabs (signature)
A vertical stack of language tabs on the right edge of the picture, one per loaded subtitle (52×44dp, 76×60dp at ≥840dp, 6dp or 8dp apart). The speaking tab is white with black text and sits flush to the edge; idle tabs are opaque raised grey with muted text, pulled 10dp in. On a switch the active tab flips (x-axis rotation 90° → 0°, 260ms, FastOutSlowIn) and its reach animates over 220ms. The stack slides in from the edge (200ms), shows with the controls or for 1.4 s after a switch, and slides out (160ms).

### Language Switch (signature)
The same two tabs laid side by side in the control bar's bottom-start: a 10px booth-grey tray at 80% with 3dp padding holding 52×42dp tabs with 7px corners; the active tab white with black text, the idle one transparent with muted text. Colour changes over 180ms. Tapping either selects it. It is the visible equivalent of the double-tap gesture.

### Seek Readout and Hint Pill
Dark translucent panels over the picture (booth grey at 88%). The seek readout is centred, 16px corners, 14×24dp padding: a headline-sized signed delta (`+0:24`) over a body-size `position / duration` line in white at 75%. The hint pill sits 72dp below the top inset, max 480dp wide, at least 48dp tall, body text with an optional semibold white text action. A notice over a film is never the brightest thing on screen.

### Subtitle Line
Centred, low, white Inter semibold with the subtitle outline, 32dp side padding, cross-fading (160ms in, 90ms out) when the language changes.

## Do's and Don'ts

### Do:
- **Do** keep the app dark-only: ground #000, surfaces as lighter greys, no light scheme.
- **Do** mark the active or primary thing by inverting to a white fill with black text.
- **Do** keep progress blue (#3EA6FF) on watched progress only.
- **Do** use Inter from the bundled font files, and carry the Inter OFL license with any build that ships them.
- **Do** set every time, duration, count and offset in tabular figures.
- **Do** give every gesture a visible control (the language switch, ±10 s buttons) with a content description and a 48dp target.
- **Do** keep subtitles white with the black stroke and soft shadow.
- **Do** respect Remove animations: the tab flip, slides and fades must resolve instantly when animations are off.

### Don't:
- **Don't** put drop shadows, outlines or divider lines on surfaces at rest; use a lighter grey step, a translucent dark panel, or spacing.
- **Don't** introduce a second accent hue, or colour-code the two subtitle languages; slots differ by position and inversion, not by ink.
- **Don't** make an overlay on the picture brighter than the picture's chrome; notices sit on 88% dark panels.
- **Don't** restore Material's default positive letter-spacing on body and labels.
- **Don't** substitute a system or downloadable font for the bundled Inter.
