# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

delegated: Kotlin + Jetpack Compose (Material 3) + AndroidX Media3 ExoPlayer. Chosen because Media3 is the platform-standard playback engine and parses SRT/WebVTT/SSA side-loaded subtitles natively; Compose because it is the current native UI toolkit.

## Users

Everyone (confirmed): ordinary people watching local video files on their Android phone or tablet. The defining scene: someone watching a film or episode in a language they only partly follow, with two subtitle files loaded (for example Turkish and English), who wants to flip between them mid-scene without stopping or digging through menus.

## Product Purpose

A local video player whose distinguishing job is instant switching between two loaded subtitle languages while the controls are hidden. Success: the viewer double-taps, the subtitle language changes in place, and they never leave the picture.

## Positioning

Two subtitle slots (primary / secondary) are a first-class concept, not a buried track menu. Double-tap anywhere on the picture swaps the active slot (confirmed). Horizontal drag scrubs backward/forward (confirmed), so double-tap is free for language switching rather than the usual ±10 s seek.

## Capabilities and Constraints

- Open local video files through the system file picker (Storage Access Framework); no network, no accounts.
- Attach up to two subtitle files (SRT, WebVTT, SSA/ASS, TTML) before or during playback; each gets a language label.
- Standard player functions: play/pause, seek bar, drag-to-seek, auto-hiding controls, fullscreen landscape, rotate lock, playback speed, resume position.
- Gesture map (confirmed): double-tap = switch subtitle language; horizontal drag = seek. Assumption: single tap toggles controls; vertical drags left/right adjust brightness/volume are out of scope for v1.
- Assumption: the two loaded subtitles show one at a time (not stacked); a "both" mode is an open decision.

## Brand Commitments

Name: Looplayer (from the project folder; assumption, not confirmed). UI copy in Turkish and English (string resources), default Turkish source strings with English translation — assumption.

## Evidence on Hand

No logo, brand assets, sample videos, or subtitles exist in the repo. Do not fabricate store ratings, download counts, or codec claims.

## Product Principles

1. The picture is the product: chrome appears only when asked for and leaves quickly.
2. Language switching costs one gesture and gives immediate, legible confirmation.
3. Gestures are always backed by visible controls (switch button in the control bar too).
4. Respect the system: Back, insets, font scale, Remove animations.

## Accessibility & Inclusion

Every gesture has a visible-control equivalent with a content description; subtitle text respects the system caption style and font scale; touch targets ≥48dp.
