"""

Classes:
  Nucleus              -- Protons + neutrons; stability check
  ElectronConfiguration -- Full Aufbau fill with orbital-level detail
  Ion                  -- Charged atom (gains/loses electrons)
  Atom                 -- Composite model: nucleus + electron config + element
  RadioactiveDecay     -- Alpha, beta-minus, beta-plus, gamma decay
"""


from __future__ import annotations 
import math
import random
from dataclasses import dataclass
from typing import Optional, List, Tuple, Dict
from elements import Element, PeriodicTable, periodic_table, to_sub, to_sup


# ─── NUCLEUS ────────────────────────────────────────────────
class Nucleus:
    """
    Represents the atomic nucleus with proton and neutron counts.
    Stability assessment uses the N/Z ratio heuristic.
    """

    def __init__(self, protons: int, neutrons: int) -> None:
        if protons < 1:
            raise ValueError("Proton count must be at least 1.")
        if neutrons < 0:
            raise ValueError("Neutron count cannot be negative.")
        self.protons  = protons
        self.neutrons = neutrons

    @property
    def mass_number(self) -> int:
        return self.protons + self.neutrons

    @property
    def nz_ratio(self) -> float:
        return self.neutrons / self.protons

    @property
    def is_stable(self) -> bool:
        """
        Approximate stability via N/Z ratio bands.
        Not a substitute for a full nuclear database.
        """
        z, n = self.protons, self.neutrons
        if z > 83:          return False          # all transbismuth nuclei unstable
        if z in (43, 61):   return False          # Tc and Pm — no stable isotopes
        r = n / z
        if z <= 20:         return 0.90 <= r <= 1.25
        if z <= 40:         return 1.00 <= r <= 1.45
        if z <= 60:         return 1.20 <= r <= 1.55
        return              1.30 <= r <= 1.65

    @property
    def binding_energy_per_nucleon_mev(self) -> float:
        """
        Semi-empirical mass formula (Weizsäcker / Bethe-Weizsäcker).
        Returns binding energy per nucleon in MeV.
        """
        A, Z, N = self.mass_number, self.protons, self.neutrons
        if A < 2: return 0.0
        # SEMF coefficients (MeV)
        av, as_, ac, aa = 15.75, 17.80, 0.711, 23.70
        # Pairing term
        if A % 2 == 1:          delta = 0.0
        elif Z % 2 == 0:        delta =  11.18 / math.sqrt(A)   # e-e
        else:                   delta = -11.18 / math.sqrt(A)   # o-o
        BE = (av * A
              - as_ * A**(2/3)
              - ac * Z*(Z-1) / A**(1/3)
              - aa * (N-Z)**2 / A
              + delta)
        return round(max(BE / A, 0), 4)

    def __repr__(self) -> str:
        return (f"Nucleus(Z={self.protons}, N={self.neutrons}, "
                f"A={self.mass_number}, stable={self.is_stable})")

    def __str__(self) -> str:
        return (f"  Protons  : {self.protons}\n"
                f"  Neutrons : {self.neutrons}\n"
                f"  A (mass#): {self.mass_number}\n"
                f"  N/Z ratio: {self.nz_ratio:.3f}\n"
                f"  Stable   : {self.is_stable}\n"
                f"  BE/A     : {self.binding_energy_per_nucleon_mev} MeV")


# ─── ELECTRON CONFIGURATION ─────────────────────────────────
# Aufbau filling order: subshell → max electrons
_AUFBAU: List[Tuple[str, int]] = [
    ("1s",2),("2s",2),("2p",6),("3s",2),("3p",6),
    ("4s",2),("3d",10),("4p",6),("5s",2),("4d",10),("5p",6),
    ("6s",2),("4f",14),("5d",10),("6p",6),
    ("7s",2),("5f",14),("6d",10),("7p",6),
]


class ElectronConfiguration:
    """
    Fills electrons into orbitals following Aufbau order.
    Note: does not model Hund's rule within degenerate orbitals or exceptions
    like Cr/Cu — those can be added as a lookup override dict.
    """

    # Known Aufbau exceptions (symbol → filled config as dict)
    _EXCEPTIONS: Dict[str, Dict[str, int]] = {
        "Cr": {"1s":2,"2s":2,"2p":6,"3s":2,"3p":6,"3d":5,"4s":1},
        "Cu": {"1s":2,"2s":2,"2p":6,"3s":2,"3p":6,"3d":10,"4s":1},
        "Mo": {"1s":2,"2s":2,"2p":6,"3s":2,"3p":6,"3d":10,"4s":2,"4p":6,"4d":5,"5s":1},
        "Ag": {"1s":2,"2s":2,"2p":6,"3s":2,"3p":6,"3d":10,"4s":2,"4p":6,"4d":10,"5s":1},
        "Au": {"1s":2,"2s":2,"2p":6,"3s":2,"3p":6,"3d":10,"4s":2,"4p":6,"4d":10,"4f":14,"5s":2,"5p":6,"5d":10,"6s":1},
        "Pd": {"1s":2,"2s":2,"2p":6,"3s":2,"3p":6,"3d":10,"4s":2,"4p":6,"4d":10},
    }

    def __init__(self, n_electrons: int, symbol: str = "") -> None:
        self.n_electrons = n_electrons
        if symbol in self._EXCEPTIONS:
            self.config: Dict[str, int] = dict(self._EXCEPTIONS[symbol])
        else:
            self.config = self._fill(n_electrons)

    @staticmethod
    def _fill(n: int) -> Dict[str, int]:
        config, rem = {}, n
        for orb, cap in _AUFBAU:
            if rem <= 0: break
            fill = min(rem, cap)
            config[orb] = fill
            rem -= fill
        return config

    @property
    def notation(self) -> str:
        """e.g. '1s² 2s² 2p⁶ 3s² 3p⁶ 4s² 3d¹⁰ 4p⁶'"""
        return " ".join(f"{orb}{to_sup(n)}" for orb, n in self.config.items())

    @property
    def valence_electrons(self) -> int:
        """Electrons in the highest principal quantum number shell."""
        shell_totals: Dict[int, int] = {}
        for orb, n in self.config.items():
            principal = int(orb[0])
            shell_totals[principal] = shell_totals.get(principal, 0) + n
        return shell_totals[max(shell_totals)] if shell_totals else 0

    @property
    def outermost_shell(self) -> int:
        return max(int(orb[0]) for orb in self.config) if self.config else 0

    def __repr__(self) -> str:
        return f"ElectronConfiguration(n={self.n_electrons}, config='{self.notation}')"


# ─── ION ────────────────────────────────────────────────────
@dataclass
class Ion:
    """An atom that has gained or lost electrons (charged species)."""
    element:  Element
    charge:   int    # positive = cation (lost e-), negative = anion (gained e-)

    @property
    def n_electrons(self) -> int:
        return self.element.z - self.charge

    @property
    def electron_config(self) -> ElectronConfiguration:
        return ElectronConfiguration(max(0, self.n_electrons), self.element.symbol)

    @property
    def notation(self) -> str:
        sign = "⁺" if self.charge > 0 else "⁻"
        mag  = abs(self.charge)
        return f"{self.element.symbol}{to_sup(mag) if mag > 1 else ''}{sign}"

    def __str__(self) -> str:
        return (f"Ion: {self.notation}\n"
                f"  Element   : {self.element.name}\n"
                f"  Charge    : {'+' if self.charge > 0 else ''}{self.charge}\n"
                f"  Electrons : {self.n_electrons}\n"
                f"  Config    : {self.electron_config.notation}")


# ─── ATOM ───────────────────────────────────────────────────
class Atom:
    """
    Composite model of an atom: element identity + nucleus + electron structure.

    Usage:
        from elements import periodic_table
        from atom import Atom
        carbon = Atom(periodic_table.get('C'))
        print(carbon)
    """

    def __init__(self, element: Element, neutrons: Optional[int] = None) -> None:
        self.element = element
        # Default neutron count ≈ round(mass) − Z
        default_n = round(element.mass) - element.z
        n = neutrons if neutrons is not None else max(0, default_n)
        self.nucleus            = Nucleus(element.z, n)
        self.electron_config    = ElectronConfiguration(element.z, element.symbol)

    # ── Properties ─────────────────────────────────────────
    @property
    def is_isotope(self) -> bool:
        default_n = round(self.element.mass) - self.element.z
        return self.nucleus.neutrons != default_n

    @property
    def isotope_symbol(self) -> str:
        return f"{''.join(to_sup(int(d)) for d in str(self.nucleus.mass_number))}{self.element.symbol}"

    def ionize(self, charge: int) -> Ion:
        """Remove (charge > 0) or add (charge < 0) electrons to produce an ion."""
        return Ion(self.element, charge)

    # ── Display ────────────────────────────────────────────
    def __repr__(self) -> str:
        return f"Atom({self.element.symbol}, Z={self.element.z}, A={self.nucleus.mass_number})"

    def __str__(self) -> str:
        return (
            f"\n{'═'*44}\n"
            f"  {self.element.symbol}  {self.element.name}  (Z = {self.element.z})\n"
            f"{'═'*44}\n"
            f"  Isotope symbol : {self.isotope_symbol}\n"
            f"  Atomic mass    : {self.element.mass} amu\n"
            f"\n  — NUCLEUS —\n{self.nucleus}\n"
            f"\n  — ELECTRONS —\n"
            f"  Total          : {self.element.z}\n"
            f"  Configuration  : {self.electron_config.notation}\n"
            f"  Shell notation : {self.element.shell_notation}\n"
            f"  Valence e⁻     : {self.electron_config.valence_electrons}\n"
            f"\n  — PROPERTIES —\n"
            f"  Category       : {self.element.category_label}\n"
            f"  Electronegativity: {self.element.electronegativity or '—'} (Pauling)\n"
            f"  Oxidation states : {self.element.oxidation_string}\n"
            f"  Melting point  : {self.element.melting_point}°C\n"
            f"  Boiling point  : {self.element.boiling_point}°C\n"
            f"  Radioactive    : {'Yes' if self.element.is_radioactive else 'No'}\n"
            f"\n  {self.element.description}\n"
        )


# ─── RADIOACTIVE DECAY ──────────────────────────────────────
class RadioactiveDecay:
    """
    Models classical decay modes for a given nucleus.
    Returns a new Nucleus (daughter) and a description string.

    Extensibility:
      – Add half-life data per isotope
      – Add decay chain traversal
      – Integrate with Monte Carlo simulation for decay probability
    """

    def __init__(self, nucleus: Nucleus) -> None:
        self.nucleus = nucleus

    # ── Decay modes ────────────────────────────────────────
    def alpha(self) -> Tuple[Optional[Nucleus], str]:
        """α decay: emit ⁴He → Z−2, N−2."""
        if self.nucleus.protons < 2 or self.nucleus.neutrons < 2:
            return None, "α decay impossible: too few nucleons."
        d = Nucleus(self.nucleus.protons - 2, self.nucleus.neutrons - 2)
        return d, (f"α decay: ⁴He emitted\n"
                   f"  Parent : Z={self.nucleus.protons}, A={self.nucleus.mass_number}\n"
                   f"  Daughter: Z={d.protons}, A={d.mass_number}")

    def beta_minus(self) -> Tuple[Nucleus, str]:
        """β⁻ decay: n → p + e⁻ + ν̄ → Z+1, N−1."""
        d = Nucleus(self.nucleus.protons + 1, self.nucleus.neutrons - 1)
        return d, (f"β⁻ decay: neutron → proton + e⁻ + antineutrino\n"
                   f"  Parent  : Z={self.nucleus.protons}, A={self.nucleus.mass_number}\n"
                   f"  Daughter: Z={d.protons}, A={d.mass_number}")

    def beta_plus(self) -> Tuple[Optional[Nucleus], str]:
        """β⁺ decay: p → n + e⁺ + ν → Z−1, N+1."""
        if self.nucleus.protons < 2:
            return None, "β⁺ decay impossible: proton count too low."
        d = Nucleus(self.nucleus.protons - 1, self.nucleus.neutrons + 1)
        return d, (f"β⁺ decay: proton → neutron + positron + neutrino\n"
                   f"  Parent  : Z={self.nucleus.protons}, A={self.nucleus.mass_number}\n"
                   f"  Daughter: Z={d.protons}, A={d.mass_number}")

    def gamma(self) -> Tuple[Nucleus, str]:
        """γ decay: excited nucleus releases photon; Z and A unchanged."""
        return self.nucleus, "γ decay: high-energy photon emitted. Z and A unchanged."

    def auto_decay(self) -> Tuple[Optional[Nucleus], str]:
        """
        Pick the most likely decay mode based on simple N/Z heuristics.
        Heavy (Z > 83) → alpha preferred.
        Neutron-rich → beta-minus.
        Proton-rich  → beta-plus.
        """
        z, n = self.nucleus.protons, self.nucleus.neutrons
        if z > 83:
            return self.alpha()
        ratio = n / z
        if ratio > 1.5:
            return self.beta_minus()
        if ratio < 0.9:
            return self.beta_plus()
        return self.gamma()


# ─── MONTE CARLO DECAY SIMULATION ───────────────────────────
class MonteCarloDecay:
    """
    Simulate radioactive decay over time steps using random sampling.
    Extensibility: hook in real half-lives per isotope.
    """

    def __init__(self, nucleus: Nucleus, decay_prob_per_step: float = 0.1) -> None:
        self.nucleus        = nucleus
        self.decay_prob     = decay_prob_per_step
        self.history:       List[str] = []

    def simulate(self, steps: int = 20) -> List[Tuple[int, str]]:
        """
        Run `steps` time steps. Each step has `decay_prob` chance of decaying.
        Returns list of (step, event_description).
        """
        events, current = [], self.nucleus
        rd = RadioactiveDecay(current)
        for step in range(1, steps + 1):
            if random.random() < self.decay_prob:
                daughter, desc = rd.auto_decay()
                if daughter:
                    events.append((step, desc))
                    current = daughter
                    rd = RadioactiveDecay(current)
                    # Stop if stable
                    if current.is_stable:
                        events.append((step, f"→ Stable nucleus reached: Z={current.protons}, A={current.mass_number}"))
                        break
                else:
                    events.append((step, desc + " (decay blocked)"))
        return events
