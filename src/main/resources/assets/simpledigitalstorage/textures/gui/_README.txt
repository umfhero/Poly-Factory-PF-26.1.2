GUI TEXTURES — Place your .png files here.

Expected files:
  storage_interface.png     — The main Storage Interface GUI background texture

GUI textures are typically 256x256 .png files (Minecraft convention),
where the actual visible area is drawn in the top-left portion
(e.g. 176x166 for a standard inventory-style GUI).

The texture is referenced in the Screen class like:
  ResourceLocation.fromNamespaceAndPath("simpledigitalstorage", "textures/gui/storage_interface.png")

Delete this file once you've added your textures.
