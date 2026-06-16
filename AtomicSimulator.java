import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

/**
 * Atomic Simulator – Interactive Java/Swing application
 * Compile: javac AtomicSimulator.java
 * Run:     java AtomicSimulator
 *
 * OOP Architecture (each class is independently extensible):
 *   Element       – Immutable data model for all 118 elements
 *   Compound      – Chemical compound with bonding metadata
 *   BohrPanel     – Animated Bohr model (Graphics2D + javax.swing.Timer)
 *   AtomTab       – Element browser with Bohr view + info panel
 *   TableTab      – Periodic table grid (18 × 10 null-layout)
 *   CompoundTab   – Compound builder with chemistry rule engine
 *   MainWindow    – JFrame host with dark theme
 */
public class AtomicSimulator {

    // ─────────────────── DESIGN TOKENS ───────────────────
    static final Color BG0   = new Color(3,7,18);
    static final Color BG1   = new Color(15,23,42);
    static final Color BG2   = new Color(30,41,59);
    static final Color BD    = new Color(45,63,84);
    static final Color TXT   = new Color(240,246,255);
    static final Color TXT2  = new Color(148,163,184);
    static final Color TXT3  = new Color(74,96,121);
    static final Color ACC   = new Color(56,189,248);
    static final Font  MONO  = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    static final Font  MONOB = new Font(Font.MONOSPACED, Font.BOLD,  13);
    static final Font  SANS  = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    static final Font  SANSS = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

    // ─────────────────── ELEMENT MODEL ───────────────────
    static class Element {
        int z, period, group, valence;
        String symbol, name, category;
        double mass, en;       // en = -1 means unknown
        int[]  shells, ox;     // ox can be null for superheavy elements
        double mp, bp;         // -9999 means unknown
        String desc;

        static final Map<String,Color> CAT_COLOR = new LinkedHashMap<>();
        static final Map<String,String> CAT_LABEL = new LinkedHashMap<>();
        static {
            CAT_COLOR.put("alkali",      new Color(248,113,113));
            CAT_COLOR.put("alkaline",    new Color(251,146,60));
            CAT_COLOR.put("transition",  new Color(96,165,250));
            CAT_COLOR.put("post-trans",  new Color(52,211,153));
            CAT_COLOR.put("metalloid",   new Color(192,132,252));
            CAT_COLOR.put("nonmetal",    new Color(250,204,21));
            CAT_COLOR.put("halogen",     new Color(74,222,128));
            CAT_COLOR.put("noble",       new Color(129,140,248));
            CAT_COLOR.put("lanthanide",  new Color(244,114,182));
            CAT_COLOR.put("actinide",    new Color(251,113,133));
            CAT_LABEL.put("alkali",     "Alkali Metal");
            CAT_LABEL.put("alkaline",   "Alkaline Earth Metal");
            CAT_LABEL.put("transition", "Transition Metal");
            CAT_LABEL.put("post-trans", "Post-Transition Metal");
            CAT_LABEL.put("metalloid",  "Metalloid");
            CAT_LABEL.put("nonmetal",   "Reactive Nonmetal");
            CAT_LABEL.put("halogen",    "Halogen");
            CAT_LABEL.put("noble",      "Noble Gas");
            CAT_LABEL.put("lanthanide", "Lanthanide");
            CAT_LABEL.put("actinide",   "Actinide");
        }
        Color  color()    { return CAT_COLOR.getOrDefault(category, Color.GRAY); }
        String catLabel() { return CAT_LABEL.getOrDefault(category, category);   }
        boolean isMetal() { return category.equals("alkali")||category.equals("alkaline")||
                                   category.equals("transition")||category.equals("post-trans"); }
        boolean isRadioactive() { return z>83||z==43||z==61||z==85||z==87; }
        String shellStr() {
            String[] N={"K","L","M","N","O","P","Q"};
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<shells.length;i++){if(i>0)sb.append("  ");sb.append(N[i]).append(": ").append(shells[i]);}
            return sb.toString();
        }
        String oxStr() {
            if(ox==null||ox.length==0) return "—";
            return Arrays.stream(ox).mapToObj(o->o>0?"+"+o:String.valueOf(o)).collect(Collectors.joining(", "));
        }
        @Override public String toString(){ return symbol+" - "+name; }
    }

    // ─────────────────── COMPOUND MODEL ──────────────────
    static class Compound {
        String formula, name, type, shape, desc, uses;
        Map<String,Integer> elems;
        boolean known, predicted;
        static final Map<String,Color> TYPE_COLOR = new HashMap<>();
        static { TYPE_COLOR.put("ionic",new Color(251,146,60)); TYPE_COLOR.put("covalent",new Color(56,189,248));
                 TYPE_COLOR.put("metallic",new Color(192,132,252)); TYPE_COLOR.put("inert",new Color(74,222,128));
                 TYPE_COLOR.put("none",new Color(248,113,113)); }
        Compound(String f,String n,String t,String sh,String d,String u,boolean k,boolean p,Object... ev){
            formula=f;name=n;type=t;shape=sh;desc=d;uses=u;known=k;predicted=p;
            elems=new LinkedHashMap<>();
            for(int i=0;i<ev.length;i+=2) elems.put((String)ev[i],(Integer)ev[i+1]);
        }
        Color typeColor(){ return TYPE_COLOR.getOrDefault(type,Color.GRAY); }
    }

    // ──────────────────── DATA ────────────────────────────
    static final List<Element>  ELEMENTS    = new ArrayList<>();
    static final List<Compound> COMPOUNDS   = new ArrayList<>();

    static Element el(int z,String s,String n,double m,int p,int g,int v,
                      String cat,double en,int[] sh,int[] ox,double mp,double bp,String d){
        Element e=new Element();
        e.z=z;e.symbol=s;e.name=n;e.mass=m;e.period=p;e.group=g;e.valence=v;
        e.category=cat;e.en=en;e.shells=sh;e.ox=ox;e.mp=mp;e.bp=bp;e.desc=d;
        return e;
    }

    static { // ── 118 elements ──
        ELEMENTS.add(el(1,"H","Hydrogen",1.008,1,1,1,"nonmetal",2.20,new int[]{1},new int[]{-1,1},-259,-253,"Most abundant element; powers stars via fusion. Essential for water and all organic molecules."));
        ELEMENTS.add(el(2,"He","Helium",4.003,1,18,0,"noble",-1,new int[]{2},new int[]{0},-272,-269,"Completely inert noble gas. Used in MRI machines, balloons, and as a cryogenic coolant."));
        ELEMENTS.add(el(3,"Li","Lithium",6.941,2,1,1,"alkali",0.98,new int[]{2,1},new int[]{1},181,1342,"Lightest solid element. Powers rechargeable batteries; used in psychiatric medication."));
        ELEMENTS.add(el(4,"Be","Beryllium",9.012,2,2,2,"alkaline",1.57,new int[]{2,2},new int[]{2},1287,2468,"Extremely stiff and light metal. Toxic if inhaled; used in aerospace and X-ray windows."));
        ELEMENTS.add(el(5,"B","Boron",10.811,2,13,3,"metalloid",2.04,new int[]{2,3},new int[]{3},2076,3927,"Essential micronutrient for plants. Used in Pyrex glass and nuclear reactor control rods."));
        ELEMENTS.add(el(6,"C","Carbon",12.011,2,14,4,"nonmetal",2.55,new int[]{2,4},new int[]{-4,-3,-2,-1,0,1,2,3,4},3550,4027,"Basis of all known life. Forms diamond, graphite, and over 10 million organic compounds."));
        ELEMENTS.add(el(7,"N","Nitrogen",14.007,2,15,5,"nonmetal",3.04,new int[]{2,5},new int[]{-3,-2,-1,1,2,3,4,5},-210,-196,"78% of Earth's atmosphere. Essential for amino acids, proteins, and DNA."));
        ELEMENTS.add(el(8,"O","Oxygen",15.999,2,16,6,"nonmetal",3.44,new int[]{2,6},new int[]{-2,-1},-218,-183,"Essential for aerobic life. Most abundant element in Earth's crust. Highly electronegative."));
        ELEMENTS.add(el(9,"F","Fluorine",18.998,2,17,7,"halogen",3.98,new int[]{2,7},new int[]{-1},-220,-188,"Most electronegative element. Used in Teflon, refrigerants, and toothpaste."));
        ELEMENTS.add(el(10,"Ne","Neon",20.180,2,18,0,"noble",-1,new int[]{2,8},new int[]{0},-249,-246,"Completely inert. Used in neon signs producing the characteristic red-orange glow."));
        ELEMENTS.add(el(11,"Na","Sodium",22.990,3,1,1,"alkali",0.93,new int[]{2,8,1},new int[]{1},98,883,"Essential electrolyte for nerve impulses. Reacts violently with water."));
        ELEMENTS.add(el(12,"Mg","Magnesium",24.305,3,2,2,"alkaline",1.31,new int[]{2,8,2},new int[]{2},650,1090,"Central atom in chlorophyll. Essential for 300+ enzymes. Used in lightweight alloys."));
        ELEMENTS.add(el(13,"Al","Aluminum",26.982,3,13,3,"post-trans",1.61,new int[]{2,8,3},new int[]{3},660,2519,"Most abundant metal in Earth's crust. Lightweight and corrosion-resistant."));
        ELEMENTS.add(el(14,"Si","Silicon",28.086,3,14,4,"metalloid",1.90,new int[]{2,8,4},new int[]{-4,4},1414,3265,"Basis of computer chips and solar cells. Named Silicon Valley after this element."));
        ELEMENTS.add(el(15,"P","Phosphorus",30.974,3,15,5,"nonmetal",2.19,new int[]{2,8,5},new int[]{-3,3,5},44,281,"Essential for DNA, RNA, and ATP (cellular energy). Used in fertilizers."));
        ELEMENTS.add(el(16,"S","Sulfur",32.065,3,16,6,"nonmetal",2.58,new int[]{2,8,6},new int[]{-2,2,4,6},115,445,"Found in cysteine and methionine amino acids. Used in rubber vulcanization."));
        ELEMENTS.add(el(17,"Cl","Chlorine",35.453,3,17,7,"halogen",3.16,new int[]{2,8,7},new int[]{-1,1,3,5,7},-102,-34,"Disinfects water and swimming pools. Forms table salt (NaCl) with sodium."));
        ELEMENTS.add(el(18,"Ar","Argon",39.948,3,18,0,"noble",-1,new int[]{2,8,8},new int[]{0},-189,-186,"Third most abundant atmospheric gas (0.93%). Used in light bulbs and arc welding."));
        ELEMENTS.add(el(19,"K","Potassium",39.098,4,1,1,"alkali",0.82,new int[]{2,8,8,1},new int[]{1},64,759,"Essential for nerve function and muscle contraction. Symbol K from Latin Kalium."));
        ELEMENTS.add(el(20,"Ca","Calcium",40.078,4,2,2,"alkaline",1.00,new int[]{2,8,8,2},new int[]{2},842,1484,"Most abundant mineral in the human body. Builds bones and teeth."));
        ELEMENTS.add(el(21,"Sc","Scandium",44.956,4,3,3,"transition",1.36,new int[]{2,8,9,2},new int[]{3},1541,2830,"Rare lightweight metal. Used in aerospace aluminum alloys."));
        ELEMENTS.add(el(22,"Ti","Titanium",47.867,4,4,4,"transition",1.54,new int[]{2,8,10,2},new int[]{2,3,4},1668,3287,"Strong, light, corrosion-resistant. Used in aircraft and medical implants."));
        ELEMENTS.add(el(23,"V","Vanadium",50.942,4,5,5,"transition",1.63,new int[]{2,8,11,2},new int[]{2,3,4,5},1910,3407,"Used in high-strength steel alloys. Promising for grid-scale energy storage."));
        ELEMENTS.add(el(24,"Cr","Chromium",51.996,4,6,6,"transition",1.66,new int[]{2,8,13,1},new int[]{2,3,6},1907,2671,"Makes steel stainless and corrosion-resistant. Hexavalent chromium is highly toxic."));
        ELEMENTS.add(el(25,"Mn","Manganese",54.938,4,7,7,"transition",1.55,new int[]{2,8,13,2},new int[]{2,3,4,6,7},1246,2061,"Essential trace element. Widest range of stable oxidation states of any element."));
        ELEMENTS.add(el(26,"Fe","Iron",55.845,4,8,8,"transition",1.83,new int[]{2,8,14,2},new int[]{2,3},1538,2861,"Most commonly used metal. Earth's core is mostly iron. Essential for hemoglobin."));
        ELEMENTS.add(el(27,"Co","Cobalt",58.933,4,9,9,"transition",1.88,new int[]{2,8,15,2},new int[]{2,3},1495,2927,"Cobalt blue pigment. Used in Li-ion batteries. Vitamin B12 contains cobalt."));
        ELEMENTS.add(el(28,"Ni","Nickel",58.693,4,10,10,"transition",1.91,new int[]{2,8,16,2},new int[]{2,3},1455,2913,"Used in stainless steel and coins. Magnetic at room temperature."));
        ELEMENTS.add(el(29,"Cu","Copper",63.546,4,11,11,"transition",1.90,new int[]{2,8,18,1},new int[]{1,2},1085,2562,"Best non-precious electrical conductor. First metal used by humans (~10,000 BCE)."));
        ELEMENTS.add(el(30,"Zn","Zinc",65.38,4,12,12,"transition",1.65,new int[]{2,8,18,2},new int[]{2},420,907,"Used in galvanizing steel and batteries. 4th most industrially used metal."));
        ELEMENTS.add(el(31,"Ga","Gallium",69.723,4,13,3,"post-trans",1.81,new int[]{2,8,18,3},new int[]{3},30,2204,"Melts in your hand (29.8 C). Used in semiconductors (GaAs) and LEDs."));
        ELEMENTS.add(el(32,"Ge","Germanium",72.64,4,14,4,"metalloid",2.01,new int[]{2,8,18,4},new int[]{-4,2,4},938,2833,"Semiconductor in early transistors. Mendeleev predicted its existence."));
        ELEMENTS.add(el(33,"As","Arsenic",74.922,4,15,5,"metalloid",2.18,new int[]{2,8,18,5},new int[]{-3,3,5},817,614,"Toxic metalloid used historically as a poison. Now used in semiconductors."));
        ELEMENTS.add(el(34,"Se","Selenium",78.96,4,16,6,"nonmetal",2.55,new int[]{2,8,18,6},new int[]{-2,2,4,6},221,685,"Essential trace element for thyroid function. Used in solar cells."));
        ELEMENTS.add(el(35,"Br","Bromine",79.904,4,17,7,"halogen",2.96,new int[]{2,8,18,7},new int[]{-1,1,3,5},-7,59,"One of two liquid elements at room temperature. Used in flame retardants."));
        ELEMENTS.add(el(36,"Kr","Krypton",83.798,4,18,0,"noble",3.0,new int[]{2,8,18,8},new int[]{0,2},-157,-153,"Used in flash photography. Surprisingly can form some compounds (KrF2)."));
        ELEMENTS.add(el(37,"Rb","Rubidium",85.468,5,1,1,"alkali",0.82,new int[]{2,8,18,8,1},new int[]{1},39,688,"Used in atomic clocks. Ignites spontaneously in air; gives purple fireworks."));
        ELEMENTS.add(el(38,"Sr","Strontium",87.62,5,2,2,"alkaline",0.95,new int[]{2,8,18,8,2},new int[]{2},777,1382,"Gives red color to fireworks. Radioactive Sr-90 is a dangerous fission product."));
        ELEMENTS.add(el(39,"Y","Yttrium",88.906,5,3,3,"transition",1.22,new int[]{2,8,18,9,2},new int[]{3},1522,3345,"Used in LED lights and targeted cancer therapy (Y-90)."));
        ELEMENTS.add(el(40,"Zr","Zirconium",91.224,5,4,4,"transition",1.33,new int[]{2,8,18,10,2},new int[]{4},1855,4409,"Used in nuclear fuel cladding. Cubic zirconia is a diamond simulant."));
        ELEMENTS.add(el(41,"Nb","Niobium",92.906,5,5,5,"transition",1.60,new int[]{2,8,18,12,1},new int[]{3,5},2477,4744,"Used in superconducting MRI magnets and high-strength steel."));
        ELEMENTS.add(el(42,"Mo","Molybdenum",95.96,5,6,6,"transition",2.16,new int[]{2,8,18,13,1},new int[]{2,3,4,6},2623,4639,"Essential for nitrogen-fixing enzymes. Used in high-strength steel alloys."));
        ELEMENTS.add(el(43,"Tc","Technetium",98,5,7,7,"transition",1.9,new int[]{2,8,18,13,2},new int[]{4,7},2157,4265,"First synthetically produced element. Tc-99m is the most used medical imaging isotope."));
        ELEMENTS.add(el(44,"Ru","Ruthenium",101.07,5,8,8,"transition",2.2,new int[]{2,8,18,15,1},new int[]{2,3,4,6,8},2334,4150,"Platinum group metal. Used in electronics and catalytic chemistry."));
        ELEMENTS.add(el(45,"Rh","Rhodium",102.906,5,9,9,"transition",2.28,new int[]{2,8,18,16,1},new int[]{1,2,3},1964,3695,"Rarest and most expensive precious metal. Critical for catalytic converters."));
        ELEMENTS.add(el(46,"Pd","Palladium",106.42,5,10,10,"transition",2.20,new int[]{2,8,18,18,0},new int[]{2,4},1555,2963,"Can absorb 900x its volume of hydrogen. Catalytic converters and fuel cells."));
        ELEMENTS.add(el(47,"Ag","Silver",107.868,5,11,11,"transition",1.93,new int[]{2,8,18,18,1},new int[]{1},962,2162,"Best electrical conductor of all metals. Used in photography and antimicrobials."));
        ELEMENTS.add(el(48,"Cd","Cadmium",112.411,5,12,12,"transition",1.69,new int[]{2,8,18,18,2},new int[]{2},321,767,"Toxic heavy metal. Used in Ni-Cd batteries and yellow/orange pigments."));
        ELEMENTS.add(el(49,"In","Indium",114.818,5,13,3,"post-trans",1.78,new int[]{2,8,18,18,3},new int[]{3},157,2072,"Used in ITO for touchscreens and flat-panel displays."));
        ELEMENTS.add(el(50,"Sn","Tin",118.710,5,14,4,"post-trans",1.96,new int[]{2,8,18,18,4},new int[]{2,4},232,2602,"Used in solder, bronze, and tin cans. Has most stable isotopes (10)."));
        ELEMENTS.add(el(51,"Sb","Antimony",121.760,5,15,5,"metalloid",2.05,new int[]{2,8,18,18,5},new int[]{-3,3,5},631,1587,"Used in flame retardants and lead-acid batteries. Ancient eye cosmetic."));
        ELEMENTS.add(el(52,"Te","Tellurium",127.60,5,16,6,"metalloid",2.10,new int[]{2,8,18,18,6},new int[]{-2,2,4,6},450,988,"Used in CdTe solar cells and thermoelectric devices."));
        ELEMENTS.add(el(53,"I","Iodine",126.904,5,17,7,"halogen",2.66,new int[]{2,8,18,18,7},new int[]{-1,1,3,5,7},114,184,"Essential for thyroid hormones. Sublimes at room temperature. Antiseptic."));
        ELEMENTS.add(el(54,"Xe","Xenon",131.293,5,18,0,"noble",2.60,new int[]{2,8,18,18,8},new int[]{0,2,4,6,8},-112,-108,"Used in flash lamps and ion thrusters. First noble gas to form compounds (1962)."));
        ELEMENTS.add(el(55,"Cs","Cesium",132.905,6,1,1,"alkali",0.79,new int[]{2,8,18,18,8,1},new int[]{1},29,671,"Most electropositive stable element. Used in atomic clocks."));
        ELEMENTS.add(el(56,"Ba","Barium",137.327,6,2,2,"alkaline",0.89,new int[]{2,8,18,18,8,2},new int[]{2},727,1870,"Used in oil drilling mud. BaSO4 is used as a GI X-ray contrast agent."));
        // Lanthanides — shown in row 9 of periodic table grid
        ELEMENTS.add(el(57,"La","Lanthanum",138.905,6,3,3,"lanthanide",1.10,new int[]{2,8,18,18,9,2},new int[]{3},920,3464,"First lanthanide. Used in camera lenses and high-intensity arc lamps."));
        ELEMENTS.add(el(58,"Ce","Cerium",140.116,6,0,3,"lanthanide",1.12,new int[]{2,8,18,19,9,2},new int[]{3,4},798,3443,"Most abundant lanthanide. Used in catalytic converters and self-cleaning ovens."));
        ELEMENTS.add(el(59,"Pr","Praseodymium",140.908,6,0,3,"lanthanide",1.13,new int[]{2,8,18,21,8,2},new int[]{3,4},931,3520,"Used in high-strength permanent magnets for aircraft engines."));
        ELEMENTS.add(el(60,"Nd","Neodymium",144.242,6,0,3,"lanthanide",1.14,new int[]{2,8,18,22,8,2},new int[]{3},1021,3074,"Used in the strongest permanent magnets (Nd2Fe14B). Essential for EV motors."));
        ELEMENTS.add(el(61,"Pm","Promethium",145,6,0,3,"lanthanide",1.13,new int[]{2,8,18,23,8,2},new int[]{3},1100,3000,"Only radioactive lanthanide. Used in nuclear batteries for spacecraft."));
        ELEMENTS.add(el(62,"Sm","Samarium",150.36,6,0,3,"lanthanide",1.17,new int[]{2,8,18,24,8,2},new int[]{2,3},1072,1794,"Sm-Co magnets work at high temperatures. Used in cancer treatment."));
        ELEMENTS.add(el(63,"Eu","Europium",151.964,6,0,3,"lanthanide",1.2,new int[]{2,8,18,25,8,2},new int[]{2,3},822,1529,"Red phosphors for TV screens. Visible in Euro banknote security features."));
        ELEMENTS.add(el(64,"Gd","Gadolinium",157.25,6,0,3,"lanthanide",1.20,new int[]{2,8,18,25,9,2},new int[]{3},1313,3273,"Used as MRI contrast agents. Highest neutron capture cross-section of stable elements."));
        ELEMENTS.add(el(65,"Tb","Terbium",158.925,6,0,3,"lanthanide",1.10,new int[]{2,8,18,27,8,2},new int[]{3},1356,3230,"Green phosphors for displays. Terfenol-D is the most magnetostrictive material."));
        ELEMENTS.add(el(66,"Dy","Dysprosium",162.500,6,0,3,"lanthanide",1.22,new int[]{2,8,18,28,8,2},new int[]{3},1412,2567,"Enhances Nd magnets for high-temp use. Highest magnetic moment per atom."));
        ELEMENTS.add(el(67,"Ho","Holmium",164.930,6,0,3,"lanthanide",1.23,new int[]{2,8,18,29,8,2},new int[]{3},1474,2700,"Highest magnetic moment of any naturally occurring element."));
        ELEMENTS.add(el(68,"Er","Erbium",167.259,6,0,3,"lanthanide",1.24,new int[]{2,8,18,30,8,2},new int[]{3},1529,2868,"Used in fiber optic amplifiers (EDFA). Pink color in glass and ceramics."));
        ELEMENTS.add(el(69,"Tm","Thulium",168.934,6,0,3,"lanthanide",1.25,new int[]{2,8,18,31,8,2},new int[]{3},1545,1950,"Rarest stable lanthanide. Used in portable X-ray machines."));
        ELEMENTS.add(el(70,"Yb","Ytterbium",173.054,6,0,3,"lanthanide",1.1,new int[]{2,8,18,32,8,2},new int[]{2,3},824,1196,"Used in stainless steel, high-power lasers, and the most precise atomic clocks."));
        ELEMENTS.add(el(71,"Lu","Lutetium",174.967,6,0,3,"lanthanide",1.27,new int[]{2,8,18,32,9,2},new int[]{3},1663,3402,"Densest and hardest lanthanide. Used in PET scan detectors."));
        ELEMENTS.add(el(72,"Hf","Hafnium",178.49,6,4,4,"transition",1.3,new int[]{2,8,18,32,10,2},new int[]{4},2233,4603,"Used in nuclear reactor control rods and microchip gate insulators."));
        ELEMENTS.add(el(73,"Ta","Tantalum",180.948,6,5,5,"transition",1.5,new int[]{2,8,18,32,11,2},new int[]{5},3017,5458,"Highly biocompatible. Used in surgical implants and capacitors."));
        ELEMENTS.add(el(74,"W","Tungsten",183.84,6,6,6,"transition",2.36,new int[]{2,8,18,32,12,2},new int[]{2,4,6},3422,5555,"Highest melting point of any element (3422 C). Used in light bulb filaments."));
        ELEMENTS.add(el(75,"Re","Rhenium",186.207,6,7,7,"transition",1.9,new int[]{2,8,18,32,13,2},new int[]{1,2,3,4,5,6,7},3186,5596,"Second highest melting point. Used in jet engine turbine blades."));
        ELEMENTS.add(el(76,"Os","Osmium",190.23,6,8,8,"transition",2.2,new int[]{2,8,18,32,14,2},new int[]{2,3,4,6,8},3033,5012,"Densest naturally occurring element (22.59 g/cm3). Used in fountain pen nibs."));
        ELEMENTS.add(el(77,"Ir","Iridium",192.217,6,9,9,"transition",2.20,new int[]{2,8,18,32,15,2},new int[]{1,2,3,4},2446,4428,"Second densest element. Iridium layer marks the K-Pg extinction event."));
        ELEMENTS.add(el(78,"Pt","Platinum",195.084,6,10,10,"transition",2.28,new int[]{2,8,18,32,17,1},new int[]{2,4},1768,3825,"Precious noble metal. Used in catalytic converters, fuel cells, and jewelry."));
        ELEMENTS.add(el(79,"Au","Gold",196.967,6,11,11,"transition",2.54,new int[]{2,8,18,32,18,1},new int[]{1,3},1064,2856,"Noble metal that doesn't tarnish. Its color arises from relativistic electron effects."));
        ELEMENTS.add(el(80,"Hg","Mercury",200.59,6,12,12,"transition",2.00,new int[]{2,8,18,32,18,2},new int[]{1,2},-39,357,"Only liquid metal at room temperature. Highly toxic. Used in fluorescent lights."));
        ELEMENTS.add(el(81,"Tl","Thallium",204.383,6,13,3,"post-trans",1.62,new int[]{2,8,18,32,18,3},new int[]{1,3},304,1473,"Highly toxic heavy metal. Used in some semiconductors."));
        ELEMENTS.add(el(82,"Pb","Lead",207.2,6,14,4,"post-trans",2.33,new int[]{2,8,18,32,18,4},new int[]{2,4},328,1749,"Dense metal for radiation shielding. Highly toxic to the nervous system."));
        ELEMENTS.add(el(83,"Bi","Bismuth",208.980,6,15,5,"post-trans",2.02,new int[]{2,8,18,32,18,5},new int[]{3,5},271,1564,"Heaviest stable element. Used in stomach medicines. Forms iridescent crystals."));
        ELEMENTS.add(el(84,"Po","Polonium",209,6,16,6,"metalloid",2.0,new int[]{2,8,18,32,18,6},new int[]{2,4},254,962,"Highly radioactive. Discovered by Marie Curie, named after Poland."));
        ELEMENTS.add(el(85,"At","Astatine",210,6,17,7,"halogen",2.2,new int[]{2,8,18,32,18,7},new int[]{-1,1,3,5},302,337,"Rarest naturally occurring element. Earth's crust contains only ~30 g total."));
        ELEMENTS.add(el(86,"Rn","Radon",222,6,18,0,"noble",2.2,new int[]{2,8,18,32,18,8},new int[]{0,2},-71,-62,"Radioactive noble gas that seeps into buildings. Second leading cause of lung cancer."));
        ELEMENTS.add(el(87,"Fr","Francium",223,7,1,1,"alkali",0.70,new int[]{2,8,18,32,18,8,1},new int[]{1},27,677,"Most unstable naturally occurring element. Only ~20-30 atoms exist on Earth at any time."));
        ELEMENTS.add(el(88,"Ra","Radium",226,7,2,2,"alkaline",0.9,new int[]{2,8,18,32,18,8,2},new int[]{2},700,1737,"Highly radioactive. Discovered by Marie Curie. Used in early cancer treatment."));
        // Actinides — shown in row 10 of periodic table grid
        ELEMENTS.add(el(89,"Ac","Actinium",227,7,3,3,"actinide",1.1,new int[]{2,8,18,32,18,9,2},new int[]{3},1050,3200,"First actinide. 225x more radioactive than radium. Neutron source."));
        ELEMENTS.add(el(90,"Th","Thorium",232.038,7,0,4,"actinide",1.3,new int[]{2,8,18,32,18,10,2},new int[]{4},1750,4820,"Potential nuclear fuel. 3x more abundant than uranium in Earth's crust."));
        ELEMENTS.add(el(91,"Pa","Protactinium",231.036,7,0,5,"actinide",1.5,new int[]{2,8,18,32,20,9,2},new int[]{4,5},1568,4027,"Rare, highly toxic radioactive element. Intermediate in U-235 decay chain."));
        ELEMENTS.add(el(92,"U","Uranium",238.029,7,0,6,"actinide",1.38,new int[]{2,8,18,32,21,9,2},new int[]{3,4,5,6},1135,4131,"U-235 undergoes fission chain reactions. Primary nuclear fuel."));
        ELEMENTS.add(el(93,"Np","Neptunium",237,7,0,7,"actinide",1.36,new int[]{2,8,18,32,22,9,2},new int[]{3,4,5,6},644,4000,"First transuranic element, produced in nuclear reactors. Named after Neptune."));
        ELEMENTS.add(el(94,"Pu","Plutonium",244,7,0,8,"actinide",1.28,new int[]{2,8,18,32,24,8,2},new int[]{3,4,5,6},640,3228,"Used in nuclear weapons and reactors. Pu-238 powers spacecraft like Voyager."));
        ELEMENTS.add(el(95,"Am","Americium",243,7,0,9,"actinide",1.13,new int[]{2,8,18,32,25,8,2},new int[]{3,4,5,6},1176,2607,"Am-241 is the ionization source in household smoke detectors."));
        ELEMENTS.add(el(96,"Cm","Curium",247,7,0,10,"actinide",1.28,new int[]{2,8,18,32,25,9,2},new int[]{3,4},1345,3110,"Named after Marie and Pierre Curie. Intensely radioactive heat source."));
        ELEMENTS.add(el(97,"Bk","Berkelium",247,7,0,11,"actinide",1.3,new int[]{2,8,18,32,27,8,2},new int[]{3,4},986,2627,"Named after Berkeley, California. Required to synthesize heavier elements."));
        ELEMENTS.add(el(98,"Cf","Californium",251,7,0,12,"actinide",1.3,new int[]{2,8,18,32,28,8,2},new int[]{2,3,4},900,1472,"Strongest neutron emitter known. Used to start nuclear reactors."));
        ELEMENTS.add(el(99,"Es","Einsteinium",252,7,0,13,"actinide",1.3,new int[]{2,8,18,32,29,8,2},new int[]{2,3},860,996,"Named after Albert Einstein. First produced in hydrogen bomb test (1952)."));
        ELEMENTS.add(el(100,"Fm","Fermium",257,7,0,14,"actinide",1.3,new int[]{2,8,18,32,30,8,2},new int[]{2,3},1527,-9999,"Named after Enrico Fermi. Also found in hydrogen bomb test debris."));
        ELEMENTS.add(el(101,"Md","Mendelevium",258,7,0,14,"actinide",1.3,new int[]{2,8,18,32,31,8,2},new int[]{2,3},827,-9999,"Named after Mendeleev. First element produced one atom at a time."));
        ELEMENTS.add(el(102,"No","Nobelium",259,7,0,14,"actinide",1.3,new int[]{2,8,18,32,32,8,2},new int[]{2,3},827,-9999,"Named after Alfred Nobel. Discovery disputed (Cold War US vs Soviet)."));
        ELEMENTS.add(el(103,"Lr","Lawrencium",262,7,3,14,"actinide",1.3,new int[]{2,8,18,32,32,8,3},new int[]{3},1627,-9999,"Named after Ernest Lawrence. Last actinide element."));
        ELEMENTS.add(el(104,"Rf","Rutherfordium",267,7,4,4,"transition",-1,new int[]{2,8,18,32,32,10,2},new int[]{4},2100,5500,"First transactinide. Named after Rutherford. Behaves chemically like hafnium."));
        ELEMENTS.add(el(105,"Db","Dubnium",268,7,5,5,"transition",-1,new int[]{2,8,18,32,32,11,2},new int[]{5},-9999,-9999,"Named after Dubna, Russia. Synthesized in single atoms."));
        ELEMENTS.add(el(106,"Sg","Seaborgium",271,7,6,6,"transition",-1,new int[]{2,8,18,32,32,12,2},new int[]{6},-9999,-9999,"Named after Glenn Seaborg, who was alive at time of naming."));
        ELEMENTS.add(el(107,"Bh","Bohrium",272,7,7,7,"transition",-1,new int[]{2,8,18,32,32,13,2},new int[]{7},-9999,-9999,"Named after Niels Bohr. Half-life of seconds."));
        ELEMENTS.add(el(108,"Hs","Hassium",270,7,8,8,"transition",-1,new int[]{2,8,18,32,32,14,2},new int[]{8},-9999,-9999,"Named after Hesse, Germany."));
        ELEMENTS.add(el(109,"Mt","Meitnerium",276,7,9,9,"transition",-1,new int[]{2,8,18,32,32,15,2},null,-9999,-9999,"Named after Lise Meitner, overlooked for the Nobel Prize."));
        ELEMENTS.add(el(110,"Ds","Darmstadtium",281,7,10,10,"transition",-1,new int[]{2,8,18,32,32,17,1},null,-9999,-9999,"Named after Darmstadt, Germany. Only made in minute quantities."));
        ELEMENTS.add(el(111,"Rg","Roentgenium",282,7,11,11,"transition",-1,new int[]{2,8,18,32,32,18,1},null,-9999,-9999,"Named after Wilhelm Rontgen (discoverer of X-rays)."));
        ELEMENTS.add(el(112,"Cn","Copernicium",285,7,12,12,"transition",-1,new int[]{2,8,18,32,32,18,2},null,-9999,-9999,"Named after Copernicus. May be gaseous at room temperature."));
        ELEMENTS.add(el(113,"Nh","Nihonium",286,7,13,3,"post-trans",-1,new int[]{2,8,18,32,32,18,3},null,-9999,-9999,"Named after Japan (Nihon). First element found by an Asian team."));
        ELEMENTS.add(el(114,"Fl","Flerovium",289,7,14,4,"post-trans",-1,new int[]{2,8,18,32,32,18,4},null,-9999,-9999,"Named after Flerov Laboratory. May behave as a noble gas."));
        ELEMENTS.add(el(115,"Mc","Moscovium",290,7,15,5,"post-trans",-1,new int[]{2,8,18,32,32,18,5},null,-9999,-9999,"Named after Moscow Oblast, Russia."));
        ELEMENTS.add(el(116,"Lv","Livermorium",293,7,16,6,"post-trans",-1,new int[]{2,8,18,32,32,18,6},null,-9999,-9999,"Named after Lawrence Livermore National Laboratory."));
        ELEMENTS.add(el(117,"Ts","Tennessine",294,7,17,7,"halogen",-1,new int[]{2,8,18,32,32,18,7},null,-9999,-9999,"Named after Tennessee. Expected to be a solid metalloid."));
        ELEMENTS.add(el(118,"Og","Oganesson",294,7,18,0,"noble",-1,new int[]{2,8,18,32,32,18,8},null,-9999,-9999,"Heaviest known element. Named after Yuri Oganessian. Only 5 atoms ever made."));
    }

    static { // ── 25 known compounds ──
        COMPOUNDS.add(new Compound("H\u2082O","Water","covalent","Bent","Universal solvent. Polar molecule with unique hydrogen bonding. Density maximum at 4 C.","Solvent, biological processes, industrial cooling",true,false,"H",2,"O",1));
        COMPOUNDS.add(new Compound("NaCl","Sodium Chloride","ionic","FCC Crystal","Common table salt. Ionic crystal; dissociates into Na+ and Cl- in water.","Food preservation, de-icing roads, chemical production",true,false,"Na",1,"Cl",1));
        COMPOUNDS.add(new Compound("CO\u2082","Carbon Dioxide","covalent","Linear","Two C=O double bonds. Major greenhouse gas; product of combustion and respiration.","Fire extinguishers, carbonation, photosynthesis reactant",true,false,"C",1,"O",2));
        COMPOUNDS.add(new Compound("NH\u2083","Ammonia","covalent","Trigonal Pyramidal","Lone pair gives trigonal pyramidal shape. Weak base; strong smell.","Fertilizers (Haber process), cleaning products, refrigerant",true,false,"N",1,"H",3));
        COMPOUNDS.add(new Compound("CH\u2084","Methane","covalent","Tetrahedral","Simplest hydrocarbon. 86x warming potential of CO2 over 20 years.","Natural gas fuel, hydrogen production",true,false,"C",1,"H",4));
        COMPOUNDS.add(new Compound("HCl","Hydrochloric Acid","covalent","Linear","Strong acid; fully dissociates in water. Present in gastric acid (stomach).","PVC production, metal pickling, pH control",true,false,"H",1,"Cl",1));
        COMPOUNDS.add(new Compound("H\u2082SO\u2084","Sulfuric Acid","covalent","Tetrahedral (at S)","Most widely produced industrial chemical. Highly corrosive and dehydrating.","Battery acid, fertilizers, chemical processing",true,false,"H",2,"S",1,"O",4));
        COMPOUNDS.add(new Compound("Fe\u2082O\u2083","Iron(III) Oxide (Rust)","ionic","Crystal Lattice","Common rust. Forms when iron reacts with oxygen and moisture.","Pigments, polishing compounds, thermite",true,false,"Fe",2,"O",3));
        COMPOUNDS.add(new Compound("CaCO\u2083","Calcium Carbonate","ionic","Crystal Lattice","Found in limestone, marble, chalk, and seashells. Dissolves in acid.","Construction, antacids, paper, chalk, cement",true,false,"Ca",1,"C",1,"O",3));
        COMPOUNDS.add(new Compound("SiO\u2082","Silicon Dioxide","covalent","Network Solid","Network covalent solid. Main component of sand, quartz, and glass.","Glass, semiconductors, concrete, quartz crystals",true,false,"Si",1,"O",2));
        COMPOUNDS.add(new Compound("MgO","Magnesium Oxide","ionic","FCC Crystal","High melting point (2852 C). Antacid and refractory material.","Antacids, refractory bricks, dietary supplements",true,false,"Mg",1,"O",1));
        COMPOUNDS.add(new Compound("NaOH","Sodium Hydroxide","ionic","Crystal Lattice","Strong base; highly caustic. Dissolves fats via saponification (soap-making).","Soap/paper production, drain cleaner, food processing",true,false,"Na",1,"O",1,"H",1));
        COMPOUNDS.add(new Compound("Al\u2082O\u2083","Aluminum Oxide","ionic","Crystal Lattice","Very hard material (Mohs 9). Ruby and sapphire are impure Al2O3.","Abrasives, ceramics, gemstones, aluminum smelting",true,false,"Al",2,"O",3));
        COMPOUNDS.add(new Compound("TiO\u2082","Titanium Dioxide","ionic","Crystal","Brilliant white, highly stable pigment. Also photocatalytic under UV light.","White paint, sunscreen, food coloring, solar cells",true,false,"Ti",1,"O",2));
        COMPOUNDS.add(new Compound("KCl","Potassium Chloride","ionic","FCC Crystal","Ionic salt similar to NaCl. Used in lethal injection protocols.","Fertilizers, salt substitute, IV fluids",true,false,"K",1,"Cl",1));
        COMPOUNDS.add(new Compound("H\u2082O\u2082","Hydrogen Peroxide","covalent","Bent","Unstable oxidizer; decomposes to water and oxygen.","Antiseptic, hair bleaching, rocket propellant (high conc.)",true,false,"H",2,"O",2));
        COMPOUNDS.add(new Compound("CO","Carbon Monoxide","covalent","Linear","Colorless, odorless, toxic. Binds hemoglobin 200x stronger than O2.","Steelmaking (reducing agent), chemical synthesis",true,false,"C",1,"O",1));
        COMPOUNDS.add(new Compound("SO\u2082","Sulfur Dioxide","covalent","Bent","Pungent gas from burning fossil fuels. Causes acid rain.","Wine preservative, bleaching, H2SO4 production",true,false,"S",1,"O",2));
        COMPOUNDS.add(new Compound("N\u2082O","Nitrous Oxide","covalent","Linear","Laughing gas. ~300x warming potential of CO2 per molecule.","Anesthesia, whipped cream propellant, racing fuel",true,false,"N",2,"O",1));
        COMPOUNDS.add(new Compound("HNO\u2083","Nitric Acid","covalent","Planar","Strong oxidizing acid. Passivates metals with a thin protective oxide layer.","Fertilizers, explosives, metal etching",true,false,"H",1,"N",1,"O",3));
        COMPOUNDS.add(new Compound("ZnO","Zinc Oxide","ionic","Hexagonal","White powder. n-type semiconductor and photocatalytic properties.","Sunscreen, rubber, pharmaceuticals, LEDs",true,false,"Zn",1,"O",1));
        COMPOUNDS.add(new Compound("CaO","Calcium Oxide","ionic","FCC Crystal","Quicklime. Reacts vigorously with water (exothermic) to form Ca(OH)2.","Cement, steel production, water treatment",true,false,"Ca",1,"O",1));
        COMPOUNDS.add(new Compound("AgCl","Silver Chloride","ionic","FCC Crystal","White precipitate; light-sensitive. Classic qualitative test for Cl- ions.","Photography, reference electrodes",true,false,"Ag",1,"Cl",1));
        COMPOUNDS.add(new Compound("CuSO\u2084","Copper(II) Sulfate","ionic","Crystal","Blue crystalline solid when hydrated. Bright blue color in solution.","Fungicide, electroplating, chemistry demonstrations",true,false,"Cu",1,"S",1,"O",4));
        COMPOUNDS.add(new Compound("HF","Hydrogen Fluoride","covalent","Linear","Weak acid but extremely dangerous. Penetrates skin and attacks bone.","Glass etching, semiconductor production, Teflon",true,false,"H",1,"F",1));
    }

    // ──────────────── CHEMISTRY ENGINE ───────────────────
    static final Set<String> METALS = new HashSet<>(Arrays.asList("alkali","alkaline","transition","post-trans"));

    static int gcd(int a, int b){ while(b!=0){int t=b;b=a%b;a=t;} return a; }

    static String sub(int n){
        if(n==1) return "";
        String subs="₀₁₂₃₄₅₆₇₈₉";
        StringBuilder sb=new StringBuilder();
        for(char c:String.valueOf(n).toCharArray()) sb.append(subs.charAt(c-'0'));
        return sb.toString();
    }

    static int[] gridPos(Element el){
        int z=el.z;
        if(z>=57&&z<=71) return new int[]{9,z-54};
        if(z>=89&&z<=103) return new int[]{10,z-86};
        return new int[]{el.period, el.group>0?el.group:3};
    }

    static Map<String,Integer> trayToMap(java.util.List<int[]> tray){
        Map<String,Integer> m=new LinkedHashMap<>();
        for(int[] item:tray) m.merge(ELEMENTS.get(item[0]).symbol,item[1],Integer::sum);
        return m;
    }

    static Compound lookupCompound(Map<String,Integer> keyMap){
        for(Compound c:COMPOUNDS){ if(c.elems.equals(keyMap)) return c; }
        return null;
    }

    static Compound predictBond(java.util.List<int[]> tray){
        if(tray.size()!=2) return null;
        Element a=ELEMENTS.get(tray.get(0)[0]), b=ELEMENTS.get(tray.get(1)[0]);
        if(a.category.equals("noble")||b.category.equals("noble"))
            return new Compound("No reaction","None","inert",null,
                "Noble gases have complete valence shells (8 electrons) and do not form compounds under normal conditions.","",false,false);
        boolean aM=METALS.contains(a.category), bM=METALS.contains(b.category);
        if(aM&&bM)
            return new Compound(a.symbol+"-"+b.symbol+" Alloy","Metallic alloy","metallic",null,
                "Both elements are metals and form an alloy via metallic bonding (delocalized electron sea). No discrete compound formula.","",false,true);
        if(aM||bM){
            Element M=aM?a:b, NM=aM?b:a;
            int mOx=(M.ox!=null)?Arrays.stream(M.ox).filter(o->o>0).findFirst().orElse(1):1;
            int nmOx=Math.abs((NM.ox!=null)?Arrays.stream(NM.ox).filter(o->o<0).findFirst().orElse(-1):-1);
            int g=gcd(mOx,nmOx), mC=nmOx/g, nmC=mOx/g;
            return new Compound(M.symbol+sub(mC)+NM.symbol+sub(nmC),"Predicted ionic","ionic",null,
                "Ionic compound: "+M.name+" (+"+mOx+") transfers electrons to "+NM.name+" (-"+nmOx+"). "+
                "Formula balanced by cross-multiplying oxidation states: "+mC+":"+nmC+".","",false,true);
        }
        int nA=a.z==1?1:Math.max(0,8-a.valence), nB=b.z==1?1:Math.max(0,8-b.valence);
        if(nA==0||nB==0)
            return new Compound("Unlikely","None","none",null,
                "Complete valence shells — bond formation unlikely under normal conditions.","",false,false);
        int g=gcd(nA,nB), aC=nB/g, bC=nA/g;
        return new Compound(a.symbol+sub(aC)+b.symbol+sub(bC),"Predicted covalent","covalent",null,
            "Covalent compound: "+a.name+" needs "+nA+" electron"+(nA>1?"s":"")+", "+b.name+" needs "+nB+
            ". Atoms share electrons to achieve stable octets (duet for H).","",false,true);
    }

    // ─────────────── BOHR MODEL PANEL ────────────────────
    static class BohrPanel extends JPanel {
        Element element;
        double time=0;
        final javax.swing.Timer timer;
        int[] sx,sy; float[] sr,sp;
        private final Font nucleusFont = new Font(Font.MONOSPACED, Font.BOLD, 14);

        BohrPanel(Element el){
            element=el;
            setBackground(BG0);
            setPreferredSize(new Dimension(340,340));
            Random rnd=new Random(7);
            sx=new int[60]; sy=new int[60]; sr=new float[60]; sp=new float[60];
            for(int i=0;i<60;i++){sx[i]=rnd.nextInt(340);sy[i]=rnd.nextInt(340);sr[i]=rnd.nextFloat()*1.2f+0.3f;sp[i]=(float)(rnd.nextDouble()*Math.PI*2);}
            timer=new javax.swing.Timer(16,e->{time+=0.016;repaint();});
            timer.start();
        }
        void stop(){ timer.stop(); }
        void start(){ timer.start(); }
        void setElement(Element el){ element=el; repaint(); }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(element==null) return;
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),cx=w/2,cy=h/2;
            g2.setColor(BG0); g2.fillRect(0,0,w,h);
            // Stars
            for(int i=0;i<60;i++){
                float a=0.25f+0.2f*(float)Math.sin(time+sp[i]);
                g2.setColor(new Color(220,232,248,(int)(a*180)));
                int r=Math.max(1,(int)sr[i]);
                g2.fillOval(sx[i]-r,sy[i]-r,r*2,r*2);
            }
            int n=element.shells.length, maxR=(int)(Math.min(cx,cy)*0.84);
            int minR=Math.min(18+element.z/7,32)+18;
            for(int si=0;si<n;si++){
                double frac=n==1?1.0:(double)si/(n-1);
                int r=minR+(int)(frac*(maxR-minR));
                int cnt=element.shells[si], dir=si%2==0?1:-1;
                double spd=0.45/(si+1);
                // Orbit ring
                g2.setColor(new Color(148,163,184,22));
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawOval(cx-r,cy-r,r*2,r*2);
                // Shell label
                g2.setFont(SANSS); g2.setColor(new Color(148,163,184,80));
                g2.drawString("n="+(si+1),cx+r-20,cy-4);
                // Electrons (cap display at 16)
                int vis=Math.min(cnt,16);
                for(int ei=0;ei<vis;ei++){
                    double base=(double)ei/vis*2*Math.PI;
                    double angle=base+time*spd*dir*7;
                    int ex=(int)(cx+r*Math.cos(angle)), ey=(int)(cy+r*Math.sin(angle));
                    // Trails
                    for(int tr=5;tr>=1;tr--){
                        double ta=base+(time-tr*0.06)*spd*dir*7;
                        int tx=(int)(cx+r*Math.cos(ta)), ty=(int)(cy+r*Math.sin(ta));
                        int alpha=Math.max(0,65-tr*13);
                        g2.setColor(new Color(56,189,248,alpha));
                        int tR=Math.max(1,3-tr/2);
                        g2.fillOval(tx-tR,ty-tR,tR*2,tR*2);
                    }
                    g2.setColor(new Color(56,189,248,0));
                    // Glow rings
                    for(int gl=3;gl>=1;gl--){
                        g2.setColor(new Color(56,189,248,20*gl));
                        g2.fillOval(ex-gl*4,ey-gl*4,gl*8,gl*8);
                    }
                    g2.setColor(new Color(125,211,252));
                    g2.fillOval(ex-4,ey-4,8,8);
                }
                if(cnt>vis){
                    g2.setColor(new Color(148,163,184,140));
                    g2.setFont(SANSS);
                    g2.drawString("+"+(cnt-vis),cx+r+4,cy+4);
                }
            }
            // Nucleus
            Color cc=element.color();
            int nR=Math.max(15,Math.min(34,11+element.z/7));
            int pR=(int)(nR*(1.0+0.05*Math.sin(time*2.5)));
            for(int gl=5;gl>=1;gl--){
                g2.setColor(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),35/gl));
                g2.fillOval(cx-pR*gl,cy-pR*gl,pR*gl*2,pR*gl*2);
            }
            g2.setColor(cc); g2.fillOval(cx-pR,cy-pR,pR*2,pR*2);
            g2.setColor(new Color(255,255,255,55)); g2.fillOval(cx-pR/2,cy-pR/2,pR,pR);
            g2.setColor(new Color(15,23,42));
            int fs=Math.max(10,Math.min(17,pR*2/3));
            g2.setFont(nucleusFont.deriveFont(Font.BOLD, (float)fs));
            FontMetrics fm=g2.getFontMetrics();
            g2.drawString(element.symbol,cx-fm.stringWidth(element.symbol)/2,cy+fm.getAscent()/2-1);
        }
    }

    // ─────────────────── ATOM TAB ─────────────────────────
    static class AtomTab extends JPanel {
        Element current;
        BohrPanel bohr;
        DefaultListModel<Element> listModel=new DefaultListModel<>();
        JList<Element> elementList;
        JTextField searchField;
        JPanel infoPanel;
        JLabel infoHeading;
        JLabel[] infoValues = new JLabel[12];

        AtomTab(){
            setLayout(new BorderLayout());
            setBackground(BG1);
            // LEFT: search + list
            JPanel left=new JPanel(new BorderLayout());
            left.setBackground(BG1);
            left.setPreferredSize(new Dimension(220,0));
            left.setBorder(BorderFactory.createMatteBorder(0,0,0,1,BD));
            searchField=new JTextField();
            searchField.setBackground(BG2); searchField.setForeground(TXT);
            searchField.setCaretColor(ACC); searchField.setFont(MONO);
            searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,BD),
                BorderFactory.createEmptyBorder(8,10,8,10)));
            ELEMENTS.forEach(listModel::addElement);
            elementList=new JList<>(listModel);
            elementList.setBackground(BG1); elementList.setForeground(TXT); elementList.setFont(MONO);
            elementList.setSelectionBackground(new Color(56,189,248,35));
            elementList.setCellRenderer((list,el,idx,sel,foc)->{
                JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,3,1));
                row.setOpaque(true);
                row.setBackground(sel?BG2:BG1);
                row.setBorder(BorderFactory.createMatteBorder(0,sel?3:0,0,0,el.color()));
                JLabel z=new JLabel(String.format("%3d",el.z));
                z.setFont(SANSS); z.setForeground(TXT3);
                JLabel s=new JLabel(el.symbol);
                s.setFont(MONOB); s.setForeground(el.color());
                s.setPreferredSize(new Dimension(26,16));
                JLabel nm=new JLabel(el.name);
                nm.setFont(SANSS); nm.setForeground(sel?TXT:TXT2);
                row.add(z); row.add(s); row.add(nm);
                return row;
            });
            searchField.getDocument().addDocumentListener(new DocumentListener(){
                void filter(){
                    String q=searchField.getText().toLowerCase();
                    listModel.clear();
                    ELEMENTS.stream().filter(e->q.isEmpty()||e.name.toLowerCase().contains(q)||
                        e.symbol.equalsIgnoreCase(q)||String.valueOf(e.z).equals(q))
                        .forEach(listModel::addElement);
                }
                public void insertUpdate(DocumentEvent e){filter();} public void removeUpdate(DocumentEvent e){filter();} public void changedUpdate(DocumentEvent e){filter();}
            });
            elementList.addListSelectionListener(e->{
                if(!e.getValueIsAdjusting()&&elementList.getSelectedValue()!=null)
                    selectElement(elementList.getSelectedValue());
            });
            JScrollPane listScroll=new JScrollPane(elementList);
            listScroll.getViewport().setBackground(BG1); listScroll.setBorder(null);
            left.add(searchField,BorderLayout.NORTH); left.add(listScroll,BorderLayout.CENTER);
            // CENTER: Bohr model
            JPanel center=new JPanel(new GridBagLayout());
            center.setBackground(BG1);
            bohr=new BohrPanel(ELEMENTS.get(5));
            center.add(bohr);
            // RIGHT: info panel
            infoPanel=new JPanel(); infoPanel.setLayout(new BoxLayout(infoPanel,BoxLayout.Y_AXIS));
            infoPanel.setBackground(BG1); infoPanel.setBorder(new EmptyBorder(16,14,16,14));
            JScrollPane infoScroll=new JScrollPane(infoPanel);
            infoScroll.getViewport().setBackground(BG1); infoScroll.setBorder(null);
            infoScroll.setPreferredSize(new Dimension(270,0));
            infoScroll.setBorder(BorderFactory.createMatteBorder(0,1,0,0,BD));
            add(left,BorderLayout.WEST); add(center,BorderLayout.CENTER); add(infoScroll,BorderLayout.EAST);
            selectElement(ELEMENTS.get(5)); // default: Carbon
        }

        JLabel infoRow(String label, String value, Color col){
            JPanel row=new JPanel(new BorderLayout()); row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
            row.setBorder(new EmptyBorder(0,0,10,0));
            JLabel lbl=new JLabel(label.toUpperCase()); lbl.setFont(SANSS); lbl.setForeground(TXT3);
            JLabel val=new JLabel("<html>"+value+"</html>"); val.setFont(MONO); val.setForeground(col!=null?col:TXT);
            row.add(lbl,BorderLayout.NORTH); row.add(val,BorderLayout.CENTER);
            return val; // unused but keeps signature
        }

        void selectElement(Element el){
            current=el; bohr.setElement(el);
            infoPanel.removeAll();
            // Helper to add a property row
            Object[] props={
                "Atomic Number", String.valueOf(el.z), null,
                "Atomic Mass", el.mass+" amu", null,
                "Period / Group", el.period+" / "+(el.group>0?el.group:"—"), null,
                "Category", el.catLabel(), el.color(),
                "Shells (Bohr)", el.shellStr(), ACC,
                "Valence Electrons", String.valueOf(el.valence), null,
                "Electronegativity", el.en>0?el.en+" (Pauling)":"—", null,
                "Oxidation States", el.oxStr(), null,
                "Melting Point", el.mp>-9000?el.mp+" °C":"—", null,
                "Boiling Point", el.bp>-9000?el.bp+" °C":"—", null,
                "Radioactive", el.isRadioactive()?"Yes":"No", el.isRadioactive()?new Color(248,113,113):new Color(74,222,128),
                "Metallic", el.isMetal()?"Yes — Metal":el.category.equals("metalloid")?"Partial — Metalloid":"No — Nonmetal", null,
            };
            JLabel heading=new JLabel(el.name+" ("+el.symbol+")");
            heading.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
            heading.setForeground(el.color());
            heading.setAlignmentX(LEFT_ALIGNMENT);
            heading.setBorder(new EmptyBorder(0,0,14,0));
            infoPanel.add(heading);
            for(int i=0;i<props.length;i+=3){
                String label=(String)props[i], value=(String)props[i+1];
                Color col=(Color)props[i+2];
                JPanel row=new JPanel(new BorderLayout()); row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
                row.setBorder(new EmptyBorder(0,0,8,0));
                row.setAlignmentX(LEFT_ALIGNMENT);
                JLabel lbl=new JLabel(label.toUpperCase()); lbl.setFont(SANSS); lbl.setForeground(TXT3);
                JLabel val=new JLabel("<html>"+value+"</html>"); val.setFont(MONO); val.setForeground(col!=null?col:TXT);
                row.add(lbl,BorderLayout.NORTH); row.add(val,BorderLayout.CENTER);
                infoPanel.add(row);
            }
            JSeparator sep=new JSeparator(); sep.setForeground(BD); sep.setBackground(BD);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));
            sep.setAlignmentX(LEFT_ALIGNMENT);
            infoPanel.add(sep); infoPanel.add(Box.createVerticalStrut(10));
            JLabel aboutLbl=new JLabel("ABOUT"); aboutLbl.setFont(SANSS); aboutLbl.setForeground(TXT3); aboutLbl.setAlignmentX(LEFT_ALIGNMENT);
            JTextArea desc=new JTextArea(el.desc); desc.setFont(SANS); desc.setForeground(TXT2);
            desc.setBackground(BG1); desc.setEditable(false); desc.setLineWrap(true); desc.setWrapStyleWord(true);
            desc.setMaximumSize(new Dimension(240,200)); desc.setAlignmentX(LEFT_ALIGNMENT);
            desc.setBorder(new EmptyBorder(6,0,0,0));
            infoPanel.add(aboutLbl); infoPanel.add(desc);
            infoPanel.revalidate(); infoPanel.repaint();
        }
    }

    // ──────────────── PERIODIC TABLE TAB ─────────────────
    static class TableTab extends JPanel {
        AtomTab atomTab; JLabel hoverBar;
        TableTab(AtomTab at){
            atomTab=at;
            setLayout(new BorderLayout()); setBackground(BG0);
            // Hover info bar
            hoverBar=new JLabel("  Hover over an element for details — click to view its atom");
            hoverBar.setFont(MONO); hoverBar.setForeground(TXT3);
            hoverBar.setOpaque(true); hoverBar.setBackground(BG1);
            hoverBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,BD),BorderFactory.createEmptyBorder(8,12,8,12)));
            add(hoverBar,BorderLayout.NORTH);
            // Legend
            JPanel legend=new JPanel(new FlowLayout(FlowLayout.LEFT,6,6));
            legend.setBackground(BG1);
            legend.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BD));
            for(Map.Entry<String,Color> e:Element.CAT_COLOR.entrySet()){
                JPanel chip=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));
                chip.setBackground(new Color(e.getValue().getRed(),e.getValue().getGreen(),e.getValue().getBlue(),25));
                chip.setBorder(BorderFactory.createLineBorder(new Color(e.getValue().getRed(),e.getValue().getGreen(),e.getValue().getBlue(),70)));
                JLabel dot=new JLabel("●"); dot.setForeground(e.getValue()); dot.setFont(SANSS);
                JLabel lbl=new JLabel(Element.CAT_LABEL.get(e.getKey())); lbl.setForeground(TXT2); lbl.setFont(SANSS);
                chip.add(dot); chip.add(lbl); legend.add(chip);
            }
            // Table panel (null layout)
            int cW=38, cH=36, gap=2;
            JPanel table=new JPanel(null);
            table.setBackground(BG0);
            table.setPreferredSize(new Dimension(18*(cW+gap)+10, 10*(cH+gap)+30));
            // Placeholders for lanthanides/actinides in main rows
            for(int[] ph:new int[][]{{6,3},{7,3}}){
                JLabel pl=new JLabel(ph[0]==6?"*":"**",SwingConstants.CENTER);
                pl.setFont(SANSS); pl.setForeground(ph[0]==6?Element.CAT_COLOR.get("lanthanide"):Element.CAT_COLOR.get("actinide"));
                pl.setBounds((ph[1]-1)*(cW+gap),(ph[0]-1)*(cH+gap),cW,cH);
                table.add(pl);
            }
            // Row labels for lanthanide/actinide rows
            for(int[] rl:new int[][]{{9,1},{10,1}}){
                JLabel lbl=new JLabel(rl[0]==9?"*":"**",SwingConstants.RIGHT);
                lbl.setFont(SANSS); lbl.setForeground(rl[0]==9?Element.CAT_COLOR.get("lanthanide"):Element.CAT_COLOR.get("actinide"));
                lbl.setBounds(0,(rl[0]-1)*(cH+gap),cW,cH);
                table.add(lbl);
            }
            for(Element el:ELEMENTS){
                int[] pos=gridPos(el); int row=pos[0],col=pos[1];
                Color cc=el.color();
                JButton btn=new JButton("<html><center><font size=1><font color='#4a6079'>"+el.z+"</font></font><br><b>"+el.symbol+"</b></center></html>");
                btn.setFont(MONOB); btn.setForeground(cc);
                btn.setBackground(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),18));
                btn.setBorder(BorderFactory.createLineBorder(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),60),1));
                btn.setFocusPainted(false);
                btn.setBounds((col-1)*(cW+gap),(row-1)*(cH+gap),cW,cH);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.addMouseListener(new MouseAdapter(){
                    public void mouseEntered(MouseEvent e){
                        btn.setBackground(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),45));
                        btn.setBorder(BorderFactory.createLineBorder(cc,1));
                        hoverBar.setText("  "+el.symbol+" ("+el.z+") — "+el.name+"   |   "+el.mass+" amu   |   "+el.catLabel()+(el.en>0?"   |   EN: "+el.en:""));
                    }
                    public void mouseExited(MouseEvent e){
                        btn.setBackground(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),18));
                        btn.setBorder(BorderFactory.createLineBorder(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),60),1));
                        hoverBar.setText("  Hover over an element for details — click to view its atom");
                    }
                });
                btn.addActionListener(e->{
                    atomTab.selectElement(el);
                    atomTab.elementList.setSelectedValue(el,true);
                    JTabbedPane tp=(JTabbedPane)SwingUtilities.getAncestorOfClass(JTabbedPane.class,this);
                    if(tp!=null) tp.setSelectedIndex(0);
                });
                table.add(btn);
            }
            JPanel content=new JPanel(new BorderLayout());
            content.setBackground(BG0);
            content.add(legend,BorderLayout.NORTH);
            content.add(new JScrollPane(table){{getViewport().setBackground(BG0);setBorder(null);}},BorderLayout.CENTER);
            add(content,BorderLayout.CENTER);
        }
    }

    // ──────────────── COMPOUND TAB ───────────────────────
    static class CompoundTab extends JPanel {
        java.util.List<int[]> tray=new ArrayList<>(); // [elementIndex, count]
        DefaultListModel<Element> pickModel=new DefaultListModel<>();
        JList<Element> pickList;
        JTextField pickSearch;
        JPanel trayPanel, resultPanel;

        CompoundTab(){
            setLayout(new BorderLayout()); setBackground(BG1);
            // LEFT: element picker
            JPanel left=new JPanel(new BorderLayout());
            left.setBackground(BG1); left.setPreferredSize(new Dimension(230,0));
            left.setBorder(BorderFactory.createMatteBorder(0,0,0,1,BD));
            pickSearch=new JTextField();
            pickSearch.setBackground(BG2); pickSearch.setForeground(TXT); pickSearch.setCaretColor(ACC); pickSearch.setFont(MONO);
            pickSearch.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,BD),BorderFactory.createEmptyBorder(8,10,8,10)));
            pickSearch.putClientProperty("JTextField.placeholderText","Search element to add…");
            ELEMENTS.forEach(pickModel::addElement);
            pickList=new JList<>(pickModel);
            pickList.setBackground(BG1); pickList.setForeground(TXT); pickList.setFont(MONO);
            pickList.setSelectionBackground(new Color(56,189,248,35));
            pickList.setCellRenderer((list,el,idx,sel,foc)->{
                JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2)); row.setOpaque(true);
                row.setBackground(sel?BG2:BG1);
                JLabel sym=new JLabel(el.symbol); sym.setFont(MONOB); sym.setForeground(el.color());
                sym.setPreferredSize(new Dimension(28,16));
                JLabel nm=new JLabel(el.name); nm.setFont(SANS); nm.setForeground(TXT2);
                row.add(sym); row.add(nm); return row;
            });
            pickSearch.getDocument().addDocumentListener(new DocumentListener(){
                void filter(){String q=pickSearch.getText().toLowerCase();pickModel.clear();ELEMENTS.stream().filter(e->q.isEmpty()||e.name.toLowerCase().contains(q)||e.symbol.equalsIgnoreCase(q)||String.valueOf(e.z).equals(q)).forEach(pickModel::addElement);}
                public void insertUpdate(DocumentEvent e){filter();} public void removeUpdate(DocumentEvent e){filter();} public void changedUpdate(DocumentEvent e){filter();}
            });
            pickList.addMouseListener(new MouseAdapter(){
                public void mouseClicked(MouseEvent e){ if(e.getClickCount()==2&&pickList.getSelectedValue()!=null) addToTray(ELEMENTS.indexOf(pickList.getSelectedValue())); }
            });
            JLabel pickHint=new JLabel("  Double-click element to add");
            pickHint.setFont(SANSS); pickHint.setForeground(TXT3); pickHint.setOpaque(true); pickHint.setBackground(BG1);
            pickHint.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,BD),BorderFactory.createEmptyBorder(6,8,6,8)));
            JScrollPane pScroll=new JScrollPane(pickList); pScroll.getViewport().setBackground(BG1); pScroll.setBorder(null);
            left.add(pickSearch,BorderLayout.NORTH); left.add(pScroll,BorderLayout.CENTER); left.add(pickHint,BorderLayout.SOUTH);
            // RIGHT: tray + results
            JPanel right=new JPanel(new BorderLayout()); right.setBackground(BG1);
            // Tray
            trayPanel=new JPanel(); trayPanel.setLayout(new BoxLayout(trayPanel,BoxLayout.Y_AXIS));
            trayPanel.setBackground(BG1); trayPanel.setBorder(new EmptyBorder(14,16,14,16));
            // Result
            resultPanel=new JPanel(); resultPanel.setLayout(new BoxLayout(resultPanel,BoxLayout.Y_AXIS));
            resultPanel.setBackground(BG1); resultPanel.setBorder(new EmptyBorder(14,16,14,16));
            JScrollPane resultScroll=new JScrollPane(resultPanel); resultScroll.getViewport().setBackground(BG1); resultScroll.setBorder(BorderFactory.createMatteBorder(1,0,0,0,BD));
            right.add(trayPanel,BorderLayout.NORTH); right.add(resultScroll,BorderLayout.CENTER);
            add(left,BorderLayout.WEST); add(right,BorderLayout.CENTER);
            refreshTray(); showResultHint();
        }

        void addToTray(int idx){
            for(int[] item:tray){ if(item[0]==idx){item[1]++;refreshTray();return;} }
            if(tray.size()>=4){ tray.remove(0); }
            tray.add(new int[]{idx,1}); refreshTray();
        }
        void removeFromTray(int idx){ tray.removeIf(item->item[0]==idx); refreshTray(); showResultHint(); }

        void refreshTray(){
            trayPanel.removeAll();
            JLabel title=new JLabel("COMPOUND TRAY"); title.setFont(SANSS); title.setForeground(TXT3); title.setAlignmentX(LEFT_ALIGNMENT);
            title.setBorder(new EmptyBorder(0,0,10,0)); trayPanel.add(title);
            if(tray.isEmpty()){
                JLabel hint=new JLabel("← Double-click elements in the left panel to add them here");
                hint.setFont(SANS); hint.setForeground(TXT3); hint.setAlignmentX(LEFT_ALIGNMENT);
                trayPanel.add(hint);
            } else {
                JPanel chips=new JPanel(new FlowLayout(FlowLayout.LEFT,6,4)); chips.setOpaque(false); chips.setAlignmentX(LEFT_ALIGNMENT);
                for(int[] item:new ArrayList<>(tray)){
                    Element el=ELEMENTS.get(item[0]); Color cc=el.color();
                    JPanel chip=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
                    chip.setBackground(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),18));
                    chip.setBorder(BorderFactory.createLineBorder(new Color(cc.getRed(),cc.getGreen(),cc.getBlue(),80)));
                    JLabel sym=new JLabel(el.symbol); sym.setFont(MONOB); sym.setForeground(cc);
                    JButton minus=new JButton("-"); minus.setFont(SANSS); minus.setForeground(TXT2); minus.setBackground(BD); minus.setBorder(BorderFactory.createEmptyBorder(1,5,1,5)); minus.setFocusPainted(false);
                    JLabel cnt=new JLabel(String.valueOf(item[1])); cnt.setFont(MONO); cnt.setForeground(TXT);
                    JButton plus=new JButton("+"); plus.setFont(SANSS); plus.setForeground(TXT2); plus.setBackground(BD); plus.setBorder(BorderFactory.createEmptyBorder(1,5,1,5)); plus.setFocusPainted(false);
                    JButton del=new JButton("×"); del.setFont(SANSS); del.setForeground(TXT3); del.setBackground(new Color(0,0,0,0)); del.setBorder(BorderFactory.createEmptyBorder(1,3,1,3)); del.setFocusPainted(false);
                    minus.addActionListener(e->{item[1]=Math.max(1,item[1]-1);refreshTray();});
                    plus.addActionListener(e->{item[1]++;refreshTray();});
                    del.addActionListener(e->removeFromTray(item[0]));
                    chip.add(sym); chip.add(minus); chip.add(cnt); chip.add(plus); chip.add(del);
                    chips.add(chip);
                }
                trayPanel.add(chips);
                // Buttons
                JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,6,6)); btns.setOpaque(false); btns.setAlignmentX(LEFT_ALIGNMENT);
                JButton analyze=new JButton("Analyze →"); analyze.setFont(MONOB); analyze.setForeground(new Color(15,23,42)); analyze.setBackground(ACC); analyze.setBorder(BorderFactory.createEmptyBorder(6,14,6,14)); analyze.setFocusPainted(false); analyze.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                JButton clear=new JButton("Clear"); clear.setFont(MONO); clear.setForeground(TXT2); clear.setBackground(BG2); clear.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BD),BorderFactory.createEmptyBorder(6,12,6,12))); clear.setFocusPainted(false);
                analyze.addActionListener(e->analyze());
                clear.addActionListener(e->{tray.clear();refreshTray();showResultHint();});
                btns.add(analyze); btns.add(clear);
                trayPanel.add(Box.createVerticalStrut(8)); trayPanel.add(btns);
            }
            trayPanel.revalidate(); trayPanel.repaint();
        }

        void analyze(){
            Map<String,Integer> keyMap=trayToMap(tray);
            Compound c=lookupCompound(keyMap);
            if(c==null && tray.size()==2) c=predictBond(tray);
            else if(c==null) c=new Compound("?","Unknown","none",null,"Multi-element compound prediction supports 2-element combinations. For 3+ elements, check the library or try 2-element pairs first.","",false,false);
            showResult(c);
        }

        void showResultHint(){
            resultPanel.removeAll();
            JLabel hint=new JLabel("<html><center>🧪<br><br>Add elements to the tray and click Analyze<br><font color='#4a6079'>The engine checks known compounds and predicts bonding via chemistry rules</font></center></html>");
            hint.setFont(SANS); hint.setForeground(TXT2); hint.setHorizontalAlignment(SwingConstants.CENTER);
            resultPanel.add(Box.createVerticalGlue()); resultPanel.add(hint); resultPanel.add(Box.createVerticalGlue());
            resultPanel.revalidate(); resultPanel.repaint();
        }

        void showResult(Compound c){
            resultPanel.removeAll();
            // Formula
            JPanel fRow=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0)); fRow.setOpaque(false); fRow.setAlignmentX(LEFT_ALIGNMENT);
            JLabel fLbl=new JLabel(c.formula); fLbl.setFont(new Font(Font.MONOSPACED,Font.BOLD,30)); fLbl.setForeground(c.typeColor());
            fRow.add(fLbl);
            if(c.name!=null&&!c.name.equals("None")){
                JLabel nLbl=new JLabel(c.name); nLbl.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14)); nLbl.setForeground(TXT2); fRow.add(nLbl);
            }
            resultPanel.add(fRow);
            resultPanel.add(Box.createVerticalStrut(10));
            // Badges
            JPanel badges=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0)); badges.setOpaque(false); badges.setAlignmentX(LEFT_ALIGNMENT);
            Color tc=c.typeColor(); String tlabel=c.type.substring(0,1).toUpperCase()+c.type.substring(1);
            JLabel typeBadge=badge(tlabel+" Bond",tc);
            if(c.known) badges.add(badge("Known Compound ✓",new Color(74,222,128)));
            if(c.predicted) badges.add(badge("Predicted",new Color(251,191,36)));
            badges.add(typeBadge);
            resultPanel.add(badges);
            // Description
            if(c.desc!=null&&!c.desc.isEmpty()){ resultPanel.add(Box.createVerticalStrut(14)); resultPanel.add(card("Chemistry / Bonding",c.desc)); }
            if(c.uses!=null&&!c.uses.isEmpty()){ resultPanel.add(Box.createVerticalStrut(8)); resultPanel.add(card("Uses",c.uses)); }
            if(c.shape!=null){ resultPanel.add(Box.createVerticalStrut(8)); resultPanel.add(card("Molecular Geometry (VSEPR)",c.shape)); }
            // Element breakdown
            if(!tray.isEmpty()){
                resultPanel.add(Box.createVerticalStrut(8));
                JPanel ecards=new JPanel(); ecards.setLayout(new BoxLayout(ecards,BoxLayout.Y_AXIS)); ecards.setOpaque(false); ecards.setAlignmentX(LEFT_ALIGNMENT);
                JLabel elbl=new JLabel("ELEMENTS IN TRAY"); elbl.setFont(SANSS); elbl.setForeground(TXT3); elbl.setAlignmentX(LEFT_ALIGNMENT);
                ecards.add(elbl); ecards.add(Box.createVerticalStrut(6));
                for(int[] item:tray){
                    Element el=ELEMENTS.get(item[0]); Color cc=el.color();
                    JPanel erow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
                    erow.setBackground(BG2); erow.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BD),BorderFactory.createEmptyBorder(2,4,2,4)));
                    erow.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
                    JLabel esym=new JLabel(el.symbol+" ×"+item[1]); esym.setFont(MONOB); esym.setForeground(cc);
                    JLabel einfo=new JLabel(el.name+" | Z="+el.z+" | "+el.catLabel()+" | Valence: "+el.valence);
                    einfo.setFont(SANSS); einfo.setForeground(TXT2);
                    erow.add(esym); erow.add(einfo);
                    ecards.add(erow); ecards.add(Box.createVerticalStrut(4));
                }
                resultPanel.add(ecards);
            }
            resultPanel.revalidate(); resultPanel.repaint();
        }

        static JLabel badge(String text, Color col){
            JLabel b=new JLabel(text); b.setFont(SANSS); b.setForeground(col);
            b.setOpaque(true); b.setBackground(new Color(col.getRed(),col.getGreen(),col.getBlue(),25));
            b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(col.getRed(),col.getGreen(),col.getBlue(),70)),BorderFactory.createEmptyBorder(3,8,3,8)));
            return b;
        }
        static JPanel card(String title, String content){
            JPanel p=new JPanel(new BorderLayout()); p.setBackground(BG2); p.setAlignmentX(LEFT_ALIGNMENT);
            p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BD),BorderFactory.createEmptyBorder(10,12,10,12)));
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE,200));
            JLabel t=new JLabel(title.toUpperCase()); t.setFont(SANSS); t.setForeground(TXT3);
            JTextArea c=new JTextArea(content); c.setFont(SANS); c.setForeground(TXT2); c.setBackground(BG2);
            c.setEditable(false); c.setLineWrap(true); c.setWrapStyleWord(true); c.setBorder(new EmptyBorder(6,0,0,0));
            p.add(t,BorderLayout.NORTH); p.add(c,BorderLayout.CENTER);
            return p;
        }
    }

    // ─────────────── MAIN WINDOW ─────────────────────────
    static class MainWindow extends JFrame {
        MainWindow(){
            setTitle("⚛ Atomic Simulator");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1000,660));
            setPreferredSize(new Dimension(1150,720));
            getContentPane().setBackground(BG0);
            // Header
            JPanel header=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
            header.setBackground(BG1); header.setPreferredSize(new Dimension(0,44));
            header.setBorder(BorderFactory.createMatteBorder(0,0,1,BD.getRed()>0?1:0,BD));
            JLabel title=new JLabel("  ⚛  ATOMIC SIMULATOR  ");
            title.setFont(new Font(Font.MONOSPACED,Font.BOLD,13)); title.setForeground(ACC);
            title.setBorder(BorderFactory.createMatteBorder(0,0,0,1,BD));
            title.setPreferredSize(new Dimension(190,44)); title.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel sub=new JLabel("   All 118 elements · Bohr model animation · Chemistry rules · Compound builder");
            sub.setFont(SANSS); sub.setForeground(TXT3);
            header.add(title); header.add(sub);
            add(header,BorderLayout.NORTH);
            // Tabs
            AtomTab atomTab=new AtomTab();
            JTabbedPane tabs=new JTabbedPane();
            tabs.setBackground(BG1); tabs.setForeground(TXT2);
            tabs.addTab("⚛  Atom View",atomTab);
            tabs.addTab("📋  Periodic Table",new TableTab(atomTab));
            tabs.addTab("🔗  Compounds",new CompoundTab());
            tabs.addChangeListener(e -> {
                if(tabs.getSelectedIndex() == 0) atomTab.bohr.start();
                else atomTab.bohr.stop();
            }); 
            add(tabs,BorderLayout.CENTER);
            pack();
            setLocationRelativeTo(null);
        }
    }

    // ─────────────── ENTRY POINT ──────────────────────────
    public static void main(String[] args){
        // Apply dark system UI hints before creating any components
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext","true");
        try {
            for(UIManager.LookAndFeelInfo info:UIManager.getInstalledLookAndFeels())
                if("Nimbus".equals(info.getName())){ UIManager.setLookAndFeel(info.getClassName()); break; }
            UIManager.put("control",BG2); UIManager.put("nimbusBase",BG1);
            UIManager.put("nimbusLightBackground",BG2); UIManager.put("text",TXT);
            UIManager.put("nimbusSelectedText",TXT); UIManager.put("nimbusSelectionBackground",new Color(56,189,248,80));
            UIManager.put("TabbedPane.contentAreaColor",BG1); UIManager.put("TabbedPane.selected",BG2);
        } catch(Exception ignored){}
        SwingUtilities.invokeLater(()->new MainWindow().setVisible(true));
    }
}
