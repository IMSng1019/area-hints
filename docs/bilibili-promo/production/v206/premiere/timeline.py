"""将总剪帧表写为 Premiere 可导入的 FCP 7 XML，保留源片修剪余量。"""
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
BASE = HERE.parent
FPS = 60
RATE = 48000
TITLE = "Areas Hint · 走进提瓦特"


def tag(parent, name, value=None, **attributes):
    element = ET.SubElement(parent, name, attributes)
    if value is not None:
        element.text = str(value)
    return element


def tags(parent, **values):
    for name, value in values.items():
        tag(parent, name, value)


def rate(parent):
    tags(tag(parent, "rate"), timebase=FPS, ntsc="FALSE")


def picture(parent):
    characteristics = tag(parent, "samplecharacteristics")
    rate(characteristics)
    tags(characteristics, width=1920, height=1080, anamorphic="FALSE",
         pixelaspectratio="square", fielddominance="none")


def sound(parent, depth):
    tags(tag(parent, "samplecharacteristics"), depth=depth, samplerate=RATE)


def inspect(path, kind):
    bundled = Path(os.environ["LOCALAPPDATA"]) / "Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static/ffprobe.exe"
    probe = os.environ.get("FFPROBE") or shutil.which("ffprobe") or str(bundled)
    result = subprocess.check_output([probe, "-v", "error", "-select_streams", kind + ":0",
                                      "-show_streams", "-of", "json", str(path)])
    stream = json.loads(result)["streams"][0]
    if kind == "v":
        assert stream["avg_frame_rate"] == "60/1", path
        return int(stream["nb_frames"]), 0, 0
    assert int(stream["sample_rate"]) == RATE, path
    return round(float(stream["duration"]) * FPS), stream["channels"], stream["bits_per_sample"]


def build():
    edit = json.loads((BASE / "edit.json").read_text(encoding="utf-8"))
    total = edit["frames"]
    assert edit["fps"] == FPS and total == 12360
    xml = ET.Element("xmeml", version="5")
    seq = tag(xml, "sequence", id="areas-hint-v206", explodedTracks="true")
    tags(seq, name=TITLE, duration=total)
    rate(seq)
    timecode = tag(seq, "timecode")
    rate(timecode)
    tags(timecode, string="00:00:00:00", frame=0, displayformat="NDF")
    media = tag(seq, "media")
    video = tag(media, "video")
    picture(tag(video, "format"))
    track = tag(video, "track")
    audio = tag(media, "audio")
    tag(audio, "numOutputChannels", 2)
    sound(tag(audio, "format"), 24)
    outputs = tag(audio, "outputs")
    for channel in (1, 2):
        group = tag(outputs, "group")
        tags(group, index=channel, numchannels=1, downmix=0)
        tag(tag(group, "channel"), "index", channel)
    sources = {}
    item_index = 0

    def clip(lane, name, relative, start, source_in, length, kind, channel=None):
        nonlocal item_index
        path = (BASE / relative).resolve()
        first = path not in sources
        if first:
            sources[path] = (f"asset-{len(sources) + 1}", *inspect(path, kind))
        asset_id, frames, channels, depth = sources[path]
        assert source_in + length <= frames, (path, source_in, length, frames)
        item_index += 1
        item = tag(lane, "clipitem", id=f"item-{item_index}")
        if channel is not None:
            assert channels == 2
            item.set("premiereChannelType", "stereo")
            tag(item, "masterclipid", "master-" + asset_id)
        tags(item, name=name, enabled="TRUE", duration=frames)
        rate(item)
        tags(item, start=start, end=start + length)
        tag(item, "in", source_in)
        tag(item, "out", source_in + length)
        asset = tag(item, "file", id=asset_id)
        if first:
            tags(asset, name=path.name, pathurl=path.as_uri().replace("file:///", "file://localhost/"))
            rate(asset)
            tag(asset, "duration", frames)
            attributes = tag(asset, "media")
            if kind == "v":
                picture(tag(attributes, "video"))
            else:
                item_audio = tag(attributes, "audio")
                sound(item_audio, depth)
                tag(item_audio, "channelcount", channels)
        if kind == "a":
            tags(tag(item, "sourcetrack"), mediatype="audio", trackindex=channel)
        return item

    end = 0
    for cut in edit["clips"]:
        assert cut["start"] == end
        clip(track, f'{cut["shot"]:02d} · {Path(cut["path"]).stem}', cut["path"],
             end, cut["in"], cut["duration"], "v")
        end += cut["duration"]
    assert end == total
    stems = [("Voice", "普通话旁白"), ("Score", "原创配乐"), ("Game", "游戏声与音效")]
    for stem_index, (filename, label) in enumerate(stems):
        pair = []
        # Premiere 的交换格式将立体声轨展开为左右两条，并通过声道索引还原轨道。
        for channel in (1, 2):
            lane = tag(audio, "track", premiereTrackType="Stereo",
                       currentExplodedTrackIndex=str(channel - 1), totalExplodedTrackCount="2")
            pair.append(clip(lane, label, f"audio/{filename}.wav", 0, 0, total, "a", channel))
            tags(lane, enabled="TRUE", locked="FALSE", outputchannelindex=channel)
        for item in pair:
            for index, partner in enumerate(pair, 1):
                ref = tag(item, "link")
                tags(ref, linkclipref=partner.get("id"), mediatype="audio",
                     trackindex=stem_index * 2 + index, clipindex=1, groupindex=1)
    for chapter in edit["chapters"]:
        marker = tag(seq, "marker")
        tag(marker, "name", chapter["name"])
        tag(marker, "comment", "<CHAPTER>")
        tag(marker, "in", chapter["start"])
        tag(marker, "out", -1)
    ET.indent(xml)
    file = HERE / "Areas-Hint.xml"
    file.write_bytes(b'<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE xmeml>\n' +
                     ET.tostring(xml, encoding="utf-8"))
    print(f"{file}: {item_index} clips, {len(sources)} assets, {total} frames")


if __name__ == "__main__":
    build()
