# Pikselowa grafika aurelii — 0.1.42

Trzy autorskie zasoby RGBA utworzono wbudowanym generatorem obrazów (`image_gen`, tryb built-in). Oryginalne PNG z przezroczystością zachowano w źródłach:

- `src/main/resources/assets/crimeandlaw/textures/cultivation/aurelia_leaf.png`
- `src/main/resources/assets/crimeandlaw/textures/cultivation/pruning_shears.png`
- `src/main/resources/assets/crimeandlaw/textures/cultivation/aurelia_seeds.png`

`PixelSprite` odczytuje te źródła przy przeładowaniu zasobów. W grze każdy materiał ma dokładnie 32×32 piksele, własną paletę 12–16 kolorów, binarną alfę i wymuszone filtrowanie nearest. Próbkowanie środków komórek i ograniczenie palety są częścią renderera, a oryginały pozostają niezmienione. Zasoby są poza atlasem bloków, więc nie zmieniają jego mipmap. Łodyga ma własny materiał 8×8 generowany z czterech zielonych kolorów; żaden model nie zapożycza tekstur bloków Minecrafta.

Model rośliny ma sześć osobnych, kanciasto zagiętych liści z przednią, tylną i bocznymi powierzchniami. Sylwetki są schodkowe i mają grubość. Sekator, nasiona i liść w ekwipunku także mają solidne boczne ścianki, jak przestrzenne przedmioty Minecrafta. Współliniowe krawędzie są łączone zamiast budowania kostki dla każdego piksela. Zastępuje to realistyczny liść i modele korzystające z materiałów bloków z 0.1.41.

## Pełne prompty

### Liść

> Use case: stylized-concept. Production asset for a Minecraft-style voxel game. TRUE low-resolution PIXEL ART, a strict 32 by 32 logical pixel grid enlarged with nearest-neighbor only. Large crisp square pixels, hard stepped contours, 10-14 flat colors total, absolutely NO realistic surface detail, no soft gradients, no antialiasing, no lighting bloom, no shadows outside the sprite, no background, no text. One object only, centered and large, square canvas with fully transparent background. Front orthographic view, not isometric; will be extruded into a 3D voxel item by game code. A single fictional aurelia leaf pointing straight UP, central short stem at bottom, broad angular oval shape with three short stepped lobes each side, moss and sage green with a muted golden central vein and a few single-pixel vein highlights. Strong dark green edge, muted olive shadows. Symmetric enough for sampling, clean connected silhouette, about 24 of 32 pixels wide and 29 high. Game texture rather than illustration.

### Nożyce

> Use case: stylized-concept. Production asset for a Minecraft-style voxel game. TRUE low-resolution PIXEL ART, a strict 32 by 32 logical pixel grid enlarged with nearest-neighbor only. Large crisp square pixels, hard stepped contours, 10-14 flat colors total, absolutely NO realistic surface detail, no soft gradients, no antialiasing, no lighting bloom, no shadows outside the sprite, no background, no text. One object only, centered and large, square canvas with fully transparent background. Front orthographic view, not isometric; will be extruded into a 3D voxel item by game code. One distinctive pair of pruning shears, recognisable as a Minecraft inventory tool. Diagonal from lower left to upper right; two open dark teal/copper grip loops lower left, solid copper hinge in center, two slightly open sharp silver steel blades upper right, subtle steel-blue flat pixels for blade edges. Contiguous sturdy silhouette. NOT a collage of block textures. Designed in a strict 32x32 pixel grid, 28 pixels tall.

### Nasiona

> Use case: stylized-concept. Production asset for a Minecraft-style voxel game. TRUE low-resolution PIXEL ART, a strict 32 by 32 logical pixel grid enlarged with nearest-neighbor only. Large crisp square pixels, hard stepped contours, 10-14 flat colors total, absolutely NO realistic surface detail, no soft gradients, no antialiasing, no lighting bloom, no shadows outside the sprite, no background, no text. One object only, centered and large, square canvas with fully transparent background. Front orthographic view, not isometric; will be extruded into a 3D voxel item by game code. A small cluster of THREE plump fictional aurelia seeds, pointed oval seeds with golden ochre shells, dark brown outlines, a short muted sage green seam on each seed, a few square highlight pixels. Three overlapping large clearly readable seeds. Strong coherent Minecraft inventory sprite with crisp 32x32 pixel-grid geometry; no pouch, no soil, no leaves, no other objects.
