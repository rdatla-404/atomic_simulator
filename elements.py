from dataclasses import dataclass, field
from typing import Optional, List, Tuple


# ____ CATEGORY METADATA ____________________
CATEGORY_LABELS = {
    "alkali":      "Alkali Metal",
    "alkaline":    "Alkaline Earth Metal",
    "transition":  "Transition Metal",
    "post-trans":  "Post-Transition Metal",
    "metalloid":   "Metalloid",
    "nonmetal":    "Reactive Nonmetal",
    "halogen":     "Halogen",
    "noble":       "Noble Gas",
    "lanthanide":  "Lanthanide",
    "actinide":    "Actinide",
}

METAL_CATEGORIES = {"alkali", "alkaline", "transition", "post-trans"}
SUBSCRIPTS       = str.maketrans("0123456789", "₀₁₂₃₄₅₆₇₈₉")
SUPERSCRIPTS     = str.maketrans("0123456789", "⁰¹²³⁴⁵⁶⁷⁸⁹")


def to_sub(n: int) -> str:
    return "" if n == 1 else str(n).translate(SUBSCRIPTS)


def to_sup(n: int) -> str:
    return str(n).translate(SUPERSCRIPTS)

# ___ ELEMENT DATACLASS _________________________________________
@dataclass
class Element:
    """Immutable data model for a chemical element."""
    z:                  int                  # atomic number
    symbol:             str
    name:               str
    mass:               float                # standard atomic weight (amu)
    period:             int
    group:              Optional[int]        # None for lanthanides/actinides
    valence_electrons:  int
    category:           str
    electronegativity:  Optional[float]      # Pauling scale; None = unknown
    shells:             Tuple[int, ...]      # electrons per shell (K, L, M, ...)
    oxidation_states:   Tuple[int, ...]      # common states; empty for superheavy
    melting_point:      Optional[float]      # °C; None = unknown
    boiling_point:      Optional[float]      # °C; None = unknown
    description:        str

    # __ Derived properties _______________________
    @property
    def is_metal(self) -> bool:
        return self.category in METAL_CATEGORIES

    @property
    def is_radioactive(self) -> bool:
        return self.z > 83 or self.z in (43, 61, 85, 87)

    @property
    def is_synthetic(self) -> bool:
        return self.z > 94 or self.z in (43, 61)

    @property
    def category_label(self) -> str:
        return CATEGORY_LABELS.get(self.category, self.category)

    @property
    def shell_notation(self) -> str:
        """e.g. 'K:2  L:8  M:18'"""
        names = list("KLMNOPQ")
        return "  ".join(f"{names[i]}:{n}" for i, n in enumerate(self.shells))

    @property
    def spectroscopic_config(self) -> str:
        """Aufbau-order spectroscopic notation (approximate for exceptions)."""
        order = [("1s",2),("2s",2),("2p",6),("3s",2),("3p",6),
                 ("4s",2),("3d",10),("4p",6),("5s",2),("4d",10),("5p",6),
                 ("6s",2),("4f",14),("5d",10),("6p",6),
                 ("7s",2),("5f",14),("6d",10),("7p",6)]
        rem, parts = self.z, []
        for name, cap in order:
            if rem <= 0: break
            fill = min(rem, cap); rem -= fill
            parts.append(f"{name}{to_sup(fill)}")
        return " ".join(parts)

    @property
    def oxidation_string(self) -> str:
        if not self.oxidation_states: return "—"
        return ", ".join(f"+{o}" if o > 0 else str(o) for o in self.oxidation_states)

    def __str__(self) -> str:
        lines = [
            f"{'─'*42}",
            f" {self.symbol:>3}  {self.name}  (Z = {self.z})",
            f"{'─'*42}",
            f"  Atomic mass    : {self.mass} amu",
            f"  Period / Group : {self.period} / {self.group or '—'}",
            f"  Category       : {self.category_label}",
            f"  Shells         : {self.shell_notation}",
            f"  Config         : {self.spectroscopic_config}",
            f"  Valence e⁻     : {self.valence_electrons}",
            f"  Oxidation      : {self.oxidation_string}",
            f"  Electronegativity: {self.electronegativity or '—'}",
            f"  Melting point  : {self.melting_point}°C" if self.melting_point is not None else "  Melting point  : —",
            f"  Boiling point  : {self.boiling_point}°C" if self.boiling_point is not None else "  Boiling point  : —",
            f"  Radioactive    : {'Yes' if self.is_radioactive else 'No'}",
            f"",
            f"  {self.description}",
        ]
        return "\n".join(lines)


# ___ HELPER TO CONSTRUCT ELEMENTS _________________
def _e(z, s, n, m, p, g, v, cat, en, sh, ox, mp, bp, desc) -> Element:
    return Element(z=z, symbol=s, name=n, mass=m, period=p, group=g,
                   valence_electrons=v, category=cat,
                   electronegativity=en if en != -1 else None,
                   shells=tuple(sh), oxidation_states=tuple(ox) if ox else (),
                   melting_point=mp, boiling_point=bp, description=desc)


# ___ ALL 118 ELEMENTS _____________________________________
ALL_ELEMENTS: List[Element] = [
    # Period 1
    _e(1,"H","Hydrogen",1.008,1,1,1,"nonmetal",2.20,[1],[-1,1],-259,-253,"Most abundant element; powers stars via nuclear fusion. Essential for water and organic molecules."),
    _e(2,"He","Helium",4.003,1,18,0,"noble",-1,[2],[0],-272,-269,"Completely inert noble gas. Used in MRI machines, balloons, and as cryogenic coolant."),
    # Period 2
    _e(3,"Li","Lithium",6.941,2,1,1,"alkali",0.98,[2,1],[1],181,1342,"Lightest solid element. Powers rechargeable batteries and used in psychiatric medication."),
    _e(4,"Be","Beryllium",9.012,2,2,2,"alkaline",1.57,[2,2],[2],1287,2468,"Extremely stiff and light metal. Toxic if inhaled; used in aerospace and X-ray windows."),
    _e(5,"B","Boron",10.811,2,13,3,"metalloid",2.04,[2,3],[3],2076,3927,"Essential micronutrient for plants. Used in Pyrex glass and nuclear reactor control rods."),
    _e(6,"C","Carbon",12.011,2,14,4,"nonmetal",2.55,[2,4],[-4,-3,-2,-1,0,1,2,3,4],3550,4027,"Basis of all known life. Forms diamond, graphite, and over 10 million organic compounds."),
    _e(7,"N","Nitrogen",14.007,2,15,5,"nonmetal",3.04,[2,5],[-3,-2,-1,1,2,3,4,5],-210,-196,"78% of Earth's atmosphere. Essential for amino acids, proteins, and DNA."),
    _e(8,"O","Oxygen",15.999,2,16,6,"nonmetal",3.44,[2,6],[-2,-1],-218,-183,"Essential for aerobic life. Most abundant element in Earth's crust. Highly electronegative."),
    _e(9,"F","Fluorine",18.998,2,17,7,"halogen",3.98,[2,7],[-1],-220,-188,"Most electronegative element. Used in Teflon, refrigerants, and toothpaste."),
    _e(10,"Ne","Neon",20.180,2,18,0,"noble",-1,[2,8],[0],-249,-246,"Completely inert. Produces characteristic red-orange glow in neon signs."),
    # Period 3
    _e(11,"Na","Sodium",22.990,3,1,1,"alkali",0.93,[2,8,1],[1],98,883,"Essential electrolyte for nerve impulses. Reacts violently with water."),
    _e(12,"Mg","Magnesium",24.305,3,2,2,"alkaline",1.31,[2,8,2],[2],650,1090,"Central atom in chlorophyll for photosynthesis. Essential for 300+ enzymes."),
    _e(13,"Al","Aluminum",26.982,3,13,3,"post-trans",1.61,[2,8,3],[3],660,2519,"Most abundant metal in Earth's crust. Lightweight and corrosion-resistant."),
    _e(14,"Si","Silicon",28.086,3,14,4,"metalloid",1.90,[2,8,4],[-4,4],1414,3265,"Basis of computer chips and solar cells. Named Silicon Valley after this element."),
    _e(15,"P","Phosphorus",30.974,3,15,5,"nonmetal",2.19,[2,8,5],[-3,3,5],44,281,"Essential for DNA, RNA, and ATP (cellular energy). Used in fertilizers."),
    _e(16,"S","Sulfur",32.065,3,16,6,"nonmetal",2.58,[2,8,6],[-2,2,4,6],115,445,"Found in amino acids. Used in gunpowder and vulcanizing rubber."),
    _e(17,"Cl","Chlorine",35.453,3,17,7,"halogen",3.16,[2,8,7],[-1,1,3,5,7],-102,-34,"Disinfects water. Forms table salt (NaCl) with sodium."),
    _e(18,"Ar","Argon",39.948,3,18,0,"noble",-1,[2,8,8],[0],-189,-186,"Third most abundant atmospheric gas (0.93%). Used in light bulbs and welding."),
    # Period 4
    _e(19,"K","Potassium",39.098,4,1,1,"alkali",0.82,[2,8,8,1],[1],64,759,"Essential for nerve function and muscle contraction. Symbol K from Latin Kalium."),
    _e(20,"Ca","Calcium",40.078,4,2,2,"alkaline",1.00,[2,8,8,2],[2],842,1484,"Most abundant mineral in the human body. Builds bones and teeth."),
    _e(21,"Sc","Scandium",44.956,4,3,3,"transition",1.36,[2,8,9,2],[3],1541,2830,"Rare lightweight metal used in aerospace aluminum alloys."),
    _e(22,"Ti","Titanium",47.867,4,4,4,"transition",1.54,[2,8,10,2],[2,3,4],1668,3287,"Strong, lightweight, corrosion-resistant. Used in aircraft and medical implants."),
    _e(23,"V","Vanadium",50.942,4,5,5,"transition",1.63,[2,8,11,2],[2,3,4,5],1910,3407,"Used in high-strength steel alloys. Promising for grid-scale energy storage."),
    _e(24,"Cr","Chromium",51.996,4,6,6,"transition",1.66,[2,8,13,1],[2,3,6],1907,2671,"Makes steel stainless. Hexavalent chromium is highly toxic."),
    _e(25,"Mn","Manganese",54.938,4,7,7,"transition",1.55,[2,8,13,2],[2,3,4,6,7],1246,2061,"Essential trace element. Widest range of stable oxidation states."),
    _e(26,"Fe","Iron",55.845,4,8,8,"transition",1.83,[2,8,14,2],[2,3],1538,2861,"Most commonly used metal. Earth's core is mostly iron. Essential for hemoglobin."),
    _e(27,"Co","Cobalt",58.933,4,9,9,"transition",1.88,[2,8,15,2],[2,3],1495,2927,"Cobalt blue pigment. Used in Li-ion batteries. Vitamin B12 contains cobalt."),
    _e(28,"Ni","Nickel",58.693,4,10,10,"transition",1.91,[2,8,16,2],[2,3],1455,2913,"Used in stainless steel and coins. Magnetic at room temperature."),
    _e(29,"Cu","Copper",63.546,4,11,11,"transition",1.90,[2,8,18,1],[1,2],1085,2562,"Best non-precious electrical conductor. First metal used by humans (~10,000 BCE)."),
    _e(30,"Zn","Zinc",65.38,4,12,12,"transition",1.65,[2,8,18,2],[2],420,907,"Used in galvanizing steel and batteries. 4th most industrially used metal."),
    _e(31,"Ga","Gallium",69.723,4,13,3,"post-trans",1.81,[2,8,18,3],[3],30,2204,"Melts at body temperature (29.8°C). Used in semiconductors (GaAs) and LEDs."),
    _e(32,"Ge","Germanium",72.64,4,14,4,"metalloid",2.01,[2,8,18,4],[-4,2,4],938,2833,"Semiconductor in early transistors. Mendeleev predicted its existence."),
    _e(33,"As","Arsenic",74.922,4,15,5,"metalloid",2.18,[2,8,18,5],[-3,3,5],817,614,"Toxic metalloid used historically as a poison; now used in semiconductors."),
    _e(34,"Se","Selenium",78.96,4,16,6,"nonmetal",2.55,[2,8,18,6],[-2,2,4,6],221,685,"Essential trace element for thyroid function. Used in solar cells."),
    _e(35,"Br","Bromine",79.904,4,17,7,"halogen",2.96,[2,8,18,7],[-1,1,3,5],-7,59,"One of two liquid elements at room temperature. Used in flame retardants."),
    _e(36,"Kr","Krypton",83.798,4,18,0,"noble",3.0,[2,8,18,8],[0,2],-157,-153,"Used in flash photography. Can form some compounds (KrF₂)."),
    # Period 5
    _e(37,"Rb","Rubidium",85.468,5,1,1,"alkali",0.82,[2,8,18,8,1],[1],39,688,"Used in atomic clocks. Ignites spontaneously in air; gives purple fireworks."),
    _e(38,"Sr","Strontium",87.62,5,2,2,"alkaline",0.95,[2,8,18,8,2],[2],777,1382,"Gives red color to fireworks. Radioactive Sr-90 is a dangerous fission product."),
    _e(39,"Y","Yttrium",88.906,5,3,3,"transition",1.22,[2,8,18,9,2],[3],1522,3345,"Used in LED lights and targeted cancer therapy (Y-90)."),
    _e(40,"Zr","Zirconium",91.224,5,4,4,"transition",1.33,[2,8,18,10,2],[4],1855,4409,"Used in nuclear fuel cladding. Cubic zirconia is a diamond simulant."),
    _e(41,"Nb","Niobium",92.906,5,5,5,"transition",1.60,[2,8,18,12,1],[3,5],2477,4744,"Used in superconducting MRI magnets and high-strength steel."),
    _e(42,"Mo","Molybdenum",95.96,5,6,6,"transition",2.16,[2,8,18,13,1],[2,3,4,6],2623,4639,"Essential for nitrogen-fixing enzymes. Used in high-strength steel alloys."),
    _e(43,"Tc","Technetium",98,5,7,7,"transition",1.9,[2,8,18,13,2],[4,7],2157,4265,"First synthetically produced element. Tc-99m is the most used medical imaging isotope."),
    _e(44,"Ru","Ruthenium",101.07,5,8,8,"transition",2.2,[2,8,18,15,1],[2,3,4,6,8],2334,4150,"Platinum group metal used in electronics and catalytic chemistry."),
    _e(45,"Rh","Rhodium",102.906,5,9,9,"transition",2.28,[2,8,18,16,1],[1,2,3],1964,3695,"Rarest and most expensive precious metal. Critical for catalytic converters."),
    _e(46,"Pd","Palladium",106.42,5,10,10,"transition",2.20,[2,8,18,18,0],[2,4],1555,2963,"Can absorb 900× its volume of hydrogen. Used in catalytic converters."),
    _e(47,"Ag","Silver",107.868,5,11,11,"transition",1.93,[2,8,18,18,1],[1],962,2162,"Best electrical conductor of all metals. Used in photography and antimicrobials."),
    _e(48,"Cd","Cadmium",112.411,5,12,12,"transition",1.69,[2,8,18,18,2],[2],321,767,"Toxic heavy metal. Used in Ni-Cd batteries and yellow/orange pigments."),
    _e(49,"In","Indium",114.818,5,13,3,"post-trans",1.78,[2,8,18,18,3],[3],157,2072,"Used in ITO for touchscreens and flat-panel displays."),
    _e(50,"Sn","Tin",118.710,5,14,4,"post-trans",1.96,[2,8,18,18,4],[2,4],232,2602,"Used in solder, bronze, and tin cans. Has most stable isotopes of any element (10)."),
    _e(51,"Sb","Antimony",121.760,5,15,5,"metalloid",2.05,[2,8,18,18,5],[-3,3,5],631,1587,"Used in flame retardants and lead-acid batteries. Ancient eye cosmetic (kohl)."),
    _e(52,"Te","Tellurium",127.60,5,16,6,"metalloid",2.10,[2,8,18,18,6],[-2,2,4,6],450,988,"Used in CdTe solar cells and thermoelectric devices."),
    _e(53,"I","Iodine",126.904,5,17,7,"halogen",2.66,[2,8,18,18,7],[-1,1,3,5,7],114,184,"Essential for thyroid hormones. Sublimes at room temperature. Antiseptic."),
    _e(54,"Xe","Xenon",131.293,5,18,0,"noble",2.60,[2,8,18,18,8],[0,2,4,6,8],-112,-108,"Used in flash lamps and ion thrusters. First noble gas to form compounds (1962)."),
    # Period 6
    _e(55,"Cs","Cesium",132.905,6,1,1,"alkali",0.79,[2,8,18,18,8,1],[1],29,671,"Most electropositive stable element. Used in atomic clocks."),
    _e(56,"Ba","Barium",137.327,6,2,2,"alkaline",0.89,[2,8,18,18,8,2],[2],727,1870,"Used in oil drilling mud. BaSO₄ used as GI X-ray contrast agent."),
    _e(57,"La","Lanthanum",138.905,6,3,3,"lanthanide",1.10,[2,8,18,18,9,2],[3],920,3464,"First lanthanide. Used in camera lenses and high-intensity arc lamps."),
    _e(58,"Ce","Cerium",140.116,6,None,3,"lanthanide",1.12,[2,8,18,19,9,2],[3,4],798,3443,"Most abundant lanthanide. Used in catalytic converters and self-cleaning ovens."),
    _e(59,"Pr","Praseodymium",140.908,6,None,3,"lanthanide",1.13,[2,8,18,21,8,2],[3,4],931,3520,"Used in high-strength permanent magnets for aircraft engines."),
    _e(60,"Nd","Neodymium",144.242,6,None,3,"lanthanide",1.14,[2,8,18,22,8,2],[3],1021,3074,"Used in the strongest permanent magnets (Nd₂Fe₁₄B). Essential for EV motors."),
    _e(61,"Pm","Promethium",145,6,None,3,"lanthanide",1.13,[2,8,18,23,8,2],[3],1100,3000,"Only radioactive lanthanide. Used in nuclear batteries for spacecraft."),
    _e(62,"Sm","Samarium",150.36,6,None,3,"lanthanide",1.17,[2,8,18,24,8,2],[2,3],1072,1794,"Sm-Co magnets work at high temperatures. Used in cancer treatment (Sm-153)."),
    _e(63,"Eu","Europium",151.964,6,None,3,"lanthanide",1.2,[2,8,18,25,8,2],[2,3],822,1529,"Red phosphors for TV screens. Visible in Euro banknote security features."),
    _e(64,"Gd","Gadolinium",157.25,6,None,3,"lanthanide",1.20,[2,8,18,25,9,2],[3],1313,3273,"Used as MRI contrast agents. Highest neutron capture cross-section of stable elements."),
    _e(65,"Tb","Terbium",158.925,6,None,3,"lanthanide",1.10,[2,8,18,27,8,2],[3],1356,3230,"Green phosphors for displays. Terfenol-D is the most magnetostrictive material."),
    _e(66,"Dy","Dysprosium",162.500,6,None,3,"lanthanide",1.22,[2,8,18,28,8,2],[3],1412,2567,"Enhances Nd magnets for high-temperature performance. Highest magnetic moment per atom."),
    _e(67,"Ho","Holmium",164.930,6,None,3,"lanthanide",1.23,[2,8,18,29,8,2],[3],1474,2700,"Highest magnetic moment of any naturally occurring element."),
    _e(68,"Er","Erbium",167.259,6,None,3,"lanthanide",1.24,[2,8,18,30,8,2],[3],1529,2868,"Used in fiber optic amplifiers (EDFA). Pink color in glass and ceramics."),
    _e(69,"Tm","Thulium",168.934,6,None,3,"lanthanide",1.25,[2,8,18,31,8,2],[3],1545,1950,"Rarest stable lanthanide. Used in portable X-ray machines."),
    _e(70,"Yb","Ytterbium",173.054,6,None,3,"lanthanide",1.1,[2,8,18,32,8,2],[2,3],824,1196,"Used in stainless steel, lasers, and the most precise atomic clocks."),
    _e(71,"Lu","Lutetium",174.967,6,None,3,"lanthanide",1.27,[2,8,18,32,9,2],[3],1663,3402,"Densest and hardest lanthanide. Last of the lanthanide series."),
    _e(72,"Hf","Hafnium",178.49,6,4,4,"transition",1.3,[2,8,18,32,10,2],[4],2233,4603,"Used in nuclear reactor control rods and microchip gate insulators."),
    _e(73,"Ta","Tantalum",180.948,6,5,5,"transition",1.5,[2,8,18,32,11,2],[5],3017,5458,"Highly biocompatible. Used in surgical implants and capacitors."),
    _e(74,"W","Tungsten",183.84,6,6,6,"transition",2.36,[2,8,18,32,12,2],[2,4,6],3422,5555,"Highest melting point of any element (3422°C). Used in light bulb filaments."),
    _e(75,"Re","Rhenium",186.207,6,7,7,"transition",1.9,[2,8,18,32,13,2],[1,2,3,4,5,6,7],3186,5596,"Second highest melting point. Used in jet engine turbine blades."),
    _e(76,"Os","Osmium",190.23,6,8,8,"transition",2.2,[2,8,18,32,14,2],[2,3,4,6,8],3033,5012,"Densest naturally occurring element (22.59 g/cm³). Used in fountain pen nibs."),
    _e(77,"Ir","Iridium",192.217,6,9,9,"transition",2.20,[2,8,18,32,15,2],[1,2,3,4],2446,4428,"Second densest element. Iridium layer marks the K-Pg extinction event."),
    _e(78,"Pt","Platinum",195.084,6,10,10,"transition",2.28,[2,8,18,32,17,1],[2,4],1768,3825,"Precious noble metal. Used in catalytic converters, fuel cells, and jewelry."),
    _e(79,"Au","Gold",196.967,6,11,11,"transition",2.54,[2,8,18,32,18,1],[1,3],1064,2856,"Noble metal that doesn't tarnish. Color arises from relativistic electron effects."),
    _e(80,"Hg","Mercury",200.59,6,12,12,"transition",2.00,[2,8,18,32,18,2],[1,2],-39,357,"Only liquid metal at room temperature. Highly toxic."),
    _e(81,"Tl","Thallium",204.383,6,13,3,"post-trans",1.62,[2,8,18,32,18,3],[1,3],304,1473,"Highly toxic heavy metal. Used in some semiconductors."),
    _e(82,"Pb","Lead",207.2,6,14,4,"post-trans",2.33,[2,8,18,32,18,4],[2,4],328,1749,"Dense metal for radiation shielding. Highly toxic to the nervous system."),
    _e(83,"Bi","Bismuth",208.980,6,15,5,"post-trans",2.02,[2,8,18,32,18,5],[3,5],271,1564,"Heaviest stable element. Used in stomach medicines. Forms iridescent crystals."),
    _e(84,"Po","Polonium",209,6,16,6,"metalloid",2.0,[2,8,18,32,18,6],[2,4],254,962,"Highly radioactive. Discovered by Marie Curie, named after Poland."),
    _e(85,"At","Astatine",210,6,17,7,"halogen",2.2,[2,8,18,32,18,7],[-1,1,3,5],302,337,"Rarest naturally occurring element. Earth contains only ~30 g total."),
    _e(86,"Rn","Radon",222,6,18,0,"noble",2.2,[2,8,18,32,18,8],[0,2],-71,-62,"Radioactive noble gas. Second leading cause of lung cancer."),
    # Period 7
    _e(87,"Fr","Francium",223,7,1,1,"alkali",0.70,[2,8,18,32,18,8,1],[1],27,677,"Most unstable naturally occurring element. Only ~20–30 atoms exist on Earth at any time."),
    _e(88,"Ra","Radium",226,7,2,2,"alkaline",0.9,[2,8,18,32,18,8,2],[2],700,1737,"Highly radioactive. Discovered by Marie Curie. Used in early cancer treatment."),
    _e(89,"Ac","Actinium",227,7,3,3,"actinide",1.1,[2,8,18,32,18,9,2],[3],1050,3200,"First actinide. 225× more radioactive than radium. Neutron source."),
    _e(90,"Th","Thorium",232.038,7,None,4,"actinide",1.3,[2,8,18,32,18,10,2],[4],1750,4820,"Potential nuclear fuel. 3× more abundant than uranium in Earth's crust."),
    _e(91,"Pa","Protactinium",231.036,7,None,5,"actinide",1.5,[2,8,18,32,20,9,2],[4,5],1568,4027,"Rare, highly toxic radioactive. Intermediate in U-235 decay chain."),
    _e(92,"U","Uranium",238.029,7,None,6,"actinide",1.38,[2,8,18,32,21,9,2],[3,4,5,6],1135,4131,"U-235 undergoes fission chain reactions. Primary nuclear fuel."),
    _e(93,"Np","Neptunium",237,7,None,7,"actinide",1.36,[2,8,18,32,22,9,2],[3,4,5,6],644,4000,"First transuranic element. Named after Neptune."),
    _e(94,"Pu","Plutonium",244,7,None,8,"actinide",1.28,[2,8,18,32,24,8,2],[3,4,5,6],640,3228,"Used in nuclear weapons and reactors. Pu-238 powers spacecraft like Voyager."),
    _e(95,"Am","Americium",243,7,None,9,"actinide",1.13,[2,8,18,32,25,8,2],[3,4,5,6],1176,2607,"Am-241 is the ionization source in household smoke detectors."),
    _e(96,"Cm","Curium",247,7,None,10,"actinide",1.28,[2,8,18,32,25,9,2],[3,4],1345,3110,"Named after Marie and Pierre Curie. Intensely radioactive heat source."),
    _e(97,"Bk","Berkelium",247,7,None,11,"actinide",1.3,[2,8,18,32,27,8,2],[3,4],986,2627,"Named after Berkeley, California. Required to synthesize heavier elements."),
    _e(98,"Cf","Californium",251,7,None,12,"actinide",1.3,[2,8,18,32,28,8,2],[2,3,4],900,1472,"Strongest neutron emitter known. Used to start nuclear reactors."),
    _e(99,"Es","Einsteinium",252,7,None,13,"actinide",1.3,[2,8,18,32,29,8,2],[2,3],860,996,"Named after Albert Einstein. First produced in hydrogen bomb test (1952)."),
    _e(100,"Fm","Fermium",257,7,None,14,"actinide",1.3,[2,8,18,32,30,8,2],[2,3],None,None,"Named after Enrico Fermi. Discovered in hydrogen bomb test debris."),
    _e(101,"Md","Mendelevium",258,7,None,14,"actinide",1.3,[2,8,18,32,31,8,2],[2,3],None,None,"Named after Mendeleev. First element produced one atom at a time."),
    _e(102,"No","Nobelium",259,7,None,14,"actinide",1.3,[2,8,18,32,32,8,2],[2,3],None,None,"Named after Alfred Nobel. Discovery disputed between US and Soviet teams."),
    _e(103,"Lr","Lawrencium",262,7,3,14,"actinide",1.3,[2,8,18,32,32,8,3],[3],None,None,"Named after Ernest Lawrence. Last actinide element."),
    _e(104,"Rf","Rutherfordium",267,7,4,4,"transition",-1,[2,8,18,32,32,10,2],[4],None,None,"First transactinide element. Behaves chemically like hafnium."),
    _e(105,"Db","Dubnium",268,7,5,5,"transition",-1,[2,8,18,32,32,11,2],[5],None,None,"Named after Dubna, Russia. Synthesized in single atoms."),
    _e(106,"Sg","Seaborgium",271,7,6,6,"transition",-1,[2,8,18,32,32,12,2],[6],None,None,"Named after Glenn Seaborg — rare honor given while he was still alive."),
    _e(107,"Bh","Bohrium",272,7,7,7,"transition",-1,[2,8,18,32,32,13,2],[7],None,None,"Named after Niels Bohr. Half-life of seconds."),
    _e(108,"Hs","Hassium",270,7,8,8,"transition",-1,[2,8,18,32,32,14,2],[8],None,None,"Named after Hesse, Germany."),
    _e(109,"Mt","Meitnerium",276,7,9,9,"transition",-1,[2,8,18,32,32,15,2],[],None,None,"Named after Lise Meitner, overlooked for the Nobel Prize."),
    _e(110,"Ds","Darmstadtium",281,7,10,10,"transition",-1,[2,8,18,32,32,17,1],[],None,None,"Named after Darmstadt, Germany."),
    _e(111,"Rg","Roentgenium",282,7,11,11,"transition",-1,[2,8,18,32,32,18,1],[],None,None,"Named after Wilhelm Röntgen (discoverer of X-rays)."),
    _e(112,"Cn","Copernicium",285,7,12,12,"transition",-1,[2,8,18,32,32,18,2],[],None,None,"Named after Copernicus. May be gaseous at room temperature."),
    _e(113,"Nh","Nihonium",286,7,13,3,"post-trans",-1,[2,8,18,32,32,18,3],[],None,None,"Named after Japan (Nihon). First element found by an Asian team (RIKEN)."),
    _e(114,"Fl","Flerovium",289,7,14,4,"post-trans",-1,[2,8,18,32,32,18,4],[],None,None,"Named after Flerov Laboratory. May behave as a noble gas."),
    _e(115,"Mc","Moscovium",290,7,15,5,"post-trans",-1,[2,8,18,32,32,18,5],[],None,None,"Named after Moscow Oblast, Russia."),
    _e(116,"Lv","Livermorium",293,7,16,6,"post-trans",-1,[2,8,18,32,32,18,6],[],None,None,"Named after Lawrence Livermore National Laboratory."),
    _e(117,"Ts","Tennessine",294,7,17,7,"halogen",-1,[2,8,18,32,32,18,7],[],None,None,"Named after Tennessee. Expected to be a solid metalloid."),
    _e(118,"Og","Oganesson",294,7,18,0,"noble",-1,[2,8,18,32,32,18,8],[],None,None,"Heaviest known element. Named after Yuri Oganessian. Only 5 atoms ever synthesized."),
]


# _______ PERIODIC TABLE CLASS __________________________
class PerodicTable
    """
    Central registry for all elements.
    Supports O(1) lookup by Z and symbol, and O(n) filtered queries.
    """

    def __init__(self) -> None:
        self._all      = list(ALL_ELEMENTS)
        self._by_z     = {el.z:     el for el in self._all}
        self._by_sym   = {el.symbol: el for el in self._all}
        self._by_name  = {el.name.lower(): el for el in self._all}


    # __ Lookup _______________________________________
    def get(self, query) -> Optional[Element]:
        """Accept atomic number (int), symbol, or name (str)."""
        if isinstance(query, int):
            return self._by_z.get(query)
        q = query.strip()
        return (self._by_sym.get(q)
                or self._by_sym.get(q.capitalize())
                or self._by_name.get(q.lower()))

    def search(self, text: str) -> List[Element]:
        """Case-insensitive substring search on name or symbol."""
        t = text.lower()
        return [el for el in self._all
                if t in el.name.lower() or t == el.symbol.lower() or t == str(el.z)]

    # __ Filtered queries (room for more) _________________________
    def by_category(self, cat: str)  -> List[Element]: return [e for e in self._all if e.category == cat]
    def by_period(self, p: int)      -> List[Element]: return [e for e in self._all if e.period == p]
    def by_group(self, g: int)       -> List[Element]: return [e for e in self._all if e.group == g]
    def metals(self)                 -> List[Element]: return [e for e in self._all if e.is_metal]
    def nonmetals(self)              -> List[Element]: return [e for e in self._all if not e.is_metal and e.category != "metalloid"]
    def radioactive(self)            -> List[Element]: return [e for e in self._all if e.is_radioactive]
    def stable(self)                 -> List[Element]: return [e for e in self._all if not e.is_radioactive]
    def all(self)                    -> List[Element]: return list(self._all)
    def __len__(self) -> int:
        return len(self._all)
    def __iter__(self):
        return iter(self._all)

    def __contains__(self, item) -> bool:
        if isinstance(item, int): return item in self._by_z
        if isinstance(item, str): return item in self._by_sym
        return item in self._all

    def print_ascii_table(self) -> None:
        """Print a simplified ASCII periodic table to the console."""
        grid: dict = {}
        for el in self._all:
            z = el.z
            if 57 <= z <= 71:
                row, col = 9, z - 54
            elif 89 <= z <= 103:
                row, col = 10, z - 86
            elif el.group:
                row, col = el.period, el.group
            else:
                continue
            grid[(row, col)] = el.symbol

        print("\n  PERIODIC TABLE OF ELEMENTS")
        print("  " + "─" * 72)
        for row in range(1, 11):
            line = f"  {row:2} |"
            for col in range(1, 19):
                sym = grid.get((row, col), "")
                line += f"{sym:>4}"
            print(line)
        print("  " + "─" * 72)
        print("       1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18")
        print()


# ___ SINGLETON INSTANCE _______________
periodic_table = PeriodicTable()
