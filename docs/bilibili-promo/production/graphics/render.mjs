import {existsSync, mkdirSync, renameSync} from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";
import {bundle} from "@remotion/bundler";
import {getCompositions, renderMedia} from "@remotion/renderer";

const root = path.dirname(fileURLToPath(import.meta.url));
const output = path.resolve(root, "../output");
const cache = path.resolve(root, ".cache");
mkdirSync(cache, {recursive: true});
mkdirSync(output, {recursive: true});
const browserExecutable = [
  process.env.REMOTION_BROWSER_EXECUTABLE,
  "C:/Program Files/Google/Chrome/Application/chrome.exe",
  "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
].find(candidate => candidate && existsSync(candidate));
const serveUrl = await bundle({
  entryPoint: path.join(root, "src/index.jsx"), publicDir: path.join(root, "public"),
  outDir: path.join(cache, "bundle-" + process.pid),
});
const settings = {serveUrl, browserExecutable, port: Number(process.env.REMOTION_RENDER_PORT || 42181)};
const compositions = await getCompositions(settings);
const requested = process.argv.slice(2);
for (const id of requested) {
  if (!compositions.some(composition => composition.id === id)) throw new Error("Unknown composition " + id);
}
for (const composition of compositions.filter(item => !requested.length || requested.includes(item.id))) {
  const target = path.join(output, composition.id + ".mp4");
  const staging = path.join(cache, composition.id + "-" + process.pid + ".mp4");
  let reported = -1;
  await renderMedia({...settings, composition, outputLocation: staging, codec:"h264", crf:16,
    colorSpace:"bt709", muted:true, concurrency:4, jpegQuality:94,
    onProgress: ({progress}) => {
      const step = Math.floor(progress * 4);
      if (step !== reported) console.log(composition.id, step * 25 + "%");
      reported = step;
    },
  });
  renameSync(staging, target);
  console.log("Ready", target);
}
