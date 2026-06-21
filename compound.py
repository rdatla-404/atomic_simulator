"""
compound.py

Classes:
  Bond           - Describes a chemical bond between two elements
  Compound       - A molecule/compound with formula, bonding, and properties
  BondPredictor  - Chemistry rules engine: ionic / covalent / metallic prediction
  CompoundLibrary - Curated database of 25 known compounds
"""

from __future__ import annotations
import math
from dataclasses import dataclass, field
from typing import Optional, List, Dict, Tuple
from elements import Element, PeriodicTable, periodic_table, METAL_CATEGORIES, to_sub


# ─── CHEMISTRY HELPERS ──────────────────────────────────────
def _gcd(a: int, b: int) -> int:
    while b: a, b = b, a % b
    return a

def _en_diff(a: Element, b: Element) -> Optional[float]:
    if a.electronegativity is None or b.electronegativity is None:
        return None
    return abs(a.electronegativity - b.electronegativity)


# ─── BOND ───────────────────────────────────────────────────
BOND_TYPES = ("ionic", "covalent", "metallic", "hydrogen",
              "van_der_waals", "coordinate", "inert", "none")


class Bond:
    """
    Represents a chemical bond between two elements.

    Bond type classification by electronegativity difference (Pauling):
      ΔEN < 0.4   → nonpolar covalent
      0.4-1.7     → polar covalent
      > 1.7       → ionic
    """

    def __init__(self, a: Element, b: Element,
                 bond_type: str = "covalent", order: int = 1) -> None:
        if bond_type not in BOND_TYPES:
            raise ValueError(f"Unknown bond type '{bond_type}'. Valid: {BOND_TYPES}")
        self.a, self.b      = a, b
        self.bond_type      = bond_type
        self.order          = order  # 1=single, 2=double, 3=triple

    @property
    def electronegativity_diff(self) -> Optional[float]:
        return _en_diff(self.a, self.b)

    @property
    def is_polar(self) -> bool:
        diff = self.electronegativity_diff
        if diff is None: return False
        return 0.4 < diff < 1.7

    @property
    def predicted_type(self) -> str:
        """Predict type from Pauling ΔEN rule."""
        diff = self.electronegativity_diff
        if diff is None: return self.bond_type
        if diff > 1.7:  return "ionic"
        if diff > 0.4:  return "polar covalent"
        return "nonpolar covalent"

    def __repr__(self) -> str:
        return (f"Bond({self.a.symbol}–{self.b.symbol}, "
                f"type={self.bond_type}, order={self.order})")

    def __str__(self) -> str:
        diff = self.electronegativity_diff
        return (f"Bond: {self.a.symbol} — {self.b.symbol}\n"
                f"  Type            : {self.bond_type}\n"
                f"  Bond order      : {self.order}\n"
                f"  ΔEN             : {round(diff,2) if diff is not None else '—'}\n"
                f"  Polar           : {self.is_polar}\n"
                f"  Predicted type  : {self.predicted_type}")


# ─── COMPOUND ───────────────────────────────────────────────
class Compound:
    """
    A chemical compound with formula, element composition, bonding, and metadata.

    Usage:
        from compound import Compound, BondPredictor
        pred = BondPredictor()
        result = pred.analyze([('H', 2), ('O', 1)])
        print(result)
    """

    def __init__(self, formula: str, name: str,
                 composition: Dict[str, int],
                 bond_type:   str,
                 shape:       Optional[str]   = None,
                 description: str             = "",
                 uses:        str             = "",
                 known:       bool            = False,
                 predicted:   bool            = False) -> None:
        self.formula        = formula
        self.name           = name
        self.composition    = composition   # {symbol: count}
        self.bond_type      = bond_type
        self.shape          = shape
        self.description    = description
        self.uses           = uses
        self.known          = known
        self.predicted      = predicted

    @property
    def molar_mass(self) -> float:
        total = 0.0
        for sym, cnt in self.composition.items():
            el = periodic_table.get(sym)
            if el: total += el.mass * cnt
        return round(total, 4)

    @property
    def element_count(self) -> int:
        return len(self.composition)

    @property
    def atom_count(self) -> int:
        return sum(self.composition.values())

    def _center_and_peripheral(self) -> Tuple[Optional[str], Optional[str], int]:
        """
        Heuristic for simple AXn molecules: the element with count==1 is
        treated as the central atom, the other as the peripheral (repeated)
        atom. Only meaningful for exactly two distinct elements.
        Returns (center_symbol, peripheral_symbol, peripheral_count) or
        (None, None, 0) if the heuristic doesn't apply.
        """
        if len(self.composition) != 2:
            return None, None, 0
        items = sorted(self.composition.items(), key=lambda kv: kv[1])
        (sym_a, n_a), (sym_b, n_b) = items
        if n_a == 1:
            return sym_a, sym_b, n_b
        return None, None, 0

    def render_structure(self) -> str:
        """
        Render a simple ASCII diagram of the compound's geometry.
        This is illustrative, not a true molecular model — it exists to give
        a visual sense of shape (linear / bent / pyramidal / tetrahedral /
        lattice), not a chemically rigorous structure.
        """
        shape  = (self.shape or "").lower()
        center, periph, n = self._center_and_peripheral()

        # Lattice / network solids (ionic crystals, network covalent solids)
        if any(k in shape for k in ("crystal", "lattice", "network", "fcc", "hexagonal")):
            syms = list(self.composition.keys())
            a = syms[0]
            b = syms[1] if len(syms) > 1 else syms[0]
            rows = []
            for r in range(3):
                row = [a if (r + c) % 2 == 0 else b for c in range(4)]
                rows.append("  " + "  ".join(row))
            return "\n".join(rows) + "\n  (repeating lattice)"

        if center and n == 2 and "linear" in shape:
            return f"  {periph} — {center} — {periph}"

        if center and n == 2 and "bent" in shape:
            return (f"        {center}\n"
                    f"       / \\\n"
                    f"      {periph}   {periph}")

        if center and n == 3 and "pyramidal" in shape:
            return (f"          {center}\n"
                    f"        / | \\\n"
                    f"       {periph}  {periph}  {periph}")

        if center and n == 3 and "planar" in shape:
            return (f"      {periph}   {periph}\n"
                    f"        \\ /\n"
                    f"         {center}\n"
                    f"         |\n"
                    f"         {periph}")

        if center and n == 4 and "tetrahedral" in shape:
            return (f"            {periph}\n"
                    f"            |\n"
                    f"      {periph} — {center} — {periph}\n"
                    f"            |\n"
                    f"            {periph}")

        if self.atom_count == 2:
            a, b = list(self.composition.keys())
            return f"  {a} — {b}"

        # Generic fallback: simple bonded chain
        return "  " + " — ".join(
            sym if cnt == 1 else f"{sym}{to_sub(cnt)}"
            for sym, cnt in self.composition.items()
        )

    def __repr__(self) -> str:
        return f"Compound({self.formula}, {self.name}, type={self.bond_type})"

    def __str__(self) -> str:
        status = "✓ Known" if self.known else ("~ Predicted" if self.predicted else "? Unknown")
        comp   = " + ".join(f"{cnt}×{sym}" for sym, cnt in self.composition.items())
        lines  = [
            f"\n{'─'*46}",
            f"  {self.formula}   {self.name}   [{status}]",
            f"{'─'*46}",
            f"  Composition   : {comp}",
            f"  Molar mass    : {self.molar_mass} g/mol",
            f"  Bond type     : {self.bond_type}",
        ]
        if self.shape:        lines.append(f"  Shape (VSEPR) : {self.shape}")
        if self.bond_type not in ("none", "inert"):
            lines.append(f"\n  Structure:\n{self.render_structure()}")
        if self.description:  lines.append(f"\n  {self.description}")
        if self.uses:         lines.append(f"\n  Uses: {self.uses}")
        return "\n".join(lines)


# ─── COMPOUND LIBRARY (25 known compounds) ──────────────────
def _c(f, n, comp, t, sh, d, u) -> Compound:
    return Compound(formula=f, name=n, composition=comp, bond_type=t,
                    shape=sh, description=d, uses=u, known=True, predicted=False)


class CompoundLibrary:
    """Curated database of well-known compounds."""

    _DB: List[Compound] = [
        _c("H₂O",   "Water",                  {"H":2,"O":1},        "covalent", "Bent",
           "Universal solvent. Polar molecule with unique hydrogen bonding. Density maximum at 4°C.",
           "Solvent, biological processes, industrial cooling"),
        _c("NaCl",  "Sodium Chloride (Salt)",  {"Na":1,"Cl":1},      "ionic",    "FCC Crystal",
           "Common table salt. Ionic crystal; dissociates into Na⁺ and Cl⁻ in water.",
           "Food preservation, de-icing roads, chemical production"),
        _c("CO₂",   "Carbon Dioxide",          {"C":1,"O":2},        "covalent", "Linear",
           "Two C=O double bonds. Greenhouse gas; product of combustion and respiration.",
           "Fire extinguishers, carbonation, photosynthesis reactant"),
        _c("NH₃",   "Ammonia",                 {"N":1,"H":3},        "covalent", "Trigonal Pyramidal",
           "Lone pair gives trigonal pyramidal shape. Weak base with strong smell.",
           "Fertilizers (Haber process), cleaning products, refrigerant"),
        _c("CH₄",   "Methane",                 {"C":1,"H":4},        "covalent", "Tetrahedral",
           "Simplest hydrocarbon. 86× warming potential of CO₂ over 20 years.",
           "Natural gas fuel, hydrogen production, chemical feedstock"),
        _c("HCl",   "Hydrochloric Acid",       {"H":1,"Cl":1},       "covalent", "Linear",
           "Strong acid; fully dissociates in water. Present in gastric acid.",
           "PVC production, metal pickling, pH control"),
        _c("H₂SO₄", "Sulfuric Acid",           {"H":2,"S":1,"O":4},  "covalent", "Tetrahedral (at S)",
           "Most widely produced industrial chemical. Highly corrosive and dehydrating.",
           "Battery acid, fertilizers, chemical processing"),
        _c("Fe₂O₃", "Iron(III) Oxide (Rust)",  {"Fe":2,"O":3},       "ionic",    "Crystal Lattice",
           "Common rust. Forms when iron reacts with oxygen and moisture.",
           "Pigments, polishing compounds, thermite"),
        _c("CaCO₃", "Calcium Carbonate",       {"Ca":1,"C":1,"O":3}, "ionic",    "Crystal Lattice",
           "Found in limestone, marble, chalk, and seashells. Dissolves in acid.",
           "Construction, antacids, paper, chalk, cement"),
        _c("SiO₂",  "Silicon Dioxide",         {"Si":1,"O":2},       "covalent", "Network Solid",
           "Network covalent solid. Main component of sand, quartz, and glass.",
           "Glass, semiconductors, concrete, quartz crystals"),
        _c("MgO",   "Magnesium Oxide",         {"Mg":1,"O":1},       "ionic",    "FCC Crystal",
           "High melting point (2852°C). Antacid and refractory material.",
           "Antacids, refractory bricks, supplements"),
        _c("NaOH",  "Sodium Hydroxide (Lye)",  {"Na":1,"O":1,"H":1}, "ionic",    "Crystal Lattice",
           "Strong base; highly caustic. Dissolves fats via saponification (soap-making).",
           "Soap/paper production, drain cleaner, food processing"),
        _c("Al₂O₃", "Aluminum Oxide (Alumina)",{"Al":2,"O":3},       "ionic",    "Crystal Lattice",
           "Very hard (Mohs 9). Ruby and sapphire are impure Al₂O₃.",
           "Abrasives, ceramics, gemstones, aluminum smelting"),
        _c("TiO₂",  "Titanium Dioxide",        {"Ti":1,"O":2},       "ionic",    "Crystal",
           "Brilliant white, stable pigment. Photocatalytic under UV light.",
           "White paint, sunscreen, food coloring (E171), solar cells"),
        _c("KCl",   "Potassium Chloride",      {"K":1,"Cl":1},       "ionic",    "FCC Crystal",
           "Ionic salt similar to NaCl. Used in lethal injection protocols.",
           "Fertilizers, salt substitute, IV fluids"),
        _c("H₂O₂",  "Hydrogen Peroxide",       {"H":2,"O":2},        "covalent", "Bent",
           "Unstable oxidizer; decomposes to water and oxygen. Bleaching and antiseptic.",
           "Antiseptic, hair bleaching, rocket propellant (high concentration)"),
        _c("CO",    "Carbon Monoxide",         {"C":1,"O":1},        "covalent", "Linear",
           "Colorless, odorless, toxic. Binds hemoglobin 200× stronger than O₂.",
           "Steelmaking (reducing agent), chemical synthesis"),
        _c("SO₂",   "Sulfur Dioxide",          {"S":1,"O":2},        "covalent", "Bent",
           "Pungent gas from burning fossil fuels. Causes acid rain.",
           "Wine preservative, bleaching, H₂SO₄ precursor"),
        _c("N₂O",   "Nitrous Oxide",           {"N":2,"O":1},        "covalent", "Linear",
           "Laughing gas. ~300× warming potential of CO₂ per molecule.",
           "Anesthesia, whipped cream propellant, racing fuel oxidizer"),
        _c("HNO₃",  "Nitric Acid",             {"H":1,"N":1,"O":3},  "covalent", "Planar",
           "Strong oxidizing acid. Passivates metals with a thin protective oxide layer.",
           "Fertilizers, explosives (TNT), metal etching"),
        _c("ZnO",   "Zinc Oxide",              {"Zn":1,"O":1},       "ionic",    "Hexagonal",
           "White powder. n-type semiconductor and photocatalytic properties.",
           "Sunscreen, rubber, pharmaceuticals, LEDs"),
        _c("CaO",   "Calcium Oxide (Quicklime)",{"Ca":1,"O":1},      "ionic",    "FCC Crystal",
           "Reacts vigorously with water (exothermic) to form Ca(OH)₂ (slaked lime).",
           "Cement, steel production, water treatment"),
        _c("AgCl",  "Silver Chloride",         {"Ag":1,"Cl":1},      "ionic",    "FCC Crystal",
           "White precipitate; light-sensitive. Classic qualitative test for Cl⁻.",
           "Photography, reference electrodes"),
        _c("CuSO₄", "Copper(II) Sulfate",      {"Cu":1,"S":1,"O":4}, "ionic",    "Crystal",
           "Blue crystalline solid when hydrated. Bright blue in aqueous solution.",
           "Fungicide, electroplating, chemistry demonstrations"),
        _c("HF",    "Hydrogen Fluoride",       {"H":1,"F":1},        "covalent", "Linear",
           "Weak acid but extremely dangerous. Penetrates skin and attacks bone.",
           "Glass etching, semiconductor production, Teflon synthesis"),
    ]

    def __init__(self) -> None:
        self._index: Dict[str, Compound] = {}
        for c in self._DB:
            key = self._make_key(c.composition)
            self._index[key] = c

    @staticmethod
    def _make_key(comp: Dict[str, int]) -> str:
        return "|".join(f"{s}:{n}" for s, n in sorted(comp.items()))

    def lookup(self, composition: Dict[str, int]) -> Optional[Compound]:
        return self._index.get(self._make_key(composition))

    def all(self) -> List[Compound]:
        return list(self._DB)

    def by_type(self, bond_type: str) -> List[Compound]:
        return [c for c in self._DB if c.bond_type == bond_type]

    def search(self, text: str) -> List[Compound]:
        t = text.lower()
        return [c for c in self._DB
                if t in c.name.lower() or t in c.formula.lower()]


# ___ BOND PREDICTOR ____________________________
class BondPredictor:
    """
    Predicts chemical bonding and compound formulas using:
      1. Lookup in CompoundLibrary (exact match)
      2. Ionic rule  : metal + nonmetal → cross-multiply oxidation states
      3. Covalent rule : nonmetal + nonmetal → share electrons for octets
      4. Metallic rule : metal + metal → alloy
      5. Noble gas rule : no reaction under normal conditions
    """

    def __init__(self) -> None:
        self.library = CompoundLibrary()

    def analyze(self, pairs: List[Tuple[str, int]]) -> Compound:
        """
        Args:
            pairs: list of (element_symbol, atom_count), e.g. [('H',2),('O',1)]
        Returns:
            Compound — either known (from library) or predicted.
        """
        comp: Dict[str, int] = {}
        elements: List[Element] = []
        for sym, cnt in pairs:
            el = periodic_table.get(sym)
            if el is None:
                raise ValueError(f"Unknown element symbol: '{sym}'")
            comp[sym]  = comp.get(sym, 0) + cnt
            if el not in elements:
                elements.append(el)

        # 1. Library lookup
        known = self.library.lookup(comp)
        if known:
            return known

        # 2. Prediction
        if len(elements) == 2:
            return self._predict_two(elements[0], elements[1])
        if len(elements) >= 3:
            return self._predict_multi(elements, comp)

        return Compound(
            formula="?", name="Unknown", composition=comp,
            bond_type="none", description="Need at least one element."
        )

    def _predict_two(self, a: Element, b: Element) -> Compound:
        # Noble gases: no reaction
        if a.category == "noble" or b.category == "noble":
            return Compound(
                formula="No reaction", name="None",
                composition={a.symbol: 1, b.symbol: 1},
                bond_type="inert",
                description=(
                    "Noble gases have complete valence shells (8 electrons) "
                    "and do not form compounds under normal conditions."
                )
            )

        a_metal = a.category in METAL_CATEGORIES
        b_metal = b.category in METAL_CATEGORIES

        # Metal + Metal → metallic alloy
        if a_metal and b_metal:
            return Compound(
                formula=f"{a.symbol}–{b.symbol} Alloy",
                name="Metallic alloy",
                composition={a.symbol: 1, b.symbol: 1},
                bond_type="metallic",
                description=(
                    f"{a.name} and {b.name} form an alloy via metallic bonding "
                    f"(delocalized 'sea' of electrons). Composition ratio varies; "
                    f"no fixed stoichiometric formula."
                ),
                predicted=True
            )

        # Metal + Nonmetal → ionic
        if a_metal or b_metal:
            M,  NM  = (a, b) if a_metal else (b, a)
            m_ox   = next((o for o in M.oxidation_states  if o >  0), 1)
            nm_ox  = abs(next((o for o in NM.oxidation_states if o < 0), -1))
            g      = _gcd(m_ox, nm_ox)
            m_c, nm_c = nm_ox // g, m_ox // g
            formula = f"{M.symbol}{to_sub(m_c)}{NM.symbol}{to_sub(nm_c)}"
            return Compound(
                formula=formula,
                name=f"Predicted ionic compound",
                composition={M.symbol: m_c, NM.symbol: nm_c},
                bond_type="ionic",
                description=(
                    f"Ionic compound: {M.name} (charge +{m_ox}) transfers "
                    f"electron(s) to {NM.name} (charge −{nm_ox}). "
                    f"Formula balanced by cross-multiplying oxidation states "
                    f"({m_ox} × {nm_c} = {nm_ox} × {m_c}). "
                    f"ΔEN = {round(_en_diff(M, NM), 2) if _en_diff(M, NM) else '?'}."
                ),
                predicted=True
            )

        # Both nonmetals → covalent
        need_a = 1 if a.z == 1 else max(0, 8 - a.valence_electrons)
        need_b = 1 if b.z == 1 else max(0, 8 - b.valence_electrons)

        if need_a == 0 or need_b == 0:
            return Compound(
                formula="Unlikely", name="None",
                composition={a.symbol: 1, b.symbol: 1},
                bond_type="none",
                description=(
                    "One or both elements have complete valence shells. "
                    "Bond formation is unlikely under normal conditions."
                )
            )

        g         = _gcd(need_a, need_b)
        a_c, b_c  = need_b // g, need_a // g
        formula   = f"{a.symbol}{to_sub(a_c)}{b.symbol}{to_sub(b_c)}"
        diff      = _en_diff(a, b)
        pol       = "polar" if diff and diff > 0.4 else "nonpolar"

        return Compound(
            formula=formula,
            name=f"Predicted covalent compound",
            composition={a.symbol: a_c, b.symbol: b_c},
            bond_type="covalent",
            description=(
                f"{pol.capitalize()} covalent compound. "
                f"{a.name} needs {need_a} electron(s), {b.name} needs {need_b}. "
                f"Atoms share electrons to achieve stable octets "
                f"(H uses a duet). ΔEN = {round(diff, 2) if diff else '?'}."
            ),
            predicted=True
        )

    def _predict_multi(self, elements: List[Element], comp: Dict[str, int]) -> Compound:
        """
        Heuristic prediction for 3+ element combinations not found in the
        library. Real polyatomic chemistry (e.g. which atoms bond to which,
        true molecular geometry) requires a table of known polyatomic ions
        that this simulator doesn't model — so this method makes one
        simplifying assumption: classify the whole compound's character by
        the single largest electronegativity gap between any two elements
        present, using the exact atom counts the user supplied.

        This gives a reasonable ionic-vs-covalent classification and a
        correct molar mass, but the formula is taken as-given (not
        re-balanced) and no shape/structure is inferred for 3+ elements —
        this is flagged honestly in the description rather than guessed.
        """
        formula = "".join(
            f"{sym}{to_sub(cnt)}" for sym, cnt in comp.items()
        )

        max_diff   = 0.0
        max_pair   = None
        any_metal  = any(el.category in METAL_CATEGORIES for el in elements)
        for i in range(len(elements)):
            for j in range(i + 1, len(elements)):
                d = _en_diff(elements[i], elements[j])
                if d is not None and d > max_diff:
                    max_diff, max_pair = d, (elements[i], elements[j])

        if max_diff > 1.7 or (any_metal and max_diff > 0):
            bond_type = "ionic"
            char_desc = (
                f"Classified ionic — largest ΔEN is "
                f"{round(max_diff, 2)} between {max_pair[0].symbol} and "
                f"{max_pair[1].symbol}." if max_pair else "Classified ionic (metal present)."
            )
        else:
            bond_type = "covalent"
            char_desc = (
                f"Classified covalent — largest ΔEN is "
                f"{round(max_diff, 2)}." if max_pair else "Classified covalent."
            )

        names = ", ".join(el.name for el in elements)
        return Compound(
            formula=formula,
            name="Predicted multi-element compound",
            composition=comp,
            bond_type=bond_type,
            shape=None,
            description=(
                f"Predicted from {names} using the atom counts you entered. "
                f"{char_desc} NOTE: this simulator does not model polyatomic "
                f"ions, so the formula is taken as-entered rather than "
                f"re-balanced, and no VSEPR shape is inferred for 3+ element "
                f"compounds — check CompoundLibrary first for known examples."
            ),
            predicted=True
        )

    def build_bond(self, sym_a: str, sym_b: str) -> Bond:
        """Construct a Bond object between two elements by symbol."""
        a = periodic_table.get(sym_a)
        b = periodic_table.get(sym_b)
        if a is None or b is None:
            raise ValueError(f"Unknown symbol(s): {sym_a}, {sym_b}")
        result = self.analyze([(sym_a, 1), (sym_b, 1)])
        return Bond(a, b, result.bond_type)


# ___ SINGLETON INSTANCE _________________________________
bond_predictor   = BondPredictor()
compound_library = CompoundLibrary()
