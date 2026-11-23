# 206 秒音轨

在 PR 中将 `Voice.wav`、`Score.wav`、`Game.wav` 从零点对齐，三个轨道增益均设为 0 dB。三者的采样值之和等于 `Mix.wav`。`narration.srt` 含 54 条字幕，入点按普通话意群分配，出点按合成波形的实际长度计算。

| WAV | 内容 | 综合响度 | 真峰值 |
| --- | --- | ---: | ---: |
| Voice | 普通话旁白 | −15.58 LUFS-I | −1.70 dBTP |
| Score | 原创拨弦、持续弦音、木质打击乐 | −28.07 LUFS-I | −7.07 dBTP |
| Game | 游戏声、首次录点轻敲、两声射线交点 | −38.04 LUFS-I | −18.38 dBTP |
| Mix | 投稿混音 | −16.01 LUFS-I | −1.72 dBTP |

每个 WAV 均为 48 kHz、16 位立体声 PCM，共 9,888,000 个立体声采样帧。开场先留脚步，地图匹配切的“走进去”始于 165.970 秒。最后一句字幕在 203.080 秒结束，尾奏延续至 206 秒。

## 重做

在本目录执行下列命令。环境使用 Python 3.13、NumPy、SciPy、sherpa-onnx 2.0.3 和 FFmpeg。依赖中的中英双语 MeloTTS 权重为 `model.int8.onnx`，SHA-256 为 `f085f5079e05f039b800aeb542f5253c26a303211b0c6465d0d9387977855a63`。本机沿用试制版的临时模型目录，其他机器可以给 `narrate.py` 指定 `--model` 和 `--library`。旁白使用合成普通话女声，配乐和三声提示音由脚本在本地生成。

```powershell
python -X utf8 narrate.py
python -X utf8 score.py
python -X utf8 mix.py
```

修改旁白时编辑 `cues.tsv` 的时间窗与台词。游戏声使用八个实机中间片中的独立游戏声道，来自 OBS 第四条录音轨。修改内嵌实机的剪点时，同步修改 `mix.py` 中对应的游戏声入点。
