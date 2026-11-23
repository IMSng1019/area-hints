"""在本机 ComfyUI 中以描述书实机首帧为参考，重绘风起地过场插画。"""
import argparse
import sys
from pathlib import Path

import numpy as np
import torch
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument('--comfy', required=True, type=Path)
parser.add_argument('--checkpoint', required=True, type=Path)
parser.add_argument('--source', required=True, type=Path)
parser.add_argument('--output', required=True, type=Path)
parser.add_argument('--scene', choices=('tree', 'harbor'), default='tree')
args = parser.parse_args()
sys.argv = sys.argv[:1]
sys.path.insert(0, str(args.comfy))
import folder_paths
import nodes

positive = (
    "masterpiece, best quality, no humans, landscape, a single enormous ancient oak tree "
    "with a broad emerald canopy in a sunlit grassy meadow, pale blue sky, early morning, "
    "a tiny stone monument under the tree, golden wind, luminous leaf edges, "
    "delicate storybook gouache painting, watercolor on warm ivory textured paper, "
    "handpainted botanical illustration, soft pigments, atmospheric perspective, "
    "expressive brushstrokes, subtle ink linework, beautiful restrained color palette"
)
negative = (
    "people, character, girl, face, text, signature, watermark, writing, "
    "letters, interface, photograph, 3d render, red oversaturated colors, "
    "heavy outlines, oversharpened, blurry, low quality"
)
if args.scene == 'harbor':
    positive = (
        "masterpiece landscape painting, no humans, Chinese coastal harbor village, "
        "ancient Chinese curved tiled roofs, terraced houses and a stone wharf, mountain bay, "
        "serene ochre mist on blue green water, hills with pavilions, dawn light, "
        "traditional travel journal illustration, delicate line and wash, detailed pen and ink, "
        "watercolor on warm paper, limited ochre celadon and indigo color palette"
    )
folder_paths.add_model_folder_path("checkpoints", str(args.checkpoint.parent))
with torch.inference_mode():
    model, clip, vae = nodes.CheckpointLoaderSimple().load_checkpoint(args.checkpoint.name)
    pixels = Image.open(args.source).convert("RGB")
    if args.scene == 'harbor':
        pixels = pixels.crop((320, 240, 1810, 860))
    pixels = pixels.resize((960, 544), Image.Resampling.LANCZOS)
    reference = torch.tensor(np.asarray(pixels).copy(), dtype=torch.float32).unsqueeze(0) / 255
    latent = nodes.VAEEncode().encode(vae, reference)[0]
    encoder = nodes.CLIPTextEncode()
    latent = nodes.common_ksampler(model, 831726, 28, 6.5, 'dpmpp_2m', 'karras',
                                  encoder.encode(clip, positive)[0], encoder.encode(clip, negative)[0],
                                  latent, denoise=0.57)[0]
    result = nodes.VAEDecode().decode(vae, latent)[0][0].clamp(0, 1).cpu().numpy()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray((result * 255).astype(np.uint8)).save(args.output, quality=96, subsampling=0)
