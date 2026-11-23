# Area Hints · 修订配音

54 句普通话旁白采用本机 GPT-SoVITS v2ProPlus 的“流萤”预设合成，使用预设附带的开心语气参考。润色台词和配音时间窗在 `cues.tsv`。模型包中的权重与参考音频单独保存在本机，包内未见发布授权说明。

`Voice.wav` 为独立干声。旁白停顿回到数字静音，句间响度以线性增益调整。`Game.wav` 对环境声做了频谱降噪和衰减。开场前 450 毫秒的游戏轨 RMS 由约 −39 dBFS 降至 −52 dBFS。`Score.wav` 为独立器乐。三个分轨均从时间零点起放，按单位增益相加即为 `Mix.wav`，总长 206 秒，采样率 48 kHz。终混实测 −16.00 LUFS-I、−2.80 dBTP。

## 重制

在本目录运行。第一步使用 GPT-SoVITS 附带的 Python，第二步使用已安装 NumPy、SciPy 的 Python 和 FFmpeg。更换安装路径时，向 `synthesize.py` 传入 `--engine` 和 `--model`。每句的实际发音时长用于控制字幕，品牌、键名及地图联动名称的发音分段在 `pronunciation.py`。

```powershell
& 'G:/GPT-SoVITS-v2pro-20250604-nvidia50/runtime/python.exe' -X utf8 -B -u synthesize.py
python -X utf8 finish.py --lock-srt narration.srt
```

已用独立中文识别器核对实际生成的语音。中英文混读处仍会出现识别差异，Area Hints 和 Xaero 的最终发音可分别在 00:13、02:28 试听。
