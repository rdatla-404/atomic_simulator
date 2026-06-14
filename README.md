# Atomic Simulator

Interactive atomic and chemistry simulator built in **Java** (GUI) and **Python** (OOP engine).

---

## File Overview

| File | Language | Role |
|---|---|---|
| `AtomicSimulator.java` | Java 8+ | Interactive Swing GUI |
| `elements.py` | Python 3.8+ | All 118 elements + PeriodicTable class |
| `atom.py` | Python 3.8+ | Nucleus, ElectronConfig, Atom, Decay |
| `compound.py` | Python 3.8+ | Bond, Compound, BondPredictor, Library |
| `main.py` | Python 3.8+ | CLI entry point and demo |

---

## Java GUI — Run Instructions

Requires: **Java 8 or later** (`java -version` to check)

```bash
# Compile
javac AtomicSimulator.java

# Run
java AtomicSimulator
```

### GUI Features
- ** Atom View** — animated Bohr model, searchable element list, full property panel
- ** Periodic Table** — color-coded 18×10 grid, hover info bar, click to view atom
- ** Compounds** — double-click elements to build a compound, click Analyze for chemistry rules

---

## Python CLI — Run Instructions

Requires: **Python 3.8+**, no external packages.

```bash
# Interactive menu
python main.py

# Quick non-interactive demo
python main.py demo
```

### CLI Commands
```
atom      — Full element/atom info (shells, config, nucleus, properties)
compare   — Side-by-side comparison of two elements
compound  — Build a compound from element pairs (e.g. H 2 O 1)
library   — Browse all 25 known compounds
bond      — Analyze a bond between two elements (type, polarity, ΔEN)
decay     — Radioactive decay simulation (alpha/beta/gamma + Monte Carlo)
table     — ASCII periodic table
search    — Find elements by name or symbol
```

---

## OOP Architecture

```
Java:
  AtomicSimulator
  ├── Element            (data model, 118 entries)
  ├── Compound           (data model, 25 entries)
  ├── BohrPanel          (Graphics2D + javax.swing.Timer)
  ├── AtomTab            (search + animated view + info)
  ├── TableTab           (18×10 null-layout grid)
  ├── CompoundTab        (tray + chemistry engine)
  └── MainWindow         (JFrame host)

Python:
  elements.py
  ├── Element            (@dataclass — immutable)
  └── PeriodicTable      (lookup, search, filter methods)

  atom.py
  ├── Nucleus            (Z, N, stability, binding energy)
  ├── ElectronConfiguration  (Aufbau fill, exceptions)
  ├── Ion                (charged atom)
  ├── Atom               (composite model)
  ├── RadioactiveDecay   (α, β⁻, β⁺, γ)
  └── MonteCarloDecay    (probabilistic simulation)

  compound.py
  ├── Bond               (type, order, polarity, ΔEN)
  ├── Compound           (formula, composition, molar mass)
  ├── CompoundLibrary    (25 known compounds, exact lookup)
  └── BondPredictor      (ionic / covalent / metallic rules)
```

---

## Chemistry Rules Engine

The predictor applies chemistry laws in order:

1. **Library lookup** — exact match against 25 known compounds
2. **Noble gas** — no reaction (complete valence shell)
3. **Metal + Metal** → metallic alloy (delocalized electrons)
4. **Metal + Nonmetal** → ionic compound (cross-multiply oxidation states)
5. **Nonmetal + Nonmetal** → covalent compound (share electrons for octets / duet for H)

---

## Room for Improvement

| Area | What to add |
|---|---|
| Java GUI | 3-D molecular geometry viewer (JavaFX or JOGL) |
| Java GUI | Export Bohr model as PNG |
| Java GUI | Isotope selector per element |
| Java GUI | Decay chain visualizer |
| Python | Real half-life data per isotope |
| Python | Nuclear binding energy chart |
| Python | Full VSEPR geometry solver |
| Python | Lewis structure electron counter |
| Python | API integration with PubChem / NIST |
| Both | More compounds in the library |
| Both | Orbital-level quantum number assignment |
| Both | Electron affinity and ionization energy data |
