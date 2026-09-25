---
name: Artemis Telemetry
description: A restrained institutional report for installations and university contacts.
colors:
  ink: "#172e46"
  muted: "#526474"
  blue: "#176ca4"
  deep: "#102e49"
  canvas: "#f6f7f8"
  paper: "#fff"
  line: "#d8e0e6"
  soft: "#e9f0f5"
  link: "#0b639a"
  control-border: "#748797"
  focus: "#b56a00"
  unknown: "#8a9cab"
  table-heading: "#eaf0f4"
  row-hover: "#f7fafc"
  danger: "#a12929"
  danger-bg: "#fff0ee"
typography:
  display:
    fontFamily: "Source Sans 3, Segoe UI, sans-serif"
    fontSize: "clamp(36px, 4vw, 52px)"
    fontWeight: 650
    lineHeight: 1.12
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Source Sans 3, Segoe UI, sans-serif"
    fontSize: "clamp(28px, 3.3vw, 38px)"
    fontWeight: 650
    lineHeight: 1.2
    letterSpacing: "-0.025em"
  title:
    fontSize: "23px"
    fontWeight: 650
    lineHeight: 1.5
    letterSpacing: "-0.015em"
  chart-title:
    fontSize: "19px"
    fontWeight: 650
    lineHeight: 1.5
  body:
    fontFamily: "Source Sans 3, Segoe UI, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontSize: "14px"
    fontWeight: 600
    lineHeight: 1.5
  data:
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
  detail:
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.5
rounded:
  bar: "2px"
  control: "5px"
  table: "6px"
  panel: "8px"
spacing:
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "20px"
  xl: "24px"
  panel: "32px"
components:
  button-primary:
    backgroundColor: "{colors.blue}"
    textColor: "{colors.paper}"
    rounded: "{rounded.control}"
    padding: "9px 16px"
  button-secondary:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "9px 16px"
  button-header:
    backgroundColor: "transparent"
    textColor: "{colors.paper}"
    rounded: "{rounded.control}"
    padding: "9px 16px"
  button-text:
    backgroundColor: "transparent"
    textColor: "{colors.link}"
    rounded: "{rounded.control}"
    padding: "8px"
  input:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "9px 12px"
  login-panel:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.panel}"
    padding: "32px"
  chart-bar:
    backgroundColor: "{colors.blue}"
    height: "12px"
  table:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.table}"
    typography: "{typography.data}"
---

# Design System: Artemis Telemetry

## Overview

**Creative North Star: "The Institutional Report"**

An institutional report for maintainers: a navy masthead anchors a light canvas, with compact typography, labeled distributions, and flat data tables. White surfaces and dividing rules establish hierarchy while native controls keep interaction familiar.

The visual system serves careful reading and comparison. Text carries counts, uncertainty, and action meaning; color reinforces those meanings. The implementation uses self-hosted type, CSS bars, and an inline geometric brand mark without raster imagery.

**Key Characteristics:**
- Restrained navy and blue on a light canvas.
- Flat, bordered surfaces and compact report typography.
- Native controls, visible keyboard focus, and textual chart values.

Source authority: `client/src/styles.css`, `client/src/app/app.html`, and `client/src/app/chart.ts`. Tokens above describe the implementation; `PRODUCT.md` holds product constraints.

## Colors

Navy provides structure, blue indicates actions and measured values, and cool neutrals support prolonged reading.

### Primary

- **Report Navy** (`deep`): masthead and link hover.
- **Telemetry Blue** (`blue`): primary action, reported chart bars, and the login rule.
- **Link Blue** (`link`): underlined links and history actions.

### Neutral

- **Ink** and **Muted Slate** (`ink`, `muted`): primary text and secondary explanations.
- **Canvas** and **Paper** (`canvas`, `paper`): page background and contained data/form surfaces.
- **Divider** and **Soft Track** (`line`, `soft`): separating rules and chart tracks.
- **Control Border** (`control-border`): input, select, and secondary-button boundaries.
- **Unknown Slate** (`unknown`): bars for “Not reported,” always paired with text.
- **Table Heading** and **Row Hover** (`table-heading`, `row-hover`): compact table orientation and pointer feedback.

Amber focus is reserved for keyboard orientation. Danger text on a pale danger surface identifies errors alongside explanatory wording.

**The Text First Rule.** Every distribution retains a category label, count, and percentage; color is supplementary.

## Typography

Source Sans 3 is self-hosted through `@fontsource/source-sans-3`, using Latin regular and semibold files. Segoe UI and sans-serif are fallbacks. The frontmatter records declared CSS weights; headings request 650 while the shipped font faces are 400 and 600, with font synthesis disabled.

Display typography belongs to the sign-in introduction; the headline role is the page title, title is a section heading, and chart-title is a distribution heading. Body copy uses the regular face; labels and emphasized institution names use semibold. Tables use the data role and secondary notes use detail. Counts and table data use tabular numerals. The login introduction limits its descriptive paragraph to 31ch.

## Layout

The centered main container is at most 1408px wide with 24px horizontal padding and 44px top padding. The masthead aligns its content to the resulting 1360px inner width. Page and section headings pair text with a compact trailing action or annotation.

Filters use three columns: a flexible search field, 250px environment selector, and 200px recency selector. Charts share one paper surface with two equal columns, a 40px column gap, and 26px horizontal inset. The directory keeps a 1020px minimum table width; history keeps 820px. Their focusable, labeled containers scroll horizontally.

At 850px and below, filters become two columns with search spanning both; chart insets and gaps tighten. At 600px and below, main gutters become 16px, filters and charts stack, pagination wraps into two rows, and footer content stacks. The page heading is 27px on this narrow layout. Sign-in changes from a two-column composition to a stacked introduction and form; its display heading becomes 36px.

## Elevation & Depth

Surfaces are flat with no box shadows. Paper against canvas, thin divider borders, and a heavier top rule for history establish grouping. Avoid adding elevation where a border or spacing already communicates the relationship.

**The Flat Surface Rule.** Use tonal contrast and dividing rules to group content; retain the implemented shadow-free surfaces.

## Shapes

Controls and alerts have gently rounded corners; table wrappers and containing panels use the larger radii recorded above. Chart tracks are shallow rectangles with a subtle radius and clipped fills. Borders are single-pixel rules, except the history section's two-pixel ink rule. The brand is an inline, stroked geometric SVG.

## Components

- **Buttons:** primary blue, paper secondary with a control border, transparent outlined masthead action, and underlined text action. Standard buttons have a 42px minimum height; pagination uses 36px and compact padding. Enabled hover applies `brightness(0.92)`; disabled buttons use half opacity and the default cursor.
- **Fields:** native inputs and selects with visible labels, paper background, a control border, and 44px minimum height. Labels sit 6px above fields. Preserve native selection and password-manager affordances.
- **Keyboard focus:** a three-pixel amber outline with a three-pixel offset applies through `:focus-visible`. The skip link becomes visible on focus. History receives focus on opening; closing restores its initiating button, falling back to search when that row is absent.
- **Panels:** the login form uses a paper surface, divider border, and panel radius. Its padding drops from 32px to 24px at the medium breakpoint. Charts share a containing surface rather than separate elevated cards.
- **Distribution tables:** row labels accompany CSS bars, counts, and percentages. Bars are hidden from assistive technology; semantic table labels carry the information. Unknown values retain explicit wording and the unknown bar color. At narrow widths, chart sections stack with dividing rules.
- **Directory and history:** shaded table headings, top-aligned cells, underlined links, and muted secondary lines support scanning. Row hover is subtle. History is an inline section below the directory, with a close action and its own pagination.
- **Feedback:** loading states use status text, errors use alerts, and empty states provide an explanation and a reset action when filtering caused the empty result. Preserve clear wording for missing values.

Only the login form has entrance motion: an eight-pixel vertical settle over 0.3s with `ease-out`, enabled solely when reduced motion is not requested. There are no animated chart fills.

## Do's and Don'ts

### Do

- Do retain visible labels, counts, percentages, and explicit missing-data wording.
- Do keep tables readable through keyboard-accessible horizontal scrolling on narrow screens.
- Do use the shared focus treatment and restore focus when closing history.
- Do use self-hosted Source Sans 3 and the implemented native control styles.

### Don't

- Don't replace missing data with zero or rely on bar color alone.
- Don't introduce decorative shadows, gradients, or raster imagery into this report layout.
- Don't hide table columns merely to fit a small viewport.
