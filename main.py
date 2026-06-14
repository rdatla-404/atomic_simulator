"""
main.py — Python CLI entry point for the Atomic Simulator

  PeriodicTable  →  elements.py
  Atom           →  atom.py
  RadioactiveDecay / MonteCarloDecay  --  atom.py
  BondPredictor / CompoundLibrary     --  compound.py

Usage:
    python main.py                # interactive menu
    python main.py demo           # quick non-interactive demo
"""

import sys
from elements  import periodic_table
from atom      import Atom, RadioactiveDecay, MonteCarloDecay, Nucleus
from compound  import bond_predictor, compound_library


# ─── COLOUR CODES (ANSI) ────────────────────────────────────
class C:
    RESET  = "\033[0m"
    BOLD   = "\033[1m"
    CYAN   = "\033[96m"
    BLUE   = "\033[94m"
    GREEN  = "\033[92m"
    YELLOW = "\033[93m"
    RED    = "\033[91m"
    GREY   = "\033[90m"
    WHITE  = "\033[97m"

    @staticmethod
    def header(text: str) -> str:
        return f"\n{C.BOLD}{C.CYAN}{text}{C.RESET}"

    @staticmethod
    def label(text: str) -> str:
        return f"{C.GREY}{text}{C.RESET}"

    @staticmethod
    def value(text: str) -> str:
        return f"{C.WHITE}{text}{C.RESET}"

    @staticmethod
    def ok(text: str) -> str:
        return f"{C.GREEN}{text}{C.RESET}"

    @staticmethod
    def warn(text: str) -> str:
        return f"{C.YELLOW}{text}{C.RESET}"

    @staticmethod
    def err(text: str) -> str:
        return f"{C.RED}{text}{C.RESET}"


# ─── HELPERS ────────────────────────────────────────────────
def _separator(char: str = "─", width: int = 52) -> None:
    print(f"{C.GREY}{char * width}{C.RESET}")


def _prompt(msg: str) -> str:
    return input(f"\n{C.CYAN}▶ {msg}{C.RESET} ").strip()


# ─── MENU ACTIONS ───────────────────────────────────────────
def action_atom() -> None:
    """Display full atom info for a chosen element."""
    query = _prompt("Enter element symbol, name, or atomic number:")
    el    = periodic_table.get(query)
    if el is None:
        print(C.err(f"  Element '{query}' not found."))
        return
    atom = Atom(el)
    print(atom)


def action_compare() -> None:
    """Side-by-side comparison of two elements."""
    q1 = _prompt("First element symbol/name/Z:")
    q2 = _prompt("Second element symbol/name/Z:")
    e1, e2 = periodic_table.get(q1), periodic_table.get(q2)
    if not e1 or not e2:
        print(C.err("  One or both elements not found."))
        return
    print(C.header(f"  {e1.symbol} vs {e2.symbol}  —  Comparison"))
    rows = [
        ("Atomic Number",     e1.z,                   e2.z),
        ("Atomic Mass",       f"{e1.mass} amu",        f"{e2.mass} amu"),
        ("Period",            e1.period,               e2.period),
        ("Group",             e1.group or "—",          e2.group or "—"),
        ("Category",          e1.category_label,       e2.category_label),
        ("Valence e⁻",        e1.valence_electrons,    e2.valence_electrons),
        ("Electronegativity", e1.electronegativity or "—", e2.electronegativity or "—"),
        ("Melting Point",     f"{e1.melting_point}°C" if e1.melting_point else "—",
                              f"{e2.melting_point}°C" if e2.melting_point else "—"),
        ("Radioactive",       "Yes" if e1.is_radioactive else "No",
                              "Yes" if e2.is_radioactive else "No"),
    ]
    _separator()
    print(f"  {'Property':<22} {e1.symbol:<18} {e2.symbol}")
    _separator()
    for prop, v1, v2 in rows:
        print(f"  {C.GREY}{prop:<22}{C.RESET} {C.WHITE}{str(v1):<18}{C.RESET} {C.WHITE}{v2}{C.RESET}")
    _separator()


def action_compound() -> None:
    """Build a compound from element symbols and counts."""
    print(C.header("  Compound Builder"))
    print(f"  {C.GREY}Enter elements as 'H 2 O 1' (symbol count pairs):{C.RESET}")
    raw = _prompt("Elements:")
    tokens = raw.split()
    if len(tokens) < 2 or len(tokens) % 2 != 0:
        print(C.err("  Please enter pairs: symbol count  (e.g. Na 1 Cl 1)"))
        return
    pairs = []
    try:
        for i in range(0, len(tokens), 2):
            pairs.append((tokens[i], int(tokens[i + 1])))
    except ValueError:
        print(C.err("  Counts must be integers."))
        return
    compound = bond_predictor.analyze(pairs)
    print(compound)


def action_library() -> None:
    """Browse known compounds in the library."""
    print(C.header("  Compound Library  (25 known compounds)"))
    _separator()
    for c in compound_library.all():
        tag = C.ok("ionic") if c.bond_type == "ionic" else (
              C.BLUE + "covalent" + C.RESET if c.bond_type == "covalent" else
              C.warn(c.bond_type))
        print(f"  {C.WHITE}{c.formula:<12}{C.RESET}  {c.name:<35}  {tag}")
    _separator()


def action_decay() -> None:
    """Simulate radioactive decay for a chosen element."""
    query = _prompt("Enter element symbol/name/Z to decay:")
    el    = periodic_table.get(query)
    if el is None:
        print(C.err(f"  Element '{query}' not found.")); return
    if not el.is_radioactive:
        print(C.warn(f"  {el.name} is stable; no spontaneous decay expected."))
        return
    atom = Atom(el)
    rd   = RadioactiveDecay(atom.nucleus)
    print(C.header(f"  {el.name}  —  Auto Decay"))
    daughter, desc = rd.auto_decay()
    print(f"\n{desc}")
    if daughter:
        daughter_el = periodic_table.get(daughter.protons)
        name = daughter_el.name if daughter_el else "Unknown"
        print(f"\n  Daughter element: {name} (Z={daughter.protons})")
        print(f"  Daughter stable : {daughter.is_stable}")
    mode_q = _prompt("Run Monte Carlo decay simulation? (y/n):")
    if mode_q.lower() == "y":
        steps_s = _prompt("Number of time steps [default 30]:")
        steps   = int(steps_s) if steps_s.isdigit() else 30
        prob_s  = _prompt("Decay probability per step 0–1 [default 0.2]:")
        try: prob = float(prob_s)
        except ValueError: prob = 0.2
        mc     = MonteCarloDecay(atom.nucleus, prob)
        events = mc.simulate(steps)
        print(C.header(f"  Monte Carlo — {steps} steps, prob={prob}"))
        if not events:
            print(f"  {C.GREY}No decay events occurred.{C.RESET}")
        for step, desc in events:
            print(f"  {C.CYAN}Step {step:3}{C.RESET}: {desc}")


def action_table() -> None:
    """Print an ASCII periodic table."""
    periodic_table.print_ascii_table()


def action_search() -> None:
    """Search elements by partial name or symbol."""
    query   = _prompt("Search term (name or symbol):")
    results = periodic_table.search(query)
    if not results:
        print(C.err(f"  No elements found matching '{query}'."))
        return
    print(C.header(f"  Search results for '{query}'  ({len(results)} found)"))
    _separator()
    for el in results:
        print(f"  {C.WHITE}{el.z:>4}{C.RESET}  "
              f"{C.BOLD}{el.symbol:<4}{C.RESET}  "
              f"{el.name:<20}  "
              f"{C.GREY}{el.category_label}{C.RESET}")
    _separator()


def action_bond() -> None:
    """Analyze a bond between two elements."""
    sym_a = _prompt("First element symbol:").strip()
    sym_b = _prompt("Second element symbol:").strip()
    try:
        bond = bond_predictor.build_bond(sym_a, sym_b)
        print(C.header(f"  Bond Analysis: {sym_a} — {sym_b}"))
        print(bond)
    except ValueError as e:
        print(C.err(f"  {e}"))


# ─── QUICK DEMO ─────────────────────────────────────────────
def run_demo() -> None:
    print(C.header("  ATOMIC SIMULATOR — QUICK DEMO"))
    print(f"  {C.GREY}Python OOP demonstration of all modules{C.RESET}")
    _separator("═")

    # 1. Atom info
    print(C.header("  1. Atom: Carbon (C)"))
    carbon = Atom(periodic_table.get("C"))
    print(carbon)

    # 2. Compound lookup
    print(C.header("  2. Compound: Water (H₂O)"))
    water = bond_predictor.analyze([("H", 2), ("O", 1)])
    print(water)

    # 3. Bond prediction
    print(C.header("  3. Predict: Na + Cl"))
    nacl = bond_predictor.analyze([("Na", 1), ("Cl", 1)])
    print(nacl)

    # 4. Covalent prediction
    print(C.header("  4. Predict: N + H (unknown to library)"))
    nh = bond_predictor.analyze([("C", 1), ("S", 2)])
    print(nh)

    # 5. Decay
    print(C.header("  5. Radioactive Decay: Uranium-238"))
    u_atom = Atom(periodic_table.get("U"))
    rd     = RadioactiveDecay(u_atom.nucleus)
    d, msg = rd.alpha()
    print(f"\n{msg}")
    if d:
        el = periodic_table.get(d.protons)
        print(f"  Daughter: {el.name} (Z={el.z})" if el else f"  Daughter: Z={d.protons}")

    # 6. Periodic table
    print(C.header("  6. Periodic Table (ASCII)"))
    periodic_table.print_ascii_table()

    print(C.ok("\n  Demo complete. Run 'python main.py' for the interactive menu.\n"))


# ─── INTERACTIVE MENU ───────────────────────────────────────
MENU = [
    ("atom",     "Show full atom info",              action_atom),
    ("compare",  "Compare two elements side-by-side",action_compare),
    ("compound", "Build and analyze a compound",     action_compound),
    ("library",  "Browse known compounds",           action_library),
    ("bond",     "Analyze bond between two elements",action_bond),
    ("decay",    "Simulate radioactive decay",       action_decay),
    ("table",    "Print ASCII periodic table",       action_table),
    ("search",   "Search elements by name/symbol",   action_search),
    ("quit",     "Exit",                             None),
]


def run_menu() -> None:
    print(f"\n{C.BOLD}{C.CYAN}  ⚛  ATOMIC SIMULATOR  —  Python CLI{C.RESET}")
    print(f"  {C.GREY}All 118 elements · OOP chemistry engine · Type 'demo' to skip menu{C.RESET}")
    while True:
        print(C.header("  MENU"))
        for key, desc, _ in MENU:
            print(f"    {C.CYAN}{key:<10}{C.RESET}  {C.GREY}{desc}{C.RESET}")
        choice = _prompt("Command:").lower()
        if choice == "demo":
            run_demo(); continue
        matched = [fn for key, _, fn in MENU if key == choice]
        if not matched:
            print(C.err(f"  Unknown command '{choice}'."))
            continue
        fn = matched[0]
        if fn is None:
            print(C.ok("\n  Goodbye.\n")); break
        try:
            fn()
        except KeyboardInterrupt:
            print()
        except Exception as exc:
            print(C.err(f"  Error: {exc}"))


# ─── ENTRY POINT ────────────────────────────────────────────
if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "demo":
        run_demo()
    else:
        run_menu()
