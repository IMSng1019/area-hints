"""用本地流萤 v2ProPlus 预设朗读已定稿的台词；中间语句缓存到校片目录。"""
import argparse
import csv
import os
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from pronunciation import cache_key, segments

HERE = Path(__file__).resolve().parent
WORK = HERE.parents[1] / "review" / "voice-revision"
ENGINE = Path("G:/GPT-SoVITS-v2pro-20250604-nvidia50")
MODEL = Path("Q:/gpt-sovits/v2ProPlus/流萤")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine", type=Path, default=ENGINE)
    parser.add_argument("--model", type=Path, default=MODEL)
    parser.add_argument("--only", type=int, nargs="+")
    args = parser.parse_args()
    engine = args.engine.resolve()
    model = args.model.resolve()
    WORK.mkdir(exist_ok=True, parents=True)
    os.chdir(engine)
    os.environ["PATH"] = str(engine / "runtime") + os.pathsep + os.environ["PATH"]
    os.environ["NLTK_DATA"] = str(engine / "runtime/nltk_data")
    os.environ["NUMBA_CACHE_DIR"] = str(WORK / "numba-cache")
    os.environ["HF_HOME"] = str(WORK / "hf-cache")
    os.environ["HF_HUB_OFFLINE"] = "1"
    os.environ["TRANSFORMERS_OFFLINE"] = "1"
    sys.path[:0] = [str(engine), str(engine / "GPT_SoVITS")]
    from TTS_infer_pack.TTS import TTS, TTS_Config
    import soundfile as sf
    import torch
    import numpy as np

    conf = dict(TTS_Config.default_configs["v2ProPlus"])
    conf.update(device="cuda" if torch.cuda.is_available() else "cpu", is_half=torch.cuda.is_available(),
                t2s_weights_path=str(next(model.glob("*.ckpt"))), vits_weights_path=str(next(model.glob("*.pth"))))
    config = TTS_Config({"custom": conf})
    # 运行时会保存推理设置，只把本次副本写在工程校片目录。
    config.configs_path = str(WORK / "voice-config.yaml")
    tts = TTS(config)
    reference = next(p for p in model.rglob("*.wav") if p.name.startswith("【开心】"))
    reference_text = reference.stem.split("】")[-1]
    rows = list(csv.DictReader((HERE / "cues.tsv").read_text(encoding="utf8").splitlines(), delimiter="\t"))
    for i, row in enumerate(rows, 1):
        if args.only and i not in args.only:
            continue
        parts = segments(row["line"])
        key = cache_key(parts)
        wav = WORK / f"{i:02d}-{key}.wav"
        if wav.is_file():
            continue
        options = dict(ref_audio_path=str(reference), prompt_text=reference_text,
                       prompt_lang="all_zh", text_split_method="cut0", speed_factor=1.0, seed=1100 + i,
                       top_k=5, top_p=1, temperature=0.93, fragment_interval=0.05,
                       parallel_infer=True, split_bucket=False, batch_size=1)
        pieces = []
        for lang, phrase in parts:
            rate, fragment = next(tts.run(dict(options, text=phrase, text_lang=lang)))
            if len(parts) == 1 and lang == "zh":
                pieces.append(fragment)
                continue
            # 模型会补充句尾静音，拼接英文专名时去除过长间隔。
            valid = np.flatnonzero(np.abs(fragment.astype("float32")) > 100)
            if not valid.size:
                raise ValueError(phrase)
            pieces.append(fragment[max(0, valid[0]-rate//40):valid[-1]+rate//20])
        pcm = np.concatenate(pieces)
        if len(pcm) < rate * 0.18 or abs(pcm.astype("float32")).max() < 64:
            raise RuntimeError(f"第 {i} 条生成异常")
        sf.write(wav.with_suffix(".tmp.wav"), pcm, rate, subtype="PCM_16")
        wav.with_suffix(".tmp.wav").replace(wav)
        print(f"语句 {i:02d}  {len(pcm)/rate:.2f} 秒  {row['line']}", flush=True)


if __name__ == "__main__":
    main()
