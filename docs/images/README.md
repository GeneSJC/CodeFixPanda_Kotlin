# README artwork

The images used by the front-page `README.md`.

| File | Used for | Replace with |
|---|---|---|
| `banner.png` | The hero image at the top | Keep, or a new design at about 1600 x 520 |
| `divider.png` | The gradient rule between sections | Keep |
| `panda.png` | The footer mascot, made from `panda-source.png` | Keep |
| `panda-source.png` | The mascot from the launcher icon art; the only input `scripts/make_art.py` reads | Keep |
| `screen-welcome.jpg`, `screen-mode-gate.jpg`, `screen-top-repos-beginner.jpg` | The first row of the "Screens" gallery: the token gate, the mode gate, and Top Repos in the blue mode | Real phone screenshots, resized to 540 px wide. Retake them with the same file names when the screens change |
| `screen-top-repos.jpg`, `screen-repo-issues.jpg` | The second row: Top Repos (orange mode) and a repo's issues | Same as above |
| `placeholder-demo.png` | The demo slot under the gallery. It is hidden in the README for now (an HTML comment) | An animated `demo.gif` (keep it under about 8 MB; GitHub shows it inline). Unhide the block in the README and change the one `src` path |

`scripts/make_art.py` generates the banner, divider, panda and the demo placeholder. It is README tooling only: it needs Pillow, reads
`panda-source.png` from this folder and has nothing to do with the Android app. Run it from the repository root (`python3 scripts/make_art.py`) after changing colors or text. The real screenshots and GIF are not
generated; just add them.

Tips: take screenshots in light mode with the demo data (nothing personal on screen), and keep each image under about
400 KB so the page loads fast (an optimizer such as `pngquant` or Squoosh helps).

Note on the navigation row: each README section has an explicit `<a id="..."></a>` anchor above its heading, and the row links to those. Headings with an emoji such as the map or the building carry a hidden character that GitHub keeps in its generated link, so links to the generated anchor can silently fail.
