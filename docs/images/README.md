# README artwork

The images used by the front-page `README.md`.

| File | Used for | Replace with |
|---|---|---|
| `banner.png` | The hero image at the top | Keep, or a new design at about 1600 x 520 |
| `divider.png` | The gradient rule between sections | Keep |
| `panda.png` | The footer mascot | Keep |
| `placeholder-welcome.png`, `placeholder-top-repos.png`, `placeholder-repo-issues.png` | The three phone screenshots in the "Screens" gallery | Real screenshots, about 540 x 1100 (or any phone-shaped size). Save as `screen-welcome.png`, `screen-top-repos.png`, `screen-repo-issues.png` and change the three `src` paths in the README |
| `placeholder-demo.png` | The demo slot under the gallery | An animated `demo.gif` (keep it under about 8 MB; GitHub shows it inline). Change the one `src` path in the README |

`make_art.py` generates the banner, divider, panda and placeholders (needs Pillow; it reads the app's own panda from
`app/src/main/res`). Run it from this folder after changing colors or text. The real screenshots and GIF are not
generated; just add them.

Tips: take screenshots in light mode with the demo data (nothing personal on screen), and keep each PNG under about
400 KB so the page loads fast (an optimizer such as `pngquant` or Squoosh helps).

Note on the navigation row: each README section has an explicit `<a id="..."></a>` anchor above its heading, and the row links to those. Headings with an emoji such as the map or the building carry a hidden character that GitHub keeps in its generated link, so links to the generated anchor can silently fail.
