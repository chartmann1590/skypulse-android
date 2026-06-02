---
name: SkyPulse
colors:
  surface: '#0e131e'
  surface-dim: '#0e131e'
  surface-bright: '#343946'
  surface-container-lowest: '#090e19'
  surface-container-low: '#171b27'
  surface-container: '#1b1f2b'
  surface-container-high: '#252a36'
  surface-container-highest: '#303541'
  on-surface: '#dee2f2'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#dee2f2'
  inverse-on-surface: '#2b303c'
  outline: '#849495'
  outline-variant: '#3a494b'
  surface-tint: '#00dbe7'
  primary: '#e1fdff'
  on-primary: '#00363a'
  primary-container: '#00f2ff'
  on-primary-container: '#006a71'
  inverse-primary: '#00696f'
  secondary: '#bbc3ff'
  on-secondary: '#001d93'
  secondary-container: '#0231de'
  on-secondary-container: '#b1bbff'
  tertiary: '#fff6e4'
  on-tertiary: '#3b2f00'
  tertiary-container: '#fed83a'
  on-tertiary-container: '#725e00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#74f5ff'
  primary-fixed-dim: '#00dbe7'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#dee0ff'
  secondary-fixed-dim: '#bbc3ff'
  on-secondary-fixed: '#000f5d'
  on-secondary-fixed-variant: '#002ccd'
  tertiary-fixed: '#ffe173'
  tertiary-fixed-dim: '#e8c423'
  on-tertiary-fixed: '#221b00'
  on-tertiary-fixed-variant: '#554500'
  background: '#0e131e'
  on-background: '#dee2f2'
  surface-variant: '#303541'
  pitch-black: '#05070A'
  glass-surface: rgba(20, 66, 114, 0.15)
  glass-stroke: rgba(255, 255, 255, 0.1)
  text-high: '#FFFFFF'
  text-med: '#BFBFBF'
  alert-red: '#FF3D00'
typography:
  display-lg:
    fontFamily: Sora
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Sora
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Sora
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  data-lg:
    fontFamily: JetBrains Mono
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  margin-mobile: 16px
  margin-desktop: 32px
  gutter: 16px
  card-padding: 20px
---

## Brand & Style

The design system is engineered for a premium aviation experience, blending the precision of a high-tech cockpit with the accessibility of modern consumer electronics. It targets aviation enthusiasts and professionals who require real-time data visualization without the steep learning curve of legacy avionics.

The aesthetic follows a **Futuristic Glassmorphism** direction. This style utilizes deep-space backgrounds and semi-transparent "HUD" (Heads-Up Display) layers to create a sense of depth and sophistication. High-contrast primary accents provide critical information hierarchy, while frosted glass effects soften the technical edge, making the interface feel airy and advanced.

## Colors

The palette is optimized for low-light environments (cockpit or night-time use) while maintaining high readability. 

- **Backgrounds:** A layered approach using `pitch-black` for the base level and `neutral` (deep navy) for major interface sections.
- **Accents:** `primary` (cyan) is reserved for active states, interactive buttons, and critical data points. `secondary` (electric blue) is used for secondary navigational elements and branding.
- **Functional Colors:** `text-high` ensures maximum legibility for dynamic data, while `text-med` is used for static labels and metadata.

## Typography

This design system uses a tri-font strategy to balance character and utility:
- **Sora (Headlines):** Used for large headers and branding. Its geometric construction feels modern and slightly futuristic.
- **Inter (Body):** The workhorse for UI text, chosen for its exceptional readability on mobile displays and neutral tone.
- **JetBrains Mono (Data):** Used for cockpit data, coordinates, and flight numbers. The monospaced nature ensures that fluctuating numbers don't cause layout jitter.

Use uppercase styling for `label-sm` to evoke a technical, instrumentation feel.

## Layout & Spacing

The layout follows a **Fluid Grid** model based on an 8px spacing rhythm. 

- **Mobile:** A 4-column grid with 16px side margins. Cards usually span the full width to maximize data visibility.
- **Tablet/Desktop:** A 12-column grid. Sidebars are used for persistent navigation, with content modules spanning 4 or 6 columns.
- **Density:** High information density is acceptable for data screens, provided that 16px gutters are maintained between logical groups to prevent visual clutter.

## Elevation & Depth

Depth is established through **Glassmorphism and Tonal Layering** rather than traditional drop shadows.

1.  **Base Layer:** Solid `#05070A`.
2.  **Surface Layer:** Semi-transparent `glass-surface` with a `20px` backdrop blur. This layer features a 1px solid `glass-stroke` to define edges.
3.  **Active Layer:** For modals or floating menus, a subtle glow effect (0px 4px 20px) using the `primary` color at 10% opacity is applied to suggest a light-emitting HUD element.

Avoid heavy black shadows; instead, use slightly lighter navy tints for "raised" surfaces.

## Shapes

The shape language is ultra-modern and soft, contrasting with the technical data it contains. 

- **Primary Containers:** 24px to 32px corner radius (Rounded-XL/XXL) for cards and main UI shells.
- **Interactive Elements:** 12px radius for input fields and smaller buttons.
- **Selection States:** Use subtle "pill" shapes for chips and toggle switches to maintain the fluid, aerodynamic feel.

## Components

### Buttons
- **Primary:** Solid `primary` color with `text-black`. High corner radius (32px).
- **Secondary:** Ghost button with `glass-stroke` and `primary` text.
- **State:** On hover/tap, add a subtle neon outer glow (4px blur) in the button's accent color.

### Cards
- Always use the glassmorphic style: backdrop-blur, semi-transparent navy fill, and a 1px border.
- Internal padding should be a consistent 20px.

### Inputs & Fields
- Filled with a dark, low-opacity blue. Use a bottom-only border (2px) that turns `primary` (Cyan) on focus.

### Icons
- Use thin-stroke (1.5px) line icons. 
- For active navigation items, icons can have a small "aura" (inner glow) to simulate an illuminated instrument panel.

### Lists
- Use horizontal dividers with a 10% white opacity. Each list item should have a minimum touch target height of 56px.