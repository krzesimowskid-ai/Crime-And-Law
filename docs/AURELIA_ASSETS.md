# Pochodzenie grafiki aurelii

Końcowy zasób: `src/main/resources/assets/crimeandlaw/textures/plant/aurelia_leaf.png`.
Oryginalny PNG RGBA, 1254×1254, przezroczyste tło, 1 962 253 bajty.
Obraz utworzono wbudowanym generatorem obrazów (`image_gen`, tryb built-in)
na potrzeby tej modyfikacji. Zachowano oryginalne piksele i kanał alfa.
Tekstura jest odczytywana bezpośrednio przez renderery z mipmapami.

Geometria zakrzywionych liści, łodygi i ogonków jest zdefiniowana w
`AureliaMesh.java`; modele nożyc i nasion są autorskimi modelami JSON,
korzystającymi ze standardowych materiałów Minecrafta. Nie jest to kopia
modelu rzeczywistej rośliny narkotycznej.

## Pełny prompt tekstury

> Use case: stylized-concept. Asset type: final production UV texture for a detailed 3D fictional plant in a Minecraft game mod. Create exactly ONE single broad ovate leaf, flat orthographic overhead view, perfectly centered on a square 1024x1024 transparent canvas. Leaf blade spans x=120..904 and y=70..850, pointed tip at the TOP, short narrow petiole at the BOTTOM ending near y=960; no other objects. Rich emerald and jade green with subtle warm golden central midrib and branching fine veins, slightly violet shading at the serrated edges, fine natural cellular microtexture, softly curled edge highlights. Beautiful believable fantasy botany, high quality game texture with hand painted realism, lighting baked very gently and evenly. It is an original fantasy ornamental leaf, single undivided blade, NOT cannabis and not a real narcotic plant. Crisp detailed silhouette with small teeth and fully transparent outside; no drop shadow, no ground, no white outline, no text, no logo, no watermark, no texture grid. This is a flat leaf albedo UV image, NOT a render of a plant, NOT a background illustration. Preserve visible detail at 256-pixel downsampling. Alpha transparent background.

Generator zwrócił 1254×1254 zamiast wymiaru opisanego w prompcie.
Zasób nie jest dołączany jako sprite do wspólnego atlasu bloków;
osobny renderer zachowuje pełny łańcuch mipmap także dla tego rozmiaru.
